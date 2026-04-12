package com.example.kksales.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

import com.squareup.moshi.JsonClass

enum class TransactionType {
    INCOME,   // Intäkt
    EXPENSE   // Kostnad
}

enum class TransactionCategory(val displayName: String, val type: TransactionType) {
    SALES("Försäljning", TransactionType.INCOME),
    PURCHASE("Inköp", TransactionType.EXPENSE),
    TRANSPORT("Transport", TransactionType.INCOME),
    STORAGE("Lagerhållning", TransactionType.INCOME),
    DEPOSIT("Insättning", TransactionType.INCOME),
    WITHDRAWAL("Uttag", TransactionType.EXPENSE),
    OTHER_INCOME("Övrig intäkt", TransactionType.INCOME),
    OTHER_EXPENSE("Övrig kostnad", TransactionType.EXPENSE);

    companion object {
        fun fromString(value: String): TransactionCategory {
            return values().find { it.name.equals(value, ignoreCase = true) || it.displayName.equals(value, ignoreCase = true) } 
                ?: OTHER_INCOME
        }
    }
}

@JsonClass(generateAdapter = true)
@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Int,
    val productId: Int,
    val amount: Double, // Totalbelopp
    val vatAmount: Double = 0.0,
    val vatRate: Double = 0.0,
    val quantity: Int = 0,
    val unitCost: Double = 0.0,
    val timestamp: Long,
    val category: TransactionCategory,
    val type: TransactionType,
    val paymentMethod: String = "",
    val receiptImageUri: String? = null,
    val description: String = "",
    val isSynced: Boolean = false,
    val receiverId: Int? = null
)
