package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.SleepStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SleepStateDao {
    @Query("SELECT * FROM sleep_state WHERE id = 1 LIMIT 1")
    fun getSleepState(): Flow<SleepStateEntity?>

    @Query("SELECT * FROM sleep_state WHERE id = 1 LIMIT 1")
    suspend fun getSleepStateDirect(): SleepStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(state: SleepStateEntity)

    @Update
    suspend fun updateState(state: SleepStateEntity)
}
