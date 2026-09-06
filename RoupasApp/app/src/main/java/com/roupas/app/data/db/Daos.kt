package com.roupas.app.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PecaDao {
    @Query("SELECT * FROM pecas WHERE tipo = :tipo ORDER BY codigo ASC")
    fun observarPorTipo(tipo: String): Flow<List<PecaEntity>>

    @Query("SELECT * FROM pecas WHERE tipo = :tipo ORDER BY codigo ASC")
    suspend fun listarPorTipo(tipo: String): List<PecaEntity>

    @Query("SELECT * FROM pecas WHERE id = :id LIMIT 1")
    suspend fun buscarPorId(id: Long): PecaEntity?

    @Query("SELECT * FROM pecas WHERE tipo = :tipo AND codigo = :codigo LIMIT 1")
    suspend fun buscarPorCodigo(tipo: String, codigo: String): PecaEntity?

    @Query("SELECT * FROM pecas WHERE nome LIKE '%' || :termo || '%' OR codigo LIKE '%' || :termo || '%'")
    suspend fun pesquisar(termo: String): List<PecaEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun inserir(peca: PecaEntity): Long

    @Update
    suspend fun atualizar(peca: PecaEntity)

    @Query("DELETE FROM pecas WHERE id = :id")
    suspend fun excluir(id: Long)

    @Query("SELECT COUNT(*) FROM pecas WHERE tipo = :tipo")
    suspend fun contarPorTipo(tipo: String): Int
}

@Dao
interface AssociacaoDao {
    @Query("SELECT * FROM associacoes_terno_gravata WHERE ternoCodigo = :ternoCodigo")
    fun observarGravatasDoTerno(ternoCodigo: String): Flow<List<AssociacaoTernoGravataEntity>>

    @Query("SELECT * FROM associacoes_terno_gravata WHERE gravataCodigo = :gravataCodigo")
    suspend fun ternosDaGravata(gravataCodigo: String): List<AssociacaoTernoGravataEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun inserir(associacao: AssociacaoTernoGravataEntity)

    // Recebe o mesmo código nos dois parâmetros quando se quer excluir tudo relacionado a um
    // único código de peça (funciona pois prefixos T/G nunca colidem entre si).
    @Query("DELETE FROM associacoes_terno_gravata WHERE ternoCodigo = :ternoCodigo OR gravataCodigo = :gravataCodigo")
    suspend fun excluirRelacionadasA(ternoCodigo: String, gravataCodigo: String)
}

@Dao
interface CombinacaoDao {
    @Query("SELECT * FROM combinacoes")
    fun observarTodas(): Flow<List<CombinacaoEntity>>

    @Query("SELECT * FROM combinacoes WHERE ternoCodigo = :ternoCodigo AND gravataCodigo = :gravataCodigo")
    fun observarCamisasDaCombinacao(ternoCodigo: String, gravataCodigo: String): Flow<List<CombinacaoEntity>>

    @Query("SELECT * FROM combinacoes WHERE ternoCodigo = :ternoCodigo AND gravataCodigo = :gravataCodigo AND camisaCodigo = :camisaCodigo LIMIT 1")
    suspend fun buscar(ternoCodigo: String, gravataCodigo: String, camisaCodigo: String): CombinacaoEntity?

    @Query("SELECT * FROM combinacoes WHERE id = :id LIMIT 1")
    suspend fun buscarPorId(id: Long): CombinacaoEntity?

    @Query("SELECT * FROM combinacoes WHERE ternoCodigo = :codigo OR gravataCodigo = :codigo OR camisaCodigo = :codigo")
    suspend fun listarRelacionadasAoCodigo(codigo: String): List<CombinacaoEntity>

    @Query("SELECT * FROM combinacoes")
    suspend fun listarTodas(): List<CombinacaoEntity>

    @Query("SELECT COUNT(*) FROM combinacoes WHERE ternoCodigo = :ternoCodigo")
    suspend fun contarCombinacoesDoTerno(ternoCodigo: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun inserir(combinacao: CombinacaoEntity): Long

    @Query("DELETE FROM combinacoes WHERE id = :id")
    suspend fun excluir(id: Long)

    @Query("DELETE FROM combinacoes WHERE ternoCodigo = :codigo OR gravataCodigo = :codigo OR camisaCodigo = :codigo")
    suspend fun excluirComReferenciaA(codigo: String)
}

@Dao
interface RegistroUsoDao {
    @Query("SELECT * FROM registros_uso WHERE data = :data")
    fun observarPorData(data: String): Flow<List<RegistroUsoEntity>>

    @Query("SELECT * FROM registros_uso WHERE combinacaoId = :combinacaoId ORDER BY data DESC")
    suspend fun historicoDaCombinacao(combinacaoId: Long): List<RegistroUsoEntity>

    @Query("SELECT * FROM registros_uso WHERE combinacaoId = :combinacaoId ORDER BY data DESC LIMIT 3")
    fun observarUltimosTres(combinacaoId: Long): Flow<List<RegistroUsoEntity>>

    @Query("SELECT COUNT(*) FROM registros_uso WHERE combinacaoId = :combinacaoId")
    suspend fun contarUsos(combinacaoId: Long): Int

    @Query("SELECT * FROM registros_uso ORDER BY data DESC")
    fun observarTodos(): Flow<List<RegistroUsoEntity>>

    @Insert
    suspend fun inserir(registro: RegistroUsoEntity): Long

    @Query("DELETE FROM registros_uso WHERE id = :id")
    suspend fun excluir(id: Long)

    @Query("DELETE FROM registros_uso WHERE combinacaoId = :combinacaoId")
    suspend fun excluirDaCombinacao(combinacaoId: Long)

    @Query("DELETE FROM registros_uso")
    suspend fun limparTudo()

    @Query(
        """
        SELECT r.data as data, c.ternoCodigo as ternoCodigo, c.gravataCodigo as gravataCodigo, c.camisaCodigo as camisaCodigo
        FROM registros_uso r INNER JOIN combinacoes c ON r.combinacaoId = c.id
        ORDER BY r.data ASC
        """
    )
    suspend fun listarTodosComCombinacao(): List<RegistroComCombinacao>
}

/** Projeção usada apenas na exportação do calendário.txt (data + códigos da combinação). */
data class RegistroComCombinacao(
    val data: String,
    val ternoCodigo: String,
    val gravataCodigo: String,
    val camisaCodigo: String
)
