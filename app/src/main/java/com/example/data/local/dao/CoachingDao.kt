package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.CoachingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CoachingDao {
    @Query("SELECT * FROM coaching_schedules ORDER BY id ASC")
    fun getAllSchedules(): Flow<List<CoachingEntity>>

    @Query("SELECT * FROM coaching_schedules ORDER BY id ASC")
    fun getAllDirect(): List<CoachingEntity>

    @Query("SELECT * FROM coaching_schedules WHERE id = :id LIMIT 1")
    suspend fun getScheduleById(id: Int): CoachingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: CoachingEntity): Long

    @Update
    suspend fun updateSchedule(schedule: CoachingEntity)

    @Delete
    suspend fun deleteSchedule(schedule: CoachingEntity)

    @Query("SELECT COUNT(*) FROM coaching_schedules")
    suspend fun getCount(): Int
}
