package com.mtlc.studyplan.realtime

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.mtlc.studyplan.eventbus.AppEventBus
import com.mtlc.studyplan.eventbus.AppEvent
import com.mtlc.studyplan.animations.AnimationManager
import com.mtlc.studyplan.actions.TaskCompletionResult
import com.mtlc.studyplan.actions.Achievement
import com.mtlc.studyplan.data.Task
import com.mtlc.studyplan.data.UserProgress
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealTimeUpdateManager @Inject constructor(
    private val appEventBus: AppEventBus,
    private val animationManager: AnimationManager
) {

    // Progress update flows
    private val _progressUpdates = MutableSharedFlow<ProgressUpdate>()
    val progressUpdates: SharedFlow<ProgressUpdate> = _progressUpdates.asSharedFlow()

    private val _streakUpdates = MutableSharedFlow<StreakUpdate>()
    val streakUpdates: SharedFlow<StreakUpdate> = _streakUpdates.asSharedFlow()

    private val _achievementUpdates = MutableSharedFlow<AchievementUpdate>()
    val achievementUpdates: SharedFlow<AchievementUpdate> = _achievementUpdates.asSharedFlow()

    private val _taskUpdates = MutableSharedFlow<TaskUpdate>()
    val taskUpdates: SharedFlow<TaskUpdate> = _taskUpdates.asSharedFlow()

    private val _pointsUpdates = MutableSharedFlow<PointsUpdate>()
    val pointsUpdates: SharedFlow<PointsUpdate> = _pointsUpdates.asSharedFlow()

    init {
        observeAppEvents()
    }

    private fun observeAppEvents() {
        CoroutineScope(Dispatchers.Main).launch {
            appEventBus.events.collect { event ->
                when (event) {
                    is AppEvent.TaskCompleted -> {
                        handleTaskCompletionUpdate(event.result)
                    }
                    is AppEvent.TaskCreated -> {
                        handleTaskCreated(event.task)
                    }
                    is AppEvent.TaskUpdated -> {
                        handleTaskUpdated(event.task)
                    }
                    is AppEvent.ProgressUpdated -> {
                        handleProgressUpdate(event.progress)
                    }
                    is AppEvent.StreakUpdated -> {
                        handleStreakUpdate(event.newStreak, event.previousStreak)
                    }
                    is AppEvent.AchievementUnlocked -> {
                        handleAchievementUpdate(event.achievement)
                    }
                    is AppEvent.PointsEarned -> {
                        handlePointsUpdate(event.points, event.source)
                    }
                }
            }
        }
    }

    private suspend fun handleTaskCompletionUpdate(result: TaskCompletionResult) {
        // Emit task update
        _taskUpdates.emit(
            TaskUpdate(
                task = result.task,
                type = TaskUpdateType.COMPLETED,
                pointsEarned = result.pointsEarned
            )
        )

        // Emit progress update
        _progressUpdates.emit(
            ProgressUpdate(
                type = ProgressUpdateType.TASK_COMPLETED,
                task = result.task,
                pointsEarned = result.pointsEarned,
                newTotalPoints = result.newTotalPoints,
                studyMinutesAdded = result.task.actualMinutes ?: result.task.estimatedMinutes
            )
        )

        // Emit points update
        _pointsUpdates.emit(
            PointsUpdate(
                pointsEarned = result.pointsEarned,
                newTotal = result.newTotalPoints,
                source = PointsSource.TASK_COMPLETION,
                relatedTaskId = result.task.id
            )
        )

        // Emit streak update if applicable
        if (result.streakExtended) {
            _streakUpdates.emit(
                StreakUpdate(
                    oldStreak = result.previousStreak ?: 0,
                    newStreak = result.newStreak ?: 0,
                    isExtension = true,
                    milestone = determineStreakMilestone(result.newStreak ?: 0)
                )
            )
        }

        // Emit achievement updates
        result.newAchievements.forEach { achievement ->
            _achievementUpdates.emit(
                AchievementUpdate(
                    achievement = achievement,
                    type = AchievementUpdateType.UNLOCKED,
                    relatedTask = result.task
                )
            )
        }
    }

    private suspend fun handleTaskCreated(task: Task) {
        _taskUpdates.emit(
            TaskUpdate(
                task = task,
                type = TaskUpdateType.CREATED
            )
        )
    }

    private suspend fun handleTaskUpdated(task: Task) {
        _taskUpdates.emit(
            TaskUpdate(
                task = task,
                type = TaskUpdateType.UPDATED
            )
        )
    }

    private suspend fun handleProgressUpdate(progress: UserProgress) {
        _progressUpdates.emit(
            ProgressUpdate(
                type = ProgressUpdateType.DAILY_SUMMARY,
                dailyProgress = progress
            )
        )
    }

    private suspend fun handleStreakUpdate(newStreak: Int, previousStreak: Int) {
        _streakUpdates.emit(
            StreakUpdate(
                oldStreak = previousStreak,
                newStreak = newStreak,
                isExtension = newStreak > previousStreak,
                milestone = determineStreakMilestone(newStreak)
            )
        )
    }

    private suspend fun handleAchievementUpdate(achievement: Achievement) {
        _achievementUpdates.emit(
            AchievementUpdate(
                achievement = achievement,
                type = AchievementUpdateType.UNLOCKED
            )
        )
    }

    private suspend fun handlePointsUpdate(points: Int, source: String) {
        _pointsUpdates.emit(
            PointsUpdate(
                pointsEarned = points,
                newTotal = 0, // Would need to calculate from repository
                source = PointsSource.fromString(source),
                relatedTaskId = null
            )
        )
    }

    private fun determineStreakMilestone(streak: Int): StreakMilestone? {
        return when (streak) {
            7 -> StreakMilestone.WEEK_STREAK
            14 -> StreakMilestone.TWO_WEEK_STREAK
            30 -> StreakMilestone.MONTH_STREAK
            50 -> StreakMilestone.FIFTY_DAY_STREAK
            100 -> StreakMilestone.HUNDRED_DAY_STREAK
            365 -> StreakMilestone.YEAR_STREAK
            else -> null
        }
    }

    // Public methods for manual updates
    suspend fun notifyTaskCompletion(result: TaskCompletionResult) {
        handleTaskCompletionUpdate(result)
    }

    suspend fun notifyProgressUpdate(progress: UserProgress) {
        handleProgressUpdate(progress)
    }

    suspend fun notifyAchievementUnlock(achievement: Achievement, relatedTask: Task? = null) {
        _achievementUpdates.emit(
            AchievementUpdate(
                achievement = achievement,
                type = AchievementUpdateType.UNLOCKED,
                relatedTask = relatedTask
            )
        )
    }

    suspend fun notifyPointsEarned(points: Int, source: PointsSource, taskId: String? = null) {
        _pointsUpdates.emit(
            PointsUpdate(
                pointsEarned = points,
                newTotal = 0, // Would calculate from repository
                source = source,
                relatedTaskId = taskId
            )
        )
    }
}

