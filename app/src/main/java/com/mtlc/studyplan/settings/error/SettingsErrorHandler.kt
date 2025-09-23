package com.mtlc.studyplan.settings.error

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

class SettingsErrorHandler(
    private val context: Context
) {

    private val errorLog = mutableListOf<ErrorEvent>()
    private val retryStrategies = ConcurrentHashMap<String, RetryStrategy>()
    private val fallbackValues = ConcurrentHashMap<String, Any?>()
    private val errorMutex = Mutex()

    private val _errorState = MutableStateFlow(ErrorState())
    val errorState: StateFlow<ErrorState> = _errorState.asStateFlow()

    companion object {
        private const val TAG = "SettingsErrorHandler"
        private const val MAX_ERROR_LOG_SIZE = 500
        private const val DEFAULT_MAX_RETRIES = 3
        private const val DEFAULT_RETRY_DELAY_MS = 1000L
    }

    data class ErrorState(
        val hasErrors: Boolean = false,
        val errorCount: Int = 0,
        val lastErrorTime: Long? = null,
        val criticalErrorCount: Int = 0,
        val isRecoveryMode: Boolean = false
    )

    data class ErrorEvent(
        val timestamp: Long,
        val errorType: ErrorType,
        val severity: ErrorSeverity,
        val operation: String,
        val message: String,
        val exception: Throwable?,
        val context: Map<String, Any> = emptyMap(),
        val recoveryAction: RecoveryAction? = null
    )

    enum class ErrorType {
        NETWORK_ERROR,
        IO_ERROR,
        VALIDATION_ERROR,
        SECURITY_ERROR,
        CORRUPTION_ERROR,
        PERMISSION_ERROR,
        MEMORY_ERROR,
        TIMEOUT_ERROR,
        UNKNOWN_ERROR
    }

    enum class ErrorSeverity {
        LOW,      // Non-critical, operation can continue
        MEDIUM,   // Important but recoverable
        HIGH,     // Significant impact, needs attention
        CRITICAL  // System-threatening, immediate action needed
    }

    enum class RecoveryAction {
        RETRY,
        FALLBACK,
        RESET,
        IGNORE,
        USER_INTERVENTION
    }

    data class RetryStrategy(
        val maxRetries: Int = DEFAULT_MAX_RETRIES,
        val delayMs: Long = DEFAULT_RETRY_DELAY_MS,
        val backoffMultiplier: Double = 1.5,
        val maxDelayMs: Long = 30000L
    )

    suspend fun <T> handleError(
        operation: String,
        error: Throwable,
        context: Map<String, Any> = emptyMap(),
        fallbackValue: T? = null,
        retryStrategy: RetryStrategy? = null
    ): ErrorHandlingResult<T> {

        val errorType = classifyError(error)
        val severity = determineSeverity(errorType, error)
        val recoveryAction = determineRecoveryAction(errorType, severity, operation)

        val errorEvent = ErrorEvent(
            timestamp = System.currentTimeMillis(),
            errorType = errorType,
            severity = severity,
            operation = operation,
            message = error.message ?: "Unknown error",
            exception = error,
            context = context,
            recoveryAction = recoveryAction
        )

        logError(errorEvent)

        return when (recoveryAction) {
            RecoveryAction.RETRY -> {
                val strategy = retryStrategy ?: getDefaultRetryStrategy(operation)
                handleRetry(operation, error, strategy, fallbackValue)
            }
            RecoveryAction.FALLBACK -> {
                val typedFallback = resolveFallbackValue(operation, fallbackValue)
                val message = if (typedFallback != null) {
                    "Used fallback value"
                } else {
                    "Fallback value unavailable"
                }
                ErrorHandlingResult.Recovered(typedFallback, message)
            }
            RecoveryAction.RESET -> {
                handleReset(operation)
                ErrorHandlingResult.Recovered(fallbackValue, "System reset performed")
            }
            RecoveryAction.IGNORE -> {
                ErrorHandlingResult.Ignored("Error ignored based on policy")
            }
            RecoveryAction.USER_INTERVENTION -> {
                ErrorHandlingResult.RequiresUserAction(
                    message = generateUserFriendlyMessage(errorEvent),
                    suggestedActions = getSuggestedUserActions(errorEvent)
                )
            }
        }
    }

    private fun classifyError(error: Throwable): ErrorType {
        return when (error) {
            is IOException -> ErrorType.IO_ERROR
            is SecurityException -> ErrorType.SECURITY_ERROR
            is IllegalArgumentException -> ErrorType.VALIDATION_ERROR
            is OutOfMemoryError -> ErrorType.MEMORY_ERROR
            is java.util.concurrent.TimeoutException -> ErrorType.TIMEOUT_ERROR
            is java.net.UnknownHostException,
            is java.net.ConnectException -> ErrorType.NETWORK_ERROR
            else -> {
                // Try to classify by message
                val message = error.message?.lowercase() ?: ""
                when {
                    message.contains("permission") -> ErrorType.PERMISSION_ERROR
                    message.contains("corrupt") -> ErrorType.CORRUPTION_ERROR
                    message.contains("network") -> ErrorType.NETWORK_ERROR
                    message.contains("timeout") -> ErrorType.TIMEOUT_ERROR
                    else -> ErrorType.UNKNOWN_ERROR
                }
            }
        }
    }

    private fun determineSeverity(errorType: ErrorType, error: Throwable): ErrorSeverity {
        return when (errorType) {
            ErrorType.SECURITY_ERROR -> ErrorSeverity.CRITICAL
            ErrorType.CORRUPTION_ERROR -> ErrorSeverity.CRITICAL
            ErrorType.MEMORY_ERROR -> ErrorSeverity.HIGH
            ErrorType.PERMISSION_ERROR -> ErrorSeverity.HIGH
            ErrorType.IO_ERROR -> ErrorSeverity.MEDIUM
            ErrorType.NETWORK_ERROR -> ErrorSeverity.MEDIUM
            ErrorType.TIMEOUT_ERROR -> ErrorSeverity.MEDIUM
            ErrorType.VALIDATION_ERROR -> ErrorSeverity.LOW
            ErrorType.UNKNOWN_ERROR -> ErrorSeverity.MEDIUM
        }
    }

    private fun determineRecoveryAction(
        errorType: ErrorType,
        severity: ErrorSeverity,
        operation: String
    ): RecoveryAction {
        return when {
            severity == ErrorSeverity.CRITICAL -> RecoveryAction.USER_INTERVENTION
            errorType == ErrorType.NETWORK_ERROR -> RecoveryAction.RETRY
            errorType == ErrorType.TIMEOUT_ERROR -> RecoveryAction.RETRY
            errorType == ErrorType.IO_ERROR && operation.contains("read") -> RecoveryAction.FALLBACK
            errorType == ErrorType.VALIDATION_ERROR -> RecoveryAction.FALLBACK
            errorType == ErrorType.CORRUPTION_ERROR -> RecoveryAction.RESET
            severity == ErrorSeverity.LOW -> RecoveryAction.IGNORE
            else -> RecoveryAction.FALLBACK
        }
    }

    private suspend fun <T> handleRetry(
        operation: String,
        originalError: Throwable,
        strategy: RetryStrategy,
        fallbackValue: T?
    ): ErrorHandlingResult<T> {
        var lastError = originalError
        var delay = strategy.delayMs

        repeat(strategy.maxRetries) { attempt ->
            try {
                kotlinx.coroutines.delay(delay)

                // The actual retry would be handled by the calling code
                // This is just the framework for retry logic
                return ErrorHandlingResult.Retry(attempt + 1, "Retry attempt ${attempt + 1}")
            } catch (e: Exception) {
                lastError = e
                delay = minOf(
                    (delay * strategy.backoffMultiplier).toLong(),
                    strategy.maxDelayMs
                )

                logError(ErrorEvent(
                    timestamp = System.currentTimeMillis(),
                    errorType = classifyError(e),
                    severity = ErrorSeverity.LOW,
                    operation = "$operation-retry-${attempt + 1}",
                    message = "Retry attempt ${attempt + 1} failed: ${e.message}",
                    exception = e
                ))
            }
        }

        // All retries failed, fall back
        return ErrorHandlingResult.Failed(
            error = lastError,
            message = "All retry attempts failed",
            fallbackValue = fallbackValue
        )
    }

    private suspend fun handleReset(operation: String) {
        logError(ErrorEvent(
            timestamp = System.currentTimeMillis(),
            errorType = ErrorType.UNKNOWN_ERROR,
            severity = ErrorSeverity.HIGH,
            operation = operation,
            message = "Performing system reset due to critical error",
            exception = null,
            recoveryAction = RecoveryAction.RESET
        ))

        // Clear error state
        _errorState.value = ErrorState()

        // Clear error log
        errorMutex.withLock {
            errorLog.clear()
        }

        // Reset retry strategies
        retryStrategies.clear()
    }

    private fun generateUserFriendlyMessage(errorEvent: ErrorEvent): String {
        return when (errorEvent.errorType) {
            ErrorType.NETWORK_ERROR -> "Unable to connect to the network. Please check your internet connection and try again."
            ErrorType.IO_ERROR -> "There was a problem accessing your settings. Please try again."
            ErrorType.VALIDATION_ERROR -> "Some of your settings contain invalid values. They have been reset to safe defaults."
            ErrorType.SECURITY_ERROR -> "A security issue was detected. Your settings have been protected from unauthorized access."
            ErrorType.CORRUPTION_ERROR -> "Your settings file appears to be corrupted. A backup has been restored."
            ErrorType.PERMISSION_ERROR -> "The app doesn't have permission to access your settings. Please check app permissions."
            ErrorType.MEMORY_ERROR -> "The device is running low on memory. Some features may be temporarily unavailable."
            ErrorType.TIMEOUT_ERROR -> "The operation took too long to complete. Please try again."
            ErrorType.UNKNOWN_ERROR -> "An unexpected error occurred. The issue has been logged for investigation."
        }
    }

    private fun getSuggestedUserActions(errorEvent: ErrorEvent): List<String> {
        return when (errorEvent.errorType) {
            ErrorType.NETWORK_ERROR -> listOf(
                "Check your internet connection",
                "Try again in a few moments",
                "Use offline mode if available"
            )
            ErrorType.IO_ERROR -> listOf(
                "Restart the app",
                "Check available storage space",
                "Contact support if the problem persists"
            )
            ErrorType.VALIDATION_ERROR -> listOf(
                "Review your recent settings changes",
                "Reset to default values",
                "Contact support for assistance"
            )
            ErrorType.SECURITY_ERROR -> listOf(
                "Change your password",
                "Review app permissions",
                "Contact support immediately"
            )
            ErrorType.CORRUPTION_ERROR -> listOf(
                "Restore from backup",
                "Reset to default settings",
                "Contact support for data recovery"
            )
            ErrorType.PERMISSION_ERROR -> listOf(
                "Check app permissions in system settings",
                "Grant necessary permissions",
                "Restart the app"
            )
            ErrorType.MEMORY_ERROR -> listOf(
                "Close other apps",
                "Restart the device",
                "Clear app cache"
            )
            ErrorType.TIMEOUT_ERROR -> listOf(
                "Try again",
                "Check network connection",
                "Wait a moment before retrying"
            )
            ErrorType.UNKNOWN_ERROR -> listOf(
                "Restart the app",
                "Update to the latest version",
                "Contact support with error details"
            )
        }
    }

    private suspend fun logError(errorEvent: ErrorEvent) {
        errorMutex.withLock {
            errorLog.add(errorEvent)

            // Maintain log size
            if (errorLog.size > MAX_ERROR_LOG_SIZE) {
                errorLog.removeAt(0)
            }

            // Update error state
            updateErrorState(errorEvent)

            // Log to system
            when (errorEvent.severity) {
                ErrorSeverity.LOW -> Log.d(TAG, "Error: ${errorEvent.operation} - ${errorEvent.message}")
                ErrorSeverity.MEDIUM -> Log.w(TAG, "Error: ${errorEvent.operation} - ${errorEvent.message}", errorEvent.exception)
                ErrorSeverity.HIGH -> Log.e(TAG, "Error: ${errorEvent.operation} - ${errorEvent.message}", errorEvent.exception)
                ErrorSeverity.CRITICAL -> {
                    Log.e(TAG, "CRITICAL Error: ${errorEvent.operation} - ${errorEvent.message}", errorEvent.exception)
                    // Could trigger crash reporting or alerting here
                }
            }
        }
    }

    private fun updateErrorState(errorEvent: ErrorEvent) {
        val currentState = _errorState.value

        val newErrorCount = currentState.errorCount + 1
        val newCriticalCount = if (errorEvent.severity == ErrorSeverity.CRITICAL) {
            currentState.criticalErrorCount + 1
        } else {
            currentState.criticalErrorCount
        }

        val isRecoveryMode = newCriticalCount > 0 ||
                           (newErrorCount > 10 && System.currentTimeMillis() - (currentState.lastErrorTime ?: 0) < 60000)

        _errorState.value = currentState.copy(
            hasErrors = true,
            errorCount = newErrorCount,
            lastErrorTime = errorEvent.timestamp,
            criticalErrorCount = newCriticalCount,
            isRecoveryMode = isRecoveryMode
        )
    }

    fun setFallbackValue(operation: String, value: Any?) {
        fallbackValues[operation] = value
    }

    private fun getFallbackValue(operation: String): Any? {
        return fallbackValues[operation]
    }

    /**
     * Type-safe fallback value registry using sealed class pattern
     */
    sealed class FallbackValue {
        data class BooleanFallback(val value: Boolean) : FallbackValue()
        data class StringFallback(val value: String) : FallbackValue()
        data class IntFallback(val value: Int) : FallbackValue()
        data class FloatFallback(val value: Float) : FallbackValue()

        companion object {
            fun from(value: Any?): FallbackValue? {
                return when (value) {
                    is Boolean -> BooleanFallback(value)
                    is String -> StringFallback(value)
                    is Int -> IntFallback(value)
                    is Float -> FloatFallback(value)
                    else -> null
                }
            }
        }

        fun extractBoolean(): Boolean? = if (this is BooleanFallback) value else null
        fun extractString(): String? = if (this is StringFallback) value else null
        fun extractInt(): Int? = if (this is IntFallback) value else null
        fun extractFloat(): Float? = if (this is FloatFallback) value else null
    }

    // Type-specific fallback resolution methods
    fun resolveBooleanFallback(operation: String, explicitFallback: Boolean?): Boolean? {
        if (explicitFallback != null) return explicitFallback
        val storedValue = fallbackValues[operation] ?: return null
        return FallbackValue.from(storedValue)?.extractBoolean()
    }

    fun resolveStringFallback(operation: String, explicitFallback: String?): String? {
        if (explicitFallback != null) return explicitFallback
        val storedValue = fallbackValues[operation] ?: return null
        return FallbackValue.from(storedValue)?.extractString()
    }

    fun resolveIntFallback(operation: String, explicitFallback: Int?): Int? {
        if (explicitFallback != null) return explicitFallback
        val storedValue = fallbackValues[operation] ?: return null
        return FallbackValue.from(storedValue)?.extractInt()
    }

    fun resolveFloatFallback(operation: String, explicitFallback: Float?): Float? {
        if (explicitFallback != null) return explicitFallback
        val storedValue = fallbackValues[operation] ?: return null
        return FallbackValue.from(storedValue)?.extractFloat()
    }

    // Generic fallback for backward compatibility - avoid if possible
    @Suppress("UNCHECKED_CAST") // Generic fallback method - prefer type-specific methods above
    private fun <T> resolveFallbackValue(operation: String, explicitFallback: T?): T? {
        if (explicitFallback != null) {
            return explicitFallback
        }

        val storedValue = fallbackValues[operation] ?: return null

        return try {
            when (storedValue) {
                is Boolean -> if (explicitFallback is Boolean?) storedValue as T else null
                is String -> if (explicitFallback is String?) storedValue as T else null
                is Int -> if (explicitFallback is Int?) storedValue as T else null
                is Float -> if (explicitFallback is Float?) storedValue as T else null
                else -> storedValue as? T
            }
        } catch (e: ClassCastException) {
            Log.w(TAG, "Fallback value for $operation has unexpected type ${storedValue.javaClass.simpleName}")
            null
        }
    }

    fun setRetryStrategy(operation: String, strategy: RetryStrategy) {
        retryStrategies[operation] = strategy
    }

    private fun getDefaultRetryStrategy(operation: String): RetryStrategy {
        return retryStrategies[operation] ?: when {
            operation.contains("network") -> RetryStrategy(maxRetries = 5, delayMs = 2000L)
            operation.contains("io") -> RetryStrategy(maxRetries = 3, delayMs = 1000L)
            operation.contains("backup") -> RetryStrategy(maxRetries = 2, delayMs = 5000L)
            else -> RetryStrategy()
        }
    }

    fun generateErrorReport(): ErrorReport {
        val recentErrors = errorLog.takeLast(50)
        val errorTypeCounts = recentErrors.groupingBy { it.errorType }.eachCount()
        val severityCounts = recentErrors.groupingBy { it.severity }.eachCount()
        val operationCounts = recentErrors.groupingBy { it.operation }.eachCount()

        return ErrorReport(
            totalErrors = errorLog.size,
            recentErrors = recentErrors,
            errorTypeCounts = errorTypeCounts,
            severityCounts = severityCounts,
            operationCounts = operationCounts,
            currentErrorState = _errorState.value,
            recommendations = generateErrorRecommendations()
        )
    }

    private fun generateErrorRecommendations(): List<String> {
        val recommendations = mutableListOf<String>()
        val state = _errorState.value

        if (state.criticalErrorCount > 0) {
            recommendations.add("Address critical errors immediately")
            recommendations.add("Consider system reset if problems persist")
        }

        if (state.isRecoveryMode) {
            recommendations.add("System is in recovery mode - monitor carefully")
            recommendations.add("Avoid non-essential operations")
        }

        if (state.errorCount > 50) {
            recommendations.add("High error rate detected - investigate root cause")
            recommendations.add("Consider updating application")
        }

        val recentErrors = errorLog.takeLast(10)
        val networkErrors = recentErrors.count { it.errorType == ErrorType.NETWORK_ERROR }
        if (networkErrors > 5) {
            recommendations.add("Multiple network errors - check connectivity")
        }

        val ioErrors = recentErrors.count { it.errorType == ErrorType.IO_ERROR }
        if (ioErrors > 3) {
            recommendations.add("Multiple I/O errors - check storage and permissions")
        }

        return recommendations
    }

    fun clearErrorHistory() {
        errorLog.clear()
        _errorState.value = ErrorState()
    }

    sealed class ErrorHandlingResult<T> {
        data class Success<T>(val value: T) : ErrorHandlingResult<T>()
        data class Recovered<T>(val value: T?, val message: String) : ErrorHandlingResult<T>()
        data class Failed<T>(val error: Throwable, val message: String, val fallbackValue: T?) : ErrorHandlingResult<T>()
        data class Retry<T>(val attemptNumber: Int, val message: String) : ErrorHandlingResult<T>()
        data class Ignored<T>(val message: String) : ErrorHandlingResult<T>()
        data class RequiresUserAction<T>(
            val message: String,
            val suggestedActions: List<String>
        ) : ErrorHandlingResult<T>()
    }

    data class ErrorReport(
        val totalErrors: Int,
        val recentErrors: List<ErrorEvent>,
        val errorTypeCounts: Map<ErrorType, Int>,
        val severityCounts: Map<ErrorSeverity, Int>,
        val operationCounts: Map<String, Int>,
        val currentErrorState: ErrorState,
        val recommendations: List<String>
    )
}
