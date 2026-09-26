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
import com.ballooner.ui.comiclist.ComicListRoute
import com.ballooner.ui.settings.SettingsRoute

object Routes {
    const val COMIC_LIST = "comics"
    const val SETTINGS = "settings"
    const val COMIC = "comic/{$COMIC_ID_KEY}"

    fun comic(comicId: Long): String = "comic/$comicId"
}

@Composable
fun BalloonerNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.COMIC_LIST) {
        composable(Routes.COMIC_LIST) {
            ComicListRoute(
                onOpenComic = { comicId -> navController.navigate(Routes.comic(comicId)) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsRoute(onNavigateBack = { navController.popBackStack() })
        }
        composable(
            route = Routes.COMIC,
            arguments = listOf(navArgument(COMIC_ID_KEY) { type = NavType.LongType }),
        ) {
            ComicEditorRoute(onNavigateBack = { navController.popBackStack() })
        }
    }
}
