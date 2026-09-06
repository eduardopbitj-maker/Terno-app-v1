package com.roupas.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.roupas.app.data.db.AppDatabase
import com.roupas.app.data.repository.RoupasRepository
import com.roupas.app.ui.navigation.AppNavigation
import com.roupas.app.ui.theme.RoupasAppTheme
import com.roupas.app.util.PreferencesManager
import com.roupas.app.viewmodel.RoupasViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.obter(applicationContext)
        val repositorio = RoupasRepository(database)
        val preferencias = PreferencesManager(applicationContext)

        setContent {
            RoupasAppTheme {
                val viewModel = viewModel(factory = RoupasViewModelFactory(repositorio, preferencias))
                androidx.compose.material3.Surface {
                    AppNavigation(viewModel = viewModel)
                }
            }
        }
    }
}
