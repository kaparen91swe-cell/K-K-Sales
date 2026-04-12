package com.example.kksales

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.ShoppingBag
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
import androidx.compose.runtime.collectAsState
import com.example.kksales.ui.screen.CatalogScreen
import com.example.kksales.ui.screen.ProfileScreen
import com.example.kksales.ui.theme.KKSalesTheme
import com.example.kksales.ui.viewmodel.CatalogViewModel
import com.example.kksales.ui.viewmodel.UserViewModel

import com.example.kksales.ui.screen.LoginScreen
import com.example.kksales.ui.screen.AdminDashboardScreen
import com.example.kksales.ui.screen.BookkeepingScreen
import com.example.kksales.ui.screen.CartScreen
import com.example.kksales.ui.screen.ChatScreen
import com.example.kksales.ui.viewmodel.AdminViewModel
import com.example.kksales.ui.viewmodel.BookkeepingViewModel
import com.example.kksales.ui.viewmodel.ChatViewModel
import com.example.kksales.ui.viewmodel.UpdateViewModel
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.runtime.LaunchedEffect

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

            // Automatisk synk och uppdatering vid start
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
                    val config = context.resources.configuration
                    config.setLocale(locale)
                    context.resources.updateConfiguration(config, context.resources.displayMetrics)
                    (context as? android.app.Activity)?.recreate()
                }
            }

            KKSalesTheme {
                if (user == null) {
                    LoginScreen(userViewModel)
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
    object Catalog : Screen("catalog", "Catalog", Icons.Rounded.ShoppingBag)
    object Profile : Screen("profile", "Profile", Icons.Rounded.Person)
    object Admin : Screen("admin", "Admin", Icons.Rounded.AdminPanelSettings)
    object Bookkeeping : Screen("bookkeeping", "Statistik", Icons.Rounded.Assessment)
    object Chat : Screen("chat", "Chatt", Icons.AutoMirrored.Rounded.Chat)
    object Cart : Screen("cart", "Korg", Icons.Rounded.ShoppingBag) // Hidden from bottom bar
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

    val items = mutableListOf(Screen.Bookkeeping, Screen.Catalog, Screen.Chat, Screen.Profile)
    if (isAdmin) {
        items.add(Screen.Admin)
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
            composable(Screen.Profile.route) { ProfileScreen(userViewModel, catalogViewModel) }
            composable(Screen.Admin.route) { AdminDashboardScreen(adminViewModel) }
            composable(Screen.Bookkeeping.route) { BookkeepingScreen(bookkeepingViewModel, userViewModel) }
            composable(Screen.Chat.route) { ChatScreen(chatViewModel) }
            composable(Screen.Cart.route) { 
                CartScreen(
                    viewModel = catalogViewModel,
                    onNavigateBack = { navController.popBackStack() }
                ) 
            }
        }
    }
}
