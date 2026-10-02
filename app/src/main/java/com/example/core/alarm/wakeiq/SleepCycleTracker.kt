package com.example.core.alarm.wakeiq

/**
 * Ultradian 90-minute sleep cycle calculator and sleep debt analyzer inspired by WakeIQ.
 */
class SleepCycleTracker {

    data class SleepRecommendation(
        val cycleCount: Int,
        val totalMinutes: Int,
        val wakeTimestamp: Long,
        val label: String
    )

    /**
     * Calculates optimal wakeup timestamps starting from bedtimeMillis.
     * Each cycle is ~90 minutes plus optional sleep onset latency (e.g. 15 mins).
     */
    fun calculateWakeOptions(
        bedtimeMillis: Long,
        onsetLatencyMinutes: Int = 15
    ): List<SleepRecommendation> {
        val options = mutableListOf<SleepRecommendation>()
        val cycleMinutes = 90

        // 3 cycles (4.5h - Nap), 4 cycles (6h - Core Enforcer), 5 cycles (7.5h - Optimal), 6 cycles (9h - Full Recovery)
        val cycleCounts = listOf(3, 4, 5, 6)
        val labels = listOf("Power Reset (4.5h)", "Enforcer Baseline (6.0h)", "Optimal Cognitive (7.5h)", "Full Recovery (9.0h)")

        for (i in cycleCounts.indices) {
            val count = cycleCounts[i]
            val durationMinutes = onsetLatencyMinutes + (count * cycleMinutes)
            val wakeTime = bedtimeMillis + (durationMinutes * 60 * 1000L)
            options.add(
                SleepRecommendation(
                    cycleCount = count,
                    totalMinutes = durationMinutes,
                    wakeTimestamp = wakeTime,
                    label = labels[i]
                )
            )
        }

        return options
    }

    /**
     * Evaluates sleep score based on duration and schedule consistency.
     */
    fun calculateSleepScore(actualSleepMinutes: Int, targetMinutes: Int = 360): Int {
        val ratio = actualSleepMinutes.toFloat() / targetMinutes.toFloat()
        return when {
            ratio >= 0.95f && ratio <= 1.25f -> 100
            ratio >= 0.80f -> (ratio * 100).toInt()
            ratio >= 0.60f -> 65
            else -> 40
        }
    }
}
