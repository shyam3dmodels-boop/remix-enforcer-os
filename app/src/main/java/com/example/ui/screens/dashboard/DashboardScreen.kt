package com.example.ui.screens.dashboard

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.recorder.TelegramC2Manager
import com.example.ui.EnforcerViewModel
import com.example.ui.theme.HarmonyGlassCard
import com.example.ui.theme.KamakuraCloudShadow
import com.example.ui.theme.KamakuraCloudWhite
import com.example.ui.theme.KamakuraCoralWarning
import com.example.ui.theme.KamakuraDeepCobalt
import com.example.ui.theme.KamakuraEmerald
import com.example.ui.theme.KamakuraSignBlue
import com.example.ui.theme.KamakuraSkyBackground
import com.example.ui.theme.KamakuraSkyBlue
import com.example.ui.theme.KamakuraSoftHorizon
import com.example.ui.theme.KamakuraSunWarmth
import com.example.ui.theme.KamakuraTextPrimary
import com.example.ui.theme.KamakuraTextSecondary
import com.example.ui.theme.LiquidGlassButton
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.theme.LiquidGlassFill
import com.example.ui.theme.PaletteCornflower
import com.example.ui.theme.PaletteIceCyan
import com.example.ui.theme.PaletteMintFrost
import com.example.ui.theme.PaletteSoftSky
import java.util.Calendar

private fun getTimeBasedGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Good Morning, Harsh ☀️"
        in 12..16 -> "Good Afternoon, Harsh 🌤️"
        in 17..21 -> "Good Evening, Harsh ✨"
        else -> "Good Night, Harsh 🌙"
    }
}

