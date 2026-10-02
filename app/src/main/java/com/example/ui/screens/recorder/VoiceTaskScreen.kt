package com.example.ui.screens.recorder

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.VoiceTaskEntity
import com.example.ui.EnforcerViewModel
import com.example.ui.theme.HarmonyGlassCard
import com.example.ui.theme.KamakuraCloudShadow
import com.example.ui.theme.KamakuraCloudWhite
import com.example.ui.theme.KamakuraCoralWarning
import com.example.ui.theme.KamakuraDeepCobalt
import com.example.ui.theme.KamakuraEmerald
import com.example.ui.theme.KamakuraGlassContainer
import com.example.ui.theme.KamakuraSignBlue
import com.example.ui.theme.KamakuraSkyBackground
import com.example.ui.theme.KamakuraSkyBlue
import com.example.ui.theme.KamakuraSunWarmth
import com.example.ui.theme.KamakuraTextPrimary
import com.example.ui.theme.KamakuraTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VoiceTaskScreen(
    viewModel: EnforcerViewModel,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.allVoiceTasks.collectAsStateWithLifecycle()
    var spokenInputText by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("ALL") }

    val categories = listOf("ALL", "HOMEWORK", "IMPORTANT_TEST", "DUE_DATE", "GENERAL")

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag("voice_task_screen")
        ) {
            // Header: Kamakura Station Style
            HarmonyGlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(KamakuraDeepCobalt),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = "Voice Tasks",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = "ACADEMIC VOICE TASKS",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = KamakuraSignBlue
                            )
                        }
                        Text(
                            text = "Classroom speech & teacher instructions auto-extractor",
                            style = MaterialTheme.typography.bodySmall,
                            color = KamakuraTextSecondary
                        )
                    }

                    // Task counter badge
                    val pendingCount = tasks.count { !it.isCompleted }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (pendingCount > 0) KamakuraSunWarmth.copy(alpha = 0.2f) else KamakuraEmerald.copy(alpha = 0.2f))
                            .border(1.dp, if (pendingCount > 0) KamakuraSunWarmth else KamakuraEmerald, RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "$pendingCount PENDING",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (pendingCount > 0) KamakuraSunWarmth else KamakuraEmerald
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Speech Input Box
            HarmonyGlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "🎙️ Real-time Classroom Voice Extractor",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = KamakuraDeepCobalt
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = spokenInputText,
                            onValueChange = { spokenInputText = it },
                            placeholder = {
                                Text(
                                    "E.g., Complete exercise 4.2 for homework submit tomorrow",
                                    fontSize = 12.sp,
                                    color = KamakuraTextSecondary.copy(alpha = 0.7f)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("voice_task_input"),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White.copy(alpha = 0.8f),
                                focusedBorderColor = KamakuraSkyBlue,
                                unfocusedBorderColor = Color(0xFFBFDBFE),
                                focusedTextColor = KamakuraTextPrimary,
                                unfocusedTextColor = KamakuraTextPrimary
                            ),
                            singleLine = true
                        )

                        Button(
                            onClick = {
                                if (spokenInputText.isNotBlank()) {
                                    viewModel.parseAndSaveVoiceTask(spokenInputText)
                                    spokenInputText = ""
                                }
                            },
                            modifier = Modifier
                                .height(50.dp)
                                .testTag("parse_voice_task_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = KamakuraSignBlue)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Parse & Add",
                                tint = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Quick simulation pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "Pitch Review" to "Note this action item: Review client pitch presentation due Friday",
                            "DB Migration" to "High priority task: Complete Firebase Firestore architectural migration",
                            "Contract Review" to "Important deadline: Finalize legal contract review Monday morning"
                        ).forEach { (label, phrase) ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(KamakuraCloudShadow)
                                    .clickable { spokenInputText = phrase }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    color = KamakuraDeepCobalt,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Category Filter Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                categories.forEach { cat ->
                    val selected = selectedCategoryFilter == cat
                    FilterChip(
                        selected = selected,
                        onClick = { selectedCategoryFilter = cat },
                        label = {
                            Text(
                                text = cat.replace("_", " "),
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = KamakuraSkyBlue,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White.copy(alpha = 0.85f),
                            labelColor = KamakuraTextPrimary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = if (selected) KamakuraSignBlue else Color(0xFFBFDBFE)
                        ),
                        modifier = Modifier.testTag("filter_chip_$cat")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tasks List
            val filteredTasks = tasks.filter {
                if (selectedCategoryFilter == "ALL") true else it.category == selectedCategoryFilter
            }

            if (filteredTasks.isEmpty()) {
                HarmonyGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 20.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Assignment,
                            contentDescription = null,
                            tint = KamakuraSkyBlue.copy(alpha = 0.6f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No Academic Tasks Logged Yet",
                            fontWeight = FontWeight.Bold,
                            color = KamakuraTextPrimary
                        )
                        Text(
                            text = "Speak teacher notes or tap quick presets above!",
                            fontSize = 12.sp,
                            color = KamakuraTextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredTasks, key = { it.id }) { task ->
                        VoiceTaskCard(
                            task = task,
                            onToggle = { isChecked ->
                                viewModel.toggleVoiceTaskCompleted(task.id, isChecked)
                            },
                            onDelete = {
                                viewModel.deleteVoiceTask(task.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VoiceTaskCard(
    task: VoiceTaskEntity,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val urgencyColor = when (task.urgencyLevel) {
        "CRITICAL" -> KamakuraCoralWarning
        "HIGH" -> KamakuraSunWarmth
        else -> KamakuraSkyBlue
    }

    HarmonyGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 18.dp,
        containerColor = if (task.isCompleted) Color.White.copy(alpha = 0.7f) else Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onToggle(!task.isCompleted) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (task.isCompleted) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                    contentDescription = "Toggle Complete",
                    tint = if (task.isCompleted) KamakuraEmerald else KamakuraTextSecondary
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Category pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(urgencyColor.copy(alpha = 0.15f))
                            .border(1.dp, urgencyColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = task.category.replace("_", " "),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = urgencyColor
                        )
                    }

                    Text(
                        text = "• " + SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(task.timestamp)),
                        fontSize = 11.sp,
                        color = KamakuraTextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = task.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = if (task.isCompleted) KamakuraTextSecondary else KamakuraTextPrimary,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
                )

                if (task.lectureTitle.isNotBlank()) {
                    Text(
                        text = "Source: ${task.lectureTitle}",
                        fontSize = 11.sp,
                        color = KamakuraTextSecondary
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Task",
                    tint = KamakuraCoralWarning.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
