package com.example.ui.screens.coaching

import android.graphics.Bitmap
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.CoachingEntity
import com.example.ui.EnforcerViewModel
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberBg
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberGreen
import com.example.ui.theme.CyberRed
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.CyberTextMuted

@Composable
fun CoachingScreen(
    viewModel: EnforcerViewModel,
    onNavigateToRecorder: (() -> Unit)? = null
) {
    val coachingList by viewModel.coachingList.collectAsStateWithLifecycle()
    val previewBitmap by viewModel.wallpaperPreviewBitmap.collectAsStateWithLifecycle()
    val geofenceSilentActive by viewModel.geofenceActiveStatus.collectAsStateWithLifecycle()
    val isClassSilentActive by viewModel.isClassSilentActive.collectAsStateWithLifecycle()
    val lectures by viewModel.lectures.collectAsStateWithLifecycle()

    val activeEnforcerMode by viewModel.activeEnforcerMode.collectAsStateWithLifecycle()
    val isWifiAutoMuteEnabled by viewModel.isWifiAutoMuteEnabled.collectAsStateWithLifecycle()
    val isGeofenceAutoRecordEnabled by viewModel.isGeofenceAutoRecordEnabled.collectAsStateWithLifecycle()
    val isTimetableAutoRecordEnabled by viewModel.isTimetableAutoRecordEnabled.collectAsStateWithLifecycle()
    val savedWifiKeywords by viewModel.savedWifiKeywords.collectAsStateWithLifecycle()
    val detectedSsid by viewModel.detectedSsid.collectAsStateWithLifecycle()
    val activeLocationGeofence by viewModel.activeLocationGeofence.collectAsStateWithLifecycle()

    var showWifiEditDialog by remember { mutableStateOf(false) }
    var tempWifiKeywords by remember { mutableStateOf("") }

    var showPreviewDialog by remember { mutableStateOf(false) }
    val checkedMap = remember { mutableStateMapOf<String, Boolean>() }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBg)
            .padding(horizontal = 20.dp)
            .verticalScroll(scrollState)
            .testTag("coaching_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Title Header
        Column {
            Text(
                text = "FOCUS ROUTINES & CHECKLIST",
                style = MaterialTheme.typography.headlineMedium,
                color = CyberCyan
            )
            Text(
                text = "DYNAMIC WALLPAPER // 100M GEOFENCE ENGINE",
                style = MaterialTheme.typography.labelSmall,
                color = CyberGreen
            )
        }

        // Lecture Vault Quick Launch Banner
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.6f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToRecorder?.invoke() }
                .testTag("coaching_lecture_vault_banner")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CyberCyan.copy(alpha = 0.15f))
                            .border(1.dp, CyberCyan, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "MEETING & THOUGHT RECORDER",
                            color = CyberCyan,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "${lectures.size} lectures saved • Tap to capture or review",
                            color = Color(0xFF94A3B8),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Open Recorder",
                    tint = CyberCyan,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Smart Mode & Environment Banner (STUDY_MODE <-> CLASS_RECORDER_MODE)
        Card(
            colors = CardDefaults.cardColors(
                containerColor = when (activeEnforcerMode) {
                    com.example.core.coaching.ActiveEnforcerMode.CLASS_RECORDER_MODE -> Color(0xFF1B2A1E)
                    com.example.core.coaching.ActiveEnforcerMode.SAFE_CLASS_MODE -> Color(0xFF1E2838)
                    else -> CyberSurface
                }
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                when (activeEnforcerMode) {
                    com.example.core.coaching.ActiveEnforcerMode.CLASS_RECORDER_MODE -> CyberGreen
                    com.example.core.coaching.ActiveEnforcerMode.SAFE_CLASS_MODE -> CyberAmber
                    else -> CyberCyan.copy(alpha = 0.5f)
                }
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("active_mode_status_card")
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when (activeEnforcerMode) {
                                        com.example.core.coaching.ActiveEnforcerMode.CLASS_RECORDER_MODE -> CyberGreen.copy(alpha = 0.2f)
                                        com.example.core.coaching.ActiveEnforcerMode.SAFE_CLASS_MODE -> CyberAmber.copy(alpha = 0.2f)
                                        else -> CyberCyan.copy(alpha = 0.15f)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (activeEnforcerMode) {
                                    com.example.core.coaching.ActiveEnforcerMode.CLASS_RECORDER_MODE -> Icons.Default.Mic
                                    com.example.core.coaching.ActiveEnforcerMode.SAFE_CLASS_MODE -> Icons.Default.VolumeMute
                                    else -> Icons.Default.Security
                                },
                                contentDescription = null,
                                tint = when (activeEnforcerMode) {
                                    com.example.core.coaching.ActiveEnforcerMode.CLASS_RECORDER_MODE -> CyberGreen
                                    com.example.core.coaching.ActiveEnforcerMode.SAFE_CLASS_MODE -> CyberAmber
                                    else -> CyberCyan
                                },
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = activeEnforcerMode.label,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = when (activeEnforcerMode) {
                                    com.example.core.coaching.ActiveEnforcerMode.CLASS_RECORDER_MODE -> CyberGreen
                                    com.example.core.coaching.ActiveEnforcerMode.SAFE_CLASS_MODE -> CyberAmber
                                    else -> CyberCyan
                                }
                            )
                            Text(
                                text = if (activeLocationGeofence != null) "Inside perimeter: $activeLocationGeofence" else "Outside focus perimeter",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                when (activeEnforcerMode) {
                                    com.example.core.coaching.ActiveEnforcerMode.CLASS_RECORDER_MODE -> CyberGreen
                                    com.example.core.coaching.ActiveEnforcerMode.SAFE_CLASS_MODE -> CyberAmber
                                    else -> CyberSurfaceVariant
                                }
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (activeEnforcerMode == com.example.core.coaching.ActiveEnforcerMode.STUDY_MODE) "STUDY" else "IN-CLASS",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = if (activeEnforcerMode == com.example.core.coaching.ActiveEnforcerMode.STUDY_MODE) Color.White else CyberBg
                        )
                    }
                }

                Text(
                    text = activeEnforcerMode.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )

                // Quick Geofence Simulation Triggers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val firstCenter = coachingList.firstOrNull()
                    OutlinedButton(
                        onClick = {
                            if (firstCenter != null) {
                                viewModel.simulateEnterCoachingCenter(firstCenter)
                            } else {
                                viewModel.simulateGeofenceArrival("Focus Sanctum")
                            }
                        },
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(34.dp).testTag("btn_sim_enter_center")
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = CyberGreen, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ENTER SANCTUM", color = CyberGreen, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { viewModel.simulateLeaveCoachingCenter() },
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(34.dp).testTag("btn_sim_leave_center")
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("LEAVE CENTER", color = CyberCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Smart Wi-Fi Auto-Mute Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF223252)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("wifi_auto_mute_card")
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isWifiAutoMuteEnabled) CyberCyan.copy(alpha = 0.2f) else CyberSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isWifiAutoMuteEnabled) Icons.Default.Wifi else Icons.Default.WifiOff,
                                contentDescription = null,
                                tint = if (isWifiAutoMuteEnabled) CyberCyan else CyberTextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "SMART WI-FI AUTO-MUTE",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                            Text(
                                text = if (detectedSsid != null) "Current SSID: \"$detectedSsid\"" else "Wi-Fi: Scanning nearby APs",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = if (detectedSsid != null) CyberCyan else Color(0xFF94A3B8)
                            )
                        }
                    }

                    Switch(
                        checked = isWifiAutoMuteEnabled,
                        onCheckedChange = { viewModel.setWifiAutoMuteEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberBg,
                            checkedTrackColor = CyberCyan,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = CyberSurfaceVariant
                        ),
                        modifier = Modifier.testTag("switch_wifi_auto_mute")
                    )
                }

                Text(
                    text = "Automatically silences phone & triggers Focus Shield whenever connected to office or workspace Wi-Fi networks.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )

                Text(
                    text = "MATCHED SSIDS: $savedWifiKeywords",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = CyberTextMuted
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.scanAndApplyWifiClassShield() },
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f).height(32.dp).testTag("btn_scan_wifi_now")
                    ) {
                        Text("SCAN WI-FI NOW", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = CyberCyan)
                    }

                    OutlinedButton(
                        onClick = {
                            tempWifiKeywords = savedWifiKeywords
                            showWifiEditDialog = true
                        },
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberAmber.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(32.dp).testTag("btn_edit_wifi_ssids")
                    ) {
                        Text("EDIT SSIDS", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = CyberAmber)
                    }
                }
            }
        }

        // Automated Triggers: Geofence & Timetable Auto-Record Controls
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF223252)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("automation_triggers_card")
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "AUTOMATED CLASS RECORDING TRIGGERS",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = CyberGreen
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Geofence Auto-Record",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            text = "Start lecture recorder upon entering center perimeter",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    Switch(
                        checked = isGeofenceAutoRecordEnabled,
                        onCheckedChange = { viewModel.setGeofenceAutoRecordEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberBg,
                            checkedTrackColor = CyberGreen,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = CyberSurfaceVariant
                        ),
                        modifier = Modifier.testTag("switch_geofence_auto_record")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Timetable Auto-Record",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            text = "Trigger background lecture capture when scheduled session starts",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    Switch(
                        checked = isTimetableAutoRecordEnabled,
                        onCheckedChange = { viewModel.setTimetableAutoRecordEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberBg,
                            checkedTrackColor = CyberGreen,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = CyberSurfaceVariant
                        ),
                        modifier = Modifier.testTag("switch_timetable_auto_record")
                    )
                }
            }
        }

        // Timetable-based Class Auto-Silent Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isClassSilentActive) Color(0xFF1B2A1E) else CyberSurface
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isClassSilentActive) CyberGreen else CyberCyan.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("coaching_timetable_silent_card")
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isClassSilentActive) CyberGreen.copy(alpha = 0.2f) else CyberCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isClassSilentActive) Icons.Default.VolumeMute else Icons.Default.Schedule,
                                contentDescription = null,
                                tint = if (isClassSilentActive) CyberGreen else CyberCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "TIMETABLE AUTO-SILENT ENGINE",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (isClassSilentActive) CyberGreen else CyberCyan
                            )
                            Text(
                                text = if (isClassSilentActive) "CLASS IN SESSION • RINGER MUTED" else "SCHEDULE ARMED • STANDBY",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = if (isClassSilentActive) CyberGreen else Color(0xFF94A3B8)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isClassSilentActive) CyberGreen else CyberSurfaceVariant)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isClassSilentActive) "MUTED" else "NORMAL",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = if (isClassSilentActive) CyberBg else Color.White
                        )
                    }
                }

                Text(
                    text = "Automatically mutes your phone's ringer and notification streams during designated focus sessions to prevent disturbances during deep work.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.manuallyTriggerClassSilent() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isClassSilentActive) CyberAmber else CyberGreen
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .testTag("btn_trigger_class_silent")
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeMute,
                            contentDescription = null,
                            tint = CyberBg,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "MUTE NOW",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = CyberBg
                        )
                    }

                    OutlinedButton(
                        onClick = { viewModel.manuallyRestoreClassRinger() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .testTag("btn_restore_class_ringer")
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "RESTORE RINGER",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Coaching Sessions
        coachingList.forEach { coaching ->
            CoachingItemCard(
                coaching = coaching,
                checkedMap = checkedMap,
                onToggleCheck = { item ->
                    val key = "${coaching.id}_$item"
                    checkedMap[key] = !(checkedMap[key] ?: false)
                },
                onApplyWallpaper = {
                    viewModel.applyLockscreenWallpaper(coaching)
                },
                onPreviewWallpaper = {
                    viewModel.generateWallpaperPreview(coaching)
                    showPreviewDialog = true
                },
                onSimulateArrival = {
                    viewModel.simulateGeofenceArrival(coaching.title)
                },
                onSimulateDeparture = {
                    viewModel.simulateGeofenceDeparture(coaching.title)
                },
                onRecordLecture = {
                    onNavigateToRecorder?.invoke()
                }
            )
        }

        // Wallpaper Reset / I Remember Section
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF223252)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Restore,
                        contentDescription = null,
                        tint = CyberAmber,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "LOCKSCREEN RECOVERY // 'I REMEMBER'",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }

                Text(
                    text = "Once you've packed your essentials or started your session, tap below to clear the checklist from your lockscreen and restore your wallpaper.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF94A3B8)
                )

                Button(
                    onClick = { viewModel.restoreOriginalWallpaper() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("i_remember_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = CyberGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "I REMEMBER (CLEAR WALLPAPER)",
                        color = CyberGreen,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }

    // Wallpaper Preview Dialog
    if (showPreviewDialog && previewBitmap != null) {
        AlertDialog(
            onDismissRequest = { showPreviewDialog = false },
            containerColor = CyberSurface,
            title = {
                Text(
                    text = "LOCKSCREEN WALLPAPER PREVIEW",
                    style = MaterialTheme.typography.titleMedium,
                    color = CyberCyan
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 200.dp, height = 360.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(2.dp, CyberCyan, RoundedCornerShape(12.dp))
                    ) {
                        Image(
                            bitmap = previewBitmap!!.asImageBitmap(),
                            contentDescription = "Wallpaper Preview",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "High-contrast dynamic Canvas bitmap",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showPreviewDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
                ) {
                    Text("CLOSE PREVIEW", color = CyberBg)
                }
            }
        )
    }

    // Wi-Fi SSIDs Configuration Dialog
    if (showWifiEditDialog) {
        AlertDialog(
            onDismissRequest = { showWifiEditDialog = false },
            containerColor = CyberSurface,
            title = {
                Text(
                    text = "CONFIGURE SCHOOL/CAMPUS WI-FI",
                    style = MaterialTheme.typography.titleMedium,
                    color = CyberCyan
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Enter comma-separated network names (SSID keywords) that represent your office, studio, or coworking space.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                    OutlinedTextField(
                        value = tempWifiKeywords,
                        onValueChange = { tempWifiKeywords = it },
                        placeholder = { Text("e.g. Office,Coworking,Studio,Library", color = CyberTextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = CyberBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = CyberCyan
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("input_wifi_ssids"),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateWifiKeywords(tempWifiKeywords)
                        showWifiEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
                ) {
                    Text("SAVE SSIDS", color = CyberBg)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showWifiEditDialog = false },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberTextMuted)
                ) {
                    Text("CANCEL")
                }
            }
        )
    }
}

@Composable
fun CoachingItemCard(
    coaching: CoachingEntity,
    checkedMap: Map<String, Boolean>,
    onToggleCheck: (String) -> Unit,
    onApplyWallpaper: () -> Unit,
    onPreviewWallpaper: () -> Unit,
    onSimulateArrival: () -> Unit,
    onSimulateDeparture: () -> Unit,
    onRecordLecture: () -> Unit
) {
    val items = coaching.checklist.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    Card(
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF223252)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().testTag("coaching_card_${coaching.id}")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = coaching.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }
            }

            // Time Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyberSurfaceVariant)
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            text = "DEPARTURE",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = coaching.departureTime,
                            style = MaterialTheme.typography.titleLarge,
                            color = CyberAmber
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyberSurfaceVariant)
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            text = "SESSION START",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = coaching.sessionTime,
                            style = MaterialTheme.typography.titleLarge,
                            color = CyberGreen
                        )
                    }
                }
            }

            // Checklist Section
            Text(
                text = "MANDATORY ITEMS CHECKLIST:",
                style = MaterialTheme.typography.labelSmall,
                color = CyberCyan
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items.forEach { item ->
                    val isChecked = checkedMap["${coaching.id}_$item"] ?: false
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isChecked) Color(0xFF14241B) else CyberSurfaceVariant)
                            .clickable { onToggleCheck(item) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (isChecked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                            contentDescription = item,
                            tint = if (isChecked) CyberGreen else Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = item,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isChecked) CyberGreen else Color(0xFFE2E8F0)
                        )
                    }
                }
            }

            // Wallpaper Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onApplyWallpaper,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).testTag("apply_wallpaper_btn_${coaching.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Wallpaper,
                        contentDescription = null,
                        tint = CyberBg,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SET WALLPAPER",
                        color = CyberBg,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                OutlinedButton(
                    onClick = onPreviewWallpaper,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).testTag("preview_wallpaper_btn_${coaching.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PREVIEW",
                        color = CyberCyan,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            // Geofence Simulation Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSimulateArrival,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeMute,
                        contentDescription = null,
                        tint = CyberGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ENTER 100m (MUTE)",
                        color = CyberGreen,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                Button(
                    onClick = onSimulateDeparture,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "EXIT 100m (UNMUTE)",
                        color = CyberCyan,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            // Lecture Recording Direct Trigger
            Button(
                onClick = onRecordLecture,
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan.copy(alpha = 0.15f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("record_lecture_btn_${coaching.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    tint = CyberCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "RECORD LECTURE FOR THIS CLASS",
                    color = CyberCyan,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
