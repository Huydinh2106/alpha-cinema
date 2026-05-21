package com.example.alphacinema

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.alphacinema.payment.MomoSdkCoordinator
import com.example.alphacinema.ui.app.AppScreen
import com.example.alphacinema.ui.theme.AlphaCinemaTheme
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : ComponentActivity() {

    // Launcher xin quyền thông báo (Android 13+)
    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Log.d("FCM", "Notification permission granted")
            } else {
                Log.w("FCM", "Notification permission denied")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        com.example.alphacinema.data.local.SettingsManager.init(this)
        val splashScreen = installSplashScreen()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )

        super.onCreate(savedInstanceState)

        // Xin quyền thông báo cho Android 13+
        requestNotificationPermission()

        // Khởi tạo kênh thông báo
        com.example.alphacinema.notification.NotificationHelper.createAllChannels(this)

        // Lấy FCM token và cập nhật vào Firestore
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                Log.d("FCM", "===== FCM DEVICE TOKEN: $token =====")
                updateTokenInFirestore(token)
            }
            .addOnFailureListener { e ->
                Log.e("FCM", "Failed to get FCM token", e)
            }

        // Gắn exit animation listener để dismiss hệ thống splash TỨC THÌ
        // Không có animation exit → không thấy nó tồn tại
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            splashScreen.setOnExitAnimationListener { splashScreenView ->
                splashScreenView.remove()
            }
        }

        // Bắt sự kiện khi click vào thông báo từ background
        handleFCMIntent(intent)

        // Kiểm tra xem có intent mở thông báo hay không
        val showNotification = intent.getBooleanExtra("open_notifications", false)
        Log.d("MainActivity", "onCreate open_notifications: $showNotification")

        setContent {
            AlphaCinemaTheme {
                AppScreen(
                    modifier = Modifier.fillMaxSize(),
                    initialShowNotification = showNotification
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        
        handleFCMIntent(intent)
        
        // Khi app đang mở, nếu nhận intent mới, Compose có thể không re-compose AppScreen ngay lập tức.
        // Tuy nhiên, vì người dùng yêu cầu, ta nên pass nó xuống, nhưng vì MainActivity dùng setContent,
        // cách tốt nhất là cập nhật một mutableState. Để nhanh, ta gọi lại setContent.
        val showNotification = intent.getBooleanExtra("open_notifications", false)
        Log.d("MainActivity", "onNewIntent open_notifications: $showNotification")
        
        setContent {
            AlphaCinemaTheme {
                val notificationType = intent.getStringExtra("notification_type")
                val movieId = intent.getStringExtra("movieId")
                val plan = intent.getStringExtra("plan")

                AppScreen(
                    modifier = Modifier.fillMaxSize(),
                    initialShowNotification = showNotification,
                    initialNotificationType = notificationType,
                    initialMovieId = movieId,
                    initialPlan = plan
                )
            }
        }
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (MomoSdkCoordinator.handleActivityResult(requestCode, resultCode, data)) {
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun handleFCMIntent(intent: Intent) {
        // FCM background clicks put the notification payload into intent extras.
        // CHỈ đánh dấu cần mở màn hình thông báo. KHÔNG lưu Firestore ở đây.
        // Cloud Functions (onNewMovieAdded, ...) đã lưu document có đầy đủ trường (kèm imageUrl).
        // Trước đây hàm này lưu thêm 1 document fallback với title "Thông báo từ Alpha Cinema"
        // và không có imageUrl → tạo ra "thông báo lỗi" thứ 2 trong list.
        val extras = intent.extras ?: return
        val sentTime = extras.getLong("google.sent_time", 0L)
        if (sentTime > 0L) {

            intent.putExtra("open_notifications", true)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun updateTokenInFirestore(token: String) {
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            db.collection("users").document(currentUser.uid)
                .update("fcmTokens", com.google.firebase.firestore.FieldValue.arrayUnion(token))
                .addOnSuccessListener { Log.d("FCM", "Token updated in Firestore") }
                .addOnFailureListener { e -> Log.w("FCM", "Error updating token", e) }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AlphaCinemaTheme {
        AppScreen()
    }
}
