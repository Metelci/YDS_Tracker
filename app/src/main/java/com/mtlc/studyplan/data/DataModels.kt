//region VERİ MODELLERİ
package com.mtlc.studyplan.data

import kotlinx.serialization.Serializable

data class Task(val id: String, val desc: String, val details: String? = null)
data class DayPlan(val day: String, val tasks: List<Task>)
data class WeekPlan(val week: Int, val month: Int, val title: String, val days: List<DayPlan>)
data class Achievement(val id: String, val title: String, val description: String, val condition: (UserProgress) -> Boolean)
data class ExamInfo(val name: String, val applicationStart: java.time.LocalDate, val applicationEnd: java.time.LocalDate, val examDate: java.time.LocalDate)
data class UserProgress(
    val completedTasks: Set<String> = emptySet(),
    val streakCount: Int = 0,
    val lastCompletionDate: Long = 0L,
    val unlockedAchievements: Set<String> = emptySet(),
    val totalPoints: Int = 0, // New field for total points earned
    val currentStreakMultiplier: Float = 1f, // Current multiplier based on streak
)

data class TaskLog(
    val taskId: String,
    val timestampMillis: Long,
    val minutesSpent: Int,
    val correct: Boolean,
    val category: String,
    val pointsEarned: Int = 0, // New field for points earned from this task
)

data class WeaknessSummary(
    val category: String,
    val total: Int,
    val incorrect: Int,
    val incorrectRate: Double,
)

/**
 * Enhanced streak system data models
 */
data class StreakMultiplier(
    val streakDays: Int,
    val multiplier: Float,
    val title: String,
    val description: String,
    val isFireStreak: Boolean = false
) {
    companion object {
        fun getMultiplierForStreak(streakDays: Int): StreakMultiplier = when {
            streakDays >= 50 -> StreakMultiplier(
                streakDays = streakDays,
                multiplier = 5f,
                title = "🔥 Fire Streak",
                description = "Legendary ${streakDays}-day streak!",
                isFireStreak = true
            )
            streakDays >= 30 -> StreakMultiplier(
                streakDays = streakDays,
                multiplier = 5f,
                title = "Master Streak",
                description = "5x points multiplier"
            )
            streakDays >= 14 -> StreakMultiplier(
                streakDays = streakDays,
                multiplier = 3f,
                title = "Power Streak",
                description = "3x points multiplier"
            )
            streakDays >= 7 -> StreakMultiplier(
                streakDays = streakDays,
                multiplier = 2f,
                title = "Building Streak",
                description = "2x points multiplier"
            )
            else -> StreakMultiplier(
                streakDays = streakDays,
                multiplier = 1f,
                title = "Starting Strong",
                description = "Base points"
            )
        }
    }
}

enum class TaskCategory(val basePoints: Int, val displayName: String) {
    GRAMMAR(10, "Grammar"),
    READING(15, "Reading"),
    LISTENING(12, "Listening"),
    VOCABULARY(8, "Vocabulary"),
    PRACTICE_EXAM(50, "Practice Exam"),
    OTHER(5, "Other");

    companion object {
        fun fromString(category: String): TaskCategory = when (category.lowercase()) {
            "grammar", "gramer" -> GRAMMAR
            "reading", "okuma" -> READING
            "listening", "dinleme" -> LISTENING
            "vocabulary", "vocab", "kelime" -> VOCABULARY
            "exam", "practice", "mock" -> PRACTICE_EXAM
            else -> OTHER
        }
    }
}

data class PointsTransaction(
    val basePoints: Int,
    val multiplier: Float,
    val totalPoints: Int,
    val taskCategory: TaskCategory,
    val streakMultiplier: StreakMultiplier,
    val timestampMillis: Long = System.currentTimeMillis()
) {
    val isMultiplierBonus: Boolean = multiplier > 1f
}

data class StreakState(
    val currentStreak: Int,
    val multiplier: StreakMultiplier,
    val isInDanger: Boolean = false, // True if streak will break today without activity
    val hoursUntilBreak: Int = 0,    // Hours remaining before streak breaks
    val lastActivityDate: Long = 0L   // Last activity timestamp
) {
    val isFireStreak: Boolean = multiplier.isFireStreak
    val nextMilestone: Int = when {
        currentStreak < 7 -> 7
        currentStreak < 14 -> 14
        currentStreak < 30 -> 30
        currentStreak < 50 -> 50
        else -> ((currentStreak / 50) + 1) * 50 // Next 50-day milestone
    }
    val progressToNextMilestone: Float = currentStreak.toFloat() / nextMilestone.toFloat()
}

/**
 * Enhanced Achievement System
 */
enum class AchievementCategory(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val color: Long
) {
    GRAMMAR_MASTER(
        id = "grammar_master",
        title = "Grammar Master",
        description = "Master the building blocks of language",
        icon = "🎓",
        color = 0xFF2196F3 // Blue
    ),
    SPEED_DEMON(
        id = "speed_demon",
        title = "Speed Demon",
        description = "Lightning-fast task completion",
        icon = "⚡",
        color = 0xFFFF9800 // Orange
    ),
    CONSISTENCY_CHAMPION(
        id = "consistency_champion",
        title = "Consistency Champion",
        description = "Unwavering dedication to daily practice",
        icon = "📅",
        color = 0xFF4CAF50 // Green
    ),
    PROGRESS_PIONEER(
        id = "progress_pioneer",
        title = "Progress Pioneer",
        description = "Steadily advancing through the curriculum",
        icon = "📈",
        color = 0xFF9C27B0 // Purple
    )
}

