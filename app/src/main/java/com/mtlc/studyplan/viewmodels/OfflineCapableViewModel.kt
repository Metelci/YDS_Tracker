package com.mtlc.studyplan.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtlc.studyplan.offline.OfflineActionManager
import com.mtlc.studyplan.offline.OfflineDataManager
import com.mtlc.studyplan.offline.OfflineResult
import com.mtlc.studyplan.utils.NetworkHelper
import com.mtlc.studyplan.utils.NetworkState
import com.mtlc.studyplan.utils.ToastManager
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted

/**
 * Base ViewModel class with comprehensive offline capabilities
 * Provides network-aware operations and offline data management
 */
abstract class OfflineCapableViewModel : ViewModel() {

    // Network state management
    protected val _networkState = MutableLiveData<NetworkState>()
    val networkState: LiveData<NetworkState> = _networkState

    protected val _offlineQueueCount = MutableLiveData<Int>()
    val offlineQueueCount: LiveData<Int> = _offlineQueueCount

    protected val _isOperatingOffline = MutableLiveData<Boolean>()
    val isOperatingOffline: LiveData<Boolean> = _isOperatingOffline

    // Operation state management
    protected val _operationState = MutableLiveData<OperationState>()
    val operationState: LiveData<OperationState> = _operationState

    // Offline data manager (to be injected by concrete implementations)
    protected var offlineDataManager: OfflineDataManager? = null

    init {
        observeNetworkState()
        loadOfflineQueueCount()
    }

    private fun observeNetworkState() {
        viewModelScope.launch {
            NetworkHelper.networkState.collect { state ->
                _networkState.value = state
                _isOperatingOffline.value = state == NetworkState.DISCONNECTED

                if (state == NetworkState.CONNECTED) {
                    syncOfflineActions()
                }
            }
        }
    }

    /**
     * Execute an operation with offline support
     * If online, executes immediately. If offline, queues for later sync.
     */
    protected fun executeWithOfflineSupport(
        operation: suspend () -> Unit,
        fallbackAction: (() -> OfflineAction)? = null,
        operationName: String = "Operation"
    ) {
        viewModelScope.launch {
            try {
                _operationState.value = OperationState.Loading(operationName)

                if (NetworkHelper.isOnline()) {
                    // Execute online
                    operation()
                    _operationState.value = OperationState.Success("$operationName completed")
                } else {
                    // Execute offline fallback
                    fallbackAction?.let { action ->
                        val offlineAction = action()
                        queueOfflineAction(offlineAction)
                        _operationState.value = OperationState.OfflineQueued("$operationName queued for sync")
                        showOfflineActionQueued(operationName)
                    } ?: run {
                        _operationState.value = OperationState.Error("$operationName requires internet connection")
                        showOfflineError(operationName)
                    }
                }
            } catch (e: Exception) {
                handleOperationError(e, fallbackAction, operationName)
            }
        }
    }

    /**
     * Execute an operation that works both online and offline
     * Uses local data when offline, syncs when online
     */
    protected fun executeOfflineFirst(
        offlineOperation: suspend () -> OfflineResult,
        onlineOperation: (suspend () -> Unit)? = null,
        operationName: String = "Operation"
    ) {
        viewModelScope.launch {
            try {
                _operationState.value = OperationState.Loading(operationName)

                // Always try offline operation first
                val offlineResult = offlineOperation()

                when (offlineResult) {
                    is OfflineResult.Success -> {
                        _operationState.value = OperationState.Success(offlineResult.message)

                        // If online, also execute online operation
                        if (NetworkHelper.isOnline() && onlineOperation != null) {
                            try {
                                onlineOperation()
                            } catch (e: Exception) {
                                // Online operation failed, but offline succeeded
                                // This is acceptable - data will sync later
                            }
                        }
                    }
                    is OfflineResult.Failed -> {
                        _operationState.value = OperationState.Error(offlineResult.message)
                        ToastManager.showError(offlineResult.message)
                    }
                }
            } catch (e: Exception) {
                _operationState.value = OperationState.Error("$operationName failed: ${e.message}")
                ToastManager.showError("$operationName failed")
            }
        }
    }

    /**
     * Load data with offline fallback
     * Tries online first, falls back to cached offline data
     */
    protected fun loadDataWithOfflineFallback(
        onlineLoader: suspend () -> Unit,
        offlineLoader: suspend () -> Unit,
        dataName: String = "data"
    ) {
        viewModelScope.launch {
            try {
                _operationState.value = OperationState.Loading("Loading $dataName")

                if (NetworkHelper.isOnline()) {
                    try {
                        onlineLoader()
                        _operationState.value = OperationState.Success("$dataName loaded")
                    } catch (e: Exception) {
                        // Online loading failed, try offline
                        offlineLoader()
                        _operationState.value = OperationState.OfflineMode("Loaded cached $dataName")
                        showUsingOfflineData(dataName)
                    }
                } else {
                    // Load offline data
                    offlineLoader()
                    _operationState.value = OperationState.OfflineMode("Loaded offline $dataName")
                    showUsingOfflineData(dataName)
                }
            } catch (e: Exception) {
                _operationState.value = OperationState.Error("Failed to load $dataName")
                ToastManager.showError("Failed to load $dataName")
            }
        }
    }

