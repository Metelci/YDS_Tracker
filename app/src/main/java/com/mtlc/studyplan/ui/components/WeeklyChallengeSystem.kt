package com.mtlc.studyplan.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtlc.studyplan.data.TaskCategory
import com.mtlc.studyplan.data.UserProgress
import com.mtlc.studyplan.gamification.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.Contextual
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import kotlin.random.Random

/**
 * Weekly challenge system with rotating challenges and progress tracking
 */
@Serializable
data class WeeklyChallenge(
    val id: String,
    val weekStart: @Contextual LocalDate,
    val weekEnd: @Contextual LocalDate,
    val type: WeeklyChallengeType,
    val title: String,
    val description: String,
    val targetValue: Int,
    val currentProgress: Int = 0,
    val isCompleted: Boolean = false,
    val pointsReward: Int,
    val bonusReward: String? = null,
    val difficulty: ChallengeDifficulty = ChallengeDifficulty.MEDIUM,
    val category: TaskCategory? = null,
    val milestones: List<ChallengeMilestone> = emptyList(),
    val completedAt: Long? = null
)

enum class WeeklyChallengeType(
    val displayName: String,
    val icon: ImageVector,
    val color: Color,
    val description: String
) {
    TASK_VOLUME(
        displayName = "Volume Master",
        icon = Icons.AutoMirrored.Filled.Assignment,
        color = Color(0xFF2196F3),
        description = "Complete a target number of tasks"
    ),
    STREAK_BUILDER(
        displayName = "Streak Builder",
        icon = Icons.Default.LocalFireDepartment,
        color = Color(0xFFFF5722),
        description = "Maintain daily study streak"
    ),
    CATEGORY_FOCUS(
        displayName = "Category Expert",
        icon = Icons.Default.Category,
        color = Color(0xFF9C27B0),
        description = "Focus on specific skill category"
    ),
    SPEED_DEMON(
        displayName = "Speed Challenge",
        icon = Icons.Default.Speed,
        color = Color(0xFFFF9800),
        description = "Complete tasks faster than average"
    ),
    ACCURACY_MASTER(
        displayName = "Precision Pro",
        icon = Icons.Default.GpsFixed,
        color = Color(0xFF4CAF50),
        description = "Achieve high accuracy scores"
    ),
    CONSISTENCY_KING(
        displayName = "Consistency Champion",
        icon = Icons.Default.Schedule,
        color = Color(0xFF607D8B),
        description = "Study at consistent times daily"
    ),
    PERFECT_WEEK(
        displayName = "Perfect Week",
        icon = Icons.Default.Diamond,
        color = Color(0xFFE91E63),
        description = "Complete all planned tasks for the week"
    )
}

/**
 * Weekly challenge display component
 */
@Composable
fun WeeklyChallengeCard(
    challenge: WeeklyChallenge,
    onChallengeClick: (WeeklyChallenge) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val progress = if (challenge.targetValue > 0) {
        (challenge.currentProgress.toFloat() / challenge.targetValue.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "challengeProgress"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onChallengeClick(challenge) },
        colors = CardDefaults.cardColors(
            containerColor = if (challenge.isCompleted) {
                challenge.type.color.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (challenge.isCompleted) {
            androidx.compose.foundation.BorderStroke(2.dp, challenge.type.color)
        } else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with icon and title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        color = challenge.type.color,
                        shape = CircleShape,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = challenge.type.icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    Column {
                        Text(
                            text = challenge.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (challenge.isCompleted) challenge.type.color else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = challenge.type.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Completion status or difficulty
                if (challenge.isCompleted) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Completed",
                        tint = challenge.type.color,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Surface(
                        color = challenge.difficulty.color,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = challenge.difficulty.displayName,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Description
            Text(
                text = challenge.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Progress section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Progress: ${challenge.currentProgress}/${challenge.targetValue}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = challenge.type.color
                    )
                }

                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = challenge.type.color,
                    trackColor = challenge.type.color.copy(alpha = 0.2f)
                )
            }

            // Milestones (if any)
            if (challenge.milestones.isNotEmpty()) {
                ChallengeNearestMilestone(
                    milestones = challenge.milestones,
                    currentProgress = challenge.currentProgress,
                    color = challenge.type.color
                )
            }

            // Rewards section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${challenge.pointsReward} pts",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                challenge.bonusReward?.let { bonus ->
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Bonus: $bonus",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            // Time remaining
            val timeRemaining = getTimeRemainingText(challenge.weekEnd)
            if (timeRemaining.isNotEmpty() && !challenge.isCompleted) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = timeRemaining,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ChallengeNearestMilestone(
    milestones: List<ChallengeMilestone>,
    currentProgress: Int,
    color: Color
) {
    val nextMilestone = milestones.firstOrNull { !it.isUnlocked && it.progress > currentProgress }

    nextMilestone?.let { milestone ->
        Surface(
            color = color.copy(alpha = 0.1f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Next: ${milestone.reward} at ${milestone.progress}",
                    style = MaterialTheme.typography.bodySmall,
                    color = color,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Weekly challenges overview screen
 */
@Composable
fun WeeklyChallengesScreen(
    challenges: List<WeeklyChallenge>,
    onChallengeClick: (WeeklyChallenge) -> Unit,
    onRefresh: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Weekly Challenges",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = getCurrentWeekText(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh challenges"
                )
            }
        }

        // Challenge stats summary
        ChallengesSummaryCard(challenges = challenges)

        // Active challenges
        val activeChallenges = challenges.filter { !it.isCompleted }
        val completedChallenges = challenges.filter { it.isCompleted }

        if (activeChallenges.isNotEmpty()) {
            Text(
                text = "Active Challenges (${activeChallenges.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(activeChallenges) { challenge ->
                    WeeklyChallengeCard(
                        challenge = challenge,
                        onChallengeClick = onChallengeClick
                    )
                }
            }
        }

        // Completed challenges
        if (completedChallenges.isNotEmpty()) {
            Text(
                text = "Completed This Week (${completedChallenges.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(completedChallenges) { challenge ->
                    CompletedChallengeCard(challenge = challenge)
                }
            }
        }
    }
}

@Composable
private fun ChallengesSummaryCard(challenges: List<WeeklyChallenge>) {
    val completedCount = challenges.count { it.isCompleted }
    val totalPoints = challenges.filter { it.isCompleted }.sumOf { it.pointsReward }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SummaryStatItem(
                label = "Completed",
                value = "$completedCount/${challenges.size}",
                icon = Icons.Default.CheckCircle
            )
            SummaryStatItem(
                label = "Points Earned",
                value = "$totalPoints",
                icon = Icons.Default.EmojiEvents
            )
            SummaryStatItem(
                label = "Week Progress",
                value = "${(completedCount.toFloat() / challenges.size * 100).toInt()}%",
                icon = Icons.AutoMirrored.Filled.TrendingUp
            )
        }
    }
}

