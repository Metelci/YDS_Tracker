package com.mtlc.studyplan.settings.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtlc.studyplan.core.error.AppError
import com.mtlc.studyplan.core.error.ErrorType
import com.mtlc.studyplan.settings.data.PrivacyData
import com.mtlc.studyplan.settings.data.SettingAction
import com.mtlc.studyplan.settings.data.SettingItem
import com.mtlc.studyplan.settings.data.SettingsKeys
import com.mtlc.studyplan.settings.data.SettingsRepository
import com.mtlc.studyplan.settings.ui.BaseSettingsUiState
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * ViewModel for managing privacy settings.
 */
class PrivacySettingsViewModel(
    private val repository: SettingsRepository,
    private val context: Context
) : ViewModel() {

    data class PrivacyUiState(
        override val isLoading: Boolean = false,
        override val isError: Boolean = false,
        override val isSuccess: Boolean = false,
        val settings: List<SettingItem> = emptyList(),
        override val error: AppError? = null
    ) : BaseSettingsUiState

    private val _uiState = MutableStateFlow(PrivacyUiState())
    val uiState: StateFlow<PrivacyUiState> = _uiState.asStateFlow()

    private val _dataExportComplete = MutableLiveData<Boolean>()
    val dataExportComplete: LiveData<Boolean> = _dataExportComplete

    private val _dataClearComplete = MutableLiveData<Boolean>()
    val dataClearComplete: LiveData<Boolean> = _dataClearComplete

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        handleError(exception)
    }

    init {
        loadPrivacySettings()
    }

    private fun loadPrivacySettings() {
        _uiState.value = _uiState.value.copy(isLoading = true, isError = false)

        viewModelScope.launch(exceptionHandler) {
            repository.getPrivacySettings()
                .catch { error -> handleError(error) }
                .collectLatest { privacyData ->
                    val items = buildPrivacySettingsList(privacyData)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isError = false,
                        isSuccess = true,
                        settings = items,
                        error = null
                    )
                }
        }
    }

    private fun buildPrivacySettingsList(privacyData: PrivacyData): List<SettingItem> {
        return listOf(
            SettingItem.ActionSetting(
                id = "data_export",
                title = "Export Personal Data",
                description = "Download a copy of the personal data stored on this device",
                action = SettingAction.ExportPersonalData,
                buttonText = "Export",
                actionType = SettingItem.ActionSetting.ActionType.SECONDARY,
                isEnabled = true,
                category = "privacy",
                sortOrder = 1
            ),
            SettingItem.ActionSetting(
                id = "clear_personal_data",
                title = "Clear Personal Data",
                description = "Permanently delete all personal data saved on this device",
                action = SettingAction.ClearPersonalData,
                buttonText = "Clear",
                actionType = SettingItem.ActionSetting.ActionType.DESTRUCTIVE,
                isEnabled = true,
                category = "privacy",
                sortOrder = 2
            )
        )
    }

    fun exportPersonalData() {
        viewModelScope.launch(exceptionHandler) {
            try {
                repository.exportPersonalData()
                _dataExportComplete.value = true
            } catch (exception: Exception) {
                handleError(exception)
                _dataExportComplete.value = false
            }
        }
    }

    fun clearAllPersonalData() {
        viewModelScope.launch(exceptionHandler) {
            try {
                repository.clearAllPersonalData()
                _dataClearComplete.value = true
            } catch (exception: Exception) {
                handleError(exception)
                _dataClearComplete.value = false
            }
        }
    }

    fun refresh() {
        loadPrivacySettings()
    }

    fun retry() {
        loadPrivacySettings()
    }

    private fun handleError(exception: Throwable) {
        val resolvedError = when (exception) {
            is AppError -> exception
            is IllegalArgumentException -> AppError(
                type = ErrorType.VALIDATION,
                message = exception.message ?: "Invalid privacy setting",
                cause = exception
            )
            is SecurityException -> AppError(
                type = ErrorType.PERMISSION,
                message = "Permission denied while updating privacy settings",
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
            error = resolvedError
        )
    }

    override fun onCleared() {
        super.onCleared()
        // No additional cleanup required.
    }
}
