@file:OptIn(ExperimentalCoroutinesApi::class)

package com.mtlc.studyplan.settings.feedback

import android.content.Context
import android.os.Vibrator
import android.os.VibratorManager
import android.os.VibrationEffect
import androidx.annotation.StringRes
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * Comprehensive user feedback and loading states management for settings
 */
class SettingsFeedbackManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _loadingStates = MutableStateFlow<Map<String, LoadingState>>(emptyMap())
    val loadingStates: StateFlow<Map<String, LoadingState>> = _loadingStates.asStateFlow()

    private val _feedbackEvents = MutableSharedFlow<FeedbackEvent>()
    val feedbackEvents: SharedFlow<FeedbackEvent> = _feedbackEvents.asSharedFlow()

    private val _progressStates = MutableStateFlow<Map<String, ProgressState>>(emptyMap())
    val progressStates: StateFlow<Map<String, ProgressState>> = _progressStates.asStateFlow()

    private val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        // For API 30, still use VIBRATOR_SERVICE but with proper typing
        context.getSystemService(Vibrator::class.java)
    }

    /**
     * Show loading state for an operation
     */
    fun showLoading(
        operationId: String,
        message: String,
        cancellable: Boolean = false,
        onCancel: (() -> Unit)? = null
    ) {
        val currentStates = _loadingStates.value.toMutableMap()
        currentStates[operationId] = LoadingState.Loading(
            message = message,
            cancellable = cancellable,
            onCancel = onCancel,
            startTime = System.currentTimeMillis()
        )
        _loadingStates.value = currentStates
    }

    /**
     * Hide loading state for an operation
     */
    fun hideLoading(operationId: String) {
        val currentStates = _loadingStates.value.toMutableMap()
        currentStates.remove(operationId)
        _loadingStates.value = currentStates
    }

    /**
     * Show progress for an operation
     */
    fun showProgress(
        operationId: String,
        message: String,
        progress: Float,
        indeterminate: Boolean = false
    ) {
        val currentStates = _progressStates.value.toMutableMap()
        currentStates[operationId] = ProgressState(
            message = message,
            progress = progress.coerceIn(0f, 1f),
            indeterminate = indeterminate,
            startTime = System.currentTimeMillis()
        )
        _progressStates.value = currentStates
    }

    /**
     * Update progress for an operation
     */
    fun updateProgress(operationId: String, progress: Float, message: String? = null) {
        val currentStates = _progressStates.value.toMutableMap()
        val currentState = currentStates[operationId]

        if (currentState != null) {
            currentStates[operationId] = currentState.copy(
                progress = progress.coerceIn(0f, 1f),
                message = message ?: currentState.message
            )
            _progressStates.value = currentStates
        }
    }

    /**
     * Hide progress for an operation
     */
    fun hideProgress(operationId: String) {
        val currentStates = _progressStates.value.toMutableMap()
        currentStates.remove(operationId)
        _progressStates.value = currentStates
    }

    /**
     * Show success feedback
     */
    suspend fun showSuccess(
        message: String,
        duration: FeedbackDuration = FeedbackDuration.SHORT,
        haptic: Boolean = true,
        actionLabel: String? = null,
        onAction: (() -> Unit)? = null
    ) {
        if (haptic) {
            performHapticFeedback(HapticType.SUCCESS)
        }

        _feedbackEvents.emit(
            FeedbackEvent.Success(
                message = message,
                duration = duration,
                actionLabel = actionLabel,
                onAction = onAction
            )
        )
    }

    /**
     * Show error feedback
     */
    suspend fun showError(
        message: String,
        duration: FeedbackDuration = FeedbackDuration.LONG,
        haptic: Boolean = true,
        actionLabel: String? = "Retry",
        onAction: (() -> Unit)? = null
    ) {
        if (haptic) {
            performHapticFeedback(HapticType.ERROR)
        }

        _feedbackEvents.emit(
            FeedbackEvent.Error(
                message = message,
                duration = duration,
                actionLabel = actionLabel,
                onAction = onAction
            )
        )
    }

    /**
     * Show warning feedback
     */
    suspend fun showWarning(
        message: String,
        duration: FeedbackDuration = FeedbackDuration.MEDIUM,
        haptic: Boolean = false,
        actionLabel: String? = null,
        onAction: (() -> Unit)? = null
    ) {
        if (haptic) {
            performHapticFeedback(HapticType.WARNING)
        }

        _feedbackEvents.emit(
            FeedbackEvent.Warning(
                message = message,
                duration = duration,
                actionLabel = actionLabel,
                onAction = onAction
            )
        )
    }

    /**
     * Show info feedback
     */
    suspend fun showInfo(
        message: String,
        duration: FeedbackDuration = FeedbackDuration.MEDIUM,
        actionLabel: String? = null,
        onAction: (() -> Unit)? = null
    ) {
        _feedbackEvents.emit(
            FeedbackEvent.Info(
                message = message,
                duration = duration,
                actionLabel = actionLabel,
                onAction = onAction
            )
        )
    }

    /**
     * Show confirmation dialog
     */
    suspend fun showConfirmation(
        title: String,
        message: String,
        confirmLabel: String = "Confirm",
        cancelLabel: String = "Cancel",
        destructive: Boolean = false
    ): Boolean = suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { _ ->
            // Handle cancellation if needed
        }
        scope.launch {
            _feedbackEvents.emit(
                FeedbackEvent.Confirmation(
                    title = title,
                    message = message,
                    confirmLabel = confirmLabel,
                    cancelLabel = cancelLabel,
                    destructive = destructive,
                    onResult = { confirmed ->
                        if (continuation.isActive) {
                            continuation.resumeWith(Result.success(confirmed))
                        }
                    }
                )
            )
        }
    }

    /**
     * Show validation feedback for settings
     */
    suspend fun showValidationFeedback(
        settingKey: String,
        errors: List<String>,
        warnings: List<String> = emptyList()
    ) {
        if (errors.isNotEmpty()) {
            showError(
                message = "Validation failed: ${errors.joinToString(", ")}",
                duration = FeedbackDuration.LONG
            )
        } else if (warnings.isNotEmpty()) {
            showWarning(
                message = "Warning: ${warnings.joinToString(", ")}",
                duration = FeedbackDuration.MEDIUM
            )
        }
    }

    /**
     * Show operation feedback with loading and completion
     */
    suspend fun <T> withFeedback(
        operationId: String,
        loadingMessage: String,
        successMessage: String? = null,
        operation: suspend () -> T
    ): Result<T> {
        return try {
            showLoading(operationId, loadingMessage)

            val result = operation()

            hideLoading(operationId)

            if (successMessage != null) {
                showSuccess(successMessage)
            }

            Result.success(result)
        } catch (e: Exception) {
            hideLoading(operationId)
            showError("Operation failed: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Show progress operation feedback
     */
    suspend fun <T> withProgressFeedback(
        operationId: String,
        initialMessage: String,
        successMessage: String? = null,
        operation: suspend (updateProgress: (Float, String?) -> Unit) -> T
    ): Result<T> {
        return try {
            showProgress(operationId, initialMessage, 0f, indeterminate = false)

            val result = operation { progress, message ->
                updateProgress(operationId, progress, message)
            }

            updateProgress(operationId, 1f, "Completed")
            delay(500) // Brief delay to show completion
            hideProgress(operationId)

            if (successMessage != null) {
                showSuccess(successMessage)
            }

            Result.success(result)
        } catch (e: Exception) {
            hideProgress(operationId)
            showError("Operation failed: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Perform haptic feedback with null safety
     */
    private fun performHapticFeedback(type: HapticType) {
        if (vibrator?.hasVibrator() != true) return

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val effect = when (type) {
                    HapticType.SUCCESS -> android.os.VibrationEffect.createWaveform(
                        longArrayOf(0, 50, 50, 50), -1
                    )
                    HapticType.ERROR -> android.os.VibrationEffect.createWaveform(
                        longArrayOf(0, 100, 50, 100, 50, 100), -1
                    )
                    HapticType.WARNING -> android.os.VibrationEffect.createOneShot(75, 150)
                    HapticType.INFO -> android.os.VibrationEffect.createOneShot(25, 100)
                    HapticType.CLICK -> android.os.VibrationEffect.createOneShot(10, 50)
                }
                vibrator?.vibrate(effect)
            } else {
                val pattern = when (type) {
                    HapticType.SUCCESS -> longArrayOf(0, 50, 50, 50)
                    HapticType.ERROR -> longArrayOf(0, 100, 50, 100, 50, 100)
                    HapticType.WARNING -> longArrayOf(0, 75)
                    HapticType.INFO -> longArrayOf(0, 25)
                    HapticType.CLICK -> longArrayOf(0, 10)
                }
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
            }
        } catch (e: Exception) {
            // Ignore vibration errors
        }
    }

    /**
     * Get current loading states
     */
    fun getLoadingStates(): Map<String, LoadingState> = _loadingStates.value

    /**
     * Get current progress states
     */
    fun getProgressStates(): Map<String, ProgressState> = _progressStates.value

    /**
     * Check if any operation is loading
     */
    fun isAnyOperationLoading(): Boolean = _loadingStates.value.isNotEmpty()

    /**
     * Check if specific operation is loading
     */
    fun isOperationLoading(operationId: String): Boolean = _loadingStates.value.containsKey(operationId)

    /**
     * Clear all loading and progress states
     */
    fun clearAllStates() {
        _loadingStates.value = emptyMap()
        _progressStates.value = emptyMap()
    }

    /**
     * Dispose resources
     */
    fun dispose() {
        scope.cancel()
        clearAllStates()
    }
}

/**
 * Loading state sealed class
 */
sealed class LoadingState {
    data class Loading(
        val message: String,
        val cancellable: Boolean = false,
        val onCancel: (() -> Unit)? = null,
        val startTime: Long = System.currentTimeMillis()
    ) : LoadingState()
}

/**
 * Progress state data class
 */
data class ProgressState(
    val message: String,
    val progress: Float, // 0.0 to 1.0
    val indeterminate: Boolean = false,
    val startTime: Long = System.currentTimeMillis()
)

/**
 * Feedback event sealed class
 */
sealed class FeedbackEvent {
    data class Success(
        val message: String,
        val duration: FeedbackDuration,
        val actionLabel: String? = null,
        val onAction: (() -> Unit)? = null
    ) : FeedbackEvent()

    data class Error(
        val message: String,
        val duration: FeedbackDuration,
        val actionLabel: String? = null,
        val onAction: (() -> Unit)? = null
    ) : FeedbackEvent()

    data class Warning(
        val message: String,
        val duration: FeedbackDuration,
        val actionLabel: String? = null,
        val onAction: (() -> Unit)? = null
    ) : FeedbackEvent()

    data class Info(
        val message: String,
        val duration: FeedbackDuration,
        val actionLabel: String? = null,
        val onAction: (() -> Unit)? = null
    ) : FeedbackEvent()

    data class Confirmation(
        val title: String,
        val message: String,
        val confirmLabel: String = "Confirm",
        val cancelLabel: String = "Cancel",
        val destructive: Boolean = false,
        val onResult: (Boolean) -> Unit
    ) : FeedbackEvent()
}

/**
 * Feedback duration enum
 */
enum class FeedbackDuration {
    SHORT,   // ~2 seconds
    MEDIUM,  // ~4 seconds
    LONG,    // ~6 seconds
    INDEFINITE // Until dismissed
}

/**
 * Haptic feedback types
 */
enum class HapticType {
    SUCCESS,
    ERROR,
    WARNING,
    INFO,
    CLICK
}

/**
 * Extension functions for easier usage
 */
fun FeedbackDuration.toSnackbarDuration(): SnackbarDuration {
    return when (this) {
        FeedbackDuration.SHORT -> SnackbarDuration.Short
        FeedbackDuration.MEDIUM -> SnackbarDuration.Short
        FeedbackDuration.LONG -> SnackbarDuration.Long
        FeedbackDuration.INDEFINITE -> SnackbarDuration.Indefinite
    }
}

/**
 * Settings-specific feedback extensions
 */
object SettingsFeedback {

    suspend fun SettingsFeedbackManager.showSettingSaved(settingName: String) {
        showSuccess("$settingName saved successfully")
    }

    suspend fun SettingsFeedbackManager.showSettingValidationError(settingName: String, error: String) {
        showError("$settingName: $error")
    }

    suspend fun SettingsFeedbackManager.showBackupCompleted(fileName: String) {
        showSuccess("Settings backed up to $fileName")
    }

    suspend fun SettingsFeedbackManager.showRestoreCompleted(settingsCount: Int) {
        showSuccess("Successfully restored $settingsCount settings")
    }

    suspend fun SettingsFeedbackManager.showSyncCompleted() {
        showSuccess("Settings synchronized successfully")
    }

    suspend fun SettingsFeedbackManager.showPermissionRequired(permission: String) {
        showWarning(
            message = "Permission required: $permission",
            actionLabel = "Grant",
            onAction = {
                // Handle permission request
            }
        )
    }

    suspend fun SettingsFeedbackManager.confirmResetSettings(): Boolean {
        return showConfirmation(
            title = "Reset Settings",
            message = "This will reset all settings to their default values. This action cannot be undone.",
            confirmLabel = "Reset",
            cancelLabel = "Cancel",
            destructive = true
        )
    }

    suspend fun SettingsFeedbackManager.confirmDeleteData(): Boolean {
        return showConfirmation(
            title = "Delete All Data",
            message = "This will permanently delete all your settings and data. This action cannot be undone.",
            confirmLabel = "Delete",
            cancelLabel = "Cancel",
            destructive = true
        )
    }
}