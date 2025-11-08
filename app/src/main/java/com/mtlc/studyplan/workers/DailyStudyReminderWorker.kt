package com.mtlc.studyplan.workers

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import com.mtlc.studyplan.data.isMutedToday
import com.mtlc.studyplan.data.isQuietNow
import com.mtlc.studyplan.integration.AppIntegrationManager
import com.mtlc.studyplan.integration.StudyStats
import com.mtlc.studyplan.notifications.NotificationManager
import com.mtlc.studyplan.power.PowerStateChecker
import com.mtlc.studyplan.services.recordNotificationDeliveryAttempt
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * Daily Study Reminder Worker - Ensures 100% reliable delivery of personalized study reminders
 * at 6:00 PM local time with motivational content and comprehensive tracking.
 */
class DailyStudyReminderWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val notificationManager: NotificationManager,
    private val appIntegrationManager: AppIntegrationManager
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "daily_study_reminder"
        private const val REMINDER_HOUR = 18 // 6:00 PM
        private const val REMINDER_MINUTE = 0

        /**
         * Schedule daily study reminders at 6:00 PM local time
         */
        fun schedule(context: Context) {
            val workManager = WorkManager.getInstance(context)

            // Calculate initial delay to next 6:00 PM
            val now = ZonedDateTime.now(ZoneId.systemDefault())
            val nextReminder = now.withHour(REMINDER_HOUR).withMinute(REMINDER_MINUTE).withSecond(0)

            // If it's already past 6:00 PM today, schedule for tomorrow
            val initialDelay = if (now.isAfter(nextReminder)) {
                nextReminder.plusDays(1).toEpochSecond() - now.toEpochSecond()
            } else {
                nextReminder.toEpochSecond() - now.toEpochSecond()
            }

            val reminderRequest = PeriodicWorkRequestBuilder<DailyStudyReminderWorker>(
                repeatInterval = 1,
                repeatIntervalTimeUnit = TimeUnit.DAYS,
            )
                .setInitialDelay(initialDelay, TimeUnit.SECONDS)
                .setConstraints(
                    Constraints.Builder()
                        // Battery-aware: avoid firing when battery is low; let OS defer
                        .setRequiresBatteryNotLow(true)
                        .setRequiresDeviceIdle(false)
                        .setRequiresStorageNotLow(false)
                        .build(),
                )
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS,
                )
                .build()

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                reminderRequest,
            )
        }

        /**
         * Cancel daily study reminders
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }

    override suspend fun doWork(): Result {
        return try {
            val notificationConfig = appIntegrationManager.getNotificationConfig()
            if (!notificationConfig.areNotificationsEnabled || !notificationConfig.allowStudyReminders) {
                return Result.success()
            }

            if (isMutedToday(applicationContext) || isQuietNow(applicationContext)) {
                trackDeliveryAttempt(success = false, reason = "muted_or_quiet_hours")
                return Result.success()
            }

            if (PowerStateChecker.isPowerConstrained(applicationContext)) {
                trackDeliveryAttempt(success = false, reason = "power_saver_or_doze")
                return Result.success()
            }

            val studyStats = appIntegrationManager.getStudyStats()
            val motivationalMessage = generateMotivationalMessage(studyStats)
            val notificationId = System.currentTimeMillis().toInt()

            val calendarIntent = if (notificationConfig.calendarIntegrationEnabled) {
                createCalendarPendingIntent(motivationalMessage, notificationId)
            } else {
                null
            }

            notificationManager.showDailyStudyReminder(
                context = applicationContext,
                title = "?? Study Time Reminder",
                message = motivationalMessage,
                notificationId = notificationId,
                calendarIntent = calendarIntent,
            )

            trackDeliveryAttempt(success = true)
            Result.success()
        } catch (e: Exception) {
            trackDeliveryAttempt(success = false, reason = e.message ?: "unknown_error")
            Result.retry()
        }
    }

    private suspend fun trackDeliveryAttempt(success: Boolean, reason: String? = null) {
        applicationContext.recordNotificationDeliveryAttempt(success, reason)
    }

    private fun generateMotivationalMessage(studyStats: StudyStats): String {
        val currentStreak = studyStats.currentStreak
        val completedTasks = studyStats.totalTasksCompleted
        val weeklyGoal = studyStats.weeklyGoalHours

        return when {
            currentStreak >= 7 -> "?? Amazing! You're on a $currentStreak-day streak! Keep the momentum going!"
            currentStreak >= 3 -> "? Great job! $currentStreak days strong. Your dedication is paying off!"
            completedTasks > 10 -> "?? Impressive! You've completed $completedTasks tasks. You're unstoppable!"
            weeklyGoal > 0 -> "?? Time to crush your $weeklyGoal-hour weekly goal. You've got this!"
            else -> "?? Every great journey begins with a single step. Let's make today count!"
        }
    }

    private fun createCalendarPendingIntent(message: String, requestCode: Int): PendingIntent? {
        val beginTime = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1)
        val endTime = beginTime + TimeUnit.HOURS.toMillis(1)

        val intent = Intent(Intent.ACTION_INSERT)
            .setData(CalendarContract.Events.CONTENT_URI)
            .putExtra(CalendarContract.Events.TITLE, "StudyPlan Daily Reminder")
            .putExtra(CalendarContract.Events.DESCRIPTION, message)
            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginTime)
            .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTime)
            .putExtra(CalendarContract.Events.ALL_DAY, false)

        if (intent.resolveActivity(applicationContext.packageManager) == null) {
            return null
        }

        return PendingIntent.getActivity(
            applicationContext,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
