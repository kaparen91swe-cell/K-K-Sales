package com.example.kksales.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AddShoppingCart
import androidx.compose.material.icons.rounded.Inventory
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.kksales.R
import com.example.kksales.data.local.entity.Product
import com.example.kksales.data.local.entity.formatQuantity
import com.example.kksales.ui.viewmodel.BookkeepingViewModel
import com.example.kksales.ui.viewmodel.UserViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestockScreen(
    viewModel: BookkeepingViewModel,
    userViewModel: UserViewModel,
    onNavigateBack: () -> Unit
) {
    val products by viewModel.products.collectAsState()
    val viewingUser by viewModel.currentUser.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Lagerpåfyllning") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Tillbaka")
                    }
                }
            )
        }
    ) { padding ->
        if (products.isEmpty()) {
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(products) { product ->
                    RestockProductItem(product, viewingUser) { quantity ->
                        viewModel.registerRestock(product, quantity)
                    }
                }
            }
        }
    }
}

@Composable
fun RestockProductItem(product: Product, user: com.example.kksales.data.local.entity.User?, onRestock: (Int) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    val resellerPrice = user?.productResellerPrices?.get(product.id) ?: product.resellerPrice

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (product.imageUri != null) {
                    AsyncImage(
                        model = product.imageUri,
                        contentDescription = product.name,
                        modifier = Modifier.size(100.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = product.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Ditt pris: ${String.format(Locale.getDefault(), "%.2f", resellerPrice)} kr/${product.unit}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Inventory, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Huvudlager: ${product.formatQuantity()}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { showDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(Icons.Rounded.AddShoppingCart, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Fyll på lager")
            }
        }
    }

    if (showDialog) {
        RestockDialog(
            product = product,
            resellerPrice = resellerPrice,
            onDismiss = { showDialog = false },
            onConfirm = { quantity ->
                onRestock(quantity)
                showDialog = false
            }
        )
    }
}

@Composable
fun RestockDialog(
    product: Product,
    resellerPrice: Double,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var quantityStr by remember { mutableStateOf("1") }
    var selectedUnit by remember { mutableStateOf(if(product.unit == "g") "Gram" else product.unit) }
    val units = if(product.unit == "g") listOf("Gram", "Hekto", "Kilo") else listOf(product.unit)
    var expanded by remember { mutableStateOf(false) }

    val quantity = quantityStr.replace(",", ".").toDoubleOrNull() ?: 0.0
    val multiplier = when (selectedUnit) {
        "Hekto" -> 100.0
        "Kilo" -> 1000.0
        else -> 1.0
    }
    val totalGrams = (quantity * multiplier).toInt()
    val totalCost = resellerPrice * totalGrams

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Fyll på ${product.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Välj mängd att hämta från huvudlagret:")
                
                if (units.size > 1) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(selectedUnit)
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            units.forEach { unit ->
                                DropdownMenuItem(text = { Text(unit) }, onClick = { selectedUnit = unit; expanded = false })
                            }
                        }
                    }
                }

                TextField(
                    value = quantityStr,
                    onValueChange = { quantityStr = it },
                    label = { Text("Antal $selectedUnit") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Kostnad: ${String.format(Locale.getDefault(), "%.2f", totalCost)} kr", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.Red)
                    Text(text = "Detta dras från ditt personliga saldo.", style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(totalGrams) }, enabled = totalGrams > 0) {
                Text("Bekräfta Inköp")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Avbryt") }
        }
    )
}
