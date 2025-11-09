package com.mtlc.studyplan.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtlc.studyplan.data.DayProgress
import com.mtlc.studyplan.data.StudyProgressRepository
import com.mtlc.studyplan.data.StreakMultiplier
import com.mtlc.studyplan.data.TaskLog
import com.mtlc.studyplan.data.UserProgress
import com.mtlc.studyplan.database.entities.TaskEntity
import com.mtlc.studyplan.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class AnalyticsViewModel(
    private val analyticsEngine: AnalyticsEngine,
    private val taskRepository: TaskRepository,
    private val studyProgressRepository: StudyProgressRepository
) : ViewModel() {

    private val _analyticsData = MutableStateFlow<AnalyticsData?>(null)
    val analyticsData: StateFlow<AnalyticsData?> = _analyticsData.asStateFlow()

    private val _weeklyData = MutableStateFlow<List<WeeklyAnalyticsData>>(emptyList())
    val weeklyData: StateFlow<List<WeeklyAnalyticsData>> = _weeklyData.asStateFlow()

    private val _performanceData = MutableStateFlow<PerformanceData?>(null)
    val performanceData: StateFlow<PerformanceData?> = _performanceData.asStateFlow()

    private val _selectedTab = MutableStateFlow(AnalyticsTab.OVERVIEW)
    val selectedTab: StateFlow<AnalyticsTab> = _selectedTab.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadAnalytics(AnalyticsTimeframe.LAST_30_DAYS)
    }

    fun loadAnalytics(timeframe: AnalyticsTimeframe) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val completedTasks = taskRepository.completedTasks.first()
                val currentWeek = studyProgressRepository.currentWeek.first()
                val userProgress = buildUserProgress(completedTasks, currentWeek)
                val taskLogs = completedTasks.mapNotNull { it.toTaskLog() }

                val data = analyticsEngine.generateAnalytics(
                    days = timeframe.days,
                    taskLogs = taskLogs,
                    userProgress = userProgress
                )
                _analyticsData.value = data

                // Get weekly data
                val weekly = analyticsEngine.getWeeklyData(
                    days = timeframe.days,
                    taskLogs = taskLogs
                )
                _weeklyData.value = weekly

                // Get performance data
                val performance = analyticsEngine.getPerformanceData(
                    days = timeframe.days,
                    taskLogs = taskLogs
                )
                _performanceData.value = performance
            } catch (e: Exception) {
                // Handle error - emit default data
                _analyticsData.value = AnalyticsData()
                _weeklyData.value = emptyList()
                _performanceData.value = PerformanceData(
                    averageAccuracy = 0f,
                    averageSpeed = 0f,
                    consistencyScore = 0f,
                    weakAreas = emptyList(),
                    totalMinutes = 0,
                    taskCount = 0
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectTab(tab: AnalyticsTab) {
        _selectedTab.value = tab
    }

    fun refreshAnalytics() {
        val currentTimeframe = when (_weeklyData.value.size) {
            in 0..1 -> AnalyticsTimeframe.LAST_7_DAYS
            in 2..4 -> AnalyticsTimeframe.LAST_30_DAYS
            in 5..12 -> AnalyticsTimeframe.LAST_90_DAYS
            else -> AnalyticsTimeframe.ALL_TIME
        }
        loadAnalytics(currentTimeframe)
    }

    private fun buildUserProgress(tasks: List<TaskEntity>, currentWeek: Int): UserProgress? {
        if (tasks.isEmpty()) return null

        val streakCount = calculateCurrentStreak(tasks)
        val totalPoints = tasks.sumOf { it.pointsValue }
        val lastCompletion = tasks.mapNotNull { it.completedAt }.maxOrNull() ?: 0L
        val dayProgress = buildDayProgress(tasks, currentWeek)
        val streakMultiplier = StreakMultiplier.getMultiplierForStreak(streakCount)

        return UserProgress(
            completedTasks = tasks.map { it.id }.toSet(),
            streakCount = streakCount,
            lastCompletionDate = lastCompletion,
            unlockedAchievements = emptySet(),
            totalPoints = totalPoints,
            currentStreakMultiplier = streakMultiplier.multiplier,
            dayProgress = dayProgress,
            skippedDays = emptySet(),
            totalXp = totalPoints
        )
    }

    private fun TaskEntity.toTaskLog(): TaskLog? {
        val completionTime = completedAt ?: return null
        val minutes = if (actualMinutes > 0) actualMinutes else estimatedMinutes
        return TaskLog(
            taskId = id,
            category = category.name,
            correct = isCompleted,
            minutesSpent = minutes,
            timestampMillis = completionTime,
            pointsEarned = pointsValue
        )
    }

    private fun calculateCurrentStreak(tasks: List<TaskEntity>): Int {
        val zone = ZoneId.systemDefault()
        val completionDays = tasks
            .filter { it.streakContribution && it.completedAt != null }
            .map { Instant.ofEpochMilli(it.completedAt!!).atZone(zone).toLocalDate() }
            .distinct()
            .sorted()

        if (completionDays.isEmpty()) return 0

        var streak = 1
        for (index in completionDays.lastIndex downTo 1) {
            val current = completionDays[index]
            val previous = completionDays[index - 1]
            val gap = ChronoUnit.DAYS.between(previous, current)
            if (gap == 1L) {
                streak++
            } else if (gap > 1L) {
                break
            }
        }

        return streak
    }

    private fun buildDayProgress(tasks: List<TaskEntity>, currentWeek: Int): List<DayProgress> {
        val zone = ZoneId.systemDefault()
        val now = LocalDate.now(zone)

        return tasks
            .filter { it.completedAt != null }
            .groupBy { Instant.ofEpochMilli(it.completedAt!!).atZone(zone).toLocalDate() }
            .map { (date, dayTasks) ->
                val weeksAgo = ChronoUnit.WEEKS.between(date, now).toInt().coerceAtLeast(0)
                val normalizedWeek = (currentWeek - weeksAgo).coerceAtLeast(1)
                DayProgress(
                    weekIndex = normalizedWeek,
                    dayIndex = date.dayOfWeek.ordinal,
                    completedTasks = dayTasks.map { it.id }.toSet()
                )
            }
            .sortedBy { it.weekIndex * 10 + it.dayIndex }
    }
}
