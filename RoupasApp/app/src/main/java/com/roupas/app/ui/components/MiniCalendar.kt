package com.roupas.app.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roupas.app.ui.theme.DouradoDestaque
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * Calendário minimizado da tela inicial (seção 2.1): mês anterior à esquerda, mês atual ao
 * centro (sem blur, em destaque) e mês seguinte à direita — ambos os laterais desfocados.
 * Um toque em qualquer ponto expande para o calendário completo.
 */
@Composable
fun MiniCalendarioTresMeses(
    mesReferencia: YearMonth = YearMonth.now(),
    diasComUso: Set<LocalDate> = emptySet(),
    onExpandir: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surfaceVariant)
                )
            )
            .clickable { onExpandir() }
            .padding(vertical = 14.dp, horizontal = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        MesMiniatura(
            mes = mesReferencia.minusMonths(1),
            diasComUso = diasComUso,
            emDestaque = false,
            modifier = Modifier.weight(0.8f)
        )
        MesMiniatura(
            mes = mesReferencia,
            diasComUso = diasComUso,
            emDestaque = true,
            modifier = Modifier.weight(1.3f)
        )
        MesMiniatura(
            mes = mesReferencia.plusMonths(1),
            diasComUso = diasComUso,
            emDestaque = false,
            modifier = Modifier.weight(0.8f)
        )
    }
}

@Composable
private fun MesMiniatura(
    mes: YearMonth,
    diasComUso: Set<LocalDate>,
    emDestaque: Boolean,
    modifier: Modifier = Modifier
) {
    // Blur real (RenderEffect) só existe a partir da API 31; abaixo disso caímos para uma
    // opacidade reduzida, que visualmente também "esmaece" o mês fora de foco.
    val suportaBlurNativo = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val modificadorEfeito = when {
        emDestaque -> Modifier
        suportaBlurNativo -> Modifier.blur(radius = 3.dp)
        else -> Modifier.alpha(0.45f)
    }

    Column(
        modifier = modifier.then(modificadorEfeito),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = mes.month.getDisplayName(TextStyle.SHORT, Locale("pt", "BR")).replaceFirstChar { it.uppercase() },
            fontSize = if (emDestaque) 13.sp else 10.sp,
            fontWeight = if (emDestaque) FontWeight.Bold else FontWeight.Medium,
            color = if (emDestaque) DouradoDestaque else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(2.dp))
        GradeDiasMinuscula(mes = mes, diasComUso = diasComUso, emDestaque = emDestaque)
    }
}

@Composable
private fun GradeDiasMinuscula(mes: YearMonth, diasComUso: Set<LocalDate>, emDestaque: Boolean) {
    val primeiroDiaSemana = mes.atDay(1).dayOfWeek.value % 7 // domingo = 0
    val totalDias = mes.lengthOfMonth()
    val celulas = List(primeiroDiaSemana) { null } + (1..totalDias).map { it }

    // Tamanho da fonte bem pequeno para caber nas 6 linhas x 7 colunas do espaço minimizado.
    val tamanhoFonte = if (emDestaque) 8.sp else 6.sp
    val hoje = LocalDate.now()

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier.height(if (emDestaque) 58.dp else 50.dp),
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
        userScrollEnabled = false
    ) {
        items(celulas.size) { indice ->
            val dia = celulas[indice]
            if (dia == null) {
                Box(Modifier.size(if (emDestaque) 9.dp else 7.dp))
            } else {
                val data = mes.atDay(dia)
                val ehHoje = emDestaque && data == hoje
                val temUso = diasComUso.contains(data)
                Box(
                    modifier = Modifier
                        .size(if (emDestaque) 9.dp else 7.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            when {
                                ehHoje -> DouradoDestaque
                                temUso -> DouradoDestaque.copy(alpha = 0.35f)
                                else -> androidx.compose.ui.graphics.Color.Transparent
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = dia.toString(),
                        fontSize = tamanhoFonte,
                        lineHeight = tamanhoFonte,
                        textAlign = TextAlign.Center,
                        color = if (ehHoje) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
