package com.example.alphacinema.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.alphacinema.MainActivity
import com.example.alphacinema.R

object NotificationHelper {
    const val CHANNEL_MEDIA = "media_playback"
    const val CHANNEL_LOCAL = "local_reminder"
    const val CHANNEL_PUSH = "push_notifications"

    fun createAllChannels(context: Context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            // Media playback channel (low importance, no sound)
            val mediaChannel = NotificationChannel(
                CHANNEL_MEDIA,
                "Media Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Điều khiển phim/nhạc đang phát"
                setShowBadge(false)
            }
            // Local reminder channel (default importance)
            val localChannel = NotificationChannel(
                CHANNEL_LOCAL,
                "Nhắc nhở nội bộ",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Nhắc nhở tải phim, dọn dẹp bộ nhớ"
                enableLights(true)
                lightColor = Color.BLUE
            }
            // Push notifications channel (high importance, sound & vibration)
            val pushChannel = NotificationChannel(
                CHANNEL_PUSH,
                "Thông báo đẩy",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Cảnh báo bảo mật, giao dịch, khuyến mãi"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 250, 250)
            }
            manager.createNotificationChannels(listOf(mediaChannel, localChannel, pushChannel))
        }
    }

    /** Helper to build a generic push notification */
    fun showPushNotification(
        context: Context,
        title: String,
        body: String,
        channelId: String = CHANNEL_PUSH,
        deepLinkIntent: Intent? = null,
        actionButtons: List<Pair<Int, Intent>> = emptyList()
    ) {
        val pendingIntent = deepLinkIntent?.let {
            PendingIntent.getActivity(
                context,
                0,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } ?: PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification_alpha)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        // Add any action buttons if provided
        actionButtons.forEachIndexed { index, (iconRes, intent) ->
            val pi = PendingIntent.getBroadcast(
                context,
                index,
                intent,
                PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(iconRes, "Action", pi)
        }

        with(NotificationManagerCompat.from(context)) {
            notify((System.currentTimeMillis() % 10000).toInt(), builder.build())
        }
    }
}
