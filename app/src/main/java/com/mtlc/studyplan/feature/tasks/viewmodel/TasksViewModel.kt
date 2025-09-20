package com.mtlc.studyplan.feature.tasks.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mtlc.studyplan.feature.tasks.*
import com.mtlc.studyplan.data.*
import com.mtlc.studyplan.core.error.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * UI State for Tasks with comprehensive error handling
 */
data class TasksUiState(
    val tasks: List<com.mtlc.studyplan.feature.tasks.TaskItem> = emptyList(),
    val filteredTasks: List<com.mtlc.studyplan.feature.tasks.TaskItem> = emptyList(),
    val selectedCategory: com.mtlc.studyplan.data.TaskCategory? = null,
    val selectedDifficulty: com.mtlc.studyplan.shared.TaskDifficulty? = null,
    val completedTasksCount: Int = 0,
    val totalXp: Int = 0,
    val totalStudyTime: Int = 0,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: AppError? = null,
    val operationStates: Map<String, Boolean> = emptyMap()
) {
    val isSuccess: Boolean get() = tasks.isNotEmpty() && error == null && !isLoading
    val isError: Boolean get() = error != null
    val isEmpty: Boolean get() = tasks.isEmpty() && !isLoading && error == null
    val completionRate: Float get() = if (tasks.isNotEmpty()) completedTasksCount.toFloat() / tasks.size else 0f

    fun isTaskLoading(taskId: String): Boolean = operationStates["task_$taskId"] == true
}

/**
 * Enhanced Tasks ViewModel with comprehensive error handling
 */
