package com.example.kksales.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.kksales.R
import com.example.kksales.ui.viewmodel.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSettingsScreen(viewModel: UserViewModel, onNavigateBack: () -> Unit) {
    val settings by viewModel.settings.collectAsState()
    
    var consumption by remember { mutableStateOf(settings.fuelConsumption.toString()) }
    var bonus by remember { mutableStateOf(settings.vehicleBonusPerUnit.toString()) }
    var fee by remember { mutableStateOf(settings.vehicleFeePerUnit.toString()) }
    var isFetching by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_global_settings)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(stringResource(R.string.title_fuel_mgmt), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            
            Button(
                onClick = { 
                    isFetching = true
                    viewModel.fetchCurrentFuelPrices()
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        isFetching = false
                    }, 1000)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isFetching
            ) {
                if (isFetching) CircularProgressIndicator(modifier = Modifier.size(20.dp)) else Text(stringResource(R.string.action_fetch_prices))
            }

            Text(stringResource(R.string.label_defaults_info), style = MaterialTheme.typography.labelSmall)
            
            OutlinedTextField(
                value = consumption,
                onValueChange = { consumption = it },
                label = { Text(stringResource(R.string.label_consumption_per_mil)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
            )
            
            OutlinedTextField(
                value = bonus,
                onValueChange = { bonus = it },
                label = { Text(stringResource(R.string.label_own_car_bonus)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
            )
            
            OutlinedTextField(
                value = fee,
                onValueChange = { fee = it },
                label = { Text(stringResource(R.string.label_borrowed_car_fee)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
            )

            Button(
                onClick = {
                    viewModel.updateSettings(settings.copy(
                        fuelConsumption = consumption.toDoubleOrNull() ?: settings.fuelConsumption,
                        vehicleBonusPerUnit = bonus.toDoubleOrNull() ?: settings.vehicleBonusPerUnit,
                        vehicleFeePerUnit = fee.toDoubleOrNull() ?: settings.vehicleFeePerUnit
                    ))
                    onNavigateBack()
                },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Text(stringResource(R.string.action_save_settings))
            }
        }
    }
}
