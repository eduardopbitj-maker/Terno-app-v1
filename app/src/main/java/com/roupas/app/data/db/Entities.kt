package com.roupas.app.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tabela de peças. A unicidade do código DENTRO do tipo é garantida por um índice único
 * composto (tipo + codigo) — reforça em nível de banco a regra da seção 1.2 do prompt,
 * além da validação feita na camada de repositório (que dá a mensagem amigável ao usuário).
 */
@Entity(
    tableName = "pecas",
    indices = [Index(value = ["tipo", "codigo"], unique = true)]
)
data class PecaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tipo: String,       // "TERNO" | "GRAVATA" | "CAMISA"
    val nome: String,
    val codigo: String,     // ex: T01, G01, C01
    val fotoPath: String? = null
)

/**
 * Associação gravata <-> terno (seção 1.3). Não duplica peças, apenas referencia códigos.
 */
@Entity(
    tableName = "associacoes_terno_gravata",
    indices = [Index(value = ["ternoCodigo", "gravataCodigo"], unique = true)]
)
data class AssociacaoTernoGravataEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ternoCodigo: String,
    val gravataCodigo: String
)

/**
 * Combinação completa terno+gravata+camisa (seção 1.4). Única por trio de códigos.
 */
@Entity(
    tableName = "combinacoes",
    indices = [Index(value = ["ternoCodigo", "gravataCodigo", "camisaCodigo"], unique = true)]
)
data class CombinacaoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ternoCodigo: String,
    val gravataCodigo: String,
    val camisaCodigo: String
)

/**
 * Registro de uso no calendário. Vários registros podem existir no mesmo dia (seção 9.6).
 */
@Entity(tableName = "registros_uso")
data class RegistroUsoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val data: String,          // yyyy-MM-dd
    val combinacaoId: Long
)
