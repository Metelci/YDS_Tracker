package com.mtlc.studyplan.settings.ui

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialContainerTransform
import com.mtlc.studyplan.R
import com.mtlc.studyplan.core.error.AppError
import com.mtlc.studyplan.databinding.FragmentBaseSettingsBinding
import com.mtlc.studyplan.settings.data.SettingItem
import com.mtlc.studyplan.ui.components.ErrorCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Base fragment providing common functionality for all settings fragments
 */
abstract class BaseSettingsFragment : Fragment() {

    private var _binding: FragmentBaseSettingsBinding? = null
    protected val binding get() = _binding!!

    protected lateinit var settingsAdapter: BaseSettingsAdapter
    private var loadingAnimator: ValueAnimator? = null
    private var pendingUndo: PendingUndoAction? = null

    data class PendingUndoAction(
        val setting: SettingItem,
        val oldValue: Any?,
        val newValue: Any?,
        val timestamp: Long
    )

    companion object {
        private const val UNDO_TIMEOUT_MS = 5000L
        private const val LOADING_ANIMATION_DURATION = 1000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set up shared element transition
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            drawingViewId = R.id.fragment_container
            duration = 300L
            scrimColor = android.graphics.Color.TRANSPARENT
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBaseSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSwipeRefresh()
        observeUiState()

        // Set fragment title
        getFragmentTitle()?.let { title ->
            binding.fragmentTitle.text = title
            binding.fragmentTitle.isVisible = true
        }
    }

