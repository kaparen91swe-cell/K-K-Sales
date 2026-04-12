package com.example.kksales.data.local.dao

import androidx.room.*
import com.example.kksales.data.local.entity.UserInventory
import kotlinx.coroutines.flow.Flow

@Dao
interface UserInventoryDao {
    @Query("SELECT * FROM user_inventory WHERE userId = :userId")
    fun getUserInventory(userId: Int): Flow<List<UserInventory>>

    @Query("SELECT * FROM user_inventory WHERE userId = :userId AND productId = :productId")
    suspend fun getUserInventoryItem(userId: Int, productId: Int): UserInventory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(item: UserInventory)

    @Update
    suspend fun update(item: UserInventory)

    @Delete
    suspend fun delete(item: UserInventory)

    @Query("DELETE FROM user_inventory WHERE userId = :userId AND productId = :productId")
    suspend fun deleteItem(userId: Int, productId: Int)
}
