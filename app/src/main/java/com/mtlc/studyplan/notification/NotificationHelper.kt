package com.mtlc.studyplan.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.mtlc.studyplan.R // Projenizin R dosyasını import edin

object NotificationHelper {

    private const val CHANNEL_ID = "YDS_REMINDER_CHANNEL"
    private const val CHANNEL_NAME = "YDS/YÖKDİL Hatırlatıcıları"
    private const val NOTIFICATION_ID = 1

    fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Günlük çalışma planı hatırlatıcıları."
        }
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    fun showReminderNotification(context: Context) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Bildirim ikonu
            .setContentTitle("Çalışma Zamanı!")
            .setContentText("Bugünkü hedeflerini tamamlamayı unutma.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }
}