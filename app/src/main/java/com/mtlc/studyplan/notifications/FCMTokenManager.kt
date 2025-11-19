package com.mtlc.studyplan.notifications

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.mtlc.studyplan.utils.SecurityUtils
import kotlinx.coroutines.tasks.await
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages FCM token operations including storage, retrieval, and backend synchronization
 */
@Singleton
class FCMTokenManager @Inject constructor(
    private val context: Context
) {
    private val firebaseMessaging = FirebaseMessaging.getInstance()

    companion object {
        private const val TAG = "FCMTokenManager"
        private const val KEY_FCM_TOKEN = "fcm_token"
        private const val KEY_TOKEN_TIMESTAMP = "token_timestamp"
    }

    suspend fun initializeFCM() {
        try {
            val token = firebaseMessaging.token.await()
            storeFCMToken(token)
            Log.d(TAG, "FCM initialized successfully")
        } catch (e: java.util.concurrent.ExecutionException) {
            Log.e(TAG, "Failed to get FCM token", e)
        } catch (e: java.lang.InterruptedException) {
            Log.e(TAG, "FCM initialization interrupted", e)
            Thread.currentThread().interrupt()
        } catch (e: com.google.firebase.FirebaseException) {
            Log.e(TAG, "Firebase error during FCM initialization", e)
        }
    }

    suspend fun onTokenRefreshed(newToken: String) {
        Log.d(TAG, "FCM token refreshed")
        storeFCMToken(newToken)
        notifyTokenUpdateToBackend(newToken)
    }

    private fun storeFCMToken(token: String) {
        val prefs = SecurityUtils.getEncryptedSharedPreferences(context)
        val encrypted = try {
            SecurityUtils.encryptString(token)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to encrypt FCM token", e)
            return
        }
        prefs.edit()
            .putString(KEY_FCM_TOKEN, encrypted)
            .putLong(KEY_TOKEN_TIMESTAMP, System.currentTimeMillis())
            .apply()
    }

    fun getStoredFCMToken(): String? {
        val prefs = SecurityUtils.getEncryptedSharedPreferences(context)
        val encrypted = prefs.getString(KEY_FCM_TOKEN, null) ?: return null
        return try {
            SecurityUtils.decryptString(encrypted)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decrypt stored FCM token", e)
            null
        }
    }

    suspend fun deleteFCMToken() {
        try {
            firebaseMessaging.deleteToken().await()
            clearStoredToken()
            Log.d(TAG, "FCM token deleted")
        } catch (e: java.util.concurrent.ExecutionException) {
            Log.e(TAG, "Failed to execute FCM token deletion", e)
        } catch (e: java.lang.InterruptedException) {
            Log.e(TAG, "FCM token deletion interrupted", e)
            Thread.currentThread().interrupt()
        } catch (e: com.google.firebase.FirebaseException) {
            Log.e(TAG, "Firebase error during token deletion", e)
        }
    }

    private fun clearStoredToken() {
        val prefs = SecurityUtils.getEncryptedSharedPreferences(context)
        prefs.edit().clear().apply()
    }

    suspend fun manageTopicSubscription(topic: String, subscribe: Boolean): Result<Unit> {
        return try {
            if (subscribe) {
                firebaseMessaging.subscribeToTopic(topic).await()
                Log.d(TAG, "Subscribed to topic: $topic")
            } else {
                firebaseMessaging.unsubscribeFromTopic(topic).await()
                Log.d(TAG, "Unsubscribed from topic: $topic")
            }
            Result.success(Unit)
        } catch (e: java.util.concurrent.ExecutionException) {
            val action = if (subscribe) "subscribe" else "unsubscribe"
            Log.e(TAG, "Failed to $action to topic: $topic", e)
            Result.failure(e)
        } catch (e: java.lang.InterruptedException) {
            val action = if (subscribe) "subscribe" else "unsubscribe"
            Log.e(TAG, "Topic $action interrupted: $topic", e)
            Thread.currentThread().interrupt()
            Result.failure(e)
        } catch (e: com.google.firebase.FirebaseException) {
            val action = if (subscribe) "subscribe" else "unsubscribe"
            Log.e(TAG, "Firebase error during topic $action: $topic", e)
            Result.failure(e)
        }
    }

    private suspend fun notifyTokenUpdateToBackend(token: String) {
        try {
            // Example API call (replace with actual implementation)
            // apiService.registerFCMToken(token)
            Log.d(TAG, "Token update notified to backend")
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "Network error notifying backend about token update", e)
        } catch (e: java.io.IOException) {
            Log.e(TAG, "I/O error notifying backend about token update", e)
        } catch (e: retrofit2.HttpException) {
            Log.e(TAG, "HTTP error notifying backend about token update", e)
        }
    }
}
