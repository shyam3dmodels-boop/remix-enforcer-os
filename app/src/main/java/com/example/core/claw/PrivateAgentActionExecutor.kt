package com.example.core.claw

import android.content.Context
import android.util.Log
import kotlinx.coroutines.delay
import org.json.JSONObject

/**
 * Universal Action Execution Engine for PrivateAgent.
 *
 * Implements full execution of:
 * - click / tap (coordinates or text / ID)
 * - type / paste text
 * - press_key (home, back, recents, enter)
 * - scroll (up, down, left, right)
 * - launch_app
 * - toggle_flashlight, adjust_volume, open_wifi, open_bluetooth
 * - call_phone, send_sms
 */
class PrivateAgentActionExecutor private constructor(private val context: Context) {

    private val hardwareController = PrivateAgentHardwareController.getInstance(context)
    private val telephonyDispatcher = PrivateAgentTelephonyDispatcher.getInstance(context)
    private val feedbackManager = PrivateAgentFeedbackManager.getInstance(context)

    suspend fun executeAction(actionJson: JSONObject): ExecutionResult {
        val actionType = actionJson.optString("action", "").lowercase().trim()
        val params = actionJson.optJSONObject("params") ?: JSONObject()
        val service = MobileClawAccessibilityService.instance

        Log.i(TAG, "Executing Action: $actionType with params: $params")
        feedbackManager.triggerStepHaptic()

        return try {
            when (actionType) {
                "click", "tap" -> {
                    val x = params.optDouble("x", -1.0).toFloat()
                    val y = params.optDouble("y", -1.0).toFloat()
                    val text = params.optString("text", "")
                    val resId = params.optString("res_id", "")

                    if (x >= 0 && y >= 0 && service != null) {
                        service.tap(x, y)
                        delay(600)
                        ExecutionResult(true, "Tapped coordinates ($x, $y)")
                    } else if (text.isNotBlank() && service != null) {
                        val clicked = service.clickByText(text)
                        delay(600)
                        ExecutionResult(clicked, if (clicked) "Clicked element with text '$text'" else "Could not find text '$text'")
                    } else if (resId.isNotBlank() && service != null) {
                        val clicked = service.clickByViewId(resId)
                        delay(600)
                        ExecutionResult(clicked, if (clicked) "Clicked element with resId '$resId'" else "Could not find resId '$resId'")
                    } else {
                        ExecutionResult(false, "Invalid click parameters or Accessibility Service inactive")
                    }
                }

                "type", "input_text" -> {
                    val text = params.optString("text", "")
                    val clearFirst = params.optBoolean("clear_first", false)
                    if (service != null && text.isNotBlank()) {
                        service.typeText(text)
                        delay(800)
                        ExecutionResult(true, "Typed text: \"$text\"")
                    } else {
                        ExecutionResult(false, "Accessibility service inactive or empty text")
                    }
                }

                "press_key", "key_event" -> {
                    val key = params.optString("key", "").lowercase().trim()
                    if (service == null) return ExecutionResult(false, "Service inactive")

                    val success = when (key) {
                        "home" -> service.pressHome()
                        "back" -> service.pressBack()
                        "recents", "overview" -> service.pressRecents()
                        "enter", "search", "submit" -> {
                            // Tap IME enter coordinate on bottom right
                            val (w, h) = service.getScreenDimensions()
                            service.tap(w * 0.90f, h * 0.92f)
                            true
                        }
                        else -> false
                    }
                    delay(500)
                    ExecutionResult(success, "Pressed key '$key'")
                }

                "scroll" -> {
                    if (service == null) return ExecutionResult(false, "Service inactive")
                    val direction = params.optString("direction", "down").lowercase()
                    val amount = params.optDouble("amount", 0.6).toFloat()

                    when (direction) {
                        "up" -> service.scrollUp(amount)
                        "down" -> service.scrollDown(amount)
                        "left" -> service.scrollLeft(amount)
                        "right" -> service.scrollRight(amount)
                        else -> service.scrollDown(amount)
                    }
                    delay(800)
                    ExecutionResult(true, "Scrolled $direction ($amount)")
                }

                "launch_app", "open_app" -> {
                    val pkg = params.optString("package", "")
                    val appName = params.optString("app_name", "").lowercase()
                    val resolvedPkg = if (pkg.isNotBlank()) {
                        pkg
                    } else {
                        resolvePackageForName(appName)
                    }

                    if (service != null && resolvedPkg.isNotBlank()) {
                        service.launchApp(resolvedPkg)
                        delay(2000)
                        ExecutionResult(true, "Launched app: $resolvedPkg")
                    } else {
                        ExecutionResult(false, "Could not resolve application package for: $appName")
                    }
                }

                "toggle_flashlight", "torch" -> {
                    val turnOn = if (params.has("enabled")) params.optBoolean("enabled") else null
                    val ok = hardwareController.toggleFlashlight(turnOn)
                    ExecutionResult(ok, if (ok) "Toggled flashlight" else "Failed to toggle flashlight")
                }

                "adjust_volume", "volume" -> {
                    val level = params.optString("level", "up")
                    val ok = hardwareController.adjustVolume(level)
                    ExecutionResult(ok, if (ok) "Adjusted volume: $level" else "Failed to adjust volume")
                }

                "open_wifi" -> {
                    val ok = hardwareController.openWifiSettings()
                    ExecutionResult(ok, "Opened WiFi settings")
                }

                "open_bluetooth" -> {
                    val ok = hardwareController.openBluetoothSettings()
                    ExecutionResult(ok, "Opened Bluetooth settings")
                }

                "call", "call_phone" -> {
                    val number = params.optString("number", "")
                    val direct = params.optBoolean("direct", false)
                    val ok = telephonyDispatcher.callNumber(number, direct)
                    ExecutionResult(ok, "Initiated call to $number")
                }

                "send_sms", "sms" -> {
                    val number = params.optString("number", "")
                    val text = params.optString("text", "")
                    val ok = telephonyDispatcher.sendSms(number, text)
                    ExecutionResult(ok, "Opened SMS to $number")
                }

                "finish", "complete" -> {
                    feedbackManager.triggerSuccessHaptic()
                    val message = params.optString("message", "Task successfully completed.")
                    ExecutionResult(true, message, isFinished = true)
                }

                else -> {
                    ExecutionResult(false, "Unknown action type: $actionType")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing action $actionType", e)
            ExecutionResult(false, "Action Error: ${e.message}")
        }
    }

    private fun resolvePackageForName(name: String): String {
        return when {
            name.contains("youtube") -> "com.google.android.youtube"
            name.contains("whatsapp") -> "com.whatsapp"
            name.contains("telegram") -> "org.telegram.messenger"
            name.contains("settings") -> "com.android.settings"
            name.contains("chrome") -> "com.android.chrome"
            name.contains("camera") -> "com.google.android.GoogleCamera"
            name.contains("maps") -> "com.google.android.apps.maps"
            name.contains("spotify") -> "com.spotify.music"
            name.contains("instagram") -> "com.instagram.android"
            name.contains("twitter") || name.contains("x") -> "com.twitter.android"
            name.contains("dialer") || name.contains("phone") -> "com.google.android.dialer"
            name.contains("message") || name.contains("sms") -> "com.google.android.apps.messaging"
            else -> name
        }
    }

    data class ExecutionResult(
        val isSuccess: Boolean,
        val message: String,
        val isFinished: Boolean = false
    )

    companion object {
        private const val TAG = "PrivateAgentExecutor"

        @Volatile
        private var instance: PrivateAgentActionExecutor? = null

        fun getInstance(context: Context): PrivateAgentActionExecutor {
            return instance ?: synchronized(this) {
                instance ?: PrivateAgentActionExecutor(context).also { instance = it }
            }
        }
    }
}
