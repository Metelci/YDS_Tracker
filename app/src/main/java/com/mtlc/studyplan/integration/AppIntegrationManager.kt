package com.mtlc.studyplan.integration

import com.mtlc.studyplan.data.StudyStreak
import com.mtlc.studyplan.data.Task
import com.mtlc.studyplan.data.TaskRepository
import com.mtlc.studyplan.data.TaskStats
import com.mtlc.studyplan.repository.UserSettingsRepository
import com.mtlc.studyplan.settings.data.SettingsKeys
import com.mtlc.studyplan.settings.data.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton


data class AchievementBadge(
    val id: String,
    val title: String,
    val description: String,
    val isUnlocked: Boolean,
    val unlockedAt: Long? = null
)

private data class AchievementDefinition(
    val id: String,
    val title: String,
    val description: String,
    val condition: (AchievementContext) -> Boolean
)

data class AchievementContext(
    val stats: TaskStats,
    val streak: StudyStreak,
    val earlyCompletions: Int,
    val lateNightCompletions: Int,
    val weekendCompletions: Int,
    val weekdayCompletions: Int,
    val highPriorityCount: Int,
    val criticalPriorityCount: Int,
    val totalPoints: Int,
    val maxDailyTasks: Int,
    val vocabularyCount: Int,
    val grammarCount: Int,
    val readingCount: Int,
    val listeningCount: Int
)

