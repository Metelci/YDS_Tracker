package com.mtlc.studyplan.settings.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.transition.MaterialFadeThrough
import com.mtlc.studyplan.R
import com.mtlc.studyplan.databinding.FragmentSettingsSearchBinding
import com.mtlc.studyplan.settings.deeplink.SettingsDeepLinkHandler
import com.mtlc.studyplan.settings.search.SearchResult
import com.mtlc.studyplan.settings.viewmodel.SettingsSearchViewModel
import com.mtlc.studyplan.settings.viewmodel.SettingsSearchViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Fragment for global settings search with advanced features
 */
class SettingsSearchFragment : Fragment() {

    private var _binding: FragmentSettingsSearchBinding? = null
    private val binding get() = _binding!!

    private lateinit var searchAdapter: SettingsSearchAdapter
    private lateinit var deepLinkHandler: SettingsDeepLinkHandler

    private val viewModel: SettingsSearchViewModel by viewModels {
        SettingsSearchViewModelFactory(requireContext())
    }

    companion object {
        private const val ARG_INITIAL_QUERY = "initial_query"
        private const val ARG_DEEP_LINK_PATH = "deep_link_path"

        fun newInstance(initialQuery: String? = null, deepLinkPath: String? = null): SettingsSearchFragment {
            return SettingsSearchFragment().apply {
                arguments = Bundle().apply {
                    initialQuery?.let { putString(ARG_INITIAL_QUERY, it) }
                    deepLinkPath?.let { putString(ARG_DEEP_LINK_PATH, it) }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set up transitions
        enterTransition = MaterialFadeThrough()
        exitTransition = MaterialFadeThrough()

        deepLinkHandler = SettingsDeepLinkHandler(requireActivity())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSearchInput()
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()

        // Handle initial query or deep link
        arguments?.let { args ->
            args.getString(ARG_INITIAL_QUERY)?.let { query ->
                binding.searchInput.setText(query)
                viewModel.search(query)
            }

            args.getString(ARG_DEEP_LINK_PATH)?.let { path ->
                viewModel.navigateToDeepLink(path)
            }
        }

        // Initialize search engine
        viewModel.initializeSearch()
    }

    /**
     * Setup search input with debouncing and suggestions
     */
    private fun setupSearchInput() {
        binding.searchInput.apply {
            // Real-time search with debouncing handled by ViewModel
            addTextChangedListener { text ->
                val query = text.toString().trim()
                viewModel.search(query)

                // Show/hide clear button
                binding.clearSearchButton.isVisible = query.isNotEmpty()
            }

            // Handle search action
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    val query = text.toString().trim()
                    if (query.isNotEmpty()) {
                        viewModel.addToSearchHistory(query)
                        hideKeyboard()
                    }
                    true
                } else false
            }

            // Focus and show keyboard
            requestFocus()
            showKeyboard()
        }
    }

    /**
     * Setup RecyclerView for search results
     */
    private fun setupRecyclerView() {
        searchAdapter = SettingsSearchAdapter(
            onResultClick = { result ->
                viewModel.onSearchResultClick(result)
                navigateToSetting(result)
            },
            onCategoryClick = { categoryId ->
                navigateToCategory(categoryId)
            }
        )

        binding.searchResultsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = searchAdapter

            // Add scroll listener for hiding keyboard
            addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: androidx.recyclerview.widget.RecyclerView, newState: Int) {
                    if (newState == androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_DRAGGING) {
                        hideKeyboard()
                    }
                }
            })
        }
    }

    /**
     * Setup click listeners
     */
    private fun setupClickListeners() {
        binding.backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.clearSearchButton.setOnClickListener {
            binding.searchInput.text?.clear()
            binding.searchInput.requestFocus()
            showKeyboard()
        }

        binding.voiceSearchButton.setOnClickListener {
            // TODO: Implement voice search
            showNotYetImplemented("Voice search")
        }

        binding.searchHistoryButton.setOnClickListener {
            showSearchHistory()
        }
    }

    /**
     * Observe ViewModel state changes
     */
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { uiState ->
                updateUI(uiState)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.navigationEvents.collectLatest { event ->
                handleNavigationEvent(event)
            }
        }
    }

    /**
     * Update UI based on state
     */
    private fun updateUI(uiState: SettingsSearchViewModel.SearchUiState) {
        // Update loading state
        binding.searchProgressIndicator.isVisible = uiState.isLoading

        // Update results
        when {
            uiState.isLoading -> {
                showLoadingState()
            }
            uiState.hasError -> {
                showErrorState(uiState.error?.message ?: "Search failed")
            }
            uiState.query.isEmpty() -> {
                showEmptySearchState()
            }
            uiState.results.isEmpty() && uiState.query.isNotEmpty() -> {
                showNoResultsState(uiState.query)
            }
            else -> {
                showResultsState(uiState.results, uiState.query)
            }
        }

        // Update search suggestions
        updateSearchSuggestions(uiState.suggestions)
    }

    /**
     * Show loading state
     */
    private fun showLoadingState() {
        binding.apply {
            searchResultsRecyclerView.isVisible = false
            emptyStateContainer.isVisible = false
            loadingContainer.isVisible = true
        }
    }

    /**
     * Show error state
     */
    private fun showErrorState(message: String) {
        binding.apply {
            searchResultsRecyclerView.isVisible = false
            loadingContainer.isVisible = false
            emptyStateContainer.isVisible = true
            emptyStateIcon.setImageResource(R.drawable.ic_error)
            emptyStateTitle.text = "Search Error"
            emptyStateMessage.text = message
            emptyStateAction.isVisible = true
            emptyStateAction.text = "Retry"
            emptyStateAction.setOnClickListener {
                val query = searchInput.text.toString().trim()
                if (query.isNotEmpty()) {
                    viewModel.search(query)
                }
            }
        }
    }

    /**
     * Show empty search state (no query entered)
     */
    private fun showEmptySearchState() {
        binding.apply {
            searchResultsRecyclerView.isVisible = false
            loadingContainer.isVisible = false
            emptyStateContainer.isVisible = true
            emptyStateIcon.setImageResource(R.drawable.ic_search)
            emptyStateTitle.text = "Search Settings"
            emptyStateMessage.text = "Find any setting quickly by typing keywords"
            emptyStateAction.isVisible = true
            emptyStateAction.text = "Browse Categories"
            emptyStateAction.setOnClickListener {
                // Navigate back to main settings
                parentFragmentManager.popBackStack()
            }
        }

        // Show recent searches or suggestions
        showRecentSearches()
    }

    /**
     * Show no results state
     */
    private fun showNoResultsState(query: String) {
        binding.apply {
            searchResultsRecyclerView.isVisible = false
            loadingContainer.isVisible = false
            emptyStateContainer.isVisible = true
            emptyStateIcon.setImageResource(R.drawable.ic_search_off)
            emptyStateTitle.text = "No Results Found"
            emptyStateMessage.text = "No settings found for \"$query\". Try different keywords."
            emptyStateAction.isVisible = true
            emptyStateAction.text = "Clear Search"
            emptyStateAction.setOnClickListener {
                binding.searchInput.text?.clear()
            }
        }
    }

    /**
     * Show results state
     */
    private fun showResultsState(results: List<SearchResult>, query: String) {
        binding.apply {
            emptyStateContainer.isVisible = false
            loadingContainer.isVisible = false
            searchResultsRecyclerView.isVisible = true
        }

        searchAdapter.updateResults(results, query)
    }

    /**
     * Navigate to specific setting
     */
    private fun navigateToSetting(result: SearchResult) {
        deepLinkHandler.navigateToSetting(
            categoryId = result.item.categoryId,
            settingId = result.item.id
        )

        // Add to analytics
        viewModel.trackSettingAccess(result.item.id, "search")
    }

    /**
     * Navigate to category
     */
    private fun navigateToCategory(categoryId: String) {
        deepLinkHandler.navigateToCategory(categoryId)
    }

    /**
     * Handle navigation events from ViewModel
     */
    private fun handleNavigationEvent(event: SettingsSearchViewModel.NavigationEvent) {
        when (event) {
            is SettingsSearchViewModel.NavigationEvent.NavigateToSetting -> {
                navigateToSetting(event.result)
            }
            is SettingsSearchViewModel.NavigationEvent.NavigateToCategory -> {
                navigateToCategory(event.categoryId)
            }
        }
    }

    /**
     * Update search suggestions
     */
    private fun updateSearchSuggestions(suggestions: List<String>) {
        // TODO: Implement search suggestions UI
    }

    /**
     * Show recent searches
     */
    private fun showRecentSearches() {
        // TODO: Implement recent searches UI
    }

    /**
     * Show search history dialog
     */
    private fun showSearchHistory() {
        // TODO: Implement search history dialog
    }

    /**
     * Show keyboard
     */
    private fun showKeyboard() {
        binding.searchInput.requestFocus()
        val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
            as android.view.inputmethod.InputMethodManager
        imm.showSoftInput(binding.searchInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
    }

    /**
     * Hide keyboard
     */
    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
            as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(binding.searchInput.windowToken, 0)
    }

    /**
     * Show not yet implemented message
     */
    private fun showNotYetImplemented(feature: String) {
        com.google.android.material.snackbar.Snackbar.make(
            binding.root,
            "$feature coming soon!",
            com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}