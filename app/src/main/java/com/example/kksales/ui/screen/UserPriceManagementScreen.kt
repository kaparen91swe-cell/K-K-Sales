package com.example.kksales.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kksales.data.local.entity.User
import com.example.kksales.ui.viewmodel.CatalogViewModel
import com.example.kksales.ui.viewmodel.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserPriceManagementScreen(
    userViewModel: UserViewModel,
    catalogViewModel: CatalogViewModel,
    onNavigateBack: () -> Unit
) {
    val users by userViewModel.allUsers.collectAsState()
    val products by catalogViewModel.products.collectAsState()
    val resellers = users.filter { it.isReseller }
    
    var selectedUser by remember { mutableStateOf<User?>(null) }
    var expandedUser by remember { mutableStateOf(false) }

    // Initialize with first reseller if none selected
    LaunchedEffect(resellers) {
        if (selectedUser == null && resellers.isNotEmpty()) {
            selectedUser = resellers.first()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Redigera Säljarpriser") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Text("Välj säljare:", style = MaterialTheme.typography.labelSmall)
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { expandedUser = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(selectedUser?.name ?: "Välj säljare")
                }
                DropdownMenu(expanded = expandedUser, onDismissRequest = { expandedUser = false }) {
                    resellers.forEach { user ->
                        DropdownMenuItem(
                            text = { Text(user.name) }, 
                            onClick = { 
                                selectedUser = user
                                expandedUser = false 
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (selectedUser != null) {
                Text("Priser för ${selectedUser!!.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(products) { product ->
                        val currentPrice = remember(selectedUser!!.id, product.id) {
                            mutableStateOf(selectedUser!!.productResellerPrices[product.id]?.toString() ?: "")
                        }
                        
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(product.name, fontWeight = FontWeight.Bold)
                                    Text("Standard: ${product.resellerPrice} kr", style = MaterialTheme.typography.labelSmall)
                                }
                                OutlinedTextField(
                                    value = currentPrice.value,
                                    onValueChange = { 
                                        currentPrice.value = it
                                        val newPrices = selectedUser!!.productResellerPrices.toMutableMap()
                                        val price = it.replace(",", ".").toDoubleOrNull()
                                        if (price != null) newPrices[product.id] = price else newPrices.remove(product.id)
                                        userViewModel.updateUser(selectedUser!!.copy(productResellerPrices = newPrices))
                                    },
                                    modifier = Modifier.width(100.dp),
                                    label = { Text("Pris") },
                                    singleLine = true,
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                                )
                            }
                        }
                    }
                }
                
                Button(
                    onClick = onNavigateBack,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                ) {
                    Text("Klar")
                }
            }
        }
    }
}
