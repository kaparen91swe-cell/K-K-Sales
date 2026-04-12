package com.example.kksales.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.kksales.data.local.entity.ChatGroup
import com.example.kksales.data.local.entity.ChatMessage
import com.example.kksales.data.repository.ChatRepository
import com.example.kksales.data.repository.UserRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatViewModel(
    val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    val currentUserId: Int
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    val allUsers = userRepository.allUsers.stateIn(
        viewModelScope, 
        SharingStarted.WhileSubscribed(5000), 
        emptyList()
    )

    val userGroups: StateFlow<List<ChatGroup>> = chatRepository.getGroupsForUser(currentUserId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun loadPrivateMessages(otherUserId: Int) {
        viewModelScope.launch {
            chatRepository.getPrivateMessages(currentUserId, otherUserId).collect {
                _messages.value = it
            }
        }
    }

    fun loadGroupMessages(groupId: Int) {
        viewModelScope.launch {
            chatRepository.getGroupMessages(groupId).collect {
                _messages.value = it
            }
        }
    }

    fun sendMessage(receiverId: Int?, groupId: Int?, content: String) {
        viewModelScope.launch {
            chatRepository.sendMessage(currentUserId, receiverId, groupId, content)
        }
    }

    fun deleteGroup(groupId: Int) {
        viewModelScope.launch {
            chatRepository.deleteGroup(groupId)
        }
    }

    class Factory(
        private val chatRepository: ChatRepository,
        private val userRepository: UserRepository,
        private val currentUserId: Int
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ChatViewModel(chatRepository, userRepository, currentUserId) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
