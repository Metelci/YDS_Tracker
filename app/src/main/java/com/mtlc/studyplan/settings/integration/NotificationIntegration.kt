package com.mtlc.studyplan.settings.integration

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtlc.studyplan.notifications.NotificationHelper
import com.mtlc.studyplan.settings.data.SettingsKeys
import com.mtlc.studyplan.settings.data.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Integration layer between settings and notification system
 */
class NotificationIntegration(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val notificationHelper: NotificationHelper = NotificationHelper
) {

    data class NotificationState(
        val pushNotificationsEnabled: Boolean = true,
        val studyRemindersEnabled: Boolean = true,
        val studyReminderTime: String = "09:00",
        val achievementAlertsEnabled: Boolean = true,
        val emailSummariesEnabled: Boolean = false,
        val emailSummaryFrequency: String = "weekly",
        val weeklyReportsEnabled: Boolean = true,
        val streakWarningsEnabled: Boolean = true,
        val goalRemindersEnabled: Boolean = true,
        val socialNotificationsEnabled: Boolean = true,
        val quietHoursEnabled: Boolean = false,
        val quietHoursStart: String = "22:00",
        val quietHoursEnd: String = "08:00",
        val notificationSound: String = "default",
        val vibrationEnabled: Boolean = true
    )

    private val _notificationState = MutableStateFlow(NotificationState())
    val notificationState: StateFlow<NotificationState> = _notificationState.asStateFlow()

    init {
        observeNotificationSettings()
    }

    private fun observeNotificationSettings() {
        settingsRepository.settingsState
            .map { settings ->
                NotificationState(
                    pushNotificationsEnabled = settings[SettingsKeys.Notifications.PUSH_NOTIFICATIONS] as? Boolean ?: true,
                    studyRemindersEnabled = settings[SettingsKeys.Notifications.STUDY_REMINDERS] as? Boolean ?: true,
                    studyReminderTime = settings[SettingsKeys.Notifications.STUDY_REMINDER_TIME] as? String ?: "09:00",
                    achievementAlertsEnabled = settings[SettingsKeys.Notifications.ACHIEVEMENT_ALERTS] as? Boolean ?: true,
                    emailSummariesEnabled = settings[SettingsKeys.Notifications.EMAIL_SUMMARIES] as? Boolean ?: false,
                    emailSummaryFrequency = settings[SettingsKeys.Notifications.EMAIL_SUMMARY_FREQUENCY] as? String ?: "weekly",
                    weeklyReportsEnabled = settings[SettingsKeys.Notifications.WEEKLY_REPORTS] as? Boolean ?: true,
                    streakWarningsEnabled = settings[SettingsKeys.Notifications.STREAK_WARNINGS] as? Boolean ?: true,
                    goalRemindersEnabled = settings[SettingsKeys.Notifications.GOAL_REMINDERS] as? Boolean ?: true,
                    socialNotificationsEnabled = settings[SettingsKeys.Notifications.SOCIAL_NOTIFICATIONS] as? Boolean ?: true,
                    quietHoursEnabled = settings[SettingsKeys.Notifications.QUIET_HOURS_ENABLED] as? Boolean ?: false,
                    quietHoursStart = settings[SettingsKeys.Notifications.QUIET_HOURS_START] as? String ?: "22:00",
                    quietHoursEnd = settings[SettingsKeys.Notifications.QUIET_HOURS_END] as? String ?: "08:00",
                    notificationSound = settings[SettingsKeys.Notifications.NOTIFICATION_SOUND] as? String ?: "default",
                    vibrationEnabled = settings[SettingsKeys.Notifications.VIBRATION_ENABLED] as? Boolean ?: true
                )
            }
            .onEach { _notificationState.value = it }
            .launchIn(kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main))
    }

    /**
     * Check if notifications are allowed at current time
     */
    fun areNotificationsAllowed(): Boolean {
        val state = _notificationState.value
        if (!state.pushNotificationsEnabled) return false
        if (!state.quietHoursEnabled) return true

        val now = LocalTime.now()
        val startTime = LocalTime.parse(state.quietHoursStart, DateTimeFormatter.ofPattern("HH:mm"))
        val endTime = LocalTime.parse(state.quietHoursEnd, DateTimeFormatter.ofPattern("HH:mm"))

        return if (startTime.isBefore(endTime)) {
            // Same day: 22:00 - 08:00 next day
            now.isBefore(startTime) || now.isAfter(endTime)
        } else {
            // Crosses midnight: 08:00 - 22:00
            now.isAfter(endTime) && now.isBefore(startTime)
        }
    }

    /**
     * Show study reminder if enabled
     */
    fun showStudyReminderIfEnabled() {
        val state = _notificationState.value
        if (state.studyRemindersEnabled && areNotificationsAllowed()) {
            notificationHelper.showStudyReminderNotification(context)
        }
    }

    /**
     * Show achievement notification if enabled
     */
    fun showAchievementNotificationIfEnabled(title: String, message: String) {
        val state = _notificationState.value
        if (state.achievementAlertsEnabled && areNotificationsAllowed()) {
            notificationHelper.showApplicationReminderNotification(
                context = context,
                title = title,
                message = message,
                notificationId = System.currentTimeMillis().toInt()
            )
        }
    }

    /**
     * Show streak warning if enabled
     */
    fun showStreakWarningIfEnabled(daysAtRisk: Int) {
        val state = _notificationState.value
        if (state.streakWarningsEnabled && areNotificationsAllowed()) {
            notificationHelper.showApplicationReminderNotification(
                context = context,
                title = "Streak at Risk! 🔥",
                message = "Your $daysAtRisk day streak is at risk. Complete a task today to maintain it!",
                notificationId = 9999
            )
        }
    }

    /**
     * Show goal reminder if enabled
     */
    fun showGoalReminderIfEnabled(goalDescription: String) {
        val state = _notificationState.value
        if (state.goalRemindersEnabled && areNotificationsAllowed()) {
            notificationHelper.showApplicationReminderNotification(
                context = context,
                title = "Goal Reminder 🎯",
                message = goalDescription,
                notificationId = 8888
            )
        }
    }

    /**
     * Update notification settings
     */
    suspend fun togglePushNotifications() {
        val current = _notificationState.value.pushNotificationsEnabled
        settingsRepository.updateSetting(SettingsKeys.Notifications.PUSH_NOTIFICATIONS, !current)
    }

    suspend fun toggleStudyReminders() {
        val current = _notificationState.value.studyRemindersEnabled
        settingsRepository.updateSetting(SettingsKeys.Notifications.STUDY_REMINDERS, !current)
    }

    suspend fun updateStudyReminderTime(time: String) {
        settingsRepository.updateSetting(SettingsKeys.Notifications.STUDY_REMINDER_TIME, time)
    }

    suspend fun toggleAchievementAlerts() {
        val current = _notificationState.value.achievementAlertsEnabled
        settingsRepository.updateSetting(SettingsKeys.Notifications.ACHIEVEMENT_ALERTS, !current)
    }

    suspend fun toggleEmailSummaries() {
        val current = _notificationState.value.emailSummariesEnabled
        settingsRepository.updateSetting(SettingsKeys.Notifications.EMAIL_SUMMARIES, !current)
    }

    suspend fun updateEmailSummaryFrequency(frequency: String) {
        settingsRepository.updateSetting(SettingsKeys.Notifications.EMAIL_SUMMARY_FREQUENCY, frequency)
    }

    suspend fun toggleWeeklyReports() {
        val current = _notificationState.value.weeklyReportsEnabled
        settingsRepository.updateSetting(SettingsKeys.Notifications.WEEKLY_REPORTS, !current)
    }

    suspend fun toggleStreakWarnings() {
        val current = _notificationState.value.streakWarningsEnabled
        settingsRepository.updateSetting(SettingsKeys.Notifications.STREAK_WARNINGS, !current)
    }

    suspend fun toggleGoalReminders() {
        val current = _notificationState.value.goalRemindersEnabled
        settingsRepository.updateSetting(SettingsKeys.Notifications.GOAL_REMINDERS, !current)
    }

    suspend fun toggleSocialNotifications() {
        val current = _notificationState.value.socialNotificationsEnabled
        settingsRepository.updateSetting(SettingsKeys.Notifications.SOCIAL_NOTIFICATIONS, !current)
    }

    suspend fun toggleQuietHours() {
        val current = _notificationState.value.quietHoursEnabled
        settingsRepository.updateSetting(SettingsKeys.Notifications.QUIET_HOURS_ENABLED, !current)
    }

    suspend fun updateQuietHoursStart(time: String) {
        settingsRepository.updateSetting(SettingsKeys.Notifications.QUIET_HOURS_START, time)
    }

    suspend fun updateQuietHoursEnd(time: String) {
        settingsRepository.updateSetting(SettingsKeys.Notifications.QUIET_HOURS_END, time)
    }

    suspend fun updateNotificationSound(sound: String) {
        settingsRepository.updateSetting(SettingsKeys.Notifications.NOTIFICATION_SOUND, sound)
    }

    suspend fun toggleVibration() {
        val current = _notificationState.value.vibrationEnabled
        settingsRepository.updateSetting(SettingsKeys.Notifications.VIBRATION_ENABLED, !current)
    }
}

