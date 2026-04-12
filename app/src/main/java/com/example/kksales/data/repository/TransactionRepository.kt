package com.example.kksales.data.repository

import com.example.kksales.data.local.dao.TransactionDao
import com.example.kksales.data.local.entity.Transaction
import kotlinx.coroutines.flow.Flow

class TransactionRepository(
    private val transactionDao: com.example.kksales.data.local.dao.TransactionDao,
    private val apiService: com.example.kksales.data.remote.api.ApiService
) {
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()

    suspend fun refreshTransactions() {
        try {
            val remoteTransactions = apiService.getAllTransactions()
            remoteTransactions.forEach { transactionDao.insertTransaction(it) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getTransactionsByUserId(userId: Int): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByUserId(userId)
    }

    suspend fun getUnsyncedTransactions(): List<Transaction> {
        return transactionDao.getUnsyncedTransactions()
    }

    suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun insertTransaction(transaction: Transaction) {
        transactionDao.insertTransaction(transaction)
        try {
            apiService.syncTransaction(transaction)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteAllTransactions() {
        transactionDao.deleteAllTransactions()
    }
}
