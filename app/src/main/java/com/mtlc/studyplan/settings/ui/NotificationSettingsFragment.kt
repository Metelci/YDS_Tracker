package com.mtlc.studyplan.settings.ui

import android.app.TimePickerDialog
import android.os.Bundle
import androidx.fragment.app.viewModels
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.mtlc.studyplan.settings.data.*
import com.mtlc.studyplan.settings.viewmodel.NotificationSettingsViewModel
import com.mtlc.studyplan.settings.viewmodel.NotificationSettingsViewModelFactory
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*

/**
 * Fragment for notification settings with time picker and frequency selection
 */
class NotificationSettingsFragment : BaseSettingsFragment() {

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

    override fun getUiStateFlow(): Flow<*> = viewModel.uiState

    override fun extractSettingsFromUiState(uiState: Any): List<SettingItem> {
        return when (uiState) {
            is NotificationSettingsViewModel.NotificationUiState -> uiState.settings
            else -> emptyList()
        }
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
            "email_summaries" -> {
                if (newValue is Boolean) {
                    viewModel.updateEmailSummaries(newValue)
                }
            }
            "email_frequency" -> {
                if (newValue is String) {
                    val frequency = EmailFrequency.valueOf(newValue)
                    viewModel.updateEmailFrequency(frequency)
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
            "email_summaries" -> {
                val message = if (newValue == true) {
                    "Email summaries enabled. You'll receive weekly progress reports."
                } else {
                    "Email summaries disabled."
                }
                showSettingFeedback(message)
            }
        }
    }

    override fun onRefreshRequested() {
        viewModel.refresh()
    }

    override fun onRetryRequested() {
        viewModel.retry()
    }

    override fun getFragmentTitle(): String = "Notification Settings"

    override fun isImportantSetting(setting: SettingItem): Boolean {
        return when (setting.id) {
            "push_notifications",
            "study_reminders",
            "email_summaries" -> true
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
            "email_summaries" -> if (newValue == true) {
                "Email summaries enabled"
            } else {
                "Email summaries disabled"
            }
            else -> "${setting.title} updated"
        }
    }

    /**
     * Show time picker for study reminder time
     */
    fun showTimePicker(currentTime: TimeValue, onTimeSelected: (TimeValue) -> Unit) {
        val timePicker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_12H)
            .setHour(currentTime.hour)
            .setMinute(currentTime.minute)
            .setTitleText("Select reminder time")
            .build()

        timePicker.addOnPositiveButtonClickListener {
            val newTime = TimeValue(timePicker.hour, timePicker.minute)
            onTimeSelected(newTime)

            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, newTime.hour)
                set(Calendar.MINUTE, newTime.minute)
            }
            val timeString = timeFormat.format(calendar.time)

            showSettingFeedback("Reminder time set to $timeString")
        }

        timePicker.show(parentFragmentManager, "time_picker")
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

/**
 * Data classes for notification settings
 */
data class TimeValue(
    val hour: Int,
    val minute: Int
) {
    fun formatTime(): String {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        val format = SimpleDateFormat("h:mm a", Locale.getDefault())
        return format.format(calendar.time)
    }
}

enum class EmailFrequency {
    DAILY, WEEKLY, MONTHLY, NEVER
}

/**
 * Custom setting type for time selection
 */
data class TimeSetting(
    override val id: String,
    override val title: String,
    override val description: String,
    val currentTime: TimeValue,
    override val isEnabled: Boolean,
    override val category: String,
    override val sortOrder: Int
) : SettingItem