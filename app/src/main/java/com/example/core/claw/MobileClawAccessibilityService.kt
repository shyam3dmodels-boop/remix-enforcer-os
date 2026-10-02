package com.example.core.claw

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.WindowManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * MobileClaw Accessibility Service for Secondary Brain 2.0.
 *
 * Implements full mobile automation agent capabilities inspired by:
 * - ChenKuanSun/MobileClaw
 * - wamynobe/mobclaw
 *
 * Provides remote programmatic tapping, swiping, typing, element clicking,
 * UI hierarchy inspection (readScreen/dumpHierarchy), and system navigation.
 */
class MobileClawAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isServiceActive.value = true
        Log.i(TAG, "🟢 MobileClaw Accessibility Agent Service connected and operational")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkg = event.packageName?.toString()
        if (!pkg.isNullOrBlank()) {
            _activePackageName.value = pkg
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "⚠️ MobileClaw Accessibility Agent interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance === this) {
            instance = null
            _isServiceActive.value = false
        }
        Log.i(TAG, "🔴 MobileClaw Accessibility Agent Service destroyed")
    }

    // ─── Gesture Dispatching (Taps & Swipes) ──────────────────────────────────

    /**
     * Dispatches a single tap gesture at screen coordinates (x, y).
     */
    fun tap(x: Float, y: Float, onComplete: ((Boolean) -> Unit)? = null): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Log.w(TAG, "dispatchGesture requires Android 7.0+ (API 24)")
            onComplete?.invoke(false)
            return false
        }

        val path = Path().apply {
            moveTo(x, y)
            lineTo(x, y)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, 50)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        return dispatchGesture(
            gesture,
            object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    Log.d(TAG, "✓ MobileClaw tap completed at ($x, $y)")
                    onComplete?.invoke(true)
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    Log.w(TAG, "✗ MobileClaw tap cancelled at ($x, $y)")
                    onComplete?.invoke(false)
                }
            },
            null
        )
    }

    /**
     * Dispatches a swipe gesture from (startX, startY) to (endX, endY) over durationMs.
     */
    fun swipe(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        durationMs: Long = 300L,
        onComplete: ((Boolean) -> Unit)? = null
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Log.w(TAG, "dispatchGesture requires Android 7.0+")
            onComplete?.invoke(false)
            return false
        }

        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        val safeDuration = durationMs.coerceIn(50L, 3000L)
        val stroke = GestureDescription.StrokeDescription(path, 0, safeDuration)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        return dispatchGesture(
            gesture,
            object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    Log.d(TAG, "✓ MobileClaw swipe completed from ($startX, $startY) -> ($endX, $endY)")
                    onComplete?.invoke(true)
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    Log.w(TAG, "✗ MobileClaw swipe cancelled from ($startX, $startY) -> ($endX, $endY)")
                    onComplete?.invoke(false)
                }
            },
            null
        )
    }

    /**
     * Scrolls down by ratio of screen height.
     */
    fun scrollDown(ratio: Float = 0.5f, onComplete: ((Boolean) -> Unit)? = null): Boolean {
        val (width, height) = getScreenDimensions()
        val startY = height * 0.75f
        val endY = height * (0.75f - ratio.coerceIn(0.1f, 0.7f))
        val x = width * 0.5f
        return swipe(x, startY, x, endY, 350L, onComplete)
    }

    /**
     * Scrolls up by ratio of screen height.
     */
    fun scrollUp(ratio: Float = 0.5f, onComplete: ((Boolean) -> Unit)? = null): Boolean {
        val (width, height) = getScreenDimensions()
        val startY = height * 0.25f
        val endY = height * (0.25f + ratio.coerceIn(0.1f, 0.7f))
        val x = width * 0.5f
        return swipe(x, startY, x, endY, 350L, onComplete)
    }

    // ─── Text Input & Keyboard Automation ─────────────────────────────────────

    /**
     * Types text into the currently focused input field.
     */
    fun typeText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val focusedNode = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)

        if (focusedNode != null && focusedNode.isEditable) {
            val args = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            val success = focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            focusedNode.recycle()
            if (success) {
                Log.d(TAG, "✓ MobileClaw typeText performed via ACTION_SET_TEXT: \"$text\"")
                return true
            }
        }

        // Fallback: Copy to clipboard and attempt paste
        return try {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("mobile_claw_input", text)
            clipboard.setPrimaryClip(clip)

            val pasteTarget = focusedNode ?: root
            val pasted = pasteTarget.performAction(AccessibilityNodeInfo.ACTION_PASTE)
            Log.d(TAG, "MobileClaw typeText clipboard paste fallback result: $pasted")
            pasted
        } catch (e: Exception) {
            Log.e(TAG, "typeText failed", e)
            false
        }
    }

    // ─── Targeted Element Clicking ────────────────────────────────────────────

    /**
     * Clicks an element containing or matching the given text string.
     */
    fun clickByText(query: String, exactMatch: Boolean = false): Boolean {
        val root = rootInActiveWindow ?: return false
        val nodes = root.findAccessibilityNodeInfosByText(query)
        if (nodes.isNullOrEmpty()) {
            Log.w(TAG, "MobileClaw: No nodes found with text: \"$query\"")
            return false
        }

        for (node in nodes) {
            val nodeText = node.text?.toString() ?: ""
            val desc = node.contentDescription?.toString() ?: ""
            val matches = if (exactMatch) {
                nodeText.equals(query, ignoreCase = true) || desc.equals(query, ignoreCase = true)
            } else {
                nodeText.contains(query, ignoreCase = true) || desc.contains(query, ignoreCase = true)
            }

            if (matches) {
                var targetNode: AccessibilityNodeInfo? = node
                while (targetNode != null && !targetNode.isClickable) {
                    targetNode = targetNode.parent
                }
                if (targetNode != null) {
                    val clicked = targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    Log.d(TAG, "✓ MobileClaw clicked element with text \"$query\": $clicked")
                    return clicked
                }
            }
        }
        return false
    }

    /**
     * Clicks an element by its resource ID (e.g. "com.example:id/submit_btn").
     */
    fun clickByViewId(viewId: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val nodes = root.findAccessibilityNodeInfosByViewId(viewId)
        if (nodes.isNullOrEmpty()) return false

        val node = nodes[0]
        var target: AccessibilityNodeInfo? = node
        while (target != null && !target.isClickable) {
            target = target.parent
        }
        val clicked = target?.performAction(AccessibilityNodeInfo.ACTION_CLICK) ?: false
        Log.d(TAG, "MobileClaw clicked by ID \"$viewId\": $clicked")
        return clicked
    }

    // ─── Screen Hierarchy Extraction (Read Screen) ────────────────────────────

    /**
     * Recursively traverses root node to produce structured JSON of the screen.
     */
    fun dumpHierarchyJson(): JSONObject {
        val json = JSONObject()
        val root = rootInActiveWindow
        if (root == null) {
            json.put("error", "Active window root node is null or inaccessible")
            json.put("package", _activePackageName.value)
            return json
        }

        val (screenWidth, screenHeight) = getScreenDimensions()
        json.put("package", root.packageName?.toString() ?: _activePackageName.value)
        json.put("screen_width", screenWidth)
        json.put("screen_height", screenHeight)
        json.put("timestamp", System.currentTimeMillis())

        val rootNodeJson = parseNodeRecursive(root)
        json.put("hierarchy", rootNodeJson)

        val flatElements = JSONArray()
        flattenInteractiveNodes(root, flatElements)
        json.put("interactive_elements", flatElements)
        json.put("interactive_count", flatElements.length())

        return json
    }

    /**
     * Produces a clean markdown/text summary of the screen for LLM / Telegram view.
     */
    fun summarizeScreen(): String {
        val dump = dumpHierarchyJson()
        val pkg = dump.optString("package", "Unknown")
        val interactiveArr = dump.optJSONArray("interactive_elements") ?: JSONArray()
        val totalInteractive = dump.optInt("interactive_count", 0)

        val sb = StringBuilder()
        sb.appendLine("📱 *MobileClaw Screen Inspection*")
        sb.appendLine("• Foreground App: `${pkg}`")
        sb.appendLine("• Interactive Elements: `$totalInteractive`")
        sb.appendLine()

        if (totalInteractive == 0) {
            sb.appendLine("ℹ️ No interactive elements detected in current window.")
        } else {
            sb.appendLine("*Detected Action Targets:*")
            val limit = interactiveArr.length().coerceAtMost(12)
            for (i in 0 until limit) {
                val item = interactiveArr.getJSONObject(i)
                val text = item.optString("text").ifBlank { item.optString("desc") }
                val bounds = item.optJSONObject("bounds")
                val cx = bounds?.optInt("center_x") ?: 0
                val cy = bounds?.optInt("center_y") ?: 0
                val type = item.optString("class").substringAfterLast(".")

                val label = if (text.isNotBlank()) "\"$text\"" else "[$type]"
                sb.appendLine("${i + 1}. $label at `($cx, $cy)`")
            }
            if (interactiveArr.length() > limit) {
                sb.appendLine("... and ${interactiveArr.length() - limit} more elements.")
            }
        }
        return sb.toString()
    }

    private fun parseNodeRecursive(node: AccessibilityNodeInfo): JSONObject {
        val obj = JSONObject()
        obj.put("class", node.className?.toString() ?: "")
        obj.put("package", node.packageName?.toString() ?: "")
        val text = node.text?.toString() ?: ""
        if (text.isNotBlank()) obj.put("text", text)

        val desc = node.contentDescription?.toString() ?: ""
        if (desc.isNotBlank()) obj.put("desc", desc)

        val viewId = node.viewIdResourceName
        if (!viewId.isNullOrBlank()) obj.put("id", viewId)

        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        val boundsJson = JSONObject().apply {
            put("left", bounds.left)
            put("top", bounds.top)
            put("right", bounds.right)
            put("bottom", bounds.bottom)
            put("center_x", bounds.centerX())
            put("center_y", bounds.centerY())
        }
        obj.put("bounds", boundsJson)
        obj.put("clickable", node.isClickable)
        obj.put("editable", node.isEditable)
        obj.put("scrollable", node.isScrollable)
        obj.put("enabled", node.isEnabled)

        val childCount = node.childCount
        if (childCount > 0) {
            val childrenArray = JSONArray()
            for (i in 0 until childCount) {
                val child = node.getChild(i)
                if (child != null) {
                    childrenArray.put(parseNodeRecursive(child))
                    child.recycle()
                }
            }
            obj.put("children", childrenArray)
        }

        return obj
    }

    private fun flattenInteractiveNodes(node: AccessibilityNodeInfo, list: JSONArray) {
        val text = node.text?.toString() ?: ""
        val desc = node.contentDescription?.toString() ?: ""
        val isInteractive = node.isClickable || node.isEditable || node.isScrollable || text.isNotBlank() || desc.isNotBlank()

        if (isInteractive) {
            val bounds = Rect()
            node.getBoundsInScreen(bounds)
            if (bounds.width() > 0 && bounds.height() > 0) {
                val item = JSONObject().apply {
                    put("class", node.className?.toString() ?: "")
                    if (text.isNotBlank()) put("text", text)
                    if (desc.isNotBlank()) put("desc", desc)
                    if (!node.viewIdResourceName.isNullOrBlank()) put("id", node.viewIdResourceName)
                    put("clickable", node.isClickable)
                    put("editable", node.isEditable)
                    put("scrollable", node.isScrollable)
                    put("bounds", JSONObject().apply {
                        put("left", bounds.left)
                        put("top", bounds.top)
                        put("right", bounds.right)
                        put("bottom", bounds.bottom)
                        put("center_x", bounds.centerX())
                        put("center_y", bounds.centerY())
                    })
                }
                list.put(item)
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                flattenInteractiveNodes(child, list)
                child.recycle()
            }
        }
    }

    // ─── System Navigation Actions ────────────────────────────────────────────

    fun pressBack(): Boolean = performGlobalAction(GLOBAL_ACTION_BACK)
    fun pressHome(): Boolean = performGlobalAction(GLOBAL_ACTION_HOME)
    fun pressRecents(): Boolean = performGlobalAction(GLOBAL_ACTION_RECENTS)
    fun openNotifications(): Boolean = performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
    fun lockScreen(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        } else {
            false
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun getScreenDimensions(): Pair<Int, Int> {
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(metrics)
        return Pair(metrics.widthPixels, metrics.heightPixels)
    }

    companion object {
        private const val TAG = "MobileClaw"

        @Volatile
        var instance: MobileClawAccessibilityService? = null
            private set

        private val _isServiceActive = MutableStateFlow(false)
        val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

        private val _activePackageName = MutableStateFlow("android")
        val activePackageName: StateFlow<String> = _activePackageName.asStateFlow()

        fun isRunning(): Boolean = instance != null
    }
}
