package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.AiChatDao
import com.example.data.local.dao.AiKeyDao
import com.example.data.local.dao.AppLimitDao
import com.example.data.local.dao.CoachingDao
import com.example.data.local.dao.DeviceProfileDao
import com.example.data.local.dao.LectureDao
import com.example.data.local.dao.SleepStateDao
import com.example.data.local.dao.VoiceTaskDao
import com.example.data.local.entity.AiChatMessageEntity
import com.example.data.local.entity.AiChatSessionEntity
import com.example.data.local.entity.AiKeyEntity
import com.example.data.local.entity.AppLimitEntity
import com.example.data.local.entity.CoachingEntity
import com.example.data.local.entity.DeviceProfileEntity
import com.example.data.local.entity.LectureEntity
import com.example.data.local.entity.SleepStateEntity
import com.example.data.local.entity.VoiceTaskEntity

@Database(
    entities = [
        CoachingEntity::class,
        AppLimitEntity::class,
        SleepStateEntity::class,
        LectureEntity::class,
        VoiceTaskEntity::class,
        DeviceProfileEntity::class,
        AiKeyEntity::class,
        AiChatSessionEntity::class,
        AiChatMessageEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun coachingDao(): CoachingDao
    abstract fun appLimitDao(): AppLimitDao
    abstract fun sleepStateDao(): SleepStateDao
    abstract fun lectureDao(): LectureDao
    abstract fun voiceTaskDao(): VoiceTaskDao
    abstract fun deviceProfileDao(): DeviceProfileDao
    abstract fun aiKeyDao(): AiKeyDao
    abstract fun aiChatDao(): AiChatDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "enforcer_database.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
