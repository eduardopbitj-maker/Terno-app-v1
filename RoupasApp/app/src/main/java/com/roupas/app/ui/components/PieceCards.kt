package com.roupas.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.roupas.app.data.model.IndicadorUso
import com.roupas.app.data.model.ModoVisualizacao
import com.roupas.app.data.model.Peca

/** Miniatura da foto da peça, com um placeholder elegante quando não há foto. */
@Composable
private fun FotoPeca(fotoPath: String?, tamanho: androidx.compose.ui.unit.Dp, formaRedonda: Boolean = false) {
    val forma = if (formaRedonda) CircleShape else RoundedCornerShape(14.dp)
    if (fotoPath != null) {
        AsyncImage(
            model = fotoPath,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(tamanho).clip(forma)
        )
    } else {
        Box(
            modifier = Modifier
                .size(tamanho)
                .clip(forma)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Checkroom, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CartaoBase(onClick: () -> Unit, conteudo: @Composable RowScope.() -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            content = conteudo
        )
    }
}

/** Modo Detalhado (5.1): foto, nome, código, gravatas associadas e nº de combinações. */
@Composable
fun CartaoDetalhado(peca: Peca, gravatasAssociadas: List<String>, numeroCombinacoes: Int, onClick: () -> Unit) {
    CartaoBase(onClick) {
        FotoPeca(peca.fotoPath, 64.dp)
        Column(Modifier.weight(1f)) {
            Text(peca.nome, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(peca.codigo, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            if (gravatasAssociadas.isNotEmpty()) {
                Text(
                    "Gravatas: " + gravatasAssociadas.joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                "$numeroCombinacoes combinação(ões) possível(is)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/** Modo Compacto (5.2): foto, nome e nº de combinações. */
@Composable
fun CartaoCompacto(peca: Peca, numeroCombinacoes: Int, onClick: () -> Unit) {
    CartaoBase(onClick) {
        FotoPeca(peca.fotoPath, 44.dp)
        Column(Modifier.weight(1f)) {
            Text(peca.nome, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text("$numeroCombinacoes", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
    }
}

/** Modo Grade (5.3): retângulos verticais com foto e nome; nível de densidade (2/3/4 colunas). */
@Composable
fun CartaoGrade(peca: Peca, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FotoPeca(peca.fotoPath, 72.dp)
            Spacer(Modifier.height(6.dp))
            Text(
                peca.nome,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

/** Variante compacta do modo Grade (4º nível de densidade: mesma 4 colunas, itens menores). */
@Composable
fun CartaoGradeCompacto(peca: Peca, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FotoPeca(peca.fotoPath, 44.dp)
            Spacer(Modifier.height(3.dp))
            Text(
                peca.nome,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

/** Modo Peças (5.4): terno + gravata + camisa empilhados, com os indicadores de uso à direita. */
@Composable
fun CartaoCombinacaoPecas(
    fotoTerno: String?, nomeTerno: String,
    fotoGravata: String?, nomeGravata: String,
    fotoCamisa: String?, nomeCamisa: String,
    indicadores: List<IndicadorUso>,
    ultimaUtilizacao: String?,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LinhaPecaEmpilhada(fotoTerno, nomeTerno)
                LinhaPecaEmpilhada(fotoGravata, nomeGravata)
                LinhaPecaEmpilhada(fotoCamisa, nomeCamisa)
                if (ultimaUtilizacao != null) {
                    Text(
                        "Última utilização: $ultimaUtilizacao",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            IndicadoresUso(indicadores)
        }
    }
}

@Composable
private fun LinhaPecaEmpilhada(foto: String?, nome: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FotoPeca(foto, 28.dp)
        Text(nome, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
