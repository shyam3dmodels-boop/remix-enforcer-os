package com.example.ui.screens.ai

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChatBubbleOutline

import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.core.ai.AiKeySyncManager
import com.example.core.ai.ModelOption
import com.example.core.cloud.FirestoreChatMessage
import com.example.data.local.entity.AiChatMessageEntity
import com.example.data.local.entity.AiChatSessionEntity
import com.example.data.local.entity.AiKeyEntity
import com.example.ui.EnforcerViewModel
import com.example.ui.theme.KamakuraTextPrimary
import com.example.ui.theme.KamakuraTextSecondary
import com.example.ui.theme.LiquidGlassButton
import com.example.ui.theme.LiquidGlassFill
import com.example.ui.theme.PaletteCornflower
import com.example.ui.theme.PaletteIceCyan
import com.example.ui.theme.PaletteMintFrost
import com.example.ui.theme.PaletteSoftSky
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatAiScreen(
    viewModel: EnforcerViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val firestoreMessages by viewModel.aiChatMessages.collectAsState()
    val localMessages by viewModel.localSessionMessages.collectAsState()
    val allSessions by viewModel.allChatSessions.collectAsState()
    val allKeys by viewModel.allAiKeys.collectAsState()
    val activeModel by viewModel.activeModel.collectAsState()
    val activeSessionId by viewModel.activeSessionId.collectAsState()
    val isAiThinking by viewModel.isAiChatThinking.collectAsState()
    val isConnectorEnabled by viewModel.isTelemetryConnectorEnabled.collectAsState()
    val steps by viewModel.stepsToday.collectAsState()
    val batteryStatus by viewModel.batteryStatus.collectAsState()

    var textInput by remember { mutableStateOf("") }
    var isAgentMode by remember { mutableStateOf(false) }
    var showModelPicker by remember { mutableStateOf(false) }
    var showHistoryDrawer by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()

    // Determine active message list: Local Session if available, else Firestore stream
    val displayMessages: List<DisplayMessage> = remember(localMessages, firestoreMessages) {
        if (localMessages.isNotEmpty()) {
            localMessages.map {
                DisplayMessage(
                    id = it.id.toString(),
                    sender = it.sender,
                    content = it.content,
                    groundingMeta = it.groundingMeta,
                    modelBadge = it.modelUsed
                )
            }
        } else {
            firestoreMessages.map {
                DisplayMessage(
                    id = it.id.ifEmpty { it.timestamp },
                    sender = it.sender,
                    content = it.content,
                    groundingMeta = it.grounding_context?.let { g -> "steps:${g["steps_at_time"]}|loc:${g["active_location"]}" } ?: "",
                    modelBadge = activeModel.displayName
                )
            }
        }
    }

    LaunchedEffect(displayMessages.size) {
        if (displayMessages.isNotEmpty()) {
            listState.animateScrollToItem(displayMessages.size - 1)
        }
    }

    // Hardware & Autonomous C2 dispatcher when Agent Mode is active
    val handleSendMessage: (String) -> Unit = { rawText ->
        if (rawText.isNotBlank()) {
            val input = rawText.trim()
            if (isAgentMode) {
                val lower = input.lowercase()
                when {
                    lower.contains("photo") || lower.contains("camera") || lower.contains("picture") -> {
                        viewModel.executeTelegramCommandDirect(if (lower.contains("front")) "/photo front" else "/photo")
                    }
                    lower.contains("locate") || lower.contains("gps") || lower.contains("where") -> {
                        viewModel.executeTelegramCommandDirect("/locate")
                    }
                    lower.contains("siren") || lower.contains("alarm") -> {
                        viewModel.executeTelegramCommandDirect("/siren 10")
                    }
                    lower.contains("mute") || lower.contains("silence") -> {
                        viewModel.executeTelegramCommandDirect("/mute")
                    }
                    lower.contains("wipe") -> {
                        viewModel.executeTelegramCommandDirect("/wipe")
                    }
                    lower.contains("scan") || lower.contains("arp") || lower.contains("wifi") || lower.contains("netscan") -> {
                        viewModel.executeTelegramCommandDirect("/status")
                    }
                    lower.contains("status") || lower.contains("telemetry") || lower.contains("battery") -> {
                        viewModel.executeTelegramCommandDirect("/status")
                    }
                }
                viewModel.sendAiChatMessage("[AGENT] $input")
            } else {
                viewModel.sendAiChatMessage(input)
            }
        }
    }

    val quickPrompts = if (isAgentMode) {
        listOf(
            "⏰ Set 6:30 AM Wakeup Alarm",
            "📝 Add Task: Review OS Architecture",
            "📍 Pin Current GPS Coordinates",
            "📸 Take Front Photo",
            "🛡️ Wi-Fi ARP Security Scan",
            "🔋 Battery & Telemetry"
        )
    } else {
        listOf(
            "⏰ Set Alarm for 7:00 AM",
            "📝 Add To-Do: Study & Code",
            "📍 Pin GPS Location",
            "📊 Steps & Health Check",
            "🎙️ Transcribe Voice Memo",
            "🧠 Search Brain Memory"
        )
    }

    Box(modifier = modifier.fillMaxSize().testTag("chat_ai_screen")) {
        Column(modifier = Modifier.fillMaxSize()) {

            // 1. Dedicated Header Bar (Model Selector + Session History + Settings)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                shape = RoundedCornerShape(22.dp),
                color = LiquidGlassFill,
                shadowElevation = 3.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left: Back button & Title
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            IconButton(
                                onClick = onNavigateBack,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    tint = PaletteCornflower
                                )
                            }
                            Text(
                                text = "SECONDARY BRAIN",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp,
                                    fontSize = 15.sp
                                ),
                                color = PaletteCornflower
                            )
                        }

                        // Right Actions: History Drawer & Settings Modal
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // History Icon with count badge
                            IconButton(
                                onClick = { showHistoryDrawer = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.TopEnd) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = "Previous Chats",
                                        tint = PaletteCornflower,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    if (allSessions.isNotEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(PaletteCornflower)
                                        )
                                    }
                                }
                            }

                            // Settings Gear Icon
                            IconButton(
                                onClick = { showSettingsDialog = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "AI Settings & Keys",
                                    tint = PaletteCornflower,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Mode Switcher: Clean Frosted Glass Segmented Pill (Chat vs Agent)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White.copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, PaletteIceCyan.copy(alpha = 0.6f)),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Chat Pill
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (!isAgentMode) PaletteCornflower else Color.Transparent,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable { isAgentMode = false }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ChatBubbleOutline,
                                            contentDescription = "Chat Mode",
                                            tint = if (!isAgentMode) Color.White else KamakuraTextSecondary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "Chat",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = if (!isAgentMode) FontWeight.ExtraBold else FontWeight.Medium,
                                                fontSize = 12.sp
                                            ),
                                            color = if (!isAgentMode) Color.White else KamakuraTextSecondary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(2.dp))

                                // Agent Pill
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isAgentMode) Color(0xFF0D9488) else Color.Transparent,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable { isAgentMode = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Bolt,
                                            contentDescription = "Agent Mode",
                                            tint = if (isAgentMode) Color.White else KamakuraTextSecondary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "Agent",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = if (isAgentMode) FontWeight.ExtraBold else FontWeight.Medium,
                                                fontSize = 12.sp
                                            ),
                                            color = if (isAgentMode) Color.White else KamakuraTextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Center Row: Clean Interactive Model Selector Pill & Telemetry Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Clickable Model Selector Pill (like Claude selector)
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White.copy(alpha = 0.90f),
                            modifier = Modifier
                                .clickable { showModelPicker = true }
                                .border(1.5.dp, if (isAgentMode) Color(0xFF0D9488).copy(alpha = 0.4f) else PaletteCornflower.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(activeModel.iconEmoji, fontSize = 14.sp)
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = activeModel.displayName,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            ),
                                            color = if (isAgentMode) Color(0xFF0D9488) else PaletteCornflower
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Choose Model",
                                            tint = if (isAgentMode) Color(0xFF0D9488) else PaletteCornflower,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Text(
                                        text = "${activeModel.provider.uppercase()} • ${if (isAgentMode) "C2 DAEMON" else activeModel.badge}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = KamakuraTextSecondary
                                    )
                                }
                            }
                        }

                        // Telemetry Connector Toggle Pill (Privacy-First)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isConnectorEnabled) {
                                if (isAgentMode) Color(0xFF0D9488).copy(alpha = 0.15f) else PaletteMintFrost.copy(alpha = 0.85f)
                            } else {
                                Color.LightGray.copy(alpha = 0.25f)
                            },
                            border = BorderStroke(
                                1.dp,
                                if (isConnectorEnabled) {
                                    if (isAgentMode) Color(0xFF0D9488).copy(alpha = 0.5f) else PaletteCornflower.copy(alpha = 0.5f)
                                } else {
                                    Color.LightGray.copy(alpha = 0.4f)
                                }
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { viewModel.toggleTelemetryConnector() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isConnectorEnabled) {
                                        if (isAgentMode) Icons.Default.Bolt else Icons.Default.AutoAwesome
                                    } else {
                                        Icons.Default.LocationOn
                                    },
                                    contentDescription = "Telemetry Connector",
                                    tint = if (isConnectorEnabled) {
                                        if (isAgentMode) Color(0xFF0D9488) else PaletteCornflower
                                    } else {
                                        Color.Gray
                                    },
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = if (isConnectorEnabled) {
                                        "🔗 Connector: ON ($steps stp • ${batteryStatus.percent}%)"
                                    } else {
                                        "🔒 Connector: OFF"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = if (isConnectorEnabled) {
                                        if (isAgentMode) Color(0xFF0D9488) else PaletteCornflower
                                    } else {
                                        Color.Gray
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Subtle Status Banner when Agent Mode is Active
            AnimatedVisibility(
                visible = isAgentMode,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0D9488).copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color(0xFF0D9488).copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0D9488))
                        )
                        Text(
                            text = "⚡ Autonomous Agent Active: Hardware execution & vision commands enabled",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp
                            ),
                            color = Color(0xFF0F766E)
                        )
                    }
                }
            }

            // 2. Chat Messages Stream
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .testTag("chat_messages_list"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (displayMessages.isEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        EmptyChatGreetingCard(
                            model = activeModel,
                            isAgentMode = isAgentMode,
                            onPromptSelected = handleSendMessage,
                            onOpenSettings = { showSettingsDialog = true }
                        )
                    }
                } else {
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    items(displayMessages, key = { it.id }) { msg ->
                        MessageBubble(message = msg, activeModel = activeModel)
                    }
                }

                if (isAiThinking) {
                    item {
                        ThinkingBubble(model = activeModel)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            // 3. Quick Suggestions Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(quickPrompts) { prompt ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.90f),
                        modifier = Modifier
                            .clickable { handleSendMessage(prompt) }
                            .border(1.dp, if (isAgentMode) Color(0xFF0D9488).copy(alpha = 0.4f) else PaletteIceCyan.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                    ) {
                        Text(
                            text = prompt,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = if (isAgentMode) Color(0xFF0D9488) else PaletteCornflower,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // 4. Liquid Glass Input Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                shape = RoundedCornerShape(26.dp),
                color = LiquidGlassFill,
                shadowElevation = 5.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = {
                            Text(
                                text = if (isAgentMode) {
                                    "Command agent (e.g. 'take photo', 'locate', 'siren')..."
                                } else {
                                    "Ask ${activeModel.displayName} anything..."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = KamakuraTextSecondary,
                                fontSize = 13.sp
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_field"),
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isAgentMode) Color(0xFF0D9488) else PaletteCornflower,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.White.copy(alpha = 0.92f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.85f)
                        ),
                        maxLines = 3
                    )

                    LiquidGlassButton(
                        onClick = {
                            if (textInput.isNotBlank()) {
                                val prompt = textInput
                                textInput = ""
                                handleSendMessage(prompt)
                            }
                        },
                        modifier = Modifier
                            .size(46.dp)
                            .testTag("chat_send_button"),
                        cornerRadius = 23.dp,
                        containerColor = if (isAgentMode) Color(0xFF0D9488) else PaletteCornflower
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = if (isAgentMode) Icons.Default.Bolt else Icons.Default.Send,
                                contentDescription = if (isAgentMode) "Execute Agent Action" else "Send",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // --- Model Picker Bottom Sheet ---
        if (showModelPicker) {
            ModalBottomSheet(
                onDismissRequest = { showModelPicker = false },
                sheetState = sheetState,
                containerColor = Color.White
            ) {
                ModelPickerSheet(
                    activeModel = activeModel,
                    onSelectModel = {
                        viewModel.setActiveModel(it)
                        showModelPicker = false
                    },
                    onOpenSettings = {
                        showModelPicker = false
                        showSettingsDialog = true
                    }
                )
            }
        }

        // --- Previous Chats History Drawer ---
        if (showHistoryDrawer) {
            PreviousChatsDrawer(
                sessions = allSessions,
                activeSessionId = activeSessionId,
                onSelectSession = {
                    viewModel.selectChatSession(it)
                    showHistoryDrawer = false
                },
                onNewSession = {
                    viewModel.startNewChatSession()
                    showHistoryDrawer = false
                },
                onDeleteSession = { viewModel.deleteChatSession(it) },
                onBackupToTelegram = { viewModel.backupCurrentSessionToTelegram() },
                onClose = { showHistoryDrawer = false }
            )
        }

        // --- Universal AI Key Vault & Settings Modal ---
        if (showSettingsDialog) {
            UniversalAiKeyVaultDialog(
                allKeys = allKeys,
                activeModel = activeModel,
                onSaveUniversalKey = { key -> viewModel.saveUniversalApiKey(key) },
                onDeleteKey = { viewModel.deleteAiKey(it) },
                onSyncWithServer = { viewModel.syncKeysWithServer() },
                onBackupToTelegram = { viewModel.backupCurrentSessionToTelegram() },
                onDismiss = { showSettingsDialog = false }
            )
        }
    }
}

data class DisplayMessage(
    val id: String,
    val sender: String,
    val content: String,
    val groundingMeta: String = "",
    val modelBadge: String = ""
)

@Composable
private fun MessageBubble(message: DisplayMessage, activeModel: ModelOption) {
    val isUser = message.sender.equals("USER", ignoreCase = true)
    val isAgentAction = message.content.startsWith("[AGENT")
    val cleanContent = if (isAgentAction) {
        message.content.replaceFirst(Regex("^\\[AGENT.*?\\]\\s*"), "")
    } else {
        message.content
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (isAgentAction) Color(0xFF0D9488).copy(alpha = 0.20f) else PaletteIceCyan)
            ) {
                if (isAgentAction) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Agent",
                        tint = Color(0xFF0D9488),
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Text(activeModel.iconEmoji, fontSize = 16.sp)
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 295.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomStart = if (isUser) 18.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 18.dp
                ),
                color = when {
                    isUser && isAgentAction -> Color(0xFF0D9488)
                    isUser -> PaletteCornflower
                    else -> Color.White.copy(alpha = 0.95f)
                },
                shadowElevation = 2.dp,
                modifier = Modifier.border(
                    width = 1.dp,
                    color = when {
                        isUser -> Color.Transparent
                        isAgentAction -> Color(0xFF0D9488).copy(alpha = 0.35f)
                        else -> PaletteIceCyan.copy(alpha = 0.5f)
                    },
                    shape = RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isUser) 18.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 18.dp
                    )
                )
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    if (isAgentAction) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isUser) Color.White.copy(alpha = 0.22f) else Color(0xFF0D9488).copy(alpha = 0.12f),
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = if (isUser) Color.White else Color(0xFF0D9488),
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = "AGENT HARDWARE ACTION",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    color = if (isUser) Color.White else Color(0xFF0D9488)
                                )
                            }
                        }
                    }

                    Text(
                        text = cleanContent,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = if (isUser) Color.White else KamakuraTextPrimary,
                            lineHeight = 20.sp
                        )
                    )
                }
            }

            // Model badge and grounding note for AI responses
            if (!isUser) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.padding(start = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (message.modelBadge.isNotBlank()) message.modelBadge else activeModel.displayName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (isAgentAction) Color(0xFF0D9488) else PaletteCornflower
                    )
                    Text(
                        text = if (isAgentAction) "• C2 Direct Execution" else "• Local DB + Cloud Synced",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = KamakuraTextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun ThinkingBubble(model: ModelOption) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(PaletteIceCyan)
        ) {
            Text(model.iconEmoji, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White.copy(alpha = 0.92f),
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = PaletteCornflower,
                    strokeWidth = 2.dp
                )
                Text(
                    text = "${model.displayName} is reasoning...",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    ),
                    color = KamakuraTextSecondary
                )
            }
        }
    }
}

