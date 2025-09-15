package com.mtlc.studyplan.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit
//region DATA CLASSES
//endregion

//region DATASTORE VE REPOSITORY
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "yds_progress")

class ProgressRepository(private val dataStore: DataStore<Preferences>) {
    private object Keys {
        val COMPLETED_TASKS = stringSetPreferencesKey("completed_tasks")
        val STREAK_COUNT = intPreferencesKey("streak_count")
        val LAST_COMPLETION_DATE = longPreferencesKey("last_completion_date")
        val UNLOCKED_ACHIEVEMENTS = stringSetPreferencesKey("unlocked_achievements")
        val TASK_LOGS = stringSetPreferencesKey("task_logs")
        val TOTAL_POINTS = intPreferencesKey("total_points")
        val CURRENT_STREAK_MULTIPLIER = floatPreferencesKey("current_streak_multiplier")
    }

    val userProgressFlow = dataStore.data.map { preferences ->
        val streakCount = preferences[Keys.STREAK_COUNT] ?: 0
        UserProgress(
            completedTasks = preferences[Keys.COMPLETED_TASKS] ?: emptySet(),
            streakCount = streakCount,
            lastCompletionDate = preferences[Keys.LAST_COMPLETION_DATE] ?: 0L,
            unlockedAchievements = preferences[Keys.UNLOCKED_ACHIEVEMENTS] ?: emptySet(),
            totalPoints = preferences[Keys.TOTAL_POINTS] ?: 0,
            currentStreakMultiplier = StreakMultiplier.getMultiplierForStreak(streakCount).multiplier
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
            preferences[Keys.TOTAL_POINTS] = progress.totalPoints
            preferences[Keys.CURRENT_STREAK_MULTIPLIER] = progress.currentStreakMultiplier
        }
    }

    suspend fun addTaskLog(log: TaskLog) {
        dataStore.edit { preferences ->
            val cur = preferences[Keys.TASK_LOGS] ?: emptySet()
            preferences[Keys.TASK_LOGS] = (cur + encodeTaskLog(log)).toList().takeLast(1000).toSet()
        }
    }

    /**
     * Enhanced streak system methods
     */
    suspend fun completeTaskWithPoints(
        taskId: String,
        taskDescription: String,
        taskDetails: String?,
        minutesSpent: Int
    ): PointsTransaction {
        val taskCategory = TaskCategory.fromString(taskDescription + " " + (taskDetails ?: ""))
        var result: PointsTransaction? = null

        userProgressFlow.collect { progress ->
            val streakMultiplier = StreakMultiplier.getMultiplierForStreak(progress.streakCount)
            val basePoints = taskCategory.basePoints
            val totalPoints = (basePoints * streakMultiplier.multiplier).toInt()

            val pointsTransaction = PointsTransaction(
                basePoints = basePoints,
                multiplier = streakMultiplier.multiplier,
                totalPoints = totalPoints,
                taskCategory = taskCategory,
                streakMultiplier = streakMultiplier
            )

            // Create enhanced task log with points
            val taskLog = TaskLog(
                taskId = taskId,
                timestampMillis = System.currentTimeMillis(),
                minutesSpent = minutesSpent,
                correct = true,
                category = taskCategory.displayName,
                pointsEarned = totalPoints
            )

            // Update progress with new points and potentially updated streak
            val updatedProgress = updateStreakAndPoints(progress, totalPoints)

            // Save both progress and task log
            saveProgress(updatedProgress)
            addTaskLog(taskLog)

            result = pointsTransaction
        }

        return result ?: PointsTransaction(0, 1f, 0, TaskCategory.OTHER, StreakMultiplier.getMultiplierForStreak(0))
    }

    private fun updateStreakAndPoints(progress: UserProgress, pointsEarned: Int): UserProgress {
        val now = System.currentTimeMillis()
        val today = LocalDate.now()
        val lastActivityDate = if (progress.lastCompletionDate > 0) {
            LocalDate.ofEpochDay(TimeUnit.MILLISECONDS.toDays(progress.lastCompletionDate))
        } else null

        val newStreakCount = when {
            lastActivityDate == null -> 1 // First completion
            lastActivityDate == today -> progress.streakCount // Same day, no change
            lastActivityDate == today.minusDays(1) -> progress.streakCount + 1 // Continue streak
            else -> 1 // Streak broken, start over
        }

        val newMultiplier = StreakMultiplier.getMultiplierForStreak(newStreakCount).multiplier

        return progress.copy(
            totalPoints = progress.totalPoints + pointsEarned,
            streakCount = newStreakCount,
            lastCompletionDate = now,
            currentStreakMultiplier = newMultiplier
        )
    }

