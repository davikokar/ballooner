package com.ballooner.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ballooner.ui.project.ProjectRoute
import com.ballooner.ui.projectlist.ProjectListRoute
import com.ballooner.ui.settings.SettingsRoute

object Routes {
    const val PROJECT_LIST = "projects"
    const val SETTINGS = "settings"
    const val PROJECT_ARG = "projectId"
    const val NEW_ARG = "new"
    const val PROJECT = "project/{$PROJECT_ARG}?$NEW_ARG={$NEW_ARG}"

    fun project(projectId: Long, isNew: Boolean = false): String = "project/$projectId?$NEW_ARG=$isNew"
}

@Composable
fun BalloonerNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.PROJECT_LIST) {
        composable(Routes.PROJECT_LIST) {
            ProjectListRoute(
                onOpenProject = { projectId -> navController.navigate(Routes.project(projectId)) },
                onCreatedProject = { projectId ->
                    navController.navigate(Routes.project(projectId, isNew = true))
                },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsRoute(onNavigateBack = { navController.popBackStack() })
        }
        composable(
            route = Routes.PROJECT,
            arguments = listOf(
                navArgument(Routes.PROJECT_ARG) { type = NavType.LongType },
                navArgument(Routes.NEW_ARG) {
                    type = NavType.BoolType
                    defaultValue = false
                },
            ),
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong(Routes.PROJECT_ARG) ?: 0L
            val isNew = backStackEntry.arguments?.getBoolean(Routes.NEW_ARG) ?: false
            ProjectRoute(
                projectId = projectId,
                autoOpenPicker = isNew,
                onNavigateBack = { navController.popBackStack() },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
    }
}
