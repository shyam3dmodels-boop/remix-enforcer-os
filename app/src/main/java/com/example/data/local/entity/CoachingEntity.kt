package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "coaching_schedules")
data class CoachingEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val departureTime: String,
    val sessionTime: String,
    val checklist: String = "ID Card,Notes Register,Charger,Water Bottle,Study Bag",
    val latitude: Double = 28.6139,
    val longitude: Double = 77.2090,
    val radiusMeters: Float = 100f,
    val isGeofenceActive: Boolean = true,
    val isSilentOnArrival: Boolean = true,
    val isActive: Boolean = true
)
