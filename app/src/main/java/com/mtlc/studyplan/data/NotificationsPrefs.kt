package com.mtlc.studyplan.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate

val Context.notificationsDataStore: DataStore<Preferences> by preferencesDataStore("yds_notif_prefs")

object NotificationsPrefsKeys {
    val QUIET_START_HOUR = intPreferencesKey("quiet_start_hour") // 22
    val QUIET_END_HOUR = intPreferencesKey("quiet_end_hour")     // 7
    val MUTE_TODAY_YEAR = intPreferencesKey("mute_today_y")
    val MUTE_TODAY_DAYOFYEAR = intPreferencesKey("mute_today_doy")
}

suspend fun isMutedToday(context: Context): Boolean {
    val today = LocalDate.now()
    val p = context.notificationsDataStore.data.first()
    val y = p[NotificationsPrefsKeys.MUTE_TODAY_YEAR] ?: -1
    val d = p[NotificationsPrefsKeys.MUTE_TODAY_DAYOFYEAR] ?: -1
    return (y == today.year && d == today.dayOfYear)
}

suspend fun muteToday(context: Context) {
    val t = LocalDate.now()
    context.notificationsDataStore.edit { e ->
        e[NotificationsPrefsKeys.MUTE_TODAY_YEAR] = t.year
        e[NotificationsPrefsKeys.MUTE_TODAY_DAYOFYEAR] = t.dayOfYear
    }
}

suspend fun isQuietNow(context: Context): Boolean {
    val now = java.time.LocalTime.now().hour
    val prefs = context.notificationsDataStore.data.first()
    val start = prefs[NotificationsPrefsKeys.QUIET_START_HOUR] ?: 22
    val end = prefs[NotificationsPrefsKeys.QUIET_END_HOUR] ?: 7
    return if (start <= end) {
        now in start until end
    } else {
        // Quiet across midnight
        now >= start || now < end
    }
}

