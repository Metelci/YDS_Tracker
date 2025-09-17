package com.mtlc.studyplan.settings.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.transition.MaterialContainerTransform
import com.mtlc.studyplan.R
import com.mtlc.studyplan.databinding.FragmentSettingsDetailBinding
import com.mtlc.studyplan.settings.data.SettingsRepository
import com.mtlc.studyplan.settings.viewmodel.SettingsDetailViewModel
import com.mtlc.studyplan.settings.viewmodel.SettingsDetailViewModelFactory
import com.mtlc.studyplan.ui.components.ErrorCard
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Fragment displaying detailed settings for a specific category
 */
class SettingsDetailFragment : Fragment() {

    private var _binding: FragmentSettingsDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var settingsAdapter: SettingsDetailAdapter
    private lateinit var repository: SettingsRepository

    private val viewModel: SettingsDetailViewModel by viewModels {
        SettingsDetailViewModelFactory(repository, requireContext())
    }

    companion object {
        private const val ARG_CATEGORY_ID = "category_id"

        fun newInstance(categoryId: String): SettingsDetailFragment {
            return SettingsDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CATEGORY_ID, categoryId)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set up shared element transition
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            drawingViewId = R.id.fragment_container
            duration = 300L
            scrimColor = android.graphics.Color.TRANSPARENT
        }

        repository = SettingsRepository(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()

        val categoryId = arguments?.getString(ARG_CATEGORY_ID)
        if (categoryId != null) {
            viewModel.loadCategorySettings(categoryId)
        }
    }

    private fun setupRecyclerView() {
        settingsAdapter = SettingsDetailAdapter { settingItem ->
            // Handle setting item click
            viewModel.handleSettingClick(settingItem)
        }

        binding.settingsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = settingsAdapter
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { uiState ->
                when {
                    uiState.isLoading -> showLoading()
                    uiState.isError -> showError(uiState.error!!)
                    uiState.isSuccess -> showSuccess(uiState)
                }
            }
        }
    }

    private fun showLoading() {
        binding.apply {
            progressIndicator.visibility = View.VISIBLE
            settingsRecyclerView.visibility = View.GONE
            errorContainer.visibility = View.GONE
        }
    }

    private fun showError(error: com.mtlc.studyplan.core.error.AppError) {
        binding.apply {
            progressIndicator.visibility = View.GONE
            settingsRecyclerView.visibility = View.GONE
            errorContainer.visibility = View.VISIBLE
        }

        // Show error card
        val errorCard = ErrorCard(
            error = error,
            onRetry = { viewModel.retry() },
            onDismiss = { binding.errorContainer.visibility = View.GONE }
        )

        binding.errorContainer.removeAllViews()
        binding.errorContainer.addView(errorCard)
    }

    private fun showSuccess(uiState: SettingsDetailViewModel.SettingsDetailUiState) {
        binding.apply {
            progressIndicator.visibility = View.GONE
            errorContainer.visibility = View.GONE
            settingsRecyclerView.visibility = View.VISIBLE
        }

        settingsAdapter.updateSettings(uiState.sections)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::repository.isInitialized) {
            repository.dispose()
        }
    }
}