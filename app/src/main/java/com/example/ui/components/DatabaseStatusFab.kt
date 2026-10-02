package com.example.ui.components

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.DatabaseStatusInfo
import com.example.ui.theme.KamakuraTextPrimary
import com.example.ui.theme.KamakuraTextSecondary
import com.example.ui.theme.LiquidGlassButton
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.theme.LiquidGlassFill
import com.example.ui.theme.PaletteCornflower
import com.example.ui.theme.PaletteIceCyan
import com.example.ui.theme.PaletteMintFrost
import com.example.ui.theme.PaletteSoftSky

/**
 * Floating Action Button displaying real-time Room Database connection status.
 * Features an animated pulsing status beacon, latency counter, and opens
 * a detailed inspector dialog upon tap.
 */
@Composable
fun DatabaseStatusFab(
    status: DatabaseStatusInfo,
    onPing: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    val pulseTransition = rememberInfiniteTransition(label = "dbPulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .border(
                width = 1.2.dp,
                brush = Brush.horizontalGradient(
                    listOf(PaletteCornflower, PaletteIceCyan)
                ),
                shape = RoundedCornerShape(22.dp)
            )
            .clickable { showDialog = true }
            .testTag("floating_database_btn"),
        shape = RoundedCornerShape(22.dp),
        color = LiquidGlassFill,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Pulsing live connection beacon
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .scale(if (status.isConnected) pulseScale else 1f)
                        .clip(CircleShape)
                        .background(
                            if (status.isConnected) Color(0x6622C55E) else Color(0x66EF4444)
                        )
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (status.isConnected) Color(0xFF22C55E) else Color(0xFFEF4444)
                        )
                )
            }

            Text(
                text = if (status.isConnected) "DB: CONNECTED" else "DB: OFFLINE",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                ),
                color = PaletteCornflower
            )

            // Latency Pill
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = PaletteIceCyan.copy(alpha = 0.70f)
            ) {
                Text(
                    text = if (status.isConnected) "${status.pingLatencyMs}ms" else "ERR",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    color = PaletteCornflower,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }

    if (showDialog) {
        DatabaseInspectorDialog(
            status = status,
            onPing = onPing,
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
fun DatabaseInspectorDialog(
    status: DatabaseStatusInfo,
    onPing: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        LiquidGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("database_inspector_dialog"),
            cornerRadius = 24.dp,
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🗄️", fontSize = 24.sp)
                        Column {
                            Text(
                                text = "ROOM DATABASE ENGINE",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = PaletteCornflower
                            )
                            Text(
                                text = "Local Encrypted SQLite Persistence",
                                style = MaterialTheme.typography.labelSmall,
                                color = KamakuraTextSecondary
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (status.isConnected) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                    ) {
                        Text(
                            text = if (status.isConnected) "ONLINE" else "ERROR",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (status.isConnected) Color(0xFF15803D) else Color(0xFFB91C1C),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                HorizontalDivider(color = PaletteIceCyan.copy(alpha = 0.5f))

                // Database details
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DbInfoRow(label = "Filename", value = status.databaseName)
                    DbInfoRow(label = "Schema Version", value = "v${status.schemaVersion} (Room SQLite WAL)")
                    DbInfoRow(
                        label = "Connection Ping",
                        value = if (status.isConnected) "${status.pingLatencyMs} ms (Healthy)" else "Unreachable"
                    )
                }

                HorizontalDivider(color = PaletteIceCyan.copy(alpha = 0.5f))

                // Entity Table Counts
                Text(
                    text = "SYNCED LOCAL ENTITY TABLES",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = PaletteCornflower
                )

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = PaletteMintFrost.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TableRecordRow(icon = "📝", tableName = "voice_tasks", count = status.voiceTasksCount)
                        TableRecordRow(icon = "⏱️", tableName = "app_limits", count = status.appLimitsCount)
                        TableRecordRow(icon = "🛡️", tableName = "coaching_schedule", count = status.coachingCount)
                        TableRecordRow(icon = "🎙️", tableName = "lecture_recordings", count = status.lectureCount)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LiquidGlassButton(
                        onClick = onPing,
                        cornerRadius = 14.dp,
                        containerColor = PaletteIceCyan.copy(alpha = 0.85f),
                        modifier = Modifier.testTag("db_dialog_ping_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Ping",
                                tint = PaletteCornflower,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Ping DB Now",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = PaletteCornflower
                            )
                        }
                    }

                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "Close",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = PaletteCornflower
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DbInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = KamakuraTextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            ),
            color = KamakuraTextPrimary
        )
    }
}

@Composable
private fun TableRecordRow(icon: String, tableName: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = icon, fontSize = 14.sp)
            Text(
                text = tableName,
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                color = KamakuraTextPrimary
            )
        }
        Text(
            text = "$count records",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = PaletteCornflower
        )
    }
}
