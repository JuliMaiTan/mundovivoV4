package com.mundovivo.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mundovivo.llm.ModelCatalog
import com.mundovivo.llm.ModelId
import com.mundovivo.ui.model.LLMTestScreen
import com.mundovivo.ui.model.ModelDownloadScreen
import com.mundovivo.ui.model.ModelSelectionScreen

/**
 * Rotas de navegação da Fase 0.
 */
object Routes {
    const val MODEL_SELECTION = "model_selection"
    const val MODEL_DOWNLOAD = "model_download/{modelId}"
    const val LLM_TEST = "llm_test/{modelId}"

    fun modelDownload(modelId: String) = "model_download/$modelId"
    fun llmTest(modelId: String) = "llm_test/$modelId"
}

/**
 * NavHost principal do app.
 */
@Composable
fun MundoVivoNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = Routes.MODEL_SELECTION
    ) {
        composable(Routes.MODEL_SELECTION) {
            ModelSelectionScreen(
                onModelSelected = { model ->
                    navController.navigate(Routes.modelDownload(model.id.value))
                }
            )
        }

        composable(
            route = Routes.MODEL_DOWNLOAD,
            arguments = listOf(navArgument("modelId") { type = NavType.StringType })
        ) { backStackEntry ->
            val modelId = backStackEntry.arguments?.getString("modelId") ?: return@composable
            val model = ModelCatalog.findById(ModelId(modelId)) ?: return@composable

            ModelDownloadScreen(
                model = model,
                onDownloadComplete = {
                    navController.navigate(Routes.llmTest(modelId)) {
                        popUpTo(Routes.MODEL_SELECTION) { inclusive = false }
                    }
                },
                onCancel = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Routes.LLM_TEST,
            arguments = listOf(navArgument("modelId") { type = NavType.StringType })
        ) { backStackEntry ->
            val modelId = backStackEntry.arguments?.getString("modelId") ?: return@composable
            val model = ModelCatalog.findById(ModelId(modelId)) ?: return@composable

            LLMTestScreen(
                model = model,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
