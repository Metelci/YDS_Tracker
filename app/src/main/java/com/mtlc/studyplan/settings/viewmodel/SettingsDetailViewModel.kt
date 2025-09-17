package com.mtlc.studyplan.settings.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtlc.studyplan.core.error.AppError
import com.mtlc.studyplan.core.error.ErrorType
import com.mtlc.studyplan.settings.data.SettingItem
import com.mtlc.studyplan.settings.data.SettingsRepository
import com.mtlc.studyplan.settings.data.SettingsSection
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * ViewModel for managing settings detail screen state and operations
 */
class SettingsDetailViewModel(
    private val repository: SettingsRepository,
    private val context: Context
) : ViewModel() {

    data class SettingsDetailUiState(
        val isLoading: Boolean = false,
        val isError: Boolean = false,
        val isSuccess: Boolean = false,
        val sections: List<SettingsSection> = emptyList(),
        val error: AppError? = null,
        val categoryId: String? = null,
        val categoryTitle: String? = null
    )

    private val _uiState = MutableStateFlow(SettingsDetailUiState())
    val uiState: StateFlow<SettingsDetailUiState> = _uiState.asStateFlow()

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        handleError(exception)
    }

    /**
     * Load settings for a specific category
     */
    fun loadCategorySettings(categoryId: String) {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            isError = false,
            categoryId = categoryId
        )

        viewModelScope.launch(exceptionHandler) {
            try {
                repository.getCategorySettings(categoryId)
                    .catch { exception ->
                        handleError(exception)
                    }
                    .collectLatest { sections ->
                        val categoryTitle = repository.getCategoryTitle(categoryId)

                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isError = false,
                            isSuccess = true,
                            sections = sections,
                            categoryTitle = categoryTitle,
                            error = null
                        )
                    }
            } catch (exception: Exception) {
                handleError(exception)
            }
        }
    }

    /**
     * Handle setting value change
     */
    fun handleSettingClick(settingItem: SettingItem) {
        viewModelScope.launch(exceptionHandler) {
            try {
                when (settingItem) {
                    is com.mtlc.studyplan.settings.data.ActionSetting -> {
                        // Handle action settings (e.g., clear cache, export data)
                        repository.executeAction(settingItem.id)
                    }
                    else -> {
                        // Other setting types are handled by the adapter directly
                        // through onSettingChanged callback
                    }
                }
            } catch (exception: Exception) {
                handleError(exception)
            }
        }
    }

    /**
     * Update setting value
     */
    fun updateSetting(settingId: String, newValue: Any?) {
        viewModelScope.launch(exceptionHandler) {
            try {
                repository.updateSetting(settingId, newValue)
            } catch (exception: Exception) {
                handleError(exception)
            }
        }
    }

    /**
     * Validate setting value before updating
     */
    fun validateAndUpdateSetting(settingItem: SettingItem, newValue: Any?) {
        viewModelScope.launch(exceptionHandler) {
            try {
                val isValid = repository.validateSetting(settingItem, newValue)
                if (isValid) {
                    repository.updateSetting(settingItem.id, newValue)
                } else {
                    val error = AppError(
                        type = ErrorType.VALIDATION,
                        message = "Invalid value for ${settingItem.title}",
                        cause = null
                    )
                    _uiState.value = _uiState.value.copy(
                        isError = true,
                        error = error
                    )
                }
            } catch (exception: Exception) {
                handleError(exception)
            }
        }
    }

    /**
     * Reset all settings in current category to defaults
     */
    fun resetCategorySettings() {
        val categoryId = _uiState.value.categoryId ?: return

        viewModelScope.launch(exceptionHandler) {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                repository.resetCategoryToDefaults(categoryId)

                // Reload the category settings to reflect changes
                loadCategorySettings(categoryId)
            } catch (exception: Exception) {
                handleError(exception)
            }
        }
    }

    /**
     * Export category settings
     */
    fun exportCategorySettings() {
        val categoryId = _uiState.value.categoryId ?: return

        viewModelScope.launch(exceptionHandler) {
            try {
                repository.exportCategorySettings(categoryId)
            } catch (exception: Exception) {
                handleError(exception)
            }
        }
    }

    /**
     * Retry loading settings after an error
     */
    fun retry() {
        val categoryId = _uiState.value.categoryId
        if (categoryId != null) {
            loadCategorySettings(categoryId)
        }
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
     * Search within category settings
     */
    fun searchSettings(query: String) {
        val categoryId = _uiState.value.categoryId ?: return

        viewModelScope.launch(exceptionHandler) {
            try {
                val filteredSections = if (query.isBlank()) {
                    repository.getCategorySettingsSync(categoryId)
                } else {
                    repository.searchCategorySettings(categoryId, query)
                }

                _uiState.value = _uiState.value.copy(
                    sections = filteredSections
                )
            } catch (exception: Exception) {
                handleError(exception)
            }
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