package com.mtlc.studyplan.feature.home.viewmodel

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
 * UI State for Home with comprehensive error handling
 */
data class HomeUiState(
    val plan: List<WeekPlan> = emptyList(),
    val userProgress: UserProgress? = null,
    val todaysTasks: List<Task> = emptyList(),
    val todaysProgress: DayProgress? = null,
    val settings: PlanDurationSettings? = null,
    val currentWeekIndex: Int = 0,
    val currentDayIndex: Int = 0,
    val studyStreakDays: Int = 0,
    val completionPercentage: Float = 0f,
    val totalXp: Int = 0,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: AppError? = null,
    val operationStates: Map<String, Boolean> = emptyMap()
) {
    val isSuccess: Boolean get() = plan.isNotEmpty() && userProgress != null && error == null && !isLoading
    val isError: Boolean get() = error != null
    val isEmpty: Boolean get() = plan.isEmpty() && !isLoading && error == null

    fun isOperationLoading(operation: String): Boolean = operationStates[operation] == true

    val hasTasksToday: Boolean get() = todaysTasks.isNotEmpty()
    val completedTasksToday: Int get() = todaysProgress?.completedTasks?.size ?: 0
    val totalTasksToday: Int get() = todaysTasks.size
    val todayCompletionRate: Float get() = if (totalTasksToday > 0) completedTasksToday.toFloat() / totalTasksToday else 0f
}

/**
 * Enhanced Home ViewModel with comprehensive error handling
 */
