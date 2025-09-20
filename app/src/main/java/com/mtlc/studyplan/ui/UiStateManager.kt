package com.mtlc.studyplan.ui

import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.mtlc.studyplan.R

/**
 * Comprehensive UI State Management System
 * Provides consistent state handling across all screens
 */
class UiStateManager {

    private val _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> = _uiState

    fun createLoadingState(message: String = "Loading..."): UiState {
        return UiState.Loading(
            message = message,
            progress = -1 // Indeterminate
        )
    }

    fun createLoadingWithProgressState(message: String, progress: Int): UiState {
        return UiState.Loading(
            message = message,
            progress = progress
        )
    }

    fun createErrorState(
        error: String,
        retryAction: (() -> Unit)? = null,
        @DrawableRes icon: Int = R.drawable.ic_error
    ): UiState {
        return UiState.Error(
            message = error,
            retryAction = retryAction,
            icon = icon
        )
    }

    fun createEmptyState(
        message: String = "No data available",
        description: String = "Try refreshing or check back later",
        actionText: String? = null,
        action: (() -> Unit)? = null,
        @DrawableRes icon: Int = R.drawable.ic_list
    ): UiState {
        return UiState.Empty(
            message = message,
            description = description,
            actionText = actionText,
            action = action,
            icon = icon
        )
    }

    fun createSuccessState(hasData: Boolean = true): UiState {
        return UiState.Success(hasData = hasData)
    }

    fun createOfflineState(
        hasData: Boolean = false,
        message: String = "You're offline",
        description: String = "Some features may be limited"
    ): UiState {
        return UiState.Offline(
            hasData = hasData,
            message = message,
            description = description
        )
    }

    fun createRefreshingState(hasData: Boolean = true): UiState {
        return UiState.Refreshing(hasData = hasData)
    }

    fun createSyncingState(
        hasData: Boolean = true,
        pendingActions: Int = 0
    ): UiState {
        return UiState.Syncing(
            hasData = hasData,
            pendingActions = pendingActions
        )
    }

    // State transition methods
    fun setLoading(message: String = "Loading...") {
        _uiState.value = createLoadingState(message)
    }

    fun setLoadingWithProgress(message: String, progress: Int) {
        _uiState.value = createLoadingWithProgressState(message, progress)
    }

    fun setError(error: String, retryAction: (() -> Unit)? = null) {
        _uiState.value = createErrorState(error, retryAction)
    }

    fun setEmpty(
        message: String = "No data available",
        actionText: String? = null,
        action: (() -> Unit)? = null
    ) {
        _uiState.value = createEmptyState(message, actionText = actionText, action = action)
    }

    fun setSuccess(hasData: Boolean = true) {
        _uiState.value = createSuccessState(hasData)
    }

    fun setOffline(hasData: Boolean = false) {
        _uiState.value = createOfflineState(hasData)
    }

    fun setRefreshing(hasData: Boolean = true) {
        _uiState.value = createRefreshingState(hasData)
    }

    fun setSyncing(hasData: Boolean = true, pendingActions: Int = 0) {
        _uiState.value = createSyncingState(hasData, pendingActions)
    }

    fun getCurrentState(): UiState? = _uiState.value

    fun isLoading(): Boolean = _uiState.value is UiState.Loading

    fun isError(): Boolean = _uiState.value is UiState.Error

    fun isEmpty(): Boolean = _uiState.value is UiState.Empty

    fun isOffline(): Boolean = _uiState.value is UiState.Offline

    fun hasData(): Boolean = when (val state = _uiState.value) {
        is UiState.Success -> state.hasData
        is UiState.Offline -> state.hasData
        is UiState.Refreshing -> state.hasData
        is UiState.Syncing -> state.hasData
        else -> false
    }
}

/**
 * Comprehensive UI State representation
 */
sealed class UiState {
    data class Loading(
        val message: String = "Loading...",
        val progress: Int = -1 // -1 for indeterminate
    ) : UiState()

    data class Error(
        val message: String,
        val retryAction: (() -> Unit)? = null,
        @DrawableRes val icon: Int = R.drawable.ic_error
    ) : UiState()

    data class Empty(
        val message: String,
        val description: String = "",
        val actionText: String? = null,
        val action: (() -> Unit)? = null,
        @DrawableRes val icon: Int = R.drawable.ic_list
    ) : UiState()

    data class Success(
        val hasData: Boolean = true
    ) : UiState()

