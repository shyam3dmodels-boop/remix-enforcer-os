package com.example.core.recorder

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

class LectureRecorderService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        when (action) {
            ACTION_START -> {
                val title = intent?.getStringExtra(EXTRA_TITLE) ?: "Lecture"
                val subject = intent?.getStringExtra(EXTRA_SUBJECT) ?: "Class"
                val notification = buildNotification(title, subject, isPaused = false)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            }
            ACTION_STOP -> {
                LectureRecorderManager.instance.stopRecording(this)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_PAUSE -> {
                LectureRecorderManager.instance.pauseRecording(this)
            }
            ACTION_RESUME -> {
                LectureRecorderManager.instance.resumeRecording(this)
            }
        }
        return START_NOT_STICKY
    }

    private fun buildNotification(
        title: String,
        subject: String,
        isPaused: Boolean
    ): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val statusText = if (isPaused) "PAUSED // Tap to resume" else "RECORDING AUDIO // Tap to open"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("ENFORCER: $title [$subject]")
            .setContentText(statusText)
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "lecture_recorder_channel"
        private const val NOTIFICATION_ID = 2002
        private const val ACTION_START = "com.example.recorder.START"
        private const val ACTION_STOP = "com.example.recorder.STOP"
        private const val ACTION_PAUSE = "com.example.recorder.PAUSE"
        private const val ACTION_RESUME = "com.example.recorder.RESUME"
        private const val EXTRA_TITLE = "extra_title"
        private const val EXTRA_SUBJECT = "extra_subject"

        fun startService(context: Context, title: String, subject: String) {
            val intent = Intent(context, LectureRecorderService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_SUBJECT, subject)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun updateNotification(context: Context, isPaused: Boolean) {
            val intent = Intent(context, LectureRecorderService::class.java).apply {
                action = if (isPaused) ACTION_PAUSE else ACTION_RESUME
            }
            context.startService(intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, LectureRecorderService::class.java)
            context.stopService(intent)
        }
    }
}
