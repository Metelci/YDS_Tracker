package com.mtlc.studyplan.settings.viewmodel

import android.content.Context
import androidx.lifecycle.*
import com.mtlc.studyplan.core.error.AppError
import com.mtlc.studyplan.core.error.ErrorType
import com.mtlc.studyplan.settings.data.*
import com.mtlc.studyplan.settings.ui.BaseSettingsUiState
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for managing privacy settings
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
        override val error: AppError? = null,
        val profileVisibilityEnabled: Boolean = true,
        val profileVisibilityLevel: ProfileVisibilityLevel = ProfileVisibilityLevel.FRIENDS_ONLY,
        val anonymousAnalytics: Boolean = true,
        val progressSharing: Boolean = true
    ) : BaseSettingsUiState

    private val _uiState = MutableStateFlow(PrivacyUiState())
    val uiState: StateFlow<PrivacyUiState> = _uiState.asStateFlow()

    // Live data for action completion
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

    /**
     * Load privacy settings
     */
    private fun loadPrivacySettings() {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            isError = false
        )

        viewModelScope.launch(exceptionHandler) {
            try {
                repository.getPrivacySettings()
                    .catch { exception ->
                        handleError(exception)
                    }
                    .collectLatest { privacyData ->
                        val settings = buildPrivacySettingsList(privacyData)

                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isError = false,
                            isSuccess = true,
                            settings = settings,
                            profileVisibilityEnabled = privacyData.profileVisibilityEnabled,
                            profileVisibilityLevel = privacyData.profileVisibilityLevel,
                            anonymousAnalytics = privacyData.anonymousAnalytics,
                            progressSharing = privacyData.progressSharing,
                            error = null
                        )
                    }
            } catch (exception: Exception) {
                handleError(exception)
            }
        }
    }

    /**
     * Build privacy settings list
     */
    private fun buildPrivacySettingsList(privacyData: PrivacyData): List<SettingItem> {
        return listOf(
            // Profile Visibility Toggle
            ToggleSetting(
                id = "profile_visibility_enabled",
                title = "Profile Visibility",
                description = "Allow others to see your study profile",
                value = privacyData.profileVisibilityEnabled,
                key = SettingsKeys.Privacy.PROFILE_VISIBILITY_ENABLED,
                defaultValue = true,
                isEnabled = true,
                category = "privacy",
                sortOrder = 1
            ),

            // Profile Visibility Level Selection
            SelectionSetting(
                id = "profile_visibility_level",
                title = "Visibility Level",
                description = "Choose who can see your profile information",
                options = listOf(
                    SelectionOption(
                        value = ProfileVisibilityLevel.PUBLIC,
                        display = "Public",
                        description = "Anyone can see your profile"
                    ),
                    SelectionOption(
                        value = ProfileVisibilityLevel.FRIENDS_ONLY,
                        display = "Friends Only",
                        description = "Only your study buddies can see your profile"
                    ),
                    SelectionOption(
                        value = ProfileVisibilityLevel.PRIVATE,
                        display = "Private",
                        description = "Your profile is completely private"
                    )
                ),
                currentValue = privacyData.profileVisibilityLevel,
                key = SettingsKeys.Privacy.PROFILE_VISIBILITY_LEVEL,
                isEnabled = privacyData.profileVisibilityEnabled,
                category = "privacy",
                sortOrder = 2
            ),

            // Anonymous Analytics Toggle
            ToggleSetting(
                id = "anonymous_analytics",
                title = "Anonymous Analytics",
                description = "Help improve the app by sharing anonymous usage data",
                value = privacyData.anonymousAnalytics,
                key = SettingsKeys.Privacy.ANONYMOUS_ANALYTICS,
                defaultValue = true,
                isEnabled = true,
                category = "privacy",
                sortOrder = 3
            ),

            // Progress Sharing Toggle
            ToggleSetting(
                id = "progress_sharing",
                title = "Progress Sharing",
                description = "Allow sharing of your study progress and achievements",
                value = privacyData.progressSharing,
                key = SettingsKeys.Privacy.PROGRESS_SHARING,
                defaultValue = true,
                isEnabled = true,
                category = "privacy",
                sortOrder = 4
            ),

            // Data Export Action
            ActionSetting(
                id = "data_export",
                title = "Export Personal Data",
                description = "Download a copy of all your personal data",
                buttonText = "Export",
                actionType = ActionSetting.ActionType.SECONDARY,
                isEnabled = true,
                category = "privacy",
                sortOrder = 5
            ),

            // Clear Data Action
            ActionSetting(
                id = "clear_personal_data",
                title = "Clear Personal Data",
                description = "Permanently delete all your personal data",
                buttonText = "Clear",
                actionType = ActionSetting.ActionType.DESTRUCTIVE,
                isEnabled = true,
                category = "privacy",
                sortOrder = 6
            )
        )
    }

    /**
     * Update profile visibility enabled state
     */
    fun updateProfileVisibilityEnabled(enabled: Boolean) {
        viewModelScope.launch(exceptionHandler) {
            try {
                repository.updatePrivacySetting("profile_visibility_enabled", enabled)
            } catch (exception: Exception) {
                handleError(exception)
            }
        }
    }

    /**
     * Update profile visibility level
     */
    fun updateProfileVisibilityLevel(level: ProfileVisibilityLevel) {
        viewModelScope.launch(exceptionHandler) {
            try {
                repository.updatePrivacySetting("profile_visibility_level", level)
            } catch (exception: Exception) {
                handleError(exception)
            }
        }
    }

    /**
     * Update anonymous analytics setting
     */
    fun updateAnonymousAnalytics(enabled: Boolean) {
        viewModelScope.launch(exceptionHandler) {
            try {
                repository.updatePrivacySetting("anonymous_analytics", enabled)
            } catch (exception: Exception) {
                handleError(exception)
            }
        }
    }

    /**
     * Update progress sharing setting
     */
    fun updateProgressSharing(enabled: Boolean) {
        viewModelScope.launch(exceptionHandler) {
            try {
                repository.updatePrivacySetting("progress_sharing", enabled)
            } catch (exception: Exception) {
                handleError(exception)
            }
        }
    }

    /**
     * Export personal data
     */
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

    /**
     * Clear all personal data
     */
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

    /**
     * Refresh settings
     */
    fun refresh() {
        loadPrivacySettings()
    }

    /**
     * Retry loading after error
     */
    fun retry() {
        loadPrivacySettings()
    }

    private fun handleError(exception: Throwable) {
        val appError = when (exception) {
            is AppError -> exception
            is IllegalArgumentException -> AppError(
                type = ErrorType.VALIDATION,
                message = exception.message ?: "Invalid privacy setting",
                cause = exception
            )
            is SecurityException -> AppError(
                type = ErrorType.PERMISSION,
                message = "Permission denied for privacy operation",
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
 * Data classes for privacy settings
 */

