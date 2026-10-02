package com.example.core.telemetry

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import com.example.data.local.entity.DeviceProfileEntity
import java.util.UUID

/**
 * Provides persistent, privacy-preserving device identification and system telemetry
 * for parental safety link and study partner accountability.
 */
class DeviceInfoProvider(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getOrGenerateDeviceUuid(): String {
        var uuid = prefs.getString(KEY_DEVICE_UUID, null)
        if (uuid == null) {
            val raw = UUID.randomUUID().toString().replace("-", "")
            uuid = "${raw.substring(0, 4)}-${raw.substring(4, 8)}-${raw.substring(8, 12)}-${raw.substring(12, 16)}"
            prefs.edit().putString(KEY_DEVICE_UUID, uuid).apply()
        }
        return uuid
    }

    fun getDeviceName(): String {
        val model = Build.MODEL
        val manufacturer = Build.MANUFACTURER
        return if (model.startsWith(manufacturer, ignoreCase = true)) {
            model
        } else {
            "${manufacturer.replaceFirstChar { it.uppercase() }} $model"
        }
    }

    fun getBuildFingerprint(): String {
        return Build.FINGERPRINT ?: "google/pixel/release-keys"
    }

    fun getAndroidVersion(): String {
        return "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
    }

    fun getInitialProfile(): DeviceProfileEntity {
        return DeviceProfileEntity(
            deviceUuid = getOrGenerateDeviceUuid(),
            deviceName = getDeviceName(),
            modelName = Build.MODEL,
            androidVersion = getAndroidVersion(),
            totalStepsRecorded = 0,
            studyDwellMinutes = 0,
            lastWifiSsid = "Scanning...",
            lastActiveTimestamp = System.currentTimeMillis(),
            isGuardianLinked = prefs.getBoolean(KEY_GUARDIAN_LINKED, false),
            guardianHandle = prefs.getString(KEY_GUARDIAN_HANDLE, "@StudyGuardianBot") ?: "@StudyGuardianBot"
        )
    }

    fun getUserName(): String {
        return prefs.getString(KEY_USER_NAME, "Harsh (Commander)") ?: "Harsh (Commander)"
    }

    fun getUserId(): String {
        var uid = prefs.getString(KEY_USER_ID, null)
        if (uid == null) {
            val nameClean = getUserName().take(3).uppercase().filter { it.isLetter() }.ifEmpty { "USR" }
            val suffix = (10..99).random()
            uid = "USR-$nameClean-$suffix"
            prefs.edit().putString(KEY_USER_ID, uid).apply()
        }
        return uid
    }

    fun getUserRole(): String {
        return prefs.getString(KEY_USER_ROLE, "Fleet Commander") ?: "Fleet Commander"
    }

    fun registerUser(name: String, role: String = "Field Agent", email: String = ""): String {
        val cleanName = name.trim().ifEmpty { "Agent" }
        val namePrefix = cleanName.take(3).uppercase().filter { it.isLetter() }.ifEmpty { "USR" }
        val suffix = (10..99).random()
        val generatedUid = "USR-$namePrefix-$suffix"

        prefs.edit()
            .putString(KEY_USER_NAME, cleanName)
            .putString(KEY_USER_ID, generatedUid)
            .putString(KEY_USER_ROLE, role)
            .putString(KEY_USER_EMAIL, email)
            .apply()

        return generatedUid
    }

    fun setGuardianLinked(linked: Boolean, handle: String) {
        prefs.edit()
            .putBoolean(KEY_GUARDIAN_LINKED, linked)
            .putString(KEY_GUARDIAN_HANDLE, handle)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "enforcer_device_telemetry_prefs"
        private const val KEY_DEVICE_UUID = "key_persistent_device_uuid"
        private const val KEY_USER_NAME = "key_user_name"
        private const val KEY_USER_ID = "key_user_id"
        private const val KEY_USER_ROLE = "key_user_role"
        private const val KEY_USER_EMAIL = "key_user_email"
        private const val KEY_GUARDIAN_LINKED = "key_guardian_linked"
        private const val KEY_GUARDIAN_HANDLE = "key_guardian_handle"

        @Volatile
        private var instance: DeviceInfoProvider? = null

        fun getInstance(context: Context): DeviceInfoProvider {
            return instance ?: synchronized(this) {
                instance ?: DeviceInfoProvider(context.applicationContext).also { instance = it }
            }
        }
    }
}
