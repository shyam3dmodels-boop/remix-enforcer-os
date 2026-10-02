package com.example.core.shellmind

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.core.termux.TermuxBridgeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * ShellMind Terminal Execution Engine.
 * Executes Linux / Termux terminal commands, streams output, and integrates with the AI coding loop.
 */
class ShellMindTerminalExecutor private constructor(private val context: Context) {

    private val termuxBridge = TermuxBridgeManager.getInstance(context)
    private val workspaceManager = ShellMindWorkspaceManager.getInstance(context)

    private val _terminalOutput = MutableStateFlow<List<TerminalLogEntry>>(emptyList())
    val terminalOutput: StateFlow<List<TerminalLogEntry>> = _terminalOutput.asStateFlow()

    private val _isExecuting = MutableStateFlow(false)
    val isExecuting: StateFlow<Boolean> = _isExecuting.asStateFlow()

    private val _commandHistory = MutableStateFlow<List<String>>(
        listOf("ls -la", "python3 --version", "git status", "pip list", "node -v")
    )
    val commandHistory: StateFlow<List<String>> = _commandHistory.asStateFlow()

    companion object {
        private const val TAG = "ShellMindTerminal"

        @Volatile
        private var instance: ShellMindTerminalExecutor? = null

        fun getInstance(context: Context): ShellMindTerminalExecutor {
            return instance ?: synchronized(this) {
                instance ?: ShellMindTerminalExecutor(context.applicationContext).also { instance = it }
            }
        }
    }

    suspend fun executeCommand(command: String): TerminalExecutionResult = withContext(Dispatchers.IO) {
        val trimmed = command.trim()
        if (trimmed.isBlank()) return@withContext TerminalExecutionResult(1, "", "Empty command")

        _isExecuting.value = true
        appendLog(TerminalLogEntry.Command(trimmed))

        // Record into history
        val currentHist = _commandHistory.value.toMutableList()
        currentHist.remove(trimmed)
        currentHist.add(0, trimmed)
        _commandHistory.value = currentHist.take(30)

        // 1. First attempt: Termux Bridge / Socket Server execution
        try {
            val bridgeResp = termuxBridge.executePythonScript(trimmed)
            if (bridgeResp.startsWith("Error:") && bridgeResp.contains("socket")) {
                // Fallback to local process execution in workspace directory
                val localResult = executeLocalProcess(trimmed)
                _isExecuting.value = false
                return@withContext localResult
            }

            appendLog(TerminalLogEntry.Output(bridgeResp))
            _isExecuting.value = false
            return@withContext TerminalExecutionResult(0, bridgeResp, "")
        } catch (e: Exception) {
            Log.w(TAG, "Bridge execution fallback: ${e.message}")
            val localResult = executeLocalProcess(trimmed)
            _isExecuting.value = false
            return@withContext localResult
        }
    }

    private fun executeLocalProcess(command: String): TerminalExecutionResult {
        return try {
            val workspaceDir = java.io.File(workspaceManager.getWorkspacePath())
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command), null, workspaceDir)

            val stdoutBuilder = StringBuilder()
            val stderrBuilder = StringBuilder()

            val outReader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (outReader.readLine().also { line = it } != null) {
                line?.let {
                    stdoutBuilder.append(it).append("\n")
                    appendLog(TerminalLogEntry.Output(it))
                }
            }

            val errReader = BufferedReader(InputStreamReader(process.errorStream))
            while (errReader.readLine().also { line = it } != null) {
                line?.let {
                    stderrBuilder.append(it).append("\n")
                    appendLog(TerminalLogEntry.Error(it))
                }
            }

            val exitCode = process.waitFor()
            val stdout = stdoutBuilder.toString().trim()
            val stderr = stderrBuilder.toString().trim()

            if (exitCode == 0) {
                appendLog(TerminalLogEntry.Success("Process exited with code 0"))
            } else {
                appendLog(TerminalLogEntry.Error("Process exited with error code $exitCode"))
            }

            TerminalExecutionResult(exitCode, stdout, stderr)
        } catch (e: Exception) {
            val err = "Execution failed: ${e.message}"
            appendLog(TerminalLogEntry.Error(err))
            TerminalExecutionResult(1, "", err)
        }
    }

    fun clearLogs() {
        _terminalOutput.value = emptyList()
    }

    private fun appendLog(entry: TerminalLogEntry) {
        val current = _terminalOutput.value.toMutableList()
        current.add(entry)
        _terminalOutput.value = current.takeLast(300)
    }
}

sealed class TerminalLogEntry(val text: String, val timestamp: Long = System.currentTimeMillis()) {
    class Command(text: String) : TerminalLogEntry("❯ $text")
    class Output(text: String) : TerminalLogEntry(text)
    class Error(text: String) : TerminalLogEntry("✖ $text")
    class Success(text: String) : TerminalLogEntry("✔ $text")
}

data class TerminalExecutionResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String
) {
    val isSuccess: Boolean get() = exitCode == 0
}
