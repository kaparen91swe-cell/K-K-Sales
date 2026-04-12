package com.example.kksales.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.kksales.KKSalesApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class FuelUpdateWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val app = applicationContext as KKSalesApplication
        val repository = app.settingsRepository

        try {
            val random = Random()
            
            // Simulerade baspriser
            val new95 = 17.50 + random.nextDouble() * 1.0
            val new98 = 18.50 + random.nextDouble() * 1.0
            val newDiesel = 17.80 + random.nextDouble() * 1.5

            val stations = listOf("OKQ8", "Circle K", "Preem", "ST1", "Ingo", "Shell")
            
            val currentSettings = repository.getSettings()
            val updatedSettings = currentSettings.copy(
                fuelPrice95 = Math.round(new95 * 100.0) / 100.0,
                fuelPrice98 = Math.round(new98 * 100.0) / 100.0,
                fuelPriceDiesel = Math.round(newDiesel * 100.0) / 100.0,
                cheapestStation95 = stations.random() + " (Längs rutten)",
                cheapestStation98 = stations.random() + " (Längs rutten)",
                cheapestStationDiesel = stations.random() + " (Längs rutten)",
                lastFuelUpdate = System.currentTimeMillis()
            )

            repository.updateSettings(updatedSettings)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
