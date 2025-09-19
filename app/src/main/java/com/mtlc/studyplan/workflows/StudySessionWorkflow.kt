package com.mtlc.studyplan.workflows

import android.content.Context
import com.mtlc.studyplan.shared.SharedAppViewModel
import com.mtlc.studyplan.data.StudySessionResult
import com.mtlc.studyplan.utils.NotificationHelper
import com.mtlc.studyplan.utils.ToastManager
import com.mtlc.studyplan.R
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Complete Study Session Journey Implementation
 * Handles the entire workflow from session start to completion with proper feedback
 */
class StudySessionWorkflow(
    private val sharedViewModel: SharedAppViewModel,
    private val context: Context
) {

    suspend fun executeStudySession(taskId: String): WorkflowResult {
        return try {
            // Step 1: Start session
            val task = sharedViewModel.findTaskById(taskId)
            if (task == null) {
                return WorkflowResult.Error("Task not found")
            }

            sharedViewModel.startStudySession(taskId)
            showSessionStartedFeedback(task.title)

            // Step 2: Track progress
            val sessionResult = trackSessionProgress(taskId, task.estimatedMinutes)

            // Step 3: Complete session
            sharedViewModel.completeTask(taskId)

            // Step 4: Update all related data
            updateProgressData(sessionResult)
            updateStreakData()
            val achievements = checkAchievements(sessionResult)

            // Step 5: Show success feedback
            showSessionCompletedFeedback(sessionResult, achievements)

            WorkflowResult.Success(sessionResult)
        } catch (e: Exception) {
            handleWorkflowError(e, "Study Session")
            WorkflowResult.Error(e.message ?: "Study session failed")
        }
    }

    private suspend fun trackSessionProgress(taskId: String, estimatedMinutes: Int): StudySessionResult {
        val startTime = System.currentTimeMillis()

        // Simulate session progress tracking
        val actualDuration = estimatedMinutes // In real app, this would be user-controlled
        delay(1000) // Brief delay to simulate session processing

        val endTime = System.currentTimeMillis()
        val pointsEarned = calculatePointsEarned(actualDuration)

        return StudySessionResult(
            taskId = taskId,
            duration = actualDuration,
            pointsEarned = pointsEarned,
            startTime = startTime,
            endTime = endTime,
            completionRate = 1.0f
        )
    }

    private fun calculatePointsEarned(durationMinutes: Int): Int {
        // Base points + bonus for longer sessions
        return when {
            durationMinutes >= 60 -> 50 + (durationMinutes - 60) * 2
            durationMinutes >= 30 -> 30 + (durationMinutes - 30) * 1
            else -> durationMinutes * 1
        }
    }

    private suspend fun updateProgressData(result: StudySessionResult) {
        // Update study statistics
        val currentStats = sharedViewModel.studyStats.value
        val updatedStats = currentStats.copy(
            totalStudyTime = currentStats.totalStudyTime + result.duration,
            totalTasksCompleted = currentStats.totalTasksCompleted + 1,
            totalXP = currentStats.totalXP + result.pointsEarned
        )
        sharedViewModel.updateStudyStats(updatedStats)
    }

    private suspend fun updateStreakData() {
        val currentStreak = sharedViewModel.currentStreak.value
        val today = LocalDateTime.now().toLocalDate()
        sharedViewModel.extendStreak(today)
    }

    private suspend fun checkAchievements(result: StudySessionResult): List<String> {
        val newAchievements = mutableListOf<String>()
        val currentStats = sharedViewModel.studyStats.value

        // Check study time achievements
        if (currentStats.totalStudyTime >= 1000 && currentStats.totalStudyTime - result.duration < 1000) {
            newAchievements.add("Study Master: 1000+ minutes studied!")
        }

        // Check task completion achievements
        if (currentStats.totalTasksCompleted >= 50 && currentStats.totalTasksCompleted - 1 < 50) {
            newAchievements.add("Task Crusher: 50 tasks completed!")
        }

        // Check streak achievements
        val currentStreak = sharedViewModel.currentStreak.value
        if (currentStreak == 7) {
            newAchievements.add("Week Warrior: 7-day streak!")
        } else if (currentStreak == 30) {
            newAchievements.add("Month Master: 30-day streak!")
        }

        return newAchievements
    }

    private fun showSessionStartedFeedback(taskTitle: String) {
        ToastManager.showSuccess("Study session started! 📚\n$taskTitle")
    }

    private fun showSessionCompletedFeedback(result: StudySessionResult, achievements: List<String>) {
        val duration = formatDuration(result.duration)
        val currentStreak = sharedViewModel.currentStreak.value
        val streakExtended = currentStreak > 0

        val message = buildString {
            append("Great job! Session completed ✅\n")
            append("Time: $duration\n")
            append("Points earned: +${result.pointsEarned}")
            if (streakExtended) append("\n🔥 Streak: $currentStreak days!")
            if (achievements.isNotEmpty()) {
                append("\n\n🎉 New Achievements:")
                achievements.forEach { append("\n• $it") }
            }
        }

        NotificationHelper.showAchievementNotification(
            context = context,
            title = "Study Session Complete!",
            message = message,
            icon = R.drawable.ic_check_circle
        )

        // Also show toast for immediate feedback
        ToastManager.showSuccess("Session complete! +${result.pointsEarned} XP 🎉")
    }

    private fun formatDuration(minutes: Int): String {
        return when {
            minutes < 60 -> "${minutes}m"
            else -> "${minutes / 60}h ${minutes % 60}m"
        }
    }

    private fun handleWorkflowError(error: Exception, operation: String) {
        val errorMessage = "Failed to complete $operation: ${error.message ?: "Unknown error"}"
        ToastManager.showError(errorMessage)

        // Log error for debugging
        android.util.Log.e("StudySessionWorkflow", errorMessage, error)
    }
}

// Data classes for workflow results
sealed class WorkflowResult {
    data class Success<T>(val data: T) : WorkflowResult()
    data class Error(val message: String) : WorkflowResult()
    data class ValidationError(val message: String) : WorkflowResult()
    object QueuedForLater : WorkflowResult()
}

data class StudySessionResult(
    val taskId: String,
    val duration: Int,
    val pointsEarned: Int,
    val startTime: Long,
    val endTime: Long,
    val completionRate: Float
)