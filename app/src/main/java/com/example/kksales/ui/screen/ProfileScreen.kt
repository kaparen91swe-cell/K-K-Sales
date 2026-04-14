package com.example.kksales.ui.screen

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.example.kksales.R
import com.example.kksales.data.local.entity.Transaction
import com.example.kksales.data.local.entity.TransactionCategory
import com.example.kksales.data.local.entity.TransactionType
import com.example.kksales.data.local.entity.User
import com.example.kksales.data.local.entity.UserInventory
import com.example.kksales.ui.viewmodel.CatalogViewModel
import com.example.kksales.ui.viewmodel.UserViewModel
import com.example.kksales.ui.viewmodel.AdminViewModel
import com.example.kksales.util.FileUtils
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import android.widget.Toast
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: UserViewModel, 
    catalogViewModel: CatalogViewModel,
    adminViewModel: AdminViewModel,
    navController: NavController
) {
    val user by viewModel.user.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
    
    var showAddTaskDialog by remember { mutableStateOf(false) }
    val isKaparen = user?.isAdmin == true || user?.role?.contains("Boss", ignoreCase = true) == true

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_profile)) },
                actions = {
                    IconButton(onClick = { 
                        viewModel.logout {
                            catalogViewModel.clearCartAndRestoreStock()
                        }
                    }) {
                        Icon(Icons.Rounded.Logout, contentDescription = stringResource(R.string.action_logout))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            UserInfoHeader(
                user = user, 
                transactions = transactions, 
                viewModel = viewModel,
                onAddTaskClick = if (isKaparen) { { showAddTaskDialog = true } } else null
            )

            if (user?.role == "Transportör") {
                FuelCalculatorSection(user!!, settings, viewModel)
            }

            if (user?.isAdminPlus == true) {
                DeveloperModeSection(settings, viewModel)
            }

            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.label_tools), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
            
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MenuCard(stringResource(R.string.label_history), stringResource(R.string.desc_history), Icons.Rounded.History) {
                    navController.navigate("history")
                }
                
                if (isKaparen) {
                    MenuCard(stringResource(R.string.label_manage_users), stringResource(R.string.desc_manage_users), Icons.Rounded.People) {
                        navController.navigate("manage_users")
                    }
                    MenuCard(stringResource(R.string.label_settings), stringResource(R.string.desc_settings), Icons.Rounded.Settings) {
                        navController.navigate("global_settings")
                    }
                }
            }
        }

        if (showAddTaskDialog) {
            val products by catalogViewModel.products.collectAsState()
            AddTaskDialog(
                users = allUsers.filter { it.id != user?.id },
                products = products,
                settings = settings,
                onDismiss = { showAddTaskDialog = false },
                onConfirm = { toUserId, title, desc, addr, dist, pId, qty, unit, fType, fPrice ->
                    viewModel.addTask(
                        com.example.kksales.data.local.entity.Task(
                            assignedToUserId = toUserId,
                            assignedByUserId = user?.id ?: 0,
                            title = title,
                            description = desc,
                            address = addr,
                            distanceKm = dist,
                            productId = pId,
                            quantity = qty,
                            unit = unit,
                            fuelTypeAtCreation = fType,
                            fuelPriceAtCreation = fPrice
                        )
                    )
                    showAddTaskDialog = false
                }
            )
        }
    }
}

@Composable
fun MenuCard(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(icon, null, modifier = Modifier.padding(8.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            }
            Spacer(Modifier.weight(1f))
            Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
fun DeveloperModeSection(settings: com.example.kksales.data.local.entity.AppSettings, viewModel: UserViewModel) {
    var devClickCount by remember { mutableIntStateOf(0) }
    
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f).clickable { 
                        if (settings.isDeveloperModeEnabled) {
                            devClickCount++
                        }
                    }
                ) {
                    Text(stringResource(R.string.label_dev_mode), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.desc_dev_mode), style = MaterialTheme.typography.labelSmall)
                }
                Switch(
                    checked = settings.isDeveloperModeEnabled,
                    onCheckedChange = { 
                        viewModel.toggleDeveloperMode() 
                        devClickCount = 0
                    }
                )
            }

            if (settings.isDeveloperModeEnabled && devClickCount >= 3) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        viewModel.triggerGithubUpdate()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Rounded.CloudUpload, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.action_push_github))
                }

                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        viewModel.resetGlobalStatistics()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Rounded.DeleteForever, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.action_reset_stats))
                }
            }
        }
    }
}

