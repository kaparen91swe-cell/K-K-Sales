package com.example.kksales.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import com.example.kksales.data.local.entity.ChatGroup
import com.example.kksales.data.local.entity.ChatMessage
import com.example.kksales.data.local.entity.User
import com.example.kksales.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel) {
    var activeConversationUser by remember { mutableStateOf<User?>(null) }
    var activeConversationGroup by remember { mutableStateOf<ChatGroup?>(null) }
    var isCreatingGroup by remember { mutableStateOf(false) }

    val allUsers by viewModel.allUsers.collectAsState()
    val userGroups by viewModel.userGroups.collectAsState()

    if (isCreatingGroup) {
        CreateGroupDialog(
            allUsers = allUsers,
            onDismiss = { isCreatingGroup = false },
            onCreate = { name, members ->
                viewModel.viewModelScope.launch {
                    viewModel.chatRepository.createGroup(name, viewModel.currentUserId, members)
                }
                isCreatingGroup = false
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Signal-style Header
            TopAppBar(
                title = { Text("Chat", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.headlineMedium) },
                actions = {
                    IconButton(onClick = { /* Search */ }) { Icon(Icons.Rounded.Search, null) }
                    IconButton(onClick = { isCreatingGroup = true }) { Icon(Icons.Rounded.GroupAdd, null) }
                    IconButton(onClick = { /* Settings */ }) { Icon(Icons.Rounded.MoreVert, null) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )

            // Conversations List
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item { 
                    Text("GRUPPER", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) 
                }
                items(userGroups) { group ->
                    ConversationItem(
                        title = group.name,
                        lastMessage = "Gruppmeddelande",
                        timestamp = "",
                        isGroup = true,
                        onClick = { activeConversationGroup = group }
                    )
                }

                item { 
                    Text("KONTAKTER", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) 
                }
                items(allUsers.filter { it.id != viewModel.currentUserId }) { user ->
                    ConversationItem(
                        title = user.name,
                        lastMessage = "Tryck för att chatta",
                        timestamp = "",
                        isGroup = false,
                        onClick = { activeConversationUser = user }
                    )
                }
            }
        }

        // Full-screen Conversation View (Signal style transition)
        AnimatedVisibility(
            visible = activeConversationUser != null || activeConversationGroup != null,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
        ) {
            val title = activeConversationUser?.name ?: activeConversationGroup?.name ?: ""
            
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AvatarCircle(name = title, size = 36.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(title, style = MaterialTheme.typography.titleMedium)
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { 
                                activeConversationUser = null
                                activeConversationGroup = null
                            }) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, null)
                            }
                        },
                        actions = {
                            if (activeConversationGroup != null && activeConversationGroup?.createdBy == viewModel.currentUserId) {
                                IconButton(onClick = {
                                    viewModel.deleteGroup(activeConversationGroup!!.id)
                                    activeConversationGroup = null
                                }) {
                                    Icon(Icons.Rounded.Delete, "Ta bort grupp", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                            IconButton(onClick = { /* Video Call */ }) { Icon(Icons.Rounded.VideoCall, null) }
                            IconButton(onClick = { /* Call */ }) { Icon(Icons.Rounded.Call, null) }
                        }
                    )
                },
                contentWindowInsets = WindowInsets.statusBars // Hantera endast statusfältet här
            ) { padding ->
                ChatConversation(
                    viewModel = viewModel,
                    userId = activeConversationUser?.id,
                    groupId = activeConversationGroup?.id,
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
fun ConversationItem(title: String, lastMessage: String, timestamp: String, isGroup: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarCircle(name = title, isGroup = isGroup)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(timestamp, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Text(
                lastMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun AvatarCircle(name: String, isGroup: Boolean = false, size: androidx.compose.ui.unit.Dp = 48.dp) {
    val backgroundColor = remember(name) {
        val colors = listOf(Color(0xFF2196F3), Color(0xFFE91E63), Color(0xFF4CAF50), Color(0xFFFF9800), Color(0xFF9C27B0))
        colors[name.length % colors.size]
    }
    
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        if (isGroup) {
            Icon(Icons.Rounded.Groups, null, tint = Color.White, modifier = Modifier.size(size * 0.6f))
        } else {
            Text(
                text = name.take(1).uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.4f).sp
            )
        }
    }
}

@Composable
fun ChatConversation(
    viewModel: ChatViewModel,
    userId: Int?,
    groupId: Int?,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.messages.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    var text by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(userId, groupId) {
        if (userId != null) viewModel.loadPrivateMessages(userId)
        else if (groupId != null) viewModel.loadGroupMessages(groupId)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(messages) { message ->
                val isMe = message.senderId == viewModel.currentUserId
                val sender = allUsers.find { it.id == message.senderId }
                SignalChatBubble(
                    message = message,
                    isMe = isMe,
                    senderName = sender?.name ?: "Okänd"
                )
            }
        }

        // Signal Input Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Surface(
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { /* Add Attachment */ }) {
                        Icon(Icons.Rounded.Add, null, tint = MaterialTheme.colorScheme.primary)
                    }
                    
                    TextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Meddelande") },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = false,
                        maxLines = 4
                    )
                    
                    if (text.isBlank()) {
                        IconButton(onClick = { /* Microphone */ }) {
                            Icon(Icons.Rounded.Mic, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        IconButton(
                            onClick = {
                                viewModel.sendMessage(userId, groupId, text)
                                text = ""
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Send, null, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SignalChatBubble(message: ChatMessage, isMe: Boolean, senderName: String) {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeString = sdf.format(Date(message.timestamp))

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        if (!isMe) {
            Text(
                senderName, 
                style = MaterialTheme.typography.labelSmall, 
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 12.dp, bottom = 2.dp)
            )
        }
        
        Surface(
            color = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMe) 16.dp else 4.dp,
                bottomEnd = if (isMe) 4.dp else 16.dp
            ),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Column {
                    Text(
                        message.content, 
                        color = if (isMe) Color.White else MaterialTheme.colorScheme.onSecondaryContainer,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        timeString,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isMe) Color.White.copy(alpha = 0.7f) else Color.Gray,
                        modifier = Modifier.align(Alignment.End).padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CreateGroupDialog(allUsers: List<User>, onDismiss: () -> Unit, onCreate: (String, List<Int>) -> Unit) {
    var name by remember { mutableStateOf("") }
    val selectedMembers = remember { mutableStateListOf<Int>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Skapa ny grupp") },
        text = {
            Column {
                OutlinedTextField(
                    value = name, 
                    onValueChange = { name = it }, 
                    label = { Text("Gruppnamn") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Välj medlemmar", fontWeight = FontWeight.Bold)
                LazyColumn(modifier = Modifier.height(250.dp)) {
                    items(allUsers) { user ->
                        ListItem(
                            headlineContent = { Text(user.name) },
                            leadingContent = { AvatarCircle(name = user.name, size = 32.dp) },
                            trailingContent = {
                                Checkbox(
                                    checked = selectedMembers.contains(user.id),
                                    onCheckedChange = { 
                                        if (it == true) selectedMembers.add(user.id)
                                        else selectedMembers.remove(user.id)
                                    }
                                )
                            },
                            modifier = Modifier.clickable {
                                if (selectedMembers.contains(user.id)) selectedMembers.remove(user.id)
                                else selectedMembers.add(user.id)
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name, selectedMembers.toList()) }, 
                enabled = name.isNotBlank() && selectedMembers.isNotEmpty()
            ) {
                Text("Skapa")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Avbryt") }
        }
    )
}
