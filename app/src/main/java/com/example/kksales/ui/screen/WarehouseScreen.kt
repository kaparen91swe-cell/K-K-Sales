package com.example.kksales.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Warehouse
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.kksales.data.local.entity.User
import com.example.kksales.ui.viewmodel.AdminViewModel
import com.example.kksales.ui.viewmodel.UserViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarehouseScreen(
    viewModel: UserViewModel,
    adminViewModel: AdminViewModel,
    navController: NavController
) {
    val allUsers by viewModel.allUsers.collectAsState()
    val warehouseKeepers = allUsers.filter { it.isLageransvarig }
    val allProducts by adminViewModel.products.collectAsState()
    
    var selectedUser by remember { mutableStateOf<User?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Lager") })
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Sektion för produktskapande/hantering
            Box(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                AdminInventorySection(adminViewModel)
            }
            
            HorizontalDivider()
            
            // Sektion för lagerhållare
            Column(modifier = Modifier.weight(1f).padding(16.dp)) {
                Text("Lagerhållare", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                
                if (warehouseKeepers.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.Warehouse, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                            Text("Inga lagerhållare", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(warehouseKeepers) { user ->
                            WarehouseKeeperItem(user, onClick = { selectedUser = user })
                        }
                    }
                }
            }
        }

        selectedUser?.let { user ->
            UserSettingsDialog(
                user = user,
                products = allProducts,
                onDismiss = { selectedUser = null },
                onSave = { updatedUser ->
                    viewModel.updateUser(updatedUser)
                    selectedUser = null
                }
            )
        }
    }
}

@Composable
fun WarehouseKeeperItem(user: User, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Icon(Icons.Rounded.Person, null, modifier = Modifier.padding(8.dp))
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(user.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(user.role ?: "Lagerhållare", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
            
            Column(horizontalAlignment = Alignment.End) {
                if (user.storageCost != null) {
                    Text("${String.format(Locale.getDefault(), "%.2f", user.storageCost)} kr", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(if(user.storagePaymentInterval == "Weekly") "per vecka" else "per månad", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