    fun getStreakState(progress: UserProgress): StreakState {
        val now = System.currentTimeMillis()
        val today = LocalDate.now()
        val lastActivityDate = if (progress.lastCompletionDate > 0) {
            LocalDate.ofEpochDay(TimeUnit.MILLISECONDS.toDays(progress.lastCompletionDate))
        } else today

        val daysSinceLastActivity = java.time.temporal.ChronoUnit.DAYS.between(lastActivityDate, today).toInt()
        val isInDanger = daysSinceLastActivity >= 1 && progress.streakCount > 0

        // Calculate hours until streak breaks (assuming streak breaks after 48 hours of inactivity)
        val hoursUntilBreak = if (isInDanger) {
            val hoursSinceLastActivity = TimeUnit.MILLISECONDS.toHours(now - progress.lastCompletionDate).toInt()
            (48 - hoursSinceLastActivity).coerceAtLeast(0)
        } else 0

        return StreakState(
            currentStreak = progress.streakCount,
            multiplier = StreakMultiplier.getMultiplierForStreak(progress.streakCount),
            isInDanger = isInDanger,
            hoursUntilBreak = hoursUntilBreak,
            lastActivityDate = progress.lastCompletionDate
        )
    }

    /**
     * Achievement system integration
     */
    suspend fun createAchievementTracker(): AchievementTracker {
        return AchievementTracker(dataStore, this)
    }

    /**
     * Check for achievement unlocks after task completion
     */
    suspend fun checkAchievementUnlocks(): List<AchievementUnlock> {
        val tracker = createAchievementTracker()
        return tracker.checkAndUnlockAchievements()
    }

    /**
     * Get comprehensive achievement state
     */
    suspend fun getAchievementState(): AchievementState {
        val tracker = createAchievementTracker()
        return tracker.achievementStateFlow.first()
    }

    /**
     * Enhanced task completion with achievement checking
     */
    suspend fun completeTask(taskId: String, taskDescription: String, taskDetails: String?, minutesSpent: Int) {
        dataStore.edit { preferences ->
            val currentTasks = preferences[Keys.COMPLETED_TASKS] ?: emptySet()
            preferences[Keys.COMPLETED_TASKS] = currentTasks + taskId

            // Update last completion date and streak
            val now = System.currentTimeMillis()
            val currentProgress = UserProgress(
                completedTasks = currentTasks + taskId,
                lastCompletionDate = preferences[Keys.LAST_COMPLETION_DATE] ?: 0L,
                streakCount = preferences[Keys.STREAK_COUNT] ?: 0,
                unlockedAchievements = preferences[Keys.UNLOCKED_ACHIEVEMENTS] ?: emptySet(),
                totalPoints = preferences[Keys.TOTAL_POINTS] ?: 0
            )

            val updatedProgress = updateStreakAndPoints(currentProgress, 0)
            preferences[Keys.STREAK_COUNT] = updatedProgress.streakCount
            preferences[Keys.LAST_COMPLETION_DATE] = now
        }

        // Add task log
        val taskLog = TaskLog(
            taskId = taskId,
            timestampMillis = System.currentTimeMillis(),
            minutesSpent = minutesSpent,
            correct = true,
            category = taskDescription
        )
        addTaskLog(taskLog)
    }
}

// Simple pipe-delimited encode/decode for logs
private fun encodeTaskLog(log: TaskLog): String = listOf(
    log.taskId,
    log.timestampMillis.toString(),
    log.minutesSpent.toString(),
    if (log.correct) "1" else "0",
    log.category.replace("|", "/"),
    log.pointsEarned.toString()
).joinToString("|")

private fun decodeTaskLog(s: String): TaskLog? = runCatching {
    val parts = s.split('|')
    TaskLog(
        taskId = parts.getOrNull(0) ?: return null,
        timestampMillis = parts.getOrNull(1)?.toLongOrNull() ?: 0L,
        minutesSpent = parts.getOrNull(2)?.toIntOrNull() ?: 0,
        correct = parts.getOrNull(3) == "1",
        category = parts.getOrNull(4) ?: "unknown",
        pointsEarned = parts.getOrNull(5)?.toIntOrNull() ?: 0
    )
}.getOrNull()

//endregion
