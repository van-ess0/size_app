package com.sizesapp.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sizesapp.ui.common.AppViewModelProvider
import com.sizesapp.ui.home.HomeScreen
import com.sizesapp.ui.home.HomeViewModel
import com.sizesapp.ui.itemedit.ItemEditScreen
import com.sizesapp.ui.itemedit.ItemEditViewModel
import com.sizesapp.ui.recommend.RecommendScreen
import com.sizesapp.ui.recommend.RecommendViewModel
import com.sizesapp.ui.scan.ScanScreen
import com.sizesapp.ui.scan.ScanViewModel
import com.sizesapp.ui.settings.SettingsScreen
import com.sizesapp.ui.settings.SettingsViewModel

private data class BottomTab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val bottomTabs = listOf(
    BottomTab(Destinations.HOME, "Closet", Icons.Filled.Checkroom),
    BottomTab(Destinations.RECOMMEND, "Sizes", Icons.Filled.Search),
    BottomTab(Destinations.SETTINGS, "Settings", Icons.Filled.Settings),
)

@Composable
fun SizesNavGraph() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.hierarchy?.firstOrNull()?.route
    val showBottomBar = bottomTabs.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomTabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Destinations.HOME,
            modifier = Modifier.padding(padding),
        ) {
            composable(Destinations.HOME) {
                val viewModel = viewModel<HomeViewModel>(factory = AppViewModelProvider.Factory)
                HomeScreen(
                    viewModel = viewModel,
                    onScanClick = { navController.navigate(Destinations.SCAN) },
                    onItemClick = { itemId -> navController.navigate(Destinations.editItemRoute(itemId)) },
                )
            }

            composable(Destinations.SCAN) {
                val viewModel = viewModel<ScanViewModel>(factory = AppViewModelProvider.Factory)
                ScanScreen(
                    viewModel = viewModel,
                    onScanned = { photoPath, parsed ->
                        navController.navigate(
                            Destinations.newItemRoute(
                                photoPath = photoPath,
                                brand = parsed.guessedBrand,
                                sizeLabel = parsed.guessedSizeLabel,
                                sizeSystem = parsed.guessedSizeSystem?.name,
                                rawText = parsed.rawText,
                            ),
                        ) { popUpTo(Destinations.HOME) }
                    },
                    onClose = { navController.popBackStack() },
                )
            }

            composable(
                route = Destinations.ITEM_EDIT_PATTERN,
                arguments = listOf(
                    navArgument(Destinations.ARG_ITEM_ID) { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument(Destinations.ARG_PHOTO_PATH) { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument(Destinations.ARG_BRAND) { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument(Destinations.ARG_SIZE_LABEL) { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument(Destinations.ARG_SIZE_SYSTEM) { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument(Destinations.ARG_RAW_TEXT) { type = NavType.StringType; nullable = true; defaultValue = null },
                ),
            ) {
                val viewModel = viewModel<ItemEditViewModel>(factory = AppViewModelProvider.Factory)
                ItemEditScreen(viewModel = viewModel, onDone = { navController.popBackStack() })
            }

            composable(Destinations.RECOMMEND) {
                val viewModel = viewModel<RecommendViewModel>(factory = AppViewModelProvider.Factory)
                RecommendScreen(viewModel = viewModel)
            }

            composable(Destinations.SETTINGS) {
                val viewModel = viewModel<SettingsViewModel>(factory = AppViewModelProvider.Factory)
                SettingsScreen(viewModel = viewModel)
            }
        }
    }
}
