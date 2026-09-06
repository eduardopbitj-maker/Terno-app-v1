package com.roupas.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roupas.app.data.model.*
import com.roupas.app.data.repository.CombinacaoComEstatisticas
import com.roupas.app.data.repository.ResultadoOperacao
import com.roupas.app.data.repository.RoupasRepository
import com.roupas.app.util.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class RoupasViewModel(
    private val repo: RoupasRepository,
    private val prefs: PreferencesManager
) : ViewModel() {

    // Ternos, gravatas e camisas cadastrados — observados diretamente do banco (Flow).
    val ternos: StateFlow<List<Peca>> =
        repo.observarPecas(TipoPeca.TERNO).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val gravatas: StateFlow<List<Peca>> =
        repo.observarPecas(TipoPeca.GRAVATA).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val camisas: StateFlow<List<Peca>> =
        repo.observarPecas(TipoPeca.CAMISA).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todosOsUsos: StateFlow<List<RegistroUso>> =
        repo.observarTodosOsUsos().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val conjuntos: StateFlow<List<CombinacaoComEstatisticas>> =
        repo.observarCombinacoesComEstatisticas().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ---- Modo de visualização: persistido e único para toda a navegação (ajuste 3) ----
    private val _modoVisualizacao = MutableStateFlow(prefs.lerModoVisualizacao())
    val modoVisualizacao: StateFlow<ModoVisualizacao> = _modoVisualizacao
    fun definirModoVisualizacao(modo: ModoVisualizacao) {
        _modoVisualizacao.value = modo
        prefs.salvarModoVisualizacao(modo)
    }

    // ---- Prazo dos indicadores de utilização: persistido, padrão 30 dias (ajuste 1) ----
    private val _prazoIndicadorDias = MutableStateFlow(prefs.lerPrazoIndicadorDias())
    val prazoIndicadorDias: StateFlow<Int> = _prazoIndicadorDias
    fun definirPrazoIndicadorDias(dias: Int) {
        if (dias <= 0) return
        _prazoIndicadorDias.value = dias
        prefs.salvarPrazoIndicadorDias(dias)
    }

    private val _erro = MutableStateFlow<String?>(null)
    val erro: StateFlow<String?> = _erro
    fun limparErro() { _erro.value = null }

    fun cadastrarPeca(tipo: TipoPeca, nome: String, codigo: String, fotoPath: String?, aoConcluir: () -> Unit) {
        viewModelScope.launch {
            when (val resultado = repo.cadastrarPeca(tipo, nome, codigo, fotoPath)) {
                is ResultadoOperacao.Sucesso -> aoConcluir()
                is ResultadoOperacao.Erro -> _erro.value = resultado.mensagem
            }
        }
    }

    fun editarPeca(id: Long, nome: String, codigo: String, fotoPath: String?, aoConcluir: () -> Unit) {
        viewModelScope.launch {
            when (val resultado = repo.editarPeca(id, nome, codigo, fotoPath)) {
                is ResultadoOperacao.Sucesso -> aoConcluir()
                is ResultadoOperacao.Erro -> _erro.value = resultado.mensagem
            }
        }
    }

    fun excluirPeca(peca: Peca, aoConcluir: () -> Unit) {
        viewModelScope.launch {
            repo.excluirPeca(peca)
            aoConcluir()
        }
    }

    suspend fun proximoCodigo(tipo: TipoPeca) = repo.proximoCodigoDisponivel(tipo)

    fun associarGravataATernos(gravataCodigo: String, ternos: List<String>) {
        viewModelScope.launch { repo.associarGravataATernos(gravataCodigo, ternos) }
    }

    fun criarCombinacao(ternoCodigo: String, gravataCodigo: String, camisaCodigo: String, aoConcluir: (Long) -> Unit) {
        viewModelScope.launch { aoConcluir(repo.criarCombinacao(ternoCodigo, gravataCodigo, camisaCodigo)) }
    }

    fun registrarUso(combinacaoId: Long, data: LocalDate) {
        viewModelScope.launch { repo.registrarUso(combinacaoId, data) }
    }

    fun removerUso(registroId: Long) {
        viewModelScope.launch { repo.removerUso(registroId) }
    }

    fun observarGravatasDoTerno(ternoCodigo: String) = repo.observarGravatasDoTerno(ternoCodigo)
    fun observarCamisasDaCombinacao(ternoCodigo: String, gravataCodigo: String) =
        repo.observarCamisasDaCombinacao(ternoCodigo, gravataCodigo)

    /** Indicadores da combinação já reagindo ao prazo configurável (ajuste 1). */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun observarIndicadores(combinacaoId: Long) =
        _prazoIndicadorDias.flatMapLatest { prazo -> repo.observarIndicadores(combinacaoId, prazo) }

    fun observarUsosDoDia(data: LocalDate) = repo.observarUsosDoDia(data)

    suspend fun contarCombinacoesDoTerno(ternoCodigo: String) = repo.contarCombinacoesDoTerno(ternoCodigo)

    // ---- Pesquisa e filtros (seção 11) ----
    suspend fun pesquisarPecas(termo: String, tipo: TipoPeca?, comFoto: Boolean?) =
        repo.pesquisarPecas(termo, tipo, comFoto)

    // ---- Importação / exportação ZIP ----
    fun validarImportacaoZip(arquivo: java.io.File) = repo.validarImportacaoZip(arquivo)
    fun aplicarImportacaoZip(dadosBrutos: Any, pastaFotosDestino: java.io.File, aoConcluir: () -> Unit) {
        viewModelScope.launch {
            repo.aplicarImportacaoZip(dadosBrutos, pastaFotosDestino)
            aoConcluir()
        }
    }
    fun exportarZip(destinoTemporario: java.io.File, aoConcluir: () -> Unit) {
        viewModelScope.launch {
            repo.exportarZip(destinoTemporario)
            aoConcluir()
        }
    }
}

class RoupasViewModelFactory(
    private val repo: RoupasRepository,
    private val prefs: PreferencesManager
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = RoupasViewModel(repo, prefs) as T
}
