package com.example.kksales.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ListenableWorker.Result
import com.example.kksales.KKSalesApplication
import com.example.kksales.data.local.entity.TransactionCategory
import com.example.kksales.data.remote.api.OrderRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val app = applicationContext as KKSalesApplication
        val transactionRepo = app.transactionRepository
        val apiService = app.apiService

        val unsyncedTransactions = transactionRepo.getUnsyncedTransactions()

        if (unsyncedTransactions.isEmpty()) return@withContext Result.success()

        var allSuccessful = true
        for (transaction in unsyncedTransactions) {
            try {
                if (transaction.category == TransactionCategory.PURCHASE || transaction.category == TransactionCategory.SALES) {
                    val response = apiService.processOrder(
                        OrderRequest(
                            userId = transaction.userId,
                            productId = transaction.productId,
                            quantity = transaction.quantity,
                            paymentMethod = transaction.paymentMethod,
                            totalAmount = transaction.amount,
                            timestamp = transaction.timestamp
                        )
                    )
                    if (response.success) {
                        transactionRepo.updateTransaction(transaction.copy(isSynced = true))
                    } else {
                        allSuccessful = false
                    }
                } else {
                    // Sync other transaction types if needed
                    transactionRepo.updateTransaction(transaction.copy(isSynced = true))
                }
            } catch (e: Exception) {
                allSuccessful = false
            }
        }

        if (allSuccessful) Result.success() else Result.retry()
    }
}
