package com.example.ui.screens.alarm

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.alarm.SmartAlarmScheduler
import com.example.ui.EnforcerViewModel
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberBg
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberGreen
import com.example.ui.theme.CyberRed
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceVariant
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AlarmScreen(
    viewModel: EnforcerViewModel,
    modifier: Modifier = Modifier
) {
    val isMasterEnabled by viewModel.isAlarmMasterEnabled.collectAsState()
    val isClassShieldActive by viewModel.isClassShieldActive.collectAsState()
    val silenceUntilMillis by viewModel.silenceUntilMillis.collectAsState()
    val autoCoachingSuppress by viewModel.autoCoachingSuppress.collectAsState()
    val isVibrateOnly by viewModel.isVibrateOnly.collectAsState()
    val scheduledAlarmTime by viewModel.scheduledAlarmTime.collectAsState()
    val scheduledAlarmReason by viewModel.scheduledAlarmReason.collectAsState()
    val isAlarmRingingNow by viewModel.isAlarmRingingNow.collectAsState()

    // Live clock ticks every 5 seconds for accurate dynamic previews
    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTimeMillis = System.currentTimeMillis()
            delay(5000L)
        }
    }

    val isSilencedNow = silenceUntilMillis > currentTimeMillis

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. HEADER & MASTER SWITCH ---
        AlarmHeaderSection(
            isMasterEnabled = isMasterEnabled,
            onToggleMaster = { viewModel.setAlarmMasterEnabled(it) }
        )

        // --- 2. ACTIVE ALARM BANNER (If scheduled or ringing) ---
        if (isAlarmRingingNow || scheduledAlarmTime > currentTimeMillis) {
            ActiveAlarmBanner(
                isRinging = isAlarmRingingNow,
                triggerTime = scheduledAlarmTime,
                reason = scheduledAlarmReason,
                onCancel = { viewModel.cancelActiveAlarm() },
                onEmergencySilence = { viewModel.emergencyKillAllAlarms() }
            )
        }

        // --- 3. DISABLE ALARM IN ANY CONDITION / CLASS SHIELD ---
        ClassSafetySection(
            isClassShieldActive = isClassShieldActive,
            isSilencedNow = isSilencedNow,
            silenceUntilMillis = silenceUntilMillis,
            autoCoachingSuppress = autoCoachingSuppress,
            isVibrateOnly = isVibrateOnly,
            onEmergencyKill = { viewModel.emergencyKillAllAlarms() },
            onToggleClassShield = { viewModel.setClassShieldActive(it) },
            onSilenceDuration = { viewModel.silenceAlarmsForDuration(it) },
            onClearSilence = { viewModel.clearTimedSilence() },
            onToggleCoachingSuppress = { viewModel.setAutoCoachingSuppress(it) },
            onToggleVibrateOnly = { viewModel.setVibrateOnly(it) }
        )

        // --- 4. AUTOMATIC 6-HOUR SLEEP CALCULATOR ---
        SixHourSleepCalculatorSection(
            currentTimeMillis = currentTimeMillis,
            scheduledAlarmTime = scheduledAlarmTime,
            onScheduleWakeup = { buffer -> viewModel.schedule6HourWakeup(buffer) },
            onCancelAlarm = { viewModel.cancelActiveAlarm() }
        )

        // --- 5. MANUAL ALARM SCHEDULER ---
        ManualAlarmSchedulerSection(
            currentTimeMillis = currentTimeMillis,
            onScheduleManual = { h, m, label -> viewModel.scheduleManualAlarm(h, m, label) },
            onSchedulePreset = { mins, label -> viewModel.schedulePresetAlarm(mins, label) }
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ==========================================
// 1. HEADER & MASTER ENGINE
// ==========================================

@Composable
private fun AlarmHeaderSection(
    isMasterEnabled: Boolean,
    onToggleMaster: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CyberBorder, RoundedCornerShape(14.dp))
            .testTag("alarm_header_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isMasterEnabled) CyberGreen else CyberRed)
                    )
                    Text(
                        text = "ENFORCER OS // ALARM CORE",
                        style = MaterialTheme.typography.labelMedium,
                        color = CyberCyan,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Alarm & Sleep Engine",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isMasterEnabled) "Active • Armed for exact schedule" else "DISARMED • Suppressed in all conditions",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isMasterEnabled) Color(0xFF94A3B8) else CyberAmber
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Switch(
                    checked = isMasterEnabled,
                    onCheckedChange = onToggleMaster,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyberCyan,
                        checkedTrackColor = CyberCyan.copy(alpha = 0.3f),
                        uncheckedThumbColor = Color(0xFF64748B),
                        uncheckedTrackColor = Color(0xFF1E293B)
                    ),
                    modifier = Modifier.testTag("master_alarm_switch")
                )
                Text(
                    text = if (isMasterEnabled) "MASTER ON" else "MASTER OFF",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isMasterEnabled) CyberCyan else Color(0xFF64748B),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ==========================================
// 2. ACTIVE ALARM BANNER
// ==========================================

@Composable
private fun ActiveAlarmBanner(
    isRinging: Boolean,
    triggerTime: Long,
    reason: String,
    onCancel: () -> Unit,
    onEmergencySilence: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val formattedTime = if (triggerTime > 0) sdf.format(Date(triggerTime)) else "Active"

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isRinging) CyberRed.copy(alpha = 0.25f) else CyberSurfaceVariant
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isRinging) 2.dp else 1.dp,
                color = if (isRinging) CyberRed else CyberCyan,
                shape = RoundedCornerShape(14.dp)
            )
            .testTag("active_alarm_banner")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isRinging) Icons.Default.Warning else Icons.Default.Alarm,
                        contentDescription = null,
                        tint = if (isRinging) CyberRed else CyberCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (isRinging) "🚨 ALARM IS RINGING RIGHT NOW!" else "ACTIVE SCHEDULED ALARM",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isRinging) CyberRed else CyberCyan,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (triggerTime > 0 && !isRinging) {
                    val remainingMs = triggerTime - System.currentTimeMillis()
                    val remainingHrs = remainingMs / (3600 * 1000)
                    val remainingMins = (remainingMs % (3600 * 1000)) / (60 * 1000)
                    val countdownStr = if (remainingHrs > 0) "in ${remainingHrs}h ${remainingMins}m" else "in ${remainingMins}m"

                    Text(
                        text = countdownStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    if (reason.isNotEmpty()) {
                        Text(
                            text = reason,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isRinging) {
                        Button(
                            onClick = onEmergencySilence,
                            colors = ButtonDefaults.buttonColors(containerColor = CyberRed),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("banner_emergency_silence")
                        ) {
                            Icon(imageVector = Icons.Default.VolumeOff, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("SILENCE NOW", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        OutlinedButton(
                            onClick = onCancel,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberAmber),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberAmber),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("banner_cancel_alarm")
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("DISARM")
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. DISABLE ALARM IN ANY CONDITION / CLASS SHIELD
// ==========================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ClassSafetySection(
    isClassShieldActive: Boolean,
    isSilencedNow: Boolean,
    silenceUntilMillis: Long,
    autoCoachingSuppress: Boolean,
    isVibrateOnly: Boolean,
    onEmergencyKill: () -> Unit,
    onToggleClassShield: (Boolean) -> Unit,
    onSilenceDuration: (Float) -> Unit,
    onClearSilence: () -> Unit,
    onToggleCoachingSuppress: (Boolean) -> Unit,
    onToggleVibrateOnly: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CyberBorder, RoundedCornerShape(14.dp))
            .testTag("class_safety_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Section Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = CyberRed,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "CLASSROOM SAFEGUARD & KILL SWITCH",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "Guarantees your alarm never rings, strobes, or blares during lectures, exams, or quiet classrooms.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8)
            )

            // EMERGENCY KILL BUTTON
            Button(
                onClick = onEmergencyKill,
                colors = ButtonDefaults.buttonColors(containerColor = CyberRed),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("emergency_kill_alarm_button")
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeOff,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = "🚨 EMERGENCY KILL ALL ALARMS (ANY CONDITION)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = "Instantly stops blaring siren, strobe, vibration & clears pending alarms",
                        color = Color(0xFFFFD1D1),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            HorizontalDivider(color = CyberBorder, thickness = 1.dp)

            // CLASS SHIELD TOGGLE
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Safe Class Shield",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Unconditionally blocks all alarm sound & flash until turned off",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                }
                Switch(
                    checked = isClassShieldActive,
                    onCheckedChange = onToggleClassShield,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyberRed,
                        checkedTrackColor = CyberRed.copy(alpha = 0.3f),
                        uncheckedThumbColor = Color(0xFF64748B),
                        uncheckedTrackColor = Color(0xFF1E293B)
                    ),
                    modifier = Modifier.testTag("class_shield_switch")
                )
            }

            // TIMED CLASS SILENCE SHORTCUTS
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Silence For Lecture Duration:",
                        style = MaterialTheme.typography.labelMedium,
                        color = CyberCyan,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (isSilencedNow) {
                        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                        Text(
                            text = "Muted until ${sdf.format(Date(silenceUntilMillis))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberAmber,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val durations = listOf(
                        1.0f to "1 Hour",
                        1.5f to "1.5 Hours",
                        2.0f to "2 Hours",
                        4.0f to "4 Hours"
                    )

                    durations.forEach { (hrs, label) ->
                        OutlinedButton(
                            onClick = { onSilenceDuration(hrs) },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = CyberCyan
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("silence_${label.replace(" ", "_")}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(label, style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    if (isSilencedNow) {
                        OutlinedButton(
                            onClick = onClearSilence,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberAmber),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberAmber),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("clear_timed_silence_button")
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear Mute", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            HorizontalDivider(color = CyberBorder, thickness = 1.dp)

            // AUTO-COACHING TIMETABLE SUPPRESSION
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = CyberGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text(
                            text = "Auto-Mute In Timetable Classes",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Silences alarm if time coincides with your Coaching Timetable",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
                Switch(
                    checked = autoCoachingSuppress,
                    onCheckedChange = onToggleCoachingSuppress,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyberGreen,
                        checkedTrackColor = CyberGreen.copy(alpha = 0.3f),
                        uncheckedThumbColor = Color(0xFF64748B),
                        uncheckedTrackColor = Color(0xFF1E293B)
                    ),
                    modifier = Modifier.testTag("auto_coaching_suppress_switch")
                )
            }

            // VIBRATE-ONLY STEALTH MODE
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Vibration,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text(
                            text = "Stealth Vibration Mode",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Silent vibration instead of blaring 100% volume siren & strobe",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
                Switch(
                    checked = isVibrateOnly,
                    onCheckedChange = onToggleVibrateOnly,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyberCyan,
                        checkedTrackColor = CyberCyan.copy(alpha = 0.3f),
                        uncheckedThumbColor = Color(0xFF64748B),
                        uncheckedTrackColor = Color(0xFF1E293B)
                    ),
                    modifier = Modifier.testTag("stealth_vibrate_only_switch")
                )
            }
        }
    }
}

