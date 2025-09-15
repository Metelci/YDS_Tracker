package com.mtlc.studyplan.gamification

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtlc.studyplan.data.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.random.Random

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
) {
    SPEED_CHALLENGE("Speed Challenge", "⚡", Color(0xFFFF9800)),
    ACCURACY_CHALLENGE("Accuracy Challenge", "🎯", Color(0xFF4CAF50)),
    VOLUME_CHALLENGE("Volume Challenge", "📈", Color(0xFF2196F3)),
    STREAK_CHALLENGE("Streak Challenge", "🔥", Color(0xFFE91E63)),
    CATEGORY_FOCUS("Category Focus", "🎓", Color(0xFF9C27B0)),
    PERFECT_DAY("Perfect Day", "💎", Color(0xFFFFD700))
}

enum class ChallengeDifficulty(
    val displayName: String,
    val color: Color,
    val pointsMultiplier: Float
) {
    EASY("Easy", Color(0xFF4CAF50), 1.0f),
    MEDIUM("Medium", Color(0xFFFF9800), 1.5f),
    HARD("Hard", Color(0xFFE91E63), 2.0f),
    EXPERT("Expert", Color(0xFF9C27B0), 3.0f),
    LEGENDARY("Legendary", Color(0xFFFFD700), 5.0f)
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
) {
    FRESH_START("Fresh Start", "🌅", Color(0xFF4CAF50)),
    PHOENIX_RISING("Phoenix Rising", "🔥", Color(0xFFFF5722)),
    SECOND_CHANCE("Second Chance", "💪", Color(0xFF2196F3)),
    REDEMPTION_ARC("Redemption Arc", "⚡", Color(0xFF9C27B0))
}

enum class ComebackDuration(val days: Int, val displayName: String) {
    SHORT_BOOST(3, "3-day boost"),
    WEEK_BOOST(7, "1-week boost"),
    EXTENDED_BOOST(14, "2-week boost"),
    SUPER_BOOST(30, "1-month boost")
}

/**
 * Study Buddy Comparison System (Anonymous)
 */
