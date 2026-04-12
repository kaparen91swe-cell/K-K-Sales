package com.example.kksales.data.local.dao

import androidx.room.*
import com.example.kksales.data.local.entity.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserById(id: Int): User?

    @Query("SELECT * FROM users WHERE id = :id")
    fun getUserFlowById(id: Int): Flow<User?>

    @Query("SELECT * FROM users WHERE name = :name LIMIT 1")
    suspend fun getUserByName(name: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Update
    suspend fun updateUser(user: User)

    @Delete
    suspend fun deleteUser(user: User)

    @Query("UPDATE users SET balance = 0.0")
    suspend fun resetAllBalances()

    @Query("UPDATE users SET cashBalance = 0.0")
    suspend fun resetAllCashBalances()
}