class HomeViewModel(
    private val planRepository: PlanRepository,
    private val progressRepository: ProgressRepository,
    private val settingsStore: PlanSettingsStore,
    private val overridesStore: PlanOverridesStore,
    private val context: Context
) : ViewModel() {

    private val errorHandler = ErrorHandler(ErrorLogger(context))

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _globalError = MutableStateFlow<AppError?>(null)
    val globalError: StateFlow<AppError?> = _globalError.asStateFlow()

    private val today = LocalDate.now()

    init {
        loadHomeData()
        observeDataChanges()

        // Observe global errors
        viewModelScope.launch {
            errorHandler.globalErrors.collect { error ->
                _globalError.value = error
            }
        }
    }

    private fun loadHomeData() {
        viewModelScope.launch {
            setLoadingState(true)

            executeWithErrorHandling(
                operation = {
                    val plan = planRepository.planFlow.first()
                    val progress = progressRepository.userProgressFlow.first()
                    val settings = settingsStore.settingsFlow.first()

                    val currentIndices = calculateCurrentIndices(settings, today)
                    val todaysTasks = getTodaysTasks(plan, currentIndices.first, currentIndices.second)
                    val todaysProgress = getTodaysProgress(progress, currentIndices.first, currentIndices.second)
                    val streakDays = calculateStreakDays(progress)
                    val completionPercentage = calculateOverallCompletion(plan, progress)

                    HomeData(
                        plan = plan,
                        progress = progress,
                        settings = settings,
                        todaysTasks = todaysTasks,
                        todaysProgress = todaysProgress,
                        currentWeekIndex = currentIndices.first,
                        currentDayIndex = currentIndices.second,
                        streakDays = streakDays,
                        completionPercentage = completionPercentage
                    )
                },
                onSuccess = { data ->
                    _uiState.value = _uiState.value.copy(
                        plan = data.plan,
                        userProgress = data.progress,
                        todaysTasks = data.todaysTasks,
                        todaysProgress = data.todaysProgress,
                        settings = data.settings,
                        currentWeekIndex = data.currentWeekIndex,
                        currentDayIndex = data.currentDayIndex,
                        studyStreakDays = data.streakDays,
                        completionPercentage = data.completionPercentage,
                        totalXp = data.progress.totalXp,
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
            planRepository.planFlow.collect { plan ->
                try {
                    updatePlanInState(plan)
                } catch (e: Exception) {
                    handleError(ErrorMapper.mapThrowable(e))
                }
            }
        }

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
            settingsStore.settingsFlow.collect { settings ->
                try {
                    updateSettingsInState(settings)
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
                    loadHomeData()
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

    fun completeTask(taskId: String) {
        viewModelScope.launch {
            setOperationLoading("complete_$taskId", true)

            executeWithErrorHandling(
                operation = {
                    validateTaskOperation(taskId)

                    val task = findTaskById(taskId)
                        ?: throw AppError.DataError.NotFound

                    val currentProgress = _uiState.value.userProgress
                        ?: throw AppError.DataError.NotFound

                    val updatedProgress = markTaskCompleted(currentProgress, task)

                    // Update progress in repository
                    // progressRepository.updateProgress(updatedProgress)

                    updatedProgress
                },
                onSuccess = { updatedProgress ->
                    updateProgressInState(updatedProgress)
                    setOperationLoading("complete_$taskId", false)
                },
                onError = { error ->
                    setOperationLoading("complete_$taskId", false)
                    handleError(error)
                }
            )
        }
    }

    fun undoTaskCompletion(taskId: String) {
        viewModelScope.launch {
            setOperationLoading("undo_$taskId", true)

            executeWithErrorHandling(
                operation = {
                    validateTaskOperation(taskId)

                    val task = findTaskById(taskId)
                        ?: throw AppError.DataError.NotFound

                    val currentProgress = _uiState.value.userProgress
                        ?: throw AppError.DataError.NotFound

                    val updatedProgress = markTaskIncomplete(currentProgress, task)

                    // Update progress in repository
                    // progressRepository.updateProgress(updatedProgress)

                    updatedProgress
                },
                onSuccess = { updatedProgress ->
                    updateProgressInState(updatedProgress)
                    setOperationLoading("undo_$taskId", false)
                },
                onError = { error ->
                    setOperationLoading("undo_$taskId", false)
                    handleError(error)
                }
            )
        }
    }

    fun skipToday() {
        viewModelScope.launch {
            setOperationLoading("skip_today", true)

            executeWithErrorHandling(
                operation = {
                    val currentProgress = _uiState.value.userProgress
                        ?: throw AppError.DataError.NotFound

                    val updatedProgress = markDaySkipped(currentProgress)

                    // Update progress in repository
                    // progressRepository.updateProgress(updatedProgress)

                    updatedProgress
                },
                onSuccess = { updatedProgress ->
                    updateProgressInState(updatedProgress)
                    setOperationLoading("skip_today", false)
                },
                onError = { error ->
                    setOperationLoading("skip_today", false)
                    handleError(error)
                }
            )
        }
    }

    fun addCustomTask(description: String, duration: Int) {
        viewModelScope.launch {
            setOperationLoading("add_custom_task", true)

            executeWithErrorHandling(
                operation = {
                    validateCustomTask(description, duration)

                    val currentWeek = _uiState.value.currentWeekIndex
                    val currentDay = _uiState.value.currentDayIndex

                    planRepository.addCustomTask(
                        week = currentWeek,
                        dayIndex = currentDay,
                        desc = description,
                        details = null
                    )

                    description
                },
                onSuccess = {
                    setOperationLoading("add_custom_task", false)
                    // Plan will be updated through flow observer
                },
                onError = { error ->
                    setOperationLoading("add_custom_task", false)
                    handleError(error)
                }
            )
        }
    }

    fun removeCustomTask(taskId: String) {
        viewModelScope.launch {
            setOperationLoading("remove_$taskId", true)

            executeWithErrorHandling(
                operation = {
                    validateTaskOperation(taskId)

                    val currentWeek = _uiState.value.currentWeekIndex
                    val currentDay = _uiState.value.currentDayIndex

                    // Extract suffix from task ID
                    val suffix = extractTaskSuffix(taskId)

                    planRepository.removeCustomTask(
                        week = currentWeek,
                        dayIndex = currentDay,
                        idSuffix = suffix
                    )

                    taskId
                },
                onSuccess = {
                    setOperationLoading("remove_$taskId", false)
                    // Plan will be updated through flow observer
                },
                onError = { error ->
                    setOperationLoading("remove_$taskId", false)
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
        loadHomeData()
    }

    // Private helper methods
    private fun setLoadingState(isLoading: Boolean) {
        _uiState.value = _uiState.value.copy(isLoading = isLoading, error = null)
    }

    private fun setRefreshingState(isRefreshing: Boolean) {
        _uiState.value = _uiState.value.copy(isRefreshing = isRefreshing)
    }

    private fun setOperationLoading(operation: String, isLoading: Boolean) {
        val currentStates = _uiState.value.operationStates.toMutableMap()
        if (isLoading) {
            currentStates[operation] = true
        } else {
            currentStates.remove(operation)
        }
        _uiState.value = _uiState.value.copy(operationStates = currentStates)
    }

    private suspend fun <T> executeWithErrorHandling(
        operation: suspend () -> T,
        onSuccess: (T) -> Unit = {},
        onError: (AppError) -> Unit = { error -> handleError(error) }
    ) {
        val result = errorHandler.handleOperation(
            operation = operation,
            additionalInfo = mapOf(
                "viewModel" to "HomeViewModel",
                "timestamp" to System.currentTimeMillis(),
                "currentWeek" to _uiState.value.currentWeekIndex,
                "currentDay" to _uiState.value.currentDayIndex
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
    private fun validateTaskOperation(taskId: String) {
        if (taskId.isBlank()) {
            throw AppError.ValidationError.RequiredFieldEmpty
        }

        val taskExists = _uiState.value.todaysTasks.any { it.id == taskId }
        if (!taskExists) {
            throw AppError.DataError.NotFound
        }
    }

    private fun validateCustomTask(description: String, duration: Int) {
        if (description.isBlank()) {
            throw AppError.ValidationError.RequiredFieldEmpty
        }

        if (duration <= 0 || duration > 480) { // Max 8 hours
            throw AppError.ValidationError.OutOfRange(
                field = "duration",
                min = 1,
                max = 480
            )
        }
    }

    // Data manipulation methods
    private fun updatePlanInState(plan: List<WeekPlan>) {
        val settings = _uiState.value.settings ?: return
        val currentIndices = calculateCurrentIndices(settings, today)
        val todaysTasks = getTodaysTasks(plan, currentIndices.first, currentIndices.second)

        _uiState.value = _uiState.value.copy(
            plan = plan,
            todaysTasks = todaysTasks,
            currentWeekIndex = currentIndices.first,
            currentDayIndex = currentIndices.second
        )
    }

    private fun updateProgressInState(progress: UserProgress) {
        val settings = _uiState.value.settings ?: return
        val currentIndices = calculateCurrentIndices(settings, today)
        val todaysProgress = getTodaysProgress(progress, currentIndices.first, currentIndices.second)
        val streakDays = calculateStreakDays(progress)
        val completionPercentage = calculateOverallCompletion(_uiState.value.plan, progress)

        _uiState.value = _uiState.value.copy(
            userProgress = progress,
            todaysProgress = todaysProgress,
            studyStreakDays = streakDays,
            completionPercentage = completionPercentage,
            totalXp = progress.totalXp
        )
    }

    private fun updateSettingsInState(settings: PlanDurationSettings) {
        val currentIndices = calculateCurrentIndices(settings, today)
        val todaysTasks = getTodaysTasks(_uiState.value.plan, currentIndices.first, currentIndices.second)

        _uiState.value = _uiState.value.copy(
            settings = settings,
            todaysTasks = todaysTasks,
            currentWeekIndex = currentIndices.first,
            currentDayIndex = currentIndices.second
        )
    }

    // Calculation methods
    private fun calculateCurrentIndices(settings: PlanDurationSettings, date: LocalDate): Pair<Int, Int> {
        val startDate = LocalDate.ofEpochDay(settings.startEpochDay)
        val daysSinceStart = ChronoUnit.DAYS.between(startDate, date).toInt()

        val weekIndex = (daysSinceStart / 7).coerceAtLeast(0)
        val dayIndex = (daysSinceStart % 7).coerceAtLeast(0)

        return Pair(weekIndex, dayIndex)
    }

    private fun getTodaysTasks(plan: List<WeekPlan>, weekIndex: Int, dayIndex: Int): List<Task> {
        return plan.getOrNull(weekIndex)?.days?.getOrNull(dayIndex)?.tasks ?: emptyList()
    }

    private fun getTodaysProgress(progress: UserProgress, weekIndex: Int, dayIndex: Int): DayProgress? {
        return progress.dayProgress.find { it.weekIndex == weekIndex && it.dayIndex == dayIndex }
    }

    private fun calculateStreakDays(progress: UserProgress): Int {
        // Calculate consecutive study days
        return progress.streakCount
    }

    private fun calculateOverallCompletion(plan: List<WeekPlan>, progress: UserProgress): Float {
        val totalTasks = plan.sumOf { week -> week.days.sumOf { day -> day.tasks.size } }
        val completedTasks = progress.dayProgress.sumOf { it.completedTasks.size }

        return if (totalTasks > 0) completedTasks.toFloat() / totalTasks else 0f
    }

    private fun findTaskById(taskId: String): Task? {
        return _uiState.value.todaysTasks.find { it.id == taskId }
    }

    private fun markTaskCompleted(progress: UserProgress, task: Task): UserProgress {
        // Create updated progress with task marked as completed
        val weekIndex = _uiState.value.currentWeekIndex
        val dayIndex = _uiState.value.currentDayIndex

        val dayProgressList = progress.dayProgress.toMutableList()
        val existingDayProgress = dayProgressList.find { it.weekIndex == weekIndex && it.dayIndex == dayIndex }

        if (existingDayProgress != null) {
            val updatedCompletedTasks = existingDayProgress.completedTasks.toMutableSet()
            updatedCompletedTasks.add(task.id)

            val updatedDayProgress = existingDayProgress.copy(completedTasks = updatedCompletedTasks)
            val index = dayProgressList.indexOfFirst { it.weekIndex == weekIndex && it.dayIndex == dayIndex }
            dayProgressList[index] = updatedDayProgress
        } else {
            val newDayProgress = DayProgress(
                weekIndex = weekIndex,
                dayIndex = dayIndex,
                completedTasks = setOf(task.id)
            )
            dayProgressList.add(newDayProgress)
        }

        val xpEarned = calculateTaskXp(task)
        return progress.copy(
            dayProgress = dayProgressList,
            totalPoints = progress.totalPoints + xpEarned,
            totalXp = progress.totalXp + xpEarned
        )
    }

    private fun markTaskIncomplete(progress: UserProgress, task: Task): UserProgress {
        // Create updated progress with task marked as incomplete
        val weekIndex = _uiState.value.currentWeekIndex
        val dayIndex = _uiState.value.currentDayIndex

        val dayProgressList = progress.dayProgress.toMutableList()
        val existingDayProgress = dayProgressList.find { it.weekIndex == weekIndex && it.dayIndex == dayIndex }

        if (existingDayProgress != null) {
            val updatedCompletedTasks = existingDayProgress.completedTasks.toMutableSet()
            updatedCompletedTasks.remove(task.id)

            val updatedDayProgress = existingDayProgress.copy(completedTasks = updatedCompletedTasks)
            val index = dayProgressList.indexOfFirst { it.weekIndex == weekIndex && it.dayIndex == dayIndex }
            dayProgressList[index] = updatedDayProgress
        }

        val xpValue = calculateTaskXp(task)
        return progress.copy(
            dayProgress = dayProgressList,
            totalPoints = (progress.totalPoints - xpValue).coerceAtLeast(0),
            totalXp = (progress.totalXp - xpValue).coerceAtLeast(0)
        )
    }

    private fun markDaySkipped(progress: UserProgress): UserProgress {
        // Mark the current day as skipped
        val weekIndex = _uiState.value.currentWeekIndex
        val dayIndex = _uiState.value.currentDayIndex

        val skippedDays = progress.skippedDays.toMutableSet()
        skippedDays.add("${weekIndex}_$dayIndex")

        return progress.copy(skippedDays = skippedDays)
    }

    private fun calculateTaskXp(task: Task): Int {
        return 10
    }

    private fun extractTaskSuffix(taskId: String): String {
        // Extract suffix from custom task ID
        return taskId.substringAfterLast("_")
    }

    override fun onCleared() {
        super.onCleared()
        errorHandler.clearGlobalError()
    }

    /**
     * Data class for internal use
     */
    private data class HomeData(
        val plan: List<WeekPlan>,
        val progress: UserProgress,
        val settings: PlanDurationSettings,
        val todaysTasks: List<Task>,
        val todaysProgress: DayProgress?,
        val currentWeekIndex: Int,
        val currentDayIndex: Int,
        val streakDays: Int,
        val completionPercentage: Float
    )
}

/**
 * Factory for HomeViewModel
 */
class HomeViewModelFactory(
    private val planRepository: PlanRepository,
    private val progressRepository: ProgressRepository,
    private val settingsStore: PlanSettingsStore,
    private val overridesStore: PlanOverridesStore,
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(
                planRepository,
                progressRepository,
                settingsStore,
                overridesStore,
                context
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}