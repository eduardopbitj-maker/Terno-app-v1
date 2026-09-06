package com.roupas.app.util

import com.roupas.app.data.db.AppDatabase
import com.roupas.app.data.db.AssociacaoTernoGravataEntity
import com.roupas.app.data.db.CombinacaoEntity
import com.roupas.app.data.db.PecaEntity
import com.roupas.app.data.db.RegistroUsoEntity
import com.roupas.app.data.model.TipoPeca
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Resumo mostrado ao usuário antes de confirmar a importação (seção 15). */
data class ResumoImportacao(
    val ternos: Int,
    val gravatas: Int,
    val camisas: Int,
    val combinacoes: Int,
    val fotos: Int,
    val registrosCalendario: Int,
    val erros: List<String>
)

/**
 * Estrutura intermediária lida do ZIP antes de gravar no banco — permite mostrar o resumo
 * (seção 15) antes de qualquer escrita, e depois aplicar a resolução de conflitos de código
 * (seção 16) de forma consistente entre legendas, combinações e calendário.
 */
private data class DadosImportados(
    val legendas: MutableMap<TipoPeca, MutableList<Pair<String, String>>> = mutableMapOf(
        TipoPeca.TERNO to mutableListOf(), TipoPeca.GRAVATA to mutableListOf(), TipoPeca.CAMISA to mutableListOf()
    ),
    val combinacoes: MutableList<Triple<String, String, String>> = mutableListOf(),
    val calendario: MutableList<Pair<String, Triple<String, String, String>>> = mutableListOf(), // data -> (T,G,C)
    val fotos: MutableMap<String, ByteArray> = mutableMapOf(), // codigo -> bytes do jpg
    val erros: MutableList<String> = mutableListOf()
)

object ZipImportExport {

    /** Lê e valida a estrutura do ZIP (seção 15), sem gravar nada ainda. */
    fun validarEler(arquivoZip: File): Pair<ResumoImportacao, Any> {
        val dados = DadosImportados()

        if (!arquivoZip.name.endsWith(".zip", ignoreCase = true)) {
            dados.erros += "O arquivo selecionado não é um .zip."
        }

        runCatching {
            ZipInputStream(arquivoZip.inputStream()).use { zis ->
                var entrada: ZipEntry? = zis.nextEntry
                var possuiCombinacoes = false
                while (entrada != null) {
                    val nome = entrada.name
                    when {
                        nome.endsWith("legendas.txt") -> lerLegendas(zis.readBytes().decodeToString(), dados)
                        nome.endsWith("combinacoes.txt") || nome.endsWith("combinações.txt") -> {
                            possuiCombinacoes = true
                            lerCombinacoes(zis.readBytes().decodeToString(), dados)
                        }
                        nome.endsWith("calendario.txt") || nome.endsWith("calendário.txt") ->
                            lerCalendario(zis.readBytes().decodeToString(), dados)
                        nome.contains("/terno/") || nome.contains("/gravata/") || nome.contains("/camisa/") -> {
                            if (nome.endsWith(".jpg", ignoreCase = true)) {
                                val codigo = File(nome).nameWithoutExtension
                                dados.fotos[codigo] = zis.readBytes()
                            }
                        }
                    }
                    zis.closeEntry()
                    entrada = zis.nextEntry
                }
                if (!possuiCombinacoes) {
                    dados.erros += "O arquivo combinações.txt não foi encontrado (peças ainda podem ser importadas)."
                }
            }
        }.onFailure { dados.erros += "Não foi possível ler o arquivo ZIP: ${it.message}" }

        // Detecta combinações apontando para peças inexistentes (seção 15)
        val todosCodigos = dados.legendas.values.flatten().map { it.first }.toSet()
        dados.combinacoes.forEach { (t, g, c) ->
            if (t !in todosCodigos) dados.erros += "Combinação aponta para terno inexistente: $t"
            if (g !in todosCodigos) dados.erros += "Combinação aponta para gravata inexistente: $g"
            if (c !in todosCodigos) dados.erros += "Combinação aponta para camisa inexistente: $c"
        }

        // Detecta códigos duplicados dentro do próprio arquivo importado
        TipoPeca.entries.forEach { tipo ->
            val codigos = dados.legendas[tipo]!!.map { it.first }
            val duplicados = codigos.groupingBy { it }.eachCount().filter { it.value > 1 }.keys
            if (duplicados.isNotEmpty()) dados.erros += "Códigos duplicados de ${tipo.label.lowercase()}: ${duplicados.joinToString()}"
        }

        val resumo = ResumoImportacao(
            ternos = dados.legendas[TipoPeca.TERNO]!!.size,
            gravatas = dados.legendas[TipoPeca.GRAVATA]!!.size,
            camisas = dados.legendas[TipoPeca.CAMISA]!!.size,
            combinacoes = dados.combinacoes.size,
            fotos = dados.fotos.size,
            registrosCalendario = dados.calendario.size,
            erros = dados.erros
        )
        return resumo to dados
    }

