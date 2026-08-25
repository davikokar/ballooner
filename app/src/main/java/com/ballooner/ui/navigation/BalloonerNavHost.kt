package com.ballooner.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ballooner.ui.project.ProjectRoute
import com.ballooner.ui.projectcreate.ProjectCreateRoute
import com.ballooner.ui.projectlist.ProjectListRoute

object Routes {
    const val PROJECT_LIST = "projects"
    const val PROJECT_CREATE = "projects/create"
    const val PROJECT_ARG = "projectId"
    const val PROJECT = "project/{$PROJECT_ARG}"

    fun project(projectId: Long): String = "project/$projectId"
}

@Composable
fun BalloonerNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.PROJECT_LIST) {
        composable(Routes.PROJECT_LIST) {
            ProjectListRoute(
                onCreateProject = { navController.navigate(Routes.PROJECT_CREATE) },
                onOpenProject = { projectId -> navController.navigate(Routes.project(projectId)) },
            )
        }
        composable(Routes.PROJECT_CREATE) {
            ProjectCreateRoute(
                onProjectSaved = { navController.popBackStack() },
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.PROJECT,
            arguments = listOf(navArgument(Routes.PROJECT_ARG) { type = NavType.LongType }),
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong(Routes.PROJECT_ARG) ?: 0L
            ProjectRoute(
                projectId = projectId,
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}
