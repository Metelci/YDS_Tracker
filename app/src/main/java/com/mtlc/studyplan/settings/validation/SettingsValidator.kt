package com.mtlc.studyplan.settings.validation

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.mtlc.studyplan.settings.data.ValidationResult
import com.mtlc.studyplan.settings.data.ValidationRule
import kotlinx.coroutines.flow.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Comprehensive settings validation and error handling system
 * Aligned with existing ValidationRule and ValidationResult classes
 */
class SettingsValidator(private val context: Context) {

    private val validationRules = ConcurrentHashMap<String, List<ValidationRule>>()
    private val validationCache = ConcurrentHashMap<String, ValidationResult>()

    private val _validationErrors = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val validationErrors: StateFlow<Map<String, List<String>>> = _validationErrors.asStateFlow()

    /**
     * Register validation rules for a setting
     */
    fun registerValidationRules(settingKey: String, rules: List<ValidationRule>) {
        validationRules[settingKey] = rules
        clearValidationCache(settingKey)
    }

    /**
     * Validate a single setting value using existing ValidationRule system
     */
    suspend fun validateSetting(settingKey: String, value: Any?): ValidationResult {
        val cacheKey = "$settingKey:${value.hashCode()}"

        // Check cache first
        validationCache[cacheKey]?.let { return it }

        val rules = validationRules[settingKey] ?: emptyList()
        val errors = mutableListOf<String>()

        // Apply all validation rules using existing ValidationRule.validate method
        rules.forEach { rule ->
            try {
                val result = rule.validate(value)
                if (result is ValidationResult.Invalid) {
                    errors.add(result.message)
                }
            } catch (e: Exception) {
                errors.add("Validation failed: ${e.message}")
            }
        }

        val result = if (errors.isNotEmpty()) {
            ValidationResult.Invalid(errors.firstOrNull() ?: "Validation failed")
        } else {
            ValidationResult.Valid
        }

        // Cache the result
        validationCache[cacheKey] = result

        // Update global validation state
        updateValidationState(settingKey, errors)

        return result
    }

    /**
     * Validate multiple settings at once
     */
    suspend fun validateSettings(settings: Map<String, Any?>): Map<String, ValidationResult> {
        return settings.mapValues { (key, value) ->
            validateSetting(key, value)
        }
    }

    /**
     * Validate all registered settings with simplified summary
     */
    suspend fun validateAllSettings(currentSettings: Map<String, Any?>): ValidationSummary {
        val results = mutableMapOf<String, ValidationResult>()
        val allErrors = mutableListOf<String>()

        validationRules.keys.forEach { settingKey ->
            val value = currentSettings[settingKey]
            val result = validateSetting(settingKey, value)
            results[settingKey] = result

            if (result is ValidationResult.Invalid) {
                allErrors.add(result.message)
            }
        }

        return ValidationSummary(
            results = results,
            totalErrors = allErrors.size,
            isValid = allErrors.isEmpty(),
            errorMessages = allErrors
        )
    }

    /**
     * Clear validation cache for a setting
     */
    fun clearValidationCache(settingKey: String? = null) {
        if (settingKey != null) {
            validationCache.keys.removeAll { it.startsWith("$settingKey:") }
        } else {
            validationCache.clear()
        }
    }

    /**
     * Update global validation state
     */
    private fun updateValidationState(settingKey: String, errors: List<String>) {
        val currentErrors = _validationErrors.value.toMutableMap()

        if (errors.isEmpty()) {
            currentErrors.remove(settingKey)
        } else {
            currentErrors[settingKey] = errors
        }

        _validationErrors.value = currentErrors
    }

    /**
     * Get all current validation errors
     */
    fun getAllValidationErrors(): Map<String, List<String>> {
        return _validationErrors.value
    }

    /**
     * Check if a setting has validation errors
     */
    fun hasValidationErrors(settingKey: String): Boolean {
        return _validationErrors.value[settingKey]?.isNotEmpty() == true
    }

    /**
     * Get validation errors for a specific setting
     */
    fun getValidationErrors(settingKey: String): List<String> {
        return _validationErrors.value[settingKey] ?: emptyList()
    }

