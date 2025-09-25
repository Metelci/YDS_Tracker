package com.mtlc.studyplan.eventbus

import kotlinx.serialization.Serializable

/**
 * Base sealed interface for all events in the system
 */
sealed interface Event {
    val timestamp: Long get() = System.currentTimeMillis()
    val eventId: String get() = "${this::class.simpleName}_${timestamp}_${hashCode()}"
}

/**
 * Events related to task operations
 */
sealed interface TaskEvent : Event {
    @Serializable
    data class TaskCreated(
        val taskId: String,
        val taskTitle: String,
        val category: String,
        val priority: String
    ) : TaskEvent

    @Serializable
    data class TaskCompleted(
        val taskId: String,
        val taskTitle: String,
        val category: String,
        val studyMinutes: Int,
        val pointsEarned: Int
    ) : TaskEvent

    @Serializable
    data class TaskUpdated(
        val taskId: String,
        val taskTitle: String,
        val changedFields: List<String>
    ) : TaskEvent

    @Serializable
    data class TaskDeleted(
        val taskId: String,
        val taskTitle: String
    ) : TaskEvent

    @Serializable
    data class TaskMoved(
        val taskId: String,
        val oldCategory: String,
        val newCategory: String
    ) : TaskEvent

    @Serializable
    data class TasksReordered(
        val taskIds: List<String>,
        val context: String
    ) : TaskEvent
}

/**
 * Events related to progress tracking
 */
sealed interface ProgressEvent : Event {
    @Serializable
    data class DailyGoalReached(
        val goalType: String, // "tasks" or "minutes"
        val goalValue: Int,
        val actualValue: Int
    ) : ProgressEvent

    @Serializable
    data class WeeklyGoalReached(
        val goalType: String,
        val goalValue: Int,
        val actualValue: Int
    ) : ProgressEvent

    @Serializable
    data class ProgressUpdated(
        val date: String,
        val tasksCompleted: Int,
        val studyMinutes: Int,
        val pointsEarned: Int
    ) : ProgressEvent

    @Serializable
    data class EfficiencyMilestone(
        val efficiency: Float,
        val previousBest: Float
    ) : ProgressEvent
}

/**
 * Events related to achievements
 */
sealed interface AchievementEvent : Event {
    @Serializable
    data class AchievementUnlocked(
        val achievementId: String,
        val achievementTitle: String,
        val achievementDescription: String,
        val pointsReward: Int,
        val category: String,
        val rarity: String
    ) : AchievementEvent

    @Serializable
    data class AchievementProgress(
        val achievementId: String,
        val currentProgress: Int,
        val threshold: Int,
        val progressPercentage: Float
    ) : AchievementEvent

    @Serializable
    data class CategoryCompleted(
        val category: String,
        val completedCount: Int,
        val totalCount: Int
    ) : AchievementEvent

    @Serializable
    data class RarityMilestone(
        val rarity: String,
        val unlockedCount: Int
    ) : AchievementEvent
}

/**
 * Events related to streaks
 */
sealed interface StreakEvent : Event {
    @Serializable
    data class StreakExtended(
        val streakType: String,
        val newStreakLength: Int,
        val isPersonalBest: Boolean
    ) : StreakEvent

    @Serializable
    data class StreakBroken(
        val streakType: String,
        val brokenStreakLength: Int,
        val longestStreak: Int
    ) : StreakEvent

    @Serializable
    data class StreakMilestone(
        val streakType: String,
        val milestone: Int, // e.g., 7, 30, 100 days
        val currentStreak: Int
    ) : StreakEvent

    @Serializable
    data class StreakFreezed(
        val streakType: String,
        val streakLength: Int,
        val freezesRemaining: Int
    ) : StreakEvent

    @Serializable
    data class PerfectDay(
        val tasksCompleted: Int,
        val studyMinutes: Int,
        val pointsEarned: Int,
        val efficiency: Float
    ) : StreakEvent

    @Serializable
    data class StreakRecovered(
        val streakType: String,
        val recoveredFromLength: Int,
        val newLength: Int
    ) : StreakEvent
}

/**
 * Events related to user settings
 */
sealed interface SettingsEvent : Event {
    @Serializable
    data class ThemeChanged(
        val newTheme: String,
        val previousTheme: String
    ) : SettingsEvent

    @Serializable
    data class GoalUpdated(
        val goalType: String,
        val newValue: Int,
        val previousValue: Int
    ) : SettingsEvent

    @Serializable
    data class NotificationSettingsChanged(
        val settingName: String,
        val enabled: Boolean
    ) : SettingsEvent

    @Serializable
    data class StudySessionSettingsChanged(
        val sessionLength: Int,
        val breakLength: Int,
        val autoStart: Boolean
    ) : SettingsEvent

