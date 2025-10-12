package com.mtlc.studyplan.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Base ViewModel class providing common functionality and architecture patterns
 */
abstract class BaseViewModel<UiState, Intent> : ViewModel() {
    
    abstract val initialState: UiState
    
    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    // Provide access to current state value for subclasses
    protected val currentState: UiState
        get() = _uiState.value
    
    protected val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        handleError(exception)
    }
    
    /**
     * Handle UI intents/actions dispatched from the UI
     */
    fun dispatchIntent(intent: Intent) {
        processIntent(intent)
    }
    
    /**
     * Process the intent in a safe coroutine scope
     */
    protected fun safeLaunch(block: suspend () -> Unit) {
        viewModelScope.launch(exceptionHandler) {
            block()
        }
    }
    
    /**
     * Update the UI state
     */
    protected fun updateState(newState: UiState) {
        _uiState.value = newState
    }
    
    /**
     * Process specific intent
     */
    protected abstract fun processIntent(intent: Intent)
    
    /**
     * Handle errors consistently across ViewModels
     */
    protected open fun handleError(throwable: Throwable) {
        // Log error and handle appropriately
        // This can be extended in subclasses to handle errors differently
        throwable.printStackTrace()
    }
    
    /**
     * Initialize ViewModel with common setup
     */
    protected open fun initialize() {
        // Common initialization logic can go here
    }
    
    init {
        initialize()
    }
}
