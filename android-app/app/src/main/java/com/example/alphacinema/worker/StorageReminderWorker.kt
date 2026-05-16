package com.example.alphacinema.worker

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.alphacinema.R
import com.example.alphacinema.notification.NotificationHelper

class StorageReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        // Giả lập logic kiểm tra phim đã tải về lâu chưa xem
        val pendingMoviesCount = checkPendingMovies()
        
        if (pendingMoviesCount > 0) {
            val notification = NotificationCompat.Builder(applicationContext, NotificationHelper.CHANNEL_LOCAL)
                .setSmallIcon(android.R.drawable.ic_dialog_info) // Đảm bảo có icon này trong drawable
                .setContentTitle("Bạn có $pendingMoviesCount phim chưa xem")
                .setContentText("Mở Alpha Cinema để xem ngay những bộ phim đã tải về!")
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()

            with(NotificationManagerCompat.from(applicationContext)) {
                notify(NOTIF_ID, notification)
            }
        }
        return Result.success()
    }

    private fun checkPendingMovies(): Int {
        // TODO: Kết nối với database local (Room) để kiểm tra
        return 3 
    }

    companion object {
        const val NOTIF_ID = 202
    }
}
