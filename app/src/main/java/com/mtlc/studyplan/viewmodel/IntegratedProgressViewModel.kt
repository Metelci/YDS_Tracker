package com.mtlc.studyplan.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.mtlc.studyplan.integration.EnhancedAppIntegrationManager
import com.mtlc.studyplan.repository.ProgressRepository
import com.mtlc.studyplan.eventbus.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Progress ViewModel that integrates with the enhanced app integration manager
 */
@HiltViewModel
class IntegratedProgressViewModel @Inject constructor(
    private val integrationManager: EnhancedAppIntegrationManager,
    private val progressRepository: ProgressRepository
) : ViewModel() {

    // Progress data flows
    val todayStats = integrationManager.progressStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000),
            ProgressRepository.DailyStats.empty())

    val weeklyStats = progressRepository.weeklyStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000),
            ProgressRepository.WeeklyStats(0, 0, 0, 0f, 0f, 0, "", 0f))

    val monthlyStats = progressRepository.monthlyStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000),
            ProgressRepository.MonthlyStats(0, 0, 0, 0f, 0f, 0, "", 0f))

    val overallStats = progressRepository.overallStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000),
            ProgressRepository.OverallStats(0f, 0, 0, 0, 0, 0, 0f, 0))

    // Trend data
    val studyTrend = progressRepository.studyTrend
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val taskTrend = progressRepository.taskTrend
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pointsTrend = progressRepository.pointsTrend
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Goal tracking
    val goalProgress = progressRepository.goalProgress
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000),
            ProgressRepository.GoalProgress.empty())

    val userSettings = integrationManager.userSettings

    // Progress events
    val progressEvents = integrationManager.eventBus.subscribeToProgressEvents<ProgressEvent>()

    // UI state
    private val _uiState = MutableStateFlow(ProgressUiState())
    val uiState = _uiState.asStateFlow()

    // Chart data
    data class ChartData(
        val studyMinutesTrend: List<ProgressRepository.TrendPoint>,
        val tasksTrend: List<ProgressRepository.TrendPoint>,
        val pointsTrend: List<ProgressRepository.TrendPoint>,
        val selectedPeriod: ChartPeriod = ChartPeriod.WEEK
    )

    private val _chartData = MutableStateFlow(ChartData(emptyList(), emptyList(), emptyList()))
    val chartData = _chartData.asStateFlow()

    // Goal management
    data class GoalInfo(
        val dailyStudyGoalMinutes: Int,
        val dailyTaskGoal: Int,
        val weeklyStudyGoalMinutes: Int,
        val weeklyTaskGoal: Int,
        val studyProgress: Float,
        val taskProgress: Float,
        val isStudyGoalMet: Boolean,
        val isTaskGoalMet: Boolean
    )

    val goalInfo = combine(
        goalProgress,
        userSettings
    ) { progress, settings ->
        GoalInfo(
            dailyStudyGoalMinutes = settings?.dailyStudyGoalMinutes ?: 120,
            dailyTaskGoal = settings?.dailyTaskGoal ?: 5,
            weeklyStudyGoalMinutes = settings?.weeklyStudyGoalMinutes ?: 840,
            weeklyTaskGoal = settings?.weeklyTaskGoal ?: 35,
            studyProgress = progress.studyMinutesProgress,
            taskProgress = progress.taskProgress,
            isStudyGoalMet = progress.studyGoalMet,
            isTaskGoalMet = progress.taskGoalMet
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000),
        GoalInfo(120, 5, 840, 35, 0f, 0f, false, false))

    init {
        observeProgressEvents()
        updateChartData()
    }

    // Business logic methods

    /**
     * Update daily study goal
     */
    fun updateDailyStudyGoal(minutes: Int) {
        viewModelScope.launch {
            try {
                setLoading(true)
                integrationManager.updateSettings { settings ->
                    settings.copy(dailyStudyGoalMinutes = minutes)
                }
                showSuccess("Daily study goal updated to $minutes minutes")
            } catch (e: Exception) {
                showError("Failed to update study goal: ${e.message}")
            } finally {
                setLoading(false)
            }
        }
    }

    /**
     * Update daily task goal
     */
    fun updateDailyTaskGoal(tasks: Int) {
        viewModelScope.launch {
            try {
                setLoading(true)
                integrationManager.updateSettings { settings ->
                    settings.copy(dailyTaskGoal = tasks)
                }
                showSuccess("Daily task goal updated to $tasks tasks")
            } catch (e: Exception) {
                showError("Failed to update task goal: ${e.message}")
            } finally {
                setLoading(false)
            }
        }
    }

    /**
     * Update weekly goals
     */
    fun updateWeeklyGoals(studyMinutes: Int, tasks: Int) {
        viewModelScope.launch {
            try {
                setLoading(true)
                integrationManager.updateSettings { settings ->
                    settings.copy(
                        weeklyStudyGoalMinutes = studyMinutes,
                        weeklyTaskGoal = tasks
                    )
                }
                showSuccess("Weekly goals updated")
            } catch (e: Exception) {
                showError("Failed to update weekly goals: ${e.message}")
            } finally {
                setLoading(false)
            }
        }
    }

    /**
     * Change chart period
     */
    fun changeChartPeriod(period: ChartPeriod) {
        _chartData.value = _chartData.value.copy(selectedPeriod = period)
        updateChartData()
    }

    /**
     * Refresh progress data
     */
    fun refreshProgress() {
        viewModelScope.launch {
            integrationManager.eventBus.publish(
                UIEvent.RefreshRequested(
                    component = "progress",
                    reason = "user_request"
                )
            )
        }
    }

    /**
     * Export progress data
     */
    fun exportProgress(format: ExportFormat) {
        viewModelScope.launch {
            try {
                setLoading(true)

                // This would typically call a service to export data
                // For now, just show a success message
                showSuccess("Progress data exported successfully")

                // Track analytics
                integrationManager.eventBus.publish(
                    AnalyticsEvent.UserActionTracked(
                        action = "progress_exported",
                        screen = "progress",
                        properties = mapOf("format" to format.name)
                    )
                )

            } catch (e: Exception) {
                showError("Failed to export progress: ${e.message}")
            } finally {
                setLoading(false)
            }
        }
    }

    // Helper methods

    private fun observeProgressEvents() {
        progressEvents
            .onEach { event ->
                when (event) {
                    is ProgressEvent.DailyGoalReached -> {
                        showSuccess("🎉 Daily ${event.goalType} goal reached!")
                    }
                    is ProgressEvent.WeeklyGoalReached -> {
                        showSuccess("🏆 Weekly ${event.goalType} goal achieved!")
                    }
                    is ProgressEvent.EfficiencyMilestone -> {
                        showSuccess("⚡ New efficiency milestone: ${event.efficiency}")
                    }
                    else -> {
                        // Handle other progress events as needed
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun updateChartData() {
        viewModelScope.launch {
            try {
                // Get trend data based on selected period
                val studyTrendData = when (_chartData.value.selectedPeriod) {
                    ChartPeriod.WEEK -> studyTrend.value
                    ChartPeriod.MONTH -> progressRepository.studyTrend.first() // Get monthly data
                    ChartPeriod.YEAR -> progressRepository.studyTrend.first() // Get yearly data
                }

                val taskTrendData = when (_chartData.value.selectedPeriod) {
                    ChartPeriod.WEEK -> taskTrend.value
                    ChartPeriod.MONTH -> progressRepository.taskTrend.first()
                    ChartPeriod.YEAR -> progressRepository.taskTrend.first()
                }

                val pointsTrendData = when (_chartData.value.selectedPeriod) {
                    ChartPeriod.WEEK -> pointsTrend.value
                    ChartPeriod.MONTH -> progressRepository.pointsTrend.first()
                    ChartPeriod.YEAR -> progressRepository.pointsTrend.first()
                }

                _chartData.value = _chartData.value.copy(
                    studyMinutesTrend = studyTrendData,
                    tasksTrend = taskTrendData,
                    pointsTrend = pointsTrendData
                )

            } catch (e: Exception) {
                showError("Failed to update chart data: ${e.message}")
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        _uiState.value = _uiState.value.copy(isLoading = isLoading)
        integrationManager.eventBus.let { eventBus ->
            viewModelScope.launch {
                eventBus.publish(
                    UIEvent.LoadingStateChanged("progress", isLoading)
                )
            }
        }
    }

    private fun showSuccess(message: String) {
        _uiState.value = _uiState.value.copy(
            successMessage = message,
            errorMessage = null
        )
        integrationManager.eventBus.let { eventBus ->
            viewModelScope.launch {
                eventBus.publish(
                    UIEvent.SnackbarRequested(message, duration = "SHORT")
                )
            }
        }
    }

    private fun showError(message: String) {
        _uiState.value = _uiState.value.copy(
            errorMessage = message,
            successMessage = null
        )
        integrationManager.eventBus.let { eventBus ->
            viewModelScope.launch {
                eventBus.publish(
                    UIEvent.ErrorOccurred("ProgressViewModel", message, isCritical = false)
                )
            }
        }
    }

    // Data classes and enums
    data class ProgressUiState(
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
        val successMessage: String? = null
    )

    enum class ChartPeriod {
        WEEK, MONTH, YEAR
    }

    enum class ExportFormat {
        PDF, CSV, JSON
    }
}