private val achievementDefinitions = listOf(
    // Task Completion Milestones
    AchievementDefinition(
        id = "task_starter",
        title = "Getting Started",
        description = "Complete your first task",
        condition = { ctx -> ctx.stats.completedTasks >= 1 }
    ),
    AchievementDefinition(
        id = "committed_learner",
        title = "Committed Learner",
        description = "Complete 10 tasks",
        condition = { ctx -> ctx.stats.completedTasks >= 10 }
    ),
    AchievementDefinition(
        id = "task_crusher",
        title = "Task Crusher",
        description = "Complete 25 tasks",
        condition = { ctx -> ctx.stats.completedTasks >= 25 }
    ),
    AchievementDefinition(
        id = "task_master",
        title = "Task Master",
        description = "Complete 50 tasks",
        condition = { ctx -> ctx.stats.completedTasks >= 50 }
    ),
    AchievementDefinition(
        id = "century_maker",
        title = "Century Maker",
        description = "Complete 100 tasks",
        condition = { ctx -> ctx.stats.completedTasks >= 100 }
    ),
    AchievementDefinition(
        id = "task_legend",
        title = "Task Legend",
        description = "Complete 250 tasks",
        condition = { ctx -> ctx.stats.completedTasks >= 250 }
    ),
    AchievementDefinition(
        id = "unstoppable",
        title = "Unstoppable",
        description = "Complete 500 tasks",
        condition = { ctx -> ctx.stats.completedTasks >= 500 }
    ),

    // Streak Achievements
    AchievementDefinition(
        id = "streak_builder",
        title = "Streak Builder",
        description = "Maintain a 3-day study streak",
        condition = { ctx -> ctx.streak.currentStreak >= 3 }
    ),
    AchievementDefinition(
        id = "streak_master",
        title = "Streak Master",
        description = "Maintain a 7-day study streak",
        condition = { ctx -> ctx.streak.currentStreak >= 7 }
    ),
    AchievementDefinition(
        id = "week_warrior",
        title = "Week Warrior",
        description = "Maintain a 14-day study streak",
        condition = { ctx -> ctx.streak.currentStreak >= 14 }
    ),
    AchievementDefinition(
        id = "monthly_champion",
        title = "Monthly Champion",
        description = "Maintain a 30-day study streak",
        condition = { ctx -> ctx.streak.currentStreak >= 30 }
    ),
    AchievementDefinition(
        id = "consistency_king",
        title = "Consistency King",
        description = "Maintain a 60-day study streak",
        condition = { ctx -> ctx.streak.currentStreak >= 60 }
    ),
    AchievementDefinition(
        id = "legendary_streak",
        title = "Legendary Streak",
        description = "Maintain a 100-day study streak",
        condition = { ctx -> ctx.streak.currentStreak >= 100 }
    ),

    // Time-Based Achievements
    AchievementDefinition(
        id = "early_bird",
        title = "Early Bird",
        description = "Complete 5 tasks before 9 AM",
        condition = { ctx -> ctx.earlyCompletions >= 5 }
    ),
    AchievementDefinition(
        id = "night_owl",
        title = "Night Owl",
        description = "Complete 10 tasks after 9 PM",
        condition = { ctx -> ctx.lateNightCompletions >= 10 }
    ),
    AchievementDefinition(
        id = "weekday_warrior",
        title = "Weekday Warrior",
        description = "Complete 20 tasks on weekdays",
        condition = { ctx -> ctx.weekdayCompletions >= 20 }
    ),
    AchievementDefinition(
        id = "weekend_hustler",
        title = "Weekend Hustler",
        description = "Complete 10 tasks on weekends",
        condition = { ctx -> ctx.weekendCompletions >= 10 }
    ),

    // Priority-Based Achievements
    AchievementDefinition(
        id = "priority_master",
        title = "Priority Master",
        description = "Complete 10 high-priority tasks",
        condition = { ctx -> ctx.highPriorityCount >= 10 }
    ),
    AchievementDefinition(
        id = "crisis_manager",
        title = "Crisis Manager",
        description = "Complete 5 critical-priority tasks",
        condition = { ctx -> ctx.criticalPriorityCount >= 5 }
    ),

    // Category-Based Achievements (YDS Focused)
    AchievementDefinition(
        id = "vocabulary_wizard",
        title = "Vocabulary Wizard",
        description = "Complete 25 vocabulary tasks",
        condition = { ctx -> ctx.vocabularyCount >= 25 }
    ),
    AchievementDefinition(
        id = "grammar_guru",
        title = "Grammar Guru",
        description = "Complete 25 grammar tasks",
        condition = { ctx -> ctx.grammarCount >= 25 }
    ),
    AchievementDefinition(
        id = "reading_expert",
        title = "Reading Expert",
        description = "Complete 25 reading tasks",
        condition = { ctx -> ctx.readingCount >= 25 }
    ),
    AchievementDefinition(
        id = "listening_master",
        title = "Listening Master",
        description = "Complete 25 listening tasks",
        condition = { ctx -> ctx.listeningCount >= 25 }
    ),
    AchievementDefinition(
        id = "yds_allstar",
        title = "YDS All-Star",
        description = "Complete at least 10 tasks in each YDS category",
        condition = { ctx ->
            ctx.vocabularyCount >= 10 && ctx.grammarCount >= 10 &&
            ctx.readingCount >= 10 && ctx.listeningCount >= 10
        }
    ),

    // Speed & Efficiency
    AchievementDefinition(
        id = "speed_demon",
        title = "Speed Demon",
        description = "Complete 5 tasks in one day",
        condition = { ctx -> ctx.maxDailyTasks >= 5 }
    ),
    AchievementDefinition(
        id = "marathon_runner",
        title = "Marathon Runner",
        description = "Complete 10 tasks in one day",
        condition = { ctx -> ctx.maxDailyTasks >= 10 }
    ),

    // Points & Rewards
    AchievementDefinition(
        id = "point_collector",
        title = "Point Collector",
        description = "Earn 500 total points",
        condition = { ctx -> ctx.totalPoints >= 500 }
    ),
    AchievementDefinition(
        id = "point_master",
        title = "Point Master",
        description = "Earn 1000 total points",
        condition = { ctx -> ctx.totalPoints >= 1000 }
    ),
    AchievementDefinition(
        id = "point_legend",
        title = "Point Legend",
        description = "Earn 2500 total points",
        condition = { ctx -> ctx.totalPoints >= 2500 }
    ),
    AchievementDefinition(
        id = "point_tycoon",
        title = "Point Tycoon",
        description = "Earn 5000 total points",
        condition = { ctx -> ctx.totalPoints >= 5000 }
    )
)

