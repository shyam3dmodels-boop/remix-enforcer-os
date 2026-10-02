package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_keys")
data class AiKeyEntity(
    @PrimaryKey val provider: String, // "gemini", "nvidia", "deepseek", "openrouter", "claude", "groq"
    val apiKey: String,
    val selectedModel: String = "",
    val isActive: Boolean = true,
    val isSyncedWithServer: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
