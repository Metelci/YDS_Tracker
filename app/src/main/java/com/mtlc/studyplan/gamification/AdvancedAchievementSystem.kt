package com.mtlc.studyplan.gamification

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.mtlc.studyplan.data.*
import com.mtlc.studyplan.ui.celebrations.CelebrationIntensity
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlin.math.*

/**
 * Advanced Achievement System - Enhanced with progress tracking, predictions, and hidden achievements
 */

@Serializable
data class AdvancedAchievement(
    val id: String,
    val category: AchievementCategory,
    val tier: AchievementTier,
    val title: String,
    val description: String,
    val targetValue: Int,
    val pointsReward: Int,
    val isHidden: Boolean = false,
    val hiddenHint: String = "",
    val prerequisites: List<String> = emptyList(),
    val isUnlocked: Boolean = false,
    val currentProgress: Int = 0,
    val unlockedDate: Long? = null,
    val rarity: AchievementRarity = AchievementRarity.COMMON,
    val specialReward: SpecialReward? = null
) {
    val progressPercentage: Float
        get() = if (targetValue > 0) (currentProgress.toFloat() / targetValue.toFloat()).coerceIn(0f, 1f) else 0f

    val fullTitle: String
        get() = "${tier.title} ${title}"

    val isVisible: Boolean
        get() = !isHidden || currentProgress > 0 || isUnlocked

    val estimatedTimeToUnlock: EstimatedTime?
        get() = if (isUnlocked) null else calculateEstimatedTime()

    private fun calculateEstimatedTime(): EstimatedTime? {
        val remaining = targetValue - currentProgress
        if (remaining <= 0) return EstimatedTime.READY_TO_UNLOCK

        // This would use historical data to estimate completion time
        return when (category) {
            AchievementCategory.GRAMMAR_MASTER -> EstimatedTime.estimateByDailyRate(remaining, 3) // 3 grammar tasks/day avg
            AchievementCategory.SPEED_DEMON -> EstimatedTime.estimateByDailyRate(remaining, 2) // 2 speed tasks/day avg
            AchievementCategory.CONSISTENCY_CHAMPION -> EstimatedTime.DAYS(remaining) // 1:1 for streak days
            AchievementCategory.PROGRESS_PIONEER -> EstimatedTime.estimateByWeeklyRate(remaining, 5) // 5% progress/week avg
        }
    }
}

enum class AchievementRarity(
    val displayName: String,
    val color: Color,
    val glowEffect: Boolean,
    val pointsMultiplier: Float
) {
    COMMON("Common", Color(0xFF9E9E9E), false, 1.0f),
    UNCOMMON("Uncommon", Color(0xFF4CAF50), false, 1.5f),
    RARE("Rare", Color(0xFF2196F3), true, 2.0f),
    EPIC("Epic", Color(0xFF9C27B0), true, 3.0f),
    LEGENDARY("Legendary", Color(0xFFFF9800), true, 5.0f),
    MYTHIC("Mythic", Color(0xFFE91E63), true, 10.0f)
}

@Serializable
data class SpecialReward(
    val type: SpecialRewardType,
    val value: String,
    val description: String
) {
    enum class SpecialRewardType {
        COSMETIC_UNLOCK,
        TITLE_UNLOCK,
        FEATURE_UNLOCK,
        BONUS_MULTIPLIER,
        EXCLUSIVE_CELEBRATION
    }
}

sealed class EstimatedTime {
    object READY_TO_UNLOCK : EstimatedTime()
    data class DAYS(val days: Int) : EstimatedTime()
    data class WEEKS(val weeks: Int) : EstimatedTime()
    data class MONTHS(val months: Int) : EstimatedTime()

