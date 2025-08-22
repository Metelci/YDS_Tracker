package com.mtlc.studyplan.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

//region DATASTORE VE REPOSITORY
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "yds_progress")

class ProgressRepository(private val dataStore: DataStore<Preferences>) {
    private object Keys {
        val COMPLETED_TASKS = stringSetPreferencesKey("completed_tasks")
        val STREAK_COUNT = intPreferencesKey("streak_count")
        val LAST_COMPLETION_DATE = longPreferencesKey("last_completion_date")
        val UNLOCKED_ACHIEVEMENTS = stringSetPreferencesKey("unlocked_achievements")
    }

    val userProgressFlow = dataStore.data.map { preferences ->
        UserProgress(
            completedTasks = preferences[Keys.COMPLETED_TASKS] ?: emptySet(),
            streakCount = preferences[Keys.STREAK_COUNT] ?: 0,
            lastCompletionDate = preferences[Keys.LAST_COMPLETION_DATE] ?: 0L,
            unlockedAchievements = preferences[Keys.UNLOCKED_ACHIEVEMENTS] ?: emptySet()
        )
    }

    suspend fun saveProgress(progress: UserProgress) {
        dataStore.edit { preferences ->
            preferences[Keys.COMPLETED_TASKS] = progress.completedTasks
            preferences[Keys.STREAK_COUNT] = progress.streakCount
            preferences[Keys.LAST_COMPLETION_DATE] = progress.lastCompletionDate
            preferences[Keys.UNLOCKED_ACHIEVEMENTS] = progress.unlockedAchievements
        }
    }
}
//endregion