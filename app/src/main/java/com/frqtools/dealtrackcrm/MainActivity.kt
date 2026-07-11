package com.frqtools.dealtrackcrm

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import kotlinx.coroutines.delay
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
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = AppRepository(applicationContext)
        val viewModel: MainViewModel by viewModels { ViewModelFactory(applicationContext, repository) }

        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val context = LocalContext.current
                val coroutineScope = rememberCoroutineScope()
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

                // Determine if bottom navigation should be visible (only for top level tabs)
                val topLevelRoutes = listOf(
                    Routes.HOME,
                    Routes.CLIENTS,
                    Routes.DEALS,
                    Routes.FOLLOW_UPS,
                    Routes.SETTINGS
                )

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

                val isBottomBarVisible = currentRoute in topLevelRoutes

                // Close drawer on back press if open
                if (drawerState.isOpen) {
                    BackHandler {
                        coroutineScope.launch { drawerState.close() }
                    }
                }

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    gesturesEnabled = currentRoute in topLevelRoutes,
                    drawerContent = {
                        val settingsState by viewModel.settings.collectAsStateWithLifecycle()
                        ModalDrawerSheet(
                            drawerContainerColor = MaterialTheme.colorScheme.surface,
                            drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                            modifier = Modifier.width(310.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(PrimaryBlue, PrimaryBlue.copy(alpha = 0.8f))
                                        )
                                    )
                                    .padding(horizontal = 20.dp, vertical = 24.dp)
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(CircleShape)
                                                .border(2.dp, Color.White, CircleShape)
                                                .background(Color.White.copy(alpha = 0.2f))
                                        ) {
                                            Image(
                                                painter = painterResource(id = R.drawable.img_dealtrack_logo),
                                                contentDescription = "App Logo",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text(
                                                text = settingsState.businessName,
                                                color = Color.White,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 18.sp
                                            )
                                            Text(
                                                text = settingsState.ownerName,
                                                color = Color.White.copy(alpha = 0.85f),
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Your Professional Deal Partner",
                                        color = Color.White.copy(alpha = 0.75f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            NavigationDrawerItem(
                                label = { Text("Settings & Profile", fontWeight = FontWeight.Bold) },
                                selected = currentRoute == Routes.SETTINGS,
                                onClick = {
                                    coroutineScope.launch { drawerState.close() }
                                    navController.navigate(Routes.SETTINGS) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(Icons.Default.Settings, contentDescription = "Settings", tint = PrimaryBlue) },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = PrimaryContainer,
                                    selectedTextColor = PrimaryBlue,
                                    selectedIconColor = PrimaryBlue,
                                    unselectedTextColor = OnSurfaceVariantText,
                                    unselectedIconColor = OnSurfaceVariantText
                                )
                            )

                            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp), color = OutlineColor.copy(alpha = 0.3f))

                            Text(
                                text = "App & Community",
                                style = AppTypography.bodySmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp),
                                color = OnSurfaceVariantText,
                                modifier = Modifier.padding(start = 24.dp, top = 8.dp, bottom = 8.dp)
                            )

                            NavigationDrawerItem(
                                label = { Text("Share App", fontWeight = FontWeight.Bold) },
                                selected = false,
                                onClick = {
                                    coroutineScope.launch { drawerState.close() }
                                    val shareText = """
                                        📈 *Streamline your deals with DealTrack CRM!*
                                        
                                        Keep your business running flawlessly with the ultimate offline-first CRM companion.
                                        • 👥 Manage clients in a secure offline vault
                                        • 🤝 Trace negotiations and log interaction history
                                        • 🔔 Set smart alarm reminders for follow-ups
                                        
                                        Download DealTrack CRM today and close more sales!
                                        🔗 Download App: https://play.google.com/store/apps/details?id=com.frqtools.dealtrackcrm
                                    """.trimIndent()
                                    
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, "Share DealTrack CRM via")
                                    context.startActivity(shareIntent)
                                },
                                icon = { Icon(Icons.Default.Share, contentDescription = "Share", tint = PrimaryBlue) },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                colors = NavigationDrawerItemDefaults.colors(unselectedTextColor = OnSurfaceText)
                            )

                            NavigationDrawerItem(
                                label = { Text("Rate Us", fontWeight = FontWeight.Bold) },
                                selected = false,
                                onClick = {
                                    coroutineScope.launch { drawerState.close() }
                                    try {
                                        val rateIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.frqtools.dealtrackcrm")).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET)
                                        }
                                        context.startActivity(rateIntent)
                                    } catch (e: Exception) {
                                        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.frqtools.dealtrackcrm"))
                                        context.startActivity(webIntent)
                                    }
                                },
                                icon = { Icon(Icons.Default.Star, contentDescription = "Rate Us", tint = PrimaryBlue) },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                colors = NavigationDrawerItemDefaults.colors(unselectedTextColor = OnSurfaceText)
                            )

                            NavigationDrawerItem(
                                label = { Text("Check for Updates", fontWeight = FontWeight.Bold) },
                                selected = false,
                                onClick = {
                                    coroutineScope.launch { drawerState.close() }
                                    val updateIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.frqtools.dealtrackcrm"))
                                    context.startActivity(updateIntent)
                                },
                                icon = { Icon(Icons.Default.SystemUpdate, contentDescription = "Check for Update", tint = PrimaryBlue) },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                colors = NavigationDrawerItemDefaults.colors(unselectedTextColor = OnSurfaceText)
                            )

                            NavigationDrawerItem(
                                label = { Text("More Useful Apps", fontWeight = FontWeight.Bold) },
                                selected = false,
                                onClick = {
                                    coroutineScope.launch { drawerState.close() }
                                    val moreIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/dev?id=5671242375404230146"))
                                    context.startActivity(moreIntent)
                                },
                                icon = { Icon(Icons.Default.Apps, contentDescription = "More Apps", tint = PrimaryBlue) },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                colors = NavigationDrawerItemDefaults.colors(unselectedTextColor = OnSurfaceText)
                            )

                            NavigationDrawerItem(
                                label = { Text("WhatsApp Support", fontWeight = FontWeight.Bold) },
                                selected = false,
                                onClick = {
                                    coroutineScope.launch { drawerState.close() }
                                    try {
                                        val supportText = "Hello DealTrack CRM Support team, I need assistance with the app."
                                        val waUrl = "https://wa.me/923252604441?text=${Uri.encode(supportText)}"
                                        val waIntent = Intent(Intent.ACTION_VIEW, Uri.parse(waUrl))
                                        context.startActivity(waIntent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "WhatsApp support number: +923252604441", Toast.LENGTH_LONG).show()
                                    }
                                },
                                icon = { Icon(Icons.Default.Chat, contentDescription = "WhatsApp Support", tint = PrimaryBlue) },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                colors = NavigationDrawerItemDefaults.colors(unselectedTextColor = OnSurfaceText)
                            )
                        }
                    }
                ) {
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
                            startDestination = Routes.SPLASH,
                            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
                        ) {
                            // Splash Screen
                            composable(Routes.SPLASH) {
                                SplashScreen(navController, viewModel)
                            }

                            // Onboarding Screen
                            composable(Routes.ONBOARDING) {
                                OnboardingScreen(navController, viewModel)
                            }

                            // Home Tab
                            composable(Routes.HOME) {
                                HomeScreen(navController, viewModel, onMenuClick = { coroutineScope.launch { drawerState.open() } })
                            }

                            // Clients Tab
                            composable(Routes.CLIENTS) {
                                ClientListScreen(navController, viewModel, onMenuClick = { coroutineScope.launch { drawerState.open() } })
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
                                DealListScreen(navController, viewModel, onMenuClick = { coroutineScope.launch { drawerState.open() } })
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
                                FollowUpListScreen(navController, viewModel, onMenuClick = { coroutineScope.launch { drawerState.open() } })
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
                                SettingsScreen(navController, viewModel, onMenuClick = { coroutineScope.launch { drawerState.open() } })
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
}
