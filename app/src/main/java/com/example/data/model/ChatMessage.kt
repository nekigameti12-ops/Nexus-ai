package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MessageSender {
    USER,
    NEXUS,
    SYSTEM
}

enum class MessageStatus {
    IDLE,
    THINKING,
    EXECUTING,
    SUCCESS,
    ERROR
}

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageStatus = MessageStatus.SUCCESS,
    val actionsSummary: String? = null,
    val isVoice: Boolean = false,
    val errorMessage: String? = null
)
