package com.mtlc.studyplan.ui.celebrations

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.zIndex
import com.mtlc.studyplan.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Types of celebrations that can be triggered
 */
sealed class CelebrationType {
    data class TaskCompletion(
        val taskId: String,
        val points: Int,
        val taskCategory: TaskCategory,
        val position: androidx.compose.ui.geometry.Offset? = null
    ) : CelebrationType()

    data class DailyGoalAchieved(
        val tasksCompleted: Int,
        val totalTasks: Int,
        val streakCount: Int,
        val pointsEarned: Int
    ) : CelebrationType()

    data class LevelUp(
        val achievement: CategorizedAchievement,
        val newLevel: AchievementTier,
        val totalPoints: Int
    ) : CelebrationType()

    data class MilestoneReward(
        val milestoneType: MilestoneType,
        val value: Int,
        val reward: String,
        val points: Int
    ) : CelebrationType()
}

/**
 * Types of milestones for major celebrations
 */
enum class MilestoneType(val displayName: String, val icon: String) {
    WEEK_COMPLETION("Week Complete", "📅"),
    MONTH_COMPLETION("Month Complete", "🏆"),
    STREAK_MILESTONE("Streak Milestone", "🔥"),
    PROGRAM_COMPLETION("Program Complete", "🎓")
}

/**
 * Celebration event data class
 */
data class CelebrationEvent(
    val type: CelebrationType,
    val timestamp: Long = System.currentTimeMillis(),
    val id: String = "${type::class.simpleName}_${System.currentTimeMillis()}"
)

/**
 * Main celebration manager composable
 */
@Composable
fun CelebrationManager(
    celebrations: List<CelebrationEvent>,
    onCelebrationComplete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = modifier.fillMaxSize()) {
        celebrations.forEach { celebration ->
            key(celebration.id) {
                when (celebration.type) {
                    is CelebrationType.TaskCompletion -> {
                        TaskCompletionCelebration(
                            event = celebration.type,
                            onComplete = { onCelebrationComplete(celebration.id) }
                        )
                    }
                    is CelebrationType.DailyGoalAchieved -> {
                        DailyGoalCelebration(
                            event = celebration.type,
                            onComplete = { onCelebrationComplete(celebration.id) }
                        )
                    }
                    is CelebrationType.LevelUp -> {
                        LevelUpCelebration(
                            event = celebration.type,
                            onComplete = { onCelebrationComplete(celebration.id) }
                        )
                    }
                    is CelebrationType.MilestoneReward -> {
                        MilestoneCelebration(
                            event = celebration.type,
                            onComplete = { onCelebrationComplete(celebration.id) }
                        )
                    }
                }
            }
        }
    }

    // Trigger haptic feedback for new celebrations
    LaunchedEffect(celebrations.size) {
        if (celebrations.isNotEmpty()) {
            val latest = celebrations.last()
            when (latest.type) {
                is CelebrationType.TaskCompletion -> {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                is CelebrationType.DailyGoalAchieved -> {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    delay(200)
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                is CelebrationType.LevelUp -> {
                    // Triple haptic for level up
                    repeat(3) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        delay(150)
                    }
                }
                is CelebrationType.MilestoneReward -> {
                    // Extended haptic sequence for milestones
                    coroutineScope.launch {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        delay(100)
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        delay(100)
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        delay(300)
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                }
            }
        }
    }
}

/**
 * Celebration state management
 */
@Stable
class CelebrationState {
    private val _celebrations = mutableStateListOf<CelebrationEvent>()
    val celebrations: List<CelebrationEvent> = _celebrations

    fun triggerCelebration(type: CelebrationType) {
        val event = CelebrationEvent(type)
        _celebrations.add(event)
    }

    fun completeCelebration(celebrationId: String) {
        _celebrations.removeAll { it.id == celebrationId }
    }

    fun clearAll() {
        _celebrations.clear()
    }
}

/**
 * Remember celebration state across recompositions
 */
@Composable
fun rememberCelebrationState(): CelebrationState {
    return remember { CelebrationState() }
}

/**
 * Core animation specifications for celebrations
 */
object CelebrationAnimations {

    val taskCompletionScale = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )

    val dailyGoalBounce = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessHigh
    )

    val levelUpEnter = slideInVertically { -it } + fadeIn() + scaleIn(
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )

    val levelUpExit = slideOutVertically { -it } + fadeOut() + scaleOut()

    val milestoneEnter = scaleIn(
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        )
    ) + fadeIn()

    val milestoneExit = scaleOut() + fadeOut()

    val floatingPointsEnter = slideInVertically { it / 2 } + fadeIn()
    val floatingPointsExit = slideOutVertically { -it } + fadeOut()

    val rippleExpansion = tween<Float>(
        durationMillis = 600,
        easing = FastOutSlowInEasing
    )

    val sparkleRotation = infiniteRepeatable<Float>(
        animation = tween(2000, easing = LinearEasing),
        repeatMode = RepeatMode.Restart
    )
}