@Composable
private fun SummaryStatItem(
    label: String,
    value: String,
    icon: ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun CompletedChallengeCard(
    challenge: WeeklyChallenge,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.width(120.dp),
        colors = CardDefaults.cardColors(
            containerColor = challenge.type.color.copy(alpha = 0.1f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, challenge.type.color)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = challenge.type.icon,
                contentDescription = null,
                tint = challenge.type.color,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = challenge.title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = challenge.type.color,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Surface(
                color = challenge.type.color,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "+${challenge.pointsReward}",
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Challenge generator for creating weekly challenges
 */
object WeeklyChallengeGenerator {

    fun generateWeeklyCharlenges(userProgress: UserProgress): List<WeeklyChallenge> {
        val currentWeek = getCurrentWeek()
        val challenges = mutableListOf<WeeklyChallenge>()

        // Generate 3-5 challenges per week
        val challengeCount = Random.nextInt(3, 6)
        val selectedTypes = WeeklyChallengeType.values().toList().shuffled().take(challengeCount)

        selectedTypes.forEach { type ->
            val challenge = generateChallengeForType(type, currentWeek, userProgress)
            challenges.add(challenge)
        }

        return challenges
    }

    private fun generateChallengeForType(
        type: WeeklyChallengeType,
        weekStart: LocalDate,
        userProgress: UserProgress
    ): WeeklyChallenge {
        val weekEnd = weekStart.plusDays(6)
        val difficulty = calculateDifficulty(userProgress)

        return when (type) {
            WeeklyChallengeType.TASK_VOLUME -> WeeklyChallenge(
                id = "weekly_${type.name.lowercase()}_${weekStart}",
                weekStart = weekStart,
                weekEnd = weekEnd,
                type = type,
                title = "Complete ${getDifficultyTarget(difficulty, 20, 35, 50)} Tasks",
                description = "Finish your weekly task target across all categories",
                targetValue = getDifficultyTarget(difficulty, 20, 35, 50),
                pointsReward = getDifficultyTarget(difficulty, 300, 500, 750),
                difficulty = difficulty,
                milestones = createTaskVolumeMilestones(getDifficultyTarget(difficulty, 20, 35, 50))
            )

            WeeklyChallengeType.STREAK_BUILDER -> WeeklyChallenge(
                id = "weekly_${type.name.lowercase()}_${weekStart}",
                weekStart = weekStart,
                weekEnd = weekEnd,
                type = type,
                title = "Maintain ${getDifficultyTarget(difficulty, 5, 7, 7)} Day Streak",
                description = "Study every day for the entire week",
                targetValue = getDifficultyTarget(difficulty, 5, 7, 7),
                pointsReward = getDifficultyTarget(difficulty, 400, 600, 800),
                difficulty = difficulty,
                bonusReward = "2x Streak Multiplier"
            )

            WeeklyChallengeType.CATEGORY_FOCUS -> {
                val category = TaskCategory.values().random()
                WeeklyChallenge(
                    id = "weekly_${type.name.lowercase()}_${weekStart}",
                    weekStart = weekStart,
                    weekEnd = weekEnd,
                    type = type,
                    title = "${category.displayName} Focus",
                    description = "Complete ${getDifficultyTarget(difficulty, 8, 12, 18)} ${category.displayName.lowercase()} tasks",
                    targetValue = getDifficultyTarget(difficulty, 8, 12, 18),
                    pointsReward = getDifficultyTarget(difficulty, 250, 400, 600),
                    difficulty = difficulty,
                    category = category
                )
            }

            WeeklyChallengeType.SPEED_DEMON -> WeeklyChallenge(
                id = "weekly_${type.name.lowercase()}_${weekStart}",
                weekStart = weekStart,
                weekEnd = weekEnd,
                type = type,
                title = "Speed Master",
                description = "Complete ${getDifficultyTarget(difficulty, 10, 15, 25)} tasks under estimated time",
                targetValue = getDifficultyTarget(difficulty, 10, 15, 25),
                pointsReward = getDifficultyTarget(difficulty, 350, 550, 800),
                difficulty = difficulty
            )

            WeeklyChallengeType.ACCURACY_MASTER -> WeeklyChallenge(
                id = "weekly_${type.name.lowercase()}_${weekStart}",
                weekStart = weekStart,
                weekEnd = weekEnd,
                type = type,
                title = "Precision Perfect",
                description = "Achieve ${getDifficultyTarget(difficulty, 85, 90, 95)}% average accuracy",
                targetValue = getDifficultyTarget(difficulty, 85, 90, 95),
                pointsReward = getDifficultyTarget(difficulty, 400, 600, 900),
                difficulty = difficulty
            )

            WeeklyChallengeType.CONSISTENCY_KING -> WeeklyChallenge(
                id = "weekly_${type.name.lowercase()}_${weekStart}",
                weekStart = weekStart,
                weekEnd = weekEnd,
                type = type,
                title = "Consistency Champion",
                description = "Study at the same time for ${getDifficultyTarget(difficulty, 4, 5, 7)} days",
                targetValue = getDifficultyTarget(difficulty, 4, 5, 7),
                pointsReward = getDifficultyTarget(difficulty, 300, 450, 700),
                difficulty = difficulty
            )

            WeeklyChallengeType.PERFECT_WEEK -> WeeklyChallenge(
                id = "weekly_${type.name.lowercase()}_${weekStart}",
                weekStart = weekStart,
                weekEnd = weekEnd,
                type = type,
                title = "Perfect Week",
                description = "Complete 100% of your planned tasks",
                targetValue = 100,
                pointsReward = 1000,
                difficulty = ChallengeDifficulty.EXPERT,
                bonusReward = "Perfect Week Badge"
            )
        }
    }

    private fun createTaskVolumeMilestones(targetValue: Int): List<ChallengeMilestone> {
        return listOf(
            ChallengeMilestone(targetValue / 4, "Quarter Complete Badge", 50),
            ChallengeMilestone(targetValue / 2, "Halfway Champion", 100),
            ChallengeMilestone((targetValue * 0.75).toInt(), "Almost There!", 150)
        )
    }

    private fun calculateDifficulty(userProgress: UserProgress): ChallengeDifficulty {
        val completedTasks = userProgress.completedTasks.size
        val streakDays = userProgress.streakCount

        return when {
            completedTasks > 200 && streakDays > 30 -> ChallengeDifficulty.EXPERT
            completedTasks > 100 && streakDays > 14 -> ChallengeDifficulty.HARD
            completedTasks > 50 -> ChallengeDifficulty.MEDIUM
            else -> ChallengeDifficulty.EASY
        }
    }

    private fun getDifficultyTarget(difficulty: ChallengeDifficulty, easy: Int, medium: Int, hard: Int): Int {
        return when (difficulty) {
            ChallengeDifficulty.EASY -> easy
            ChallengeDifficulty.MEDIUM -> medium
            ChallengeDifficulty.HARD, ChallengeDifficulty.EXPERT -> hard
            ChallengeDifficulty.LEGENDARY -> (hard * 1.5f).toInt()
        }
    }
}

// Helper functions
private fun getCurrentWeek(): LocalDate {
    val now = LocalDate.now()
    val weekFields = WeekFields.of(DayOfWeek.MONDAY, 1)
    return now.with(weekFields.dayOfWeek(), 1)
}

private fun getCurrentWeekText(): String {
    val weekStart = getCurrentWeek()
    val weekEnd = weekStart.plusDays(6)
    val formatter = DateTimeFormatter.ofPattern("MMM d")
    return "${weekStart.format(formatter)} - ${weekEnd.format(formatter)}"
}

private fun getTimeRemainingText(weekEnd: LocalDate): String {
    val daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), weekEnd.plusDays(1))
    return when {
        daysRemaining > 1 -> "$daysRemaining days remaining"
        daysRemaining == 1L -> "Last day!"
        else -> ""
    }
}