/**
 * ViewModel for notification management in UI
 */
class NotificationViewModel(
    private val notificationIntegration: NotificationIntegration
) : ViewModel() {

    val notificationState = notificationIntegration.notificationState

    fun togglePushNotifications() {
        viewModelScope.launch {
            notificationIntegration.togglePushNotifications()
        }
    }

    fun toggleStudyReminders() {
        viewModelScope.launch {
            notificationIntegration.toggleStudyReminders()
        }
    }

    fun updateStudyReminderTime(time: String) {
        viewModelScope.launch {
            notificationIntegration.updateStudyReminderTime(time)
        }
    }

    fun toggleAchievementAlerts() {
        viewModelScope.launch {
            notificationIntegration.toggleAchievementAlerts()
        }
    }

    fun toggleEmailSummaries() {
        viewModelScope.launch {
            notificationIntegration.toggleEmailSummaries()
        }
    }

    fun updateEmailSummaryFrequency(frequency: String) {
        viewModelScope.launch {
            notificationIntegration.updateEmailSummaryFrequency(frequency)
        }
    }

    fun toggleWeeklyReports() {
        viewModelScope.launch {
            notificationIntegration.toggleWeeklyReports()
        }
    }

    fun toggleStreakWarnings() {
        viewModelScope.launch {
            notificationIntegration.toggleStreakWarnings()
        }
    }

    fun toggleGoalReminders() {
        viewModelScope.launch {
            notificationIntegration.toggleGoalReminders()
        }
    }

    fun toggleSocialNotifications() {
        viewModelScope.launch {
            notificationIntegration.toggleSocialNotifications()
        }
    }

    fun toggleQuietHours() {
        viewModelScope.launch {
            notificationIntegration.toggleQuietHours()
        }
    }

    fun updateQuietHoursStart(time: String) {
        viewModelScope.launch {
            notificationIntegration.updateQuietHoursStart(time)
        }
    }

    fun updateQuietHoursEnd(time: String) {
        viewModelScope.launch {
            notificationIntegration.updateQuietHoursEnd(time)
        }
    }

    fun updateNotificationSound(sound: String) {
        viewModelScope.launch {
            notificationIntegration.updateNotificationSound(sound)
        }
    }

    fun toggleVibration() {
        viewModelScope.launch {
            notificationIntegration.toggleVibration()
        }
    }
}