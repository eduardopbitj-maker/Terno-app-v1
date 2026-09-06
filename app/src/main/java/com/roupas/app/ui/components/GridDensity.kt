package com.roupas.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.roupas.app.data.model.Peca

/** Os 4 níveis de densidade da Grade (seção 5.3): 2, 3 ou 4 colunas, e uma variante compacta de 4. */
enum class DensidadeGrade(val colunas: Int, val compacta: Boolean) {
    DUAS_COLUNAS(2, false),
    TRES_COLUNAS(3, false),
    QUATRO_COLUNAS(4, false),
    QUATRO_COLUNAS_COMPACTA(4, true)
}

/** Linha de 4 pontos para escolher a densidade da grade. */
@Composable
fun SeletorDensidadeGrade(densidadeAtual: DensidadeGrade, aoMudar: (DensidadeGrade) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        DensidadeGrade.entries.forEach { densidade ->
            val selecionado = densidade == densidadeAtual
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .padding(6.dp)
                    .size(if (selecionado) 12.dp else 8.dp)
                    .clip(CircleShape)
                    .background(
                        if (selecionado) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    .clickable { aoMudar(densidade) }
            )
        }
    }
}

/**
 * Grade não-lazy (em linhas manuais) das peças, respeitando a densidade escolhida — usada
 * dentro de uma LazyColumn externa (que já contém outros cabeçalhos), por isso não usa
 * LazyVerticalGrid (evitaria conflito de scroll aninhado).
 */
@Composable
fun GradeDePecas(pecas: List<Peca>, densidade: DensidadeGrade, onClick: (Peca) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        pecas.chunked(densidade.colunas).forEach { linha ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                linha.forEach { peca ->
                    androidx.compose.foundation.layout.Box(modifier = Modifier.weight(1f)) {
                        if (densidade.compacta) {
                            CartaoGradeCompacto(peca) { onClick(peca) }
                        } else {
                            CartaoGrade(peca) { onClick(peca) }
                        }
                    }
                }
                // completa a última linha para manter o alinhamento quando incompleta
                repeat(densidade.colunas - linha.size) {
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
