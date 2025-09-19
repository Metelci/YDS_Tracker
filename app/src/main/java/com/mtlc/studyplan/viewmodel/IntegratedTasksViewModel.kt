package com.mtlc.studyplan.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.mtlc.studyplan.integration.EnhancedAppIntegrationManager
import com.mtlc.studyplan.repository.TaskRepository
import com.mtlc.studyplan.database.entities.TaskEntity
import com.mtlc.studyplan.shared.TaskCategory
import com.mtlc.studyplan.shared.TaskPriority
import com.mtlc.studyplan.eventbus.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Tasks ViewModel that integrates with the enhanced app integration manager
 */
@HiltViewModel
class IntegratedTasksViewModel @Inject constructor(
    private val integrationManager: EnhancedAppIntegrationManager,
    private val taskRepository: TaskRepository
) : ViewModel() {

    // Task data flows
    val allTasks = taskRepository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingTasks = taskRepository.pendingTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedTasks = taskRepository.completedTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayTasks = taskRepository.todayTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val overdueTasks = taskRepository.overdueTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val upcomingTasks = taskRepository.upcomingTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Task statistics
    val taskProgress = integrationManager.taskProgress
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000),
            TaskRepository.TaskProgress(0, 0, 0, 0, 0f))

    val categoryProgress = taskRepository.categoryProgress
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Task events
    val taskEvents = integrationManager.taskEvents
        .filterIsInstance<TaskEvent>()

    // UI state
    private val _uiState = MutableStateFlow(TasksUiState())
    val uiState = _uiState.asStateFlow()

    // Filter and sort state
    private val _filterState = MutableStateFlow(TaskFilterState())
    val filterState = _filterState.asStateFlow()

    // Search state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // Filtered and sorted tasks
    val filteredTasks = combine(
        allTasks,
        _filterState,
        _searchQuery
    ) { tasks, filter, query ->
        filterAndSortTasks(tasks, filter, query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        observeTaskEvents()
    }

    // Business logic methods

    /**
     * Create a new task
     */
    fun createTask(
        title: String,
        description: String = "",
        category: TaskCategory = TaskCategory.OTHER,
        priority: TaskPriority = TaskPriority.MEDIUM,
        estimatedMinutes: Int = 30,
        dueDate: Long? = null,
        reminderTime: Long? = null
    ) {
        if (title.isBlank()) {
            showError("Task title cannot be empty")
            return
        }

        viewModelScope.launch {
            try {
                setLoading(true)

                val task = TaskEntity(
                    title = title.trim(),
                    description = description.trim(),
                    category = category,
                    priority = priority,
                    estimatedMinutes = estimatedMinutes,
                    dueDate = dueDate,
                    reminderTime = reminderTime,
                    difficulty = "intermediate"
                )

                integrationManager.createTask(task)
                showSuccess("Task created successfully")

            } catch (e: Exception) {
                showError("Failed to create task: ${e.message}")
            } finally {
                setLoading(false)
            }
        }
    }

    /**
     * Complete a task
     */
    fun completeTask(taskId: String, actualMinutes: Int = 0) {
        viewModelScope.launch {
            try {
                setLoading(true)

                // Calculate points based on priority and time
                val task = taskRepository.getTaskById(taskId)
                val pointsEarned = calculatePoints(task, actualMinutes)

                val gamificationResult = integrationManager.completeTask(taskId, actualMinutes, pointsEarned)

                if (gamificationResult != null) {
                    showSuccess("Task completed! +${gamificationResult.pointsEarned} pts")
                } else {
                    showSuccess("Task completed")
                }

            } catch (e: Exception) {
                showError("Failed to complete task: ${e.message}")
            } finally {
                setLoading(false)
            }
        }
    }

    /**
     * Update a task
     */
    fun updateTask(task: TaskEntity) {
        viewModelScope.launch {
            try {
                setLoading(true)
                taskRepository.updateTask(task.copy(lastModified = System.currentTimeMillis()))
                showSuccess("Task updated successfully")
            } catch (e: Exception) {
                showError("Failed to update task: ${e.message}")
            } finally {
                setLoading(false)
            }
        }
    }

    /**
     * Delete a task
     */
    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            try {
                setLoading(true)
                taskRepository.deleteTask(taskId)
                showSuccess("Task deleted successfully")
            } catch (e: Exception) {
                showError("Failed to delete task: ${e.message}")
            } finally {
                setLoading(false)
            }
        }
    }

    /**
     * Move task to category
     */
    fun moveTaskToCategory(taskId: String, newCategory: TaskCategory) {
        viewModelScope.launch {
            try {
                taskRepository.moveTaskToCategory(taskId, newCategory)
                showSuccess("Task moved to ${newCategory.name}")
            } catch (e: Exception) {
                showError("Failed to move task: ${e.message}")
            }
        }
    }

    /**
     * Reorder tasks
     */
    fun reorderTasks(tasks: List<TaskEntity>) {
        viewModelScope.launch {
            try {
                taskRepository.reorderTasks(tasks)
            } catch (e: Exception) {
                showError("Failed to reorder tasks: ${e.message}")
            }
        }
    }

    /**
     * Search tasks
     */
    fun searchTasks(query: String) {
        _searchQuery.value = query
    }

    /**
     * Update filter
     */
    fun updateFilter(filter: TaskFilterState) {
        _filterState.value = filter
    }

    /**
     * Clear filters
     */
    fun clearFilters() {
        _filterState.value = TaskFilterState()
        _searchQuery.value = ""
    }

    /**
     * Refresh tasks
     */
    fun refreshTasks() {
        viewModelScope.launch {
            integrationManager.eventBus.publish(
                UIEvent.RefreshRequested(
                    component = "tasks",
                    reason = "user_request"
                )
            )
        }
    }

    // Helper methods

    private fun observeTaskEvents() {
        taskEvents
            .onEach { event ->
                when (event) {
                    is TaskEvent.TaskCompleted -> {
                        showSuccess("Task '${event.taskTitle}' completed! +${event.pointsEarned} points")
                    }
                    is TaskEvent.TaskCreated -> {
                        // Task creation success is handled in createTask method
                    }
                    is TaskEvent.TaskDeleted -> {
                        // Task deletion success is handled in deleteTask method
                    }
                    else -> {
                        // Handle other task events as needed
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun filterAndSortTasks(
        tasks: List<TaskEntity>,
        filter: TaskFilterState,
        query: String
    ): List<TaskEntity> {
        var filteredTasks = tasks

        // Apply search query
        if (query.isNotBlank()) {
            filteredTasks = filteredTasks.filter { task ->
                task.title.contains(query, ignoreCase = true) ||
                task.description.contains(query, ignoreCase = true) ||
                task.notes?.contains(query, ignoreCase = true) == true
            }
        }

        // Apply filters
        if (filter.category != null) {
            filteredTasks = filteredTasks.filter { it.category == filter.category }
        }

        if (filter.priority != null) {
            filteredTasks = filteredTasks.filter { it.priority == filter.priority }
        }

        if (filter.completionStatus != null) {
            filteredTasks = filteredTasks.filter { it.isCompleted == filter.completionStatus }
        }

        if (filter.hasReminder != null) {
            filteredTasks = filteredTasks.filter { (it.reminderTime != null) == filter.hasReminder }
        }

        if (filter.isOverdue != null) {
            val currentTime = System.currentTimeMillis()
            filteredTasks = filteredTasks.filter { task ->
                val isOverdue = task.dueDate != null && task.dueDate!! < currentTime && !task.isCompleted
                isOverdue == filter.isOverdue
            }
        }

        // Apply sorting
        return when (filter.sortBy) {
            TaskSortBy.PRIORITY -> filteredTasks.sortedByDescending { it.priority.ordinal }
            TaskSortBy.DUE_DATE -> filteredTasks.sortedBy { it.dueDate ?: Long.MAX_VALUE }
            TaskSortBy.CREATED_DATE -> filteredTasks.sortedByDescending { it.createdAt }
            TaskSortBy.TITLE -> filteredTasks.sortedBy { it.title }
            TaskSortBy.CATEGORY -> filteredTasks.sortedBy { it.category.name }
        }
    }

    private fun calculatePoints(task: TaskEntity?, actualMinutes: Int): Int {
        if (task == null) return 10

        val basePoints = when (task.priority) {
            TaskPriority.LOW -> 5
            TaskPriority.MEDIUM -> 10
            TaskPriority.HIGH -> 15
            TaskPriority.CRITICAL -> 20
        }

        val timeBonus = (actualMinutes / 15) * 2 // 2 points per 15 minutes
        return basePoints + timeBonus
    }

    private fun setLoading(isLoading: Boolean) {
        _uiState.value = _uiState.value.copy(isLoading = isLoading)
        integrationManager.eventBus.let { eventBus ->
            viewModelScope.launch {
                eventBus.publish(
                    UIEvent.LoadingStateChanged("tasks", isLoading)
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
                    UIEvent.ErrorOccurred("TasksViewModel", message, isCritical = false)
                )
            }
        }
    }

    // Data classes
    data class TasksUiState(
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
        val successMessage: String? = null
    )

    data class TaskFilterState(
        val category: TaskCategory? = null,
        val priority: TaskPriority? = null,
        val completionStatus: Boolean? = null, // null = all, true = completed, false = pending
        val hasReminder: Boolean? = null,
        val isOverdue: Boolean? = null,
        val sortBy: TaskSortBy = TaskSortBy.PRIORITY
    )

    enum class TaskSortBy {
        PRIORITY, DUE_DATE, CREATED_DATE, TITLE, CATEGORY
    }
}
