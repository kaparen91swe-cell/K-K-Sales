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
    version = 16,
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
                Product(name = "Standard Produkt 1", salesPrice = 100.0, resellerPrice = 80.0, quantity = 50, unit = "st", unitCost = 50.0),
                Product(name = "Standard Produkt 2", salesPrice = 200.0, resellerPrice = 160.0, quantity = 30, unit = "st", unitCost = 100.0)
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