enum class AchievementTier(
    val id: String,
    val title: String,
    val multiplier: Int,
    val color: Long
) {
    BRONZE("bronze", "Bronze", 1, 0xFFCD7F32),
    SILVER("silver", "Silver", 2, 0xFFC0C0C0),
    GOLD("gold", "Gold", 3, 0xFFFFD700),
    PLATINUM("platinum", "Platinum", 5, 0xFFE5E4E2)
}

@Serializable
data class CategorizedAchievement(
    val id: String,
    val category: AchievementCategory,
    val tier: AchievementTier,
    val title: String,
    val description: String,
    val targetValue: Int,
    val isUnlocked: Boolean = false,
    val unlockedDate: Long? = null,
    val pointsReward: Int = 0
) {
    val fullTitle: String = "${tier.title} ${title}"
    val sortOrder: Int = category.ordinal * 100 + tier.ordinal

    // Condition function moved to AchievementDataSource for serialization compatibility
    fun checkCondition(progress: AchievementProgress): Boolean {
        return CategorizedAchievementDataSource.getConditionFunction(this.id)(progress)
    }
}

data class AchievementProgress(
    val userProgress: UserProgress,
    val taskLogs: List<TaskLog>,
    val streakState: StreakState,

    // Category-specific progress tracking
    val grammarTasksCompleted: Int = 0,
    val grammarPerfectSessions: Int = 0,
    val grammarStreakDays: Int = 0,

    val fastCompletions: Int = 0,
    val averageSpeedRatio: Float = 1f,
    val fastExamCompletions: Int = 0,
    val fastCompletionStreakDays: Int = 0,

    val maxConsecutiveDays: Int = 0,
    val currentStreakDays: Int = 0,
    val totalStudyDays: Int = 0,

    val overallProgressPercent: Float = 0f,
    val weeklyProgressPercent: Float = 0f,
    val programCompletionPercent: Float = 0f
)

@Serializable
data class AchievementState(
    val categoryProgress: Map<AchievementCategory, CategoryProgress>,
    val unlockedAchievements: Set<String>,
    val totalAchievements: Int,
    val totalPoints: Int,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Serializable
data class CategoryProgress(
    val category: AchievementCategory,
    val achievements: List<CategorizedAchievement>,
    val unlockedCount: Int,
    val totalCount: Int,
    val categoryPoints: Int,
    val nextAchievement: CategorizedAchievement?,
    val completionPercentage: Float
) {
    val isCompleted: Boolean = unlockedCount == totalCount
    val currentTier: AchievementTier? = achievements
        .filter { it.isUnlocked }
        .maxByOrNull { it.tier.ordinal }?.tier
}

data class AchievementUnlock(
    val achievement: CategorizedAchievement,
    val isNewTier: Boolean,
    val isNewCategory: Boolean,
    val pointsEarned: Int,
    val totalCategoryPoints: Int,
    val timestampMillis: Long = System.currentTimeMillis()
)
/**
 * Vocabulary Learning System Data Models
 */
enum class VocabCategory(val displayName: String) {
    ACADEMIC("Academic"),
    EVERYDAY("Everyday"),
    BUSINESS("Business"),
    EXAM_SPECIFIC("Exam Specific"),
    GRAMMAR_FOCUSED("Grammar Focused")
}

@Serializable
data class VocabularyItem(
    val word: String,
    val definition: String,
    val difficulty: Int, // 1-5 scale
    val category: VocabCategory,
    val contexts: List<String>,
    val relatedWords: List<String>, // synonyms, antonyms, word family
    val grammarPattern: String?, // "used_with_gerunds", "followed_by_infinitive"
    val masteryLevel: Float = 0.0f, // 0.0 - 1.0
    val lastEncountered: Long = 0L,
    val errorCount: Int = 0,
    val successRate: Float = 0.0f,
    val weekIntroduced: Int // Which week in 30-week plan
)

enum class ReviewDifficulty {
    EASY,
    MEDIUM,
    HARD
}

@Serializable
data class VocabularyProgress(
    val wordId: String,
    val masteryLevel: Float,
    val lastReviewDate: Long,
    val nextReviewDate: Long,
    val reviewCount: Int,
    val successCount: Int,
    val errorCount: Int,
    val currentInterval: Int // days until next review
) {
    val successRate: Float = if (reviewCount > 0) successCount.toFloat() / reviewCount.toFloat() else 0.0f
}

@Serializable
data class VocabularySession(
    val sessionId: String,
    val vocabularyItems: List<String>, // word IDs
    val startTime: Long,
    val endTime: Long? = null,
    val correctAnswers: Int = 0,
    val totalQuestions: Int = 0,
    val sessionType: String // "review", "new_words", "weak_areas"
) {
    val accuracy: Float = if (totalQuestions > 0) correctAnswers.toFloat() / totalQuestions.toFloat() else 0.0f
    val isCompleted: Boolean = endTime != null
}

//endregion