@Composable
private fun EmptyChatGreetingCard(
    model: ModelOption,
    isAgentMode: Boolean,
    onPromptSelected: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color.White.copy(alpha = 0.94f),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (isAgentMode) Color(0xFF0D9488).copy(alpha = 0.5f) else PaletteIceCyan.copy(alpha = 0.6f),
                RoundedCornerShape(22.dp)
            )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isAgentMode) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0D9488).copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Agent",
                            tint = Color(0xFF0D9488),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else {
                    Text(model.iconEmoji, fontSize = 28.sp)
                }

                Column {
                    Text(
                        text = if (isAgentMode) "Autonomous Device Agent" else "Active Model: ${model.displayName}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isAgentMode) Color(0xFF0D9488) else PaletteCornflower
                        )
                    )
                    Text(
                        text = if (isAgentMode) "Local C2 Hardware Daemon • Direct Peripheral Execution" else "${model.provider.uppercase()} Engine • Multi-Tier Sync",
                        style = MaterialTheme.typography.labelSmall,
                        color = KamakuraTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (isAgentMode) {
                    "Agent Mode connects your Secondary Brain directly to Android hardware controls. Send commands to snap photos, track GPS, sound alarms, audit network ARP tables, or automate background actions."
                } else {
                    model.description
                },
                style = MaterialTheme.typography.bodySmall.copy(
                    color = KamakuraTextPrimary,
                    lineHeight = 18.sp
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isAgentMode) Color(0xFF0D9488).copy(alpha = 0.12f) else PaletteIceCyan.copy(alpha = 0.4f),
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            if (isAgentMode) {
                                onPromptSelected("Check hardware status and battery telemetry")
                            } else {
                                onOpenSettings()
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isAgentMode) Icons.Default.Bolt else Icons.Default.Key,
                            contentDescription = if (isAgentMode) "Status" else "Manage Keys",
                            tint = if (isAgentMode) Color(0xFF0D9488) else PaletteCornflower,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isAgentMode) "Audit Hardware" else "Configure Keys",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isAgentMode) Color(0xFF0D9488) else PaletteCornflower
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isAgentMode) Color(0xFF0D9488).copy(alpha = 0.18f) else PaletteMintFrost.copy(alpha = 0.6f),
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            if (isAgentMode) {
                                onPromptSelected("Take photo front")
                            } else {
                                onPromptSelected("Summarize all active voice tasks and health status")
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isAgentMode) Icons.Default.Bolt else Icons.Default.AutoAwesome,
                            contentDescription = "Quick Start",
                            tint = if (isAgentMode) Color(0xFF0D9488) else PaletteCornflower,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isAgentMode) "Snap Front Cam" else "Quick Summary",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isAgentMode) Color(0xFF0D9488) else PaletteCornflower
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelPickerSheet(
    activeModel: ModelOption,
    onSelectModel: (ModelOption) -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "CHOOSE AI MODEL",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = PaletteCornflower
                )
                Text(
                    text = "Select from Claude, Gemini, DeepSeek, NVIDIA, OpenRouter & Groq",
                    style = MaterialTheme.typography.labelSmall,
                    color = KamakuraTextSecondary
                )
            }
            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = "API Keys",
                    tint = PaletteCornflower
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(AiKeySyncManager.AVAILABLE_MODELS) { opt ->
                val isSelected = opt.id == activeModel.id
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) PaletteIceCyan.copy(alpha = 0.45f) else Color.White,
                    shadowElevation = if (isSelected) 2.dp else 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectModel(opt) }
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) PaletteCornflower else Color.LightGray.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(16.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(opt.iconEmoji, fontSize = 24.sp)
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = opt.displayName,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = PaletteCornflower
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = PaletteMintFrost.copy(alpha = 0.8f)
                                ) {
                                    Text(
                                        text = opt.badge,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = PaletteCornflower,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = opt.description,
                                style = MaterialTheme.typography.labelSmall,
                                color = KamakuraTextSecondary,
                                maxLines = 2
                            )
                        }

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Selected",
                                tint = PaletteCornflower,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
    }
}

