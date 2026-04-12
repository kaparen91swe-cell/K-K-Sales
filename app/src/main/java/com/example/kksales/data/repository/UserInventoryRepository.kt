package com.example.kksales.data.repository

import com.example.kksales.data.local.dao.UserInventoryDao
import com.example.kksales.data.local.entity.UserInventory
import kotlinx.coroutines.flow.Flow

class UserInventoryRepository(private val userInventoryDao: UserInventoryDao) {
    fun getUserInventory(userId: Int): Flow<List<UserInventory>> = 
        userInventoryDao.getUserInventory(userId)

    suspend fun addProductToInventory(userId: Int, productId: Int, quantity: Int) {
        val existing = userInventoryDao.getUserInventoryItem(userId, productId)
        if (existing != null) {
            userInventoryDao.update(existing.copy(quantity = existing.quantity + quantity))
        } else {
            userInventoryDao.insertOrUpdate(UserInventory(userId = userId, productId = productId, quantity = quantity))
        }
    }

    suspend fun removeProductFromInventory(userId: Int, productId: Int, quantity: Int): Boolean {
        val existing = userInventoryDao.getUserInventoryItem(userId, productId)
        return if (existing != null && existing.quantity >= quantity) {
            val newQuantity = existing.quantity - quantity
            if (newQuantity > 0) {
                userInventoryDao.update(existing.copy(quantity = newQuantity))
            } else {
                userInventoryDao.delete(existing)
            }
            true
        } else {
            false
        }
    }
}
