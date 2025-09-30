package com.mtlc.studyplan.settings.ui

import androidx.fragment.app.viewModels
import com.google.android.material.snackbar.Snackbar
import com.mtlc.studyplan.settings.data.*
import com.mtlc.studyplan.settings.viewmodel.NotificationSettingsViewModel
import com.mtlc.studyplan.settings.viewmodel.NotificationSettingsViewModelFactory
import kotlinx.coroutines.flow.Flow

/**
 * Fragment for notification settings with time picker and frequency selection
 */
class NotificationSettingsFragment : BaseSettingsFragment<NotificationSettingsViewModel.NotificationUiState>() {

    private val viewModel: NotificationSettingsViewModel by viewModels {
        NotificationSettingsViewModelFactory(
            SettingsRepository(requireContext()),
            requireContext()
        )
    }

    companion object {
        fun newInstance() = NotificationSettingsFragment()
    }

    override fun createAdapter(): BaseSettingsAdapter {
        return NotificationSettingsAdapter { setting, value ->
            handleSettingChange(setting, value)
        }
    }

    override fun getUiStateFlow(): Flow<NotificationSettingsViewModel.NotificationUiState> = viewModel.uiState

    override fun extractSettingsFromUiState(uiState: NotificationSettingsViewModel.NotificationUiState): List<SettingItem> {
        return uiState.settings
    }

    override fun getCurrentSettingValue(setting: SettingItem): Any? {
        return when (setting) {
            is ToggleSetting -> setting.value
            is SelectionSetting<*> -> setting.currentValue
            is TimeSetting -> setting.currentTime
            else -> null
        }
    }

    override fun applySettingChange(setting: SettingItem, newValue: Any?) {
        when (setting.id) {
            "push_notifications" -> {
                if (newValue is Boolean) {
                    viewModel.updatePushNotifications(newValue)
                }
            }
            "study_reminders" -> {
                if (newValue is Boolean) {
                    viewModel.updateStudyReminders(newValue)
                }
            }
            "study_reminder_time" -> {
                if (newValue is TimeValue) {
                    viewModel.updateStudyReminderTime(newValue)
                }
            }
            "achievement_alerts" -> {
                if (newValue is Boolean) {
                    viewModel.updateAchievementAlerts(newValue)
                }
            }
            "notification_test" -> {
                handleTestNotification()
            }
        }
    }

    override fun persistSettingChange(setting: SettingItem, newValue: Any?) {
        // Changes are automatically persisted through ViewModel
        when (setting.id) {
            "push_notifications" -> {
                val message = if (newValue == true) {
                    "Push notifications enabled. You'll receive study reminders and alerts."
                } else {
                    "Push notifications disabled. You won't receive any notifications."
                }
                showSettingFeedback(message)
            }
            "study_reminders" -> {
                val message = if (newValue == true) {
                    "Study reminders enabled. We'll remind you to study daily."
                } else {
                    "Study reminders disabled."
                }
                showSettingFeedback(message)
            }
        }
    }
    override fun onRetryRequested() {
        viewModel.retry()
    }

    override fun getFragmentTitle(): String = "Notification Settings"

    override fun isImportantSetting(setting: SettingItem): Boolean {
        return when (setting.id) {
            "push_notifications",
            "study_reminders" -> true
            else -> false
        }
    }

    override fun getSettingChangeMessage(setting: SettingItem, newValue: Any?): String {
        return when (setting.id) {
            "push_notifications" -> if (newValue == true) {
                "Notifications enabled"
            } else {
                "Notifications disabled"
            }
            "study_reminders" -> if (newValue == true) {
                "Study reminders enabled"
            } else {
                "Study reminders disabled"
            }
            else -> "${setting.title} updated"
        }
    }

    /**
     * Handle test notification action
     */
    private fun handleTestNotification() {
        viewModel.sendTestNotification()
        showSettingFeedback("Test notification sent!")
    }

    /**
     * Show feedback for setting changes
     */
    private fun showSettingFeedback(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
            .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
            .show()
    }

    override fun getEmptyStateTitle(): String = "No Notification Settings"

    override fun getEmptyStateMessage(): String =
        "Notification settings are not available at the moment. Please try again later."
}

