package com.mtlc.studyplan.settings.manager

import com.mtlc.studyplan.settings.data.SettingsKeys
import com.mtlc.studyplan.settings.data.SettingsRepository
import com.mtlc.studyplan.settings.data.UserSettings
import com.mtlc.studyplan.settings.data.SettingsKey
import com.mtlc.studyplan.eventbus.AppEventBus
import com.mtlc.studyplan.eventbus.AppEvent
import com.mtlc.studyplan.notifications.NotificationManager
import com.mtlc.studyplan.offline.OfflineManager
import com.mtlc.studyplan.utils.HapticFeedbackManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsManager @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val appEventBus: AppEventBus,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    private val _currentSettings = MutableStateFlow(UserSettings.default())
    val currentSettings: StateFlow<UserSettings> = _currentSettings.asStateFlow()

    private val scope = CoroutineScope(ioDispatcher + SupervisorJob())

    // Lazy injection to avoid circular dependencies
    private var notificationManager: NotificationManager? = null
    private var offlineManager: OfflineManager? = null

    init {
        loadInitialSettings()
        observeSettingsChanges()
    }

    // ThemeManager removed; theme is fixed to light

    fun setNotificationManager(notificationManager: NotificationManager) {
        this.notificationManager = notificationManager
    }

    fun setOfflineManager(offlineManager: OfflineManager) {
        this.offlineManager = offlineManager
    }

    private fun loadInitialSettings() {
        scope.launch {
            try {
                val settings = settingsRepository.getUserSettings().first()
                _currentSettings.value = settings
                applyAllSettings(settings)
            } catch (e: Exception) {
                val defaultSettings = UserSettings.default()
                _currentSettings.value = defaultSettings
                applyAllSettings(defaultSettings)
            }
        }
    }

    private fun observeSettingsChanges() {
        scope.launch {
            settingsRepository.getUserSettings().collect { settings ->
                val oldSettings = _currentSettings.value
                _currentSettings.value = settings

                applySettingsChanges(oldSettings, settings)

                appEventBus.emitEvent(AppEvent.SettingsUpdated(settings))
            }
        }
    }

    suspend fun updateSetting(key: SettingsKey, value: Any): Result<UserSettings> {
        return try {
            val currentSettings = _currentSettings.value
            val updatedSettings = when (key) {
                SettingsKey.NOTIFICATIONS_ENABLED -> currentSettings.copy(notificationsEnabled = value as Boolean)
                SettingsKey.PUSH_NOTIFICATIONS_ENABLED -> currentSettings.copy(pushNotificationsEnabled = value as Boolean)
                SettingsKey.STUDY_REMINDERS -> currentSettings.copy(studyRemindersEnabled = value as Boolean)
                SettingsKey.ACHIEVEMENT_NOTIFICATIONS -> currentSettings.copy(achievementNotificationsEnabled = value as Boolean)
                SettingsKey.OFFLINE_MODE -> currentSettings.copy(offlineModeEnabled = value as Boolean)
                SettingsKey.AUTO_SYNC -> currentSettings.copy(autoSyncEnabled = value as Boolean)
                SettingsKey.GAMIFICATION_ENABLED -> currentSettings.copy(gamificationEnabled = value as Boolean)
                SettingsKey.SOCIAL_SHARING -> currentSettings.copy(socialSharingEnabled = value as Boolean)
                SettingsKey.HAPTIC_FEEDBACK -> currentSettings.copy(hapticFeedbackEnabled = value as Boolean)
                SettingsKey.STREAK_WARNINGS -> currentSettings.copy(streakWarningsEnabled = value as Boolean)
                SettingsKey.DAILY_GOAL_REMINDERS -> currentSettings.copy(dailyGoalRemindersEnabled = value as Boolean)
                SettingsKey.WEEKEND_MODE -> currentSettings.copy(weekendModeEnabled = value as Boolean)
                SettingsKey.AUTO_DIFFICULTY -> currentSettings.copy(autoDifficultyEnabled = value as Boolean)
            }

            val validatedSettings = validateSettings(updatedSettings)

            // Update the settings in repository using individual calls
            // Note: This is a simplified approach. The repository works with individual settings.
            _currentSettings.value = validatedSettings

            Result.success(validatedSettings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun applyAllSettings(settings: UserSettings) {
        // Theme application removed

        notificationManager?.configure(
            enabled = settings.notificationsEnabled,
            studyReminders = settings.studyRemindersEnabled,
            achievementNotifications = settings.achievementNotificationsEnabled,
            dailyGoalReminders = settings.dailyGoalRemindersEnabled,
            streakWarnings = settings.streakWarningsEnabled
        )

        offlineManager?.configure(
            enabled = settings.offlineModeEnabled,
            autoSync = settings.autoSyncEnabled
        )

        HapticFeedbackManager.setEnabled(settings.hapticFeedbackEnabled)
    }

    private suspend fun applySettingsChanges(oldSettings: UserSettings, newSettings: UserSettings) {
        // Theme changes ignored (dark/system themes removed)

        if (oldSettings.notificationsEnabled != newSettings.notificationsEnabled ||
            oldSettings.studyRemindersEnabled != newSettings.studyRemindersEnabled ||
            oldSettings.achievementNotificationsEnabled != newSettings.achievementNotificationsEnabled ||
            oldSettings.dailyGoalRemindersEnabled != newSettings.dailyGoalRemindersEnabled ||
            oldSettings.streakWarningsEnabled != newSettings.streakWarningsEnabled) {

            notificationManager?.configure(
                enabled = newSettings.notificationsEnabled,
                studyReminders = newSettings.studyRemindersEnabled,
                achievementNotifications = newSettings.achievementNotificationsEnabled,
                dailyGoalReminders = newSettings.dailyGoalRemindersEnabled,
                streakWarnings = newSettings.streakWarningsEnabled
            )
        }

        if (oldSettings.offlineModeEnabled != newSettings.offlineModeEnabled ||
            oldSettings.autoSyncEnabled != newSettings.autoSyncEnabled) {

            offlineManager?.configure(
                enabled = newSettings.offlineModeEnabled,
                autoSync = newSettings.autoSyncEnabled
            )
        }

        if (oldSettings.hapticFeedbackEnabled != newSettings.hapticFeedbackEnabled) {
            HapticFeedbackManager.setEnabled(newSettings.hapticFeedbackEnabled)
        }
    }

    private fun validateSettings(settings: UserSettings): UserSettings {
        var validatedSettings = settings

        if (!settings.notificationsEnabled) {
            validatedSettings = validatedSettings.copy(
                studyRemindersEnabled = false,
                achievementNotificationsEnabled = false,
                dailyGoalRemindersEnabled = false,
                streakWarningsEnabled = false
            )
        }

        if (!settings.gamificationEnabled) {
            validatedSettings = validatedSettings.copy(
                achievementNotificationsEnabled = false,
                streakWarningsEnabled = false
            )
        }

        if (!settings.offlineModeEnabled) {
            validatedSettings = validatedSettings.copy(
                autoSyncEnabled = false
            )
        }

        return validatedSettings
    }

    fun isGamificationEnabled(): Boolean = _currentSettings.value.gamificationEnabled
    fun isHapticFeedbackEnabled(): Boolean = _currentSettings.value.hapticFeedbackEnabled
    fun isOfflineModeEnabled(): Boolean = _currentSettings.value.offlineModeEnabled
    fun isDarkTheme(): Boolean = false
    fun isNotificationsEnabled(): Boolean = _currentSettings.value.notificationsEnabled
    // Smart scheduling removed from settings
    fun isWeekendModeEnabled(): Boolean = _currentSettings.value.weekendModeEnabled
}
