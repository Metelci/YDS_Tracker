package com.mtlc.studyplan.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * Achievement tracking and progress calculation system
 */
class AchievementTracker(
    private val dataStore: DataStore<Preferences>,
    private val progressRepository: ProgressRepository
) {

    private object Keys {
        val UNLOCKED_ACHIEVEMENTS = stringSetPreferencesKey("unlocked_categorized_achievements")
        val ACHIEVEMENT_PROGRESS_DATA = stringPreferencesKey("achievement_progress_data")
        val LAST_CALCULATION_DATE = stringPreferencesKey("last_achievement_calculation")
        val ACHIEVEMENT_STATE_CACHE = stringPreferencesKey("achievement_state_cache")
    }

    /**
     * Flow of current achievement state
     */
    val achievementStateFlow: Flow<AchievementState> = combine(
        progressRepository.userProgressFlow,
        progressRepository.taskLogsFlow,
        dataStore.data
    ) { userProgress, taskLogs, preferences ->
        calculateAchievementState(userProgress, taskLogs, preferences)
    }

    /**
     * Calculate comprehensive achievement progress
     */
    private suspend fun calculateAchievementState(
        userProgress: UserProgress,
        taskLogs: List<TaskLog>,
        preferences: Preferences
    ): AchievementState {
        val unlockedAchievements = preferences[Keys.UNLOCKED_ACHIEVEMENTS] ?: emptySet()

        // Calculate detailed progress metrics
        val achievementProgress = calculateDetailedProgress(userProgress, taskLogs)

        // Process each category
        val categoryProgress = AchievementCategory.values().associate { category ->
            category to calculateCategoryProgress(category, achievementProgress, unlockedAchievements)
        }

        val totalPoints = categoryProgress.values.sumOf { it.categoryPoints }
        val totalAchievements = CategorizedAchievementDataSource.allCategorizedAchievements.size

        return AchievementState(
            categoryProgress = categoryProgress,
            unlockedAchievements = unlockedAchievements,
            totalAchievements = totalAchievements,
            totalPoints = totalPoints
        )
    }

    /**
     * Calculate detailed progress metrics from user data
     */
    private suspend fun calculateDetailedProgress(
        userProgress: UserProgress,
        taskLogs: List<TaskLog>
    ): AchievementProgress {
        val streakState = progressRepository.getStreakState(userProgress)

        // Grammar-specific calculations
        val grammarLogs = taskLogs.filter { isGrammarTask(it.category) }
        val grammarTasksCompleted = grammarLogs.size
        val grammarPerfectSessions = calculatePerfectGrammarSessions(grammarLogs)
        val grammarStreakDays = calculateGrammarStreak(grammarLogs)

        // Speed-specific calculations
        val speedMetrics = calculateSpeedMetrics(taskLogs)

        // Consistency calculations
        val consistencyMetrics = calculateConsistencyMetrics(taskLogs)

        // Progress calculations
        val progressMetrics = calculateProgressMetrics(userProgress, taskLogs)

        return AchievementProgress(
            userProgress = userProgress,
            taskLogs = taskLogs,
            streakState = streakState,

            grammarTasksCompleted = grammarTasksCompleted,
            grammarPerfectSessions = grammarPerfectSessions,
            grammarStreakDays = grammarStreakDays,

            fastCompletions = speedMetrics.fastCompletions,
            averageSpeedRatio = speedMetrics.averageSpeedRatio,
            fastExamCompletions = speedMetrics.fastExamCompletions,
            fastCompletionStreakDays = speedMetrics.streakDays,

            maxConsecutiveDays = consistencyMetrics.maxStreak,
            currentStreakDays = consistencyMetrics.currentStreak,
            totalStudyDays = consistencyMetrics.totalDays,

            overallProgressPercent = progressMetrics.overallPercent,
            weeklyProgressPercent = progressMetrics.weeklyPercent,
            programCompletionPercent = progressMetrics.programPercent
        )
    }

    /**
     * Calculate category-specific progress
     */
    private fun calculateCategoryProgress(
        category: AchievementCategory,
        progress: AchievementProgress,
        unlockedAchievements: Set<String>
    ): CategoryProgress {
        val achievements = CategorizedAchievementDataSource.getAchievementsByCategory(category)
        val unlockedInCategory = achievements.filter { it.id in unlockedAchievements }
        val nextAchievement = CategorizedAchievementDataSource.getNextAchievementInCategory(category, unlockedAchievements)

        val categoryPoints = unlockedInCategory.sumOf { it.pointsReward }
        val completionPercentage = if (achievements.isNotEmpty()) {
            unlockedInCategory.size.toFloat() / achievements.size.toFloat()
        } else 0f

        return CategoryProgress(
            category = category,
            achievements = achievements.map { achievement ->
                achievement.copy(isUnlocked = achievement.id in unlockedAchievements)
            },
            unlockedCount = unlockedInCategory.size,
            totalCount = achievements.size,
            categoryPoints = categoryPoints,
            nextAchievement = nextAchievement,
            completionPercentage = completionPercentage
        )
    }

    /**
     * Check for new achievement unlocks and update state
     */
    suspend fun checkAndUnlockAchievements(): List<AchievementUnlock> {
        val currentState = achievementStateFlow.first()
        val newUnlocks = mutableListOf<AchievementUnlock>()

        for (category in AchievementCategory.values()) {
            val categoryProgress = currentState.categoryProgress[category] ?: continue
            val achievementProgress = calculateDetailedProgress(
                currentState.categoryProgress.values.first().achievements.first().let {
                    // This is a bit of a hack to get user progress - should be refactored
                    progressRepository.userProgressFlow.first()
                },
                progressRepository.taskLogsFlow.first()
            )

            for (achievement in categoryProgress.achievements) {
                if (!achievement.isUnlocked && achievement.checkCondition(achievementProgress)) {
                    val unlock = unlockAchievement(achievement, categoryProgress)
                    newUnlocks.add(unlock)
                }
            }
        }

        return newUnlocks
    }

    /**
     * Unlock a specific achievement
     */
    private suspend fun unlockAchievement(
        achievement: CategorizedAchievement,
        categoryProgress: CategoryProgress
    ): AchievementUnlock {
        dataStore.edit { preferences ->
            val current = preferences[Keys.UNLOCKED_ACHIEVEMENTS] ?: emptySet()
            preferences[Keys.UNLOCKED_ACHIEVEMENTS] = current + achievement.id
        }

        val isNewTier = categoryProgress.currentTier?.ordinal?.let {
            achievement.tier.ordinal > it
        } ?: true

        val isNewCategory = categoryProgress.unlockedCount == 0

        val unlock = AchievementUnlock(
            achievement = achievement.copy(
                isUnlocked = true,
                unlockedDate = System.currentTimeMillis()
            ),
            isNewTier = isNewTier,
            isNewCategory = isNewCategory,
            pointsEarned = achievement.pointsReward,
            totalCategoryPoints = categoryProgress.categoryPoints + achievement.pointsReward
        )

        // Cache the updated achievement state
        cacheAchievementState()

        return unlock
    }

    /**
     * Cache achievement state to DataStore for persistence
     */
    private suspend fun cacheAchievementState() {
        val currentState = achievementStateFlow.first()
        val stateJson = Json.encodeToString(currentState)

        dataStore.edit { preferences ->
            preferences[Keys.ACHIEVEMENT_STATE_CACHE] = stateJson
            preferences[Keys.LAST_CALCULATION_DATE] = System.currentTimeMillis().toString()
        }
    }

    /**
     * Load cached achievement state from DataStore
     */
    private suspend fun loadCachedAchievementState(): AchievementState? {
        val preferences = dataStore.data.first()
        val cachedJson = preferences[Keys.ACHIEVEMENT_STATE_CACHE]

        return cachedJson?.let {
            try {
                Json.decodeFromString<AchievementState>(it)
            } catch (e: Exception) {
                null // Return null if parsing fails
            }
        }
    }

    /**
     * Helper functions for specific calculations
     */

    private fun isGrammarTask(category: String): Boolean {
        return category.lowercase().contains("grammar") || category.lowercase().contains("gramer")
    }

    private fun calculatePerfectGrammarSessions(grammarLogs: List<TaskLog>): Int {
        // Group by day and check for perfect sessions (all correct tasks in a day)
        val dailySessions = grammarLogs.groupBy { log ->
            LocalDate.ofEpochDay(TimeUnit.MILLISECONDS.toDays(log.timestampMillis))
        }

        return dailySessions.count { (_, logs) ->
            logs.isNotEmpty() && logs.all { it.correct }
        }
    }

    private fun calculateGrammarStreak(grammarLogs: List<TaskLog>): Int {
        if (grammarLogs.isEmpty()) return 0

        val dailyGrammarActivity = grammarLogs
            .groupBy { LocalDate.ofEpochDay(TimeUnit.MILLISECONDS.toDays(it.timestampMillis)) }
            .keys
            .sorted()

        // Calculate current streak from most recent day
        var streak = 0
        val today = LocalDate.now()
        var checkDate = today

        while (checkDate in dailyGrammarActivity) {
            streak++
            checkDate = checkDate.minusDays(1)
        }

        return streak
    }

    private data class SpeedMetrics(
        val fastCompletions: Int,
        val averageSpeedRatio: Float,
        val fastExamCompletions: Int,
        val streakDays: Int
    )

    private fun calculateSpeedMetrics(taskLogs: List<TaskLog>): SpeedMetrics {
        // This would need task duration estimates - simplified for now
        val estimatedMinutes = 15 // Average estimated time
        val fastCompletions = taskLogs.count { it.minutesSpent < estimatedMinutes }

        val averageSpeedRatio = if (taskLogs.isNotEmpty()) {
            taskLogs.map { it.minutesSpent.toFloat() / estimatedMinutes }.average().toFloat()
        } else 1f

        val fastExamCompletions = taskLogs.count {
            it.category.contains("exam", ignoreCase = true) && it.minutesSpent < 105 // 15 min early from 120 min
        }

        // Calculate speed streak (simplified)
        val speedStreakDays = calculateStreakDays(taskLogs) { it.minutesSpent < estimatedMinutes }

        return SpeedMetrics(fastCompletions, averageSpeedRatio, fastExamCompletions, speedStreakDays)
    }

    private data class ConsistencyMetrics(
        val maxStreak: Int,
        val currentStreak: Int,
        val totalDays: Int
    )

    private fun calculateConsistencyMetrics(taskLogs: List<TaskLog>): ConsistencyMetrics {
        val dailyActivity = taskLogs
            .groupBy { LocalDate.ofEpochDay(TimeUnit.MILLISECONDS.toDays(it.timestampMillis)) }
            .keys
            .sorted()

        if (dailyActivity.isEmpty()) {
            return ConsistencyMetrics(0, 0, 0)
        }

        var maxStreak = 0
        var currentStreak = 0
        var tempStreak = 1

        val today = LocalDate.now()
        var checkDate = today

        // Calculate current streak
        while (checkDate in dailyActivity) {
            currentStreak++
            checkDate = checkDate.minusDays(1)
        }

        // Calculate max streak
        for (i in 1 until dailyActivity.size) {
            if (dailyActivity[i].isEqual(dailyActivity[i-1].plusDays(1))) {
                tempStreak++
            } else {
                maxStreak = maxOf(maxStreak, tempStreak)
                tempStreak = 1
            }
        }
        maxStreak = maxOf(maxStreak, tempStreak)

        return ConsistencyMetrics(maxStreak, currentStreak, dailyActivity.size)
    }

    private data class ProgressMetrics(
        val overallPercent: Float,
        val weeklyPercent: Float,
        val programPercent: Float
    )

    private fun calculateProgressMetrics(userProgress: UserProgress, taskLogs: List<TaskLog>): ProgressMetrics {
        // These would need to be calculated based on actual curriculum structure
        // Simplified calculations for now
        val totalTasks = 1000 // Total estimated tasks in program
        val completedTasks = userProgress.completedTasks.size

        val overallPercent = (completedTasks.toFloat() / totalTasks * 100).coerceAtMost(100f)

        // Weekly progress (last 7 days)
        val weekAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
        val weeklyTasks = taskLogs.count { it.timestampMillis >= weekAgo }
        val weeklyPercent = (weeklyTasks.toFloat() / 35 * 100).coerceAtMost(100f) // ~5 tasks per day

        // Program completion based on 30-week structure
        val programPercent = (overallPercent / 30 * 30).coerceAtMost(100f) // 30 weeks total

        return ProgressMetrics(overallPercent, weeklyPercent, programPercent)
    }

    private fun calculateStreakDays(taskLogs: List<TaskLog>, condition: (TaskLog) -> Boolean): Int {
        val qualifyingDays = taskLogs
            .filter(condition)
            .groupBy { LocalDate.ofEpochDay(TimeUnit.MILLISECONDS.toDays(it.timestampMillis)) }
            .keys
            .sorted()

        if (qualifyingDays.isEmpty()) return 0

        var streak = 0
        val today = LocalDate.now()
        var checkDate = today

        while (checkDate in qualifyingDays) {
            streak++
            checkDate = checkDate.minusDays(1)
        }

        return streak
    }

    /**
     * Get progress for specific achievement
     */
    fun getAchievementProgress(achievementId: String): Flow<Float> {
        return achievementStateFlow.map { state ->
            val achievement = CategorizedAchievementDataSource.getAchievementById(achievementId)
            if (achievement != null) {
                val progress = calculateDetailedProgress(
                    state.categoryProgress.values.first().achievements.first().let {
                        // Again, this needs refactoring for proper data access
                        UserProgress() // Placeholder
                    },
                    emptyList() // Placeholder
                )
                CategorizedAchievementDataSource.calculateAchievementProgress(achievement, progress)
            } else 0f
        }
    }
}

/**
 * Extension functions for easier integration
 */
suspend fun ProgressRepository.createAchievementTracker(dataStore: DataStore<Preferences>): AchievementTracker {
    return AchievementTracker(dataStore, this)
}