    data class Offline(
        val hasData: Boolean = false,
        val message: String = "You're offline",
        val description: String = "Some features may be limited"
    ) : UiState()

    data class Refreshing(
        val hasData: Boolean = true
    ) : UiState()

    data class Syncing(
        val hasData: Boolean = true,
        val pendingActions: Int = 0
    ) : UiState()
}

/**
 * Extension functions for easy UI updates based on state
 */
fun View.updateVisibilityForState(state: UiState, stateType: UiStateType) {
    visibility = when (stateType) {
        UiStateType.LOADING -> if (state is UiState.Loading) View.VISIBLE else View.GONE
        UiStateType.ERROR -> if (state is UiState.Error) View.VISIBLE else View.GONE
        UiStateType.EMPTY -> if (state is UiState.Empty) View.VISIBLE else View.GONE
        UiStateType.CONTENT -> {
            when (state) {
                is UiState.Success -> if (state.hasData) View.VISIBLE else View.GONE
                is UiState.Offline -> if (state.hasData) View.VISIBLE else View.GONE
                is UiState.Refreshing -> if (state.hasData) View.VISIBLE else View.GONE
                is UiState.Syncing -> if (state.hasData) View.VISIBLE else View.GONE
                else -> View.GONE
            }
        }
        UiStateType.OFFLINE_INDICATOR -> if (state is UiState.Offline) View.VISIBLE else View.GONE
        UiStateType.REFRESH_INDICATOR -> if (state is UiState.Refreshing) View.VISIBLE else View.GONE
        UiStateType.SYNC_INDICATOR -> if (state is UiState.Syncing) View.VISIBLE else View.GONE
    }
}

fun ProgressBar.updateForState(state: UiState) {
    when (state) {
        is UiState.Loading -> {
            visibility = View.VISIBLE
            if (state.progress >= 0) {
                isIndeterminate = false
                progress = state.progress
            } else {
                isIndeterminate = true
            }
        }
        is UiState.Refreshing -> {
            visibility = View.VISIBLE
            isIndeterminate = true
        }
        else -> {
            visibility = View.GONE
        }
    }
}

fun TextView.updateForState(state: UiState, textType: UiTextType) {
    when (textType) {
        UiTextType.LOADING_MESSAGE -> {
            text = if (state is UiState.Loading) state.message else ""
            visibility = if (state is UiState.Loading) View.VISIBLE else View.GONE
        }
        UiTextType.ERROR_MESSAGE -> {
            text = if (state is UiState.Error) state.message else ""
            visibility = if (state is UiState.Error) View.VISIBLE else View.GONE
        }
        UiTextType.EMPTY_MESSAGE -> {
            text = if (state is UiState.Empty) state.message else ""
            visibility = if (state is UiState.Empty) View.VISIBLE else View.GONE
        }
        UiTextType.EMPTY_DESCRIPTION -> {
            text = if (state is UiState.Empty) state.description else ""
            visibility = if (state is UiState.Empty && state.description.isNotEmpty()) View.VISIBLE else View.GONE
        }
        UiTextType.OFFLINE_MESSAGE -> {
            text = if (state is UiState.Offline) state.message else ""
            visibility = if (state is UiState.Offline) View.VISIBLE else View.GONE
        }
        UiTextType.SYNC_MESSAGE -> {
            text = when (state) {
                is UiState.Syncing -> "Syncing ${state.pendingActions} actions..."
                else -> ""
            }
            visibility = if (state is UiState.Syncing) View.VISIBLE else View.GONE
        }
    }
}

fun Button.updateForState(state: UiState, buttonType: UiButtonType) {
    when (buttonType) {
        UiButtonType.RETRY -> {
            visibility = if (state is UiState.Error && state.retryAction != null) View.VISIBLE else View.GONE
            setOnClickListener {
                if (state is UiState.Error) state.retryAction?.invoke()
            }
        }
        UiButtonType.EMPTY_ACTION -> {
            visibility = if (state is UiState.Empty && state.action != null) View.VISIBLE else View.GONE
            text = if (state is UiState.Empty) state.actionText ?: "Try Again" else ""
            setOnClickListener {
                if (state is UiState.Empty) state.action?.invoke()
            }
        }
    }
}

