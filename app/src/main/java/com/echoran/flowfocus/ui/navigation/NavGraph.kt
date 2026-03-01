package com.echoran.flowfocus.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.echoran.flowfocus.ui.screens.TaskListScreen
import com.echoran.flowfocus.ui.screens.TimerScreen
import com.echoran.flowfocus.ui.screens.WhitelistScreen

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Tasks : Screen("tasks", "待办", Icons.Filled.List)
    object Timer : Screen("timer", "专注", Icons.Filled.PlayArrow)
    object Stats : Screen("stats", "统计", Icons.Filled.Info)
    object Whitelist : Screen("whitelist", "白名单", Icons.Filled.Warning)
    object Settings : Screen("settings", "设置", Icons.Filled.Settings)
}

@Composable
fun FlowFocusNavGraph() {
    val navController = rememberNavController()

    val items = listOf(
        Screen.Tasks,
        Screen.Stats,
        Screen.Settings
    )

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            
            // Hide bottom bar on Timer screen and Whitelist screen
            val showBottomBar = currentRoute != null && 
                !currentRoute.startsWith(Screen.Timer.route) && 
                currentRoute != Screen.Whitelist.route

            if (showBottomBar) {
                NavigationBar {
                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Tasks.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Tasks.route) {
                TaskListScreen(
                    onNavigateToTimer = { taskId ->
                        navController.navigate(Screen.Timer.route + "/$taskId") {
                            popUpTo(Screen.Tasks.route) { inclusive = false }
                        }
                    }
                )
            }
            composable(Screen.Timer.route + "/{taskId}") { backStackEntry ->
                val taskId = backStackEntry.arguments?.getString("taskId")?.toLongOrNull()
                TimerScreen(
                    taskId = taskId,
                    onExit = { navController.popBackStack() }
                )
            }
            composable(Screen.Timer.route) {
                TimerScreen(
                    taskId = null,
                    onExit = { navController.popBackStack() }
                )
            }
            composable(Screen.Whitelist.route) {
                WhitelistScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Stats.route) {
                com.echoran.flowfocus.ui.screens.StatisticsScreen()
            }
            composable(Screen.Settings.route) {
                com.echoran.flowfocus.ui.screens.SettingsScreen(
                    onNavigateToWhitelist = {
                        navController.navigate(Screen.Whitelist.route)
                    }
                )
            }
        }
    }
}
