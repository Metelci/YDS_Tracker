package com.mtlc.studyplan.integration

import com.mtlc.studyplan.data.StudyStreak
import com.mtlc.studyplan.data.Task
import com.mtlc.studyplan.data.TaskRepository
import com.mtlc.studyplan.data.TaskStats
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
    val condition: (TaskStats, StudyStreak, Int) -> Boolean
)

private val achievementDefinitions = listOf(
    AchievementDefinition(
        id = "task_starter",
        title = "Getting Started",
        description = "Complete your first task",
        condition = { stats, _, _ -> stats.completedTasks >= 1 }
    ),
    AchievementDefinition(
        id = "task_crusher",
        title = "Task Crusher",
        description = "Complete 25 tasks",
        condition = { stats, _, _ -> stats.completedTasks >= 25 }
    ),
    AchievementDefinition(
        id = "task_master",
        title = "Task Master",
        description = "Complete 50 tasks",
        condition = { stats, _, _ -> stats.completedTasks >= 50 }
    ),
    AchievementDefinition(
        id = "streak_builder",
        title = "Streak Builder",
        description = "Maintain a 3-day study streak",
        condition = { _, streak, _ -> streak.currentStreak >= 3 }
    ),
    AchievementDefinition(
        id = "streak_master",
        title = "Streak Master",
        description = "Maintain a 7-day study streak",
        condition = { _, streak, _ -> streak.currentStreak >= 7 }
    ),
    AchievementDefinition(
        id = "early_bird",
        title = "Early Bird",
        description = "Complete 5 tasks before 9 AM",
        condition = { _, _, earlyCompletions -> earlyCompletions >= 5 }
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
    private val taskRepository: TaskRepository
) {

    private val _studyStreak = MutableStateFlow(StudyStreak())
    val studyStreak: StateFlow<StudyStreak> = _studyStreak.asStateFlow()

    private val _achievements = MutableStateFlow(initialAchievementBadges())
    val achievements: StateFlow<List<AchievementBadge>> = _achievements.asStateFlow()

    private val _newAchievementsCount = MutableStateFlow(0)
    private val _unreadSocialCount = MutableStateFlow(0)
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

    fun getUnreadSocialCount(): Flow<Int> {
        return _unreadSocialCount.asStateFlow()
    }

    fun getStreakRiskStatus(): Flow<Boolean> {
        return studyStreak.map { streak ->
            !streak.isActiveToday() && streak.currentStreak > 0
        }
    }

    fun getSettingsUpdateCount(): Flow<Int> {
        return _settingsUpdateCount.asStateFlow()
    }

    fun getUnreadSocialNotifications(): Flow<Int> {
        return _unreadSocialCount.asStateFlow()
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
        val earlyMorningCompletions = taskRepository.getEarlyMorningCompletedTasks().size
        val previous = _achievements.value.associateBy { it.id }

        val updatedBadges = achievementDefinitions.map { definition ->
            val wasUnlocked = previous[definition.id]?.isUnlocked == true
            val isUnlocked = definition.condition(stats, streak, earlyMorningCompletions)
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

    // Social features (mock implementation)
    suspend fun markSocialNotificationsAsRead() {
        _unreadSocialCount.value = 0
    }

    suspend fun markAchievementsAsViewed() {
        _newAchievementsCount.value = 0
    }

    suspend fun markSettingsUpdatesAsViewed() {
        _settingsUpdateCount.value = 0
    }

    // Simulation methods for testing
    fun simulateNewSocialNotification() {
        _unreadSocialCount.value = _unreadSocialCount.value + 1
    }

    fun simulateSettingsUpdate() {
        _settingsUpdateCount.value = _settingsUpdateCount.value + 1
    }

    // Notification configuration for workers
    fun getNotificationConfig(): NotificationConfig {
        // This would integrate with actual settings system
        // For now, return default enabled config
        return NotificationConfig(
            areNotificationsEnabled = true,
            allowStudyReminders = true,
            allowAchievementAlerts = true,
            allowStreakWarnings = true,
            allowGoalReminders = true,
            useQuietHours = false,
            quietHoursStart = "22:00",
            quietHoursEnd = "08:00",
            useVibration = true,
            calendarIntegrationEnabled = true
        )
    }

    // Study statistics for personalized notifications
    fun getStudyStats(): StudyStats {
        // This would integrate with actual statistics
        // For now, return mock data
        return StudyStats(
            totalTasksCompleted = 15,
            currentStreak = 3,
            weeklyGoalHours = 10
        )
    }
}

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


