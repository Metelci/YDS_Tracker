package com.mtlc.studyplan.settings.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mtlc.studyplan.core.error.AppError
import com.mtlc.studyplan.settings.data.SettingsRepository
import com.mtlc.studyplan.settings.search.SearchResult
import com.mtlc.studyplan.settings.search.SettingsSearchEngine
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for settings search functionality
 */
class SettingsSearchViewModel(
    private val context: Context,
    private val repository: SettingsRepository,
    private val searchEngine: SettingsSearchEngine
) : ViewModel() {

    data class SearchUiState(
        val isLoading: Boolean = false,
        val hasError: Boolean = false,
        val error: AppError? = null,
        val query: String = "",
        val results: List<SearchResult> = emptyList(),
        val suggestions: List<String> = emptyList(),
        val recentSearches: List<String> = emptyList(),
        val isIndexed: Boolean = false
    )

    sealed class NavigationEvent {
        data class NavigateToSetting(val result: SearchResult) : NavigationEvent()
        data class NavigateToCategory(val categoryId: String) : NavigationEvent()
    }

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<NavigationEvent>()
    val navigationEvents: SharedFlow<NavigationEvent> = _navigationEvents.asSharedFlow()

    private val searchHistoryManager = SearchHistoryManager(context)
    private val searchAnalytics = SearchAnalytics(context)

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        handleError(exception)
    }

    private val _searchQuery = MutableStateFlow("")
    private val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        setupSearchFlow()
        loadSearchHistory()
    }

    /**
     * Setup reactive search flow with debouncing
     */
    private fun setupSearchFlow() {
        viewModelScope.launch {
            searchQuery
                .debounce(300) // Debounce search input
                .distinctUntilChanged()
                .collectLatest { query ->
                    performSearch(query)
                }
        }
    }

    /**
     * Initialize search engine with settings data
     */
    fun initializeSearch() {
        viewModelScope.launch(exceptionHandler) {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                // Load all settings data
                val categories = repository.getAllCategoriesSync()
                val allSettings = categories.associate { category ->
                    category.id to repository.getCategorySettingsSync(category.id)
                        .flatMap { it.settings }
                }

                // Index settings for search
                searchEngine.indexSettings(categories, allSettings)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isIndexed = true
                )

            } catch (exception: Exception) {
                handleError(exception)
            }
        }
    }

    /**
     * Perform search with the given query
     */
    fun search(query: String) {
        _searchQuery.value = query.trim()
    }

    /**
     * Internal search implementation
     */
    private suspend fun performSearch(query: String) {
        _uiState.value = _uiState.value.copy(
            query = query,
            isLoading = query.isNotEmpty()
        )

        if (query.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                results = emptyList(),
                isLoading = false
            )
            return
        }

        if (query.length < 2) {
            _uiState.value = _uiState.value.copy(
                results = emptyList(),
                isLoading = false
            )
            updateSearchSuggestions(query)
            return
        }

        try {
            searchEngine.search(query).collect { results ->
                _uiState.value = _uiState.value.copy(
                    results = results,
                    isLoading = false,
                    hasError = false,
                    error = null
                )

                // Track search analytics
                searchAnalytics.trackSearch(query, results.size)
            }
        } catch (exception: Exception) {
            handleError(exception)
        }
    }

    /**
     * Handle search result click
     */
    fun onSearchResultClick(result: SearchResult) {
        viewModelScope.launch {
            // Track result click
            searchAnalytics.trackResultClick(result.item.id, result.relevanceScore)

            // Add to search history if not already there
            addToSearchHistory(_uiState.value.query)

            // Navigate to setting
            _navigationEvents.emit(NavigationEvent.NavigateToSetting(result))
        }
    }

    /**
     * Navigate to deep link path
     */
    fun navigateToDeepLink(path: String) {
        viewModelScope.launch {
            try {
                val parts = path.split("/")
                if (parts.size >= 3 && parts[0] == "settings") {
                    val categoryId = parts[1]
                    val settingId = parts[2]

                    val setting = searchEngine.getSettingById(settingId)
                    if (setting != null) {
                        val result = SearchResult(
                            item = setting,
                            relevanceScore = 10.0,
                            matchedText = setting.title,
                            highlightRanges = emptyList()
                        )
                        _navigationEvents.emit(NavigationEvent.NavigateToSetting(result))
                    } else {
                        _navigationEvents.emit(NavigationEvent.NavigateToCategory(categoryId))
                    }
                }
            } catch (exception: Exception) {
                handleError(exception)
            }
        }
    }

    /**
     * Add query to search history
     */
    fun addToSearchHistory(query: String) {
        if (query.isNotEmpty() && query.length >= 2) {
            viewModelScope.launch {
                searchHistoryManager.addToHistory(query)
                loadSearchHistory()
            }
        }
    }

    /**
     * Load search history
     */
    private fun loadSearchHistory() {
        viewModelScope.launch {
            val history = searchHistoryManager.getSearchHistory()
            _uiState.value = _uiState.value.copy(recentSearches = history)
        }
    }

    /**
     * Clear search history
     */
    fun clearSearchHistory() {
        viewModelScope.launch {
            searchHistoryManager.clearHistory()
            loadSearchHistory()
        }
    }

    /**
     * Update search suggestions based on partial query
     */
    private fun updateSearchSuggestions(partialQuery: String) {
        if (partialQuery.isEmpty()) {
            _uiState.value = _uiState.value.copy(suggestions = emptyList())
            return
        }

        viewModelScope.launch {
            val suggestions = generateSearchSuggestions(partialQuery)
            _uiState.value = _uiState.value.copy(suggestions = suggestions)
        }
    }

    /**
     * Generate search suggestions
     */
    private fun generateSearchSuggestions(partialQuery: String): List<String> {
        val suggestions = mutableListOf<String>()
        val query = partialQuery.lowercase()

        // Common setting keywords
        val commonKeywords = listOf(
            "notifications", "privacy", "backup", "sync", "theme", "language",
            "password", "security", "data", "export", "import", "reset",
            "toggle", "enable", "disable", "settings", "preferences"
        )

        // Add matching keywords
        commonKeywords.filter { it.startsWith(query) }
            .take(5)
            .forEach { suggestions.add(it) }

        // Add from recent searches
        _uiState.value.recentSearches
            .filter { it.lowercase().contains(query) }
            .take(3)
            .forEach { suggestions.add(it) }

        return suggestions.distinct().take(8)
    }

    /**
     * Track setting access for analytics
     */
    fun trackSettingAccess(settingId: String, source: String) {
        viewModelScope.launch {
            searchAnalytics.trackSettingAccess(settingId, source)
        }
    }

    /**
     * Handle errors
     */
    private fun handleError(exception: Throwable) {
        val appError = when (exception) {
            is AppError -> exception
            else -> AppError(
                type = com.mtlc.studyplan.core.error.ErrorType.UNKNOWN,
                message = exception.message ?: "Search failed",
                cause = exception
            )
        }

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            hasError = true,
            error = appError
        )
    }

    override fun onCleared() {
        super.onCleared()
        searchEngine.dispose()
    }
}

