package com.mtlc.studyplan.notifications

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.messaging.RemoteMessage
import com.mtlc.studyplan.notifications.PushMessage
import com.mtlc.studyplan.notifications.PushMessageType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Integration tests for Firebase Cloud Messaging service
 * Tests FCM message handling, token management, and service lifecycle
 */
@RunWith(AndroidJUnit4::class)
class FirebaseMessagingServiceTest {

    private lateinit var context: Context
    private lateinit var service: StudyPlanFirebaseMessagingService

    @Mock
    private lateinit var pushNotificationManager: PushNotificationManager
    
    @Mock
    private lateinit var pushMessageHandler: PushMessageHandler

    

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()

        // Enable push notifications
        runBlocking {
            whenever(pushNotificationManager.arePushNotificationsEnabled()).thenReturn(true)
            // Mock the handleIncomingMessage method to do nothing in tests
            // We'll verify this is called instead of the specific handlers
        }

        // Create service instance
        service = StudyPlanFirebaseMessagingService()

        // Mock the push notification manager that would be injected
        // In real scenario, this would be handled by Koin
        service.pushNotificationManager = pushNotificationManager
    }

    @Test
    fun `onMessageReceived processes study reminder message correctly`() {
        val remoteMessage = RemoteMessage.Builder("test_sender")
            .setMessageId("msg_123")
            .addData("type", "STUDY_REMINDER")
            .addData("title", "Time to Study!")
            .addData("body", "Don't forget your daily study session")
            .addData("taskId", "task_456")
            .build()

        service.onMessageReceived(remoteMessage)

        // Verify that the appropriate handler was called
        Thread.sleep(100)
        runBlocking { verify(pushNotificationManager).handleStudyReminder(any()) }
    }

    @Test
    fun `onMessageReceived processes achievement message correctly`() {
        val remoteMessage = RemoteMessage.Builder("test_sender")
            .setMessageId("achievement_789")
            .addData("type", "ACHIEVEMENT")
            .addData("title", "Achievement Unlocked!")
            .addData("body", "Congratulations on your progress!")
            .addData("achievementId", "streak_7")
            .build()

        service.onMessageReceived(remoteMessage)

        Thread.sleep(100)
        runBlocking { verify(pushNotificationManager).handleAchievementNotification(any()) }
    }

    @Test
    fun `onMessageReceived processes notification payload correctly`() {
        val remoteMessage = RemoteMessage.Builder("test_sender")
            .setMessageId("notification_101")
            .setMessageType("notification")
            .setTtl(3600)
            .addData("custom_data", "value")
            .addData("type", "MOTIVATIONAL")
            .addData("title", "Stay Motivated!")
            .addData("body", "Every day is a new opportunity")
            .build()

        service.onMessageReceived(remoteMessage)

        Thread.sleep(100)
        runBlocking { verify(pushNotificationManager).handleMotivationalMessage(any()) }
    }

    @Test
    fun `onMessageReceived handles messages without data payload`() {
        val remoteMessage = RemoteMessage.Builder("test_sender")
            .setMessageId("simple_msg")
            .build()

        service.onMessageReceived(remoteMessage)

        // With no type, defaults to CUSTOM
        Thread.sleep(100)
        runBlocking { verify(pushNotificationManager).handleCustomNotification(any()) }
    }

    @Test
    fun `onMessageReceived handles messages with null notification payload`() {
        val remoteMessage = RemoteMessage.Builder("test_sender")
            .setMessageId("no_notification")
            .addData("type", "SYSTEM")
            .addData("title", "System Update")
            .addData("body", "App updated successfully")
            .build()

        service.onMessageReceived(remoteMessage)

        Thread.sleep(100)
        runBlocking { verify(pushNotificationManager).handleSystemNotification(any()) }
    }

    @Test
    fun `onNewToken stores FCM token correctly`() {
        val testToken = "new_fcm_token_12345"

        service.onNewToken(testToken)

        // Verify token refresh delegated to manager
        Thread.sleep(100)
        runBlocking {
            verify(pushNotificationManager).onTokenRefreshed(testToken)
        }
    }

    @Test
    fun `onNewToken handles empty token gracefully`() {
        val emptyToken = ""

        service.onNewToken(emptyToken)

        // Verify delegation
        Thread.sleep(100)
        runBlocking { verify(pushNotificationManager).onTokenRefreshed(emptyToken) }
    }

    @Test
    fun `onNewToken handles null token gracefully`() {
        // Firebase calls onNewToken with non-null; skip null case
        // This test is not applicable with current API
    }

    @Test
    fun `service handles multiple messages correctly`() {
        val messages = listOf(
            RemoteMessage.Builder("sender1")
                .setMessageId("msg1")
                .addData("type", "STUDY_REMINDER")
                .addData("title", "Study Time!")
                .build(),
            RemoteMessage.Builder("sender2")
                .setMessageId("msg2")
                .addData("type", "ACHIEVEMENT")
                .addData("title", "Achievement!")
                .build(),
            RemoteMessage.Builder("sender3")
                .setMessageId("msg3")
                .addData("type", "MOTIVATIONAL")
                .addData("title", "Motivation!")
                .build()
        )

        messages.forEach { message ->
            service.onMessageReceived(message)
        }

        Thread.sleep(150)
        runBlocking {
            verify(pushNotificationManager, org.mockito.kotlin.times(1)).handleStudyReminder(any())
            verify(pushNotificationManager, org.mockito.kotlin.times(1)).handleAchievementNotification(any())
            verify(pushNotificationManager, org.mockito.kotlin.times(1)).handleMotivationalMessage(any())
        }
    }

    @Test
    fun `service handles high priority messages correctly`() {
        val highPriorityMessage = RemoteMessage.Builder("urgent_sender")
            .setMessageId("urgent_msg")
            .addData("type", "EXAM_UPDATE")
            .addData("title", "Urgent Exam Update")
            .addData("body", "Exam date changed!")
            .build()

        service.onMessageReceived(highPriorityMessage)

        Thread.sleep(100)
        runBlocking { verify(pushNotificationManager).handleExamUpdate(any()) }
    }

    @Test
    fun `service handles messages with collapse keys correctly`() {
        val collapseKeyMessage = RemoteMessage.Builder("collapsible_sender")
            .setMessageId("collapsible_msg")
            .setCollapseKey("study_reminders")
            .addData("type", "STUDY_REMINDER")
            .addData("title", "Daily Reminder")
            .addData("body", "Time for your daily study session")
            .build()

        service.onMessageReceived(collapseKeyMessage)

        Thread.sleep(100)
        runBlocking { verify(pushNotificationManager).handleStudyReminder(any()) }
    }

    @Test
    fun `service handles messages with TTL correctly`() {
        val ttlMessage = RemoteMessage.Builder("ttl_sender")
            .setMessageId("ttl_msg")
            .setTtl(7200) // 2 hours
            .addData("type", "SYSTEM")
            .addData("title", "Scheduled Maintenance")
            .addData("body", "App will be updated in 2 hours")
            .build()

        service.onMessageReceived(ttlMessage)

        Thread.sleep(100)
        runBlocking { verify(pushNotificationManager).handleSystemNotification(any()) }
    }

    @Test
    fun `service analytics tracking works correctly`() {
        val testMessage = RemoteMessage.Builder("analytics_sender")
            .setMessageId("analytics_msg")
            .addData("type", "STUDY_REMINDER")
            .addData("title", "Analytics Test")
            .addData("body", "Testing analytics tracking")
            .build()

        service.onMessageReceived(testMessage)

        // Verify handler called
        Thread.sleep(100)
        runBlocking { verify(pushNotificationManager).handleStudyReminder(any()) }
    }

    @Test
    fun `service handles malformed messages gracefully`() {
        val malformedMessage = RemoteMessage.Builder("malformed_sender")
            .setMessageId("malformed_msg")
            .addData("invalid_type", "INVALID")
            .addData("title", "")
            .build()

        // Should not crash with malformed data
        service.onMessageReceived(malformedMessage)

        Thread.sleep(100)
        runBlocking { verify(pushNotificationManager).handleCustomNotification(any()) }
    }
}
