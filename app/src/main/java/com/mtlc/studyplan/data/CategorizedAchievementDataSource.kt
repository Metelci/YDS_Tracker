@file:Suppress("CyclomaticComplexMethod")
package com.mtlc.studyplan.data

/**
 * Comprehensive categorized achievement system
 * Defines all achievements organized by category and tier
 */
object CategorizedAchievementDataSource {

    /**
     * All categorized achievements organized by category and tier
     */
    val allCategorizedAchievements: List<CategorizedAchievement> = listOf(
        // GRAMMAR MASTER CATEGORY
        CategorizedAchievement(
            id = "grammar_bronze",
            category = AchievementCategory.GRAMMAR_MASTER,
            tier = AchievementTier.BRONZE,
            title = "Grammar Novice",
            description = "Complete 50 grammar tasks",
            targetValue = 50,
            pointsReward = 100
        ),
        CategorizedAchievement(
            id = "grammar_silver",
            category = AchievementCategory.GRAMMAR_MASTER,
            tier = AchievementTier.SILVER,
            title = "Grammar Scholar",
            description = "Complete 150 grammar tasks",
            targetValue = 150,
            pointsReward = 300
        ),
        CategorizedAchievement(
            id = "grammar_gold",
            category = AchievementCategory.GRAMMAR_MASTER,
            tier = AchievementTier.GOLD,
            title = "Grammar Expert",
            description = "Perfect score on 10 grammar-focused sessions",
            targetValue = 10,
            pointsReward = 500
        ),
        CategorizedAchievement(
            id = "grammar_platinum",
            category = AchievementCategory.GRAMMAR_MASTER,
            tier = AchievementTier.PLATINUM,
            title = "Grammar Virtuoso",
            description = "30-day grammar streak",
            targetValue = 30,
            pointsReward = 1000
        ),

        // SPEED DEMON CATEGORY
        CategorizedAchievement(
            id = "speed_bronze",
            category = AchievementCategory.SPEED_DEMON,
            tier = AchievementTier.BRONZE,
            title = "Quick Starter",
            description = "Complete task in under estimated time 10 times",
            targetValue = 10,
            pointsReward = 100
        ),
        CategorizedAchievement(
            id = "speed_silver",
            category = AchievementCategory.SPEED_DEMON,
            tier = AchievementTier.SILVER,
            title = "Swift Solver",
            description = "Average 20% faster than estimated time over week",
            targetValue = 80, // 80% of estimated time = 20% faster
            pointsReward = 250
        ),
        CategorizedAchievement(
            id = "speed_gold",
            category = AchievementCategory.SPEED_DEMON,
            tier = AchievementTier.GOLD,
            title = "Lightning Finisher",
            description = "Complete full practice exam 15 minutes early",
            targetValue = 1,
            pointsReward = 400
        ),
        CategorizedAchievement(
            id = "speed_platinum",
            category = AchievementCategory.SPEED_DEMON,
            tier = AchievementTier.PLATINUM,
            title = "Speed Legend",
            description = "Maintain fast completion rate for 30 days",
            targetValue = 30,
            pointsReward = 800
        ),

        // CONSISTENCY CHAMPION CATEGORY
        CategorizedAchievement(
            id = "consistency_bronze",
            category = AchievementCategory.CONSISTENCY_CHAMPION,
            tier = AchievementTier.BRONZE,
            title = "Week Warrior",
            description = "7-day study streak",
            targetValue = 7,
            pointsReward = 150
        ),
        CategorizedAchievement(
            id = "consistency_silver",
            category = AchievementCategory.CONSISTENCY_CHAMPION,
            tier = AchievementTier.SILVER,
            title = "Month Master",
            description = "30-day study streak",
            targetValue = 30,
            pointsReward = 400
        ),
        CategorizedAchievement(
            id = "consistency_gold",
            category = AchievementCategory.CONSISTENCY_CHAMPION,
            tier = AchievementTier.GOLD,
            title = "Century Scholar",
            description = "100-day study streak",
            targetValue = 100,
            pointsReward = 1000
        ),
        CategorizedAchievement(
            id = "consistency_platinum",
            category = AchievementCategory.CONSISTENCY_CHAMPION,
            tier = AchievementTier.PLATINUM,
            title = "Dedication Deity",
            description = "Complete tasks every day for 6 months (180 days)",
            targetValue = 180,
            pointsReward = 2000
        ),

        // PROGRESS PIONEER CATEGORY
        CategorizedAchievement(
            id = "progress_bronze",
            category = AchievementCategory.PROGRESS_PIONEER,
            tier = AchievementTier.BRONZE,
            title = "Quarter Climber",
            description = "Reach 25% overall progress",
            targetValue = 25,
            pointsReward = 200
        ),
        CategorizedAchievement(
            id = "progress_silver",
            category = AchievementCategory.PROGRESS_PIONEER,
            tier = AchievementTier.SILVER,
            title = "Halfway Hero",
            description = "Reach 50% overall progress",
            targetValue = 50,
            pointsReward = 500
        ),
        CategorizedAchievement(
            id = "progress_gold",
            category = AchievementCategory.PROGRESS_PIONEER,
            tier = AchievementTier.GOLD,
            title = "Three-Quarter Titan",
            description = "Reach 75% overall progress",
            targetValue = 75,
            pointsReward = 750
        ),
        CategorizedAchievement(
            id = "progress_platinum",
            category = AchievementCategory.PROGRESS_PIONEER,
            tier = AchievementTier.PLATINUM,
            title = "Program Perfectionist",
            description = "Complete entire 30-week program",
            targetValue = 100,
            pointsReward = 1500
        )
    )