    companion object {
        fun estimateByDailyRate(remaining: Int, dailyRate: Int): EstimatedTime {
            val days = (remaining.toFloat() / dailyRate).roundToInt()
            return when {
                days <= 7 -> DAYS(days)
                days <= 30 -> WEEKS(days / 7)
                else -> MONTHS(days / 30)
            }
        }

        fun estimateByWeeklyRate(remaining: Int, weeklyRate: Int): EstimatedTime {
            val weeks = (remaining.toFloat() / weeklyRate).roundToInt()
            return when {
                weeks <= 4 -> WEEKS(weeks)
                else -> MONTHS(weeks / 4)
            }
        }
    }

    override fun toString(): String = when (this) {
        READY_TO_UNLOCK -> "Ready to unlock!"
        is DAYS -> "${days}d remaining"
        is WEEKS -> "${weeks}w remaining"
        is MONTHS -> "${months}mo remaining"
    }
}

/**
 * Achievement Progress Predictor
 */
data class AchievementPrediction(
    val achievement: AdvancedAchievement,
    val estimatedDaysToComplete: Int,
    val confidence: Float, // 0.0 to 1.0
    val recommendedActions: List<String>,
    val milestoneAlerts: List<MilestoneAlert>
)

data class MilestoneAlert(
    val atProgress: Int,
    val message: String,
    val celebrationIntensity: CelebrationIntensity
)

/**
 * Enhanced Achievement Categories with detailed progressions
 */
object AdvancedAchievementDataSource {

