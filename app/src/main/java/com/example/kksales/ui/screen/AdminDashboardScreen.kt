package com.example.kksales.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.kksales.R
import com.example.kksales.data.local.entity.BulkPrice
import com.example.kksales.data.local.entity.Product
import com.example.kksales.ui.viewmodel.AdminViewModel
import coil.compose.AsyncImage
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.example.kksales.data.local.entity.formatQuantity

import com.example.kksales.util.FileUtils
import android.net.Uri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(viewModel: AdminViewModel) {
    val products by viewModel.products.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<Product?>(null) }
    var showImageManager by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_inventory)) },
                actions = {
                    IconButton(onClick = { showImageManager = true }) {
                        Icon(Icons.Rounded.FolderOpen, contentDescription = "Hantera alla produktbilder")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.action_add_product))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(products) { product ->
                AdminProductItem(
                    product = product,
                    onEdit = { productToEdit = it },
                    onDelete = { viewModel.deleteProduct(it) }
                )
            }
        }

        if (showImageManager) {
            AdminImageManagerDialog(onDismiss = { showImageManager = false })
        }

        if (showAddDialog) {
            ProductDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { name, cost, price, profit, qty, unit, imageUri, bulkPrices, threshold ->
                    val savedUri = imageUri?.let { uriStr ->
                        FileUtils.saveImageToInternalStorage(context, Uri.parse(uriStr))
                    } ?: imageUri
                    
                    viewModel.addProduct(name, cost, price, profit, qty, unit, savedUri, bulkPrices, threshold)
                    showAddDialog = false
                }
            )
        }

        productToEdit?.let { product ->
            ProductDialog(
                product = product,
                onDismiss = { productToEdit = null },
                onConfirm = { name, cost, price, profit, qty, unit, imageUri, bulkPrices, threshold ->
                    val savedUri = if (imageUri != null && imageUri != product.imageUri) {
                        FileUtils.saveImageToInternalStorage(context, Uri.parse(imageUri))
                    } else {
                        imageUri
                    }

                    viewModel.updateProduct(product.copy(
                        name = name, 
                        unitCost = cost, 
                        salesPrice = price, 
                        profitPerUnit = profit,
                        resellerPrice = price - profit,
                        quantity = qty, 
                        unit = unit,
                        imageUri = savedUri,
                        bulkPrices = bulkPrices,
                        lowStockThreshold = threshold
                    ))
                    productToEdit = null
                }
            )
        }
    }
}

