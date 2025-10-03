package com.mtlc.studyplan.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtlc.studyplan.accessibility.AccessibilityManager
import com.mtlc.studyplan.settings.data.SettingsRepository
import com.mtlc.studyplan.settings.data.SettingItem
import com.mtlc.studyplan.ui.animations.SettingsAnimationCoordinator
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainSettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val accessibilityManager: AccessibilityManager,
    private val animationCoordinator: SettingsAnimationCoordinator
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainSettingsUiState())
    val uiState: StateFlow<MainSettingsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<MainSettingsEvent>()
    val events: SharedFlow<MainSettingsEvent> = _events.asSharedFlow()

    init {
        loadSettings()
        observeAccessibilityChanges()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                // Get all categories from the repository
                settingsRepository.getAllCategories().collect { categories ->
                    val allSettingItems = mutableListOf<SettingItem>()

                    // For each category, get its setting items
                    for (category in categories) {
                        val categoryItems = settingsRepository.getCategorySettingItems(category.id)
                        allSettingItems.addAll(categoryItems)
                    }

                    val groupedSettings = allSettingItems.groupBy { it.category }

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        settingsByCategory = groupedSettings,
                        hasSettings = allSettingItems.isNotEmpty()
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
                _events.emit(MainSettingsEvent.Error(e.message ?: "Failed to load settings"))
            }
        }
    }

    private fun observeAccessibilityChanges() {
        // Accessibility management will be implemented later
        // For now, this is a no-op
    }

    fun handleDeepLink(url: String) {
        viewModelScope.launch {
            try {
                // Simplified deep link handling - just extract setting key from URL
                val settingKey = url.substringAfterLast("/")
                _events.emit(MainSettingsEvent.NavigateToSetting(settingKey, null))
            } catch (e: Exception) {
                _events.emit(MainSettingsEvent.Error("Invalid settings link"))
            }
        }
    }

    fun onSettingClicked(settingKey: String) {
        viewModelScope.launch {
            _events.emit(MainSettingsEvent.NavigateToSetting(settingKey, null))
        }
    }

    fun onCategoryClicked(category: String) {
        viewModelScope.launch {
            _events.emit(MainSettingsEvent.NavigateToCategory(category))
        }
    }

    // Search removed

    fun onBackupClicked() {
        viewModelScope.launch {
            _events.emit(MainSettingsEvent.NavigateToBackup)
        }
    }

    fun refreshSettings() {
        loadSettings()
    }

    data class MainSettingsUiState(
        val isLoading: Boolean = false,
        val settingsByCategory: Map<String, List<SettingItem>> = emptyMap(),
        val hasSettings: Boolean = false,
        val error: String? = null
    )

    sealed class MainSettingsEvent {
        data class NavigateToSetting(val settingKey: String, val action: String?) : MainSettingsEvent()
        data class NavigateToCategory(val category: String) : MainSettingsEvent()
        object NavigateToBackup : MainSettingsEvent()
        data class Error(val message: String) : MainSettingsEvent()
    }
}

// Additional ViewModels removed to prevent compilation errors
// They can be re-implemented when the required dependencies are available
