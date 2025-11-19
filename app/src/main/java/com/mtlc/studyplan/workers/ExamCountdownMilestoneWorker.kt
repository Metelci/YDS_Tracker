package com.mtlc.studyplan.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mtlc.studyplan.data.ExamMilestone
import com.mtlc.studyplan.data.YdsExamService
import com.mtlc.studyplan.data.isMutedToday
import com.mtlc.studyplan.data.isQuietNow
import com.mtlc.studyplan.notifications.NotificationManager
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Worker that checks daily if the exam countdown has reached a milestone
 * (90, 60, 30, 14, 7, 1, or 0 days) and sends a notification if appropriate.
 *
 * Respects user preferences (quiet hours, mute settings) and prevents
 * duplicate notifications using SharedPreferences tracking.
 */
class ExamCountdownMilestoneWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val notificationManager: NotificationManager
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "exam_countdown_milestones"
        private const val PREFS_NAME = "exam_milestones"
        private const val KEY_PREFIX = "milestone_sent_"

        // Turkey time zone for exam day notifications
        private val TURKEY_ZONE = ZoneId.of("Europe/Istanbul")
        private const val EXAM_DAY_NOTIFICATION_HOUR = 7
    }

    override suspend fun doWork(): Result {
        try {
            // Respect quiet hours and mute-today settings
            if (isMutedToday(applicationContext) || isQuietNow(applicationContext)) {
                return Result.success()
            }

            // Get the next exam from YdsExamService
            val nextExam = YdsExamService.getNextExam() ?: return Result.success()

            // Calculate days remaining
            val today = LocalDate.now()
            val daysRemaining = ChronoUnit.DAYS.between(today, nextExam.examDate).toInt()

            // Check if this is a milestone day
            val milestone = ExamMilestone.fromDaysRemaining(
                days = daysRemaining,
                examName = nextExam.name,
                examDate = nextExam.examDate,
            ) ?: return Result.success()

            // For exam day (0 days), only send notification at 7 AM Turkey time
            if (daysRemaining == 0) {
                val turkeyTime = LocalTime.now(TURKEY_ZONE)
                if (turkeyTime.hour < EXAM_DAY_NOTIFICATION_HOUR) {
                    // Too early for exam day notification, will retry later
                    return Result.success()
                }
            }

            // Check if we've already sent this milestone notification
            if (shouldSendMilestone(nextExam.examDate.toString(), milestone.daysUntil)) {
                // Send the milestone notification
                notificationManager.showExamMilestoneNotification(milestone)

                // Mark this milestone as sent
                markMilestoneSent(nextExam.examDate.toString(), milestone.daysUntil)
            }

            return Result.success()
        } catch (e: Exception) {
            // Log the error and retry (non-critical operation)
            return Result.retry()
        }
    }

    /**
     * Check if a milestone notification should be sent
     *
     * @param examId Unique identifier for the exam (using exam date)
     * @param milestone The milestone day (90, 60, 30, 14, 7, 1, 0)
     * @return true if the milestone has not been sent yet
     */
    private fun shouldSendMilestone(examId: String, milestone: Int): Boolean {
        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = "$KEY_PREFIX${examId}_$milestone"
        return !prefs.getBoolean(key, false)
    }

    /**
     * Mark a milestone as sent to prevent duplicate notifications
     *
     * @param examId Unique identifier for the exam (using exam date)
     * @param milestone The milestone day (90, 60, 30, 14, 7, 1, 0)
     */
    private fun markMilestoneSent(examId: String, milestone: Int) {
        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = "$KEY_PREFIX${examId}_$milestone"
        prefs.edit().putBoolean(key, true).apply()
    }
}