    /**
     * Get achievements by category
     */
    fun getAchievementsByCategory(category: AchievementCategory): List<CategorizedAchievement> {
        return allCategorizedAchievements.filter { it.category == category }
            .sortedBy { it.tier.ordinal }
    }

    /**
     * Get achievements by tier
     */
    fun getAchievementsByTier(tier: AchievementTier): List<CategorizedAchievement> {
        return allCategorizedAchievements.filter { it.tier == tier }
            .sortedBy { it.category.ordinal }
    }

    /**
     * Get next achievement in category
     */
    fun getNextAchievementInCategory(
        category: AchievementCategory,
        unlockedAchievements: Set<String>
    ): CategorizedAchievement? {
        return getAchievementsByCategory(category)
            .firstOrNull { it.id !in unlockedAchievements }
    }

    /**
     * Calculate achievement progress for a specific achievement
     */
    fun calculateAchievementProgress(
        achievement: CategorizedAchievement,
        progress: AchievementProgress
    ): Float {
        val currentValue = when (achievement.category) {
            AchievementCategory.GRAMMAR_MASTER -> when (achievement.tier) {
                AchievementTier.BRONZE, AchievementTier.SILVER -> progress.grammarTasksCompleted
                AchievementTier.GOLD -> progress.grammarPerfectSessions
                AchievementTier.PLATINUM -> progress.grammarStreakDays
            }
            AchievementCategory.SPEED_DEMON -> when (achievement.tier) {
                AchievementTier.BRONZE -> progress.fastCompletions
                AchievementTier.SILVER -> (100 - (progress.averageSpeedRatio * 100)).toInt()
                AchievementTier.GOLD -> progress.fastExamCompletions
                AchievementTier.PLATINUM -> progress.fastCompletionStreakDays
            }
            AchievementCategory.CONSISTENCY_CHAMPION -> progress.currentStreakDays
            AchievementCategory.PROGRESS_PIONEER -> when (achievement.tier) {
                AchievementTier.PLATINUM -> progress.programCompletionPercent.toInt()
                else -> progress.overallProgressPercent.toInt()
            }
        }

        return (currentValue.toFloat() / achievement.targetValue.toFloat()).coerceIn(0f, 1f)
    }

    /**
     * Get category statistics
     */
    fun getCategoryStats(category: AchievementCategory): CategoryStats {
        val achievements = getAchievementsByCategory(category)
        val totalPoints = achievements.sumOf { it.pointsReward }

        return CategoryStats(
            category = category,
            totalAchievements = achievements.size,
            totalPossiblePoints = totalPoints,
            tiers = AchievementTier.entries.toList()
        )
    }

    /**
     * Calculate total possible points across all achievements
     */
    val totalPossiblePoints: Int = allCategorizedAchievements.sumOf { it.pointsReward }

    /**
     * Get achievement by ID
     */
    fun getAchievementById(id: String): CategorizedAchievement? {
        return allCategorizedAchievements.find { it.id == id }
    }

    /**
     * Get condition function for achievement ID (for serialization compatibility)
     */
    fun getConditionFunction(achievementId: String): (AchievementProgress) -> Boolean {
        return when (achievementId) {
            // Grammar Master conditions
            "grammar_bronze" -> { progress -> progress.grammarTasksCompleted >= 50 }
            "grammar_silver" -> { progress -> progress.grammarTasksCompleted >= 150 }
            "grammar_gold" -> { progress -> progress.grammarPerfectSessions >= 10 }
            "grammar_platinum" -> { progress -> progress.grammarStreakDays >= 30 }

            // Speed Demon conditions
            "speed_bronze" -> { progress -> progress.fastCompletions >= 10 }
            "speed_silver" -> { progress -> (progress.averageSpeedRatio * 100).toInt() <= 80 }
            "speed_gold" -> { progress -> progress.fastExamCompletions >= 1 }
            "speed_platinum" -> { progress -> progress.fastCompletionStreakDays >= 30 }

            // Consistency Champion conditions
            "consistency_bronze" -> { progress -> progress.currentStreakDays >= 7 }
            "consistency_silver" -> { progress -> progress.currentStreakDays >= 30 }
            "consistency_gold" -> { progress -> progress.currentStreakDays >= 100 }
            "consistency_platinum" -> { progress -> progress.currentStreakDays >= 180 }

            // Progress Pioneer conditions
            "progress_bronze" -> { progress -> progress.overallProgressPercent >= 25f }
            "progress_silver" -> { progress -> progress.overallProgressPercent >= 50f }
            "progress_gold" -> { progress -> progress.overallProgressPercent >= 75f }
            "progress_platinum" -> { progress -> progress.programCompletionPercent >= 100f }

            // Default fallback
            else -> { _ -> false }
        }
    }
}

data class CategoryStats(
    val category: AchievementCategory,
    val totalAchievements: Int,
    val totalPossiblePoints: Int,
    val tiers: List<AchievementTier>
)