private fun initialAchievementBadges(): List<AchievementBadge> = achievementDefinitions.map { definition ->
    AchievementBadge(
        id = definition.id,
        title = definition.title,
        description = definition.description,
        isUnlocked = false,
        unlockedAt = null
    )
}
@Singleton
class AppIntegrationManager @Inject constructor(
    private val taskRepository: TaskRepository,
    private val settingsRepository: SettingsRepository,
    private val userSettingsRepository: UserSettingsRepository
) {

    private val _studyStreak = MutableStateFlow(StudyStreak())
    val studyStreak: StateFlow<StudyStreak> = _studyStreak.asStateFlow()

    private val _achievements = MutableStateFlow(initialAchievementBadges())
    val achievements: StateFlow<List<AchievementBadge>> = _achievements.asStateFlow()

    private val _newAchievementsCount = MutableStateFlow(0)
    private val _settingsUpdateCount = MutableStateFlow(0)

    // Core data flows for badge system
    fun getPendingTasksCount(): Flow<Int> {
        return taskRepository.getAllTasks().map { tasks ->
            tasks.count { !it.isCompleted }
        }
    }

    fun getNewAchievementsCount(): Flow<Int> {
        return _newAchievementsCount.asStateFlow()
    }

    fun getStreakRiskStatus(): Flow<Boolean> {
        return studyStreak.map { streak ->
            !streak.isActiveToday() && streak.currentStreak > 0
        }
    }

    fun getSettingsUpdateCount(): Flow<Int> {
        return _settingsUpdateCount.asStateFlow()
    }

    // Core task operations
    fun getAllTasks(): Flow<List<Task>> {
        return taskRepository.getAllTasks()
    }

    suspend fun addTask(task: Task): Result<Task> {
        return try {
            val savedTask = taskRepository.insertTask(task)
            Result.success(savedTask)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTask(task: Task): Result<Task> {
        return try {
            val updatedTask = taskRepository.updateTask(task)
            updateStreakIfNeeded(task)
            checkForNewAchievements()
            Result.success(updatedTask)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTask(taskId: String): Result<Unit> {
        return try {
            taskRepository.deleteTask(taskId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun completeTask(taskId: String): Result<Task> {
        return try {
            val task = taskRepository.getTaskById(taskId)
            if (task != null) {
                val completedTask = task.copy(
                    isCompleted = true,
                    completedAt = System.currentTimeMillis()
                )
                val updatedTask = taskRepository.updateTask(completedTask)
                updateStreakIfNeeded(completedTask)
                checkForNewAchievements()
                Result.success(updatedTask)
            } else {
                Result.failure(Exception("Task not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Statistics and analytics
    suspend fun getTaskStats(): TaskStats {
        val allTasks = taskRepository.getAllTasksSync()
        val completed = allTasks.count { it.isCompleted }
        val total = allTasks.size
        val pending = total - completed
        val overdue = allTasks.count { task ->
            !task.isCompleted && task.dueDate != null && task.dueDate < System.currentTimeMillis()
        }

        return TaskStats(
            totalTasks = total,
            completedTasks = completed,
            pendingTasks = pending,
            overdueTasks = overdue,
            completionRate = if (total > 0) completed.toFloat() / total.toFloat() else 0f
        )
    }

    fun getTaskStatsFlow(): Flow<TaskStats> {
        return taskRepository.getAllTasks().map { allTasks ->
            val completed = allTasks.count { it.isCompleted }
            val total = allTasks.size
            val pending = total - completed
            val overdue = allTasks.count { task ->
                !task.isCompleted && task.dueDate != null && task.dueDate < System.currentTimeMillis()
            }

            TaskStats(
                totalTasks = total,
                completedTasks = completed,
                pendingTasks = pending,
                overdueTasks = overdue,
                completionRate = if (total > 0) completed.toFloat() / total.toFloat() else 0f
            )
        }
    }

    suspend fun getTodaysTasks(): List<Task> {
        return taskRepository.getTodaysTasks()
    }

    suspend fun getUpcomingTasks(): List<Task> {
        return taskRepository.getUpcomingTasks()
    }

    suspend fun getTasksByCategory(category: String): List<Task> {
        return taskRepository.getTasksByCategory(category)
    }

    // Achievement system
    private suspend fun checkForNewAchievements() {
        val stats = getTaskStats()
        val streak = _studyStreak.value

        // Build comprehensive achievement context
        val context = AchievementContext(
            stats = stats,
            streak = streak,
            earlyCompletions = taskRepository.getEarlyMorningCompletedTasks().size,
            lateNightCompletions = taskRepository.getLateNightCompletedTasks().size,
            weekendCompletions = taskRepository.getWeekendCompletedTasks().size,
            weekdayCompletions = taskRepository.getWeekdayCompletedTasks().size,
            highPriorityCount = taskRepository.getTasksByPriority(com.mtlc.studyplan.data.TaskPriority.HIGH).size,
            criticalPriorityCount = taskRepository.getTasksByPriority(com.mtlc.studyplan.data.TaskPriority.CRITICAL).size,
            totalPoints = taskRepository.getTotalPointsEarned(),
            maxDailyTasks = taskRepository.getMaxTasksCompletedInOneDay(),
            vocabularyCount = taskRepository.getTasksByCategory("YDS Vocabulary").count { it.isCompleted },
            grammarCount = taskRepository.getTasksByCategory("YDS Grammar").count { it.isCompleted },
            readingCount = taskRepository.getTasksByCategory("YDS Reading").count { it.isCompleted },
            listeningCount = taskRepository.getTasksByCategory("YDS Listening").count { it.isCompleted }
        )

        val previous = _achievements.value.associateBy { it.id }

        val updatedBadges = achievementDefinitions.map { definition ->
            val wasUnlocked = previous[definition.id]?.isUnlocked == true
            val isUnlocked = definition.condition(context)
            val unlockedAt = when {
                !isUnlocked -> null
                wasUnlocked -> previous[definition.id]?.unlockedAt
                else -> System.currentTimeMillis()
            }

            AchievementBadge(
                id = definition.id,
                title = definition.title,
                description = definition.description,
                isUnlocked = isUnlocked,
                unlockedAt = unlockedAt
            )
        }

        val newlyUnlocked = updatedBadges.count { badge ->
            badge.isUnlocked && previous[badge.id]?.isUnlocked != true
        }

        if (newlyUnlocked > 0) {
            _newAchievementsCount.value = _newAchievementsCount.value + newlyUnlocked
        }

        _achievements.value = updatedBadges
    }

    private suspend fun updateStreakIfNeeded(task: Task) {
        if (task.isCompleted && task.completedAt != null) {
            val currentStreak = _studyStreak.value
            val today = System.currentTimeMillis()
            val oneDayMs = 24 * 60 * 60 * 1000

            val newStreak = if (currentStreak.lastCompletionDate == null ||
                today - currentStreak.lastCompletionDate > oneDayMs
            ) {
                // First task today or streak broken
                if (currentStreak.lastCompletionDate != null &&
                    today - currentStreak.lastCompletionDate <= 2 * oneDayMs
                ) {
                    // Within 2 days, continue streak
                    currentStreak.copy(
                        currentStreak = currentStreak.currentStreak + 1,
                        lastCompletionDate = today,
                        longestStreak = maxOf(currentStreak.longestStreak, currentStreak.currentStreak + 1)
                    )
                } else {
                    // Start new streak
                    currentStreak.copy(
                        currentStreak = 1,
                        lastCompletionDate = today,
                        longestStreak = maxOf(currentStreak.longestStreak, 1)
                    )
                }
            } else {
                // Same day, just update timestamp
                currentStreak.copy(lastCompletionDate = today)
            }

            _studyStreak.value = newStreak
        }
    }

    suspend fun markAchievementsAsViewed() {
        _newAchievementsCount.value = 0
    }

    suspend fun markSettingsUpdatesAsViewed() {
        _settingsUpdateCount.value = 0
    }

    fun simulateSettingsUpdate() {
        _settingsUpdateCount.value = _settingsUpdateCount.value + 1
    }

    // Notification configuration for workers
    fun getNotificationConfig(): NotificationConfig {
        val notificationsEnabled = settingsRepository.getBoolean(SettingsKeys.Notifications.PUSH_NOTIFICATIONS, true)
        val studyRemindersEnabled = settingsRepository.getBoolean(SettingsKeys.Notifications.STUDY_REMINDERS, true)
        val achievementAlertsEnabled = settingsRepository.getBoolean(SettingsKeys.Notifications.ACHIEVEMENT_ALERTS, true)
        val goalRemindersEnabled = settingsRepository.getBoolean(SettingsKeys.Notifications.GOAL_REMINDERS, true)
        val streakWarningsEnabled = settingsRepository.getBoolean(SettingsKeys.Notifications.STREAK_WARNINGS, true)
        val quietHoursEnabled = settingsRepository.getBoolean(SettingsKeys.Notifications.QUIET_HOURS_ENABLED, false)
        val quietHoursStart = settingsRepository.getString(SettingsKeys.Notifications.QUIET_HOURS_START, "22:00")
        val quietHoursEnd = settingsRepository.getString(SettingsKeys.Notifications.QUIET_HOURS_END, "08:00")
        val vibrationEnabled = settingsRepository.getBoolean(SettingsKeys.Notifications.VIBRATION_ENABLED, true)

        return NotificationConfig(
            areNotificationsEnabled = notificationsEnabled,
            allowStudyReminders = studyRemindersEnabled,
            allowAchievementAlerts = achievementAlertsEnabled,
            allowStreakWarnings = streakWarningsEnabled,
            allowGoalReminders = goalRemindersEnabled,
            useQuietHours = quietHoursEnabled,
            quietHoursStart = quietHoursStart,
            quietHoursEnd = quietHoursEnd,
            useVibration = vibrationEnabled,
            calendarIntegrationEnabled = studyRemindersEnabled
        )
    }

    // Study statistics for personalized notifications
    suspend fun getStudyStats(): StudyStats {
        val tasks = taskRepository.getAllTasksSync()
        val totalCompleted = tasks.count { it.isCompleted }
        val weeklyGoalMinutes = userSettingsRepository.getUserSettingsSync()?.weeklyStudyGoalMinutes ?: 0
        val weeklyGoalHours = (weeklyGoalMinutes / 60).coerceAtLeast(0)

        return StudyStats(
            totalTasksCompleted = totalCompleted,
            currentStreak = studyStreak.value.currentStreak,
            weeklyGoalHours = weeklyGoalHours
        )
    }}

/**
 * Notification configuration for workers
 */
data class NotificationConfig(
    val areNotificationsEnabled: Boolean,
    val allowStudyReminders: Boolean,
    val allowAchievementAlerts: Boolean,
    val allowStreakWarnings: Boolean,
    val allowGoalReminders: Boolean,
    val useQuietHours: Boolean,
    val quietHoursStart: String,
    val quietHoursEnd: String,
    val useVibration: Boolean,
    val calendarIntegrationEnabled: Boolean = true
)

/**
 * Study statistics for personalized notifications
 */
data class StudyStats(
    val totalTasksCompleted: Int,
    val currentStreak: Int,
    val weeklyGoalHours: Int
)











