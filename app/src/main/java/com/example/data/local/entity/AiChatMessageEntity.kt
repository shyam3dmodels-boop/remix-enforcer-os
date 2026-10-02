package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_chat_messages")
data class AiChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val sender: String, // "USER" or "AI"
    val content: String,
    val groundingMeta: String = "",
    val modelUsed: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
