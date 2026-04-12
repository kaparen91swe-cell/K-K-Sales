package com.example.kksales.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Int = 1,
    val fuelPrice95: Double = 18.0,
    val fuelPrice98: Double = 19.0,
    val fuelPriceDiesel: Double = 18.5,
    val cheapestStation95: String = "Okänd",
    val cheapestStation98: String = "Okänd",
    val cheapestStationDiesel: String = "Okänd",
    val lastFuelUpdate: Long = 0,
    val selectedFuelType: String = "95", // "95", "98", "Diesel"
    val fuelConsumption: Double = 0.7,
    val vehicleBonusPerUnit: Double = 20.0,
    val vehicleFeePerUnit: Double = 15.0,
    val isDeveloperModeEnabled: Boolean = false
) {
    val fuelPrice: Double
        get() = when (selectedFuelType) {
            "98" -> fuelPrice98
            "Diesel" -> fuelPriceDiesel
            else -> fuelPrice95
        }
}
