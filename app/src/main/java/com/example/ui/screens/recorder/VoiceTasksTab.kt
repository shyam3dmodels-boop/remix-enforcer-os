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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Task
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.VoiceTaskEntity
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VoiceTasksTab(
    viewModel: EnforcerViewModel,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.allVoiceTasks.collectAsStateWithLifecycle()
    val isSummaryEnabled by viewModel.isDailySummaryEnabled.collectAsStateWithLifecycle()
    val summaryHour by viewModel.summaryHour.collectAsStateWithLifecycle()
    val summaryMinute by viewModel.summaryMinute.collectAsStateWithLifecycle()
    val lastSummarySentTime by viewModel.lastSummarySentTime.collectAsStateWithLifecycle()
    val lastSummaryPendingCount by viewModel.lastSummaryPendingCount.collectAsStateWithLifecycle()

    var spokenInputText by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("ALL") }

    val categories = listOf("ALL", "HOMEWORK", "IMPORTANT_TEST", "DUE_DATE", "GENERAL")

    val pendingHomeworkCount = remember(tasks) { tasks.count { !it.isCompleted && it.category == "HOMEWORK" } }
    val pendingTestCount = remember(tasks) { tasks.count { !it.isCompleted && it.category == "IMPORTANT_TEST" } }
    val totalPendingCount = remember(tasks) { tasks.count { !it.isCompleted } }

    val filteredTasks = remember(tasks, selectedCategoryFilter) {
        if (selectedCategoryFilter == "ALL") tasks
        else tasks.filter { it.category == selectedCategoryFilter }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // End-of-Day Silent Notification Summary Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isSummaryEnabled) CyberSurface else CyberSurfaceVariant.copy(alpha = 0.5f)
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isSummaryEnabled) CyberGreen.copy(alpha = 0.6f) else CyberBorder
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("end_of_day_summary_card")
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSummaryEnabled) CyberGreen.copy(alpha = 0.15f) else CyberSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isSummaryEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                contentDescription = null,
                                tint = if (isSummaryEnabled) CyberGreen else CyberTextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "END-OF-DAY SILENT SUMMARY",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (isSummaryEnabled) CyberGreen else CyberTextMuted
                            )
                            val timeStr = String.format(Locale.getDefault(), "%02d:%02d %s", if (summaryHour % 12 == 0) 12 else summaryHour % 12, summaryMinute, if (summaryHour >= 12) "PM" else "AM")
                            Text(
                                text = if (isSummaryEnabled) "Scheduled daily at $timeStr (Silent)" else "Summary disabled",
                                style = MaterialTheme.typography.bodySmall,
                                color = CyberTextMuted
                            )
                        }
                    }

                    Switch(
                        checked = isSummaryEnabled,
                        onCheckedChange = { viewModel.setDailySummaryEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberBg,
                            checkedTrackColor = CyberGreen,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = CyberSurfaceVariant
                        ),
                        modifier = Modifier.testTag("switch_daily_summary")
                    )
                }

                // Status breakdown
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(CyberAmber.copy(alpha = 0.15f))
                            .border(1.dp, CyberAmber.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(vertical = 6.dp, horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "📚 $pendingHomeworkCount Missed HW",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = CyberAmber
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(CyberRed.copy(alpha = 0.15f))
                            .border(1.dp, CyberRed.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(vertical = 6.dp, horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🎯 $pendingTestCount Priority Tasks",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = CyberRed
                        )
                    }

                    Button(
                        onClick = { viewModel.sendDailySummaryNow() },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(30.dp).testTag("btn_trigger_summary_now")
                    ) {
                        Text(
                            text = "TEST NOW",
                            color = CyberBg,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
        // Voice-to-Task Quick Input Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("voice_to_task_input_card")
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CyberCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "VOICE-TO-TASK CONVERTER",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = CyberCyan
                        )
                        Text(
                            text = "Say \"Note this for homework\" or \"Important test topic\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = CyberTextMuted
                        )
                    }
                }

                OutlinedTextField(
                    value = spokenInputText,
                    onValueChange = { spokenInputText = it },
                    placeholder = {
                        Text(
                            text = "e.g. \"Note this for homework: Complete HC Verma Ch 4 Q 1-15\"",
                            fontSize = 12.sp,
                            color = CyberTextMuted
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_voice_task_phrase"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = CyberBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = CyberCyan
                    ),
                    maxLines = 3,
                    shape = RoundedCornerShape(8.dp)
                )

                // Quick Spoken Phrase Templates
                Text(
                    text = "QUICK VOICE TRIGGER SHORTCUTS:",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = CyberTextMuted
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            spokenInputText = "Note this for homework: Solve organic chemistry mechanism problems 1 to 10"
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberAmber),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberAmber.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f).height(32.dp).testTag("btn_voice_quick_hw")
                    ) {
                        Text("📚 HW Note", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }

                    OutlinedButton(
                        onClick = {
                            spokenInputText = "Important test topic: Doppler effect in acoustics and electromagnetic waves"
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberRed),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberRed.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f).height(32.dp).testTag("btn_voice_quick_test")
                    ) {
                        Text("🎯 Priority Task", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }

                    Button(
                        onClick = {
                            if (spokenInputText.isNotBlank()) {
                                viewModel.parseAndSaveVoiceTask(spokenInputText, "Lecture Session")
                                spokenInputText = ""
                            }
                        },
                        enabled = spokenInputText.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberCyan,
                            disabledContainerColor = CyberSurfaceVariant
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(32.dp).testTag("btn_convert_voice_task")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Convert",
                            tint = CyberBg,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "PARSE",
                            color = CyberBg,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Category Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            categories.forEach { cat ->
                val isSelected = selectedCategoryFilter == cat
                val label = when (cat) {
                    "ALL" -> "ALL (${tasks.size})"
                    "HOMEWORK" -> "📚 HW (${tasks.count { it.category == "HOMEWORK" }})"
                    "IMPORTANT_TEST" -> "🎯 TEST (${tasks.count { it.category == "IMPORTANT_TEST" }})"
                    "DUE_DATE" -> "⏰ DUE"
                    else -> "📝 GENERAL"
                }

                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategoryFilter = cat },
                    label = { Text(label, fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyberCyan,
                        selectedLabelColor = CyberBg,
                        containerColor = CyberSurface,
                        labelColor = CyberTextMuted
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = if (isSelected) CyberCyan else CyberBorder
                    ),
                    modifier = Modifier.height(28.dp).testTag("filter_chip_$cat")
                )
            }
        }

        // Tasks List
        if (filteredTasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberSurface)
                    .border(1.dp, CyberBorder, RoundedCornerShape(12.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Assignment,
                        contentDescription = null,
                        tint = CyberTextMuted,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "NO VOICE TASKS LOGGED YET",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Say or type keywords like \"Note this for homework\" or \"Important test topic\" to auto-populate your daily study checklist.",
                        style = MaterialTheme.typography.bodySmall,
                        color = CyberTextMuted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredTasks, key = { it.id }) { task ->
                    VoiceTaskCard(
                        task = task,
                        onToggleCompleted = { viewModel.toggleVoiceTaskCompleted(task.id, !task.isCompleted) },
                        onDelete = { viewModel.deleteVoiceTask(task.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun VoiceTaskCard(
    task: VoiceTaskEntity,
    onToggleCompleted: () -> Unit,
    onDelete: () -> Unit
) {
    val categoryColor = when (task.category) {
        "IMPORTANT_TEST" -> CyberRed
        "HOMEWORK" -> CyberAmber
        "DUE_DATE" -> CyberCyan
        else -> CyberGreen
    }

    val categoryBadge = when (task.category) {
        "IMPORTANT_TEST" -> "🎯 HIGH PRIORITY"
        "HOMEWORK" -> "📋 ACTION ITEM"
        "DUE_DATE" -> "⏰ DEADLINE"
        else -> "📝 GENERAL"
    }

    val sdf = SimpleDateFormat("MMM d, hh:mm a", Locale.getDefault())
    val dateStr = sdf.format(Date(task.timestamp))

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted) CyberSurfaceVariant.copy(alpha = 0.5f) else CyberSurface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (task.isCompleted) CyberBorder else categoryColor.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("voice_task_${task.id}")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconButton(
                onClick = onToggleCompleted,
                modifier = Modifier.size(24.dp).testTag("btn_toggle_task_${task.id}")
            ) {
                Icon(
                    imageVector = if (task.isCompleted) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                    contentDescription = if (task.isCompleted) "Completed" else "Incomplete",
                    tint = if (task.isCompleted) CyberGreen else CyberCyan,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(categoryColor.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = categoryBadge,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            color = categoryColor
                        )
                    }
                    Text(
                        text = "• $dateStr",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = CyberTextMuted
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (task.isCompleted) FontWeight.Normal else FontWeight.SemiBold,
                    color = if (task.isCompleted) CyberTextMuted else Color.White,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )

                if (task.rawVoiceText.isNotBlank() && task.rawVoiceText != task.title) {
                    Text(
                        text = "Spoken: \"${task.rawVoiceText}\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = CyberTextMuted,
                        fontSize = 10.sp
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp).testTag("btn_delete_task_${task.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete task",
                    tint = CyberTextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
