package com.example.kksales.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.kksales.data.local.dao.*
import com.example.kksales.data.local.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Database(
    entities = [
        Product::class,
        User::class,
        Transaction::class,
        CartItem::class,
        ChatMessage::class,
        ChatGroup::class,
        ChatGroupMember::class,
        UserInventory::class,
        AppSettings::class,
        Task::class
    ],
    version = 22,
    exportSchema = false
)
@androidx.room.TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun userDao(): UserDao
    abstract fun transactionDao(): TransactionDao
    abstract fun cartDao(): CartDao
    abstract fun chatDao(): ChatDao
    abstract fun userInventoryDao(): UserInventoryDao
    abstract fun settingsDao(): AppSettingsDao
    abstract fun taskDao(): TaskDao

    companion object {
        const val DATABASE_NAME = "kksales_db"

        val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Destructive migration will handle this if fallbackToDestructiveMigration is on,
                // but we bump version to force a re-seed.
            }
        }

        val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Ingen ändring behövs i tabeller, vi ökade bara versionen för att tvinga Room att validera om.
                // Eller om du faktiskt ändrat Product, lägg till kolumnen här.
            }
        }

        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Check if columns exist before adding them to avoid "duplicate column" errors
                val cursor = db.query("PRAGMA table_info(transactions)")
                var hasVatAmount = false
                var hasVatRate = false
                val nameIndex = cursor.getColumnIndex("name")
                if (nameIndex != -1) {
                    while (cursor.moveToNext()) {
                        val columnName = cursor.getString(nameIndex)
                        if (columnName == "vatAmount") hasVatAmount = true
                        if (columnName == "vatRate") hasVatRate = true
                    }
                }
                cursor.close()

                if (!hasVatAmount) {
                    db.execSQL("ALTER TABLE transactions ADD COLUMN vatAmount REAL NOT NULL DEFAULT 0.0")
                }
                if (!hasVatRate) {
                    db.execSQL("ALTER TABLE transactions ADD COLUMN vatRate REAL NOT NULL DEFAULT 0.0")
                }
            }
        }

        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Safe migration to match the current schema and fix integrity hash issues
            }
        }

        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE app_settings ADD COLUMN isDeveloperModeEnabled INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN preferredFuelType TEXT")
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `tasks` (
                        `id` INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL, 
                        `assignedToUserId` INTEGER NOT NULL, 
                        `assignedByUserId` INTEGER NOT NULL, 
                        `title` TEXT NOT NULL, 
                        `description` TEXT NOT NULL, 
                        `timestamp` INTEGER NOT NULL, 
                        `isCompleted` INTEGER NOT NULL, 
                        `completedAt` INTEGER
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Migration logic
            }
        }

        fun getCallback(scope: kotlinx.coroutines.CoroutineScope, productDao: () -> ProductDao): RoomDatabase.Callback {
            return object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    scope.launch {
                        seedDatabase(productDao())
                    }
                }
            }
        }

        private suspend fun seedDatabase(productDao: ProductDao) {
            val defaultProducts = listOf(
                Product(
                    name = "❄️DUNDER KOLA❄️ Coke", 
                    unitCost = 400.0, 
                    salesPrice = 800.0, 
                    profitPerUnit = 150.0, 
                    resellerPrice = 650.0, 
                    quantity = 50, 
                    unit = "g", 
                    lowStockThreshold = 10,
                    bulkPrices = listOf(
                        BulkPrice(2, 1500.0),
                        BulkPrice(3, 2100.0),
                        BulkPrice(5, 3000.0),
                        BulkPrice(10, 5500.0),
                        BulkPrice(25, 12500.0)
                    )
                ),
                Product(
                    name = "⚡👞 Tjack 👞⚡ Amphetamine", 
                    unitCost = 30.0, 
                    salesPrice = 140.0, 
                    profitPerUnit = 40.0, 
                    resellerPrice = 100.0, 
                    quantity = 1000, 
                    unit = "g", 
                    lowStockThreshold = 100,
                    bulkPrices = listOf(
                        BulkPrice(5, 700.0),
                        BulkPrice(10, 1100.0),
                        BulkPrice(50, 2500.0),
                        BulkPrice(100, 4500.0),
                        BulkPrice(200, 8000.0),
                        BulkPrice(500, 16000.0)
                    )
                ),
                Product(
                    name = "🍫Hash🍫 Dry Sift (OG Kush)", 
                    unitCost = 50.0,
                    salesPrice = 125.0, 
                    profitPerUnit = 25.0, 
                    resellerPrice = 100.0, 
                    quantity = 500, 
                    unit = "g", 
                    lowStockThreshold = 50,
                    bulkPrices = listOf(
                        BulkPrice(4, 500.0),
                        BulkPrice(25, 2500.0),
                        BulkPrice(50, 4000.0),
                        BulkPrice(100, 7500.0)
                    )
                )
            )
            defaultProducts.forEach { productDao.insertProduct(it) }
        }
    }
}

class Converters {
    @androidx.room.TypeConverter
    fun fromBulkPricesList(value: List<com.example.kksales.data.local.entity.BulkPrice>?): String? {
        val moshi = com.squareup.moshi.Moshi.Builder().build()
        val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, com.example.kksales.data.local.entity.BulkPrice::class.java)
        val adapter = moshi.adapter<List<com.example.kksales.data.local.entity.BulkPrice>>(type)
        return adapter.toJson(value)
    }

    @androidx.room.TypeConverter
    fun toBulkPricesList(value: String?): List<com.example.kksales.data.local.entity.BulkPrice>? {
        if (value == null) return null
        val moshi = com.squareup.moshi.Moshi.Builder().build()
        val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, com.example.kksales.data.local.entity.BulkPrice::class.java)
        val adapter = moshi.adapter<List<com.example.kksales.data.local.entity.BulkPrice>>(type)
        return try {
            adapter.fromJson(value)
        } catch (e: Exception) {
            emptyList()
        }
    }

    @androidx.room.TypeConverter
    fun fromProductCommissions(value: Map<Int, Double>?): String? {
        val moshi = com.squareup.moshi.Moshi.Builder().build()
        val type = com.squareup.moshi.Types.newParameterizedType(Map::class.java, Int::class.javaObjectType, Double::class.javaObjectType)
        val adapter = moshi.adapter<Map<Int, Double>>(type)
        return adapter.toJson(value)
    }

    @androidx.room.TypeConverter
    fun toProductCommissions(value: String?): Map<Int, Double>? {
        if (value == null) return null
        val moshi = com.squareup.moshi.Moshi.Builder().build()
        val type = com.squareup.moshi.Types.newParameterizedType(Map::class.java, Int::class.javaObjectType, Double::class.javaObjectType)
        val adapter = moshi.adapter<Map<Int, Double>>(type)
        return try {
            adapter.fromJson(value)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    @androidx.room.TypeConverter
    fun fromTransactionType(value: com.example.kksales.data.local.entity.TransactionType): String = value.name

    @androidx.room.TypeConverter
    fun toTransactionType(value: String): com.example.kksales.data.local.entity.TransactionType = 
        com.example.kksales.data.local.entity.TransactionType.valueOf(value)

    @androidx.room.TypeConverter
    fun fromTransactionCategory(value: com.example.kksales.data.local.entity.TransactionCategory): String = value.name

    @androidx.room.TypeConverter
    fun toTransactionCategory(value: String): com.example.kksales.data.local.entity.TransactionCategory = 
        com.example.kksales.data.local.entity.TransactionCategory.valueOf(value)
}
