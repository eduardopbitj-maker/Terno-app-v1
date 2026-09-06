package com.roupas.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.roupas.app.data.model.IndicadorUso
import com.roupas.app.ui.theme.AmareloUso
import com.roupas.app.ui.theme.VerdeUso
import com.roupas.app.ui.theme.VermelhoUso

/**
 * Renderiza os 3 quadrados de indicação de uso (seção 8.2):
 * transparente (sem uso ativo) | vermelho -> amarelo -> verde conforme se aproxima de 30 dias.
 * Nunca bloqueia a interação — é somente visual (seção 8.3), então não recebe onClick aqui.
 */
@Composable
fun IndicadoresUso(indicadores: List<IndicadorUso>, tamanho: androidx.compose.ui.unit.Dp = 14.dp) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        indicadores.take(3).forEach { indicador ->
            val cor = corParaProgresso(indicador.progresso)
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(tamanho)
                    .clip(RoundedCornerShape(3.dp))
                    .background(cor)
            )
        }
    }
}

private fun corParaProgresso(progresso: Float?): Color {
    if (progresso == null) return Color.Transparent
    return if (progresso <= 0.5f) {
        // primeira metade da janela de 30 dias: vermelho -> amarelo
        lerp(VermelhoUso, AmareloUso, progresso / 0.5f)
    } else {
        // segunda metade: amarelo -> verde
        lerp(AmareloUso, VerdeUso, (progresso - 0.5f) / 0.5f)
    }
}
