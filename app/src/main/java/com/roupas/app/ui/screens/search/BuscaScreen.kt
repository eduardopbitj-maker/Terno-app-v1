package com.roupas.app.ui.screens.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.roupas.app.data.model.Peca
import com.roupas.app.data.model.TipoPeca
import com.roupas.app.viewmodel.RoupasViewModel

private enum class FiltroExtra {
    NENHUM, COM_FOTO, SEM_FOTO, COMBINACOES_NAO_USADAS_30D, COMBINACOES_MENOS_USADAS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuscaScreen(viewModel: RoupasViewModel, onVoltar: () -> Unit) {
    var termo by remember { mutableStateOf("") }
    var tipoFiltro by remember { mutableStateOf<TipoPeca?>(null) }
    var filtroExtra by remember { mutableStateOf(FiltroExtra.NENHUM) }
    var resultadosPecas by remember { mutableStateOf<List<Peca>>(emptyList()) }

    val conjuntos by viewModel.conjuntos.collectAsState()
    val ternos by viewModel.ternos.collectAsState()
    val gravatas by viewModel.gravatas.collectAsState()
    val camisas by viewModel.camisas.collectAsState()

    fun buscarNome(codigo: String): String =
        (ternos + gravatas + camisas).find { it.codigo == codigo }?.nome ?: codigo

    LaunchedEffect(termo, tipoFiltro, filtroExtra) {
        if (filtroExtra == FiltroExtra.COM_FOTO || filtroExtra == FiltroExtra.SEM_FOTO) {
            resultadosPecas = viewModel.pesquisarPecas(termo, tipoFiltro, filtroExtra == FiltroExtra.COM_FOTO)
        } else {
            resultadosPecas = viewModel.pesquisarPecas(termo, tipoFiltro, null)
        }
    }

    val conjuntosFiltrados = remember(conjuntos, filtroExtra) {
        when (filtroExtra) {
            FiltroExtra.COMBINACOES_NAO_USADAS_30D ->
                conjuntos.filter { it.diasDesdeUltimoUso == null || it.diasDesdeUltimoUso > 30 }
            FiltroExtra.COMBINACOES_MENOS_USADAS ->
                conjuntos.sortedBy { it.totalUsos }.take(10)
            else -> emptyList()
        }
    }

    val mostrandoConjuntos = filtroExtra == FiltroExtra.COMBINACOES_NAO_USADAS_30D ||
        filtroExtra == FiltroExtra.COMBINACOES_MENOS_USADAS

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pesquisar") },
                navigationIcon = { IconButton(onClick = onVoltar) { Icon(Icons.Filled.ArrowBack, "Voltar") } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = termo,
                onValueChange = { termo = it },
                label = { Text("Nome ou código") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))

            Text("Filtrar por tipo", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = tipoFiltro == null, onClick = { tipoFiltro = null }, label = { Text("Todos") })
                TipoPeca.entries.forEach { tipo ->
                    FilterChip(
                        selected = tipoFiltro == tipo,
                        onClick = { tipoFiltro = tipo },
                        label = { Text(tipo.label) }
                    )
                }
            }
            Spacer(Modifier.height(10.dp))

            Text("Outros filtros", style = MaterialTheme.typography.labelLarge)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilterChip(selected = filtroExtra == FiltroExtra.COM_FOTO, onClick = {
                    filtroExtra = if (filtroExtra == FiltroExtra.COM_FOTO) FiltroExtra.NENHUM else FiltroExtra.COM_FOTO
                }, label = { Text("Com foto") })
                FilterChip(selected = filtroExtra == FiltroExtra.SEM_FOTO, onClick = {
                    filtroExtra = if (filtroExtra == FiltroExtra.SEM_FOTO) FiltroExtra.NENHUM else FiltroExtra.SEM_FOTO
                }, label = { Text("Sem foto") })
            }
            Spacer(Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilterChip(selected = filtroExtra == FiltroExtra.COMBINACOES_NAO_USADAS_30D, onClick = {
                    filtroExtra = if (filtroExtra == FiltroExtra.COMBINACOES_NAO_USADAS_30D) FiltroExtra.NENHUM else FiltroExtra.COMBINACOES_NAO_USADAS_30D
                }, label = { Text("Combinações paradas") })
                FilterChip(selected = filtroExtra == FiltroExtra.COMBINACOES_MENOS_USADAS, onClick = {
                    filtroExtra = if (filtroExtra == FiltroExtra.COMBINACOES_MENOS_USADAS) FiltroExtra.NENHUM else FiltroExtra.COMBINACOES_MENOS_USADAS
                }, label = { Text("Menos usadas") })
            }

            Spacer(Modifier.height(16.dp))
            Divider()
            Spacer(Modifier.height(8.dp))

            if (mostrandoConjuntos) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(conjuntosFiltrados, key = { it.combinacao.id }) { item ->
                        ListItem(
                            headlineContent = {
                                Text(
                                    "${buscarNome(item.combinacao.ternoCodigo)} + " +
                                        "${buscarNome(item.combinacao.gravataCodigo)} + " +
                                        buscarNome(item.combinacao.camisaCodigo)
                                )
                            },
                            supportingContent = {
                                Text(
                                    "Usada ${item.totalUsos}x" +
                                        (item.ultimaUtilizacao?.let { " · última em $it" } ?: " · nunca utilizada")
                                )
                            }
                        )
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(resultadosPecas, key = { it.id }) { peca ->
                        ListItem(
                            headlineContent = { Text(peca.nome) },
                            supportingContent = { Text("${peca.codigo} · ${peca.tipo.label}") }
                        )
                    }
                }
            }
        }
    }
}
