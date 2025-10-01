package com.mtlc.studyplan.core.error

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.ConcurrentHashMap

enum class ErrorType {
    NETWORK,
    VALIDATION,
    FILE_IO,
    PERMISSION,
    DATA,
    SECURITY,
    UNKNOWN
}

/**
 * Comprehensive error types for the StudyPlan application
 */
sealed class AppError(
    override val message: String,
    open val userMessage: String,
    open val isRecoverable: Boolean = true,
    override val cause: Throwable? = null
) : Throwable(message, cause) {

    data class Generic(
        val errorType: ErrorType,
        override val message: String,
        override val userMessage: String,
        override val isRecoverable: Boolean = true,
        override val cause: Throwable? = null
    ) : AppError(message, userMessage, isRecoverable, cause)

    companion object {
        operator fun invoke(
            type: ErrorType,
            message: String,
            userMessage: String = message,
            isRecoverable: Boolean = true,
            cause: Throwable? = null
        ): AppError = Generic(type, message, userMessage, isRecoverable, cause)
    }

    /**
     * Network-related errors
     */
    sealed class NetworkError(
        override val message: String,
        override val userMessage: String,
        override val cause: Throwable? = null
    ) : AppError(message, userMessage, true, cause) {

        object NoConnection : NetworkError(
            message = "No internet connection available",
            userMessage = "Please check your internet connection and try again"
        )

        object Timeout : NetworkError(
            message = "Network request timed out",
            userMessage = "Request took too long. Please try again"
        )

        object ServerError : NetworkError(
            message = "Server error occurred",
            userMessage = "Server is temporarily unavailable. Please try again later"
        )

        data class UnknownNetwork(
            override val cause: Throwable
        ) : NetworkError(
            message = "Unknown network error: ${cause.message}",
            userMessage = "Connection problem occurred. Please try again",
            cause = cause
        )
    }

    /**
     * Data validation errors
     */
    sealed class ValidationError(
        override val message: String,
        override val userMessage: String
    ) : AppError(message, userMessage, true) {

        object InvalidInput : ValidationError(
            message = "Invalid input provided",
            userMessage = "Please check your input and try again"
        )

        object RequiredFieldEmpty : ValidationError(
            message = "Required field is empty",
            userMessage = "Please fill in all required fields"
        )

        data class InvalidFormat(val field: String) : ValidationError(
            message = "Invalid format for field: $field",
            userMessage = "Please enter a valid $field"
        )

        data class OutOfRange(val field: String, val min: Any?, val max: Any?) : ValidationError(
            message = "Value out of range for field: $field",
            userMessage = "Please enter a value between $min and $max"
        )
    }

    /**
     * Data persistence errors
     */
    sealed class DataError(
        override val message: String,
        override val userMessage: String,
        override val cause: Throwable? = null
    ) : AppError(message, userMessage, true, cause) {

        object NotFound : DataError(
            message = "Requested data not found",
            userMessage = "The requested item could not be found"
        )

        object CorruptedData : DataError(
            message = "Data corruption detected",
            userMessage = "Data appears to be corrupted. Please restart the app"
        )

        object StorageFull : DataError(
            message = "Storage space is full",
            userMessage = "Not enough storage space. Please free up some space"
        )

        data class DatabaseError(override val cause: Throwable) : DataError(
            message = "Database error: ${cause.message}",
            userMessage = "Failed to save data. Please try again",
            cause = cause
        )
    }

    /**
     * Permission and security errors
     */
    sealed class SecurityError(
        override val message: String,
        override val userMessage: String,
        override val isRecoverable: Boolean = false
    ) : AppError(message, userMessage, isRecoverable) {

        object PermissionDenied : SecurityError(
            message = "Required permission denied",
            userMessage = "Permission required to continue. Please grant permission in settings",
            isRecoverable = true
        )

        object Unauthorized : SecurityError(
            message = "User not authorized",
            userMessage = "Please log in again to continue"
        )

        object SessionExpired : SecurityError(
            message = "Session has expired",
            userMessage = "Your session has expired. Please log in again",
            isRecoverable = true
        )
    }

    /**
     * Business logic errors
     */
    sealed class BusinessError(
        override val message: String,
        override val userMessage: String
    ) : AppError(message, userMessage, true) {

        object InvalidOperation : BusinessError(
            message = "Invalid operation attempted",
            userMessage = "This operation is not allowed at this time"
        )

        object QuotaExceeded : BusinessError(
            message = "Usage quota exceeded",
            userMessage = "You've reached your usage limit. Please upgrade or try again later"
        )

        data class ConflictError(val conflictType: String) : BusinessError(
            message = "Conflict detected: $conflictType",
            userMessage = "A conflict occurred. Please refresh and try again"
        )
    }

    /**
     * Unexpected system errors
     */
    sealed class SystemError(
        override val message: String,
        override val userMessage: String,
        override val isRecoverable: Boolean = false,
        override val cause: Throwable? = null
    ) : AppError(message, userMessage, isRecoverable, cause) {

        data class UnexpectedError(
            override val cause: Throwable
        ) : SystemError(
            message = "Unexpected error: ${cause.message}",
            userMessage = "An unexpected error occurred. Please restart the app",
            cause = cause
        )

        object OutOfMemory : SystemError(
            message = "Application out of memory",
            userMessage = "App is running low on memory. Please close other apps and try again",
            isRecoverable = true
        )
    }
}

