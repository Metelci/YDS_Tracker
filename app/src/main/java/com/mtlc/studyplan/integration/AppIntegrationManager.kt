package com.mtlc.studyplan.integration

import com.mtlc.studyplan.data.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppIntegrationManager @Inject constructor(
    private val taskRepository: TaskRepository
) {

    private val _studyStreak = MutableStateFlow(StudyStreak())
    val studyStreak: StateFlow<StudyStreak> = _studyStreak.asStateFlow()

    private val _achievements = MutableStateFlow<List<Achievement>>(Achievement.ALL_ACHIEVEMENTS)
    val achievements: StateFlow<List<Achievement>> = _achievements.asStateFlow()

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
        val currentAchievements = _achievements.value.toMutableList()
        var newAchievements = 0

        // Check for task completion achievements
        currentAchievements.forEachIndexed { index, achievement ->
            if (!achievement.isUnlocked) {
                val shouldUnlock = when (achievement.id) {
                    "task_crusher" -> stats.completedTasks >= 50
                    "streak_master" -> _studyStreak.value.currentStreak >= 7
                    "early_bird" -> checkEarlyBirdAchievement()
                    else -> false
                }

                if (shouldUnlock) {
                    currentAchievements[index] = achievement.copy(
                        isUnlocked = true,
                        unlockedAt = System.currentTimeMillis()
                    )
                    newAchievements++
                }
            }
        }

        if (newAchievements > 0) {
            _achievements.value = currentAchievements
            _newAchievementsCount.value = _newAchievementsCount.value + newAchievements
        }
    }

    private suspend fun checkEarlyBirdAchievement(): Boolean {
        // Check if user has completed 5 tasks before 9 AM
        val earlyTasks = taskRepository.getEarlyMorningCompletedTasks()
        return earlyTasks.size >= 5
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
}