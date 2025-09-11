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
import com.mtlc.studyplan.data.TaskLog

//region DATASTORE VE REPOSITORY
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "yds_progress")

class ProgressRepository(private val dataStore: DataStore<Preferences>) {
    private object Keys {
        val COMPLETED_TASKS = stringSetPreferencesKey("completed_tasks")
        val STREAK_COUNT = intPreferencesKey("streak_count")
        val LAST_COMPLETION_DATE = longPreferencesKey("last_completion_date")
        val UNLOCKED_ACHIEVEMENTS = stringSetPreferencesKey("unlocked_achievements")
        val TASK_LOGS = stringSetPreferencesKey("task_logs")
    }

    val userProgressFlow = dataStore.data.map { preferences ->
        UserProgress(
            completedTasks = preferences[Keys.COMPLETED_TASKS] ?: emptySet(),
            streakCount = preferences[Keys.STREAK_COUNT] ?: 0,
            lastCompletionDate = preferences[Keys.LAST_COMPLETION_DATE] ?: 0L,
            unlockedAchievements = preferences[Keys.UNLOCKED_ACHIEVEMENTS] ?: emptySet()
        )
    }

    val taskLogsFlow = dataStore.data.map { preferences ->
        val raw = preferences[Keys.TASK_LOGS] ?: emptySet()
        raw.mapNotNull { decodeTaskLog(it) }
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

// Simple pipe-delimited encode/decode for logs
private fun encodeTaskLog(log: TaskLog): String = listOf(
    log.taskId,
    log.timestampMillis.toString(),
    log.minutesSpent.toString(),
    if (log.correct) "1" else "0",
    log.category.replace("|", "/")
).joinToString("|")

private fun decodeTaskLog(s: String): TaskLog? = runCatching {
    val parts = s.split('|')
    TaskLog(
        taskId = parts.getOrNull(0) ?: return null,
        timestampMillis = parts.getOrNull(1)?.toLongOrNull() ?: 0L,
        minutesSpent = parts.getOrNull(2)?.toIntOrNull() ?: 0,
        correct = parts.getOrNull(3) == "1",
        category = parts.getOrNull(4) ?: "unknown",
    )
}.getOrNull()

suspend fun ProgressRepository.addTaskLog(log: TaskLog) {
    dataStore.edit { preferences ->
        val cur = preferences[Keys.TASK_LOGS] ?: emptySet()
        preferences[Keys.TASK_LOGS] = (cur + encodeTaskLog(log)).takeLast(1000).toSet()
    }
}
//endregion