    /**
     * Setup RecyclerView with common configuration
     */
    private fun setupRecyclerView() {
        settingsAdapter = createAdapter()

        binding.settingsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = settingsAdapter
            itemAnimator = createItemAnimator()

            // Add scroll listener for animations
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    handleScrollEffects(dy)
                }
            })
        }
    }

    /**
     * Setup swipe refresh functionality
     */
    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            refreshSettings()
        }
    }

    /**
     * Create custom item animator for smooth animations
     */
    private fun createItemAnimator(): RecyclerView.ItemAnimator {
        return object : androidx.recyclerview.widget.DefaultItemAnimator() {
            override fun getAddDuration(): Long = 300L
            override fun getRemoveDuration(): Long = 300L
            override fun getMoveDuration(): Long = 300L
            override fun getChangeDuration(): Long = 200L
        }
    }

    /**
     * Observe UI state changes from ViewModel
     */
    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            getUiStateFlow().collectLatest { uiState ->
                when {
                    uiState.isLoading -> showLoading()
                    uiState.isError -> showError(uiState.error!!)
                    uiState.isSuccess -> showSuccess(uiState)
                }
            }
        }
    }

    /**
     * Show loading state with animation
     */
    protected fun showLoading() {
        binding.apply {
            swipeRefresh.isRefreshing = false
            errorContainer.isVisible = false
            emptyStateContainer.isVisible = false
            settingsRecyclerView.isVisible = false
            loadingContainer.isVisible = true
        }

        // Animate loading indicator
        loadingAnimator?.cancel()
        loadingAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = LOADING_ANIMATION_DURATION
            interpolator = DecelerateInterpolator()
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE

            addUpdateListener { animation ->
                val alpha = 0.3f + (animation.animatedFraction * 0.7f)
                binding.loadingIndicator.alpha = alpha
            }

            start()
        }
    }

    /**
     * Show error state with retry option
     */
    protected fun showError(error: AppError) {
        binding.apply {
            loadingContainer.isVisible = false
            settingsRecyclerView.isVisible = false
            emptyStateContainer.isVisible = false
            errorContainer.isVisible = true
            swipeRefresh.isRefreshing = false
        }

        loadingAnimator?.cancel()

        // Show error card
        val errorCard = ErrorCard(
            error = error,
            onRetry = { retryLoading() },
            onDismiss = { binding.errorContainer.isVisible = false }
        )

        binding.errorContainer.removeAllViews()
        binding.errorContainer.addView(errorCard)
    }

    /**
     * Show success state with settings list
     */
    protected open fun showSuccess(uiState: Any) {
        binding.apply {
            loadingContainer.isVisible = false
            errorContainer.isVisible = false
            swipeRefresh.isRefreshing = false
        }

        loadingAnimator?.cancel()

        val settings = extractSettingsFromUiState(uiState)

        if (settings.isEmpty()) {
            showEmptyState()
        } else {
            binding.emptyStateContainer.isVisible = false
            binding.settingsRecyclerView.isVisible = true
            settingsAdapter.updateSettings(settings)
        }
    }

    /**
     * Show empty state
     */
    private fun showEmptyState() {
        binding.apply {
            settingsRecyclerView.isVisible = false
            emptyStateContainer.isVisible = true
            emptyStateTitle.text = getEmptyStateTitle()
            emptyStateMessage.text = getEmptyStateMessage()
        }
    }

    /**
     * Handle setting value changes with undo functionality
     */
    protected fun handleSettingChange(setting: SettingItem, newValue: Any?) {
        val oldValue = getCurrentSettingValue(setting)

        // Create undo action
        pendingUndo = PendingUndoAction(
            setting = setting,
            oldValue = oldValue,
            newValue = newValue,
            timestamp = System.currentTimeMillis()
        )

        // Apply change immediately for UI responsiveness
        applySettingChange(setting, newValue)

        // Show undo snackbar for important changes
        if (isImportantSetting(setting)) {
            showUndoSnackbar(setting, oldValue, newValue)
        }

        // Persist change
        persistSettingChange(setting, newValue)
    }

    /**
     * Show undo snackbar for important setting changes
     */
    private fun showUndoSnackbar(setting: SettingItem, oldValue: Any?, newValue: Any?) {
        val message = getSettingChangeMessage(setting, newValue)

        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction(R.string.undo) {
                undoSettingChange()
            }
            .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
            .show()

        // Auto-clear undo after timeout
        viewLifecycleOwner.lifecycleScope.launch {
            delay(UNDO_TIMEOUT_MS)
            pendingUndo = null
        }
    }

    /**
     * Undo the last setting change
     */
    private fun undoSettingChange() {
        pendingUndo?.let { undo ->
            applySettingChange(undo.setting, undo.oldValue)
            persistSettingChange(undo.setting, undo.oldValue)
            pendingUndo = null

            // Show confirmation
            Snackbar.make(binding.root, "Change undone", Snackbar.LENGTH_SHORT)
                .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                .show()
        }
    }

    /**
     * Handle scroll effects (fade toolbar, etc.)
     */
    private fun handleScrollEffects(dy: Int) {
        // Fade fragment title based on scroll
        if (binding.fragmentTitle.isVisible) {
            val scrollY = binding.settingsRecyclerView.computeVerticalScrollOffset()
            val alpha = 1f - (scrollY / 200f).coerceIn(0f, 1f)
            binding.fragmentTitle.alpha = alpha
        }
    }

    /**
     * Refresh settings data
     */
    private fun refreshSettings() {
        onRefreshRequested()
    }

    /**
     * Retry loading after error
     */
    private fun retryLoading() {
        onRetryRequested()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        loadingAnimator?.cancel()
        _binding = null
    }

    // Abstract methods to be implemented by subclasses
    protected abstract fun createAdapter(): BaseSettingsAdapter
    protected abstract fun getUiStateFlow(): Flow<*>
    protected abstract fun extractSettingsFromUiState(uiState: Any): List<SettingItem>
    protected abstract fun getCurrentSettingValue(setting: SettingItem): Any?
    protected abstract fun applySettingChange(setting: SettingItem, newValue: Any?)
    protected abstract fun persistSettingChange(setting: SettingItem, newValue: Any?)
    protected abstract fun onRefreshRequested()
    protected abstract fun onRetryRequested()

    // Optional overrides
    protected open fun getFragmentTitle(): String? = null
    protected open fun getEmptyStateTitle(): String = "No settings available"
    protected open fun getEmptyStateMessage(): String = "Settings will appear here when available"
    protected open fun isImportantSetting(setting: SettingItem): Boolean = false
    protected open fun getSettingChangeMessage(setting: SettingItem, newValue: Any?): String =
        "${setting.title} updated"
}