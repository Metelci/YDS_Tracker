package com.mtlc.studyplan.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtlc.studyplan.accessibility.AccessibilityManager
import com.mtlc.studyplan.settings.deeplink.SettingsDeepLinkHandler
import com.mtlc.studyplan.settings.repository.SettingsRepository
import com.mtlc.studyplan.settings.repository.SettingItem
import com.mtlc.studyplan.settings.search.SettingsSearchEngine
import com.mtlc.studyplan.ui.animations.SettingsAnimationCoordinator
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainSettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val searchEngine: SettingsSearchEngine,
    private val deepLinkHandler: SettingsDeepLinkHandler,
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
                val allSettings = settingsRepository.getAllSettings()
                val settingItems = allSettings.map { (key, value) ->
                    val metadata = settingsRepository.getSettingMetadata(key)
                    SettingItem(
                        key = key,
                        title = metadata.title,
                        description = metadata.description,
                        value = value,
                        type = metadata.type,
                        category = metadata.category
                    )
                }

                val groupedSettings = settingItems.groupBy { it.category }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    settingsByCategory = groupedSettings,
                    hasSettings = settingItems.isNotEmpty()
                )
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
        viewModelScope.launch {
            accessibilityManager.accessibilityState.collect { accessibilityState ->
                _uiState.value = _uiState.value.copy(
                    accessibilityState = accessibilityState
                )
            }
        }
    }

    fun handleDeepLink(url: String) {
        viewModelScope.launch {
            try {
                val result = deepLinkHandler.handleDeepLink(url)
                _events.emit(MainSettingsEvent.NavigateToSetting(result.settingKey, result.action))
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

    fun onSearchClicked() {
        viewModelScope.launch {
            _events.emit(MainSettingsEvent.NavigateToSearch)
        }
    }

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
        val error: String? = null,
        val accessibilityState: AccessibilityManager.AccessibilityState = AccessibilityManager.AccessibilityState()
    )

    sealed class MainSettingsEvent {
        data class NavigateToSetting(val settingKey: String, val action: String?) : MainSettingsEvent()
        data class NavigateToCategory(val category: String) : MainSettingsEvent()
        object NavigateToSearch : MainSettingsEvent()
        object NavigateToBackup : MainSettingsEvent()
        data class Error(val message: String) : MainSettingsEvent()
    }
}

class SettingsSearchViewModel(
    private val searchEngine: SettingsSearchEngine,
    private val voiceSearchManager: com.mtlc.studyplan.settings.search.VoiceSearchManager,
    private val accessibilityManager: AccessibilityManager,
    private val animationCoordinator: SettingsAnimationCoordinator
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SearchEvent>()
    val events: SharedFlow<SearchEvent> = _events.asSharedFlow()

    fun search(query: String) {
        if (query.isBlank()) {
            clearSearch()
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true)

            try {
                val results = searchEngine.search(query)
                _uiState.value = _uiState.value.copy(
                    isSearching = false,
                    query = query,
                    results = results,
                    hasResults = results.isNotEmpty()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSearching = false,
                    error = e.message
                )
            }
        }
    }

    fun clearSearch() {
        _uiState.value = SearchUiState()
    }

    fun startVoiceSearch() {
        viewModelScope.launch {
            try {
                voiceSearchManager.startVoiceSearch()
                _uiState.value = _uiState.value.copy(isListeningForVoice = true)
            } catch (e: Exception) {
                _events.emit(SearchEvent.Error("Voice search not available"))
            }
        }
    }

    data class SearchUiState(
        val isSearching: Boolean = false,
        val isListeningForVoice: Boolean = false,
        val query: String = "",
        val results: List<com.mtlc.studyplan.settings.search.SearchResult> = emptyList(),
        val hasResults: Boolean = false,
        val error: String? = null
    )

    sealed class SearchEvent {
        data class NavigateToSetting(val settingKey: String) : SearchEvent()
        data class Error(val message: String) : SearchEvent()
    }
}

