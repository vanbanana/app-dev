package com.example.sketchto3view.data.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.sketchto3view.MainActivity
import com.example.sketchto3view.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service that keeps generation tasks alive when the app goes to background.
 * Shows a persistent notification with progress: "正在生成三视图 (N/M)".
 * Holds a WakeLock to prevent the CPU from sleeping during image download.
 */
@AndroidEntryPoint
class GenerationForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "generation_service_channel"
        const val NOTIFICATION_ID = 1001
        const val WAKE_LOCK_TAG = "Sketch3View:GenerationWakeLock"
    }

    @Inject
    lateinit var generationServiceManager: GenerationServiceManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireWakeLock()
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            WAKE_LOCK_TAG
        ).apply {
            acquire(30 * 60 * 1000L) // 30 minutes max
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification(0, 0, 0)
        startForeground(NOTIFICATION_ID, notification)

        // Observe task status and update notification
        serviceScope.launch {
            generationServiceManager.serviceStatus.collectLatest { status ->
                if (status.activeCount == 0 && status.queuedCount == 0) {
                    stopSelf()
                } else {
                    val updatedNotification = buildNotification(
                        completed = status.completedCount,
                        total = status.totalCount,
                        queued = status.queuedCount
                    )
                    val notificationManager = getSystemService(NotificationManager::class.java)
                    notificationManager.notify(NOTIFICATION_ID, updatedNotification)
                }
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "三视图生成",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "显示三视图生成进度"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(completed: Int, total: Int, queued: Int): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = if (total > 0) {
            "正在生成三视图 ($completed/$total)"
        } else {
            "正在准备生成..."
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Sketch3View")
            .setContentText(contentText)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)

        if (total > 0) {
            builder.setProgress(total, completed, false)
        }

        if (queued > 0) {
            builder.setSubText("${queued}个排队中")
        }

        return builder.build()
    }
}
