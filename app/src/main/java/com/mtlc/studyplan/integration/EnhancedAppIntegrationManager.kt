package com.mtlc.studyplan.integration

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.mtlc.studyplan.repository.*
import com.mtlc.studyplan.eventbus.*
import com.mtlc.studyplan.gamification.GamificationManager
import com.mtlc.studyplan.gamification.GamificationTaskResult
import com.mtlc.studyplan.data.dataStore
import com.mtlc.studyplan.settings.integration.AppIntegrationManager
import com.mtlc.studyplan.database.entities.UserSettingsEntity
import com.mtlc.studyplan.database.entities.TaskEntity
import com.mtlc.studyplan.di.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enhanced app integration manager that coordinates all app subsystems
 * with repository layer and reactive event bus
 */
@Singleton
class EnhancedAppIntegrationManager @Inject constructor(
    private val context: Context,
    private val taskRepository: TaskRepository,
    private val progressRepository: ProgressRepository,
    private val achievementRepository: AchievementRepository,
    private val streakRepository: StreakRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val socialRepository: SocialRepository,
    val eventBus: EventBus,
    @ApplicationScope private val applicationScope: CoroutineScope
) {

    // Legacy integration manager for existing features
    private var legacyIntegrationManager: AppIntegrationManager? = null

    private val gamificationManager = GamificationManager(context.dataStore, progressRepository)

    // Master app state combining all subsystems
    data class MasterAppState(
        val taskState: TaskState,
        val progressState: ProgressState,
        val achievementState: AchievementState,
        val streakState: StreakState,
        val settingsState: SettingsState,
        val socialState: SocialState,
        val syncState: SyncState,
        val isInitialized: Boolean = false,
        val lastUpdated: Long = System.currentTimeMillis()
    )

    // Individual state classes
    data class TaskState(
        val totalTasks: Int = 0,
        val pendingTasks: Int = 0,
        val completedTasks: Int = 0,
        val todayTasks: Int = 0,
        val overdueTasks: Int = 0,
        val completionRate: Float = 0f,
        val isLoading: Boolean = false
    )

    data class ProgressState(
        val todayTasksCompleted: Int = 0,
        val todayStudyMinutes: Int = 0,
        val todayPointsEarned: Int = 0,
        val currentStreak: Int = 0,
        val weeklyProgress: Float = 0f,
        val monthlyProgress: Float = 0f,
        val goalProgress: Float = 0f,
        val isLoading: Boolean = false
    )

    data class AchievementState(
        val totalAchievements: Int = 0,
        val unlockedCount: Int = 0,
        val completionRate: Float = 0f,
        val totalPointsEarned: Int = 0,
        val newUnlockedCount: Int = 0,
        val isLoading: Boolean = false
    )

    data class StreakState(
        val currentDailyStreak: Int = 0,
        val longestStreak: Int = 0,
        val isActive: Boolean = false,
        val perfectDays: Int = 0,
        val riskDays: Int = 0,
        val freezesAvailable: Int = 0,
        val isLoading: Boolean = false
    )

    data class SettingsState(
        val theme: String = "system",
        val accentColor: String = "#1976D2",
        val notificationsEnabled: Boolean = true,
        val dailyStudyGoal: Int = 120,
        val dailyTaskGoal: Int = 5,
        val socialSharingEnabled: Boolean = false,
        val isLoading: Boolean = false
    )

    data class SocialState(
        val todayActivities: Int = 0,
        val weeklyActivities: Int = 0,
        val totalPoints: Int = 0,
        val totalShares: Int = 0,
        val highlightActivities: Int = 0,
        val isLoading: Boolean = false
    )

    data class SyncState(
        val isOnline: Boolean = true,
        val lastSyncTime: Long = 0L,
        val isSyncing: Boolean = false,
        val pendingChanges: Int = 0,
        val syncErrors: List<String> = emptyList()
    )

    // Master state flow
    private val _masterAppState = MutableStateFlow(
        MasterAppState(
            taskState = TaskState(),
            progressState = ProgressState(),
            achievementState = AchievementState(),
            streakState = StreakState(),
            settingsState = SettingsState(),
            socialState = SocialState(),
            syncState = SyncState()
        )
    )
    val masterAppState: StateFlow<MasterAppState> = _masterAppState.asStateFlow()

    // Reactive flows for UI components
    val taskProgress: Flow<TaskRepository.TaskProgress> = taskRepository.todayProgress
    val progressStats: Flow<ProgressRepository.DailyStats> = progressRepository.todayStats
    val achievementStats: Flow<AchievementRepository.AchievementStats> = achievementRepository.achievementStats
    val streakInfo: Flow<StreakRepository.DailyStreakInfo> = streakRepository.dailyStreakInfo
    val userSettings: Flow<UserSettingsEntity?> = userSettingsRepository.getUserSettings()
    val socialStats: Flow<SocialRepository.UserSocialStats> = socialRepository.getUserStats("default_user")

    // Event flows for UI reactions
    val taskEvents: Flow<TaskEvent> = eventBus.subscribeToCategory(TaskEvent::class)
    val achievementEvents: Flow<AchievementEvent> = eventBus.subscribeToCategory(AchievementEvent::class)
    val streakEvents: Flow<StreakEvent> = eventBus.subscribeToCategory(StreakEvent::class)
    val uiEvents: Flow<UIEvent> = eventBus.subscribeToCategory(UIEvent::class)

    // Navigation and UI state
    val navigationEvents: Flow<UIEvent.NavigationRequested> = eventBus.subscribeToUIEvents()
    val snackbarEvents: Flow<UIEvent.SnackbarRequested> = eventBus.subscribeToUIEvents()
    val dialogEvents: Flow<UIEvent.DialogRequested> = eventBus.subscribeToUIEvents()
    val loadingEvents: Flow<UIEvent.LoadingStateChanged> = eventBus.subscribeToUIEvents()

    init {
        initializeIntegration()
        setupEventHandling()
        startDataSync()
    }

    /**
     * Initialize the integration manager
     */
    fun initializeLegacyIntegration(legacyManager: AppIntegrationManager) {
        this.legacyIntegrationManager = legacyManager
    }

    /**
     * Initialize all integrations and start reactive flows
     */
    private fun initializeIntegration() {
        applicationScope.launch {
            try {
                // Initialize default data if needed
                initializeDefaultData()

                // Start combining all states
                combineAllStates()

                // Mark as initialized
                _masterAppState.value = _masterAppState.value.copy(isInitialized = true)

                // Publish initialization event
                eventBus.publish(
                    AnalyticsEvent.UserActionTracked(
                        action = "app_initialized",
                        screen = "startup",
                        properties = mapOf("duration" to "0") // Could track actual duration
                    )
                )

            } catch (e: Exception) {
                eventBus.publish(
                    UIEvent.ErrorOccurred(
                        component = "AppIntegration",
                        errorMessage = "Failed to initialize app: ${e.message}",
                        isCritical = true
                    )
                )
            }
        }
    }

    /**
     * Setup event handling for automated responses
     */
    private fun setupEventHandling() {
        // Handle loading state changes
        eventBus.subscribeToUIEvents<UIEvent.LoadingStateChanged>()
            .onEach { event ->
                updateLoadingState(event.component, event.isLoading)
            }
            .launchIn(applicationScope)

        // Handle refresh requests
        eventBus.subscribeToUIEvents<UIEvent.RefreshRequested>()
            .onEach { event ->
                handleRefreshRequest(event.component, event.reason)
            }
            .launchIn(applicationScope)

        // Handle error occurrences
        eventBus.subscribeToUIEvents<UIEvent.ErrorOccurred>()
            .onEach { event ->
                handleError(event)
            }
            .launchIn(applicationScope)
    }

    /**
     * Start data synchronization flows
     */
    private fun startDataSync() {
        applicationScope.launch {
            // Sync repositories refresh triggers
            combine(
                taskRepository.refreshTrigger,
                progressRepository.refreshTrigger,
                achievementRepository.refreshTrigger,
                streakRepository.refreshTrigger,
                userSettingsRepository.refreshTrigger,
                socialRepository.refreshTrigger
            ) { triggers ->
                triggers.maxOrNull() ?: 0L
            }
            .distinctUntilChanged()
            .onEach {
                _masterAppState.value = _masterAppState.value.copy(lastUpdated = it)
            }
            .launchIn(this)
        }
    }

    /**
     * Combine all states into master state
     */
    private fun combineAllStates() {
        combine(
            taskProgress,
            progressStats,
            achievementStats,
            streakInfo,
            userSettings,
            socialStats
        ) { taskProg, progStats, achStats, streakInf, settings, socialSt ->

            val taskState = TaskState(
                totalTasks = taskProg.completedTotal + taskProg.pendingTotal,
                pendingTasks = taskProg.pendingTotal,
                completedTasks = taskProg.completedTotal,
                todayTasks = taskProg.totalToday,
                completionRate = taskProg.completionRate
            )

            val progressState = ProgressState(
                todayTasksCompleted = progStats.tasksCompleted,
                todayStudyMinutes = progStats.studyMinutes,
                todayPointsEarned = progStats.pointsEarned,
                currentStreak = progStats.streak,
                goalProgress = progStats.goalProgress.overallProgress
            )

            val achievementState = AchievementState(
                totalAchievements = achStats.totalAchievements,
                unlockedCount = achStats.unlockedCount,
                completionRate = achStats.completionRate,
                totalPointsEarned = achStats.totalPointsEarned,
                newUnlockedCount = achStats.newUnlockedCount
            )

            val streakState = StreakState(
                currentDailyStreak = streakInf.currentStreak,
                longestStreak = streakInf.longestStreak,
                isActive = streakInf.isActive,
                perfectDays = streakInf.perfectDays,
                riskDays = streakInf.riskDays,
                freezesAvailable = streakInf.freezesAvailable
            )

            val settingsState = SettingsState(
                theme = settings?.theme ?: "system",
                accentColor = settings?.accentColor ?: "#1976D2",
                notificationsEnabled = settings?.notificationsEnabled ?: true,
                dailyStudyGoal = settings?.dailyStudyGoalMinutes ?: 120,
                dailyTaskGoal = settings?.dailyTaskGoal ?: 5,
                socialSharingEnabled = settings?.socialSharingEnabled ?: false
            )

            val socialState = SocialState(
                todayActivities = socialSt.todayActivities,
                weeklyActivities = socialSt.weeklyActivities,
                totalPoints = socialSt.totalPoints,
                totalShares = socialSt.totalShares,
                highlightActivities = socialSt.highlightActivities
            )

            MasterAppState(
                taskState = taskState,
                progressState = progressState,
                achievementState = achievementState,
                streakState = streakState,
                settingsState = settingsState,
                socialState = socialState,
                syncState = _masterAppState.value.syncState,
                isInitialized = true,
                lastUpdated = System.currentTimeMillis()
            )
        }
        .onEach { newState ->
            _masterAppState.value = newState
        }
        .launchIn(applicationScope)
    }

    /**
     * Initialize default data if needed
     */
    private suspend fun initializeDefaultData() {
        // Initialize user settings if not exists
        val settings = userSettingsRepository.getUserSettingsSync()
        if (settings == null) {
            userSettingsRepository.insertUserSettings(UserSettingsEntity())
        }

        // Initialize default streaks if not exists
        val dailyStreak = streakRepository.getDailyStreak()
        if (dailyStreak == null) {
            streakRepository.initializeDefaultStreaks()
        }
    }

    // Business logic methods

    /**
     * Complete a task with full integration
     */
    suspend fun completeTask(
        taskId: String,
        actualMinutes: Int = 0,
        pointsEarned: Int = 10
    ): GamificationTaskResult? {
        return try {
            val task = taskRepository.getTaskById(taskId)
            if (task == null) {
                eventBus.publish(
                    UIEvent.ErrorOccurred(
                        component = "TaskCompletion",
                        errorMessage = "Task not found: $taskId",
                        isCritical = false
                    )
                )
                null
            } else {
                taskRepository.completeTask(taskId, actualMinutes)

                val gamificationResult = gamificationManager.completeTaskWithGamification(
                    taskId = taskId,
                    taskDescription = task.title,
                    taskDetails = task.description,
                    minutesSpent = actualMinutes,
                    isCorrect = true
                )

                eventBus.publish(
                    TaskEvent.TaskCompleted(
                        taskId = taskId,
                        taskTitle = task.title,
                        category = task.category.name,
                        studyMinutes = actualMinutes,
                        pointsEarned = gamificationResult.pointsEarned.toInt()
                    )
                )

                gamificationResult
            }
        } catch (e: Exception) {
            eventBus.publish(
                UIEvent.ErrorOccurred(
                    component = "TaskCompletion",
                    errorMessage = "Failed to complete task: ${e.message}",
                    isCritical = false
                )
            )
            null
        }
    }

    /**
     * Create a new task with integration
     */
    suspend fun createTask(task: TaskEntity) {
        try {
            taskRepository.insertTask(task)

            eventBus.publish(
                TaskEvent.TaskCreated(
                    taskId = task.id,
                    taskTitle = task.title,
                    category = task.category.name,
                    priority = task.priority.name
                )
            )

        } catch (e: Exception) {
            eventBus.publish(
                UIEvent.ErrorOccurred(
                    component = "TaskCreation",
                    errorMessage = "Failed to create task: ${e.message}",
                    isCritical = false
                )
            )
        }
    }

    /**
     * Update user settings with integration
     */
    suspend fun updateSettings(updater: suspend (UserSettingsEntity) -> UserSettingsEntity) {
        try {
            userSettingsRepository.updateSetting("default_user", updater)

            eventBus.publish(
                UIEvent.RefreshRequested(
                    component = "all",
                    reason = "settings_updated"
                )
            )

        } catch (e: Exception) {
            eventBus.publish(
                UIEvent.ErrorOccurred(
                    component = "SettingsUpdate",
                    errorMessage = "Failed to update settings: ${e.message}",
                    isCritical = false
                )
            )
        }
    }

    /**
     * Trigger manual sync
     */
    suspend fun triggerSync() {
        try {
            _masterAppState.value = _masterAppState.value.copy(
                syncState = _masterAppState.value.syncState.copy(isSyncing = true)
            )

            eventBus.publish(
                SyncEvent.SyncStarted(
                    syncType = "manual",
                    components = listOf("tasks", "progress", "achievements", "streaks", "settings", "social")
                )
            )

            // Simulate sync process
            kotlinx.coroutines.delay(2000)

            _masterAppState.value = _masterAppState.value.copy(
                syncState = _masterAppState.value.syncState.copy(
                    isSyncing = false,
                    lastSyncTime = System.currentTimeMillis()
                )
            )

            eventBus.publish(
                SyncEvent.SyncCompleted(
                    syncType = "manual",
                    components = listOf("tasks", "progress", "achievements", "streaks", "settings", "social"),
                    duration = 2000L,
                    changes = 0
                )
            )

        } catch (e: Exception) {
            _masterAppState.value = _masterAppState.value.copy(
                syncState = _masterAppState.value.syncState.copy(isSyncing = false)
            )

            eventBus.publish(
                SyncEvent.SyncFailed(
                    syncType = "manual",
                    component = "app",
                    errorMessage = e.message ?: "Unknown error",
                    retryCount = 0
                )
            )
        }
    }

    // Helper methods

    private fun updateLoadingState(component: String, isLoading: Boolean) {
        val currentState = _masterAppState.value

        val updatedState = when (component) {
            "tasks" -> currentState.copy(taskState = currentState.taskState.copy(isLoading = isLoading))
            "progress" -> currentState.copy(progressState = currentState.progressState.copy(isLoading = isLoading))
            "achievements" -> currentState.copy(achievementState = currentState.achievementState.copy(isLoading = isLoading))
            "streaks" -> currentState.copy(streakState = currentState.streakState.copy(isLoading = isLoading))
            "settings" -> currentState.copy(settingsState = currentState.settingsState.copy(isLoading = isLoading))
            "social" -> currentState.copy(socialState = currentState.socialState.copy(isLoading = isLoading))
            else -> currentState
        }

        _masterAppState.value = updatedState
    }

    private suspend fun handleRefreshRequest(component: String, reason: String) {
        when (component) {
            "all" -> {
                // Refresh all repositories
                taskRepository.refreshTrigger
                progressRepository.refreshTrigger
                achievementRepository.refreshTrigger
                streakRepository.refreshTrigger
                userSettingsRepository.refreshTrigger
                socialRepository.refreshTrigger
            }
            "tasks" -> taskRepository.refreshTrigger
            "progress" -> progressRepository.refreshTrigger
            // Add other components as needed
        }
    }

    private suspend fun handleError(event: UIEvent.ErrorOccurred) {
        // Log error for analytics
        eventBus.publish(
            AnalyticsEvent.ErrorTracked(
                errorType = "UI_ERROR",
                errorMessage = event.errorMessage,
                context = mapOf(
                    "component" to event.component,
                    "critical" to event.isCritical.toString()
                )
            )
        )

        // Show error to user if critical
        if (event.isCritical) {
            eventBus.publish(
                UIEvent.DialogRequested(
                    title = "Error",
                    message = event.errorMessage,
                    type = "ERROR"
                )
            )
        }
    }

    /**
     * Get summary of app state for debugging
     */
    fun getAppStateSummary(): String {
        val state = _masterAppState.value
        return """
            App State Summary:
            - Initialized: ${state.isInitialized}
            - Tasks: ${state.taskState.completedTasks}/${state.taskState.totalTasks} completed
            - Today's Progress: ${state.progressState.todayTasksCompleted} tasks, ${state.progressState.todayStudyMinutes} minutes
            - Current Streak: ${state.streakState.currentDailyStreak} days
            - Achievements: ${state.achievementState.unlockedCount}/${state.achievementState.totalAchievements} unlocked
            - Last Updated: ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(state.lastUpdated))}
        """.trimIndent()
    }
}