class SettingsBackupViewModel(
    private val backupManager: SettingsBackupManager,
    private val accessibilityManager: AccessibilityManager,
    private val animationCoordinator: SettingsAnimationCoordinator
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<BackupEvent>()
    val events: SharedFlow<BackupEvent> = _events.asSharedFlow()

    fun exportSettings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true)

            try {
                val result = backupManager.exportSettings()
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    lastExportTime = System.currentTimeMillis()
                )
                _events.emit(BackupEvent.ExportComplete(result.filePath))
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isExporting = false)
                _events.emit(BackupEvent.Error("Export failed: ${e.message}"))
            }
        }
    }

    fun importSettings(filePath: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isImporting = true)

            try {
                val result = backupManager.importSettings(filePath)
                _uiState.value = _uiState.value.copy(isImporting = false)

                if (result.conflicts.isNotEmpty()) {
                    _events.emit(BackupEvent.ConflictsDetected(result.conflicts))
                } else {
                    _events.emit(BackupEvent.ImportComplete)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isImporting = false)
                _events.emit(BackupEvent.Error("Import failed: ${e.message}"))
            }
        }
    }

    data class BackupUiState(
        val isExporting: Boolean = false,
        val isImporting: Boolean = false,
        val lastExportTime: Long? = null,
        val lastImportTime: Long? = null
    )

    sealed class BackupEvent {
        data class ExportComplete(val filePath: String) : BackupEvent()
        object ImportComplete : BackupEvent()
        data class ConflictsDetected(val conflicts: List<SettingsBackupManager.SettingConflict>) : BackupEvent()
        data class Error(val message: String) : BackupEvent()
    }
}

class AdvancedToggleViewModel(
    private val settingsRepository: SettingsRepository,
    private val accessibilityManager: AccessibilityManager,
    private val animationCoordinator: SettingsAnimationCoordinator
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdvancedToggleUiState())
    val uiState: StateFlow<AdvancedToggleUiState> = _uiState.asStateFlow()

    fun <T> updateSetting(key: String, value: T) {
        viewModelScope.launch {
            try {
                val validationResult = settingsRepository.validateSetting(key, value)
                if (validationResult.isValid) {
                    settingsRepository.setSetting(key, value)
                } else {
                    _uiState.value = _uiState.value.copy(
                        validationErrors = validationResult.errors
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message
                )
            }
        }
    }

    data class AdvancedToggleUiState(
        val validationErrors: List<String> = emptyList(),
        val error: String? = null
    )
}

class AccessibilityViewModel(
    private val accessibilityManager: AccessibilityManager,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccessibilityUiState())
    val uiState: StateFlow<AccessibilityUiState> = _uiState.asStateFlow()

    init {
        observeAccessibilityChanges()
    }

    private fun observeAccessibilityChanges() {
        viewModelScope.launch {
            accessibilityManager.accessibilityState.collect { state ->
                _uiState.value = _uiState.value.copy(accessibilityState = state)
            }
        }
    }

    data class AccessibilityUiState(
        val accessibilityState: AccessibilityManager.AccessibilityState = AccessibilityManager.AccessibilityState()
    )
}

class PerformanceViewModel(
    private val performanceMonitor: SettingsPerformanceMonitor,
    private val settingsRepository: SettingsRepository,
    private val accessibilityManager: AccessibilityManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PerformanceUiState())
    val uiState: StateFlow<PerformanceUiState> = _uiState.asStateFlow()

    init {
        startMonitoring()
    }

    private fun startMonitoring() {
        viewModelScope.launch {
            performanceMonitor.performanceState.collect { state ->
                _uiState.value = _uiState.value.copy(performanceState = state)
            }
        }
    }

    data class PerformanceUiState(
        val performanceState: SettingsPerformanceMonitor.PerformanceState = SettingsPerformanceMonitor.PerformanceState()
    )
}