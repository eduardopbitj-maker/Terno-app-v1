package com.roupas.app.ui.screens.io

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.roupas.app.util.copiarUriParaArquivoTemporario
import com.roupas.app.util.salvarZipEmDownloads
import com.roupas.app.viewmodel.RoupasViewModel
import java.io.File

/**
 * "Importar combinações" (seções 14-16): abre o seletor de arquivos, valida a estrutura do ZIP,
 * mostra o resumo (seção 15) e só grava no banco após confirmação explícita.
 */
@Composable
fun ImportarCombinacoesDialog(viewModel: RoupasViewModel, onFechar: () -> Unit) {
    val contexto = LocalContext.current
    var resumoEDados by remember { mutableStateOf<Pair<com.roupas.app.util.ResumoImportacao, Any>?>(null) }
    var mensagemErro by remember { mutableStateOf<String?>(null) }
    var importando by remember { mutableStateOf(false) }

    val lancadorArquivo = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) { onFechar(); return@rememberLauncherForActivityResult }
        val arquivoTemp = copiarUriParaArquivoTemporario(contexto, uri)
        val (resumo, dados) = viewModel.validarImportacaoZip(arquivoTemp)
        if (resumo.erros.any { it.contains("não é um .zip") || it.contains("Não foi possível ler") }) {
            mensagemErro = resumo.erros.joinToString("\n")
        } else {
            resumoEDados = resumo to dados
        }
    }

    // Abre o seletor assim que o diálogo é composto pela primeira vez.
    LaunchedEffect(Unit) { lancadorArquivo.launch(arrayOf("application/zip", "application/x-zip-compressed")) }

    resumoEDados?.let { (resumo, dados) ->
        AlertDialog(
            onDismissRequest = onFechar,
            title = { Text("Importação encontrada") },
            text = {
                Column {
                    Text("${resumo.ternos} ternos")
                    Text("${resumo.gravatas} gravatas")
                    Text("${resumo.camisas} camisas")
                    Text("${resumo.combinacoes} combinações")
                    Text("${resumo.fotos} fotos")
                    Text("${resumo.registrosCalendario} registros de calendário")
                    if (resumo.erros.isNotEmpty()) {
                        Spacer(modifier = Modifier.fillMaxWidth())
                        Text("Avisos:", color = MaterialTheme.colorScheme.error)
                        resumo.erros.forEach { Text("• $it", color = MaterialTheme.colorScheme.error) }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !importando,
                    onClick = {
                        importando = true
                        val pastaFotos = File(contexto.filesDir, "fotos").apply { mkdirs() }
                        viewModel.aplicarImportacaoZip(dados, pastaFotos) { onFechar() }
                    }
                ) { Text("Importar") }
            },
            dismissButton = { TextButton(onClick = onFechar) { Text("Cancelar") } }
        )
    }

    mensagemErro?.let { erro ->
        AlertDialog(
            onDismissRequest = onFechar,
            title = { Text("Não foi possível importar") },
            text = { Text(erro) },
            confirmButton = { TextButton(onClick = onFechar) { Text("Entendi") } }
        )
    }
}

/**
 * "Exportar combinações" (seções 17-19): pede o nome do arquivo e salva o .zip em Downloads.
 */
@Composable
fun ExportarCombinacoesDialog(viewModel: RoupasViewModel, onFechar: () -> Unit) {
    val contexto = LocalContext.current
    var nomeArquivo by remember { mutableStateOf("guarda-roupa") }
    var exportando by remember { mutableStateOf(false) }
    var mensagemResultado by remember { mutableStateOf<String?>(null) }

    val lancadorPermissao = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedida ->
        if (concedida) {
            realizarExportacao(viewModel, contexto, nomeArquivo) { sucesso ->
                exportando = false
                mensagemResultado = if (sucesso) "Arquivo salvo em Downloads." else "Não foi possível salvar o arquivo."
            }
        } else {
            exportando = false
            mensagemResultado = "Permissão de armazenamento negada."
        }
    }

    if (mensagemResultado != null) {
        AlertDialog(
            onDismissRequest = onFechar,
            title = { Text("Exportação") },
            text = { Text(mensagemResultado!!) },
            confirmButton = { TextButton(onClick = onFechar) { Text("Ok") } }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onFechar,
        title = { Text("Exportar combinações") },
        text = {
            OutlinedTextField(
                value = nomeArquivo,
                onValueChange = { nomeArquivo = it },
                label = { Text("Nome do arquivo") },
                singleLine = true,
                supportingText = { Text("Será salvo como \"$nomeArquivo.zip\" em Downloads") }
            )
        },
        confirmButton = {
            TextButton(
                enabled = nomeArquivo.isNotBlank() && !exportando,
                onClick = {
                    exportando = true
                    val precisaPermissao = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
                    if (precisaPermissao) {
                        lancadorPermissao.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    } else {
                        realizarExportacao(viewModel, contexto, nomeArquivo) { sucesso ->
                            exportando = false
                            mensagemResultado = if (sucesso) "Arquivo salvo em Downloads." else "Não foi possível salvar o arquivo."
                        }
                    }
                }
            ) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onFechar) { Text("Cancelar") } }
    )
}

private fun realizarExportacao(
    viewModel: RoupasViewModel,
    contexto: android.content.Context,
    nomeArquivo: String,
    aoConcluir: (Boolean) -> Unit
) {
    val temporario = File(contexto.cacheDir, "exportacao_temp.zip")
    viewModel.exportarZip(temporario) {
        val sucesso = runCatching { salvarZipEmDownloads(contexto, nomeArquivo, temporario) }.getOrDefault(false)
        aoConcluir(sucesso)
    }
}
