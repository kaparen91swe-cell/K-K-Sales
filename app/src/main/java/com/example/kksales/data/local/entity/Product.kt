package com.example.kksales.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val unitCost: Double,
    val salesPrice: Double,
    val profitPerUnit: Double = 0.0, // Den vinst firman ska ha per enhet
    val resellerPrice: Double = 0.0, // Pris som säljaren betalar till Admin (beräknas nu)
    val quantity: Int, // Sparas alltid i Gram (g)
    val unit: String = "g",
    val imageUri: String? = null,
    val bulkPrices: List<BulkPrice> = emptyList(),
    val lowStockThreshold: Int = 500 // Standard: varna vid 500g
)

fun Product.formatQuantity(): String {
    if (unit != "g") return "$quantity $unit"
    
    val kilos = quantity / 1000
    val remainingAfterKilos = quantity % 1000
    val hectos = remainingAfterKilos / 100
    val grams = remainingAfterKilos % 100
    
    val parts = mutableListOf<String>()
    if (kilos > 0) parts.add("${kilos}kg")
    if (hectos > 0) parts.add("${hectos}hg")
    if (grams > 0 || parts.isEmpty()) parts.add("${grams}g")
    
    return parts.joinToString(" ")
}

fun Product.calculatePrice(quantity: Int, basePrice: Double? = null): Double {
    val priceToUse = basePrice ?: salesPrice
    
    if (bulkPrices.isEmpty()) {
        return priceToUse * quantity
    }
    
    var currentQuantity = quantity
    var currentTotal = 0.0
    
    val sortedBulkPrices = bulkPrices.sortedByDescending { it.quantity }
    
    // If using a custom basePrice (reseller), we might want to scale bulk prices too, 
    // but for now let's assume bulk prices are for customers and resellers pay unit price 
    // UNLESS we are calculating for a customer.
    // However, if basePrice is provided and differs from salesPrice, 
    // it's likely a reseller calculation. 
    
    if (basePrice != null && basePrice != salesPrice) {
        // Reseller unit price calculation (usually no bulk discount on top of reseller price)
        return basePrice * quantity
    }

    for (bp in sortedBulkPrices) {
        if (currentQuantity >= bp.quantity) {
            val bundles = currentQuantity / bp.quantity
            currentTotal += bundles * bp.price
            currentQuantity %= bp.quantity
        }
    }
    
    return currentTotal + (currentQuantity * priceToUse)
}
