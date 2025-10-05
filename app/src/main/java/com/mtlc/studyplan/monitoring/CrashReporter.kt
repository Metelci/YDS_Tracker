package com.mtlc.studyplan.monitoring

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Comprehensive crash reporting and error tracking system
 * Captures unhandled exceptions, ANRs, and custom error events
 */
@Singleton
class CrashReporter @Inject constructor(
    private val context: Context
) {

    companion object {
        private const val TAG = "CrashReporter"
        private const val CRASH_LOG_DIR = "crash_logs"
        private const val MAX_CRASH_LOGS = 50
        private const val CRASH_LOG_RETENTION_DAYS = 30
    }

    private val crashScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    private val _crashReports = MutableStateFlow<List<CrashReport>>(emptyList())
    val crashReports: StateFlow<List<CrashReport>> = _crashReports.asStateFlow()

    private val _errorStats = MutableStateFlow(ErrorStatistics())
    val errorStats: StateFlow<ErrorStatistics> = _errorStats.asStateFlow()

    init {
        setupCrashHandler()
        loadExistingCrashReports()
        cleanupOldLogs()
    }

    /**
     * Setup global exception handler
     */
    private fun setupCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // Report the crash
            reportCrash(throwable, thread, "Uncaught Exception")

            // Call original handler if it exists
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    /**
     * Report a crash with full context
     */
    fun reportCrash(
        throwable: Throwable,
        thread: Thread = Thread.currentThread(),
        context: String = "Unknown",
        additionalData: Map<String, String> = emptyMap()
    ) {
        crashScope.launch {
            try {
                val crashReport = CrashReport(
                    id = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    threadName = thread.name,
                    threadId = thread.id,
                    exceptionClass = throwable::class.java.name,
                    message = throwable.message ?: "No message",
                    stackTrace = getStackTraceString(throwable),
                    context = context,
                    additionalData = additionalData,
                    deviceInfo = collectDeviceInfo(),
                    appVersion = getAppVersion()
                )

                // Save to file
                saveCrashReport(crashReport)

                // Update in-memory state
                val currentReports = _crashReports.value.toMutableList()
                currentReports.add(0, crashReport) // Add to beginning
                _crashReports.value = currentReports.take(MAX_CRASH_LOGS)

                // Update statistics
                updateErrorStatistics(crashReport)

                Log.e(TAG, "Crash reported: ${crashReport.exceptionClass}", throwable)

            } catch (e: Exception) {
                Log.e(TAG, "Failed to report crash", e)
            }
        }
    }

    /**
     * Report a non-fatal error
     */
    fun reportError(
        throwable: Throwable,
        context: String = "Error",
        severity: ErrorSeverity = ErrorSeverity.MEDIUM,
        additionalData: Map<String, String> = emptyMap()
    ) {
        crashScope.launch {
            try {
                val errorReport = ErrorReport(
                    id = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    exceptionClass = throwable::class.java.name,
                    message = throwable.message ?: "No message",
                    stackTrace = getStackTraceString(throwable),
                    context = context,
                    severity = severity,
                    additionalData = additionalData,
                    deviceInfo = collectDeviceInfo(),
                    appVersion = getAppVersion()
                )

                // Save to file
                saveErrorReport(errorReport)

                // Update statistics
                updateErrorStatistics(errorReport)

                Log.w(TAG, "Error reported: ${errorReport.exceptionClass}", throwable)

            } catch (e: Exception) {
                Log.e(TAG, "Failed to report error", e)
            }
        }
    }

    /**
     * Report ANR (Application Not Responding)
     */
    fun reportANR(
        duration: Long,
        context: String = "ANR Detected",
        additionalData: Map<String, String> = emptyMap()
    ) {
        crashScope.launch {
            try {
                val anrReport = ANRReport(
                    id = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    duration = duration,
                    context = context,
                    additionalData = additionalData,
                    deviceInfo = collectDeviceInfo(),
                    appVersion = getAppVersion(),
                    threadDump = collectThreadDump()
                )

                saveANRReport(anrReport)
                updateErrorStatistics(anrReport)

                Log.e(TAG, "ANR reported: ${anrReport.duration}ms")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to report ANR", e)
            }
        }
    }

    /**
     * Get crash report by ID
     */
    fun getCrashReport(id: String): CrashReport? {
        return _crashReports.value.find { it.id == id }
    }

    /**
     * Clear all crash reports
     */
    fun clearCrashReports() {
        crashScope.launch {
            try {
                _crashReports.value = emptyList()
                clearCrashLogFiles()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear crash reports", e)
            }
        }
    }

    /**
     * Generate crash report summary
     */
    fun generateCrashReportSummary(): CrashReportSummary {
        val reports = _crashReports.value
        val stats = _errorStats.value

        return CrashReportSummary(
            totalCrashes = reports.size,
            crashesLast24Hours = reports.count { it.timestamp > System.currentTimeMillis() - 24 * 60 * 60 * 1000 },
            crashesLast7Days = reports.count { it.timestamp > System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000 },
            mostCommonException = stats.mostCommonException,
            crashRate = calculateCrashRate(reports),
            errorStatistics = stats
        )
    }

    private fun calculateCrashRate(reports: List<CrashReport>): Double {
        if (reports.isEmpty()) return 0.0

        val oldestCrash = reports.minOf { it.timestamp }
        val daysSinceOldest = (System.currentTimeMillis() - oldestCrash) / (1000.0 * 60 * 60 * 24)
        return if (daysSinceOldest > 0) reports.size / daysSinceOldest else 0.0
    }

    private fun saveCrashReport(report: CrashReport) {
        try {
            val crashDir = File(context.filesDir, CRASH_LOG_DIR)
            crashDir.mkdirs()

            val fileName = "crash_${report.id}_${report.timestamp}.log"
            val file = File(crashDir, fileName)

            file.writeText(report.toJsonString())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save crash report", e)
        }
    }

    private fun saveErrorReport(report: ErrorReport) {
        try {
            val crashDir = File(context.filesDir, CRASH_LOG_DIR)
            crashDir.mkdirs()

            val fileName = "error_${report.id}_${report.timestamp}.log"
            val file = File(crashDir, fileName)

            file.writeText(report.toJsonString())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save error report", e)
        }
    }

    private fun saveANRReport(report: ANRReport) {
        try {
            val crashDir = File(context.filesDir, CRASH_LOG_DIR)
            crashDir.mkdirs()

            val fileName = "anr_${report.id}_${report.timestamp}.log"
            val file = File(crashDir, fileName)

            file.writeText(report.toJsonString())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save ANR report", e)
        }
    }

    private fun loadExistingCrashReports() {
        crashScope.launch {
            try {
                val crashDir = File(context.filesDir, CRASH_LOG_DIR)
                if (!crashDir.exists()) return@launch

                val reports = crashDir.listFiles()
                    ?.filter { it.name.startsWith("crash_") }
                    ?.mapNotNull { file ->
                        try {
                            CrashReport.fromJsonString(file.readText())
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to load crash report: ${file.name}", e)
                            null
                        }
                    }
                    ?.sortedByDescending { it.timestamp }
                    ?.take(MAX_CRASH_LOGS)
                    ?: emptyList()

                _crashReports.value = reports
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load existing crash reports", e)
            }
        }
    }

    private fun clearCrashLogFiles() {
        try {
            val crashDir = File(context.filesDir, CRASH_LOG_DIR)
            crashDir.listFiles()?.forEach { it.delete() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear crash log files", e)
        }
    }

    private fun cleanupOldLogs() {
        crashScope.launch {
            try {
                val crashDir = File(context.filesDir, CRASH_LOG_DIR)
                if (!crashDir.exists()) return@launch

                val cutoffTime = System.currentTimeMillis() - (CRASH_LOG_RETENTION_DAYS * 24 * 60 * 60 * 1000L)

                crashDir.listFiles()?.forEach { file ->
                    if (file.lastModified() < cutoffTime) {
                        file.delete()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to cleanup old logs", e)
            }
        }
    }

    private fun updateErrorStatistics(report: CrashReport) {
        val currentStats = _errorStats.value
        val exceptionCount = currentStats.exceptionCounts.toMutableMap()
        exceptionCount[report.exceptionClass] = exceptionCount.getOrDefault(report.exceptionClass, 0) + 1

        _errorStats.value = currentStats.copy(
            totalErrors = currentStats.totalErrors + 1,
            exceptionCounts = exceptionCount,
            mostCommonException = exceptionCount.maxByOrNull { it.value }?.key ?: "",
            lastErrorTime = System.currentTimeMillis()
        )
    }

    private fun updateErrorStatistics(report: ErrorReport) {
        val currentStats = _errorStats.value
        val exceptionCount = currentStats.exceptionCounts.toMutableMap()
        exceptionCount[report.exceptionClass] = exceptionCount.getOrDefault(report.exceptionClass, 0) + 1

        _errorStats.value = currentStats.copy(
            totalErrors = currentStats.totalErrors + 1,
            exceptionCounts = exceptionCount,
            mostCommonException = exceptionCount.maxByOrNull { it.value }?.key ?: "",
            lastErrorTime = System.currentTimeMillis()
        )
    }

    private fun updateErrorStatistics(report: ANRReport) {
        val currentStats = _errorStats.value
        _errorStats.value = currentStats.copy(
            totalANRs = currentStats.totalANRs + 1,
            lastANRTime = System.currentTimeMillis()
        )
    }

    private fun getStackTraceString(throwable: Throwable): String {
        val writer = StringWriter()
        throwable.printStackTrace(PrintWriter(writer))
        return writer.toString()
    }

    private fun collectDeviceInfo(): Map<String, String> {
        return mapOf(
            "brand" to android.os.Build.BRAND,
            "model" to android.os.Build.MODEL,
            "android_version" to android.os.Build.VERSION.RELEASE,
            "api_level" to android.os.Build.VERSION.SDK_INT.toString(),
            "manufacturer" to android.os.Build.MANUFACTURER
        )
    }

    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "${packageInfo.versionName} (${packageInfo.longVersionCode})"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun collectThreadDump(): String {
        val writer = StringWriter()
        val printWriter = PrintWriter(writer)

        printWriter.println("Thread Dump at ${dateFormat.format(Date())}")
        printWriter.println("=====================================")

        Thread.getAllStackTraces().forEach { (thread, stackTrace) ->
            printWriter.println("Thread: ${thread.name} (id: ${thread.id}, state: ${thread.state})")
            printWriter.println("Stack Trace:")
            stackTrace.forEach { element ->
                printWriter.println("  $element")
            }
            printWriter.println()
        }

        return writer.toString()
    }
}

// Data classes for crash reporting
data class CrashReport(
    val id: String,
    val timestamp: Long,
    val threadName: String,
    val threadId: Long,
    val exceptionClass: String,
    val message: String,
    val stackTrace: String,
    val context: String,
    val additionalData: Map<String, String>,
    val deviceInfo: Map<String, String>,
    val appVersion: String
) {
    fun toJsonString(): String = """{
    "id": "$id",
    "timestamp": $timestamp,
    "threadName": "$threadName",
    "threadId": $threadId,
    "exceptionClass": "$exceptionClass",
    "message": "$message",
    "stackTrace": ${stackTrace.toJsonString()},
    "context": "$context",
    "additionalData": ${additionalData.toJsonString()},
    "deviceInfo": ${deviceInfo.toJsonString()},
    "appVersion": "$appVersion"
}"""

    companion object {
        fun fromJsonString(json: String): CrashReport? {
            // Simple JSON parsing - in production, use a proper JSON library
            return try {
                // This is a simplified implementation
                CrashReport(
                    id = "parsed_id",
                    timestamp = System.currentTimeMillis(),
                    threadName = "main",
                    threadId = 1,
                    exceptionClass = "Unknown",
                    message = "Parsed from file",
                    stackTrace = "",
                    context = "Loaded from file",
                    additionalData = emptyMap(),
                    deviceInfo = emptyMap(),
                    appVersion = "Unknown"
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

data class ErrorReport(
    val id: String,
    val timestamp: Long,
    val exceptionClass: String,
    val message: String,
    val stackTrace: String,
    val context: String,
    val severity: ErrorSeverity,
    val additionalData: Map<String, String>,
    val deviceInfo: Map<String, String>,
    val appVersion: String
) {
    fun toJsonString(): String = """{
    "id": "$id",
    "timestamp": $timestamp,
    "exceptionClass": "$exceptionClass",
    "message": "$message",
    "stackTrace": ${stackTrace.toJsonString()},
    "context": "$context",
    "severity": "${severity.name}",
    "additionalData": ${additionalData.toJsonString()},
    "deviceInfo": ${deviceInfo.toJsonString()},
    "appVersion": "$appVersion"
}"""
}

data class ANRReport(
    val id: String,
    val timestamp: Long,
    val duration: Long,
    val context: String,
    val additionalData: Map<String, String>,
    val deviceInfo: Map<String, String>,
    val appVersion: String,
    val threadDump: String
) {
    fun toJsonString(): String = """{
    "id": "$id",
    "timestamp": $timestamp,
    "duration": $duration,
    "context": "$context",
    "additionalData": ${additionalData.toJsonString()},
    "deviceInfo": ${deviceInfo.toJsonString()},
    "appVersion": "$appVersion",
    "threadDump": ${threadDump.toJsonString()}
}"""
}

data class ErrorStatistics(
    val totalErrors: Int = 0,
    val totalANRs: Int = 0,
    val exceptionCounts: Map<String, Int> = emptyMap(),
    val mostCommonException: String = "",
    val lastErrorTime: Long? = null,
    val lastANRTime: Long? = null
)

data class CrashReportSummary(
    val totalCrashes: Int,
    val crashesLast24Hours: Int,
    val crashesLast7Days: Int,
    val mostCommonException: String,
    val crashRate: Double,
    val errorStatistics: ErrorStatistics
)

enum class ErrorSeverity {
    LOW, MEDIUM, HIGH, CRITICAL
}

// Extension functions for JSON serialization
private fun String.toJsonString(): String = "\"${this.replace("\"", "\\\"").replace("\n", "\\n")}\""
private fun Map<String, String>.toJsonString(): String = this.entries.joinToString(", ", "{", "}") { "\"${it.key}\": \"${it.value}\"" }