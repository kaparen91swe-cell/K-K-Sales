package com.example.kksales.ui.screen

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.res.stringResource
import com.example.kksales.R
import com.example.kksales.data.local.entity.Transaction
import com.example.kksales.data.local.entity.TransactionCategory
import com.example.kksales.data.local.entity.TransactionType
import com.example.kksales.data.local.entity.User
import com.example.kksales.data.local.entity.UserInventory
import com.example.kksales.ui.viewmodel.CatalogViewModel
import com.example.kksales.ui.viewmodel.UserViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: UserViewModel, catalogViewModel: CatalogViewModel) {
    val user by viewModel.user.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val userTasks by viewModel.userTasks.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    
    var selectedTab by remember { mutableIntStateOf(0) }
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
        },
        floatingActionButton = {
            if (isKaparen && selectedTab == 0) {
                FloatingActionButton(onClick = { showAddTaskDialog = true }, containerColor = MaterialTheme.colorScheme.tertiaryContainer) {
                    Icon(Icons.AutoMirrored.Rounded.Assignment, contentDescription = "Nytt uppdrag")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            UserInfoHeader(user, transactions, viewModel)

            val products by catalogViewModel.products.collectAsState()
            val inventory by viewModel.userInventory.collectAsState()
            val settings by viewModel.settings.collectAsState()

            if (user?.role == "Transportör") {
                FuelCalculatorSection(user!!, settings, viewModel)
            }

            if (user?.isAdminPlus == true) {
                DeveloperModeSection(settings, viewModel)
            }

            if (inventory.isNotEmpty()) {
                UserInventorySection(inventory, products)
            }

            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Historik") }
                )
                if (isKaparen) {
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text(stringResource(R.string.tab_manage_users)) }
                    )
                }
            }

            Box(modifier = Modifier.padding(16.dp)) {
                if (selectedTab == 0) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (userTasks.isNotEmpty()) {
                            TaskListSection(userTasks, onComplete = { viewModel.completeTask(it.id) })
                        }
                        TransactionList(transactions, products)
                    }
                } else if (selectedTab == 1 && isKaparen) {
                    UserManagementList(allUsers, user, viewModel, products)
                }
            }
        }

        if (showAddTaskDialog) {
            val products by catalogViewModel.products.collectAsState()
            val settings by viewModel.settings.collectAsState()
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
fun DeveloperModeSection(settings: com.example.kksales.data.local.entity.AppSettings, viewModel: UserViewModel) {
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
                Column {
                    Text("Developer Mode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Låser upp redigering och designverktyg", style = MaterialTheme.typography.labelSmall)
                }
                Switch(
                    checked = settings.isDeveloperModeEnabled,
                    onCheckedChange = { viewModel.toggleDeveloperMode() }
                )
            }

            if (settings.isDeveloperModeEnabled) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        // Här triggas "Spara & Push" logiken
                        // I en verklig app skulle detta anropa ett API som startar en GitHub Action
                        viewModel.triggerGithubUpdate()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Rounded.CloudUpload, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Spara Design & Pusha till GitHub")
                }
                Text(
                    "Varning: Detta kommer att generera en ny APK och uppdatera version.json på GitHub.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
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
                    Text("Adress: ${task.address}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
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
                
                TextField(value = address, onValueChange = { address = it }, label = { Text("Leveransadress") }, modifier = Modifier.fillMaxWidth())
                TextField(value = distanceKm, onValueChange = { distanceKm = it }, label = { Text("Avstånd (km)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                
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
fun AdminRegisterDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Boolean, Boolean, Boolean, Boolean, Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isAdmin by remember { mutableStateOf(false) }
    var isAdminPlus by remember { mutableStateOf(false) }
    var isReseller by remember { mutableStateOf(false) }
    var isLageransvarig by remember { mutableStateOf(false) }
    var isTransportor by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "Skapa ny användare", 
                style = MaterialTheme.typography.headlineSmall, 
                fontWeight = FontWeight.Bold 
            ) 
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp), 
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Namn") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Rounded.Person, null) },
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Lösenord (valfritt)") },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Rounded.Lock, null) },
                        singleLine = true
                    )
                }
                
                item {
                    Text(
                        "Behörigheter & Roller", 
                        style = MaterialTheme.typography.titleSmall, 
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(modifier = Modifier.padding(4.dp)) {
                            RoleToggleRow("Administratör", isAdmin) { isAdmin = it }
                            RoleToggleRow("Admin+", isAdminPlus) { isAdminPlus = it }
                            RoleToggleRow("Säljare", isReseller) { isReseller = it }
                            RoleToggleRow("Lageransvarig", isLageransvarig) { isLageransvarig = it }
                            RoleToggleRow("Transportör", isTransportor) { isTransportor = it }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, password, isAdmin, isAdminPlus, isReseller, isLageransvarig, isTransportor) },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Skapa Användare")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Avbryt")
            }
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
fun UserInfoHeader(user: User?, transactions: List<Transaction>, viewModel: UserViewModel) {
    var showIconPicker by remember { mutableStateOf(false) }
    val isAdminPlus = user?.isAdminPlus == true

    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clickable { showIconPicker = true },
                contentAlignment = Alignment.Center
            ) {
                val iconRes = when (user?.profileIcon) {
                    "mafia_1", "mafia_2" -> R.drawable.placeholder
                    "rasta_1" -> R.drawable.placeholder
                    "car_1", "car_2" -> R.drawable.placeholder
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

            Text(user?.name ?: "...", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            if (user?.role != null) {
                Text(user.role, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                if (user.role == "Transportör") {
                    Text(user.vehicleType ?: "Ingen bil vald", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                }
            }
            
            if (isAdminPlus) {
                Text("ADMIN+", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Black)
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
        Triple("mafia_1", Icons.Rounded.Pets, "Boss"),
        Triple("mafia_2", Icons.Rounded.Security, "Boss"),
        Triple("rasta_1", Icons.AutoMirrored.Rounded.DirectionsRun, "Säljare"),
        Triple("car_1", Icons.Rounded.DirectionsCar, "Transportör"),
        Triple("car_2", Icons.Rounded.LocalShipping, "Transportör")
    )

    val availableIcons = if (user.isAdmin) {
        allIcons
    } else {
        allIcons.filter { it.third == "Alla" || it.third == user.role }
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
fun TransactionList(transactions: List<Transaction>, products: List<com.example.kksales.data.local.entity.Product>) {
    Column {
        Text(stringResource(R.string.label_transaction_history), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        if (transactions.isEmpty()) {
            Text(stringResource(R.string.msg_no_transactions))
        } else {
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(transactions) { transaction ->
                    val productName = products.find { it.id == transaction.productId }?.name ?: "Okänd produkt"
                    TransactionItem(transaction, productName = productName)
                }
            }
        }
    }
}

@Composable
fun UserManagementList(users: List<User>, currentUser: User?, viewModel: UserViewModel, products: List<com.example.kksales.data.local.entity.Product>) {
    val settings by viewModel.settings.collectAsState()
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showCreateUserDialog by remember { mutableStateOf(false) }
    var showPriceEditDialog by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Hantera Användare", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            IconButton(onClick = { showSettingsDialog = true }) {
                Icon(Icons.Rounded.Settings, contentDescription = "Appinställningar")
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(users.filter { it.name != "Admin" }) { user ->
                UserAdminItem(user, 
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

    if (showSettingsDialog) {
        AppSettingsDialog(
            settings = settings,
            user = currentUser,
            onDismiss = { showSettingsDialog = false },
            onSave = { newSettings ->
                viewModel.updateSettings(newSettings)
                showSettingsDialog = false
            },
            onAddUser = { 
                showCreateUserDialog = true
                showSettingsDialog = false
            },
            onClearHistory = { viewModel.clearAllTransactions() },
            onResetBalances = { viewModel.resetAllUserBalances() },
            onEditUserPrices = { 
                showPriceEditDialog = true
                showSettingsDialog = false
            }
        )
    }

    if (showCreateUserDialog) {
        AdminRegisterDialog(
            onDismiss = { showCreateUserDialog = false },
            onConfirm = { name, pass, isAdmin, isAdminPlus, isReseller, isLageransvarig, isTransportor ->
                viewModel.registerUser(
                    name = name, 
                    password = if(pass.isBlank()) null else pass, 
                    isAdmin = isAdmin,
                    isAdminPlus = isAdminPlus,
                    isReseller = isReseller,
                    isLageransvarig = isLageransvarig,
                    isTransportor = isTransportor,
                    onSuccess = {},
                    onError = {}
                )
                showCreateUserDialog = false
            }
        )
    }

    if (showPriceEditDialog) {
        UserPriceManagementDialog(
            users = users.filter { it.isReseller },
            products = products,
            onDismiss = { showPriceEditDialog = false },
            onSave = { updatedUser ->
                viewModel.updateUser(updatedUser)
            }
        )
    }
}

@Composable
fun UserPriceManagementDialog(
    users: List<User>,
    products: List<com.example.kksales.data.local.entity.Product>,
    onDismiss: () -> Unit,
    onSave: (User) -> Unit
) {
    var selectedUser by remember { mutableStateOf<User?>(users.firstOrNull()) }
    var expandedUser by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Redigera Säljarpriser") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { expandedUser = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(selectedUser?.name ?: "Välj säljare")
                    }
                    DropdownMenu(expanded = expandedUser, onDismissRequest = { expandedUser = false }) {
                        users.forEach { user ->
                            DropdownMenuItem(text = { Text(user.name) }, onClick = { selectedUser = user; expandedUser = false })
                        }
                    }
                }

                selectedUser?.let { user ->
                    products.forEach { product ->
                        val currentPrice = remember(user.id, product.id) { 
                            mutableStateOf(user.productResellerPrices[product.id]?.toString() ?: "") 
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(product.name, modifier = Modifier.weight(1f))
                            TextField(
                                value = currentPrice.value,
                                onValueChange = { 
                                    currentPrice.value = it
                                    val newPrices = user.productResellerPrices.toMutableMap()
                                    val price = it.replace(",", ".").toDoubleOrNull()
                                    if (price != null) newPrices[product.id] = price else newPrices.remove(product.id)
                                    onSave(user.copy(productResellerPrices = newPrices))
                                },
                                modifier = Modifier.width(80.dp),
                                label = { Text("Pris") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("Klar") } }
    )
}

@Composable
fun AppSettingsDialog(
    settings: com.example.kksales.data.local.entity.AppSettings,
    user: User?,
    onDismiss: () -> Unit,
    onSave: (com.example.kksales.data.local.entity.AppSettings) -> Unit,
    onAddUser: () -> Unit,
    onClearHistory: () -> Unit,
    onResetBalances: () -> Unit,
    onEditUserPrices: () -> Unit
) {
    var p95 by remember { mutableStateOf(settings.fuelPrice95.toString()) }
    var p98 by remember { mutableStateOf(settings.fuelPrice98.toString()) }
    var pDiesel by remember { mutableStateOf(settings.fuelPriceDiesel.toString()) }
    var selectedType by remember { mutableStateOf(settings.selectedFuelType) }
    var consumption by remember { mutableStateOf(settings.fuelConsumption.toString()) }
    var bonus by remember { mutableStateOf(settings.vehicleBonusPerUnit.toString()) }
    var fee by remember { mutableStateOf(settings.vehicleFeePerUnit.toString()) }
    
    var isFetching by remember { mutableStateOf(false) }
    val isAdminPlus = user?.isAdminPlus == true

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = {
                onSave(settings.copy(
                    fuelPrice95 = p95.toDoubleOrNull() ?: settings.fuelPrice95,
                    fuelPrice98 = p98.toDoubleOrNull() ?: settings.fuelPrice98,
                    fuelPriceDiesel = pDiesel.toDoubleOrNull() ?: settings.fuelPriceDiesel,
                    selectedFuelType = selectedType,
                    fuelConsumption = consumption.toDoubleOrNull() ?: settings.fuelConsumption,
                    vehicleBonusPerUnit = bonus.toDoubleOrNull() ?: settings.vehicleBonusPerUnit,
                    vehicleFeePerUnit = fee.toDoubleOrNull() ?: settings.vehicleFeePerUnit
                ))
            }) { Text("Spara") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Avbryt") }
        },
        title = { Text("Globala Inställningar") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                
                Button(onClick = onAddUser, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Skapa ny användare")
                }

                Button(onClick = onEditUserPrices, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Sell, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Redigera Säljarpriser")
                }

                if (isAdminPlus) {
                    HorizontalDivider()
                    Text("Admin+ Verktyg", style = MaterialTheme.typography.labelSmall, color = Color.Red)
                    OutlinedButton(
                        onClick = {
                            onClearHistory()
                            onDismiss()
                        }, 
                        modifier = Modifier.fillMaxWidth(), 
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                    ) {
                        Icon(Icons.Rounded.Delete, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Nollställ All Historik")
                    }
                    OutlinedButton(
                        onClick = {
                            onResetBalances()
                            onDismiss()
                        }, 
                        modifier = Modifier.fillMaxWidth(), 
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                    ) {
                        Icon(Icons.Rounded.Refresh, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Nollställ Allas Saldo & Kontanter")
                    }
                }

                HorizontalDivider()
                Text("Drivmedelspriser (kr/L)", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextField(value = p95, onValueChange = { p95 = it }, label = { Text("95") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    TextField(value = p98, onValueChange = { p98 = it }, label = { Text("98") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    TextField(value = pDiesel, onValueChange = { pDiesel = it }, label = { Text("Diesel") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
                
                Button(
                    onClick = { 
                        isFetching = true
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            p95 = "17.89"
                            p98 = "18.74"
                            pDiesel = "18.12"
                            isFetching = false
                        }, 1500)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isFetching
                ) {
                    if (isFetching) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    else Text("Hämta dagens priser (Sök)")
                }

                Text("Aktiv bränsletyp", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("95", "98", "Diesel").forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type) }
                        )
                    }
                }

                TextField(value = consumption, onValueChange = { consumption = it }, label = { Text("Förbrukning (L/mil)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                TextField(value = bonus, onValueChange = { bonus = it }, label = { Text("Bonus Egen Bil (kr/enhet)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                TextField(value = fee, onValueChange = { fee = it }, label = { Text("Avgift Lånad Bil (kr/enhet)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
        }
    )
}

@Composable
fun UserAdminItem(
    user: User, 
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
                                    DropdownMenuItem(
                                        text = { Text(v) }, 
                                        onClick = { 
                                            onSetVehicle(v)
                                            expandedVehicleMenu = false 
                                        }
                                    )
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
                            DropdownMenuItem(
                                text = { Text("Ingen roll") }, 
                                onClick = { 
                                    onSetRole(null)
                                    expandedRoleMenu = false 
                                }
                            )
                            roles.forEach { role ->
                                DropdownMenuItem(
                                    text = { Text(role) }, 
                                    onClick = { 
                                        onSetRole(role)
                                        expandedRoleMenu = false 
                                    }
                                )
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
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = user.isAdmin, onCheckedChange = { onToggleAdmin() })
                        Text("Admin", style = MaterialTheme.typography.bodySmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = user.isAdminPlus, onCheckedChange = { onToggleAdminPlus() })
                        Text("Admin+", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = user.isReseller, onCheckedChange = { onToggleReseller() })
                        Text("Säljare", style = MaterialTheme.typography.bodySmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = user.isLageransvarig, onCheckedChange = { 
                            onUpdateUser(user.copy(isLageransvarig = !user.isLageransvarig)) 
                        })
                        Text("Lager", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = user.isTransportor, onCheckedChange = { 
                            onUpdateUser(user.copy(isTransportor = !user.isTransportor)) 
                        })
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
                Button(onClick = { 
                    onResetCash()
                    showResetCashConfirm = false 
                }) {
                    Text("Ja, nollställ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetCashConfirm = false }) {
                    Text("Avbryt")
                }
            }
        )
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Återställ saldo") },
            text = { Text("Vill du nollställa saldot för ${user.name}?") },
            confirmButton = {
                Button(onClick = { 
                    onResetBalance()
                    showResetConfirm = false 
                }) {
                    Text("Återställ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("Avbryt")
                }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Ta bort användare") },
            text = { Text("Är du säker på att du vill ta bort ${user.name}? Detta går inte att ångra.") },
            confirmButton = {
                Button(
                    onClick = { 
                        onDelete()
                        showDeleteConfirm = false 
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Ta bort")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Avbryt")
                }
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
    
    val commissions = remember { mutableStateMapOf<Int, String>().apply {
        user.productCommissions.forEach { (id, value) -> put(id, value.toString()) }
    } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Inställningar för ${user.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                Text("Fordon & Drivmedel", style = MaterialTheme.typography.titleSmall)
                TextField(value = fuelPrice, onValueChange = { fuelPrice = it }, label = { Text("Drivmedelspris (kr/L)") }, placeholder = { Text("Global standard") }, modifier = Modifier.fillMaxWidth())
                TextField(value = fuelConsumption, onValueChange = { fuelConsumption = it }, label = { Text("Förbrukning (L/mil)") }, placeholder = { Text("Global standard") }, modifier = Modifier.fillMaxWidth())
                TextField(value = bonus, onValueChange = { bonus = it }, label = { Text("Bonus Egen Bil (kr/enhet)") }, placeholder = { Text("Global standard") }, modifier = Modifier.fillMaxWidth())
                TextField(value = fee, onValueChange = { fee = it }, label = { Text("Avgift Lånad Bil (kr/enhet)") }, placeholder = { Text("Global standard") }, modifier = Modifier.fillMaxWidth())
                
                Spacer(modifier = Modifier.height(16.dp))
                Text("Provision per produkt (kr/g)", style = MaterialTheme.typography.titleSmall)
                
                products.forEach { product ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(product.name, modifier = Modifier.weight(1f))
                        TextField(
                            value = commissions[product.id] ?: "",
                            onValueChange = { commissions[product.id] = it },
                            modifier = Modifier.width(80.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            placeholder = { Text("0.0") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val updatedCommissions = commissions.mapValues { it.value.replace(",", ".").toDoubleOrNull() ?: 0.0 }.filterValues { it != 0.0 }
                onSave(user.copy(
                    fuelPrice = fuelPrice.replace(",", ".").toDoubleOrNull(),
                    fuelConsumption = fuelConsumption.replace(",", ".").toDoubleOrNull(),
                    vehicleBonusPerUnit = bonus.replace(",", ".").toDoubleOrNull(),
                    vehicleFeePerUnit = fee.replace(",", ".").toDoubleOrNull(),
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
