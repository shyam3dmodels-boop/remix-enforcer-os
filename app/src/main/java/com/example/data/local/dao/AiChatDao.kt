package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.AiChatMessageEntity
import com.example.data.local.entity.AiChatSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AiChatDao {
    @Query("SELECT * FROM ai_chat_sessions ORDER BY updatedAt DESC")
    fun getAllSessions(): Flow<List<AiChatSessionEntity>>

    @Query("SELECT * FROM ai_chat_sessions WHERE sessionId = :sessionId LIMIT 1")
    suspend fun getSessionDirect(sessionId: String): AiChatSessionEntity?

    @Query("SELECT * FROM ai_chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSession(sessionId: String): Flow<List<AiChatMessageEntity>>

    @Query("SELECT * FROM ai_chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getMessagesForSessionDirect(sessionId: String): List<AiChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: AiChatSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: AiChatMessageEntity): Long

    @Query("UPDATE ai_chat_sessions SET messageCount = messageCount + 1, updatedAt = :now WHERE sessionId = :sessionId")
    suspend fun incrementMessageCount(sessionId: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE ai_chat_sessions SET isSyncedToTelegram = 1 WHERE sessionId = :sessionId")
    suspend fun markTelegramSynced(sessionId: String)

    @Query("DELETE FROM ai_chat_sessions WHERE sessionId = :sessionId")
    suspend fun deleteSession(sessionId: String)

    @Query("DELETE FROM ai_chat_messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesForSession(sessionId: String)
}
