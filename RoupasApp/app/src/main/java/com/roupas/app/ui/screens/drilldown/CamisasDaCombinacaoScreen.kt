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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Ao clicar em uma gravata (a partir de um terno): mostra as camisas associadas a essa
 * combinação terno+gravata específica (seção 7). Ao clicar em uma camisa, a combinação
 * T+G+C é identificada e o calendário é aberto para escolher o dia de uso.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CamisasDaCombinacaoScreen(
    viewModel: RoupasViewModel,
    ternoCodigo: String,
    ternoNome: String,
    gravata: Peca,
    onVoltar: () -> Unit
) {
    val todasCamisas by viewModel.camisas.collectAsState()
    val codigosAssociados by viewModel.observarCamisasDaCombinacao(ternoCodigo, gravata.codigo)
        .collectAsState(initial = emptyList())
    val camisasDaCombinacao = remember(todasCamisas, codigosAssociados) {
        todasCamisas.filter { it.codigo in codigosAssociados }
    }
    val modo by viewModel.modoVisualizacao.collectAsState()
    var camisaParaAgendar by remember { mutableStateOf<Peca?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Camisas · $ternoNome + ${gravata.nome}") },
                navigationIcon = { IconButton(onClick = onVoltar) { Icon(Icons.Filled.ArrowBack, "Voltar") } }
            )
        }
    ) { padding ->
        if (camisasDaCombinacao.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
                Text("Nenhuma camisa associada a esta combinação ainda.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(camisasDaCombinacao, key = { it.id }) { camisa ->
                    when (modo) {
                        ModoVisualizacao.COMPACTO -> CartaoCompacto(camisa, 0) { camisaParaAgendar = camisa }
                        ModoVisualizacao.GRADE -> CartaoGrade(camisa) { camisaParaAgendar = camisa }
                        else -> CartaoDetalhado(camisa, emptyList(), 0) { camisaParaAgendar = camisa }
                    }
                }
            }
        }
    }

    camisaParaAgendar?.let { camisa ->
        EscolherDiaParaUsoDialog(
            viewModel = viewModel,
            ternoCodigo = ternoCodigo,
            gravataCodigo = gravata.codigo,
            camisaCodigo = camisa.codigo,
            onFechar = { camisaParaAgendar = null }
        )
    }
}

/** Abre um seletor de data (Material3 DatePicker) e registra o uso da combinação no dia escolhido. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EscolherDiaParaUsoDialog(
    viewModel: RoupasViewModel,
    ternoCodigo: String,
    gravataCodigo: String,
    camisaCodigo: String,
    onFechar: () -> Unit
) {
    val estadoData = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())

    DatePickerDialog(
        onDismissRequest = onFechar,
        confirmButton = {
            TextButton(onClick = {
                val millis = estadoData.selectedDateMillis ?: return@TextButton
                val dia = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                viewModel.criarCombinacao(ternoCodigo, gravataCodigo, camisaCodigo) { combinacaoId ->
                    viewModel.registrarUso(combinacaoId, dia)
                }
                onFechar()
            }) { Text("Registrar uso") }
        },
        dismissButton = { TextButton(onClick = onFechar) { Text("Cancelar") } }
    ) {
        DatePicker(state = estadoData)
    }
}
