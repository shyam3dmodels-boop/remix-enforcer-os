package com.example.core.discipline

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Generates structured, high-accountability daily study reports
 * that can be reviewed locally or shared to peer study mentors via Telegram.
 */
class StudyProofManager private constructor(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    suspend fun generateStudyProofReport(): StudyProofReport = withContext(Dispatchers.IO) {
        val db = AppDatabase.getInstance(context)
        val allVoiceTasks = db.voiceTaskDao().getAllTasksDirect()
        val allSchedules = db.coachingDao().getAllDirect()
        val appUsageTracker = AppUsageTracker(context)

        // Task stats
        val completedTasks = allVoiceTasks.filter { it.isCompleted }
        val pendingTasks = allVoiceTasks.filter { !it.isCompleted }
        val pendingHomework = pendingTasks.filter { it.category == "HOMEWORK" }
        val pendingTests = pendingTasks.filter { it.category == "IMPORTANT_TEST" }

        // Screen time stats for top distracting apps
        val checkedPackages = listOf(
            "com.google.android.youtube" to "YouTube",
            "com.instagram.android" to "Instagram",
            "com.whatsapp" to "WhatsApp",
            "com.reddit.frontpage" to "Reddit",
            "com.twitter.android" to "X/Twitter"
        )
        val usageMap = appUsageTracker.getTodayUsageMinutesMap()
        val topAppUsages = checkedPackages.mapNotNull { (pkg, name) ->
            val mins = usageMap[pkg] ?: 0
            if (mins > 0) "$name: ${mins}m" else null
        }
        val totalSampledDistractionMin = checkedPackages.sumOf { (pkg, _) ->
            usageMap[pkg] ?: 0
        }

        // Focus Milestone Target
        val milestoneTargetName = "Quarterly Focus Target"
        val milestoneDateMillis = System.currentTimeMillis() + (45L * 24 * 60 * 60 * 1000)
        val now = System.currentTimeMillis()
        val daysToMilestone = ((milestoneDateMillis - now) / (1000 * 60 * 60 * 24)).coerceAtLeast(0)

        val dateStr = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(Date())
        val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())

        val hours = totalSampledDistractionMin / 60
        val mins = totalSampledDistractionMin % 60
        val screenTimeText = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"

        val formattedText = """
            🛡️ <b>[SECONDARY BRAIN // TELEMETRY SUMMARY]</b>
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            📅 <b>Date:</b> $dateStr ($timeStr)
            🎯 <b>Milestone:</b> $milestoneTargetName ($daysToMilestone days remaining)
            
            ⏱️ <b>Active Screen Engagement:</b> $screenTimeText
            ${if (topAppUsages.isNotEmpty()) "📱 <b>Top Habits:</b>\n" + topAppUsages.joinToString("\n") { "  • $it" } else "📱 <b>App Usages:</b> Balanced digital wellness!"}
            
            📚 <b>Personal Goal Consistency:</b>
            • Completed Today: <b>${completedTasks.size}</b> action items
            • Pending Action Items: <b>${pendingHomework.size + pendingTests.size}</b> items
            
            🏫 <b>Coaching Center Timetable:</b>
            • Active Sessions: ${allSchedules.size} scheduled lectures
            • Class Shield: ${if (com.example.core.alarm.AlarmSafetyManager.getInstance(context).isClassShieldActive.value) "Armed (Class Silent)" else "Disarmed"}
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            <i>Generated via Remix Enforcer OS Peer Accountability Pipe</i>
        """.trimIndent()

        StudyProofReport(
            dateString = dateStr,
            timeString = timeStr,
            totalScreenTimeFormatted = screenTimeText,
            completedTaskCount = completedTasks.size,
            pendingHomeworkCount = pendingHomework.size,
            pendingTestCount = pendingTests.size,
            daysToExam = daysToMilestone.toInt(),
            formattedCardText = formattedText
        )
    }

    data class StudyProofReport(
        val dateString: String,
        val timeString: String,
        val totalScreenTimeFormatted: String,
        val completedTaskCount: Int,
        val pendingHomeworkCount: Int,
        val pendingTestCount: Int,
        val daysToExam: Int,
        val formattedCardText: String
    )

    companion object {
        private const val PREFS_NAME = "enforcer_study_proof_prefs"

        @Volatile
        private var instance: StudyProofManager? = null

        fun getInstance(context: Context): StudyProofManager {
            return instance ?: synchronized(this) {
                instance ?: StudyProofManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