/**
 * UI State wrapper that includes error handling
 */
data class UiState<T>(
    val data: T? = null,
    val isLoading: Boolean = false,
    val error: AppError? = null,
    val isRefreshing: Boolean = false
) {
    val isSuccess: Boolean get() = data != null && error == null && !isLoading
    val isError: Boolean get() = error != null
    val isEmpty: Boolean get() = data == null && !isLoading && error == null

    companion object {
        @Suppress("ALWAYS_TRUE_PARAMETER")
        fun <T> loading(data: T? = null): UiState<T> = UiState(data = data, isLoading = true)
        fun <T> success(data: T): UiState<T> = UiState(data = data)
        fun <T> error(error: AppError, data: T? = null): UiState<T> = UiState(data = data, error = error)
        @Suppress("ALWAYS_TRUE_PARAMETER")
        fun <T> refreshing(data: T): UiState<T> = UiState(data = data, isRefreshing = true)
    }
}

/**
 * Error mapper to convert exceptions to AppError types
 */
object ErrorMapper {

    fun mapThrowable(throwable: Throwable): AppError {
        return when (throwable) {
            is UnknownHostException, is ConnectException -> AppError.NetworkError.NoConnection
            is SocketTimeoutException -> AppError.NetworkError.Timeout
            is IOException -> AppError.NetworkError.UnknownNetwork(throwable)
            is SecurityException -> AppError.SecurityError.PermissionDenied
            is OutOfMemoryError -> AppError.SystemError.OutOfMemory
            is IllegalArgumentException -> AppError.ValidationError.InvalidInput
            is IllegalStateException -> AppError.BusinessError.InvalidOperation
            else -> AppError.SystemError.UnexpectedError(throwable)
        }
    }

    fun mapHttpError(code: Int, message: String): AppError {
        return when (code) {
            401 -> AppError.SecurityError.Unauthorized
            403 -> AppError.SecurityError.PermissionDenied
            404 -> AppError.DataError.NotFound
            408 -> AppError.NetworkError.Timeout
            429 -> AppError.BusinessError.QuotaExceeded
            in 500..599 -> AppError.NetworkError.ServerError
            else -> AppError.NetworkError.UnknownNetwork(RuntimeException("HTTP $code: $message"))
        }
    }
}

/**
 * Error logger with different log levels and crash reporting
 */
class ErrorLogger(private val context: Context) {

    private val TAG = "StudyPlan_Error"

    fun logError(error: AppError, additionalInfo: Map<String, Any> = emptyMap()) {
        val logMessage = buildString {
            appendLine("Error Type: ${error::class.simpleName}")
            appendLine("Message: ${error.message}")
            appendLine("User Message: ${error.userMessage}")
            appendLine("Recoverable: ${error.isRecoverable}")
            if (error.cause != null) {
                appendLine("Cause: ${error.cause}")
            }
            if (additionalInfo.isNotEmpty()) {
                appendLine("Additional Info:")
                additionalInfo.forEach { (key, value) ->
                    appendLine("  $key: $value")
                }
            }
        }

        when (error) {
            is AppError.SystemError -> Log.e(TAG, logMessage, error.cause)
            is AppError.NetworkError -> Log.w(TAG, logMessage, error.cause)
            is AppError.SecurityError -> Log.e(TAG, logMessage)
            is AppError.DataError -> Log.e(TAG, logMessage, error.cause)
            is AppError.ValidationError -> Log.d(TAG, logMessage)
            is AppError.BusinessError -> Log.i(TAG, logMessage)
            is AppError.Generic -> Log.w(TAG, logMessage, error.cause)
        }

        // In production, send to crash reporting service
        // Firebase Crashlytics, Bugsnag, etc.
        if (!error.isRecoverable) {
            // reportTocrashlytics(error, additionalInfo)
        }
    }

