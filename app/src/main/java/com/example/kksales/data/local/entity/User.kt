package com.example.kksales.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val password: String? = null,
    val balance: Double, // Säljarens vinst / Personligt saldo
    val cashBalance: Double = 0.0, // Mottagna kontanter (ska redovisas till Admin)
    val isAdmin: Boolean = false,
    val isReseller: Boolean = false, // Om användaren är en säljare
    val isLageransvarig: Boolean = false,
    val isTransportor: Boolean = false,
    val isAdminPlus: Boolean = false,
    val profileIcon: String? = null, // Namn på ikonen/resursen
    val role: String? = null, // t.ex. "Transportör", "Säljare", "Boss"
    val vehicleType: String? = null, // "Egen bil", "Lånad bil", "Ingen bil"
    
    // Användarspecifika inställningar (null = använd globala)
    val fuelPrice: Double? = null,
    val fuelConsumption: Double? = null,
    val vehicleBonusPerUnit: Double? = null,
    val vehicleFeePerUnit: Double? = null,
    
    // Provision per produkt (ProduktID -> Provision i kr/g)
    val productCommissions: Map<Int, Double> = emptyMap(),
    
    // Specifika priser per produkt för denna användare (ProduktID -> Pris)
    val productResellerPrices: Map<Int, Double> = emptyMap()
)
