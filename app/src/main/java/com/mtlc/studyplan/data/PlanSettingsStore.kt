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
    // Minutes of study availability per day (Mon..Sun)
    val monMinutes: Int = 60,
    val tueMinutes: Int = 60,
    val wedMinutes: Int = 60,
    val thuMinutes: Int = 60,
    val friMinutes: Int = 60,
    val satMinutes: Int = 120,
    val sunMinutes: Int = 120,
    // Date format pattern; if blank or null, use locale format
    val dateFormatPattern: String? = null,
)

class PlanSettingsStore(private val dataStore: DataStore<Preferences>) {
    private object Keys {
        val START_EPOCH_DAY = longPreferencesKey("plan_start_epoch_day")
        val TOTAL_WEEKS = intPreferencesKey("plan_total_weeks")
        val END_EPOCH_DAY = longPreferencesKey("plan_end_epoch_day")
        val TOTAL_MONTHS = intPreferencesKey("plan_total_months")
        val MON_MIN = intPreferencesKey("plan_mon_minutes")
        val TUE_MIN = intPreferencesKey("plan_tue_minutes")
        val WED_MIN = intPreferencesKey("plan_wed_minutes")
        val THU_MIN = intPreferencesKey("plan_thu_minutes")
        val FRI_MIN = intPreferencesKey("plan_fri_minutes")
        val SAT_MIN = intPreferencesKey("plan_sat_minutes")
        val SUN_MIN = intPreferencesKey("plan_sun_minutes")
        val DATE_FORMAT_PATTERN = androidx.datastore.preferences.core.stringPreferencesKey("plan_date_format_pattern")
    }

    private val defaultStart = LocalDate.now().toEpochDay()

    val settingsFlow: Flow<PlanDurationSettings> = dataStore.data.map { prefs ->
        PlanDurationSettings(
            startEpochDay = prefs[Keys.START_EPOCH_DAY] ?: defaultStart,
            totalWeeks = (prefs[Keys.TOTAL_WEEKS] ?: 30).coerceIn(1, 104),
            endEpochDay = prefs[Keys.END_EPOCH_DAY],
            totalMonths = prefs[Keys.TOTAL_MONTHS],
            monMinutes = (prefs[Keys.MON_MIN] ?: 60).coerceIn(0, 600),
            tueMinutes = (prefs[Keys.TUE_MIN] ?: 60).coerceIn(0, 600),
            wedMinutes = (prefs[Keys.WED_MIN] ?: 60).coerceIn(0, 600),
            thuMinutes = (prefs[Keys.THU_MIN] ?: 60).coerceIn(0, 600),
            friMinutes = (prefs[Keys.FRI_MIN] ?: 60).coerceIn(0, 600),
            satMinutes = (prefs[Keys.SAT_MIN] ?: 120).coerceIn(0, 600),
            sunMinutes = (prefs[Keys.SUN_MIN] ?: 120).coerceIn(0, 600),
            dateFormatPattern = prefs[Keys.DATE_FORMAT_PATTERN],
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
            // Availability per day
            prefs[Keys.MON_MIN] = next.monMinutes.coerceIn(0, 600)
            prefs[Keys.TUE_MIN] = next.tueMinutes.coerceIn(0, 600)
            prefs[Keys.WED_MIN] = next.wedMinutes.coerceIn(0, 600)
            prefs[Keys.THU_MIN] = next.thuMinutes.coerceIn(0, 600)
            prefs[Keys.FRI_MIN] = next.friMinutes.coerceIn(0, 600)
            prefs[Keys.SAT_MIN] = next.satMinutes.coerceIn(0, 600)
            prefs[Keys.SUN_MIN] = next.sunMinutes.coerceIn(0, 600)
            next.dateFormatPattern?.takeIf { it.isNotBlank() }?.let { pattern ->
                prefs[Keys.DATE_FORMAT_PATTERN] = pattern
            } ?: run { prefs.remove(Keys.DATE_FORMAT_PATTERN) }
        }
    }
}
