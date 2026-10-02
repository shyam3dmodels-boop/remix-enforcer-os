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

    fun updateProfile(
        name: String? = null,
        location: String? = null,
        lat: Double? = null,
        lon: Double? = null,
        bio: String? = null,
        handle: String? = null,
        directive: String? = null
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
        editor.apply()
    }

    fun setLocation(label: String, lat: Double = _latitude.value, lon: Double = _longitude.value) {
        updateProfile(location = label, lat = lat, lon = lon)
    }

    companion object {
        private const val PREFS_NAME = "user_profile_prefs"
        private const val KEY_USER_NAME = "key_user_name"
        private const val KEY_LOCATION_LABEL = "key_location_label"
        private const val KEY_LATITUDE = "key_latitude"
        private const val KEY_LONGITUDE = "key_longitude"
        private const val KEY_USER_BIO = "key_user_bio"
        private const val KEY_TELEGRAM_HANDLE = "key_telegram_handle"
        private const val KEY_CUSTOM_DIRECTIVE = "key_custom_directive"

        @Volatile
        private var INSTANCE: UserProfileManager? = null

        fun getInstance(context: Context): UserProfileManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserProfileManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
