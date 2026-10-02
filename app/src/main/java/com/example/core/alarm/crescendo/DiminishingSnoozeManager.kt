package com.example.core.alarm.crescendo

import android.content.Context
import android.content.SharedPreferences

/**
 * Diminishing Snooze Protocol manager inspired by AlarmClockXtreme.
 * Enforces strictly shrinking snooze intervals and locks out snoozing after maximum allowed attempts.
 */
class DiminishingSnoozeManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("diminishing_snooze_prefs", Context.MODE_PRIVATE)

    // Schedule: 1st snooze = 10 mins, 2nd snooze = 5 mins, 3rd snooze = 2 mins, 4th = Lockout
    private val snoozeIntervalsMinutes = listOf(10, 5, 2)
    val maxSnoozes: Int = snoozeIntervalsMinutes.size

    var currentSnoozeCount: Int
        get() = prefs.getInt(KEY_SNOOZE_COUNT, 0)
        private set(value) = prefs.edit().putInt(KEY_SNOOZE_COUNT, value).apply()

    fun canSnooze(): Boolean {
        return currentSnoozeCount < maxSnoozes
    }

    /**
     * Gets the minutes allowed for the next snooze, or 0 if locked out.
     */
    fun getNextSnoozeMinutes(): Int {
        if (!canSnooze()) return 0
        return snoozeIntervalsMinutes[currentSnoozeCount]
    }

    /**
     * Consumes one snooze and returns the delay in minutes.
     * Returns 0 if no more snoozes are allowed.
     */
    fun consumeSnooze(): Int {
        if (!canSnooze()) return 0
        val minutes = snoozeIntervalsMinutes[currentSnoozeCount]
        currentSnoozeCount++
        return minutes
    }

    fun resetSnoozeCount() {
        currentSnoozeCount = 0
    }

    companion object {
        private const val KEY_SNOOZE_COUNT = "key_current_snooze_count"
    }
}
