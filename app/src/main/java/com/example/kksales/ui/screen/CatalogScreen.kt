package com.example.kksales.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddShoppingCart
import androidx.compose.material.icons.rounded.Inventory
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import coil.compose.AsyncImage
import com.example.kksales.R
import androidx.compose.ui.draw.alpha
import com.example.kksales.data.local.entity.Product
import com.example.kksales.data.local.entity.calculatePrice
import com.example.kksales.data.local.entity.formatQuantity
import com.example.kksales.ui.viewmodel.CatalogViewModel
import com.example.kksales.ui.viewmodel.UserViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    viewModel: CatalogViewModel,
    userViewModel: UserViewModel,
    onNavigateToCart: () -> Unit
) {
    val products by viewModel.products.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.orderResult.collectLatest { result ->
            result.onSuccess {
                snackbarHostState.showSnackbar(context.getString(R.string.msg_order_success))
            }
            result.onFailure { error ->
                snackbarHostState.showSnackbar(context.getString(R.string.msg_error, error.message ?: ""))
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            val currentLanguage by userViewModel.language.collectAsState()

            TopAppBar(
                title = { Text(stringResource(R.string.title_catalog)) },
                actions = {
                    val cartItems by viewModel.cartItems.collectAsState()
                    BadgedBox(
                        badge = {
                            if (cartItems.isNotEmpty()) {
                                Badge {
                                    Text(cartItems.sumOf { it.quantity }.toString())
                                }
                            }
                        },
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        IconButton(onClick = onNavigateToCart) {
                            Icon(Icons.Rounded.ShoppingCart, contentDescription = stringResource(R.string.title_cart))
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (products.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(products) { product ->
                    ProductItem(product) { quantity, unit ->
                        viewModel.addToCart(product.id, quantity, unit)
                    }
                }
            }
        }
    }
}

@Composable
fun ProductItem(product: Product, onOrder: (Int, String) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (product.imageUri != null) {
                    AsyncImage(
                        model = product.imageUri,
                        contentDescription = product.name,
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (product.bulkPrices.isNotEmpty()) {
                        product.bulkPrices.forEach { bulkPrice ->
                            Text(
                                text = stringResource(R.string.label_discount, bulkPrice.quantity, product.unit, stringResource(R.string.currency_symbol), String.format("%.2f", bulkPrice.price)),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Inventory,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Lager: ${product.formatQuantity()}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { showDialog = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = product.quantity > 0
            ) {
                Icon(Icons.Rounded.AddShoppingCart, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.action_add_to_cart))
            }
        }
    }

    if (showDialog) {
        OrderDialog(
            product = product,
            onDismiss = { showDialog = false },
            onConfirm = { quantity, unit ->
                onOrder(quantity, unit)
                showDialog = false
            }
        )
    }
}

@Composable
fun OrderDialog(
    product: Product,
    onDismiss: () -> Unit,
    onConfirm: (Int, String) -> Unit
) {
    var quantityStr by remember { mutableStateOf("1") }
    val unitGram = stringResource(R.string.label_gram)
    val unitHekto = stringResource(R.string.label_hekto)
    val unitKilo = stringResource(R.string.label_kilo)
    
    var selectedUnit by remember { mutableStateOf(unitGram) }
    val units = listOf(unitGram, unitHekto, unitKilo)
    var expanded by remember { mutableStateOf(false) }

    val quantity = quantityStr.toIntOrNull() ?: 0
    
    val basePrice = product.salesPrice
    val displayPrice = when (selectedUnit) {
        unitHekto -> basePrice * 100
        unitKilo -> basePrice * 1000
        else -> basePrice
    }
    
    val multiplier = when (selectedUnit) {
        unitHekto -> 100
        unitKilo -> 1000
        else -> 1
    }
    
    val totalGrams = quantity * multiplier
    val totalPrice = product.calculatePrice(totalGrams)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.action_add_to_cart)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("${stringResource(R.string.label_name)}: ${product.name}")
                
                Text(stringResource(R.string.label_select_quantity), style = MaterialTheme.typography.labelLarge)
                
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(selectedUnit)
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        units.forEach { unit ->
                            DropdownMenuItem(
                                text = { Text(unit) },
                                onClick = {
                                    selectedUnit = unit
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                TextField(
                    value = quantityStr,
                    onValueChange = { quantityStr = it },
                    label = { Text(stringResource(R.string.label_count_unit, selectedUnit)) },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    val bestPriceForUnit = product.calculatePrice(multiplier)
                    if (product.bulkPrices.isEmpty()) {
                        Text(
                            text = stringResource(R.string.label_price_per, selectedUnit, stringResource(R.string.currency_symbol), String.format("%.2f", bestPriceForUnit)),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        val isDiscounted = totalPrice < (displayPrice * quantity)
                        Text(
                            text = if (isDiscounted) stringResource(R.string.msg_bulk_discount) else stringResource(R.string.label_price_level, stringResource(R.string.currency_symbol), String.format("%.2f", bestPriceForUnit), selectedUnit),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isDiscounted) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isDiscounted) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    Text(
                        text = stringResource(R.string.label_total, stringResource(R.string.currency_symbol), String.format("%.2f", totalPrice)),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(totalGrams, selectedUnit.lowercase()) },
                enabled = quantity > 0
            ) {
                Text(stringResource(R.string.action_add_to_cart))
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
