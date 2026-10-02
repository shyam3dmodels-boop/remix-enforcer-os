package com.example.core.coaching

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.util.Log

class GeofenceSilentManager(private val context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val prefs = context.getSharedPreferences("enforcer_geofence_prefs", Context.MODE_PRIVATE)

    fun hasDndPermission(): Boolean {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            nm?.isNotificationPolicyAccessGranted == true
        } else {
            true
        }
    }

    fun applyCoachingArrivalSilent(coachingTitle: String): Boolean {
        val am = audioManager ?: return false
        try {
            // Save original volume
            val originalRinger = am.ringerMode
            prefs.edit().putInt(KEY_ORIGINAL_RINGER, originalRinger).apply()

            // Set to VIBRATE or SILENT
            if (hasDndPermission()) {
                am.ringerMode = AudioManager.RINGER_MODE_SILENT
            } else {
                am.ringerMode = AudioManager.RINGER_MODE_VIBRATE
            }

            // Clear checklist lockscreen wallpaper
            DynamicWallpaperEngine(context).clearLockscreenWallpaper()
            Log.d("GeofenceSilentManager", "Arrival at $coachingTitle -> Phone silenced")
            return true
        } catch (e: Exception) {
            Log.e("GeofenceSilentManager", "Failed to mute audio on arrival", e)
            return false
        }
    }

    fun applyCoachingDepartureRestore(coachingTitle: String): Boolean {
        val am = audioManager ?: return false
        try {
            val originalRinger = prefs.getInt(KEY_ORIGINAL_RINGER, AudioManager.RINGER_MODE_NORMAL)
            am.ringerMode = originalRinger
            Log.d("GeofenceSilentManager", "Departed $coachingTitle -> Ringer restored to $originalRinger")
            return true
        } catch (e: Exception) {
            Log.e("GeofenceSilentManager", "Failed to restore ringer on departure", e)
            return false
        }
    }

    companion object {
        const val KEY_ORIGINAL_RINGER = "key_original_ringer_mode"
    }
}
