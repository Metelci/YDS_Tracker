package com.mtlc.studyplan.fragments

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.mtlc.studyplan.R
import com.mtlc.studyplan.ui.OfflineIndicatorView
import com.mtlc.studyplan.utils.NetworkHelper
import com.mtlc.studyplan.utils.NetworkState
import com.mtlc.studyplan.utils.ToastManager
import com.mtlc.studyplan.viewmodels.OfflineCapableViewModel
import kotlinx.coroutines.launch

/**
 * Base Fragment class with comprehensive offline capabilities
 * Provides UI components and interactions for offline mode
 */
abstract class OfflineCapableFragment : Fragment() {

    protected lateinit var offlineIndicator: OfflineIndicatorView
    protected lateinit var networkStateObserver: Observer<NetworkState>
    protected var offlineActionDialog: AlertDialog? = null

    private var wasOffline = false
    private var pendingOfflineActions = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupOfflineSupport()
        observeViewModelStates()
    }

    private fun setupOfflineSupport() {
        // Create offline indicator
        offlineIndicator = OfflineIndicatorView(requireContext())

        // Setup network state observer
        networkStateObserver = Observer { state ->
            handleNetworkStateChange(state)
        }

        // Observe network state changes
        getOfflineCapableViewModel()?.networkState?.observe(viewLifecycleOwner, networkStateObserver)

        // Observe offline queue count
        getOfflineCapableViewModel()?.offlineQueueCount?.observe(viewLifecycleOwner) { count ->
            pendingOfflineActions = count
            updateOfflineIndicator()
        }
    }

    private fun observeViewModelStates() {
        getOfflineCapableViewModel()?.operationState?.observe(viewLifecycleOwner) { state ->
            handleOperationState(state)
        }
    }

    private fun handleNetworkStateChange(state: NetworkState) {
        when (state) {
            NetworkState.CONNECTED -> {
                hideOfflineIndicator()

                if (wasOffline) {
                    showBackOnlineMessage()
                    showSyncingIndicator()
                }
                wasOffline = false
            }
            NetworkState.DISCONNECTED -> {
                showOfflineIndicator()
                if (!wasOffline) {
                    showGoingOfflineMessage()
                }
                wasOffline = true
            }
            NetworkState.POOR -> {
                showPoorConnectionIndicator()
            }
            NetworkState.UNKNOWN -> {
                // Show minimal indication
                showConnectionCheckingIndicator()
            }
        }

        // Notify subclasses
        onNetworkStateChanged(state)
    }

    private fun handleOperationState(state: com.mtlc.studyplan.viewmodels.OperationState) {
        when (state) {
            is com.mtlc.studyplan.viewmodels.OperationState.Loading -> {
                showLoadingIndicator(state.operation)
            }
            is com.mtlc.studyplan.viewmodels.OperationState.Success -> {
                hideLoadingIndicator()
                showSuccessFeedback(state.message)
            }
            is com.mtlc.studyplan.viewmodels.OperationState.Error -> {
                hideLoadingIndicator()
                showErrorFeedback(state.message)
            }
            is com.mtlc.studyplan.viewmodels.OperationState.OfflineQueued -> {
                hideLoadingIndicator()
                showOfflineQueuedFeedback(state.message)
            }
            is com.mtlc.studyplan.viewmodels.OperationState.OfflineMode -> {
                hideLoadingIndicator()
                showOfflineModeFeedback(state.message)
            }
            is com.mtlc.studyplan.viewmodels.OperationState.Idle -> {
                hideLoadingIndicator()
            }
        }
    }

    /**
     * Handle actions that can be performed offline
     * Shows appropriate dialogs and feedback
     */
    protected fun handleOfflineAction(action: () -> Unit, description: String) {
        if (NetworkHelper.isOnline()) {
            action()
        } else {
            showOfflineActionDialog(description) {
                action() // Execute anyway and queue for sync
            }
        }
    }

    /**
     * Handle actions that require internet connection
     * Shows error message when offline
     */
    protected fun handleOnlineOnlyAction(action: () -> Unit, description: String) {
        if (NetworkHelper.isOnline()) {
            action()
        } else {
            showOnlineOnlyErrorDialog(description)
        }
    }

    /**
     * Execute action with loading state and offline support
     */
    protected fun executeActionWithFeedback(
        action: suspend () -> Unit,
        loadingMessage: String = "Processing...",
        successMessage: String = "Action completed",
        errorMessage: String = "Action failed"
    ) {
        lifecycleScope.launch {
            try {
                showLoadingIndicator(loadingMessage)
                action()
                hideLoadingIndicator()
                showSuccessFeedback(successMessage)
            } catch (e: Exception) {
                hideLoadingIndicator()
                showErrorFeedback("$errorMessage: ${e.message}")
            }
        }
    }

    // UI feedback methods
    private fun showOfflineIndicator() {
        // Add offline indicator to the view
        onShowOfflineIndicator()
    }

    private fun hideOfflineIndicator() {
        onHideOfflineIndicator()
    }

    private fun showPoorConnectionIndicator() {
        ToastManager.showWarning("⚠️ Poor connection - some features may be limited")
        onShowPoorConnectionIndicator()
    }

    private fun showConnectionCheckingIndicator() {
        onShowConnectionCheckingIndicator()
    }

    private fun showSyncingIndicator() {
        if (pendingOfflineActions > 0) {
            ToastManager.showInfo("🔄 Syncing $pendingOfflineActions offline changes...")
        }
        onShowSyncingIndicator()
    }

    private fun showGoingOfflineMessage() {
        ToastManager.showWarning("📱 You're now offline. Changes will be saved and synced later.")
    }

    private fun showBackOnlineMessage() {
        ToastManager.showSuccess("🌐 You're back online!")
    }

    private fun showLoadingIndicator(message: String) {
        onShowLoadingIndicator(message)
    }

    private fun hideLoadingIndicator() {
        onHideLoadingIndicator()
    }

    private fun showSuccessFeedback(message: String) {
        ToastManager.showSuccess(message)
        onShowSuccessFeedback(message)
    }

    private fun showErrorFeedback(message: String) {
        ToastManager.showError(message)
        onShowErrorFeedback(message)
    }

    private fun showOfflineQueuedFeedback(message: String) {
        ToastManager.showWarning("📥 $message")
        onShowOfflineQueuedFeedback(message)
    }

    private fun showOfflineModeFeedback(message: String) {
        ToastManager.showInfo("📱 $message")
        onShowOfflineModeFeedback(message)
    }

    private fun showOfflineActionDialog(description: String, action: () -> Unit) {
        offlineActionDialog?.dismiss()

        offlineActionDialog = AlertDialog.Builder(requireContext())
            .setTitle("Offline Action")
            .setMessage("You're currently offline. $description will be saved and synced when you're back online.\n\nContinue?")
            .setPositiveButton("Continue") { _, _ ->
                action()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .setIcon(R.drawable.ic_cloud_off)
            .show()
    }

    private fun showOnlineOnlyErrorDialog(description: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Internet Required")
            .setMessage("$description requires an internet connection. Please check your connection and try again.")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .setIcon(R.drawable.ic_warning)
            .show()
    }

    protected fun showOfflineStatusDialog() {
        val viewModel = getOfflineCapableViewModel()
        val queueCount = viewModel?.offlineQueueCount?.value ?: 0
        val connectionStatus = NetworkHelper.getConnectionStatusMessage()

        val message = buildString {
            append("Connection Status: $connectionStatus\n\n")
            if (queueCount > 0) {
                append("Offline Actions: $queueCount pending\n")
                append("These will sync automatically when you're online.\n\n")
            }
            if (!NetworkHelper.isOnline()) {
                append("You can continue studying offline. Your progress will be saved locally and synced later.")
            }
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Offline Status")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .apply {
                if (queueCount > 0 && NetworkHelper.isOnline()) {
                    setNeutralButton("Sync Now") { _, _ ->
                        getOfflineCapableViewModel()?.forceSyncOfflineActions()
                    }
                }
            }
            .show()
    }

    // Abstract and overridable methods for subclasses
    protected abstract fun getOfflineCapableViewModel(): OfflineCapableViewModel?

    protected open fun onNetworkStateChanged(networkState: NetworkState) {
        // Override in subclasses for custom network state handling
    }

    protected open fun onShowOfflineIndicator() {
        // Override in subclasses for custom offline indicator
    }

    protected open fun onHideOfflineIndicator() {
        // Override in subclasses
    }

    protected open fun onShowPoorConnectionIndicator() {
        // Override in subclasses
    }

    protected open fun onShowConnectionCheckingIndicator() {
        // Override in subclasses
    }

    protected open fun onShowSyncingIndicator() {
        // Override in subclasses
    }

    protected open fun onShowLoadingIndicator(message: String) {
        // Override in subclasses for custom loading indicator
    }

    protected open fun onHideLoadingIndicator() {
        // Override in subclasses
    }

    protected open fun onShowSuccessFeedback(message: String) {
        // Override in subclasses for custom success feedback
    }

    protected open fun onShowErrorFeedback(message: String) {
        // Override in subclasses for custom error feedback
    }

    protected open fun onShowOfflineQueuedFeedback(message: String) {
        // Override in subclasses
    }

    protected open fun onShowOfflineModeFeedback(message: String) {
        // Override in subclasses
    }

    // Utility methods
    protected fun isOnline(): Boolean = NetworkHelper.isOnline()

    protected fun canSync(): Boolean = NetworkHelper.canSync()

    protected fun shouldShowOfflineWarning(): Boolean = !NetworkHelper.isOnline()

    protected fun getConnectionQuality(): String {
        return when (NetworkHelper.getNetworkQuality()) {
            com.mtlc.studyplan.utils.NetworkQuality.EXCELLENT -> "Excellent"
            com.mtlc.studyplan.utils.NetworkQuality.GOOD -> "Good"
            com.mtlc.studyplan.utils.NetworkQuality.FAIR -> "Fair"
            com.mtlc.studyplan.utils.NetworkQuality.POOR -> "Poor"
            com.mtlc.studyplan.utils.NetworkQuality.NO_CONNECTION -> "No Connection"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        offlineActionDialog?.dismiss()
    }
}

/**
 * Simple offline indicator view
 * Shows current connection status
 */
class OfflineIndicatorView(context: Context) : androidx.appcompat.widget.AppCompatTextView(context) {

    init {
        text = "📱 Offline Mode"
        setBackgroundColor(android.graphics.Color.parseColor("#FF9800"))
        setTextColor(android.graphics.Color.WHITE)
        setPadding(16, 8, 16, 8)
        textAlignment = TEXT_ALIGNMENT_CENTER
        visibility = View.GONE
    }

    fun showOffline() {
        text = "📱 Offline Mode"
        setBackgroundColor(android.graphics.Color.parseColor("#FF9800"))
        visibility = View.VISIBLE
    }

    fun showPoorConnection() {
        text = "⚠️ Poor Connection"
        setBackgroundColor(android.graphics.Color.parseColor("#FF5722"))
        visibility = View.VISIBLE
    }

    fun showSyncing() {
        text = "🔄 Syncing..."
        setBackgroundColor(android.graphics.Color.parseColor("#2196F3"))
        visibility = View.VISIBLE
    }

    fun hide() {
        visibility = View.GONE
    }
}