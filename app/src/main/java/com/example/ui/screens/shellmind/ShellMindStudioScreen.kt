package com.example.ui.screens.shellmind

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.shellmind.CodingChatMessage
import com.example.core.shellmind.ShellMindAgentEngine
import com.example.core.shellmind.ShellMindTerminalExecutor
import com.example.core.shellmind.ShellMindWorkspaceManager
import com.example.core.shellmind.TerminalLogEntry
import com.example.core.shellmind.WorkspaceFileItem
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShellMindStudioScreen(
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    val agentEngine = remember { ShellMindAgentEngine.getInstance(context) }
    val workspaceManager = remember { ShellMindWorkspaceManager.getInstance(context) }
    val terminalExecutor = remember { ShellMindTerminalExecutor.getInstance(context) }

    val isAgentBusy by agentEngine.isAgentBusy.collectAsStateWithLifecycle()
    val chatMessages by agentEngine.chatMessages.collectAsStateWithLifecycle()
    val currentThought by agentEngine.currentThought.collectAsStateWithLifecycle()

    val terminalLogs by terminalExecutor.terminalOutput.collectAsStateWithLifecycle()
    val isTerminalBusy by terminalExecutor.isExecuting.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: AI Chat, 1: Code Editor, 2: Terminal Console
    var currentEditingFile by remember { mutableStateOf<String?>("main.py") }
    var fileContent by remember { mutableStateOf("# Welcome to ShellMind On-Device IDE\n\ndef main():\n    print('Hello from Secondary Brain & Termux!')\n\nif __name__ == '__main__':\n    main()\n") }
    var userPromptInput by remember { mutableStateOf("") }
    var terminalInput by remember { mutableStateOf("") }
    var showConfigDialog by remember { mutableStateOf(false) }

    var workspaceFiles by remember { mutableStateOf(workspaceManager.listFiles()) }

    LaunchedEffect(selectedTab) {
        if (selectedTab == 1) {
            workspaceFiles = workspaceManager.listFiles()
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = Color(0xFF12141A),
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(onClick = onNavigateBack, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = PaletteCornflower)
                            }
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "⚡ SHELLMIND IDE",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = PaletteCornflower.copy(alpha = 0.25f)
                                    ) {
                                        Text(
                                            text = "vibe-coder",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PaletteIceCyan,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "On-Device Coding Agent & Termux Bridge",
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = { showConfigDialog = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = PaletteCornflower)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 3-Tab Selector Pill (AI Chat | Editor | Terminal)
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color(0xFF1E222D),
                        contentColor = PaletteCornflower,
                        divider = {},
                        indicator = {},
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .height(38.dp)
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            modifier = Modifier.clip(RoundedCornerShape(10.dp))
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                                Text("AI Coder", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            modifier = Modifier.clip(RoundedCornerShape(10.dp))
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(14.dp))
                                Text("Editor", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            modifier = Modifier.clip(RoundedCornerShape(10.dp))
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(14.dp))
                                Text("Terminal", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFF0D0F14),
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (selectedTab) {
                0 -> ShellMindChatView(
                    agentEngine = agentEngine,
                    chatMessages = chatMessages,
                    isBusy = isAgentBusy,
                    currentThought = currentThought,
                    userPromptInput = userPromptInput,
                    onPromptChange = { userPromptInput = it },
                    onSendPrompt = {
                        val prompt = userPromptInput
                        userPromptInput = ""
                        coroutineScope.launch {
                            agentEngine.sendUserPrompt(prompt)
                        }
                    },
                    onRunInTerminal = { cmd ->
                        selectedTab = 2
                        coroutineScope.launch {
                            terminalExecutor.executeCommand(cmd)
                        }
                    }
                )
                1 -> ShellMindEditorView(
                    workspaceManager = workspaceManager,
                    files = workspaceFiles,
                    currentFileName = currentEditingFile,
                    fileContent = fileContent,
                    onContentChange = { fileContent = it },
                    onSelectFile = { item ->
                        currentEditingFile = item.relativePath
                        val read = workspaceManager.readFile(item.relativePath)
                        if (read != null) fileContent = read
                    },
                    onSaveFile = {
                        currentEditingFile?.let { name ->
                            val ok = workspaceManager.writeFile(name, fileContent)
                            Toast.makeText(context, if (ok) "Saved $name" else "Save failed", Toast.LENGTH_SHORT).show()
                            workspaceFiles = workspaceManager.listFiles()
                        }
                    },
                    onAskAiToEdit = {
                        selectedTab = 0
                        userPromptInput = "Please refactor or review the file '$currentEditingFile':\n```\n$fileContent\n```"
                    }
                )
                2 -> ShellMindTerminalView(
                    terminalExecutor = terminalExecutor,
                    logs = terminalLogs,
                    isExecuting = isTerminalBusy,
                    terminalInput = terminalInput,
                    onInputChange = { terminalInput = it },
                    onExecute = {
                        val cmd = terminalInput
                        terminalInput = ""
                        coroutineScope.launch {
                            terminalExecutor.executeCommand(cmd)
                        }
                    }
                )
            }
        }
    }

    // Config dialog
    if (showConfigDialog) {
        AlertDialog(
            onDismissRequest = { showConfigDialog = false },
            containerColor = Color(0xFF1E222D),
            title = { Text("⚡ ShellMind Engine Settings", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Selected Model:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    OutlinedTextField(
                        value = agentEngine.activeModel,
                        onValueChange = { agentEngine.activeModel = it },
                        textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("API Base URL:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    OutlinedTextField(
                        value = agentEngine.customBaseUrl,
                        onValueChange = { agentEngine.customBaseUrl = it },
                        textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("API Key:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    OutlinedTextField(
                        value = agentEngine.customApiKey,
                        onValueChange = { agentEngine.customApiKey = it },
                        textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showConfigDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PaletteCornflower)
                ) {
                    Text("SAVE & APPLY", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

// ─── TAB 1: AI VIBE CODER CHAT VIEW ──────────────────────────────────────────
@Composable
private fun ShellMindChatView(
    agentEngine: ShellMindAgentEngine,
    chatMessages: List<CodingChatMessage>,
    isBusy: Boolean,
    currentThought: String,
    userPromptInput: String,
    onPromptChange: (String) -> Unit,
    onSendPrompt: () -> Unit,
    onRunInTerminal: (String) -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        // Thought indicator
        AnimatedVisibility(visible = currentThought.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = PaletteCornflower.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, PaletteCornflower.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = PaletteCornflower)
                    Text(
                        text = currentThought,
                        color = PaletteIceCyan,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Messages list
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(chatMessages) { msg ->
                val isUser = msg.sender == "USER"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isUser) PaletteCornflower else if (msg.isToolOutput) Color(0xFF1E222D) else Color(0xFF181B22),
                        border = if (!isUser) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A2E3B)) else null,
                        modifier = Modifier.widthIn(max = 340.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (isUser) "YOU" else if (msg.isToolOutput) "TOOL EXECUTION" else "SHELLMIND",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isUser) Color.White.copy(alpha = 0.8f) else PaletteSoftSky
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = msg.content,
                                color = Color.White,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                fontFamily = if (msg.isToolOutput) FontFamily.Monospace else FontFamily.Default
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Quick prompts row
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            QuickChip(label = "🐍 Python Script", onClick = { onPromptChange("Create a python script in test.py that tests network speed.") })
            QuickChip(label = "🔍 Git Status", onClick = { onPromptChange("Check git status and explain uncommitted files.") })
            QuickChip(label = "🛠️ Fix Bugs", onClick = { onPromptChange("Inspect main.py, check for syntax errors, and fix them.") })
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Input row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1E222D),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E3444)),
                modifier = Modifier.weight(1f)
            ) {
                BasicTextField(
                    value = userPromptInput,
                    onValueChange = onPromptChange,
                    textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                    cursorBrush = SolidColor(PaletteCornflower),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                    decorationBox = { innerTextField ->
                        if (userPromptInput.isEmpty()) {
                            Text("Describe code or terminal task...", color = Color(0xFF64748B), fontSize = 13.sp)
                        }
                        innerTextField()
                    }
                )
            }

            IconButton(
                onClick = onSendPrompt,
                enabled = !isBusy && userPromptInput.isNotBlank(),
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (!isBusy && userPromptInput.isNotBlank()) PaletteCornflower else Color(0xFF2A2E3B))
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ─── TAB 2: WORKSPACE FILE EXPLORER & CODE EDITOR ────────────────────────────
@Composable
private fun ShellMindEditorView(
    workspaceManager: ShellMindWorkspaceManager,
    files: List<WorkspaceFileItem>,
    currentFileName: String?,
    fileContent: String,
    onContentChange: (String) -> Unit,
    onSelectFile: (WorkspaceFileItem) -> Unit,
    onSaveFile: () -> Unit,
    onAskAiToEdit: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        // Files horizontal pill scroll
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            files.forEach { file ->
                val isSelected = currentFileName == file.relativePath
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) PaletteCornflower else Color(0xFF1E222D),
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onSelectFile(file) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (file.isDirectory) Icons.Default.Folder else Icons.Default.Description,
                            contentDescription = null,
                            tint = if (isSelected) Color.White else PaletteSoftSky,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = file.name,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else Color(0xFFCBD5E1)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Editor Action Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📄 ${currentFileName ?: "Untitled"}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = PaletteIceCyan
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = onAskAiToEdit,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2E3B)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("🤖 Ask AI", fontSize = 11.sp, color = PaletteIceCyan)
                }

                Button(
                    onClick = onSaveFile,
                    colors = ButtonDefaults.buttonColors(containerColor = PaletteCornflower),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("💾 Save", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Code Editor Text Field
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF08090C),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF222634)),
            modifier = Modifier.fillMaxSize()
        ) {
            BasicTextField(
                value = fileContent,
                onValueChange = onContentChange,
                textStyle = TextStyle(
                    color = Color(0xFFE2E8F0),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 18.sp
                ),
                cursorBrush = SolidColor(PaletteCornflower),
                modifier = Modifier.fillMaxSize().padding(12.dp)
            )
        }
    }
}