/**
 * Color schemes for different celebration types
 */
object CelebrationColors {
    val taskCompletion = listOf(
        Color(0xFF4CAF50), // Green
        Color(0xFF8BC34A), // Light Green
        Color(0xFFCDDC39)  // Lime
    )

    val dailyGoal = listOf(
        Color(0xFF2196F3), // Blue
        Color(0xFF03A9F4), // Light Blue
        Color(0xFF00BCD4), // Cyan
        Color(0xFF009688)  // Teal
    )

    val levelUp = listOf(
        Color(0xFFFF9800), // Orange
        Color(0xFFFF5722), // Deep Orange
        Color(0xFFE91E63), // Pink
        Color(0xFF9C27B0)  // Purple
    )

    val milestone = listOf(
        Color(0xFFFFD700), // Gold
        Color(0xFFFFC107), // Amber
        Color(0xFFFF9800), // Orange
        Color(0xFFE91E63)  // Pink
    )

    val fire = listOf(
        Color(0xFFFF5722), // Deep Orange
        Color(0xFFFF9800), // Orange
        Color(0xFFFFC107), // Amber
        Color(0xFFFFEB3B)  // Yellow
    )
}

/**
 * Utility functions for celebration management
 */
object CelebrationUtils {

    fun getColorSchemeForCelebration(type: CelebrationType): List<Color> {
        return when (type) {
            is CelebrationType.TaskCompletion -> CelebrationColors.taskCompletion
            is CelebrationType.DailyGoalAchieved -> CelebrationColors.dailyGoal
            is CelebrationType.LevelUp -> CelebrationColors.levelUp
            is CelebrationType.MilestoneReward -> when (type.milestoneType) {
                MilestoneType.STREAK_MILESTONE -> CelebrationColors.fire
                else -> CelebrationColors.milestone
            }
        }
    }

    fun getDurationForCelebration(type: CelebrationType): Long {
        return when (type) {
            is CelebrationType.TaskCompletion -> 1500L
            is CelebrationType.DailyGoalAchieved -> 3000L
            is CelebrationType.LevelUp -> 4000L
            is CelebrationType.MilestoneReward -> 5000L
        }
    }

    fun getIntensityForCelebration(type: CelebrationType): CelebrationIntensity {
        return when (type) {
            is CelebrationType.TaskCompletion -> CelebrationIntensity.SUBTLE
            is CelebrationType.DailyGoalAchieved -> CelebrationIntensity.MODERATE
            is CelebrationType.LevelUp -> CelebrationIntensity.HIGH
            is CelebrationType.MilestoneReward -> CelebrationIntensity.EPIC
        }
    }
}

/**
 * Celebration intensity levels
 */
enum class CelebrationIntensity(
    val particleCount: Int,
    val animationMultiplier: Float,
    val soundVolume: Float
) {
    SUBTLE(particleCount = 8, animationMultiplier = 0.8f, soundVolume = 0.3f),
    MODERATE(particleCount = 15, animationMultiplier = 1.0f, soundVolume = 0.5f),
    HIGH(particleCount = 25, animationMultiplier = 1.2f, soundVolume = 0.7f),
    EPIC(particleCount = 40, animationMultiplier = 1.5f, soundVolume = 1.0f)
}