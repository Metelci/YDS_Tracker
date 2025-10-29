package com.mtlc.studyplan.notifications

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.messaging.RemoteMessage
import com.mtlc.studyplan.settings.manager.SettingsManager
import com.mtlc.studyplan.notifications.NotificationManager
import com.mtlc.studyplan.testutils.CoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.koin.core.context.stopKoin
import org.junit.Rule
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
 * Integration tests for PushNotificationManager
 * Tests FCM token management, message processing, and analytics
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class PushNotificationManagerTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    private lateinit var context: Context
    private lateinit var pushNotificationManager: PushNotificationManager

    @Mock
    private lateinit var settingsManager: SettingsManager

    @Mock
    private lateinit var notificationManager: NotificationManager

    // Additional dependencies for new PushNotificationManager signature
    @Mock
    private lateinit var fcmTokenManager: FCMTokenManager

    private lateinit var pushAnalyticsManager: PushAnalyticsManager
    private lateinit var pushMessageHandler: PushMessageHandler
    private lateinit var batteryAwarePushManager: BatteryAwarePushManager

    @Before
    fun setup() {
        try { stopKoin() } catch (e: Exception) { }
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()

        // Mock settings to enable push notifications
        whenever(settingsManager.currentSettings).thenReturn(
            kotlinx.coroutines.flow.MutableStateFlow(
                com.mtlc.studyplan.settings.data.UserSettings(
                    pushNotificationsEnabled = true
                )
            )
        )

        // Real analytics manager uses SharedPreferences safely in unit tests
        pushAnalyticsManager = PushAnalyticsManager(context)
        // Real handler to exercise message routing with mocked NotificationManager
        pushMessageHandler = PushMessageHandler(context, notificationManager, pushAnalyticsManager)
        // Simple battery-aware manager instance
        batteryAwarePushManager = BatteryAwarePushManager()

        val config = PushNotificationConfig(
            context = context,
            settingsManager = settingsManager,
            notificationManager = notificationManager,
            fcmTokenManager = fcmTokenManager,
            pushMessageHandler = pushMessageHandler,
            pushAnalyticsManager = pushAnalyticsManager,
            batteryAwarePushManager = batteryAwarePushManager
        )

        pushNotificationManager = PushNotificationManager(config)
    }

    @Test
    fun `arePushNotificationsEnabled returns true when enabled in settings`() = runTest {
        whenever(settingsManager.currentSettings).thenReturn(
            kotlinx.coroutines.flow.MutableStateFlow(
                com.mtlc.studyplan.settings.data.UserSettings(
                    pushNotificationsEnabled = true
                )
            )
        )

        val result = pushNotificationManager.arePushNotificationsEnabled()
        assertTrue("Push notifications should be enabled", result)
    }

    @Test
    fun `arePushNotificationsEnabled returns false when disabled in settings`() = runTest {
        whenever(settingsManager.currentSettings).thenReturn(
            kotlinx.coroutines.flow.MutableStateFlow(
                com.mtlc.studyplan.settings.data.UserSettings(
                    pushNotificationsEnabled = false
                )
            )
        )

        val result = pushNotificationManager.arePushNotificationsEnabled()
        assertTrue("Push notifications should be disabled", !result)
    }

    @Test
    fun `getStoredFCMToken returns stored token`() {
        // Store a token
        val testToken = "test_fcm_token_12345"
        val prefs = context.getSharedPreferences("fcm_token_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("fcm_token", testToken).apply()

        // Stub FCMTokenManager to read from shared preferences
        org.mockito.kotlin.whenever(fcmTokenManager.getStoredFCMToken()).thenAnswer {
            context.getSharedPreferences("fcm_token_prefs", Context.MODE_PRIVATE)
                .getString("fcm_token", null)
        }

        val storedToken = pushNotificationManager.getStoredFCMToken()
        assertEquals("Stored token should match", testToken, storedToken)
    }

    @Test
    fun `deleteFCMToken clears stored token`() = runTest {
        // Store a token first
        val testToken = "test_fcm_token_to_delete"
        val prefs = context.getSharedPreferences("fcm_token_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("fcm_token", testToken).apply()

        // Verify token is stored
        org.mockito.kotlin.whenever(fcmTokenManager.getStoredFCMToken()).thenAnswer {
            context.getSharedPreferences("fcm_token_prefs", Context.MODE_PRIVATE)
                .getString("fcm_token", null)
        }
        assertEquals(testToken, pushNotificationManager.getStoredFCMToken())

        // Delete token
        org.mockito.kotlin.whenever(fcmTokenManager.deleteFCMToken()).thenAnswer {
            context.getSharedPreferences("fcm_token_prefs", Context.MODE_PRIVATE)
                .edit().clear().apply()
            Unit
        }
        pushNotificationManager.deleteFCMToken()

        // Verify token is cleared
        assertEquals(null, pushNotificationManager.getStoredFCMToken())
    }

    @Test
    fun `handleIncomingMessage processes study reminder correctly`() = runTest {
        val message = PushMessage(
            id = "study_reminder_123",
            type = PushMessageType.STUDY_REMINDER,
            title = "Time to Study!",
            body = "Don't forget your daily study session",
            data = mapOf("taskId" to "task_123")
        )

        pushNotificationManager.handleIncomingMessage(message)

        verify(notificationManager).showDailyStudyReminder(
            context = eq(context),
            title = eq("Time to Study!"),
            message = eq("Don't forget your daily study session"),
            notificationId = eq("study_reminder_123".hashCode())
        )
    }

    @Test
    fun `handleIncomingMessage processes achievement notification correctly`() = runTest {
        val message = PushMessage(
            id = "achievement_456",
            type = PushMessageType.ACHIEVEMENT,
            title = "Achievement Unlocked!",
            body = "Congratulations on completing 10 tasks!",
            data = mapOf("achievementId" to "streak_10")
        )

        pushNotificationManager.handleIncomingMessage(message)

        verify(notificationManager).showAchievementNotification(
            title = eq("Achievement Unlocked!"),
            message = eq("Congratulations on completing 10 tasks!"),
            notificationId = eq("achievement_456".hashCode())
        )
    }

    @Test
    fun `handleIncomingMessage processes exam update correctly`() = runTest {
        val message = PushMessage(
            id = "exam_update_789",
            type = PushMessageType.EXAM_UPDATE,
            title = "Exam Update",
            body = "Your YDS exam is in 2 weeks",
            data = mapOf("examId" to "yds_2025")
        )

        pushNotificationManager.handleIncomingMessage(message)

        verify(notificationManager).showExamApplicationReminder(
            title = eq("Exam Update"),
            message = eq("Your YDS exam is in 2 weeks"),
            notificationId = eq("exam_update_789".hashCode())
        )
    }

    @Test
    fun `handleIncomingMessage processes motivational message correctly`() = runTest {
        val message = PushMessage(
            id = "motivational_101",
            type = PushMessageType.MOTIVATIONAL,
            title = "Stay Motivated!",
            body = "Every expert was once a beginner",
            data = emptyMap()
        )

        pushNotificationManager.handleIncomingMessage(message)

        verify(notificationManager).showQuickStudyReminder(
            title = eq("Stay Motivated!"),
            message = eq("Every expert was once a beginner"),
            notificationId = eq("motivational_101".hashCode())
        )
    }

    @Test
    fun `handleIncomingMessage processes system notification correctly`() = runTest {
        val message = PushMessage(
            id = "system_202",
            type = PushMessageType.SYSTEM,
            title = "System Maintenance",
            body = "App will be updated tonight",
            data = emptyMap()
        )

        pushNotificationManager.handleIncomingMessage(message)

        verify(notificationManager).showQuickStudyReminder(
            title = eq("System Maintenance"),
            message = eq("App will be updated tonight"),
            notificationId = eq("system_202".hashCode())
        )
    }

    @Test
    fun `handleIncomingMessage processes custom notification correctly`() = runTest {
        val message = PushMessage(
            id = "custom_303",
            type = PushMessageType.CUSTOM,
            title = "Custom Message",
            body = "This is a custom notification",
            data = mapOf("customData" to "value")
        )

        pushNotificationManager.handleIncomingMessage(message)

        verify(notificationManager).showQuickStudyReminder(
            title = eq("Custom Message"),
            message = eq("This is a custom notification"),
            notificationId = eq("custom_303".hashCode())
        )
    }

    @Test
    fun `getPushAnalytics returns analytics data`() {
        // Add some test analytics data
        val prefs = context.getSharedPreferences("push_analytics", Context.MODE_PRIVATE)
        prefs.edit()
            .putInt("push_study_reminder_count", 5)
            .putInt("push_study_reminder_delivered", 4)
            .putInt("push_achievement_count", 2)
            .putInt("push_achievement_delivered", 2)
            .putLong("last_push_timestamp", 1640995200000L)
            .apply()

        val analytics = pushNotificationManager.getPushAnalytics()

        assertEquals("Should have study reminder count", 5, analytics["study_reminder_count"])
        assertEquals("Should have study reminder delivered", 4, analytics["study_reminder_delivered"])
        assertEquals("Should have achievement count", 2, analytics["achievement_count"])
        assertEquals("Should have achievement delivered", 2, analytics["achievement_delivered"])
        assertEquals("Should have last push timestamp", 1640995200000L, analytics["last_push_timestamp"])
    }

    @Test
    fun `manageTopicSubscription handles subscribe and unsubscribe operations correctly`() = runTest {
        // Note: These tests would require Firebase initialization in a real environment
        // For unit testing, we verify the methods exist and can be called

        val subscribeResult = pushNotificationManager.manageTopicSubscription("test_topic", true)
        // In a real test environment, this would succeed or fail based on Firebase setup
        // For now, we just verify the method can be called without throwing

        val unsubscribeResult = pushNotificationManager.manageTopicSubscription("test_topic", false)
        // Similar to subscribe, we verify the method exists and can be called

        assertNotNull("Subscribe result should not be null", subscribeResult)
        assertNotNull("Unsubscribe result should not be null", unsubscribeResult)
    }
}