@Composable
fun TaskListSection(tasks: List<com.example.kksales.data.local.entity.Task>, onComplete: (com.example.kksales.data.local.entity.Task) -> Unit) {
    Column {
        Text("Aktuella Uppdrag", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        tasks.filter { !it.isCompleted }.forEach { task ->
            TaskItem(task, onComplete = { onComplete(task) })
            Spacer(modifier = Modifier.height(8.dp))
        }
        if (tasks.any { it.isCompleted }) {
            Text("Slutförda Uppdrag", style = MaterialTheme.typography.titleSmall, color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
            tasks.filter { it.isCompleted }.take(3).forEach { task ->
                TaskItem(task, onComplete = {})
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun TaskItem(task: com.example.kksales.data.local.entity.Task, onComplete: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(task.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                if (task.description.isNotEmpty()) {
                    Text(task.description, style = MaterialTheme.typography.bodySmall)
                }
                
                if (task.address != null) {
                    Text(
                        text = "Adress: ${task.address}", 
                        style = MaterialTheme.typography.bodySmall, 
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            val uri = android.net.Uri.parse("google.navigation:q=${android.net.Uri.encode(task.address)}")
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                            intent.setPackage("com.google.android.apps.maps")
                            context.startActivity(intent)
                        }
                    )
                }
                if (task.productId != null && task.quantity != null) {
                    Text("Leverans: ${task.quantity} ${task.unit ?: ""} (Produkt ID: ${task.productId})", style = MaterialTheme.typography.bodySmall)
                }
                if (task.distanceKm != null) {
                    val fuelPrice = task.fuelPriceAtCreation ?: 0.0
                    val estimatedCost = (task.distanceKm / 10.0) * 0.7 * fuelPrice
                    Text("Sträcka: ${task.distanceKm} km (Bränsle: ${task.fuelTypeAtCreation ?: "95"} @ ${String.format(Locale.getDefault(), "%.2f", fuelPrice)} kr)", style = MaterialTheme.typography.labelSmall)
                    Text("Beräknad bränslekostnad: ${String.format(Locale.getDefault(), "%.2f", estimatedCost)} kr", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }

                val date = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(task.timestamp))
                Text(date, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
            if (!task.isCompleted) {
                IconButton(onClick = onComplete) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = "Klar", tint = Color(0xFF4CAF50))
                }
            } else {
                Icon(Icons.Rounded.DoneAll, contentDescription = "Slutförd", tint = Color.Gray, modifier = Modifier.padding(12.dp))
            }
        }
    }
}

@Composable
fun AddTaskDialog(
    users: List<User>,
    products: List<com.example.kksales.data.local.entity.Product>,
    settings: com.example.kksales.data.local.entity.AppSettings,
    onDismiss: () -> Unit,
    onConfirm: (Int, String, String, String?, Double?, Int?, Int?, String?, String?, Double?) -> Unit
) {
    var selectedUserId by remember { mutableIntStateOf(users.firstOrNull()?.id ?: 0) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    
    var address by remember { mutableStateOf("") }
    var distanceKm by remember { mutableStateOf("") }
    var selectedProductId by remember { mutableStateOf<Int?>(null) }
    var quantity by remember { mutableStateOf("") }
    
    var expandedUser by remember { mutableStateOf(false) }
    var expandedProduct by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Skapa Arbetsorder / Uppdrag") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                Text("Tilldela till:", style = MaterialTheme.typography.labelSmall)
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { expandedUser = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(users.find { it.id == selectedUserId }?.name ?: "Välj användare")
                    }
                    DropdownMenu(expanded = expandedUser, onDismissRequest = { expandedUser = false }) {
                        users.forEach { user ->
                            DropdownMenuItem(text = { Text(user.name) }, onClick = { selectedUserId = user.id; expandedUser = false })
                        }
                    }
                }
                
                TextField(value = title, onValueChange = { title = it }, label = { Text("Rubrik") }, modifier = Modifier.fillMaxWidth())
                TextField(value = description, onValueChange = { description = it }, label = { Text("Beskrivning") }, modifier = Modifier.fillMaxWidth())
                
                HorizontalDivider()
                Text("Leveransdetaljer (Valfritt)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                
                TextField(
                    value = address, 
                    onValueChange = { address = it }, 
                    label = { Text("Leveransadress") }, 
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = {
                            val uri = android.net.Uri.parse("geo:0,0?q=${android.net.Uri.encode(address)}")
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                            intent.setPackage("com.google.android.apps.maps")
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // Maps not installed
                            }
                        }) {
                            Icon(Icons.Rounded.Map, "Sök på karta")
                        }
                    }
                )
                TextField(
                    value = distanceKm, 
                    onValueChange = { distanceKm = it }, 
                    label = { Text("Avstånd (km)") }, 
                    modifier = Modifier.fillMaxWidth(), 
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    trailingIcon = {
                        IconButton(onClick = {
                            // Hämta pris från settings automatiskt
                            // Detta fält kan t.ex. användas för att trigga en beräkning
                        }) {
                            Icon(Icons.Rounded.AutoFixHigh, "Hämta data")
                        }
                    }
                )
                
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { expandedProduct = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(products.find { it.id == selectedProductId }?.name ?: "Välj produkt")
                    }
                    DropdownMenu(expanded = expandedProduct, onDismissRequest = { expandedProduct = false }) {
                        DropdownMenuItem(text = { Text("Ingen produkt") }, onClick = { selectedProductId = null; expandedProduct = false })
                        products.forEach { product ->
                            DropdownMenuItem(text = { Text(product.name) }, onClick = { selectedProductId = product.id; expandedProduct = false })
                        }
                    }
                }
                
                if (selectedProductId != null) {
                    TextField(value = quantity, onValueChange = { quantity = it }, label = { Text("Mängd (${products.find { it.id == selectedProductId }?.unit ?: "g"})") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
            }
        },
        confirmButton = {
            Button(onClick = { 
                onConfirm(
                    selectedUserId, 
                    title, 
                    description, 
                    address.ifBlank { null },
                    distanceKm.toDoubleOrNull(),
                    selectedProductId,
                    quantity.toIntOrNull(),
                    products.find { it.id == selectedProductId }?.unit,
                    settings.selectedFuelType,
                    settings.fuelPrice
                ) 
            }, enabled = title.isNotEmpty() && selectedUserId != 0) {
                Text("Skicka Order")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Avbryt") }
        }
    )
}

@Composable
fun RoleToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(
            checked = checked, 
            onCheckedChange = onCheckedChange,
            thumbContent = if (checked) {
                {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize),
                    )
                }
            } else null
        )
    }
}