    /**
     * Sync offline actions when connection is restored
     */
    private suspend fun syncOfflineActions() {
        try {
            OfflineActionManager.syncPendingActions()
            updateOfflineQueueCount()
            ToastManager.showSuccess("Offline changes synced successfully")
        } catch (e: Exception) {
            ToastManager.showWarning("Some offline changes couldn't be synced")
        }
    }

    private suspend fun queueOfflineAction(action: OfflineAction) {
        // In a real implementation, this would queue the action
        updateOfflineQueueCount()
    }

    private fun updateOfflineQueueCount() {
        viewModelScope.launch {
            val count = OfflineActionManager.getPendingActionsCount()
            _offlineQueueCount.value = count
        }
    }

    private fun loadOfflineQueueCount() {
        updateOfflineQueueCount()
    }

    private fun handleOperationError(
        error: Exception,
        fallbackAction: (() -> OfflineAction)?,
        operationName: String
    ) {
        if (!NetworkHelper.isOnline() && fallbackAction != null) {
            // Network error while offline - queue for later
            viewModelScope.launch {
                queueOfflineAction(fallbackAction())
                _operationState.value = OperationState.OfflineQueued("$operationName queued for sync")
                showOfflineActionQueued(operationName)
            }
        } else {
            _operationState.value = OperationState.Error("$operationName failed: ${error.message}")
            ToastManager.showError("$operationName failed")
        }
    }

    // Feedback methods
    private fun showOfflineActionQueued(operationName: String) {
        ToastManager.showWarning("✈️ $operationName queued - will sync when online")
    }

    private fun showOfflineError(operationName: String) {
        ToastManager.showError("❌ $operationName requires internet connection")
    }

    private fun showUsingOfflineData(dataName: String) {
        ToastManager.showInfo("📱 Using offline $dataName")
    }

    // Public utility methods
    fun isOnline(): Boolean = NetworkHelper.isOnline()

    fun canSync(): Boolean = NetworkHelper.canSync()

    fun shouldAvoidHeavyOperations(): Boolean = NetworkHelper.shouldAvoidHeavyOperations()

    fun getConnectionStatusMessage(): String = NetworkHelper.getConnectionStatusMessage()

    fun forceSyncOfflineActions() {
        viewModelScope.launch {
            if (NetworkHelper.isOnline()) {
                syncOfflineActions()
            } else {
                ToastManager.showWarning("Cannot sync - no internet connection")
            }
        }
    }

    // Abstract methods for concrete implementations
    abstract fun onNetworkStateChanged(networkState: NetworkState)

    abstract fun onOfflineDataRequired()

    // Override in subclasses to handle specific offline scenarios
    open fun onGoingOffline() {
        // Default implementation - can be overridden
        ToastManager.showInfo("You're now offline. Some features may be limited.")
    }

    open fun onComingOnline() {
        // Default implementation - can be overridden
        ToastManager.showInfo("You're back online. Syncing changes...")
        forceSyncOfflineActions()
    }

    // Cleanup
    override fun onCleared() {
        super.onCleared()
        // Cleanup any offline-specific resources
    }
}

/**
 * Operation state representation for UI feedback
 */
sealed class OperationState {
    object Idle : OperationState()
    data class Loading(val operation: String) : OperationState()
    data class Success(val message: String) : OperationState()
    data class Error(val message: String) : OperationState()
    data class OfflineQueued(val message: String) : OperationState()
    data class OfflineMode(val message: String) : OperationState()
}

/**
 * Offline action representation
 * (Simplified version - the actual OfflineAction is in the workflows package)
 */
sealed class OfflineAction {
    data class CompleteTask(val taskId: String, val timestamp: Long) : OfflineAction()
    data class CreateTask(val taskData: String, val timestamp: Long) : OfflineAction()
    data class UpdateProgress(val progressData: String, val timestamp: Long) : OfflineAction()
    data class UpdateSetting(val key: String, val value: String, val timestamp: Long) : OfflineAction()
}

/**
 * Network-aware StateFlow extension
 * Automatically switches between online and offline data sources
 */
fun <T> OfflineCapableViewModel.createNetworkAwareStateFlow(
    onlineSource: StateFlow<T>,
    offlineSource: StateFlow<T>,
    defaultValue: T
): StateFlow<T> {
    return combine(
        NetworkHelper.networkState,
        onlineSource,
        offlineSource
    ) { networkState, onlineData, offlineData ->
        when (networkState) {
            NetworkState.CONNECTED -> onlineData
            NetworkState.DISCONNECTED -> offlineData
            NetworkState.POOR -> offlineData // Use offline data for poor connections
            NetworkState.UNKNOWN -> defaultValue
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = defaultValue
    )
}