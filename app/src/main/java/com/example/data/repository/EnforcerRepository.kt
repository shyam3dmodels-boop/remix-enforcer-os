package com.example.data.repository

import com.example.data.local.dao.AiChatDao
import com.example.data.local.dao.AiKeyDao
import com.example.data.local.dao.AppLimitDao
import com.example.data.local.dao.CoachingDao
import com.example.data.local.dao.LectureDao
import com.example.data.local.dao.SleepStateDao
import com.example.data.local.entity.AiChatMessageEntity
import com.example.data.local.entity.AiChatSessionEntity
import com.example.data.local.entity.AiKeyEntity
import com.example.data.local.entity.AppLimitEntity
import com.example.data.local.entity.CoachingEntity
import com.example.data.local.entity.LectureBookmark
import com.example.data.local.entity.LectureEntity
import com.example.data.local.entity.SleepStateEntity
import kotlinx.coroutines.flow.Flow

class EnforcerRepository(
    private val coachingDao: CoachingDao,
    private val appLimitDao: AppLimitDao,
    private val sleepStateDao: SleepStateDao,
    private val lectureDao: LectureDao,
    private val voiceTaskDao: com.example.data.local.dao.VoiceTaskDao,
    private val deviceProfileDao: com.example.data.local.dao.DeviceProfileDao,
    private val aiKeyDao: AiKeyDao,
    private val aiChatDao: AiChatDao
) {
    val allCoachingSchedules: Flow<List<CoachingEntity>> = coachingDao.getAllSchedules()
    val allAppLimits: Flow<List<AppLimitEntity>> = appLimitDao.getAllLimits()
    val sleepState: Flow<SleepStateEntity?> = sleepStateDao.getSleepState()
    val allLectures: Flow<List<LectureEntity>> = lectureDao.getAllLectures()
    val allVoiceTasks: Flow<List<com.example.data.local.entity.VoiceTaskEntity>> = voiceTaskDao.getAllTasks()
    val deviceProfile: Flow<com.example.data.local.entity.DeviceProfileEntity?> = deviceProfileDao.getProfile()
    val allAiKeys: Flow<List<AiKeyEntity>> = aiKeyDao.getAllKeys()
    val allChatSessions: Flow<List<AiChatSessionEntity>> = aiChatDao.getAllSessions()

    fun getSessionMessages(sessionId: String): Flow<List<AiChatMessageEntity>> =
        aiChatDao.getMessagesForSession(sessionId)

    suspend fun getSessionMessagesDirect(sessionId: String): List<AiChatMessageEntity> =
        aiChatDao.getMessagesForSessionDirect(sessionId)

    suspend fun getAiKeyDirect(provider: String): AiKeyEntity? =
        aiKeyDao.getKeyDirect(provider.lowercase())

    suspend fun saveAiKey(key: AiKeyEntity) =
        aiKeyDao.insertOrUpdate(key)

    suspend fun deleteAiKey(provider: String) =
        aiKeyDao.deleteKey(provider.lowercase())

    suspend fun saveChatSession(session: AiChatSessionEntity) =
        aiChatDao.insertSession(session)

    suspend fun saveChatMessage(msg: AiChatMessageEntity): Long {
        val id = aiChatDao.insertMessage(msg)
        aiChatDao.incrementMessageCount(msg.sessionId)
        return id
    }

    suspend fun markSessionTelegramSynced(sessionId: String) =
        aiChatDao.markTelegramSynced(sessionId)

    suspend fun deleteChatSession(sessionId: String) {
        aiChatDao.deleteMessagesForSession(sessionId)
        aiChatDao.deleteSession(sessionId)
    }

    suspend fun getDeviceProfileDirect(): com.example.data.local.entity.DeviceProfileEntity? =
        deviceProfileDao.getProfileDirect()

    suspend fun saveDeviceProfile(profile: com.example.data.local.entity.DeviceProfileEntity) =
        deviceProfileDao.insertOrUpdate(profile)

    suspend fun updateDeviceSteps(uuid: String, steps: Int) =
        deviceProfileDao.updateSteps(uuid, steps)

    suspend fun updateDeviceDwell(uuid: String, minutes: Int) =
        deviceProfileDao.updateDwellMinutes(uuid, minutes)

    suspend fun updateDeviceWifi(uuid: String, ssid: String) =
        deviceProfileDao.updateWifiAndTimestamp(uuid, ssid, System.currentTimeMillis())

    suspend fun getAllLecturesDirect(): List<LectureEntity> = lectureDao.getAllLecturesDirect()

    suspend fun saveVoiceTask(task: com.example.data.local.entity.VoiceTaskEntity): Long =
        voiceTaskDao.insertTask(task)

    suspend fun updateVoiceTask(task: com.example.data.local.entity.VoiceTaskEntity) =
        voiceTaskDao.updateTask(task)

    suspend fun deleteVoiceTask(id: Int) =
        voiceTaskDao.deleteById(id)

    suspend fun toggleVoiceTaskCompleted(id: Int, isCompleted: Boolean) =
        voiceTaskDao.setCompleted(id, isCompleted)

    suspend fun initializeDefaultsIfNeeded() {
        if (coachingDao.getCount() == 0) {
            coachingDao.insertSchedule(
                CoachingEntity(
                    title = "Deep Work Session (Morning Focus)",
                    departureTime = "07:30 AM",
                    sessionTime = "08:00 AM",
                    checklist = "Key Documents,Laptop,Notebook,Charger,Water Bottle",
                    latitude = 28.6139,
                    longitude = 77.2090,
                    radiusMeters = 100f,
                    isGeofenceActive = true,
                    isSilentOnArrival = true
                )
            )
            coachingDao.insertSchedule(
                CoachingEntity(
                    title = "Strategy & Review (Evening Block)",
                    departureTime = "04:15 PM",
                    sessionTime = "04:45 PM",
                    checklist = "Work Planner,Action Checklist,Tablet,Water Bottle,Earbuds",
                    latitude = 28.6150,
                    longitude = 77.2100,
                    radiusMeters = 100f,
                    isGeofenceActive = true,
                    isSilentOnArrival = true
                )
            )
        }

        if (appLimitDao.getCount() == 0) {
            val defaultLimits = listOf(
                AppLimitEntity(
                    packageName = "com.google.android.youtube",
                    appName = "YouTube / Video",
                    dailyLimitMinutes = 45,
                    warningMinutes = 40,
                    category = "Entertainment"
                ),
                AppLimitEntity(
                    packageName = "com.instagram.android",
                    appName = "Instagram / Socials",
                    dailyLimitMinutes = 30,
                    warningMinutes = 25,
                    category = "Social"
                ),
                AppLimitEntity(
                    packageName = "com.android.chrome",
                    appName = "Chrome / Web Surfing",
                    dailyLimitMinutes = 45,
                    warningMinutes = 40,
                    category = "Browser"
                ),
                AppLimitEntity(
                    packageName = "com.openai.chatgpt",
                    appName = "AI Apps / Chat",
                    dailyLimitMinutes = 45,
                    warningMinutes = 40,
                    category = "AI / LLM"
                )
            )
            appLimitDao.insertAll(defaultLimits)
        }

        if (sleepStateDao.getSleepStateDirect() == null) {
            sleepStateDao.insertOrUpdate(
                SleepStateEntity(
                    id = 1,
                    sleepDurationHours = 6,
                    screenOffDetectionMinutes = 20,
                    batteryGuardThreshold = 30,
                    isAutoSleepActive = true,
                    sleepRecordedTime = 0L,
                    scheduledWakeTime = 0L,
                    isAlarmRinging = false,
                    wakefulnessGuardActive = false
                )
            )
        }

        if (lectureDao.getCount() == 0) {
            val sampleBookmarks = listOf(
                LectureBookmark(timeSeconds = 120, label = "Architecture Core Decisions"),
                LectureBookmark(timeSeconds = 345, label = "Zero-Retention Privacy Spec"),
                LectureBookmark(timeSeconds = 620, label = "Implementation Action Items")
            )
            lectureDao.insertLecture(
                LectureEntity(
                    title = "System Design & Memory Hub Review",
                    subject = "Engineering",
                    coachingSessionName = "Weekly Architecture Sync",
                    filePath = "",
                    durationSeconds = 840L,
                    fileSizeBytes = 3840000L,
                    timestamp = System.currentTimeMillis() - (3600 * 1000 * 5),
                    bookmarksJson = LectureEntity.encodeBookmarks(sampleBookmarks),
                    notes = "Agreed on zero-retention Groq API processing paired with local Room persistence and event-driven step sensors."
                )
            )
        }

        if (aiKeyDao.getCount() == 0) {
            val defaultKeys = listOf(
                AiKeyEntity("claude", "", "claude-3-5-sonnet-20241022", true),
                AiKeyEntity("gemini", "", "gemini-1.5-flash", true),
                AiKeyEntity("nvidia", "", "nvidia/llama-3.1-nemotron-70b-instruct", true),
                AiKeyEntity("deepseek", "", "deepseek-chat", true),
                AiKeyEntity("openrouter", "", "anthropic/claude-3.5-sonnet", true),
                AiKeyEntity("groq", "", "llama-3.3-70b-versatile", true)
            )
            aiKeyDao.insertAll(defaultKeys)
        }
    }

    suspend fun saveCoaching(schedule: CoachingEntity) {
        if (schedule.id == 0) {
            coachingDao.insertSchedule(schedule)
        } else {
            coachingDao.updateSchedule(schedule)
        }
    }

    suspend fun deleteCoaching(schedule: CoachingEntity) = coachingDao.deleteSchedule(schedule)

    suspend fun updateAppLimit(limit: AppLimitEntity) = appLimitDao.updateLimit(limit)
    suspend fun addAppLimit(limit: AppLimitEntity) = appLimitDao.insertLimit(limit)
    suspend fun deleteAppLimit(limit: AppLimitEntity) = appLimitDao.deleteLimit(limit)

    suspend fun updateSleepState(state: SleepStateEntity) = sleepStateDao.insertOrUpdate(state)
    suspend fun getSleepStateDirect(): SleepStateEntity? = sleepStateDao.getSleepStateDirect()

    suspend fun saveLecture(lecture: LectureEntity): Long {
        return if (lecture.id == 0L) {
            lectureDao.insertLecture(lecture)
        } else {
            lectureDao.updateLecture(lecture)
            lecture.id
        }
    }

    suspend fun deleteLecture(lecture: LectureEntity) = lectureDao.deleteLecture(lecture)
    suspend fun deleteLectureById(id: Long) = lectureDao.deleteLectureById(id)
    suspend fun getLectureById(id: Long) = lectureDao.getLectureById(id)
    suspend fun getLectureByFilePath(filePath: String) = lectureDao.getLectureByFilePath(filePath)
    suspend fun updateLecture(lecture: LectureEntity) = lectureDao.updateLecture(lecture)
}