// ==========================================
// 4. AUTOMATIC 6-HOUR SLEEP CALCULATOR
// ==========================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SixHourSleepCalculatorSection(
    currentTimeMillis: Long,
    scheduledAlarmTime: Long,
    onScheduleWakeup: (Int) -> Unit,
    onCancelAlarm: () -> Unit
) {
    var selectedBufferMinutes by remember { mutableIntStateOf(15) }
    val sdfTime = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    // Calculate wake time from current time with selected buffer
    val calculatedWakeMillis = SmartAlarmScheduler.calculate6HourWakeTime(
        currentTimeMillis,
        selectedBufferMinutes
    )
    val calculatedWakeFormatted = sdfTime.format(Date(calculatedWakeMillis))

    Card(
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CyberBorder, RoundedCornerShape(14.dp))
            .testTag("six_hour_calculator_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Bedtime,
                    contentDescription = null,
                    tint = CyberCyan,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "AUTOMATIC 6-HOUR SLEEP CALCULATOR",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "Scientifically timed to 4 complete 90-minute sleep cycles. Waking between cycles prevents grogginess and morning sleep inertia.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8)
            )

            // Current Time -> Wake Time Card
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberSurfaceVariant),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberBorder, RoundedCornerShape(10.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "SLEEP TIME",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = sdfTime.format(Date(currentTimeMillis)),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Now",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberGreen
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "+6h 00m",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberCyan,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (selectedBufferMinutes > 0) "+${selectedBufferMinutes}m buffer" else "direct",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "OPTIMAL WAKE TIME",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberCyan
                        )
                        Text(
                            text = calculatedWakeFormatted,
                            style = MaterialTheme.typography.titleLarge,
                            color = CyberCyan,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Cycle 4 End",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberGreen
                        )
                    }
                }
            }

            // Fall Asleep Buffer Selection
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Time to Fall Asleep Buffer:",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFCBD5E1)
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val buffers = listOf(
                        0 to "0 min (Instant)",
                        10 to "10 min (Quick)",
                        15 to "15 min (Standard)",
                        30 to "30 min (Relaxed)"
                    )

                    buffers.forEach { (mins, label) ->
                        FilterChip(
                            selected = selectedBufferMinutes == mins,
                            onClick = { selectedBufferMinutes = mins },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyberCyan.copy(alpha = 0.2f),
                                selectedLabelColor = CyberCyan,
                                containerColor = CyberBg,
                                labelColor = Color(0xFF94A3B8)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedBufferMinutes == mins,
                                borderColor = CyberBorder,
                                selectedBorderColor = CyberCyan,
                                borderWidth = 1.dp
                            ),
                            modifier = Modifier.testTag("buffer_chip_$mins")
                        )
                    }
                }
            }

            // 4 Sleep Cycles Breakdown
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(CyberBg)
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "4-Cycle Biological Regeneration:",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8),
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CycleBadge("Cycle 1", "0 - 1.5h", "Deep Slow-Wave")
                    CycleBadge("Cycle 2", "1.5 - 3h", "HGH & Cellular")
                    CycleBadge("Cycle 3", "3 - 4.5h", "Memory Consol.")
                    CycleBadge("Cycle 4", "4.5 - 6h", "Peak REM Wake")
                }
            }

            // Action Button to Arm 6-Hour Alarm
            Button(
                onClick = { onScheduleWakeup(selectedBufferMinutes) },
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("arm_6hour_alarm_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Alarm,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "LOCK 6H SLEEP & ARM ALARM FOR $calculatedWakeFormatted",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun CycleBadge(cycle: String, duration: String, desc: String) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(CyberSurface)
            .border(1.dp, CyberBorder, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = cycle, style = MaterialTheme.typography.labelSmall, color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 9.sp)
        Text(text = duration, style = MaterialTheme.typography.labelSmall, color = Color.White, fontSize = 8.sp)
        Text(text = desc, style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B), fontSize = 7.sp)
    }
}

