package com.mtlc.studyplan.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mtlc.studyplan.data.isMutedToday
import com.mtlc.studyplan.data.isQuietNow
import com.mtlc.studyplan.notifications.NotificationManager
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.random.Random

/**
 * Worker that detects when a user hasn't studied for 3+ consecutive days
 * and sends a warm, encouraging reminder without judgment or pressure.
 */
class InactivityReminderWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val notificationManager: NotificationManager,
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "inactivity_reminder"
        const val INACTIVITY_THRESHOLD_DAYS = 3

        // Reminder message IDs
        private val REMINDER_IDS = intArrayOf(
            android.R.string.ok,  // Placeholder - we'll use string resources instead
        )
    }

    override suspend fun doWork(): Result {
        try {
            // Respect quiet hours and mute-today settings
            if (isMutedToday(applicationContext) || isQuietNow(applicationContext)) {
                return Result.success()
            }

            // Check if user has been inactive for 3+ days
            val daysSinceLastStudy = getLastStudyDateDaysAgo()

            if (daysSinceLastStudy >= INACTIVITY_THRESHOLD_DAYS) {
                // Send gentle comeback reminder
                val selectedMessage = selectRandomMessage()
                val title = applicationContext.getString(com.mtlc.studyplan.R.string.comeback_reminder_title)
                notificationManager.showGentleComebackReminder(
                    title = title,
                    message = selectedMessage,
                    notificationId = System.currentTimeMillis().toInt()
                )
            }

            return Result.success()

        } catch (e: Exception) {
            // Log but don't crash - reminder is non-critical
            return Result.retry()
        }
    }

    /**
     * Calculate days since last study date using DataStore or shared preferences
     * Returns Int.MAX_VALUE if no last study date is found
     */
    private suspend fun getLastStudyDateDaysAgo(): Int {
        return try {
            val prefs = applicationContext.getSharedPreferences(
                "study_tracking",
                Context.MODE_PRIVATE
            )
            val lastStudyDateMillis = prefs.getLong("last_study_date_millis", -1L)

            if (lastStudyDateMillis == -1L) {
                // No prior study recorded - don't send reminder yet
                return 0
            }

            val lastStudyDate = LocalDate.ofEpochDay(lastStudyDateMillis / 86400000)
            val today = LocalDate.now()
            ChronoUnit.DAYS.between(lastStudyDate, today).toInt()
        } catch (e: Exception) {
            0  // Safe default - don't send reminder if we can't determine last study date
        }
    }

    /**
     * Select a random warm reminder message from string resources
     */
    private fun selectRandomMessage(): String {
        val messages = listOf(
            com.mtlc.studyplan.R.string.comeback_reminder_1,
            com.mtlc.studyplan.R.string.comeback_reminder_2,
            com.mtlc.studyplan.R.string.comeback_reminder_3,
            com.mtlc.studyplan.R.string.comeback_reminder_4,
            com.mtlc.studyplan.R.string.comeback_reminder_5
        )

        val selectedResourceId = messages[Random.nextInt(messages.size)]
        return applicationContext.getString(selectedResourceId)
    }
}
