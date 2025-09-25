package com.mtlc.studyplan.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.hilt.work.HiltWorker
import com.mtlc.studyplan.data.ExamCalendarDataSource
import com.mtlc.studyplan.notifications.NotificationManager
import com.mtlc.studyplan.data.isMutedToday
import com.mtlc.studyplan.data.isQuietNow
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate

@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val notificationManager: NotificationManager
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
                notificationManager.showExamApplicationReminder(
                    title = title,
                    message = message,
                    notificationId = exam.name.hashCode()
                )
            }
            wasSpecialNotificationSent = true
        }

        if (!wasSpecialNotificationSent) {
            notificationManager.showQuickStudyReminder()
        }

        return Result.success()
    }
}

fun scheduleDailyReminder(context: Context) {
    DailyStudyReminderWorker.schedule(context)
}
