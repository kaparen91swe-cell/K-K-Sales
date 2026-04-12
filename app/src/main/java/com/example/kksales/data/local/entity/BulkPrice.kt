package com.example.kksales.data.local.entity

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BulkPrice(
    val quantity: Int,
    val price: Double
)
