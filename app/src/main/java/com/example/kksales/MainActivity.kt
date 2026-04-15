package com.example.kksales

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.kksales.ui.screen.*
import com.example.kksales.ui.theme.KKSalesTheme
import com.example.kksales.ui.viewmodel.*

class MainActivity : ComponentActivity() {
    private lateinit var catalogViewModel: CatalogViewModel

    override fun attachBaseContext(newBase: Context) {
        val app = newBase.applicationContext as? KKSalesApplication
        val lang = app?.userPreferencesManager?.languageBlocking ?: "sv"
        val locale = java.util.Locale(lang)
        java.util.Locale.setDefault(locale)
        val config = newBase.resources.configuration
        config.setLocale(locale)
        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val app = LocalContext.current.applicationContext as KKSalesApplication
            
            val userViewModel: UserViewModel = viewModel(
                factory = UserViewModel.Factory(
                    app.userRepository,
                    app.transactionRepository,
                    app.userInventoryRepository,
                    app.settingsRepository,
                    app.userPreferencesManager,
                    app.taskRepository
                )
            )

            catalogViewModel = viewModel(
                factory = CatalogViewModel.Factory(
                    app.productRepository,
                    app.orderRepository,
                    app.cartRepository,
                    app.userRepository,
                    app.userPreferencesManager
                )
            )

            val adminViewModel: AdminViewModel = viewModel(
                factory = AdminViewModel.Factory(app.productRepository, app.transactionRepository)
            )

            val bookkeepingViewModel: BookkeepingViewModel = viewModel(
                factory = BookkeepingViewModel.Factory(
                    app.transactionRepository, 
                    app.userRepository,
                    app.productRepository,
                    app.settingsRepository
                )
            )

            val updateViewModel: UpdateViewModel = viewModel(
                factory = UpdateViewModel.Factory(app.updateRepository)
            )

            LaunchedEffect(Unit) {
                bookkeepingViewModel.syncData()
                updateViewModel.checkForUpdates()
            }

            val user by userViewModel.user.collectAsState()
            val language by userViewModel.language.collectAsState()

            val chatViewModel: ChatViewModel = viewModel(
                factory = ChatViewModel.Factory(app.chatRepository, app.userRepository, user?.id ?: 0)
            )

            val context = LocalContext.current
            var lastLanguage by remember { mutableStateOf(language) }

            LaunchedEffect(language) {
                if (language != lastLanguage) {
                    lastLanguage = language
                    val locale = java.util.Locale(language)
                    java.util.Locale.setDefault(locale)
                    
                    val resources = context.resources
                    val configuration = resources.configuration
                    configuration.setLocale(locale)
                    
                    resources.displayMetrics?.let { metrics ->
                        @Suppress("DEPRECATION")
                        resources.updateConfiguration(configuration, metrics)
                    }
                    (context as? android.app.Activity)?.recreate()
                }
            }

            KKSalesTheme {
                if (user == null) {
                    LoginScreen(userViewModel, updateViewModel)
                } else {
                    MainScreen(userViewModel, catalogViewModel, adminViewModel, bookkeepingViewModel, chatViewModel)
                }
            }
        }
    }

    override fun onDestroy() {
        if (::catalogViewModel.isInitialized) {
            catalogViewModel.clearCartAndRestoreStock()
        }
        super.onDestroy()
    }
}

sealed class Screen(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Catalog : Screen("catalog", "Katalog", Icons.Rounded.ShoppingBag)
    object Profile : Screen("profile", "Min sida", Icons.Rounded.Person)
    object Warehouse : Screen("warehouse", "Lager", Icons.Rounded.Warehouse)
    object Bookkeeping : Screen("bookkeeping", "Statistik", Icons.Rounded.Assessment)
    object Chat : Screen("chat", "Chatt", Icons.AutoMirrored.Rounded.Chat)
    object Cart : Screen("cart", "Korg", Icons.Rounded.ShoppingCart)
    object Restock : Screen("restock", "Hämta", Icons.Rounded.Inventory)
}

@Composable
fun MainScreen(
    userViewModel: UserViewModel,
    catalogViewModel: CatalogViewModel,
    adminViewModel: AdminViewModel,
    bookkeepingViewModel: BookkeepingViewModel,
    chatViewModel: ChatViewModel
) {
    val navController = rememberNavController()
    val user by userViewModel.user.collectAsState()
    val isAdmin = user?.isAdmin ?: false
    val isReseller = user?.isReseller ?: false

    val items = mutableListOf(Screen.Bookkeeping, Screen.Catalog, Screen.Chat, Screen.Profile)
    if (isAdmin) {
        items.add(Screen.Warehouse)
    } else if (isReseller) {
        items.add(Screen.Restock)
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
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
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Bookkeeping.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Catalog.route) {
                CatalogScreen(
                    viewModel = catalogViewModel,
                    userViewModel = userViewModel,
                    onNavigateToCart = { navController.navigate(Screen.Cart.route) }
                )
            }
            composable(Screen.Profile.route) { ProfileScreen(userViewModel, catalogViewModel, adminViewModel, navController) }
            composable("history") { 
                HistoryScreen(userViewModel, catalogViewModel) { navController.popBackStack() } 
            }
            composable("manage_users") { 
                UserManagementScreen(userViewModel, catalogViewModel, navController) { navController.popBackStack() } 
            }
            composable("global_settings") { 
                GlobalSettingsScreen(userViewModel) { navController.popBackStack() } 
            }
            composable("inventory_management") { 
                InventoryManagementScreen(adminViewModel) { navController.popBackStack() } 
            }
            composable("create_user") { CreateUserScreen(userViewModel) { navController.popBackStack() } }
            composable("edit_user_prices") { UserPriceManagementScreen(userViewModel, catalogViewModel) { navController.popBackStack() } }
            composable(Screen.Warehouse.route) { WarehouseScreen(userViewModel, adminViewModel, navController) }
            composable(Screen.Bookkeeping.route) { BookkeepingScreen(bookkeepingViewModel, userViewModel) }
            composable(Screen.Chat.route) { ChatScreen(chatViewModel) }
            composable(Screen.Restock.route) { RestockScreen(bookkeepingViewModel, userViewModel, onNavigateBack = { navController.popBackStack() }) }
            composable(Screen.Cart.route) { 
                CartScreen(
                    viewModel = catalogViewModel,
                    onNavigateBack = { navController.popBackStack() }
                ) 
            }
        }
    }
}
