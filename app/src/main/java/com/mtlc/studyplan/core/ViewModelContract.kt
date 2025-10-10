package com.mtlc.studyplan.core

import kotlinx.coroutines.flow.StateFlow

/**
 * Interface defining common architecture patterns for ViewModels
 */
interface StudyPlanViewModel<UiState, Intent> {
    val uiState: StateFlow<UiState>
    
    /**
     * Dispatch an intent/action to the ViewModel
     */
    fun dispatchIntent(intent: Intent)
}

/**
 * Convenience function to update state in a consistent way
 */
fun <T> StateFlow<T>.updateState(block: (T) -> T): T {
    return block(this.value)
}