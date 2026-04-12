package com.example.kksales.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.kksales.R
import com.example.kksales.data.local.entity.CartItem
import com.example.kksales.data.local.entity.Product
import com.example.kksales.data.local.entity.calculatePrice
import com.example.kksales.ui.viewmodel.CatalogViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    viewModel: CatalogViewModel,
    onNavigateBack: () -> Unit
) {
    val cartItems by viewModel.cartItems.collectAsState()
    val products by viewModel.products.collectAsState()
    val totalAmount by viewModel.cartTotal.collectAsState()
    
    var showCheckoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_cart)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = null)
                    }
                }
            )
        },
        bottomBar = {
            if (cartItems.isNotEmpty()) {
                Surface(
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Totalt:",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${stringResource(R.string.currency_symbol)}${String.format("%.2f", totalAmount)}",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showCheckoutDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.Payments, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.action_checkout))
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (cartItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.msg_cart_empty))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(cartItems) { item ->
                    val product = products.find { it.id == item.productId }
                    CartItemRow(item, product) {
                        viewModel.removeFromCart(item)
                    }
                }
            }
        }
    }

    if (showCheckoutDialog) {
        val admins by viewModel.admins.collectAsState()
        CheckoutDialog(
            totalAmount = totalAmount,
            admins = admins,
            onDismiss = { showCheckoutDialog = false },
            onConfirm = { paymentMethod, receiverId ->
                viewModel.checkout(paymentMethod, receiverId)
                showCheckoutDialog = false
            }
        )
    }
}

@Composable
fun CartItemRow(item: CartItem, product: Product?, onRemove: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (product?.imageUri != null) {
                AsyncImage(
                    model = product.imageUri,
                    contentDescription = product.name,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product?.name ?: "Okänd produkt",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                val unitLabel = when(item.unitType) {
                    "hekto" -> "hg"
                    "kilo" -> "kg"
                    else -> "g"
                }
                Text(
                    text = "${item.quantity} $unitLabel",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Text(
                text = "${stringResource(R.string.currency_symbol)}${String.format("%.2f", product?.calculatePrice(item.quantity) ?: 0.0)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            IconButton(onClick = onRemove) {
                Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun CheckoutDialog(
    totalAmount: Double,
    admins: List<com.example.kksales.data.local.entity.User>,
    onDismiss: () -> Unit,
    onConfirm: (String, Int?) -> Unit
) {
    var selectedMethod by remember { mutableStateOf("Kontant") }
    var selectedAdminId by remember { mutableStateOf<Int?>(null) }
    
    val paymentMethods = listOf("Kontant", "Konto")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Kassa - Betalning") },
        text = {
            Column {
                Text(
                    text = "Att betala: ${stringResource(R.string.currency_symbol)}${String.format("%.2f", totalAmount)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Text("Välj betalsätt:", style = MaterialTheme.typography.titleMedium)
                
                paymentMethods.forEach { method ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedMethod == method,
                            onClick = { selectedMethod = method }
                        )
                        Text(
                            text = method,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                if (selectedMethod == "Kontant") {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Vem tar emot pengarna?", style = MaterialTheme.typography.titleSmall)
                    
                    admins.forEach { admin ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedAdminId == admin.id,
                                onClick = { selectedAdminId = admin.id }
                            )
                            Text(
                                text = admin.name,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedMethod, if (selectedMethod == "Kontant") selectedAdminId else null) },
                enabled = selectedMethod != "Kontant" || selectedAdminId != null
            ) {
                Text("Slutför köp")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
