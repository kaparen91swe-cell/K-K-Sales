package com.example.kksales.data.repository

import com.example.kksales.data.local.dao.ProductDao
import com.example.kksales.data.local.entity.Product
import kotlinx.coroutines.flow.Flow

class ProductRepository(
    private val productDao: com.example.kksales.data.local.dao.ProductDao,
    private val apiService: com.example.kksales.data.remote.api.ApiService
) {
    val allProducts: Flow<List<Product>> = productDao.getAllProducts()

    suspend fun refreshProducts() {
        try {
            val remoteProducts = apiService.getAllProducts()
            remoteProducts.forEach { productDao.insertProduct(it) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getProductById(id: Int): Product? {
        return productDao.getProductById(id)
    }

    suspend fun insertProduct(product: Product): Long {
        val id = productDao.insertProduct(product)
        try {
            apiService.syncProduct(product.copy(id = id.toInt()))
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return id
    }

    suspend fun updateProduct(product: Product) {
        productDao.updateProduct(product)
        try {
            apiService.syncProduct(product)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteProduct(product: Product) {
        productDao.deleteProduct(product)
    }
}
