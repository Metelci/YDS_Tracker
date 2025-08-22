package com.mtlc.studyplan.notifications

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import com.mtlc.studyplan.MainActivity
import com.mtlc.studyplan.R

//region BİLDİRİM VE ARKA PLAN İŞLERİ
object NotificationHelper {
    private const val CHANNEL_ID_REMINDER = "YDS_REMINDER_CHANNEL"
    private const val NOTIFICATION_ID_REMINDER = 1

    private const val CHANNEL_ID_APPLICATION = "YDS_APPLICATION_CHANNEL"

    // GÜNLÜK HATIRLATICI BİLDİRİMİ İÇİN DÜZELTME
    fun showStudyReminderNotification(context: Context) {
        // 1. Adım: Uygulamayı açacak olan Intent'i oluştur.
        // Bu, MainActivity'yi hedef alır.
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        // 2. Adım: Intent'i bir PendingIntent'e sar.
        // Bu, bildirimin başka bir uygulamadan (sistem arayüzü) sizin uygulamanızı güvenli bir şekilde başlatmasını sağlar.
        val pendingIntent: android.app.PendingIntent = android.app.PendingIntent.getActivity(
            context,
            0,
            intent,
            android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_REMINDER)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Çalışma Zamanı!")
            .setContentText("Bugünkü hedeflerini tamamlamayı unutma.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent) // <-- 3. Adım: PendingIntent'i bildirime ekle.

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID_REMINDER, builder.build())
    }

    // SINAV BAŞVURU BİLDİRİMİ İÇİN DE AYNI DÜZELTME
    fun showApplicationReminderNotification(context: Context, title: String, message: String, notificationId: Int) {
        // Aynı şekilde bu bildirim için de bir PendingIntent oluşturuyoruz.
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: android.app.PendingIntent = android.app.PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_APPLICATION)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent) // <-- Aynı şekilde buraya da ekliyoruz.

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, builder.build())
    }
}
//endregion