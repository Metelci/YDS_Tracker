package com.mtlc.studyplan.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Modern Compose-based Loading State Management System
 * Provides consistent loading indicators and progress feedback
 */
@Singleton
class LoadingStateManager @Inject constructor() {

    private val _loadingStates = MutableStateFlow<Map<String, LoadingStateData>>(emptyMap())
    val loadingStates: StateFlow<Map<String, LoadingStateData>> = _loadingStates.asStateFlow()

    private val _globalLoading = MutableStateFlow(false)
    val globalLoading: StateFlow<Boolean> = _globalLoading.asStateFlow()

    fun showLoading(
        key: String,
        message: String = "Loading...",
        type: LoadingType = LoadingType.SHIMMER,
        cancellable: Boolean = false
    ) {
        val currentStates = _loadingStates.value.toMutableMap()
        currentStates[key] = LoadingStateData(
            isLoading = true,
            message = message,
            type = type,
            cancellable = cancellable,
            startTime = System.currentTimeMillis()
        )
        _loadingStates.value = currentStates
        updateGlobalLoading()
    }

    fun hideLoading(key: String) {
        val currentStates = _loadingStates.value.toMutableMap()
        currentStates.remove(key)
        _loadingStates.value = currentStates
        updateGlobalLoading()
    }

    fun updateLoadingProgress(key: String, progress: Float, message: String? = null) {
        val currentStates = _loadingStates.value.toMutableMap()
        currentStates[key]?.let { state ->
            currentStates[key] = state.copy(
                progress = progress,
                message = message ?: state.message
            )
            _loadingStates.value = currentStates
        }
    }

    private fun updateGlobalLoading() {
        _globalLoading.value = _loadingStates.value.any { it.value.isLoading }
    }

    fun isLoading(key: String): Boolean {
        return _loadingStates.value[key]?.isLoading == true
    }

    fun getLoadingState(key: String): LoadingStateData? {
        return _loadingStates.value[key]
    }

    fun clearAllLoading() {
        _loadingStates.value = emptyMap()
        _globalLoading.value = false
    }
}

data class LoadingStateData(
    val isLoading: Boolean = false,
    val message: String = "",
    val type: LoadingType = LoadingType.SHIMMER,
    val progress: Float = 0f,
    val cancellable: Boolean = false,
    val startTime: Long = System.currentTimeMillis()
)

enum class LoadingType {
    SHIMMER,
    SPINNER,
    PROGRESS_BAR,
    SKELETON,
    PULL_TO_REFRESH
}

object LoadingKeys {
    const val TASKS_LOADING = "tasks_loading"
    const val PROGRESS_LOADING = "progress_loading"
    const val SETTINGS_LOADING = "settings_loading"
    const val SYNC_LOADING = "sync_loading"
    const val TASK_COMPLETION = "task_completion"
    const val DATA_EXPORT = "data_export"
    const val THEME_CHANGE = "theme_change"
}

/**
 * Extension functions for common loading operations
 */
fun LoadingStateManager.showTaskCompletion() {
    showLoading(
        LoadingKeys.TASK_COMPLETION,
        "Completing task...",
        LoadingType.SPINNER
    )
}

fun LoadingStateManager.showDataSync() {
    showLoading(
        LoadingKeys.SYNC_LOADING,
        "Syncing data...",
        LoadingType.PROGRESS_BAR
    )
}

fun LoadingStateManager.showThemeChange() {
    showLoading(
        LoadingKeys.THEME_CHANGE,
        "Applying theme...",
        LoadingType.SHIMMER
    )
}
