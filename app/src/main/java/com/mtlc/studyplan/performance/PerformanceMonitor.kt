package com.mtlc.studyplan.performance

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PerformanceMonitor @Inject constructor() {

    private val _performanceMetrics = MutableStateFlow(PerformanceMetrics())
    val performanceMetrics: StateFlow<PerformanceMetrics> = _performanceMetrics.asStateFlow()

    private val frameMetrics = mutableListOf<Long>()
    private val memoryMetrics = mutableListOf<Long>()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    init {
        startFrameTracking()
        trackMemoryUsage()
    }

    fun startFrameTracking() {
        scope.launch {
            while (true) {
                val frameStart = System.nanoTime()
                delay(16) // Target 60fps
                val frameTime = System.nanoTime() - frameStart

                synchronized(frameMetrics) {
                    frameMetrics.add(frameTime)

                    // Keep only last 100 frames
                    if (frameMetrics.size > 100) {
                        frameMetrics.removeAt(0)
                    }
                }

                updateMetrics()
            }
        }
    }

    fun trackMemoryUsage() {
        scope.launch {
            while (true) {
                val memoryInfo = Runtime.getRuntime().let { runtime ->
                    runtime.totalMemory() - runtime.freeMemory()
                }

                synchronized(memoryMetrics) {
                    memoryMetrics.add(memoryInfo)

                    // Keep only last 60 measurements (1 minute at 1-second intervals)
                    if (memoryMetrics.size > 60) {
                        memoryMetrics.removeAt(0)
                    }
                }

                updateMetrics()
                delay(1000) // Check every second
            }
        }
    }

    private fun updateMetrics() {
        val avgFrameTime = synchronized(frameMetrics) {
            if (frameMetrics.isNotEmpty()) frameMetrics.average() else 0.0
        }

        val avgMemoryUsage = synchronized(memoryMetrics) {
            if (memoryMetrics.isNotEmpty()) memoryMetrics.average() else 0.0
        }

        val fps = if (avgFrameTime > 0) 1_000_000_000.0 / avgFrameTime else 0.0

        _performanceMetrics.value = PerformanceMetrics(
            averageFrameTime = avgFrameTime,
            currentFps = fps,
            memoryUsageMB = avgMemoryUsage / (1024 * 1024),
            isPerformanceGood = fps >= 55 && avgMemoryUsage < 200 * 1024 * 1024 // 200MB threshold
        )
    }

    fun logPerformanceIssue(context: String, issue: String) {
        Log.w("Performance", "Performance issue in $context: $issue")

        // Record performance issue
        val currentMetrics = _performanceMetrics.value
        val issueMetrics = currentMetrics.copy(
            performanceIssues = currentMetrics.performanceIssues + PerformanceIssue(
                context = context,
                issue = issue,
                timestamp = System.currentTimeMillis(),
                fps = currentMetrics.currentFps,
                memoryUsage = currentMetrics.memoryUsageMB
            )
        )
        _performanceMetrics.value = issueMetrics
    }

    fun measureOperation(operationName: String, operation: () -> Unit): Long {
        val startTime = System.nanoTime()
        try {
            operation()
        } finally {
            val endTime = System.nanoTime()
            val duration = endTime - startTime

            // Log slow operations (> 16ms for 60fps)
            if (duration > 16_000_000) {
                logPerformanceIssue(operationName, "Slow operation: ${duration / 1_000_000}ms")
            }

            return duration
        }
    }

    fun measureSuspendOperation(operationName: String, operation: suspend () -> Unit): suspend () -> Unit = {
        val startTime = System.nanoTime()
        try {
            operation()
        } finally {
            val endTime = System.nanoTime()
            val duration = endTime - startTime

            // Log slow operations
            if (duration > 100_000_000) { // 100ms for suspend operations
                logPerformanceIssue(operationName, "Slow suspend operation: ${duration / 1_000_000}ms")
            }
        }
    }

    fun recordCacheHit() {
        val currentMetrics = _performanceMetrics.value
        _performanceMetrics.value = currentMetrics.copy(
            cacheHits = currentMetrics.cacheHits + 1
        )
    }

    fun recordCacheMiss() {
        val currentMetrics = _performanceMetrics.value
        _performanceMetrics.value = currentMetrics.copy(
            cacheMisses = currentMetrics.cacheMisses + 1
        )
    }

    fun getPerformanceReport(): String {
        val metrics = _performanceMetrics.value
        val cacheHitRate = if (metrics.cacheHits + metrics.cacheMisses > 0) {
            metrics.cacheHits.toDouble() / (metrics.cacheHits + metrics.cacheMisses)
        } else {
            0.0
        }

        return buildString {
            appendLine("=== Performance Report ===")
            appendLine("Average FPS: %.1f".format(metrics.currentFps))
            appendLine("Frame Time: %.2fms".format(metrics.averageFrameTime / 1_000_000))
            appendLine("Memory Usage: %.1fMB".format(metrics.memoryUsageMB))
            appendLine("Cache Hit Rate: %.1f%%".format(cacheHitRate * 100))
            appendLine("Performance Good: ${metrics.isPerformanceGood}")
            appendLine("Recent Issues: ${metrics.performanceIssues.size}")
            appendLine("========================")

            if (metrics.performanceIssues.isNotEmpty()) {
                appendLine("\nRecent Performance Issues:")
                metrics.performanceIssues.takeLast(5).forEach { issue ->
                    appendLine("- ${issue.context}: ${issue.issue}")
                }
            }
        }
    }

    fun clearMetrics() {
        synchronized(frameMetrics) {
            frameMetrics.clear()
        }
        synchronized(memoryMetrics) {
            memoryMetrics.clear()
        }
        _performanceMetrics.value = PerformanceMetrics()
    }
}

data class PerformanceMetrics(
    val averageFrameTime: Double = 0.0,
    val currentFps: Double = 60.0,
    val memoryUsageMB: Double = 0.0,
    val isPerformanceGood: Boolean = true,
    val cacheHits: Int = 0,
    val cacheMisses: Int = 0,
    val performanceIssues: List<PerformanceIssue> = emptyList()
)

data class PerformanceIssue(
    val context: String,
    val issue: String,
    val timestamp: Long,
    val fps: Double,
    val memoryUsage: Double
)

// Performance measurement annotations and extensions
inline fun <T> PerformanceMonitor.measured(operationName: String, crossinline block: () -> T): T {
    var result: T? = null
    measureOperation(operationName) {
        result = block()
    }
    return result!!
}

// Performance tracker factory
fun createPerformanceTracker(
    operationName: String,
    performanceMonitor: PerformanceMonitor
): PerformanceTracker {
    return PerformanceTracker(operationName, performanceMonitor)
}

class PerformanceTracker(
    private val operationName: String,
    private val performanceMonitor: PerformanceMonitor
) {
    fun track(operation: () -> Unit) {
        performanceMonitor.measureOperation(operationName, operation)
    }
}