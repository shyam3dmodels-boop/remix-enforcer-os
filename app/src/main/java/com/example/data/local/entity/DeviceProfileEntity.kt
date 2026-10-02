package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "device_profile")
data class DeviceProfileEntity(
    @PrimaryKey val deviceUuid: String,
    val deviceName: String,
    val modelName: String,
    val androidVersion: String,
    val totalStepsRecorded: Int = 0,
    val studyDwellMinutes: Int = 0,
    val lastWifiSsid: String = "Unknown",
    val lastActiveTimestamp: Long = System.currentTimeMillis(),
    val isGuardianLinked: Boolean = false,
    val guardianHandle: String = "@StudyGuardianBot"
)
