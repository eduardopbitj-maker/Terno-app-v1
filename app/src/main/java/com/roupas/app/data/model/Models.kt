package com.roupas.app.data.model

/**
 * Tipos de peça suportados pelo aplicativo.
 * O [prefixo] é usado para gerar/validar o código único de cada peça (T01, G01, C01...).
 */
enum class TipoPeca(val prefixo: String, val label: String) {
    TERNO("T", "Terno"),
    GRAVATA("G", "Gravata"),
    CAMISA("C", "Camisa");

    companion object {
        fun porPrefixo(prefixo: String): TipoPeca? = entries.find { it.prefixo == prefixo }
    }
}

/**
 * Modos de visualização de listas de peças (seção 5 do prompt).
 */
enum class ModoVisualizacao {
    DETALHADO, COMPACTO, GRADE, PECAS
}

/**
 * Uma peça independente (terno, gravata ou camisa).
 * Nunca é duplicada: combinações e associações sempre referenciam o [codigo].
 */
data class Peca(
    val id: Long = 0,
    val tipo: TipoPeca,
    val nome: String,
    val codigo: String,
    val fotoPath: String? = null
)

/**
 * Associação simples gravata -> terno (independente de combinação completa),
 * usada para popular a lista de gravatas ao navegar a partir de um terno.
 */
data class AssociacaoTernoGravata(
    val ternoCodigo: String,
    val gravataCodigo: String
)

/**
 * Uma combinação só existe com as 3 peças presentes (regra da seção 1.4).
 */
data class Combinacao(
    val id: Long = 0,
    val ternoCodigo: String,
    val gravataCodigo: String,
    val camisaCodigo: String
)

/**
 * Um registro de uso de uma combinação em uma data específica (calendário).
 * Um mesmo dia pode ter mais de um registro (seção 9, item 6).
 */
data class RegistroUso(
    val id: Long = 0,
    val data: String, // formato ISO: yyyy-MM-dd
    val combinacaoId: Long
)

/**
 * Estado visual de um dos 3 indicadores de utilização (seção 8.2).
 * O valor [progresso] vai de 0f (uso recém-feito, vermelho) a 1f (30 dias completos, verde),
 * usado para interpolar a cor vermelho -> amarelo -> verde. null = quadrado transparente (sem uso ativo).
 */
data class IndicadorUso(
    val progresso: Float?,
    val dataUso: String?
)