// ─── TAB 3: LIVE TERMUX TERMINAL CONSOLE ───────────────────────────────────────
@Composable
private fun ShellMindTerminalView(
    terminalExecutor: ShellMindTerminalExecutor,
    logs: List<TerminalLogEntry>,
    isExecuting: Boolean,
    terminalInput: String,
    onInputChange: (String) -> Unit,
    onExecute: () -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050608))
            .padding(12.dp)
    ) {
        // Quick Terminal Commands
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            QuickChip(label = "ls -la", onClick = { onInputChange("ls -la") })
            QuickChip(label = "python3 main.py", onClick = { onInputChange("python3 main.py") })
            QuickChip(label = "git status", onClick = { onInputChange("git status") })
            QuickChip(label = "pip list", onClick = { onInputChange("pip list") })
            QuickChip(label = "clear", onClick = { terminalExecutor.clearLogs() })
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Terminal Log Console
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF0A0C10),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E222D)),
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(logs) { entry ->
                    val color = when (entry) {
                        is TerminalLogEntry.Command -> PaletteIceCyan
                        is TerminalLogEntry.Error -> Color(0xFFF43F5E)
                        is TerminalLogEntry.Success -> Color(0xFF10B981)
                        is TerminalLogEntry.Output -> Color(0xFFCBD5E1)
                    }
                    Text(
                        text = entry.text,
                        color = color,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Command Input Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF141720),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A2E3B)),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("❯", color = PaletteCornflower, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    BasicTextField(
                        value = terminalInput,
                        onValueChange = onInputChange,
                        textStyle = TextStyle(color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                        cursorBrush = SolidColor(PaletteCornflower),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Button(
                onClick = onExecute,
                enabled = !isExecuting && terminalInput.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PaletteCornflower),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier.height(40.dp)
            ) {
                if (isExecuting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Text("RUN", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun QuickChip(label: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF1E222D),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E3444)),
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFCBD5E1),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
