package com.mtlc.studyplan.actions

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import com.mtlc.studyplan.shared.AppIntegrationManager
import com.mtlc.studyplan.animations.AnimationManager
import com.mtlc.studyplan.feedback.FeedbackManager
import com.mtlc.studyplan.achievements.AchievementManager
import com.mtlc.studyplan.data.Task
import com.mtlc.studyplan.data.Achievement
import com.mtlc.studyplan.data.ProgressMilestone
import com.mtlc.studyplan.social.SocialActivity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskCompletionHandler @Inject constructor(
    private val appIntegrationManager: AppIntegrationManager,
    private val animationManager: AnimationManager,
    private val feedbackManager: FeedbackManager,
    private val achievementManager: AchievementManager
) {

    suspend fun completeTask(
        taskId: String,
        studyMinutes: Int? = null,
        quality: TaskQuality = TaskQuality.GOOD,
        notes: String = ""
    ): TaskCompletionResult {

        return try {
            // 1. Start completion animation
            animationManager.startTaskCompletionAnimation(taskId)

            // 2. Complete the task with details
            val completionResult = appIntegrationManager.completeTaskWithDetails(
                taskId = taskId,
                actualMinutes = studyMinutes,
                quality = quality,
                notes = notes
            ).getOrThrow()

            // 3. Show immediate feedback
            feedbackManager.showTaskCompletionFeedback(completionResult)

            // 4. Handle achievements
            if (completionResult.newAchievements.isNotEmpty()) {
                handleAchievementUnlocks(completionResult.newAchievements)
            }

            // 5. Handle streak updates
            if (completionResult.streakExtended) {
                handleStreakExtension(completionResult.newStreak ?: 0)
            }

            // 6. Handle milestone celebrations
            if (completionResult.triggeredMilestone) {
                handleMilestoneCelebration(completionResult.milestone!!)
            }

            // 7. Complete animation
            animationManager.completeTaskCompletionAnimation(taskId, true)

            completionResult

        } catch (e: Exception) {
            // Handle error with feedback
            animationManager.completeTaskCompletionAnimation(taskId, false)
            feedbackManager.showTaskCompletionError(e.message ?: "Completion failed")
            throw e
        }
    }

    private suspend fun handleAchievementUnlocks(achievements: List<Achievement>) {
        achievements.forEach { achievement ->
            // Show achievement unlock animation
            animationManager.showAchievementUnlockAnimation(achievement)

            // Show notification
            feedbackManager.showAchievementNotification(achievement)

            // Update social activity
            appIntegrationManager.createSocialActivity(
                SocialActivity.achievementUnlocked(achievement)
            )
        }
    }

    private suspend fun handleStreakExtension(newStreak: Int) {
        // Show streak animation
        animationManager.showStreakExtensionAnimation(newStreak)

        // Check for streak milestones
        when (newStreak) {
            7 -> {
                val achievement = Achievement.WEEK_STREAK
                appIntegrationManager.unlockAchievement(achievement.id)
            }
            30 -> {
                val achievement = Achievement.MONTH_STREAK
                appIntegrationManager.unlockAchievement(achievement.id)
            }
            100 -> {
                val achievement = Achievement.HUNDRED_DAY_STREAK
                appIntegrationManager.unlockAchievement(achievement.id)
            }
        }
    }

    private suspend fun handleMilestoneCelebration(milestone: ProgressMilestone) {
        // Show milestone celebration
        animationManager.showMilestoneCelebration(milestone)

        // Create social post if enabled
        val settings = appIntegrationManager.getUserSettings().first()
        if (settings.socialSharingEnabled) {
            appIntegrationManager.createSocialActivity(
                SocialActivity.milestoneReached(milestone)
            )
        }
    }
}

data class TaskCompletionDetails(
    val studyMinutes: Int,
    val quality: TaskQuality,
    val notes: String
)

enum class TaskQuality(val displayName: String, val multiplier: Float) {
    POOR("Struggled", 0.8f),
    OKAY("Okay", 1.0f),
    GOOD("Good", 1.2f),
    EXCELLENT("Excellent", 1.5f)
}

data class TaskCompletionResult(
    val task: Task,
    val pointsEarned: Int,
    val newTotalPoints: Int,
    val previousStreak: Int?,
    val newStreak: Int?,
    val streakExtended: Boolean,
    val newAchievements: List<Achievement>,
    val triggeredMilestone: Boolean,
    val milestone: ProgressMilestone?
)

// Extension functions for Achievement constants
object Achievement {
    val WEEK_STREAK = createAchievement("week_streak", "Week Warrior", "Maintain a 7-day streak")
    val MONTH_STREAK = createAchievement("month_streak", "Month Master", "Maintain a 30-day streak")
    val HUNDRED_DAY_STREAK = createAchievement("hundred_day_streak", "Century Scholar", "Maintain a 100-day streak")

    private fun createAchievement(id: String, title: String, description: String) =
        Achievement(id, title, description, 100, AchievementCategory.STREAK)
}

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val pointsReward: Int,
    val category: AchievementCategory
)

enum class AchievementCategory {
    STREAK,
    TASKS,
    STUDY_TIME,
    SOCIAL
}

data class ProgressMilestone(
    val id: String,
    val title: String,
    val description: String,
    val type: MilestoneType
)

enum class MilestoneType {
    DAILY_GOAL,
    WEEKLY_GOAL,
    MONTHLY_GOAL,
    CUSTOM_GOAL
}