@Serializable
data class StudyBuddyComparison(
    val categoryProgress: Map<String, BuddyProgress>,
    val overallRanking: BuddyRanking,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Serializable
data class BuddyProgress(
    val category: String,
    val yourProgress: Float,
    val averageProgress: Float,
    val topPercentile: Float,
    val encouragementMessage: String
)

@Serializable
data class BuddyRanking(
    val percentile: Int, // 0-100
    val rank: String, // "Top 10%", "Above Average", etc.
    val motivationMessage: String
)

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
 * Challenge Generator
 */
object ChallengeGenerator {

    fun generateDailyChallenge(userProgress: UserProgress, taskLogs: List<TaskLog>): DailyChallenge {
        val challengeTypes = ChallengeType.values()
        val selectedType = challengeTypes[Random.nextInt(challengeTypes.size)]

        val difficulty = calculateRecommendedDifficulty(userProgress, taskLogs)

        return when (selectedType) {
            ChallengeType.SPEED_CHALLENGE -> createSpeedChallenge(difficulty, userProgress)
            ChallengeType.ACCURACY_CHALLENGE -> createAccuracyChallenge(difficulty, userProgress)
            ChallengeType.VOLUME_CHALLENGE -> createVolumeChallenge(difficulty, userProgress)
            ChallengeType.STREAK_CHALLENGE -> createStreakChallenge(difficulty, userProgress)
            ChallengeType.CATEGORY_FOCUS -> createCategoryFocusChallenge(difficulty, userProgress)
            ChallengeType.PERFECT_DAY -> createPerfectDayChallenge(difficulty, userProgress)
        }
    }

    private fun createSpeedChallenge(difficulty: ChallengeDifficulty, userProgress: UserProgress): DailyChallenge {
        val targetTasks = when (difficulty) {
            ChallengeDifficulty.EASY -> 3
            ChallengeDifficulty.MEDIUM -> 5
            ChallengeDifficulty.HARD -> 8
            ChallengeDifficulty.EXPERT -> 12
            ChallengeDifficulty.LEGENDARY -> 20
        }

        return DailyChallenge(
            id = "speed_${System.currentTimeMillis()}",
            date = LocalDate.now().toString(),
            type = ChallengeType.SPEED_CHALLENGE,
            title = "Lightning Fast",
            description = "Complete $targetTasks tasks faster than estimated time",
            targetValue = targetTasks,
            pointsReward = (100 * targetTasks * difficulty.pointsMultiplier).toInt(),
            difficulty = difficulty
        )
    }

    private fun createAccuracyChallenge(difficulty: ChallengeDifficulty, userProgress: UserProgress): DailyChallenge {
        val accuracyTarget = when (difficulty) {
            ChallengeDifficulty.EASY -> 80
            ChallengeDifficulty.MEDIUM -> 90
            ChallengeDifficulty.HARD -> 95
            ChallengeDifficulty.EXPERT -> 98
            ChallengeDifficulty.LEGENDARY -> 100
        }

        return DailyChallenge(
            id = "accuracy_${System.currentTimeMillis()}",
            date = LocalDate.now().toString(),
            type = ChallengeType.ACCURACY_CHALLENGE,
            title = "Precision Master",
            description = "Achieve $accuracyTarget% accuracy on all tasks today",
            targetValue = accuracyTarget,
            pointsReward = (200 * difficulty.pointsMultiplier).toInt(),
            difficulty = difficulty
        )
    }

    private fun createVolumeChallenge(difficulty: ChallengeDifficulty, userProgress: UserProgress): DailyChallenge {
        val taskCount = when (difficulty) {
            ChallengeDifficulty.EASY -> 5
            ChallengeDifficulty.MEDIUM -> 8
            ChallengeDifficulty.HARD -> 12
            ChallengeDifficulty.EXPERT -> 18
            ChallengeDifficulty.LEGENDARY -> 25
        }

        return DailyChallenge(
            id = "volume_${System.currentTimeMillis()}",
            date = LocalDate.now().toString(),
            type = ChallengeType.VOLUME_CHALLENGE,
            title = "Productivity Beast",
            description = "Complete $taskCount tasks in one day",
            targetValue = taskCount,
            pointsReward = (50 * taskCount * difficulty.pointsMultiplier).toInt(),
            difficulty = difficulty
        )
    }

    private fun createStreakChallenge(difficulty: ChallengeDifficulty, userProgress: UserProgress): DailyChallenge {
        return DailyChallenge(
            id = "streak_${System.currentTimeMillis()}",
            date = LocalDate.now().toString(),
            type = ChallengeType.STREAK_CHALLENGE,
            title = "Streak Protector",
            description = "Maintain your study streak - don't break the chain!",
            targetValue = 1,
            pointsReward = (userProgress.streakCount * 50 * difficulty.pointsMultiplier).toInt(),
            difficulty = difficulty
        )
    }

    private fun createCategoryFocusChallenge(difficulty: ChallengeDifficulty, userProgress: UserProgress): DailyChallenge {
        val categories = TaskCategory.values()
        val focusCategory = categories[Random.nextInt(categories.size)]
        val taskCount = when (difficulty) {
            ChallengeDifficulty.EASY -> 3
            ChallengeDifficulty.MEDIUM -> 5
            ChallengeDifficulty.HARD -> 8
            ChallengeDifficulty.EXPERT -> 12
            ChallengeDifficulty.LEGENDARY -> 15
        }

        return DailyChallenge(
            id = "category_${System.currentTimeMillis()}",
            date = LocalDate.now().toString(),
            type = ChallengeType.CATEGORY_FOCUS,
            title = "${focusCategory.displayName} Focus",
            description = "Complete $taskCount ${focusCategory.displayName.lowercase()} tasks",
            targetValue = taskCount,
            pointsReward = (75 * taskCount * difficulty.pointsMultiplier).toInt(),
            difficulty = difficulty
        )
    }

    private fun createPerfectDayChallenge(difficulty: ChallengeDifficulty, userProgress: UserProgress): DailyChallenge {
        val requirements = when (difficulty) {
            ChallengeDifficulty.EASY -> "Complete 5 tasks with 85% accuracy"
            ChallengeDifficulty.MEDIUM -> "Complete 8 tasks with 90% accuracy under time"
            ChallengeDifficulty.HARD -> "Complete 12 tasks with 95% accuracy, all under time"
            ChallengeDifficulty.EXPERT -> "Complete 15 tasks with 98% accuracy, all under time"
            ChallengeDifficulty.LEGENDARY -> "Complete 20 tasks with 100% accuracy, all under time"
        }

        return DailyChallenge(
            id = "perfect_${System.currentTimeMillis()}",
            date = LocalDate.now().toString(),
            type = ChallengeType.PERFECT_DAY,
            title = "Perfect Day",
            description = requirements,
            targetValue = 1,
            pointsReward = (500 * difficulty.pointsMultiplier).toInt(),
            difficulty = difficulty
        )
    }

    private fun calculateRecommendedDifficulty(userProgress: UserProgress, taskLogs: List<TaskLog>): ChallengeDifficulty {
        val recentPerformance = calculateRecentPerformance(taskLogs)
        val streakLevel = userProgress.streakCount

        return when {
            streakLevel > 100 && recentPerformance > 0.9f -> ChallengeDifficulty.LEGENDARY
            streakLevel > 50 && recentPerformance > 0.8f -> ChallengeDifficulty.EXPERT
            streakLevel > 14 && recentPerformance > 0.7f -> ChallengeDifficulty.HARD
            streakLevel > 7 -> ChallengeDifficulty.MEDIUM
            else -> ChallengeDifficulty.EASY
        }
    }

    private fun calculateRecentPerformance(taskLogs: List<TaskLog>): Float {
        val recent = taskLogs.takeLast(20)
        if (recent.isEmpty()) return 0.5f
        return recent.count { it.correct }.toFloat() / recent.size
    }
}

/**
 * Comeback System Manager
 */
object ComebackSystem {

    fun checkForComebackBonus(
        currentStreak: Int,
        previousBestStreak: Int,
        daysSinceLastActivity: Int
    ): ComebackBonus? {

        // Only trigger comeback if there was a previous streak worth protecting
        if (previousBestStreak < 7) return null

        return when {
            daysSinceLastActivity == 1 && previousBestStreak >= 50 -> ComebackBonus(
                type = ComebackType.PHOENIX_RISING,
                title = "Phoenix Rising",
                description = "Your legendary streak may be broken, but legends are reborn from ashes!",
                multiplier = 3.0f,
                duration = ComebackDuration.EXTENDED_BOOST
            )

            daysSinceLastActivity <= 3 && previousBestStreak >= 30 -> ComebackBonus(
                type = ComebackType.SECOND_CHANCE,
                title = "Second Chance",
                description = "Everyone deserves a second chance at greatness!",
                multiplier = 2.5f,
                duration = ComebackDuration.WEEK_BOOST
            )

            daysSinceLastActivity <= 7 && previousBestStreak >= 14 -> ComebackBonus(
                type = ComebackType.REDEMPTION_ARC,
                title = "Redemption Arc",
                description = "Every hero's journey includes a comeback story.",
                multiplier = 2.0f,
                duration = ComebackDuration.WEEK_BOOST
            )

            daysSinceLastActivity <= 14 -> ComebackBonus(
                type = ComebackType.FRESH_START,
                title = "Fresh Start",
                description = "New beginnings bring new opportunities!",
                multiplier = 1.5f,
                duration = ComebackDuration.SHORT_BOOST
            )

            else -> null
        }
    }

    fun getComebackEncouragement(daysSinceLastActivity: Int): String {
        return when {
            daysSinceLastActivity == 1 -> "Welcome back! You were missed. Ready to rebuild that amazing streak?"
            daysSinceLastActivity <= 3 -> "Great to see you again! Every expert was once a beginner. Let's get back on track!"
            daysSinceLastActivity <= 7 -> "You're back! The best time to plant a tree was 20 years ago. The second best time is now."
            daysSinceLastActivity <= 14 -> "Welcome back, champion! Progress isn't about perfection - it's about persistence."
            else -> "Every ending is a new beginning. You've got this - let's start your comeback story!"
        }
    }
}

/**
 * Study Buddy Comparison Generator
 */
object StudyBuddySystem {

    fun generateComparison(userProgress: UserProgress): StudyBuddyComparison {
        // Simulate anonymous comparison data
        val categoryComparisons = AchievementCategory.values().associate { category ->
            category.name to generateCategoryComparison(category, userProgress)
        }

        val overallRanking = generateOverallRanking(userProgress)

        return StudyBuddyComparison(
            categoryProgress = categoryComparisons,
            overallRanking = overallRanking
        )
    }

    private fun generateCategoryComparison(category: AchievementCategory, userProgress: UserProgress): BuddyProgress {
        // Simulate user progress in this category (would be calculated from actual data)
        val userCategoryProgress = Random.nextFloat() * 100f
        val averageProgress = Random.nextFloat() * 80f
        val topPercentileProgress = 90f + Random.nextFloat() * 10f

        val encouragement = when {
            userCategoryProgress > topPercentileProgress ->
                "🏆 Outstanding! You're in the top tier for ${category.title}!"
            userCategoryProgress > averageProgress * 1.2f ->
                "🌟 Excellent work! You're above average in ${category.title}."
            userCategoryProgress > averageProgress ->
                "👍 Good progress! You're doing well in ${category.title}."
            else ->
                "💪 Keep going! You have room to grow in ${category.title}."
        }

        return BuddyProgress(
            category = category.title,
            yourProgress = userCategoryProgress,
            averageProgress = averageProgress,
            topPercentile = topPercentileProgress,
            encouragementMessage = encouragement
        )
    }

    private fun generateOverallRanking(userProgress: UserProgress): BuddyRanking {
        val percentile = calculatePercentile(userProgress)

        val rank = when {
            percentile >= 95 -> "Top 5%"
            percentile >= 90 -> "Top 10%"
            percentile >= 75 -> "Top 25%"
            percentile >= 50 -> "Above Average"
            percentile >= 25 -> "Average"
            else -> "Growing"
        }

        val motivation = when {
            percentile >= 90 -> "🏆 You're a true champion! Your dedication is inspiring others."
            percentile >= 75 -> "🌟 Fantastic work! You're among the most committed learners."
            percentile >= 50 -> "🚀 Great progress! You're ahead of many fellow learners."
            percentile >= 25 -> "📈 You're on the right track! Keep building that momentum."
            else -> "🌱 Every expert was once a beginner. You're building something amazing!"
        }

        return BuddyRanking(
            percentile = percentile,
            rank = rank,
            motivationMessage = motivation
        )
    }

    private fun calculatePercentile(userProgress: UserProgress): Int {
        // Simplified percentile calculation based on streak and completed tasks
        val streakScore = minOf(userProgress.streakCount * 2, 100)
        val taskScore = minOf(userProgress.completedTasks.size / 10, 100)
        return ((streakScore + taskScore) / 2).coerceIn(5, 99)
    }
}

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

/**
 * Motivation Mechanics UI Components
 */
@Composable
fun DailyChallengeCard(
    challenge: DailyChallenge,
    onChallengeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = challenge.type.color.copy(alpha = 0.1f)
        ),
        border = BorderStroke(1.dp, challenge.type.color),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Challenge header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = challenge.type.icon,
                        fontSize = 24.sp
                    )
                    Text(
                        text = challenge.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = challenge.type.color
                    )
                }

                Surface(
                    color = challenge.difficulty.color,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = challenge.difficulty.displayName,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Challenge description
            Text(
                text = challenge.description,
                style = MaterialTheme.typography.bodyLarge
            )

            // Progress bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Progress: ${challenge.currentProgress}/${challenge.targetValue}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${(challenge.currentProgress.toFloat() / challenge.targetValue * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = challenge.type.color,
                        fontWeight = FontWeight.Bold
                    )
                }

                LinearProgressIndicator(
                    progress = { challenge.currentProgress.toFloat() / challenge.targetValue },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = challenge.type.color,
                    trackColor = challenge.type.color.copy(alpha = 0.3f)
                )
            }

            // Reward info
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Reward: ${challenge.pointsReward} pts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = challenge.type.color
                )

                if (challenge.bonusMultiplier > 1f) {
                    Surface(
                        color = Color(0xFFFFD700),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${challenge.bonusMultiplier}x BONUS!",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ComebackBonusCard(
    comebackBonus: ComebackBonus,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = comebackBonus.type.color.copy(alpha = 0.1f)
        ),
        border = BorderStroke(2.dp, comebackBonus.type.color),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = comebackBonus.type.icon,
                fontSize = 48.sp
            )

            Text(
                text = comebackBonus.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = comebackBonus.type.color,
                textAlign = TextAlign.Center
            )

            Text(
                text = comebackBonus.description,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )

            Surface(
                color = comebackBonus.type.color,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "${comebackBonus.multiplier}x Points Bonus - ${comebackBonus.duration.displayName}",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun StudyBuddyComparisonCard(
    comparison: StudyBuddyComparison,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "📊 Study Buddy Comparison",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            // Overall ranking
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                ) {
                    Text(
                        text = comparison.overallRanking.rank,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Column {
                    Text(
                        text = "Your Ranking",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "${comparison.overallRanking.percentile}th percentile",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                text = comparison.overallRanking.motivationMessage,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}