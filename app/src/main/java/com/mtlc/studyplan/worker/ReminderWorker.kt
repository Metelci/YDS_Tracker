package com.mtlc.studyplan.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mtlc.studyplan.ExamCalendarDataSource
import com.mtlc.studyplan.notification.NotificationHelper
import java.time.LocalDate

class ReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val today = LocalDate.now()

        // İYİLEŞTİRME 1: firstOrNull yerine filter kullanıldı.
        // Bu sayede, aynı güne denk gelen birden fazla olayı (örn: bir sınavın başlangıcı ve diğerinin sonu)
        // yakalayabiliriz.
        val examsWithEventsToday = ExamCalendarDataSource.upcomingExams.filter {
            it.applicationStart == today || it.applicationEnd == today
        }

        var wasSpecialNotificationSent = false
        if (examsWithEventsToday.isNotEmpty()) {
            // O gün etkinliği olan HER sınav için ayrı bir bildirim gönder
            examsWithEventsToday.forEach { exam ->
                val title = "Sınav Başvuru Hatırlatıcısı"
                val message = if (today == exam.applicationStart) {
                    "${exam.name} için başvurular bugün başladı!"
                } else {
                    "${exam.name} için bugün son başvuru günü!"
                }
                // Her bildirimin farklı olması için benzersiz bir ID oluşturuyoruz
                NotificationHelper.showApplicationReminderNotification(
                    applicationContext,
                    title,
                    message,
                    exam.name.hashCode() // Sınav isminden benzersiz ID üret
                )
            }
            wasSpecialNotificationSent = true
        }

        // Eğer bugün özel bir gün değilse, standart günlük çalışma hatırlatıcısını gönder
        if (!wasSpecialNotificationSent) {
            NotificationHelper.showStudyReminderNotification(applicationContext)
        }

        return Result.success()
    }
}
