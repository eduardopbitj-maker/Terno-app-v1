package com.roupas.app.data.repository

import com.roupas.app.data.db.*
import com.roupas.app.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Resultado de operações que podem falhar por regra de negócio (ex: código duplicado). */
sealed class ResultadoOperacao {
    data object Sucesso : ResultadoOperacao()
    data class Erro(val mensagem: String) : ResultadoOperacao()
}

class RoupasRepository(private val db: AppDatabase) {

    private val pecaDao = db.pecaDao()
    private val associacaoDao = db.associacaoDao()
    private val combinacaoDao = db.combinacaoDao()
    private val registroUsoDao = db.registroUsoDao()

    private fun PecaEntity.toModel() = Peca(id, TipoPeca.valueOf(tipo), nome, codigo, fotoPath)

    // ---------- PEÇAS ----------

    fun observarPecas(tipo: TipoPeca): Flow<List<Peca>> =
        pecaDao.observarPorTipo(tipo.name).map { lista -> lista.map { it.toModel() } }

    /**
     * Cadastra uma peça nova validando a unicidade do código dentro do tipo (seção 1.2).
     * Retorna Erro com mensagem amigável se o código já estiver em uso.
     */
    suspend fun cadastrarPeca(tipo: TipoPeca, nome: String, codigo: String, fotoPath: String?): ResultadoOperacao {
        if (nome.isBlank() || codigo.isBlank()) {
            return ResultadoOperacao.Erro("Nome e código são obrigatórios.")
        }
        val existente = pecaDao.buscarPorCodigo(tipo.name, codigo)
        if (existente != null) {
            return ResultadoOperacao.Erro("O código \"$codigo\" já está em uso para ${tipo.label.lowercase()}s.")
        }
        pecaDao.inserir(PecaEntity(tipo = tipo.name, nome = nome, codigo = codigo, fotoPath = fotoPath))
        return ResultadoOperacao.Sucesso
    }

    /** Edita uma peça existente, revalidando unicidade de código (ignorando ela mesma). */
    suspend fun editarPeca(id: Long, nome: String, codigo: String, fotoPath: String?): ResultadoOperacao {
        val atual = pecaDao.buscarPorId(id) ?: return ResultadoOperacao.Erro("Peça não encontrada.")
        if (nome.isBlank() || codigo.isBlank()) {
            return ResultadoOperacao.Erro("Nome e código são obrigatórios.")
        }
        val existente = pecaDao.buscarPorCodigo(atual.tipo, codigo)
        if (existente != null && existente.id != id) {
            return ResultadoOperacao.Erro("O código \"$codigo\" já está em uso.")
        }
        pecaDao.atualizar(atual.copy(nome = nome, codigo = codigo, fotoPath = fotoPath))
        return ResultadoOperacao.Sucesso
    }

    /** Gera o próximo código disponível de um tipo (ex.: T01, T02, T03...). Usado no formulário e na importação. */
    suspend fun proximoCodigoDisponivel(tipo: TipoPeca): String {
        val existentes = pecaDao.listarPorTipo(tipo.name).map { it.codigo }
        var n = 1
        while (existentes.contains("${tipo.prefixo}%02d".format(n))) n++
        return "${tipo.prefixo}%02d".format(n)
    }

    /**
     * Exclui uma peça e todos os dados dependentes (associações, combinações, registros de uso),
     * evitando referências inválidas (seção 12 e 20).
     */
    suspend fun excluirPeca(peca: Peca) {
        // Remove primeiro os registros de calendário das combinações afetadas, depois as
        // combinações em si, depois as associações soltas, e por fim a peça — nessa ordem
        // para nunca deixar referência inválida (seção 12 e 20).
        val relacionadas = combinacaoDao.listarRelacionadasAoCodigo(peca.codigo)
        relacionadas.forEach { comb ->
            registroUsoDao.excluirDaCombinacao(comb.id)
            combinacaoDao.excluir(comb.id)
        }
        associacaoDao.excluirRelacionadasA(ternoCodigo = peca.codigo, gravataCodigo = peca.codigo)
        pecaDao.excluir(peca.id)
    }

    // ---------- ASSOCIAÇÕES (gravata <-> ternos) ----------

    fun observarGravatasDoTerno(ternoCodigo: String): Flow<List<String>> =
        associacaoDao.observarGravatasDoTerno(ternoCodigo).map { it.map { a -> a.gravataCodigo } }

    suspend fun contarCombinacoesDoTerno(ternoCodigo: String): Int =
        combinacaoDao.contarCombinacoesDoTerno(ternoCodigo)

    suspend fun associarGravataATernos(gravataCodigo: String, ternosCodigos: List<String>) {
        ternosCodigos.forEach { terno ->
            associacaoDao.inserir(AssociacaoTernoGravataEntity(ternoCodigo = terno, gravataCodigo = gravataCodigo))
        }
    }

    // ---------- COMBINAÇÕES (terno + gravata + camisa) ----------

    fun observarCamisasDaCombinacao(ternoCodigo: String, gravataCodigo: String): Flow<List<String>> =
        combinacaoDao.observarCamisasDaCombinacao(ternoCodigo, gravataCodigo).map { it.map { c -> c.camisaCodigo } }

    /** Cria a combinação terno+gravata+camisa e garante também a associação terno<->gravata. */
    suspend fun criarCombinacao(ternoCodigo: String, gravataCodigo: String, camisaCodigo: String): Long {
        associacaoDao.inserir(AssociacaoTernoGravataEntity(ternoCodigo = ternoCodigo, gravataCodigo = gravataCodigo))
        val existente = combinacaoDao.buscar(ternoCodigo, gravataCodigo, camisaCodigo)
        if (existente != null) return existente.id
        return combinacaoDao.inserir(
            CombinacaoEntity(ternoCodigo = ternoCodigo, gravataCodigo = gravataCodigo, camisaCodigo = camisaCodigo)
        )
    }

