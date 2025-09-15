package com.mtlc.studyplan.ui.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

data class UndoAction(
    val id: String,
    val actionDescription: String,
    val undoAction: suspend () -> Unit,
    val timestamp: Long = System.currentTimeMillis()
)

class UndoManager {
    private val undoActions = ConcurrentHashMap<String, UndoAction>()
    private var cleanupJob: Job? = null

    fun addUndoAction(action: UndoAction) {
        undoActions[action.id] = action

        // Schedule cleanup after 10 seconds
        cleanupJob?.cancel()
        cleanupJob = null
    }

    fun getUndoAction(id: String): UndoAction? = undoActions[id]

    fun removeUndoAction(id: String) {
        undoActions.remove(id)
    }

    fun clear() {
        undoActions.clear()
        cleanupJob?.cancel()
        cleanupJob = null
    }

    private fun scheduleCleanup(scope: CoroutineScope, actionId: String) {
        cleanupJob = scope.launch {
            delay(10000) // 10 seconds
            removeUndoAction(actionId)
        }
    }
}

@Composable
fun rememberUndoManager(): UndoManager {
    return remember { UndoManager() }
}

@Composable
fun UndoSnackbarEffect(
    undoManager: UndoManager,
    snackbarHostState: SnackbarHostState,
    recentAction: UndoAction?,
    onActionConsumed: () -> Unit,
    scope: CoroutineScope = rememberCoroutineScope()
) {
    LaunchedEffect(recentAction) {
        recentAction?.let { action ->
            val result = snackbarHostState.showSnackbar(
                message = action.actionDescription,
                actionLabel = "Undo",
                duration = SnackbarDuration.Long
            )

            when (result) {
                SnackbarResult.ActionPerformed -> {
                    // User tapped "Undo"
                    action.undoAction()
                    undoManager.removeUndoAction(action.id)
                }
                SnackbarResult.Dismissed -> {
                    // Snackbar was dismissed, remove the undo action
                    undoManager.removeUndoAction(action.id)
                }
            }

            onActionConsumed()
        }
    }
}