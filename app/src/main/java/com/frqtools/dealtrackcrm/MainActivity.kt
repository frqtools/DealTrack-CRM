package com.frqtools.dealtrackcrm

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.frqtools.dealtrackcrm.data.AppDatabase
import com.frqtools.dealtrackcrm.data.AppRepository
import com.frqtools.dealtrackcrm.ui.*
import com.frqtools.dealtrackcrm.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = AppDatabase.getDatabase(applicationContext)
        val repository = AppRepository(db)
        val viewModel: MainViewModel by viewModels { ViewModelFactory(repository) }

        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val context = LocalContext.current

                // Request POST_NOTIFICATIONS on Android 13+ on first launch
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (!isGranted) {
                        Toast.makeText(context, "reminders require notification permission", Toast.LENGTH_SHORT).show()
                    }
                }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val status = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                        if (status != PackageManager.PERMISSION_GRANTED) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                // Determine if bottom navigation should be visible (only for top level tabs)
                val topLevelRoutes = listOf(
                    Routes.HOME,
                    Routes.CLIENTS,
                    Routes.DEALS,
                    Routes.FOLLOW_UPS,
                    Routes.SETTINGS
                )
                val isBottomBarVisible = currentRoute in topLevelRoutes

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (isBottomBarVisible) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 3.dp
                            ) {
                                NavigationBarItem(
                                    selected = currentRoute == Routes.HOME,
                                    onClick = {
                                        navController.navigate(Routes.HOME) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (currentRoute == Routes.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                                            contentDescription = "Home"
                                        )
                                    },
                                    label = { Text("Home", style = AppTypography.labelSmall.copy(fontWeight = FontWeight.ExtraBold)) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = PrimaryBlue,
                                        selectedTextColor = PrimaryBlue,
                                        indicatorColor = PrimaryContainer,
                                        unselectedIconColor = OnSurfaceVariantText,
                                        unselectedTextColor = OnSurfaceVariantText
                                    )
                                )

                                NavigationBarItem(
                                    selected = currentRoute == Routes.CLIENTS,
                                    onClick = {
                                        navController.navigate(Routes.CLIENTS) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (currentRoute == Routes.CLIENTS) Icons.Filled.People else Icons.Outlined.People,
                                            contentDescription = "Clients"
                                        )
                                    },
                                    label = { Text("Clients", style = AppTypography.labelSmall.copy(fontWeight = FontWeight.ExtraBold)) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = PrimaryBlue,
                                        selectedTextColor = PrimaryBlue,
                                        indicatorColor = PrimaryContainer,
                                        unselectedIconColor = OnSurfaceVariantText,
                                        unselectedTextColor = OnSurfaceVariantText
                                    ),
                                    modifier = Modifier.testTag("clients_tab")
                                )

                                NavigationBarItem(
                                    selected = currentRoute == Routes.DEALS,
                                    onClick = {
                                        navController.navigate(Routes.DEALS) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (currentRoute == Routes.DEALS) Icons.Filled.Handshake else Icons.Outlined.Handshake,
                                            contentDescription = "Deals"
                                        )
                                    },
                                    label = { Text("Deals", style = AppTypography.labelSmall.copy(fontWeight = FontWeight.ExtraBold)) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = PrimaryBlue,
                                        selectedTextColor = PrimaryBlue,
                                        indicatorColor = PrimaryContainer,
                                        unselectedIconColor = OnSurfaceVariantText,
                                        unselectedTextColor = OnSurfaceVariantText
                                    )
                                )

                                NavigationBarItem(
                                    selected = currentRoute == Routes.FOLLOW_UPS,
                                    onClick = {
                                        navController.navigate(Routes.FOLLOW_UPS) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (currentRoute == Routes.FOLLOW_UPS) Icons.Filled.Notifications else Icons.Outlined.NotificationsNone,
                                            contentDescription = "Follow-Ups"
                                        )
                                    },
                                    label = { Text("Follow", style = AppTypography.labelSmall.copy(fontWeight = FontWeight.ExtraBold)) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = PrimaryBlue,
                                        selectedTextColor = PrimaryBlue,
                                        indicatorColor = PrimaryContainer,
                                        unselectedIconColor = OnSurfaceVariantText,
                                        unselectedTextColor = OnSurfaceVariantText
                                    )
                                )

                                NavigationBarItem(
                                    selected = currentRoute == Routes.SETTINGS,
                                    onClick = {
                                        navController.navigate(Routes.SETTINGS) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (currentRoute == Routes.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                                            contentDescription = "Settings"
                                        )
                                    },
                                    label = { Text("Settings", style = AppTypography.labelSmall.copy(fontWeight = FontWeight.ExtraBold)) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = PrimaryBlue,
                                        selectedTextColor = PrimaryBlue,
                                        indicatorColor = PrimaryContainer,
                                        unselectedIconColor = OnSurfaceVariantText,
                                        unselectedTextColor = OnSurfaceVariantText
                                    )
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Routes.HOME,
                        modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
                    ) {
                        // Home Tab
                        composable(Routes.HOME) {
                            HomeScreen(navController, viewModel)
                        }

                        // Clients Tab
                        composable(Routes.CLIENTS) {
                            ClientListScreen(navController, viewModel)
                        }

                        // Client Profile Screen
                        composable(
                            route = Routes.CLIENT_PROFILE,
                            arguments = listOf(navArgument("clientId") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val clientId = backStackEntry.arguments?.getInt("clientId") ?: 0
                            ClientProfileScreen(navController, viewModel, clientId)
                        }

                        // Add / Edit Client Screen
                        composable(
                            route = Routes.ADD_EDIT_CLIENT,
                            arguments = listOf(navArgument("clientId") {
                                type = NavType.IntType
                                defaultValue = 0
                            })
                        ) { backStackEntry ->
                            val clientId = backStackEntry.arguments?.getInt("clientId") ?: 0
                            AddEditClientScreen(navController, viewModel, clientId)
                        }

                        // Deals Tab
                        composable(Routes.DEALS) {
                            DealListScreen(navController, viewModel)
                        }

                        // Add / Edit Deal Screen
                        composable(
                            route = Routes.ADD_EDIT_DEAL,
                            arguments = listOf(
                                navArgument("dealId") {
                                    type = NavType.IntType
                                    defaultValue = 0
                                },
                                navArgument("clientId") {
                                    type = NavType.IntType
                                    defaultValue = 0
                                }
                            )
                        ) { backStackEntry ->
                            val dealId = backStackEntry.arguments?.getInt("dealId") ?: 0
                            val clientId = backStackEntry.arguments?.getInt("clientId") ?: 0
                            AddEditDealScreen(navController, viewModel, dealId, clientId)
                        }

                        // Add / Edit Interaction Screen
                        composable(
                            route = Routes.ADD_EDIT_INTERACTION,
                            arguments = listOf(
                                navArgument("interactionId") {
                                    type = NavType.IntType
                                    defaultValue = 0
                                },
                                navArgument("clientId") {
                                    type = NavType.IntType
                                    defaultValue = 0
                                }
                            )
                        ) { backStackEntry ->
                            val interactionId = backStackEntry.arguments?.getInt("interactionId") ?: 0
                            val clientId = backStackEntry.arguments?.getInt("clientId") ?: 0
                            AddEditInteractionScreen(navController, viewModel, interactionId, clientId)
                        }

                        // Follow-Ups Tab
                        composable(Routes.FOLLOW_UPS) {
                            FollowUpListScreen(navController, viewModel)
                        }

                        // Add / Edit Follow-Up Screen
                        composable(
                            route = Routes.ADD_EDIT_FOLLOW_UP,
                            arguments = listOf(
                                navArgument("followUpId") {
                                    type = NavType.IntType
                                    defaultValue = 0
                                },
                                navArgument("clientId") {
                                    type = NavType.IntType
                                    defaultValue = 0
                                }
                            )
                        ) { backStackEntry ->
                            val followUpId = backStackEntry.arguments?.getInt("followUpId") ?: 0
                            val clientId = backStackEntry.arguments?.getInt("clientId") ?: 0
                            AddEditFollowUpScreen(navController, viewModel, followUpId, clientId)
                        }

                        // Settings Tab
                        composable(Routes.SETTINGS) {
                            SettingsScreen(navController, viewModel)
                        }

                        // Search Screen
                        composable(Routes.SEARCH) {
                            SearchScreen(navController, viewModel)
                        }
                    }
                }
            }
        }
    }
}
