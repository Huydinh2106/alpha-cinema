package com.example.alphacinema.service

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.alphacinema.MainActivity
import com.example.alphacinema.R
import com.example.alphacinema.notification.NotificationHelper

class MediaPlaybackService : Service() {

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIF_ID, buildMediaNotification())
    }

    private fun buildMediaNotification(): android.app.Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, NotificationHelper.CHANNEL_MEDIA)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Đang phát")
            .setContentText("Tên phim đang chiếu...")
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                android.R.drawable.ic_media_pause,
                "Pause",
                getActionPendingIntent(ACTION_PAUSE)
            )
            .addAction(
                android.R.drawable.ic_media_next,
                "Tiếp",
                getActionPendingIntent(ACTION_NEXT)
            )
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle())
            .setOngoing(true)

        return builder.build()
    }

    private fun getActionPendingIntent(action: String): PendingIntent {
        val intent = Intent(this, MediaPlaybackService::class.java).apply { this.action = action }
        return PendingIntent.getService(this, action.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> { /* Handle Play */ }
            ACTION_PAUSE -> { /* Handle Pause */ }
            ACTION_NEXT -> { /* Handle Next */ }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        const val NOTIF_ID = 101
        const val ACTION_PLAY = "action_play"
        const val ACTION_PAUSE = "action_pause"
        const val ACTION_NEXT = "action_next"
    }
}
