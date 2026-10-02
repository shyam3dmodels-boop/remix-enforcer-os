package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.AiKeyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AiKeyDao {
    @Query("SELECT * FROM ai_keys ORDER BY provider ASC")
    fun getAllKeys(): Flow<List<AiKeyEntity>>

    @Query("SELECT * FROM ai_keys WHERE provider = :provider LIMIT 1")
    suspend fun getKeyDirect(provider: String): AiKeyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(key: AiKeyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(keys: List<AiKeyEntity>)

    @Query("DELETE FROM ai_keys WHERE provider = :provider")
    suspend fun deleteKey(provider: String)

    @Query("SELECT COUNT(*) FROM ai_keys")
    suspend fun getCount(): Int
}
