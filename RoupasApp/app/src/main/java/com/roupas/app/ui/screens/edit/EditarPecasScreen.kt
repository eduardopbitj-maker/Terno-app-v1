package com.roupas.app.ui.screens.edit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.roupas.app.data.model.Peca
import com.roupas.app.data.model.TipoPeca
import com.roupas.app.ui.screens.addpiece.AdicionarOuEditarPecaDialog
import com.roupas.app.viewmodel.RoupasViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarPecasScreen(viewModel: RoupasViewModel, onVoltar: () -> Unit) {
    var abaSelecionada by remember { mutableStateOf(TipoPeca.TERNO) }
    val ternos by viewModel.ternos.collectAsState()
    val gravatas by viewModel.gravatas.collectAsState()
    val camisas by viewModel.camisas.collectAsState()

    var pecaParaEditar by remember { mutableStateOf<Peca?>(null) }
    var pecaParaExcluir by remember { mutableStateOf<Peca?>(null) }

    val listaAtual = when (abaSelecionada) {
        TipoPeca.TERNO -> ternos
        TipoPeca.GRAVATA -> gravatas
        TipoPeca.CAMISA -> camisas
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editar peças") },
                navigationIcon = { IconButton(onClick = onVoltar) { Icon(Icons.Filled.ArrowBack, "Voltar") } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = TipoPeca.entries.indexOf(abaSelecionada)) {
                TipoPeca.entries.forEach { tipo ->
                    Tab(
                        selected = abaSelecionada == tipo,
                        onClick = { abaSelecionada = tipo },
                        text = { Text(tipo.label) }
                    )
                }
            }

            if (listaAtual.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(24.dp)) {
                    Text("Nenhuma peça cadastrada nesta categoria ainda.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(listaAtual, key = { it.id }) { peca ->
                        ListItem(
                            headlineContent = { Text(peca.nome) },
                            supportingContent = { Text(peca.codigo) },
                            trailingContent = {
                                IconButton(onClick = { pecaParaExcluir = peca }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Excluir")
                                }
                            },
                            modifier = Modifier.clickable { pecaParaEditar = peca }
                        )
                    }
                }
            }
        }
    }

    pecaParaEditar?.let { peca ->
        AdicionarOuEditarPecaDialog(
            viewModel = viewModel,
            pecaExistente = peca,
            tipoFixo = true,
            ternosDisponiveis = ternos,
            gravatasDisponiveis = gravatas,
            onFechar = { pecaParaEditar = null }
        )
    }

    pecaParaExcluir?.let { peca ->
        AlertDialog(
            onDismissRequest = { pecaParaExcluir = null },
            title = { Text("Excluir peça") },
            text = {
                Text(
                    "Tem certeza que deseja excluir \"${peca.nome}\" (${peca.codigo})? " +
                        "Todas as combinações e registros de calendário relacionados a ela também serão removidos."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.excluirPeca(peca) { pecaParaExcluir = null }
                }) { Text("Excluir") }
            },
            dismissButton = { TextButton(onClick = { pecaParaExcluir = null }) { Text("Cancelar") } }
        )
    }
}
