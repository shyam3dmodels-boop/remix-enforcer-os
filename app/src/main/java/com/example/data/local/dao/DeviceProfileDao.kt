package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.DeviceProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceProfileDao {
    @Query("SELECT * FROM device_profile LIMIT 1")
    fun getProfile(): Flow<DeviceProfileEntity?>

    @Query("SELECT * FROM device_profile LIMIT 1")
    suspend fun getProfileDirect(): DeviceProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(profile: DeviceProfileEntity)

    @Update
    suspend fun updateProfile(profile: DeviceProfileEntity)

    @Query("UPDATE device_profile SET totalStepsRecorded = :steps WHERE deviceUuid = :uuid")
    suspend fun updateSteps(uuid: String, steps: Int)

    @Query("UPDATE device_profile SET studyDwellMinutes = :minutes WHERE deviceUuid = :uuid")
    suspend fun updateDwellMinutes(uuid: String, minutes: Int)

    @Query("UPDATE device_profile SET lastWifiSsid = :ssid, lastActiveTimestamp = :timestamp WHERE deviceUuid = :uuid")
    suspend fun updateWifiAndTimestamp(uuid: String, ssid: String, timestamp: Long)
}
