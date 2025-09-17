package com.mtlc.studyplan.feature.progress.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mtlc.studyplan.data.*
import com.mtlc.studyplan.core.error.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * UI State for Progress with comprehensive error handling
 */
data class ProgressUiState(
    val userProgress: UserProgress? = null,
    val weeklyStats: WeeklyStats? = null,
    val monthlyStats: MonthlyStats? = null,
    val achievements: List<Achievement> = emptyList(),
    val streakData: StreakData? = null,
    val studyTimeToday: Int = 0,
    val studyTimeThisWeek: Int = 0,
    val studyTimeThisMonth: Int = 0,
    val completionRate: Float = 0f,
    val xpEarned: Int = 0,
    val rank: Int = 0,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: AppError? = null,
    val loadingOperations: Set<String> = emptySet()
) {
    val isSuccess: Boolean get() = userProgress != null && error == null && !isLoading
    val isError: Boolean get() = error != null
    val isEmpty: Boolean get() = userProgress == null && !isLoading && error == null

    fun isOperationLoading(operation: String): Boolean = loadingOperations.contains(operation)
}

/**
 * Data classes for progress statistics
 */
data class WeeklyStats(
    val studyDays: Int,
    val totalMinutes: Int,
    val averageSessionLength: Int,
    val tasksCompleted: Int,
    val weekNumber: Int
)

data class MonthlyStats(
    val studyDays: Int,
    val totalMinutes: Int,
    val longestStreak: Int,
    val tasksCompleted: Int,
    val month: String
)

data class StreakData(
    val currentStreak: Int,
    val longestStreak: Int,
    val lastStudyDate: LocalDate?,
    val isStreakAtRisk: Boolean
)

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val isUnlocked: Boolean,
    val unlockedDate: LocalDate?,
    val progress: Float = 0f,
    val maxProgress: Float = 1f
)

/**
 * Enhanced Progress ViewModel with comprehensive error handling
 */
