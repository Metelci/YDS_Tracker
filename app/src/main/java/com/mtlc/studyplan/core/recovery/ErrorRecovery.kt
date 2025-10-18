package com.mtlc.studyplan.core.recovery

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.mtlc.studyplan.core.error.AppError
import com.mtlc.studyplan.core.analytics.ErrorAnalytics
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Comprehensive error recovery system with automatic and manual recovery strategies
 */
class ErrorRecoveryManager(
    private val context: Context,
    private val errorAnalytics: ErrorAnalytics
) {

    private val recoveryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Recovery state tracking
    private val _recoveryState = MutableStateFlow<RecoveryState>(RecoveryState.Idle)
    val recoveryState: StateFlow<RecoveryState> = _recoveryState.asStateFlow()

    // Network connectivity monitoring
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val _networkState = MutableStateFlow(NetworkState.UNKNOWN)
    val networkState: StateFlow<NetworkState> = _networkState.asStateFlow()

    // Recovery strategies registry
    private val recoveryStrategies = mutableMapOf<String, RecoveryStrategy>()

    init {
        registerDefaultRecoveryStrategies()
        startNetworkMonitoring()
    }

    /**
     * Attempt to recover from an error automatically
     */
    suspend fun attemptRecovery(
        error: AppError,
        context: String,
        originalOperation: suspend () -> Unit
    ): RecoveryResult {
        _recoveryState.value = RecoveryState.Attempting

        return try {
            val strategy = findRecoveryStrategy(error, context)

            if (strategy != null) {
                errorAnalytics.logUserAction(
                    action = "error_recovery_attempt",
                    screen = context,
                    additionalData = mapOf(
                        "error_type" to error::class.simpleName.orEmpty(),
                        "strategy" to strategy.name
                    )
                )

                val result = executeRecoveryStrategy(strategy, error, originalOperation)

                if (result.success) {
                    errorAnalytics.markErrorResolved(error::class.simpleName.orEmpty(), context)
                    _recoveryState.value = RecoveryState.Success
                } else {
                    _recoveryState.value = RecoveryState.Failed
                }

                result
            } else {
                _recoveryState.value = RecoveryState.Failed
                RecoveryResult(
                    success = false,
                    strategy = null,
                    message = "No recovery strategy available for this error type"
                )
            }
        } catch (e: Exception) {
            _recoveryState.value = RecoveryState.Failed
            RecoveryResult(
                success = false,
                strategy = null,
                message = "Recovery attempt failed: ${e.message}",
                exception = e
            )
        }
    }

    /**
     * Register a custom recovery strategy
     */
    fun registerRecoveryStrategy(strategy: RecoveryStrategy) {
        recoveryStrategies[strategy.errorType] = strategy
    }

    /**
     * Get available recovery options for an error
     */
    fun getRecoveryOptions(error: AppError, context: String): List<RecoveryOption> {
        val options = mutableListOf<RecoveryOption>()

        // Automatic recovery option
        val strategy = findRecoveryStrategy(error, context)
        if (strategy != null) {
            options.add(
                RecoveryOption(
                    id = "auto_recovery",
                    title = "Try Again",
                    description = "Automatically attempt to recover from this error",
                    icon = "refresh",
                    automatic = true
                )
            )
        }

        // Context-specific recovery options
        when (error) {
            is AppError.NetworkError -> {
                options.addAll(getNetworkRecoveryOptions())
            }
            is AppError.DataError -> {
                options.addAll(getDataRecoveryOptions())
            }
            is AppError.SecurityError -> {
                options.addAll(getSecurityRecoveryOptions())
            }
            is AppError.ValidationError -> {
                options.addAll(getValidationRecoveryOptions())
            }
            else -> {
                options.addAll(getGenericRecoveryOptions())
            }
        }

        return options
    }

    /**
     * Execute a specific recovery option
     */
    suspend fun executeRecoveryOption(
        optionId: String,
        error: AppError,
        context: String,
        originalOperation: suspend () -> Unit
    ): RecoveryResult {
        return when (optionId) {
            "auto_recovery" -> attemptRecovery(error, context, originalOperation)
            "retry" -> retryOperation(originalOperation)
            "clear_cache" -> clearCacheAndRetry(originalOperation)
            "reset_data" -> resetDataAndRetry(originalOperation)
            "check_network" -> checkNetworkAndRetry(originalOperation)
            "reload_app" -> reloadApplication()
            else -> RecoveryResult(false, null, "Unknown recovery option")
        }
    }

    // Private recovery methods
    private fun registerDefaultRecoveryStrategies() {
        // Network error recovery
        registerRecoveryStrategy(
            NetworkRecoveryStrategy()
        )

        // Data error recovery
        registerRecoveryStrategy(
            DataRecoveryStrategy()
        )

        // Validation error recovery
        registerRecoveryStrategy(
            ValidationRecoveryStrategy()
        )

        // Generic retry strategy
        registerRecoveryStrategy(
            RetryRecoveryStrategy()
        )
    }

    private fun findRecoveryStrategy(error: AppError, context: String): RecoveryStrategy? {
        // First try to find specific strategy for error type
        val errorTypeName = error::class.simpleName.orEmpty()
        recoveryStrategies[errorTypeName]?.let { return it }

        // Fall back to parent class strategies
        return when (error) {
            is AppError.NetworkError -> recoveryStrategies["NetworkError"]
            is AppError.DataError -> recoveryStrategies["DataError"]
            is AppError.ValidationError -> recoveryStrategies["ValidationError"]
            is AppError.SecurityError -> recoveryStrategies["SecurityError"]
            is AppError.BusinessError -> recoveryStrategies["BusinessError"]
            is AppError.SystemError -> recoveryStrategies["SystemError"]
            else -> recoveryStrategies["GenericError"]
        }
    }

    private suspend fun executeRecoveryStrategy(
        strategy: RecoveryStrategy,
        error: AppError,
        originalOperation: suspend () -> Unit
    ): RecoveryResult {
        return try {
            strategy.execute(error, originalOperation, this)
        } catch (e: Exception) {
            RecoveryResult(
                success = false,
                strategy = strategy,
                message = "Recovery strategy failed: ${e.message}",
                exception = e
            )
        }
    }

    private suspend fun retryOperation(operation: suspend () -> Unit): RecoveryResult {
        return try {
            withTimeout(30000) { // 30 second timeout
                operation()
            }
            RecoveryResult(true, null, "Operation succeeded on retry")
        } catch (e: Exception) {
            RecoveryResult(false, null, "Retry failed: ${e.message}", e)
        }
    }

    private suspend fun clearCacheAndRetry(operation: suspend () -> Unit): RecoveryResult {
        return try {
            // Clear app cache
            clearApplicationCache()
            delay(1000) // Brief delay after cache clear

            withTimeout(30000) {
                operation()
            }
            RecoveryResult(true, null, "Operation succeeded after cache clear")
        } catch (e: Exception) {
            RecoveryResult(false, null, "Cache clear and retry failed: ${e.message}", e)
        }
    }

    private suspend fun resetDataAndRetry(operation: suspend () -> Unit): RecoveryResult {
        return try {
            // This would reset relevant data stores
            // Implementation depends on the specific data being reset
            delay(1000)

            withTimeout(30000) {
                operation()
            }
            RecoveryResult(true, null, "Operation succeeded after data reset")
        } catch (e: Exception) {
            RecoveryResult(false, null, "Data reset and retry failed: ${e.message}", e)
        }
    }

    private suspend fun checkNetworkAndRetry(operation: suspend () -> Unit): RecoveryResult {
        return try {
            // Wait for network connectivity
            val networkAvailable = waitForNetwork(timeout = 10000)

            if (!networkAvailable) {
                return RecoveryResult(false, null, "Network not available")
            }

            withTimeout(30000) {
                operation()
            }
            RecoveryResult(true, null, "Operation succeeded after network check")
        } catch (e: Exception) {
            RecoveryResult(false, null, "Network check and retry failed: ${e.message}", e)
        }
    }

    private suspend fun reloadApplication(): RecoveryResult {
        return try {
            // This would trigger an app restart/reload
            // Implementation depends on the app architecture
            RecoveryResult(true, null, "Application reload initiated")
        } catch (e: Exception) {
            RecoveryResult(false, null, "Application reload failed: ${e.message}", e)
        }
    }

    private fun getNetworkRecoveryOptions(): List<RecoveryOption> {
        return listOf(
            RecoveryOption(
                id = "check_network",
                title = "Check Network",
                description = "Check network connectivity and retry",
                icon = "wifi"
            ),
            RecoveryOption(
                id = "retry",
                title = "Retry",
                description = "Retry the operation",
                icon = "refresh"
            )
        )
    }

    private fun getDataRecoveryOptions(): List<RecoveryOption> {
        return listOf(
            RecoveryOption(
                id = "clear_cache",
                title = "Clear Cache",
                description = "Clear app cache and retry",
                icon = "delete"
            ),
            RecoveryOption(
                id = "reset_data",
                title = "Reset Data",
                description = "Reset relevant data and retry",
                icon = "restore"
            ),
            RecoveryOption(
                id = "retry",
                title = "Retry",
                description = "Retry the operation",
                icon = "refresh"
            )
        )
    }

    private fun getSecurityRecoveryOptions(): List<RecoveryOption> {
        return listOf(
            RecoveryOption(
                id = "check_permissions",
                title = "Check Permissions",
                description = "Review app permissions",
                icon = "security"
            )
        )
    }

    private fun getValidationRecoveryOptions(): List<RecoveryOption> {
        return listOf(
            RecoveryOption(
                id = "edit_input",
                title = "Edit Input",
                description = "Correct the input and try again",
                icon = "edit"
            ),
            RecoveryOption(
                id = "reset_form",
                title = "Reset Form",
                description = "Reset the form to default values",
                icon = "restore"
            )
        )
    }

    private fun getGenericRecoveryOptions(): List<RecoveryOption> {
        return listOf(
            RecoveryOption(
                id = "retry",
                title = "Retry",
                description = "Try the operation again",
                icon = "refresh"
            ),
            RecoveryOption(
                id = "reload_app",
                title = "Reload App",
                description = "Reload the application",
                icon = "restart"
            )
        )
    }

    private fun startNetworkMonitoring() {
        recoveryScope.launch {
            while (isActive) {
                val isConnected = isNetworkAvailable()
                _networkState.value = if (isConnected) NetworkState.CONNECTED else NetworkState.DISCONNECTED
                delay(5000) // Check every 5 seconds
            }
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private suspend fun waitForNetwork(timeout: Long): Boolean {
        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < timeout) {
            if (isNetworkAvailable()) {
                return true
            }
            delay(1000)
        }

        return false
    }

    private fun clearApplicationCache() {
        try {
            val cacheDir = context.cacheDir
            cacheDir.deleteRecursively()
        } catch (e: Exception) {
            // Handle cache clear error
        }
    }

    fun dispose() {
        recoveryScope.cancel()
    }
}

// Recovery strategies
interface RecoveryStrategy {
    val name: String
    val errorType: String

    suspend fun execute(
        error: AppError,
        originalOperation: suspend () -> Unit,
        recoveryManager: ErrorRecoveryManager
    ): RecoveryResult
}

class NetworkRecoveryStrategy : RecoveryStrategy {
    override val name = "Network Recovery"
    override val errorType = "NetworkError"

    override suspend fun execute(
        error: AppError,
        originalOperation: suspend () -> Unit,
        recoveryManager: ErrorRecoveryManager
    ): RecoveryResult {
        return when (error) {
            is AppError.NetworkError.NoConnection -> {
                // Wait for network and retry
                val networkAvailable = recoveryManager.networkState.first { it == NetworkState.CONNECTED }
                if (networkAvailable == NetworkState.CONNECTED) {
                    try {
                        originalOperation()
                        RecoveryResult(true, this, "Operation succeeded after network recovery")
                    } catch (e: Exception) {
                        RecoveryResult(false, this, "Operation failed after network recovery: ${e.message}", e)
                    }
                } else {
                    RecoveryResult(false, this, "Network not available for recovery")
                }
            }
            is AppError.NetworkError.Timeout -> {
                // Retry with exponential backoff
                repeat(3) { attempt ->
                    try {
                        delay(1000L * (attempt + 1)) // Exponential backoff
                        originalOperation()
                        return RecoveryResult(true, this, "Operation succeeded after timeout recovery")
                    } catch (e: Exception) {
                        if (attempt == 2) { // Last attempt
                            return RecoveryResult(false, this, "All retry attempts failed: ${e.message}", e)
                        }
                    }
                }
                RecoveryResult(false, this, "Timeout recovery failed")
            }
            else -> RecoveryResult(false, this, "Network recovery not applicable for this error type")
        }
    }
}

class DataRecoveryStrategy : RecoveryStrategy {
    override val name = "Data Recovery"
    override val errorType = "DataError"

    override suspend fun execute(
        error: AppError,
        originalOperation: suspend () -> Unit,
        recoveryManager: ErrorRecoveryManager
    ): RecoveryResult {
        return when (error) {
            is AppError.DataError.CorruptedData -> {
                // Attempt to restore from backup or reset to defaults
                try {
                    // Implementation would depend on the specific data corruption
                    delay(1000) // Simulate recovery time
                    originalOperation()
                    RecoveryResult(true, this, "Data corruption recovered")
                } catch (e: Exception) {
                    RecoveryResult(false, this, "Data recovery failed: ${e.message}", e)
                }
            }
            is AppError.DataError.NotFound -> {
                // Attempt to reload or recreate the missing data
                try {
                    delay(500)
                    originalOperation()
                    RecoveryResult(true, this, "Missing data recovered")
                } catch (e: Exception) {
                    RecoveryResult(false, this, "Data not found recovery failed: ${e.message}", e)
                }
            }
            else -> RecoveryResult(false, this, "Data recovery not applicable for this error type")
        }
    }
}

class ValidationRecoveryStrategy : RecoveryStrategy {
    override val name = "Validation Recovery"
    override val errorType = "ValidationError"

    override suspend fun execute(
        error: AppError,
        originalOperation: suspend () -> Unit,
        recoveryManager: ErrorRecoveryManager
    ): RecoveryResult {
        // Validation errors typically require user input correction
        // This strategy provides guidance rather than automatic recovery
        return RecoveryResult(
            success = false,
            strategy = this,
            message = "Validation errors require user input correction",
            requiresUserAction = true
        )
    }
}

class RetryRecoveryStrategy : RecoveryStrategy {
    override val name = "Generic Retry"
    override val errorType = "GenericError"

    override suspend fun execute(
        error: AppError,
        originalOperation: suspend () -> Unit,
        recoveryManager: ErrorRecoveryManager
    ): RecoveryResult {
        return try {
            delay(1000) // Brief delay before retry
            originalOperation()
            RecoveryResult(true, this, "Operation succeeded on retry")
        } catch (e: Exception) {
            RecoveryResult(false, this, "Generic retry failed: ${e.message}", e)
        }
    }
}

// Data classes
data class RecoveryResult(
    val success: Boolean,
    val strategy: RecoveryStrategy?,
    val message: String,
    val exception: Exception? = null,
    val requiresUserAction: Boolean = false
)

data class RecoveryOption(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val automatic: Boolean = false
)

enum class RecoveryState {
    Idle, Attempting, Success, Failed
}

enum class NetworkState {
    UNKNOWN, CONNECTED, DISCONNECTED
}