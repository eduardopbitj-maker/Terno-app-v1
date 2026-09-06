package com.roupas.app.ui.navigation

import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.roupas.app.data.model.Peca
import com.roupas.app.ui.screens.addpiece.AdicionarOuEditarPecaDialog
import com.roupas.app.ui.screens.calendar.CalendarioCompletoScreen
import com.roupas.app.ui.screens.drilldown.CamisasDaCombinacaoScreen
import com.roupas.app.ui.screens.drilldown.GravatasDoTernoScreen
import com.roupas.app.ui.screens.edit.EditarPecasScreen
import com.roupas.app.ui.screens.home.HomeScreen
import com.roupas.app.ui.screens.io.ExportarCombinacoesDialog
import com.roupas.app.ui.screens.io.ImportarCombinacoesDialog
import com.roupas.app.ui.screens.search.BuscaScreen
import com.roupas.app.viewmodel.RoupasViewModel

private const val ROTA_HOME = "home"
private const val ROTA_CALENDARIO = "calendario"
private const val ROTA_EDITAR_PECAS = "editar_pecas"
private const val ROTA_BUSCA = "busca"
private const val ROTA_GRAVATAS_DO_TERNO = "gravatas/{ternoCodigo}"
private const val ROTA_CAMISAS_DA_COMBINACAO = "camisas/{ternoCodigo}/{gravataCodigo}"

@Composable
fun AppNavigation(viewModel: RoupasViewModel) {
    val navController: NavHostController = rememberNavController()
    var mostrarAdicionarPeca by remember { mutableStateOf(false) }
    var mostrarImportar by remember { mutableStateOf(false) }
    var mostrarExportar by remember { mutableStateOf(false) }

    val ternos by viewModel.ternos.collectAsState()
    val gravatas by viewModel.gravatas.collectAsState()

    NavHost(navController = navController, startDestination = ROTA_HOME) {
        composable(ROTA_HOME) {
            HomeScreen(
                viewModel = viewModel,
                onAbrirCalendarioCompleto = { navController.navigate(ROTA_CALENDARIO) },
                onAdicionarPeca = { mostrarAdicionarPeca = true },
                onImportar = { mostrarImportar = true },
                onExportar = { mostrarExportar = true },
                onAbrirBusca = { navController.navigate(ROTA_BUSCA) },
                onAbrirEditarPecas = { navController.navigate(ROTA_EDITAR_PECAS) },
                onAbrirTerno = { terno -> navController.navigate("gravatas/${terno.codigo}") },
                onAbrirGravataAvulsa = { /* na sessão Gravatas o toque não navega (não há um terno de contexto) */ }
            )
        }

        composable(ROTA_CALENDARIO) {
            CalendarioCompletoScreen(viewModel = viewModel, onVoltar = { navController.popBackStack() })
        }

        composable(ROTA_EDITAR_PECAS) {
            EditarPecasScreen(viewModel = viewModel, onVoltar = { navController.popBackStack() })
        }

        composable(ROTA_BUSCA) {
            BuscaScreen(viewModel = viewModel, onVoltar = { navController.popBackStack() })
        }

        composable(
            route = ROTA_GRAVATAS_DO_TERNO,
            arguments = listOf(navArgument("ternoCodigo") { type = NavType.StringType })
        ) { entrada ->
            val ternoCodigo = entrada.arguments?.getString("ternoCodigo").orEmpty()
            val terno = ternos.find { it.codigo == ternoCodigo }
            if (terno != null) {
                GravatasDoTernoScreen(
                    viewModel = viewModel,
                    terno = terno,
                    onVoltar = { navController.popBackStack() },
                    onAbrirGravata = { gravata -> navController.navigate("camisas/${terno.codigo}/${gravata.codigo}") }
                )
            }
        }

        composable(
            route = ROTA_CAMISAS_DA_COMBINACAO,
            arguments = listOf(
                navArgument("ternoCodigo") { type = NavType.StringType },
                navArgument("gravataCodigo") { type = NavType.StringType }
            )
        ) { entrada ->
            val ternoCodigo = entrada.arguments?.getString("ternoCodigo").orEmpty()
            val gravataCodigo = entrada.arguments?.getString("gravataCodigo").orEmpty()
            val terno = ternos.find { it.codigo == ternoCodigo }
            val gravata = gravatas.find { it.codigo == gravataCodigo }
            if (terno != null && gravata != null) {
                CamisasDaCombinacaoScreen(
                    viewModel = viewModel,
                    ternoCodigo = terno.codigo,
                    ternoNome = terno.nome,
                    gravata = gravata,
                    onVoltar = { navController.popBackStack() }
                )
            }
        }
    }

    if (mostrarAdicionarPeca) {
        AdicionarOuEditarPecaDialog(
            viewModel = viewModel,
            ternosDisponiveis = ternos,
            gravatasDisponiveis = gravatas,
            onFechar = { mostrarAdicionarPeca = false }
        )
    }

    if (mostrarImportar) {
        ImportarCombinacoesDialog(viewModel = viewModel, onFechar = { mostrarImportar = false })
    }

    if (mostrarExportar) {
        ExportarCombinacoesDialog(viewModel = viewModel, onFechar = { mostrarExportar = false })
    }
}
