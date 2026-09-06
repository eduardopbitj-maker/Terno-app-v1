package com.roupas.app.ui.screens.addpiece

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.roupas.app.data.model.Peca
import com.roupas.app.viewmodel.RoupasViewModel

/**
 * "+ Adicionar conjunto": obriga a ordem Terno -> Gravata -> Camisa (ajuste 2). O conjunto só
 * é criado quando os três estiverem selecionados; a criação reutiliza as peças já cadastradas,
 * sem duplicá-las — apenas cria a relação (via viewModel.criarCombinacao).
 */
@Composable
fun CriarConjuntoDialog(
    viewModel: RoupasViewModel,
    ternos: List<Peca>,
    gravatas: List<Peca>,
    camisas: List<Peca>,
    onFechar: () -> Unit
) {
    var ternoSelecionado by remember { mutableStateOf<Peca?>(null) }
    var gravataSelecionada by remember { mutableStateOf<Peca?>(null) }
    var camisaSelecionada by remember { mutableStateOf<Peca?>(null) }

    AlertDialog(
        onDismissRequest = onFechar,
        title = { Text("Adicionar conjunto") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("1. Escolha o terno", style = MaterialTheme.typography.labelLarge)
                if (ternos.isEmpty()) {
                    Text("Nenhum terno cadastrado ainda.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                ternos.forEach { terno ->
                    LinhaEscolha(
                        texto = "${terno.codigo} · ${terno.nome}",
                        selecionado = ternoSelecionado?.id == terno.id,
                        onSelecionar = {
                            ternoSelecionado = terno
                            gravataSelecionada = null
                            camisaSelecionada = null
                        }
                    )
                }

                if (ternoSelecionado != null) {
                    Divider()
                    Text("2. Escolha a gravata", style = MaterialTheme.typography.labelLarge)
                    if (gravatas.isEmpty()) {
                        Text("Nenhuma gravata cadastrada ainda.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    gravatas.forEach { gravata ->
                        LinhaEscolha(
                            texto = "${gravata.codigo} · ${gravata.nome}",
                            selecionado = gravataSelecionada?.id == gravata.id,
                            onSelecionar = {
                                gravataSelecionada = gravata
                                camisaSelecionada = null
                            }
                        )
                    }
                }

                if (ternoSelecionado != null && gravataSelecionada != null) {
                    Divider()
                    Text("3. Escolha a camisa", style = MaterialTheme.typography.labelLarge)
                    if (camisas.isEmpty()) {
                        Text("Nenhuma camisa cadastrada ainda.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    camisas.forEach { camisa ->
                        LinhaEscolha(
                            texto = "${camisa.codigo} · ${camisa.nome}",
                            selecionado = camisaSelecionada?.id == camisa.id,
                            onSelecionar = { camisaSelecionada = camisa }
                        )
                    }
                }
            }
        },
        confirmButton = {
            val podeCriar = ternoSelecionado != null && gravataSelecionada != null && camisaSelecionada != null
            TextButton(
                enabled = podeCriar,
                onClick = {
                    viewModel.criarCombinacao(
                        ternoSelecionado!!.codigo,
                        gravataSelecionada!!.codigo,
                        camisaSelecionada!!.codigo
                    ) { onFechar() }
                }
            ) { Text("Criar conjunto") }
        },
        dismissButton = { TextButton(onClick = onFechar) { Text("Cancelar") } }
    )
}

@Composable
private fun LinhaEscolha(texto: String, selecionado: Boolean, onSelecionar: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable { onSelecionar() }
    ) {
        RadioButton(selected = selecionado, onClick = onSelecionar)
        Text(texto)
    }
}
