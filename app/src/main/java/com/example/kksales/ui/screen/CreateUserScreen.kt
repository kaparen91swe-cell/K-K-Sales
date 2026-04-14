package com.example.kksales.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kksales.ui.viewmodel.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateUserScreen(viewModel: UserViewModel, onNavigateBack: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isAdmin by remember { mutableStateOf(false) }
    var isAdminPlus by remember { mutableStateOf(false) }
    var isReseller by remember { mutableStateOf(false) }
    var isLageransvarig by remember { mutableStateOf(false) }
    var isTransportor by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Skapa ny användare") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Namn") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Rounded.Person, null) },
                    isError = errorMessage != null,
                    supportingText = { errorMessage?.let { Text(it) } }
                )
            }
            item {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Lösenord (valfritt)") },
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Rounded.Lock, null) }
                )
            }
            item {
                Text("Behörigheter & Roller", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        RoleToggleRow("Administratör", isAdmin) { isAdmin = it }
                        RoleToggleRow("Ägare", isAdminPlus) { isAdminPlus = it }
                        RoleToggleRow("Säljare", isReseller) { isReseller = it }
                        RoleToggleRow("Lagerhållare", isLageransvarig) { isLageransvarig = it }
                        RoleToggleRow("Transportör", isTransportor) { isTransportor = it }
                    }
                }
            }
            item {
                Button(
                    onClick = {
                        errorMessage = null
                        viewModel.registerUser(name, if(password.isBlank()) null else password, isAdmin, isAdminPlus, isReseller, isLageransvarig, isTransportor, 
                            onSuccess = { onNavigateBack() }, 
                            onError = { error -> errorMessage = error }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = name.isNotBlank()
                ) {
                    Text("Skapa Användare")
                }
            }
        }
    }
}
