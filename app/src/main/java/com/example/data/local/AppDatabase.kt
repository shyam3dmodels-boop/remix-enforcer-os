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
    version = AppDatabase.DATABASE_VERSION,
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
        const val DATABASE_VERSION = 5
        const val DATABASE_NAME = "enforcer_database.db"

        private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `lectures` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `subject` TEXT NOT NULL, `title` TEXT NOT NULL, `filePath` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `durationSec` INTEGER NOT NULL, `fileSizeBytes` INTEGER NOT NULL, `notes` TEXT NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `voice_tasks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `transcript` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `isCompleted` INTEGER NOT NULL, `isSynced` INTEGER NOT NULL)")
            }
        }

        private val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `device_profiles` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `studentName` TEXT NOT NULL, `centerName` TEXT NOT NULL, `centerLatitude` REAL NOT NULL, `centerLongitude` REAL NOT NULL, `centerRadiusMeters` REAL NOT NULL, `customInstructions` TEXT NOT NULL)")
            }
        }

        private val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `ai_keys` (`provider` TEXT PRIMARY KEY NOT NULL, `apiKey` TEXT NOT NULL, `model` TEXT NOT NULL, `isEnabled` INTEGER NOT NULL)")
            }
        }

        private val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `ai_chat_sessions` (`id` TEXT PRIMARY KEY NOT NULL, `title` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `model` TEXT NOT NULL, `provider` TEXT NOT NULL, `systemPrompt` TEXT NOT NULL, `messageCount` INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `ai_chat_messages` (`id` TEXT PRIMARY KEY NOT NULL, `sessionId` TEXT NOT NULL, `role` TEXT NOT NULL, `content` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `model` TEXT NOT NULL, `tokenCount` INTEGER NOT NULL, `isError` INTEGER NOT NULL, FOREIGN KEY(`sessionId`) REFERENCES `ai_chat_sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_chat_messages_sessionId` ON `ai_chat_messages` (`sessionId`)")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
