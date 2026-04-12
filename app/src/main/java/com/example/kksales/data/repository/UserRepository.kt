package com.example.kksales.data.repository

import com.example.kksales.data.local.dao.UserDao
import com.example.kksales.data.local.entity.User
import com.example.kksales.data.remote.api.ApiService
import kotlinx.coroutines.flow.Flow

class UserRepository(
    private val userDao: UserDao,
    private val apiService: ApiService
) {
    val allUsers: Flow<List<User>> = userDao.getAllUsers()

    suspend fun getUserById(id: Int): User? {
        return userDao.getUserById(id)
    }

    fun getUserFlowById(id: Int): Flow<User?> {
        return userDao.getUserFlowById(id)
    }

    suspend fun getUserByName(name: String): User? {
        return userDao.getUserByName(name)
    }

    suspend fun refreshUsers() {
        try {
            val remoteUsers = apiService.getAllUsers()
            remoteUsers.forEach { userDao.insertUser(it) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun insertUser(user: User) {
        userDao.insertUser(user)
        try {
            apiService.registerUser(user)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun updateUser(user: User) {
        userDao.updateUser(user)
        try {
            apiService.updateUser(user.id, user)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteUser(user: User) {
        userDao.deleteUser(user)
    }

    suspend fun resetAllBalances() {
        userDao.resetAllBalances()
    }

    suspend fun resetAllCashBalances() {
        userDao.resetAllCashBalances()
    }
}
