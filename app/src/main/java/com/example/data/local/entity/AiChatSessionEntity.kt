package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_chat_sessions")
data class AiChatSessionEntity(
    @PrimaryKey val sessionId: String,
    val title: String,
    val provider: String = "claude",
    val model: String = "claude-3-5-sonnet-20241022",
    val messageCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSyncedToTelegram: Boolean = false
)
