package com.roupas.app.util

import android.content.Context
import com.roupas.app.data.model.ModoVisualizacao

/**
 * Preferências simples e persistentes do app (seções "Prazo dos indicadores" e
 * "Persistência do modo de visualização" dos ajustes mais recentes).
 * Usa SharedPreferences por ser leve e não exigir novas dependências no Gradle.
 */
class PreferencesManager(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("roupas_app_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val CHAVE_MODO_VISUALIZACAO = "modo_visualizacao"
        private const val CHAVE_PRAZO_INDICADOR_DIAS = "prazo_indicador_dias"
        const val PRAZO_PADRAO_DIAS = 30
    }

    fun lerModoVisualizacao(): ModoVisualizacao {
        val salvo = prefs.getString(CHAVE_MODO_VISUALIZACAO, null) ?: return ModoVisualizacao.DETALHADO
        return runCatching { ModoVisualizacao.valueOf(salvo) }.getOrDefault(ModoVisualizacao.DETALHADO)
    }

    fun salvarModoVisualizacao(modo: ModoVisualizacao) {
        prefs.edit().putString(CHAVE_MODO_VISUALIZACAO, modo.name).apply()
    }

    fun lerPrazoIndicadorDias(): Int = prefs.getInt(CHAVE_PRAZO_INDICADOR_DIAS, PRAZO_PADRAO_DIAS)

    fun salvarPrazoIndicadorDias(dias: Int) {
        prefs.edit().putInt(CHAVE_PRAZO_INDICADOR_DIAS, dias).apply()
    }
}
