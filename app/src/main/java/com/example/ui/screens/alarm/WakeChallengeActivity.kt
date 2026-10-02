package com.example.ui.screens.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.alarm.AlarmSafetyManager
import com.example.core.alarm.RelentlessAlarmService
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberBg
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberGreen
import com.example.ui.theme.CyberRed
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.random.Random

class WakeChallengeActivity : ComponentActivity() {

    private val dismissReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Wake screen and show over lockscreen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        )

        val filter = IntentFilter().apply {
            addAction(AlarmSafetyManager.ACTION_DISMISS_WAKE_CHALLENGE)
            addAction(RelentlessAlarmService.ACTION_STOP_ALARM)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(dismissReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(dismissReceiver, filter)
        }

        setContent {
            MyApplicationTheme {
                WakeChallengeContent(
                    onDismissSuccess = {
                        stopAlarmAndExit()
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(dismissReceiver)
        } catch (_: Exception) {}
    }

    private fun stopAlarmAndExit() {
        val stopIntent = Intent(this, RelentlessAlarmService::class.java).apply {
            action = RelentlessAlarmService.ACTION_STOP_ALARM
        }
        startService(stopIntent)
        AlarmSafetyManager.getInstance(this).emergencyKillAllAlarms()
        finish()
    }
}

@Composable
fun WakeChallengeContent(
    onDismissSuccess: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var isCompleted by remember { mutableStateOf(false) }

    // Mode: 0 = Moving Target, 1 = Math Numericals, 2 = Shake Device, 3 = Memory Matrix, 4 = Typing Mantra
    var challengeMode by remember { mutableIntStateOf(2) } // default to Shake for immediate arousal

    // Diminishing Snooze Manager
    val snoozeManager = remember { com.example.core.alarm.crescendo.DiminishingSnoozeManager(context) }
    var currentSnoozeCount by remember { mutableIntStateOf(snoozeManager.currentSnoozeCount) }

    // Moving Target State
    var targetsTapped by remember { mutableIntStateOf(0) }
    val totalRequired = 5

    // Math numerical state
    var num1 by remember { mutableIntStateOf(17) }
    var num2 by remember { mutableIntStateOf(24) }
    var mathInput by remember { mutableStateOf("") }
    var mathSolvedCount by remember { mutableIntStateOf(0) }
    val mathRequired = 3
    var mathError by remember { mutableStateOf(false) }

    fun nextMathProblem() {
        num1 = Random.nextInt(12, 49)
        num2 = Random.nextInt(13, 38)
        mathInput = ""
        mathError = false
    }

    // Shake Challenge State
    var currentShakes by remember { mutableIntStateOf(0) }
    val targetShakes = 30
    val shakeDetector = remember {
        com.example.core.alarm.captcha.ShakeDetectorCaptcha(
            context = context,
            requiredShakes = targetShakes,
            onProgress = { cur, _ -> currentShakes = cur },
            onComplete = {
                isCompleted = true
                onDismissSuccess()
            }
        )
    }

    LaunchedEffect(challengeMode) {
        if (challengeMode == 2) {
            shakeDetector.start()
        } else {
            shakeDetector.stop()
        }
    }

    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            shakeDetector.stop()
        }
    }

    // Memory Matrix State
    val matrixCaptcha = remember { com.example.core.alarm.captcha.MemoryMatrixCaptcha(gridSize = 9, sequenceLength = 4) }
    var highlightedTile by remember { mutableStateOf<Int?>(null) }
    var matrixStep by remember { mutableIntStateOf(0) }
    var matrixError by remember { mutableStateOf(false) }

    LaunchedEffect(challengeMode) {
        if (challengeMode == 3) {
            // Flash sequence
            matrixCaptcha.generateNewSequence()
            for (tile in matrixCaptcha.sequence) {
                highlightedTile = tile
                delay(600L)
                highlightedTile = null
                delay(200L)
            }
        }
    }

    // Typing Mantra State
    val typingCaptcha = remember { com.example.core.alarm.captcha.TypingMantraCaptcha() }
    var typedText by remember { mutableStateOf("") }
    var typingProgress by remember { mutableFloatStateOf(0f) }

    // Flash background warning state
    var flashAlert by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (!isCompleted) {
            flashAlert = !flashAlert
            delay(500L)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (flashAlert) Color(0xFF1F080C) else CyberBg)
            .testTag("wake_challenge_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(18.dp))

            // Alarm Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(CyberRed.copy(alpha = 0.2f))
                    .border(1.dp, CyberRed, RoundedCornerShape(8.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Alert",
                    tint = CyberRed,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "ENFORCER OS // WAKE PROTOCOL",
                    color = CyberRed,
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "RELENTLESS ALARM ACTIVE",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )

            // Diminishing Snooze & In-Class Silence Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Diminishing Snooze Button
                Button(
                    onClick = {
                        val snoozeMins = snoozeManager.consumeSnooze()
                        if (snoozeMins > 0) {
                            currentSnoozeCount = snoozeManager.currentSnoozeCount
                            val scheduler = com.example.core.alarm.SmartAlarmScheduler(context)
                            scheduler.schedulePresetAlarm(snoozeMins, "Snoozed Wakeup ($snoozeMins min)")
                            onDismissSuccess()
                        }
                    },
                    enabled = snoozeManager.canSnooze(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberAmber,
                        disabledContainerColor = Color(0xFF334155)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (snoozeManager.canSnooze()) "SNOOZE (+${snoozeManager.getNextSnoozeMinutes()}m)" else "SNOOZE LOCKED 🛑",
                        color = if (snoozeManager.canSnooze()) CyberBg else Color(0xFF94A3B8),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }

                // 1-Tap In-Class Silence Button
                Button(
                    onClick = onDismissSuccess,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberRed),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeOff,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "IN-CLASS MUTE",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            // Challenge Mode Horizontal Tabs
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val modes = listOf(
                    Triple(2, "📱 SHAKE", CyberCyan),
                    Triple(1, "🧮 MATH", CyberGreen),
                    Triple(3, "🧠 MEMORY", CyberAmber),
                    Triple(4, "✍️ TYPING", Color(0xFFE879F9)),
                    Triple(0, "🎯 TARGET", Color(0xFF60A5FA))
                )
                items(modes.size) { idx ->
                    val (modeId, title, accent) = modes[idx]
                    val isSelected = challengeMode == modeId
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) accent.copy(alpha = 0.25f) else CyberSurface)
                            .border(1.dp, if (isSelected) accent else Color(0xFF223252), RoundedCornerShape(14.dp))
                            .clickable { challengeMode = modeId }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = title,
                            color = if (isSelected) accent else Color(0xFF94A3B8),
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Mode 2: SHAKE SENSOR CHALLENGE
            if (challengeMode == 2) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CyberSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF223252)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = "Shake",
                            tint = CyberCyan,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "SHAKE PHONE TO WAKE",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Shake vigorously $targetShakes times to prove consciousness.",
                            color = Color(0xFF94A3B8),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                        )

                        // Progress Bar
                        val progress = (currentShakes.toFloat() / targetShakes.toFloat()).coerceIn(0f, 1f)
                        androidx.compose.material3.LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth(0.85f).height(14.dp).clip(RoundedCornerShape(7.dp)),
                            color = CyberCyan,
                            trackColor = Color(0xFF1E293B)
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "$currentShakes / $targetShakes Shakes",
                            color = CyberCyan,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Mode 1: MATH NUMERICALS
            else if (challengeMode == 1) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CyberSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF223252)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "PROBLEM ${mathSolvedCount + 1} OF $mathRequired",
                            color = CyberGreen,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "$num1 × $num2 = ?",
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedTextField(
                            value = mathInput,
                            onValueChange = { mathInput = it },
                            label = { Text("Your Answer") },
                            isError = mathError,
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberGreen,
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth(0.8f)
                        )
                        if (mathError) {
                            Text(
                                text = "❌ Incorrect! Try again.",
                                color = CyberRed,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                val expected = num1 * num2
                                if (mathInput.trim().toIntOrNull() == expected) {
                                    mathSolvedCount++
                                    if (mathSolvedCount >= mathRequired) {
                                        isCompleted = true
                                        onDismissSuccess()
                                    } else {
                                        nextMathProblem()
                                    }
                                } else {
                                    mathError = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth(0.8f)
                        ) {
                            Text(
                                text = if (mathSolvedCount + 1 >= mathRequired) "SUBMIT & DISMISS" else "CHECK & NEXT",
                                color = CyberBg,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Mode 3: MEMORY MATRIX
            else if (challengeMode == 3) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CyberSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF223252)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "REPEAT THE SEQUENCE",
                            color = CyberAmber,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = if (matrixError) "❌ Sequence failed! Resetting..." else "Tap the highlighted tiles in order",
                            color = if (matrixError) CyberRed else Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 14.dp)
                        )

                        // 3x3 Grid
                        for (row in 0 until 3) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(vertical = 5.dp)
                            ) {
                                for (col in 0 until 3) {
                                    val index = row * 3 + col
                                    val isLit = highlightedTile == index
                                    val isSelected = matrixCaptcha.userPicks.contains(index)
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                when {
                                                    isLit -> CyberAmber
                                                    isSelected -> CyberGreen
                                                    else -> Color(0xFF1E293B)
                                                }
                                            )
                                            .border(
                                                2.dp,
                                                if (isLit) Color.White else Color(0xFF334155),
                                                RoundedCornerShape(12.dp)
                                            )
                                            .clickable {
                                                val (correct, complete) = matrixCaptcha.onTileClicked(index)
                                                if (!correct) {
                                                    matrixError = true
                                                    matrixCaptcha.resetUserPicks()
                                                } else if (complete) {
                                                    isCompleted = true
                                                    onDismissSuccess()
                                                }
                                            }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Mode 4: TYPING MANTRA
            else if (challengeMode == 4) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CyberSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF223252)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "TYPE THE MANTRA EXACTLY",
                            color = Color(0xFFE879F9),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "\"${typingCaptcha.currentMantra}\"",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E293B))
                                .padding(12.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedTextField(
                            value = typedText,
                            onValueChange = {
                                typedText = it
                                typingProgress = typingCaptcha.calculateMatchProgress(it)
                                if (typingCaptcha.validate(it)) {
                                    isCompleted = true
                                    onDismissSuccess()
                                }
                            },
                            label = { Text("Type here...") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFE879F9),
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth(0.95f)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        androidx.compose.material3.LinearProgressIndicator(
                            progress = { typingProgress },
                            modifier = Modifier.fillMaxWidth(0.95f).height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFFE879F9),
                            trackColor = Color(0xFF1E293B)
                        )
                    }
                }
            }

            // Mode 0: MOVING TARGET
            else {
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CyberSurface)
                        .border(1.dp, Color(0xFF223252), RoundedCornerShape(16.dp))
                ) {
                    val arenaWidth = constraints.maxWidth.toFloat()
                    val arenaHeight = constraints.maxHeight.toFloat()
                    val targetSizePx = 180f

                    var posX by remember { mutableFloatStateOf(arenaWidth / 2f - targetSizePx / 2f) }
                    var posY by remember { mutableFloatStateOf(arenaHeight / 2f - targetSizePx / 2f) }
                    var velX by remember { mutableFloatStateOf(7f) }
                    var velY by remember { mutableFloatStateOf(9f) }

                    LaunchedEffect(targetsTapped) {
                        while (targetsTapped < totalRequired) {
                            posX += velX
                            posY += velY

                            if (posX <= 0f) {
                                posX = 0f
                                velX = (Random.nextFloat() * 4f + 6f)
                            } else if (posX + targetSizePx >= arenaWidth) {
                                posX = arenaWidth - targetSizePx
                                velX = -(Random.nextFloat() * 4f + 6f)
                            }

                            if (posY <= 0f) {
                                posY = 0f
                                velY = (Random.nextFloat() * 4f + 6f)
                            } else if (posY + targetSizePx >= arenaHeight) {
                                posY = arenaHeight - targetSizePx
                                velY = -(Random.nextFloat() * 4f + 6f)
                            }

                            delay(16L)
                        }
                    }

                    if (targetsTapped < totalRequired) {
                        Box(
                            modifier = Modifier
                                .offset { IntOffset(posX.roundToInt(), posY.roundToInt()) }
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(CyberCyan)
                                .border(3.dp, Color.White, CircleShape)
                                .clickable {
                                    targetsTapped++
                                    velX = if (Random.nextBoolean()) 8f else -8f
                                    velY = if (Random.nextBoolean()) 10f else -10f
                                    if (targetsTapped >= totalRequired) {
                                        isCompleted = true
                                        onDismissSuccess()
                                    }
                                }
                                .testTag("bouncing_wake_target"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.FlashOn,
                                    contentDescription = "Tap Target",
                                    tint = CyberBg,
                                    modifier = Modifier.size(26.dp)
                                )
                                Text(
                                    text = "TAP ME",
                                    color = CyberBg,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Emergency manual dismiss fallback
            Button(
                onClick = {
                    isCompleted = true
                    onDismissSuccess()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF243048)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("emergency_dismiss_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Alarm,
                    contentDescription = null,
                    tint = CyberCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "EMERGENCY DISMISS ALARM",
                    color = CyberCyan,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

