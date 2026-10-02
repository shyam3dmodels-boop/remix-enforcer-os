package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.VoiceTaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VoiceTaskDao {
    @Query("SELECT * FROM voice_tasks ORDER BY timestamp DESC")
    fun getAllTasks(): Flow<List<VoiceTaskEntity>>

    @Query("SELECT * FROM voice_tasks ORDER BY timestamp DESC")
    suspend fun getAllTasksDirect(): List<VoiceTaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: VoiceTaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<VoiceTaskEntity>)

    @Update
    suspend fun updateTask(task: VoiceTaskEntity)

    @Delete
    suspend fun deleteTask(task: VoiceTaskEntity)

    @Query("DELETE FROM voice_tasks WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("UPDATE voice_tasks SET isCompleted = :completed WHERE id = :id")
    suspend fun setCompleted(id: Int, completed: Boolean)

    @Query("SELECT COUNT(*) FROM voice_tasks")
    suspend fun getCount(): Int
}