    // ---------- CALENDÁRIO / USO ----------

    private val formatoData = DateTimeFormatter.ISO_LOCAL_DATE

    fun observarUsosDoDia(data: LocalDate): Flow<List<RegistroUso>> =
        registroUsoDao.observarPorData(data.format(formatoData)).map { lista ->
            lista.map { RegistroUso(it.id, it.data, it.combinacaoId) }
        }

    fun observarTodosOsUsos(): Flow<List<RegistroUso>> =
        registroUsoDao.observarTodos().map { lista -> lista.map { RegistroUso(it.id, it.data, it.combinacaoId) } }

    suspend fun registrarUso(combinacaoId: Long, data: LocalDate): Long =
        registroUsoDao.inserir(RegistroUsoEntity(data = data.format(formatoData), combinacaoId = combinacaoId))

    suspend fun removerUso(registroId: Long) = registroUsoDao.excluir(registroId)

    suspend fun contarUsos(combinacaoId: Long): Int = registroUsoDao.contarUsos(combinacaoId)

    /**
     * Calcula os 3 indicadores de uso de uma combinação (seção 8, agora com prazo
     * configurável — ajuste "Prazo dos indicadores de utilização"): as 3 utilizações mais
     * recentes ainda dentro da janela de [prazoDias] dias, cada uma com seu progresso
     * (0 = recém-usado / vermelho, 1 = completando o prazo / verde). O histórico completo
     * nunca é apagado — apenas a janela de exibição muda com o prazo.
     */
    fun observarIndicadores(combinacaoId: Long, prazoDias: Int): Flow<List<IndicadorUso>> =
        registroUsoDao.observarUltimosTres(combinacaoId).map { registros ->
            val hoje = LocalDate.now()
            val ativos = registros.mapNotNull { r ->
                val data = LocalDate.parse(r.data, formatoData)
                val diasPassados = java.time.temporal.ChronoUnit.DAYS.between(data, hoje)
                if (diasPassados in 0..prazoDias.toLong()) {
                    IndicadorUso(progresso = (diasPassados.toFloat() / prazoDias).coerceIn(0f, 1f), dataUso = r.data)
                } else null
            }
            // completa até 3 quadrados com "vazio" (transparente)
            (ativos + List(3) { IndicadorUso(null, null) }).take(3)
        }

    suspend fun ultimaUtilizacao(combinacaoId: Long): String? =
        registroUsoDao.historicoDaCombinacao(combinacaoId).firstOrNull()?.data

    // ---------- PESQUISA E FILTROS (seção 11) ----------

    /** Pesquisa por nome ou código, opcionalmente restringindo por tipo e por presença de foto. */
    suspend fun pesquisarPecas(termo: String, tipo: TipoPeca?, comFoto: Boolean?): List<Peca> {
        val resultado = if (termo.isBlank()) {
            TipoPeca.entries.flatMap { pecaDao.listarPorTipo(it.name) }
        } else {
            pecaDao.pesquisar(termo)
        }
        return resultado
            .filter { tipo == null || it.tipo == tipo.name }
            .filter { comFoto == null || (it.fotoPath != null) == comFoto }
            .map { it.toModel() }
    }

    /** Estatísticas de uso de cada combinação, para os filtros "não usada em 30 dias" / "menos usada". */
    fun observarCombinacoesComEstatisticas(): Flow<List<CombinacaoComEstatisticas>> =
        kotlinx.coroutines.flow.combine(
            combinacaoDao.observarTodas(),
            registroUsoDao.observarTodos()
        ) { combinacoes, registros ->
            val porCombinacao = registros.groupBy { it.combinacaoId }
            combinacoes.map { comb ->
                val usos = porCombinacao[comb.id].orEmpty()
                val ultima = usos.maxByOrNull { it.data }?.data
                val diasDesdeUltimoUso = ultima?.let {
                    java.time.temporal.ChronoUnit.DAYS.between(LocalDate.parse(it, formatoData), LocalDate.now())
                }
                CombinacaoComEstatisticas(
                    combinacao = Combinacao(comb.id, comb.ternoCodigo, comb.gravataCodigo, comb.camisaCodigo),
                    totalUsos = usos.size,
                    ultimaUtilizacao = ultima,
                    diasDesdeUltimoUso = diasDesdeUltimoUso
                )
            }
        }

    // ---------- IMPORTAÇÃO / EXPORTAÇÃO ZIP (seções 14-19) ----------

    /** Valida o ZIP e devolve o resumo (seção 15) + os dados brutos, para confirmar antes de gravar. */
    fun validarImportacaoZip(arquivo: java.io.File) = com.roupas.app.util.ZipImportExport.validarEler(arquivo)

    /** Efetivamente grava os dados validados no banco, resolvendo conflitos de código (seção 16). */
    suspend fun aplicarImportacaoZip(dadosBrutos: Any, pastaFotosDestino: java.io.File) =
        com.roupas.app.util.ZipImportExport.aplicarImportacao(db, dadosBrutos, pastaFotosDestino)

    /** Gera o ZIP de exportação (seções 17-19) no arquivo temporário indicado. */
    suspend fun exportarZip(destinoTemporario: java.io.File) =
        com.roupas.app.util.ZipImportExport.exportar(db, destinoTemporario)
}

/** Estatísticas de uso de uma combinação, usadas nos filtros de pesquisa (seção 11). */
data class CombinacaoComEstatisticas(
    val combinacao: Combinacao,
    val totalUsos: Int,
    val ultimaUtilizacao: String?,
    val diasDesdeUltimoUso: Long?
)
