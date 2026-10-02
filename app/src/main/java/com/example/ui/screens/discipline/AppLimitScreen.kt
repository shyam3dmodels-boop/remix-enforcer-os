package com.example.ui.screens.discipline

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterBAndW
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.AppLimitEntity
import com.example.ui.EnforcerViewModel
import com.example.ui.theme.HarmonyGlassCard
import com.example.ui.theme.KamakuraCloudShadow
import com.example.ui.theme.KamakuraCloudWhite
import com.example.ui.theme.KamakuraCoralWarning
import com.example.ui.theme.KamakuraDeepCobalt
import com.example.ui.theme.KamakuraEmerald
import com.example.ui.theme.KamakuraSignBlue
import com.example.ui.theme.KamakuraSkyBackground
import com.example.ui.theme.KamakuraSkyBlue
import com.example.ui.theme.KamakuraSunWarmth
import com.example.ui.theme.KamakuraTextPrimary
import com.example.ui.theme.KamakuraTextSecondary

@Composable
fun AppLimitScreen(viewModel: EnforcerViewModel) {
    val limits by viewModel.appLimits.collectAsStateWithLifecycle()
    val hasPermission = viewModel.hasUsageStatsPermission()
    var showAddDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState)
                .testTag("app_limit_screen"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Screen Header: Station Style
            HarmonyGlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 24.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "APP DISCIPLINE SHIELD",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp,
                            color = KamakuraSignBlue
                        )
                        Text(
                            text = "Parental Screen Time & Focus Enforcer",
                            fontSize = 11.sp,
                            color = KamakuraTextSecondary
                        )
                    }

                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = KamakuraSignBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("add_limit_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Limit",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add App", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Permission Card (if not granted)
            if (!hasPermission) {
                HarmonyGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 20.dp,
                    containerColor = KamakuraSunWarmth.copy(alpha = 0.15f),
                    borderColor = KamakuraSunWarmth
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
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = KamakuraSunWarmth,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "USAGE ACCESS PERMISSION REQUIRED",
                                fontWeight = FontWeight.Bold,
                                color = KamakuraSignBlue
                            )
                        }
                        Text(
                            text = "To track foreground screentime on designated apps, grant Usage Access permission to Remix Enforcer OS.",
                            fontSize = 12.sp,
                            color = KamakuraTextPrimary
                        )
                        Button(
                            onClick = { viewModel.openUsageStatsSettings() },
                            colors = ButtonDefaults.buttonColors(containerColor = KamakuraSunWarmth),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Grant Usage Access",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Two-Stage Focus Mechanics Card
            HarmonyGlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp
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
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = KamakuraSignBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "TWO-STAGE FOCUS MECHANICS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = KamakuraSignBlue
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Min 40 Card
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(KamakuraCloudShadow)
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FilterBAndW,
                                        contentDescription = null,
                                        tint = KamakuraSunWarmth,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text("MIN 40", color = KamakuraSunWarmth, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                                Text(
                                    text = "Grayscale saturation warning to cut dopamine loops.",
                                    fontSize = 11.sp,
                                    color = KamakuraTextSecondary,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }

                        // Min 45 Card
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(KamakuraCloudShadow)
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = KamakuraCoralWarning,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text("MIN 45", color = KamakuraCoralWarning, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                                Text(
                                    text = "Hard Lockout barrier and auto-minimize to home screen.",
                                    fontSize = 11.sp,
                                    color = KamakuraTextSecondary,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Monitored Apps List
            Text(
                text = "MONITORED APPS & DAILY QUOTAS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp,
                color = KamakuraDeepCobalt
            )

            limits.forEach { limit ->
                val usedMinutes = viewModel.getTodayUsageMinutes(limit.packageName)
                AppLimitItemCard(
                    limit = limit,
                    usedMinutes = usedMinutes,
                    onToggleRestricted = {
                        viewModel.updateAppLimit(limit.copy(isRestricted = !limit.isRestricted))
                    },
                    onDelete = {
                        viewModel.deleteAppLimit(limit)
                    }
                )
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // Add Custom Limit Dialog
    if (showAddDialog) {
        var appName by remember { mutableStateOf("") }
        var packageName by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("Social") }
        var limitMins by remember { mutableIntStateOf(45) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text("Add Monitored App", color = KamakuraSignBlue, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = appName,
                        onValueChange = { appName = it },
                        label = { Text("App Name (e.g. Reddit, Twitter)") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = packageName,
                        onValueChange = { packageName = it },
                        label = { Text("Package Name (e.g. com.reddit.frontpage)") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category (Social / AI / Browser / Entertainment)") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (appName.isNotBlank() && packageName.isNotBlank()) {
                            viewModel.addCustomAppLimit(
                                AppLimitEntity(
                                    appName = appName,
                                    packageName = packageName,
                                    category = category,
                                    dailyLimitMinutes = limitMins,
                                    warningMinutes = (limitMins - 5).coerceAtLeast(5)
                                )
                            )
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KamakuraSignBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save App", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showAddDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel", color = KamakuraTextSecondary)
                }
            }
        )
    }
}

@Composable
fun AppLimitItemCard(
    limit: AppLimitEntity,
    usedMinutes: Int,
    onToggleRestricted: () -> Unit,
    onDelete: () -> Unit
) {
    val progress = (usedMinutes.toFloat() / limit.dailyLimitMinutes.toFloat()).coerceIn(0f, 1f)
    val isOverWarning = usedMinutes >= limit.warningMinutes
    val isOverLimit = usedMinutes >= limit.dailyLimitMinutes

    HarmonyGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("app_limit_item_${limit.appName}"),
        cornerRadius = 20.dp,
        containerColor = Color.White
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = limit.appName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = KamakuraTextPrimary
                    )
                    Text(
                        text = "${limit.category} • ${limit.packageName}",
                        fontSize = 11.sp,
                        color = KamakuraTextSecondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = limit.isRestricted,
                        onCheckedChange = { onToggleRestricted() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = KamakuraEmerald,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFFCBD5E1)
                        )
                    )

                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = KamakuraCoralWarning.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Progress Bar & Minutes Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Usage Today: ${usedMinutes}m / ${limit.dailyLimitMinutes}m limit",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isOverLimit) KamakuraCoralWarning else if (isOverWarning) KamakuraSunWarmth else KamakuraSignBlue
                )

                Text(
                    text = if (isOverLimit) "HARD LOCKOUT" else if (isOverWarning) "GRAYSCALE WARN" else "NORMAL",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isOverLimit) KamakuraCoralWarning else if (isOverWarning) KamakuraSunWarmth else KamakuraEmerald
                )
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (isOverLimit) KamakuraCoralWarning else if (isOverWarning) KamakuraSunWarmth else KamakuraSkyBlue,
                trackColor = KamakuraCloudShadow
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Warning: ${limit.warningMinutes}m",
                    fontSize = 10.sp,
                    color = KamakuraTextSecondary
                )
                Text(
                    text = "Hard Lockout: ${limit.dailyLimitMinutes}m",
                    fontSize = 10.sp,
                    color = KamakuraTextSecondary
                )
            }
        }
    }
}