fun ImageView.updateForState(state: UiState, imageType: UiImageType) {
    when (imageType) {
        UiImageType.ERROR_ICON -> {
            visibility = if (state is UiState.Error) View.VISIBLE else View.GONE
            if (state is UiState.Error) {
                setImageResource(state.icon)
            }
        }
        UiImageType.EMPTY_ICON -> {
            visibility = if (state is UiState.Empty) View.VISIBLE else View.GONE
            if (state is UiState.Empty) {
                setImageResource(state.icon)
            }
        }
        UiImageType.OFFLINE_ICON -> {
            visibility = if (state is UiState.Offline) View.VISIBLE else View.GONE
            if (state is UiState.Offline) {
                setImageResource(R.drawable.ic_cloud)
            }
        }
    }
}

/**
 * Compose-specific UI state handling
 */
@Composable
fun UiStateContent(
    state: UiState,
    onRetry: (() -> Unit)? = null,
    loadingContent: @Composable (UiState.Loading) -> Unit = { DefaultLoadingContent(it) },
    errorContent: @Composable (UiState.Error) -> Unit = { DefaultErrorContent(it, onRetry) },
    emptyContent: @Composable (UiState.Empty) -> Unit = { DefaultEmptyContent(it) },
    offlineContent: @Composable (UiState.Offline) -> Unit = { DefaultOfflineContent(it) },
    content: @Composable () -> Unit
) {
    when (state) {
        is UiState.Loading -> loadingContent(state)
        is UiState.Error -> errorContent(state)
        is UiState.Empty -> emptyContent(state)
        is UiState.Offline -> {
            if (state.hasData) {
                content()
                // Show offline indicator overlay
            } else {
                offlineContent(state)
            }
        }
        is UiState.Success -> {
            if (state.hasData) {
                content()
            } else {
                emptyContent(UiState.Empty("No data available"))
            }
        }
        is UiState.Refreshing -> {
            content()
            // Show refresh indicator
        }
        is UiState.Syncing -> {
            content()
            // Show sync indicator
        }
    }
}

@Composable
fun DefaultLoadingContent(state: UiState.Loading) {
    // Default loading composable implementation
    // This would contain actual Compose UI
}

@Composable
fun DefaultErrorContent(state: UiState.Error, onRetry: (() -> Unit)?) {
    // Default error composable implementation
}

@Composable
fun DefaultEmptyContent(state: UiState.Empty) {
    // Default empty state composable implementation
}

@Composable
fun DefaultOfflineContent(state: UiState.Offline) {
    // Default offline composable implementation
}

/**
 * Enums for different UI element types
 */
enum class UiStateType {
    LOADING,
    ERROR,
    EMPTY,
    CONTENT,
    OFFLINE_INDICATOR,
    REFRESH_INDICATOR,
    SYNC_INDICATOR
}

enum class UiTextType {
    LOADING_MESSAGE,
    ERROR_MESSAGE,
    EMPTY_MESSAGE,
    EMPTY_DESCRIPTION,
    OFFLINE_MESSAGE,
    SYNC_MESSAGE
}

enum class UiButtonType {
    RETRY,
    EMPTY_ACTION
}

enum class UiImageType {
    ERROR_ICON,
    EMPTY_ICON,
    OFFLINE_ICON
}

/**
 * Common UI state scenarios
 */
object CommonUiStates {
    fun networkError(retryAction: () -> Unit) = UiState.Error(
        message = "Network connection error. Please check your internet connection.",
        retryAction = retryAction,
        icon = R.drawable.ic_cloud
    )

    fun serverError(retryAction: () -> Unit) = UiState.Error(
        message = "Server error. Please try again later.",
        retryAction = retryAction,
        icon = R.drawable.ic_error
    )

    fun noTasks(createAction: () -> Unit) = UiState.Empty(
        message = "No tasks yet",
        description = "Create your first task to get started with your study journey",
        actionText = "Create Task",
        action = createAction,
        icon = R.drawable.ic_task
    )

    fun noProgress() = UiState.Empty(
        message = "No progress data",
        description = "Complete some tasks to see your progress",
        icon = R.drawable.ic_arrow_forward
    )

    fun noAchievements() = UiState.Empty(
        message = "No achievements yet",
        description = "Keep studying to unlock your first achievement!",
        icon = R.drawable.ic_star
    )

    fun noFriends(addFriendsAction: () -> Unit) = UiState.Empty(
        message = "No friends yet",
        description = "Add friends to share your study progress and compete together",
        actionText = "Add Friends",
        action = addFriendsAction,
        icon = R.drawable.ic_people
    )

    val loadingTasks = UiState.Loading("Loading your tasks...")
    val loadingProgress = UiState.Loading("Loading progress data...")
    val syncingOfflineActions = UiState.Syncing(hasData = true, pendingActions = 5)
}

