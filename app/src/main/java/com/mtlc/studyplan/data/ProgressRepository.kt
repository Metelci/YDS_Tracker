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
import androidx.datastore.preferences.core.booleanPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onStart
import com.mtlc.studyplan.storage.room.StudyPlanDatabase
import com.mtlc.studyplan.storage.room.TaskLogEntity
import com.mtlc.studyplan.storage.room.VocabProgressEntity
import com.mtlc.studyplan.storage.room.VocabSessionEntity
import com.mtlc.studyplan.PlanDataSource.getAppContext
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
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
        val TASK_LOGS_MIGRATED = booleanPreferencesKey("task_logs_migrated")
        val TOTAL_POINTS = intPreferencesKey("total_points")
        val CURRENT_STREAK_MULTIPLIER = floatPreferencesKey("current_streak_multiplier")
        val VOCABULARY_PROGRESS = stringSetPreferencesKey("vocabulary_progress")
        val VOCABULARY_SESSIONS = stringSetPreferencesKey("vocabulary_sessions")
        val PRACTICE_SESSIONS = stringSetPreferencesKey("practice_sessions")
    }

    val userProgressFlow = dataStore.data.map { preferences ->
        val streakCount = preferences[Keys.STREAK_COUNT] ?: 0
        UserProgress(
            completedTasks = preferences[Keys.COMPLETED_TASKS] ?: emptySet(),
            streakCount = streakCount,
            lastCompletionDate = preferences[Keys.LAST_COMPLETION_DATE] ?: 0L,
            unlockedAchievements = preferences[Keys.UNLOCKED_ACHIEVEMENTS] ?: emptySet(),
            totalPoints = preferences[Keys.TOTAL_POINTS] ?: 0,
            currentStreakMultiplier = StreakMultiplier.getMultiplierForStreak(streakCount).multiplier,
            dayProgress = emptyList(),
            totalXp = preferences[Keys.TOTAL_POINTS] ?: 0
        )
    }

    private val db by lazy { StudyPlanDatabase.get(getAppContext()) }
    private val taskLogDao by lazy { db.taskLogDao() }

    val taskLogsFlow: Flow<List<TaskLog>> = taskLogDao
        .observeAll()
        .onStart { migrateTaskLogsIfNeeded() }
        .map { list -> list.map { it.toTaskLog() } }
        .distinctUntilChanged()

    private val vocabularyDao by lazy { db.vocabularyDao() }

    val vocabularyProgressFlow: Flow<List<VocabularyProgress>> = vocabularyDao
        .observeAllProgress()
        .onStart { migrateVocabularyIfNeeded() }
        .map { list -> list.map { it.toModel() } }
        .distinctUntilChanged()

    val vocabularySessionsFlow: Flow<List<VocabularySession>> = vocabularyDao
        .observeSessions()
        .onStart { migrateVocabularyIfNeeded() }
        .map { list -> list.map { it.toModel() } }
        .distinctUntilChanged()

    val practiceSessionsFlow = dataStore.data.map { preferences ->
        val raw = preferences[Keys.PRACTICE_SESSIONS] ?: emptySet()
        raw.mapNotNull { decodePracticeSession(it) }
    }

    suspend fun saveProgress(progress: UserProgress) {
        dataStore.edit { preferences ->
            preferences[Keys.COMPLETED_TASKS] = progress.completedTasks
            preferences[Keys.STREAK_COUNT] = progress.streakCount
            preferences[Keys.LAST_COMPLETION_DATE] = progress.lastCompletionDate
            preferences[Keys.UNLOCKED_ACHIEVEMENTS] = progress.unlockedAchievements
            val storedPoints = maxOf(progress.totalPoints, progress.totalXp)
            preferences[Keys.TOTAL_POINTS] = storedPoints
            preferences[Keys.CURRENT_STREAK_MULTIPLIER] = progress.currentStreakMultiplier
        }
    }

    suspend fun addTaskLog(log: TaskLog) {
        taskLogDao.insert(log.toEntity())
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
            totalXp = progress.totalXp + pointsEarned,
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

    /**
     * Vocabulary System Integration
     */
    suspend fun saveVocabularyProgress(progress: VocabularyProgress) {
        vocabularyDao.upsertProgress(progress.toEntity())
    }

    suspend fun saveVocabularySession(session: VocabularySession) {
        vocabularyDao.insertSession(session.toEntity())
    }

    suspend fun getVocabularyProgress(wordId: String): VocabularyProgress? =
        vocabularyDao.getProgress(wordId)?.toModel()

    suspend fun updateVocabularyMastery(
        wordId: String,
        wasCorrect: Boolean,
        reviewTime: Long,
        difficulty: ReviewDifficulty
    ) {
        val existing = getVocabularyProgress(wordId)
        val now = System.currentTimeMillis()

        val updated = if (existing != null) {
            val newSuccessCount = if (wasCorrect) existing.successCount + 1 else existing.successCount
            val newErrorCount = if (!wasCorrect) existing.errorCount + 1 else existing.errorCount
            val newReviewCount = existing.reviewCount + 1
            val newMasteryLevel = calculateMasteryLevel(existing.masteryLevel, wasCorrect, difficulty)
            val newInterval = calculateNextInterval(existing.currentInterval, wasCorrect, newMasteryLevel)

            existing.copy(
                masteryLevel = newMasteryLevel,
                lastReviewDate = now,
                nextReviewDate = now + java.util.concurrent.TimeUnit.DAYS.toMillis(newInterval.toLong()),
                reviewCount = newReviewCount,
                successCount = newSuccessCount,
                errorCount = newErrorCount,
                currentInterval = newInterval
            )
        } else {
            val initialMasteryLevel = if (wasCorrect) 0.2f else 0.1f
            val initialInterval = if (wasCorrect) 3 else 1

            VocabularyProgress(
                wordId = wordId,
                masteryLevel = initialMasteryLevel,
                lastReviewDate = now,
                nextReviewDate = now + java.util.concurrent.TimeUnit.DAYS.toMillis(initialInterval.toLong()),
                reviewCount = 1,
                successCount = if (wasCorrect) 1 else 0,
                errorCount = if (!wasCorrect) 1 else 0,
                currentInterval = initialInterval
            )
        }

        saveVocabularyProgress(updated)
    }

    private fun calculateMasteryLevel(currentLevel: Float, wasCorrect: Boolean, difficulty: ReviewDifficulty): Float {
        val baseChange = when (difficulty) {
            ReviewDifficulty.EASY -> if (wasCorrect) 0.15f else -0.05f
            ReviewDifficulty.MEDIUM -> if (wasCorrect) 0.10f else -0.10f
            ReviewDifficulty.HARD -> if (wasCorrect) 0.05f else -0.15f
        }

        val adjustedChange = if (wasCorrect && currentLevel > 0.7f) {
            baseChange * (1.0f - currentLevel)
        } else {
            baseChange
        }

        return (currentLevel + adjustedChange).coerceIn(0.0f, 1.0f)
    }

    private fun calculateNextInterval(currentInterval: Int, wasCorrect: Boolean, masteryLevel: Float): Int {
        val baseIntervals = listOf(1, 3, 7, 14, 30, 60, 120)
        val currentIndex = baseIntervals.indexOfFirst { it >= currentInterval }.takeIf { it >= 0 } ?: 0

        val newIndex = when {
            wasCorrect -> {
                val masteryBonus = if (masteryLevel > 0.8f) 1 else 0
                kotlin.math.min(currentIndex + 1 + masteryBonus, baseIntervals.size - 1)
            }
            else -> {
                val penalty = if (masteryLevel < 0.3f) 2 else 1
                kotlin.math.max(currentIndex - penalty, 0)
            }
        }

        return baseIntervals[newIndex]
    }

    // Save a practice session summary
    suspend fun savePracticeSession(session: PracticeSessionSummary) {
        dataStore.edit { preferences ->
            val cur = preferences[Keys.PRACTICE_SESSIONS] ?: emptySet()
            preferences[Keys.PRACTICE_SESSIONS] = (cur + encodePracticeSession(session)).toList().takeLast(200).toSet()
        }
    }

    private suspend fun migrateTaskLogsIfNeeded() {
        val prefs = dataStore.data.first()
        val migrated = prefs[Keys.TASK_LOGS_MIGRATED] ?: false
        if (migrated) return
        val raw = prefs[Keys.TASK_LOGS] ?: emptySet()
        if (raw.isNotEmpty()) {
            raw.mapNotNull { decodeTaskLog(it) }.forEach { t -> taskLogDao.insert(t.toEntity()) }
        }
        dataStore.edit { it[Keys.TASK_LOGS_MIGRATED] = true }
    }

    private suspend fun migrateVocabularyIfNeeded() {
        val prefs = dataStore.data.first()
        val migrated = prefs[booleanPreferencesKey("vocab_migrated")] ?: false
        if (migrated) return
        val pRaw = prefs[Keys.VOCABULARY_PROGRESS] ?: emptySet()
        if (pRaw.isNotEmpty()) {
            pRaw.mapNotNull { decodeVocabularyProgress(it) }
                .forEach { vp -> vocabularyDao.upsertProgress(vp.toEntity()) }
        }
        val sRaw = prefs[Keys.VOCABULARY_SESSIONS] ?: emptySet()
        if (sRaw.isNotEmpty()) {
            sRaw.mapNotNull { decodeVocabularySession(it) }
                .forEach { vs -> vocabularyDao.insertSession(vs.toEntity()) }
        }
        dataStore.edit { it[booleanPreferencesKey("vocab_migrated")] = true }
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

// Vocabulary Progress encode/decode
private fun encodeVocabularyProgress(progress: VocabularyProgress): String {
    val json = Json { ignoreUnknownKeys = true }
    return json.encodeToString(progress)
}

private fun decodeVocabularyProgress(s: String): VocabularyProgress? = runCatching {
    val json = Json { ignoreUnknownKeys = true }
    json.decodeFromString<VocabularyProgress>(s)
}.getOrNull()

// Vocabulary Session encode/decode
private fun encodeVocabularySession(session: VocabularySession): String {
    val json = Json { ignoreUnknownKeys = true }
    return json.encodeToString(session)
}

private fun decodeVocabularySession(s: String): VocabularySession? = runCatching {
    val json = Json { ignoreUnknownKeys = true }
    json.decodeFromString<VocabularySession>(s)
}.getOrNull()

// (removed duplicate helpers; see single set below)

// Practice session summary encode/decode
@kotlinx.serialization.Serializable
data class PracticeCategoryStat(
    val category: String,
    val total: Int,
    val answered: Int,
    val correct: Int,
    val accuracy: Float
)

@kotlinx.serialization.Serializable
data class PracticeSessionSummary(
    val sessionId: String,
    val timestamp: Long,
    val minutes: Int,
    val total: Int,
    val answered: Int,
    val correct: Int,
    val accuracy: Float,
    val perCategory: List<PracticeCategoryStat>
)

private fun encodePracticeSession(session: PracticeSessionSummary): String {
    val json = Json { ignoreUnknownKeys = true }
    return json.encodeToString(session)
}

private fun decodePracticeSession(s: String): PracticeSessionSummary? = runCatching {
    val json = Json { ignoreUnknownKeys = true }
    json.decodeFromString<PracticeSessionSummary>(s)
}.getOrNull()

//endregion

// Mapping helpers and migration (single source of truth)
private fun TaskLogEntity.toTaskLog(): TaskLog = TaskLog(
    taskId = taskId,
    timestampMillis = timestampMillis,
    minutesSpent = minutesSpent,
    correct = correct,
    category = category,
    pointsEarned = pointsEarned
)

private fun TaskLog.toEntity(): TaskLogEntity = TaskLogEntity(
    taskId = taskId,
    timestampMillis = timestampMillis,
    minutesSpent = minutesSpent,
    correct = correct,
    category = category,
    pointsEarned = pointsEarned
)

private fun VocabProgressEntity.toModel() = VocabularyProgress(
    wordId = wordId,
    masteryLevel = masteryLevel,
    lastReviewDate = lastReviewDate,
    nextReviewDate = nextReviewDate,
    reviewCount = reviewCount,
    successCount = successCount,
    errorCount = errorCount,
    currentInterval = currentInterval
)

private fun VocabularyProgress.toEntity() = VocabProgressEntity(
    wordId = wordId,
    masteryLevel = masteryLevel,
    lastReviewDate = lastReviewDate,
    nextReviewDate = nextReviewDate,
    reviewCount = reviewCount,
    successCount = successCount,
    errorCount = errorCount,
    currentInterval = currentInterval
)

private fun VocabSessionEntity.toModel() = VocabularySession(
    sessionId = sessionId,
    vocabularyItems = if (vocabularyItemsCsv.isBlank()) emptyList() else vocabularyItemsCsv.split('|'),
    startTime = startTime,
    endTime = endTime,
    correctAnswers = correctAnswers,
    totalQuestions = totalQuestions,
    sessionType = sessionType
)

private fun VocabularySession.toEntity() = VocabSessionEntity(
    sessionId = sessionId,
    vocabularyItemsCsv = vocabularyItems.joinToString("|"),
    startTime = startTime,
    endTime = endTime,
    correctAnswers = correctAnswers,
    totalQuestions = totalQuestions,
    sessionType = sessionType
)

