package com.mtlc.studyplan.settings.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mtlc.studyplan.core.error.AppError
import com.mtlc.studyplan.settings.backup.SettingsBackupManager
import com.mtlc.studyplan.settings.data.SettingsRepository
import com.mtlc.studyplan.settings.sync.CloudSyncManager
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

/**
 * ViewModel for backup and sync settings management
 */
class BackupSettingsViewModel(
    private val context: Context,
    private val repository: SettingsRepository,
    private val backupManager: SettingsBackupManager,
    private val cloudSyncManager: CloudSyncManager
) : ViewModel() {

    data class BackupUiState(
        val isExporting: Boolean = false,
        val isImporting: Boolean = false,
        val isSyncing: Boolean = false,
        val exportProgress: Float = 0f,
        val importProgress: Float = 0f,
        val syncProgress: Float = 0f,
        val lastBackupDate: Date? = null,
        val lastSyncDate: Date? = null,
        val backupSize: Long = 0L,
        val isCloudSyncConfigured: Boolean = false,
        val isCloudSyncEnabled: Boolean = false,
        val error: AppError? = null,
        val pendingConflicts: List<SettingsBackupManager.SettingConflict> = emptyList()
    )

    sealed class BackupEvent {
        object ExportSuccess : BackupEvent()
        data class ImportSuccess(val result: SettingsBackupManager.ImportResult) : BackupEvent()
        data class SyncSuccess(val result: CloudSyncManager.SyncResult) : BackupEvent()
        data class ImportConflicts(val conflicts: List<SettingsBackupManager.SettingConflict>) : BackupEvent()
    }

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<BackupEvent>()
    val events: SharedFlow<BackupEvent> = _events.asSharedFlow()

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        handleError(exception)
    }

    init {
        observeBackupState()
        observeCloudSyncState()
        loadInitialState()
    }

    /**
     * Observe backup manager state
     */
    private fun observeBackupState() {
        viewModelScope.launch {
            backupManager.backupState.collect { backupState ->
                _uiState.value = _uiState.value.copy(
                    isExporting = backupState.isExporting,
                    isImporting = backupState.isImporting,
                    exportProgress = backupState.exportProgress,
                    importProgress = backupState.importProgress,
                    lastBackupDate = backupState.lastBackupDate,
                    backupSize = backupState.backupSize,
                    error = backupState.error
                )
            }
        }
    }

    /**
     * Observe cloud sync state
     */
    private fun observeCloudSyncState() {
        viewModelScope.launch {
            cloudSyncManager.syncState.collect { syncState ->
                _uiState.value = _uiState.value.copy(
                    isSyncing = syncState.isSyncing,
                    syncProgress = syncState.syncProgress,
                    lastSyncDate = syncState.lastSyncDate,
                    isCloudSyncConfigured = syncState.isConfigured,
                    isCloudSyncEnabled = syncState.isEnabled
                )
            }
        }
    }

    /**
     * Load initial state
     */
    private fun loadInitialState() {
        viewModelScope.launch(exceptionHandler) {
            // Load last backup information from preferences
            loadBackupHistory()
        }
    }

    /**
     * Export settings to file
     */
    fun exportSettings(uri: Uri) {
        viewModelScope.launch(exceptionHandler) {
            try {
                val result = backupManager.exportSettings(uri)
                if (result.isSuccess) {
                    _events.emit(BackupEvent.ExportSuccess)
                    saveBackupHistory(uri, Date())
                } else {
                    result.exceptionOrNull()?.let { throw it }
                }
            } catch (exception: Exception) {
                handleError(exception)
            }
        }
    }

    /**
     * Import settings from file
     */
    fun importSettings(uri: Uri, strategy: SettingsBackupManager.MergeStrategy) {
        viewModelScope.launch(exceptionHandler) {
            try {
                val result = backupManager.importSettings(uri, strategy)
                if (result.isSuccess) {
                    val importResult = result.getOrThrow()

                    if (importResult.conflicts.isNotEmpty()) {
                        _events.emit(BackupEvent.ImportConflicts(importResult.conflicts))
                    } else {
                        _events.emit(BackupEvent.ImportSuccess(importResult))
                    }
                } else {
                    result.exceptionOrNull()?.let { throw it }
                }
            } catch (exception: Exception) {
                handleError(exception)
            }
        }
    }

    /**
     * Configure cloud sync provider
     */
    fun configureCloudSync(provider: CloudSyncManager.SyncProvider) {
        viewModelScope.launch(exceptionHandler) {
            try {
                val result = cloudSyncManager.configureProvider(provider)
                if (result.isFailure) {
                    result.exceptionOrNull()?.let { throw it }
                }
            } catch (exception: Exception) {
                handleError(exception)
            }
        }
    }

    /**
     * Toggle automatic sync
     */
    fun toggleAutoSync(enabled: Boolean) {
        viewModelScope.launch(exceptionHandler) {
            try {
                if (enabled) {
                    val result = cloudSyncManager.enableAutoSync()
                    if (result.isFailure) {
                        result.exceptionOrNull()?.let { throw it }
                    }
                } else {
                    // Disable auto sync
                    // TODO: Implement disable auto sync
                }
            } catch (exception: Exception) {
                handleError(exception)
            }
        }
    }

    /**
     * Perform manual sync
     */
    fun performManualSync() {
        viewModelScope.launch(exceptionHandler) {
            try {
                val result = cloudSyncManager.performSync()
                if (result.isSuccess) {
                    _events.emit(BackupEvent.SyncSuccess(result.getOrThrow()))
                } else {
                    result.exceptionOrNull()?.let { throw it }
                }
            } catch (exception: Exception) {
                handleError(exception)
            }
        }
    }

    /**
     * Resolve import conflicts with default strategy
     */
    fun resolveConflictsWithDefaults() {
        viewModelScope.launch(exceptionHandler) {
            try {
                val conflicts = _uiState.value.pendingConflicts

                // Apply default resolution (keep current values)
                conflicts.forEach { conflict ->
                    // TODO: Apply conflict resolution
                }

                _uiState.value = _uiState.value.copy(pendingConflicts = emptyList())
            } catch (exception: Exception) {
                handleError(exception)
            }
        }
    }

    /**
     * Clear backup history
     */
    fun clearBackupHistory() {
        viewModelScope.launch(exceptionHandler) {
            try {
                clearStoredBackupHistory()
                _uiState.value = _uiState.value.copy(
                    lastBackupDate = null,
                    backupSize = 0L
                )
            } catch (exception: Exception) {
                handleError(exception)
            }
        }
    }

    /**
     * Load backup history from storage
     */
    private suspend fun loadBackupHistory() {
        try {
            val prefs = context.getSharedPreferences("backup_history", Context.MODE_PRIVATE)

            val lastBackupTimestamp = prefs.getLong("last_backup_timestamp", 0L)
            val backupSize = prefs.getLong("last_backup_size", 0L)

            val lastBackupDate = if (lastBackupTimestamp > 0) {
                Date(lastBackupTimestamp)
            } else null

            _uiState.value = _uiState.value.copy(
                lastBackupDate = lastBackupDate,
                backupSize = backupSize
            )
        } catch (exception: Exception) {
            // Ignore errors loading history
        }
    }

    /**
     * Save backup history to storage
     */
    private fun saveBackupHistory(uri: Uri, date: Date) {
        try {
            val prefs = context.getSharedPreferences("backup_history", Context.MODE_PRIVATE)
            prefs.edit()
                .putLong("last_backup_timestamp", date.time)
                .putString("last_backup_uri", uri.toString())
                .apply()
        } catch (exception: Exception) {
            // Ignore errors saving history
        }
    }

    /**
     * Clear stored backup history
     */
    private fun clearStoredBackupHistory() {
        val prefs = context.getSharedPreferences("backup_history", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    /**
     * Handle errors
     */
    private fun handleError(exception: Throwable) {
        val appError = when (exception) {
            is AppError -> exception
            else -> AppError(
                type = com.mtlc.studyplan.core.error.ErrorType.UNKNOWN,
                message = exception.message ?: "Operation failed",
                cause = exception
            )
        }

        _uiState.value = _uiState.value.copy(error = appError)
    }

    override fun onCleared() {
        super.onCleared()
        backupManager.dispose()
        cloudSyncManager.dispose()
    }
}

/**
 * Factory for creating BackupSettingsViewModel
 */
class BackupSettingsViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BackupSettingsViewModel::class.java)) {
            val repository = SettingsRepository(context)
            val backupManager = SettingsBackupManager(context, repository)
            val cloudSyncManager = CloudSyncManager(context, repository, backupManager)
            return BackupSettingsViewModel(context, repository, backupManager, cloudSyncManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}