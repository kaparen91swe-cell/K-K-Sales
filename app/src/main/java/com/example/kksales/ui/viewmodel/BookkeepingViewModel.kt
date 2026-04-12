package com.example.kksales.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.kksales.data.local.entity.Transaction
import com.example.kksales.data.local.entity.TransactionCategory
import com.example.kksales.data.local.entity.TransactionType
import com.example.kksales.data.local.entity.User
import com.example.kksales.data.local.entity.BulkPrice
import com.example.kksales.data.local.entity.Product
import com.example.kksales.data.local.entity.calculatePrice
import com.example.kksales.data.local.entity.AppSettings
import com.example.kksales.data.repository.TransactionRepository
import com.example.kksales.data.repository.UserRepository
import com.example.kksales.data.repository.ProductRepository
import com.example.kksales.data.repository.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

import androidx.core.content.FileProvider
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.UnitValue
import com.itextpdf.layout.element.Cell
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BookkeepingUiState(
    val transactions: List<Transaction> = emptyList(),
    val totalRevenue: Double = 0.0,
    val totalCost: Double = 0.0,
    val totalProfit: Double = 0.0,
    val users: List<User> = emptyList(),
    val selectedUserId: Int? = null,
    val selectedCategory: String = "Alla",
    val customAmount: String = "",
    val customDescription: String = "",
    val exportProgress: Float? = null,
    val showExportCompleteOptions: File? = null,
    val chartData: Map<String, Double> = emptyMap()
)

