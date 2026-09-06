package com.roupas.app.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResult
import com.yalantis.ucrop.UCrop
import java.io.File

/**
 * Cria o arquivo temporário (.jpg) que receberá a foto tirada pela câmera, antes da edição.
 */
fun criarArquivoFotoTemporario(context: Context): File {
    val pasta = File(context.filesDir, "temp").apply { mkdirs() }
    return File(pasta, "captura_${System.currentTimeMillis()}.jpg")
}

/**
 * Monta o Intent do editor de imagem (uCrop) permitindo redimensionar e rotacionar (seção 4,
 * campo 4), salvando o resultado em .jpg com o nome vinculado ao CÓDIGO da peça — nunca ao nome —
 * dentro da pasta de fotos do app.
 */
fun iniciarEdicaoDeImagem(context: Context, uriOrigem: Uri, codigoPeca: String): Intent {
    val pastaFotos = File(context.filesDir, "fotos").apply { mkdirs() }
    val arquivoDestino = File(pastaFotos, "$codigoPeca.jpg")
    val uriDestino = Uri.fromFile(arquivoDestino)

    val opcoes = UCrop.Options().apply {
        setCompressionFormat(android.graphics.Bitmap.CompressFormat.JPEG)
        setFreeStyleCropEnabled(true) // permite redimensionar livremente
        setHideBottomControls(false) // mantém os controles de rotação visíveis
    }

    return UCrop.of(uriOrigem, uriDestino)
        .withOptions(opcoes)
        .getIntent(context)
}

/** Extrai o caminho do arquivo final após o usuário concluir o recorte/rotação. */
fun extrairCaminhoResultadoEdicao(resultado: ActivityResult): String? {
    if (resultado.resultCode != Activity.RESULT_OK) return null
    val data = resultado.data ?: return null
    val uriResultado = UCrop.getOutput(data) ?: return null
    return uriResultado.path
}
