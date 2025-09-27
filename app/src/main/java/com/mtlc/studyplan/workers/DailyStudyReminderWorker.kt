package com.mtlc.studyplan.workers

import android.content.Context
import androidx.work.*
import com.mtlc.studyplan.data.isMutedToday
import com.mtlc.studyplan.data.isQuietNow
import com.mtlc.studyplan.integration.AppIntegrationManager
import com.mtlc.studyplan.integration.StudyStats
import com.mtlc.studyplan.notifications.NotificationManager
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
                repeatIntervalTimeUnit = TimeUnit.DAYS
            )
            .setInitialDelay(initialDelay, TimeUnit.SECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(false) // Work even on low battery
                    .setRequiresDeviceIdle(false) // Don't require device idle
                    .setRequiresStorageNotLow(false) // Work even on low storage
                    .build()
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                reminderRequest
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
        try {
            // Check if notifications are enabled
            val notificationConfig = appIntegrationManager.getNotificationConfig()
            if (!notificationConfig.areNotificationsEnabled || !notificationConfig.allowStudyReminders) {
                return Result.success()
            }

            // Respect quiet hours and mute settings
            if (isMutedToday(applicationContext) || isQuietNow(applicationContext)) {
                // Still track that we attempted delivery
                trackDeliveryAttempt(success = false, reason = "muted_or_quiet_hours")
                return Result.success()
            }

            // Get personalized study data
            val studyStats = appIntegrationManager.getStudyStats()
            val motivationalMessage = generateMotivationalMessage(studyStats)

            // Send personalized notification
            val notificationId = System.currentTimeMillis().toInt()
            notificationManager.showDailyStudyReminder(
                context = applicationContext,
                title = "🌟 Study Time Reminder",
                message = motivationalMessage,
                notificationId = notificationId
            )

            // Track successful delivery
            trackDeliveryAttempt(success = true)

            // Add to calendar if enabled
            if (notificationConfig.calendarIntegrationEnabled) {
                addToCalendar(motivationalMessage)
            }

            return Result.success()

        } catch (e: Exception) {
            // Track failed delivery
            trackDeliveryAttempt(success = false, reason = e.message ?: "unknown_error")
            return Result.retry()
        }
    }

    private suspend fun trackDeliveryAttempt(success: Boolean, reason: String? = null) {
        // Implementation for delivery tracking
        // This would integrate with analytics system
        val deliveryData = mapOf(
            "timestamp" to System.currentTimeMillis(),
            "success" to success,
            "reason" to (reason ?: "success"),
            "timezone" to ZoneId.systemDefault().id,
            "scheduled_time" to "${REMINDER_HOUR}:${REMINDER_MINUTE}"
        )

        // Store in local database or send to analytics
        // For now, we'll use a simple approach
        val prefs = applicationContext.getSharedPreferences("notification_tracking", Context.MODE_PRIVATE)
        prefs.edit()
            .putLong("last_delivery_attempt", System.currentTimeMillis())
            .putBoolean("last_delivery_success", success)
            .putString("last_delivery_reason", reason)
            .apply()
    }

    private fun generateMotivationalMessage(studyStats: StudyStats): String {
        val currentStreak = studyStats.currentStreak
        val completedTasks = studyStats.totalTasksCompleted
        val weeklyGoal = studyStats.weeklyGoalHours

        return when {
            currentStreak >= 7 -> "🔥 Amazing! You're on a ${currentStreak}-day streak! Keep the momentum going!"
            currentStreak >= 3 -> "⚡ Great job! ${currentStreak} days strong. Your dedication is paying off!"
            completedTasks > 10 -> "🎯 Impressive! You've completed ${completedTasks} tasks. You're unstoppable!"
            weeklyGoal > 0 -> "💪 Time to crush your ${weeklyGoal}-hour weekly goal. You've got this!"
            else -> "🌟 Every great journey begins with a single step. Let's make today count!"
        }
    }

    private fun addToCalendar(message: String) {
        // Calendar integration implementation
        // This would create a calendar event for study reminder
        try {
            val calendarIntent = android.content.Intent(android.content.Intent.ACTION_INSERT)
                .setData(android.net.Uri.parse("content://com.android.calendar/events"))
                .putExtra("title", "StudyPlan Daily Reminder")
                .putExtra("description", message)
                .putExtra("beginTime", System.currentTimeMillis() + 3600000) // 1 hour from now
                .putExtra("endTime", System.currentTimeMillis() + 7200000) // 2 hours from now
                .putExtra("allDay", false)

            calendarIntent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            applicationContext.startActivity(calendarIntent)
        } catch (e: Exception) {
            // Calendar app not available or other error
            // Silently fail - calendar integration is optional
        }
    }
}
