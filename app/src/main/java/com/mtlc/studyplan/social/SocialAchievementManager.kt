package com.mtlc.studyplan.social

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.mtlc.studyplan.shared.AppIntegrationManager
import com.mtlc.studyplan.realtime.RealTimeUpdateManager
import com.mtlc.studyplan.realtime.AchievementUpdateType
import com.mtlc.studyplan.actions.Achievement
import com.mtlc.studyplan.actions.AchievementCategory
import com.mtlc.studyplan.data.Task
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocialAchievementManager @Inject constructor(
    private val appIntegrationManager: AppIntegrationManager,
    private val socialRepository: SocialRepository,
    private val realTimeUpdateManager: RealTimeUpdateManager
) {

    init {
        observeAchievementUnlocks()
    }

    private fun observeAchievementUnlocks() {
        CoroutineScope(Dispatchers.IO).launch {
            realTimeUpdateManager.achievementUpdates
                .collect { update ->
                    if (update.type == AchievementUpdateType.UNLOCKED) {
                        handleAchievementUnlock(update.achievement, update.relatedTask)
                    }
                }
        }
    }

    private suspend fun handleAchievementUnlock(
        achievement: Achievement,
        relatedTask: Task?
    ) {
        // Check if user allows social sharing
        val settings = appIntegrationManager.getUserSettings().first()
        if (!settings.socialSharingEnabled) return

        // Create social activity
        val socialActivity = SocialActivity(
            id = UUID.randomUUID().toString(),
            userId = "current_user", // Replace with actual user ID
            type = SocialActivityType.ACHIEVEMENT_UNLOCKED,
            content = generateAchievementMessage(achievement, relatedTask),
            achievementId = achievement.id,
            relatedTaskId = relatedTask?.id,
            pointsEarned = achievement.pointsReward,
            timestamp = System.currentTimeMillis(),
            isPublic = settings.publicAchievements
        )

        // Save to repository
        socialRepository.createActivity(socialActivity)

        // Notify friends if enabled
        if (settings.notifyFriendsOfAchievements) {
            notifyFriendsOfAchievement(achievement, socialActivity)
        }

        // Update leaderboards
        updateLeaderboards(achievement)

        // Create celebration event
        appIntegrationManager.createCelebrationEvent(
            CelebrationEvent(
                type = CelebrationEventType.ACHIEVEMENT_UNLOCKED,
                achievement = achievement,
                socialActivity = socialActivity
            )
        )
    }

    private fun generateAchievementMessage(
        achievement: Achievement,
        relatedTask: Task?
    ): String {
        return when (achievement.category) {
            AchievementCategory.STREAK -> {
                "🔥 Reached ${achievement.title}! ${achievement.description}"
            }
            AchievementCategory.TASKS -> {
                if (relatedTask != null) {
                    "✅ Unlocked '${achievement.title}' by completing '${relatedTask.title}'!"
                } else {
                    "✅ Unlocked '${achievement.title}'! ${achievement.description}"
                }
            }
            AchievementCategory.STUDY_TIME -> {
                "📚 Study milestone achieved! ${achievement.title} - ${achievement.description}"
            }
            AchievementCategory.SOCIAL -> {
                "👥 ${achievement.title}! ${achievement.description}"
            }
        }
    }

    private suspend fun notifyFriendsOfAchievement(
        achievement: Achievement,
        activity: SocialActivity
    ) {
        val friends = socialRepository.getFriends()

        friends.forEach { friend ->
            val notification = SocialNotification(
                id = UUID.randomUUID().toString(),
                recipientId = friend.userId,
                senderId = "current_user",
                type = SocialNotificationType.FRIEND_ACHIEVEMENT,
                title = "Friend Achievement!",
                content = "Your friend unlocked: ${achievement.title}",
                relatedActivityId = activity.id,
                timestamp = System.currentTimeMillis()
            )

            socialRepository.sendNotification(notification)
        }
    }

    private suspend fun updateLeaderboards(achievement: Achievement) {
        // Update weekly leaderboard
        socialRepository.updateLeaderboardEntry(
            leaderboardType = LeaderboardType.WEEKLY_ACHIEVEMENTS,
            userId = "current_user",
            points = achievement.pointsReward
        )

        // Update all-time leaderboard
        socialRepository.updateLeaderboardEntry(
            leaderboardType = LeaderboardType.ALL_TIME_ACHIEVEMENTS,
            userId = "current_user",
            points = achievement.pointsReward
        )
    }

    // Public methods for manual sharing
    suspend fun shareAchievement(
        achievement: Achievement,
        customMessage: String? = null
    ): Result<SocialActivity> {
        return try {
            val activity = SocialActivity(
                id = UUID.randomUUID().toString(),
                userId = "current_user",
                type = SocialActivityType.ACHIEVEMENT_SHARED,
                content = customMessage ?: generateAchievementMessage(achievement, null),
                achievementId = achievement.id,
                timestamp = System.currentTimeMillis(),
                isPublic = true
            )

            socialRepository.createActivity(activity)
            Result.success(activity)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun celebrateWithFriends(achievement: Achievement): Result<List<SocialNotification>> {
        return try {
            val friends = socialRepository.getFriends()
            val notifications = friends.map { friend ->
                SocialNotification(
                    id = UUID.randomUUID().toString(),
                    recipientId = friend.userId,
                    senderId = "current_user",
                    type = SocialNotificationType.CELEBRATION_REQUEST,
                    title = "Celebrate with me!",
                    content = "I just unlocked '${achievement.title}'! Send me a high-five 🙌",
                    relatedAchievementId = achievement.id,
                    timestamp = System.currentTimeMillis()
                )
            }

            notifications.forEach { notification ->
                socialRepository.sendNotification(notification)
            }

            Result.success(notifications)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun shareTaskCompletion(
        task: Task,
        pointsEarned: Int,
        message: String? = null
    ): Result<SocialActivity> {
        return try {
            val activity = SocialActivity(
                id = UUID.randomUUID().toString(),
                userId = "current_user",
                type = SocialActivityType.TASK_COMPLETED,
                content = message ?: "Just completed '${task.title}' and earned $pointsEarned points! 💪",
                relatedTaskId = task.id,
                pointsEarned = pointsEarned,
                timestamp = System.currentTimeMillis(),
                isPublic = true
            )

            socialRepository.createActivity(activity)
            Result.success(activity)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun shareStreak(streak: Int, message: String? = null): Result<SocialActivity> {
        return try {
            val activity = SocialActivity(
                id = UUID.randomUUID().toString(),
                userId = "current_user",
                type = SocialActivityType.STREAK_MILESTONE,
                content = message ?: "🔥 I'm on a $streak day streak! Can't stop, won't stop! 🚀",
                streakCount = streak,
                timestamp = System.currentTimeMillis(),
                isPublic = true
            )

            socialRepository.createActivity(activity)
            Result.success(activity)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reactToActivity(
        activityId: String,
        reaction: SocialReaction
    ): Result<SocialActivityReaction> {
        return try {
            val activityReaction = SocialActivityReaction(
                id = UUID.randomUUID().toString(),
                activityId = activityId,
                userId = "current_user",
                reaction = reaction,
                timestamp = System.currentTimeMillis()
            )

            socialRepository.addReaction(activityReaction)

            // Notify activity owner if it's not us
            val activity = socialRepository.getActivity(activityId)
            if (activity?.userId != "current_user") {
                val notification = SocialNotification(
                    id = UUID.randomUUID().toString(),
                    recipientId = activity?.userId ?: "",
                    senderId = "current_user",
                    type = SocialNotificationType.ACTIVITY_REACTION,
                    title = "Someone reacted to your post!",
                    content = "You received a ${reaction.emoji} reaction",
                    relatedActivityId = activityId,
                    timestamp = System.currentTimeMillis()
                )

                socialRepository.sendNotification(notification)
            }

            Result.success(activityReaction)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun commentOnActivity(
        activityId: String,
        comment: String
    ): Result<SocialActivityComment> {
        return try {
            val activityComment = SocialActivityComment(
                id = UUID.randomUUID().toString(),
                activityId = activityId,
                userId = "current_user",
                content = comment,
                timestamp = System.currentTimeMillis()
            )

            socialRepository.addComment(activityComment)

            // Notify activity owner
            val activity = socialRepository.getActivity(activityId)
            if (activity?.userId != "current_user") {
                val notification = SocialNotification(
                    id = UUID.randomUUID().toString(),
                    recipientId = activity?.userId ?: "",
                    senderId = "current_user",
                    type = SocialNotificationType.ACTIVITY_COMMENT,
                    title = "New comment on your post!",
                    content = comment.take(100),
                    relatedActivityId = activityId,
                    timestamp = System.currentTimeMillis()
                )

                socialRepository.sendNotification(notification)
            }

            Result.success(activityComment)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// Data classes for social features
data class SocialActivity(
    val id: String,
    val userId: String,
    val type: SocialActivityType,
    val content: String,
    val achievementId: String? = null,
    val relatedTaskId: String? = null,
    val pointsEarned: Int = 0,
    val streakCount: Int? = null,
    val timestamp: Long,
    val isPublic: Boolean = true,
    val reactions: List<SocialActivityReaction> = emptyList(),
    val comments: List<SocialActivityComment> = emptyList()
) {
    companion object {
        fun achievementUnlocked(achievement: Achievement) = SocialActivity(
            id = UUID.randomUUID().toString(),
            userId = "current_user",
            type = SocialActivityType.ACHIEVEMENT_UNLOCKED,
            content = "🏆 Just unlocked '${achievement.title}'!",
            achievementId = achievement.id,
            pointsEarned = achievement.pointsReward,
            timestamp = System.currentTimeMillis()
        )

        fun milestoneReached(milestone: com.mtlc.studyplan.actions.ProgressMilestone) = SocialActivity(
            id = UUID.randomUUID().toString(),
            userId = "current_user",
            type = SocialActivityType.MILESTONE_REACHED,
            content = "🎯 Reached milestone: ${milestone.title}! ${milestone.description}",
            timestamp = System.currentTimeMillis()
        )
    }
}

enum class SocialActivityType {
    ACHIEVEMENT_UNLOCKED,
    ACHIEVEMENT_SHARED,
    TASK_COMPLETED,
    STREAK_MILESTONE,
    MILESTONE_REACHED,
    STUDY_SESSION_COMPLETED,
    GOAL_ACHIEVED
}

data class SocialNotification(
    val id: String,
    val recipientId: String,
    val senderId: String,
    val type: SocialNotificationType,
    val title: String,
    val content: String,
    val relatedActivityId: String? = null,
    val relatedAchievementId: String? = null,
    val timestamp: Long,
    val isRead: Boolean = false
)

enum class SocialNotificationType {
    FRIEND_ACHIEVEMENT,
    CELEBRATION_REQUEST,
    ACTIVITY_REACTION,
    ACTIVITY_COMMENT,
    FRIEND_REQUEST,
    LEADERBOARD_UPDATE
}

data class SocialActivityReaction(
    val id: String,
    val activityId: String,
    val userId: String,
    val reaction: SocialReaction,
    val timestamp: Long
)

enum class SocialReaction(val emoji: String, val name: String) {
    LIKE("👍", "Like"),
    LOVE("❤️", "Love"),
    FIRE("🔥", "Fire"),
    CELEBRATE("🎉", "Celebrate"),
    CLAP("👏", "Clap"),
    STRONG("💪", "Strong")
}

data class SocialActivityComment(
    val id: String,
    val activityId: String,
    val userId: String,
    val content: String,
    val timestamp: Long
)

data class Friend(
    val userId: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val level: Int = 1,
    val currentStreak: Int = 0,
    val totalPoints: Int = 0
)

enum class LeaderboardType {
    WEEKLY_ACHIEVEMENTS,
    ALL_TIME_ACHIEVEMENTS,
    WEEKLY_POINTS,
    ALL_TIME_POINTS,
    CURRENT_STREAK
}

data class LeaderboardEntry(
    val userId: String,
    val username: String,
    val displayName: String,
    val points: Int,
    val rank: Int,
    val avatarUrl: String? = null
)

data class CelebrationEvent(
    val type: CelebrationEventType,
    val achievement: Achievement? = null,
    val socialActivity: SocialActivity? = null,
    val timestamp: Long = System.currentTimeMillis()
)

enum class CelebrationEventType {
    ACHIEVEMENT_UNLOCKED,
    STREAK_MILESTONE,
    GOAL_COMPLETED,
    LEVEL_UP
}

// Repository interface for social features
interface SocialRepository {
    suspend fun createActivity(activity: SocialActivity): Result<SocialActivity>
    suspend fun getActivity(id: String): SocialActivity?
    suspend fun getFeedActivities(limit: Int = 20): List<SocialActivity>
    suspend fun getUserActivities(userId: String, limit: Int = 20): List<SocialActivity>

    suspend fun addReaction(reaction: SocialActivityReaction): Result<SocialActivityReaction>
    suspend fun removeReaction(activityId: String, userId: String): Result<Unit>
    suspend fun getReactions(activityId: String): List<SocialActivityReaction>

    suspend fun addComment(comment: SocialActivityComment): Result<SocialActivityComment>
    suspend fun getComments(activityId: String): List<SocialActivityComment>

    suspend fun sendNotification(notification: SocialNotification): Result<SocialNotification>
    suspend fun getNotifications(userId: String): List<SocialNotification>
    suspend fun markNotificationAsRead(notificationId: String): Result<Unit>

    suspend fun getFriends(): List<Friend>
    suspend fun sendFriendRequest(userId: String): Result<Unit>
    suspend fun acceptFriendRequest(userId: String): Result<Unit>

    suspend fun updateLeaderboardEntry(leaderboardType: LeaderboardType, userId: String, points: Int): Result<Unit>
    suspend fun getLeaderboard(type: LeaderboardType, limit: Int = 10): List<LeaderboardEntry>
    suspend fun getUserRank(type: LeaderboardType, userId: String): Int?
}