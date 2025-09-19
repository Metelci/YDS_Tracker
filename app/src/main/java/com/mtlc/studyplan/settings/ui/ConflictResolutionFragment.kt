package com.mtlc.studyplan.settings.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mtlc.studyplan.R
import com.mtlc.studyplan.databinding.FragmentConflictResolutionBinding
import com.mtlc.studyplan.settings.backup.SettingsBackupManager
import com.mtlc.studyplan.settings.viewmodel.ConflictResolutionViewModel
import com.mtlc.studyplan.settings.viewmodel.ConflictResolutionViewModelFactory
import kotlinx.coroutines.launch

/**
 * Fragment for resolving setting conflicts during import
 */
class ConflictResolutionFragment : Fragment() {

    private var _binding: FragmentConflictResolutionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ConflictResolutionViewModel by viewModels {
        ConflictResolutionViewModelFactory(requireContext())
    }

    private lateinit var conflictsAdapter: ConflictResolutionAdapter
    private var onResolutionComplete: ((List<ResolvedConflict>) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConflictResolutionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupButtons()
        observeViewModel()

        // Load conflicts from arguments
        arguments?.let { args ->
            val conflicts = args.getParcelableArrayList<SettingsBackupManager.SettingConflict>("conflicts")
            conflicts?.let { viewModel.loadConflicts(it) }
        }
    }

    private fun setupRecyclerView() {
        conflictsAdapter = ConflictResolutionAdapter { conflict, resolution ->
            viewModel.resolveConflict(conflict.settingId, resolution)
        }

        binding.conflictsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = conflictsAdapter
        }
    }

    private fun setupButtons() {
        // Apply All Current Values
        binding.applyCurrentButton.setOnClickListener {
            showConfirmationDialog(
                title = getString(R.string.apply_current_values_title),
                message = getString(R.string.apply_current_values_message),
                action = { viewModel.applyAllCurrent() }
            )
        }

        // Apply All Backup Values
        binding.applyBackupButton.setOnClickListener {
            showConfirmationDialog(
                title = getString(R.string.apply_backup_values_title),
                message = getString(R.string.apply_backup_values_message),
                action = { viewModel.applyAllBackup() }
            )
        }

        // Smart Merge (newer wins)
        binding.smartMergeButton.setOnClickListener {
            showConfirmationDialog(
                title = getString(R.string.smart_merge_title),
                message = getString(R.string.smart_merge_message),
                action = { viewModel.applySmartMerge() }
            )
        }

        // Apply Selected Resolutions
        binding.applySelectedButton.setOnClickListener {
            viewModel.applySelectedResolutions()
        }

        // Cancel
        binding.cancelButton.setOnClickListener {
            requireActivity().onBackPressed()
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

    private fun updateUI(state: ConflictResolutionViewModel.ConflictResolutionUiState) {
        // Update conflicts list
        conflictsAdapter.submitList(state.conflicts)

        // Update button states
        binding.applySelectedButton.isEnabled = state.hasResolutions && !state.isProcessing
        binding.applyCurrentButton.isEnabled = !state.isProcessing
        binding.applyBackupButton.isEnabled = !state.isProcessing
        binding.smartMergeButton.isEnabled = !state.isProcessing

        // Update progress
        binding.progressBar.visibility = if (state.isProcessing) View.VISIBLE else View.GONE

        // Update stats
        binding.conflictStats.text = getString(
            R.string.conflict_stats_format,
            state.resolvedCount,
            state.totalCount
        )

        // Update resolution summary
        if (state.hasResolutions) {
            val summary = state.resolutionSummary
            binding.resolutionSummary.apply {
                visibility = View.VISIBLE
                text = getString(
                    R.string.resolution_summary_format,
                    summary.keepCurrent,
                    summary.useBackup,
                    summary.manualReview
                )
            }
        } else {
            binding.resolutionSummary.visibility = View.GONE
        }
    }

    private fun handleEvent(event: ConflictResolutionViewModel.ConflictResolutionEvent) {
        when (event) {
            is ConflictResolutionViewModel.ConflictResolutionEvent.ResolutionComplete -> {
                onResolutionComplete?.invoke(event.resolutions)
                requireActivity().onBackPressed()
            }
            is ConflictResolutionViewModel.ConflictResolutionEvent.Error -> {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.error_title)
                    .setMessage(event.message)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            }
        }
    }

    private fun showConfirmationDialog(
        title: String,
        message: String,
        action: () -> Unit
    ) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.apply) { _, _ -> action() }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    fun setOnResolutionCompleteListener(listener: (List<ResolvedConflict>) -> Unit) {
        onResolutionComplete = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(conflicts: List<SettingsBackupManager.SettingConflict>): ConflictResolutionFragment {
            return ConflictResolutionFragment().apply {
                arguments = Bundle().apply {
                    putParcelableArrayList("conflicts", ArrayList(conflicts))
                }
            }
        }
    }

    /**
     * Resolved conflict data class
     */
    data class ResolvedConflict(
        val settingId: String,
        val resolution: SettingsBackupManager.ConflictResolution,
        val finalValue: Any?,
        val timestamp: Long = System.currentTimeMillis()
    )
}