package com.mtlc.studyplan.settings.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtlc.studyplan.core.error.AppError
import com.mtlc.studyplan.core.error.ErrorType
import com.mtlc.studyplan.settings.data.SettingItem
import com.mtlc.studyplan.settings.data.SettingsRepository
import com.mtlc.studyplan.settings.data.SettingsSection
import com.mtlc.studyplan.settings.data.SettingsUpdateRequest
import com.mtlc.studyplan.settings.data.SettingAction
import com.mtlc.studyplan.settings.data.SettingsOperationResult
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
                val sections = repository.getCategorySections(categoryId)
                val category = repository.getAllCategoriesSync().find { it.id == categoryId }
                val categoryTitle = category?.title ?: "Settings"

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isError = false,
                    isSuccess = true,
                    sections = sections,
                    categoryTitle = categoryTitle,
                    error = null
                )
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
                        // Handle action settings by converting to appropriate action
                        val action = when (settingItem.id) {
                            "clear_cache" -> SettingAction.ClearCache
                            "reset_settings" -> SettingAction.ResetSettings
                            "export_data" -> SettingAction.ExportData
                            "reset_progress" -> SettingAction.ResetProgress
                            "sync_data" -> SettingAction.SyncData
                            else -> return@launch
                        }
                        repository.updateSetting(SettingsUpdateRequest.PerformAction(action))
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
                val request = when (newValue) {
                    is Boolean -> SettingsUpdateRequest.UpdateBoolean(settingId, newValue)
                    is Int -> SettingsUpdateRequest.UpdateInt(settingId, newValue)
                    is Float -> SettingsUpdateRequest.UpdateFloat(settingId, newValue)
                    is String -> SettingsUpdateRequest.UpdateString(settingId, newValue)
                    else -> return@launch
                }
                repository.updateSetting(request)
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
                val request = when (newValue) {
                    is Boolean -> SettingsUpdateRequest.UpdateBoolean(settingItem.id, newValue)
                    is Int -> SettingsUpdateRequest.UpdateInt(settingItem.id, newValue)
                    is Float -> SettingsUpdateRequest.UpdateFloat(settingItem.id, newValue)
                    is String -> SettingsUpdateRequest.UpdateString(settingItem.id, newValue)
                    else -> return@launch
                }
                val result = repository.updateSetting(request)
                when (result) {
                    is SettingsOperationResult.ValidationError -> {
                        val error = AppError(
                            type = ErrorType.VALIDATION,
                            message = result.errors.firstOrNull()?.message ?: "Invalid value for ${settingItem.title}",
                            cause = null
                        )
                        _uiState.value = _uiState.value.copy(
                            isError = true,
                            error = error
                        )
                    }
                    is SettingsOperationResult.Error -> throw Exception(result.message)
                    else -> { /* Success - do nothing */ }
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

                // Reset all settings to defaults (no category-specific reset available)
                repository.resetAllToDefaults()

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
                // Export all settings (no category-specific export available)
                repository.exportAllSettings()
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
