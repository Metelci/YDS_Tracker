package com.mtlc.studyplan.settings.security

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

class SettingsSecurityManager(
    private val context: Context
) {

    private val auditLog = mutableListOf<SecurityAuditEvent>()
    private val securityViolations = ConcurrentHashMap<String, SecurityViolation>()
    private val rateLimiter = RateLimiter()
    private val auditMutex = Mutex()

    // Coroutine scope for async security logging
    private val securityScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _securityState = MutableStateFlow(SecurityState())
    val securityState: StateFlow<SecurityState> = _securityState.asStateFlow()

    companion object {
        private const val TAG = "SettingsSecurityManager"
        private const val MAX_AUDIT_LOG_SIZE = 1000
        private const val MAX_STRING_LENGTH = 10000
        private const val MAX_SETTING_KEY_LENGTH = 256

        // Security patterns
        private val SQL_INJECTION_PATTERN = Pattern.compile("('|(\\-\\-)|(;)|(\\|)|(\\*)|(%))", Pattern.CASE_INSENSITIVE)
        private val XSS_PATTERN = Pattern.compile("<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE)
        private val PATH_TRAVERSAL_PATTERN = Pattern.compile("(\\.\\.[\\\\/])|(\\.\\.\\.)", Pattern.CASE_INSENSITIVE)
        private val MALICIOUS_COMMAND_PATTERN = Pattern.compile("(exec|eval|system|shell|cmd|powershell)", Pattern.CASE_INSENSITIVE)
    }

    data class SecurityState(
        val threatLevel: ThreatLevel = ThreatLevel.LOW,
        val violationCount: Int = 0,
        val isAuditingEnabled: Boolean = true,
        val lastViolationTime: Long? = null,
        val blockedOperations: Set<String> = emptySet()
    )

    enum class ThreatLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    fun validateAndSanitizeSettingKey(key: String): ValidationResult {
        return try {
            // 1. Check key length
            if (key.length > MAX_SETTING_KEY_LENGTH) {
                logSecurityEvent(SecurityEventType.VALIDATION_FAILED, "Setting key too long: ${key.length}")
                return ValidationResult(false, "Setting key exceeds maximum length", sanitizeString(key.take(MAX_SETTING_KEY_LENGTH)))
            }

            // 2. Check for malicious patterns
            val threats = detectThreats(key)
            if (threats.isNotEmpty()) {
                logSecurityEvent(SecurityEventType.THREAT_DETECTED, "Malicious patterns in key: $threats")
                return ValidationResult(false, "Malicious patterns detected: ${threats.joinToString()}", sanitizeString(key))
            }

            // 3. Validate key format
            if (!isValidKeyFormat(key)) {
                logSecurityEvent(SecurityEventType.VALIDATION_FAILED, "Invalid key format: $key")
                return ValidationResult(false, "Invalid key format", sanitizeString(key))
            }

            ValidationResult(true, null, key)
        } catch (e: Exception) {
            logSecurityEvent(SecurityEventType.ERROR, "Validation error: ${e.message}")
            ValidationResult(false, "Validation error", sanitizeString(key))
        }
    }

    fun validateAndSanitizeSettingValue(value: Any?): ValidationResult {
        return try {
            when (value) {
                null -> ValidationResult(true, null, null)
                is String -> validateStringValue(value)
                is Number -> validateNumberValue(value)
                is Boolean -> ValidationResult(true, null, value)
                is Collection<*> -> validateCollectionValue(value)
                else -> {
                    logSecurityEvent(SecurityEventType.VALIDATION_FAILED, "Unsupported value type: ${value::class.java}")
                    ValidationResult(false, "Unsupported value type", value.toString())
                }
            }
        } catch (e: Exception) {
            logSecurityEvent(SecurityEventType.ERROR, "Value validation error: ${e.message}")
            ValidationResult(false, "Validation error", value.toString())
        }
    }

    private fun validateStringValue(value: String): ValidationResult {
        // Check length
        if (value.length > MAX_STRING_LENGTH) {
            logSecurityEvent(SecurityEventType.VALIDATION_FAILED, "String value too long: ${value.length}")
            return ValidationResult(false, "String value exceeds maximum length", value.take(MAX_STRING_LENGTH))
        }

        // Check for threats
        val threats = detectThreats(value)
        if (threats.isNotEmpty()) {
            logSecurityEvent(SecurityEventType.THREAT_DETECTED, "Malicious patterns in value: $threats")
            return ValidationResult(false, "Malicious patterns detected: ${threats.joinToString()}", sanitizeString(value))
        }

        return ValidationResult(true, null, value)
    }

    private fun validateNumberValue(value: Number): ValidationResult {
        // Check for reasonable number ranges
        when (value) {
            is Int -> {
                if (value < Int.MIN_VALUE / 2 || value > Int.MAX_VALUE / 2) {
                    logSecurityEvent(SecurityEventType.VALIDATION_FAILED, "Suspicious integer value: $value")
                    return ValidationResult(false, "Number value out of safe range", 0)
                }
            }
            is Long -> {
                if (value < Long.MIN_VALUE / 2 || value > Long.MAX_VALUE / 2) {
                    logSecurityEvent(SecurityEventType.VALIDATION_FAILED, "Suspicious long value: $value")
                    return ValidationResult(false, "Number value out of safe range", 0L)
                }
            }
            is Float -> {
                if (!value.isFinite()) {
                    logSecurityEvent(SecurityEventType.VALIDATION_FAILED, "Invalid float value: $value")
                    return ValidationResult(false, "Invalid float value", 0.0f)
                }
            }
            is Double -> {
                if (!value.isFinite()) {
                    logSecurityEvent(SecurityEventType.VALIDATION_FAILED, "Invalid double value: $value")
                    return ValidationResult(false, "Invalid double value", 0.0)
                }
            }
        }

        return ValidationResult(true, null, value)
    }

    private fun validateCollectionValue(value: Collection<*>): ValidationResult {
        if (value.size > 1000) {
            logSecurityEvent(SecurityEventType.VALIDATION_FAILED, "Collection too large: ${value.size}")
            return ValidationResult(false, "Collection exceeds maximum size", value.take(1000))
        }

        // Validate each element
        for (element in value) {
            val elementResult = validateAndSanitizeSettingValue(element)
            if (!elementResult.isValid) {
                return ValidationResult(false, "Invalid collection element: ${elementResult.error}", value)
            }
        }

        return ValidationResult(true, null, value)
    }

    private fun detectThreats(input: String): List<String> {
        val threats = mutableListOf<String>()

        if (SQL_INJECTION_PATTERN.matcher(input).find()) {
            threats.add("SQL_INJECTION")
        }

        if (XSS_PATTERN.matcher(input).find()) {
            threats.add("XSS")
        }

        if (PATH_TRAVERSAL_PATTERN.matcher(input).find()) {
            threats.add("PATH_TRAVERSAL")
        }

        if (MALICIOUS_COMMAND_PATTERN.matcher(input).find()) {
            threats.add("COMMAND_INJECTION")
        }

        return threats
    }

    private fun sanitizeString(input: String): String {
        return input
            .replace(SQL_INJECTION_PATTERN.toRegex(), "")
            .replace(XSS_PATTERN.toRegex(), "")
            .replace(PATH_TRAVERSAL_PATTERN.toRegex(), "")
            .replace(MALICIOUS_COMMAND_PATTERN.toRegex(), "")
            .take(MAX_STRING_LENGTH)
    }

    private fun isValidKeyFormat(key: String): Boolean {
        // Key should contain only alphanumeric characters, underscores, and dots
        return key.matches(Regex("^[a-zA-Z0-9_.]+$"))
    }

    suspend fun checkRateLimit(operation: String, identifier: String): Boolean {
        return rateLimiter.checkRateLimit(operation, identifier)
    }

    fun logSecurityEvent(eventType: SecurityEventType, details: String, severity: SecuritySeverity = SecuritySeverity.MEDIUM) {
        // Launch coroutine for async security logging (fire-and-forget)
        securityScope.launch {
            try {
                auditMutex.withLock {
                    val event = SecurityAuditEvent(
                        timestamp = System.currentTimeMillis(),
                        eventType = eventType,
                        details = details,
                        severity = severity,
                        source = "SettingsSecurityManager"
                    )

                    auditLog.add(event)

                    // Maintain audit log size
                    if (auditLog.size > MAX_AUDIT_LOG_SIZE) {
                        auditLog.removeAt(0)
                    }

                    // Update security state
                    updateSecurityState(event)

                    // Log to system
                    when (severity) {
                        SecuritySeverity.LOW -> Log.d(TAG, "Security event: $eventType - $details")
                        SecuritySeverity.MEDIUM -> Log.w(TAG, "Security event: $eventType - $details")
                        SecuritySeverity.HIGH -> Log.e(TAG, "Security event: $eventType - $details")
                        SecuritySeverity.CRITICAL -> {
                            Log.e(TAG, "CRITICAL Security event: $eventType - $details")
                            // Could trigger additional alerting mechanisms here
                        }
                    }
                }
            } catch (e: Exception) {
                // Fallback logging if mutex operations fail
                Log.e(TAG, "Failed to log security event: ${e.message}")
            }
        }
    }

    private fun updateSecurityState(event: SecurityAuditEvent) {
        val currentState = _securityState.value

        val newViolationCount = if (event.eventType in listOf(
                SecurityEventType.THREAT_DETECTED,
                SecurityEventType.VALIDATION_FAILED,
                SecurityEventType.RATE_LIMIT_EXCEEDED
            )) {
            currentState.violationCount + 1
        } else {
            currentState.violationCount
        }

        val newThreatLevel = calculateThreatLevel(newViolationCount, event.severity)

        _securityState.value = currentState.copy(
            threatLevel = newThreatLevel,
            violationCount = newViolationCount,
            lastViolationTime = if (newViolationCount > currentState.violationCount) System.currentTimeMillis() else currentState.lastViolationTime
        )
    }

    private fun calculateThreatLevel(violationCount: Int, recentSeverity: SecuritySeverity): ThreatLevel {
        return when {
            recentSeverity == SecuritySeverity.CRITICAL -> ThreatLevel.CRITICAL
            violationCount >= 50 || recentSeverity == SecuritySeverity.HIGH -> ThreatLevel.HIGH
            violationCount >= 20 -> ThreatLevel.MEDIUM
            else -> ThreatLevel.LOW
        }
    }

    fun generateSecurityReport(): SecurityReport {
        val recentEvents = auditLog.takeLast(100)
        val threatCounts = recentEvents.groupingBy { it.eventType }.eachCount()
        val severityCounts = recentEvents.groupingBy { it.severity }.eachCount()

        return SecurityReport(
            totalEvents = auditLog.size,
            recentEvents = recentEvents,
            threatCounts = threatCounts,
            severityCounts = severityCounts,
            currentThreatLevel = _securityState.value.threatLevel,
            recommendations = generateSecurityRecommendations()
        )
    }

    private fun generateSecurityRecommendations(): List<String> {
        val recommendations = mutableListOf<String>()
        val state = _securityState.value

        when (state.threatLevel) {
            ThreatLevel.HIGH, ThreatLevel.CRITICAL -> {
                recommendations.add("Consider implementing additional authentication measures")
                recommendations.add("Review and monitor all setting changes")
                recommendations.add("Enable enhanced logging")
            }
            ThreatLevel.MEDIUM -> {
                recommendations.add("Monitor for unusual activity patterns")
                recommendations.add("Review recent setting changes")
            }
            ThreatLevel.LOW -> {
                recommendations.add("Continue monitoring security events")
            }
        }

        if (state.violationCount > 10) {
            recommendations.add("Investigate source of validation failures")
        }

        return recommendations
    }

    fun getSecurityHash(data: String): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val hashBytes = md.digest(data.toByteArray())
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            logSecurityEvent(SecurityEventType.ERROR, "Failed to generate security hash: ${e.message}")
            ""
        }
    }

    fun verifyIntegrity(data: String, expectedHash: String): Boolean {
        val actualHash = getSecurityHash(data)
        return actualHash == expectedHash
    }

    data class ValidationResult(
        val isValid: Boolean,
        val error: String?,
        val sanitizedValue: Any?
    )

    data class SecurityAuditEvent(
        val timestamp: Long,
        val eventType: SecurityEventType,
        val details: String,
        val severity: SecuritySeverity,
        val source: String
    )

    data class SecurityReport(
        val totalEvents: Int,
        val recentEvents: List<SecurityAuditEvent>,
        val threatCounts: Map<SecurityEventType, Int>,
        val severityCounts: Map<SecuritySeverity, Int>,
        val currentThreatLevel: ThreatLevel,
        val recommendations: List<String>
    )

    data class SecurityViolation(
        val type: String,
        val timestamp: Long,
        val details: String
    )

    enum class SecurityEventType {
        VALIDATION_FAILED,
        THREAT_DETECTED,
        RATE_LIMIT_EXCEEDED,
        UNAUTHORIZED_ACCESS,
        DATA_INTEGRITY_VIOLATION,
        ERROR,
        INFO
    }

    enum class SecuritySeverity {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    private class RateLimiter {
        private val operationCounts = ConcurrentHashMap<String, MutableMap<String, RateLimitEntry>>()

        suspend fun checkRateLimit(operation: String, identifier: String): Boolean {
            val now = System.currentTimeMillis()
            val operationMap = operationCounts.getOrPut(operation) { ConcurrentHashMap() }

            val entry = operationMap.getOrPut(identifier) { RateLimitEntry(0, now) }

            // Reset count if enough time has passed (1 minute window)
            if (now - entry.lastReset > 60000) {
                entry.count = 0
                entry.lastReset = now
            }

            entry.count++

            // Different limits for different operations
            val limit = when (operation) {
                "setting_write" -> 100 // 100 writes per minute
                "setting_read" -> 1000 // 1000 reads per minute
                "search" -> 50 // 50 searches per minute
                "backup" -> 5 // 5 backups per minute
                else -> 50
            }

            return entry.count <= limit
        }

        private data class RateLimitEntry(
            var count: Int,
            var lastReset: Long
        )
    }
}