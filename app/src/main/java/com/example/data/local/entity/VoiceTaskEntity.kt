package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "voice_tasks")
data class VoiceTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val category: String = "HOMEWORK", // "HOMEWORK", "IMPORTANT_TEST", "DUE_DATE", "GENERAL"
    val lectureTitle: String = "Classroom Notes",
    val timestamp: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false,
    val urgencyLevel: String = "HIGH", // "NORMAL", "HIGH", "CRITICAL"
    val rawVoiceText: String = ""
)
