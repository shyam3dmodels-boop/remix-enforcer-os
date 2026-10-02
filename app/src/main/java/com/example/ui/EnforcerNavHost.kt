package com.example.ui

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.DatabaseStatusFab
import com.example.ui.screens.ai.ChatAiScreen
import com.example.ui.screens.alarm.AlarmScreen
import com.example.ui.screens.coaching.CoachingScreen
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.dashboard.ManagerDashboardScreen
import com.example.ui.screens.discipline.AppLimitScreen
import com.example.ui.screens.recorder.LectureRecorderScreen
import com.example.ui.screens.recorder.TelegramUploadsScreen
import com.example.ui.screens.recorder.VoiceTaskScreen
import com.example.ui.theme.KamakuraSkyBackground
import com.example.ui.theme.PaletteCornflower
import com.example.ui.theme.PaletteIceCyan
import com.example.ui.theme.PaletteMintFrost
import com.example.ui.theme.PaletteSoftSky
import kotlinx.coroutines.launch

sealed class Screen(val route: String, val title: String, val subtitle: String, val icon: ImageVector) {
    data object Dashboard : Screen("dashboard", "Hub Manager", "Personal Telemetry", Icons.Default.Dashboard)
    data object ChatAi : Screen("chat_ai", "Second Brain AI", "Context-Grounded Chat", Icons.Default.Psychology)
    data object Tasks : Screen("tasks", "Voice Tasks", "Zero-Retention Action Items", Icons.Default.Assignment)
    data object Alarm : Screen("alarm", "Smart Alarms", "Relentless Challenge", Icons.Default.Alarm)
    data object Recorder : Screen("recorder", "Audio Recorder", "Ambient Notes & AAC", Icons.Default.Mic)
    data object Discipline : Screen("discipline", "Digital Limits", "Habits & Screen Time", Icons.Default.Block)
    data object Coaching : Screen("coaching", "Focus Routines", "Daily Schedule & Silent Shields", Icons.Default.School)
    data object Uploads : Screen("uploads", "Telegram Cloud", "C2 Channel & Media", Icons.Default.CloudUpload)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnforcerNavHost(
    viewModel: EnforcerViewModel,
    hasCameraPermission: Boolean = true,
    onRequestCameraPermission: () -> Unit = {}
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Dashboard.route

    val batteryStatus by viewModel.batteryStatus.collectAsState()
    val networkType by viewModel.networkType.collectAsState()
    val databaseStatus by viewModel.databaseStatus.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    val screens = listOf(
        Screen.Dashboard,
        Screen.ChatAi,
        Screen.Tasks,
        Screen.Alarm,
        Screen.Recorder,
        Screen.Discipline,
        Screen.Coaching,
        Screen.Uploads
    )

    val currentScreen = screens.find { it.route == currentRoute } ?: Screen.Dashboard

    KamakuraSkyBackground(modifier = Modifier.fillMaxSize()) {
        ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color.White.copy(alpha = 0.98f),
                drawerTonalElevation = 8.dp,
                modifier = Modifier
                    .width(320.dp)
                    .testTag("hamburger_nav_drawer")
            ) {
                // Drawer Header with 4-Color Gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(PaletteCornflower, PaletteSoftSky, PaletteIceCyan)
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("🌤️", fontSize = 24.sp)
                                }
                            }
                            Column {
                                Text(
                                    text = "SECONDARY BRAIN",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.sp
                                    ),
                                    color = Color.White
                                )
                                Text(
                                    text = "v2.0 Personal Telemetry Hub",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PaletteMintFrost
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Device Chip in Drawer
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = viewModel.deviceInfoProvider.getOrGenerateDeviceUuid(),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = PaletteCornflower
                                )
                                Text(
                                    text = "Zero-Retention",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PaletteCornflower
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Drawer Navigation Destinations
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    screens.forEach { screen ->
                        val selected = currentRoute == screen.route
                        NavigationDrawerItem(
                            icon = {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = screen.title,
                                    tint = if (selected) PaletteCornflower else Color(0xFF6B7280)
                                )
                            },
                            label = {
                                Column {
                                    Text(
                                        text = screen.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (selected) PaletteCornflower else Color(0xFF1F2937)
                                    )
                                    Text(
                                        text = screen.subtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (selected) PaletteCornflower.copy(alpha = 0.8f) else Color(0xFF9CA3AF)
                                    )
                                }
                            },
                            selected = selected,
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = PaletteMintFrost.copy(alpha = 0.50f),
                                unselectedContainerColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("drawer_item_${screen.route}")
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                // Glassmorphic Hamburger TopBar
                Surface(
                    color = Color.White.copy(alpha = 0.90f),
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, PaletteSoftSky.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Hamburger Button
                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                    }
                                },
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(PaletteIceCyan.copy(alpha = 0.6f))
                                    .testTag("hamburger_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Open Navigation Menu",
                                    tint = PaletteCornflower,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = currentScreen.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = PaletteCornflower
                                )
                                Text(
                                    text = "Secondary Brain 2.0",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PaletteCornflower.copy(alpha = 0.75f)
                                )
                            }
                        }

                        // Status indicators (Battery & 5G)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = PaletteMintFrost.copy(alpha = 0.60f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (batteryStatus.isCharging) Icons.Default.BatteryChargingFull else if (batteryStatus.percent > 20) Icons.Default.BatteryFull else Icons.Default.BatteryAlert,
                                        contentDescription = if (batteryStatus.isCharging) "Battery Charging" else "Battery",
                                        tint = PaletteCornflower,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "${batteryStatus.percent}%",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = PaletteCornflower
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = PaletteIceCyan.copy(alpha = 0.70f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SignalCellularAlt,
                                        contentDescription = "Network",
                                        tint = PaletteCornflower,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = networkType,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = PaletteCornflower
                                    )
                                }
                            }
                        }
                    }
                }
            },
            floatingActionButton = {
                DatabaseStatusFab(
                    status = databaseStatus,
                    onPing = { viewModel.pingDatabase() }
                )
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Dashboard.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Dashboard.route) {
                    DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToChatAi = {
                            navController.navigate(Screen.ChatAi.route)
                        }
                    )
                }
                composable(Screen.ChatAi.route) {
                    ChatAiScreen(
                        viewModel = viewModel,
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }
                composable(Screen.Tasks.route) {
                    VoiceTaskScreen(viewModel = viewModel)
                }
                composable(Screen.Alarm.route) {
                    AlarmScreen(viewModel = viewModel)
                }
                composable(Screen.Recorder.route) {
                    LectureRecorderScreen(
                        viewModel = viewModel,
                        onNavigateToUploads = {
                            navController.navigate(Screen.Uploads.route)
                        }
                    )
                }
                composable(Screen.Discipline.route) {
                    AppLimitScreen(viewModel = viewModel)
                }
                composable(Screen.Coaching.route) {
                    CoachingScreen(
                        viewModel = viewModel,
                        onNavigateToRecorder = {
                            navController.navigate(Screen.Recorder.route)
                        }
                    )
                }
                composable(Screen.Uploads.route) {
                    TelegramUploadsScreen(
                        viewModel = viewModel,
                        hasCameraPermission = hasCameraPermission,
                        onRequestCameraPermission = onRequestCameraPermission,
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
    }
}
}
