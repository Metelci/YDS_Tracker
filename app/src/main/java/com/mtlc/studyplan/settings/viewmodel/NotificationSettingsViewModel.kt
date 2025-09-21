package com.mtlc.studyplan.settings.viewmodel

import android.content.Context
import androidx.lifecycle.*
import com.mtlc.studyplan.core.error.AppError
import com.mtlc.studyplan.core.error.ErrorType
import com.mtlc.studyplan.settings.data.*
import com.mtlc.studyplan.settings.ui.BaseSettingsUiState
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for managing notification settings
 */
class NotificationSettingsViewModel(
    private val repository: SettingsRepository,
    private val context: Context
) : ViewModel() {

    data class NotificationUiState(
        override val isLoading: Boolean = false,
        override val isError: Boolean = false,
        override val isSuccess: Boolean = false,
        val settings: List<SettingItem> = emptyList(),
        override val error: AppError? = null,
        val pushNotifications: Boolean = true,
        val studyReminders: Boolean = true,
        val studyReminderTime: TimeValue = TimeValue(9, 0),
        val achievementAlerts: Boolean = true,
        val emailSummaries: Boolean = false,
        val emailFrequency: EmailFrequency = EmailFrequency.WEEKLY
    ) : BaseSettingsUiState

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        handleError(exception)
    }

    init {
        loadNotificationSettings()
    }

    /**
     * Load notification settings
     */
    private fun loadNotificationSettings() {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            isError = false
        )

        viewModelScope.launch(exceptionHandler) {
            try {
                repository.getNotificationSettings()
                    .catch { exception ->
                        handleError(exception)
                    }
                    .collectLatest { notificationData ->
                        val settings = buildNotificationSettingsList(notificationData)

                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isError = false,
                            isSuccess = true,
                            settings = settings,
                            pushNotifications = notificationData.pushNotifications,
                            studyReminders = notificationData.studyReminders,
                            studyReminderTime = notificationData.studyReminderTime,
                            achievementAlerts = notificationData.achievementAlerts,
                            emailSummaries = notificationData.emailSummaries,
                            emailFrequency = notificationData.emailFrequency,
                            error = null
                        )
                    }
            } catch (exception: Exception) {
                handleError(exception)
            }
        }
    }

    /**
     * Build notification settings list
     */
    private fun buildNotificationSettingsList(notificationData: NotificationData): List<SettingItem> {
        return listOf(
            // Push Notifications Toggle
            ToggleSetting(
                id = "push_notifications",
                title = "Push Notifications",
                description = "Receive notifications on your device",
                value = notificationData.pushNotifications,
                key = SettingsKeys.Notifications.PUSH_NOTIFICATIONS,
                defaultValue = true,
                isEnabled = true,
                category = "notifications",
                sortOrder = 1
            ),

            // Study Reminders Toggle
            ToggleSetting(
                id = "study_reminders",
                title = "Study Reminders",
                description = "Daily reminders to maintain your study streak",
                value = notificationData.studyReminders,
                key = SettingsKeys.Notifications.STUDY_REMINDERS,
                defaultValue = true,
                isEnabled = notificationData.pushNotifications,
                category = "notifications",
                sortOrder = 2
            ),

            // Study Reminder Time
            TimeSetting(
                id = "study_reminder_time",
                title = "Reminder Time",
                description = "When to send daily study reminders",
                currentTime = notificationData.studyReminderTime,
                key = SettingsKeys.Notifications.STUDY_REMINDER_TIME,
                isEnabled = notificationData.pushNotifications && notificationData.studyReminders,
                category = "notifications",
                sortOrder = 3
            ),

            // Achievement Alerts Toggle
            ToggleSetting(
                id = "achievement_alerts",
                title = "Achievement Alerts",
                description = "Get notified when you unlock achievements",
                value = notificationData.achievementAlerts,
                key = SettingsKeys.Notifications.ACHIEVEMENT_ALERTS,
                defaultValue = true,
                isEnabled = notificationData.pushNotifications,
                category = "notifications",
                sortOrder = 4
            ),

            // Email Summaries Toggle
            ToggleSetting(
                id = "email_summaries",
                title = "Email Summaries",
                description = "Receive progress summaries via email",
                value = notificationData.emailSummaries,
                key = SettingsKeys.Notifications.EMAIL_SUMMARIES,
                defaultValue = false,
                isEnabled = true,
                category = "notifications",
                sortOrder = 5
            ),

            // Email Frequency Selection
            SelectionSetting(
                id = "email_frequency",
                title = "Email Frequency",
                description = "How often to receive email summaries",
                options = listOf(
                    SelectionOption(
                        value = EmailFrequency.DAILY,
                        display = "Daily",
                        description = "Receive daily progress updates"
                    ),
                    SelectionOption(
                        value = EmailFrequency.WEEKLY,
                        display = "Weekly",
                        description = "Receive weekly progress summaries"
                    ),
                    SelectionOption(
                        value = EmailFrequency.MONTHLY,
                        display = "Monthly",
                        description = "Receive monthly progress reports"
                    ),
                    SelectionOption(
                        value = EmailFrequency.NEVER,
                        display = "Never",
                        description = "Don't send email summaries"
                    )
                ),
                currentValue = notificationData.emailFrequency,
                key = SettingsKeys.Notifications.EMAIL_SUMMARY_FREQUENCY,
                isEnabled = notificationData.emailSummaries,
                category = "notifications",
                sortOrder = 6
            ),

            // Test Notification Action
            ActionSetting(
                id = "notification_test",
                title = "Send Test Notification",
                description = "Test your notification settings",
                action = com.mtlc.studyplan.settings.data.SettingAction.SendTestNotification,
                buttonText = "Send Test",
                actionType = com.mtlc.studyplan.settings.data.SettingItem.ActionSetting.ActionType.SECONDARY,
                isEnabled = notificationData.pushNotifications,
                category = "notifications",
                sortOrder = 7
            )
        )
    }

    /**
     * Update push notifications setting
     */
    fun updatePushNotifications(enabled: Boolean) {
        viewModelScope.launch(exceptionHandler) {
            try {
                repository.updateNotificationSetting("push_notifications", enabled)
            } catch (exception: Exception) {
                handleError(exception)
            }
        }
    }

    /**
     * Update study reminders setting
     */
    fun updateStudyReminders(enabled: Boolean) {
        viewModelScope.launch(exceptionHandler) {
            try {
                repository.updateNotificationSetting("study_reminders", enabled)
            } catch (exception: Exception) {
                handleError(exception)
            }
        }
    }

    /**
     * Update study reminder time
     */
    fun updateStudyReminderTime(time: TimeValue) {
        viewModelScope.launch(exceptionHandler) {
            try {
                repository.updateNotificationSetting("study_reminder_time", time)
            } catch (exception: Exception) {
                handleError(exception)
            }
        }
    }

    /**
     * Update achievement alerts setting
     */
    fun updateAchievementAlerts(enabled: Boolean) {
        viewModelScope.launch(exceptionHandler) {
            try {
                repository.updateNotificationSetting("achievement_alerts", enabled)
            } catch (exception: Exception) {
                handleError(exception)
            }
        }
    }

    /**
     * Update email summaries setting
     */
    fun updateEmailSummaries(enabled: Boolean) {
        viewModelScope.launch(exceptionHandler) {
            try {
                repository.updateNotificationSetting("email_summaries", enabled)
            } catch (exception: Exception) {
                handleError(exception)
            }
        }
    }

    /**
     * Update email frequency setting
     */
    fun updateEmailFrequency(frequency: EmailFrequency) {
        viewModelScope.launch(exceptionHandler) {
            try {
                repository.updateNotificationSetting("email_frequency", frequency)
            } catch (exception: Exception) {
                handleError(exception)
            }
        }
    }

    /**
     * Send test notification
     */
    fun sendTestNotification() {
        viewModelScope.launch(exceptionHandler) {
            try {
                repository.sendTestNotification()
            } catch (exception: Exception) {
                handleError(exception)
            }
        }
    }

    /**
     * Refresh settings
     */
    fun refresh() {
        loadNotificationSettings()
    }

    /**
     * Retry loading after error
     */
    fun retry() {
        loadNotificationSettings()
    }

    private fun handleError(exception: Throwable) {
        val appError = when (exception) {
            is AppError -> exception
            is IllegalArgumentException -> AppError(
                type = ErrorType.VALIDATION,
                message = exception.message ?: "Invalid notification setting",
                cause = exception
            )
            is SecurityException -> AppError(
                type = ErrorType.PERMISSION,
                message = "Permission denied for notifications",
                cause = exception
            )
            else -> AppError(
                type = ErrorType.UNKNOWN,
                message = exception.message ?: "An unexpected error occurred",
                cause = exception
            )
        }

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            isError = true,
            isSuccess = false,
            error = appError
        )
    }

    override fun onCleared() {
        super.onCleared()
        // Cleanup is handled by repository dispose
    }
}

/**
 * Data classes for notification settings
 */