    @Serializable
    data class PrivacySettingsChanged(
        val settingName: String,
        val enabled: Boolean
    ) : SettingsEvent
}

/**
 * Events related to social features
 */
sealed interface SocialEvent : Event {
    @Serializable
    data class ActivityCreated(
        val activityId: String,
        val activityType: String,
        val title: String,
        val pointsEarned: Int,
        val isPublic: Boolean
    ) : SocialEvent

    @Serializable
    data class ActivityShared(
        val activityId: String,
        val shareCount: Int
    ) : SocialEvent

    @Serializable
    data class ReactionAdded(
        val activityId: String,
        val emoji: String,
        val totalReactions: Int
    ) : SocialEvent

    @Serializable
    data class CommentAdded(
        val activityId: String,
        val comment: String,
        val totalComments: Int
    ) : SocialEvent

    @Serializable
    data class ChallengeStarted(
        val challengeName: String,
        val participants: Int
    ) : SocialEvent

    @Serializable
    data class ChallengeCompleted(
        val challengeName: String,
        val rank: Int,
        val totalParticipants: Int
    ) : SocialEvent
}

/**
 * Events related to UI state and navigation
 */
sealed interface UIEvent : Event {
    @Serializable
    data class NavigationRequested(
        val destination: String,
        val arguments: Map<String, String> = emptyMap()
    ) : UIEvent

    @Serializable
    data class BottomSheetRequested(
        val content: String,
        val data: Map<String, String> = emptyMap()
    ) : UIEvent

    @Serializable
    data class SnackbarRequested(
        val message: String,
        val actionLabel: String? = null,
        val duration: String = "SHORT"
    ) : UIEvent

    @Serializable
    data class DialogRequested(
        val title: String,
        val message: String,
        val type: String = "INFO" // INFO, WARNING, ERROR, CONFIRMATION
    ) : UIEvent

    @Serializable
    data class LoadingStateChanged(
        val component: String,
        val isLoading: Boolean
    ) : UIEvent

    @Serializable
    data class ErrorOccurred(
        val component: String,
        val errorMessage: String,
        val errorCode: String? = null,
        val isCritical: Boolean = false
    ) : UIEvent

    @Serializable
    data class RefreshRequested(
        val component: String,
        val reason: String = "user_action"
    ) : UIEvent
}

/**
 * Events related to data synchronization
 */
sealed interface SyncEvent : Event {
    @Serializable
    data class SyncStarted(
        val syncType: String, // "auto", "manual", "background"
        val components: List<String>
    ) : SyncEvent

    @Serializable
    data class SyncCompleted(
        val syncType: String,
        val components: List<String>,
        val duration: Long,
        val changes: Int
    ) : SyncEvent

    @Serializable
    data class SyncFailed(
        val syncType: String,
        val component: String,
        val errorMessage: String,
        val retryCount: Int
    ) : SyncEvent

    @Serializable
    data class ConflictDetected(
        val component: String,
        val localVersion: String,
        val remoteVersion: String,
        val conflictType: String
    ) : SyncEvent

    @Serializable
    data class DataBackedUp(
        val backupType: String,
        val dataSize: Long,
        val location: String
    ) : SyncEvent
}

/**
 * Events related to notifications
 */
sealed interface NotificationEvent : Event {
    @Serializable
    data class NotificationScheduled(
        val notificationId: String,
        val type: String,
        val scheduledTime: Long
    ) : NotificationEvent

    @Serializable
    data class NotificationShown(
        val notificationId: String,
        val type: String
    ) : NotificationEvent

    @Serializable
    data class NotificationClicked(
        val notificationId: String,
        val action: String
    ) : NotificationEvent

    @Serializable
    data class NotificationDismissed(
        val notificationId: String,
        val dismissalReason: String
    ) : NotificationEvent

    @Serializable
    data class ReminderTriggered(
        val reminderType: String,
        val content: String
    ) : NotificationEvent
}

/**
 * Events related to analytics and performance
 */
sealed interface AnalyticsEvent : Event {
    @Serializable
    data class UserActionTracked(
        val action: String,
        val screen: String,
        val properties: Map<String, String> = emptyMap()
    ) : AnalyticsEvent

    @Serializable
    data class PerformanceMetric(
        val metric: String,
        val value: Double,
        val unit: String,
        val context: String
    ) : AnalyticsEvent

    @Serializable
    data class ErrorTracked(
        val errorType: String,
        val errorMessage: String,
        val stackTrace: String? = null,
        val context: Map<String, String> = emptyMap()
    ) : AnalyticsEvent

    @Serializable
    data class FeatureUsage(
        val feature: String,
        val usageCount: Int,
        val usageTime: Long
    ) : AnalyticsEvent
}