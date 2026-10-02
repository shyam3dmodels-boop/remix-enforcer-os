package com.example.ui.screens.claw

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.claw.MobileClawAccessibilityService
import com.example.core.claw.PrivateAgentFeedbackManager
import com.example.core.claw.PrivateAgentFloatingBubbleService
import com.example.core.claw.PrivateAgentHardwareController
import com.example.core.claw.PrivateAgentReActEngine
import com.example.core.claw.PrivateAgentTelephonyDispatcher
import com.example.core.termux.TermuxLlmManager
import com.example.ui.theme.*

@Composable
fun PrivateAgentStudioScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToShellMind: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val reactEngine = remember { PrivateAgentReActEngine.getInstance(context) }
    val hardwareController = remember { PrivateAgentHardwareController.getInstance(context) }
    val telephonyDispatcher = remember { PrivateAgentTelephonyDispatcher.getInstance(context) }
    val feedbackManager = remember { PrivateAgentFeedbackManager.getInstance(context) }
    val termuxLlm = remember { TermuxLlmManager.getInstance(context) }

    val isRunning by reactEngine.isRunning.collectAsStateWithLifecycle()
    val currentGoal by reactEngine.currentGoal.collectAsStateWithLifecycle()
    val currentThought by reactEngine.currentThought.collectAsStateWithLifecycle()
    val executionLogs by reactEngine.executionLogs.collectAsStateWithLifecycle()
    val isBubbleShowing by PrivateAgentFloatingBubbleService.isBubbleShowing.collectAsStateWithLifecycle()
    val isAccessibilityActive by MobileClawAccessibilityService.isServiceActive.collectAsStateWithLifecycle()

    val isTermuxServerOnline by termuxLlm.isServerOnline.collectAsStateWithLifecycle()
    val termuxServerUrl by termuxLlm.serverUrl.collectAsStateWithLifecycle()
    val termuxTokensPerSec by termuxLlm.tokensPerSec.collectAsStateWithLifecycle()
    val termuxBenchmarkLog by termuxLlm.lastBenchmarkLog.collectAsStateWithLifecycle()

    var goalInput by remember { mutableStateOf("") }
    var selectedModel by remember { mutableStateOf("deepseek-chat") }
    var customBaseUrl by remember { mutableStateOf(reactEngine.baseUrl) }
    var customApiKey by remember { mutableStateOf(reactEngine.apiKey) }
    var showConfigDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Top Bar ──────────────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = PaletteCornflower)
                    }
                    Text(
                        text = "⚡ AI AGENT STUDIO",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                            color = Color.White
                        )
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = onNavigateToShellMind,
                        colors = ButtonDefaults.buttonColors(containerColor = PaletteCornflower),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("💻 IDE", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                    }
                    IconButton(onClick = { showConfigDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Config", tint = PaletteCornflower)
                    }
                }
            }
        }

        // ── Status Banner & Quick Toggles ────────────────────────────────────
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Brush.horizontalGradient(listOf(PaletteCornflower, PaletteIceCyan))
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Autonomous Screen Assistant",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Active Provider: ${if (selectedModel.contains("local")) "Termux Local LLM" else selectedModel}",
                                fontSize = 12.sp,
                                color = PaletteIceCyan
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isAccessibilityActive) CyberGreen.copy(alpha = 0.15f) else CyberRed.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isAccessibilityActive) CyberGreen else CyberRed)
                        ) {
                            Text(
                                text = if (isAccessibilityActive) "● SERVICE ON" else "● SERVICE OFF",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isAccessibilityActive) CyberGreen else CyberRed,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (isBubbleShowing) {
                                    PrivateAgentFloatingBubbleService.stop(context)
                                } else {
                                    PrivateAgentFloatingBubbleService.start(context)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isBubbleShowing) CyberRed else PaletteCornflower
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                if (isBubbleShowing) Icons.Default.Close else Icons.Default.SmartToy,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isBubbleShowing) "Hide Bubble" else "Launch Bubble", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Accessibility, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Accessibility", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // ── 1.5. Termux Local AI LLM Engine Hub Card ─────────────────────────
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isTermuxServerOnline) CyberGreen else PaletteIceCyan.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "🐚 Termux Local AI LLM",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                            if (termuxTokensPerSec != null) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = PaletteMintFrost.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "${"%.1f".format(termuxTokensPerSec)} tok/s",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PaletteIceCyan,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isTermuxServerOnline) CyberGreen.copy(alpha = 0.15f) else Color(0xFF64748B).copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isTermuxServerOnline) CyberGreen else Color(0xFF64748B)
                            )
                        ) {
                            Text(
                                text = if (isTermuxServerOnline) "● ONLINE (127.0.0.1:8080)" else "● OFFLINE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isTermuxServerOnline) CyberGreen else Color(0xFF94A3B8),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "100% Offline On-Device Reasoning (DeepSeek-R1 1.5B / Qwen2.5 / Llama 3.2 GGUF via llama.cpp)",
                        fontSize = 11.sp,
                        color = DarkTextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                termuxLlm.launchTermuxServer(1)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PaletteCornflower),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Start Local Server", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                customBaseUrl = "http://127.0.0.1:8080/v1"
                                selectedModel = "local-model"
                                reactEngine.baseUrl = customBaseUrl
                                reactEngine.modelName = selectedModel
                                termuxLlm.runLocalBenchmarkTest("Explain AI in 5 words.")
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PaletteIceCyan),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Use & Test Speed", fontSize = 11.sp)
                        }
                    }

                    if (termuxBenchmarkLog.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF070B12),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = termuxBenchmarkLog,
                                fontSize = 10.sp,
                                color = if (termuxBenchmarkLog.startsWith("✅")) CyberGreen else if (termuxBenchmarkLog.startsWith("❌")) CyberRed else Color(0xFF94A3B8),
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
        }

        // ── Goal Dispatcher Input ────────────────────────────────────────────
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "🎯 Natural Language Device Goal",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = goalInput,
                        onValueChange = { goalInput = it },
                        placeholder = { Text("e.g., Open YouTube and play lofi beats...", color = DarkTextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PaletteCornflower,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (goalInput.isNotBlank()) {
                                    reactEngine.baseUrl = customBaseUrl
                                    reactEngine.apiKey = customApiKey
                                    reactEngine.modelName = selectedModel
                                    reactEngine.startGoal(goalInput)
                                }
                            },
                            enabled = !isRunning && goalInput.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = PaletteCornflower),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Execute Goal")
                        }

                        if (isRunning) {
                            Button(
                                onClick = { reactEngine.stop() },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberRed),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Abort")
                            }
                        }
                    }
                }
            }
        }

        // ── Direct Hardware & Telephony Quick Actions ────────────────────────
        item {
            Text(
                text = "⚡ Quick System & Hardware Actions",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color.White,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionChip(
                    icon = Icons.Default.FlashlightOn,
                    label = "Flashlight",
                    onClick = { hardwareController.toggleFlashlight() },
                    modifier = Modifier.weight(1f)
                )
                QuickActionChip(
                    icon = Icons.Default.Wifi,
                    label = "WiFi Setup",
                    onClick = { hardwareController.openWifiSettings() },
                    modifier = Modifier.weight(1f)
                )
                QuickActionChip(
                    icon = Icons.Default.Bluetooth,
                    label = "Bluetooth",
                    onClick = { hardwareController.openBluetoothSettings() },
                    modifier = Modifier.weight(1f)
                )
                QuickActionChip(
                    icon = Icons.Default.VolumeUp,
                    label = "Volume Up",
                    onClick = { hardwareController.adjustVolume("up") },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // ── ReAct Live Observation & Steps Terminal ──────────────────────────
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "💭 Live ReAct Thought Stream",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = PaletteIceCyan
                        )
                        if (isRunning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = PaletteIceCyan,
                                strokeWidth = 2.dp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF070B12),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = currentThought,
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Execution Log History",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = DarkTextSecondary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF05080E),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp, max = 220.dp)
                    ) {
                        LazyColumn(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (executionLogs.isEmpty()) {
                                item {
                                    Text(
                                        text = "Terminal idle. Enter a natural language goal to trigger autonomous ReAct.",
                                        fontSize = 11.sp,
                                        color = DarkTextSecondary,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            } else {
                                items(executionLogs) { log ->
                                    Text(
                                        text = log,
                                        fontSize = 11.sp,
                                        color = if (log.startsWith("✓")) CyberGreen else if (log.startsWith("✗") || log.startsWith("⚠️")) CyberRed else Color(0xFF94A3B8),
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ── LLM Provider Configuration Dialog ────────────────────────────────────
    if (showConfigDialog) {
        AlertDialog(
            onDismissRequest = { showConfigDialog = false },
            title = { Text("Universal LLM Settings", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select Model Provider:", fontSize = 12.sp, color = DarkTextSecondary)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = selectedModel == "deepseek-chat",
                            onClick = {
                                selectedModel = "deepseek-chat"
                                customBaseUrl = "https://api.deepseek.com/v1"
                            },
                            label = { Text("DeepSeek V3", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = selectedModel == "deepseek-reasoner",
                            onClick = {
                                selectedModel = "deepseek-reasoner"
                                customBaseUrl = "https://api.deepseek.com/v1"
                            },
                            label = { Text("DeepSeek R1", fontSize = 11.sp) }
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = selectedModel == "llama-3.3-70b-versatile",
                            onClick = {
                                selectedModel = "llama-3.3-70b-versatile"
                                customBaseUrl = "https://api.groq.com/openai/v1"
                            },
                            label = { Text("Groq Llama 3.3", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = selectedModel.contains("ollama"),
                            onClick = {
                                selectedModel = "llama3:latest"
                                customBaseUrl = "http://10.0.2.2:11434/v1"
                            },
                            label = { Text("Ollama Local", fontSize = 11.sp) }
                        )
                    }

                    OutlinedTextField(
                        value = customBaseUrl,
                        onValueChange = { customBaseUrl = it },
                        label = { Text("Base URL") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = customApiKey,
                        onValueChange = { customApiKey = it },
                        label = { Text("API Key (Bearer Token)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        reactEngine.baseUrl = customBaseUrl
                        reactEngine.apiKey = customApiKey
                        reactEngine.modelName = selectedModel
                        showConfigDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PaletteCornflower)
                ) {
                    Text("Save Config")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfigDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = DarkSurface
        )
    }
}

@Composable
private fun QuickActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterVertically,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = label, tint = PaletteIceCyan, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Medium)
        }
    }
}