    val allAdvancedAchievements: List<AdvancedAchievement> = listOf(
        // GRAMMAR MASTER - Enhanced progression
        AdvancedAchievement(
            id = "grammar_apprentice",
            category = AchievementCategory.GRAMMAR_MASTER,
            tier = AchievementTier.BRONZE,
            title = "Grammar Apprentice",
            description = "Master the basics with 25 grammar tasks",
            targetValue = 25,
            pointsReward = 100,
            rarity = AchievementRarity.COMMON
        ),
        AdvancedAchievement(
            id = "grammar_scholar",
            category = AchievementCategory.GRAMMAR_MASTER,
            tier = AchievementTier.BRONZE,
            title = "Grammar Scholar",
            description = "Complete 50 grammar tasks",
            targetValue = 50,
            pointsReward = 200,
            rarity = AchievementRarity.COMMON,
            prerequisites = listOf("grammar_apprentice")
        ),
        AdvancedAchievement(
            id = "grammar_expert",
            category = AchievementCategory.GRAMMAR_MASTER,
            tier = AchievementTier.SILVER,
            title = "Grammar Expert",
            description = "Complete 150 grammar tasks with 90% accuracy",
            targetValue = 150,
            pointsReward = 500,
            rarity = AchievementRarity.UNCOMMON,
            prerequisites = listOf("grammar_scholar")
        ),
        AdvancedAchievement(
            id = "grammar_perfectionist",
            category = AchievementCategory.GRAMMAR_MASTER,
            tier = AchievementTier.GOLD,
            title = "Grammar Perfectionist",
            description = "Achieve 10 perfect grammar sessions",
            targetValue = 10,
            pointsReward = 1000,
            rarity = AchievementRarity.RARE
        ),
        AdvancedAchievement(
            id = "grammar_virtuoso",
            category = AchievementCategory.GRAMMAR_MASTER,
            tier = AchievementTier.PLATINUM,
            title = "Grammar Virtuoso",
            description = "Maintain 30-day grammar streak",
            targetValue = 30,
            pointsReward = 2000,
            rarity = AchievementRarity.EPIC,
            specialReward = SpecialReward(
                SpecialReward.SpecialRewardType.TITLE_UNLOCK,
                "Grammar Guru",
                "Exclusive title for grammar masters"
            )
        ),

        // HIDDEN GRAMMAR ACHIEVEMENTS
        AdvancedAchievement(
            id = "grammar_lightning",
            category = AchievementCategory.GRAMMAR_MASTER,
            tier = AchievementTier.GOLD,
            title = "Lightning Grammarian",
            description = "Complete 100 grammar tasks in under 5 minutes each",
            targetValue = 100,
            pointsReward = 1500,
            rarity = AchievementRarity.LEGENDARY,
            isHidden = true,
            hiddenHint = "Speed meets precision in grammar mastery"
        ),

        // SPEED DEMON - Enhanced progression
        AdvancedAchievement(
            id = "speed_novice",
            category = AchievementCategory.SPEED_DEMON,
            tier = AchievementTier.BRONZE,
            title = "Speed Novice",
            description = "Complete 5 tasks faster than estimated time",
            targetValue = 5,
            pointsReward = 100,
            rarity = AchievementRarity.COMMON
        ),
        AdvancedAchievement(
            id = "speed_racer",
            category = AchievementCategory.SPEED_DEMON,
            tier = AchievementTier.BRONZE,
            title = "Speed Racer",
            description = "Complete 25 tasks under time",
            targetValue = 25,
            pointsReward = 250,
            rarity = AchievementRarity.COMMON,
            prerequisites = listOf("speed_novice")
        ),
        AdvancedAchievement(
            id = "swift_solver",
            category = AchievementCategory.SPEED_DEMON,
            tier = AchievementTier.SILVER,
            title = "Swift Solver",
            description = "Average 25% faster than estimated time over 50 tasks",
            targetValue = 50,
            pointsReward = 600,
            rarity = AchievementRarity.UNCOMMON
        ),
        AdvancedAchievement(
            id = "lightning_finisher",
            category = AchievementCategory.SPEED_DEMON,
            tier = AchievementTier.GOLD,
            title = "Lightning Finisher",
            description = "Complete practice exam 20 minutes early",
            targetValue = 1,
            pointsReward = 1200,
            rarity = AchievementRarity.RARE
        ),
        AdvancedAchievement(
            id = "speed_legend",
            category = AchievementCategory.SPEED_DEMON,
            tier = AchievementTier.PLATINUM,
            title = "Speed Legend",
            description = "Maintain 50% speed bonus for 30 days",
            targetValue = 30,
            pointsReward = 2500,
            rarity = AchievementRarity.EPIC,
            specialReward = SpecialReward(
                SpecialReward.SpecialRewardType.BONUS_MULTIPLIER,
                "1.5x_speed_bonus",
                "Permanent 50% speed bonus multiplier"
            )
        ),

        // HIDDEN SPEED ACHIEVEMENTS
        AdvancedAchievement(
            id = "speed_demon_ultimate",
            category = AchievementCategory.SPEED_DEMON,
            tier = AchievementTier.PLATINUM,
            title = "Ultimate Speed Demon",
            description = "Complete entire weekly plan in one day",
            targetValue = 1,
            pointsReward = 5000,
            rarity = AchievementRarity.MYTHIC,
            isHidden = true,
            hiddenHint = "When one day contains a week's worth of dedication"
        ),

        // CONSISTENCY CHAMPION - Enhanced progression
        AdvancedAchievement(
            id = "consistency_starter",
            category = AchievementCategory.CONSISTENCY_CHAMPION,
            tier = AchievementTier.BRONZE,
            title = "Consistency Starter",
            description = "3-day study streak",
            targetValue = 3,
            pointsReward = 50,
            rarity = AchievementRarity.COMMON
        ),
        AdvancedAchievement(
            id = "week_warrior",
            category = AchievementCategory.CONSISTENCY_CHAMPION,
            tier = AchievementTier.BRONZE,
            title = "Week Warrior",
            description = "7-day study streak",
            targetValue = 7,
            pointsReward = 150,
            rarity = AchievementRarity.COMMON,
            prerequisites = listOf("consistency_starter")
        ),
        AdvancedAchievement(
            id = "month_master",
            category = AchievementCategory.CONSISTENCY_CHAMPION,
            tier = AchievementTier.SILVER,
            title = "Month Master",
            description = "30-day study streak",
            targetValue = 30,
            pointsReward = 500,
            rarity = AchievementRarity.UNCOMMON
        ),
        AdvancedAchievement(
            id = "century_scholar",
            category = AchievementCategory.CONSISTENCY_CHAMPION,
            tier = AchievementTier.GOLD,
            title = "Century Scholar",
            description = "100-day study streak",
            targetValue = 100,
            pointsReward = 1500,
            rarity = AchievementRarity.RARE
        ),
        AdvancedAchievement(
            id = "dedication_deity",
            category = AchievementCategory.CONSISTENCY_CHAMPION,
            tier = AchievementTier.PLATINUM,
            title = "Dedication Deity",
            description = "365-day study streak",
            targetValue = 365,
            pointsReward = 5000,
            rarity = AchievementRarity.LEGENDARY,
            specialReward = SpecialReward(
                SpecialReward.SpecialRewardType.EXCLUSIVE_CELEBRATION,
                "golden_phoenix",
                "Exclusive golden phoenix celebration"
            )
        ),

        // HIDDEN CONSISTENCY ACHIEVEMENTS
        AdvancedAchievement(
            id = "comeback_king",
            category = AchievementCategory.CONSISTENCY_CHAMPION,
            tier = AchievementTier.GOLD,
            title = "Comeback King",
            description = "Rebuild a 50+ day streak after breaking a 100+ day streak",
            targetValue = 50,
            pointsReward = 2000,
            rarity = AchievementRarity.EPIC,
            isHidden = true,
            hiddenHint = "Sometimes falling down makes the climb back up even more meaningful"
        ),

        // PROGRESS PIONEER - Enhanced progression
        AdvancedAchievement(
            id = "progress_starter",
            category = AchievementCategory.PROGRESS_PIONEER,
            tier = AchievementTier.BRONZE,
            title = "Progress Starter",
            description = "Reach 10% overall progress",
            targetValue = 10,
            pointsReward = 100,
            rarity = AchievementRarity.COMMON
        ),
        AdvancedAchievement(
            id = "quarter_climber",
            category = AchievementCategory.PROGRESS_PIONEER,
            tier = AchievementTier.BRONZE,
            title = "Quarter Climber",
            description = "Reach 25% overall progress",
            targetValue = 25,
            pointsReward = 300,
            rarity = AchievementRarity.COMMON,
            prerequisites = listOf("progress_starter")
        ),
        AdvancedAchievement(
            id = "halfway_hero",
            category = AchievementCategory.PROGRESS_PIONEER,
            tier = AchievementTier.SILVER,
            title = "Halfway Hero",
            description = "Reach 50% overall progress",
            targetValue = 50,
            pointsReward = 750,
            rarity = AchievementRarity.UNCOMMON
        ),
        AdvancedAchievement(
            id = "three_quarter_titan",
            category = AchievementCategory.PROGRESS_PIONEER,
            tier = AchievementTier.GOLD,
            title = "Three-Quarter Titan",
            description = "Reach 75% overall progress",
            targetValue = 75,
            pointsReward = 1250,
            rarity = AchievementRarity.RARE
        ),
        AdvancedAchievement(
            id = "program_perfectionist",
            category = AchievementCategory.PROGRESS_PIONEER,
            tier = AchievementTier.PLATINUM,
            title = "Program Perfectionist",
            description = "Complete entire 30-week program",
            targetValue = 100,
            pointsReward = 3000,
            rarity = AchievementRarity.LEGENDARY,
            specialReward = SpecialReward(
                SpecialReward.SpecialRewardType.TITLE_UNLOCK,
                "Master Graduate",
                "Elite title for program completers"
            )
        ),

        // HIDDEN CROSS-CATEGORY ACHIEVEMENTS
        AdvancedAchievement(
            id = "grand_slam",
            category = AchievementCategory.PROGRESS_PIONEER,
            tier = AchievementTier.PLATINUM,
            title = "Grand Slam Champion",
            description = "Unlock all Bronze achievements across all categories",
            targetValue = 8, // Number of Bronze achievements
            pointsReward = 2500,
            rarity = AchievementRarity.LEGENDARY,
            isHidden = true,
            hiddenHint = "Master the fundamentals across all disciplines"
        ),

        AdvancedAchievement(
            id = "perfectionist_supreme",
            category = AchievementCategory.PROGRESS_PIONEER,
            tier = AchievementTier.PLATINUM,
            title = "Perfectionist Supreme",
            description = "Unlock all achievements in the app",
            targetValue = 25, // Total number of achievements
            pointsReward = 10000,
            rarity = AchievementRarity.MYTHIC,
            isHidden = true,
            hiddenHint = "The ultimate goal of true dedication",
            specialReward = SpecialReward(
                SpecialReward.SpecialRewardType.EXCLUSIVE_CELEBRATION,
                "mythic_ascension",
                "Legendary ascension celebration with cosmic effects"
            )
        )
    )

