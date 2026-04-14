package com.example.kksales.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.ui.res.stringResource
import com.example.kksales.R
import com.example.kksales.ui.viewmodel.CatalogViewModel
import com.example.kksales.ui.viewmodel.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(
    userViewModel: UserViewModel,
    catalogViewModel: CatalogViewModel,
    navController: NavController,
    onNavigateBack: () -> Unit
) {
    val users by userViewModel.allUsers.collectAsState()
    val products by catalogViewModel.products.collectAsState()
    val loggedInUser by userViewModel.user.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_manage_users)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, null)
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("edit_user_prices") }) {
                        Icon(Icons.Rounded.Sell, contentDescription = stringResource(R.string.desc_reseller_prices))
                    }
                    IconButton(onClick = { navController.navigate("create_user") }) {
                        Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.desc_create_user))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(users.filter { it.name != "Admin" }) { user ->
                UserAdminItem(
                    user = user,
                    loggedInUser = loggedInUser,
                    onToggleAdmin = { userViewModel.toggleAdminStatus(user) },
                    onToggleAdminPlus = { userViewModel.toggleAdminPlusStatus(user) },
                    onToggleReseller = { userViewModel.toggleResellerStatus(user) },
                    onDelete = { userViewModel.deleteUser(user) },
                    onResetBalance = { userViewModel.resetUserBalance(user) },
                    onResetCash = { userViewModel.resetCashBalance(user) },
                    onSetRole = { role -> userViewModel.updateUserRole(user, role) },
                    onSetVehicle = { vehicle -> userViewModel.updateVehicleType(user, vehicle) },
                    onUpdateUser = { updatedUser -> userViewModel.updateUser(updatedUser) },
                    products = products
                )
            }
        }
    }
}
