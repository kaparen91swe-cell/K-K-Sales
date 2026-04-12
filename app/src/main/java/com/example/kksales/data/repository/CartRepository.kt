package com.example.kksales.data.repository

import com.example.kksales.data.local.dao.CartDao
import com.example.kksales.data.local.entity.CartItem
import kotlinx.coroutines.flow.Flow

class CartRepository(private val cartDao: CartDao) {
    fun getCartItemsByUserId(userId: Int): Flow<List<CartItem>> {
        return cartDao.getCartItemsByUserId(userId)
    }

    suspend fun addToCart(cartItem: CartItem) {
        cartDao.insertCartItem(cartItem)
    }

    suspend fun removeFromCart(cartItem: CartItem) {
        cartDao.deleteCartItem(cartItem)
    }

    suspend fun clearCart(userId: Int) {
        cartDao.clearCartByUserId(userId)
    }
}
