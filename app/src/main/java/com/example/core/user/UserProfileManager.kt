package com.example.core.user

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages user profile customization details (Name, Location, Coordinates, Bio, Telegram Handle, AI Directives).
 * Allows the user to customize their identity and location on device and via Telegram C2 commands.
 */
class UserProfileManager private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _userName = MutableStateFlow(prefs.getString(KEY_USER_NAME, "Harsh") ?: "Harsh")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _locationLabel = MutableStateFlow(prefs.getString(KEY_LOCATION_LABEL, "New Delhi, India") ?: "New Delhi, India")
    val locationLabel: StateFlow<String> = _locationLabel.asStateFlow()

    private val _latitude = MutableStateFlow(prefs.getFloat(KEY_LATITUDE, 28.6139f).toDouble())
    val latitude: StateFlow<Double> = _latitude.asStateFlow()

    private val _longitude = MutableStateFlow(prefs.getFloat(KEY_LONGITUDE, 77.2090f).toDouble())
    val longitude: StateFlow<Double> = _longitude.asStateFlow()

    private val _userBio = MutableStateFlow(prefs.getString(KEY_USER_BIO, "Secondary Brain 2.0 Architect") ?: "Secondary Brain 2.0 Architect")
    val userBio: StateFlow<String> = _userBio.asStateFlow()

    private val _telegramHandle = MutableStateFlow(prefs.getString(KEY_TELEGRAM_HANDLE, "@harsh") ?: "@harsh")
    val telegramHandle: StateFlow<String> = _telegramHandle.asStateFlow()

    private val _customDirective = MutableStateFlow(
        prefs.getString(KEY_CUSTOM_DIRECTIVE, "Focus on deep productivity, clean architecture, and relentless discipline.") 
            ?: "Focus on deep productivity, clean architecture, and relentless discipline."
    )
    val customDirective: StateFlow<String> = _customDirective.asStateFlow()

    private val _avatarEmoji = MutableStateFlow(prefs.getString(KEY_AVATAR_EMOJI, "⚡") ?: "⚡")
    val avatarEmoji: StateFlow<String> = _avatarEmoji.asStateFlow()

    private val _currentStatus = MutableStateFlow(prefs.getString(KEY_CURRENT_STATUS, "In Deep Work 🚀") ?: "In Deep Work 🚀")
    val currentStatus: StateFlow<String> = _currentStatus.asStateFlow()

    private val _aiPersonaStyle = MutableStateFlow(prefs.getString(KEY_AI_PERSONA_STYLE, "Tony Stark Jarvis") ?: "Tony Stark Jarvis")
    val aiPersonaStyle: StateFlow<String> = _aiPersonaStyle.asStateFlow()

    fun updateProfile(
        name: String? = null,
        location: String? = null,
        lat: Double? = null,
        lon: Double? = null,
        bio: String? = null,
        handle: String? = null,
        directive: String? = null,
        avatar: String? = null,
        status: String? = null,
        personaStyle: String? = null
    ) {
        val editor = prefs.edit()
        name?.let {
            editor.putString(KEY_USER_NAME, it.trim())
            _userName.value = it.trim()
        }
        location?.let {
            editor.putString(KEY_LOCATION_LABEL, it.trim())
            _locationLabel.value = it.trim()
        }
        lat?.let {
            editor.putFloat(KEY_LATITUDE, it.toFloat())
            _latitude.value = it
        }
        lon?.let {
            editor.putFloat(KEY_LONGITUDE, it.toFloat())
            _longitude.value = it
        }
        bio?.let {
            editor.putString(KEY_USER_BIO, it.trim())
            _userBio.value = it.trim()
        }
        handle?.let {
            editor.putString(KEY_TELEGRAM_HANDLE, it.trim())
            _telegramHandle.value = it.trim()
        }
        directive?.let {
            editor.putString(KEY_CUSTOM_DIRECTIVE, it.trim())
            _customDirective.value = it.trim()
        }
        avatar?.let {
            editor.putString(KEY_AVATAR_EMOJI, it.trim())
            _avatarEmoji.value = it.trim()
        }
        status?.let {
            editor.putString(KEY_CURRENT_STATUS, it.trim())
            _currentStatus.value = it.trim()
        }
        personaStyle?.let {
            editor.putString(KEY_AI_PERSONA_STYLE, it.trim())
            _aiPersonaStyle.value = it.trim()
        }
        editor.apply()
    }

    fun setLocation(label: String, lat: Double = _latitude.value, lon: Double = _longitude.value) {
        updateProfile(location = label, lat = lat, lon = lon)
    }

    fun applyLocationPreset(preset: LocationPreset) {
        updateProfile(location = preset.label, lat = preset.lat, lon = preset.lon)
    }

    fun toProfileMap(): Map<String, Any> {
        return mapOf(
            "name" to _userName.value,
            "location" to _locationLabel.value,
            "latitude" to _latitude.value,
            "longitude" to _longitude.value,
            "bio" to _userBio.value,
            "telegramHandle" to _telegramHandle.value,
            "customDirective" to _customDirective.value,
            "avatar" to _avatarEmoji.value,
            "status" to _currentStatus.value,
            "personaStyle" to _aiPersonaStyle.value,
            "lastUpdated" to System.currentTimeMillis()
        )
    }

    data class LocationPreset(val name: String, val label: String, val lat: Double, val lon: Double, val icon: String)

    companion object {
        private const val PREFS_NAME = "user_profile_prefs"
        private const val KEY_USER_NAME = "key_user_name"
        private const val KEY_LOCATION_LABEL = "key_location_label"
        private const val KEY_LATITUDE = "key_latitude"
        private const val KEY_LONGITUDE = "key_longitude"
        private const val KEY_USER_BIO = "key_user_bio"
        private const val KEY_TELEGRAM_HANDLE = "key_telegram_handle"
        private const val KEY_CUSTOM_DIRECTIVE = "key_custom_directive"
        private const val KEY_AVATAR_EMOJI = "key_avatar_emoji"
        private const val KEY_CURRENT_STATUS = "key_current_status"
        private const val KEY_AI_PERSONA_STYLE = "key_ai_persona_style"

        val DEFAULT_LOCATION_PRESETS = listOf(
            LocationPreset("🏠 Home", "New Delhi Home HQ", 28.6139, 77.2090, "🏠"),
            LocationPreset("🏢 Office", "Cyber City Innovation Hub", 28.4986, 77.0878, "🏢"),
            LocationPreset("📚 Library", "Central Study & Research Hall", 28.5355, 77.3910, "📚"),
            LocationPreset("🏋️ Gym", "Fitness & Conditioning Bunker", 28.6289, 77.2065, "🏋️"),
            LocationPreset("🥷 Stealth", "Encrypted Undisclosed Location", 0.0, 0.0, "🥷")
        )

        val AVATAR_OPTIONS = listOf("⚡", "🧠", "👑", "🚀", "🎯", "🛡️", "🔥", "💻", "🤖", "🦅")

        val STATUS_OPTIONS = listOf(
            "In Deep Work 🚀",
            "Focusing • Do Not Disturb ⛔",
            "Studying & Researching 📚",
            "Traveling ✈️",
            "Offline / Resting 🌙"
        )

        val PERSONA_STYLES = listOf(
            "Tony Stark Jarvis" to "Brilliant, witty, hyper-capable, futuristic engineering focus.",
            "Relentless Enforcer" to "Zero excuses, strict accountability, high-discipline coaching.",
            "Gentle Mentor" to "Patient, deeply encouraging, step-by-step guidance.",
            "Direct & Concise" to "Raw facts, bullet points, zero fluff, instant speed."
        )

        @Volatile
        private var INSTANCE: UserProfileManager? = null

        fun getInstance(context: Context): UserProfileManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserProfileManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