// ==========================================
// 5. MANUAL ALARM SCHEDULER
// ==========================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ManualAlarmSchedulerSection(
    currentTimeMillis: Long,
    onScheduleManual: (Int, Int, String) -> Unit,
    onSchedulePreset: (Int, String) -> Unit
) {
    val currentCal = remember(currentTimeMillis) {
        Calendar.getInstance().apply { timeInMillis = currentTimeMillis }
    }

    // Default to next hour
    var selectedHour by remember {
        val nextH = (currentCal.get(Calendar.HOUR_OF_DAY) + 1) % 24
        mutableIntStateOf(if (nextH % 12 == 0) 12 else nextH % 12)
    }
    var selectedMinute by remember { mutableIntStateOf(0) }
    var isAm by remember {
        val nextH = (currentCal.get(Calendar.HOUR_OF_DAY) + 1) % 24
        mutableStateOf(nextH < 12)
    }
    var alarmLabel by remember { mutableStateOf("Wake Up & Conquer") }

    // Calculated 24-hour hourOfDay
    val hourOfDay = if (isAm) {
        if (selectedHour == 12) 0 else selectedHour
    } else {
        if (selectedHour == 12) 12 else selectedHour + 12
    }

    // Calculate when this alarm will ring
    val previewCal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hourOfDay)
        set(Calendar.MINUTE, selectedMinute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (timeInMillis <= currentTimeMillis) {
            add(Calendar.DAY_OF_YEAR, 1)
        }
    }

    val diffMs = previewCal.timeInMillis - currentTimeMillis
    val diffHours = diffMs / (3600 * 1000)
    val diffMins = (diffMs % (3600 * 1000)) / (60 * 1000)
    val dayLabel = if (previewCal.get(Calendar.DAY_OF_YEAR) == currentCal.get(Calendar.DAY_OF_YEAR)) "Today" else "Tomorrow"
    val sdfTime = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val previewFormatted = sdfTime.format(previewCal.time)

    Card(
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CyberBorder, RoundedCornerShape(14.dp))
            .testTag("manual_alarm_scheduler_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = CyberGreen,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "MANUAL ALARM SCHEDULER",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "Pick exact hour and minute for custom study sessions, class dismissal alerts, or morning routines.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8)
            )

            // TIME PICKER CONTROLS
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberSurfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberBorder, RoundedCornerShape(12.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Hour : Minute & AM/PM Selector
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Hour Picker
                        TimeNumberPicker(
                            value = selectedHour,
                            range = 1..12,
                            onValueChange = { selectedHour = it },
                            label = "HOUR"
                        )

                        Text(
                            text = ":",
                            style = MaterialTheme.typography.displaySmall,
                            color = CyberCyan,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        // Minute Picker
                        TimeNumberPicker(
                            value = selectedMinute,
                            range = 0..59,
                            step = 5,
                            formatTwoDigits = true,
                            onValueChange = { selectedMinute = it },
                            label = "MIN"
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        // AM / PM Toggle
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilterChip(
                                selected = isAm,
                                onClick = { isAm = true },
                                label = { Text("AM", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyberCyan,
                                    selectedLabelColor = Color.Black,
                                    containerColor = CyberBg,
                                    labelColor = Color(0xFF94A3B8)
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isAm,
                                    borderColor = CyberBorder,
                                    selectedBorderColor = CyberCyan,
                                    borderWidth = 1.dp
                                ),
                                modifier = Modifier.testTag("chip_am")
                            )

                            FilterChip(
                                selected = !isAm,
                                onClick = { isAm = false },
                                label = { Text("PM", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyberCyan,
                                    selectedLabelColor = Color.Black,
                                    containerColor = CyberBg,
                                    labelColor = Color(0xFF94A3B8)
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = !isAm,
                                    borderColor = CyberBorder,
                                    selectedBorderColor = CyberCyan,
                                    borderWidth = 1.dp
                                ),
                                modifier = Modifier.testTag("chip_pm")
                            )
                        }
                    }

                    // Calculation Preview
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(CyberBg)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Rings $dayLabel at $previewFormatted (in ${diffHours}h ${diffMins}m)",
                            style = MaterialTheme.typography.labelMedium,
                            color = CyberGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Alarm Label TextField
            OutlinedTextField(
                value = alarmLabel,
                onValueChange = { alarmLabel = it },
                label = { Text("Alarm Label / Reason") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyberCyan,
                    unfocusedBorderColor = CyberBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color(0xFFCBD5E1),
                    focusedLabelColor = CyberCyan,
                    unfocusedLabelColor = Color(0xFF94A3B8),
                    cursorColor = CyberCyan
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("alarm_label_input")
            )

            // SET ALARM BUTTON
            Button(
                onClick = { onScheduleManual(hourOfDay, selectedMinute, alarmLabel) },
                colors = ButtonDefaults.buttonColors(containerColor = CyberGreen),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("set_manual_alarm_button")
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SET ALARM FOR $previewFormatted",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge
                )
            }

            HorizontalDivider(color = CyberBorder, thickness = 1.dp)

            // QUICK NAP / PRESET BUTTONS
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Quick Power Nap & Focus Presets:",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFCBD5E1)
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val presets = listOf(
                        15 to "15m Power Nap",
                        30 to "30m Nap",
                        45 to "45m Focus Break",
                        90 to "90m Full Cycle"
                    )

                    presets.forEach { (mins, label) ->
                        OutlinedButton(
                            onClick = { onSchedulePreset(mins, label) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("preset_${mins}m")
                        ) {
                            Icon(imageVector = Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(label, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeNumberPicker(
    value: Int,
    range: IntRange,
    step: Int = 1,
    formatTwoDigits: Boolean = false,
    onValueChange: (Int) -> Unit,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(CyberBg)
                .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            IconButton(
                onClick = {
                    val nextVal = if (value - step < range.first) range.last else value - step
                    onValueChange(nextVal)
                },
                modifier = Modifier.size(32.dp)
            ) {
                Text("-", color = CyberCyan, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            val displayText = if (formatTwoDigits) String.format(Locale.US, "%02d", value) else value.toString()
            Text(
                text = displayText,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            IconButton(
                onClick = {
                    val nextVal = if (value + step > range.last) range.first else value + step
                    onValueChange(nextVal)
                },
                modifier = Modifier.size(32.dp)
            ) {
                Text("+", color = CyberCyan, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}
