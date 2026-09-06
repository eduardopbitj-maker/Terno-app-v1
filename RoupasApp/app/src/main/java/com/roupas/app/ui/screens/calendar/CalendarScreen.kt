package com.roupas.app.ui.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.roupas.app.data.model.RegistroUso
import com.roupas.app.ui.theme.DouradoDestaque
import com.roupas.app.util.PreferencesManager
import com.roupas.app.viewmodel.RoupasViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarioCompletoScreen(viewModel: RoupasViewModel, onVoltar: () -> Unit) {
    var mesAtual by remember { mutableStateOf(YearMonth.now()) }
    var diaSelecionado by remember { mutableStateOf<LocalDate?>(null) }
    var mostrarSeletorAno by remember { mutableStateOf(false) }
    var mostrarConfigPrazo by remember { mutableStateOf(false) }

    val todosOsUsos by viewModel.todosOsUsos.collectAsState()
    val usosPorDia = remember(todosOsUsos) {
        todosOsUsos.groupBy { runCatching { LocalDate.parse(it.data) }.getOrNull() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendário") },
                navigationIcon = { IconButton(onClick = onVoltar) { Icon(Icons.Filled.ChevronLeft, "Voltar") } },
                actions = {
                    // Ajuste 1: opção fácil de encontrar, junto ao calendário maximizado,
                    // para configurar o prazo (em dias) usado pelos indicadores de utilização.
                    IconButton(onClick = { mostrarConfigPrazo = true }) { Icon(Icons.Filled.Timelapse, "Prazo dos indicadores") }
                    IconButton(onClick = { mesAtual = YearMonth.now() }) { Icon(Icons.Filled.Today, "Hoje") }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            // Navegação de mês/ano
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { mesAtual = mesAtual.minusMonths(1) }) { Icon(Icons.Filled.ChevronLeft, "Mês anterior") }
                Text(
                    text = "${mesAtual.month.getDisplayName(TextStyle.FULL, Locale("pt", "BR")).replaceFirstChar { it.uppercase() }} ${mesAtual.year}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.clickable { mostrarSeletorAno = true }
                )
                IconButton(onClick = { mesAtual = mesAtual.plusMonths(1) }) { Icon(Icons.Filled.ChevronRight, "Próximo mês") }
            }

            Spacer(Modifier.height(12.dp))
            GradeCalendarioCompleto(
                mes = mesAtual,
                usosPorDia = usosPorDia,
                onDiaClicado = { diaSelecionado = it }
            )
        }
    }

    if (mostrarSeletorAno) {
        SeletorDeAnoDialog(
            anoAtual = mesAtual.year,
            onSelecionar = { ano -> mesAtual = mesAtual.withYear(ano); mostrarSeletorAno = false },
            onFechar = { mostrarSeletorAno = false }
        )
    }

    diaSelecionado?.let { dia ->
        DetalheDoDiaDialog(
            viewModel = viewModel,
            dia = dia,
            usos = usosPorDia[dia].orEmpty(),
            onFechar = { diaSelecionado = null }
        )
    }

    if (mostrarConfigPrazo) {
        ConfigurarPrazoIndicadorDialog(viewModel = viewModel, onFechar = { mostrarConfigPrazo = false })
    }
}

/**
 * Diálogo para configurar o prazo (em dias) usado pelos 3 indicadores de utilização
 * (ajuste 1). O histórico de usos nunca é apagado — só a janela de exibição muda.
 */
