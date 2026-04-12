package com.example.kksales.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val senderId: Int,
    val receiverId: Int? = null, // Null if it's a group message
    val groupId: Int? = null,    // Null if it's a private message
    val content: String,         // Encrypted text
    val timestamp: Long,
    val isSystemMessage: Boolean = false,
    val isSynced: Boolean = false
)

@Entity(tableName = "chat_groups")
data class ChatGroup(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val isMandatory: Boolean = false, // If true, all users are members
    val createdBy: Int? = null        // User ID of the creator
)

@Entity(tableName = "chat_group_members", primaryKeys = ["groupId", "userId"])
data class ChatGroupMember(
    val groupId: Int,
    val userId: Int
)
