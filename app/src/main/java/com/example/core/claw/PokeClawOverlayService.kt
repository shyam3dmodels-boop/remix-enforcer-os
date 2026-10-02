package com.example.core.claw

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * PokeClaw Interactive Floating Overlay Service.
 *
 * Inspired by:
 * - agents-io/PokeClaw
 * - HenryZ838978/pocketclaw
 *
 * Provides:
 * 1. Movable companion avatar bubble on screen.
 * 2. Real-time ReAct AI Thought stream banner.
 * 3. Visual click marker ripples & target element bounding boxes.
 * 4. Emergency Kill Switch / Pause button.
 */
class PokeClawOverlayService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)
    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var params: WindowManager.LayoutParams? = null

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        _isOverlayShowing.value = true
        showFloatingBubble()
        Log.i(TAG, "🟢 PokeClaw Floating Overlay Service initialized")
    }

    private fun showFloatingBubble() {
        if (floatingView != null) return

        windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 50
            y = 200
        }

        // Programmatic overlay view container
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12, 12, 12, 12)
        }

        // 1. Companion Avatar Bubble + Emergency Pause button
        val bubbleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val avatarText = TextView(this).apply {
            text = "🐾"
            textSize = 28f
            setPadding(14, 14, 14, 14)
            setBackgroundColor(0xCC0F172A.toInt()) // Frosted Slate
        }

        val statusText = TextView(this).apply {
            text = "PokeClaw AI"
            textSize = 11f
            setTextColor(0xFF38BDF8.toInt())
            setPadding(12, 6, 12, 6)
            setBackgroundColor(0xEE1E293B.toInt())
        }

        val killBtn = TextView(this).apply {
            text = "⏹ STOP"
            textSize = 10f
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(10, 6, 10, 6)
            setBackgroundColor(0xEEEF4444.toInt())
            setOnClickListener {
                stopAutomationEmergency()
            }
        }

        bubbleRow.addView(avatarText)
        bubbleRow.addView(statusText)
        bubbleRow.addView(killBtn)

        // 2. Live ReAct Thought Bubble
        val thoughtBox = TextView(this).apply {
            id = ID_THOUGHT_TEXT
            text = "Awaiting user directive..."
            textSize = 10f
            setTextColor(0xFFE2E8F0.toInt())
            setPadding(12, 8, 12, 8)
            setBackgroundColor(0xDD0B0F19.toInt())
        }

        rootLayout.addView(bubbleRow)
        rootLayout.addView(thoughtBox)
        floatingView = rootLayout

        // Drag & Touch Listener
        avatarText.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params?.x ?: 0
                    initialY = params?.y ?: 0
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params?.let {
                        it.x = initialX + (event.rawX - initialTouchX).toInt()
                        it.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager?.updateViewLayout(floatingView, it)
                    }
                    true
                }
                else -> false
            }
        }

        try {
            windowManager?.addView(floatingView, params)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add floating window overlay", e)
        }
    }

    fun updateThought(text: String, stageAvatar: String = "🐾") {
        scope.launch {
            floatingView?.let { root ->
                val thoughtView = root.findViewById<TextView>(ID_THOUGHT_TEXT)
                thoughtView?.text = text
            }
            _currentThought.value = text
        }
    }

    fun stopAutomationEmergency() {
        Log.w(TAG, "🚨 Emergency Stop triggered from PokeClaw floating bubble")
        MobileClawReActRunner.instance?.cancelAll()
        updateThought("⏹ Automation Stopped by user.")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (floatingView != null && windowManager != null) {
            try {
                windowManager?.removeView(floatingView)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing floating view", e)
            }
        }
        floatingView = null
        instance = null
        _isOverlayShowing.value = false
        job.cancel()
        Log.i(TAG, "🔴 PokeClaw Floating Overlay Service destroyed")
    }

    companion object {
        private const val TAG = "PokeClawOverlay"
        private const val ID_THOUGHT_TEXT = 10091

        var instance: PokeClawOverlayService? = null
            private set

        private val _isOverlayShowing = MutableStateFlow(false)
        val isOverlayShowing: StateFlow<Boolean> = _isOverlayShowing.asStateFlow()

        private val _currentThought = MutableStateFlow("Ready")
        val currentThought: StateFlow<String> = _currentThought.asStateFlow()

        fun start(context: Context) {
            try {
                val intent = Intent(context, PokeClawOverlayService::class.java)
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start PokeClawOverlayService", e)
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, PokeClawOverlayService::class.java)
                context.stopService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop PokeClawOverlayService", e)
            }
        }
    }
}
