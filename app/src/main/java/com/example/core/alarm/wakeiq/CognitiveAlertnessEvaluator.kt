package com.example.core.alarm.wakeiq

import androidx.compose.ui.graphics.Color
import kotlin.random.Random

/**
 * Post-awakening cognitive arousal and alertness evaluator inspired by WakeIQ.
 * Implements the Stroop Color-Word test to evaluate prefrontal cortex activation upon waking.
 */
class CognitiveAlertnessEvaluator {

    data class StroopChallenge(
        val word: String,
        val displayColor: Color,
        val colorName: String,
        val options: List<String>
    )

    private val colorPairs = listOf(
        Pair("RED", Color(0xFFEF4444)),
        Pair("BLUE", Color(0xFF3B82F6)),
        Pair("GREEN", Color(0xFF10B981)),
        Pair("YELLOW", Color(0xFFEAB308)),
        Pair("PURPLE", Color(0xFFA855F7))
    )

    fun generateStroopTest(): StroopChallenge {
        val wordPair = colorPairs.random()
        var colorPair = colorPairs.random()
        // Ensure word and ink color conflict for high cognitive interference test
        while (colorPair.first == wordPair.first) {
            colorPair = colorPairs.random()
        }

        val correctColorName = colorPair.first
        val otherOptions = colorPairs.filter { it.first != correctColorName }.shuffled().take(3).map { it.first }
        val allOptions = (otherOptions + correctColorName).shuffled()

        return StroopChallenge(
            word = wordPair.first,
            displayColor = colorPair.second,
            colorName = correctColorName,
            options = allOptions
        )
    }

    /**
     * Calculates alertness score (0-100%) based on reaction time (ms) and accuracy.
     */
    fun computeAlertnessScore(reactionTimeMs: Long, isCorrect: Boolean): Int {
        if (!isCorrect) return 30
        return when {
            reactionTimeMs < 1200 -> 100
            reactionTimeMs < 2000 -> 85
            reactionTimeMs < 3500 -> 70
            reactionTimeMs < 5000 -> 55
            else -> 40
        }
    }
}
