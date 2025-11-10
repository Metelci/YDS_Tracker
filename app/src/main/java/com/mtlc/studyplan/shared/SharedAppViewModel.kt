package com.mtlc.studyplan.shared

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mtlc.studyplan.data.*
import com.mtlc.studyplan.settings.data.SettingsRepository
import com.mtlc.studyplan.settings.data.SettingsUpdateRequest
import com.mtlc.studyplan.settings.integration.AppIntegrationManager
import com.mtlc.studyplan.repository.TaskRepository as RoomTaskRepository
import com.mtlc.studyplan.database.entities.TaskEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit


/**
 * Centralized SharedViewModel for app-wide state management
 * Ensures all screens share data and updates propagate in real-time
 */
class SharedAppViewModel(
    application: Application,
    private val planRepository: PlanRepository,
    private val settingsRepository: SettingsRepository,
    private val appIntegrationManager: AppIntegrationManager,
    private val taskRepository: RoomTaskRepository
) : AndroidViewModel(application) {

    // ============ SHARED STATE FLOWS ============

    // Tasks and plan data - consolidated for better performance
    val planFlow = planRepository.planFlow

    // Create a consolidated state that combines all plan-derived data
    data class TaskState(
        val todayTasks: List<AppTask> = emptyList(),
        val allTasks: List<AppTask> = emptyList()
    )

    val taskState = planFlow.map { plan ->
        TaskState(
            todayTasks = getTodayTasksFromPlan(plan),
            allTasks = getAllTasksFromPlan(plan)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TaskState())

    // Derived flows for compatibility
    val todayTasks = taskState.map { it.todayTasks }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTasks = taskState.map { it.allTasks }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Study statistics (real-time from TaskRepository)
    private val completedTasks: StateFlow<List<TaskEntity>> =
        taskRepository.completedTasks
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val studyStats: StateFlow<StudyStats> =
        completedTasks
            .map { computeStudyStats(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StudyStats())

    val currentStreak: StateFlow<Int> =
        completedTasks
            .map { calculateCurrentStreak(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val achievements = MutableStateFlow(emptyList<String>())

    // Settings state
    private val notificationsEnabledFlow = settingsRepository.getSettingFlow("notifications_enabled", true)
    private val gamificationEnabledFlow = settingsRepository.getSettingFlow("gamification_enabled", true)
    private val autoAdjustDifficultyFlow = settingsRepository.getSettingFlow("difficulty_auto_adjust", true)
    private val dailyGoalTasksFlow = settingsRepository.getSettingFlow("daily_goal_tasks", 5)

    val appSettings = combine(
        notificationsEnabledFlow,
        gamificationEnabledFlow,
        autoAdjustDifficultyFlow,
        dailyGoalTasksFlow
    ) { notificationsEnabled, gamificationEnabled, autoAdjustDifficulty, dailyGoalTasks ->
        AppSettings(
            notificationsEnabled = notificationsEnabled,
            gamificationEnabled = gamificationEnabled,
            autoAdjustDifficulty = autoAdjustDifficulty,
            dailyGoalTasks = dailyGoalTasks
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    // Navigation events
    private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
    val navigationEvent: SharedFlow<NavigationEvent> = _navigationEvent.asSharedFlow()

    // UI state
    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    private var cachedPlan: List<WeekPlan> = emptyList()

    init {
        viewModelScope.launch {
            planFlow.collect { plan ->
                cachedPlan = plan
            }
        }
        viewModelScope.launch {
            appSettings.collect { settings ->
                applySettingsGlobally(settings)
            }
        }
    }

    // ============ INITIALIZATION ============

    // ============ TASK OPERATIONS ============

    fun completeTask(taskId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                val taskDetails = findTaskDetails(taskId)

                // Update integration manager if available
                appIntegrationManager.handleTaskCompletion(
                    taskId = taskId,
                    taskDescription = taskDetails?.description ?: "Task",
                    taskDetails = taskDetails?.details,
                    minutesSpent = taskDetails?.estimatedMinutes ?: 15
                )

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    lastCompletedTask = taskId,
                    showSuccessFeedback = true
                )

                showTaskCompletedFeedback(taskDetails?.description ?: "Task")

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to complete task: ${e.message}"
                )
            }
        }
    }

    fun createTask(title: String, category: TaskCategory, difficulty: TaskDifficulty) {
        viewModelScope.launch {
            try {
                // Implementation would add to plan repository
                // For now, trigger navigation to tasks
                navigateToTasks(TaskFilter.CreateNew)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to create task: ${e.message}"
                )
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            try {
                // Implementation would remove from plan
                showTaskDeletedFeedback()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to delete task: ${e.message}"
                )
            }
        }
    }

    // ============ NAVIGATION METHODS ============

    fun navigateToTasks(filter: TaskFilter? = null) {
        viewModelScope.launch {
            _navigationEvent.emit(NavigationEvent.GoToTasks(filter))
        }
    }

    fun navigateToSettings() {
        viewModelScope.launch {
            _navigationEvent.emit(NavigationEvent.GoToSettings)
        }
    }

    fun navigateToHome() {
        viewModelScope.launch {
            _navigationEvent.emit(NavigationEvent.GoToHome)
        }
    }

    // ============ SETTINGS INTEGRATION ============

    fun applySettings(settings: AppSettings) {
        viewModelScope.launch {
            try {
                // Update individual settings in repository
                settingsRepository.updateSetting(SettingsUpdateRequest.UpdateBoolean("notifications_enabled", settings.notificationsEnabled))
                settingsRepository.updateSetting(SettingsUpdateRequest.UpdateBoolean("gamification_enabled", settings.gamificationEnabled))
                // Theme mode removed; always light
                settingsRepository.updateSetting(SettingsUpdateRequest.UpdateBoolean("difficulty_auto_adjust", settings.autoAdjustDifficulty))
                settingsRepository.updateSetting(SettingsUpdateRequest.UpdateInt("daily_goal_tasks", settings.dailyGoalTasks))

                showSettingsUpdatedFeedback()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to update settings: ${e.message}"
                )
            }
        }
    }

    // ============ INTERNAL: STATS COMPUTATION FROM ROOM TASKS ============

    private fun computeStudyStats(tasks: List<TaskEntity>): StudyStats {
        if (tasks.isEmpty()) return StudyStats()
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val weekStart = today.with(java.time.DayOfWeek.MONDAY)

        val completedToday = tasks.filter { it.completedAt != null }.count { te ->
            val d = java.time.Instant.ofEpochMilli(te.completedAt!!).atZone(zone).toLocalDate()
            d == today
        }
        val thisWeek = tasks.filter { it.completedAt != null }.filter { te ->
            val d = java.time.Instant.ofEpochMilli(te.completedAt!!).atZone(zone).toLocalDate()
            !d.isBefore(weekStart)
        }
        val totalStudyTime = tasks.sumOf { it.actualMinutes }
        val thisWeekStudyTime = thisWeek.sumOf { it.actualMinutes }
        val avgSession = if (tasks.isNotEmpty()) totalStudyTime / tasks.size else 0
        val totalXp = tasks.sumOf { it.pointsValue }

        return StudyStats(
            totalTasksCompleted = tasks.size,
            currentStreak = calculateCurrentStreak(tasks),
            totalStudyTime = totalStudyTime,
            thisWeekTasks = thisWeek.size,
            thisWeekStudyTime = thisWeekStudyTime,
            averageSessionTime = avgSession,
            totalXP = totalXp
        )
    }

    private fun calculateCurrentStreak(tasks: List<TaskEntity>): Int {
        val zone = ZoneId.systemDefault()
        val days: List<LocalDate> = tasks
            .mapNotNull { it.completedAt }
            .map { java.time.Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
            .distinct()
            .sorted()
        if (days.isEmpty()) return 0
        var streak = 1
        for (i in days.lastIndex downTo 1) {
            val gap = ChronoUnit.DAYS.between(days[i - 1], days[i])
            if (gap == 1L) streak++ else if (gap > 1L) break
        }
        return streak
    }

    private suspend fun applySettingsGlobally(settings: AppSettings) {
        // Theme mode handled via integrations; no direct action required here.
        // Future: propagate settings for notifications/gamification through dedicated channels.
    }

    // ============ DATA CALCULATION HELPERS ============

    private fun getTodayTasksFromPlan(plan: List<WeekPlan>): List<AppTask> {
        val flattenedDays = plan.flatMap { it.days }
        val today = LocalDate.now()

        val targetDay = flattenedDays.firstOrNull { day ->
            day.day.equals(today.dayOfWeek.name, ignoreCase = true)
        } ?: flattenedDays.firstOrNull() ?: return emptyList()

        return targetDay.tasks.map { task ->
            AppTask(
                id = task.id,
                title = task.desc,
                description = task.details ?: "",
                category = TaskCategory.OTHER,
                difficulty = TaskDifficulty.MEDIUM,
                estimatedMinutes = 15,
                isCompleted = false,
                xpReward = 10
            )
        }
    }

    private fun getAllTasksFromPlan(plan: List<WeekPlan>): List<AppTask> {
        return plan.flatMap { week ->
            week.days.flatMap { day ->
                day.tasks.map { task ->
                    AppTask(
                        id = task.id,
                        title = task.desc,
                        description = task.details ?: "",
                        category = TaskCategory.OTHER,
                        difficulty = TaskDifficulty.MEDIUM,
                        estimatedMinutes = 15,
                        isCompleted = false,
                        xpReward = 10
                    )
                }
            }
        }
    }



    private fun findTaskDetails(taskId: String): TaskDetails? {
        val matchingTask = cachedPlan
            .flatMap { it.days }
            .flatMap { it.tasks }
            .firstOrNull { it.id == taskId }
            ?: return null

        return TaskDetails(
            description = matchingTask.desc,
            details = matchingTask.details,
            estimatedMinutes = 15
        )
    }

    // ============ FEEDBACK METHODS ============

    private fun showTaskCompletedFeedback(taskTitle: String) {
        _uiState.value = _uiState.value.copy(
            feedbackMessage = "✅ Completed: $taskTitle",
            feedbackType = FeedbackType.SUCCESS
        )
    }

    private fun showTaskDeletedFeedback() {
        _uiState.value = _uiState.value.copy(
            feedbackMessage = "🗑️ Task deleted",
            feedbackType = FeedbackType.INFO
        )
    }

    private fun showSettingsUpdatedFeedback() {
        _uiState.value = _uiState.value.copy(
            feedbackMessage = "⚙️ Settings updated",
            feedbackType = FeedbackType.SUCCESS
        )
    }

    private fun showStreakMilestoneFeedback(streak: Int) {
        _uiState.value = _uiState.value.copy(
            feedbackMessage = "🔥 Amazing! $streak day streak!",
            feedbackType = FeedbackType.CELEBRATION
        )
    }

    fun clearFeedback() {
        _uiState.value = _uiState.value.copy(
            feedbackMessage = null,
            feedbackType = null,
            error = null
        )
    }

    fun clearSuccessFeedback() {
        _uiState.value = _uiState.value.copy(showSuccessFeedback = false)
    }
}

// ============ DATA CLASSES ============

data class AppTask(
    val id: String,
    val title: String,
    val description: String,
    val category: TaskCategory,
    val difficulty: TaskDifficulty,
    val estimatedMinutes: Int,
    val isCompleted: Boolean,
    val xpReward: Int
)

data class StudyStats(
    val totalTasksCompleted: Int = 0,
    val currentStreak: Int = 0,
    val totalStudyTime: Int = 0,
    val thisWeekTasks: Int = 0,
    val thisWeekStudyTime: Int = 0,
    val averageSessionTime: Int = 0,
    val totalXP: Int = 0
)

data class AppSettings(
    val notificationsEnabled: Boolean = true,
    val gamificationEnabled: Boolean = true,
    val autoAdjustDifficulty: Boolean = true,
    val dailyGoalTasks: Int = 5
) {
    companion object {
        fun fromMap(settingsMap: Map<String, Any>): AppSettings {
            return AppSettings(
                notificationsEnabled = settingsMap["notifications_enabled"] as? Boolean ?: true,
                gamificationEnabled = settingsMap["gamification_enabled"] as? Boolean ?: true,
                autoAdjustDifficulty = settingsMap["difficulty_auto_adjust"] as? Boolean ?: true,
                dailyGoalTasks = settingsMap["daily_goal_tasks"] as? Int ?: 5
            )
        }
    }
}

data class AppUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val feedbackMessage: String? = null,
    val feedbackType: FeedbackType? = null,
    val showSuccessFeedback: Boolean = false,
    val lastCompletedTask: String? = null
)