    fun getAchievementsByCategory(category: AchievementCategory): List<AdvancedAchievement> {
        return allAdvancedAchievements.filter { it.category == category }
            .sortedWith(compareBy({ it.tier.ordinal }, { it.targetValue }))
    }

    fun getVisibleAchievements(): List<AdvancedAchievement> {
        return allAdvancedAchievements.filter { it.isVisible }
    }

    fun getHiddenAchievements(): List<AdvancedAchievement> {
        return allAdvancedAchievements.filter { it.isHidden && !it.isUnlocked }
    }

    fun getNextAchievementInCategory(
        category: AchievementCategory,
        unlockedIds: Set<String>
    ): AdvancedAchievement? {
        return getAchievementsByCategory(category)
            .firstOrNull { it.id !in unlockedIds && canUnlock(it, unlockedIds) }
    }

    private fun canUnlock(achievement: AdvancedAchievement, unlockedIds: Set<String>): Boolean {
        return achievement.prerequisites.all { it in unlockedIds }
    }

    fun getAchievementById(id: String): AdvancedAchievement? {
        return allAdvancedAchievements.find { it.id == id }
    }
}

/**
 * Achievement Condition Functions
 */
object AdvancedAchievementConditions {

    fun getCondition(achievementId: String): (AchievementProgress) -> Boolean {
        return when (achievementId) {
            // Grammar Master conditions
            "grammar_apprentice" -> { progress -> progress.grammarTasksCompleted >= 25 }
            "grammar_scholar" -> { progress -> progress.grammarTasksCompleted >= 50 }
            "grammar_expert" -> { progress ->
                progress.grammarTasksCompleted >= 150 &&
                calculateGrammarAccuracy(progress) >= 0.9f
            }
            "grammar_perfectionist" -> { progress -> progress.grammarPerfectSessions >= 10 }
            "grammar_virtuoso" -> { progress -> progress.grammarStreakDays >= 30 }
            "grammar_lightning" -> { progress ->
                calculateFastGrammarTasks(progress) >= 100
            }

            // Speed Demon conditions
            "speed_novice" -> { progress -> progress.fastCompletions >= 5 }
            "speed_racer" -> { progress -> progress.fastCompletions >= 25 }
            "swift_solver" -> { progress ->
                progress.fastCompletions >= 50 &&
                progress.averageSpeedRatio <= 0.75f
            }
            "lightning_finisher" -> { progress -> progress.fastExamCompletions >= 1 }
            "speed_legend" -> { progress -> progress.fastCompletionStreakDays >= 30 }
            "speed_demon_ultimate" -> { progress ->
                calculateWeeklyTasksInDay(progress) >= 35
            }

            // Consistency Champion conditions
            "consistency_starter" -> { progress -> progress.currentStreakDays >= 3 }
            "week_warrior" -> { progress -> progress.currentStreakDays >= 7 }
            "month_master" -> { progress -> progress.currentStreakDays >= 30 }
            "century_scholar" -> { progress -> progress.currentStreakDays >= 100 }
            "dedication_deity" -> { progress -> progress.currentStreakDays >= 365 }
            "comeback_king" -> { progress ->
                checkComebackCondition(progress)
            }

            // Progress Pioneer conditions
            "progress_starter" -> { progress -> progress.overallProgressPercent >= 10f }
            "quarter_climber" -> { progress -> progress.overallProgressPercent >= 25f }
            "halfway_hero" -> { progress -> progress.overallProgressPercent >= 50f }
            "three_quarter_titan" -> { progress -> progress.overallProgressPercent >= 75f }
            "program_perfectionist" -> { progress -> progress.programCompletionPercent >= 100f }

            // Hidden cross-category conditions
            "grand_slam" -> { progress -> calculateBronzeAchievements(progress) >= 8 }
            "perfectionist_supreme" -> { progress -> calculateAllAchievements(progress) >= 25 }

            // Default
            else -> { _ -> false }
        }
    }

