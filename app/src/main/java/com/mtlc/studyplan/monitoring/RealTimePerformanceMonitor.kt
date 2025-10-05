package com.mtlc.studyplan.monitoring

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.mtlc.studyplan.performance.PerformanceMonitor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real-time performance monitoring system
 * Provides live metrics, alerts, and performance insights
 */
@Singleton
class RealTimePerformanceMonitor @Inject constructor(
    private val context: Context,
    private val performanceMonitor: PerformanceMonitor
) : DefaultLifecycleObserver {

    companion object {
        private const val TAG = "RealTimePerfMonitor"
        private const val MONITORING_INTERVAL_MS = 1000L // 1 second
        private const val ALERT_CHECK_INTERVAL_MS = 5000L // 5 seconds
        private const val PERFORMANCE_HISTORY_SIZE = 60 // 1 minute of data
        private const val MEMORY_WARNING_THRESHOLD_MB = 150
        private const val FPS_WARNING_THRESHOLD = 50.0
        private const val HIGH_CPU_THRESHOLD = 80.0
    }

    private val monitoringScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val mainHandler = Handler(Looper.getMainLooper())

    // Performance metrics state
    private val _performanceMetrics = MutableStateFlow(RealTimeMetrics())
    val performanceMetrics: StateFlow<RealTimeMetrics> = _performanceMetrics.asStateFlow()

    // Performance alerts
    private val _performanceAlerts = MutableStateFlow<List<PerformanceAlert>>(emptyList())
    val performanceAlerts: StateFlow<List<PerformanceAlert>> = _performanceAlerts.asStateFlow()

    // Performance history for trending
    private val performanceHistory = mutableListOf<RealTimeMetrics>()
    private val alertsHistory = mutableListOf<PerformanceAlert>()

    // Monitoring control
    private val isMonitoringActive = AtomicBoolean(false)
    private val lastAlertCheck = AtomicLong(0)

    // Activity Manager for system metrics
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    init {
        startMonitoring()
    }

    /**
     * Start real-time performance monitoring
     */
    fun startMonitoring() {
        if (isMonitoringActive.getAndSet(true)) return

        monitoringScope.launch {
            while (isActive && isMonitoringActive.get()) {
                try {
                    collectPerformanceMetrics()
                    delay(MONITORING_INTERVAL_MS)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in performance monitoring", e)
                    delay(MONITORING_INTERVAL_MS)
                }
            }
        }

        monitoringScope.launch {
            while (isActive && isMonitoringActive.get()) {
                try {
                    checkPerformanceAlerts()
                    delay(ALERT_CHECK_INTERVAL_MS)
                } catch (e: Exception) {
                    Log.e(TAG, "Error checking performance alerts", e)
                    delay(ALERT_CHECK_INTERVAL_MS)
                }
            }
        }

        Log.i(TAG, "Real-time performance monitoring started")
    }

    /**
     * Stop real-time performance monitoring
     */
    fun stopMonitoring() {
        isMonitoringActive.set(false)
        Log.i(TAG, "Real-time performance monitoring stopped")
    }

    /**
     * Collect current performance metrics
     */
    private suspend fun collectPerformanceMetrics() = withContext(Dispatchers.Default) {
        val currentTime = System.currentTimeMillis()

        // Get memory information
        val memoryInfo = Runtime.getRuntime().let { runtime ->
            val totalMemory = runtime.totalMemory()
            val freeMemory = runtime.freeMemory()
            val usedMemory = totalMemory - freeMemory
            val memoryUsagePercent = (usedMemory.toDouble() / totalMemory) * 100

            MemoryMetrics(
                usedMemoryMB = usedMemory / (1024 * 1024),
                totalMemoryMB = totalMemory / (1024 * 1024),
                freeMemoryMB = freeMemory / (1024 * 1024),
                memoryUsagePercent = memoryUsagePercent
            )
        }

        // Get CPU information (simplified)
        val cpuUsage = getCpuUsage()

        // Get frame rate from PerformanceMonitor
        val fps = performanceMonitor.performanceMetrics.value.currentFps

        // Get cache statistics
        val cacheHits = performanceMonitor.performanceMetrics.value.cacheHits
        val cacheMisses = performanceMonitor.performanceMetrics.value.cacheMisses
        val cacheHitRate = if (cacheHits + cacheMisses > 0) {
            cacheHits.toDouble() / (cacheHits + cacheMisses)
        } else 0.0

        // Get thread information
        val threadCount = Thread.activeCount()
        val daemonThreadCount = Thread.getAllStackTraces().keys.count { it.isDaemon }

        // Get network status (simplified)
        val networkStatus = getNetworkStatus()

        val metrics = RealTimeMetrics(
            timestamp = currentTime,
            memoryMetrics = memoryInfo,
            cpuUsagePercent = cpuUsage,
            fps = fps,
            cacheHitRate = cacheHitRate,
            threadCount = threadCount,
            daemonThreadCount = daemonThreadCount,
            networkStatus = networkStatus,
            activeAlerts = _performanceAlerts.value.size
        )

        // Update current metrics
        _performanceMetrics.value = metrics

        // Add to history
        synchronized(performanceHistory) {
            performanceHistory.add(metrics)
            if (performanceHistory.size > PERFORMANCE_HISTORY_SIZE) {
                performanceHistory.removeAt(0)
            }
        }
    }

    /**
     * Check for performance alerts and issues
     */
    private suspend fun checkPerformanceAlerts() = withContext(Dispatchers.Default) {
        val currentMetrics = _performanceMetrics.value
        val currentTime = System.currentTimeMillis()

        // Skip if we checked recently
        if (currentTime - lastAlertCheck.get() < ALERT_CHECK_INTERVAL_MS) return@withContext
        lastAlertCheck.set(currentTime)

        val newAlerts = mutableListOf<PerformanceAlert>()

        // Memory usage alert
        if (currentMetrics.memoryMetrics.usedMemoryMB > MEMORY_WARNING_THRESHOLD_MB) {
            newAlerts.add(PerformanceAlert(
                id = "memory_high_${currentTime}",
                type = AlertType.MEMORY,
                severity = AlertSeverity.HIGH,
                title = "High Memory Usage",
                message = "Memory usage is ${currentMetrics.memoryMetrics.usedMemoryMB}MB, which exceeds the warning threshold",
                timestamp = currentTime,
                metrics = mapOf(
                    "usedMemoryMB" to currentMetrics.memoryMetrics.usedMemoryMB.toString(),
                    "threshold" to MEMORY_WARNING_THRESHOLD_MB.toString()
                )
            ))
        }

        // Low FPS alert
        if (currentMetrics.fps > 0 && currentMetrics.fps < FPS_WARNING_THRESHOLD) {
            newAlerts.add(PerformanceAlert(
                id = "fps_low_${currentTime}",
                type = AlertType.PERFORMANCE,
                severity = AlertSeverity.MEDIUM,
                title = "Low Frame Rate",
                message = "Frame rate dropped to ${String.format(java.util.Locale.ENGLISH, "%.1f", currentMetrics.fps)} FPS",
                timestamp = currentTime,
                metrics = mapOf(
                    "fps" to currentMetrics.fps.toString(),
                    "threshold" to FPS_WARNING_THRESHOLD.toString()
                )
            ))
        }

        // High CPU usage alert
        if (currentMetrics.cpuUsagePercent > HIGH_CPU_THRESHOLD) {
            newAlerts.add(PerformanceAlert(
                id = "cpu_high_${currentTime}",
                type = AlertType.CPU,
                severity = AlertSeverity.MEDIUM,
                title = "High CPU Usage",
                message = "CPU usage is ${String.format(java.util.Locale.ENGLISH, "%.1f", currentMetrics.cpuUsagePercent)}%",
                timestamp = currentTime,
                metrics = mapOf(
                    "cpuUsage" to currentMetrics.cpuUsagePercent.toString(),
                    "threshold" to HIGH_CPU_THRESHOLD.toString()
                )
            ))
        }

        // Low cache hit rate alert
        if (currentMetrics.cacheHitRate > 0 && currentMetrics.cacheHitRate < 0.7) {
            newAlerts.add(PerformanceAlert(
                id = "cache_low_${currentTime}",
                type = AlertType.CACHE,
                severity = AlertSeverity.LOW,
                title = "Low Cache Hit Rate",
                message = "Cache hit rate is ${String.format(java.util.Locale.ENGLISH, "%.1f", currentMetrics.cacheHitRate * 100)}%",
                timestamp = currentTime,
                metrics = mapOf(
                    "cacheHitRate" to String.format(java.util.Locale.ENGLISH, "%.2f", currentMetrics.cacheHitRate)
                )
            ))
        }

        // High thread count alert
        if (currentMetrics.threadCount > 50) {
            newAlerts.add(PerformanceAlert(
                id = "threads_high_${currentTime}",
                type = AlertType.THREADS,
                severity = AlertSeverity.MEDIUM,
                title = "High Thread Count",
                message = "Application has ${currentMetrics.threadCount} active threads",
                timestamp = currentTime,
                metrics = mapOf(
                    "threadCount" to currentMetrics.threadCount.toString()
                )
            ))
        }

        // Add new alerts to the list
        if (newAlerts.isNotEmpty()) {
            val currentAlerts = _performanceAlerts.value.toMutableList()
            currentAlerts.addAll(newAlerts)
            _performanceAlerts.value = currentAlerts.takeLast(20) // Keep last 20 alerts

            // Add to history
            synchronized(alertsHistory) {
                alertsHistory.addAll(newAlerts)
            }

            // Log alerts
            newAlerts.forEach { alert ->
                Log.w(TAG, "Performance Alert: ${alert.title} - ${alert.message}")
            }
        }
    }

    /**
     * Get performance history for analysis
     */
    fun getPerformanceHistory(minutes: Int = 1): List<RealTimeMetrics> {
        val historySize = (minutes * 60 * 1000 / MONITORING_INTERVAL_MS).toInt()
        return synchronized(performanceHistory) {
            performanceHistory.takeLast(minOf(historySize, performanceHistory.size))
        }
    }

    /**
     * Get performance trends
     */
    fun getPerformanceTrends(): PerformanceTrends {
        val history = getPerformanceHistory(5) // Last 5 minutes
        if (history.size < 2) return PerformanceTrends()

        val first = history.first()
        val last = history.last()

        return PerformanceTrends(
            memoryUsageChange = last.memoryMetrics.usedMemoryMB - first.memoryMetrics.usedMemoryMB,
            fpsChange = last.fps - first.fps,
            cpuUsageChange = last.cpuUsagePercent - first.cpuUsagePercent,
            cacheHitRateChange = last.cacheHitRate - first.cacheHitRate,
            threadCountChange = last.threadCount - first.threadCount,
            averageMemoryUsage = history.map { it.memoryMetrics.usedMemoryMB }.average(),
            averageFps = history.map { it.fps }.average(),
            averageCpuUsage = history.map { it.cpuUsagePercent }.average(),
            averageCacheHitRate = history.map { it.cacheHitRate }.average()
        )
    }

    /**
     * Clear all performance alerts
     */
    fun clearAlerts() {
        _performanceAlerts.value = emptyList()
    }

    /**
     * Get performance report
     */
    fun generatePerformanceReport(): PerformanceReport {
        val currentMetrics = _performanceMetrics.value
        val trends = getPerformanceTrends()
        val history = getPerformanceHistory()

        return PerformanceReport(
            currentMetrics = currentMetrics,
            trends = trends,
            historySize = history.size,
            activeAlerts = _performanceAlerts.value.size,
            alertsHistorySize = alertsHistory.size,
            uptime = System.currentTimeMillis() - (history.firstOrNull()?.timestamp ?: System.currentTimeMillis()),
            recommendations = generateRecommendations(currentMetrics, trends)
        )
    }

    /**
     * Generate performance recommendations based on current metrics and trends
     */
    private fun generateRecommendations(metrics: RealTimeMetrics, trends: PerformanceTrends): List<String> {
        val recommendations = mutableListOf<String>()

        if (metrics.memoryMetrics.usedMemoryMB > MEMORY_WARNING_THRESHOLD_MB) {
            recommendations.add("Consider implementing memory optimizations or clearing caches")
        }

        if (trends.memoryUsageChange > 10) {
            recommendations.add("Memory usage is trending upward - investigate for memory leaks")
        }

        if (metrics.fps < FPS_WARNING_THRESHOLD && metrics.fps > 0) {
            recommendations.add("Frame rate is low - optimize UI rendering and reduce layout complexity")
        }

        if (trends.fpsChange < -5) {
            recommendations.add("Frame rate is declining - check for performance regressions")
        }

        if (metrics.cpuUsagePercent > HIGH_CPU_THRESHOLD) {
            recommendations.add("High CPU usage detected - optimize background operations")
        }

        if (metrics.cacheHitRate < 0.7 && metrics.cacheHitRate > 0) {
            recommendations.add("Low cache hit rate - consider cache size optimization")
        }

        if (metrics.threadCount > 30) {
            recommendations.add("High thread count - review coroutine usage and thread management")
        }

        if (recommendations.isEmpty()) {
            recommendations.add("Performance metrics are within acceptable ranges")
        }

        return recommendations
    }

    /**
     * Get CPU usage (simplified implementation)
     */
    private fun getCpuUsage(): Double {
        return try {
            // This is a simplified CPU usage calculation
            // In a real implementation, you'd use more sophisticated methods
            val processInfo = activityManager.runningAppProcesses?.find {
                it.pid == android.os.Process.myPid()
            }

            // Return a mock value for demonstration
            // Real implementation would require native code or more complex calculations
            (10..30).random().toDouble()
        } catch (e: Exception) {
            0.0
        }
    }

    /**
     * Get network status (simplified)
     */
    private fun getNetworkStatus(): NetworkStatus {
        // Simplified network status check
        return NetworkStatus.CONNECTED // In real implementation, check actual connectivity
    }

    override fun onDestroy(owner: LifecycleOwner) {
        stopMonitoring()
        monitoringScope.cancel()
        super.onDestroy(owner)
    }
}

