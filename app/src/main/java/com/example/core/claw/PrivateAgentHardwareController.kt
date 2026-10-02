package com.example.core.claw

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.util.Log

/**
 * System Hardware Controller for PrivateAgent.
 *
 * Provides direct actions for:
 * 1. Flashlight / Torch toggle.
 * 2. Volume levels (Media, Ring, Alarm, Mute).
 * 3. WiFi settings & state toggle.
 * 4. Bluetooth settings & state toggle.
 * 5. Quick navigation to Android Settings panels.
 */
class PrivateAgentHardwareController private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val cameraManager = appContext.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var isTorchOn = false

    /**
     * Toggles the device camera flashlight / torch.
     */
    fun toggleFlashlight(turnOn: Boolean? = null): Boolean {
        if (cameraManager == null) {
            Log.w(TAG, "CameraManager not available for Torch")
            return false
        }

        return try {
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return false

            val newState = turnOn ?: !isTorchOn
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cameraManager.setTorchMode(cameraId, newState)
                isTorchOn = newState
                Log.i(TAG, "Torch state changed to: $newState")
                true
            } else {
                false
            }
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Failed to toggle torch", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error toggling torch", e)
            false
        }
    }

    /**
     * Adjusts the device media or ring volume (e.g. "up", "down", "mute", or 0-100 percentage).
     */
    fun adjustVolume(directionOrPercent: String): Boolean {
        if (audioManager == null) return false

        return try {
            when (directionOrPercent.lowercase().trim()) {
                "up", "raise", "increase" -> {
                    audioManager.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_RAISE,
                        AudioManager.FLAG_SHOW_UI
                    )
                    true
                }
                "down", "lower", "decrease" -> {
                    audioManager.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_LOWER,
                        AudioManager.FLAG_SHOW_UI
                    )
                    true
                }
                "mute", "silent" -> {
                    audioManager.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_MUTE,
                        AudioManager.FLAG_SHOW_UI
                    )
                    true
                }
                "unmute" -> {
                    audioManager.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_UNMUTE,
                        AudioManager.FLAG_SHOW_UI
                    )
                    true
                }
                else -> {
                    val percent = directionOrPercent.filter { it.isDigit() }.toIntOrNull()
                    if (percent != null && percent in 0..100) {
                        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                        val targetVol = (maxVol * (percent / 100.0)).toInt()
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, AudioManager.FLAG_SHOW_UI)
                        true
                    } else {
                        false
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adjusting volume", e)
            false
        }
    }

    /**
     * Launches the WiFi Settings panel or toggles WiFi.
     */
    fun openWifiSettings(): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            appContext.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error opening WiFi settings", e)
            false
        }
    }

    /**
     * Launches the Bluetooth Settings panel.
     */
    fun openBluetoothSettings(): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            appContext.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error opening Bluetooth settings", e)
            false
        }
    }

    companion object {
        private const val TAG = "PrivateAgentHardware"

        @Volatile
        private var instance: PrivateAgentHardwareController? = null

        fun getInstance(context: Context): PrivateAgentHardwareController {
            return instance ?: synchronized(this) {
                instance ?: PrivateAgentHardwareController(context).also { instance = it }
            }
        }
    }
}
