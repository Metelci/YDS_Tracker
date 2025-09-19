package com.mtlc.studyplan.shared

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mtlc.studyplan.data.*
import com.mtlc.studyplan.settings.data.SettingsRepository
import com.mtlc.studyplan.settings.integration.AppIntegrationManager
import com.mtlc.studyplan.gamification.GamificationManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Centralized SharedViewModel for app-wide state management
 * Ensures all screens share data and updates propagate in real-time
 */
class SharedAppViewModel(application: Application) : AndroidViewModel(application) {

    // Core repositories - single source of truth
    private val progressRepository = ProgressRepository(application.dataStore)
    private val planRepository = PlanRepository(
        PlanOverridesStore(application.dataStore),
        PlanSettingsStore(application.dataStore)
    )
    private val settingsRepository = SettingsRepository(application)
    private val gamificationManager = GamificationManager(application.dataStore, progressRepository)

    // App Integration Manager
    private lateinit var appIntegrationManager: AppIntegrationManager

    // ============ SHARED STATE FLOWS ============

    // Tasks and plan data
    val planFlow = planRepository.planFlow
    val todayTasks = combine(
        planFlow,
        progressRepository.userProgressFlow
    ) { plan, progress ->
        getTodayTasksFromPlan(plan, progress)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTasks = combine(
        planFlow,
        progressRepository.taskLogsFlow
    ) { plan, logs ->
        getAllTasksFromPlan(plan, logs)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Progress and statistics
    val userProgress = progressRepository.userProgressFlow
    val taskLogs = progressRepository.taskLogsFlow
    val studyStats = combine(
        userProgress,
        taskLogs
    ) { progress, logs ->
        calculateStudyStats(progress, logs)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StudyStats())

    // Streak tracking
    val currentStreak = userProgress.map { it.streakCount }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Achievements
    val achievements = gamificationManager.gamificationStateFlow.map { it.achievements }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Settings state
    val appSettings = settingsRepository.settingsState.map { settingsMap ->
        AppSettings.fromMap(settingsMap)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    // Navigation events
    private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
    val navigationEvent: SharedFlow<NavigationEvent> = _navigationEvent.asSharedFlow()

    // UI state
    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    // ============ INITIALIZATION ============

    fun initializeIntegration(integrationManager: AppIntegrationManager) {
        this.appIntegrationManager = integrationManager

        // Setup reactive settings integration
        viewModelScope.launch {
            appSettings.collect { settings ->
                applySettingsGlobally(settings)
            }
        }

        // Setup task completion integration with gamification
        viewModelScope.launch {
            userProgress.collect { progress ->
                // Trigger achievements and notifications as needed
                handleProgressUpdates(progress)
            }
        }
    }

    // ============ TASK OPERATIONS ============

    fun completeTask(taskId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                // 1. Complete task in progress repository
                val taskDetails = findTaskDetails(taskId)
                progressRepository.completeTaskWithPoints(
                    taskId = taskId,
                    taskDescription = taskDetails?.description ?: "Task",
                    taskDetails = taskDetails?.details,
                    minutesSpent = taskDetails?.estimatedMinutes ?: 15
                )

                // 2. Update gamification system
                if (::appIntegrationManager.isInitialized) {
                    appIntegrationManager.handleTaskCompletion(
                        taskId = taskId,
                        taskDescription = taskDetails?.description ?: "Task",
                        taskDetails = taskDetails?.details,
                        minutesSpent = taskDetails?.estimatedMinutes ?: 15
                    )
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    lastCompletedTask = taskId,
                    showSuccessFeedback = true
                )

                // 3. Show success notification
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

    fun navigateToProgress(timeRange: TimeRange? = null) {
        viewModelScope.launch {
            _navigationEvent.emit(NavigationEvent.GoToProgress(timeRange))
        }
    }

    fun navigateToSocial(section: SocialSection? = null) {
        viewModelScope.launch {
            _navigationEvent.emit(NavigationEvent.GoToSocial(section))
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
                settingsRepository.updateSetting("notifications_enabled", settings.notificationsEnabled)
                settingsRepository.updateSetting("gamification_enabled", settings.gamificationEnabled)
                settingsRepository.updateSetting("theme_mode", settings.themeMode)
                settingsRepository.updateSetting("difficulty_auto_adjust", settings.autoAdjustDifficulty)
                settingsRepository.updateSetting("daily_goal_tasks", settings.dailyGoalTasks)

                showSettingsUpdatedFeedback()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to update settings: ${e.message}"
                )
            }
        }
    }

    private suspend fun applySettingsGlobally(settings: AppSettings) {
        if (::appIntegrationManager.isInitialized) {
            // Apply theme
            if (settings.themeMode != "system") {
                appIntegrationManager.themeIntegration.updateThemeMode(settings.themeMode)
            }

            // Apply notifications
            // Integration manager will handle notification settings

            // Apply gamification
            // Integration manager will handle gamification settings
        }
    }

    // ============ DATA CALCULATION HELPERS ============

    private fun getTodayTasksFromPlan(plan: List<DayPlan>, progress: UserProgress): List<AppTask> {
        val today = LocalDate.now()
        val todayPlan = plan.find {
            it.date.dayOfYear == today.dayOfYear && it.date.year == today.year
        }

        return todayPlan?.tasks?.map { task ->
            AppTask(
                id = task.id,
                title = task.description,
                description = task.details ?: "",
                category = TaskCategory.fromString(task.description),
                difficulty = TaskDifficulty.MEDIUM,
                estimatedMinutes = 15,
                isCompleted = progress.completedTaskIds.contains(task.id),
                xpReward = 10
            )
        } ?: emptyList()
    }

    private fun getAllTasksFromPlan(plan: List<DayPlan>, logs: List<TaskLog>): List<AppTask> {
        val completedTaskIds = logs.map { it.taskId }.toSet()

        return plan.flatMap { dayPlan ->
            dayPlan.tasks.map { task ->
                AppTask(
                    id = task.id,
                    title = task.description,
                    description = task.details ?: "",
                    category = TaskCategory.fromString(task.description),
                    difficulty = TaskDifficulty.MEDIUM,
                    estimatedMinutes = 15,
                    isCompleted = completedTaskIds.contains(task.id),
                    xpReward = 10
                )
            }
        }
    }

    private fun calculateStudyStats(progress: UserProgress, logs: List<TaskLog>): StudyStats {
        val today = LocalDate.now()
        val thisWeek = logs.filter {
            val logDate = LocalDate.ofEpochDay(it.timestampMillis / (24 * 60 * 60 * 1000))
            ChronoUnit.DAYS.between(logDate, today) <= 7
        }

        return StudyStats(
            totalTasksCompleted = progress.completedTaskIds.size,
            currentStreak = progress.streakCount,
            totalStudyTime = logs.sumOf { it.timeSpentMinutes },
            thisWeekTasks = thisWeek.size,
            thisWeekStudyTime = thisWeek.sumOf { it.timeSpentMinutes },
            averageSessionTime = if (logs.isNotEmpty()) logs.map { it.timeSpentMinutes }.average().toInt() else 0,
            totalXP = progress.totalPoints.toInt()
        )
    }

    private fun findTaskDetails(taskId: String): TaskDetails? {
        val allTasksValue = allTasks.value
        return allTasksValue.find { it.id == taskId }?.let {
            TaskDetails(
                description = it.title,
                details = it.description,
                estimatedMinutes = it.estimatedMinutes
            )
        }
    }

    private suspend fun handleProgressUpdates(progress: UserProgress) {
        // Check for streak milestones
        if (progress.streakCount > 0 && progress.streakCount % 7 == 0) {
            showStreakMilestoneFeedback(progress.streakCount)
        }

        // Check for achievement unlocks would go here
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
    val themeMode: String = "system",
    val autoAdjustDifficulty: Boolean = true,
    val dailyGoalTasks: Int = 5
) {
    companion object {
        fun fromMap(settingsMap: Map<String, Any>): AppSettings {
            return AppSettings(
                notificationsEnabled = settingsMap["notifications_enabled"] as? Boolean ?: true,
                gamificationEnabled = settingsMap["gamification_enabled"] as? Boolean ?: true,
                themeMode = settingsMap["theme_mode"] as? String ?: "system",
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
    VOCABULARY, GRAMMAR, READING, LISTENING, PRACTICE_EXAM, OTHER;

    companion object {
        fun fromString(description: String): TaskCategory {
            return when {
                description.contains("vocabulary", ignoreCase = true) -> VOCABULARY
                description.contains("grammar", ignoreCase = true) -> GRAMMAR
                description.contains("reading", ignoreCase = true) -> READING
                description.contains("listening", ignoreCase = true) -> LISTENING
                description.contains("exam", ignoreCase = true) -> PRACTICE_EXAM
                else -> OTHER
            }
        }
    }
}

enum class TaskDifficulty { EASY, MEDIUM, HARD }

enum class FeedbackType { SUCCESS, ERROR, INFO, WARNING, CELEBRATION }

// ============ NAVIGATION EVENTS ============

sealed class NavigationEvent {
    object GoToHome : NavigationEvent()
    data class GoToTasks(val filter: TaskFilter? = null) : NavigationEvent()
    data class GoToProgress(val timeRange: TimeRange? = null) : NavigationEvent()
    data class GoToSocial(val section: SocialSection? = null) : NavigationEvent()
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

sealed class TimeRange {
    object Today : TimeRange()
    object ThisWeek : TimeRange()
    object ThisMonth : TimeRange()
    object AllTime : TimeRange()
}

sealed class SocialSection {
    object Friends : SocialSection()
    object Groups : SocialSection()
    object Achievements : SocialSection()
    object Leaderboard : SocialSection()
}