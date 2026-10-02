package com.example.ui.screens.dashboard

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.window.Dialog
import com.example.ui.EnforcerViewModel
import com.example.ui.theme.KamakuraTextPrimary
import com.example.ui.theme.KamakuraTextSecondary
import com.example.ui.theme.LiquidGlassButton
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.theme.LiquidGlassFill
import com.example.ui.theme.PaletteCornflower
import com.example.ui.theme.PaletteIceCyan
import com.example.ui.theme.PaletteMintFrost
import com.example.ui.theme.PaletteSoftSky

enum class DashboardSection(val label: String, val icon: String) {
    OVERVIEW("Overview", "🎛️"),
    VOICE_AI("Voice & AI", "🎙️"),
    LOGS("Habits & Logs", "📊")
}

@Composable
fun ManagerDashboardScreen(
    viewModel: EnforcerViewModel,
    modifier: Modifier = Modifier,
    onNavigateToChatAi: () -> Unit = {}
) {
    val context = LocalContext.current
    var selectedSection by remember { mutableStateOf(DashboardSection.OVERVIEW) }

    val steps by viewModel.stepsToday.collectAsState()
    val walkingState by viewModel.walkingState.collectAsState()
    val screenTimeMinutes by viewModel.screenTimeMinutes.collectAsState()
    val unlockCount by viewModel.unlockCount.collectAsState()
    val passiveBssidCount by viewModel.passiveBssidCount.collectAsState()
    val networkType by viewModel.networkType.collectAsState()
    val proximitySummary by viewModel.proximitySummary.collectAsState()
    val batteryStatus by viewModel.batteryStatus.collectAsState()

    val latestVoiceNote by viewModel.latestVoiceNote.collectAsState()
    val latestAiSummary by viewModel.latestAiSummary.collectAsState()
    val actionItems by viewModel.extractedActionItems.collectAsState()
    val isGroqProcessing by viewModel.isGroqProcessing.collectAsState()
    val lastPinnedGps by viewModel.lastPinnedGps.collectAsState()

    val deviceUuid = remember { viewModel.deviceInfoProvider.getOrGenerateDeviceUuid() }
    val deviceModel = remember { viewModel.deviceInfoProvider.getDeviceName() }

    var showThoughtDialog by remember { mutableStateOf(false) }
    var showGroqKeyDialog by remember { mutableStateOf(false) }
    var userThoughtInput by remember { mutableStateOf("") }
    var groqKeyInput by remember { mutableStateOf("") }

    var isAudioPlaying by remember { mutableStateOf(false) }
    var playbackProgress by remember { mutableFloatStateOf(0.42f) }

    val formattedHours = screenTimeMinutes / 60
    val formattedMins = screenTimeMinutes % 60
    val screenTimeString = if (formattedHours > 0) "${formattedHours}h ${formattedMins}m" else "${formattedMins}m"

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "top_spacer") {
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Section Selector Tabs (Ensures only one section renders at a time)
        item(key = "section_selector") {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = LiquidGlassFill,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        Brush.horizontalGradient(listOf(PaletteCornflower, PaletteIceCyan)),
                        RoundedCornerShape(20.dp)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DashboardSection.values().forEach { section ->
                        val isSelected = selectedSection == section
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) PaletteCornflower else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedSection = section }
                                .testTag("tab_${section.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${section.icon} ${section.label}",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isSelected) Color.White else PaletteCornflower,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        when (selectedSection) {
            DashboardSection.OVERVIEW -> {
                // 1. Mission Control Header Card
                item(key = "header_card") {
                    LiquidGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manager_header_card"),
                        cornerRadius = 24.dp,
                        elevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(text = "🌤️", fontSize = 22.sp)
                                    Column {
                                        Text(
                                            text = "SECONDARY BRAIN 2.0",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                letterSpacing = 1.sp
                                            ),
                                            color = PaletteCornflower
                                        )
                                        Text(
                                            text = "Zero-Retention AI Telemetry Hub",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = KamakuraTextSecondary
                                        )
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Live Battery Indicator
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = PaletteMintFrost.copy(alpha = 0.65f),
                                        modifier = Modifier.padding(2.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (batteryStatus.isCharging) Icons.Default.BatteryChargingFull else if (batteryStatus.percent > 20) Icons.Default.BatteryFull else Icons.Default.BatteryAlert,
                                                contentDescription = if (batteryStatus.isCharging) "Battery Charging" else "Battery",
                                                tint = PaletteCornflower,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = "${batteryStatus.percent}%",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = PaletteCornflower
                                            )
                                        }
                                    }

                                    // Dynamic Network Indicator
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = PaletteIceCyan.copy(alpha = 0.70f),
                                        modifier = Modifier.padding(2.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.SignalCellularAlt,
                                                contentDescription = "Signal",
                                                tint = PaletteCornflower,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = networkType,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = PaletteCornflower
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Anchor Device Pill
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PhoneAndroid,
                                            contentDescription = "Device Anchor",
                                            tint = PaletteCornflower,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "DEVICE ANCHOR: ",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = PaletteCornflower
                                        )
                                        Text(
                                            text = deviceUuid.take(12) + "...",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = KamakuraTextPrimary
                                        )
                                    }
                                    Text(
                                        text = "($deviceModel)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = KamakuraTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Telemetry Matrix: Real Steps & Screen Time
                item(key = "telemetry_steps_card") {
                    LiquidGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("telemetry_matrix_card"),
                        cornerRadius = 24.dp,
                        elevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "🧪 REAL-TIME HARDWARE TELEMETRY",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = PaletteCornflower
                            )

                            // Step Sensor Row
                            MetricRow(
                                icon = Icons.Default.DirectionsWalk,
                                iconTint = PaletteCornflower,
                                label = "Step Sensor Activity",
                                value = "$steps steps",
                                subValue = "State: $walkingState • Hardware Accelerometer",
                                progress = (steps.toFloat() / 10000f).coerceIn(0.1f, 1f)
                            )

                            // Screen Time Row
                            MetricRow(
                                icon = Icons.Default.Timer,
                                iconTint = PaletteSoftSky,
                                label = "Daily Screen Usage",
                                value = screenTimeString,
                                subValue = "$unlockCount device unlocks today • UsageStats",
                                progress = (screenTimeMinutes.toFloat() / 360f).coerceIn(0.15f, 1f)
                            )

                            // Passive RF Safe Sanctum
                            MetricRow(
                                icon = Icons.Default.Sensors,
                                iconTint = PaletteCornflower,
                                label = "Proximity Environment",
                                value = proximitySummary,
                                subValue = "Passive Wi-Fi Manager • Ambient RF Mesh",
                                progress = (passiveBssidCount.toFloat() / 10f).coerceIn(0.2f, 1f)
                            )
                        }
                    }
                }

                // 3. Quick Action Pills
                item(key = "quick_actions_card") {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "🎛️ QUICK MANAGER CONTROLLERS",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = PaletteCornflower,
                            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            LiquidGlassButton(
                                onClick = { viewModel.pinGps() },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("action_pin_gps"),
                                cornerRadius = 20.dp,
                                containerColor = PaletteIceCyan.copy(alpha = 0.85f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp, horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = "Pin GPS",
                                        tint = PaletteCornflower,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Pin GPS",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = PaletteCornflower
                                    )
                                }
                            }

                            LiquidGlassButton(
                                onClick = { showThoughtDialog = true },
                                modifier = Modifier
                                    .weight(1.2f)
                                    .testTag("action_record_thought"),
                                cornerRadius = 20.dp,
                                containerColor = PaletteMintFrost.copy(alpha = 0.85f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp, horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "Record Note",
                                        tint = PaletteCornflower,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Record Note",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = PaletteCornflower
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            LiquidGlassButton(
                                onClick = { showGroqKeyDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("action_groq_key"),
                                cornerRadius = 20.dp,
                                containerColor = Color.White.copy(alpha = 0.90f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp, horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VpnKey,
                                        contentDescription = "Groq API Key",
                                        tint = PaletteCornflower,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Groq Key",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = PaletteCornflower
                                    )
                                }
                            }

                            LiquidGlassButton(
                                onClick = { viewModel.syncTelegramNow() },
                                modifier = Modifier
                                    .weight(1.2f)
                                    .testTag("action_sync_telegram"),
                                cornerRadius = 20.dp,
                                containerColor = PaletteIceCyan.copy(alpha = 0.85f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp, horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = "Sync Telegram",
                                        tint = PaletteCornflower,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Sync Telegram",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = PaletteCornflower
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Grounded AI Chat Assistant Launcher
                        LiquidGlassButton(
                            onClick = onNavigateToChatAi,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("action_chat_ai"),
                            cornerRadius = 20.dp,
                            containerColor = PaletteCornflower
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = "Chat AI",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Chat with Second Brain AI (Firestore Grounded)",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            DashboardSection.VOICE_AI -> {
                // 1. Audio Scrubber & Voice Note Player
                item(key = "audio_player_card") {
                    LiquidGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("groq_ai_voice_card"),
                        cornerRadius = 24.dp,
                        elevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("🎙️", fontSize = 20.sp)
                                    Text(
                                        text = "LATEST VOICE NOTE & SCRUBBER",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        ),
                                        color = PaletteCornflower
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = PaletteMintFrost.copy(alpha = 0.8f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CloudDone,
                                            contentDescription = "Groq Whisper",
                                            tint = PaletteCornflower,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "Whisper-v3 Turbo",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = PaletteCornflower
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Audio Player controls
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White.copy(alpha = 0.80f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "• Raw Audio Transcription:",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = PaletteCornflower
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "\"$latestVoiceNote\"",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = FontFamily.Serif,
                                            color = KamakuraTextPrimary
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = PaletteCornflower,
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clickable { isAudioPlaying = !isAudioPlaying }
                                                .testTag("audio_play_toggle")
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = if (isAudioPlaying) "❚❚" else "▶",
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        Slider(
                                            value = playbackProgress,
                                            onValueChange = { playbackProgress = it },
                                            modifier = Modifier.weight(1f),
                                            colors = SliderDefaults.colors(
                                                thumbColor = PaletteCornflower,
                                                activeTrackColor = PaletteSoftSky,
                                                inactiveTrackColor = PaletteIceCyan.copy(alpha = 0.6f)
                                            )
                                        )

                                        MiniVolumeMeter(isPlaying = isAudioPlaying)
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Groq AI Summary Card
                item(key = "ai_summary_card") {
                    LiquidGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 24.dp,
                        elevation = 2.dp
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = "Groq AI",
                                    tint = PaletteCornflower,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "AI Summary & Action Items",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = PaletteCornflower
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = "qwen/qwen3.8-27b",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                    color = KamakuraTextSecondary
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = latestAiSummary,
                                style = MaterialTheme.typography.bodyMedium.copy(color = KamakuraTextPrimary)
                            )
                        }
                    }
                }

                // 3. Extracted Action Items
                item(key = "action_items_header") {
                    Text(
                        text = "📋 Extracted Action Items (${actionItems.size})",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = PaletteCornflower
                        ),
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }

                items(actionItems, key = { it }) { item ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Item",
                                tint = PaletteCornflower,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = item,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = KamakuraTextPrimary
                            )
                        }
                    }
                }
            }

            DashboardSection.LOGS -> {
                // 1. Pinned GPS Location Log
                item(key = "gps_log_card") {
                    LiquidGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 24.dp,
                        elevation = 2.dp
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "GPS",
                                    tint = PaletteCornflower,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "ANCHORED GPS COORDINATE LOG",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = PaletteCornflower
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = lastPinnedGps ?: "No GPS fix pinned yet. Tap 'Pin GPS' on Overview to record coordinates.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = KamakuraTextPrimary
                                )
                            )
                        }
                    }
                }

                // 2. Safe Sanctum RF Mesh Log
                item(key = "rf_mesh_card") {
                    LiquidGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 24.dp,
                        elevation = 2.dp
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Wifi,
                                    contentDescription = "RF Mesh",
                                    tint = PaletteCornflower,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "RF MESH & BSSID SANCTUM",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = PaletteCornflower
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "• Active Network: $networkType\n• RF Proximity: $proximitySummary\n• Passive Beacon Verification: $passiveBssidCount detected APs in immediate vicinity.",
                                style = MaterialTheme.typography.bodyMedium.copy(color = KamakuraTextPrimary)
                            )
                        }
                    }
                }
            }
        }

        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Deferred Dialog: Quick Thought / Voice Note
    if (showThoughtDialog) {
        Dialog(onDismissRequest = { showThoughtDialog = false }) {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 24.dp,
                containerColor = Color.White
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "🎙️ Record Note / Zero-Retention Thought",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = PaletteCornflower
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Dispatched to Groq whisper-large-v3-turbo & qwen/qwen3.8-27b with zero cloud retention.",
                        style = MaterialTheme.typography.bodySmall,
                        color = KamakuraTextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = userThoughtInput,
                        onValueChange = { userThoughtInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Speak or type action item...") },
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showThoughtDialog = false }) {
                            Text("Cancel", color = KamakuraTextSecondary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        LiquidGlassButton(
                            onClick = {
                                if (userThoughtInput.isNotBlank()) {
                                    viewModel.processQuickThought(userThoughtInput)
                                    userThoughtInput = ""
                                    showThoughtDialog = false
                                }
                            },
                            cornerRadius = 14.dp,
                            containerColor = PaletteCornflower
                        ) {
                            Text(
                                text = "Dispatch AI",
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    }

    // Deferred Dialog: Enter Custom Groq API Key
    if (showGroqKeyDialog) {
        Dialog(onDismissRequest = { showGroqKeyDialog = false }) {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 24.dp,
                containerColor = Color.White
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "🔑 Custom Groq API Key",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = PaletteCornflower
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Key will be securely saved into encrypted Room database. Fallbacks to server-side token if blank.",
                        style = MaterialTheme.typography.bodySmall,
                        color = KamakuraTextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = groqKeyInput,
                        onValueChange = { groqKeyInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("gsk_...") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showGroqKeyDialog = false }) {
                            Text("Cancel", color = KamakuraTextSecondary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        LiquidGlassButton(
                            onClick = {
                                viewModel.saveGroqApiKey(groqKeyInput)
                                showGroqKeyDialog = false
                            },
                            cornerRadius = 14.dp,
                            containerColor = PaletteCornflower
                        ) {
                            Text(
                                text = "Save Key",
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricRow(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String,
    subValue: String,
    progress: Float
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.85f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(42.dp)
            ) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    color = iconTint,
                    trackColor = iconTint.copy(alpha = 0.2f),
                    strokeWidth = 3.5.dp
                )
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "• $label: $value",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = KamakuraTextPrimary
                    )
                )
                Text(
                    text = subValue,
                    style = MaterialTheme.typography.labelSmall,
                    color = KamakuraTextSecondary
                )
            }
        }
    }
}

@Composable
private fun MiniVolumeMeter(isPlaying: Boolean) {
    if (!isPlaying) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.height(18.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(PaletteMintFrost)
            )
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(9.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(PaletteSoftSky)
            )
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(PaletteCornflower)
            )
        }
        return
    }

    val infiniteTransition = rememberInfiniteTransition(label = "audioMeter")
    val meter1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "meter1"
    )
    val meter2 by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "meter2"
    )
    val meter3 by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "meter3"
    )

    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.height(18.dp)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height((18 * meter1).dp)
                .clip(RoundedCornerShape(2.dp))
                .background(PaletteMintFrost)
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .height((18 * meter2).dp)
                .clip(RoundedCornerShape(2.dp))
                .background(PaletteSoftSky)
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .height((18 * meter3).dp)
                .clip(RoundedCornerShape(2.dp))
                .background(PaletteCornflower)
        )
    }
}
