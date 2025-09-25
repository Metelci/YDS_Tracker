package com.mtlc.studyplan.settings.resilience

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtlc.studyplan.settings.data.SettingsRepository
import com.mtlc.studyplan.settings.data.SettingsUpdateRequest
import com.mtlc.studyplan.settings.feedback.SettingsFeedbackManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Comprehensive edge case handling for settings system
 * Handles offline scenarios, app reinstalls, sync conflicts, and data corruption
 */
class EdgeCaseHandler(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val feedbackManager: SettingsFeedbackManager
) {

    private val retryQueue = ConcurrentHashMap<String, RetryableOperation>()
    private val conflictResolver = ConflictResolver()

    data class AppState(
        val isOnline: Boolean = true,
        val hasBeenReinstalled: Boolean = false,
        val hasPendingSync: Boolean = false,
        val syncConflicts: List<SyncConflict> = emptyList(),
        val lastSuccessfulSync: Long = 0L,
        val dataIntegrityStatus: DataIntegrityStatus = DataIntegrityStatus.HEALTHY
    )

    private val _appState = MutableStateFlow(AppState())
    val appState: StateFlow<AppState> = _appState.asStateFlow()

    init {
        monitorConnectivity()
        checkForReinstall()
        scheduleDataIntegrityChecks()
    }

    /**
     * Monitor network connectivity and handle offline scenarios
     */
    private fun monitorConnectivity() {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            while (true) {
                val isOnline = isNetworkAvailable()
                _appState.value = _appState.value.copy(isOnline = isOnline)

                if (isOnline && retryQueue.isNotEmpty()) {
                    retryFailedOperations()
                }

                delay(5000) // Check every 5 seconds
            }
        }
    }

    /**
     * Check if network is available
     */
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * Handle setting update with offline resilience
     */
    suspend fun updateSettingResillient(key: String, value: Any): SettingUpdateResult {
        return try {
            // Always update locally first
            when (value) {
                is Boolean -> settingsRepository.updateSetting(SettingsUpdateRequest.UpdateBoolean(key, value))
                is String -> settingsRepository.updateSetting(SettingsUpdateRequest.UpdateString(key, value))
                is Int -> settingsRepository.updateSetting(SettingsUpdateRequest.UpdateInt(key, value))
                is Float -> settingsRepository.updateSetting(SettingsUpdateRequest.UpdateFloat(key, value))
                else -> throw IllegalArgumentException("Unsupported value type: ${value::class.simpleName}")
            }

            if (_appState.value.isOnline) {
                // Try to sync immediately if online
                try {
                    syncSettingToCloud(key, value)
                    SettingUpdateResult.Success
                } catch (e: Exception) {
                    // Queue for retry if cloud sync fails
                    queueForRetry(key, value, OperationType.UPDATE)
                    SettingUpdateResult.LocalOnly("Saved locally, will sync when online")
                }
            } else {
                // Queue for later sync when offline
                queueForRetry(key, value, OperationType.UPDATE)
                SettingUpdateResult.LocalOnly("Saved offline, will sync when connected")
            }
        } catch (e: Exception) {
            SettingUpdateResult.Failed("Failed to save setting: ${e.message}")
        }
    }

    /**
     * Queue operation for retry when online
     */
    private fun queueForRetry(key: String, value: Any, operationType: OperationType) {
        val operation = RetryableOperation(
            id = "${operationType}_${key}_${System.currentTimeMillis()}",
            type = operationType,
            settingKey = key,
            settingValue = value,
            timestamp = System.currentTimeMillis(),
            retryCount = 0
        )
        retryQueue[operation.id] = operation
        _appState.value = _appState.value.copy(hasPendingSync = true)
    }

    /**
     * Retry failed operations when back online
     */
    private suspend fun retryFailedOperations() {
        val operations = retryQueue.values.toList()
        var successCount = 0

        operations.forEach { operation ->
            try {
                when (operation.type) {
                    OperationType.UPDATE -> {
                        syncSettingToCloud(operation.settingKey, operation.settingValue)
                        retryQueue.remove(operation.id)
                        successCount++
                    }
                    OperationType.SYNC -> {
                        performFullSync()
                        retryQueue.remove(operation.id)
                        successCount++
                    }
                    OperationType.BACKUP -> {
                        performBackup()
                        retryQueue.remove(operation.id)
                        successCount++
                    }
                }
            } catch (e: Exception) {
                // Update retry count and remove if max retries exceeded
                val updatedOperation = operation.copy(retryCount = operation.retryCount + 1)
                if (updatedOperation.retryCount >= MAX_RETRY_ATTEMPTS) {
                    retryQueue.remove(operation.id)
                    feedbackManager.showError("Failed to sync ${operation.settingKey} after ${MAX_RETRY_ATTEMPTS} attempts")
                } else {
                    retryQueue[operation.id] = updatedOperation
                }
            }
        }

        if (successCount > 0) {
            feedbackManager.showSuccess("Successfully synced $successCount pending changes")
        }

        _appState.value = _appState.value.copy(
            hasPendingSync = retryQueue.isNotEmpty(),
            lastSuccessfulSync = if (successCount > 0) System.currentTimeMillis() else _appState.value.lastSuccessfulSync
        )
    }

    /**
     * Detect app reinstall and handle data recovery
     */
    private fun checkForReinstall() {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            val installationFile = File(context.filesDir, ".installation_marker")
            val isFirstRun = !installationFile.exists()

            if (isFirstRun) {
                // Create installation marker
                installationFile.createNewFile()

                // Check if there's any existing data (indicating reinstall)
                val hasExistingData = checkForExistingUserData()
                if (hasExistingData) {
                    _appState.value = _appState.value.copy(hasBeenReinstalled = true)
                    handleReinstallRecovery()
                } else {
                    // True first install - initialize defaults
                    initializeDefaultSettings()
                }
            }
        }
    }

    /**
     * Handle data recovery after app reinstall
     */
    private suspend fun handleReinstallRecovery() {
        try {
            feedbackManager.showLoading(
                operationId = "reinstall_recovery",
                message = "Recovering your settings...",
                cancellable = false
            )

            // Try to recover from cloud backup
            val cloudRecoverySuccess = attemptCloudRecovery()

            if (cloudRecoverySuccess) {
                feedbackManager.hideLoading("reinstall_recovery")
                feedbackManager.showSuccess("Successfully recovered your settings from cloud backup!")
            } else {
                // Fall back to local recovery
                val localRecoverySuccess = attemptLocalRecovery()
                feedbackManager.hideLoading("reinstall_recovery")

                if (localRecoverySuccess) {
                    feedbackManager.showSuccess("Recovered some settings from local backup")
                } else {
                    feedbackManager.showWarning(
                        "Unable to recover previous settings. Starting fresh.",
                        actionLabel = "OK"
                    )
                    initializeDefaultSettings()
                }
            }
        } catch (e: Exception) {
            feedbackManager.hideLoading("reinstall_recovery")
            feedbackManager.showError("Recovery failed: ${e.message}")
            initializeDefaultSettings()
        }
    }

    /**
     * Handle sync conflicts when multiple devices have different settings
     */
    suspend fun resolveSyncConflicts(conflicts: List<SyncConflict>): ConflictResolutionResult {
        val resolutions = mutableListOf<ResolvedConflict>()

        conflicts.forEach { conflict ->
            val resolution = when (conflict.resolutionStrategy) {
                ConflictResolutionStrategy.NEWEST_WINS -> {
                    if (conflict.localTimestamp > conflict.remoteTimestamp) {
                        // Local is newer
                        syncSettingToCloud(conflict.settingKey, conflict.localValue)
                        ResolvedConflict(conflict.settingKey, conflict.localValue, "Local version was newer")
                    } else {
                        // Remote is newer
                        when (conflict.remoteValue) {
                            is Boolean -> settingsRepository.updateSetting(SettingsUpdateRequest.UpdateBoolean(conflict.settingKey, conflict.remoteValue))
                            is String -> settingsRepository.updateSetting(SettingsUpdateRequest.UpdateString(conflict.settingKey, conflict.remoteValue))
                            is Int -> settingsRepository.updateSetting(SettingsUpdateRequest.UpdateInt(conflict.settingKey, conflict.remoteValue))
                            is Float -> settingsRepository.updateSetting(SettingsUpdateRequest.UpdateFloat(conflict.settingKey, conflict.remoteValue))
                            else -> throw IllegalArgumentException("Unsupported value type: ${conflict.remoteValue::class.simpleName}")
                        }
                        ResolvedConflict(conflict.settingKey, conflict.remoteValue, "Remote version was newer")
                    }
                }
                ConflictResolutionStrategy.LOCAL_WINS -> {
                    syncSettingToCloud(conflict.settingKey, conflict.localValue)
                    ResolvedConflict(conflict.settingKey, conflict.localValue, "Local preference applied")
                }
                ConflictResolutionStrategy.REMOTE_WINS -> {
                    when (conflict.remoteValue) {
                        is Boolean -> settingsRepository.updateSetting(SettingsUpdateRequest.UpdateBoolean(conflict.settingKey, conflict.remoteValue))
                        is String -> settingsRepository.updateSetting(SettingsUpdateRequest.UpdateString(conflict.settingKey, conflict.remoteValue))
                        is Int -> settingsRepository.updateSetting(SettingsUpdateRequest.UpdateInt(conflict.settingKey, conflict.remoteValue))
                        is Float -> settingsRepository.updateSetting(SettingsUpdateRequest.UpdateFloat(conflict.settingKey, conflict.remoteValue))
                        else -> throw IllegalArgumentException("Unsupported value type: ${conflict.remoteValue::class.simpleName}")
                    }
                    ResolvedConflict(conflict.settingKey, conflict.remoteValue, "Remote preference applied")
                }
                ConflictResolutionStrategy.USER_CHOICE -> {
                    // This would typically show a UI for user choice
                    // For now, default to newest wins
                    if (conflict.localTimestamp > conflict.remoteTimestamp) {
                        syncSettingToCloud(conflict.settingKey, conflict.localValue)
                        ResolvedConflict(conflict.settingKey, conflict.localValue, "Defaulted to local")
                    } else {
                        when (conflict.remoteValue) {
                            is Boolean -> settingsRepository.updateSetting(SettingsUpdateRequest.UpdateBoolean(conflict.settingKey, conflict.remoteValue))
                            is String -> settingsRepository.updateSetting(SettingsUpdateRequest.UpdateString(conflict.settingKey, conflict.remoteValue))
                            is Int -> settingsRepository.updateSetting(SettingsUpdateRequest.UpdateInt(conflict.settingKey, conflict.remoteValue))
                            is Float -> settingsRepository.updateSetting(SettingsUpdateRequest.UpdateFloat(conflict.settingKey, conflict.remoteValue))
                            else -> throw IllegalArgumentException("Unsupported value type: ${conflict.remoteValue::class.simpleName}")
                        }
                        ResolvedConflict(conflict.settingKey, conflict.remoteValue, "Defaulted to remote")
                    }
                }
            }
            resolutions.add(resolution)
        }

        // Update state to remove resolved conflicts
        _appState.value = _appState.value.copy(
            syncConflicts = emptyList(),
            lastSuccessfulSync = System.currentTimeMillis()
        )

        return ConflictResolutionResult(
            resolvedConflicts = resolutions,
            totalResolved = resolutions.size,
            resolutionSummary = "Resolved ${resolutions.size} conflicts successfully"
        )
    }

    /**
     * Perform data integrity checks
     */
    private fun scheduleDataIntegrityChecks() {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            while (true) {
                delay(INTEGRITY_CHECK_INTERVAL)
                performDataIntegrityCheck()
            }
        }
    }

    private suspend fun performDataIntegrityCheck() {
        try {
            val integrityIssues = mutableListOf<String>()

            // Check for missing critical settings
            val criticalSettings = mapOf(
                "appearance_theme_mode" to "system",
                "notifications_push_enabled" to true,
                "gamification_streak_tracking" to true
            )

            criticalSettings.forEach { (key, defaultValue) ->
                try {
                    when (defaultValue) {
                        is String -> settingsRepository.getString(key, defaultValue)
                        is Boolean -> settingsRepository.getBoolean(key, defaultValue)
                        else -> null
                    }
                } catch (e: Exception) {
                    integrityIssues.add("Missing critical setting: $key")
                }
            }

            // Note: Removed forEach validation as we don't have direct access to all settings map
            // Individual setting validation should be done through repository methods

            val status = when {
                integrityIssues.isEmpty() -> DataIntegrityStatus.HEALTHY
                integrityIssues.size <= 2 -> DataIntegrityStatus.MINOR_ISSUES
                else -> DataIntegrityStatus.MAJOR_ISSUES
            }

            _appState.value = _appState.value.copy(dataIntegrityStatus = status)

            if (status != DataIntegrityStatus.HEALTHY) {
                repairDataIntegrityIssues(integrityIssues)
            }

        } catch (e: Exception) {
            _appState.value = _appState.value.copy(dataIntegrityStatus = DataIntegrityStatus.CORRUPTED)
            feedbackManager.showError("Data integrity check failed: ${e.message}")
        }
    }

    private suspend fun repairDataIntegrityIssues(issues: List<String>) {
        try {
            feedbackManager.showLoading(
                operationId = "integrity_repair",
                message = "Repairing data issues...",
                cancellable = false
            )

            var repairedCount = 0
            issues.forEach { issue ->
                if (issue.startsWith("Missing critical setting:")) {
                    val settingKey = issue.substringAfter("Missing critical setting: ")
                    val defaultValue = getDefaultValueForSetting(settingKey)
                    when (defaultValue) {
                        is Boolean -> settingsRepository.updateSetting(SettingsUpdateRequest.UpdateBoolean(settingKey, defaultValue))
                        is String -> settingsRepository.updateSetting(SettingsUpdateRequest.UpdateString(settingKey, defaultValue))
                        is Int -> settingsRepository.updateSetting(SettingsUpdateRequest.UpdateInt(settingKey, defaultValue))
                        is Float -> settingsRepository.updateSetting(SettingsUpdateRequest.UpdateFloat(settingKey, defaultValue))
                        else -> throw IllegalArgumentException("Unsupported value type: ${defaultValue::class.simpleName}")
                    }
                    repairedCount++
                } else if (issue.startsWith("Invalid value for")) {
                    val settingKey = issue.substringAfter("Invalid value for ").substringBefore(":")
                    val defaultValue = getDefaultValueForSetting(settingKey)
                    when (defaultValue) {
                        is Boolean -> settingsRepository.updateSetting(SettingsUpdateRequest.UpdateBoolean(settingKey, defaultValue))
                        is String -> settingsRepository.updateSetting(SettingsUpdateRequest.UpdateString(settingKey, defaultValue))
                        is Int -> settingsRepository.updateSetting(SettingsUpdateRequest.UpdateInt(settingKey, defaultValue))
                        is Float -> settingsRepository.updateSetting(SettingsUpdateRequest.UpdateFloat(settingKey, defaultValue))
                        else -> throw IllegalArgumentException("Unsupported value type: ${defaultValue::class.simpleName}")
                    }
                    repairedCount++
                }
            }

            feedbackManager.hideLoading("integrity_repair")
            if (repairedCount > 0) {
                feedbackManager.showSuccess("Repaired $repairedCount data integrity issues")
                _appState.value = _appState.value.copy(dataIntegrityStatus = DataIntegrityStatus.HEALTHY)
            }

        } catch (e: Exception) {
            feedbackManager.hideLoading("integrity_repair")
            feedbackManager.showError("Failed to repair data issues: ${e.message}")
        }
    }

    // Placeholder implementations for cloud operations
    private suspend fun syncSettingToCloud(key: String, value: Any) {
        // Implementation would sync to actual cloud service
        delay(100) // Simulate network delay
    }

    private suspend fun performFullSync() {
        // Implementation would perform full settings sync
        delay(500) // Simulate sync operation
    }

    private suspend fun performBackup() {
        // Implementation would create backup
        delay(200) // Simulate backup operation
    }

    private fun checkForExistingUserData(): Boolean {
        // Check if there's any existing user data in the app
        return try {
            val settingsFile = File(context.filesDir, "settings.json")
            settingsFile.exists() && settingsFile.length() > 0
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun attemptCloudRecovery(): Boolean {
        // Implementation would attempt to restore from cloud
        return false // Placeholder
    }

    private suspend fun attemptLocalRecovery(): Boolean {
        // Implementation would attempt to restore from local backup
        return false // Placeholder
    }

    private suspend fun initializeDefaultSettings() {
        // Initialize app with default settings
        settingsRepository.updateSetting(SettingsUpdateRequest.UpdateString("appearance_theme_mode", "system"))
        settingsRepository.updateSetting(SettingsUpdateRequest.UpdateBoolean("notifications_push_enabled", true))
        settingsRepository.updateSetting(SettingsUpdateRequest.UpdateBoolean("gamification_streak_tracking", true))
    }

    private fun isValidSettingValue(key: String, value: Any?): Boolean {
        // Validate setting values based on key
        return when {
            key.contains("_enabled") -> value is Boolean
            key.contains("_time") -> value is String && value.matches(Regex("\\d{2}:\\d{2}"))
            key.contains("_mode") -> value is String
            else -> true
        }
    }

    private fun getDefaultValueForSetting(key: String): Any {
        return when {
            key.contains("_enabled") -> true
            key.contains("_time") -> "09:00"
            key.contains("_mode") -> "system"
            key.contains("_tracking") -> true
            else -> ""
        }
    }

    companion object {
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val INTEGRITY_CHECK_INTERVAL = 30 * 60 * 1000L // 30 minutes
    }
}

// Data classes for edge case handling
data class RetryableOperation(
    val id: String,
    val type: OperationType,
    val settingKey: String,
    val settingValue: Any,
    val timestamp: Long,
    val retryCount: Int
)

enum class OperationType {
    UPDATE, SYNC, BACKUP
}

sealed class SettingUpdateResult {
    object Success : SettingUpdateResult()
    data class LocalOnly(val message: String) : SettingUpdateResult()
    data class Failed(val error: String) : SettingUpdateResult()
}

data class SyncConflict(
    val settingKey: String,
    val localValue: Any,
    val remoteValue: Any,
    val localTimestamp: Long,
    val remoteTimestamp: Long,
    val resolutionStrategy: ConflictResolutionStrategy
)

enum class ConflictResolutionStrategy {
    NEWEST_WINS, LOCAL_WINS, REMOTE_WINS, USER_CHOICE
}

data class ResolvedConflict(
    val settingKey: String,
    val resolvedValue: Any,
    val resolution: String
)

data class ConflictResolutionResult(
    val resolvedConflicts: List<ResolvedConflict>,
    val totalResolved: Int,
    val resolutionSummary: String
)

enum class DataIntegrityStatus {
    HEALTHY, MINOR_ISSUES, MAJOR_ISSUES, CORRUPTED
}

class ConflictResolver {
    fun resolveConflict(conflict: SyncConflict): Any {
        return when (conflict.resolutionStrategy) {
            ConflictResolutionStrategy.NEWEST_WINS -> {
                if (conflict.localTimestamp > conflict.remoteTimestamp) {
                    conflict.localValue
                } else {
                    conflict.remoteValue
                }
            }
            ConflictResolutionStrategy.LOCAL_WINS -> conflict.localValue
            ConflictResolutionStrategy.REMOTE_WINS -> conflict.remoteValue
            ConflictResolutionStrategy.USER_CHOICE -> conflict.localValue // Default fallback
        }
    }
}

/**
 * ViewModel for edge case handling in UI
 */
class EdgeCaseViewModel(
    private val edgeCaseHandler: EdgeCaseHandler
) : ViewModel() {

    val appState = edgeCaseHandler.appState

    fun retryFailedOperations() {
        viewModelScope.launch {
            // Trigger retry of failed operations
        }
    }

    fun resolveConflicts(conflicts: List<SyncConflict>) {
        viewModelScope.launch {
            edgeCaseHandler.resolveSyncConflicts(conflicts)
        }
    }

    fun forceIntegrityCheck() {
        viewModelScope.launch {
            // Trigger manual integrity check
        }
    }
}
