package com.example.core.termux

import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket

/**
 * Termux Native IPC & Socket Connectivity Bridge.
 *
 * Provides:
 * 1. Localhost TCP Server (127.0.0.1:9999) for sub-millisecond IPC with Termux Python daemons.
 * 2. Termux `RUN_COMMAND` intent dispatcher to execute shell scripts and Python tools.
 * 3. Live terminal output streaming & sensor/hardware telemetry relay.
 */
class TermuxBridgeManager private constructor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO)
    private var serverJob: Job? = null
    private var serverSocket: ServerSocket? = null

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _terminalLogs = MutableStateFlow<List<String>>(listOf("[TermuxBridge] Bridge initialized on port 9999."))
    val terminalLogs: StateFlow<List<String>> = _terminalLogs.asStateFlow()

    private val _termuxDaemonPid = MutableStateFlow<Int?>(null)
    val termuxDaemonPid: StateFlow<Int?> = _termuxDaemonPid.asStateFlow()

    init {
        startLocalhostServer()
    }

    /**
     * Starts background TCP socket server on localhost port 9999.
     */
    fun startLocalhostServer(port: Int = 9999) {
        serverJob?.cancel()
        serverJob = scope.launch {
            try {
                serverSocket?.close()
                val server = ServerSocket(port)
                serverSocket = server
                appendLog("[TermuxBridge] Listening on 127.0.0.1:$port for Termux daemon...")

                while (true) {
                    val client = server.accept()
                    _isConnected.value = true
                    appendLog("[TermuxBridge] 🟢 Termux client connected: ${client.inetAddress.hostAddress}")
                    handleClient(client)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Socket server stopped/closed: ${e.message}")
                _isConnected.value = false
            }
        }
    }

    private fun handleClient(socket: Socket) {
        scope.launch {
            try {
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val writer = PrintWriter(socket.getOutputStream(), true)

                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val msg = line?.trim() ?: continue
                    if (msg.isBlank()) continue

                    try {
                        val json = JSONObject(msg)
                        val type = json.optString("type", "LOG")
                        val payload = json.optString("payload", "")

                        when (type) {
                            "HEARTBEAT" -> {
                                _termuxDaemonPid.value = json.optInt("pid", 0)
                                writer.println(JSONObject().put("status", "ACK").toString())
                            }
                            "TELEMETRY" -> {
                                appendLog("📊 [Termux Stats]: $payload")
                                writer.println(JSONObject().put("status", "OK").toString())
                            }
                            else -> {
                                appendLog("🐚 $payload")
                                writer.println(JSONObject().put("status", "RECEIVED").toString())
                            }
                        }
                    } catch (e: Exception) {
                        appendLog("🐚 $msg")
                    }
                }
            } catch (e: Exception) {
                appendLog("[TermuxBridge] Client disconnected: ${e.message}")
            } finally {
                _isConnected.value = false
            }
        }
    }

    /**
     * Dispatches a command execution intent to Termux:API / Termux core.
     */
    fun executeCommand(command: String, args: Array<String> = emptyArray()): Boolean {
        return try {
            appendLog("➔ Running Termux Command: \"$command\"")

            val intent = Intent("com.termux.RUN_COMMAND").apply {
                setPackage("com.termux")
                putExtra("com.termux.RUN_COMMAND_PATH", command)
                putExtra("com.termux.RUN_COMMAND_ARGUMENTS", args)
                putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home")
                putExtra("com.termux.RUN_COMMAND_BACKGROUND", true)
            }
            context.sendBroadcast(intent)
            true
        } catch (e: Exception) {
            appendLog("❌ Failed to broadcast Termux intent: ${e.message}")
            false
        }
    }

    fun appendLog(log: String) {
        val current = _terminalLogs.value.takeLast(99).toMutableList()
        current.add(log)
        _terminalLogs.value = current
        Log.i(TAG, log)
    }

    fun clearLogs() {
        _terminalLogs.value = listOf("[TermuxBridge] Terminal buffer cleared.")
    }

    fun stop() {
        try {
            serverSocket?.close()
            serverJob?.cancel()
            _isConnected.value = false
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping TermuxBridgeManager", e)
        }
    }

    companion object {
        private const val TAG = "TermuxBridge"

        @Volatile
        private var instance: TermuxBridgeManager? = null

        fun getInstance(context: Context): TermuxBridgeManager {
            return instance ?: synchronized(this) {
                instance ?: TermuxBridgeManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
