package com.example.ui.screens.claw

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.core.claw.MobileClawReActRunner
import com.example.core.claw.PocketClawBondEngine
import com.example.core.claw.PokeClawOverlayService
import com.example.core.termux.TermuxBridgeManager
import com.example.ui.theme.*

@Composable
fun ClawStudioScreen(
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bondEngine = remember { PocketClawBondEngine.getInstance(context) }
    val reactRunner = remember { MobileClawReActRunner.getInstance(context) }
    val termuxBridge = remember { TermuxBridgeManager.getInstance(context) }

    val xp by bondEngine.xp.collectAsStateWithLifecycle()
    val tasksCount by bondEngine.tasksCompleted.collectAsStateWithLifecycle()
    val currentStage by bondEngine.currentStage.collectAsStateWithLifecycle()
    val isRunning by reactRunner.isRunning.collectAsStateWithLifecycle()
    val lastLog by reactRunner.lastLog.collectAsStateWithLifecycle()
    val isOverlayShowing by PokeClawOverlayService.isOverlayShowing.collectAsStateWithLifecycle()
    val isAccessibilityActive by MobileClawAccessibilityService.isServiceActive.collectAsStateWithLifecycle()
    val isTermuxConnected by termuxBridge.isConnected.collectAsStateWithLifecycle()
    val terminalLogs by termuxBridge.terminalLogs.collectAsStateWithLifecycle()

    var customGoalInput by remember { mutableStateOf("") }
    var termuxCmdInput by remember { mutableStateOf("") }

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
                        text = "POKECLAW & MOBILECLAW STUDIO",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                            color = Color.White
                        )
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isAccessibilityActive) CyberGreen.copy(alpha = 0.15f) else CyberRed.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isAccessibilityActive) CyberGreen else CyberRed)
                ) {
                    Text(
                        text = if (isAccessibilityActive) "● ACTIVE" else "● DISABLED",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isAccessibilityActive) CyberGreen else CyberRed,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // ── 1. PocketClaw 5-Stage Bond & Evolution Card ──────────────────────
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = LiquidGlassFill,
                border = androidx.compose.foundation.BorderStroke(1.dp, PaletteSkyBlue.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(text = currentStage.avatar, fontSize = 42.sp)
                            Column {
                                Text(
                                    text = "PocketClaw Evolution",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PaletteSkyBlue
                                )
                                Text(
                                    text = "Stage: ${currentStage.title}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "$xp XP", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CyberYellow)
                            Text(text = "$tasksCount Tasks Accomplished", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = currentStage.description,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    // Progress Bar
                    val nextMax = when (currentStage) {
                        PocketClawBondEngine.EvolutionStage.LARVA -> 50
                        PocketClawBondEngine.EvolutionStage.HATCHLING -> 200
                        PocketClawBondEngine.EvolutionStage.JUVENILE -> 500
                        PocketClawBondEngine.EvolutionStage.ADULT -> 1500
                        PocketClawBondEngine.EvolutionStage.ELDER -> 3000
                    }
                    val progress = (xp.toFloat() / nextMax).coerceIn(0.05f, 1f)

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = CyberYellow,
                        trackColor = Color.White.copy(alpha = 0.1f),
                    )
                }
            }
        }

        // ── 2. Overlay HUD & Permissions Controls ────────────────────────────
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = LiquidGlassFill,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "AUTOMATION SERVICES & PERMISSIONS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PaletteCornflower
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Accessibility Agent", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(text = "Required for screen taps & reading", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                        }
                        Button(
                            onClick = {
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isAccessibilityActive) CyberGreen else PaletteCornflower),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (isAccessibilityActive) "Configured" else "Grant Access", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Divider(color = Color.White.copy(alpha = 0.08f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "PokeClaw Floating HUD", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(text = "Thought stream bubble on screen", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                        }
                        Switch(
                            checked = isOverlayShowing,
                            onCheckedChange = { enable ->
                                if (enable) {
                                    if (Settings.canDrawOverlays(context)) {
                                        PokeClawOverlayService.start(context)
                                    } else {
                                        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                                        context.startActivity(intent)
                                    }
                                } else {
                                    PokeClawOverlayService.stop(context)
                                }
                            }
                        )
                    }
                }
            }
        }

        // ── 3. One-Tap Autonomous Workflows ──────────────────────────────────
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = LiquidGlassFill,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "AUTONOMOUS GUI GOALS & WORKFLOWS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PaletteCornflower
                    )

                    // Quick Goal Chips
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { reactRunner.runGoal("Search YouTube for Lofi Chill Beats") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f))
                        ) {
                            Text("🎬 YouTube Auto", fontSize = 11.sp, color = Color.White)
                        }
                        Button(
                            onClick = { reactRunner.runGoal("Open System Settings") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f))
                        ) {
                            Text("⚙️ Open Settings", fontSize = 11.sp, color = Color.White)
                        }
                    }

                    // Custom Goal Input
                    OutlinedTextField(
                        value = customGoalInput,
                        onValueChange = { customGoalInput = it },
                        placeholder = { Text("e.g. Open Telegram and check recent messages", fontSize = 12.sp, color = Color.White.copy(alpha = 0.4f)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PaletteSkyBlue,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Button(
                        onClick = {
                            if (customGoalInput.isNotBlank()) {
                                reactRunner.runGoal(customGoalInput)
                                customGoalInput = ""
                            }
                        },
                        enabled = !isRunning,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PaletteCornflower)
                    ) {
                        if (isRunning) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Automating GUI...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Execute Autonomous Goal", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Status Log Strip
                    Text(
                        text = "Status: $lastLog",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = PaletteSkyBlue
                    )
                }
            }
        }

        // ── 4. Termux Native Connectivity & Live Terminal ────────────────────
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = LiquidGlassFill,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "TERMUX NATIVE IPC & SOCKET BRIDGE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PaletteCornflower
                            )
                            Text(
                                text = "Port 9999 • Sub-ms Localhost Socket",
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isTermuxConnected) CyberGreen.copy(alpha = 0.2f) else CyberYellow.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = if (isTermuxConnected) "CONNECTED" else "STANDBY",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isTermuxConnected) CyberGreen else CyberYellow,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Live Terminal Console Box
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF070B12),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        LazyColumn(
                            modifier = Modifier.padding(8.dp),
                            reverseLayout = true
                        ) {
                            items(terminalLogs.reversed()) { log ->
                                Text(
                                    text = log,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (log.contains("🟢")) CyberGreen else if (log.contains("❌")) CyberRed else Color(0xFF94A3B8)
                                )
                            }
                        }
                    }

                    // Command Dispatch Bar
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = termuxCmdInput,
                            onValueChange = { termuxCmdInput = it },
                            placeholder = { Text("e.g. python3 termux_enforcer_bridge.py", fontSize = 11.sp, color = Color.White.copy(alpha = 0.4f)) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PaletteSkyBlue,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        Button(
                            onClick = {
                                if (termuxCmdInput.isNotBlank()) {
                                    termuxBridge.executeCommand(termuxCmdInput)
                                    termuxCmdInput = ""
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PaletteCornflower)
                        ) {
                            Text("Run", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
