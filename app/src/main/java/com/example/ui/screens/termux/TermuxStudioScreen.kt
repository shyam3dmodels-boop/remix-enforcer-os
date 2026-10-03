package com.example.ui.screens.termux

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.termux.TermuxBridgeManager
import com.example.ui.theme.KamakuraSkyBackground
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.theme.PaletteCornflower
import com.example.ui.theme.PaletteIceCyan
import com.example.ui.theme.PaletteMintFrost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Interactive Termux Terminal Console & Local IPC Bridge Screen.
 *
 * Allows users to:
 * 1. Execute direct shell commands on-device (`uname -a`, `df -h`, `top`, `python3`, etc.)
 * 2. Connect to Termux localhost daemon on `127.0.0.1:9999`
 * 3. Inspect real-time terminal stdout/stderr stream
 * 4. Trigger quick diagnostic command presets
 */
@Composable
fun TermuxStudioScreen(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val termuxBridge = remember { TermuxBridgeManager.getInstance(context) }
    val isConnected by termuxBridge.isConnected.collectAsStateWithLifecycle()
    val terminalLogs by termuxBridge.terminalLogs.collectAsStateWithLifecycle()
    val daemonPid by termuxBridge.termuxDaemonPid.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()
    var inputCommand by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val quickCommands = listOf(
        "uname -a",
        "whoami && id",
        "df -h /data",
        "free -m",
        "netstat -tuln",
        "ps -ef | grep -i termux",
        "cat /proc/cpuinfo | grep 'model name' | head -n 2",
        "curl -s http://127.0.0.1:8080/health || echo 'Offline'"
    )

    fun runCommand(cmd: String) {
        val trimmed = cmd.trim()
        if (trimmed.isBlank()) return
        inputCommand = ""
        coroutineScope.launch(Dispatchers.IO) {
            termuxBridge.executeCommandDirect(trimmed)
        }
    }

    LaunchedEffect(terminalLogs.size) {
        if (terminalLogs.isNotEmpty()) {
            listState.animateScrollToItem(terminalLogs.size - 1)
        }
    }

    KamakuraSkyBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
                .testTag("termux_studio_screen"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── TOP HEADER BAR ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(PaletteIceCyan.copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = PaletteCornflower)
                    }
                    Column {
                        Text(
                            text = "Termux Terminal Bridge",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = PaletteCornflower
                        )
                        Text(
                            text = "Local Linux Shell & Daemon Gateway",
                            fontSize = 11.sp,
                            color = Color(0xFF6B7280)
                        )
                    }
                }

                // Connection status chip
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isConnected) PaletteMintFrost else PaletteIceCyan.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (isConnected) Color(0xFF10B981) else Color(0xFF6B7280))
                        )
                        Text(
                            text = if (isConnected) "PID ${daemonPid ?: 9999}" else "127.0.0.1:9999",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = PaletteCornflower
                        )
                    }
                }
            }

            // ── QUICK COMMAND CHIPS ──
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(quickCommands) { cmd ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White.copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, PaletteIceCyan),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { runCommand(cmd) }
                    ) {
                        Text(
                            text = cmd,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = PaletteCornflower,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            // ── TERMINAL CONSOLE VIEW ──
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = BorderStroke(1.2.dp, PaletteCornflower.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(Color(0xFFEF4444)))
                            Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(Color(0xFFF59E0B)))
                            Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(Color(0xFF10B981)))
                        }
                        Text(
                            text = "sh / termux-socket",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Color(0xFF64748B)
                        )
                        IconButton(
                            onClick = { termuxBridge.clearLogs() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.CleaningServices, contentDescription = "Clear", tint = Color(0xFF64748B), modifier = Modifier.size(14.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        items(terminalLogs) { line ->
                            Text(
                                text = line,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                color = when {
                                    line.startsWith("❌") || line.contains("ERR:") -> Color(0xFFF87171)
                                    line.startsWith("🟢") || line.contains("Success") -> Color(0xFF34D399)
                                    line.startsWith("➔") || line.startsWith("🐚") -> Color(0xFF67E8F9)
                                    else -> Color(0xFFE2E8F0)
                                }
                            )
                        }
                    }
                }
            }

            // ── COMMAND INPUT BOX ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputCommand,
                    onValueChange = { inputCommand = it },
                    placeholder = { Text("Enter shell command (e.g. ls -la, python3...)", fontSize = 12.sp, color = Color.Gray) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(alpha = 0.9f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.8f),
                        focusedBorderColor = PaletteCornflower,
                        unfocusedBorderColor = PaletteIceCyan
                    ),
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = { runCommand(inputCommand) },
                    colors = ButtonDefaults.buttonColors(containerColor = PaletteCornflower),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Execute", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
