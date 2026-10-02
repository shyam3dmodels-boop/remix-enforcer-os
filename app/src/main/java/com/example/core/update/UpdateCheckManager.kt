package com.example.core.update

import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainApplication
import com.example.core.ai.AiKeySyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * UpdateCheckManager — Handles OTA auto-updates for Remix Enforcer OS.
 *
 * On every app launch, checks the backend /api/update/check endpoint.
 * If a newer version_code is found, shows a system notification + in-app
 * dialog. Tapping "Update Now" downloads the APK via DownloadManager and
 * triggers the system installer automatically.
 *
 * This means when the admin publishes a new APK to the backend, ALL users
 * will be notified and updated on their next app launch.
 */
class UpdateCheckManager private constructor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val prefs = context.getSharedPreferences("enforcer_update_prefs", Context.MODE_PRIVATE)

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "UpdateCheckManager"
        private const val NOTIF_CHANNEL_ID = "enforcer_updates"
        private const val NOTIF_ID = 9001
        private const val PREF_LAST_DISMISSED_VERSION = "last_dismissed_version_code"

        @Volatile
        private var INSTANCE: UpdateCheckManager? = null

        fun getInstance(context: Context): UpdateCheckManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UpdateCheckManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    /** Call this on app startup to check for updates in the background. */
    fun checkForUpdateAsync(onUpdateAvailable: ((versionName: String, apkUrl: String, notes: String) -> Unit)? = null) {
        scope.launch {
            try {
                val serverUrl = AiKeySyncManager.DEFAULT_SERVER_URL
                val request = Request.Builder()
                    .url("$serverUrl/api/update/check")
                    .get()
                    .build()

                val response = okHttpClient.newCall(request).execute()
                val body = response.body?.string() ?: return@launch
                response.close()

                if (!response.isSuccessful) {
                    Log.w(TAG, "Update check failed: HTTP ${response.code}")
                    return@launch
                }

                val json = JSONObject(body)
                if (!json.optBoolean("update_available", false)) {
                    Log.i(TAG, "No update available.")
                    return@launch
                }

                val data = json.optJSONObject("data") ?: return@launch
                val remoteVersionCode = data.optInt("version_code", 0)
                val versionName = data.optString("version_name", "")
                val apkUrl = data.optString("apk_url", "")
                val releaseNotes = data.optString("release_notes", "")

                if (apkUrl.isBlank()) {
                    Log.w(TAG, "Update available but APK URL is missing.")
                    return@launch
                }

                // Get current installed version code
                val currentVersionCode = try {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionCode
                } catch (e: Exception) { 0 }

                // Check if user already dismissed this version
                val lastDismissed = prefs.getInt(PREF_LAST_DISMISSED_VERSION, 0)

                if (remoteVersionCode > currentVersionCode && remoteVersionCode != lastDismissed) {
                    Log.i(TAG, "Update available: $versionName (code $remoteVersionCode)")
                    showUpdateNotification(versionName, apkUrl, releaseNotes)
                    onUpdateAvailable?.invoke(versionName, apkUrl, releaseNotes)
                } else {
                    Log.i(TAG, "App is up to date (current: $currentVersionCode, remote: $remoteVersionCode).")
                }

            } catch (e: Exception) {
                Log.w(TAG, "Update check exception: ${e.message}")
            }
        }
    }

    /** Shows a system notification with an "Update Now" action. */
    private fun showUpdateNotification(versionName: String, apkUrl: String, notes: String) {
        createNotificationChannel()

        val downloadIntent = Intent(context, UpdateDownloadReceiver::class.java).apply {
            putExtra("apk_url", apkUrl)
            putExtra("version_name", versionName)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, NOTIF_ID, downloadIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NOTIF_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("🚀 Remix Enforcer OS — Update Available")
            .setContentText("Version $versionName is ready to install.")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Version $versionName is available.\n${notes.take(100)}\n\nTap UPDATE NOW to download and install.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(android.R.drawable.stat_sys_download, "Update Now", pendingIntent)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, notification)
    }

    /** Downloads and installs the APK using DownloadManager. */
    fun downloadAndInstallApk(apkUrl: String, versionName: String) {
        try {
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val uri = Uri.parse(apkUrl)
            val fileName = "RemixEnforcerOS_$versionName.apk"

            val request = DownloadManager.Request(uri)
                .setTitle("Remix Enforcer OS $versionName")
                .setDescription("Downloading update...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(false)

            val downloadId = dm.enqueue(request)
            Log.i(TAG, "APK download started. DownloadManager ID: $downloadId")

            // Register receiver to trigger installer when download completes
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (id == downloadId) {
                        try {
                            context.unregisterReceiver(this)
                        } catch (_: Exception) {}
                        installDownloadedApk(fileName)
                    }
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
            }

        } catch (e: Exception) {
            Log.e(TAG, "APK download failed: ${e.message}", e)
        }
    }

    private fun installDownloadedApk(fileName: String) {
        try {
            val file = java.io.File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                fileName
            )
            if (!file.exists()) {
                Log.e(TAG, "Downloaded APK not found: ${file.absolutePath}")
                return
            }

            val apkUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } else {
                Uri.fromFile(file)
            }

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            Log.e(TAG, "APK install failed: ${e.message}", e)
        }
    }

    fun dismissUpdate(versionCode: Int) {
        prefs.edit().putInt(PREF_LAST_DISMISSED_VERSION, versionCode).apply()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIF_CHANNEL_ID,
                "App Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Remix Enforcer OS OTA update notifications"
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    /** BroadcastReceiver that handles the "Update Now" notification tap. */
    class UpdateDownloadReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val apkUrl = intent.getStringExtra("apk_url") ?: return
            val versionName = intent.getStringExtra("version_name") ?: "latest"
            getInstance(context).downloadAndInstallApk(apkUrl, versionName)
        }
    }
}
