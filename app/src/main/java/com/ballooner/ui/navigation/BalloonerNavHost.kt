package com.ballooner.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ballooner.ui.projectcreate.ProjectCreateRoute
import com.ballooner.ui.projectlist.ProjectListRoute

object Routes {
    const val PROJECT_LIST = "projects"
    const val PROJECT_CREATE = "projects/create"
}

@Composable
fun BalloonerNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.PROJECT_LIST) {
        composable(Routes.PROJECT_LIST) {
            ProjectListRoute(
                onCreateProject = { navController.navigate(Routes.PROJECT_CREATE) },
            )
        }
        composable(Routes.PROJECT_CREATE) {
            ProjectCreateRoute(
                onProjectSaved = { navController.popBackStack() },
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}
