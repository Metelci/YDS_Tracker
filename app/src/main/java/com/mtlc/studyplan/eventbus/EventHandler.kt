package com.mtlc.studyplan.eventbus

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import com.mtlc.studyplan.repository.*
import com.mtlc.studyplan.di.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central event handler that coordinates automated responses to events
 */
@Singleton
class EventHandler @Inject constructor(
    private val eventBus: EventBus,
    private val taskRepository: TaskRepository,
    private val progressRepository: ProgressRepository,
    private val achievementRepository: AchievementRepository,
    private val streakRepository: StreakRepository,
    private val socialRepository: SocialRepository,
    @ApplicationScope private val applicationScope: CoroutineScope
) {

    init {
        startEventHandling()
    }

    private fun startEventHandling() {
        handleTaskEvents()
        handleProgressEvents()
        handleAchievementEvents()
        handleStreakEvents()
        handleSocialEvents()
        handleSettingsEvents()
        handleSyncEvents()
        handleNotificationEvents()
    }

    /**
     * Handle task-related events
     */
    private fun handleTaskEvents() {
        // Handle task completion
        eventBus.subscribeToTaskEvents<TaskEvent.TaskCompleted>()
            .onEach { event ->
                handleTaskCompletion(event)
            }
            .launchIn(applicationScope)

        // Handle task creation
        eventBus.subscribeToTaskEvents<TaskEvent.TaskCreated>()
            .onEach { event ->
                handleTaskCreation(event)
            }
            .launchIn(applicationScope)
    }

    /**
     * Handle progress-related events
     */
    private fun handleProgressEvents() {
        // Handle daily goal reached
        eventBus.subscribeToProgressEvents<ProgressEvent.DailyGoalReached>()
            .onEach { event ->
                handleDailyGoalReached(event)
            }
            .launchIn(applicationScope)

        // Handle progress updates
        eventBus.subscribeToProgressEvents<ProgressEvent.ProgressUpdated>()
            .onEach { event ->
                handleProgressUpdate(event)
            }
            .launchIn(applicationScope)
    }

    /**
     * Handle achievement-related events
     */
    private fun handleAchievementEvents() {
        // Handle achievement unlocked
        eventBus.subscribeToAchievementEvents<AchievementEvent.AchievementUnlocked>()
            .onEach { event ->
                handleAchievementUnlocked(event)
            }
            .launchIn(applicationScope)
    }

    /**
     * Handle streak-related events
     */
    private fun handleStreakEvents() {
        // Handle streak extended
        eventBus.subscribeToStreakEvents<StreakEvent.StreakExtended>()
            .onEach { event ->
                handleStreakExtended(event)
            }
            .launchIn(applicationScope)

        // Handle streak milestone
        eventBus.subscribeToStreakEvents<StreakEvent.StreakMilestone>()
            .onEach { event ->
                handleStreakMilestone(event)
            }
            .launchIn(applicationScope)

        // Handle perfect day
        eventBus.subscribeToStreakEvents<StreakEvent.PerfectDay>()
            .onEach { event ->
                handlePerfectDay(event)
            }
            .launchIn(applicationScope)
    }

    /**
     * Handle social-related events
     */
    private fun handleSocialEvents() {
        // Handle activity creation
        eventBus.subscribeToSocialEvents<SocialEvent.ActivityCreated>()
            .onEach { event ->
                handleActivityCreation(event)
            }
            .launchIn(applicationScope)
    }

    /**
     * Handle settings-related events
     */
    private fun handleSettingsEvents() {
        // Handle goal updates
        eventBus.subscribeToSettingsEvents<SettingsEvent.GoalUpdated>()
            .onEach { event ->
                handleGoalUpdate(event)
            }
            .launchIn(applicationScope)
    }

    /**
     * Handle sync-related events
     */
    private fun handleSyncEvents() {
        // Handle sync completion
        eventBus.subscribeToSyncEvents<SyncEvent.SyncCompleted>()
            .onEach { event ->
                handleSyncCompletion(event)
            }
            .launchIn(applicationScope)
    }

    /**
     * Handle notification-related events
     */
    private fun handleNotificationEvents() {
        // Handle notification clicks
        eventBus.subscribeToNotificationEvents<NotificationEvent.NotificationClicked>()
            .onEach { event ->
                handleNotificationClick(event)
            }
            .launchIn(applicationScope)
    }

    // Event handling implementations

    private suspend fun handleTaskCompletion(event: TaskEvent.TaskCompleted) {
        applicationScope.launch {
            try {
                // Update daily progress
                progressRepository.updateDailyStats(
                    date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                        .format(java.util.Date()),
                    tasksCompleted = 1,
                    studyMinutes = event.studyMinutes,
                    pointsEarned = event.pointsEarned
                )

                // Check and update achievements
                val unlockedAchievements = achievementRepository.checkAndUpdateTaskAchievements(
                    taskRepository.getCompletedTasksCount()
                )

                // Create social activity
                socialRepository.createTaskCompletionActivity(
                    userId = "default_user",
                    taskId = event.taskId,
                    taskTitle = event.taskTitle,
                    category = event.category,
                    difficulty = "Normal", // Could be enhanced to get from task
                    pointsEarned = event.pointsEarned,
                    studyMinutes = event.studyMinutes
                )

                // Check if daily goal is reached
                val todayProgress = progressRepository.getTodayProgressOrCreate()
                val dailyTaskGoal = 5 // Could get from settings
                if (todayProgress.tasksCompleted >= dailyTaskGoal) {
                    eventBus.publish(
                        ProgressEvent.DailyGoalReached(
                            goalType = "tasks",
                            goalValue = dailyTaskGoal,
                            actualValue = todayProgress.tasksCompleted
                        )
                    )
                }

                // Publish UI success event
                eventBus.publish(
                    UIEvent.SnackbarRequested(
                        message = "Task '${event.taskTitle}' completed! +${event.pointsEarned} points",
                        duration = "SHORT"
                    )
                )

            } catch (e: Exception) {
                eventBus.publish(
                    UIEvent.ErrorOccurred(
                        component = "TaskCompletion",
                        errorMessage = "Failed to process task completion: ${e.message}",
                        isCritical = false
                    )
                )
            }
        }
    }

    private suspend fun handleTaskCreation(event: TaskEvent.TaskCreated) {
        applicationScope.launch {
            // Track analytics
            eventBus.publish(
                AnalyticsEvent.UserActionTracked(
                    action = "task_created",
                    screen = "task_management",
                    properties = mapOf(
                        "category" to event.category,
                        "priority" to event.priority
                    )
                )
            )
        }
    }

    private suspend fun handleDailyGoalReached(event: ProgressEvent.DailyGoalReached) {
        applicationScope.launch {
            try {
                // Update streak if goal met
                val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    .format(java.util.Date())

                streakRepository.updateDailyStreakProgress(
                    date = today,
                    tasksCompleted = event.actualValue,
                    studyMinutes = if (event.goalType == "minutes") event.actualValue else 0,
                    goalMet = true
                )

                // Check for achievements
                achievementRepository.checkAndUpdateTaskAchievements(event.actualValue)

                // Create social activity for goal achievement
                socialRepository.createCustomActivity(
                    userId = "default_user",
                    activityType = "DAILY_GOAL_REACHED",
                    title = "Daily ${event.goalType} goal reached!",
                    description = "Completed ${event.actualValue} ${event.goalType} (goal: ${event.goalValue})",
                    pointsEarned = 50,
                    isHighlight = true
                )

                // Show celebration UI
                eventBus.publish(
                    UIEvent.DialogRequested(
                        title = "Goal Achieved! 🎉",
                        message = "You've reached your daily ${event.goalType} goal of ${event.goalValue}!",
                        type = "INFO"
                    )
                )

            } catch (e: Exception) {
                eventBus.publish(
                    UIEvent.ErrorOccurred(
                        component = "GoalProcessing",
                        errorMessage = "Failed to process goal achievement: ${e.message}",
                        isCritical = false
                    )
                )
            }
        }
    }

    private suspend fun handleProgressUpdate(event: ProgressEvent.ProgressUpdated) {
        applicationScope.launch {
            // Check for efficiency milestones
            val efficiency = if (event.studyMinutes > 0) {
                event.tasksCompleted.toFloat() / event.studyMinutes * 60 // tasks per hour
            } else 0f

            // Track analytics
            eventBus.publish(
                AnalyticsEvent.UserActionTracked(
                    action = "progress_updated",
                    screen = "progress_tracking",
                    properties = mapOf(
                        "tasks_completed" to event.tasksCompleted.toString(),
                        "study_minutes" to event.studyMinutes.toString(),
                        "efficiency" to efficiency.toString()
                    )
                )
            )
        }
    }

    private suspend fun handleAchievementUnlocked(event: AchievementEvent.AchievementUnlocked) {
        applicationScope.launch {
            try {
                // Create social activity
                socialRepository.createAchievementActivity(
                    userId = "default_user",
                    achievementId = event.achievementId,
                    achievementTitle = event.achievementTitle,
                    achievementDescription = event.achievementDescription,
                    pointsEarned = event.pointsReward
                )

                // Show achievement notification
                eventBus.publish(
                    UIEvent.DialogRequested(
                        title = "Achievement Unlocked! 🏆",
                        message = "${event.achievementTitle}\n${event.achievementDescription}\n+${event.pointsReward} points!",
                        type = "INFO"
                    )
                )

                // Schedule notification
                eventBus.publish(
                    NotificationEvent.NotificationScheduled(
                        notificationId = "achievement_${event.achievementId}",
                        type = "ACHIEVEMENT_UNLOCKED",
                        scheduledTime = System.currentTimeMillis()
                    )
                )

            } catch (e: Exception) {
                eventBus.publish(
                    UIEvent.ErrorOccurred(
                        component = "AchievementProcessing",
                        errorMessage = "Failed to process achievement unlock: ${e.message}",
                        isCritical = false
                    )
                )
            }
        }
    }

    private suspend fun handleStreakExtended(event: StreakEvent.StreakExtended) {
        applicationScope.launch {
            try {
                // Check for streak milestones
                val milestones = listOf(7, 14, 30, 50, 100, 200, 365)
                if (event.newStreakLength in milestones) {
                    eventBus.publish(
                        StreakEvent.StreakMilestone(
                            streakType = event.streakType,
                            milestone = event.newStreakLength,
                            currentStreak = event.newStreakLength
                        )
                    )
                }

                // Show streak update
                if (event.isPersonalBest) {
                    eventBus.publish(
                        UIEvent.SnackbarRequested(
                            message = "New personal best streak: ${event.newStreakLength} days! 🔥",
                            duration = "LONG"
                        )
                    )
                }

            } catch (e: Exception) {
                eventBus.publish(
                    UIEvent.ErrorOccurred(
                        component = "StreakProcessing",
                        errorMessage = "Failed to process streak extension: ${e.message}",
                        isCritical = false
                    )
                )
            }
        }
    }

    private suspend fun handleStreakMilestone(event: StreakEvent.StreakMilestone) {
        applicationScope.launch {
            try {
                // Create social activity
                socialRepository.createStreakMilestoneActivity(
                    userId = "default_user",
                    streakDay = event.milestone,
                    pointsEarned = event.milestone * 10 // 10 points per milestone day
                )

                // Check for achievements
                achievementRepository.checkAndUpdateStreakAchievements(event.currentStreak)

                // Show milestone celebration
                eventBus.publish(
                    UIEvent.DialogRequested(
                        title = "Streak Milestone! 🔥",
                        message = "You've reached a ${event.milestone}-day ${event.streakType} streak!",
                        type = "INFO"
                    )
                )

            } catch (e: Exception) {
                eventBus.publish(
                    UIEvent.ErrorOccurred(
                        component = "StreakMilestone",
                        errorMessage = "Failed to process streak milestone: ${e.message}",
                        isCritical = false
                    )
                )
            }
        }
    }

    private suspend fun handlePerfectDay(event: StreakEvent.PerfectDay) {
        applicationScope.launch {
            try {
                // Create social activity
                socialRepository.createPerfectDayActivity(
                    userId = "default_user",
                    tasksCompleted = event.tasksCompleted,
                    studyMinutes = event.studyMinutes,
                    pointsEarned = event.pointsEarned
                )

                // Update streak with perfect day
                val dailyStreak = streakRepository.getOrCreateDailyStreak()
                streakRepository.incrementPerfectDays(dailyStreak.id)

                // Show perfect day celebration
                eventBus.publish(
                    UIEvent.DialogRequested(
                        title = "Perfect Study Day! ⭐",
                        message = "Amazing work! You completed ${event.tasksCompleted} tasks and studied for ${event.studyMinutes} minutes with ${event.efficiency} efficiency!",
                        type = "INFO"
                    )
                )

            } catch (e: Exception) {
                eventBus.publish(
                    UIEvent.ErrorOccurred(
                        component = "PerfectDay",
                        errorMessage = "Failed to process perfect day: ${e.message}",
                        isCritical = false
                    )
                )
            }
        }
    }

    private suspend fun handleActivityCreation(event: SocialEvent.ActivityCreated) {
        applicationScope.launch {
            // Track analytics
            eventBus.publish(
                AnalyticsEvent.UserActionTracked(
                    action = "social_activity_created",
                    screen = "social",
                    properties = mapOf(
                        "activity_type" to event.activityType,
                        "points_earned" to event.pointsEarned.toString(),
                        "is_public" to event.isPublic.toString()
                    )
                )
            )
        }
    }

    private suspend fun handleGoalUpdate(event: SettingsEvent.GoalUpdated) {
        applicationScope.launch {
            // Refresh relevant UI components
            eventBus.publish(
                UIEvent.RefreshRequested(
                    component = "progress_dashboard",
                    reason = "goal_updated"
                )
            )
        }
    }

    private suspend fun handleSyncCompletion(event: SyncEvent.SyncCompleted) {
        applicationScope.launch {
            if (event.changes > 0) {
                eventBus.publish(
                    UIEvent.SnackbarRequested(
                        message = "Sync completed: ${event.changes} changes",
                        duration = "SHORT"
                    )
                )
            }
        }
    }

    private suspend fun handleNotificationClick(event: NotificationEvent.NotificationClicked) {
        applicationScope.launch {
            // Navigate based on notification type and action
            when (event.action) {
                "open_tasks" -> eventBus.publish(
                    UIEvent.NavigationRequested("tasks")
                )
                "open_progress" -> eventBus.publish(
                    UIEvent.NavigationRequested("progress")
                )
                "open_achievements" -> eventBus.publish(
                    UIEvent.NavigationRequested("achievements")
                )
            }
        }
    }
}