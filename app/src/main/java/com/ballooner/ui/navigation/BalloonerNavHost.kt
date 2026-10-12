package com.ballooner.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ballooner.ui.comiceditor.COMIC_ID_KEY
import com.ballooner.ui.comiceditor.ComicEditorRoute
import com.ballooner.ui.comiceditor.EDITOR_STEP_KEY
import com.ballooner.ui.comiceditor.EditorStep
import com.ballooner.ui.comiclist.ComicListRoute
import com.ballooner.ui.comicviewer.ComicViewerRoute
import com.ballooner.ui.settings.SettingsRoute

object Routes {
    const val COMIC_LIST = "comics"
    const val SETTINGS = "settings"
    const val COMIC = "comic/{$COMIC_ID_KEY}?$EDITOR_STEP_KEY={$EDITOR_STEP_KEY}"
    const val COMIC_VIEWER = "view/{$COMIC_ID_KEY}"

    fun comic(comicId: Long, step: EditorStep = EditorStep.LAYOUT): String =
        "comic/$comicId?$EDITOR_STEP_KEY=${step.name}"

    fun comicViewer(comicId: Long): String = "view/$comicId"
}

@Composable
fun BalloonerNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.COMIC_LIST) {
        composable(Routes.COMIC_LIST) {
            ComicListRoute(
                onViewComic = { comicId -> navController.navigate(Routes.comicViewer(comicId)) },
                // A comic that is already made is opened where its balloons are added.
                onEditComic = { comicId ->
                    navController.navigate(Routes.comic(comicId, EditorStep.BALLOONS))
                },
                onNewComic = { comicId -> navController.navigate(Routes.comic(comicId)) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsRoute(onNavigateBack = { navController.popBackStack() })
        }
        composable(
            route = Routes.COMIC_VIEWER,
            arguments = listOf(navArgument(COMIC_ID_KEY) { type = NavType.LongType }),
        ) {
            ComicViewerRoute(onNavigateBack = { navController.popBackStack() })
        }
        composable(
            route = Routes.COMIC,
            arguments = listOf(
                navArgument(COMIC_ID_KEY) { type = NavType.LongType },
                navArgument(EDITOR_STEP_KEY) {
                    type = NavType.StringType
                    defaultValue = EditorStep.LAYOUT.name
                },
            ),
        ) {
            ComicEditorRoute(
                onNavigateBack = { navController.popBackStack() },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
    }
}
