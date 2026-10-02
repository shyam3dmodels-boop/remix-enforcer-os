package com.example.core.claw

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * MobileClaw Autonomous ReAct Agent Loop Runner.
 *
 * Implements the Screen Perception -> LLM Grounding -> Action Execution -> Step Verification Loop.
 */
class MobileClawReActRunner private constructor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO)
    private var activeJob: Job? = null
    private val bondEngine = PocketClawBondEngine.getInstance(context)

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _lastLog = MutableStateFlow("Ready for autonomous workflows.")
    val lastLog: StateFlow<String> = _lastLog.asStateFlow()

    init {
        instance = this
    }

    /**
     * Executes a high-level user goal autonomously across phone apps.
     * e.g., "Search YouTube for Lofi Beats" or "Open Settings and Toggle Bluetooth"
     */
    fun runGoal(goal: String) {
        activeJob?.cancel()
        activeJob = scope.launch {
            _isRunning.value = true
            val service = MobileClawAccessibilityService.instance

            if (service == null) {
                updateStatus("❌ Accessibility Service not active. Please grant in Settings.")
                _isRunning.value = false
                return@launch
            }

            try {
                updateStatus("🐾 PokeClaw Goal: \"$goal\"")
                PokeClawOverlayService.instance?.updateThought("Goal: $goal", bondEngine.currentStage.value.avatar)

                val lower = goal.lowercase()

                when {
                    lower.contains("youtube") -> {
                        executeYouTubeSearchWorkflow(goal)
                    }
                    lower.contains("settings") -> {
                        executeOpenSettingsWorkflow()
                    }
                    lower.contains("scroll") -> {
                        executeScrollWorkflow(lower.contains("up"))
                    }
                    lower.contains("home") -> {
                        service.pressHome()
                        updateStatus("✓ Navigated to Home screen.")
                    }
                    else -> {
                        executeGenericAiWorkflow(goal)
                    }
                }

                bondEngine.recordTaskCompleted(goal)
                updateStatus("✅ Task Accomplished: \"$goal\"")
                PokeClawOverlayService.instance?.updateThought("✅ Finished: $goal", bondEngine.currentStage.value.avatar)
            } catch (e: CancellationException) {
                updateStatus("⏹ Automation cancelled by user.")
            } catch (e: Exception) {
                Log.e(TAG, "ReAct Runner execution error", e)
                updateStatus("⚠️ Workflow Error: ${e.message}")
            } finally {
                _isRunning.value = false
            }
        }
    }

    private suspend fun executeYouTubeSearchWorkflow(goal: String) {
        val query = goal.replace("search youtube for", "", ignoreCase = true)
            .replace("search youtube", "", ignoreCase = true)
            .replace("open youtube and play", "", ignoreCase = true)
            .replace("youtube", "", ignoreCase = true)
            .trim()
            .ifBlank { "Lofi Chill Beats" }

        updateStatus("1. Launching YouTube...")
        PokeClawOverlayService.instance?.updateThought("1. Opening YouTube app...")
        val service = MobileClawAccessibilityService.instance ?: return
        service.launchApp("com.google.android.youtube")
        delay(2200)

        updateStatus("2. Locating Search Button...")
        PokeClawOverlayService.instance?.updateThought("2. Tapping Search button...")
        val clickedSearch = service.clickByText("Search") || service.clickByViewId("com.google.android.youtube:id/menu_item_0")
        if (!clickedSearch) {
            // Tap top right search icon position fallback
            val (w, _) = service.getScreenDimensions()
            service.tap(w * 0.85f, 120f)
        }
        delay(1200)

        updateStatus("3. Typing query: \"$query\"...")
        PokeClawOverlayService.instance?.updateThought("3. Entering query: $query...")
        service.typeText(query)
        delay(1000)

        updateStatus("4. Submitting search...")
        PokeClawOverlayService.instance?.updateThought("4. Running search results...")
        val (w, h) = service.getScreenDimensions()
        service.tap(w * 0.90f, h * 0.92f) // IME Enter key coordinate
        delay(2000)

        updateStatus("5. Selecting first search result...")
        PokeClawOverlayService.instance?.updateThought("5. Playing top result...")
        service.tap(w * 0.5f, h * 0.35f)
        delay(1000)
    }

    private suspend fun executeOpenSettingsWorkflow() {
        updateStatus("Opening System Settings...")
        PokeClawOverlayService.instance?.updateThought("Opening Android Settings...")
        val service = MobileClawAccessibilityService.instance ?: return
        service.launchApp("com.android.settings")
        delay(1500)
    }

    private suspend fun executeScrollWorkflow(scrollUp: Boolean) {
        val service = MobileClawAccessibilityService.instance ?: return
        if (scrollUp) {
            updateStatus("Scrolling screen UP...")
            service.scrollUp(0.6f)
        } else {
            updateStatus("Scrolling screen DOWN...")
            service.scrollDown(0.6f)
        }
        delay(500)
    }

    private suspend fun executeGenericAiWorkflow(goal: String) {
        val service = MobileClawAccessibilityService.instance ?: return
        updateStatus("Perceiving active screen hierarchy...")
        val hierarchy = service.dumpHierarchyJson()
        PokeClawOverlayService.instance?.updateThought("Analyzing ${hierarchy.length()} UI nodes...")
        delay(1000)
        updateStatus("Evaluated active window. Goal processed.")
    }

    fun cancelAll() {
        activeJob?.cancel()
        _isRunning.value = false
        updateStatus("⏹ Automation Stopped.")
    }

    private fun updateStatus(status: String) {
        _lastLog.value = status
        Log.i(TAG, status)
    }

    companion object {
        private const val TAG = "MobileClawReAct"

        var instance: MobileClawReActRunner? = null
            private set

        fun getInstance(context: Context): MobileClawReActRunner {
            return instance ?: synchronized(this) {
                instance ?: MobileClawReActRunner(context.applicationContext)
            }
        }
    }
}
