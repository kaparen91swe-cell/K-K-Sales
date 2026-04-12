package com.example.kksales.data.remote.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Url

import retrofit2.http.*

interface ApiService {
    // Users
    @GET("users")
    suspend fun getAllUsers(): List<com.example.kksales.data.local.entity.User>

    @POST("users/register")
    suspend fun registerUser(@Body user: com.example.kksales.data.local.entity.User): com.example.kksales.data.local.entity.User

    @PUT("users/{id}")
    suspend fun updateUser(@Path("id") id: Int, @Body user: com.example.kksales.data.local.entity.User)

    // Products
    @GET("products")
    suspend fun getAllProducts(): List<com.example.kksales.data.local.entity.Product>

    @POST("products")
    suspend fun syncProduct(@Body product: com.example.kksales.data.local.entity.Product)

    // Transactions
    @GET("transactions")
    suspend fun getAllTransactions(): List<com.example.kksales.data.local.entity.Transaction>

    @POST("transactions")
    suspend fun syncTransaction(@Body transaction: com.example.kksales.data.local.entity.Transaction)

    @POST("orders/process")
    suspend fun processOrder(@Body orderRequest: OrderRequest): OrderResponse

    @GET("status")
    suspend fun getServerStatus(): Map<String, String>

    @GET
    suspend fun checkUpdate(@Url url: String): UpdateInfo
}

@JsonClass(generateAdapter = true)
data class UpdateInfo(
    val versionCode: Int,
    val apkUrl: String
)

@JsonClass(generateAdapter = true)
data class OrderRequest(
    @Json(name = "user_id") val userId: Int,
    @Json(name = "product_id") val productId: Int,
    @Json(name = "quantity") val quantity: Int,
    @Json(name = "payment_method") val paymentMethod: String,
    @Json(name = "total_amount") val totalAmount: Double,
    @Json(name = "timestamp") val timestamp: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class OrderResponse(
    val success: Boolean,
    val message: String,
    val transactionId: Int? = null
)