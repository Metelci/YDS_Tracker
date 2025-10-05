package com.mtlc.studyplan.notifications

import android.content.Context
import android.util.Log
import com.mtlc.studyplan.settings.manager.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Configuration class for PushNotificationManager dependencies
 */
data class PushNotificationConfig(
    val context: Context,
    val settingsManager: SettingsManager,
    val notificationManager: NotificationManager,
    val fcmTokenManager: FCMTokenManager,
    val pushMessageHandler: PushMessageHandler,
    val pushAnalyticsManager: PushAnalyticsManager,
    val batteryAwarePushManager: BatteryAwarePushManager
)

/**
 * Manager for Firebase Cloud Messaging push notifications
 * Coordinates between FCM token management, message processing, analytics, and battery optimizations
 */
@Singleton
class PushNotificationManager @Inject constructor(
    config: PushNotificationConfig
) {

    private val context = config.context
    private val settingsManager = config.settingsManager
    private val notificationManager = config.notificationManager
    private val fcmTokenManager = config.fcmTokenManager
    private val pushMessageHandler = config.pushMessageHandler
    private val pushAnalyticsManager = config.pushAnalyticsManager
    private val batteryAwarePushManager = config.batteryAwarePushManager

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val TAG = "PushNotificationManager"
    }

    init {
        scope.launch {
            fcmTokenManager.initializeFCM()
        }
    }

    /**
     * Check if push notifications are enabled in settings
     */
    suspend fun arePushNotificationsEnabled(): Boolean {
        return settingsManager.currentSettings.first().pushNotificationsEnabled
    }

    /**
     * Handle FCM token refresh
     */
    suspend fun onTokenRefreshed(newToken: String) {
        fcmTokenManager.onTokenRefreshed(newToken)
    }

    /**
     * Get stored FCM token
     */
    fun getStoredFCMToken(): String? {
        return fcmTokenManager.getStoredFCMToken()
    }

    /**
     * Delete FCM token (for logout or privacy)
     */
    suspend fun deleteFCMToken() {
        fcmTokenManager.deleteFCMToken()
    }

    /**
     * Manage FCM topic subscription
     */
    suspend fun manageTopicSubscription(topic: String, subscribe: Boolean): Result<Unit> {
        return fcmTokenManager.manageTopicSubscription(topic, subscribe)
    }

    /**
     * Handle incoming push message based on type
     */
    suspend fun handleIncomingMessage(message: PushMessage) {
        pushMessageHandler.handleIncomingMessage(message)
    }

    /**
     * Handle study reminder message
     */
    suspend fun handleStudyReminder(message: PushMessage) {
        pushMessageHandler.handleStudyReminder(message)
    }

    /**
     * Handle achievement notification
     */
    suspend fun handleAchievementNotification(message: PushMessage) {
        pushMessageHandler.handleAchievementNotification(message)
    }

    /**
     * Handle exam update notification
     */
    suspend fun handleExamUpdate(message: PushMessage) {
        pushMessageHandler.handleExamUpdate(message)
    }

    /**
     * Handle motivational message
     */
    suspend fun handleMotivationalMessage(message: PushMessage) {
        pushMessageHandler.handleMotivationalMessage(message)
    }

    /**
     * Handle system notification
     */
    suspend fun handleSystemNotification(message: PushMessage) {
        pushMessageHandler.handleSystemNotification(message)
    }

    /**
     * Handle custom notification
     */
    suspend fun handleCustomNotification(message: PushMessage) {
        pushMessageHandler.handleCustomNotification(message)
    }

    /**
     * Get push notification analytics
     */
    fun getPushAnalytics(): Map<String, Any> {
        return pushAnalyticsManager.getPushAnalytics()
    }

    /**
     * Battery optimization methods for power management
     */
    fun setDozeMode(enabled: Boolean) {
        batteryAwarePushManager.setDozeMode(enabled)
    }

    fun setLowPowerMode(enabled: Boolean) {
        batteryAwarePushManager.setLowPowerMode(enabled)
    }

    fun setCriticalPowerMode(enabled: Boolean) {
        batteryAwarePushManager.setCriticalPowerMode(enabled)
    }

    fun shouldSendNotification(messageType: PushMessageType): Boolean {
        return batteryAwarePushManager.shouldSendNotification(messageType)
    }
}