    companion object {
        /**
         * Create common validation rules using existing ValidationRule classes
         */
        object Rules {
            fun required() = ValidationRule.Required
            fun minLength(min: Int) = ValidationRule.MinLength(min)
            fun maxLength(max: Int) = ValidationRule.MaxLength(max)
            fun range(min: Number, max: Number) = ValidationRule.Range(min, max)
            fun email(message: String = "Invalid email format") = ValidationRule.Email(message)
            fun custom(validator: (Any?) -> Boolean, message: String) = ValidationRule.Custom(validator, message)
        }
    }
}

/**
 * Simplified validation summary using existing ValidationResult
 */
data class ValidationSummary(
    val results: Map<String, ValidationResult>,
    val totalErrors: Int,
    val isValid: Boolean,
    val errorMessages: List<String>
)

/**
 * Settings error handler with recovery strategies
 */
class SettingsErrorHandler(private val context: Context) {

    private val _errorEvents = MutableSharedFlow<SettingsError>()
    val errorEvents: SharedFlow<SettingsError> = _errorEvents.asSharedFlow()

    /**
     * Handle a settings error with automatic recovery attempts
     */
    suspend fun handleError(error: SettingsError): ErrorHandlingResult {
        // Emit error event for UI feedback
        _errorEvents.emit(error)

        return when (error.type) {
            SettingsErrorType.VALIDATION_FAILED -> {
                handleValidationError(error)
            }
            SettingsErrorType.PERMISSION_DENIED -> {
                handlePermissionError(error)
            }
            SettingsErrorType.STORAGE_FAILED -> {
                handleStorageError(error)
            }
            SettingsErrorType.NETWORK_FAILED -> {
                handleNetworkError(error)
            }
            SettingsErrorType.DEPENDENCY_VIOLATION -> {
                handleDependencyError(error)
            }
            SettingsErrorType.UNKNOWN -> {
                ErrorHandlingResult.Failed("Unknown error occurred")
            }
        }
    }

    private suspend fun handleValidationError(error: SettingsError): ErrorHandlingResult {
        return ErrorHandlingResult.UserActionRequired(
            message = error.message,
            action = "Please correct the invalid settings and try again",
            actionType = "VALIDATION_CORRECTION"
        )
    }

    private suspend fun handlePermissionError(error: SettingsError): ErrorHandlingResult {
        return ErrorHandlingResult.UserActionRequired(
            message = "Permission required: ${error.message}",
            action = "Grant permission to continue",
            actionType = "REQUEST_PERMISSION"
        )
    }

    private suspend fun handleStorageError(error: SettingsError): ErrorHandlingResult {
        // Attempt to retry storage operation
        return try {
            // Could implement retry logic here
            ErrorHandlingResult.Retry("Storage operation failed, retrying...")
        } catch (e: Exception) {
            ErrorHandlingResult.Failed("Storage is unavailable")
        }
    }

    private suspend fun handleNetworkError(error: SettingsError): ErrorHandlingResult {
        return ErrorHandlingResult.UserActionRequired(
            message = "Network error: ${error.message}",
            action = "Check network connection and try again",
            actionType = "CHECK_NETWORK"
        )
    }

    private suspend fun handleDependencyError(error: SettingsError): ErrorHandlingResult {
        return ErrorHandlingResult.UserActionRequired(
            message = "Setting dependency not met: ${error.message}",
            action = "Enable required settings first",
            actionType = "RESOLVE_DEPENDENCY"
        )
    }
}

/**
 * Settings error data class
 */
data class SettingsError(
    val type: SettingsErrorType,
    val message: String,
    val settingKey: String? = null,
    val cause: Throwable? = null,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Settings error types
 */
enum class SettingsErrorType {
    VALIDATION_FAILED,
    PERMISSION_DENIED,
    STORAGE_FAILED,
    NETWORK_FAILED,
    DEPENDENCY_VIOLATION,
    UNKNOWN
}

/**
 * Error handling result
 */
sealed class ErrorHandlingResult {
    object Recovered : ErrorHandlingResult()
    data class Retry(val message: String) : ErrorHandlingResult()
    data class UserActionRequired(
        val message: String,
        val action: String,
        val actionType: String
    ) : ErrorHandlingResult()
    data class Failed(val message: String) : ErrorHandlingResult()
}