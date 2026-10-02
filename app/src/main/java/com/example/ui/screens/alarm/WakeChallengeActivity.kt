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
    var targetsTapped by remember { mutableIntStateOf(0) }
    val totalRequired = 5
    var isCompleted by remember { mutableStateOf(false) }

    // Mode: 0 = Moving Target, 1 = Physics/Math Numericals
    var challengeMode by remember { mutableIntStateOf(0) }

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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

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
                    text = "ENFORCER OS // WAKEFULNESS PROTOCOL",
                    color = CyberRed,
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "RELENTLESS ALARM ACTIVE",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            // 1-Tap In-Class Silence Button (Emergency bypass)
            Button(
                onClick = onDismissSuccess,
                colors = ButtonDefaults.buttonColors(containerColor = CyberRed),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 6.dp)
                    .testTag("in_class_instant_silence_button")
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeOff,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "🚨 IN CLASS? SILENCE IMMEDIATELY (1-TAP)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge
                )
            }

            // Mode Selector Pill
            Row(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(CyberSurface)
                    .border(1.dp, Color(0xFF223252), RoundedCornerShape(20.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = { challengeMode = 0 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (challengeMode == 0) CyberCyan else Color.Transparent
                    ),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "MOVING TARGET",
                        color = if (challengeMode == 0) CyberBg else Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = { challengeMode = 1 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (challengeMode == 1) CyberGreen else Color.Transparent
                    ),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "SOLVE 3 NUMERICALS",
                        color = if (challengeMode == 1) CyberBg else Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (challengeMode == 0) {
                // Moving Target Mode
                Text(
                    text = "Tap the moving target $totalRequired times to prove consciousness.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                // Progress Indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 1..totalRequired) {
                        val achieved = i <= targetsTapped
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(if (achieved) CyberGreen else Color(0xFF1E293B))
                                .border(
                                    1.5.dp,
                                    if (achieved) CyberGreen else Color(0xFF475569),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (achieved) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Done",
                                    tint = CyberBg,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "$targetsTapped / $totalRequired Taps Completed",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (targetsTapped == totalRequired) CyberGreen else CyberCyan,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Interactive Bouncing Target Area
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
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Success",
                                tint = CyberGreen,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "CONSCIOUSNESS VERIFIED",
                                style = MaterialTheme.typography.titleLarge,
                                color = CyberGreen
                            )
                            Text(
                                text = "Alarm dismissed. Rise and conquer the day.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF94A3B8),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            } else {
                // Physics & Math Numerical Mode
                Text(
                    text = "Solve $mathRequired arithmetic numericals to force cognitive arousal and stop the alarm.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                // Math Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = CyberSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF223252)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "PROBLEM ${mathSolvedCount + 1} OF $mathRequired",
                            color = CyberCyan,
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

                        Spacer(modifier = Modifier.height(16.dp))

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
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .testTag("math_challenge_input")
                        )

                        if (mathError) {
                            Text(
                                text = "❌ Incorrect! Try again to stop the alarm.",
                                color = CyberRed,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

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
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .testTag("submit_math_answer_button")
                        ) {
                            Text(
                                text = if (mathSolvedCount + 1 >= mathRequired) "SUBMIT & DISMISS ALARM" else "CHECK & NEXT (1/3)",
                                color = CyberBg,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

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
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "EMERGENCY DISMISS ALARM",
                    color = CyberCyan,
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
