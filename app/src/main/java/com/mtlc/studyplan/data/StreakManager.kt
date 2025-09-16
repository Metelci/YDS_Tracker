package com.mtlc.studyplan.data

import androidx.compose.runtime.*
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Central manager for the enhanced streak system
 * Coordinates streak tracking, points calculation, and warning systems
 */
class StreakManager(
    private val progressRepository: ProgressRepository
) {

    /**
     * Combined flow that provides complete streak information
     */
    val streakStateFlow: Flow<StreakState> = progressRepository.userProgressFlow.map { progress ->
        progressRepository.getStreakState(progress)
    }

    /**
     * Flow for current multiplier information
     */
    val currentMultiplierFlow: Flow<StreakMultiplier> = progressRepository.userProgressFlow.map { progress ->
        StreakMultiplier.getMultiplierForStreak(progress.streakCount)
    }

    /**
     * Complete a task with enhanced points calculation
     */
    suspend fun completeTask(
        taskId: String,
        taskDescription: String,
        taskDetails: String? = null,
        minutesSpent: Int = 0
    ): PointsTransaction {
        return progressRepository.completeTaskWithPoints(
            taskId = taskId,
            taskDescription = taskDescription,
            taskDetails = taskDetails,
            minutesSpent = minutesSpent
        )
    }

    /**
     * Get current streak information
     */
    suspend fun getCurrentStreakState(): StreakState {
        val progress = progressRepository.userProgressFlow.first()
        return progressRepository.getStreakState(progress)
    }

    /**
     * Get current points total
     */
    suspend fun getCurrentPoints(): Int {
        return progressRepository.userProgressFlow.first().totalPoints
    }

    /**
     * Check if streak is in danger
     */
    suspend fun isStreakInDanger(): Boolean {
        return getCurrentStreakState().isInDanger
    }

    /**
     * Calculate points for a given task without completing it
     */
    suspend fun calculatePointsForTask(
        taskDescription: String,
        taskDetails: String? = null
    ): PointsPreview {
        val progress = progressRepository.userProgressFlow.first()
        val taskCategory = TaskCategory.fromString(taskDescription + " " + (taskDetails ?: ""))
        val streakMultiplier = StreakMultiplier.getMultiplierForStreak(progress.streakCount)
        val basePoints = taskCategory.basePoints
        val totalPoints = (basePoints * streakMultiplier.multiplier).toInt()

        return PointsPreview(
            basePoints = basePoints,
            multiplier = streakMultiplier.multiplier,
            totalPoints = totalPoints,
            taskCategory = taskCategory,
            streakMultiplier = streakMultiplier
        )
    }
}

/**
 * Preview of points calculation without actually completing the task
 */
data class PointsPreview(
    val basePoints: Int,
    val multiplier: Float,
    val totalPoints: Int,
    val taskCategory: TaskCategory,
    val streakMultiplier: StreakMultiplier
) {
    val hasMultiplierBonus: Boolean = multiplier > 1f
}

/**
 * Composable hook for accessing streak system in UI
 */
@Composable
fun rememberStreakManager(dataStore: DataStore<Preferences>): StreakManager {
    return remember(dataStore) {
        StreakManager(ProgressRepository(dataStore))
    }
}

/**
 * Composable state holder for streak information
 */
@Composable
fun rememberStreakState(streakManager: StreakManager): State<StreakState?> {
    return streakManager.streakStateFlow.collectAsState(initial = null)
}

/**
 * Composable state holder for current multiplier
 */
@Composable
fun rememberCurrentMultiplier(streakManager: StreakManager): State<StreakMultiplier?> {
    return streakManager.currentMultiplierFlow.collectAsState(initial = null)
}

/**
 * Integration utilities for existing UI components
 */
object StreakIntegration {

    /**
     * Enhanced task completion handler
     */
    suspend fun enhancedTaskCompletionHandler(
        streakManager: StreakManager,
        taskId: String,
        taskDescription: String,
        taskDetails: String? = null,
        onPointsEarned: (PointsTransaction) -> Unit = {},
        onStreakMilestone: (StreakMultiplier) -> Unit = {}
    ) {
        val transaction = streakManager.completeTask(
            taskId = taskId,
            taskDescription = taskDescription,
            taskDetails = taskDetails
        )
        onPointsEarned(transaction)

        // Check for milestone achievements
        val currentState = streakManager.streakStateFlow.first()
        if (currentState.multiplier.isFireStreak ||
            currentState.currentStreak in listOf(7, 14, 30, 50)) {
            onStreakMilestone(currentState.multiplier)
        }
    }

    /**
     * Get points preview for task cards
     */
    @Composable
    fun getTaskPointsPreview(
        streakManager: StreakManager,
        taskDescription: String,
        taskDetails: String? = null
    ): State<PointsPreview?> {
        var preview by remember { mutableStateOf<PointsPreview?>(null) }

        LaunchedEffect(taskDescription, taskDetails) {
            preview = streakManager.calculatePointsForTask(taskDescription, taskDetails)
        }

        return derivedStateOf { preview }
    }
}

/**
 * Extension functions for existing data models
 */
fun UserProgress.toStreakState(): StreakState {
    return StreakState(
        currentStreak = this.streakCount,
        multiplier = StreakMultiplier.getMultiplierForStreak(this.streakCount),
        isInDanger = false, // Will be calculated by ProgressRepository.getStreakState()
        hoursUntilBreak = 0,
        lastActivityDate = this.lastCompletionDate
    )
}

fun TaskLog.toPointsTransaction(): PointsTransaction? {
    if (pointsEarned <= 0) return null

    val category = TaskCategory.fromString(category)
    val multiplier = pointsEarned.toFloat() / category.basePoints

    return PointsTransaction(
        basePoints = category.basePoints,
        multiplier = multiplier,
        totalPoints = pointsEarned,
        taskCategory = category,
        streakMultiplier = StreakMultiplier.getMultiplierForStreak(0), // Approximation
        timestampMillis = timestampMillis
    )
}