// Data classes for real-time performance monitoring

data class RealTimeMetrics(
    val timestamp: Long = System.currentTimeMillis(),
    val memoryMetrics: MemoryMetrics = MemoryMetrics(),
    val cpuUsagePercent: Double = 0.0,
    val fps: Double = 60.0,
    val cacheHitRate: Double = 0.0,
    val threadCount: Int = 0,
    val daemonThreadCount: Int = 0,
    val networkStatus: NetworkStatus = NetworkStatus.UNKNOWN,
    val activeAlerts: Int = 0
)

data class MemoryMetrics(
    val usedMemoryMB: Long = 0,
    val totalMemoryMB: Long = 0,
    val freeMemoryMB: Long = 0,
    val memoryUsagePercent: Double = 0.0
)

data class PerformanceAlert(
    val id: String,
    val type: AlertType,
    val severity: AlertSeverity,
    val title: String,
    val message: String,
    val timestamp: Long,
    val metrics: Map<String, String> = emptyMap()
)

data class PerformanceTrends(
    val memoryUsageChange: Long = 0,
    val fpsChange: Double = 0.0,
    val cpuUsageChange: Double = 0.0,
    val cacheHitRateChange: Double = 0.0,
    val threadCountChange: Int = 0,
    val averageMemoryUsage: Double = 0.0,
    val averageFps: Double = 0.0,
    val averageCpuUsage: Double = 0.0,
    val averageCacheHitRate: Double = 0.0
)

data class PerformanceReport(
    val currentMetrics: RealTimeMetrics,
    val trends: PerformanceTrends,
    val historySize: Int,
    val activeAlerts: Int,
    val alertsHistorySize: Int,
    val uptime: Long,
    val recommendations: List<String>
)

enum class AlertType {
    MEMORY, CPU, PERFORMANCE, CACHE, THREADS, NETWORK
}

enum class AlertSeverity {
    LOW, MEDIUM, HIGH, CRITICAL
}

enum class NetworkStatus {
    CONNECTED, DISCONNECTED, UNKNOWN
}