@Composable
private fun ConfigurarPrazoIndicadorDialog(viewModel: RoupasViewModel, onFechar: () -> Unit) {
    val prazoAtual by viewModel.prazoIndicadorDias.collectAsState()
    var texto by remember { mutableStateOf(prazoAtual.toString()) }

    AlertDialog(
        onDismissRequest = onFechar,
        title = { Text("Prazo dos indicadores") },
        text = {
            Column {
                Text(
                    "Por quantos dias uma utilização continua contando nos 3 quadrados de cada combinação? " +
                        "(padrão: ${PreferencesManager.PRAZO_PADRAO_DIAS} dias)"
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = texto,
                    onValueChange = { texto = it.filter(Char::isDigit) },
                    label = { Text("Dias") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                texto.toIntOrNull()?.let { viewModel.definirPrazoIndicadorDias(it) }
                onFechar()
            }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onFechar) { Text("Cancelar") } }
    )
}

@Composable
private fun GradeCalendarioCompleto(
    mes: YearMonth,
    usosPorDia: Map<LocalDate?, List<RegistroUso>>,
    onDiaClicado: (LocalDate) -> Unit
) {
    val primeiroDiaSemana = mes.atDay(1).dayOfWeek.value % 7
    val totalDias = mes.lengthOfMonth()
    val celulas = List(primeiroDiaSemana) { null } + (1..totalDias).map { mes.atDay(it) }
    val hoje = LocalDate.now()

    Column {
        Row(Modifier.fillMaxWidth()) {
            listOf("D", "S", "T", "Q", "Q", "S", "S").forEach {
                Text(it, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
        LazyVerticalGrid(columns = GridCells.Fixed(7), modifier = Modifier.height(320.dp)) {
            items(celulas.size) { indice ->
                val data = celulas[indice]
                Box(modifier = Modifier.aspectRatio(1f).padding(2.dp), contentAlignment = Alignment.Center) {
                    if (data != null) {
                        val ehHoje = data == hoje
                        val temUso = usosPorDia[data]?.isNotEmpty() == true
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(
                                    when {
                                        ehHoje -> DouradoDestaque
                                        temUso -> DouradoDestaque.copy(alpha = 0.25f)
                                        else -> androidx.compose.ui.graphics.Color.Transparent
                                    }
                                )
                                .clickable { onDiaClicado(data) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${data.dayOfMonth}", color = if (ehHoje) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SeletorDeAnoDialog(anoAtual: Int, onSelecionar: (Int) -> Unit, onFechar: () -> Unit) {
    var texto by remember { mutableStateOf(anoAtual.toString()) }
    AlertDialog(
        onDismissRequest = onFechar,
        title = { Text("Ir para o ano") },
        text = {
            OutlinedTextField(value = texto, onValueChange = { texto = it.filter(Char::isDigit) }, singleLine = true)
        },
        confirmButton = { TextButton(onClick = { texto.toIntOrNull()?.let(onSelecionar) }) { Text("Ir") } },
        dismissButton = { TextButton(onClick = onFechar) { Text("Cancelar") } }
    )
}

/**
 * Mostra as combinações usadas em um dia (podendo ser mais de uma — seção 9.6), permite
 * remover um uso e avisa antes de qualquer substituição (seção 9.5) — a confirmação de
 * substituição é feita pelo próprio fluxo de "remover + adicionar novamente" com aviso abaixo.
 */
@Composable
private fun DetalheDoDiaDialog(viewModel: RoupasViewModel, dia: LocalDate, usos: List<RegistroUso>, onFechar: () -> Unit) {
    var usoParaRemover by remember { mutableStateOf<RegistroUso?>(null) }

    AlertDialog(
        onDismissRequest = onFechar,
        title = { Text(dia.toString()) },
        text = {
            Column {
                if (usos.isEmpty()) {
                    Text("Nenhuma combinação registrada neste dia.")
                } else {
                    usos.forEach { uso ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Combinação #${uso.combinacaoId}")
                            TextButton(onClick = { usoParaRemover = uso }) { Text("Remover") }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onFechar) { Text("Fechar") } }
    )

    usoParaRemover?.let { uso ->
        AlertDialog(
            onDismissRequest = { usoParaRemover = null },
            title = { Text("Remover combinação") },
            text = { Text("Tem certeza que deseja remover esta combinação deste dia?") },
            confirmButton = {
                TextButton(onClick = { viewModel.removerUso(uso.id); usoParaRemover = null }) { Text("Remover") }
            },
            dismissButton = { TextButton(onClick = { usoParaRemover = null }) { Text("Cancelar") } }
        )
    }
}