    // Helper functions for complex conditions
    private fun calculateGrammarAccuracy(progress: AchievementProgress): Float {
        val grammarLogs = progress.taskLogs.filter { isGrammarTask(it.category) }
        if (grammarLogs.isEmpty()) return 0f
        val correctCount = grammarLogs.count { it.correct }
        return correctCount.toFloat() / grammarLogs.size
    }

    private fun calculateFastGrammarTasks(progress: AchievementProgress): Int {
        return progress.taskLogs.count {
            isGrammarTask(it.category) && it.minutesSpent < 5
        }
    }

    private fun calculateWeeklyTasksInDay(progress: AchievementProgress): Int {
        val todayStart = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
        return progress.taskLogs.count { it.timestampMillis >= todayStart }
    }

    private fun checkComebackCondition(progress: AchievementProgress): Boolean {
        // This would need streak history data to check for the comeback pattern
        // Simplified implementation
        return progress.currentStreakDays >= 50 && progress.maxConsecutiveDays >= 100
    }

    private fun calculateBronzeAchievements(progress: AchievementProgress): Int {
        return progress.userProgress.unlockedAchievements.count { achievementId ->
            AdvancedAchievementDataSource.getAchievementById(achievementId)?.tier == AchievementTier.BRONZE
        }
    }