    fun logUserAction(action: String, result: String, additionalData: Map<String, Any> = emptyMap()) {
        val logMessage = "User Action: $action | Result: $result | Data: $additionalData"
        Log.i(TAG, logMessage)
    }

    // Future: Integration with crash reporting services
    private fun reportTocrashlytics(error: AppError, additionalInfo: Map<String, Any>) {
        // Implementation for Firebase Crashlytics or similar service
        // FirebaseCrashlytics.getInstance().apply {
        //     setCustomKey("error_type", error::class.simpleName ?: "Unknown")
        //     setCustomKey("user_message", error.userMessage)
        //     setCustomKey("is_recoverable", error.isRecoverable)
        //     additionalInfo.forEach { (key, value) ->
        //         setCustomKey(key, value.toString())
        //     }
        //     recordException(error.cause ?: RuntimeException(error.message))
        // }
    }
}

/**
 * Error handling utilities for ViewModels
 */
class ErrorHandler(val logger: ErrorLogger) {

    private val _globalErrors = MutableStateFlow<AppError?>(null)
    val globalErrors: StateFlow<AppError?> = _globalErrors.asStateFlow()

    // Track error frequencies to detect patterns
    private val errorFrequency = ConcurrentHashMap<String, Int>()

    suspend fun <T> handleOperation(
        operation: suspend () -> T,
        onError: ((AppError) -> Unit)? = null,
        additionalInfo: Map<String, Any> = emptyMap()
    ): Result<T> {
        return try {
            val result = operation()
            Result.success(result)
        } catch (exception: Throwable) {
            val error = ErrorMapper.mapThrowable(exception)

            // Track error frequency
            val errorKey = error::class.simpleName ?: "Unknown"
            val frequency = errorFrequency.getOrDefault(errorKey, 0) + 1
            errorFrequency[errorKey] = frequency

            // Log the error
            logger.logError(error, additionalInfo + mapOf<String, Any>("frequency" to frequency))

            // Handle the error
            onError?.invoke(error) ?: run {
                _globalErrors.value = error
            }

            Result.failure(exception)
        }
    }

    fun clearGlobalError() {
        _globalErrors.value = null
    }

    fun getErrorFrequency(): Map<String, Int> = errorFrequency.toMap()

    /**
     * Create retry mechanism with exponential backoff
     */
    suspend fun <T> withRetry(
        maxAttempts: Int = 3,
        initialDelayMs: Long = 1000,
        maxDelayMs: Long = 10000,
        factor: Double = 2.0,
        operation: suspend (attempt: Int) -> T
    ): T {
        var currentDelay = initialDelayMs
        var lastException: Throwable? = null

        repeat(maxAttempts) { attempt ->
            try {
                return operation(attempt + 1)
            } catch (e: Throwable) {
                lastException = e

                if (attempt < maxAttempts - 1) {
                    kotlinx.coroutines.delay(currentDelay)
                    currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelayMs)
                }
            }
        }

        throw lastException ?: RuntimeException("Retry failed with unknown error")
    }
}

/**
 * Extension functions for easier error handling in ViewModels
 */
suspend inline fun <T> safeCall(
    crossinline operation: suspend () -> T
): Result<T> {
    return try {
        Result.success(operation())
    } catch (exception: Throwable) {
        Result.failure(exception)
    }
}

suspend inline fun <T> MutableStateFlow<UiState<T>>.updateWithOperation(
    crossinline operation: suspend () -> T,
    errorHandler: ErrorHandler? = null
) {
    value = UiState.loading(value.data)

    val result = safeCall { operation() }

    value = result.fold(
        onSuccess = { data -> UiState.success(data) },
        onFailure = { exception ->
            val error = ErrorMapper.mapThrowable(exception)
            errorHandler?.logger?.logError(error)
            UiState.error(error, value.data)
        }
    )
}
