package com.mtlc.studyplan.core.analytics

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.mtlc.studyplan.core.error.AppError
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Comprehensive error analytics and logging system
 */
class ErrorAnalytics(private val context: Context) {

    private val TAG = "StudyPlan_Analytics"
    private val preferences: SharedPreferences = context.getSharedPreferences("error_analytics", Context.MODE_PRIVATE)

    // Error tracking
    private val errorCounts = ConcurrentHashMap<String, AtomicInteger>()
    private val errorSessions = ConcurrentHashMap<String, MutableList<ErrorSession>>()
    private val userActionCounts = ConcurrentHashMap<String, AtomicInteger>()

    // Flows for reactive UI
    private val _errorStats = MutableStateFlow(ErrorStatistics())
    val errorStats: StateFlow<ErrorStatistics> = _errorStats.asStateFlow()

    private val _recentErrors = MutableStateFlow<List<ErrorEvent>>(emptyList())
    val recentErrors: StateFlow<List<ErrorEvent>> = _recentErrors.asStateFlow()

    // Session management
    private var currentSession: AnalyticsSession? = null
    private val sessionScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        startNewSession()
        loadStoredAnalytics()
    }

    /**
     * Log an error event with comprehensive information
     */
    fun logError(
        error: AppError,
        context: String = "",
        additionalData: Map<String, Any> = emptyMap(),
        stackTrace: String? = null
    ) {
        val errorEvent = ErrorEvent(
            error = error,
            context = context,
            additionalData = additionalData,
            stackTrace = stackTrace,
            timestamp = LocalDateTime.now(),
            sessionId = currentSession?.id ?: "unknown"
        )

        sessionScope.launch {
            // Update error counts
            val errorKey = "${error::class.simpleName}_$context"
            errorCounts.computeIfAbsent(errorKey) { AtomicInteger(0) }.incrementAndGet()

            // Add to session errors
            val sessionErrors = errorSessions.computeIfAbsent(errorKey) { mutableListOf() }
            sessionErrors.add(
                ErrorSession(
                    timestamp = LocalDateTime.now(),
                    sessionId = currentSession?.id ?: "unknown",
                    resolved = false
                )
            )

            // Update recent errors (keep last 50)
            val recentList = _recentErrors.value.toMutableList()
            recentList.add(0, errorEvent)
            if (recentList.size > 50) {
                recentList.removeAt(recentList.size - 1)
            }
            _recentErrors.value = recentList

            // Update statistics
            updateErrorStatistics()

            // Persist to storage
            persistErrorEvent(errorEvent)

            // Log to system
            Log.e(TAG, "Error logged: ${error::class.simpleName} in $context", error.cause)

            // Send to analytics service (if enabled)
            if (isAnalyticsEnabled()) {
                sendToAnalyticsService(errorEvent)
            }
        }
    }

    /**
     * Log user action for context in error analysis
     */
    fun logUserAction(
        action: String,
        screen: String,
        success: Boolean = true,
        duration: Long? = null,
        additionalData: Map<String, Any> = emptyMap()
    ) {
        sessionScope.launch {
            val actionKey = "${screen}_$action"
            userActionCounts.computeIfAbsent(actionKey) { AtomicInteger(0) }.incrementAndGet()

            val actionEvent = UserActionEvent(
                action = action,
                screen = screen,
                success = success,
                duration = duration,
                additionalData = additionalData,
                timestamp = LocalDateTime.now(),
                sessionId = currentSession?.id ?: "unknown"
            )

            currentSession?.actions?.add(actionEvent)

            // Log significant actions
            if (!success || (duration != null && duration > 5000)) {
                Log.i(TAG, "User action: $action on $screen - Success: $success, Duration: ${duration}ms")
            }

            persistUserAction(actionEvent)
        }
    }

    /**
     * Mark an error as resolved
     */
    fun markErrorResolved(errorType: String, context: String) {
        sessionScope.launch {
            val errorKey = "${errorType}_$context"
            errorSessions[errorKey]?.forEach { session ->
                if (!session.resolved) {
                    session.resolved = true
                    session.resolvedAt = LocalDateTime.now()
                }
            }

            updateErrorStatistics()
            Log.i(TAG, "Error marked as resolved: $errorKey")
        }
    }

    /**
     * Get error patterns and insights
     */
    fun getErrorInsights(): ErrorInsights {
        val totalErrors = errorCounts.values.sumOf { it.get() }
        val uniqueErrors = errorCounts.size
        val unresolvedErrors = errorSessions.values.flatten().count { !it.resolved }

        val topErrors = errorCounts.entries
            .sortedByDescending { it.value.get() }
            .take(10)
            .map { ErrorFrequency(it.key, it.value.get()) }

        val errorTrends = calculateErrorTrends()
        val criticalPatterns = identifyCriticalPatterns()

        return ErrorInsights(
            totalErrors = totalErrors,
            uniqueErrors = uniqueErrors,
            unresolvedErrors = unresolvedErrors,
            topErrors = topErrors,
            errorTrends = errorTrends,
            criticalPatterns = criticalPatterns,
            sessionInfo = currentSession?.let { SessionInfo(it.id, it.startTime, it.actions.size) }
        )
    }

    /**
     * Export analytics data
     */
    fun exportAnalytics(format: String = "json"): String {
        return when (format.lowercase()) {
            "json" -> exportToJson()
            "csv" -> exportToCsv()
            else -> throw IllegalArgumentException("Unsupported format: $format")
        }
    }

    /**
     * Clear analytics data
     */
    fun clearAnalytics() {
        sessionScope.launch {
            errorCounts.clear()
            errorSessions.clear()
            userActionCounts.clear()
            _recentErrors.value = emptyList()
            _errorStats.value = ErrorStatistics()

            preferences.edit().clear().apply()
            clearLogFiles()

            Log.i(TAG, "Analytics data cleared")
        }
    }

    // Private methods
    private fun startNewSession() {
        currentSession = AnalyticsSession(
            id = generateSessionId(),
            startTime = LocalDateTime.now(),
            actions = mutableListOf()
        )
        Log.i(TAG, "New analytics session started: ${currentSession?.id}")
    }

    private fun loadStoredAnalytics() {
        sessionScope.launch {
            try {
                val storedErrorCounts = preferences.getString("error_counts", null)
                if (storedErrorCounts != null) {
                    val json = JSONObject(storedErrorCounts)
                    json.keys().forEach { key ->
                        errorCounts[key] = AtomicInteger(json.getInt(key))
                    }
                }

                val storedUserActions = preferences.getString("user_actions", null)
                if (storedUserActions != null) {
                    val json = JSONObject(storedUserActions)
                    json.keys().forEach { key ->
                        userActionCounts[key] = AtomicInteger(json.getInt(key))
                    }
                }

                updateErrorStatistics()
                Log.i(TAG, "Stored analytics loaded")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load stored analytics", e)
            }
        }
    }

    private fun updateErrorStatistics() {
        val stats = ErrorStatistics(
            totalErrors = errorCounts.values.sumOf { it.get() },
            uniqueErrorTypes = errorCounts.size,
            averageErrorsPerSession = calculateAverageErrorsPerSession(),
            mostCommonError = errorCounts.maxByOrNull { it.value.get() }?.key,
            lastErrorTime = _recentErrors.value.firstOrNull()?.timestamp
        )
        _errorStats.value = stats
    }

    private fun calculateAverageErrorsPerSession(): Double {
        val sessionCount = errorSessions.values.map { it.map { session -> session.sessionId }.distinct() }.flatten().distinct().size
        val totalErrors = errorCounts.values.sumOf { it.get() }
        return if (sessionCount > 0) totalErrors.toDouble() / sessionCount else 0.0
    }

    private fun calculateErrorTrends(): List<ErrorTrend> {
        // Calculate error trends over time
        // This is a simplified implementation
        return errorCounts.entries.map { (errorType, count) ->
            ErrorTrend(
                errorType = errorType,
                trend = TrendDirection.STABLE, // Simplified
                changePercentage = 0.0 // Simplified
            )
        }
    }

    private fun identifyCriticalPatterns(): List<CriticalPattern> {
        val patterns = mutableListOf<CriticalPattern>()

        // High frequency errors
        errorCounts.entries.forEach { (errorType, count) ->
            if (count.get() > 10) {
                patterns.add(
                    CriticalPattern(
                        type = "HIGH_FREQUENCY",
                        description = "Error $errorType occurs frequently (${count.get()} times)",
                        severity = PatternSeverity.HIGH,
                        recommendation = "Investigate root cause of $errorType"
                    )
                )
            }
        }

        // Unresolved errors
        val unresolvedCount = errorSessions.values.flatten().count { !it.resolved }
        if (unresolvedCount > 5) {
            patterns.add(
                CriticalPattern(
                    type = "UNRESOLVED_ERRORS",
                    description = "$unresolvedCount errors remain unresolved",
                    severity = PatternSeverity.MEDIUM,
                    recommendation = "Review and resolve outstanding errors"
                )
            )
        }

        return patterns
    }

    private fun persistErrorEvent(errorEvent: ErrorEvent) {
        try {
            val logFile = getLogFile("errors")
            val json = errorEventToJson(errorEvent)
            FileWriter(logFile, true).use { writer ->
                writer.appendLine(json.toString())
            }

            // Update preferences
            val errorCountsJson = JSONObject()
            errorCounts.forEach { (key, count) ->
                errorCountsJson.put(key, count.get())
            }
            preferences.edit().putString("error_counts", errorCountsJson.toString()).apply()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist error event", e)
        }
    }

    private fun persistUserAction(actionEvent: UserActionEvent) {
        try {
            val logFile = getLogFile("actions")
            val json = userActionToJson(actionEvent)
            FileWriter(logFile, true).use { writer ->
                writer.appendLine(json.toString())
            }

            // Update preferences
            val userActionsJson = JSONObject()
            userActionCounts.forEach { (key, count) ->
                userActionsJson.put(key, count.get())
            }
            preferences.edit().putString("user_actions", userActionsJson.toString()).apply()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist user action", e)
        }
    }

    private fun getLogFile(type: String): File {
        val logsDir = File(context.filesDir, "analytics_logs")
        if (!logsDir.exists()) {
            logsDir.mkdirs()
        }

        val today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        return File(logsDir, "${type}_$today.log")
    }

    private fun clearLogFiles() {
        try {
            val logsDir = File(context.filesDir, "analytics_logs")
            if (logsDir.exists()) {
                logsDir.listFiles()?.forEach { it.delete() }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear log files", e)
        }
    }

    private fun sendToAnalyticsService(errorEvent: ErrorEvent) {
        // Implementation for sending to external analytics service
        // This could be Firebase Analytics, Crashlytics, Bugsnag, etc.
        sessionScope.launch {
            try {
                // Example: Firebase Analytics
                // val bundle = Bundle().apply {
                //     putString("error_type", errorEvent.error::class.simpleName)
                //     putString("error_context", errorEvent.context)
                //     putString("session_id", errorEvent.sessionId)
                // }
                // firebaseAnalytics.logEvent("app_error", bundle)

                Log.d(TAG, "Error sent to analytics service: ${errorEvent.error::class.simpleName}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send error to analytics service", e)
            }
        }
    }

    private fun isAnalyticsEnabled(): Boolean {
        return preferences.getBoolean("analytics_enabled", true)
    }

    private fun generateSessionId(): String {
        return "session_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }

    private fun exportToJson(): String {
        val json = JSONObject()

        // Error counts
        val errorCountsJson = JSONObject()
        errorCounts.forEach { (key, count) ->
            errorCountsJson.put(key, count.get())
        }
        json.put("error_counts", errorCountsJson)

        // User actions
        val userActionsJson = JSONObject()
        userActionCounts.forEach { (key, count) ->
            userActionsJson.put(key, count.get())
        }
        json.put("user_actions", userActionsJson)

        // Statistics
        json.put("statistics", errorStatsToJson(_errorStats.value))

        return json.toString(2)
    }

    private fun exportToCsv(): String {
        val csv = StringBuilder()
        csv.appendLine("Type,Error/Action,Count,Percentage")

        val totalErrors = errorCounts.values.sumOf { it.get() }
        errorCounts.forEach { (key, count) ->
            val percentage = if (totalErrors > 0) (count.get().toDouble() / totalErrors * 100) else 0.0
            csv.appendLine("Error,$key,${count.get()},${String.format("%.2f", percentage)}%")
        }

        val totalActions = userActionCounts.values.sumOf { it.get() }
        userActionCounts.forEach { (key, count) ->
            val percentage = if (totalActions > 0) (count.get().toDouble() / totalActions * 100) else 0.0
            csv.appendLine("Action,$key,${count.get()},${String.format("%.2f", percentage)}%")
        }

        return csv.toString()
    }

    private fun errorEventToJson(errorEvent: ErrorEvent): JSONObject {
        return JSONObject().apply {
            put("error_type", errorEvent.error::class.simpleName)
            put("error_message", errorEvent.error.message)
            put("user_message", errorEvent.error.userMessage)
            put("context", errorEvent.context)
            put("timestamp", errorEvent.timestamp.toString())
            put("session_id", errorEvent.sessionId)
            put("additional_data", JSONObject(errorEvent.additionalData))
            errorEvent.stackTrace?.let { put("stack_trace", it) }
        }
    }

    private fun userActionToJson(actionEvent: UserActionEvent): JSONObject {
        return JSONObject().apply {
            put("action", actionEvent.action)
            put("screen", actionEvent.screen)
            put("success", actionEvent.success)
            put("timestamp", actionEvent.timestamp.toString())
            put("session_id", actionEvent.sessionId)
            actionEvent.duration?.let { put("duration", it) }
            put("additional_data", JSONObject(actionEvent.additionalData))
        }
    }

    private fun errorStatsToJson(stats: ErrorStatistics): JSONObject {
        return JSONObject().apply {
            put("total_errors", stats.totalErrors)
            put("unique_error_types", stats.uniqueErrorTypes)
            put("average_errors_per_session", stats.averageErrorsPerSession)
            put("most_common_error", stats.mostCommonError)
            put("last_error_time", stats.lastErrorTime?.toString())
        }
    }

    fun dispose() {
        sessionScope.cancel()
    }
}

// Data classes for analytics
data class ErrorEvent(
    val error: AppError,
    val context: String,
    val additionalData: Map<String, Any>,
    val stackTrace: String?,
    val timestamp: LocalDateTime,
    val sessionId: String
)

data class UserActionEvent(
    val action: String,
    val screen: String,
    val success: Boolean,
    val duration: Long?,
    val additionalData: Map<String, Any>,
    val timestamp: LocalDateTime,
    val sessionId: String
)

data class ErrorSession(
    val timestamp: LocalDateTime,
    val sessionId: String,
    var resolved: Boolean,
    var resolvedAt: LocalDateTime? = null
)

data class AnalyticsSession(
    val id: String,
    val startTime: LocalDateTime,
    val actions: MutableList<UserActionEvent>
)

data class ErrorStatistics(
    val totalErrors: Int = 0,
    val uniqueErrorTypes: Int = 0,
    val averageErrorsPerSession: Double = 0.0,
    val mostCommonError: String? = null,
    val lastErrorTime: LocalDateTime? = null
)

data class ErrorFrequency(
    val errorType: String,
    val count: Int
)

data class ErrorTrend(
    val errorType: String,
    val trend: TrendDirection,
    val changePercentage: Double
)

data class CriticalPattern(
    val type: String,
    val description: String,
    val severity: PatternSeverity,
    val recommendation: String
)

data class ErrorInsights(
    val totalErrors: Int,
    val uniqueErrors: Int,
    val unresolvedErrors: Int,
    val topErrors: List<ErrorFrequency>,
    val errorTrends: List<ErrorTrend>,
    val criticalPatterns: List<CriticalPattern>,
    val sessionInfo: SessionInfo?
)

data class SessionInfo(
    val sessionId: String,
    val startTime: LocalDateTime,
    val actionCount: Int
)

enum class TrendDirection {
    INCREASING, DECREASING, STABLE
}

enum class PatternSeverity {
    LOW, MEDIUM, HIGH, CRITICAL
}