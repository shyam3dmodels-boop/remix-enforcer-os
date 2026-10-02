package com.example.core.claw

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Draggable Edge-Snapping Floating Bubble & Expandable Chat HUD Service.
 *
 * Implements:
 * 1. Smooth edge-snapping circular companion bubble.
 * 2. Expandable floating chat panel with ReAct live thought stream & logs.
 * 3. Quick goal execution input field with instant stop emergency switch.
 * 4. Self-overlay exclusion tagging.
 */
class PrivateAgentFloatingBubbleService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)
    private var windowManager: WindowManager? = null

    private var bubbleView: View? = null
    private var panelView: View? = null
    private var bubbleParams: WindowManager.LayoutParams? = null
    private var panelParams: WindowManager.LayoutParams? = null

    private var isExpanded = false
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    private lateinit var reactEngine: PrivateAgentReActEngine

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        _isBubbleShowing.value = true
        reactEngine = PrivateAgentReActEngine.getInstance(this)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager

        createBubbleView()
        createPanelView()
        observeEngineState()

        Log.i(TAG, "🟢 PrivateAgent Floating Bubble Service active")
    }

    private fun getOverlayLayoutFlag(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
    }

    private fun createBubbleView() {
        bubbleParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            getOverlayLayoutFlag(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 30
            y = 350
        }

        val bubbleLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(8, 8, 8, 8)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xEE0F172A.toInt()) // Frosted Dark Slate
                setStroke(3, 0xFF38BDF8.toInt()) // Cyan rim
            }
        }

        val avatarText = TextView(this).apply {
            text = "⚡"
            textSize = 24f
            gravity = Gravity.CENTER
            setPadding(12, 12, 12, 12)
        }

        bubbleLayout.addView(avatarText)
        bubbleView = bubbleLayout

        // Drag & Edge-Snapping Touch Listener
        bubbleLayout.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = bubbleParams?.x ?: 0
                    initialY = bubbleParams?.y ?: 0
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    bubbleParams?.let {
                        it.x = initialX + (event.rawX - initialTouchX).toInt()
                        it.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager?.updateViewLayout(bubbleView, it)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val diffX = Math.abs(event.rawX - initialTouchX)
                    val diffY = Math.abs(event.rawY - initialTouchY)
                    if (diffX < 15 && diffY < 15) {
                        // Click detected -> Toggle Expand/Collapse
                        toggleExpand()
                    } else {
                        // Snap to left or right edge
                        snapToEdge()
                    }
                    true
                }
                else -> false
            }
        }

        try {
            windowManager?.addView(bubbleView, bubbleParams)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add bubble view", e)
        }
    }

    private fun snapToEdge() {
        val wm = windowManager ?: return
        val display = wm.defaultDisplay
        val metrics = DisplayMetrics()
        display.getMetrics(metrics)
        val screenWidth = metrics.widthPixels

        bubbleParams?.let { params ->
            val midX = screenWidth / 2
            params.x = if (params.x + 50 < midX) 20 else screenWidth - 140
            windowManager?.updateViewLayout(bubbleView, params)
        }
    }

    private fun createPanelView() {
        panelParams = WindowManager.LayoutParams(
            (resources.displayMetrics.widthPixels * 0.88f).toInt(),
            (resources.displayMetrics.heightPixels * 0.45f).toInt(),
            getOverlayLayoutFlag(),
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        val rootPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 20)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 32f
                setColor(0xF00B0F19.toInt()) // Frosted Onyx
                setStroke(2, 0xFF38BDF8.toInt())
            }
        }

        // Header Row
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, 12)
        }

        val title = TextView(this).apply {
            text = "⚡ PrivateAgent HUD"
            textSize = 15f
            setTextColor(0xFF38BDF8.toInt())
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val stopBtn = TextView(this).apply {
            text = "⏹ STOP"
            textSize = 11f
            setTextColor(Color.WHITE)
            setPadding(16, 8, 16, 8)
            background = GradientDrawable().apply {
                cornerRadius = 16f
                setColor(0xFFEF4444.toInt())
            }
            setOnClickListener {
                reactEngine.stop()
            }
        }

        val closeBtn = TextView(this).apply {
            text = " ✕ "
            textSize = 16f
            setTextColor(0xFF94A3B8.toInt())
            setPadding(12, 4, 12, 4)
            setOnClickListener {
                collapse()
            }
        }

        header.addView(title)
        header.addView(stopBtn)
        header.addView(closeBtn)

        // Thought Stream Display
        val thoughtCard = TextView(this).apply {
            id = ID_THOUGHT_TEXT
            text = "Awaiting natural language instruction..."
            textSize = 12f
            setTextColor(0xFFE2E8F0.toInt())
            setPadding(16, 12, 16, 12)
            background = GradientDrawable().apply {
                cornerRadius = 18f
                setColor(0xFF1E293B.toInt())
            }
        }

        // Log Console
        val logScroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            setPadding(0, 10, 0, 10)
        }

        val logView = TextView(this).apply {
            id = ID_LOG_TEXT
            text = "Agent ready."
            textSize = 10f
            setTextColor(0xFF94A3B8.toInt())
            setLineSpacing(4f, 1f)
        }
        logScroll.addView(logView)

        // Input Row
        val inputRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 10, 0, 0)
        }

        val inputField = EditText(this).apply {
            id = ID_INPUT_FIELD
            hint = "e.g. Open YouTube & play jazz"
            setHintTextColor(0xFF64748B.toInt())
            setTextColor(Color.WHITE)
            textSize = 12f
            setPadding(16, 10, 16, 10)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            background = GradientDrawable().apply {
                cornerRadius = 18f
                setColor(0xFF1E293B.toInt())
            }
        }

        val sendBtn = TextView(this).apply {
            text = "▶ RUN"
            textSize = 12f
            setTextColor(Color.BLACK)
            setPadding(20, 10, 20, 10)
            background = GradientDrawable().apply {
                cornerRadius = 18f
                setColor(0xFF38BDF8.toInt())
            }
            setOnClickListener {
                val goal = inputField.text.toString().trim()
                if (goal.isNotBlank()) {
                    reactEngine.startGoal(goal)
                    inputField.setText("")
                }
            }
        }

        inputRow.addView(inputField)
        inputRow.addView(sendBtn)

        rootPanel.addView(header)
        rootPanel.addView(thoughtCard)
        rootPanel.addView(logScroll)
        rootPanel.addView(inputRow)

        panelView = rootPanel
    }

    private fun toggleExpand() {
        if (isExpanded) {
            collapse()
        } else {
            expand()
        }
    }

    private fun expand() {
        if (isExpanded || panelView == null) return
        try {
            windowManager?.addView(panelView, panelParams)
            isExpanded = true
        } catch (e: Exception) {
            Log.e(TAG, "Error showing expanded panel", e)
        }
    }

    private fun collapse() {
        if (!isExpanded || panelView == null) return
        try {
            windowManager?.removeView(panelView)
            isExpanded = false
        } catch (e: Exception) {
            Log.e(TAG, "Error removing expanded panel", e)
        }
    }

    private fun observeEngineState() {
        scope.launch {
            reactEngine.currentThought.collect { thought ->
                panelView?.findViewById<TextView>(ID_THOUGHT_TEXT)?.text = thought
            }
        }

        scope.launch {
            reactEngine.executionLogs.collect { logs ->
                panelView?.findViewById<TextView>(ID_LOG_TEXT)?.text = logs.takeLast(10).joinToString("\n")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        collapse()
        if (bubbleView != null && windowManager != null) {
            try {
                windowManager?.removeView(bubbleView)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing bubble view", e)
            }
        }
        bubbleView = null
        instance = null
        _isBubbleShowing.value = false
        job.cancel()
        Log.i(TAG, "🔴 PrivateAgent Floating Bubble Service destroyed")
    }

    companion object {
        private const val TAG = "PrivateAgentBubble"
        private const val ID_THOUGHT_TEXT = 2001
        private const val ID_LOG_TEXT = 2002
        private const val ID_INPUT_FIELD = 2003

        var instance: PrivateAgentFloatingBubbleService? = null
            private set

        private val _isBubbleShowing = MutableStateFlow(false)
        val isBubbleShowing: StateFlow<Boolean> = _isBubbleShowing.asStateFlow()

        fun start(context: Context) {
            try {
                val intent = Intent(context, PrivateAgentFloatingBubbleService::class.java)
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start PrivateAgentFloatingBubbleService", e)
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, PrivateAgentFloatingBubbleService::class.java)
                context.stopService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop PrivateAgentFloatingBubbleService", e)
            }
        }
    }
}
