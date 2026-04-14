package com.example.kksales.ui.screen

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
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
    val btcPayment by viewModel.btcPayment.collectAsState()
    
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
                                text = stringResource(R.string.label_total_cost),
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

    btcPayment?.let { payment ->
        BitcoinPaymentDialog(
            payment = payment,
            onDismiss = { /* Handle dismiss */ },
            onConfirm = {
                // Here we would normally wait for server confirmation
                // For now, let's just clear the cart and assume it's pending
                viewModel.clearCartAndRestoreStock() 
            }
        )
    }
}

@Composable
fun BitcoinPaymentDialog(
    payment: com.example.kksales.data.remote.api.BtcPaymentResponse,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.label_btc_payment)) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Skanna QR-koden eller kopiera adressen nedan",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // QR Code Placeholder - In a real app we would use ZXing to generate a QR
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .background(Color.White, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    // Simulating a QR code with an icon for now
                    Icon(
                        Icons.Rounded.Payments, 
                        contentDescription = null, 
                        modifier = Modifier.size(100.dp),
                        tint = Color.Black
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    "Belopp: ${payment.amount_btc} BTC",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "(${payment.amount_sek} ${stringResource(R.string.currency_symbol)})",
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = payment.address,
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Jag har skickat betalningen")
            }
        }
    )
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
    val methodCash = stringResource(R.string.label_cash_payment)
    val methodBalance = stringResource(R.string.label_balance_payment)
    val methodBtc = stringResource(R.string.label_btc_payment)
    
    var selectedMethod by remember { mutableStateOf(methodCash) }
    var selectedAdminId by remember { mutableStateOf<Int?>(null) }
    
    val paymentMethods = listOf(methodCash, methodBalance, methodBtc)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.title_checkout)) },
        text = {
            Column {
                Text(
                    text = "${stringResource(R.string.label_total)}: ${stringResource(R.string.currency_symbol)}${String.format("%.2f", totalAmount)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Text(stringResource(R.string.label_payment_method), style = MaterialTheme.typography.titleMedium)
                
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

                if (selectedMethod == methodCash) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(stringResource(R.string.label_assign_to), style = MaterialTheme.typography.titleSmall)
                    
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
                onClick = { onConfirm(selectedMethod, if (selectedMethod == methodCash) selectedAdminId else null) },
                enabled = selectedMethod != methodCash || selectedAdminId != null
            ) {
                Text(stringResource(R.string.action_checkout))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