class ProgressViewModel(
    private val progressRepository: ProgressRepository,
    private val planRepository: PlanRepository,
    private val achievementTracker: AchievementTracker?,
    private val streakManager: StreakManager?,
    private val context: Context
) : ViewModel() {

    private val errorHandler = ErrorHandler(ErrorLogger(context))

    private val _uiState = MutableStateFlow(ProgressUiState())
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()

    private val _globalError = MutableStateFlow<AppError?>(null)
    val globalError: StateFlow<AppError?> = _globalError.asStateFlow()

    init {
        loadProgressData()
        observeDataChanges()

        // Observe global errors
        viewModelScope.launch {
            errorHandler.globalErrors.collect { error ->
                _globalError.value = error
            }
        }
    }

    private fun loadProgressData() {
        viewModelScope.launch {
            setLoadingState(true)

            executeWithErrorHandling(
                operation = {
                    val userProgress = progressRepository.userProgressFlow.first()
                    val plan = planRepository.planFlow.first()

                    val weeklyStats = calculateWeeklyStats(userProgress, plan)
                    val monthlyStats = calculateMonthlyStats(userProgress, plan)
                    val achievements = loadAchievements(userProgress)
                    val streakData = calculateStreakData(userProgress)
                    val studyTimeStats = calculateStudyTimeStats(userProgress)
                    val completionRate = calculateCompletionRate(userProgress, plan)

                    ProgressData(
                        userProgress = userProgress,
                        weeklyStats = weeklyStats,
                        monthlyStats = monthlyStats,
                        achievements = achievements,
                        streakData = streakData,
                        studyTimeToday = studyTimeStats.today,
                        studyTimeThisWeek = studyTimeStats.thisWeek,
                        studyTimeThisMonth = studyTimeStats.thisMonth,
                        completionRate = completionRate,
                        xpEarned = userProgress.totalXp,
                        rank = calculateUserRank(userProgress)
                    )
                },
                onSuccess = { data ->
                    _uiState.value = _uiState.value.copy(
                        userProgress = data.userProgress,
                        weeklyStats = data.weeklyStats,
                        monthlyStats = data.monthlyStats,
                        achievements = data.achievements,
                        streakData = data.streakData,
                        studyTimeToday = data.studyTimeToday,
                        studyTimeThisWeek = data.studyTimeThisWeek,
                        studyTimeThisMonth = data.studyTimeThisMonth,
                        completionRate = data.completionRate,
                        xpEarned = data.xpEarned,
                        rank = data.rank,
                        isLoading = false,
                        error = null
                    )
                },
                onError = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error
                    )
                }
            )
        }
    }

    private fun observeDataChanges() {
        viewModelScope.launch {
            progressRepository.userProgressFlow.collect { progress ->
                try {
                    updateProgressInState(progress)
                } catch (e: Exception) {
                    handleError(ErrorMapper.mapThrowable(e))
                }
            }
        }

        viewModelScope.launch {
            planRepository.planFlow.collect { plan ->
                try {
                    if (_uiState.value.userProgress != null) {
                        updateStatsForPlanChange(plan)
                    }
                } catch (e: Exception) {
                    handleError(ErrorMapper.mapThrowable(e))
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            setRefreshingState(true)

            executeWithErrorHandling(
                operation = {
                    loadProgressData()
                },
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isRefreshing = false)
                },
                onError = { error ->
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        error = error
                    )
                }
            )
        }
    }

    fun resetProgress() {
        viewModelScope.launch {
            setOperationLoading("reset_progress")

            executeWithErrorHandling(
                operation = {
                    validateResetOperation()

                    // Reset progress data
                    val emptyProgress = UserProgress()
                    // progressRepository.resetProgress()

                    emptyProgress
                },
                onSuccess = { emptyProgress ->
                    _uiState.value = _uiState.value.copy(
                        userProgress = emptyProgress,
                        studyTimeToday = 0,
                        studyTimeThisWeek = 0,
                        studyTimeThisMonth = 0,
                        completionRate = 0f,
                        xpEarned = 0,
                        rank = 0
                    )
                    clearOperationLoading("reset_progress")
                },
                onError = { error ->
                    clearOperationLoading("reset_progress")
                    handleError(error)
                }
            )
        }
    }

    fun exportProgress(format: String) {
        viewModelScope.launch {
            setOperationLoading("export_$format")

            executeWithErrorHandling(
                operation = {
                    validateExportOperation(format)

                    val progress = _uiState.value.userProgress
                        ?: throw AppError.DataError.NotFound

                    when (format.lowercase()) {
                        "csv" -> exportToCSV(progress)
                        "json" -> exportToJSON(progress)
                        "pdf" -> exportToPDF(progress)
                        else -> throw AppError.ValidationError.InvalidFormat("export format")
                    }
                },
                onSuccess = { exportResult ->
                    clearOperationLoading("export_$format")
                    // Show success message or handle export result
                },
                onError = { error ->
                    clearOperationLoading("export_$format")
                    handleError(error)
                }
            )
        }
    }

    fun recalculateStats() {
        viewModelScope.launch {
            setOperationLoading("recalculate")

            executeWithErrorHandling(
                operation = {
                    val progress = progressRepository.userProgressFlow.first()
                    val plan = planRepository.planFlow.first()

                    // Recalculate all statistics
                    val weeklyStats = calculateWeeklyStats(progress, plan)
                    val monthlyStats = calculateMonthlyStats(progress, plan)
                    val completionRate = calculateCompletionRate(progress, plan)

                    Triple(weeklyStats, monthlyStats, completionRate)
                },
                onSuccess = { (weekly, monthly, completion) ->
                    _uiState.value = _uiState.value.copy(
                        weeklyStats = weekly,
                        monthlyStats = monthly,
                        completionRate = completion
                    )
                    clearOperationLoading("recalculate")
                },
                onError = { error ->
                    clearOperationLoading("recalculate")
                    handleError(error)
                }
            )
        }
    }

    fun clearError() {
        _globalError.value = null
        _uiState.value = _uiState.value.copy(error = null)
        errorHandler.clearGlobalError()
    }

    fun retry() {
        clearError()
        loadProgressData()
    }

    // Private helper methods
    private fun setLoadingState(isLoading: Boolean) {
        _uiState.value = _uiState.value.copy(isLoading = isLoading, error = null)
    }

    private fun setRefreshingState(isRefreshing: Boolean) {
        _uiState.value = _uiState.value.copy(isRefreshing = isRefreshing)
    }

    private fun setOperationLoading(operation: String) {
        val currentOperations = _uiState.value.loadingOperations.toMutableSet()
        currentOperations.add(operation)
        _uiState.value = _uiState.value.copy(loadingOperations = currentOperations)
    }

    private fun clearOperationLoading(operation: String) {
        val currentOperations = _uiState.value.loadingOperations.toMutableSet()
        currentOperations.remove(operation)
        _uiState.value = _uiState.value.copy(loadingOperations = currentOperations)
    }

    private suspend fun <T> executeWithErrorHandling(
        operation: suspend () -> T,
        onSuccess: (T) -> Unit = {},
        onError: (AppError) -> Unit = { error -> handleError(error) }
    ) {
        val result = errorHandler.handleOperation(
            operation = operation,
            additionalInfo = mapOf(
                "viewModel" to "ProgressViewModel",
                "timestamp" to System.currentTimeMillis(),
                "userRank" to _uiState.value.rank
            )
        )

        result.fold(
            onSuccess = onSuccess,
            onFailure = { exception ->
                val error = ErrorMapper.mapThrowable(exception)
                onError(error)
            }
        )
    }

    private fun handleError(error: AppError) {
        _uiState.value = _uiState.value.copy(error = error)
    }

    // Validation methods
    private fun validateResetOperation() {
        // Add any validation logic for reset operation
        // For example, check if user has permission to reset
    }

    private fun validateExportOperation(format: String) {
        val supportedFormats = setOf("csv", "json", "pdf")
        if (format.lowercase() !in supportedFormats) {
            throw AppError.ValidationError.InvalidFormat("export format")
        }

        // Check if there's data to export
        if (_uiState.value.userProgress == null) {
            throw AppError.DataError.NotFound
        }
    }

    // Calculation methods
    private fun calculateWeeklyStats(progress: UserProgress, plan: List<WeekPlan>): WeeklyStats {
        // Calculate weekly statistics
        return WeeklyStats(
            studyDays = 5, // Placeholder
            totalMinutes = 150, // Placeholder
            averageSessionLength = 30, // Placeholder
            tasksCompleted = 10, // Placeholder
            weekNumber = 1 // Placeholder
        )
    }

    private fun calculateMonthlyStats(progress: UserProgress, plan: List<WeekPlan>): MonthlyStats {
        // Calculate monthly statistics
        return MonthlyStats(
            studyDays = 20, // Placeholder
            totalMinutes = 600, // Placeholder
            longestStreak = 15, // Placeholder
            tasksCompleted = 40, // Placeholder
            month = "January" // Placeholder
        )
    }

    private fun loadAchievements(progress: UserProgress): List<Achievement> {
        // Load achievements based on progress
        return listOf() // Placeholder
    }

    private fun calculateStreakData(progress: UserProgress): StreakData {
        // Calculate streak information
        return StreakData(
            currentStreak = 5, // Placeholder
            longestStreak = 15, // Placeholder
            lastStudyDate = LocalDate.now(), // Placeholder
            isStreakAtRisk = false // Placeholder
        )
    }

    private fun calculateStudyTimeStats(progress: UserProgress): StudyTimeStats {
        // Calculate study time for different periods
        return StudyTimeStats(
            today = 30, // Placeholder
            thisWeek = 150, // Placeholder
            thisMonth = 600 // Placeholder
        )
    }

    private fun calculateCompletionRate(progress: UserProgress, plan: List<WeekPlan>): Float {
        // Calculate overall completion rate
        return 0.75f // Placeholder
    }

    private fun calculateUserRank(progress: UserProgress): Int {
        // Calculate user rank based on progress
        return 1 // Placeholder
    }

    private fun updateProgressInState(progress: UserProgress) {
        _uiState.value = _uiState.value.copy(
            userProgress = progress,
            xpEarned = progress.totalXp
        )
    }

    private fun updateStatsForPlanChange(plan: List<WeekPlan>) {
        // Update statistics when plan changes
        viewModelScope.launch {
            val progress = _uiState.value.userProgress ?: return@launch
            val completionRate = calculateCompletionRate(progress, plan)
            _uiState.value = _uiState.value.copy(completionRate = completionRate)
        }
    }

    // Export methods
    private fun exportToCSV(progress: UserProgress): String {
        // Export progress to CSV format
        return "CSV export result"
    }

    private fun exportToJSON(progress: UserProgress): String {
        // Export progress to JSON format
        return "JSON export result"
    }

    private fun exportToPDF(progress: UserProgress): String {
        // Export progress to PDF format
        return "PDF export result"
    }

    override fun onCleared() {
        super.onCleared()
        errorHandler.clearGlobalError()
    }

    /**
     * Data classes for internal use
     */
    private data class ProgressData(
        val userProgress: UserProgress,
        val weeklyStats: WeeklyStats,
        val monthlyStats: MonthlyStats,
        val achievements: List<Achievement>,
        val streakData: StreakData,
        val studyTimeToday: Int,
        val studyTimeThisWeek: Int,
        val studyTimeThisMonth: Int,
        val completionRate: Float,
        val xpEarned: Int,
        val rank: Int
    )

    private data class StudyTimeStats(
        val today: Int,
        val thisWeek: Int,
        val thisMonth: Int
    )
}

/**
 * Factory for ProgressViewModel
 */
class ProgressViewModelFactory(
    private val progressRepository: ProgressRepository,
    private val planRepository: PlanRepository,
    private val achievementTracker: AchievementTracker?,
    private val streakManager: StreakManager?,
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProgressViewModel::class.java)) {
            return ProgressViewModel(
                progressRepository,
                planRepository,
                achievementTracker,
                streakManager,
                context
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}