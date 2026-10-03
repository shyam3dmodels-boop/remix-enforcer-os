package com.example.ui.screens.recorder

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.recorder.GroqAiManager
import com.example.ui.EnforcerViewModel
import com.example.ui.theme.KamakuraSkyBackground
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.theme.LiquidGlassFill
import com.example.ui.theme.PaletteCornflower
import com.example.ui.theme.PaletteIceCyan
import com.example.ui.theme.PaletteMintFrost
import com.example.ui.theme.PaletteSoftSky
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

data class TranscriptTimestampSegment(
    val id: Int,
    val startSeconds: Float,
    val endSeconds: Float,
    val text: String
)

@Composable
fun TranscribeStudioScreen(
    viewModel: EnforcerViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    val groqManager = remember { GroqAiManager.getInstance(context) }
    val groqApiKey by groqManager.apiKey.collectAsStateWithLifecycle()

    var isRecordingMic by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableIntStateOf(0) }
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordedTempFile by remember { mutableStateOf<File?>(null) }
    var selectedAudioFile by remember { mutableStateOf<File?>(null) }
    var selectedAudioName by remember { mutableStateOf<String?>(null) }

    var isTranscribing by remember { mutableStateOf(false) }
    var transcriptionText by remember { mutableStateOf("") }
    var segments by remember { mutableStateOf<List<TranscriptTimestampSegment>>(emptyList()) }
    var selectedModel by remember { mutableStateOf("whisper-large-v3-turbo") }
    var statusMessage by remember { mutableStateOf("") }
    var latencyMs by remember { mutableStateOf<Long?>(null) }

    // Audio Playback
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlayingAudio by remember { mutableStateOf(false) }

    // File Picker
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                    val tempFile = File(context.cacheDir, "imported_${System.currentTimeMillis()}.m4a")
                    val outputStream = FileOutputStream(tempFile)
                    inputStream?.copyTo(outputStream)
                    inputStream?.close()
                    outputStream.close()

                    withContext(Dispatchers.Main) {
                        selectedAudioFile = tempFile
                        selectedAudioName = uri.lastPathSegment ?: "Imported Audio"
                        Toast.makeText(context, "Audio file loaded: ${tempFile.name}", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to load audio: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // Mic Recording Timer
    LaunchedEffect(isRecordingMic) {
        if (isRecordingMic) {
            recordingSeconds = 0
            while (isRecordingMic) {
                delay(1000)
                recordingSeconds++
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaRecorder?.release()
                mediaPlayer?.release()
            } catch (_: Exception) {}
        }
    }

    fun startMicRecording() {
        try {
            val file = File(context.cacheDir, "mic_record_${System.currentTimeMillis()}.m4a")
            recordedTempFile = file

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            isRecordingMic = true
            selectedAudioFile = file
            selectedAudioName = "Live Microphone Recording"
        } catch (e: Exception) {
            Toast.makeText(context, "Mic error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun stopMicRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecordingMic = false
            Toast.makeText(context, "Recording saved (${recordingSeconds}s)", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Stop error: ${e.message}", Toast.LENGTH_SHORT).show()
            isRecordingMic = false
        }
    }

    fun executeTranscription(file: File) {
        val apiKey = groqApiKey.ifBlank {
            // Check if user has Groq key in Room DB
            viewModel.allAiKeys.value.firstOrNull { it.provider == "groq" }?.apiKey ?: ""
        }

        if (apiKey.isBlank()) {
            Toast.makeText(context, "Please set a Groq API Key first (Vault)", Toast.LENGTH_LONG).show()
            return
        }

        isTranscribing = true
        statusMessage = "Transcribing with $selectedModel..."
        val startTime = System.currentTimeMillis()

        coroutineScope.launch(Dispatchers.IO) {
            try {
                val okClient = OkHttpClient.Builder()
                    .connectTimeout(40, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .build()

                val fileBody = file.asRequestBody("audio/*".toMediaTypeOrNull())
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", file.name, fileBody)
                    .addFormDataPart("model", selectedModel)
                    .addFormDataPart("response_format", "verbose_json")
                    .build()

                val request = Request.Builder()
                    .url("https://api.groq.com/openai/v1/audio/transcriptions")
                    .header("Authorization", "Bearer $apiKey")
                    .post(requestBody)
                    .build()

                val response = okClient.newCall(request).execute()
                val responseBody = response.body?.string() ?: "{}"
                val duration = System.currentTimeMillis() - startTime

                withContext(Dispatchers.Main) {
                    latencyMs = duration
                    if (response.isSuccessful) {
                        val json = JSONObject(responseBody)
                        val text = json.optString("text", "")
                        transcriptionText = text

                        // Parse segments if available
                        val segList = mutableListOf<TranscriptTimestampSegment>()
                        val segArray = json.optJSONArray("segments")
                        if (segArray != null) {
                            for (i in 0 until segArray.length()) {
                                val item = segArray.getJSONObject(i)
                                segList.add(
                                    TranscriptTimestampSegment(
                                        id = item.optInt("id", i),
                                        startSeconds = item.optDouble("start", 0.0).toFloat(),
                                        endSeconds = item.optDouble("end", 0.0).toFloat(),
                                        text = item.optString("text", "").trim()
                                    )
                                )
                            }
                        }
                        segments = segList
                        statusMessage = "Transcription Complete (${duration}ms)"
                        Toast.makeText(context, "Transcribed in ${duration}ms! ⚡", Toast.LENGTH_SHORT).show()
                    } else {
                        statusMessage = "Error: HTTP ${response.code}"
                        Toast.makeText(context, "Groq Error: ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    statusMessage = "Failed: ${e.message}"
                    Toast.makeText(context, "Transcription failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isTranscribing = false
                }
            }
        }
    }

    KamakuraSkyBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .testTag("transcribe_studio_screen"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // ── TOP HEADER ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(PaletteIceCyan.copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = PaletteCornflower)
                    }
                    Column {
                        Text(
                            text = "Audio Transcriber",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = PaletteCornflower
                        )
                        Text(
                            text = "Groq Whisper LPU Sub-Second STT",
                            fontSize = 11.sp,
                            color = Color(0xFF6B7280)
                        )
                    }
                }

                if (latencyMs != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = PaletteMintFrost
                    ) {
                        Text(
                            text = "⚡ ${latencyMs}ms",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PaletteCornflower,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // ── 1. AUDIO SOURCE CARD (MIC OR FILE) ──
            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "1. Select or Record Audio",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PaletteCornflower
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Mic Button
                        Button(
                            onClick = {
                                if (isRecordingMic) stopMicRecording() else startMicRecording()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRecordingMic) Color(0xFFEF4444) else PaletteCornflower
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = if (isRecordingMic) Icons.Default.Stop else Icons.Default.Mic,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isRecordingMic) "Stop (${recordingSeconds}s)" else "Record Mic",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        // File Picker Button
                        OutlinedButton(
                            onClick = {
                                filePickerLauncher.launch("audio/*")
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.2.dp, PaletteCornflower)
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, tint = PaletteCornflower, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pick File", color = PaletteCornflower, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    // Active Selected Audio Badge
                    if (selectedAudioFile != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = PaletteIceCyan.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Audiotrack, contentDescription = null, tint = PaletteCornflower, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = selectedAudioName ?: selectedAudioFile?.name ?: "Audio Ready",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PaletteCornflower
                                    )
                                }

                                Button(
                                    onClick = {
                                        selectedAudioFile?.let { executeTranscription(it) }
                                    },
                                    enabled = !isTranscribing,
                                    colors = ButtonDefaults.buttonColors(containerColor = PaletteCornflower),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    if (isTranscribing) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                    } else {
                                        Text("Transcribe ⚡", fontSize = 11.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── 2. MODEL SELECTION & API KEY STATUS ──
            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "2. Engine & Model Configuration",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PaletteCornflower
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedModel == "whisper-large-v3-turbo") PaletteCornflower else PaletteIceCyan.copy(alpha = 0.6f),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedModel = "whisper-large-v3-turbo" }
                        ) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Whisper Turbo", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = if (selectedModel == "whisper-large-v3-turbo") Color.White else PaletteCornflower)
                                Text("<200ms Latency", fontSize = 9.sp, color = if (selectedModel == "whisper-large-v3-turbo") Color.White.copy(alpha = 0.8f) else Color.Gray)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedModel == "whisper-large-v3") PaletteCornflower else PaletteIceCyan.copy(alpha = 0.6f),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedModel = "whisper-large-v3" }
                        ) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Whisper Large v3", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = if (selectedModel == "whisper-large-v3") Color.White else PaletteCornflower)
                                Text("Highest Accuracy", fontSize = 9.sp, color = if (selectedModel == "whisper-large-v3") Color.White.copy(alpha = 0.8f) else Color.Gray)
                            }
                        }
                    }
                }
            }

            // ── 3. TRANSCRIPTION RESULTS & TIMESTAMPS ──
            if (transcriptionText.isNotBlank()) {
                LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Transcription Output",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = PaletteCornflower
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(transcriptionText))
                                        Toast.makeText(context, "Copied transcript to clipboard!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = PaletteCornflower, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        // Full Text Display
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = transcriptionText,
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                color = Color(0xFF1F2937),
                                modifier = Modifier.padding(12.dp)
                            )
                        }

                        // Timestamp Breakdown
                        if (segments.isNotEmpty()) {
                            Text(
                                text = "Timestamped Segments (${segments.size}):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PaletteCornflower
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                segments.forEach { seg ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = PaletteIceCyan.copy(alpha = 0.35f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = String.format("%.1fs", seg.startSeconds),
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = PaletteCornflower
                                            )
                                            Text(
                                                text = seg.text,
                                                fontSize = 11.sp,
                                                color = Color(0xFF374151),
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