@Composable
fun AdminProductItem(
    product: Product,
    onEdit: (Product) -> Unit,
    onDelete: (Product) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (product.imageUri != null) {
                AsyncImage(
                    model = product.imageUri,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp).padding(end = 16.dp),
                    contentScale = ContentScale.Crop
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = product.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(text = stringResource(R.string.label_cost_price, String.format(java.util.Locale.getDefault(), "%.2f", product.unitCost), String.format(java.util.Locale.getDefault(), "%.2f", product.salesPrice)))
                Text(text = "Lager: ${product.formatQuantity()}", color = MaterialTheme.colorScheme.secondary)
                if (product.bulkPrices.isNotEmpty()) {
                    Text("Rabatter: ${product.bulkPrices.size} st", style = MaterialTheme.typography.bodySmall)
                }
            }
            Row {
                IconButton(onClick = { onEdit(product) }) {
                    Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.action_edit), tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { onDelete(product) }) {
                    Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.action_delete), tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun UnitDropdown(
    selectedUnit: String,
    onUnitSelected: (String) -> Unit,
    availableUnits: List<String>,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            Text(selectedUnit, style = MaterialTheme.typography.bodySmall)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            availableUnits.forEach { u ->
                DropdownMenuItem(
                    text = { Text(u) },
                    onClick = {
                        onUnitSelected(u)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun fromGrams(grams: Int): Pair<String, String> {
    return when {
        grams >= 1000 && grams % 1000 == 0 -> (grams / 1000).toString() to "kg"
        grams >= 100 && grams % 100 == 0 -> (grams / 100).toString() to "hg"
        else -> grams.toString() to "g"
    }
}

private fun toGrams(amountStr: String, unit: String): Int {
    val amount = amountStr.replace(",", ".").toDoubleOrNull() ?: 0.0
    return when (unit) {
        "kg" -> (amount * 1000).toInt()
        "hg" -> (amount * 100).toInt()
        else -> amount.toInt()
    }
}

private fun formatGrams(grams: Int): String {
    if (grams >= 1000 && grams % 1000 == 0) return "${grams / 1000}kg"
    if (grams >= 100 && grams % 100 == 0) return "${grams / 100}hg"
    return "${grams}g"
}

@Composable
fun AdminImageManagerDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var images by remember { mutableStateOf(FileUtils.getAllProductImageUris(context)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mapp: Sparade Produktbilder") },
        text = {
            if (images.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    Text("Ingen bild sparad i mappen")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxHeight(0.6f)) {
                    items(images) { uri ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            AsyncImage(
                                model = uri,
                                contentDescription = null,
                                modifier = Modifier.size(60.dp).clip(RoundedCornerShape(4.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Bild_${uri.takeLast(10)}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                            IconButton(onClick = {
                                FileUtils.deleteImageFromInternalStorage(uri)
                                images = FileUtils.getAllProductImageUris(context)
                            }) {
                                Icon(Icons.Rounded.Delete, "Radera permanent", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Stäng mapp") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDialog(
    product: Product? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Double, Double, Int, String, String?, List<BulkPrice>, Int) -> Unit
) {
    var name by remember { mutableStateOf(product?.name ?: "") }
    var unitCost by remember { mutableStateOf(product?.unitCost?.toString() ?: "") }
    var salesPrice by remember { mutableStateOf(product?.salesPrice?.toString() ?: "") }
    var profitPerUnit by remember { mutableStateOf(product?.profitPerUnit?.toString() ?: "") }
    
    // Beräkna provision till säljaren automatiskt för visning
    val costNum = unitCost.replace(",", ".").toDoubleOrNull() ?: 0.0
    val salesNum = salesPrice.replace(",", ".").toDoubleOrNull() ?: 0.0
    val profitNum = profitPerUnit.replace(",", ".").toDoubleOrNull() ?: 0.0
    val commissionToReseller = if (salesNum > 0) salesNum - costNum - profitNum else 0.0
    
    val weightUnits = listOf("g", "hg", "kg")
    val allPossibleUnits = listOf("st", "kg", "g", "hg", "l", "dl", "cl", "förp")
    
    // Main unit for the product
    var baseUnit by remember { mutableStateOf(product?.unit ?: "g") }
    val isWeight = baseUnit in weightUnits

    // Quantity states
    val initialQty = product?.let { if (it.unit == "g") fromGrams(it.quantity) else it.quantity.toString() to it.unit } ?: ("" to baseUnit)
    var quantityStr by remember { mutableStateOf(initialQty.first) }
    var quantityUnit by remember { mutableStateOf(initialQty.second) }

    // Threshold states
    val initialThreshold = product?.let { if (it.unit == "g") fromGrams(it.lowStockThreshold) else it.lowStockThreshold.toString() to it.unit } ?: ("500" to baseUnit)
    var lowStockThresholdStr by remember { mutableStateOf(initialThreshold.first) }
    var thresholdUnit by remember { mutableStateOf(initialThreshold.second) }

    var imageUri by remember { mutableStateOf(product?.imageUri) }
    var showImageSourcePicker by remember { mutableStateOf(false) }
    var expandedBaseUnit by remember { mutableStateOf(false) }
    var bulkPrices by remember { mutableStateOf(product?.bulkPrices ?: emptyList()) }
    var editingBulkPrice by remember { mutableStateOf<BulkPrice?>(null) }
    var newBulkQty by remember { mutableStateOf("") }
    var newBulkUnit by remember { mutableStateOf(baseUnit) }
    var newBulkPrice by remember { mutableStateOf("") }

    val context = LocalContext.current
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        imageUri = uri?.toString()
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            // Spara bitmap till en temporär fil för att få en URI
            val tempFile = java.io.File(context.cacheDir, "temp_camera_image_${System.currentTimeMillis()}.jpg")
            java.io.FileOutputStream(tempFile).use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
            }
            imageUri = Uri.fromFile(tempFile).toString()
        }
    }

    if (showImageSourcePicker) {
        AlertDialog(
            onDismissRequest = { showImageSourcePicker = false },
            title = { Text("Välj bildkälla") },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text("Kamera") },
                        leadingContent = { Icon(Icons.Rounded.PhotoCamera, null) },
                        modifier = Modifier.clickable { 
                            cameraLauncher.launch(null)
                            showImageSourcePicker = false 
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Galleri") },
                        leadingContent = { Icon(Icons.Rounded.PhotoLibrary, null) },
                        modifier = Modifier.clickable { 
                            imagePickerLauncher.launch("image/*")
                            showImageSourcePicker = false 
                        }
                    )
                }
            },
            confirmButton = {}
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (product == null) stringResource(R.string.action_add_product) else stringResource(R.string.action_edit_product)) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                item {
                    // Image Picker
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clickable { showImageSourcePicker = true },
                        contentAlignment = Alignment.Center
                    ) {
                        if (imageUri != null) {
                            AsyncImage(model = imageUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                        } else {
                            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceVariant) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                    Icon(Icons.Rounded.Add, contentDescription = null)
                                    Text("Lägg till bild")
                                }
                            }
                        }
                    }
                }
                item { TextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.label_name)) }, modifier = Modifier.fillMaxWidth()) }
                
                item {
                    Text("Huvudenhet (Produkt-typ)", style = MaterialTheme.typography.labelSmall)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { expandedBaseUnit = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(baseUnit)
                        }
                        DropdownMenu(expanded = expandedBaseUnit, onDismissRequest = { expandedBaseUnit = false }) {
                            allPossibleUnits.forEach { u ->
                                DropdownMenuItem(text = { Text(u) }, onClick = { 
                                    baseUnit = u
                                    // Sync individual units if not weight
                                    if (u !in weightUnits) {
                                        quantityUnit = u
                                        thresholdUnit = u
                                        newBulkUnit = u
                                    } else {
                                        // If switching to weight family, ensure they are in weight family
                                        if (quantityUnit !in weightUnits) quantityUnit = "g"
                                        if (thresholdUnit !in weightUnits) thresholdUnit = "g"
                                        if (newBulkUnit !in weightUnits) newBulkUnit = "g"
                                    }
                                    expandedBaseUnit = false 
                                })
                            }
                        }
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextField(value = unitCost, onValueChange = { unitCost = it }, label = { Text("Inköp (kr)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        TextField(value = salesPrice, onValueChange = { salesPrice = it }, label = { Text("Utpris (kr)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        TextField(
                            value = profitPerUnit, 
                            onValueChange = { profitPerUnit = it }, 
                            label = { Text("Vinst firma (kr/enhet)") }, 
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                        ) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Säljarprovision", style = MaterialTheme.typography.labelSmall)
                                Text("${String.format(java.util.Locale.getDefault(), "%.2f", commissionToReseller)} kr", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        TextField(
                            value = quantityStr, 
                            onValueChange = { quantityStr = it }, 
                            label = { Text("Lagersaldo") }, 
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        if (isWeight) {
                            UnitDropdown(quantityUnit, { quantityUnit = it }, weightUnits, Modifier.width(75.dp))
                        } else {
                            Text(baseUnit, modifier = Modifier.padding(horizontal = 8.dp))
                        }
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        TextField(
                            value = lowStockThresholdStr, 
                            onValueChange = { lowStockThresholdStr = it }, 
                            label = { Text("Varningsgräns") }, 
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        if (isWeight) {
                            UnitDropdown(thresholdUnit, { thresholdUnit = it }, weightUnits, Modifier.width(75.dp))
                        } else {
                            Text(baseUnit, modifier = Modifier.padding(horizontal = 8.dp))
                        }
                    }
                }
                
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("Rabattstegar", style = MaterialTheme.typography.titleSmall)
                }

                items(bulkPrices) { bp ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val qtyDisplay = if (isWeight) formatGrams(bp.quantity) else "${bp.quantity} $baseUnit"
                        Text("$qtyDisplay för ${bp.price}kr", modifier = Modifier.weight(1f))
                        
                        IconButton(onClick = { 
                            editingBulkPrice = bp
                            newBulkQty = if (isWeight) {
                                val p = fromGrams(bp.quantity)
                                newBulkUnit = p.second
                                p.first
                            } else {
                                bp.quantity.toString()
                            }
                            newBulkPrice = bp.price.toString()
                        }) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Redigera", tint = MaterialTheme.colorScheme.primary)
                        }
                        
                        IconButton(onClick = { bulkPrices = bulkPrices.filter { it != bp } }) {
                            Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(if (editingBulkPrice != null) "Redigera rabatt" else "Lägg till rabatt", style = MaterialTheme.typography.labelSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            TextField(
                                value = newBulkQty, 
                                onValueChange = { newBulkQty = it }, 
                                label = { Text("Mängd") }, 
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            if (isWeight) {
                                UnitDropdown(newBulkUnit, { newBulkUnit = it }, weightUnits, Modifier.width(75.dp))
                            }
                            TextField(
                                value = newBulkPrice, 
                                onValueChange = { newBulkPrice = it }, 
                                label = { Text("Pris") }, 
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            IconButton(onClick = {
                                val q = if (isWeight) toGrams(newBulkQty, newBulkUnit) else newBulkQty.toIntOrNull() ?: 0
                                val p = newBulkPrice.toDoubleOrNull()
                                if (q > 0 && p != null) {
                                    val newBp = BulkPrice(q, p)
                                    bulkPrices = if (editingBulkPrice != null) {
                                        bulkPrices.map { if (it == editingBulkPrice) newBp else it }
                                    } else {
                                        bulkPrices + newBp
                                    }.sortedBy { it.quantity }
                                    
                                    newBulkQty = ""
                                    newBulkPrice = ""
                                    editingBulkPrice = null
                                }
                            }) {
                                Icon(if (editingBulkPrice != null) Icons.Rounded.Check else Icons.Rounded.Add, contentDescription = null)
                            }
                            if (editingBulkPrice != null) {
                                IconButton(onClick = {
                                    editingBulkPrice = null
                                    newBulkQty = ""
                                    newBulkPrice = ""
                                }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Avbryt")
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val finalUnit = if (isWeight) "g" else baseUnit
                val finalQty = if (isWeight) toGrams(quantityStr, quantityUnit) else quantityStr.toIntOrNull() ?: 0
                val finalThreshold = if (isWeight) toGrams(lowStockThresholdStr, thresholdUnit) else lowStockThresholdStr.toIntOrNull() ?: 500
                
                onConfirm(
                    name,
                    unitCost.replace(",", ".").toDoubleOrNull() ?: 0.0,
                    salesPrice.replace(",", ".").toDoubleOrNull() ?: 0.0,
                    profitPerUnit.replace(",", ".").toDoubleOrNull() ?: 0.0,
                    finalQty,
                    finalUnit,
                    imageUri,
                    bulkPrices,
                    finalThreshold
                )
            }) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}
