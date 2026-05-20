package com.example.alphacinema

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

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
        // Để tránh hiển thị 2 lần thông báo (1 cái trống, 1 cái có chữ), ta gom chung logic lại:
        val title = message.notification?.title ?: message.data["title"] ?: "Alpha Cinema"
        val body = message.notification?.body ?: message.data["body"] ?: ""
        val type = message.data["type"] ?: "system"

        Log.d(TAG, "Notification title: $title, body: $body, type: $type")

        // Lưu vào DB (nếu app đang mở)
        saveNotificationToFirestore(title, body, type, message.data)
        
        // Hiển thị thông báo lên thanh trạng thái
        showNotification(title, body, type, message.data)
    }

    private fun saveNotificationToFirestore(title: String, body: String, type: String, extraData: Map<String, String>) {
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val notificationData = hashMapOf<String, Any>(
                "title" to title,
                "body" to body,
                "type" to type,
                "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                "isRead" to false
            )
            // Thêm các trường dữ liệu tùy chọn (như movieId, plan)
            extraData.forEach { (key, value) ->
                if (key != "title" && key != "body" && key != "type") {
                    notificationData[key] = value
                }
            }
            db.collection("users").document(currentUser.uid)
                .collection("notifications").add(notificationData)
        }
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

        val notification = NotificationCompat.Builder(this, com.example.alphacinema.notification.NotificationHelper.CHANNEL_PUSH)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // TODO: Update to app logo
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
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
