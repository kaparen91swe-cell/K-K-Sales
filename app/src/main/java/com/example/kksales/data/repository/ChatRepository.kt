package com.example.kksales.data.repository

import com.example.kksales.data.local.dao.ChatDao
import com.example.kksales.data.local.entity.ChatGroup
import com.example.kksales.data.local.entity.ChatGroupMember
import com.example.kksales.data.local.entity.ChatMessage
import com.example.kksales.util.CryptoManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ChatRepository(
    private val chatDao: com.example.kksales.data.local.dao.ChatDao,
    private val apiService: com.example.kksales.data.remote.api.ApiService,
    private val cryptoManager: com.example.kksales.util.CryptoManager
) {
    fun getPrivateMessages(userId: Int, otherUserId: Int): Flow<List<ChatMessage>> {
        return chatDao.getPrivateMessages(userId, otherUserId).map { messages ->
            messages.map { it.copy(content = decryptSafe(it.content)) }
        }
    }

    fun getGroupMessages(groupId: Int): Flow<List<ChatMessage>> {
        return chatDao.getGroupMessages(groupId).map { messages ->
            messages.map { it.copy(content = decryptSafe(it.content)) }
        }
    }

    suspend fun sendMessage(senderId: Int, receiverId: Int?, groupId: Int?, content: String, isSystem: Boolean = false) {
        val encryptedContent = cryptoManager.encrypt(content)
        val message = ChatMessage(
            senderId = senderId,
            receiverId = receiverId,
            groupId = groupId,
            content = encryptedContent,
            timestamp = System.currentTimeMillis(),
            isSystemMessage = isSystem
        )
        chatDao.insertMessage(message)
    }

    suspend fun ensureMandatoryGroup(allUserIds: List<Int>) {
        var group = chatDao.getMandatoryGroup()
        if (group == null) {
            val groupId = chatDao.insertGroup(ChatGroup(name = "Information (Alla)", isMandatory = true))
            group = ChatGroup(id = groupId.toInt(), name = "Information (Alla)", isMandatory = true)
        }
        
        allUserIds.forEach { userId ->
            chatDao.addMemberToGroup(ChatGroupMember(group.id, userId))
        }
    }

    suspend fun createGroup(name: String, creatorId: Int, memberIds: List<Int>) {
        val groupId = chatDao.insertGroup(ChatGroup(name = name, createdBy = creatorId))
        // Lägg till skaparen också om den inte redan är i listan
        val finalMemberIds = if (memberIds.contains(creatorId)) memberIds else memberIds + creatorId
        finalMemberIds.forEach { userId ->
            chatDao.addMemberToGroup(ChatGroupMember(groupId.toInt(), userId))
        }
    }

    suspend fun deleteGroup(groupId: Int) {
        chatDao.deleteGroupMembers(groupId)
        // Man kan även välja att ta bort meddelanden här om man vill rensa helt:
        // chatDao.deleteGroupMessages(groupId)
        chatDao.deleteGroup(groupId)
    }

    fun getAllUsersFromDao(): Flow<List<com.example.kksales.data.local.entity.User>> = chatDao.getAllUsers()

    fun getGroupsForUser(userId: Int): Flow<List<ChatGroup>> = chatDao.getGroupsForUser(userId)

    fun getMessagesForUser(currentUserId: Int, otherUserId: Int) = getPrivateMessages(currentUserId, otherUserId)

    private fun decryptSafe(content: String): String {
        return try {
            cryptoManager.decrypt(content)
        } catch (e: Exception) {
            "[Krypterat meddelande]"
        }
    }
}
