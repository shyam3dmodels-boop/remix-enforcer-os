package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.AppLimitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppLimitDao {
    @Query("SELECT * FROM app_limits ORDER BY appName ASC")
    fun getAllLimits(): Flow<List<AppLimitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLimit(limit: AppLimitEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(limits: List<AppLimitEntity>)

    @Update
    suspend fun updateLimit(limit: AppLimitEntity)

    @Delete
    suspend fun deleteLimit(limit: AppLimitEntity)

    @Query("SELECT COUNT(*) FROM app_limits")
    suspend fun getCount(): Int
}
