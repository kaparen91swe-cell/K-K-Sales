package com.example.kksales.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.kksales.R
import com.example.kksales.ui.viewmodel.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(viewModel: UserViewModel) {
    var isRegistering by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val loginError by viewModel.loginError.collectAsState()
    val needsFirstPassword by viewModel.needsFirstPassword.collectAsState()
    var registerError by remember { mutableStateOf<String?>(null) }

    if (needsFirstPassword != null) {
        FirstPasswordScreen(
            user = needsFirstPassword!!,
            onConfirm = { newPass -> viewModel.setInitialPassword(needsFirstPassword!!, newPass) },
            onCancel = { viewModel.cancelFirstPassword() }
        )
        return
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "K&K Sales",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isRegistering) "Skapa nytt konto" else "Logga in för att fortsätta",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            // Username Field
            OutlinedTextField(
                value = username,
                onValueChange = { 
                    username = it
                    viewModel.clearLoginError()
                    registerError = null
                },
                label = { Text("Användarnamn") },
                leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = loginError != null || registerError != null
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // Password Field
            OutlinedTextField(
                value = password,
                onValueChange = { 
                    password = it
                    viewModel.clearLoginError()
                    registerError = null
                },
                label = { Text("Lösenord") },
                leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = loginError != null || registerError != null
            )

            var rememberMe by remember { mutableStateOf(false) }
            if (!isRegistering) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = rememberMe, onCheckedChange = { rememberMe = it })
                    Text("Spara mina uppgifter", style = MaterialTheme.typography.bodyMedium)
                }
            }

            val errorMsg = loginError ?: registerError
            if (errorMsg != null) {
                Text(
                    text = errorMsg,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 8.dp).align(Alignment.Start)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (!isRegistering) {
                Button(
                    onClick = { viewModel.login(username, password, rememberMe) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = username.isNotBlank() && password.isNotBlank()
                ) {
                    Text("Logga in")
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = { 
                    isRegistering = true 
                    username = ""
                    password = ""
                    viewModel.clearLoginError()
                }) {
                    Text("Inget konto? Skapa ett här")
                }
            } else {
                Button(
                    onClick = { 
                        viewModel.registerUser(
                            name = username, 
                            password = password, 
                            isAdmin = false, // Always normal user for new registrations
                            onSuccess = { 
                                isRegistering = false
                                // Auto-login or just let them login
                                viewModel.login(username, password)
                            },
                            onError = { registerError = it }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = username.isNotBlank() && password.isNotBlank()
                ) {
                    Text("Spara och registrera")
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = { 
                    isRegistering = false 
                    username = ""
                    password = ""
                    registerError = null
                }) {
                    Text("Tillbaka till inloggning")
                }
            }
        }
    }
}

@Composable
fun FirstPasswordScreen(
    user: com.example.kksales.data.local.entity.User,
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit
) {
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Välkommen ${user.name}!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Ditt konto har skapats av en administratör. Välj ett lösenord för att fortsätta.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it; error = null },
                label = { Text("Nytt lösenord") },
                leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it; error = null },
                label = { Text("Bekräfta lösenord") },
                leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            if (error != null) {
                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (newPassword.length < 4) {
                        error = "Lösenordet måste vara minst 4 tecken"
                    } else if (newPassword != confirmPassword) {
                        error = "Lösenorden matchar inte"
                    } else {
                        onConfirm(newPassword)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = newPassword.isNotBlank() && confirmPassword.isNotBlank()
            ) {
                Text("Spara lösenord och logga in")
            }
            
            TextButton(onClick = onCancel) {
                Text("Avbryt")
            }
        }
    }
}
