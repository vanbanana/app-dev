package com.example.sketchto3view.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.sketchto3view.MainActivity
import com.example.sketchto3view.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper for showing completion notifications when generation tasks finish.
 */
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val COMPLETION_CHANNEL_ID = "completion_channel"
        private const val COMPLETION_NOTIFICATION_ID_BASE = 2000
    }

    init {
        createCompletionChannel()
    }

    private fun createCompletionChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                COMPLETION_CHANNEL_ID,
                "生成完成通知",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "三视图生成完成时通知"
            }
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showCompletionNotification(taskId: String) {
        val pendingIntent = PendingIntent.getActivity(
            context,
            taskId.hashCode(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("navigate_to_task", taskId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, COMPLETION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("生成完成!")
            .setContentText("三视图已生成，点击查看")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.notify(
            COMPLETION_NOTIFICATION_ID_BASE + taskId.hashCode(),
            notification
        )
    }
}