class BookkeepingViewModel(
    private val transactionRepository: TransactionRepository,
    private val userRepository: UserRepository,
    private val productRepository: ProductRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser = _currentUser.asStateFlow()

    private val _selectedUserId = MutableStateFlow<Int?>(null)
    val selectedUserId = _selectedUserId.asStateFlow()

    private val _selectedCategory = MutableStateFlow("Alla")
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _customAmount = MutableStateFlow("")
    val customAmount = _customAmount.asStateFlow()

    private val _customDescription = MutableStateFlow("")
    val customDescription = _customDescription.asStateFlow()

    private val _exportProgress = MutableStateFlow<Float?>(null)
    private val _exportFile = MutableStateFlow<File?>(null)

    val users = userRepository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val products = productRepository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings = settingsRepository.settings
        .map { it ?: AppSettings() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    fun setCurrentUser(user: User?) {
        _currentUser.value = user
        if (user != null && !user.isAdmin) {
            _selectedUserId.value = user.id
        } else if (user?.isAdmin == true) {
            _selectedUserId.value = null
        }
    }

    val uiState: StateFlow<BookkeepingUiState> = combine(
        transactionRepository.allTransactions,
        _selectedUserId,
        _selectedCategory,
        _customAmount,
        _customDescription,
        users,
        _currentUser,
        _exportProgress,
        _exportFile
    ) { params: Array<Any?> ->
        val transactions = params[0] as List<Transaction>
        val selectedId = params[1] as Int?
        val category = params[2] as String
        val amountStr = params[3] as String
        val description = params[4] as String
        val allUsers = params[5] as List<User>
        val currentViewingUser = params[6] as User?
        val progress = params[7] as Float?
        val file = params[8] as File?

        val visibleTransactions = if (currentViewingUser?.isAdmin == true) {
            transactions 
        } else {
            transactions.filter { it.userId == currentViewingUser?.id }
        }

        val filteredTransactions = visibleTransactions.filter {
            (selectedId == null || it.userId == selectedId) &&
            (category == "Alla" || it.category.name == category || it.category.displayName == category)
        }

        val revenue = filteredTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val cost = filteredTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { 
            if (it.category == TransactionCategory.PURCHASE) it.unitCost * it.quantity else it.amount 
        }

        val chartData = filteredTransactions
            .groupBy { it.category.displayName }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        BookkeepingUiState(
            transactions = filteredTransactions,
            totalRevenue = revenue,
            totalCost = cost,
            totalProfit = revenue - cost,
            users = allUsers,
            selectedUserId = selectedId,
            selectedCategory = category,
            customAmount = amountStr,
            customDescription = description,
            exportProgress = progress,
            showExportCompleteOptions = file,
            chartData = chartData
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BookkeepingUiState())

    fun clearExportState() {
        _exportFile.value = null
        _exportProgress.value = null
    }

    fun registerSale(product: Product, quantity: Int, totalReceived: Double, distanceKm: Double = 0.0) {
        val user = _currentUser.value ?: return
        val s = settings.value
        
        viewModelScope.launch {
            val resellerPricePerG = user.productResellerPrices[product.id] ?: product.resellerPrice
            
            var fuelCost = 0.0
            if (distanceKm > 0) {
                val fPrice = user.fuelPrice ?: s.fuelPrice
                val fCons = user.fuelConsumption ?: s.fuelConsumption
                fuelCost = (distanceKm / 10.0) * fCons * fPrice
            }

            var vehicleAdjustment = 0.0
            if (user.role == "Transportör") {
                val bonus = user.vehicleBonusPerUnit ?: s.vehicleBonusPerUnit
                val fee = user.vehicleFeePerUnit ?: s.vehicleFeePerUnit
                
                vehicleAdjustment = when (user.vehicleType) {
                    "Egen bil" -> bonus * quantity
                    "Lånad bil" -> -fee * quantity
                    else -> 0.0
                }
            }
            
            val specificCommission = user.productCommissions[product.id]
            val profitPerUnit = specificCommission ?: (product.salesPrice - resellerPricePerG)
            val sellerProfit = (profitPerUnit * quantity) + vehicleAdjustment - fuelCost
            
            val saleTrans = Transaction(
                userId = user.id,
                productId = product.id,
                amount = totalReceived,
                quantity = quantity,
                timestamp = System.currentTimeMillis(),
                category = TransactionCategory.SALES,
                type = TransactionType.INCOME,
                paymentMethod = "Kontant",
                description = "Försäljning: ${product.name} ($quantity ${product.unit})"
            )
            transactionRepository.insertTransaction(saleTrans)
            
            if (fuelCost > 0) {
                transactionRepository.insertTransaction(
                    Transaction(
                        userId = user.id,
                        productId = -1,
                        amount = fuelCost,
                        timestamp = System.currentTimeMillis(),
                        category = TransactionCategory.OTHER_EXPENSE,
                        type = TransactionType.EXPENSE,
                        paymentMethod = "System",
                        description = "Bränsle för försäljning: ${product.name}"
                    )
                )
            }

            val updatedUser = user.copy(balance = user.balance + sellerProfit)
            userRepository.updateUser(updatedUser)
            _currentUser.value = updatedUser
        }
    }

    fun registerRestock(product: Product, quantity: Int) {
        val user = _currentUser.value ?: return
        
        viewModelScope.launch {
            val updatedProduct = product.copy(quantity = product.quantity - quantity)
            productRepository.updateProduct(updatedProduct)
            
            val resellerPrice = user.productResellerPrices[product.id] ?: product.resellerPrice
            val totalCost = resellerPrice * quantity
            
            val restockTrans = Transaction(
                userId = user.id,
                productId = product.id,
                amount = totalCost,
                quantity = quantity,
                unitCost = resellerPrice,
                timestamp = System.currentTimeMillis(),
                category = TransactionCategory.PURCHASE,
                type = TransactionType.EXPENSE,
                paymentMethod = "Konto",
                description = "Lagerpåfyllning: ${product.name}"
            )
            transactionRepository.insertTransaction(restockTrans)
            
            val updatedUser = user.copy(balance = user.balance - totalCost)
            userRepository.updateUser(updatedUser)
            _currentUser.value = updatedUser
        }
    }

    fun exportToExcel(context: android.content.Context) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _exportProgress.value = 0.1f
            val state = uiState.value
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("Bokföring")

            val headerRow = sheet.createRow(0)
            headerRow.createCell(0).setCellValue("Datum")
            headerRow.createCell(1).setCellValue("Kategori")
            headerRow.createCell(2).setCellValue("Beskrivning")
            headerRow.createCell(3).setCellValue("Typ")
            headerRow.createCell(4).setCellValue("Belopp")

            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            
            state.transactions.forEachIndexed { index, transaction ->
                val row = sheet.createRow(index + 1)
                row.createCell(0).setCellValue(sdf.format(Date(transaction.timestamp)))
                row.createCell(1).setCellValue(transaction.category.displayName)
                row.createCell(2).setCellValue(transaction.description)
                row.createCell(3).setCellValue(if (transaction.type == TransactionType.INCOME) "Intäkt" else "Kostnad")
                row.createCell(4).setCellValue(transaction.amount)
                _exportProgress.value = 0.1f + (0.7f * (index.toFloat() / state.transactions.size.coerceAtLeast(1)))
            }

            val lastRowIndex = state.transactions.size + 2
            val summaryRow1 = sheet.createRow(lastRowIndex)
            summaryRow1.createCell(3).setCellValue("Total Intäkt:")
            summaryRow1.createCell(4).setCellValue(state.totalRevenue)

            val summaryRow2 = sheet.createRow(lastRowIndex + 1)
            summaryRow2.createCell(3).setCellValue("Total Kostnad:")
            summaryRow2.createCell(4).setCellValue(state.totalCost)

            val summaryRow3 = sheet.createRow(lastRowIndex + 2)
            summaryRow3.createCell(3).setCellValue("Resultat:")
            summaryRow3.createCell(4).setCellValue(state.totalProfit)

            try {
                val fileName = "Bokforing_${System.currentTimeMillis()}.xlsx"
                val file = File(context.cacheDir, fileName)
                FileOutputStream(file).use { workbook.write(it) }
                workbook.close()
                _exportProgress.value = 1.0f
                _exportFile.value = file
            } catch (e: Exception) {
                _exportProgress.value = null
            }
        }
    }

    fun exportToPdf(context: android.content.Context) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _exportProgress.value = 0.1f
            val state = uiState.value
            val fileName = "Bokforing_${System.currentTimeMillis()}.pdf"
            val file = File(context.cacheDir, fileName)

            try {
                val writer = PdfWriter(file)
                val pdf = PdfDocument(writer)
                val document = Document(pdf)
                _exportProgress.value = 0.3f

                document.add(Paragraph("Bokföringsrapport").setBold().setFontSize(20f))
                document.add(Paragraph("Genererad: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}"))
                document.add(Paragraph(" "))

                val table = Table(UnitValue.createPointArray(floatArrayOf(100f, 100f, 150f, 80f, 80f)))
                table.addHeaderCell(Cell().add(Paragraph("Datum")))
                table.addHeaderCell(Cell().add(Paragraph("Kategori")))
                table.addHeaderCell(Cell().add(Paragraph("Beskrivning")))
                table.addHeaderCell(Cell().add(Paragraph("Typ")))
                table.addHeaderCell(Cell().add(Paragraph("Belopp")))

                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                state.transactions.forEachIndexed { index, transaction ->
                    table.addCell(Cell().add(Paragraph(sdf.format(Date(transaction.timestamp)))))
                    table.addCell(Cell().add(Paragraph(transaction.category.displayName)))
                    table.addCell(Cell().add(Paragraph(transaction.description)))
                    table.addCell(Cell().add(Paragraph(if (transaction.type == TransactionType.INCOME) "In" else "Ut")))
                    table.addCell(Cell().add(Paragraph(String.format(Locale.getDefault(), "%.2f kr", transaction.amount))))
                    _exportProgress.value = 0.3f + (0.5f * (index.toFloat() / state.transactions.size.coerceAtLeast(1)))
                }
                document.add(table)

                document.add(Paragraph(" "))
                document.add(Paragraph("Sammanställning:").setBold())
                document.add(Paragraph("Total Intäkt: ${String.format(Locale.getDefault(), "%.2f", state.totalRevenue)} kr"))
                document.add(Paragraph("Total Kostnad: ${String.format(Locale.getDefault(), "%.2f", state.totalCost)} kr"))
                document.add(Paragraph("Resultat: ${String.format(Locale.getDefault(), "%.2f", state.totalProfit)} kr").setBold())

                document.close()
                _exportProgress.value = 1.0f
                _exportFile.value = file
            } catch (e: Exception) {
                _exportProgress.value = null
            }
        }
    }

    fun openFile(context: android.content.Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val mimeType = if (file.name.endsWith(".pdf")) "application/pdf" else "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Öppna med"))
    }

    fun shareFile(context: android.content.Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val mimeType = if (file.name.endsWith(".pdf")) "application/pdf" else "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Spara/Dela bokföring"))
    }

    fun syncData() {
        viewModelScope.launch {
            try {
                val app = com.example.kksales.KKSalesApplication.instance
                val status = app.apiService.getServerStatus()
                println("Sync status: ${status["status"]}")
            } catch (e: Exception) {
                println("Sync failed: ${e.message}")
            }
        }
    }

    fun selectUser(userId: Int?) {
        _selectedUserId.value = userId
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun updateCustomAmount(amount: String) {
        _customAmount.value = amount
    }

    fun updateCustomDescription(description: String) {
        _customDescription.value = description
    }

    fun addManualTransaction(category: String, amount: Double, description: String = "", imageUri: String? = null) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val transCategory = TransactionCategory.fromString(category)
            val transaction = Transaction(
                userId = user.id,
                productId = 0,
                amount = amount,
                timestamp = System.currentTimeMillis(),
                category = transCategory,
                type = transCategory.type,
                paymentMethod = "Manual",
                description = description,
                receiptImageUri = imageUri
            )
            transactionRepository.insertTransaction(transaction)
            _customAmount.value = ""
            _customDescription.value = ""
        }
    }

    class Factory(
        private val transactionRepository: TransactionRepository,
        private val userRepository: UserRepository,
        private val productRepository: ProductRepository,
        private val settingsRepository: SettingsRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(BookkeepingViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return BookkeepingViewModel(transactionRepository, userRepository, productRepository, settingsRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
