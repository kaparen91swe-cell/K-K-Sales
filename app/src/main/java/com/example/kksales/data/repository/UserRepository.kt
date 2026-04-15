package com.example.kksales.data.repository

import com.example.kksales.data.local.dao.UserDao
import com.example.kksales.data.local.entity.User
import com.example.kksales.data.remote.api.ApiService
import kotlinx.coroutines.flow.Flow

class UserRepository(
    private val userDao: UserDao,
    val apiService: ApiService
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
            val remoteIds = remoteUsers.map { it.id }
            
            // 1. Hämta alla lokala användare
            val localUsers = userDao.getAllUsersOnce() // Vi behöver en version som inte är Flow
            
            // 2. Ta bort de som inte finns på servern längre (utom de med ID 0/temp)
            localUsers.forEach { local ->
                if (local.id != 0 && local.id !in remoteIds && local.name != "Kaparen") {
                    userDao.deleteUser(local)
                }
            }

            // 3. Spara/Uppdatera de från servern
            remoteUsers.forEach { userDao.insertUser(it) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun insertUser(user: User) {
        try {
            // 1. Registrera först på servern för att få det officiella ID:t
            val registeredUser = apiService.registerUser(user)
            // 2. Spara i lokala DB med serverns ID
            userDao.insertUser(registeredUser)
        } catch (e: Exception) {
            // Om servern är nere, spara lokalt ändå (Room ger ett temp-ID)
            userDao.insertUser(user)
            e.printStackTrace()
        }
    }

    suspend fun updateUser(user: User) {
        userDao.updateUser(user)
        try {
            val response = apiService.updateUser(user.id, user)
        } catch (e: retrofit2.HttpException) {
            if (e.code() == 404) {
                // Användaren finns inte på servern, ta bort den lokalt också
                userDao.deleteUser(user)
            }
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteUser(user: User) {
        try {
            apiService.deleteUser(user.id)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        userDao.deleteUser(user)
    }

    suspend fun resetAllBalances() {
        userDao.resetAllBalances()
    }

    suspend fun resetAllCashBalances() {
        userDao.resetAllCashBalances()
    }

    suspend fun triggerDeploy(note: String, changes: Map<String, Any>): com.example.kksales.data.remote.api.DeployResponse {
        return apiService.triggerDeploy(com.example.kksales.data.remote.api.DeployRequest(note, changes))
    }
}