// Data classes for updates
data class ProgressUpdate(
    val type: ProgressUpdateType,
    val task: Task? = null,
    val pointsEarned: Int = 0,
    val newTotalPoints: Int = 0,
    val studyMinutesAdded: Int = 0,
    val dailyProgress: UserProgress? = null,
    val timestamp: Long = System.currentTimeMillis()
)

enum class ProgressUpdateType {
    TASK_COMPLETED,
    DAILY_SUMMARY,
    MILESTONE_REACHED,
    GOAL_ACHIEVED
}

data class StreakUpdate(
    val oldStreak: Int,
    val newStreak: Int,
    val isExtension: Boolean,
    val milestone: StreakMilestone? = null,
    val timestamp: Long = System.currentTimeMillis()
)

enum class StreakMilestone(val displayName: String, val emoji: String) {
    WEEK_STREAK("Week Warrior", "🔥"),
    TWO_WEEK_STREAK("Two Week Champion", "🔥🔥"),
    MONTH_STREAK("Month Master", "🚀"),
    FIFTY_DAY_STREAK("Fifty Day Hero", "⭐"),
    HUNDRED_DAY_STREAK("Century Scholar", "💎"),
    YEAR_STREAK("Year Legend", "👑")
}

data class AchievementUpdate(
    val achievement: Achievement,
    val type: AchievementUpdateType,
    val relatedTask: Task? = null,
    val timestamp: Long = System.currentTimeMillis()
)

enum class AchievementUpdateType {
    UNLOCKED,
    PROGRESS_MADE,
    VIEWED,
    SHARED
}

data class TaskUpdate(
    val task: Task,
    val type: TaskUpdateType,
    val pointsEarned: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

enum class TaskUpdateType {
    CREATED,
    UPDATED,
    COMPLETED,
    DELETED,
    STARRED,
    UNSTARRED
}

data class PointsUpdate(
    val pointsEarned: Int,
    val newTotal: Int,
    val source: PointsSource,
    val relatedTaskId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

enum class PointsSource(val displayName: String) {
    TASK_COMPLETION("Task Completion"),
    STREAK_BONUS("Streak Bonus"),
    ACHIEVEMENT_UNLOCK("Achievement"),
    DAILY_BONUS("Daily Bonus"),
    QUALITY_BONUS("Quality Bonus"),
    SOCIAL_INTERACTION("Social"),
    OTHER("Other");

    companion object {
        fun fromString(value: String): PointsSource {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: OTHER
        }
    }
}

// Extension function for showing points animation
suspend fun RealTimeUpdateManager.showPointsAnimation(points: Int, source: PointsSource) {
    // This would trigger UI animations for points
    notifyPointsEarned(points, source)
}