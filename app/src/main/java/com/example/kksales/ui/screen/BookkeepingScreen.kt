package com.example.kksales.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import com.example.kksales.R
import com.example.kksales.data.local.entity.*
import com.example.kksales.ui.viewmodel.BookkeepingViewModel
import com.example.kksales.ui.viewmodel.UserViewModel
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.foundation.shape.CircleShape
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BookkeepingScreen(viewModel: BookkeepingViewModel, userViewModel: UserViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val loggedInUser by userViewModel.user.collectAsState()
    val viewingUser by viewModel.currentUser.collectAsState()
    val isAdmin = loggedInUser?.isAdmin == true
    val context = LocalContext.current

    // Initialize with logged in user if not set
    LaunchedEffect(loggedInUser) {
        if (viewingUser == null && loggedInUser != null) {
            viewModel.setCurrentUser(loggedInUser)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewingUser?.id == loggedInUser?.id) "Min Statistik" else "Statistik: ${viewingUser?.name ?: ""}") },
                navigationIcon = {
                    if (viewingUser?.id != loggedInUser?.id && isAdmin) {
                        IconButton(onClick = { viewModel.setCurrentUser(loggedInUser) }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Tillbaka")
                        }
                    }
                },
                actions = {
                    if (isAdmin) {
                        IconButton(onClick = { viewModel.exportToExcel(context) }) {
                            Icon(Icons.Rounded.FileDownload, contentDescription = "Excel")
                        }
                        IconButton(onClick = { viewModel.exportToPdf(context) }) {
                            Icon(Icons.Rounded.BarChart, contentDescription = "PDF")
                        }
                    }
                }
            )
        }
    ) { padding ->
        var showSellDialog by remember { mutableStateOf(false) }
        var showManualDialog by remember { mutableStateOf(false) }
        val products by viewModel.products.collectAsState()
        val settings by viewModel.settings.collectAsState()
        var transactionsExpanded by remember { mutableStateOf(true) }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isAdmin && viewingUser?.id == loggedInUser?.id) {
                    item {
                        Text("Välj användare för att se deras bokföring:", style = MaterialTheme.typography.titleSmall)
                        LazyRow(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.users) { user ->
                                UserIconItem(user = user, onClick = { viewModel.setCurrentUser(user) })
                            }
                        }
                    }
                }

                // Ekonomisk Översikt Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Ekonomisk översikt (${viewingUser?.name ?: ""})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            MiniSummaryRow(label = "Intäkter", value = uiState.totalRevenue)
                            MiniSummaryRow(label = "Kostnader", value = uiState.totalCost)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            MiniSummaryRow(
                                label = "Resultat (Vinst)",
                                value = uiState.totalProfit,
                                isBold = true,
                                valueColor = if (uiState.totalProfit >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                // Ny händelse
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Registrera händelse", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { showSellDialog = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Rounded.Sell, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Försäljning")
                            }
                            
                            Button(
                                onClick = { showManualDialog = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(Icons.Rounded.ShoppingCart, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Lagerpåfyllning")
                            }
                        }
                    }
                }

                // Section: Recent Transactions
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { transactionsExpanded = !transactionsExpanded }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Senaste händelser",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = if (transactionsExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = null
                        )
                    }
                }

                if (transactionsExpanded) {
                    items(uiState.transactions.reversed()) { transaction ->
                        val user = uiState.users.find { it.id == transaction.userId }
                        BookkeepingTransactionItem(transaction, user?.name)
                    }
                }
            }
        }

        if (showSellDialog) {
            ManualSaleDialog(
                products = products,
                viewingUser = viewingUser,
                settings = settings,
                onDismiss = { showSellDialog = false },
                onConfirm = { product, qty, received, dist ->
                    viewModel.registerSale(product, qty, received, dist)
                    showSellDialog = false
                }
            )
        }

        if (showManualDialog) {
            ManualRestockDialog(
                products = products,
                viewingUser = viewingUser,
                onDismiss = { showManualDialog = false },
                onConfirm = { product, qty ->
                    viewModel.registerRestock(product, qty)
                    showManualDialog = false
                }
            )
        }
    }
}

