package com.example.core.alarm.captcha

import kotlin.random.Random

/**
 * Visual memory matrix challenge generator inspired by Sleep as Android & WakeIQ.
 * Generates spatial sequences on a 3x3 grid that the user must memorize and repeat.
 */
class MemoryMatrixCaptcha(
    val gridSize: Int = 9,
    val sequenceLength: Int = 4
) {
    private val _sequence = mutableListOf<Int>()
    val sequence: List<Int> get() = _sequence

    private val _userPicks = mutableListOf<Int>()
    val userPicks: List<Int> get() = _userPicks

    init {
        generateNewSequence()
    }

    fun generateNewSequence() {
        _sequence.clear()
        _userPicks.clear()
        val available = (0 until gridSize).toMutableList()
        for (i in 0 until sequenceLength) {
            val pick = available.random()
            _sequence.add(pick)
            // allow repetition or distinct depending on sequence
        }
    }

    /**
     * Records user click on a tile index (0 to gridSize-1).
     * @return Pair(isCorrectSoFar, isComplete)
     */
    fun onTileClicked(index: Int): Pair<Boolean, Boolean> {
        _userPicks.add(index)
        val currentIndex = _userPicks.size - 1

        if (currentIndex >= _sequence.size || _sequence[currentIndex] != index) {
            // Wrong pick
            return Pair(false, false)
        }

        val isComplete = _userPicks.size == _sequence.size
        return Pair(true, isComplete)
    }

    fun resetUserPicks() {
        _userPicks.clear()
    }
}
