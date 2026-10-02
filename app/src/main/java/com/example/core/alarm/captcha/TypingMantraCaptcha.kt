package com.example.core.alarm.captcha

import kotlin.random.Random

/**
 * Text typing captcha requiring conscious typing of anti-procrastination mantras.
 * Inspired by Sleep as Android typing captcha.
 */
class TypingMantraCaptcha {

    private val mantras = listOf(
        "I am awake, focused, and ready to conquer my highest priorities today.",
        "Discipline is choosing between what you want now and what you want most.",
        "Rise and execute with relentless clarity and high energy.",
        "My future is created by what I do today, not tomorrow.",
        "Consciousness verified. Zero excuses, maximum execution."
    )

    var currentMantra: String = getRandomMantra()
        private set

    fun getNewMantra(): String {
        currentMantra = mantras.random()
        return currentMantra
    }

    private fun getRandomMantra(): String = mantras.random()

    /**
     * Checks if the typed text strictly matches the target mantra.
     */
    fun validate(input: String): Boolean {
        return input.trim() == currentMantra.trim()
    }

    /**
     * Returns matching character count progress (0.0 to 1.0).
     */
    fun calculateMatchProgress(input: String): Float {
        if (currentMantra.isEmpty()) return 0f
        var matchCount = 0
        val minLen = minOf(input.length, currentMantra.length)
        for (i in 0 until minLen) {
            if (input[i] == currentMantra[i]) {
                matchCount++
            } else {
                break
            }
        }
        return matchCount.toFloat() / currentMantra.length.toFloat()
    }
}