@Composable
fun DashboardScreen(
    viewModel: EnforcerViewModel,
    onNavigateToChatAi: () -> Unit = {}
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Telemetry & Hardware States
    val batteryStatus by viewModel.batteryStatus.collectAsStateWithLifecycle()
    val stepsToday by viewModel.stepsToday.collectAsStateWithLifecycle()
    val stepGoal by viewModel.stepGoal.collectAsStateWithLifecycle()
    val walkingState by viewModel.walkingState.collectAsStateWithLifecycle()
    val screenTimeMinutes by viewModel.screenTimeMinutes.collectAsStateWithLifecycle()
    val unlockCount by viewModel.unlockCount.collectAsStateWithLifecycle()
    val passiveBssidCount by viewModel.passiveBssidCount.collectAsStateWithLifecycle()
    val networkType by viewModel.networkType.collectAsStateWithLifecycle()
    val proximitySummary by viewModel.proximitySummary.collectAsStateWithLifecycle()
    val lastPinnedGps by viewModel.lastPinnedGps.collectAsStateWithLifecycle()
    val deviceProfile by viewModel.deviceProfile.collectAsStateWithLifecycle()

    // C2 Telegram Auto-Responder Manager
    val c2Manager = remember { TelegramC2Manager.getInstance(context) }
    val lastC2Command by c2Manager.lastExecutedCommand.collectAsStateWithLifecycle()
    val c2Logs by c2Manager.commandLogs.collectAsStateWithLifecycle()
    val isC2Active by c2Manager.isPollingActive.collectAsStateWithLifecycle()

    var showActionToast by remember { mutableStateOf<String?>(null) }
    var showHardwareGrid by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshBattery()
    }

    showActionToast?.let { msg ->
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        showActionToast = null
    }

    KamakuraSkyBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp)
                .verticalScroll(scrollState)
                .testTag("dashboard_screen"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(modifier = Modifier.height(6.dp))

            // ─── 1. HERO GREETING & FAST AI PROMPT CARD ────────────────────────
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 24.dp,
                elevation = 4.dp
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = getTimeBasedGreeting(),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = KamakuraSignBlue,
                                letterSpacing = (-0.3).sp
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                )
                                Text(
                                    text = "Autonomous Sentinel Active • Zero Latency",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = KamakuraTextSecondary
                                )
                            }
                        }

                        // Cloud Host Status Pill
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = PaletteMintFrost.copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, PaletteSoftSky)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "🟢 LIVE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = PaletteCornflower
                                )
                            }
                        }
                    }

                    // Interactive Fast AI Search / Ask Pill
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.95f),
                        border = BorderStroke(1.2.dp, PaletteSoftSky),
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onNavigateToChatAi() }
                            .testTag("fast_ai_prompt_pill")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Ask AI",
                                    tint = PaletteCornflower,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Ask Second Brain anything...",
                                    fontSize = 13.sp,
                                    color = KamakuraTextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = PaletteIceCyan.copy(alpha = 0.70f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "Voice Prompt",
                                        tint = PaletteCornflower,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "PROMPT",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = PaletteCornflower
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ─── 2. 4-PILLAR FAST ACTION LAUNCHPAD (BENTO 2X2) ──────────────────
            Text(
                text = "⚡ MISSION CONTROL LAUNCHPAD",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp,
                color = KamakuraDeepCobalt,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Launchpad Tile 1: AI Chat Assistant
                BentoLaunchpadTile(
                    modifier = Modifier.weight(1f),
                    title = "Second Brain AI",
                    subtitle = "DeepSeek R1 & Groq",
                    icon = Icons.Default.Psychology,
                    accentColor = PaletteCornflower,
                    badge = "GROUNDED",
                    onClick = onNavigateToChatAi
                )

                // Launchpad Tile 2: Instant Audio Recorder
                BentoLaunchpadTile(
                    modifier = Modifier.weight(1f),
                    title = "Ambient Audio",
                    subtitle = "Groq Whisper STT",
                    icon = Icons.Default.Mic,
                    accentColor = PaletteCornflower,
                    badge = "ACTIVE",
                    onClick = {
                        c2Manager.executeCommand("/record 15")
                        showActionToast = "🎙️ Ambient 15s audio capture started!"
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Launchpad Tile 3: Hardware GPS Pin
                BentoLaunchpadTile(
                    modifier = Modifier.weight(1f),
                    title = "Pin GPS Location",
                    subtitle = "${lastPinnedGps.ifEmpty { "High Precision" }}",
                    icon = Icons.Default.LocationOn,
                    accentColor = PaletteCornflower,
                    badge = "SATELLITE",
                    onClick = {
                        viewModel.pinGps()
                        c2Manager.executeCommand("/locate")
                        showActionToast = "📍 GPS Beacon pinned & broadcasted!"
                    }
                )

                // Launchpad Tile 4: Emergency Strobe & Siren
                BentoLaunchpadTile(
                    modifier = Modifier.weight(1f),
                    title = "Locator Siren",
                    subtitle = "100% Alarm + Torch",
                    icon = Icons.Default.NotificationsActive,
                    accentColor = PaletteCornflower,
                    badge = "SAFETY",
                    onClick = {
                        c2Manager.executeCommand("/siren 15")
                        showActionToast = "🚨 100% Volume Siren & Torch active!"
                    }
                )
            }

            // ─── 3. PHYSICAL TELEMETRY MATRIX (BENTO) ─────────────────────────
            Text(
                text = "📊 SENSOR & DIGITAL HABIT METRICS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp,
                color = KamakuraDeepCobalt,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Steps Pedometer Card
                LiquidGlassCard(
                    modifier = Modifier.weight(1f),
                    cornerRadius = 20.dp
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsWalk,
                                    contentDescription = "Pedometer",
                                    tint = PaletteCornflower,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "STEPS",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = KamakuraTextSecondary
                                )
                            }
                            Text(
                                text = "$walkingState",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = PaletteCornflower
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "$stepsToday",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = KamakuraTextPrimary
                        )

                        Text(
                            text = "Goal: $stepGoal steps",
                            fontSize = 10.sp,
                            color = KamakuraTextSecondary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { (stepsToday.toFloat() / stepGoal).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = PaletteCornflower,
                            trackColor = PaletteSoftSky.copy(alpha = 0.4f)
                        )
                    }
                }

                // Screen Usage & Focus Card
                LiquidGlassCard(
                    modifier = Modifier.weight(1f),
                    cornerRadius = 20.dp
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = "Screen Usage",
                                    tint = PaletteCornflower,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "SCREEN FOCUS",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = KamakuraTextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "${screenTimeMinutes / 60}h ${screenTimeMinutes % 60}m",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = KamakuraTextPrimary
                        )

                        Text(
                            text = "$unlockCount unlocks today",
                            fontSize = 10.sp,
                            color = KamakuraTextSecondary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { (screenTimeMinutes.toFloat() / 360f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = PaletteCornflower,
                            trackColor = PaletteSoftSky.copy(alpha = 0.4f)
                        )
                    }
                }
            }

            // ─── 4. TELEGRAM C2 & AUTO-RESPONDER ACTIVITY FEED ────────────────
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(PaletteIceCyan),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = "C2 Feed",
                                    tint = PaletteCornflower,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Telegram Auto-Responder Feed",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = KamakuraSignBlue
                                )
                                Text(
                                    text = "Bi-directional Command & Control",
                                    fontSize = 10.sp,
                                    color = KamakuraTextSecondary
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isC2Active) PaletteMintFrost.copy(alpha = 0.85f) else Color(0xFFFFECEC)
                        ) {
                            Text(
                                text = if (isC2Active) "LISTENING" else "STANDBY",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isC2Active) PaletteCornflower else Color(0xFFE11D48),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Last executed command pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.92f),
                        border = BorderStroke(1.dp, PaletteSoftSky),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "LATEST C2:",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = KamakuraTextSecondary
                            )
                            Text(
                                text = lastC2Command,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = PaletteCornflower
                            )
                        }
                    }

                    if (c2Logs.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            c2Logs.take(3).forEach { logLine ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = PaletteSoftSky.copy(alpha = 0.20f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = logLine,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = KamakuraTextPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ─── 5. EXPANDABLE HARDWARE COMMANDS DRAWER ───────────────────────
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow)),
                cornerRadius = 20.dp
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showHardwareGrid = !showHardwareGrid },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sensors,
                                contentDescription = "Hardware",
                                tint = PaletteCornflower,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Extended Hardware Diagnostic Tools",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = KamakuraSignBlue
                            )
                        }

                        Icon(
                            imageVector = if (showHardwareGrid) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Toggle",
                            tint = PaletteCornflower
                        )
                    }

                    if (showHardwareGrid) {
                        Spacer(modifier = Modifier.height(12.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        c2Manager.executeCommand("/arp")
                                        showActionToast = "🛡️ ARP table audit dispatched!"
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("ARP Sentinel", fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        c2Manager.executeCommand("/netscan")
                                        showActionToast = "🌐 Subnet sweep triggered!"
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Subnet Sweep", fontSize = 11.sp)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        c2Manager.executeCommand("/mute")
                                        showActionToast = "🔇 Mute triggered!"
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Stealth Mute", fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        c2Manager.executeCommand("/photo")
                                        showActionToast = "📸 Snapshot triggered!"
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Snapshot", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun BentoLaunchpadTile(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    badge: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = LiquidGlassFill,
        shadowElevation = 2.dp,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .border(
                BorderStroke(
                    1.2.dp,
                    Brush.verticalGradient(
                        listOf(
                            PaletteCornflower.copy(alpha = 0.50f),
                            PaletteIceCyan.copy(alpha = 0.60f)
                        )
                    )
                ),
                RoundedCornerShape(20.dp)
            )
            .testTag("launchpad_tile_${title.lowercase().replace(" ", "_")}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PaletteMintFrost.copy(alpha = 0.65f)
                ) {
                    Text(
                        text = badge,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PaletteCornflower,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Column {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = KamakuraTextPrimary,
                    letterSpacing = (-0.2).sp
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = KamakuraTextSecondary,
                    maxLines = 1
                )
            }
        }
    }
}
