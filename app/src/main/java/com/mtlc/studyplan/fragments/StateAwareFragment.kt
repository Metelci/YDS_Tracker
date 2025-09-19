package com.mtlc.studyplan.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.mtlc.studyplan.navigation.NavigationStateManager
import kotlinx.coroutines.launch

abstract class StateAwareFragment<T> : Fragment() {

    protected lateinit var navigationStateManager: NavigationStateManager
    private var stateRestored = false
    private var pendingStateRestore: T? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        navigationStateManager = NavigationStateManager(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Restore state when view is ready
        lifecycleScope.launch {
            if (!stateRestored) {
                restoreState()
                stateRestored = true
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (stateRestored) {
            saveCurrentState()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Save state one final time before view destruction
        if (stateRestored) {
            saveCurrentState()
        }
        stateRestored = false
    }

    // Abstract methods that subclasses must implement
    abstract fun getCurrentState(): T
    abstract fun applyState(state: T)
    abstract fun getDefaultState(): T
    abstract fun getRestoredState(): T?
    abstract fun saveState(state: T)

    // State management operations
    private fun restoreState() {
        val state = getRestoredState() ?: getDefaultState()

        // Check if view is ready for state application
        if (isViewReady()) {
            applyState(state)
        } else {
            // Store state for later application
            pendingStateRestore = state
            waitForViewReady {
                pendingStateRestore?.let { pendingState ->
                    applyState(pendingState)
                    pendingStateRestore = null
                }
            }
        }
    }

    private fun saveCurrentState() {
        try {
            val currentState = getCurrentState()
            saveState(currentState)
        } catch (e: Exception) {
            // Log error but don't crash
            android.util.Log.w("StateAwareFragment", "Failed to save state: ${e.message}")
        }
    }

    // View readiness checking
    protected open fun isViewReady(): Boolean {
        return view != null && isAdded && !isDetached
    }

    protected open fun waitForViewReady(callback: () -> Unit) {
        view?.post {
            if (isViewReady()) {
                callback()
            } else {
                // Try again in next frame
                view?.post { waitForViewReady(callback) }
            }
        }
    }

    // Utility methods for state validation
    protected fun isStateValid(state: T?): Boolean {
        return state != null && validateState(state)
    }

    protected open fun validateState(state: T): Boolean {
        return true // Override in subclasses for specific validation
    }

    // Force state save (for critical moments)
    protected fun forceSaveState() {
        saveCurrentState()
    }

    // Force state restore (for refresh scenarios)
    protected fun forceRestoreState() {
        restoreState()
    }

    // Clear saved state
    protected fun clearSavedState() {
        // Implement in subclasses if needed
    }

    // State change notification
    protected open fun onStateApplied(state: T) {
        // Override in subclasses for post-state-application logic
    }

    protected open fun onStateSaved(state: T) {
        // Override in subclasses for post-state-saving logic
    }

    // Delayed state operations
    protected fun applyStateDelayed(state: T, delayMs: Long = 100) {
        view?.postDelayed({
            if (isViewReady()) {
                applyState(state)
                onStateApplied(state)
            }
        }, delayMs)
    }

    // Batch state operations
    protected fun saveStateWithCallback(callback: (T) -> Unit) {
        val state = getCurrentState()
        saveState(state)
        onStateSaved(state)
        callback(state)
    }

    // State comparison
    protected open fun hasStateChanged(oldState: T, newState: T): Boolean {
        return oldState != newState
    }

    // Safe state operations with error handling
    protected fun safeApplyState(state: T) {
        try {
            if (isViewReady() && isStateValid(state)) {
                applyState(state)
                onStateApplied(state)
            }
        } catch (e: Exception) {
            android.util.Log.e("StateAwareFragment", "Failed to apply state: ${e.message}", e)
            // Fallback to default state
            try {
                applyState(getDefaultState())
            } catch (fallbackError: Exception) {
                android.util.Log.e("StateAwareFragment", "Failed to apply default state: ${fallbackError.message}")
            }
        }
    }

    protected fun safeSaveState() {
        try {
            if (isViewReady()) {
                val state = getCurrentState()
                if (isStateValid(state)) {
                    saveState(state)
                    onStateSaved(state)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("StateAwareFragment", "Failed to save state: ${e.message}", e)
        }
    }
}