    /**
     * Aplica os dados lidos ao banco, resolvendo conflitos de código (seção 16): peças cujo
     * código já existe recebem o próximo código disponível da sequência, e essa troca é
     * propagada para combinações e registros de calendário.
     */
    suspend fun aplicarImportacao(db: AppDatabase, dadosBrutos: Any, pastaFotosDestino: File) {
        @Suppress("UNCHECKED_CAST")
        val dados = dadosBrutos as DadosImportados
        val pecaDao = db.pecaDao()
        val combinacaoDao = db.combinacaoDao()
        val registroUsoDao = db.registroUsoDao()
        val associacaoDao = db.associacaoDao()

        // mapa código-antigo -> código-novo (por tipo)
        val remapeamento = mutableMapOf<String, String>()

        TipoPeca.entries.forEach { tipo ->
            var proximoNumero = (pecaDao.listarPorTipo(tipo.name).mapNotNull {
                it.codigo.removePrefix(tipo.prefixo).toIntOrNull()
            }.maxOrNull() ?: 0) + 1

            dados.legendas[tipo]!!.forEach { (codigoOriginal, nome) ->
                val existente = pecaDao.buscarPorCodigo(tipo.name, codigoOriginal)
                val codigoFinal = if (existente == null) codigoOriginal else {
                    val novo = "${tipo.prefixo}%02d".format(proximoNumero)
                    proximoNumero++
                    novo
                }
                remapeamento[codigoOriginal] = codigoFinal

                var fotoPath: String? = null
                dados.fotos[codigoOriginal]?.let { bytes ->
                    val arquivo = File(pastaFotosDestino, "$codigoFinal.jpg")
                    arquivo.writeBytes(bytes)
                    fotoPath = arquivo.absolutePath
                }

                pecaDao.inserir(PecaEntity(tipo = tipo.name, nome = nome, codigo = codigoFinal, fotoPath = fotoPath))
            }
        }

        // Combinações, já com os códigos remapeados
        val combinacaoIdPorTrio = mutableMapOf<Triple<String, String, String>, Long>()
        dados.combinacoes.forEach { (t, g, c) ->
            val tFinal = remapeamento[t] ?: return@forEach
            val gFinal = remapeamento[g] ?: return@forEach
            val cFinal = remapeamento[c] ?: return@forEach
            associacaoDao.inserir(AssociacaoTernoGravataEntity(ternoCodigo = tFinal, gravataCodigo = gFinal))
            val idExistente = combinacaoDao.buscar(tFinal, gFinal, cFinal)?.id
            val id = idExistente ?: combinacaoDao.inserir(CombinacaoEntity(ternoCodigo = tFinal, gravataCodigo = gFinal, camisaCodigo = cFinal))
            combinacaoIdPorTrio[Triple(tFinal, gFinal, cFinal)] = id
        }

        // Calendário, remapeado para as combinações recém-criadas
        dados.calendario.forEach { (data, trio) ->
            val (t, g, c) = trio
            val trioFinal = Triple(remapeamento[t] ?: return@forEach, remapeamento[g] ?: return@forEach, remapeamento[c] ?: return@forEach)
            val combinacaoId = combinacaoIdPorTrio[trioFinal] ?: return@forEach
            registroUsoDao.inserir(RegistroUsoEntity(data = data, combinacaoId = combinacaoId))
        }
    }