@Composable
fun UserInfoHeader(
    user: User?, 
    transactions: List<Transaction>, 
    viewModel: UserViewModel,
    onAddTaskClick: (() -> Unit)? = null
) {
    var showIconPicker by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clickable { showIconPicker = true },
                    contentAlignment = Alignment.Center
                ) {
                    val iconRes = when (user?.profileIcon) {
                        "boss_bulldog_1" -> R.drawable.boss_bulldog_1
                        "boss_bulldog_2" -> R.drawable.boss_bulldog_2
                        "boss_bulldog_3" -> R.drawable.boss_bulldog_3
                        "boss_bulldog_4" -> R.drawable.boss_bulldog_4
                        "reseller_rasta_1" -> R.drawable.reseller_rasta_1
                        "reseller_rasta_2" -> R.drawable.reseller_rasta_2
                        "reseller_rasta_3" -> R.drawable.reseller_rasta_3
                        "reseller_rasta_4" -> R.drawable.reseller_rasta_4
                        "reseller_rasta_5" -> R.drawable.reseller_rasta_5
                        "reseller_sales_1" -> R.drawable.reseller_sales_1
                        "reseller_sales_2" -> R.drawable.reseller_sales_2
                        "reseller_sales_3" -> R.drawable.reseller_sales_3
                        "reseller_sales_4" -> R.drawable.reseller_sales_4
                        "reseller_sales_5" -> R.drawable.reseller_sales_5
                        "reseller_sales_6" -> R.drawable.reseller_sales_6
                        "transporter_jah_1" -> R.drawable.transporter_jah_1
                        "transporter_jah_2" -> R.drawable.transporter_jah_2
                        "transporter_north_1" -> R.drawable.transporter_north_1
                        "transporter_express_1" -> R.drawable.transporter_express_1
                        else -> null
                    }

                    if (iconRes != null) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = androidx.compose.ui.graphics.Color.Unspecified
                        )
                    } else {
                        Icon(
                            Icons.Rounded.Person,
                            null,
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    
                    Surface(
                        modifier = Modifier.align(Alignment.BottomEnd),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            Icons.Rounded.Edit,
                            null,
                            modifier = Modifier.padding(4.dp).size(12.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                if (onAddTaskClick != null) {
                    IconButton(
                        onClick = onAddTaskClick,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shadowElevation = 2.dp
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.Assignment,
                                contentDescription = "Nytt uppdrag",
                                modifier = Modifier.padding(8.dp),
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }

            Text(user?.name ?: "...", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            if (user?.role != null) {
                Text(user.role, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                InfoStat("Vinst/Saldo", "${stringResource(R.string.currency_symbol)}${String.format(Locale.getDefault(), "%.2f", user?.balance ?: 0.0)}")
                if (user?.isAdmin == true || user?.isReseller == true) {
                    InfoStat("Kontanter", "${stringResource(R.string.currency_symbol)}${String.format(Locale.getDefault(), "%.2f", user.cashBalance)}")
                }
                val totalSpent = transactions.filter { it.type == TransactionType.EXPENSE && it.category == TransactionCategory.PURCHASE }.sumOf { it.amount }
                InfoStat("Totalt köpt", "${stringResource(R.string.currency_symbol)}${String.format(Locale.getDefault(), "%.2f", totalSpent)}")
            }
        }
    }

    if (showIconPicker && user != null) {
        IconPickerDialog(
            user = user,
            onDismiss = { showIconPicker = false },
            onSelect = { iconName ->
                viewModel.updateProfileIcon(iconName)
                showIconPicker = false
            }
        )
    }
}

@Composable
fun IconPickerDialog(user: User, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    val allIcons = listOf(
        Triple("default", Icons.Rounded.Person, "Alla"),
        Triple("boss_bulldog_1", Icons.Rounded.Pets, "Boss"),
        Triple("boss_bulldog_2", Icons.Rounded.Pets, "Boss"),
        Triple("boss_bulldog_3", Icons.Rounded.Pets, "Boss"),
        Triple("boss_bulldog_4", Icons.Rounded.Pets, "Boss"),
        Triple("reseller_rasta_1", Icons.Rounded.DirectionsRun, "Säljare"),
        Triple("reseller_rasta_2", Icons.Rounded.DirectionsRun, "Säljare"),
        Triple("reseller_rasta_3", Icons.Rounded.DirectionsRun, "Säljare"),
        Triple("reseller_rasta_4", Icons.Rounded.DirectionsRun, "Säljare"),
        Triple("reseller_rasta_5", Icons.Rounded.DirectionsRun, "Säljare"),
        Triple("reseller_sales_1", Icons.Rounded.BusinessCenter, "Säljare"),
        Triple("reseller_sales_2", Icons.Rounded.BusinessCenter, "Säljare"),
        Triple("reseller_sales_3", Icons.Rounded.BusinessCenter, "Säljare"),
        Triple("reseller_sales_4", Icons.Rounded.BusinessCenter, "Säljare"),
        Triple("reseller_sales_5", Icons.Rounded.BusinessCenter, "Säljare"),
        Triple("reseller_sales_6", Icons.Rounded.BusinessCenter, "Säljare"),
        Triple("transporter_jah_1", Icons.Rounded.LocalShipping, "Transportör"),
        Triple("transporter_jah_2", Icons.Rounded.LocalShipping, "Transportör"),
        Triple("transporter_north_1", Icons.Rounded.LocalShipping, "Transportör"),
        Triple("transporter_express_1", Icons.Rounded.LocalShipping, "Transportör")
    )

    val availableIcons = if (user.isAdminPlus) {
        allIcons
    } else {
        allIcons.filter { 
            it.third == "Alla" || 
            (it.third == "Boss" && (user.role?.contains("Boss") == true || user.isAdmin)) ||
            (it.third == "Säljare" && (user.role == "Säljare" || user.isReseller)) ||
            (it.third == "Transportör" && (user.role == "Transportör" || user.isTransportor))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Välj profilbild") },
        text = {
            Box(modifier = Modifier.height(300.dp)) {
                androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                    columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(availableIcons.size) { index ->
                        val (id, icon, category) = availableIcons[index]
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { onSelect(id) }.padding(8.dp)
                        ) {
                            Icon(icon, contentDescription = id, modifier = Modifier.size(48.dp))
                            Text(category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Avbryt") }
        }
    )
}

@Composable
fun UserInventorySection(inventory: List<UserInventory>, allProducts: List<com.example.kksales.data.local.entity.Product>) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Mitt Lager", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            inventory.forEach { item ->
                val product = allProducts.find { it.id == item.productId }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(product?.name ?: "Okänd produkt")
                    Text("${item.quantity} ${product?.unit ?: "st"}", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun FuelCalculatorSection(user: User, settings: com.example.kksales.data.local.entity.AppSettings, viewModel: UserViewModel) {
    var distanceStr by remember { mutableStateOf("") }
    val distance = distanceStr.toDoubleOrNull() ?: 0.0
    val cost = (distance / 10.0) * settings.fuelConsumption * settings.fuelPrice

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Bränsleuträkning", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Pris: ${settings.fuelPrice} kr/L | Förbr: ${settings.fuelConsumption} L/mil", style = MaterialTheme.typography.labelSmall)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextField(
                    value = distanceStr,
                    onValueChange = { distanceStr = it },
                    label = { Text("Sträcka (km)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text("Kostnad:", style = MaterialTheme.typography.labelMedium)
                    Text("${String.format(Locale.getDefault(), "%.2f", cost)} kr", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                }
            }
            
            Button(
                onClick = { 
                    viewModel.addDistanceTransaction(distance)
                    distanceStr = ""
                },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                enabled = distance > 0
            ) {
                Text("Registrera resa")
            }
        }
    }
}

@Composable
fun InfoStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun UserManagementList(users: List<User>, currentUser: User?, viewModel: UserViewModel, products: List<com.example.kksales.data.local.entity.Product>, navController: NavController) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Hantera Användare", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row {
                IconButton(onClick = { navController.navigate("edit_user_prices") }) {
                    Icon(Icons.Rounded.Sell, "Priser")
                }
                IconButton(onClick = { navController.navigate("create_user") }) {
                    Icon(Icons.Rounded.Add, "Skapa")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
            items(users.filter { it.name != "Admin" }) { user ->
                UserAdminItem(user, 
                    loggedInUser = currentUser,
                    onToggleAdmin = { viewModel.toggleAdminStatus(user) },
                    onToggleAdminPlus = { viewModel.toggleAdminPlusStatus(user) },
                    onToggleReseller = { viewModel.toggleResellerStatus(user) },
                    onDelete = { viewModel.deleteUser(user) },
                    onResetBalance = { viewModel.resetUserBalance(user) },
                    onResetCash = { viewModel.resetCashBalance(user) },
                    onSetRole = { role -> viewModel.updateUserRole(user, role) },
                    onSetVehicle = { vehicle -> viewModel.updateVehicleType(user, vehicle) },
                    onUpdateUser = { updatedUser -> viewModel.updateUser(updatedUser) },
                    products = products
                )
            }
        }
    }
}

@Composable
fun UserAdminItem(
    user: User, 
    loggedInUser: User?,
    onToggleAdmin: () -> Unit, 
    onToggleAdminPlus: () -> Unit,
    onToggleReseller: () -> Unit, 
    onDelete: () -> Unit, 
    onResetBalance: () -> Unit, 
    onResetCash: () -> Unit, 
    onSetRole: (String?) -> Unit, 
    onSetVehicle: (String?) -> Unit,
    onUpdateUser: (User) -> Unit,
    products: List<com.example.kksales.data.local.entity.Product>
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var showResetCashConfirm by remember { mutableStateOf(false) }
    var showUserSettingsDialog by remember { mutableStateOf(false) }

    val roles = listOf("Transportör", "Säljare", "Boss", "Andra Boss", "Lagerarbetare")
    val vehicles = listOf("Egen bil", "Lånad bil", "Ingen bil")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showUserSettingsDialog = true }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(user.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Roll: ${user.role ?: "Ingen"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    if (user.role == "Transportör") {
                        Text("Fordon: ${user.vehicleType ?: "Ej valt"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                    }
                    Text("Saldo (Vinst): ${String.format(Locale.getDefault(), "%.2f", user.balance)} kr", style = MaterialTheme.typography.bodySmall)
                    Text("Kontanter: ${String.format(Locale.getDefault(), "%.2f", user.cashBalance)} kr", style = MaterialTheme.typography.bodySmall, color = if(user.cashBalance > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (user.role == "Transportör") {
                        Box {
                            var expandedVehicleMenu by remember { mutableStateOf(false) }
                            IconButton(onClick = { expandedVehicleMenu = true }) {
                                Icon(Icons.Rounded.DirectionsCar, contentDescription = "Sätt fordon")
                            }
                            DropdownMenu(expanded = expandedVehicleMenu, onDismissRequest = { expandedVehicleMenu = false }) {
                                vehicles.forEach { v ->
                                    DropdownMenuItem(text = { Text(v) }, onClick = { onSetVehicle(v); expandedVehicleMenu = false })
                                }
                            }
                        }
                    }
                    Box {
                        var expandedRoleMenu by remember { mutableStateOf(false) }
                        IconButton(onClick = { expandedRoleMenu = true }) {
                            Icon(Icons.Rounded.Work, contentDescription = "Sätt roll")
                        }
                        DropdownMenu(expanded = expandedRoleMenu, onDismissRequest = { expandedRoleMenu = false }) {
                            DropdownMenuItem(text = { Text("Ingen roll") }, onClick = { onSetRole(null); expandedRoleMenu = false })
                            roles.forEach { role ->
                                DropdownMenuItem(text = { Text(role) }, onClick = { onSetRole(role); expandedRoleMenu = false })
                            }
                        }
                    }
                    IconButton(onClick = { showResetConfirm = true }) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Återställ vinst", tint = MaterialTheme.colorScheme.primary)
                    }
                    if (user.cashBalance > 0) {
                        IconButton(onClick = { showResetCashConfirm = true }) {
                            Icon(Icons.Rounded.Payments, contentDescription = "Nollställ kontanter", tint = MaterialTheme.colorScheme.secondary)
                        }
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Ta bort", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                val isOwner = loggedInUser?.isAdminPlus == true
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = user.isAdmin, 
                            onCheckedChange = { onToggleAdmin() },
                            enabled = isOwner
                        )
                        Text("Admin", style = MaterialTheme.typography.bodySmall, color = if(isOwner) Color.Unspecified else Color.Gray)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = user.isAdminPlus, 
                            onCheckedChange = { onToggleAdminPlus() },
                            enabled = isOwner
                        )
                        Text("Ägare", style = MaterialTheme.typography.bodySmall, color = if(isOwner) Color.Unspecified else Color.Gray)
                    }
                }
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = user.isReseller, onCheckedChange = { onToggleReseller() })
                        Text("Säljare", style = MaterialTheme.typography.bodySmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = user.isLageransvarig, onCheckedChange = { onUpdateUser(user.copy(isLageransvarig = !user.isLageransvarig)) })
                        Text("Lager", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = user.isTransportor, onCheckedChange = { onUpdateUser(user.copy(isTransportor = !user.isTransportor)) })
                        Text("Transport", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    if (showUserSettingsDialog) {
        UserSettingsDialog(
            user = user,
            products = products,
            onDismiss = { showUserSettingsDialog = false },
            onSave = { updatedUser ->
                onUpdateUser(updatedUser)
                showUserSettingsDialog = false
            }
        )
    }

    if (showResetCashConfirm) {
        AlertDialog(
            onDismissRequest = { showResetCashConfirm = false },
            title = { Text("Nollställ mottagna kontanter") },
            text = { Text("Har ${user.name} redovisat ${String.format(Locale.getDefault(), "%.2f", user.cashBalance)} kr kontant?") },
            confirmButton = {
                Button(onClick = { onResetCash(); showResetCashConfirm = false }) { Text("Ja, nollställ") }
            },
            dismissButton = {
                TextButton(onClick = { showResetCashConfirm = false }) { Text("Avbryt") }
            }
        )
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Återställ saldo") },
            text = { Text("Vill du nollställa saldot för ${user.name}?") },
            confirmButton = {
                Button(onClick = { onResetBalance(); showResetConfirm = false }) { Text("Återställ") }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("Avbryt") }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Ta bort användare") },
            text = { Text("Är du säker på att du vill ta bort ${user.name}? Detta går inte att ångra.") },
            confirmButton = {
                Button(onClick = { onDelete(); showDeleteConfirm = false }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Ta bort") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Avbryt") }
            }
        )
    }
}

@Composable
fun UserSettingsDialog(
    user: User,
    products: List<com.example.kksales.data.local.entity.Product>,
    onDismiss: () -> Unit,
    onSave: (User) -> Unit
) {
    var fuelPrice by remember { mutableStateOf(user.fuelPrice?.toString() ?: "") }
    var fuelConsumption by remember { mutableStateOf(user.fuelConsumption?.toString() ?: "") }
    var bonus by remember { mutableStateOf(user.vehicleBonusPerUnit?.toString() ?: "") }
    var fee by remember { mutableStateOf(user.vehicleFeePerUnit?.toString() ?: "") }
    var fuelType by remember { mutableStateOf(user.preferredFuelType ?: "95") }
    
    var storageCost by remember { mutableStateOf(user.storageCost?.toString() ?: "") }
    var storageInterval by remember { mutableStateOf(user.storagePaymentInterval ?: "Weekly") }
    var storageDay by remember { mutableStateOf(user.storagePaymentDay?.toString() ?: "1") }
    
    var newPassword by remember { mutableStateOf("") }
    var showPasswordChange by remember { mutableStateOf(false) }

    val commissions = remember { mutableStateMapOf<Int, String>().apply {
        user.productCommissions.forEach { (id, value) -> put(id, value.toString()) }
    } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Inställningar för ${user.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                if (user.isTransportor) {
                    Text("Fordon & Drivmedel", style = MaterialTheme.typography.titleSmall)
                    
                    Text("Aktiv bränsletyp", style = MaterialTheme.typography.labelSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("95", "98", "Diesel").forEach { type ->
                            FilterChip(
                                selected = fuelType == type,
                                onClick = { fuelType = type },
                                label = { Text(type) }
                            )
                        }
                    }
                    
                    TextField(value = fuelPrice, onValueChange = { fuelPrice = it }, label = { Text("Drivmedelspris (kr/L)") }, placeholder = { Text("Hämtas automatiskt") }, modifier = Modifier.fillMaxWidth())
                    TextField(value = fuelConsumption, onValueChange = { fuelConsumption = it }, label = { Text("Förbrukning (L/mil)") }, modifier = Modifier.fillMaxWidth())
                    TextField(value = bonus, onValueChange = { bonus = it }, label = { Text("Bonus Egen Bil (kr/enhet)") }, modifier = Modifier.fillMaxWidth())
                    TextField(value = fee, onValueChange = { fee = it }, label = { Text("Avgift Lånad Bil (kr/enhet)") }, modifier = Modifier.fillMaxWidth())
                }

                if (user.isLageransvarig) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("Lagerhållning & Hyra", style = MaterialTheme.typography.titleSmall)
                    TextField(value = storageCost, onValueChange = { storageCost = it }, label = { Text("Kostnad (kr)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            var expanded by remember { mutableStateOf(false) }
                            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                                Text(if(storageInterval == "Weekly") "Veckovis" else "Månadsvis")
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                DropdownMenuItem(text = { Text("Veckovis") }, onClick = { storageInterval = "Weekly"; expanded = false })
                                DropdownMenuItem(text = { Text("Månadsvis") }, onClick = { storageInterval = "Monthly"; expanded = false })
                            }
                        }
                        TextField(
                            value = storageDay, 
                            onValueChange = { storageDay = it }, 
                            label = { Text(if(storageInterval == "Weekly") "Dag (1-7)" else "Datum (1-31)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }
                
                if (user.isReseller) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Provision per produkt (kr/g)", style = MaterialTheme.typography.titleSmall)
                    
                    products.forEach { product ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(product.name, modifier = Modifier.weight(1f))
                            TextField(
                                value = commissions[product.id] ?: "",
                                onValueChange = { commissions[product.id] = it },
                                modifier = Modifier.width(80.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                Text("Säkerhet", style = MaterialTheme.typography.titleSmall)
                
                if (!showPasswordChange) {
                    Button(
                        onClick = { showPasswordChange = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Rounded.LockReset, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Ändra lösenord")
                    }
                } else {
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("Nytt lösenord") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPasswordChange = false; newPassword = "" }) {
                                Icon(Icons.Rounded.Close, null)
                            }
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val updatedCommissions = commissions.mapValues { it.value.replace(",", ".").toDoubleOrNull() ?: 0.0 }.filterValues { it != 0.0 }
                onSave(user.copy(
                    password = if (newPassword.isNotBlank()) newPassword else user.password,
                    fuelPrice = fuelPrice.replace(",", ".").toDoubleOrNull(),
                    fuelConsumption = fuelConsumption.replace(",", ".").toDoubleOrNull(),
                    vehicleBonusPerUnit = bonus.replace(",", ".").toDoubleOrNull(),
                    vehicleFeePerUnit = fee.replace(",", ".").toDoubleOrNull(),
                    preferredFuelType = fuelType,
                    storageCost = storageCost.replace(",", ".").toDoubleOrNull(),
                    storagePaymentInterval = storageInterval,
                    storagePaymentDay = storageDay.toIntOrNull(),
                    productCommissions = updatedCommissions
                ))
            }) { Text("Spara") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Avbryt") }
        }
    )
}

@Composable
fun AdminInventorySection(viewModel: AdminViewModel) {
    val products by viewModel.products.collectAsState()
    var productToEdit by remember { mutableStateOf<com.example.kksales.data.local.entity.Product?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showImageManager by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Hantera Produkter", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row {
                IconButton(onClick = { showImageManager = true }) {
                    Icon(Icons.Rounded.FolderOpen, "Hantera produktbilder")
                }
                IconButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Rounded.Add, "Lägg till produkt")
                }
            }
        }
        
        Spacer(Modifier.height(8.dp))
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
            items(products) { product ->
                AdminProductItem(
                    product = product,
                    onEdit = { productToEdit = it },
                    onDelete = { viewModel.deleteProduct(it) }
                )
            }
        }
    }

    if (showImageManager) {
        ImageManagerDialog(
            onDismiss = { showImageManager = false }
        )
    }

    if (showAddDialog) {
        ProductDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, cost, price, profit, qty, unit, imageUri, bulkPrices, threshold ->
                val savedUri = imageUri?.let { uriStr ->
                    FileUtils.saveImageToInternalStorage(context, android.net.Uri.parse(uriStr))
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
                    FileUtils.saveImageToInternalStorage(context, android.net.Uri.parse(imageUri))
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

@Composable
fun ImageManagerDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var images by remember { mutableStateOf(FileUtils.getAllProductImageUris(context)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Hanterade Produktbilder") },
        text = {
            if (images.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    Text("Inga sparade bilder")
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
                            Text(uri.substringAfterLast("/").take(15) + "...", modifier = Modifier.weight(1f))
                            IconButton(onClick = {
                                FileUtils.deleteImageFromInternalStorage(uri)
                                images = FileUtils.getAllProductImageUris(context)
                            }) {
                                Icon(Icons.Rounded.Delete, "Ta bort bild", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Stäng") }
        }
    )
}

@Composable
fun TransactionItem(transaction: Transaction, receiverName: String? = null, productName: String? = null) {
    val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                val label = if (transaction.category == TransactionCategory.PURCHASE && productName != null) productName else transaction.category.displayName
                Text(label, fontWeight = FontWeight.Bold)
                if (receiverName != null) {
                    Text("Mottagare: $receiverName", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                }
                Text(sdf.format(Date(transaction.timestamp)), style = MaterialTheme.typography.bodySmall)
            }
            Text(
                "${if (transaction.type == TransactionType.EXPENSE) "-" else "+"}${stringResource(R.string.currency_symbol)}${String.format(Locale.getDefault(), "%.2f", transaction.amount)}",
                color = if (transaction.type == TransactionType.EXPENSE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
