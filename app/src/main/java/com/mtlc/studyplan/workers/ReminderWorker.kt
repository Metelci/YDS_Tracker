package com.mtlc.studyplan.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.mtlc.studyplan.data.ExamCalendarDataSource
import com.mtlc.studyplan.notifications.NotificationHelper
import com.mtlc.studyplan.data.isMutedToday
import com.mtlc.studyplan.data.isQuietNow
import java.time.LocalDate
import java.util.Calendar
import java.util.concurrent.TimeUnit

class ReminderWorker(
    context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        // Respect quiet hours and mute-today
        if (isMutedToday(applicationContext) || isQuietNow(applicationContext)) {
            return Result.success()
        }
        val today = LocalDate.now()

        val examsWithEventsToday = ExamCalendarDataSource.upcomingExams.filter {
            it.applicationStart == today || it.applicationEnd == today
        }

        var wasSpecialNotificationSent = false
        if (examsWithEventsToday.isNotEmpty()) {
            examsWithEventsToday.forEach { exam ->
                val title = "Sınav Başvuru Hatırlatıcısı"
                val message = if (today == exam.applicationStart) {
                    "${exam.name} için başvurular bugün başladı!"
                } else {
                    "${exam.name} için bugün son başvuru günü!"
                }
                NotificationHelper.showApplicationReminderNotification(
                    applicationContext,
                    title,
                    message,
                    exam.name.hashCode()
                )
            }
            wasSpecialNotificationSent = true
        }

        if (!wasSpecialNotificationSent) {
            NotificationHelper.showStudyReminderNotification(applicationContext)
        }

        return Result.success()
    }
}

fun scheduleDailyReminder(context: Context) {
    val hour = 17 // Sabit saat 17:00
    val minute = 0

    val now = Calendar.getInstance()
    val nextNotificationTime = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        if (before(now)) {
            add(Calendar.DAY_OF_MONTH, 1)
        }
    }
    val initialDelay = nextNotificationTime.timeInMillis - now.timeInMillis

    val reminderRequest = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
        .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
        .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork("YDS_DAILY_REMINDER", ExistingPeriodicWorkPolicy.REPLACE, reminderRequest)
}
