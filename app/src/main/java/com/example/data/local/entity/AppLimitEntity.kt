package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_limits")
data class AppLimitEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val packageName: String,
    val appName: String,
    val dailyLimitMinutes: Int = 45,
    val warningMinutes: Int = 40,
    val category: String = "Social",
    val isRestricted: Boolean = true
)