@Composable
fun ManualRestockDialog(
    products: List<Product>,
    viewingUser: User?,
    onDismiss: () -> Unit,
    onConfirm: (Product, Int) -> Unit
) {
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var quantityStr by remember { mutableStateOf("1") }
    var selectedUnit by remember { mutableStateOf("g") }
    var expandedProduct by remember { mutableStateOf(false) }
    var expandedUnit by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lagerpåfyllning (Inköp)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Vilken produkt köper du in från huvudlagret?", style = MaterialTheme.typography.labelSmall)
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { expandedProduct = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(selectedProduct?.name ?: "Välj produkt")
                    }
                    DropdownMenu(expanded = expandedProduct, onDismissRequest = { expandedProduct = false }) {
                        products.forEach { product ->
                            DropdownMenuItem(text = { Text(product.name) }, onClick = { selectedProduct = product; expandedProduct = false })
                        }
                    }
                }

                if (selectedProduct != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        TextField(
                            value = quantityStr,
                            onValueChange = { quantityStr = it },
                            label = { Text("Mängd") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        Box {
                            OutlinedButton(onClick = { expandedUnit = true }) {
                                Text(selectedUnit)
                            }
                            DropdownMenu(expanded = expandedUnit, onDismissRequest = { expandedUnit = false }) {
                                listOf("g", "hg", "kg", "st").forEach { u ->
                                    DropdownMenuItem(text = { Text(u) }, onClick = { selectedUnit = u; expandedUnit = false })
                                }
                            }
                        }
                    }
                    
                    val qtyVal = quantityStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                    val grams = when(selectedUnit) {
                        "kg" -> qtyVal * 1000
                        "hg" -> qtyVal * 100
                        else -> qtyVal
                    }.toInt()
                    
                    val resellerPrice = viewingUser?.productResellerPrices?.get(selectedProduct!!.id) ?: selectedProduct!!.resellerPrice
                    val totalCost = resellerPrice * grams
                    
                    Text("Kostnad som dras från ditt saldo: ${String.format(Locale.getDefault(), "%.2f", totalCost)} kr", style = MaterialTheme.typography.bodySmall, color = Color.Red)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    val qtyVal = quantityStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                    val grams = when(selectedUnit) {
                        "kg" -> qtyVal * 1000
                        "hg" -> qtyVal * 100
                        else -> qtyVal
                    }.toInt()
                    onConfirm(selectedProduct!!, grams)
                },
                enabled = selectedProduct != null && quantityStr.isNotEmpty()
            ) {
                Text("Bekräfta Inköp")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Avbryt") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ManualSaleDialog(
    products: List<Product>,
    viewingUser: User?,
    settings: AppSettings,
    onDismiss: () -> Unit,
    onConfirm: (Product, Int, Double, Double) -> Unit
) {
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var amountReceived by remember { mutableStateOf("") }
    var quantityStr by remember { mutableStateOf("1") }
    var distanceKm by remember { mutableStateOf("") }
    var selectedUnit by remember { mutableStateOf("g") }
    var expandedProduct by remember { mutableStateOf(false) }
    var expandedUnit by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrera Försäljning") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Vilken produkt såldes?", style = MaterialTheme.typography.labelSmall)
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { expandedProduct = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(selectedProduct?.name ?: "Välj produkt")
                    }
                    DropdownMenu(expanded = expandedProduct, onDismissRequest = { expandedProduct = false }) {
                        products.forEach { product ->
                            DropdownMenuItem(text = { Text(product.name) }, onClick = { selectedProduct = product; expandedProduct = false })
                        }
                    }
                }

                if (selectedProduct != null) {
                    Text("Snabbval (Prisnivåer):", style = MaterialTheme.typography.labelSmall)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SuggestionChip(
                            onClick = { 
                                quantityStr = "1"
                                selectedUnit = selectedProduct!!.unit
                                amountReceived = selectedProduct!!.salesPrice.toString()
                            },
                            label = { Text("1 ${selectedProduct!!.unit}") }
                        )
                        selectedProduct!!.bulkPrices.forEach { bp ->
                            SuggestionChip(
                                onClick = { 
                                    quantityStr = bp.quantity.toString()
                                    selectedUnit = selectedProduct!!.unit
                                    amountReceived = bp.price.toString()
                                },
                                label = { Text("${bp.quantity}${selectedProduct!!.unit}") }
                            )
                        }
                    }

                    TextField(
                        value = amountReceived,
                        onValueChange = { amountReceived = it },
                        label = { Text("Mottaget belopp (kr)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        TextField(
                            value = quantityStr,
                            onValueChange = { quantityStr = it },
                            label = { Text("Mängd") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        Box {
                            OutlinedButton(onClick = { expandedUnit = true }) {
                                Text(selectedUnit)
                            }
                            DropdownMenu(expanded = expandedUnit, onDismissRequest = { expandedUnit = false }) {
                                listOf("g", "hg", "kg", "st").forEach { u ->
                                    DropdownMenuItem(text = { Text(u) }, onClick = { selectedUnit = u; expandedUnit = false })
                                }
                            }
                        }
                    }

                    TextField(
                        value = distanceKm,
                        onValueChange = { distanceKm = it },
                        label = { Text("Sträcka för leverans (km)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    
                    if (amountReceived.isNotEmpty() && viewingUser != null) {
                        val received = amountReceived.replace(",", ".").toDoubleOrNull() ?: 0.0
                        val qtyVal = quantityStr.replace(",", ".").toDoubleOrNull()?.toInt() ?: 0
                        val grams = when(selectedUnit) {
                            "kg" -> qtyVal * 1000
                            "hg" -> qtyVal * 100
                            else -> qtyVal
                        }
                        
                        val resellerPrice = viewingUser.productResellerPrices[selectedProduct!!.id] ?: selectedProduct!!.resellerPrice
                        val firmPart = resellerPrice * grams
                        
                        val dist = distanceKm.replace(",", ".").toDoubleOrNull() ?: 0.0
                        val fPrice = viewingUser.fuelPrice ?: settings.fuelPrice
                        val fCons = viewingUser.fuelConsumption ?: settings.fuelConsumption
                        val fuelCost = (dist / 10.0) * fCons * fPrice

                        var vehicleAdj = 0.0
                        if (viewingUser.role == "Transportör") {
                            val bonus = viewingUser.vehicleBonusPerUnit ?: settings.vehicleBonusPerUnit
                            val fee = viewingUser.vehicleFeePerUnit ?: settings.vehicleFeePerUnit
                            vehicleAdj = when (viewingUser.vehicleType) {
                                "Egen bil" -> bonus * grams
                                "Lånad bil" -> -fee * grams
                                else -> 0.0
                            }
                        }

                        val commission = received - firmPart + vehicleAdj - fuelCost
                        
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Preliminär fördelning:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Till Firman:")
                                    Text("${String.format(Locale.getDefault(), "%.2f", firmPart)} kr")
                                }
                                if (fuelCost > 0) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Bensinkostnad:")
                                        Text("-${String.format(Locale.getDefault(), "%.2f", fuelCost)} kr", color = Color.Red)
                                    }
                                }
                                if (vehicleAdj != 0.0) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(if (vehicleAdj > 0) "Bilbonus:" else "Bilavgift:")
                                        Text("${String.format(Locale.getDefault(), "%.2f", vehicleAdj)} kr")
                                    }
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Din Provision:")
                                    Text("${String.format(Locale.getDefault(), "%.2f", commission)} kr", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    val prod = selectedProduct!!
                    val qty = if (selectedUnit in listOf("g", "hg", "kg")) {
                        val amount = quantityStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                        when (selectedUnit) {
                            "kg" -> (amount * 1000).toInt()
                            "hg" -> (amount * 100).toInt()
                            else -> amount.toInt()
                        }
                    } else {
                        quantityStr.toIntOrNull() ?: 0
                    }
                    val received = amountReceived.replace(",", ".").toDoubleOrNull() ?: 0.0
                    val dist = distanceKm.replace(",", ".").toDoubleOrNull() ?: 0.0
                    onConfirm(prod, qty, received, dist)
                },
                enabled = selectedProduct != null && amountReceived.isNotEmpty()
            ) {
                Text("Registrera")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Avbryt") }
        }
    )
}

