package com.example.kksales.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.kksales.data.local.entity.Product
import com.example.kksales.data.local.entity.Transaction
import com.example.kksales.data.repository.ProductRepository
import com.example.kksales.data.repository.TransactionRepository
import com.example.kksales.data.local.entity.BulkPrice
import com.example.kksales.data.local.entity.TransactionCategory
import com.example.kksales.data.local.entity.TransactionType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AdminViewModel(
    private val productRepository: ProductRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    val products: StateFlow<List<Product>> = productRepository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addProduct(name: String, unitCost: Double, salesPrice: Double, quantity: Int, unit: String, imageUri: String?, bulkPrices: List<BulkPrice>, lowStockThreshold: Int) {
        viewModelScope.launch {
            val productId = productRepository.insertProduct(
                Product(
                    name = name,
                    unitCost = unitCost,
                    salesPrice = salesPrice,
                    quantity = quantity,
                    unit = unit,
                    imageUri = imageUri,
                    bulkPrices = bulkPrices,
                    lowStockThreshold = lowStockThreshold
                )
            ).toInt()

            // Skapa automatisk inköps-transaktion
            val totalCost = unitCost * quantity
            if (totalCost > 0) {
                transactionRepository.insertTransaction(
                    Transaction(
                        userId = 0, // Admin/System
                        productId = productId,
                        amount = 0.0, // Inga intäkter
                        quantity = quantity,
                        unitCost = unitCost,
                        timestamp = System.currentTimeMillis(),
                        category = TransactionCategory.PURCHASE,
                        type = TransactionType.EXPENSE,
                        paymentMethod = "System",
                        description = "Automatiskt inköp vid tillägg av $name"
                    )
                )
            }
        }
    }

    fun updateProduct(product: Product, quantityDiff: Int = 0) {
        viewModelScope.launch {
            productRepository.updateProduct(product)
            
            // Om man har lagt till fler i lager, skapa en ny inköps-transaktion
            if (quantityDiff > 0) {
                transactionRepository.insertTransaction(
                    Transaction(
                        userId = 0,
                        productId = product.id,
                        amount = 0.0,
                        quantity = quantityDiff,
                        unitCost = product.unitCost,
                        timestamp = System.currentTimeMillis(),
                        category = TransactionCategory.PURCHASE,
                        type = TransactionType.EXPENSE,
                        paymentMethod = "System",
                        description = "PURCHASE (Påfyllning): ${product.name}"
                    )
                )
            }
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            productRepository.deleteProduct(product)
        }
    }

    class Factory(
        private val productRepository: ProductRepository,
        private val transactionRepository: TransactionRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AdminViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AdminViewModel(productRepository, transactionRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
