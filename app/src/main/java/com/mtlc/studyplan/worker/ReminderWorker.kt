package com.mtlc.studyplan.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mtlc.studyplan.notification.NotificationHelper
import java.util.Calendar

class ReminderWorker private constructor(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        // Not: Gerçek bir uygulamada, bugünün görevlerinin tamamlanıp
        // tamamlanmadığını DataStore veya veritabanından kontrol edersiniz.
        // Şimdilik, her zaman bildirim gönderen basit bir mantık kuruyoruz.

        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        // Hafta sonu bildirim gönderme
        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
            return Result.success()
        }

        // Bildirimi göster
        NotificationHelper.showReminderNotification(context)

        return Result.success()
    }
}
