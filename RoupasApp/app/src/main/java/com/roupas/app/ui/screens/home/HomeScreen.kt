package com.roupas.app.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.roupas.app.data.model.ModoVisualizacao
import com.roupas.app.data.model.Peca
import com.roupas.app.data.model.TipoPeca
import com.roupas.app.ui.components.*
import com.roupas.app.viewmodel.RoupasViewModel
import java.time.LocalDate
import java.time.YearMonth

/** As 4 sessões da tela inicial (ajustes 2 e 3). */
private enum class Secao(val titulo: String) {
    TERNOS("Ternos"), GRAVATAS("Gravatas"), CAMISAS("Camisas"), CONJUNTOS("Conjuntos")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: RoupasViewModel,
    onAbrirCalendarioCompleto: () -> Unit,
    onAdicionarPeca: () -> Unit,
    onImportar: () -> Unit,
    onExportar: () -> Unit,
    onAbrirBusca: () -> Unit,
    onAbrirEditarPecas: () -> Unit,
    onAbrirTerno: (Peca) -> Unit,
    onAbrirGravataAvulsa: (Peca) -> Unit
) {
    var secaoAtual by remember { mutableStateOf(Secao.TERNOS) }
    var mostrarAdicionarPecaDaSecao by remember { mutableStateOf(false) }
    var mostrarCriarConjunto by remember { mutableStateOf(false) }
    var densidadeGrade by remember { mutableStateOf(DensidadeGrade.TRES_COLUNAS) }

    val ternos by viewModel.ternos.collectAsState()
    val gravatas by viewModel.gravatas.collectAsState()
    val camisas by viewModel.camisas.collectAsState()
    val conjuntos by viewModel.conjuntos.collectAsState()
    val usos by viewModel.todosOsUsos.collectAsState()
    val modo by viewModel.modoVisualizacao.collectAsState()

    fun buscarNome(codigo: String): String =
        (ternos + gravatas + camisas).find { it.codigo == codigo }?.nome ?: codigo
    fun buscarFoto(codigo: String): String? =
        (ternos + gravatas + camisas).find { it.codigo == codigo }?.fotoPath

    val diasComUso = remember(usos) {
        usos.mapNotNull { runCatching { LocalDate.parse(it.data) }.getOrNull() }.toSet()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meu Guarda-Roupa") },
                actions = {
                    IconButton(onClick = onAbrirBusca) { Icon(Icons.Filled.Search, "Pesquisar") }
                    IconButton(onClick = onAbrirEditarPecas) { Icon(Icons.Filled.Edit, "Editar peças") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                MiniCalendarioTresMeses(
                    mesReferencia = YearMonth.now(),
                    diasComUso = diasComUso,
                    onExpandir = onAbrirCalendarioCompleto
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    AcaoPrincipal(Icons.Filled.Add, "Adicionar\npeça", Modifier.weight(1f), onAdicionarPeca)
                    AcaoPrincipal(Icons.Filled.FileUpload, "Importar\ncombinações", Modifier.weight(1f), onImportar)
                    AcaoPrincipal(Icons.Filled.FileDownload, "Exportar\ncombinações", Modifier.weight(1f), onExportar)
                }
            }

            item {
                ScrollableTabRow(selectedTabIndex = Secao.entries.indexOf(secaoAtual), edgePadding = 0.dp) {
                    Secao.entries.forEach { secao ->
                        Tab(
                            selected = secaoAtual == secao,
                            onClick = { secaoAtual = secao },
                            text = { Text(secao.titulo) }
                        )
                    }
                }
            }

            if (secaoAtual != Secao.CONJUNTOS) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Visualização", style = MaterialTheme.typography.labelLarge)
                        SeletorModoVisualizacao(modo, viewModel::definirModoVisualizacao)
                    }
                }
                if (modo == ModoVisualizacao.GRADE) {
                    item { SeletorDensidadeGrade(densidadeGrade) { densidadeGrade = it } }
                }
            }

            // Cartão "+Adicionar X" no topo da lista (ajuste 2)
            item {
                when (secaoAtual) {
                    Secao.TERNOS -> CartaoAdicionar("Adicionar terno") { mostrarAdicionarPecaDaSecao = true }
                    Secao.GRAVATAS -> CartaoAdicionar("Adicionar gravata") { mostrarAdicionarPecaDaSecao = true }
                    Secao.CAMISAS -> CartaoAdicionar("Adicionar camisa") { mostrarAdicionarPecaDaSecao = true }
                    Secao.CONJUNTOS -> CartaoAdicionar("Adicionar conjunto") { mostrarCriarConjunto = true }
                }
            }

            when (secaoAtual) {
                Secao.TERNOS -> conteudoListaDePecas(ternos, modo, densidadeGrade, viewModel, onAbrirTerno)
                Secao.GRAVATAS -> conteudoListaDePecas(gravatas, modo, densidadeGrade, viewModel, onAbrirGravataAvulsa)
                Secao.CAMISAS -> conteudoListaDePecas(camisas, modo, densidadeGrade, viewModel, {})
                Secao.CONJUNTOS -> {
                    if (conjuntos.isEmpty()) {
                        item {
                            Text(
                                "Nenhum conjunto criado ainda. Toque em \"Adicionar conjunto\" acima.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(conjuntos, key = { it.combinacao.id }) { item ->
                            val indicadores by viewModel.observarIndicadores(item.combinacao.id).collectAsState(initial = emptyList())
                            CartaoCombinacaoPecas(
                                fotoTerno = buscarFoto(item.combinacao.ternoCodigo),
                                nomeTerno = buscarNome(item.combinacao.ternoCodigo),
                                fotoGravata = buscarFoto(item.combinacao.gravataCodigo),
                                nomeGravata = buscarNome(item.combinacao.gravataCodigo),
                                fotoCamisa = buscarFoto(item.combinacao.camisaCodigo),
                                nomeCamisa = buscarNome(item.combinacao.camisaCodigo),
                                indicadores = indicadores,
                                ultimaUtilizacao = item.ultimaUtilizacao,
                                onClick = { viewModel.registrarUso(item.combinacao.id, LocalDate.now()) }
                            )
                        }
                    }
                }
            }

            if (secaoAtual == Secao.TERNOS && ternos.isEmpty()) {
                item {
                    Text(
                        "Você ainda não cadastrou nenhum terno. Comece adicionando sua primeira peça.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (secaoAtual == Secao.GRAVATAS && gravatas.isEmpty()) {
                item { Text("Você ainda não cadastrou nenhuma gravata.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            if (secaoAtual == Secao.CAMISAS && camisas.isEmpty()) {
                item { Text("Você ainda não cadastrou nenhuma camisa.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }

    if (mostrarAdicionarPecaDaSecao) {
        val tipoDaSecao = when (secaoAtual) {
            Secao.TERNOS -> TipoPeca.TERNO
            Secao.GRAVATAS -> TipoPeca.GRAVATA
            Secao.CAMISAS -> TipoPeca.CAMISA
            Secao.CONJUNTOS -> TipoPeca.TERNO // não usado (conjuntos tem fluxo próprio)
        }
        com.roupas.app.ui.screens.addpiece.AdicionarOuEditarPecaDialog(
            viewModel = viewModel,
            tipoInicial = tipoDaSecao,
            tipoFixo = true,
            ternosDisponiveis = ternos,
            gravatasDisponiveis = gravatas,
            onFechar = { mostrarAdicionarPecaDaSecao = false }
        )
    }

    if (mostrarCriarConjunto) {
        com.roupas.app.ui.screens.addpiece.CriarConjuntoDialog(
            viewModel = viewModel,
            ternos = ternos,
            gravatas = gravatas,
            camisas = camisas,
            onFechar = { mostrarCriarConjunto = false }
        )
    }
}

/** Renderiza a lista de peças (Detalhado/Compacto/Grade) para uma sessão — usado por Ternos/Gravatas/Camisas. */
private fun androidx.compose.foundation.lazy.LazyListScope.conteudoListaDePecas(
    pecas: List<Peca>,
    modo: ModoVisualizacao,
    densidadeGrade: DensidadeGrade,
    viewModel: RoupasViewModel,
    onClick: (Peca) -> Unit
) {
    when (modo) {
        ModoVisualizacao.GRADE -> item { GradeDePecas(pecas, densidadeGrade, onClick) }
        else -> items(pecas, key = { it.id }) { peca ->
            CartaoDePecaComContagem(viewModel, peca, modo, onClick)
        }
    }
}

@Composable
private fun AcaoPrincipal(
    icone: androidx.compose.ui.graphics.vector.ImageVector,
    texto: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    ElevatedCard(onClick = onClick, modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            Icon(icone, contentDescription = texto)
            Spacer(Modifier.height(6.dp))
            Text(texto, style = MaterialTheme.typography.labelSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
private fun SeletorModoVisualizacao(modoAtual: ModoVisualizacao, aoMudar: (ModoVisualizacao) -> Unit) {
    var expandido by remember { mutableStateOf(false) }
    Box {
        AssistChip(onClick = { expandido = true }, label = { Text(rotuloModo(modoAtual)) })
        DropdownMenu(expanded = expandido, onDismissRequest = { expandido = false }) {
            ModoVisualizacao.entries.forEach { modo ->
                DropdownMenuItem(text = { Text(rotuloModo(modo)) }, onClick = { aoMudar(modo); expandido = false })
            }
        }
    }
}

private fun rotuloModo(modo: ModoVisualizacao) = when (modo) {
    ModoVisualizacao.DETALHADO -> "Detalhado"
    ModoVisualizacao.COMPACTO -> "Compacto"
    ModoVisualizacao.GRADE -> "Grade"
    ModoVisualizacao.PECAS -> "Peças"
}

@Composable
private fun CartaoDePecaComContagem(
    viewModel: RoupasViewModel,
    peca: Peca,
    modo: ModoVisualizacao,
    onClick: (Peca) -> Unit
) {
    val gravatasAssociadas by if (peca.tipo == TipoPeca.TERNO)
        viewModel.observarGravatasDoTerno(peca.codigo).collectAsState(initial = emptyList())
    else remember { mutableStateOf(emptyList<String>()) }

    var numeroCombinacoes by remember { mutableStateOf(0) }
    LaunchedEffect(peca.codigo) {
        if (peca.tipo == TipoPeca.TERNO) numeroCombinacoes = viewModel.contarCombinacoesDoTerno(peca.codigo)
    }

    when (modo) {
        ModoVisualizacao.DETALHADO, ModoVisualizacao.PECAS ->
            CartaoDetalhado(peca, gravatasAssociadas, numeroCombinacoes) { onClick(peca) }
        ModoVisualizacao.COMPACTO -> CartaoCompacto(peca, numeroCombinacoes) { onClick(peca) }
        ModoVisualizacao.GRADE -> CartaoGrade(peca) { onClick(peca) } // não deveria ocorrer (grade tem branch própria)
    }
}
