package com.mtlc.studyplan.services

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

private val Context.notificationTrackingDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "notification_tracking"
)

private object NotificationTrackingKeys {
    val LAST_ATTEMPT = longPreferencesKey("last_delivery_attempt")
    val LAST_SUCCESS = booleanPreferencesKey("last_delivery_success")
    val LAST_REASON = stringPreferencesKey("last_delivery_reason")
    val TOTAL_SCHEDULED = intPreferencesKey("total_scheduled")
    val TOTAL_DELIVERED = intPreferencesKey("total_delivered")
    val TOTAL_FAILED = intPreferencesKey("total_failed")
}

fun Context.notificationDeliveryStatsFlow(): Flow<DeliveryStats> {
    return notificationTrackingDataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> prefs.toDeliveryStats() }
}

fun Context.getNotificationDeliveryStats(): DeliveryStats {
    return runBlocking {
        notificationTrackingDataStore.data
            .catch { emit(emptyPreferences()) }
            .map { prefs -> prefs.toDeliveryStats() }
            .first()
    }
}

suspend fun Context.recordNotificationDeliveryAttempt(success: Boolean, reason: String?) {
    notificationTrackingDataStore.edit { prefs ->
        val scheduled = prefs[NotificationTrackingKeys.TOTAL_SCHEDULED] ?: 0
        val delivered = prefs[NotificationTrackingKeys.TOTAL_DELIVERED] ?: 0
        val failed = prefs[NotificationTrackingKeys.TOTAL_FAILED] ?: 0

        val updatedDelivered = if (success) delivered + 1 else delivered
        val updatedFailed = if (success) failed else failed + 1

        prefs[NotificationTrackingKeys.LAST_ATTEMPT] = System.currentTimeMillis()
        prefs[NotificationTrackingKeys.LAST_SUCCESS] = success
        if (reason != null) {
            prefs[NotificationTrackingKeys.LAST_REASON] = reason
        } else {
            prefs.remove(NotificationTrackingKeys.LAST_REASON)
        }
        prefs[NotificationTrackingKeys.TOTAL_SCHEDULED] = scheduled + 1
        prefs[NotificationTrackingKeys.TOTAL_DELIVERED] = updatedDelivered
        prefs[NotificationTrackingKeys.TOTAL_FAILED] = updatedFailed
    }
}

private fun Preferences.toDeliveryStats(): DeliveryStats {
    return DeliveryStats(
        lastDeliveryAttempt = this[NotificationTrackingKeys.LAST_ATTEMPT] ?: 0L,
        lastDeliverySuccess = this[NotificationTrackingKeys.LAST_SUCCESS] ?: false,
        lastDeliveryReason = this[NotificationTrackingKeys.LAST_REASON],
        totalScheduled = this[NotificationTrackingKeys.TOTAL_SCHEDULED] ?: 0,
        totalDelivered = this[NotificationTrackingKeys.TOTAL_DELIVERED] ?: 0,
        totalFailed = this[NotificationTrackingKeys.TOTAL_FAILED] ?: 0
    )
}
