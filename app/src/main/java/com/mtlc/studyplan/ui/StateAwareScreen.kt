package com.mtlc.studyplan.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.mtlc.studyplan.state.StatePreservationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun <T : Any> StateAwareScreen(
    screenKey: String,
    defaultState: T,
    stateClass: Class<T>,
    statePreservationManager: StatePreservationManager,
    content: @Composable (state: T, onStateChange: (T) -> Unit) -> Unit
) {
    var currentState by remember { mutableStateOf(defaultState) }
    var isStateRestored by remember { mutableStateOf(false) }

    // Restore state on first composition
    LaunchedEffect(screenKey) {
        val restoredState = statePreservationManager.restoreScreenState(screenKey, stateClass)
        if (restoredState != null) {
            currentState = restoredState
        }
        isStateRestored = true
    }

    // Save state on changes
    LaunchedEffect(currentState) {
        if (isStateRestored) {
            statePreservationManager.saveScreenState(screenKey, currentState)
        }
    }

    // Save state when leaving screen
    DisposableEffect(screenKey) {
        onDispose {
            CoroutineScope(Dispatchers.IO).launch {
                statePreservationManager.saveScreenState(screenKey, currentState)
            }
        }
    }

    if (isStateRestored) {
        content(currentState) { newState ->
            currentState = newState
        }
    } else {
        // Show loading while restoring state
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
fun rememberStatePreservation(
    screenKey: String,
    statePreservationManager: StatePreservationManager
): StatePreservationScope {
    return remember(screenKey) {
        StatePreservationScope(screenKey, statePreservationManager)
    }
}

class StatePreservationScope(
    private val screenKey: String,
    private val statePreservationManager: StatePreservationManager
) {
    fun saveScrollPosition(position: Int) {
        statePreservationManager.saveScrollPosition(screenKey, position)
    }

    fun restoreScrollPosition(): Int {
        return statePreservationManager.restoreScrollPosition(screenKey)
    }

    fun saveSearchQuery(query: String) {
        statePreservationManager.saveSearchQuery(screenKey, query)
    }

    fun restoreSearchQuery(): String {
        return statePreservationManager.restoreSearchQuery(screenKey)
    }

    fun saveFilterState(filters: Set<String>) {
        statePreservationManager.saveFilterState(screenKey, filters)
    }

    fun restoreFilterState(): Set<String> {
        return statePreservationManager.restoreFilterState(screenKey)
    }
}
