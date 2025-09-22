package com.mtlc.studyplan.error

import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.IOException
import java.net.UnknownHostException
import java.sql.SQLException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Modern Flow-based Error Handling System
 * Provides user-friendly error messages and recovery options
 */
@Singleton
class ErrorHandler @Inject constructor(
    private val context: Context
) {

    private val _errors = MutableSharedFlow<ErrorEvent>()
    val errors: SharedFlow<ErrorEvent> = _errors.asSharedFlow()

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    suspend fun handleError(
        throwable: Throwable,
        context: String = "",
        retryAction: (suspend () -> Unit)? = null,
        severity: ErrorSeverity = ErrorSeverity.MEDIUM
    ) {
        val errorInfo = categorizeError(throwable)

        val errorEvent = ErrorEvent(
            id = UUID.randomUUID().toString(),
            error = errorInfo,
            context = context,
            retryAction = retryAction,
            severity = severity,
            timestamp = System.currentTimeMillis()
        )

        // Log error for debugging
        logError(errorEvent)

        // Emit error event for UI handling
        _errors.emit(errorEvent)
    }

    private fun categorizeError(throwable: Throwable): ErrorInfo {
        return when (throwable) {
            is IOException, is UnknownHostException -> {
                if (isNetworkAvailable()) {
                    ErrorInfo(
                        type = ErrorType.NETWORK,
                        title = "Connection Problem",
                        message = "Please check your internet connection and try again.",
                        userAction = "Check your network settings or try again later.",
                        isRetryable = true
                    )
                } else {
                    ErrorInfo(
                        type = ErrorType.NO_INTERNET,
                        title = "No Internet Connection",
                        message = "You're currently offline. Some features may not be available.",
                        userAction = "Connect to internet to sync your data.",
                        isRetryable = true
                    )
                }
            }

            is SecurityException -> {
                ErrorInfo(
                    type = ErrorType.PERMISSION,
                    title = "Permission Required",
                    message = "This feature requires additional permissions.",
                    userAction = "Please grant the required permissions in settings.",
                    isRetryable = false
                )
            }

            is SQLException -> {
                ErrorInfo(
                    type = ErrorType.DATABASE,
                    title = "Data Error",
                    message = "There was a problem accessing your data.",
                    userAction = "Please restart the app or contact support.",
                    isRetryable = false
                )
            }

            is IllegalArgumentException, is IllegalStateException -> {
                ErrorInfo(
                    type = ErrorType.VALIDATION,
                    title = "Invalid Input",
                    message = throwable.message ?: "Please check your input and try again.",
                    userAction = "Verify your input and try again.",
                    isRetryable = true
                )
            }

            else -> {
                ErrorInfo(
                    type = ErrorType.UNKNOWN,
                    title = "Unexpected Error",
                    message = throwable.message ?: "An unknown error occurred.",
                    userAction = "Please try again or restart the app.",
                    isRetryable = true
                )
            }
        }
    }

    private fun isNetworkAvailable(): Boolean {
        return try {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            capabilities?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        } catch (e: Exception) {
            false
        }
    }

    private fun logError(errorEvent: ErrorEvent) {
        Log.e("StudyPlan", "Error in ${errorEvent.context}: ${errorEvent.error.message}")
        // Here you would typically log to crash reporting service (Firebase Crashlytics, etc.)
    }

    fun createQuickError(
        message: String,
        isRetryable: Boolean = true,
        retryAction: (suspend () -> Unit)? = null
    ): ErrorEvent {
        return ErrorEvent(
            id = UUID.randomUUID().toString(),
            error = ErrorInfo(
                type = ErrorType.UNKNOWN,
                title = "Error",
                message = message,
                userAction = if (isRetryable) "Please try again" else "Please restart the app",
                isRetryable = isRetryable
            ),
            context = "Quick Error",
            retryAction = retryAction,
            severity = ErrorSeverity.MEDIUM,
            timestamp = System.currentTimeMillis()
        )
    }
}

data class ErrorEvent(
    val id: String,
    val error: ErrorInfo,
    val context: String,
    val retryAction: (suspend () -> Unit)?,
    val severity: ErrorSeverity,
    val timestamp: Long
)

data class ErrorInfo(
    val type: ErrorType,
    val title: String,
    val message: String,
    val userAction: String,
    val isRetryable: Boolean
)

enum class ErrorType {
    NETWORK,
    NO_INTERNET,
    AUTHENTICATION,
    PERMISSION,
    NOT_FOUND,
    SERVER,
    DATABASE,
    VALIDATION,
    UNKNOWN
}

enum class ErrorSeverity {
    LOW,     // Minor issues, don't interrupt user flow
    MEDIUM,  // Show snackbar or toast
    HIGH,    // Show dialog, require user action
    CRITICAL // Block app functionality until resolved
}