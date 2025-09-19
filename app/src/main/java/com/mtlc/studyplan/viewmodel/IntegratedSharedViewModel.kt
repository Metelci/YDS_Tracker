package com.mtlc.studyplan.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.mtlc.studyplan.integration.EnhancedAppIntegrationManager
import com.mtlc.studyplan.eventbus.*
import com.mtlc.studyplan.database.entities.TaskEntity
import com.mtlc.studyplan.shared.TaskCategory
import com.mtlc.studyplan.shared.TaskPriority
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Modern shared ViewModel that uses the integrated data architecture
 */
@HiltViewModel
class IntegratedSharedViewModel @Inject constructor(
    private val integrationManager: EnhancedAppIntegrationManager
) : ViewModel() {

    // Master app state from integration manager
    val appState = integrationManager.masterAppState

    // Reactive data flows for UI
    val taskProgress = integrationManager.taskProgress
    val progressStats = integrationManager.progressStats
    val achievementStats = integrationManager.achievementStats
    val streakInfo = integrationManager.streakInfo
    val userSettings = integrationManager.userSettings
    val socialStats = integrationManager.socialStats

    // Event flows for UI reactions
    val taskEvents = integrationManager.taskEvents
    val achievementEvents = integrationManager.achievementEvents
    val streakEvents = integrationManager.streakEvents
    val uiEvents = integrationManager.uiEvents

    // Navigation and UI event flows
    val navigationEvents = integrationManager.navigationEvents
    val snackbarEvents = integrationManager.snackbarEvents
    val dialogEvents = integrationManager.dialogEvents
    val loadingEvents = integrationManager.loadingEvents

    // Derived state for UI components
    val isLoading = appState.map { state ->
        state.taskState.isLoading ||
        state.progressState.isLoading ||
        state.achievementState.isLoading ||
        state.streakState.isLoading ||
        state.settingsState.isLoading ||
        state.socialState.isLoading ||
        state.syncState.isSyncing
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isInitialized = appState.map { it.isInitialized }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val hasErrors = appState.map { state ->
        state.syncState.syncErrors.isNotEmpty()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // UI state for common components
    data class DashboardState(
        val todayTasksCompleted: Int,
        val todayStudyMinutes: Int,
        val currentStreak: Int,
        val todayGoalProgress: Float,
        val unlockedAchievements: Int,
        val totalAchievements: Int,
        val newAchievements: Int,
        val socialActivitiesToday: Int
    )

    val dashboardState = appState.map { state ->
        DashboardState(
            todayTasksCompleted = state.progressState.todayTasksCompleted,
            todayStudyMinutes = state.progressState.todayStudyMinutes,
            currentStreak = state.streakState.currentDailyStreak,
            todayGoalProgress = state.progressState.goalProgress,
            unlockedAchievements = state.achievementState.unlockedCount,
            totalAchievements = state.achievementState.totalAchievements,
            newAchievements = state.achievementState.newUnlockedCount,
            socialActivitiesToday = state.socialState.todayActivities
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000),
        DashboardState(0, 0, 0, 0f, 0, 0, 0, 0))

    // Business logic methods

    /**
     * Complete a task with full integration
     */
    fun completeTask(taskId: String, actualMinutes: Int = 0, pointsEarned: Int = 10) {
        viewModelScope.launch {
            integrationManager.completeTask(taskId, actualMinutes, pointsEarned)
        }
    }

    /**
     * Create a new task
     */
    fun createTask(
        title: String,
        description: String = "",
        category: TaskCategory = TaskCategory.OTHER,
        priority: TaskPriority = TaskPriority.MEDIUM,
        estimatedMinutes: Int = 30,
        dueDate: Long? = null
    ) {
        viewModelScope.launch {
            val task = TaskEntity(
                title = title,
                description = description,
                category = category,
                priority = priority,
                estimatedMinutes = estimatedMinutes,
                dueDate = dueDate
            )
            integrationManager.createTask(task)
        }
    }

    /**
     * Update daily study goal
     */
    fun updateDailyStudyGoal(minutes: Int) {
        viewModelScope.launch {
            integrationManager.updateSettings { settings ->
                settings.copy(dailyStudyGoalMinutes = minutes)
            }
        }
    }

    /**
     * Update daily task goal
     */
    fun updateDailyTaskGoal(tasks: Int) {
        viewModelScope.launch {
            integrationManager.updateSettings { settings ->
                settings.copy(dailyTaskGoal = tasks)
            }
        }
    }

    /**
     * Toggle theme
     */
    fun toggleTheme() {
        viewModelScope.launch {
            integrationManager.updateSettings { settings ->
                val newTheme = when (settings.theme) {
                    "light" -> "dark"
                    "dark" -> "system"
                    else -> "light"
                }
                settings.copy(theme = newTheme)
            }
        }
    }

    /**
     * Toggle notifications
     */
    fun toggleNotifications() {
        viewModelScope.launch {
            integrationManager.updateSettings { settings ->
                settings.copy(notificationsEnabled = !settings.notificationsEnabled)
            }
        }
    }

    /**
     * Toggle social sharing
     */
    fun toggleSocialSharing() {
        viewModelScope.launch {
            integrationManager.updateSettings { settings ->
                settings.copy(socialSharingEnabled = !settings.socialSharingEnabled)
            }
        }
    }

    /**
     * Trigger manual sync
     */
    fun triggerSync() {
        viewModelScope.launch {
            integrationManager.triggerSync()
        }
    }

    /**
     * Refresh all data
     */
    fun refreshAll() {
        viewModelScope.launch {
            // This will be handled by the integration manager's refresh logic
            integrationManager.eventBus.publish(
                UIEvent.RefreshRequested(
                    component = "all",
                    reason = "user_request"
                )
            )
        }
    }

    /**
     * Get debug info
     */
    fun getDebugInfo(): String {
        return integrationManager.getAppStateSummary()
    }

    /**
     * Handle navigation events
     */
    fun navigate(destination: String, arguments: Map<String, String> = emptyMap()) {
        viewModelScope.launch {
            integrationManager.eventBus.publish(
                UIEvent.NavigationRequested(destination, arguments)
            )
        }
    }

    /**
     * Show snackbar message
     */
    fun showSnackbar(message: String, actionLabel: String? = null, duration: String = "SHORT") {
        viewModelScope.launch {
            integrationManager.eventBus.publish(
                UIEvent.SnackbarRequested(message, actionLabel, duration)
            )
        }
    }

    /**
     * Show dialog
     */
    fun showDialog(title: String, message: String, type: String = "INFO") {
        viewModelScope.launch {
            integrationManager.eventBus.publish(
                UIEvent.DialogRequested(title, message, type)
            )
        }
    }

    /**
     * Report error
     */
    fun reportError(component: String, errorMessage: String, isCritical: Boolean = false) {
        viewModelScope.launch {
            integrationManager.eventBus.publish(
                UIEvent.ErrorOccurred(component, errorMessage, isCritical = isCritical)
            )
        }
    }

    /**
     * Set loading state for a component
     */
    fun setLoadingState(component: String, isLoading: Boolean) {
        viewModelScope.launch {
            integrationManager.eventBus.publish(
                UIEvent.LoadingStateChanged(component, isLoading)
            )
        }
    }

    // Lifecycle methods
    override fun onCleared() {
        super.onCleared()
        // Clean up any resources if needed
    }
}