package com.example.kksales

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.room.Room
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.kksales.data.local.db.AppDatabase
import com.example.kksales.data.preferences.UserPreferencesManager
import com.example.kksales.data.remote.api.ApiService
import com.example.kksales.data.repository.*
import com.example.kksales.data.sync.SyncWorker
import com.example.kksales.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class KKSalesApplication : Application() {

    companion object {
        lateinit var instance: KKSalesApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
        setupSyncWorker()
        setupFuelUpdateWorker()
    }

    private fun setupFuelUpdateWorker() {
        val fuelRequest = PeriodicWorkRequestBuilder<com.example.kksales.data.worker.FuelUpdateWorker>(1, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "FuelUpdateWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            fuelRequest
        )
    }

    private fun createNotificationChannel() {
        val name = "Admin Notifikationer"
        val descriptionText = "Notifikationer för nya beställningar"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel("ADMIN_NOTIFICATIONS", name, importance).apply {
            description = descriptionText
        }
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.createNotificationChannel(channel)
    }

    private fun setupSyncWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "SyncWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    private val database: AppDatabase by lazy {
        Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
        .addMigrations(AppDatabase.MIGRATION_12_13, AppDatabase.MIGRATION_15_16, AppDatabase.MIGRATION_16_17, AppDatabase.MIGRATION_17_18, AppDatabase.MIGRATION_19_20, AppDatabase.MIGRATION_20_21)
        .addCallback(AppDatabase.getCallback(kotlinx.coroutines.MainScope()) { database.productDao() })
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()
    }

    val productRepository: ProductRepository by lazy {
        ProductRepository(database.productDao(), apiService)
    }

    val userRepository: UserRepository by lazy {
        UserRepository(database.userDao(), apiService)
    }

    val transactionRepository: TransactionRepository by lazy {
        TransactionRepository(database.transactionDao(), apiService)
    }

    val userPreferencesManager: UserPreferencesManager by lazy {
        UserPreferencesManager(this)
    }

    val cryptoManager: com.example.kksales.util.CryptoManager by lazy {
        com.example.kksales.util.CryptoManager(this)
    }

    val apiService: ApiService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("bypass-tunnel-reminder", "true")
                    .addHeader("ngrok-skip-browser-warning", "true")
                    .build()
                chain.proceed(request)
            }
            .build()

        Retrofit.Builder()
            .baseUrl("https://kksales-permanent.loca.lt/")
            .addConverterFactory(MoshiConverterFactory.create())
            .client(client)
            .build()
            .create(ApiService::class.java)
    }

    val cartRepository: CartRepository by lazy {
        CartRepository(database.cartDao())
    }

    val orderRepository: OrderRepository by lazy {
        OrderRepository(
            apiService,
            database.productDao(),
            database.userDao(),
            database.transactionDao(),
            database.userInventoryDao(),
            settingsRepository,
            this
        )
    }

    val userInventoryRepository: UserInventoryRepository by lazy {
        UserInventoryRepository(database.userInventoryDao())
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(database.settingsDao())
    }

    val updateRepository: UpdateRepository by lazy {
        UpdateRepository(apiService, this)
    }

    val chatRepository: ChatRepository by lazy {
        ChatRepository(database.chatDao(), apiService, cryptoManager)
    }

    val taskRepository: TaskRepository by lazy {
        TaskRepository(database.taskDao())
    }
}
