package com.example.core.alarm

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TorchStrobeController(private val context: Context) {
    private var strobeJob: Job? = null
    private var cameraManager: CameraManager? = null
    private var cameraId: String? = null

    init {
        try {
            cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            cameraId = cameraManager?.cameraIdList?.firstOrNull { id ->
                val characteristics = cameraManager?.getCameraCharacteristics(id)
                characteristics?.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (e: Exception) {
            Log.e("TorchStrobe", "Camera init failed", e)
        }
    }

    fun startStrobe(scope: CoroutineScope) {
        if (strobeJob?.isActive == true) return
        val mgr = cameraManager ?: return
        val camId = cameraId ?: return

        strobeJob = scope.launch(Dispatchers.Default) {
            var state = false
            while (isActive) {
                try {
                    state = !state
                    mgr.setTorchMode(camId, state)
                } catch (e: CameraAccessException) {
                    Log.e("TorchStrobe", "Torch mode error", e)
                    break
                } catch (e: Exception) {
                    break
                }
                delay(120L)
            }
            // Ensure turned off on exit
            try {
                mgr.setTorchMode(camId, false)
            } catch (_: Exception) {}
        }
    }

    fun stopStrobe() {
        strobeJob?.cancel()
        strobeJob = null
        try {
            val camId = cameraId ?: return
            cameraManager?.setTorchMode(camId, false)
        } catch (_: Exception) {}
    }
}
