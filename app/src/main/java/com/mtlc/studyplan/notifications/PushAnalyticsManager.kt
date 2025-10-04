package com.mtlc.studyplan.notifications

import android.content.Context
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages analytics tracking for push notifications
 */
@Singleton
class PushAnalyticsManager @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "PushAnalyticsManager"
    }

    fun trackPushNotification(type: String, delivered: Boolean) {
        try {
            val prefs = context.getSharedPreferences("push_analytics", Context.MODE_PRIVATE)
            val currentCount = prefs.getInt("push_${type}_count", 0)
            val deliveredCount = prefs.getInt("push_${type}_delivered", 0)

            prefs.edit()
                .putInt("push_${type}_count", currentCount + 1)
                .putInt("push_${type}_delivered", if (delivered) deliveredCount + 1 else deliveredCount)
                .putLong("last_push_timestamp", System.currentTimeMillis())
                .apply()

        } catch (e: android.content.pm.PackageManager.NameNotFoundException) {
            Log.e(TAG, "Error accessing shared preferences", e)
        } catch (e: java.lang.SecurityException) {
            Log.e(TAG, "Security error accessing shared preferences", e)
        } catch (e: java.lang.IllegalArgumentException) {
            Log.e(TAG, "Invalid preference key format", e)
        }
    }

    fun getPushAnalytics(): Map<String, Any> {
        val prefs = context.getSharedPreferences("push_analytics", Context.MODE_PRIVATE)
        return mapOf(
            "study_reminder_count" to prefs.getInt("push_study_reminder_count", 0),
            "study_reminder_delivered" to prefs.getInt("push_study_reminder_delivered", 0),
            "achievement_count" to prefs.getInt("push_achievement_count", 0),
            "achievement_delivered" to prefs.getInt("push_achievement_delivered", 0),
            "exam_update_count" to prefs.getInt("push_exam_update_count", 0),
            "exam_update_delivered" to prefs.getInt("push_exam_update_delivered", 0),
            "motivational_count" to prefs.getInt("push_motivational_count", 0),
            "motivational_delivered" to prefs.getInt("push_motivational_delivered", 0),
            "system_count" to prefs.getInt("push_system_count", 0),
            "system_delivered" to prefs.getInt("push_system_delivered", 0),
            "custom_count" to prefs.getInt("push_custom_count", 0),
            "custom_delivered" to prefs.getInt("push_custom_delivered", 0),
            "last_push_timestamp" to prefs.getLong("last_push_timestamp", 0)
        )
    }
}
