@file:Suppress("NestedBlockDepth")
package com.mtlc.studyplan.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mtlc.studyplan.notifications.NotificationManager
import com.mtlc.studyplan.repository.ExamRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Worker that checks for new exam announcements and sends notifications
 * Runs after ExamSyncWorker completes to notify users of new exams
 */
class ExamNotificationWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val examRepository: ExamRepository,
    private val notificationManager: NotificationManager,
) : CoroutineWorker(context, workerParams) {

    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yyyy")
    }

    override suspend fun doWork(): Result {
        return try {
            // Get exams that haven't been notified yet
            val unnotifiedExams = examRepository.getUnnotifiedExams()

            if (unnotifiedExams.isEmpty()) {
                return Result.success()
            }

            // Send notification for each new exam
            unnotifiedExams.forEach { exam ->
                val examDate = exam.getExamDate()
                val formattedDate = examDate.format(DATE_FORMATTER)

                // Format registration period if available
                val registrationPeriod = if (exam.registrationStartEpochDay != null && exam.registrationEndEpochDay != null) {
                    val regStart = exam.getRegistrationStart()
                    val regEnd = exam.getRegistrationEnd()
                    if (regStart != null && regEnd != null) {
                        "${regStart.format(DATE_FORMATTER)} - ${regEnd.format(DATE_FORMATTER)}"
                    } else {
                        null
                    }
                } else {
                    null
                }

                // Show notification
                notificationManager.showNewExamAnnouncementNotification(
                    examName = exam.examName,
                    examDate = formattedDate,
                    registrationPeriod = registrationPeriod,
                )

                // Mark exam as notified
                examRepository.markAsNotified(exam.id)
            }

            Result.success()
        } catch (e: Exception) {
            // Log error but don't retry - notifications are non-critical
            Result.success()
        }
    }
}
