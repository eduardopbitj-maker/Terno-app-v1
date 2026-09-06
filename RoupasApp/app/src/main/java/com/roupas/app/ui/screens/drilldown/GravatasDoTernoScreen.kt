package com.roupas.app.ui.screens.drilldown

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.roupas.app.data.model.ModoVisualizacao
import com.roupas.app.data.model.Peca
import com.roupas.app.ui.components.CartaoCompacto
import com.roupas.app.ui.components.CartaoDetalhado
import com.roupas.app.ui.components.CartaoGrade
import com.roupas.app.viewmodel.RoupasViewModel

/**
 * Ao clicar em um terno na tela inicial: mostra as gravatas associadas a ele (seção 7).
 * O modo de visualização é o mesmo escolhido globalmente (ajuste "Persistência do modo de
 * visualização") — Detalhado/Compacto/Grade se aplicam aqui normalmente.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GravatasDoTernoScreen(
    viewModel: RoupasViewModel,
    terno: Peca,
    onVoltar: () -> Unit,
    onAbrirGravata: (Peca) -> Unit
) {
    val todasGravatas by viewModel.gravatas.collectAsState()
    val codigosAssociados by viewModel.observarGravatasDoTerno(terno.codigo).collectAsState(initial = emptyList())
    val gravatasDoTerno = remember(todasGravatas, codigosAssociados) {
        todasGravatas.filter { it.codigo in codigosAssociados }
    }
    val modo by viewModel.modoVisualizacao.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gravatas de ${terno.nome}") },
                navigationIcon = { IconButton(onClick = onVoltar) { Icon(Icons.Filled.ArrowBack, "Voltar") } }
            )
        }
    ) { padding ->
        if (gravatasDoTerno.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
                Text("Nenhuma gravata associada a este terno ainda.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(gravatasDoTerno, key = { it.id }) { gravata ->
                    when (modo) {
                        ModoVisualizacao.COMPACTO -> CartaoCompacto(gravata, 0) { onAbrirGravata(gravata) }
                        ModoVisualizacao.GRADE -> CartaoGrade(gravata) { onAbrirGravata(gravata) }
                        else -> CartaoDetalhado(gravata, emptyList(), 0) { onAbrirGravata(gravata) }
                    }
                }
            }
        }
    }
}
