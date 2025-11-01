package com.mtlc.studyplan.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtlc.studyplan.data.StudyProgressRepository
import com.mtlc.studyplan.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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
                // Fetch real data from repositories
                val completedTasks = taskRepository.completedTasks.first()
                val currentWeek = studyProgressRepository.currentWeek.first()

                // Convert TaskEntity to TaskLog for analytics
                val taskLogs = completedTasks.mapNotNull { task ->
                    // Only include tasks that have been completed (with completedAt timestamp)
                    task.completedAt?.let { completedTimestamp ->
                        com.mtlc.studyplan.data.TaskLog(
                            taskId = task.id.toString(),
                            category = task.category.name,
                            correct = task.isCompleted,
                            minutesSpent = task.estimatedMinutes,
                            timestampMillis = completedTimestamp
                        )
                    }
                }

                // Create basic UserProgress from repository data
                val userProgress = com.mtlc.studyplan.data.UserProgress(
                    completedTasks = completedTasks.map { it.id.toString() }.toSet(),
                    streakCount = 0, // Will be calculated by analytics engine
                    totalPoints = completedTasks.size * 10,
                    dayProgress = emptyList()
                )

                // Generate analytics with real data
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
}
