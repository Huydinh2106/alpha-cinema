package com.example.alphacinema

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
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
        enableEdgeToEdge()

        super.onCreate(savedInstanceState)

        // Xin quyền thông báo cho Android 13+
        requestNotificationPermission()

        // Lấy FCM token và in ra Logcat
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            Log.d("FCM", "===== FCM DEVICE TOKEN =====")
            Log.d("FCM", token)
            Log.d("FCM", "============================")
        }

        // Gắn exit animation listener để dismiss hệ thống splash TỨC THÌ
        // Không có animation exit → không thấy nó tồn tại
        splashScreen.setOnExitAnimationListener { splashScreenViewProvider ->
            splashScreenViewProvider.remove() // Xoá ngay, không animation
        }

        setContent {
            AlphaCinemaTheme {
                AppScreen(modifier = Modifier.fillMaxSize())
            }
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
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AlphaCinemaTheme {
        AppScreen()
    }
}