@Composable
private fun PreviousChatsDrawer(
    sessions: List<AiChatSessionEntity>,
    activeSessionId: String,
    onSelectSession: (String) -> Unit,
    onNewSession: () -> Unit,
    onDeleteSession: (String) -> Unit,
    onBackupToTelegram: () -> Unit,
    onClose: () -> Unit
) {
    Dialog(onDismissRequest = onClose) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.85f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "CHAT HISTORY",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = PaletteCornflower
                        )
                        Text(
                            text = "Stored in Local Room DB + Server SQLite",
                            style = MaterialTheme.typography.labelSmall,
                            color = KamakuraTextSecondary
                        )
                    }
                    IconButton(onClick = onClose) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = KamakuraTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Actions: New Chat & Telegram Backup
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = PaletteCornflower,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNewSession() }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "New", tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("New Chat", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = PaletteIceCyan.copy(alpha = 0.5f),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onBackupToTelegram() }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(imageVector = Icons.Default.CloudUpload, contentDescription = "Backup", tint = PaletteCornflower, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Telegram Cloud", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = PaletteCornflower)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = Color.LightGray.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(8.dp))

                if (sessions.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No previous conversations logged yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = KamakuraTextSecondary
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(sessions) { s ->
                            val isCurrent = s.sessionId == activeSessionId
                            val sdf = remember { SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()) }
                            val dateStr = remember(s.updatedAt) { sdf.format(Date(s.updatedAt)) }

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isCurrent) PaletteIceCyan.copy(alpha = 0.45f) else Color.White,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectSession(s.sessionId) }
                                    .border(
                                        width = 1.dp,
                                        color = if (isCurrent) PaletteCornflower else Color.LightGray.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = s.title.ifEmpty { "Chat Session" },
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = PaletteCornflower,
                                            maxLines = 1
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${s.provider.uppercase()} • $dateStr",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = KamakuraTextSecondary
                                        )
                                    }

                                    IconButton(
                                        onClick = { onDeleteSession(s.sessionId) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = Color.Gray.copy(alpha = 0.6f),
                                            modifier = Modifier.size(16.dp)
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
private fun UniversalAiKeyVaultDialog(
    allKeys: List<AiKeyEntity>,
    activeModel: ModelOption,
    onSaveUniversalKey: (key: String) -> Boolean,
    onDeleteKey: (provider: String) -> Unit,
    onSyncWithServer: () -> Unit,
    onBackupToTelegram: () -> Unit,
    onDismiss: () -> Unit
) {
    var pastedKey by remember { mutableStateOf("") }
    var saveStatusMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }

    val detectedProvider = remember(pastedKey) {
        if (pastedKey.isNotBlank()) com.example.core.ai.AiKeyDetector.detectProvider(pastedKey.trim()) else null
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.90f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "UNIVERSAL AI KEY VAULT",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = PaletteCornflower
                        )
                        Text(
                            text = "Auto-detects 11+ Providers from 1 Single Input",
                            style = MaterialTheme.typography.labelSmall,
                            color = KamakuraTextSecondary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = KamakuraTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Cloud Sync Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = PaletteIceCyan.copy(alpha = 0.5f),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSyncWithServer() }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(imageVector = Icons.Default.Sync, contentDescription = "Sync", tint = PaletteCornflower, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sync Server DB", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = PaletteCornflower)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = PaletteMintFrost.copy(alpha = 0.7f),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onBackupToTelegram() }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(imageVector = Icons.Default.CloudDone, contentDescription = "Telegram", tint = PaletteCornflower, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Telegram Backup", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = PaletteCornflower)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Divider(color = Color.LightGray.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(8.dp))

                // Single Universal Input Field
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.5.dp, PaletteCornflower.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "🔑 Paste Any AI API Key",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = PaletteCornflower
                            )

                            if (detectedProvider != null) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = PaletteCornflower.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "${detectedProvider.emoji} ${detectedProvider.name}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 11.sp
                                        ),
                                        color = PaletteCornflower,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = pastedKey,
                            onValueChange = {
                                pastedKey = it
                                saveStatusMessage = null
                            },
                            placeholder = {
                                Text(
                                    "Paste Claude (sk-ant-...), Gemini (AIzaSy...), Groq (gsk_...), OpenAI, OpenRouter, DeepSeek, Cerebras, Fireworks, etc.",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PaletteCornflower,
                                unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (pastedKey.isNotBlank()) {
                                Text(
                                    text = "Clear",
                                    style = MaterialTheme.typography.labelSmall.copy(color = Color.Red.copy(alpha = 0.7f)),
                                    modifier = Modifier
                                        .clickable {
                                            pastedKey = ""
                                            saveStatusMessage = null
                                        }
                                        .padding(4.dp)
                                )
                            } else {
                                Spacer(modifier = Modifier.width(4.dp))
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (pastedKey.isNotBlank()) PaletteCornflower else Color.LightGray,
                                modifier = Modifier.clickable(enabled = pastedKey.isNotBlank()) {
                                    val success = onSaveUniversalKey(pastedKey.trim())
                                    if (success) {
                                        val detected = com.example.core.ai.AiKeyDetector.detectProvider(pastedKey.trim())
                                        saveStatusMessage = "✅ Saved & Activated ${detected?.name ?: "Key"}!"
                                        isSuccess = true
                                        pastedKey = ""
                                    } else {
                                        saveStatusMessage = "❌ Invalid or unparseable API key format"
                                        isSuccess = false
                                    }
                                }
                            ) {
                                Text(
                                    text = "Save & Activate Key",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Color.White),
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                )
                            }
                        }

                        if (saveStatusMessage != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = saveStatusMessage!!,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp
                                ),
                                color = if (isSuccess) Color(0xFF2E7D32) else Color.Red
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "ACTIVE SAVED KEYS (${allKeys.size})",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp),
                    color = KamakuraTextSecondary
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Scrollable Key Inputs
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (allKeys.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF8FAFC),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier.padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No keys stored. Paste any AI key above to get started.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = KamakuraTextSecondary
                                )
                            }
                        }
                    } else {
                        allKeys.forEach { keyEntity ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, PaletteIceCyan.copy(alpha = 0.8f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = keyEntity.provider.uppercase(),
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = PaletteCornflower
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFFE8F5E9)
                                            ) {
                                                Text(
                                                    text = "ACTIVE",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF2E7D32)
                                                    ),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "Model: ${keyEntity.defaultModel} • Key: ••••••••••${keyEntity.apiKey.takeLast(4)}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace
                                            ),
                                            color = KamakuraTextSecondary
                                        )
                                    }

                                    IconButton(
                                        onClick = { onDeleteKey(keyEntity.provider) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = Color.Red.copy(alpha = 0.7f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Done Button
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = PaletteCornflower,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDismiss() }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Text(
                            text = "Done & Return to AI Chat",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