    private fun lerLegendas(conteudo: String, dados: DadosImportados) {
        var secaoAtual: TipoPeca? = null
        conteudo.lines().forEach { linhaBruta ->
            val linha = linhaBruta.trim()
            when {
                linha.equals("[TERNOS]", true) -> secaoAtual = TipoPeca.TERNO
                linha.equals("[GRAVATAS]", true) -> secaoAtual = TipoPeca.GRAVATA
                linha.equals("[CAMISAS]", true) -> secaoAtual = TipoPeca.CAMISA
                linha.contains("|") && secaoAtual != null -> {
                    val partes = linha.split("|", limit = 2)
                    if (partes.size == 2) dados.legendas[secaoAtual]!!.add(partes[0].trim() to partes[1].trim())
                }
            }
        }
    }

    private fun lerCombinacoes(conteudo: String, dados: DadosImportados) {
        conteudo.lines().forEach { linhaBruta ->
            val linha = linhaBruta.trim()
            if (linha.contains("|") && !linha.startsWith("[")) {
                val partes = linha.split("|")
                if (partes.size == 3) dados.combinacoes.add(Triple(partes[0].trim(), partes[1].trim(), partes[2].trim()))
            }
        }
    }

    private fun lerCalendario(conteudo: String, dados: DadosImportados) {
        conteudo.lines().forEach { linhaBruta ->
            val linha = linhaBruta.trim()
            if (linha.contains("|") && !linha.startsWith("[")) {
                val partes = linha.split("|")
                if (partes.size == 4) {
                    dados.calendario.add(partes[0].trim() to Triple(partes[1].trim(), partes[2].trim(), partes[3].trim()))
                }
            }
        }
    }

    /**
     * Gera o ZIP de exportação (seções 17–19): combinações.txt, legendas.txt, calendário.txt
     * e a pasta de fotos organizada por tipo, cada foto nomeada com o código da peça.
     */
    suspend fun exportar(db: AppDatabase, destino: File) {
        val pecaDao = db.pecaDao()
        val ternos = pecaDao.listarPorTipo(TipoPeca.TERNO.name)
        val gravatas = pecaDao.listarPorTipo(TipoPeca.GRAVATA.name)
        val camisas = pecaDao.listarPorTipo(TipoPeca.CAMISA.name)
        val combinacoes = db.combinacaoDao().listarTodas()

        ZipOutputStream(destino.outputStream()).use { zos ->
            // legendas.txt
            val legendas = buildString {
                appendLine("[TERNOS]")
                ternos.forEach { appendLine("${it.codigo}|${it.nome}") }
                appendLine("[GRAVATAS]")
                gravatas.forEach { appendLine("${it.codigo}|${it.nome}") }
                appendLine("[CAMISAS]")
                camisas.forEach { appendLine("${it.codigo}|${it.nome}") }
            }
            escreverEntrada(zos, "legendas.txt", legendas)

            // combinações.txt
            val combinacoesTxt = buildString {
                appendLine("[COMBINACOES]")
                combinacoes.forEach { appendLine("${it.ternoCodigo}|${it.gravataCodigo}|${it.camisaCodigo}") }
            }
            escreverEntrada(zos, "combinações.txt", combinacoesTxt)

            // calendário.txt
            val registros = db.registroUsoDao().listarTodosComCombinacao()
            val calendarioTxt = buildString {
                appendLine("[CALENDARIO]")
                registros.forEach { appendLine("${it.data}|${it.ternoCodigo}|${it.gravataCodigo}|${it.camisaCodigo}") }
            }
            escreverEntrada(zos, "calendário.txt", calendarioTxt)

            // fotos
            (ternos.map { "terno" to it } + gravatas.map { "gravata" to it } + camisas.map { "camisa" to it })
                .forEach { (pasta, peca) ->
                    peca.fotoPath?.let { caminho ->
                        val arquivo = File(caminho)
                        if (arquivo.exists()) {
                            zos.putNextEntry(ZipEntry("fotos/$pasta/${peca.codigo}.jpg"))
                            arquivo.inputStream().use { it.copyTo(zos) }
                            zos.closeEntry()
                        }
                    }
                }
        }
    }

    private fun escreverEntrada(zos: ZipOutputStream, nome: String, conteudo: String) {
        zos.putNextEntry(ZipEntry(nome))
        zos.write(conteudo.toByteArray())
        zos.closeEntry()
    }
}