data class TaskDetails(
    val description: String,
    val details: String?,
    val estimatedMinutes: Int
)


enum class TaskCategory {
    VOCABULARY, GRAMMAR, READING, LISTENING, OTHER;

    companion object {
        fun fromString(description: String): TaskCategory {
            return when {
                description.contains("vocabulary", ignoreCase = true) -> VOCABULARY
                description.contains("grammar", ignoreCase = true) -> GRAMMAR
                description.contains("reading", ignoreCase = true) -> READING
                description.contains("listening", ignoreCase = true) -> LISTENING
                else -> OTHER
            }
        }
    }
}

enum class TaskDifficulty { EASY, MEDIUM, HARD, EXPERT }

enum class TaskPriority(val displayName: String, val color: Long) {
    LOW("Low", 0xFF4CAF50),
    MEDIUM("Medium", 0xFFFF9800),
    HIGH("High", 0xFFE53E3E),
    CRITICAL("Critical", 0xFFD32F2F)
}

enum class AchievementCategory(val displayName: String) {
    TASKS("Tasks"),
    STREAKS("Streaks"),
    MILESTONES("Milestones"),
    STUDY_TIME("Study Time"),
    PERFORMANCE("Performance"),
    GENERAL("General")
}

enum class FeedbackType { SUCCESS, ERROR, INFO, WARNING, CELEBRATION }

// ============ NAVIGATION EVENTS ============

sealed class NavigationEvent {
    object GoToHome : NavigationEvent()
    data class GoToTasks(val filter: TaskFilter? = null) : NavigationEvent()
    object GoToSettings : NavigationEvent()
}

sealed class TaskFilter {
    object CreateNew : TaskFilter()
    data class ById(val taskId: String) : TaskFilter()
    data class ByCategory(val category: TaskCategory) : TaskFilter()
    data class ByDifficulty(val difficulty: TaskDifficulty) : TaskFilter()
    object CompletedOnly : TaskFilter()
    object IncompleteOnly : TaskFilter()
}

