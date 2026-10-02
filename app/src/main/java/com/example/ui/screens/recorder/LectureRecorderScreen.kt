package com.example.ui.screens.recorder

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.recorder.AudioChunkRecorder
import com.example.core.recorder.CompletedChunk
import com.example.core.recorder.CompletedRecording
import com.example.core.recorder.DistanceMicGain
import com.example.core.recorder.DisguiseProfile
import com.example.core.recorder.LectureRecorderManager
import com.example.core.recorder.RecordingState
import com.example.core.recorder.StealthCaptureService
import com.example.core.recorder.UploadQueueItem
import com.example.core.recorder.UploadQueueStatus
import com.example.data.local.entity.LectureBookmark
import com.example.data.local.entity.LectureEntity
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
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LectureRecorderScreen(
    viewModel: EnforcerViewModel,
    initialSubject: String = "",
    onNavigateToUploads: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    val recordingState by viewModel.recordingState.collectAsStateWithLifecycle()
    val recordingDuration by viewModel.recordingDurationSeconds.collectAsStateWithLifecycle()
    val currentAmp by viewModel.recordingAmplitude.collectAsStateWithLifecycle()
    val ampHistory by viewModel.recordingAmplitudeHistory.collectAsStateWithLifecycle()
    val activeBookmarks by viewModel.activeBookmarks.collectAsStateWithLifecycle()

    val isStealthActive by viewModel.isStealthServiceRunning.collectAsStateWithLifecycle()
    val activeStealthProfile by viewModel.activeStealthProfile.collectAsStateWithLifecycle()
    val chunkIdx by viewModel.chunkCurrentIndex.collectAsStateWithLifecycle()

    val lectures by viewModel.lectures.collectAsStateWithLifecycle()
    val voiceTasks by viewModel.allVoiceTasks.collectAsStateWithLifecycle()

    // Permission launcher
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (!granted) {
            Toast.makeText(context, "Microphone permission is required to record lectures", Toast.LENGTH_LONG).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBg)
            .padding(horizontal = 16.dp)
            .testTag("lecture_recorder_screen")
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "LECTURE RECORDER",
                    style = MaterialTheme.typography.headlineMedium,
                    color = CyberCyan
                )
                Text(
                    text = "HIGH-FIDELITY AUDIO // LIVE WAVEFORM // REVISION VAULT",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberTextMuted
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (onNavigateToUploads != null) {
                    IconButton(
                        onClick = onNavigateToUploads,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CyberSurface)
                            .border(1.dp, CyberCyan.copy(alpha = 0.5f), CircleShape)
                            .testTag("btn_top_uploads_monitor")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = "WorkManager Uploads Monitor",
                            tint = CyberCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (isStealthActive) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CyberGreen.copy(alpha = 0.2f))
                            .border(1.dp, CyberGreen, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Mic, contentDescription = null, tint = CyberGreen, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "CHUNK P-$chunkIdx",
                                color = CyberGreen,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                } else if (recordingState != RecordingState.IDLE) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (recordingState == RecordingState.RECORDING) CyberRed.copy(alpha = 0.2f) else CyberAmber.copy(alpha = 0.2f))
                            .border(1.dp, if (recordingState == RecordingState.RECORDING) CyberRed else CyberAmber, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (recordingState == RecordingState.RECORDING) "● LIVE" else "❚❚ PAUSED",
                            color = if (recordingState == RecordingState.RECORDING) CyberRed else CyberAmber,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tab Row
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = CyberSurface,
            contentColor = CyberCyan,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = CyberCyan,
                    height = 3.dp
                )
            },
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, CyberBorder, RoundedCornerShape(10.dp))
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (selectedTab == 0) (if (isStealthActive) CyberGreen else CyberCyan) else CyberTextMuted
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isStealthActive) "RECORDING ACTIVE (P-$chunkIdx)" else if (recordingState != RecordingState.IDLE) "RECORDING (LIVE)" else "RECORD LECTURE",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                },
                selectedContentColor = CyberCyan,
                unselectedContentColor = CyberTextMuted
            )

            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (selectedTab == 1) CyberCyan else CyberTextMuted
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "VAULT (${lectures.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                },
                selectedContentColor = CyberCyan,
                unselectedContentColor = CyberTextMuted
            )

            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Assignment,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (selectedTab == 2) CyberAmber else CyberTextMuted
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "VOICE TASKS (${voiceTasks.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                },
                selectedContentColor = CyberAmber,
                unselectedContentColor = CyberTextMuted
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        when (selectedTab) {
            0 -> {
                RecordLectureTab(
                    viewModel = viewModel,
                    hasMicPermission = hasMicPermission,
                    onRequestPermission = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                    recordingState = recordingState,
                    durationSeconds = recordingDuration,
                    currentAmp = currentAmp,
                    ampHistory = ampHistory,
                    activeBookmarks = activeBookmarks,
                    onNavigateToVault = { selectedTab = 1 },
                    initialSubject = initialSubject,
                    onNavigateToUploads = onNavigateToUploads
                )
            }
            1 -> {
                LectureVaultTab(
                    viewModel = viewModel,
                    lectures = lectures
                )
            }
            else -> {
                VoiceTasksTab(
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
fun RecordLectureTab(
    viewModel: EnforcerViewModel,
    hasMicPermission: Boolean,
    onRequestPermission: () -> Unit,
    recordingState: RecordingState,
    durationSeconds: Long,
    currentAmp: Float,
    ampHistory: List<Float>,
    activeBookmarks: List<LectureBookmark>,
    onNavigateToVault: () -> Unit,
    initialSubject: String,
    onNavigateToUploads: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coachingList by viewModel.coachingList.collectAsStateWithLifecycle()

    val isStealthActive by viewModel.isStealthServiceRunning.collectAsStateWithLifecycle()
    val activeStealthProfile by viewModel.activeStealthProfile.collectAsStateWithLifecycle()
    val chunkRecordingState by viewModel.chunkRecordingState.collectAsStateWithLifecycle()
    val chunkIdx by viewModel.chunkCurrentIndex.collectAsStateWithLifecycle()
    val chunkElapsed by viewModel.chunkElapsedSeconds.collectAsStateWithLifecycle()
    val chunkTotalElapsed by viewModel.chunkTotalElapsedSeconds.collectAsStateWithLifecycle()
    val chunkLimit by viewModel.chunkLimitSeconds.collectAsStateWithLifecycle()
    val completedChunks by viewModel.completedChunks.collectAsStateWithLifecycle()
    val chunkAmp by viewModel.chunkAmplitude.collectAsStateWithLifecycle()
    val chunkAmpHistory by viewModel.chunkAmplitudeHistory.collectAsStateWithLifecycle()
    val chunkBookmarks by viewModel.chunkBookmarks.collectAsStateWithLifecycle()

    // Audio DSP, Gain Booster & Lecture Hall Voice Focus
    val isVoiceFocusEnabled by viewModel.isVoiceFocusEnabled.collectAsStateWithLifecycle()
    val isAcHumFilterEnabled by viewModel.isAcHumFilterEnabled.collectAsStateWithLifecycle()
    val distanceGain by viewModel.distanceGain.collectAsStateWithLifecycle()

    // Pocket Gesture & Silent Haptic Cues
    val isPocketGestureEnabled by viewModel.isPocketGestureEnabled.collectAsStateWithLifecycle()
    val isHapticCuesEnabled by viewModel.isHapticCuesEnabled.collectAsStateWithLifecycle()

    // Telegram Cloud Pipe & Wipe States
    val telegramBotToken by viewModel.telegramBotToken.collectAsStateWithLifecycle()
    val telegramChatId by viewModel.telegramChatId.collectAsStateWithLifecycle()
    val telegramCloudPipeEnabled by viewModel.telegramCloudPipeEnabled.collectAsStateWithLifecycle()
    val telegramWipedFilesCount by viewModel.telegramWipedFilesCount.collectAsStateWithLifecycle()
    val telegramTotalBytesWiped by viewModel.telegramTotalBytesWiped.collectAsStateWithLifecycle()
    val telegramLastUploadStatus by viewModel.telegramLastUploadStatus.collectAsStateWithLifecycle()
    val telegramUploadQueue by viewModel.telegramUploadQueue.collectAsStateWithLifecycle()

    var botTokenInput by remember(telegramBotToken) { mutableStateOf(telegramBotToken) }
    var chatIdInput by remember(telegramChatId) { mutableStateOf(telegramChatId) }
    var isTokenMasked by remember { mutableStateOf(true) }
    var isTestingPing by remember { mutableStateOf(false) }
    var pingTestFeedback by remember { mutableStateOf<String?>(null) }

    var lectureTitle by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf(if (initialSubject.isNotBlank()) initialSubject else "Physics") }
    var selectedCoachingSession by remember { mutableStateOf(coachingList.firstOrNull()?.title ?: "") }
    var notesText by remember { mutableStateOf("") }
    var newBookmarkLabel by remember { mutableStateOf("") }

    var isStealthModeToggled by remember { mutableStateOf(false) }
    var selectedDisguiseProfile by remember { mutableStateOf(DisguiseProfile.GOOGLE_PLAY_SERVICES) }
    var selectedChunkSeconds by remember { mutableStateOf(15 * 60L) }
    var secretBookmarkInput by remember { mutableStateOf("") }

    val subjects = listOf("Physics", "Chemistry", "Mathematics", "Biology", "General")

    if (!hasMicPermission) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            colors = CardDefaults.cardColors(containerColor = CyberSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberAmber)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.MicOff,
                    contentDescription = null,
                    tint = CyberAmber,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "MICROPHONE PERMISSION REQUIRED",
                    style = MaterialTheme.typography.titleMedium,
                    color = CyberTextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Enforcer OS requires microphone access to capture high-clarity lecture audio and display live waveform telemetry.",
                    style = MaterialTheme.typography.bodySmall,
                    color = CyberTextSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onRequestPermission,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = CyberBg),
                    modifier = Modifier.testTag("grant_mic_permission_button")
                ) {
                    Text("GRANT MICROPHONE ACCESS", fontWeight = FontWeight.Bold)
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // --- Live Waveform & Meter Card (Always visible or enlarged during recording) ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("audio_waveform_card"),
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isStealthActive) CyberGreen else if (recordingState == RecordingState.RECORDING) CyberCyan else CyberBorder
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header inside card
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isStealthActive) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = CyberGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = if (isStealthActive) "CHUNK ACOUSTIC TELEMETRY [PART #$chunkIdx]" else "ACOUSTIC WAVEFORM TELEMETRY",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = if (isStealthActive) CyberGreen else CyberCyan
                            )
                        }
                        val chunkTimeDisplay = when {
                            selectedChunkSeconds >= AudioChunkRecorder.INFINITE_CHUNK_SECONDS -> "CONTINUOUS (∞)"
                            selectedChunkSeconds < 60 -> "${selectedChunkSeconds}s CHUNKS"
                            else -> "${selectedChunkSeconds / 60}m CHUNKS"
                        }
                        Text(
                            text = if (isStealthActive) "ROLLING $chunkTimeDisplay" else "AAC 44.1kHz • 128kbps",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = CyberTextMuted
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Waveform Canvas
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CyberBg)
                            .border(1.dp, CyberBorder.copy(alpha = 0.6f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        LiveWaveformCanvas(
                            amplitudes = if (isStealthActive) chunkAmpHistory else ampHistory,
                            isRecording = if (isStealthActive) (chunkRecordingState == RecordingState.RECORDING) else (recordingState == RecordingState.RECORDING)
                        )

                        if (recordingState == RecordingState.IDLE && !isStealthActive) {
                            Text(
                                text = "MICROPHONE STANDBY // READY TO RECORD",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = CyberTextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Digital Timer & Peak Decibel
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isStealthActive) "ACTIVE CHUNK ELAPSED" else "ELAPSED TIME",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = CyberTextMuted
                            )
                            val displaySeconds = if (isStealthActive) chunkElapsed else durationSeconds
                            Text(
                                text = AudioChunkRecorder.formatSeconds(displaySeconds),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 32.sp,
                                color = if (isStealthActive) CyberGreen else if (recordingState == RecordingState.RECORDING) CyberCyan else if (recordingState == RecordingState.PAUSED) CyberAmber else CyberTextPrimary
                            )
                            if (isStealthActive) {
                                Text(
                                    text = "Session Total: ${AudioChunkRecorder.formatSeconds(chunkTotalElapsed)} • Rollover: ${AudioChunkRecorder.formatSeconds(chunkLimit)}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = CyberTextSecondary
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (isStealthActive) "CURRENT CHUNK // PEAK" else "EST. SIZE // PEAK",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = CyberTextMuted
                            )
                            if (isStealthActive) {
                                Text(
                                    text = "Part #$chunkIdx • ${(chunkAmp * 100).toInt()}%",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp,
                                    color = CyberGreen
                                )
                            } else {
                                val estBytes = durationSeconds * 16000L
                                Text(
                                    text = "${LectureRecorderManager.formatBytes(estBytes)} • ${(currentAmp * 100).toInt()}%",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp,
                                    color = if (currentAmp > 0.8f) CyberRed else CyberGreen
                                )
                            }
                        }
                    }

                    // Rolling Chunk Progress Bar
                    if (isStealthActive) {
                        Spacer(modifier = Modifier.height(10.dp))
                        val progressFraction = (chunkElapsed.toFloat() / chunkLimit.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = CyberGreen,
                            trackColor = CyberSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Part $chunkIdx ${(progressFraction * 100).toInt()}% complete",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                color = CyberTextMuted
                            )
                            Text(
                                text = "Auto-rolls to Part ${chunkIdx + 1} at ${AudioChunkRecorder.formatSeconds(chunkLimit)}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                color = CyberGreen
                            )
                        }
                    }
                }
            }
        }

        // --- Audio Gain Booster & Lecture Hall Voice Focus Panel ---
        item {
            AudioGainAndVoiceFocusCard(
                isVoiceFocusEnabled = isVoiceFocusEnabled,
                isAcHumFilterEnabled = isAcHumFilterEnabled,
                currentGain = distanceGain,
                onToggleVoiceFocus = { viewModel.toggleVoiceFocus() },
                onToggleHumFilter = { viewModel.toggleAcHumFilter() },
                onSelectGain = { viewModel.setDistanceGain(it) }
            )
        }

        // --- Pocket Gesture & Silent Haptic Cues ---
        item {
            PocketGestureCard(
                isPocketGestureEnabled = isPocketGestureEnabled,
                isHapticCuesEnabled = isHapticCuesEnabled,
                onTogglePocketGesture = { viewModel.togglePocketGesture() },
                onToggleHapticCues = { viewModel.toggleHapticCues() }
            )
        }

        // --- Controls Section ---
        item {
            if (isStealthActive) {
                // ==========================================
                // ACTIVE ROLLING CHUNK RECORDER CONSOLE
                // ==========================================
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("stealth_active_card"),
                    colors = CardDefaults.cardColors(containerColor = CyberSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberGreen)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Session Status Banner
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Mic, contentDescription = null, tint = CyberGreen, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "ROLLING CHUNK RECORDING ACTIVE",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = CyberGreen
                                    )
                                    val activeChunkDesc = when {
                                        selectedChunkSeconds >= AudioChunkRecorder.INFINITE_CHUNK_SECONDS -> "Continuous mode (∞)"
                                        selectedChunkSeconds < 60 -> "Auto-saving every ${selectedChunkSeconds}s"
                                        else -> "Auto-saving every ${selectedChunkSeconds / 60}m"
                                    }
                                    Text(
                                        text = "Part #$chunkIdx • $activeChunkDesc",
                                        fontSize = 11.sp,
                                        color = CyberTextSecondary
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (chunkRecordingState == RecordingState.RECORDING) CyberRed.copy(alpha = 0.2f) else CyberAmber.copy(alpha = 0.2f))
                                    .border(1.dp, if (chunkRecordingState == RecordingState.RECORDING) CyberRed else CyberAmber, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (chunkRecordingState == RecordingState.RECORDING) "● LIVE" else "❚❚ PAUSED",
                                    color = if (chunkRecordingState == RecordingState.RECORDING) CyberRed else CyberAmber,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        // Notification Status Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = CyberBg),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder.copy(alpha = 0.8f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.GraphicEq,
                                            contentDescription = null,
                                            tint = CyberCyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Foreground Service Active",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp,
                                            color = CyberTextPrimary
                                        )
                                    }
                                    Text(
                                        text = "lecture_audio_recorder",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = CyberTextMuted
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Visible in Android notification shade with real-time controls.",
                                    fontSize = 11.sp,
                                    color = CyberTextSecondary
                                )
                            }
                        }

                        // Cloud Pipe & Wipe Active Telemetry Card
                        if (telegramCloudPipeEnabled) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = CyberBg),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.CloudUpload, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "CLOUD PIPE & WIPE ACTIVE",
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = CyberCyan
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(CyberGreen.copy(alpha = 0.2f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "0 MB FOOTPRINT",
                                                color = CyberGreen,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Text(
                                        text = telegramLastUploadStatus,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = CyberTextPrimary
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Auto-Wipe: HTTP 200 OK -> Local Delete",
                                            fontSize = 9.sp,
                                            color = CyberTextMuted
                                        )
                                        Text(
                                            text = "Wiped: $telegramWipedFilesCount files (${AudioChunkRecorder.formatBytes(telegramTotalBytesWiped)})",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.sp,
                                            color = CyberGreen
                                        )
                                    }

                                    if (telegramUploadQueue.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            telegramUploadQueue.takeLast(3).forEach { item ->
                                                val itemColor = when (item.status) {
                                                    UploadQueueStatus.QUEUED -> CyberAmber
                                                    UploadQueueStatus.IN_FLIGHT -> CyberCyan
                                                    UploadQueueStatus.COMPLETED -> CyberGreen
                                                    UploadQueueStatus.FAILED_RETRYING -> CyberRed
                                                }
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = item.chunkTitle,
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 9.sp,
                                                        color = CyberTextSecondary
                                                    )
                                                    Text(
                                                        text = item.status.name,
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = itemColor
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (onNavigateToUploads != null) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Button(
                                            onClick = onNavigateToUploads,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(34.dp)
                                                .testTag("btn_recorder_to_uploads"),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = CyberCyan.copy(alpha = 0.15f)
                                            ),
                                            border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f)),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CloudUpload,
                                                contentDescription = null,
                                                tint = CyberCyan,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "VIEW LIVE WORKMANAGER UPLOADS",
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                color = CyberCyan
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Completed Chunks in this Session
                        if (completedChunks.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "SAVED CHUNKS IN THIS SESSION (${completedChunks.size})",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = CyberTextMuted
                                )
                                completedChunks.forEach { chunk ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(CyberSurfaceVariant)
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Save, contentDescription = null, tint = CyberGreen, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Part ${chunk.chunkIndex} (${AudioChunkRecorder.formatSeconds(chunk.durationSeconds)})",
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp,
                                                color = CyberTextPrimary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        Text(
                                            text = "${LectureRecorderManager.formatBytes(chunk.fileSizeBytes)} • VAULT SAVED",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.sp,
                                            color = CyberGreen
                                        )
                                    }
                                }
                            }
                        }

                        // Stealth Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (chunkRecordingState == RecordingState.RECORDING) {
                                        viewModel.pauseStealthRecording()
                                    } else {
                                        viewModel.resumeStealthRecording()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (chunkRecordingState == RecordingState.RECORDING) CyberAmber else CyberGreen,
                                    contentColor = CyberBg
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("stealth_pause_resume_button")
                            ) {
                                Icon(
                                    imageVector = if (chunkRecordingState == RecordingState.RECORDING) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (chunkRecordingState == RecordingState.RECORDING) "PAUSE" else "RESUME",
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                onClick = {
                                    viewModel.stopStealthRecording()
                                    onNavigateToVault()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyberGreen,
                                    contentColor = CyberBg
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("stealth_stop_save_button")
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("FINALIZE & SAVE", fontWeight = FontWeight.Bold)
                            }
                        }

                        // Discard Stealth Session
                        OutlinedButton(
                            onClick = { viewModel.cancelStealthRecording() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberRed),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberRed.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .testTag("stealth_cancel_button")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("DISCARD RECORDING SESSION", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Divider(color = CyberBorder)

                        // Revision Bookmark Stamper
                        Text(
                            text = "ADD REVISION BOOKMARK",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = CyberGreen
                        )

                        val bookmarkQuickLabels = listOf("Key Insight", "Action Point", "Core Idea", "Decision Taken", "Follow-up")
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(bookmarkQuickLabels) { label ->
                                FilterChip(
                                    selected = false,
                                    onClick = { viewModel.addStealthBookmark(label) },
                                    label = { Text("+ $label", fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = CyberSurfaceVariant,
                                        labelColor = CyberGreen
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = false,
                                        borderColor = CyberBorder
                                    )
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = secretBookmarkInput,
                                onValueChange = { secretBookmarkInput = it },
                                placeholder = { Text("Bookmark note (e.g. Important step)...", fontSize = 12.sp) },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("secret_bookmark_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyberGreen,
                                    unfocusedBorderColor = CyberBorder,
                                    focusedTextColor = CyberTextPrimary,
                                    unfocusedTextColor = CyberTextPrimary
                                )
                            )

                            Button(
                                onClick = {
                                    if (secretBookmarkInput.isNotBlank()) {
                                        viewModel.addStealthBookmark(secretBookmarkInput)
                                        secretBookmarkInput = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyberGreen,
                                    contentColor = CyberBg
                                ),
                                modifier = Modifier.testTag("add_secret_bookmark_button")
                            ) {
                                Icon(Icons.Default.BookmarkAdd, contentDescription = null)
                            }
                        }

                        if (chunkBookmarks.isNotEmpty()) {
                            Text(
                                text = "SESSION BOOKMARKS (${chunkBookmarks.size})",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = CyberTextMuted
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                chunkBookmarks.forEach { bm ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(CyberSurfaceVariant)
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = AudioChunkRecorder.formatSeconds(bm.timeSeconds),
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = CyberGreen
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = bm.label,
                                            fontSize = 12.sp,
                                            color = CyberTextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (recordingState == RecordingState.IDLE) {
                // ==========================================
                // SETUP & START FORM (NORMAL OR STEALTH)
                // ==========================================
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CyberSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isStealthModeToggled) CyberGreen else CyberBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = if (isStealthModeToggled) "ROLLING CHUNK RECORDING SETUP" else "LECTURE DETAILS",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (isStealthModeToggled) CyberGreen else CyberCyan
                        )

                        // Subject chips
                        Column {
                            Text(
                                text = "Subject",
                                style = MaterialTheme.typography.labelMedium,
                                color = CyberTextSecondary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(subjects) { subject ->
                                    val isSelected = selectedSubject == subject
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedSubject = subject },
                                        label = { Text(subject, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = if (isStealthModeToggled) CyberGreen else CyberCyan,
                                            selectedLabelColor = CyberBg,
                                            containerColor = CyberSurfaceVariant,
                                            labelColor = CyberTextPrimary
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = isSelected,
                                            borderColor = CyberBorder,
                                            selectedBorderColor = if (isStealthModeToggled) CyberGreen else CyberCyan
                                        )
                                    )
                                }
                            }
                        }

                        // Lecture Title TextField
                        OutlinedTextField(
                            value = lectureTitle,
                            onValueChange = { lectureTitle = it },
                            label = { Text("Lecture Title / Topic (e.g. Kinematics L-3)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("lecture_title_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (isStealthModeToggled) CyberGreen else CyberCyan,
                                unfocusedBorderColor = CyberBorder,
                                focusedTextColor = CyberTextPrimary,
                                unfocusedTextColor = CyberTextPrimary
                            )
                        )

                        // Link to coaching session
                        if (coachingList.isNotEmpty()) {
                            Column {
                                Text(
                                    text = "Link to Focus Routine",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = CyberTextSecondary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(coachingList) { session ->
                                        val isSelected = selectedCoachingSession == session.title
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                selectedCoachingSession = session.title
                                                if (session.title.contains("Physics", ignoreCase = true)) selectedSubject = "Physics"
                                                if (session.title.contains("Chemistry", ignoreCase = true)) selectedSubject = "Chemistry"
                                                if (session.title.contains("Math", ignoreCase = true)) selectedSubject = "Mathematics"
                                            },
                                            label = { Text(session.title, maxLines = 1) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = CyberGreen,
                                                selectedLabelColor = CyberBg,
                                                containerColor = CyberSurfaceVariant,
                                                labelColor = CyberTextPrimary
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // Notes TextField
                        OutlinedTextField(
                            value = notesText,
                            onValueChange = { notesText = it },
                            label = { Text("Initial Notes / Formula keywords (Optional)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("lecture_notes_input"),
                            maxLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (isStealthModeToggled) CyberGreen else CyberCyan,
                                unfocusedBorderColor = CyberBorder,
                                focusedTextColor = CyberTextPrimary,
                                unfocusedTextColor = CyberTextPrimary
                            )
                        )

                        // -------------------------------------------------------------
                        // ROLLING CHUNK MODE & INTERVAL CONFIGURATION
                        // -------------------------------------------------------------
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("stealth_toggle_card"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isStealthModeToggled) CyberGreen.copy(alpha = 0.08f) else CyberSurfaceVariant
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isStealthModeToggled) CyberGreen else CyberBorder
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.GraphicEq,
                                            contentDescription = null,
                                            tint = if (isStealthModeToggled) CyberGreen else CyberTextSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "ROLLING CHUNK RECORDING",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = if (isStealthModeToggled) CyberGreen else CyberTextPrimary
                                            )
                                            val toggleSubtext = when {
                                                selectedChunkSeconds >= AudioChunkRecorder.INFINITE_CHUNK_SECONDS -> "Continuous stealth capture without chunk cutoff"
                                                selectedChunkSeconds < 60 -> "Auto-splits long lectures into clean ${selectedChunkSeconds}s audio chapters"
                                                else -> "Auto-splits long lectures into clean ${selectedChunkSeconds / 60}-min audio chapters"
                                            }
                                            Text(
                                                text = toggleSubtext,
                                                fontSize = 11.sp,
                                                color = CyberTextSecondary
                                            )
                                        }
                                    }

                                    Switch(
                                        checked = isStealthModeToggled,
                                        onCheckedChange = { isStealthModeToggled = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = CyberBg,
                                            checkedTrackColor = CyberGreen,
                                            uncheckedThumbColor = CyberTextSecondary,
                                            uncheckedTrackColor = CyberBg
                                        ),
                                        modifier = Modifier.testTag("stealth_mode_switch")
                                    )
                                }

                                if (isStealthModeToggled) {
                                    Text(
                                        text = "Audio is seamlessly saved in continuous chunks without missing a second. Ensures zero data loss even if phone battery drains during long 2-3 hour classes.",
                                        fontSize = 11.sp,
                                        color = CyberTextSecondary
                                    )

                                    // Rolling Chunk Duration Selector (Adjustable from 10s to Infinite)
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Chunk Duration (10s to ∞)",
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = CyberGreen
                                            )
                                            Text(
                                                text = when {
                                                    selectedChunkSeconds >= AudioChunkRecorder.INFINITE_CHUNK_SECONDS -> "Continuous (∞)"
                                                    selectedChunkSeconds < 60 -> "${selectedChunkSeconds}s"
                                                    else -> "${selectedChunkSeconds / 60}m"
                                                },
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = CyberCyan
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            val durationOptions = listOf(
                                                10L to "10s",
                                                30L to "30s",
                                                60L to "1m",
                                                300L to "5m",
                                                900L to "15m",
                                                1800L to "30m",
                                                AudioChunkRecorder.INFINITE_CHUNK_SECONDS to "∞ Infinite"
                                            )
                                            items(durationOptions) { (secs, label) ->
                                                val isSelected = selectedChunkSeconds == secs
                                                FilterChip(
                                                    selected = isSelected,
                                                    onClick = { selectedChunkSeconds = secs },
                                                    label = { Text(label, fontSize = 11.sp) },
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = CyberGreen,
                                                        selectedLabelColor = CyberBg,
                                                        containerColor = CyberBg,
                                                        labelColor = CyberTextPrimary
                                                    ),
                                                    border = FilterChipDefaults.filterChipBorder(
                                                        enabled = true,
                                                        selected = isSelected,
                                                        borderColor = CyberBorder,
                                                        selectedBorderColor = CyberGreen
                                                    )
                                                )
                                            }
                                        }
                                    }

                                    // Notification Info Banner
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(CyberBg)
                                            .border(1.dp, CyberBorder, RoundedCornerShape(6.dp))
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Security, contentDescription = null, tint = CyberGreen, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Notification displays audio status with quick Pause, Bookmark, and Stop controls.",
                                            fontSize = 10.sp,
                                            color = CyberTextSecondary
                                        )
                                    }

                                    // Cloud Pipe & Wipe (Telegram Bot API) Section
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = CyberBg),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.CloudUpload, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Column {
                                                        Text(
                                                            text = "CLOUD PIPE & WIPE",
                                                            fontFamily = FontFamily.Monospace,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 11.sp,
                                                            color = CyberCyan
                                                        )
                                                        Text(
                                                            text = "0 MB Device Storage Footprint",
                                                            fontSize = 10.sp,
                                                            color = CyberTextSecondary
                                                        )
                                                    }
                                                }

                                                Switch(
                                                    checked = telegramCloudPipeEnabled,
                                                    onCheckedChange = { viewModel.setTelegramCloudPipeEnabled(it) },
                                                    modifier = Modifier.testTag("telegram_cloud_pipe_switch"),
                                                    colors = SwitchDefaults.colors(
                                                        checkedThumbColor = CyberBg,
                                                        checkedTrackColor = CyberCyan,
                                                        uncheckedThumbColor = CyberTextMuted,
                                                        uncheckedTrackColor = CyberSurfaceVariant
                                                    )
                                                )
                                            }

                                            Text(
                                                text = "Each completed audio chunk is securely streamed via background worker to your Telegram Bot/Channel. Once confirmed uploaded (HTTP 200 OK), local cached chunks can be deleted automatically to preserve 0 MB phone storage.",
                                                fontSize = 10.sp,
                                                color = CyberTextSecondary,
                                                lineHeight = 14.sp
                                            )

                                            // Telegram Credentials
                                            OutlinedTextField(
                                                value = botTokenInput,
                                                onValueChange = { botTokenInput = it },
                                                label = { Text("Telegram Bot Token", fontSize = 11.sp) },
                                                placeholder = { Text("e.g. 123456789:ABCdefGhIJKlmNoPQRsTUVwxyZ", fontSize = 10.sp) },
                                                visualTransformation = if (isTokenMasked) PasswordVisualTransformation() else VisualTransformation.None,
                                                trailingIcon = {
                                                    IconButton(onClick = { isTokenMasked = !isTokenMasked }) {
                                                        Icon(
                                                            imageVector = if (isTokenMasked) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                            contentDescription = "Toggle token visibility",
                                                            tint = CyberCyan,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("telegram_bot_token_input"),
                                                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = CyberTextPrimary),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = CyberCyan,
                                                    unfocusedBorderColor = CyberBorder,
                                                    focusedLabelColor = CyberCyan,
                                                    cursorColor = CyberCyan
                                                ),
                                                singleLine = true
                                            )

                                            OutlinedTextField(
                                                value = chatIdInput,
                                                onValueChange = { chatIdInput = it },
                                                label = { Text("Telegram Chat ID / Channel ID", fontSize = 11.sp) },
                                                placeholder = { Text("e.g. 987654321 or -100123456789", fontSize = 10.sp) },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("telegram_chat_id_input"),
                                                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = CyberTextPrimary),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = CyberCyan,
                                                    unfocusedBorderColor = CyberBorder,
                                                    focusedLabelColor = CyberCyan,
                                                    cursorColor = CyberCyan
                                                ),
                                                singleLine = true
                                            )

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedButton(
                                                    onClick = {
                                                        viewModel.updateTelegramCredentials(botTokenInput.trim(), chatIdInput.trim())
                                                    },
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(6.dp),
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan)
                                                ) {
                                                    Icon(Icons.Default.Save, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Save Bot Info", fontSize = 10.sp, color = CyberCyan)
                                                }

                                                OutlinedButton(
                                                    onClick = {
                                                        isTestingPing = true
                                                        pingTestFeedback = "Connecting to Telegram..."
                                                        viewModel.updateTelegramCredentials(botTokenInput.trim(), chatIdInput.trim())
                                                        viewModel.testTelegramConnection { success, message ->
                                                            isTestingPing = false
                                                            pingTestFeedback = if (success) "✓ Verified: $message" else "✗ Failed: $message"
                                                            Toast.makeText(context, pingTestFeedback, Toast.LENGTH_SHORT).show()
                                                        }
                                                    },
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(6.dp),
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberGreen)
                                                ) {
                                                    Icon(Icons.Default.Send, contentDescription = null, tint = CyberGreen, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(if (isTestingPing) "Testing..." else "Test Ping", fontSize = 10.sp, color = CyberGreen)
                                                }
                                            }

                                            if (pingTestFeedback != null) {
                                                Text(
                                                    text = pingTestFeedback ?: "",
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 10.sp,
                                                    color = if (pingTestFeedback?.startsWith("✓") == true) CyberGreen else CyberAmber
                                                )
                                            }

                                            // Storage Telemetry Banner
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(CyberSurfaceVariant)
                                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Local Footprint: 0 MB",
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 10.sp,
                                                    color = CyberGreen,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "Wiped: $telegramWipedFilesCount chunks (${AudioChunkRecorder.formatBytes(telegramTotalBytesWiped)})",
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 9.sp,
                                                    color = CyberTextMuted
                                                )
                                            }

                                            // ==========================================
                                            // UPLOAD QUEUE DASHBOARD
                                            // ==========================================
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(CyberBg)
                                                    .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
                                                    .padding(10.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            imageVector = Icons.Default.CloudUpload,
                                                            contentDescription = null,
                                                            tint = CyberCyan,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = "UPLOAD QUEUE DASHBOARD",
                                                            fontFamily = FontFamily.Monospace,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = CyberCyan
                                                        )
                                                    }
                                                    if (telegramUploadQueue.any { it.status == UploadQueueStatus.COMPLETED }) {
                                                        Text(
                                                            text = "Clear Done",
                                                            fontFamily = FontFamily.Monospace,
                                                            fontSize = 9.sp,
                                                            color = CyberTextMuted,
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .clickable { viewModel.clearCompletedUploadQueue() }
                                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(6.dp))

                                                if (telegramUploadQueue.isEmpty()) {
                                                    Text(
                                                        text = "No audio uploads in queue. Chunks will appear here when recorded.",
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 9.sp,
                                                        color = CyberTextMuted,
                                                        modifier = Modifier.padding(vertical = 4.dp)
                                                    )
                                                } else {
                                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        telegramUploadQueue.forEach { queueItem ->
                                                            val statusColor = when (queueItem.status) {
                                                                UploadQueueStatus.QUEUED -> CyberAmber
                                                                UploadQueueStatus.IN_FLIGHT -> CyberCyan
                                                                UploadQueueStatus.COMPLETED -> CyberGreen
                                                                UploadQueueStatus.FAILED_RETRYING -> CyberRed
                                                            }
                                                            val statusText = when (queueItem.status) {
                                                                UploadQueueStatus.QUEUED -> "QUEUED"
                                                                UploadQueueStatus.IN_FLIGHT -> "IN FLIGHT"
                                                                UploadQueueStatus.COMPLETED -> "UPLOADED & WIPED"
                                                                UploadQueueStatus.FAILED_RETRYING -> "RETRYING (${queueItem.retryCount})"
                                                            }

                                                            Column(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .clip(RoundedCornerShape(6.dp))
                                                                    .background(CyberSurface)
                                                                    .padding(8.dp)
                                                            ) {
                                                                Row(
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                                    verticalAlignment = Alignment.CenterVertically
                                                                ) {
                                                                    Text(
                                                                        text = queueItem.chunkTitle,
                                                                        fontFamily = FontFamily.Monospace,
                                                                        fontSize = 10.sp,
                                                                        fontWeight = FontWeight.SemiBold,
                                                                        color = CyberTextPrimary,
                                                                        maxLines = 1
                                                                    )
                                                                    Text(
                                                                        text = statusText,
                                                                        fontFamily = FontFamily.Monospace,
                                                                        fontSize = 9.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = statusColor
                                                                    )
                                                                }

                                                                Spacer(modifier = Modifier.height(4.dp))

                                                                if (queueItem.status == UploadQueueStatus.IN_FLIGHT) {
                                                                    val progress = if (queueItem.totalBytes > 0) {
                                                                        (queueItem.bytesTransferred.toFloat() / queueItem.totalBytes).coerceIn(0f, 1f)
                                                                    } else 0f
                                                                    LinearProgressIndicator(
                                                                        progress = { progress },
                                                                        modifier = Modifier
                                                                            .fillMaxWidth()
                                                                            .height(3.dp)
                                                                            .clip(RoundedCornerShape(2.dp)),
                                                                        color = CyberCyan,
                                                                        trackColor = CyberSurfaceVariant
                                                                    )
                                                                    Spacer(modifier = Modifier.height(3.dp))
                                                                }

                                                                Row(
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                                ) {
                                                                    Text(
                                                                        text = "${AudioChunkRecorder.formatBytes(queueItem.bytesTransferred)} / ${AudioChunkRecorder.formatBytes(queueItem.totalBytes)}",
                                                                        fontFamily = FontFamily.Monospace,
                                                                        fontSize = 8.sp,
                                                                        color = CyberTextMuted
                                                                    )
                                                                    Text(
                                                                        text = queueItem.statusMessage,
                                                                        fontFamily = FontFamily.Monospace,
                                                                        fontSize = 8.sp,
                                                                        color = CyberTextSecondary,
                                                                        maxLines = 1
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Start Button (Adaptive for Stealth vs Normal)
                        val startButtonChunkLabel = when {
                            selectedChunkSeconds >= AudioChunkRecorder.INFINITE_CHUNK_SECONDS -> "∞ Continuous"
                            selectedChunkSeconds < 60 -> "${selectedChunkSeconds}s"
                            else -> "${selectedChunkSeconds / 60}m"
                        }
                        Button(
                            onClick = {
                                if (isStealthModeToggled) {
                                    viewModel.startStealthLectureRecording(
                                        title = lectureTitle.ifBlank { "$selectedSubject Class" },
                                        subject = selectedSubject,
                                        coachingSessionName = selectedCoachingSession,
                                        disguiseProfile = selectedDisguiseProfile,
                                        chunkMinutes = if (selectedChunkSeconds >= AudioChunkRecorder.INFINITE_CHUNK_SECONDS) 0 else (selectedChunkSeconds / 60).toInt().coerceAtLeast(1),
                                        chunkSeconds = selectedChunkSeconds
                                    )
                                } else {
                                    val success = viewModel.startLectureRecording(
                                        title = lectureTitle.ifBlank { "$selectedSubject Class" },
                                        subject = selectedSubject,
                                        coachingSessionName = selectedCoachingSession,
                                        initialNotes = notesText
                                    )
                                    if (!success) {
                                        Toast.makeText(context, "Could not start audio recording. Check mic.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isStealthModeToggled) CyberGreen else CyberCyan,
                                contentColor = CyberBg
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag(if (isStealthModeToggled) "start_stealth_recording_button" else "start_recording_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isStealthModeToggled) "START ROLLING CHUNK RECORDING ($startButtonChunkLabel)" else "START STANDARD RECORDING",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            } else {
                // ==========================================
                // STANDARD ACTIVE RECORDING CONTROLS
                // ==========================================
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CyberSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Main Action Buttons (Pause / Resume, Stop & Save, Cancel)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Pause / Resume
                            Button(
                                onClick = {
                                    if (recordingState == RecordingState.RECORDING) {
                                        viewModel.pauseLectureRecording()
                                    } else {
                                        viewModel.resumeLectureRecording()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (recordingState == RecordingState.RECORDING) CyberAmber else CyberGreen,
                                    contentColor = CyberBg
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("pause_resume_button")
                            ) {
                                Icon(
                                    imageVector = if (recordingState == RecordingState.RECORDING) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (recordingState == RecordingState.RECORDING) "PAUSE" else "RESUME",
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Stop & Save
                            Button(
                                onClick = {
                                    viewModel.stopAndSaveLectureRecording {
                                        onNavigateToVault()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyberGreen,
                                    contentColor = CyberBg
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("stop_save_recording_button")
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("STOP & SAVE", fontWeight = FontWeight.Bold)
                            }
                        }

                        // Discard button
                        OutlinedButton(
                            onClick = { viewModel.cancelLectureRecording() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberRed),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberRed.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .testTag("cancel_recording_button")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("DISCARD RECORDING", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Divider(color = CyberBorder)

                        // Live Bookmark Stamper
                        Text(
                            text = "STAMP TIME-CODED BOOKMARK",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = CyberCyan
                        )

                        // Quick bookmark chips
                        val quickLabels = listOf("Key Insight", "Priority Point", "Meeting Start", "Discussion Item", "Action Required")
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(quickLabels) { label ->
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        viewModel.addLectureBookmark(label)
                                        Toast.makeText(context, "Bookmark added: $label", Toast.LENGTH_SHORT).show()
                                    },
                                    label = { Text("+ $label", fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = CyberSurfaceVariant,
                                        labelColor = CyberCyan
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = false,
                                        borderColor = CyberBorder
                                    )
                                )
                            }
                        }

                        // Custom bookmark entry
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = newBookmarkLabel,
                                onValueChange = { newBookmarkLabel = it },
                                placeholder = { Text("Custom marker label...", fontSize = 12.sp) },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("custom_bookmark_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyberCyan,
                                    unfocusedBorderColor = CyberBorder,
                                    focusedTextColor = CyberTextPrimary,
                                    unfocusedTextColor = CyberTextPrimary
                                )
                            )

                            Button(
                                onClick = {
                                    if (newBookmarkLabel.isNotBlank()) {
                                        viewModel.addLectureBookmark(newBookmarkLabel)
                                        newBookmarkLabel = ""
                                        Toast.makeText(context, "Bookmark pinned!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyberCyan,
                                    contentColor = CyberBg
                                ),
                                modifier = Modifier.testTag("add_custom_bookmark_button")
                            ) {
                                Icon(Icons.Default.BookmarkAdd, contentDescription = null)
                            }
                        }

                        // Stamped bookmarks list
                        if (activeBookmarks.isNotEmpty()) {
                            Text(
                                text = "STAMPED MARKERS IN THIS SESSION (${activeBookmarks.size})",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = CyberTextMuted
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                activeBookmarks.forEach { bm ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(CyberSurfaceVariant)
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = LectureRecorderManager.formatSeconds(bm.timeSeconds),
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = CyberAmber
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = bm.label,
                                            fontSize = 12.sp,
                                            color = CyberTextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LectureVaultTab(
    viewModel: EnforcerViewModel,
    lectures: List<LectureEntity>
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var filterSubject by remember { mutableStateOf("All") }

    val currentPlayingId by viewModel.currentPlayingId.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isAudioPlaying.collectAsStateWithLifecycle()
    val positionMs by viewModel.audioCurrentPositionMs.collectAsStateWithLifecycle()
    val durationMs by viewModel.audioDurationMs.collectAsStateWithLifecycle()
    val audioSpeed by viewModel.audioSpeed.collectAsStateWithLifecycle()

    var lectureToDelete by remember { mutableStateOf<LectureEntity?>(null) }
    var lectureToEditNotes by remember { mutableStateOf<LectureEntity?>(null) }
    var editNotesText by remember { mutableStateOf("") }

    val subjectsFilter = listOf("All", "Physics", "Chemistry", "Mathematics", "Biology")

    val filteredLectures = lectures.filter { lecture ->
        val matchesSubject = filterSubject == "All" || lecture.subject.equals(filterSubject, ignoreCase = true)
        val matchesQuery = searchQuery.isBlank() ||
                lecture.title.contains(searchQuery, ignoreCase = true) ||
                lecture.notes.contains(searchQuery, ignoreCase = true)
        matchesSubject && matchesQuery
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("lecture_vault_container")
    ) {
        // Search bar & subject filters
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search topics, formulas, notes...") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("vault_search_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyberCyan,
                unfocusedBorderColor = CyberBorder,
                focusedTextColor = CyberTextPrimary,
                unfocusedTextColor = CyberTextPrimary
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(subjectsFilter) { subj ->
                val selected = filterSubject == subj
                FilterChip(
                    selected = selected,
                    onClick = { filterSubject = subj },
                    label = { Text(subj, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyberCyan,
                        selectedLabelColor = CyberBg,
                        containerColor = CyberSurface,
                        labelColor = CyberTextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = CyberBorder,
                        selectedBorderColor = CyberCyan
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredLectures.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = CyberTextMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "NO LECTURES FOUND",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = CyberTextMuted
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Switch to 'RECORD AUDIO' to start capturing your ambient meetings & thoughts.",
                        style = MaterialTheme.typography.bodySmall,
                        color = CyberTextMuted
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredLectures, key = { it.id }) { lecture ->
                    val isCurrentTrack = currentPlayingId == lecture.id
                    LectureCard(
                        lecture = lecture,
                        isCurrentTrack = isCurrentTrack,
                        isPlaying = isCurrentTrack && isPlaying,
                        currentPositionMs = if (isCurrentTrack) positionMs else 0,
                        totalDurationMs = if (isCurrentTrack && durationMs > 0) durationMs else (lecture.durationSeconds * 1000).toInt(),
                        currentSpeed = audioSpeed,
                        onPlayPause = { viewModel.togglePlayPauseLecture(lecture) },
                        onSeek = { viewModel.seekAudio(it) },
                        onSetSpeed = { viewModel.setAudioSpeed(it) },
                        onSkipBackward = { viewModel.skipAudioBackward(10) },
                        onSkipForward = { viewModel.skipAudioForward(10) },
                        onAddBookmark = { label ->
                            viewModel.addBookmarkToLecture(lecture, (positionMs / 1000).toLong(), label)
                        },
                        onSeekToBookmark = { timeSec ->
                            if (!isCurrentTrack) {
                                viewModel.togglePlayPauseLecture(lecture)
                            }
                            viewModel.seekAudio((timeSec * 1000).toInt())
                        },
                        onEditNotes = {
                            lectureToEditNotes = lecture
                            editNotesText = lecture.notes
                        },
                        onDelete = { lectureToDelete = lecture },
                        onShare = {
                            shareLecture(context, lecture)
                        }
                    )
                }
            }
        }
    }

    // Delete Confirmation Dialog
    lectureToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { lectureToDelete = null },
            containerColor = CyberSurface,
            title = {
                Text(
                    text = "DELETE RECORDING?",
                    color = CyberRed,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete '${target.title}'? The audio file will be permanently removed from device storage.",
                    color = CyberTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteLecture(target)
                        lectureToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberRed, contentColor = CyberBg),
                    modifier = Modifier.testTag("confirm_delete_lecture_button")
                ) {
                    Text("DELETE", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { lectureToDelete = null },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberTextPrimary)
                ) {
                    Text("CANCEL")
                }
            }
        )
    }

    // Edit Notes Dialog
    lectureToEditNotes?.let { target ->
        AlertDialog(
            onDismissRequest = { lectureToEditNotes = null },
            containerColor = CyberSurface,
            title = {
                Text(
                    text = "EDIT LECTURE NOTES",
                    color = CyberCyan,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = target.title,
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberTextMuted
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editNotesText,
                        onValueChange = { editNotesText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .testTag("edit_notes_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = CyberBorder,
                            focusedTextColor = CyberTextPrimary,
                            unfocusedTextColor = CyberTextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateLectureNotes(target, editNotesText)
                        lectureToEditNotes = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = CyberBg)
                ) {
                    Text("SAVE NOTES", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { lectureToEditNotes = null },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberTextPrimary)
                ) {
                    Text("CANCEL")
                }
            }
        )
    }
}

@Composable
fun LectureCard(
    lecture: LectureEntity,
    isCurrentTrack: Boolean,
    isPlaying: Boolean,
    currentPositionMs: Int,
    totalDurationMs: Int,
    currentSpeed: Float,
    onPlayPause: () -> Unit,
    onSeek: (Int) -> Unit,
    onSetSpeed: (Float) -> Unit,
    onSkipBackward: () -> Unit = {},
    onSkipForward: () -> Unit = {},
    onAddBookmark: (String) -> Unit = {},
    onSeekToBookmark: (Long) -> Unit,
    onEditNotes: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    val bookmarks = remember(lecture.bookmarksJson) { lecture.parseBookmarks() }
    val formattedDate = remember(lecture.timestamp) {
        SimpleDateFormat("dd MMM yyyy • hh:mm a", Locale.getDefault()).format(Date(lecture.timestamp))
    }
    var showPinBookmarkDialog by remember { mutableStateOf(false) }
    var pinBookmarkText by remember { mutableStateOf("") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("lecture_card_${lecture.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentTrack) CyberSurfaceVariant else CyberSurface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isCurrentTrack) CyberCyan else CyberBorder
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Subject Chip, Coaching session, Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val badgeColor = when (lecture.subject.lowercase(Locale.ROOT)) {
                        "physics" -> CyberCyan
                        "chemistry" -> CyberGreen
                        "mathematics" -> CyberAmber
                        else -> CyberTextSecondary
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(badgeColor.copy(alpha = 0.2f))
                            .border(1.dp, badgeColor, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = lecture.subject.uppercase(Locale.ROOT),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = badgeColor
                        )
                    }

                    if (lecture.coachingSessionName.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = lecture.coachingSessionName,
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberTextMuted,
                            maxLines = 1
                        )
                    }
                }

                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberTextMuted
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Lecture Title
            Text(
                text = lecture.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CyberTextPrimary
            )

            // Duration and file size
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "DURATION: ${LectureRecorderManager.formatSeconds(lecture.durationSeconds)}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = CyberTextSecondary
                )
                Text(
                    text = "SIZE: ${LectureRecorderManager.formatBytes(lecture.fileSizeBytes)}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = CyberTextMuted
                )
            }

            // Audio Player Controls
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(CyberBg)
                    .padding(12.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Transport buttons: Skip -10s, Play/Pause, Skip +10s
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            IconButton(
                                onClick = onSkipBackward,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(CyberSurfaceVariant)
                                    .testTag("skip_backward_button_${lecture.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FastRewind,
                                    contentDescription = "Rewind 10 seconds",
                                    tint = CyberCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(
                                onClick = onPlayPause,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(if (isPlaying) CyberAmber else CyberCyan)
                                    .testTag("play_pause_button_${lecture.id}")
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = CyberBg,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            IconButton(
                                onClick = onSkipForward,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(CyberSurfaceVariant)
                                    .testTag("skip_forward_button_${lecture.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FastForward,
                                    contentDescription = "Forward 10 seconds",
                                    tint = CyberCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Current / Total position
                        val currentSec = (currentPositionMs / 1000).toLong()
                        val totalSec = (totalDurationMs / 1000).coerceAtLeast(lecture.durationSeconds.toInt()).toLong()
                        Text(
                            text = "${LectureRecorderManager.formatSeconds(currentSec)} / ${LectureRecorderManager.formatSeconds(totalSec)}",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = if (isCurrentTrack) CyberCyan else CyberTextSecondary
                        )

                        // Speed selector chips
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(1.0f, 1.25f, 1.5f, 2.0f).forEach { spd ->
                                val active = isCurrentTrack && currentSpeed == spd
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (active) CyberCyan else CyberSurfaceVariant)
                                        .clickable { onSetSpeed(spd) }
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "${spd}x",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                        color = if (active) CyberBg else CyberTextSecondary
                                    )
                                }
                            }
                        }
                    }

                    // Scrubber Slider
                    val sliderMax = if (totalDurationMs > 0) totalDurationMs.toFloat() else (lecture.durationSeconds * 1000).toFloat()
                    Slider(
                        value = currentPositionMs.toFloat().coerceIn(0f, sliderMax.coerceAtLeast(1f)),
                        onValueChange = { onSeek(it.toInt()) },
                        valueRange = 0f..sliderMax.coerceAtLeast(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = CyberCyan,
                            activeTrackColor = CyberCyan,
                            inactiveTrackColor = CyberBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(26.dp)
                            .testTag("scrubber_slider_${lecture.id}")
                    )

                    // Pin Bookmark Action Row during playback
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val currentSec = (currentPositionMs / 1000).toLong()
                        Text(
                            text = "Pos: ${LectureRecorderManager.formatSeconds(currentSec)}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = CyberAmber
                        )

                        OutlinedButton(
                            onClick = { showPinBookmarkDialog = true },
                            modifier = Modifier
                                .height(26.dp)
                                .testTag("pin_bookmark_button_${lecture.id}"),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberAmber.copy(alpha = 0.7f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.BookmarkAdd,
                                contentDescription = null,
                                tint = CyberAmber,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "+ PIN BOOKMARK",
                                fontSize = 10.sp,
                                color = CyberAmber,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Pin Bookmark Dialog
            if (showPinBookmarkDialog) {
                val currentSec = (currentPositionMs / 1000).toLong()
                AlertDialog(
                    onDismissRequest = { showPinBookmarkDialog = false },
                    containerColor = CyberSurface,
                    title = {
                        Text(
                            text = "PIN REVISION BOOKMARK",
                            color = CyberAmber,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Timestamp: ${LectureRecorderManager.formatSeconds(currentSec)}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = CyberCyan
                            )
                            OutlinedTextField(
                                value = pinBookmarkText,
                                onValueChange = { pinBookmarkText = it },
                                placeholder = { Text("e.g., Action item / Meeting discussion...") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            val suggestions = listOf("Key Insight", "Priority Topic", "Decision Point", "Action Plan")
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(suggestions) { sugg ->
                                    FilterChip(
                                        selected = pinBookmarkText == sugg,
                                        onClick = { pinBookmarkText = sugg },
                                        label = { Text(sugg, fontSize = 10.sp) }
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (pinBookmarkText.isNotBlank()) {
                                    onAddBookmark(pinBookmarkText)
                                    pinBookmarkText = ""
                                    showPinBookmarkDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberAmber, contentColor = CyberBg)
                        ) {
                            Text("SAVE BOOKMARK", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        OutlinedButton(onClick = { showPinBookmarkDialog = false }) {
                            Text("CANCEL")
                        }
                    }
                )
            }

            // Bookmarks / Timecode Chips
            if (bookmarks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "TIME-CODED REVISION BOOKMARKS:",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = CyberTextMuted
                )
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(bookmarks) { bm ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(CyberSurfaceVariant)
                                .border(1.dp, CyberAmber.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .clickable { onSeekToBookmark(bm.timeSeconds) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = null,
                                    tint = CyberAmber,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "[${LectureRecorderManager.formatSeconds(bm.timeSeconds)}] ${bm.label}",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = CyberTextPrimary
                                )
                            }
                        }
                    }
                }
            }

            // Notes Section
            if (lecture.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(CyberSurfaceVariant.copy(alpha = 0.5f))
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            text = "LECTURE REVISION NOTES",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = CyberCyan
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = lecture.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = CyberTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer actions: Edit Notes, Share, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onEditNotes,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("EDIT NOTES", fontSize = 11.sp)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onShare,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Lecture",
                            tint = CyberTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Lecture",
                            tint = CyberRed.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LiveWaveformCanvas(
    amplitudes: List<Float>,
    isRecording: Boolean
) {
    val pulse = if (isRecording) {
        val infiniteTransition = rememberInfiniteTransition(label = "wave_anim")
        val p by infiniteTransition.animateFloat(
            initialValue = 0.9f,
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(400, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )
        p
    } else {
        1.0f
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f

        // Draw grid lines
        drawLine(
            color = CyberBorder.copy(alpha = 0.4f),
            start = Offset(0f, centerY),
            end = Offset(width, centerY),
            strokeWidth = 1f
        )

        val count = amplitudes.size.coerceAtLeast(1)
        val barWidth = (width / (count * 1.5f)).coerceIn(4f, 16f)
        val gap = barWidth * 0.5f
        val totalWidth = count * (barWidth + gap)
        val startX = (width - totalWidth) / 2f

        for (i in amplitudes.indices) {
            val amp = (amplitudes[i] * if (isRecording) pulse else 1f).coerceIn(0.06f, 1.0f)
            val barHeight = (height * 0.85f * amp).coerceAtLeast(4f)
            val x = startX + i * (barWidth + gap)
            val top = centerY - (barHeight / 2f)

            val barColor = when {
                amp > 0.75f -> CyberRed
                amp > 0.45f -> CyberAmber
                else -> CyberCyan
            }

            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

private fun shareLecture(context: Context, lecture: LectureEntity) {
    try {
        val file = File(lecture.filePath)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            if (file.exists()) {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                type = "audio/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                type = "text/plain"
            }
            putExtra(Intent.EXTRA_SUBJECT, "Lecture Audio: ${lecture.title}")
            putExtra(
                Intent.EXTRA_TEXT,
                "ENFORCER OS // LECTURE VAULT\nSubject: ${lecture.subject}\nTopic: ${lecture.title}\nDuration: ${LectureRecorderManager.formatSeconds(lecture.durationSeconds)}\nNotes: ${lecture.notes}"
            )
        }
        context.startActivity(Intent.createChooser(shareIntent, "Export Lecture"))
    } catch (_: Exception) {
        val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Lecture Audio: ${lecture.title}")
            putExtra(
                Intent.EXTRA_TEXT,
                "ENFORCER OS // LECTURE VAULT\nSubject: ${lecture.subject}\nTopic: ${lecture.title}\nDuration: ${LectureRecorderManager.formatSeconds(lecture.durationSeconds)}\nNotes: ${lecture.notes}"
            )
        }
        context.startActivity(Intent.createChooser(fallbackIntent, "Export Lecture Info"))
    }
}

@Composable
fun AudioGainAndVoiceFocusCard(
    isVoiceFocusEnabled: Boolean,
    isAcHumFilterEnabled: Boolean,
    currentGain: DistanceMicGain,
    onToggleVoiceFocus: () -> Unit,
    onToggleHumFilter: () -> Unit,
    onSelectGain: (DistanceMicGain) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("audio_gain_voice_focus_card"),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            if (isVoiceFocusEnabled) CyberCyan else CyberBorder
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row
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
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (isVoiceFocusEnabled) CyberCyan.copy(alpha = 0.2f) else CyberSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = if (isVoiceFocusEnabled) CyberCyan else CyberTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "AUDIO GAIN BOOSTER & VOICE FOCUS",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = CyberCyan
                        )
                        Text(
                            text = "Lecture Hall Acoustic DSP & Beamforming",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = CyberTextMuted
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isVoiceFocusEnabled) CyberCyan.copy(alpha = 0.15f) else CyberSurfaceVariant)
                        .border(1.dp, if (isVoiceFocusEnabled) CyberCyan else CyberBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (isVoiceFocusEnabled) "BEAMFORMING ON" else "STANDARD MIC",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        color = if (isVoiceFocusEnabled) CyberCyan else CyberTextMuted
                    )
                }
            }

            Divider(color = CyberBorder.copy(alpha = 0.5f), thickness = 0.8.dp)

            // Toggle 1: Lecture Hall Voice Focus
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Lecture Hall Voice Focus",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = CyberTextPrimary
                        )
                        if (isVoiceFocusEnabled) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(CyberGreen.copy(alpha = 0.2f))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "300Hz-3.4kHz",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 8.sp,
                                    color = CyberGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Text(
                        text = "Activates hardware vocal beamforming to isolate the lecturer's voice from student chatter.",
                        fontSize = 10.sp,
                        color = CyberTextSecondary,
                        lineHeight = 13.sp
                    )
                }

                Switch(
                    checked = isVoiceFocusEnabled,
                    onCheckedChange = { onToggleVoiceFocus() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyberBg,
                        checkedTrackColor = CyberCyan,
                        uncheckedThumbColor = CyberTextMuted,
                        uncheckedTrackColor = CyberSurfaceVariant
                    ),
                    modifier = Modifier.testTag("switch_voice_focus")
                )
            }

            // Toggle 2: AC / Fan Low-Frequency Hum Filter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "HVAC & Fan Low-End Hum Filter",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = CyberTextPrimary
                        )
                        if (isAcHumFilterEnabled) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(CyberAmber.copy(alpha = 0.2f))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "HPF GATE",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 8.sp,
                                    color = CyberAmber,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Text(
                        text = "Cuts continuous ceiling fan drone, desk rumble, and air conditioning hum.",
                        fontSize = 10.sp,
                        color = CyberTextSecondary,
                        lineHeight = 13.sp
                    )
                }

                Switch(
                    checked = isAcHumFilterEnabled,
                    onCheckedChange = { onToggleHumFilter() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyberBg,
                        checkedTrackColor = CyberAmber,
                        uncheckedThumbColor = CyberTextMuted,
                        uncheckedTrackColor = CyberSurfaceVariant
                    ),
                    modifier = Modifier.testTag("switch_ac_hum_filter")
                )
            }

            Divider(color = CyberBorder.copy(alpha = 0.5f), thickness = 0.8.dp)

            // Distance Mic Gain Booster (Presets)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DISTANCE MIC GAIN BOOSTER",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = CyberTextSecondary
                    )
                    Text(
                        text = "ACTIVE: ${currentGain.label} (${currentGain.multiplier}x)",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = CyberCyan
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DistanceMicGain.entries.forEach { gainOption ->
                        val isSelected = gainOption == currentGain
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) CyberCyan.copy(alpha = 0.2f) else CyberSurfaceVariant)
                                .border(
                                    1.dp,
                                    if (isSelected) CyberCyan else CyberBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { onSelectGain(gainOption) }
                                .padding(vertical = 8.dp, horizontal = 4.dp)
                                .testTag("gain_chip_${gainOption.multiplier}x"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${gainOption.multiplier}x",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isSelected) CyberCyan else CyberTextPrimary
                                )
                                Text(
                                    text = when (gainOption) {
                                        DistanceMicGain.FRONT_ROW -> "Front Row"
                                        DistanceMicGain.MID_HALL -> "Mid Hall"
                                        DistanceMicGain.BACK_ROW -> "Back Row"
                                        DistanceMicGain.SUPER_FAR -> "Auditorium"
                                    },
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 8.sp,
                                    color = if (isSelected) CyberCyan else CyberTextMuted,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Text(
                    text = currentGain.description,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = CyberTextMuted,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
fun PocketGestureCard(
    isPocketGestureEnabled: Boolean,
    isHapticCuesEnabled: Boolean,
    onTogglePocketGesture: () -> Unit,
    onToggleHapticCues: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pocket_gesture_card"),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            if (isPocketGestureEnabled) CyberGreen else CyberBorder
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row
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
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (isPocketGestureEnabled) CyberGreen.copy(alpha = 0.2f) else CyberSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Vibration,
                            contentDescription = null,
                            tint = if (isPocketGestureEnabled) CyberGreen else CyberTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "POCKET GESTURE & SILENT CUES",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = CyberGreen
                        )
                        Text(
                            text = "Hardware Key Triggers & Tactile Feedback",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = CyberTextMuted
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isPocketGestureEnabled) CyberGreen.copy(alpha = 0.15f) else CyberSurfaceVariant)
                        .border(1.dp, if (isPocketGestureEnabled) CyberGreen else CyberBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (isPocketGestureEnabled) "VOL DBL-TAP ON" else "OFF",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        color = if (isPocketGestureEnabled) CyberGreen else CyberTextMuted
                    )
                }
            }

            Divider(color = CyberBorder.copy(alpha = 0.5f), thickness = 0.8.dp)

            // Toggle 1: Pocket Volume Double-Tap Bookmark
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "Pocket Volume Double-Tap Bookmark",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = CyberTextPrimary
                    )
                    Text(
                        text = "Double-click Volume Up or Down to stamp an instant revision bookmark without pulling the phone out of your pocket or turning on the screen.",
                        fontSize = 10.sp,
                        color = CyberTextSecondary,
                        lineHeight = 13.sp
                    )
                }

                Switch(
                    checked = isPocketGestureEnabled,
                    onCheckedChange = { onTogglePocketGesture() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyberBg,
                        checkedTrackColor = CyberGreen,
                        uncheckedThumbColor = CyberTextMuted,
                        uncheckedTrackColor = CyberSurfaceVariant
                    ),
                    modifier = Modifier.testTag("switch_pocket_gesture")
                )
            }

            // Toggle 2: Silent Haptic Cues
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "Silent Tactile Vibration Cues",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = CyberTextPrimary
                    )
                    Text(
                        text = "Emits discreet 50ms dual-tick vibration pulses when a bookmark is pinned, or crisp clicks on recording start/stop.",
                        fontSize = 10.sp,
                        color = CyberTextSecondary,
                        lineHeight = 13.sp
                    )
                }

                Switch(
                    checked = isHapticCuesEnabled,
                    onCheckedChange = { onToggleHapticCues() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyberBg,
                        checkedTrackColor = CyberGreen,
                        uncheckedThumbColor = CyberTextMuted,
                        uncheckedTrackColor = CyberSurfaceVariant
                    ),
                    modifier = Modifier.testTag("switch_haptic_cues")
                )
            }
        }
    }
}
