package com.ankitsaini.securevault.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ankitsaini.securevault.ui.screens.*

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object AppList : Screen("app_list")
    object SecurityLog : Screen("security_log")
    object Settings : Screen("settings")
    object AppDetails : Screen("app_details/{packageName}") {
        fun createRoute(packageName: String) = "app_details/$packageName"
    }
}

@Composable
fun SecureVaultNavigation() {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToAppList = { navController.navigate(Screen.AppList.route) },
                onNavigateToSecurityLog = { navController.navigate(Screen.SecurityLog.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }
        
        composable(Screen.AppList.route) {
            AppListScreen(
                onBackClick = { navController.popBackStack() },
                onAppClick = { packageName ->
                    navController.navigate(Screen.AppDetails.createRoute(packageName))
                }
            )
        }
        
        composable(Screen.SecurityLog.route) {
            SecurityLogScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        
        composable(Screen.AppDetails.route) { backStackEntry ->
            val packageName = backStackEntry.arguments?.getString("packageName") ?: ""
            AppDetailsScreen(
                packageName = packageName,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