/**
 * Manages search history persistence
 */
class SearchHistoryManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("search_history", Context.MODE_PRIVATE)
    private val maxHistorySize = 20

    suspend fun addToHistory(query: String) {
        val history = getSearchHistory().toMutableList()

        // Remove if already exists
        history.remove(query)

        // Add to beginning
        history.add(0, query)

        // Limit size
        if (history.size > maxHistorySize) {
            history.removeAt(history.size - 1)
        }

        // Save
        prefs.edit()
            .putStringSet("queries", history.toSet())
            .apply()
    }

    fun getSearchHistory(): List<String> {
        return prefs.getStringSet("queries", emptySet())?.toList() ?: emptyList()
    }

    fun clearHistory() {
        prefs.edit().remove("queries").apply()
    }
}

/**
 * Tracks search analytics
 */
class SearchAnalytics(private val context: Context) {
    fun trackSearch(query: String, resultCount: Int) {
        // TODO: Implement analytics tracking
    }

    fun trackResultClick(settingId: String, relevanceScore: Double) {
        // TODO: Implement analytics tracking
    }

    fun trackSettingAccess(settingId: String, source: String) {
        // TODO: Implement analytics tracking
    }
}

/**
 * Factory for creating SettingsSearchViewModel
 */
class SettingsSearchViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsSearchViewModel::class.java)) {
            val repository = SettingsRepository(context)
            val searchEngine = SettingsSearchEngine(context)
            return SettingsSearchViewModel(context, repository, searchEngine) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}