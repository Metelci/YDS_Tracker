package com.mtlc.studyplan.settings.ui

import android.os.Bundle
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.mtlc.studyplan.R
import com.mtlc.studyplan.settings.data.*
import com.mtlc.studyplan.settings.viewmodel.PrivacySettingsViewModel
import com.mtlc.studyplan.settings.viewmodel.PrivacySettingsViewModelFactory
import kotlinx.coroutines.flow.Flow

/**
 * Fragment for privacy settings with comprehensive privacy controls
 */
class PrivacySettingsFragment : BaseSettingsFragment() {

    private val viewModel: PrivacySettingsViewModel by viewModels {
        PrivacySettingsViewModelFactory(
            SettingsRepository(requireContext()),
            requireContext()
        )
    }

    companion object {
        fun newInstance() = PrivacySettingsFragment()
    }

    override fun createAdapter(): BaseSettingsAdapter {
        return PrivacySettingsAdapter { setting, value ->
            handleSettingChange(setting, value)
        }
    }

    override fun getUiStateFlow(): Flow<*> = viewModel.uiState

    override fun extractSettingsFromUiState(uiState: Any): List<SettingItem> {
        return when (uiState) {
            is PrivacySettingsViewModel.PrivacyUiState -> uiState.settings
            else -> emptyList()
        }
    }

    override fun getCurrentSettingValue(setting: SettingItem): Any? {
        return when (setting) {
            is ToggleSetting -> setting.value
            is SelectionSetting<*> -> setting.currentValue
            is ActionSetting -> Unit
            else -> null
        }
    }

    override fun applySettingChange(setting: SettingItem, newValue: Any?) {
        when (setting.id) {
            "profile_visibility_enabled" -> {
                if (newValue is Boolean) {
                    viewModel.updateProfileVisibilityEnabled(newValue)
                }
            }
            "profile_visibility_level" -> {
                if (newValue is String) {
                    val visibilityLevel = ProfileVisibilityLevel.valueOf(newValue)
                    viewModel.updateProfileVisibilityLevel(visibilityLevel)
                }
            }
            "anonymous_analytics" -> {
                if (newValue is Boolean) {
                    viewModel.updateAnonymousAnalytics(newValue)
                }
            }
            "progress_sharing" -> {
                if (newValue is Boolean) {
                    viewModel.updateProgressSharing(newValue)
                }
            }
            "data_export" -> {
                handleDataExport()
            }
            "clear_personal_data" -> {
                handleClearPersonalData()
            }
        }
    }

    override fun persistSettingChange(setting: SettingItem, newValue: Any?) {
        // Changes are automatically persisted through ViewModel
        // Show confirmation for important changes
        when (setting.id) {
            "anonymous_analytics" -> {
                val message = if (newValue == true) {
                    "Anonymous analytics enabled. Thank you for helping improve the app!"
                } else {
                    "Anonymous analytics disabled. Your privacy is respected."
                }
                showSettingFeedback(message)
            }
            "progress_sharing" -> {
                val message = if (newValue == true) {
                    "Progress sharing enabled. Others can see your achievements."
                } else {
                    "Progress sharing disabled. Your progress is now private."
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

    override fun getFragmentTitle(): String = "Privacy Settings"

    override fun isImportantSetting(setting: SettingItem): Boolean {
        return when (setting.id) {
            "anonymous_analytics",
            "progress_sharing",
            "profile_visibility_enabled" -> true
            else -> false
        }
    }

    override fun getSettingChangeMessage(setting: SettingItem, newValue: Any?): String {
        return when (setting.id) {
            "anonymous_analytics" -> if (newValue == true) {
                "Analytics enabled"
            } else {
                "Analytics disabled"
            }
            "progress_sharing" -> if (newValue == true) {
                "Progress sharing enabled"
            } else {
                "Progress sharing disabled"
            }
            "profile_visibility_enabled" -> if (newValue == true) {
                "Profile visibility enabled"
            } else {
                "Profile visibility disabled"
            }
            else -> "${setting.title} updated"
        }
    }

    /**
     * Handle data export action
     */
    private fun handleDataExport() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Export Personal Data")
            .setMessage("This will create a file containing all your personal data stored in the app. This may take a few moments.")
            .setPositiveButton("Export") { _, _ ->
                viewModel.exportPersonalData()
                showDataExportProgress()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Handle clear personal data action
     */
    private fun handleClearPersonalData() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Clear Personal Data")
            .setMessage("This will permanently delete all your personal data, including progress, achievements, and settings. This action cannot be undone.")
            .setPositiveButton("Clear Data") { _, _ ->
                showClearDataConfirmation()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Show final confirmation for clearing data
     */
    private fun showClearDataConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Final Confirmation")
            .setMessage("Are you absolutely sure? This will delete everything and cannot be undone.")
            .setPositiveButton("Yes, Delete Everything") { _, _ ->
                viewModel.clearAllPersonalData()
                showDataClearProgress()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Show data export progress
     */
    private fun showDataExportProgress() {
        val snackbar = Snackbar.make(
            binding.root,
            "Exporting data...",
            Snackbar.LENGTH_INDEFINITE
        )
        snackbar.show()

        // Hide snackbar when export completes (handled by ViewModel observer)
        viewModel.dataExportComplete.observe(viewLifecycleOwner) { completed ->
            if (completed) {
                snackbar.dismiss()
                Snackbar.make(
                    binding.root,
                    "Data export completed successfully",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * Show data clear progress
     */
    private fun showDataClearProgress() {
        val snackbar = Snackbar.make(
            binding.root,
            "Clearing data...",
            Snackbar.LENGTH_INDEFINITE
        )
        snackbar.show()

        // Navigate back or refresh when clear completes
        viewModel.dataClearComplete.observe(viewLifecycleOwner) { completed ->
            if (completed) {
                snackbar.dismiss()
                Snackbar.make(
                    binding.root,
                    "All personal data has been cleared",
                    Snackbar.LENGTH_LONG
                ).show()

                // Optionally navigate back to main settings
                parentFragmentManager.popBackStack()
            }
        }
    }

    /**
     * Show feedback for setting changes
     */
    private fun showSettingFeedback(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
            .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
            .show()
    }

    override fun getEmptyStateTitle(): String = "No Privacy Settings"

    override fun getEmptyStateMessage(): String =
        "Privacy settings are not available at the moment. Please try again later."
}