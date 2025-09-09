package com.mtlc.studyplan.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

data class PlanDurationSettings(
    val startEpochDay: Long = LocalDate.now().toEpochDay(),
    val totalWeeks: Int = 30,
    val endEpochDay: Long? = null,
    val totalMonths: Int? = null,
)

class PlanSettingsStore(private val dataStore: DataStore<Preferences>) {
    private object Keys {
        val START_EPOCH_DAY = longPreferencesKey("plan_start_epoch_day")
        val TOTAL_WEEKS = intPreferencesKey("plan_total_weeks")
        val END_EPOCH_DAY = longPreferencesKey("plan_end_epoch_day")
        val TOTAL_MONTHS = intPreferencesKey("plan_total_months")
    }

    private val defaultStart = LocalDate.now().toEpochDay()

    val settingsFlow: Flow<PlanDurationSettings> = dataStore.data.map { prefs ->
        PlanDurationSettings(
            startEpochDay = prefs[Keys.START_EPOCH_DAY] ?: defaultStart,
            totalWeeks = (prefs[Keys.TOTAL_WEEKS] ?: 30).coerceIn(1, 104),
            endEpochDay = prefs[Keys.END_EPOCH_DAY],
            totalMonths = prefs[Keys.TOTAL_MONTHS],
        )
    }

    suspend fun update(transform: (PlanDurationSettings) -> PlanDurationSettings) {
        dataStore.edit { prefs ->
            val current = PlanDurationSettings(
                startEpochDay = prefs[Keys.START_EPOCH_DAY] ?: defaultStart,
                totalWeeks = (prefs[Keys.TOTAL_WEEKS] ?: 30).coerceIn(1, 104),
                endEpochDay = prefs[Keys.END_EPOCH_DAY],
                totalMonths = prefs[Keys.TOTAL_MONTHS],
            )
            val next = transform(current)
            prefs[Keys.START_EPOCH_DAY] = next.startEpochDay
            prefs[Keys.TOTAL_WEEKS] = next.totalWeeks.coerceIn(1, 104)
            if (next.endEpochDay != null) {
                prefs[Keys.END_EPOCH_DAY] = next.endEpochDay
            } else {
                prefs.remove(Keys.END_EPOCH_DAY)
            }
            if (next.totalMonths != null) {
                prefs[Keys.TOTAL_MONTHS] = next.totalMonths
            } else {
                prefs.remove(Keys.TOTAL_MONTHS)
            }
        }
    }
}
