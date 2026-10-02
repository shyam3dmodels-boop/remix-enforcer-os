package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sleep_state")
data class SleepStateEntity(
    @PrimaryKey val id: Int = 1,
    val sleepDurationHours: Int = 6,
    val screenOffDetectionMinutes: Int = 20,
    val batteryGuardThreshold: Int = 30,
    val isAutoSleepActive: Boolean = true,
    val sleepRecordedTime: Long = 0L,
    val scheduledWakeTime: Long = 0L,
    val isAlarmRinging: Boolean = false,
    val wakefulnessGuardActive: Boolean = false,
    val wakefulnessExpiryTime: Long = 0L
)
