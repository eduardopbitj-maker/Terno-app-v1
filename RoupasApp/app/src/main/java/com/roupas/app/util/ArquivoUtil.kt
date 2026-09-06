package com.roupas.app.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

/** Copia o conteúdo de um Uri escolhido pelo usuário (SAF) para um arquivo temporário legível. */
fun copiarUriParaArquivoTemporario(context: Context, uri: Uri): File {
    val destino = File(context.cacheDir, "importacao_temp.zip")
    context.contentResolver.openInputStream(uri)?.use { entrada ->
        destino.outputStream().use { saida -> entrada.copyTo(saida) }
    }
    return destino
}

/**
 * Salva o arquivo ZIP temporário na pasta pública Downloads (seção 17), usando MediaStore a
 * partir do Android 10 (API 29) e escrita direta em versões anteriores.
 */
fun salvarZipEmDownloads(context: Context, nomeArquivo: String, origemTemporaria: File): Boolean {
    val nomeFinal = if (nomeArquivo.endsWith(".zip", ignoreCase = true)) nomeArquivo else "$nomeArquivo.zip"

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val valores = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, nomeFinal)
            put(MediaStore.Downloads.MIME_TYPE, "application/zip")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, valores) ?: return false
        resolver.openOutputStream(uri)?.use { saida ->
            origemTemporaria.inputStream().use { entrada -> entrada.copyTo(saida) }
        }
        valores.clear()
        valores.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(uri, valores, null, null)
        true
    } else {
        @Suppress("DEPRECATION")
        val pastaDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        pastaDownloads.mkdirs()
        val destino = File(pastaDownloads, nomeFinal)
        origemTemporaria.copyTo(destino, overwrite = true)
        true
    }
}
