package com.example.kksales.data.repository

import com.example.kksales.data.local.dao.ProductDao
import com.example.kksales.data.local.dao.TransactionDao
import com.example.kksales.data.local.dao.UserDao
import com.example.kksales.data.local.dao.UserInventoryDao
import com.example.kksales.data.local.entity.Transaction
import com.example.kksales.data.local.entity.TransactionCategory
import com.example.kksales.data.local.entity.TransactionType
import com.example.kksales.data.local.entity.UserInventory
import com.example.kksales.data.local.entity.calculatePrice
import com.example.kksales.data.remote.api.ApiService
import com.example.kksales.data.remote.api.OrderRequest
import java.util.*

class OrderRepository(
    private val apiService: ApiService,
    private val productDao: ProductDao,
    private val userDao: UserDao,
    private val transactionDao: TransactionDao,
    private val userInventoryDao: UserInventoryDao,
    private val settingsRepository: SettingsRepository,
    private val context: android.content.Context
) {
    suspend fun placeOrder(userId: Int, productId: Int, quantity: Int, paymentMethod: String, receiverId: Int? = null): Result<Unit> {
        return try {
            val product = productDao.getProductById(productId) ?: return Result.failure(Exception("Product not found"))
            val user = userDao.getUserById(userId) ?: return Result.failure(Exception("User not found"))
            val settings = settingsRepository.getSettings()

            val totalCost = product.calculatePrice(quantity)

            // Hämta mottagaren om en sådan finns
            val receiver = if (receiverId != null) userDao.getUserById(receiverId) else null

            // LOGIK: Om mottagaren (säljaren/arbetaren) har ett eget lager, ska det dras därifrån.
            // Annars dras det från huvudlagret (Admin).
            
            if (receiver != null && !receiver.isAdmin) {
                // Säljare/Transportör/Lagerarbetare säljer från sitt egna lager
                val userInvItem = userInventoryDao.getUserInventoryItem(receiver.id, productId)
                if (userInvItem == null || userInvItem.quantity < quantity) {
                    return Result.failure(Exception("Säljaren har inte tillräckligt i sitt lager"))
                }
                
                // Minska säljarens lager
                val newQty = userInvItem.quantity - quantity
                if (newQty > 0) {
                    userInventoryDao.update(userInvItem.copy(quantity = newQty))
                } else {
                    userInventoryDao.delete(userInvItem)
                }
            } else {
                // Säljs direkt från huvudlagret (Admin)
                if (product.quantity < quantity) {
                    return Result.failure(Exception("Insufficient stock in main warehouse"))
                }
                val updatedProduct = product.copy(quantity = product.quantity - quantity)
                productDao.updateProduct(updatedProduct)
                
                // Kontrollera om lagret är lågt efter köp
                if (updatedProduct.quantity <= updatedProduct.lowStockThreshold) {
                    sendLowStockWarning(updatedProduct.name, updatedProduct.quantity)
                }
            }

            // Hantera betalning
            if (paymentMethod.contains("Konto", ignoreCase = true)) {
                // Dra från köparens saldo
                val updatedUser = user.copy(balance = user.balance - totalCost)
                userDao.updateUser(updatedUser)
            } else if (paymentMethod.contains("Kontant", ignoreCase = true) && receiver != null) {
                // Om kontant, lägg till i mottagarens cashBalance (måste redovisas till Admin senare)
                val updatedReceiver = receiver.copy(cashBalance = receiver.cashBalance + totalCost)
                userDao.updateUser(updatedReceiver)
            }

            // Hantera säljarvinst (om mottagaren är en säljare/reseller)
            // Rollen "Lagerarbetare" (eller liknande) tjänar inget, så vi kollar isReseller
            if (receiver != null && receiver.isReseller) {
                // Provision: om säljaren har en specifik provision för produkten (kr/g), använd den.
                // Annars använd standardskillnaden mellan försäljningspris och inköpspris.
                val specificCommission = receiver.productCommissions[productId]
                val profitPerUnit = specificCommission ?: (product.salesPrice - product.resellerPrice)
                
                var totalProfit = profitPerUnit * quantity
                
                // Fordonsavgift logik för Transportörer som säljer
                if (receiver.role == "Transportör") {
                    val fBonus = receiver.vehicleBonusPerUnit ?: settings.vehicleBonusPerUnit
                    val fFee = receiver.vehicleFeePerUnit ?: settings.vehicleFeePerUnit
                    
                    when (receiver.vehicleType) {
                        "Egen bil" -> {
                            // Transportören har egen bil -> Säljaren tjänar lite extra (betalas av Admin)
                            totalProfit += fBonus * quantity
                        }
                        "Lånad bil" -> {
                            // Transportören lånar bil -> Säljaren betalar en avgift för bilen
                            totalProfit -= fFee * quantity
                        }
                    }
                }

                if (totalProfit != 0.0) {
                    // Vi måste hämta mottagaren igen ifall cashBalance uppdaterades ovan
                    val currentReceiver = userDao.getUserById(receiver.id)!!
                    val finalReceiver = currentReceiver.copy(balance = currentReceiver.balance + totalProfit)
                    userDao.updateUser(finalReceiver)
                }
            }

            // LOGIK: Om en säljare KÖPER av Admin (för att fylla sitt lager)
            // Detta triggas om köparen (user) är en Reseller och mottagaren är Admin
            if (user.isReseller && (receiver == null || receiver.isAdmin)) {
                // Lägg till i köparens lager
                val existingInv = userInventoryDao.getUserInventoryItem(user.id, productId)
                if (existingInv != null) {
                    userInventoryDao.update(existingInv.copy(quantity = existingInv.quantity + quantity))
                } else {
                    userInventoryDao.insertOrUpdate(UserInventory(userId = user.id, productId = productId, quantity = quantity))
                }
                // (Lagersaldot i huvudlagret drogs redan ovan i "else" blocket för receiver != null)
            }

            val transaction = Transaction(
                userId = userId,
                productId = productId,
                amount = totalCost,
                vatAmount = totalCost * 0.25 / 1.25, // Beräkna 25% moms
                vatRate = 0.25,
                quantity = quantity,
                unitCost = product.unitCost,
                timestamp = System.currentTimeMillis(),
                category = TransactionCategory.SALES,
                type = TransactionType.INCOME,
                paymentMethod = paymentMethod,
                receiverId = receiverId,
                description = "Försäljning av ${product.name} ($quantity $product.unit)"
            )
            transactionDao.insertTransaction(transaction)

            // Försök anropa API, men låt det inte stoppa den lokala transaktionen om det misslyckas
            try {
                apiService.processOrder(
                    OrderRequest(
                        userId = userId,
                        productId = productId,
                        quantity = quantity,
                        paymentMethod = paymentMethod,
                        totalAmount = totalCost,
                        timestamp = transaction.timestamp
                    )
                )
            } catch (e: Exception) {
                // Logga felet men markera köpet som lyckat lokalt
                android.util.Log.e("OrderRepository", "API Sync failed: ${e.message}")
            }

            sendAdminNotification(user.name, product.name, quantity, totalCost, paymentMethod)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun sendAdminNotification(userName: String, productName: String, quantity: Int, totalAmount: Double, method: String) {
        val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        
        // Skapa kanal om den inte finns
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel("ADMIN_NOTIFICATIONS", "Admin Notiser", android.app.NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val intent = android.content.Intent(context, com.example.kksales.MainActivity::class.java)
        val pendingIntent = android.app.PendingIntent.getActivity(context, 0, intent, android.app.PendingIntent.FLAG_IMMUTABLE)

        val notification = androidx.core.app.NotificationCompat.Builder(context, "ADMIN_NOTIFICATIONS")
            .setSmallIcon(com.example.kksales.R.drawable.ic_launcher_foreground)
            .setContentTitle("Ny beställning!")
            .setContentText("$userName köpte $quantity g $productName för ${String.format("%.2f", totalAmount)} kr ($method)")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun sendLowStockWarning(productName: String, quantity: Int) {
        val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        
        val kilos = quantity / 1000
        val remainingAfterKilos = quantity % 1000
        val hectos = remainingAfterKilos / 100
        val grams = remainingAfterKilos % 100
        
        val parts = mutableListOf<String>()
        if (kilos > 0) parts.add("${kilos}kg")
        if (hectos > 0) parts.add("${hectos}hg")
        if (grams > 0 || parts.isEmpty()) parts.add("${grams}g")
        val formattedQty = parts.joinToString(" ")

        val intent = android.content.Intent(context, com.example.kksales.MainActivity::class.java)
        val pendingIntent = android.app.PendingIntent.getActivity(context, 0, intent, android.app.PendingIntent.FLAG_IMMUTABLE)

        val notification = androidx.core.app.NotificationCompat.Builder(context, "ADMIN_NOTIFICATIONS")
            .setSmallIcon(com.example.kksales.R.drawable.ic_launcher_foreground)
            .setContentTitle("VARNING: Lågt lager!")
            .setContentText("Lagret för $productName börjar ta slut: $formattedQty kvar.")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setDefaults(androidx.core.app.NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(productName.hashCode() + 1000, notification)
    }
}
