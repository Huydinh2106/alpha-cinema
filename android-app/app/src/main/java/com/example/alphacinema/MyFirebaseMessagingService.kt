package com.example.alphacinema

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory

import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.net.HttpURLConnection
import java.net.URL


class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCMService"
        private const val CHANNEL_ID = "alpha_cinema_notifications"
        private const val CHANNEL_NAME = "Alpha Cinema"
    }

    /**
     * Called when a new FCM token is generated.
     * This is where you would send the token to your backend server.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")
        // TODO: Send token to your backend server
        sendTokenToServer(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "Message received from: ${message.from}")

        // FCM gửi tin nhắn có thể chứa 'notification' payload, 'data' payload, hoặc CẢ HAI.

        val title = message.notification?.title ?: message.data["title"] ?: "Alpha Cinema"
        val body = message.notification?.body ?: message.data["body"] ?: ""
        val type = message.data["type"] ?: "system"

        Log.d(TAG, "Notification title: $title, body: $body, type: $type")

        // QUAN TRỌNG: KHÔNG lưu thông báo vào Firestore ở đây.
        // Cloud Functions (onNewMovieAdded, checkExpiringSubscriptions, ...) đã lưu document
        // vào users/{uid}/notifications với đầy đủ trường (bao gồm imageUrl/poster).
        // Nếu lưu lại ở client sẽ tạo document trùng lặp KHÔNG có imageUrl
        // → trông giống 1 "thông báo lỗi" thiếu poster bên cạnh thông báo gốc.

        // Hiển thị thông báo lên thanh trạng thái (chỉ áp dụng khi app foreground).
        showNotification(title, body, type, message.data)
    }


    private fun showNotification(title: String, body: String, type: String, data: Map<String, String>) {
        Log.d(TAG, "Attempting to show notification: $title")
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                com.example.alphacinema.notification.NotificationHelper.CHANNEL_PUSH,
                "Push Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Thông báo từ Alpha Cinema"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Create an Intent for the activity you want to start
        val intent = android.content.Intent(this, MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_notifications", true)
            putExtra("notification_type", type)
            data.forEach { (key, value) -> putExtra(key, value) }
        }
        val pendingIntent: android.app.PendingIntent = android.app.PendingIntent.getActivity(
            this, 0, intent, android.app.PendingIntent.FLAG_IMMUTABLE
        )

        // Kiểm tra xem có phải thông báo billing không và sử dụng ảnh local tương ứng
        val plan = data["plan"]?.lowercase()
        var bitmap: Bitmap? = null

        if (type == "billing" && plan != null) {
            val resId = when (plan) {
                "basic" -> R.drawable.basic
                "couple" -> R.drawable.couple
                "premium" -> R.drawable.premium
                else -> null
            }
            if (resId != null) {
                bitmap = BitmapFactory.decodeResource(resources, resId)
            }
        }

        // Nếu không có ảnh local, thử tải từ imageUrl
        if (bitmap == null) {
            val imageUrl = data["imageUrl"]?.takeIf { it.isNotBlank() }
            bitmap = imageUrl?.let { loadBitmapFromUrl(it) }
        }

        val builder = NotificationCompat.Builder(this, com.example.alphacinema.notification.NotificationHelper.CHANNEL_PUSH)

            .setSmallIcon(R.drawable.ic_notification_alpha)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (bitmap != null) {
            builder
                .setLargeIcon(bitmap)
                .setStyle(
                    NotificationCompat.BigPictureStyle()
                        .bigPicture(bitmap)
                        .bigLargeIcon(null as Bitmap?) // Ẩn large icon khi mở rộng để tránh trùng
                )
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    /**
     * Tải ảnh đồng bộ trong FCM service.
     * FCM service được phép thực thi tối đa ~10s nên việc tải đồng bộ là an toàn.
     */
    private fun loadBitmapFromUrl(url: String): Bitmap? {
        return try {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                doInput = true
                connectTimeout = 5_000
                readTimeout = 5_000
                instanceFollowRedirects = true
                connect()
            }
            connection.inputStream.use { input ->
                BitmapFactory.decodeStream(input)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load notification image: $url", e)
            null
        }

    }

    private fun sendTokenToServer(token: String) {
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            db.collection("users").document(currentUser.uid)
                .update("fcmTokens", com.google.firebase.firestore.FieldValue.arrayUnion(token))
                .addOnSuccessListener { Log.d(TAG, "Token updated in Firestore") }
                .addOnFailureListener { e -> Log.w(TAG, "Error updating token", e) }
        }
    }
}
