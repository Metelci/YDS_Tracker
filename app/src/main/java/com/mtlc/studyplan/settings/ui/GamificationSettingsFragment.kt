package com.mtlc.studyplan.settings.ui

import android.os.Bundle
import androidx.fragment.app.viewModels
import com.google.android.material.snackbar.Snackbar
import com.mtlc.studyplan.settings.data.*
import com.mtlc.studyplan.settings.viewmodel.GamificationSettingsViewModel
import com.mtlc.studyplan.settings.viewmodel.GamificationSettingsViewModelFactory
import kotlinx.coroutines.flow.Flow

/**
 * Fragment for gamification settings with streak tracking and rewards
 */
class GamificationSettingsFragment : BaseSettingsFragment<GamificationSettingsViewModel.GamificationUiState>() {

    private val viewModel: GamificationSettingsViewModel by viewModels {
        GamificationSettingsViewModelFactory(
            SettingsRepository(requireContext()),
            requireContext()
        )
    }

    companion object {
        fun newInstance() = GamificationSettingsFragment()
    }

    override fun createAdapter(): BaseSettingsAdapter {
        return GamificationSettingsAdapter { setting, value ->
            handleSettingChange(setting, value)
        }
    }

    override fun getUiStateFlow(): Flow<GamificationSettingsViewModel.GamificationUiState> = viewModel.uiState

    override fun extractSettingsFromUiState(uiState: GamificationSettingsViewModel.GamificationUiState): List<SettingItem> {
        return uiState.settings
    }

    override fun getCurrentSettingValue(setting: SettingItem): Any? {
        return when (setting) {
            is ToggleSetting -> setting.value
            is ActionSetting -> Unit
            else -> null
        }
    }

    override fun applySettingChange(setting: SettingItem, newValue: Any?) {
        when (setting.id) {
            "streak_tracking" -> {
                if (newValue is Boolean) {
                    viewModel.updateStreakTracking(newValue)
                }
            }
            "points_rewards" -> {
                if (newValue is Boolean) {
                    viewModel.updatePointsRewards(newValue)
                }
            }
            "celebration_effects" -> {
                if (newValue is Boolean) {
                    viewModel.updateCelebrationEffects(newValue)
                }
            }
            "streak_risk_warnings" -> {
                if (newValue is Boolean) {
                    viewModel.updateStreakRiskWarnings(newValue)
                }
            }
            "reset_streak" -> {
                handleResetStreak()
            }
            "reset_points" -> {
                handleResetPoints()
            }
        }
    }

    override fun persistSettingChange(setting: SettingItem, newValue: Any?) {
        // Changes are automatically persisted through ViewModel
        when (setting.id) {
            "streak_tracking" -> {
                val message = if (newValue == true) {
                    "Streak tracking enabled. Your study streak will be tracked daily."
                } else {
                    "Streak tracking disabled. Your current streak will be preserved."
                }
                showSettingFeedback(message)
            }
            "points_rewards" -> {
                val message = if (newValue == true) {
                    "Points & rewards enabled. You'll earn points for completed tasks."
                } else {
                    "Points & rewards disabled. Your current points will be preserved."
                }
                showSettingFeedback(message)
            }
            "celebration_effects" -> {
                val message = if (newValue == true) {
                    "Celebration effects enabled. Enjoy confetti and animations!"
                } else {
                    "Celebration effects disabled. Achievements will be shown quietly."
                }
                showSettingFeedback(message)
            }
            "streak_risk_warnings" -> {
                val message = if (newValue == true) {
                    "Streak risk warnings enabled. You'll be notified when your streak is at risk."
                } else {
                    "Streak risk warnings disabled."
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

    override fun getFragmentTitle(): String = "Gamification Settings"

    override fun isImportantSetting(setting: SettingItem): Boolean {
        return when (setting.id) {
            "streak_tracking",
            "points_rewards" -> true
            else -> false
        }
    }

    override fun getSettingChangeMessage(setting: SettingItem, newValue: Any?): String {
        return when (setting.id) {
            "streak_tracking" -> if (newValue == true) {
                "Streak tracking enabled"
            } else {
                "Streak tracking disabled"
            }
            "points_rewards" -> if (newValue == true) {
                "Points & rewards enabled"
            } else {
                "Points & rewards disabled"
            }
            "celebration_effects" -> if (newValue == true) {
                "Celebration effects enabled"
            } else {
                "Celebration effects disabled"
            }
            "streak_risk_warnings" -> if (newValue == true) {
                "Streak warnings enabled"
            } else {
                "Streak warnings disabled"
            }
            else -> "${setting.title} updated"
        }
    }

    /**
     * Handle reset streak action
     */
    private fun handleResetStreak() {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Reset Streak")
            .setMessage("This will reset your current study streak to 0. This action cannot be undone.")
            .setPositiveButton("Reset") { _, _ ->
                viewModel.resetStreak()
                showSettingFeedback("Study streak has been reset")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Handle reset points action
     */
    private fun handleResetPoints() {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Reset Points")
            .setMessage("This will reset all your earned points to 0. This action cannot be undone.")
            .setPositiveButton("Reset") { _, _ ->
                viewModel.resetPoints()
                showSettingFeedback("All points have been reset")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Show feedback for setting changes
     */
    private fun showSettingFeedback(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
            .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
            .show()
    }

    override fun getEmptyStateTitle(): String = "No Gamification Settings"

    override fun getEmptyStateMessage(): String =
        "Gamification settings are not available at the moment. Please try again later."
}
