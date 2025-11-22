package com.mtlc.studyplan.notifications

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import androidx.annotation.VisibleForTesting
import com.google.firebase.messaging.RemoteMessage
import com.mtlc.studyplan.utils.SecurityUtils
import org.koin.core.context.GlobalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Firebase Cloud Messaging service for handling push notifications
 * Processes incoming FCM messages and delegates to PushNotificationManager
 */
class StudyPlanFirebaseMessagingService : FirebaseMessagingService() {

    @VisibleForTesting
    internal lateinit var pushNotificationManager: PushNotificationManager
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val TAG = "FCMService"
    }

    override fun onCreate() {
        super.onCreate()
        // Get dependencies from Koin
        val koin = GlobalContext.get()
        pushNotificationManager = koin.get()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "FCM message received: ${remoteMessage.messageId}")

        serviceScope.launch {
            try {
                handleMessage(remoteMessage)
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Invalid message format in FCM message", e)
                trackMessageFailure(remoteMessage, e)
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Invalid state when handling FCM message", e)
                trackMessageFailure(remoteMessage, e)
            } catch (e: SecurityException) {
                Log.e(TAG, "Security violation in FCM message handling", e)
                trackMessageFailure(remoteMessage, e)
            } catch (e: RuntimeException) {
                Log.e(TAG, "Runtime error handling FCM message", e)
                trackMessageFailure(remoteMessage, e)
            }
        }
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "FCM token refreshed")
        serviceScope.launch {
            try {
                pushNotificationManager.onTokenRefreshed(token)
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Invalid token format during refresh", e)
            } catch (e: SecurityException) {
                Log.e(TAG, "Security violation during token refresh", e)
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Invalid state during token refresh", e)
            } catch (e: kotlinx.coroutines.CancellationException) {
                Log.e(TAG, "Token refresh was cancelled", e)
            }
        }
    }

    private suspend fun handleMessage(remoteMessage: RemoteMessage) {
        val pushMessage = PushMessage.fromRemoteMessage(remoteMessage)

        // Track message reception
        trackMessageReceived(pushMessage)

        // Check if push notifications are enabled
        if (!pushNotificationManager.arePushNotificationsEnabled()) {
            Log.d(TAG, "Push notifications disabled, ignoring message")
            return
        }

        // Handle the message using the unified method
        pushNotificationManager.handleIncomingMessage(pushMessage)
    }

    private suspend fun trackMessageReceived(message: PushMessage) {
        try {
            val analyticsData = mapOf(
                "message_id" to (message.id ?: "unknown"),
                "message_type" to message.type.name,
                "timestamp" to System.currentTimeMillis(),
                "has_data" to message.data.isNotEmpty(),
                "has_notification" to (message.title != null || message.body != null)
            )

            // Store analytics data
            val prefs = SecurityUtils.getEncryptedSharedPreferences(this@StudyPlanFirebaseMessagingService)
            prefs.edit().apply {
                putLong("last_message_received", System.currentTimeMillis())
                putString("last_message_type", message.type.name)
            }.apply()

        } catch (e: SecurityException) {
            Log.e(TAG, "Security violation when tracking message reception", e)
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Invalid state when tracking message reception", e)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Invalid argument when tracking message reception", e)
        } catch (e: kotlinx.coroutines.CancellationException) {
            Log.e(TAG, "Message reception tracking was cancelled", e)
        }
    }

    private suspend fun trackMessageFailure(remoteMessage: RemoteMessage, error: Exception) {
        try {
            val failureData = mapOf(
                "message_id" to (remoteMessage.messageId ?: "unknown"),
                "error_message" to error.message,
                "timestamp" to System.currentTimeMillis()
            )

            val prefs = SecurityUtils.getEncryptedSharedPreferences(this@StudyPlanFirebaseMessagingService)
            prefs.edit().apply {
                putLong("last_message_failure", System.currentTimeMillis())
                putString("last_failure_reason", error.message)
            }.apply()

        } catch (e: SecurityException) {
            Log.e(TAG, "Security violation when tracking message failure", e)
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Invalid state when tracking message failure", e)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Invalid argument when tracking message failure", e)
        } catch (e: kotlinx.coroutines.CancellationException) {
            Log.e(TAG, "Message failure tracking was cancelled", e)
        }
    }
}
