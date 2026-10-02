package com.example.core.claw

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * PocketClaw Bond & Evolution Engine.
 *
 * Inspired by:
 * - HenryZ838978/pocketclaw
 *
 * Implements the 5-Tier Bond Evolution System:
 * 1. Larva (0–50 XP): Basic assisted commands & passive listening.
 * 2. Hatchling (50–200 XP): Routine single-app automation & quick replies.
 * 3. Juvenile (200–500 XP): Cross-app multi-step workflows & navigation.
 * 4. Adult (500–1500 XP): High-accuracy proactive suggestions & autonomous planning.
 * 5. Elder (1500+ XP): Fully autonomous OS autopilot & deep bonded memory.
 */
class PocketClawBondEngine private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("pocketclaw_bond_prefs", Context.MODE_PRIVATE)

    enum class EvolutionStage(val title: String, val avatar: String, val minXp: Int, val description: String) {
        LARVA("Larva", "🐛", 0, "Basic assisted commands & passive learning"),
        HATCHLING("Hatchling", "🐣", 50, "Single-app routines & quick replies"),
        JUVENILE("Juvenile", "🦊", 200, "Multi-app workflows & screen vision"),
        ADULT("Adult", "🐺", 500, "High-accuracy proactive autopilot"),
        ELDER("Elder", "🐉", 1500, "Fully autonomous deep-bonded intelligence")
    }

    private val _xp = MutableStateFlow(prefs.getInt(KEY_XP, 120))
    val xp: StateFlow<Int> = _xp.asStateFlow()

    private val _tasksCompleted = MutableStateFlow(prefs.getInt(KEY_TASKS_COMPLETED, 14))
    val tasksCompleted: StateFlow<Int> = _tasksCompleted.asStateFlow()

    private val _currentStage = MutableStateFlow(calculateStage(_xp.value))
    val currentStage: StateFlow<EvolutionStage> = _currentStage.asStateFlow()

    private val _bondMemorySummary = MutableStateFlow(
        prefs.getString(KEY_BOND_MEMORY, "User prefers concise replies, fast automation, and dark mode HUD.") ?: ""
    )
    val bondMemorySummary: StateFlow<String> = _bondMemorySummary.asStateFlow()

    fun addExperience(amount: Int, reason: String = "Automated Task Completed") {
        val nextXp = _xp.value + amount
        _xp.value = nextXp
        prefs.edit().putInt(KEY_XP, nextXp).apply()

        val nextStage = calculateStage(nextXp)
        if (nextStage != _currentStage.value) {
            _currentStage.value = nextStage
            PokeClawOverlayService.instance?.updateThought("🎉 Evolved to ${nextStage.title} (${nextStage.avatar})!", nextStage.avatar)
        }
    }

    fun recordTaskCompleted(taskName: String) {
        val nextCount = _tasksCompleted.value + 1
        _tasksCompleted.value = nextCount
        prefs.edit().putInt(KEY_TASKS_COMPLETED, nextCount).apply()
        addExperience(15, "Completed: $taskName")
    }

    fun updateBondMemory(insight: String) {
        val current = _bondMemorySummary.value
        val updated = if (current.isBlank()) insight else "$current | $insight"
        _bondMemorySummary.value = updated
        prefs.edit().putString(KEY_BOND_MEMORY, updated).apply()
    }

    private fun calculateStage(xp: Int): EvolutionStage {
        return when {
            xp >= EvolutionStage.ELDER.minXp -> EvolutionStage.ELDER
            xp >= EvolutionStage.ADULT.minXp -> EvolutionStage.ADULT
            xp >= EvolutionStage.JUVENILE.minXp -> EvolutionStage.JUVENILE
            xp >= EvolutionStage.HATCHLING.minXp -> EvolutionStage.HATCHLING
            else -> EvolutionStage.LARVA
        }
    }

    companion object {
        private const val KEY_XP = "pocketclaw_xp"
        private const val KEY_TASKS_COMPLETED = "pocketclaw_tasks_completed"
        private const val KEY_BOND_MEMORY = "pocketclaw_bond_memory"

        @Volatile
        private var instance: PocketClawBondEngine? = null

        fun getInstance(context: Context): PocketClawBondEngine {
            return instance ?: synchronized(this) {
                instance ?: PocketClawBondEngine(context.applicationContext).also { instance = it }
            }
        }
    }
}
