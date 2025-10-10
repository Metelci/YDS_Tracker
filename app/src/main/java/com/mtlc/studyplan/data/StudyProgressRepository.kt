package com.mtlc.studyplan.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

private val Context.studyProgressDataStore: DataStore<Preferences> by preferencesDataStore(name = "study_progress")

/**
 * Repository for managing study progress data including current week, 
 * study start date, and manual week overrides.
 * 
 * This class handles complex business logic for:
 * - Calculating current week based on start date and progression
 * - Handling manual week overrides that take precedence over automatic calculation
 * - Managing different curriculum phases (Red Book, Blue Book, Green Book, Exam Camp)
 */
@Singleton
class StudyProgressRepository @Inject constructor(
    private val context: Context
) {
    private val dataStore = context.studyProgressDataStore

    private object Keys {
        val CURRENT_WEEK = intPreferencesKey("current_week")
        val STUDY_START_DATE = longPreferencesKey("study_start_date_epoch_day")
        val MANUAL_WEEK_OVERRIDE = intPreferencesKey("manual_week_override")
    }

    /**
     * Flow that emits the current week number based on complex business rules
     * 
     * Priority order for determining current week:
     * 1. Manual week override (takes highest priority)
     * 2. Previously stored current week value
     * 3. Calculated based on study start date and current date
     * 4. Default to week 1 if none of the above apply
     * 
     * Week values are clamped between 1 and 30 to align with the curriculum structure.
     */
    val currentWeek: Flow<Int> = dataStore.data.map { preferences ->
        // Check for manual override first (highest priority)
        val manualOverride = preferences[Keys.MANUAL_WEEK_OVERRIDE]
        if (manualOverride != null && manualOverride > 0) {
            return@map manualOverride.coerceIn(1, 30)
        }

        // Check for stored current week (medium priority)
        val storedWeek = preferences[Keys.CURRENT_WEEK]
        if (storedWeek != null && storedWeek > 0) {
            return@map storedWeek.coerceIn(1, 30)
        }

        // Calculate based on start date if available (lowest priority)
        val startDateEpochDay = preferences[Keys.STUDY_START_DATE]
        if (startDateEpochDay != null) {
            val startDate = LocalDate.ofEpochDay(startDateEpochDay)
            val today = LocalDate.now()
            val weeksSinceStart = ChronoUnit.WEEKS.between(startDate, today).toInt() + 1
            return@map weeksSinceStart.coerceIn(1, 30)
        }

        // Default to week 1 if no other information is available
        1
    }

    suspend fun setStudyStartDate(startDate: LocalDate) {
        dataStore.edit { preferences ->
            preferences[Keys.STUDY_START_DATE] = startDate.toEpochDay()
            // Reset manual override when setting start date
            preferences.remove(Keys.MANUAL_WEEK_OVERRIDE)
        }
    }

    suspend fun setCurrentWeek(week: Int) {
        dataStore.edit { preferences ->
            preferences[Keys.CURRENT_WEEK] = week.coerceIn(1, 30)
        }
    }

    suspend fun setManualWeekOverride(week: Int?) {
        dataStore.edit { preferences ->
            if (week != null && week > 0) {
                preferences[Keys.MANUAL_WEEK_OVERRIDE] = week.coerceIn(1, 30)
            } else {
                preferences.remove(Keys.MANUAL_WEEK_OVERRIDE)
            }
        }
    }

    suspend fun advanceToNextWeek() {
        dataStore.edit { preferences ->
            val currentWeek = preferences[Keys.CURRENT_WEEK] ?: 1
            val nextWeek = (currentWeek + 1).coerceAtMost(30)
            preferences[Keys.CURRENT_WEEK] = nextWeek
        }
    }

    suspend fun resetProgress() {
        dataStore.edit { preferences ->
            preferences.remove(Keys.CURRENT_WEEK)
            preferences.remove(Keys.STUDY_START_DATE)
            preferences.remove(Keys.MANUAL_WEEK_OVERRIDE)
        }
    }

    // Helper functions for testing different curriculum phases
    suspend fun setToRedBookPhase(week: Int = 1) = setManualWeekOverride(week.coerceIn(1, 8))
    suspend fun setToBlueBookPhase(week: Int = 9) = setManualWeekOverride(week.coerceIn(9, 18))
    suspend fun setToGreenBookPhase(week: Int = 19) = setManualWeekOverride(week.coerceIn(19, 26))
    suspend fun setToExamCampPhase(week: Int = 27) = setManualWeekOverride(week.coerceIn(27, 30))

    suspend fun getCurrentWeekSync(): Int {
        // Simplified sync version for testing
        return 1 // Default to week 1, should use Flow in real app
    }
}