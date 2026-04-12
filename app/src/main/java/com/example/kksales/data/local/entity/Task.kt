package com.example.kksales.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val assignedToUserId: Int,
    val assignedByUserId: Int,
    val title: String,
    val description: String = "",
    val address: String? = null,
    val distanceKm: Double? = null,
    val productId: Int? = null,
    val quantity: Int? = null,
    val unit: String? = null,
    val fuelTypeAtCreation: String? = null,
    val fuelPriceAtCreation: Double? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false,
    val completedAt: Long? = null
)
