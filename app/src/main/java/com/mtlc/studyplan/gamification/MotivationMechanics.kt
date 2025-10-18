package com.mtlc.studyplan.gamification

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable

/**
 * Motivation Mechanics System - Challenges, comebacks, and progress visualization
 */

@Serializable
data class DailyChallenge(
    val id: String,
    val date: String,
    val type: ChallengeType,
    val title: String,
    val description: String,
    val targetValue: Int,
    val currentProgress: Int = 0,
    val pointsReward: Int,
    val bonusMultiplier: Float = 1.5f,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val difficulty: ChallengeDifficulty = ChallengeDifficulty.MEDIUM
)

enum class ChallengeType(
    val displayName: String,
    val icon: String,
    val color: Color
)

enum class ChallengeDifficulty(
    val displayName: String,
    val color: Color
) {
    EASY("Easy", Color(0xFF4CAF50)),
    MEDIUM("Medium", Color(0xFFFF9800)),
    HARD("Hard", Color(0xFFE91E63)),
    EXPERT("Expert", Color(0xFF9C27B0)),
    LEGENDARY("Legendary", Color(0xFFFFD700))
}

@Serializable
data class WeeklyChallenge(
    val id: String,
    val weekStart: String,
    val type: ChallengeType,
    val title: String,
    val description: String,
    val targetValue: Int,
    val currentProgress: Int = 0,
    val pointsReward: Int,
    val isCompleted: Boolean = false,
    val milestones: List<ChallengeMilestone>,
    val difficulty: ChallengeDifficulty = ChallengeDifficulty.MEDIUM
)

@Serializable
data class ChallengeMilestone(
    val progress: Int,
    val reward: String,
    val pointsBonus: Int,
    val isUnlocked: Boolean = false
)

/**
 * Comeback Mechanics for broken streaks
 */
@Serializable
data class ComebackBonus(
    val type: ComebackType,
    val title: String,
    val description: String,
    val multiplier: Float,
    val duration: ComebackDuration,
    val isActive: Boolean = false,
    val activatedAt: Long? = null
)

enum class ComebackType(
    val displayName: String,
    val icon: String,
    val color: Color
)

enum class ComebackDuration(val days: Int, val displayName: String)

/**
 * Progress Visualization with Level-Up Metaphors
 */
@Serializable
data class LevelSystem(
    val currentLevel: Int,
    val currentXP: Long,
    val xpToNextLevel: Long,
    val totalXP: Long,
    val levelTitle: String,
    val nextLevelTitle: String,
    val levelBenefits: List<String>,
    val prestigeLevel: Int = 0
)

/**
 * Study Buddy Comparison Generator
 */
/**
 * Level System Calculator
 */
object LevelSystemCalculator {

    private val levelTitles = listOf(
        "Newcomer", "Learner", "Student", "Dedicated", "Focused",
        "Committed", "Advanced", "Expert", "Master", "Grandmaster",
        "Legend", "Mythic Scholar", "Transcendent", "Omniscient", "Eternal"
    )

    fun calculateLevel(totalXP: Long): LevelSystem {
        val level = calculateLevelFromXP(totalXP)
        val currentLevelXP = getXPRequiredForLevel(level)
        val nextLevelXP = getXPRequiredForLevel(level + 1)
        val xpInCurrentLevel = totalXP - currentLevelXP
        val xpToNextLevel = nextLevelXP - totalXP

        val prestigeLevel = level / 15 // Prestige every 15 levels

        return LevelSystem(
            currentLevel = level,
            currentXP = xpInCurrentLevel,
            xpToNextLevel = xpToNextLevel,
            totalXP = totalXP,
            levelTitle = getLevelTitle(level),
            nextLevelTitle = getLevelTitle(level + 1),
            levelBenefits = getLevelBenefits(level),
            prestigeLevel = prestigeLevel
        )
    }

    private fun calculateLevelFromXP(totalXP: Long): Int {
        // Exponential XP curve: XP = level^2 * 1000
        return kotlin.math.sqrt(totalXP.toDouble() / 1000.0).toInt().coerceAtLeast(1)
    }

    private fun getXPRequiredForLevel(level: Int): Long {
        return (level * level * 1000L).coerceAtLeast(0L)
    }

    private fun getLevelTitle(level: Int): String {
        val baseIndex = (level - 1) % levelTitles.size
        val prestigeMultiplier = (level - 1) / levelTitles.size

        return if (prestigeMultiplier > 0) {
            "⭐".repeat(prestigeMultiplier) + " " + levelTitles[baseIndex]
        } else {
            levelTitles[baseIndex]
        }
    }

    private fun getLevelBenefits(level: Int): List<String> {
        val benefits = mutableListOf<String>()

        when {
            level >= 50 -> benefits.add("🏆 Legendary status - Maximum celebration intensity")
            level >= 25 -> benefits.add("💎 Master tier - Enhanced particle effects")
            level >= 15 -> benefits.add("⚡ Expert level - Speed bonus multipliers")
            level >= 10 -> benefits.add("🌟 Advanced perks - Exclusive themes available")
            level >= 5 -> benefits.add("🎨 Student benefits - Custom celebration styles")
        }

        if (level % 10 == 0) {
            benefits.add("🎁 Milestone reward - Special cosmetic unlock!")
        }

        return benefits
    }
}