    private fun calculateAllAchievements(progress: AchievementProgress): Int {
        return progress.userProgress.unlockedAchievements.size
    }

    private fun isGrammarTask(category: String): Boolean {
        return category.lowercase().contains("grammar") || category.lowercase().contains("gramer")
    }
}

/**
 * Achievement Progress Tracker with predictions
 */
class AdvancedAchievementTracker(
    private val dataStore: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>,
    private val progressRepository: ProgressRepository
) {

    private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

    val advancedAchievementFlow: Flow<List<AdvancedAchievement>> = combine(
        progressRepository.userProgressFlow,
        progressRepository.taskLogsFlow
    ) { userProgress, taskLogs ->
        calculateAdvancedAchievementProgress(userProgress, taskLogs)
    }

    private fun calculateAdvancedAchievementProgress(
        userProgress: UserProgress,
        taskLogs: List<TaskLog>
    ): List<AdvancedAchievement> {
        val achievementProgress = AchievementProgress(
            userProgress = userProgress,
            taskLogs = taskLogs,
            streakState = StreakState(userProgress.streakCount, StreakMultiplier.getMultiplierForStreak(userProgress.streakCount))
            // ... other progress calculations
        )

        return AdvancedAchievementDataSource.allAdvancedAchievements.map { achievement ->
            val currentProgress = calculateSpecificProgress(achievement, achievementProgress)
            val isUnlocked = userProgress.unlockedAchievements.contains(achievement.id) ||
                             AdvancedAchievementConditions.getCondition(achievement.id)(achievementProgress)

            achievement.copy(
                currentProgress = currentProgress,
                isUnlocked = isUnlocked,
                unlockedDate = if (isUnlocked && achievement.unlockedDate == null) System.currentTimeMillis() else achievement.unlockedDate
            )
        }
    }

    private fun calculateSpecificProgress(achievement: AdvancedAchievement, progress: AchievementProgress): Int {
        return when (achievement.id) {
            "grammar_apprentice", "grammar_scholar", "grammar_expert" -> progress.grammarTasksCompleted
            "grammar_perfectionist" -> progress.grammarPerfectSessions
            "grammar_virtuoso" -> progress.grammarStreakDays
            "speed_novice", "speed_racer" -> progress.fastCompletions
            "lightning_finisher" -> progress.fastExamCompletions
            "consistency_starter", "week_warrior", "month_master", "century_scholar", "dedication_deity" -> progress.currentStreakDays
            "progress_starter", "quarter_climber", "halfway_hero", "three_quarter_titan" -> progress.overallProgressPercent.toInt()
            "program_perfectionist" -> progress.programCompletionPercent.toInt()
            else -> 0
        }
    }

    /**
     * Generate achievement predictions
     */
    fun generatePredictions(achievements: List<AdvancedAchievement>): List<AchievementPrediction> {
        return achievements
            .filter { !it.isUnlocked && it.isVisible }
            .mapNotNull { achievement ->
                achievement.estimatedTimeToUnlock?.let { estimate ->
                    AchievementPrediction(
                        achievement = achievement,
                        estimatedDaysToComplete = when (estimate) {
                            is EstimatedTime.DAYS -> estimate.days
                            is EstimatedTime.WEEKS -> estimate.weeks * 7
                            is EstimatedTime.MONTHS -> estimate.months * 30
                            EstimatedTime.READY_TO_UNLOCK -> 0
                        },
                        confidence = calculateConfidence(achievement),
                        recommendedActions = generateRecommendations(achievement),
                        milestoneAlerts = generateMilestoneAlerts(achievement)
                    )
                }
            }
            .sortedBy { it.estimatedDaysToComplete }
    }

    private fun calculateConfidence(achievement: AdvancedAchievement): Float {
        // Confidence based on current progress and historical patterns
        return when {
            achievement.progressPercentage > 0.8f -> 0.9f
            achievement.progressPercentage > 0.5f -> 0.7f
            achievement.progressPercentage > 0.2f -> 0.5f
            else -> 0.3f
        }
    }

    private fun generateRecommendations(achievement: AdvancedAchievement): List<String> {
        return when (achievement.category) {
            AchievementCategory.GRAMMAR_MASTER -> listOf(
                "Focus on daily grammar exercises",
                "Aim for 90%+ accuracy on grammar tasks",
                "Practice complex grammar structures"
            )
            AchievementCategory.SPEED_DEMON -> listOf(
                "Time yourself on practice tasks",
                "Focus on quick decision making",
                "Practice speed reading techniques"
            )
            AchievementCategory.CONSISTENCY_CHAMPION -> listOf(
                "Set daily study reminders",
                "Start with shorter sessions if struggling",
                "Track your streak progress daily"
            )
            AchievementCategory.PROGRESS_PIONEER -> listOf(
                "Complete tasks in order of difficulty",
                "Focus on understanding over speed",
                "Review completed sections regularly"
            )
        }
    }

    private fun generateMilestoneAlerts(achievement: AdvancedAchievement): List<MilestoneAlert> {
        val quarterMark = achievement.targetValue / 4
        val halfMark = achievement.targetValue / 2
        val threeQuarterMark = (achievement.targetValue * 0.75).toInt()

        return listOf(
            MilestoneAlert(quarterMark, "Quarter way to ${achievement.title}!", CelebrationIntensity.SUBTLE),
            MilestoneAlert(halfMark, "Halfway to ${achievement.title}!", CelebrationIntensity.MODERATE),
            MilestoneAlert(threeQuarterMark, "Almost there! ${achievement.title} within reach!", CelebrationIntensity.HIGH)
        )
    }
}