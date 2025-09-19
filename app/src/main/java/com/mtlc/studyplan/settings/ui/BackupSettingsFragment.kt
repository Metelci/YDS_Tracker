package com.mtlc.studyplan.settings.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.mtlc.studyplan.R
import com.mtlc.studyplan.databinding.FragmentBackupSettingsBinding
import com.mtlc.studyplan.settings.backup.SettingsBackupManager
import com.mtlc.studyplan.settings.viewmodel.BackupSettingsViewModel
import com.mtlc.studyplan.settings.viewmodel.BackupSettingsViewModelFactory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Fragment for settings backup and sync management
 */
class BackupSettingsFragment : Fragment() {

    private var _binding: FragmentBackupSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BackupSettingsViewModel by viewModels {
        BackupSettingsViewModelFactory(requireContext())
    }

    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        uri?.let { viewModel.exportSettings(it) }
    }

    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            showImportStrategyDialog(it)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBackupSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        // Export settings button
        binding.exportSettingsButton.setOnClickListener {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "studyplan_backup_$timestamp.studyplan"
            exportLauncher.launch(fileName)
        }

        // Import settings button
        binding.importSettingsButton.setOnClickListener {
            importLauncher.launch(arrayOf("application/octet-stream", "*/*"))
        }

        // Cloud sync setup button
        binding.setupCloudSyncButton.setOnClickListener {
            showCloudSyncProviderDialog()
        }

        // Enable auto sync toggle
        binding.autoSyncSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.toggleAutoSync(isChecked)
        }

        // Manual sync button
        binding.manualSyncButton.setOnClickListener {
            viewModel.performManualSync()
        }

        // Clear backup history button
        binding.clearBackupHistoryButton.setOnClickListener {
            showClearHistoryConfirmation()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                updateUI(state)
            }
        }

        lifecycleScope.launch {
            viewModel.events.collect { event ->
                handleEvent(event)
            }
        }
    }

    private fun updateUI(state: BackupSettingsViewModel.BackupUiState) {
        // Update backup status
        binding.lastBackupDate.text = state.lastBackupDate?.let { date ->
            getString(R.string.last_backup_date, SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(date))
        } ?: getString(R.string.no_backup_yet)

        // Update export progress
        binding.exportProgressBar.apply {
            visibility = if (state.isExporting) View.VISIBLE else View.GONE
            progress = (state.exportProgress * 100).toInt()
        }

        // Update import progress
        binding.importProgressBar.apply {
            visibility = if (state.isImporting) View.VISIBLE else View.GONE
            progress = (state.importProgress * 100).toInt()
        }

        // Update cloud sync status
        binding.cloudSyncStatus.text = when {
            state.isCloudSyncConfigured && state.isCloudSyncEnabled -> getString(R.string.cloud_sync_enabled)
            state.isCloudSyncConfigured -> getString(R.string.cloud_sync_configured)
            else -> getString(R.string.cloud_sync_not_configured)
        }

        binding.lastSyncDate.text = state.lastSyncDate?.let { date ->
            getString(R.string.last_sync_date, SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(date))
        } ?: getString(R.string.never_synced)

        // Update sync progress
        binding.syncProgressBar.apply {
            visibility = if (state.isSyncing) View.VISIBLE else View.GONE
            progress = (state.syncProgress * 100).toInt()
        }

        // Update buttons state
        binding.exportSettingsButton.isEnabled = !state.isExporting && !state.isImporting
        binding.importSettingsButton.isEnabled = !state.isExporting && !state.isImporting
        binding.manualSyncButton.isEnabled = state.isCloudSyncConfigured && !state.isSyncing
        binding.autoSyncSwitch.isChecked = state.isCloudSyncEnabled

        // Update backup size
        binding.backupSize.text = if (state.backupSize > 0) {
            getString(R.string.backup_size_format, formatFileSize(state.backupSize))
        } else {
            getString(R.string.no_backup_size)
        }

        // Handle errors
        state.error?.let { error ->
            showError(error.message)
        }
    }

    private fun handleEvent(event: BackupSettingsViewModel.BackupEvent) {
        when (event) {
            is BackupSettingsViewModel.BackupEvent.ExportSuccess -> {
                showSuccess(getString(R.string.export_success))
            }
            is BackupSettingsViewModel.BackupEvent.ImportSuccess -> {
                val message = getString(R.string.import_success_format,
                    event.result.totalSettings,
                    event.result.conflicts.size)
                showSuccess(message)
            }
            is BackupSettingsViewModel.BackupEvent.SyncSuccess -> {
                val message = getString(R.string.sync_success_format,
                    event.result.totalSettings,
                    event.result.conflictsResolved)
                showSuccess(message)
            }
            is BackupSettingsViewModel.BackupEvent.ImportConflicts -> {
                showImportConflictsDialog(event.conflicts)
            }
        }
    }

    private fun showImportStrategyDialog(uri: Uri) {
        val strategies = arrayOf(
            getString(R.string.merge_strategy_replace),
            getString(R.string.merge_strategy_merge),
            getString(R.string.merge_strategy_skip_existing)
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.import_strategy_title)
            .setItems(strategies) { _, which ->
                val strategy = when (which) {
                    0 -> SettingsBackupManager.MergeStrategy.REPLACE
                    1 -> SettingsBackupManager.MergeStrategy.MERGE
                    2 -> SettingsBackupManager.MergeStrategy.SKIP_EXISTING
                    else -> SettingsBackupManager.MergeStrategy.REPLACE
                }
                viewModel.importSettings(uri, strategy)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showCloudSyncProviderDialog() {
        val providers = arrayOf(
            getString(R.string.google_drive),
            getString(R.string.dropbox),
            getString(R.string.icloud)
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.choose_sync_provider)
            .setItems(providers) { _, which ->
                // TODO: Implement provider configuration
                showProviderConfigurationInProgress()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showImportConflictsDialog(conflicts: List<SettingsBackupManager.SettingConflict>) {
        val message = getString(R.string.import_conflicts_message, conflicts.size)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.import_conflicts_title)
            .setMessage(message)
            .setPositiveButton(R.string.review_conflicts) { _, _ ->
                // TODO: Show detailed conflicts screen
            }
            .setNegativeButton(R.string.apply_defaults) { _, _ ->
                viewModel.resolveConflictsWithDefaults()
            }
            .show()
    }

    private fun showClearHistoryConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.clear_backup_history_title)
            .setMessage(R.string.clear_backup_history_message)
            .setPositiveButton(R.string.clear) { _, _ ->
                viewModel.clearBackupHistory()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showProviderConfigurationInProgress() {
        Snackbar.make(binding.root, R.string.provider_configuration_in_progress, Snackbar.LENGTH_LONG)
            .show()
    }

    private fun showSuccess(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(resources.getColor(R.color.success_color, null))
            .show()
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(resources.getColor(R.color.error_color, null))
            .show()
    }

    private fun formatFileSize(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB")
        var size = bytes.toDouble()
        var unitIndex = 0

        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }

        return String.format("%.1f %s", size, units[unitIndex])
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): BackupSettingsFragment {
            return BackupSettingsFragment()
        }
    }
}