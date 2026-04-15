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
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isRegistering) stringResource(R.string.msg_register_info) else stringResource(R.string.msg_login_info),
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
                label = { Text(stringResource(R.string.label_username)) },
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
                label = { Text(stringResource(R.string.label_password)) },
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
                    Text(stringResource(R.string.label_remember_me), style = MaterialTheme.typography.bodyMedium)
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
                    Text(stringResource(R.string.action_login))
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = { 
                    isRegistering = true 
                    username = ""
                    password = ""
                    viewModel.clearLoginError()
                }) {
                    Text(stringResource(R.string.action_no_account))
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
                    Text(stringResource(R.string.action_register_save))
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = { 
                    isRegistering = false 
                    username = ""
                    password = ""
                    registerError = null
                }) {
                    Text(stringResource(R.string.action_back_to_login))
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
    val context = androidx.compose.ui.platform.LocalContext.current

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.msg_welcome_user, user.name),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.msg_first_pass_info),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it; error = null },
                label = { Text(stringResource(R.string.label_new_password)) },
                leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it; error = null },
                label = { Text(stringResource(R.string.label_confirm_password)) },
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
                        error = context.getString(R.string.error_pass_short)
                    } else if (newPassword != confirmPassword) {
                        error = context.getString(R.string.error_pass_mismatch)
                    } else {
                        onConfirm(newPassword)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = newPassword.isNotBlank() && confirmPassword.isNotBlank()
            ) {
                Text(stringResource(R.string.action_save_pass_login))
            }
            
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    }
}
