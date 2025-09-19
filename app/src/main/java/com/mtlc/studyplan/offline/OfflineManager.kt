package com.mtlc.studyplan.offline

import android.content.Context
import com.mtlc.studyplan.eventbus.AppEvent
import com.mtlc.studyplan.eventbus.AppEventBus
import com.mtlc.studyplan.network.NetworkMonitor
import com.mtlc.studyplan.settings.manager.SettingsManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineManager @Inject constructor(
    private val context: Context,
    private val networkMonitor: NetworkMonitor,
    private val settingsManager: SettingsManager,
    private val appEventBus: AppEventBus,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    private val _isOfflineModeEnabled = MutableStateFlow(false)
    val isOfflineModeEnabled: StateFlow<Boolean> = _isOfflineModeEnabled.asStateFlow()

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _pendingActions = MutableStateFlow<List<OfflineAction>>(emptyList())
    val pendingActions: StateFlow<List<OfflineAction>> = _pendingActions.asStateFlow()

    private val _syncStatus = MutableStateFlow(SyncStatus.IDLE)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val scope = CoroutineScope(ioDispatcher + SupervisorJob())

    init {
        observeNetworkChanges()
        observeSettingsChanges()
        loadPendingActions()
    }

    fun configure(enabled: Boolean, autoSync: Boolean) {
        _isOfflineModeEnabled.value = enabled

        if (enabled) {
            scope.launch {
                cacheEssentialData()
            }

            if (autoSync && _isOnline.value) {
                scope.launch {
                    syncPendingActions()
                }
            }
        }
    }

    private fun observeNetworkChanges() {
        scope.launch {
            networkMonitor.isOnline.collect { online ->
                val wasOffline = !_isOnline.value
                _isOnline.value = online

                if (online && wasOffline) {
                    val settings = settingsManager.currentSettings.value
                    if (settings.offlineModeEnabled && settings.autoSyncEnabled) {
                        syncPendingActions()
                    }

                    appEventBus.emitEvent(AppEvent.NetworkConnected)
                } else if (!online) {
                    appEventBus.emitEvent(AppEvent.NetworkDisconnected)
                }
            }
        }
    }

    private fun observeSettingsChanges() {
        scope.launch {
            settingsManager.currentSettings.collect { settings ->
                configure(settings.offlineModeEnabled, settings.autoSyncEnabled)
            }
        }
    }

    private suspend fun cacheEssentialData() {
        try {
            _syncStatus.value = SyncStatus.CACHING

            // In a real implementation, this would cache data from remote APIs
            // For now, we'll simulate successful caching
            kotlinx.coroutines.delay(1000) // Simulate network call

            _syncStatus.value = SyncStatus.CACHED

        } catch (e: Exception) {
            _syncStatus.value = SyncStatus.ERROR
        }
    }

    suspend fun queueOfflineAction(action: OfflineAction) {
        // In a real implementation, this would save to local database
        val currentActions = _pendingActions.value.toMutableList()
        currentActions.add(action)
        _pendingActions.value = currentActions

        applyActionLocally(action)
    }

    private suspend fun applyActionLocally(action: OfflineAction) {
        when (action.type) {
            OfflineActionType.TASK_COMPLETED -> {
                // Apply task completion locally
                val taskId = action.data["taskId"] as? String
                appEventBus.emitEvent(AppEvent.TaskCompleted(taskId ?: ""))
            }

            OfflineActionType.TASK_CREATED -> {
                // Apply task creation locally
                appEventBus.emitEvent(AppEvent.TaskCreated)
            }

            OfflineActionType.PROGRESS_UPDATED -> {
                // Apply progress update locally
                appEventBus.emitEvent(AppEvent.ProgressUpdated)
            }

            OfflineActionType.SETTINGS_UPDATED -> {
                // Settings are already handled by SettingsManager
            }
        }
    }

    suspend fun syncPendingActions(): Result<SyncResult> {
        if (!_isOnline.value) {
            return Result.failure(Exception("No internet connection"))
        }

        return try {
            _syncStatus.value = SyncStatus.SYNCING

            val actions = _pendingActions.value
            val results = mutableListOf<ActionSyncResult>()

            actions.forEach { action ->
                try {
                    // Simulate API call
                    kotlinx.coroutines.delay(200)

                    when (action.type) {
                        OfflineActionType.TASK_COMPLETED -> {
                            syncTaskCompletion(action)
                        }
                        OfflineActionType.TASK_CREATED -> {
                            syncTaskCreation(action)
                        }
                        OfflineActionType.PROGRESS_UPDATED -> {
                            syncProgressUpdate(action)
                        }
                        OfflineActionType.SETTINGS_UPDATED -> {
                            syncSettingsUpdate(action)
                        }
                    }

                    results.add(ActionSyncResult.Success(action.id))

                } catch (e: Exception) {
                    results.add(ActionSyncResult.Failure(action.id, e))
                }
            }

            // Remove successfully synced actions
            val successfulActionIds = results.filterIsInstance<ActionSyncResult.Success>()
                .map { it.actionId }.toSet()

            _pendingActions.value = _pendingActions.value.filter {
                it.id !in successfulActionIds
            }

            _syncStatus.value = SyncStatus.SYNCED

            val syncResult = SyncResult(
                totalActions = actions.size,
                successfulActions = results.count { it is ActionSyncResult.Success },
                failedActions = results.count { it is ActionSyncResult.Failure },
                results = results
            )

            appEventBus.emitEvent(AppEvent.SyncCompleted(syncResult))

            Result.success(syncResult)

        } catch (e: Exception) {
            _syncStatus.value = SyncStatus.ERROR
            Result.failure(e)
        }
    }

    private suspend fun syncTaskCompletion(action: OfflineAction) {
        // Simulate API call to sync task completion
        kotlinx.coroutines.delay(100)
    }

    private suspend fun syncTaskCreation(action: OfflineAction) {
        // Simulate API call to sync task creation
        kotlinx.coroutines.delay(100)
    }

    private suspend fun syncProgressUpdate(action: OfflineAction) {
        // Simulate API call to sync progress update
        kotlinx.coroutines.delay(100)
    }

    private suspend fun syncSettingsUpdate(action: OfflineAction) {
        // Simulate API call to sync settings update
        kotlinx.coroutines.delay(100)
    }

    private fun loadPendingActions() {
        // In a real implementation, this would load from local database
        // For now, we start with empty list
        _pendingActions.value = emptyList()
    }

    suspend fun completeTaskOffline(taskId: String, actualMinutes: Int): Result<String> {
        return if (_isOnline.value && !_isOfflineModeEnabled.value) {
            // Online mode - would make direct API call
            completeTaskOnline(taskId, actualMinutes)
        } else {
            // Offline mode - queue action
            val action = OfflineAction(
                id = UUID.randomUUID().toString(),
                type = OfflineActionType.TASK_COMPLETED,
                data = mapOf(
                    "taskId" to taskId,
                    "actualMinutes" to actualMinutes
                ),
                timestamp = System.currentTimeMillis()
            )

            queueOfflineAction(action)
            Result.success("Task completed offline")
        }
    }

    private suspend fun completeTaskOnline(taskId: String, actualMinutes: Int): Result<String> {
        return try {
            // Simulate API call
            kotlinx.coroutines.delay(500)
            Result.success("Task completed online")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createTaskOffline(taskData: Map<String, Any>): Result<String> {
        return if (_isOnline.value && !_isOfflineModeEnabled.value) {
            // Online mode
            createTaskOnline(taskData)
        } else {
            // Offline mode
            val action = OfflineAction(
                id = UUID.randomUUID().toString(),
                type = OfflineActionType.TASK_CREATED,
                data = taskData,
                timestamp = System.currentTimeMillis()
            )

            queueOfflineAction(action)
            Result.success("Task created offline")
        }
    }

    private suspend fun createTaskOnline(taskData: Map<String, Any>): Result<String> {
        return try {
            // Simulate API call
            kotlinx.coroutines.delay(500)
            Result.success("Task created online")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun isOperatingOffline(): Boolean {
        return !_isOnline.value || _isOfflineModeEnabled.value
    }

    fun getPendingActionsCount(): Int {
        return _pendingActions.value.size
    }
}

@Serializable
data class OfflineAction(
    val id: String,
    val type: OfflineActionType,
    val data: Map<String, Any>,
    val timestamp: Long,
    val retryCount: Int = 0
)

enum class OfflineActionType {
    TASK_COMPLETED,
    TASK_CREATED,
    TASK_UPDATED,
    TASK_DELETED,
    PROGRESS_UPDATED,
    SETTINGS_UPDATED,
    ACHIEVEMENT_UNLOCKED
}

enum class SyncStatus {
    IDLE,
    CACHING,
    CACHED,
    SYNCING,
    SYNCED,
    ERROR
}

data class SyncResult(
    val totalActions: Int,
    val successfulActions: Int,
    val failedActions: Int,
    val results: List<ActionSyncResult>
)

sealed class ActionSyncResult {
    data class Success(val actionId: String) : ActionSyncResult()
    data class Failure(val actionId: String, val error: Throwable) : ActionSyncResult()
}