class TasksViewModel(
    private val planRepository: PlanRepository,
    private val progressRepository: ProgressRepository,
    private val context: Context
) : ViewModel() {

    private val errorHandler = ErrorHandler(ErrorLogger(context))

    private val _uiState = MutableStateFlow(TasksUiState())
    val uiState: StateFlow<TasksUiState> = _uiState.asStateFlow()

    private val _globalError = MutableStateFlow<AppError?>(null)
    val globalError: StateFlow<AppError?> = _globalError.asStateFlow()

    // Filter states
    private val _categoryFilter = MutableStateFlow<com.mtlc.studyplan.data.TaskCategory?>(null)
    private val _difficultyFilter = MutableStateFlow<com.mtlc.studyplan.shared.TaskDifficulty?>(null)

    init {
        loadTasks()
        observeDataChanges()
        observeFilters()

        // Observe global errors
        viewModelScope.launch {
            errorHandler.globalErrors.collect { error ->
                _globalError.value = error
            }
        }
    }

    private fun loadTasks() {
        viewModelScope.launch {
            setLoadingState(true)

            executeWithErrorHandling(
                operation = {
                    val plan = planRepository.planFlow.first()
                    val progress = progressRepository.userProgressFlow.first()

                    val tasks = generateTasksFromPlan(plan, progress)
                    val stats = calculateTaskStatistics(tasks, progress)

                    TaskData(tasks, stats)
                },
                onSuccess = { data ->
                    _uiState.value = _uiState.value.copy(
                        tasks = data.tasks,
                        filteredTasks = data.tasks,
                        completedTasksCount = data.stats.completedCount,
                        totalXp = data.stats.totalXp,
                        totalStudyTime = data.stats.totalStudyTime,
                        isLoading = false,
                        error = null
                    )
                    applyFilters()
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
                    if (_uiState.value.tasks.isNotEmpty()) {
                        updateTasksFromPlan(plan)
                    }
                } catch (e: Exception) {
                    handleError(ErrorMapper.mapThrowable(e))
                }
            }
        }

        viewModelScope.launch {
            progressRepository.userProgressFlow.collect { progress ->
                try {
                    if (_uiState.value.tasks.isNotEmpty()) {
                        updateTaskProgress(progress)
                    }
                } catch (e: Exception) {
                    handleError(ErrorMapper.mapThrowable(e))
                }
            }
        }
    }

    private fun observeFilters() {
        viewModelScope.launch {
            combine(_categoryFilter, _difficultyFilter) { category, difficulty ->
                Pair(category, difficulty)
            }.collect { (category, difficulty) ->
                _uiState.value = _uiState.value.copy(
                    selectedCategory = category,
                    selectedDifficulty = difficulty
                )
                applyFilters()
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            setRefreshingState(true)

            executeWithErrorHandling(
                operation = {
                    // Force refresh data from repositories
                    loadTasks()
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
            setTaskOperationLoading(taskId, true)

            executeWithErrorHandling(
                operation = {
                    validateTaskOperation(taskId)

                    val task = findTaskById(taskId)
                        ?: throw AppError.DataError.NotFound

                    // Update task completion in progress repository
                    val currentProgress = progressRepository.userProgressFlow.first()
                    val updatedProgress = updateTaskCompletion(currentProgress, task, true)

                    // Save progress (this would be the actual implementation)
                    // progressRepository.updateProgress(updatedProgress)

                    task
                },
                onSuccess = { task ->
                    updateTaskInState(task.copy(isCompleted = true))
                    updateStatistics()
                    setTaskOperationLoading(taskId, false)
                },
                onError = { error ->
                    setTaskOperationLoading(taskId, false)
                    handleError(error)
                }
            )
        }
    }

    fun uncompleteTask(taskId: String) {
        viewModelScope.launch {
            setTaskOperationLoading(taskId, true)

            executeWithErrorHandling(
                operation = {
                    validateTaskOperation(taskId)

                    val task = findTaskById(taskId)
                        ?: throw AppError.DataError.NotFound

                    val currentProgress = progressRepository.userProgressFlow.first()
                    val updatedProgress = updateTaskCompletion(currentProgress, task, false)

                    // Save progress
                    // progressRepository.updateProgress(updatedProgress)

                    task
                },
                onSuccess = { task ->
                    updateTaskInState(task.copy(isCompleted = false))
                    updateStatistics()
                    setTaskOperationLoading(taskId, false)
                },
                onError = { error ->
                    setTaskOperationLoading(taskId, false)
                    handleError(error)
                }
            )
        }
    }

    fun filterByCategory(category: com.mtlc.studyplan.data.TaskCategory?) {
        _categoryFilter.value = category
    }

    fun filterByDifficulty(difficulty: com.mtlc.studyplan.shared.TaskDifficulty?) {
        _difficultyFilter.value = difficulty
    }

    fun clearFilters() {
        _categoryFilter.value = null
        _difficultyFilter.value = null
    }

    fun retryFailedOperation(taskId: String) {
        viewModelScope.launch {
            executeWithErrorHandling(
                operation = {
                    // Retry the last failed operation for this task
                    // This could be completion, un-completion, or data loading
                    val task = findTaskById(taskId)
                        ?: throw AppError.DataError.NotFound

                    // Determine what operation to retry based on current state
                    if (task.isCompleted) {
                        uncompleteTask(taskId)
                    } else {
                        completeTask(taskId)
                    }
                },
                onSuccess = {
                    // Success handled by individual operations
                },
                onError = { error ->
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
        loadTasks()
    }

    // Private helper methods
    private fun setLoadingState(isLoading: Boolean) {
        _uiState.value = _uiState.value.copy(isLoading = isLoading, error = null)
    }

    private fun setRefreshingState(isRefreshing: Boolean) {
        _uiState.value = _uiState.value.copy(isRefreshing = isRefreshing)
    }

    private fun setTaskOperationLoading(taskId: String, isLoading: Boolean) {
        val currentStates = _uiState.value.operationStates.toMutableMap()
        if (isLoading) {
            currentStates["task_$taskId"] = true
        } else {
            currentStates.remove("task_$taskId")
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
                "viewModel" to "TasksViewModel",
                "timestamp" to System.currentTimeMillis(),
                "tasksCount" to _uiState.value.tasks.size
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

    private fun validateTaskOperation(taskId: String) {
        if (taskId.isBlank()) {
            throw AppError.ValidationError.RequiredFieldEmpty
        }

        val taskExists = _uiState.value.tasks.any { it.id == taskId }
        if (!taskExists) {
            throw AppError.DataError.NotFound
        }
    }

    private fun findTaskById(taskId: String): com.mtlc.studyplan.feature.tasks.TaskItem? {
        return _uiState.value.tasks.find { it.id == taskId }
    }

    private fun updateTaskInState(updatedTask: com.mtlc.studyplan.feature.tasks.TaskItem) {
        val currentTasks = _uiState.value.tasks.toMutableList()
        val index = currentTasks.indexOfFirst { it.id == updatedTask.id }
        if (index != -1) {
            currentTasks[index] = updatedTask
            _uiState.value = _uiState.value.copy(tasks = currentTasks)
            applyFilters()
        }
    }

    private fun updateStatistics() {
        val tasks = _uiState.value.tasks
        val completedCount = tasks.count { it.isCompleted }
        val totalXp = tasks.filter { it.isCompleted }.sumOf { it.xp }
        val totalStudyTime = tasks.filter { it.isCompleted }.sumOf { it.duration }

        _uiState.value = _uiState.value.copy(
            completedTasksCount = completedCount,
            totalXp = totalXp,
            totalStudyTime = totalStudyTime
        )
    }

    private fun applyFilters() {
        val tasks = _uiState.value.tasks
        val filteredTasks = tasks.filter { task ->
            val categoryMatch = _uiState.value.selectedCategory?.let { category ->
                // Convert task.category to data.TaskCategory for comparison
                convertTaskCategoryToData(task.category) == category
            } ?: true

            val difficultyMatch = _uiState.value.selectedDifficulty?.let { difficulty ->
                // Convert task.difficulty to shared.TaskDifficulty for comparison
                convertTaskDifficultyToShared(task.difficulty) == difficulty
            } ?: true

            categoryMatch && difficultyMatch
        }

        _uiState.value = _uiState.value.copy(filteredTasks = filteredTasks)
    }

    private fun generateTasksFromPlan(plan: List<WeekPlan>, progress: UserProgress): List<com.mtlc.studyplan.feature.tasks.TaskItem> {
        // Convert plan data to TaskItems
        // This is a simplified implementation
        val tasks = mutableListOf<com.mtlc.studyplan.feature.tasks.TaskItem>()

        plan.forEachIndexed { weekIndex, weekPlan ->
            weekPlan.days.forEachIndexed { dayIndex, dayPlan ->
                dayPlan.tasks.forEach { task ->
                    val taskItem = com.mtlc.studyplan.feature.tasks.TaskItem(
                        id = "${weekIndex}_${dayIndex}_${task.id}",
                        title = task.desc,
                        category = mapTaskToCategory(task),
                        difficulty = mapTaskToDifficulty(task),
                        duration = 30, // Default duration since PlanTask doesn't have durationMinutes
                        xp = calculateXpForTask(task),
                        isCompleted = isTaskCompleted(task, progress)
                    )
                    tasks.add(taskItem)
                }
            }
        }

        return tasks
    }

    private fun calculateTaskStatistics(tasks: List<com.mtlc.studyplan.feature.tasks.TaskItem>, progress: UserProgress): TaskStatistics {
        val completedCount = tasks.count { it.isCompleted }
        val totalXp = tasks.filter { it.isCompleted }.sumOf { it.xp }
        val totalStudyTime = tasks.filter { it.isCompleted }.sumOf { it.duration }

        return TaskStatistics(completedCount, totalXp, totalStudyTime)
    }

    private fun updateTasksFromPlan(plan: List<WeekPlan>) {
        // Update tasks when plan changes
        // Implementation would merge new plan data with existing tasks
    }

    private fun updateTaskProgress(progress: UserProgress) {
        // Update task completion status based on progress
        // Implementation would check progress data and update task states
    }

    private fun updateTaskCompletion(progress: UserProgress, task: com.mtlc.studyplan.feature.tasks.TaskItem, completed: Boolean): UserProgress {
        // Update progress data with task completion
        // This would be the actual implementation to save task completion
        return progress
    }

    private fun mapTaskToCategory(task: PlanTask): com.mtlc.studyplan.feature.tasks.TaskCategory {
        // Map task type to category enum
        return when {
            task.desc.contains("vocabulary", ignoreCase = true) -> com.mtlc.studyplan.feature.tasks.TaskCategory.VOCABULARY
            task.desc.contains("grammar", ignoreCase = true) -> com.mtlc.studyplan.feature.tasks.TaskCategory.GRAMMAR
            task.desc.contains("reading", ignoreCase = true) -> com.mtlc.studyplan.feature.tasks.TaskCategory.READING
            task.desc.contains("listening", ignoreCase = true) -> com.mtlc.studyplan.feature.tasks.TaskCategory.LISTENING
            else -> com.mtlc.studyplan.feature.tasks.TaskCategory.VOCABULARY
        }
    }

    private fun mapTaskToDifficulty(task: PlanTask): com.mtlc.studyplan.feature.tasks.TaskDifficulty {
        // Map task difficulty based on description or other factors since PlanTask doesn't have durationMinutes
        return when {
            task.desc.contains("easy", ignoreCase = true) -> com.mtlc.studyplan.feature.tasks.TaskDifficulty.EASY
            task.desc.contains("hard", ignoreCase = true) -> com.mtlc.studyplan.feature.tasks.TaskDifficulty.HARD
            else -> com.mtlc.studyplan.feature.tasks.TaskDifficulty.MEDIUM
        }
    }

    private fun calculateXpForTask(task: PlanTask): Int {
        // Calculate XP based on task complexity
        return when {
            task.desc.contains("hard", ignoreCase = true) -> 50
            task.desc.contains("easy", ignoreCase = true) -> 20
            else -> 30
        }
    }

    private fun isTaskCompleted(task: PlanTask, progress: UserProgress): Boolean {
        // Check if task is completed in progress data
        return false // Placeholder
    }

    // Converter functions for filter comparisons
    private fun convertTaskCategoryToData(category: com.mtlc.studyplan.feature.tasks.TaskCategory): com.mtlc.studyplan.data.TaskCategory {
        return when (category) {
            com.mtlc.studyplan.feature.tasks.TaskCategory.VOCABULARY -> com.mtlc.studyplan.data.TaskCategory.VOCABULARY
            com.mtlc.studyplan.feature.tasks.TaskCategory.GRAMMAR -> com.mtlc.studyplan.data.TaskCategory.GRAMMAR
            com.mtlc.studyplan.feature.tasks.TaskCategory.READING -> com.mtlc.studyplan.data.TaskCategory.READING
            com.mtlc.studyplan.feature.tasks.TaskCategory.LISTENING -> com.mtlc.studyplan.data.TaskCategory.LISTENING
            com.mtlc.studyplan.feature.tasks.TaskCategory.OTHER -> com.mtlc.studyplan.data.TaskCategory.OTHER
        }
    }

    private fun convertTaskDifficultyToShared(difficulty: com.mtlc.studyplan.feature.tasks.TaskDifficulty): com.mtlc.studyplan.shared.TaskDifficulty {
        return when (difficulty) {
            com.mtlc.studyplan.feature.tasks.TaskDifficulty.EASY -> com.mtlc.studyplan.shared.TaskDifficulty.EASY
            com.mtlc.studyplan.feature.tasks.TaskDifficulty.MEDIUM -> com.mtlc.studyplan.shared.TaskDifficulty.MEDIUM
            com.mtlc.studyplan.feature.tasks.TaskDifficulty.HARD -> com.mtlc.studyplan.shared.TaskDifficulty.HARD
            com.mtlc.studyplan.feature.tasks.TaskDifficulty.EXPERT -> com.mtlc.studyplan.shared.TaskDifficulty.EXPERT
        }
    }




    override fun onCleared() {
        super.onCleared()
        errorHandler.clearGlobalError()
    }

    /**
     * Data classes for internal use
     */
    private data class TaskData(
        val tasks: List<com.mtlc.studyplan.feature.tasks.TaskItem>,
        val stats: TaskStatistics
    )

    private data class TaskStatistics(
        val completedCount: Int,
        val totalXp: Int,
        val totalStudyTime: Int
    )
}

/**
 * Factory for TasksViewModel
 */
class TasksViewModelFactory(
    private val planRepository: PlanRepository,
    private val progressRepository: ProgressRepository,
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TasksViewModel::class.java)) {
            return TasksViewModel(planRepository, progressRepository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
