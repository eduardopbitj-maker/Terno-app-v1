package com.roupas.app.ui.screens.addpiece

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.roupas.app.data.model.Peca
import com.roupas.app.data.model.TipoPeca
import com.roupas.app.util.criarArquivoFotoTemporario
import com.roupas.app.util.iniciarEdicaoDeImagem
import com.roupas.app.viewmodel.RoupasViewModel
import kotlinx.coroutines.launch
import java.io.File

/**
 * Formulário de peça (seção 4), reaproveitado também para edição (seção 6): tipo, nome, código,
 * foto (com escolha câmera/galeria + tela de recorte/rotação) e associações conforme o tipo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdicionarOuEditarPecaDialog(
    viewModel: RoupasViewModel,
    pecaExistente: Peca? = null,
    tipoInicial: TipoPeca = TipoPeca.TERNO,
    tipoFixo: Boolean = false,
    ternosDisponiveis: List<Peca> = emptyList(),
    gravatasDisponiveis: List<Peca> = emptyList(),
    onFechar: () -> Unit
) {
    val contexto = LocalContext.current
    val escopo = rememberCoroutineScope()

    var tipo by remember { mutableStateOf(pecaExistente?.tipo ?: tipoInicial) }
    var nome by remember { mutableStateOf(pecaExistente?.nome ?: "") }
    var codigo by remember { mutableStateOf(pecaExistente?.codigo ?: "") }
    var fotoPath by remember { mutableStateOf(pecaExistente?.fotoPath) }

    var ternosSelecionados by remember { mutableStateOf(setOf<String>()) }
    var ternoSelecionadoParaCamisa by remember { mutableStateOf<String?>(null) }
    var gravataSelecionadaParaCamisa by remember { mutableStateOf<String?>(null) }

    var mostrarEscolhaFoto by remember { mutableStateOf(false) }
    var uriFotoTemp by remember { mutableStateOf<Uri?>(null) }
    var arquivoFotoTemp by remember { mutableStateOf<File?>(null) }

    // Preenche automaticamente o próximo código disponível ao trocar o tipo (cadastro novo).
    LaunchedEffect(tipo, pecaExistente) {
        if (pecaExistente == null) codigo = viewModel.proximoCodigo(tipo)
    }

    // Lançador do editor de imagem (recorte + rotação) — abre depois de escolher a foto.
    val lancadorEdicao = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { resultado ->
        val caminhoFinal = com.roupas.app.util.extrairCaminhoResultadoEdicao(resultado)
        if (caminhoFinal != null) fotoPath = caminhoFinal
    }

    fun abrirEdicaoComUri(uriOrigem: Uri) {
        val intent = iniciarEdicaoDeImagem(contexto, uriOrigem, codigo.ifBlank { "peca" })
        lancadorEdicao.launch(intent)
    }

    val lancadorCamera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { sucesso ->
        if (sucesso) uriFotoTemp?.let { abrirEdicaoComUri(it) }
    }
    val lancadorGaleria = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) abrirEdicaoComUri(uri)
    }

    AlertDialog(
        onDismissRequest = onFechar,
        title = { Text(if (pecaExistente == null) "Adicionar peça" else "Editar peça") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Campo 1 — Tipo
                if (pecaExistente == null && !tipoFixo) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TipoPeca.entries.forEach { opcao ->
                            FilterChip(
                                selected = tipo == opcao,
                                onClick = { tipo = opcao },
                                label = { Text(opcao.label) }
                            )
                        }
                    }
                }

                // Campo 2 — Nome
                OutlinedTextField(value = nome, onValueChange = { nome = it }, label = { Text("Nome") }, singleLine = true)

                // Campo 3 — Código
                OutlinedTextField(value = codigo, onValueChange = { codigo = it }, label = { Text("Código") }, singleLine = true)

                // Campo 4 — Foto
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { mostrarEscolhaFoto = true },
                        contentAlignment = Alignment.Center
                    ) {
                        if (fotoPath != null) {
                            AsyncImage(model = fotoPath, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        } else {
                            Icon(Icons.Filled.AddAPhoto, contentDescription = "Adicionar foto")
                        }
                    }
                    Column {
                        TextButton(onClick = { mostrarEscolhaFoto = true }) { Text(if (fotoPath == null) "Adicionar foto" else "Trocar foto") }
                        if (fotoPath != null) {
                            TextButton(onClick = { fotoPath = null }) { Text("Remover foto") }
                        }
                    }
                }

                // Campo 5 — Associações, conforme o tipo
                when (tipo) {
                    TipoPeca.TERNO -> { /* não exibe associações (seção 4, campo 5) */ }
                    TipoPeca.GRAVATA -> {
                        Text("Associar a ternos:", style = MaterialTheme.typography.labelLarge)
                        ternosDisponiveis.forEach { terno ->
                            LinhaSelecaoMultipla(
                                texto = "${terno.codigo} · ${terno.nome}",
                                selecionado = ternosSelecionados.contains(terno.codigo),
                                onToggle = {
                                    ternosSelecionados = if (ternosSelecionados.contains(terno.codigo))
                                        ternosSelecionados - terno.codigo else ternosSelecionados + terno.codigo
                                }
                            )
                        }
                    }
                    TipoPeca.CAMISA -> {
                        Text("1. Escolha o terno:", style = MaterialTheme.typography.labelLarge)
                        ternosDisponiveis.forEach { terno ->
                            LinhaSelecaoUnica(
                                texto = "${terno.codigo} · ${terno.nome}",
                                selecionado = ternoSelecionadoParaCamisa == terno.codigo,
                                onSelecionar = { ternoSelecionadoParaCamisa = terno.codigo }
                            )
                        }
                        if (ternoSelecionadoParaCamisa != null) {
                            Text("2. Escolha a gravata:", style = MaterialTheme.typography.labelLarge)
                            gravatasDisponiveis.forEach { gravata ->
                                LinhaSelecaoUnica(
                                    texto = "${gravata.codigo} · ${gravata.nome}",
                                    selecionado = gravataSelecionadaParaCamisa == gravata.codigo,
                                    onSelecionar = { gravataSelecionadaParaCamisa = gravata.codigo }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            val podeSalvar = nome.isNotBlank() && codigo.isNotBlank()
            TextButton(
                enabled = podeSalvar,
                onClick = {
                    val aoConcluir: () -> Unit = {
                        if (tipo == TipoPeca.GRAVATA && ternosSelecionados.isNotEmpty()) {
                            viewModel.associarGravataATernos(codigo, ternosSelecionados.toList())
                        }
                        if (tipo == TipoPeca.CAMISA && ternoSelecionadoParaCamisa != null && gravataSelecionadaParaCamisa != null) {
                            viewModel.criarCombinacao(ternoSelecionadoParaCamisa!!, gravataSelecionadaParaCamisa!!, codigo) {}
                        }
                        onFechar()
                    }
                    if (pecaExistente == null) {
                        viewModel.cadastrarPeca(tipo, nome, codigo, fotoPath, aoConcluir)
                    } else {
                        viewModel.editarPeca(pecaExistente.id, nome, codigo, fotoPath, aoConcluir)
                    }
                }
            ) { Text("Cadastrar") }
        },
        dismissButton = {
            TextButton(onClick = onFechar) { Text("Descartar") }
        }
    )

    if (mostrarEscolhaFoto) {
        AlertDialog(
            onDismissRequest = { mostrarEscolhaFoto = false },
            title = { Text("Adicionar foto") },
            text = { Text("Escolha a origem da imagem.") },
            confirmButton = {
                TextButton(onClick = {
                    mostrarEscolhaFoto = false
                    val arquivo = criarArquivoFotoTemporario(contexto)
                    arquivoFotoTemp = arquivo
                    val uri = FileProvider.getUriForFile(contexto, "com.roupas.app.fileprovider", arquivo)
                    uriFotoTemp = uri
                    lancadorCamera.launch(uri)
                }) { Text("Câmera") }
            },
            dismissButton = {
                TextButton(onClick = {
                    mostrarEscolhaFoto = false
                    lancadorGaleria.launch("image/*")
                }) { Text("Galeria") }
            }
        )
    }
}

@Composable
private fun LinhaSelecaoMultipla(texto: String, selecionado: Boolean, onToggle: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable { onToggle() }
    ) {
        Checkbox(checked = selecionado, onCheckedChange = { onToggle() })
        Text(texto)
    }
}

@Composable
private fun LinhaSelecaoUnica(texto: String, selecionado: Boolean, onSelecionar: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable { onSelecionar() }
    ) {
        RadioButton(selected = selecionado, onClick = onSelecionar)
        Text(texto)
    }
}
