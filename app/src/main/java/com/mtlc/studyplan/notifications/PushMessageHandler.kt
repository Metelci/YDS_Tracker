package com.mtlc.studyplan.notifications

import android.content.Context
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles processing of incoming push messages and displaying appropriate notifications
 */
@Singleton
class PushMessageHandler @Inject constructor(
    private val context: Context,
    private val notificationManager: NotificationManager,
    private val pushAnalyticsManager: PushAnalyticsManager
) {
    companion object {
        private const val TAG = "PushMessageHandler"
    }

    suspend fun handleIncomingMessage(message: PushMessage) {
        when (message.type) {
            PushMessageType.STUDY_REMINDER -> handleStudyReminder(message)
            PushMessageType.ACHIEVEMENT -> handleAchievementNotification(message)
            PushMessageType.EXAM_UPDATE -> handleExamUpdate(message)
            PushMessageType.MOTIVATIONAL -> handleMotivationalMessage(message)
            PushMessageType.SYSTEM -> handleSystemNotification(message)
            PushMessageType.CUSTOM -> handleCustomNotification(message)
        }
    }

    suspend fun handleStudyReminder(message: PushMessage) {
        val title = message.title ?: "Study Reminder"
        val body = message.body ?: "Time to continue your studies!"

        notificationManager.showDailyStudyReminder(
            context = context,
            title = title,
            message = body,
            notificationId = message.id?.hashCode() ?: System.currentTimeMillis().toInt()
        )

        pushAnalyticsManager.trackPushNotification("study_reminder", true)
    }

    suspend fun handleAchievementNotification(message: PushMessage) {
        val title = message.title ?: "Achievement Unlocked!"
        val body = message.body ?: "Congratulations on your achievement!"

        notificationManager.showAchievementNotification(
            title = title,
            message = body,
            notificationId = message.id?.hashCode() ?: System.currentTimeMillis().toInt()
        )

        pushAnalyticsManager.trackPushNotification("achievement", true)
    }

    suspend fun handleExamUpdate(message: PushMessage) {
        val title = message.title ?: "Exam Update"
        val body = message.body ?: "Important exam information available"

        notificationManager.showExamApplicationReminder(
            title = title,
            message = body,
            notificationId = message.id?.hashCode() ?: System.currentTimeMillis().toInt()
        )

        pushAnalyticsManager.trackPushNotification("exam_update", true)
    }

    suspend fun handleMotivationalMessage(message: PushMessage) {
        val title = message.title ?: "Stay Motivated!"
        val body = message.body ?: "Keep pushing forward!"

        notificationManager.showQuickStudyReminder(
            title = title,
            message = body,
            notificationId = message.id?.hashCode() ?: System.currentTimeMillis().toInt()
        )

        pushAnalyticsManager.trackPushNotification("motivational", true)
    }

    suspend fun handleSystemNotification(message: PushMessage) {
        val title = message.title ?: "System Notification"
        val body = message.body ?: "Important system message"

        notificationManager.showQuickStudyReminder(
            title = title,
            message = body,
            notificationId = message.id?.hashCode() ?: System.currentTimeMillis().toInt()
        )

        pushAnalyticsManager.trackPushNotification("system", true)
    }

    suspend fun handleCustomNotification(message: PushMessage) {
        val title = message.title ?: "Notification"
        val body = message.body ?: "You have a new message"

        notificationManager.showQuickStudyReminder(
            title = title,
            message = body,
            notificationId = message.id?.hashCode() ?: System.currentTimeMillis().toInt()
        )

        pushAnalyticsManager.trackPushNotification("custom", true)
    }
}
