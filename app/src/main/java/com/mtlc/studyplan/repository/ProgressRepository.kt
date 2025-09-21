package com.mtlc.studyplan.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.mtlc.studyplan.data.TaskLog
import com.mtlc.studyplan.data.UserProgress

/**
 * Lightweight compatibility layer to satisfy existing repository-based code paths.
 * Real progress logic lives in com.mtlc.studyplan.data.ProgressRepository.
 */
class ProgressRepository {
    data class DailyStats(
        val date: String = "",
        val tasksCompleted: Int = 0,
        val studyMinutes: Int = 0,
        val pointsEarned: Int = 0,
        val streak: Int = 0,
        val goalProgress: GoalProgress = GoalProgress()
    )
    data class GoalProgress(val overallProgress: Float = 0f)

    private val _todayStats = MutableStateFlow(DailyStats())
    val todayStats: Flow<DailyStats> = _todayStats.asStateFlow()

    private val _refreshTrigger = MutableStateFlow(0L)
    val refreshTrigger: Flow<Long> = _refreshTrigger.asStateFlow()

    // Minimal flows to satisfy combine operations elsewhere
    private val _userProgress = MutableStateFlow(UserProgress())
    val userProgressFlow: Flow<UserProgress> = _userProgress.asStateFlow()

    private val _taskLogs = MutableStateFlow<List<TaskLog>>(emptyList())
    val taskLogsFlow: Flow<List<TaskLog>> = _taskLogs.asStateFlow()

    fun updateDailyStats(date: String, tasksCompleted: Int, studyMinutes: Int, pointsEarned: Int) {
        _todayStats.value = DailyStats(date, tasksCompleted, studyMinutes, pointsEarned)
        _refreshTrigger.value = System.currentTimeMillis()
    }

    fun getTodayProgressOrCreate(): DailyStats = _todayStats.value

    fun getTotalStudyMinutes(): Int = _todayStats.value.studyMinutes

    // Convenience to append logs from various systems
    fun addTaskLog(log: TaskLog) {
        _taskLogs.value = _taskLogs.value + log
    }

    // Minimal compatibility for gamification API
    suspend fun completeTaskWithPoints(
        taskId: String,
        taskDescription: String,
        taskDetails: String?,
        minutesSpent: Int
    ): com.mtlc.studyplan.data.PointsTransaction {
        val category = com.mtlc.studyplan.data.TaskCategory.fromString(taskDescription + " " + (taskDetails ?: ""))
        val base = category.basePoints
        val multiplier = com.mtlc.studyplan.data.StreakMultiplier.getMultiplierForStreak(_userProgress.value.streakCount)
        val total = (base * multiplier.multiplier).toInt()
        _todayStats.value = _todayStats.value.copy(
            tasksCompleted = _todayStats.value.tasksCompleted + 1,
            studyMinutes = _todayStats.value.studyMinutes + minutesSpent,
            pointsEarned = _todayStats.value.pointsEarned + total,
            streak = _userProgress.value.streakCount + 1
        )
        _refreshTrigger.value = System.currentTimeMillis()
        addTaskLog(
            TaskLog(
                taskId = taskId,
                timestampMillis = System.currentTimeMillis(),
                minutesSpent = minutesSpent,
                correct = true,
                category = category.displayName,
            )
        )
        return com.mtlc.studyplan.data.PointsTransaction(
            basePoints = base,
            multiplier = multiplier.multiplier,
            totalPoints = total,
            taskCategory = category,
            streakMultiplier = multiplier
        )
    }
}

// Wildcard import in callers brings this into scope as an unqualified symbol
val progressRepository = ProgressRepository()
