package com.example.kksales.data.local.dao

import androidx.room.*
import com.example.kksales.data.local.entity.ChatGroup
import com.example.kksales.data.local.entity.ChatGroupMember
import com.example.kksales.data.local.entity.ChatMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages WHERE (senderId = :userId AND receiverId = :otherUserId) OR (senderId = :otherUserId AND receiverId = :userId) ORDER BY timestamp ASC")
    fun getPrivateMessages(userId: Int, otherUserId: Int): Flow<List<ChatMessage>>

    @Query("SELECT * FROM chat_messages WHERE groupId = :groupId ORDER BY timestamp ASC")
    fun getGroupMessages(groupId: Int): Flow<List<ChatMessage>>

    @Insert
    suspend fun insertMessage(message: ChatMessage)

    @Query("SELECT * FROM chat_groups WHERE isMandatory = 1 LIMIT 1")
    suspend fun getMandatoryGroup(): ChatGroup?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGroup(group: ChatGroup): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addMemberToGroup(member: ChatGroupMember)

    @Query("""
        SELECT chat_groups.* FROM chat_groups 
        JOIN chat_group_members ON chat_groups.id = chat_group_members.groupId 
        WHERE chat_group_members.userId = :userId OR chat_groups.isMandatory = 1
    """)
    fun getGroupsForUser(userId: Int): Flow<List<ChatGroup>>

    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<com.example.kksales.data.local.entity.User>>

    @Query("DELETE FROM chat_groups WHERE id = :groupId")
    suspend fun deleteGroup(groupId: Int)

    @Query("DELETE FROM chat_group_members WHERE groupId = :groupId")
    suspend fun deleteGroupMembers(groupId: Int)
}
