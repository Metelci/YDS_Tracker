package com.mtlc.studyplan.settings.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mtlc.studyplan.settings.data.*
import com.mtlc.studyplan.settings.models.SettingItem
import com.mtlc.studyplan.settings.models.SettingsUiState
import com.mtlc.studyplan.settings.models.SelectionOption
import com.mtlc.studyplan.core.error.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Enhanced ViewModel for managing settings state with comprehensive error handling
 */
class EnhancedSettingsViewModel(
    private val preferencesManager: SettingsPreferencesManager,
    private val context: Context
) : ViewModel() {

    private val errorHandler = ErrorHandler(ErrorLogger(context))

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _globalError = MutableStateFlow<AppError?>(null)
    val globalError: StateFlow<AppError?> = _globalError.asStateFlow()

    init {
        // Observe global errors from error handler
        viewModelScope.launch {
            errorHandler.globalErrors.collect { error ->
                _globalError.value = error
            }
        }
    }

    fun clearError() {
        _globalError.value = null
        _uiState.value = _uiState.value.copy(error = null)
        errorHandler.clearGlobalError()
    }

    fun retry() {
        clearError()
        // Subclasses should override this to implement retry logic
    }

    protected suspend fun <T> executeWithErrorHandling(
        operation: suspend () -> T,
        onSuccess: (T) -> Unit = {},
        onError: (AppError) -> Unit = { error -> _uiState.value = _uiState.value.copy(error = error, isLoading = false) }
    ) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        val result = errorHandler.handleOperation(
            operation = operation,
            additionalInfo = mapOf(
                "viewModel" to this::class.simpleName.orEmpty(),
                "timestamp" to System.currentTimeMillis()
            )
        )

        result.fold(
            onSuccess = { data ->
                _uiState.value = _uiState.value.copy(isLoading = false, error = null)
                onSuccess(data)
            },
            onFailure = { exception ->
                val error = ErrorMapper.mapThrowable(exception)
                onError(error)
            }
        )
    }

    override fun onCleared() {
        super.onCleared()
        errorHandler.clearGlobalError()
    }

    /**
     * Privacy Settings ViewModel with Error Handling
     */
    class PrivacySettingsViewModel(
        private val preferencesManager: SettingsPreferencesManager,
        context: Context
    ) : EnhancedSettingsViewModel(preferencesManager, context) {

        init {
            loadPrivacySettings()
        }

        private fun loadPrivacySettings() {
            viewModelScope.launch {
                executeWithErrorHandling(
                    operation = {
                        preferencesManager.privacySettings.first()
                    },
                    onSuccess = { settings ->
                        _uiState.value = SettingsUiState(
                            items = buildPrivacySettingItems(settings),
                            isLoading = false
                        )
                    }
                )

                // Continue observing changes
                preferencesManager.privacySettings.collect { settings ->
                    try {
                        val items = buildPrivacySettingItems(settings)
                        _uiState.value = _uiState.value.copy(items = items)
                    } catch (e: Exception) {
                        val error = ErrorMapper.mapThrowable(e)
                        _uiState.value = _uiState.value.copy(error = error)
                    }
                }
            }
        }

        override fun retry() {
            super.retry()
            loadPrivacySettings()
        }

        private fun buildPrivacySettingItems(settings: PrivacySettings): List<SettingItem> {
            return try {
                listOf(
                    SettingItem.Selection(
                        id = "profile_visibility",
                        title = "Profile Visibility",
                        description = "Who can see your profile",
                        selectedValue = settings.profileVisibility.name,
                        options = ProfileVisibility.values().map { visibility ->
                            SelectionOption(
                                value = visibility.name,
                                displayName = when (visibility) {
                                    ProfileVisibility.PUBLIC -> "Public"
                                    ProfileVisibility.FRIENDS_ONLY -> "Friends Only"
                                    ProfileVisibility.PRIVATE -> "Private"
                                }
                            )
                        },
                        onSelectionChange = { value ->
                            updateProfileVisibility(ProfileVisibility.valueOf(value))
                        }
                    ),
                    SettingItem.Toggle(
                        id = "anonymous_analytics",
                        title = "Anonymous Analytics",
                        description = "Help improve the app with usage data",
                        isEnabled = settings.anonymousAnalytics,
                        onToggle = { enabled ->
                            updateAnonymousAnalytics(enabled)
                        }
                    ),
                    SettingItem.Toggle(
                        id = "progress_sharing",
                        title = "Progress Sharing",
                        description = "Allow sharing of progress statistics",
                        isEnabled = settings.progressSharing,
                        onToggle = { enabled ->
                            updateProgressSharing(enabled)
                        }
                    )
                )
            } catch (e: Exception) {
                throw AppError.SystemError.UnexpectedError(e)
            }
        }

        private fun updateProfileVisibility(visibility: ProfileVisibility) {
            viewModelScope.launch {
                executeWithErrorHandling(
                    operation = {
                        validateProfileVisibility(visibility)
                        val currentSettings = preferencesManager.privacySettings.first()
                        preferencesManager.updatePrivacySettings(
                            currentSettings.copy(profileVisibility = visibility)
                        )
                        visibility
                    },
                    onSuccess = {
                        // Success is handled by the flow observer
                    }
                )
            }
        }

        private fun updateAnonymousAnalytics(enabled: Boolean) {
            viewModelScope.launch {
                executeWithErrorHandling(
                    operation = {
                        val currentSettings = preferencesManager.privacySettings.first()
                        preferencesManager.updatePrivacySettings(
                            currentSettings.copy(anonymousAnalytics = enabled)
                        )
                        enabled
                    }
                )
            }
        }

        private fun updateProgressSharing(enabled: Boolean) {
            viewModelScope.launch {
                executeWithErrorHandling(
                    operation = {
                        val currentSettings = preferencesManager.privacySettings.first()
                        preferencesManager.updatePrivacySettings(
                            currentSettings.copy(progressSharing = enabled)
                        )
                        enabled
                    }
                )
            }
        }

        private fun validateProfileVisibility(visibility: ProfileVisibility) {
            // Add validation logic if needed
            when (visibility) {
                ProfileVisibility.PUBLIC,
                ProfileVisibility.FRIENDS_ONLY,
                ProfileVisibility.PRIVATE -> Unit // Valid
                else -> throw AppError.ValidationError.InvalidInput
            }
        }
    }

    /**
     * Notification Settings ViewModel with Error Handling
     */
    class NotificationSettingsViewModel(
        private val preferencesManager: SettingsPreferencesManager,
        context: Context
    ) : EnhancedSettingsViewModel(preferencesManager, context) {

        init {
            loadNotificationSettings()
        }

        private fun loadNotificationSettings() {
            viewModelScope.launch {
                executeWithErrorHandling(
                    operation = {
                        preferencesManager.notificationSettings.first()
                    },
                    onSuccess = { settings ->
                        _uiState.value = SettingsUiState(
                            items = buildNotificationSettingItems(settings),
                            isLoading = false
                        )
                    }
                )

                preferencesManager.notificationSettings.collect { settings ->
                    try {
                        val items = buildNotificationSettingItems(settings)
                        _uiState.value = _uiState.value.copy(items = items)
                    } catch (e: Exception) {
                        val error = ErrorMapper.mapThrowable(e)
                        _uiState.value = _uiState.value.copy(error = error)
                    }
                }
            }
        }

        override fun retry() {
            super.retry()
            loadNotificationSettings()
        }

        private fun buildNotificationSettingItems(settings: NotificationSettings): List<SettingItem> {
            return listOf(
                SettingItem.Toggle(
                    id = "push_notifications",
                    title = "Push Notifications",
                    description = "Receive notifications on your device",
                    isEnabled = settings.pushNotifications,
                    onToggle = { enabled ->
                        updatePushNotifications(enabled)
                    }
                ),
                SettingItem.Toggle(
                    id = "study_reminders",
                    title = "Study Reminders",
                    description = "Daily reminders to maintain your streak",
                    isEnabled = settings.studyReminders,
                    onToggle = { enabled ->
                        updateStudyReminders(enabled)
                    }
                ),
                SettingItem.Toggle(
                    id = "achievement_alerts",
                    title = "Achievement Alerts",
                    description = "Get notified when you unlock achievements",
                    isEnabled = settings.achievementAlerts,
                    onToggle = { enabled ->
                        updateAchievementAlerts(enabled)
                    }
                ),
                SettingItem.Toggle(
                    id = "email_summaries",
                    title = "Email Summaries",
                    description = "Weekly progress summaries via email",
                    isEnabled = settings.emailSummaries,
                    onToggle = { enabled ->
                        updateEmailSummaries(enabled)
                    }
                )
            )
        }

        private fun updatePushNotifications(enabled: Boolean) {
            viewModelScope.launch {
                executeWithErrorHandling(
                    operation = {
                        // Check system notification permission if enabling
                        if (enabled && !isNotificationPermissionGranted()) {
                            throw AppError.SecurityError.PermissionDenied
                        }

                        val currentSettings = preferencesManager.notificationSettings.first()
                        preferencesManager.updateNotificationSettings(
                            currentSettings.copy(pushNotifications = enabled)
                        )
                        enabled
                    }
                )
            }
        }

        private fun updateStudyReminders(enabled: Boolean) {
            viewModelScope.launch {
                executeWithErrorHandling(
                    operation = {
                        val currentSettings = preferencesManager.notificationSettings.first()
                        preferencesManager.updateNotificationSettings(
                            currentSettings.copy(studyReminders = enabled)
                        )
                        enabled
                    }
                )
            }
        }

        private fun updateAchievementAlerts(enabled: Boolean) {
            viewModelScope.launch {
                executeWithErrorHandling(
                    operation = {
                        val currentSettings = preferencesManager.notificationSettings.first()
                        preferencesManager.updateNotificationSettings(
                            currentSettings.copy(achievementAlerts = enabled)
                        )
                        enabled
                    }
                )
            }
        }

        private fun updateEmailSummaries(enabled: Boolean) {
            viewModelScope.launch {
                executeWithErrorHandling(
                    operation = {
                        // Validate email configuration if enabling
                        if (enabled && !isEmailConfigured()) {
                            throw AppError.ValidationError.InvalidFormat("email")
                        }

                        val currentSettings = preferencesManager.notificationSettings.first()
                        preferencesManager.updateNotificationSettings(
                            currentSettings.copy(emailSummaries = enabled)
                        )
                        enabled
                    }
                )
            }
        }

        private fun isNotificationPermissionGranted(): Boolean {
            // Check notification permission
            return android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU ||
                    androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
        }

        private fun isEmailConfigured(): Boolean {
            // Check if user has configured email
            // This would typically check user profile or account settings
            return true // Placeholder
        }
    }

    /**
     * Task Settings ViewModel with Error Handling
     */
    class TaskSettingsViewModel(
        private val preferencesManager: SettingsPreferencesManager,
        context: Context
    ) : EnhancedSettingsViewModel(preferencesManager, context) {

        init {
            loadTaskSettings()
        }

        private fun loadTaskSettings() {
            viewModelScope.launch {
                executeWithErrorHandling(
                    operation = {
                        preferencesManager.taskSettings.first()
                    },
                    onSuccess = { settings ->
                        _uiState.value = SettingsUiState(
                            items = buildTaskSettingItems(settings),
                            isLoading = false
                        )
                    }
                )

                preferencesManager.taskSettings.collect { settings ->
                    try {
                        val items = buildTaskSettingItems(settings)
                        _uiState.value = _uiState.value.copy(items = items)
                    } catch (e: Exception) {
                        val error = ErrorMapper.mapThrowable(e)
                        _uiState.value = _uiState.value.copy(error = error)
                    }
                }
            }
        }

        override fun retry() {
            super.retry()
            loadTaskSettings()
        }

        private fun buildTaskSettingItems(settings: TaskSettings): List<SettingItem> {
            return listOf(
                SettingItem.Toggle(
                    id = "smart_scheduling",
                    title = "Smart Scheduling",
                    description = "AI-powered study session recommendations",
                    isEnabled = settings.smartScheduling,
                    onToggle = { enabled ->
                        updateSmartScheduling(enabled)
                    }
                ),
                SettingItem.Toggle(
                    id = "auto_difficulty",
                    title = "Auto Difficulty Adjustment",
                    description = "Automatically adjust task difficulty",
                    isEnabled = settings.autoDifficultyAdjustment,
                    onToggle = { enabled ->
                        updateAutoDifficulty(enabled)
                    }
                ),
                SettingItem.Toggle(
                    id = "daily_goal_reminders",
                    title = "Daily Goal Reminders",
                    description = "Remind me of my daily study goals",
                    isEnabled = settings.dailyGoalReminders,
                    onToggle = { enabled ->
                        updateDailyGoalReminders(enabled)
                    }
                ),
                SettingItem.Toggle(
                    id = "weekend_mode",
                    title = "Weekend Mode",
                    description = "Lighter study load on weekends",
                    isEnabled = settings.weekendMode,
                    onToggle = { enabled ->
                        updateWeekendMode(enabled)
                    }
                )
            )
        }

        private fun updateSmartScheduling(enabled: Boolean) {
            viewModelScope.launch {
                executeWithErrorHandling(
                    operation = {
                        val currentSettings = preferencesManager.taskSettings.first()
                        preferencesManager.updateTaskSettings(
                            currentSettings.copy(smartScheduling = enabled)
                        )
                        enabled
                    }
                )
            }
        }

        private fun updateAutoDifficulty(enabled: Boolean) {
            viewModelScope.launch {
                executeWithErrorHandling(
                    operation = {
                        val currentSettings = preferencesManager.taskSettings.first()
                        preferencesManager.updateTaskSettings(
                            currentSettings.copy(autoDifficultyAdjustment = enabled)
                        )
                        enabled
                    }
                )
            }
        }

        private fun updateDailyGoalReminders(enabled: Boolean) {
            viewModelScope.launch {
                executeWithErrorHandling(
                    operation = {
                        val currentSettings = preferencesManager.taskSettings.first()
                        preferencesManager.updateTaskSettings(
                            currentSettings.copy(dailyGoalReminders = enabled)
                        )
                        enabled
                    }
                )
            }
        }

        private fun updateWeekendMode(enabled: Boolean) {
            viewModelScope.launch {
                executeWithErrorHandling(
                    operation = {
                        val currentSettings = preferencesManager.taskSettings.first()
                        preferencesManager.updateTaskSettings(
                            currentSettings.copy(weekendMode = enabled)
                        )
                        enabled
                    }
                )
            }
        }
    }

    // Similar implementation for NavigationSettingsViewModel, GamificationSettingsViewModel, and SocialSettingsViewModel
    // Following the same pattern with error handling
}

/**
 * Enhanced Factory for creating SettingsViewModels with dependencies including Context
 */
class EnhancedSettingsViewModelFactory(
    private val preferencesManager: SettingsPreferencesManager,
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when (modelClass) {
            EnhancedSettingsViewModel::class.java -> EnhancedSettingsViewModel(preferencesManager, context) as T
            EnhancedSettingsViewModel.PrivacySettingsViewModel::class.java -> EnhancedSettingsViewModel.PrivacySettingsViewModel(preferencesManager, context) as T
            EnhancedSettingsViewModel.NotificationSettingsViewModel::class.java -> EnhancedSettingsViewModel.NotificationSettingsViewModel(preferencesManager, context) as T
            EnhancedSettingsViewModel.TaskSettingsViewModel::class.java -> EnhancedSettingsViewModel.TaskSettingsViewModel(preferencesManager, context) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