@Composable
fun UserIconItem(user: User, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Surface(
            modifier = Modifier.size(60.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Icon(
                Icons.Rounded.Person,
                contentDescription = user.name,
                modifier = Modifier.padding(12.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Text(user.name, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun BookkeepingTransactionItem(transaction: Transaction, userName: String? = null) {
    val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    var showDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDialog = true },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(transaction.category.displayName, fontWeight = FontWeight.Bold)
                if (transaction.description.isNotEmpty()) {
                    Text(transaction.description, style = MaterialTheme.typography.bodySmall)
                }
                if (userName != null) {
                    Text(
                        "Användare: $userName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Text(
                    sdf.format(Date(transaction.timestamp)),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${if (transaction.type == TransactionType.EXPENSE) "-" else "+"}${stringResource(R.string.currency_symbol)}${String.format(Locale.getDefault(), "%.2f", transaction.amount)}",
                    color = if (transaction.type == TransactionType.EXPENSE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                if (transaction.vatAmount > 0) {
                    Text(
                        "moms: ${String.format(Locale.getDefault(), "%.2f", transaction.vatAmount)} kr",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                if (transaction.receiptImageUri != null) {
                    Icon(
                        Icons.Rounded.Receipt,
                        contentDescription = "Har kvitto",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = { TextButton(onClick = { showDialog = false }) { Text("Stäng") } },
            title = { Text("Detaljer") },
            text = {
                Column {
                    Text("Kategori: ${transaction.category.displayName}")
                    Text("Typ: ${if (transaction.type == TransactionType.EXPENSE) "Utgift" else "Inkomst"}")
                    Text("Beskrivning: ${transaction.description}")
                    Text("Datum: ${sdf.format(Date(transaction.timestamp))}")
                    Text("Moms (${(transaction.vatRate * 100).toInt()}%): ${String.format(Locale.getDefault(), "%.2f", transaction.vatAmount)} kr")
                    
                    if (transaction.receiptImageUri != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Kvitto:", fontWeight = FontWeight.Bold)
                        AsyncImage(
                            model = transaction.receiptImageUri,
                            contentDescription = "Kvitto",
                            modifier = Modifier.fillMaxWidth().height(200.dp)
                        )
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MiniSummaryRow(label: String, value: Double, valueColor: Color = Color.Unspecified, isBold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.labelMedium)
        Text(
            text = "${stringResource(R.string.currency_symbol)}${String.format(Locale.getDefault(), "%.2f", value)}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = valueColor
        )
    }
}

@Composable
fun FinancialSummaryCard(revenue: Double, cost: Double, profit: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SummaryRow(label = stringResource(R.string.label_total_revenue), value = revenue)
            SummaryRow(label = stringResource(R.string.label_total_cost), value = cost)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SummaryRow(
                label = stringResource(R.string.label_total_profit),
                value = profit,
                valueColor = if (profit >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                isBold = true
            )
        }
    }
}

@Composable
fun SummaryRow(label: String, value: Double, valueColor: Color = Color.Unspecified, isBold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = "${stringResource(R.string.currency_symbol)}${String.format(Locale.getDefault(), "%.2f", value)}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = valueColor
        )
    }
}
