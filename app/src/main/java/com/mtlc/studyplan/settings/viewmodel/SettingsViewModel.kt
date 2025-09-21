package com.mtlc.studyplan.settings.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtlc.studyplan.core.error.AppError
import com.mtlc.studyplan.core.error.ErrorType
import com.mtlc.studyplan.settings.data.SettingsCategory
import com.mtlc.studyplan.settings.data.SettingsRepository
import com.mtlc.studyplan.settings.data.SettingsUpdateRequest
import com.mtlc.studyplan.settings.data.SettingAction
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * ViewModel for managing main settings screen state and operations
 */
class SettingsViewModel(
    private val repository: SettingsRepository,
    private val context: Context
) : ViewModel() {

    data class SettingsUiState(
        val isLoading: Boolean = false,
        val isError: Boolean = false,
        val isSuccess: Boolean = false,
        val categories: List<SettingsCategory> = emptyList(),
        val filteredCategories: List<SettingsCategory> = emptyList(),
        val error: AppError? = null,
        val searchQuery: String = "",
        val isSearchActive: Boolean = false,
        val selectedCategoryId: String? = null,
        val appVersion: String = "",
        val lastBackupDate: String? = null
    )

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        handleError(exception)
    }

    init {
        loadSettings()
        loadAppInfo()
    }

    /**
     * Load all settings categories
     */
    private fun loadSettings() {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            isError = false
        )

        viewModelScope.launch(exceptionHandler) {
            try {
                repository.getAllCategories()
                    .catch { exception ->
                        handleError(exception)
                    }
                    .collectLatest { categories ->
                        val filteredCategories = if (_uiState.value.searchQuery.isBlank()) {
                            categories
                        } else {
                            filterCategories(categories, _uiState.value.searchQuery)
                        }

                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isError = false,
                            isSuccess = true,
                            categories = categories,
                            filteredCategories = filteredCategories,
                            error = null
                        )
                    }
            } catch (exception: Exception) {
                handleError(exception)
            }
        }
    }

    /**
     * Load app version and backup info
     */
    private fun loadAppInfo() {
        viewModelScope.launch(exceptionHandler) {
            try {
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                val appVersion = "${packageInfo.versionName} (${packageInfo.longVersionCode})"
                val lastBackupDate = repository.getLastBackupDate()

                _uiState.value = _uiState.value.copy(
                    appVersion = appVersion,
                    lastBackupDate = lastBackupDate
                )
            } catch (exception: Exception) {
                // Non-critical error, just log it
                val appVersion = "Unknown"
                _uiState.value = _uiState.value.copy(appVersion = appVersion)
            }
        }
    }

    /**
     * Search settings categories
     */
    fun searchCategories(query: String) {
        val trimmedQuery = query.trim()

        _uiState.value = _uiState.value.copy(
            searchQuery = trimmedQuery,
            isSearchActive = trimmedQuery.isNotEmpty(),
            filteredCategories = if (trimmedQuery.isBlank()) {
                _uiState.value.categories
            } else {
                filterCategories(_uiState.value.categories, trimmedQuery)
            }
        )
    }

    /**
     * Clear search and show all categories
     */
    fun clearSearch() {
        _uiState.value = _uiState.value.copy(
            searchQuery = "",
            isSearchActive = false,
            filteredCategories = _uiState.value.categories
        )
    }

    /**
     * Select a category (for tablet layout)
     */
    fun selectCategory(categoryId: String) {
        _uiState.value = _uiState.value.copy(
            selectedCategoryId = categoryId
        )
    }

    /**
     * Clear category selection
     */
    fun clearCategorySelection() {
        _uiState.value = _uiState.value.copy(
            selectedCategoryId = null
        )
    }

    /**
     * Export all settings
     */
    fun exportAllSettings() {
        viewModelScope.launch(exceptionHandler) {
            try {
                repository.exportAllSettings()
                // Update last backup date
                loadAppInfo()
            } catch (exception: Exception) {
                handleError(exception)
            }
        }
    }

    /**
     * Import settings from file
     */
    fun importSettings() {
        viewModelScope.launch(exceptionHandler) {
            try {
                repository.importSettings()
                // Reload settings after import
                loadSettings()
                loadAppInfo()
            } catch (exception: Exception) {
                handleError(exception)
            }
        }
    }

    /**
     * Reset all settings to defaults
     */
    fun resetAllSettings() {
        viewModelScope.launch(exceptionHandler) {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                repository.resetAllToDefaults()

                // Reload settings after reset
                loadSettings()
            } catch (exception: Exception) {
                handleError(exception)
            }
        }
    }

    /**
     * Clear app cache
     */
    fun clearAppCache() {
        viewModelScope.launch(exceptionHandler) {
            try {
                repository.updateSetting(SettingsUpdateRequest.PerformAction(SettingAction.ClearCache))
            } catch (exception: Exception) {
                handleError(exception)
            }
        }
    }

    /**
     * Get category by ID
     */
    fun getCategoryById(categoryId: String): SettingsCategory? {
        return _uiState.value.categories.find { it.id == categoryId }
    }

    /**
     * Retry loading settings after an error
     */
    fun retry() {
        loadSettings()
        loadAppInfo()
    }

    /**
     * Clear error state
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(
            isError = false,
            error = null
        )
    }

    /**
     * Refresh settings data
     */
    fun refresh() {
        loadSettings()
        loadAppInfo()
    }

    private fun filterCategories(categories: List<SettingsCategory>, query: String): List<SettingsCategory> {
        val queryLower = query.lowercase()
        return categories.filter { category ->
            category.title.lowercase().contains(queryLower) ||
            category.description.lowercase().contains(queryLower) ||
            category.searchKeywords.any { it.lowercase().contains(queryLower) }
        }
    }

    private fun handleError(exception: Throwable) {
        val appError = when (exception) {
            is AppError -> exception
            is IllegalArgumentException -> AppError(
                type = ErrorType.VALIDATION,
                message = exception.message ?: "Invalid input",
                cause = exception
            )
            is SecurityException -> AppError(
                type = ErrorType.PERMISSION,
                message = "Permission denied",
                cause = exception
            )
            is java.io.IOException -> AppError(
                type = ErrorType.NETWORK,
                message = "File operation failed",
                cause = exception
            )
            else -> AppError(
                type = ErrorType.UNKNOWN,
                message = exception.message ?: "An unexpected error occurred",
                cause = exception
            )
        }

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            isError = true,
            isSuccess = false,
            error = appError
        )
    }

    override fun onCleared() {
        super.onCleared()
        // Cleanup is handled by repository dispose
    }
}

/**
 * Factory for creating SettingsViewModel with dependencies
 */
class SettingsViewModelFactory(
    private val repository: SettingsRepository,
    private val context: Context
) : androidx.lifecycle.ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            return SettingsViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}