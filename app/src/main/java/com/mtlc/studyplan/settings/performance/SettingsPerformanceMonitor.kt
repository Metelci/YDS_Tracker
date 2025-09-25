package com.mtlc.studyplan.settings.performance

import android.content.Context
import android.os.Debug
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SettingsPerformanceMonitor(
    private val context: Context
) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _performanceState = MutableStateFlow(PerformanceState())
    val performanceState: StateFlow<PerformanceState> = _performanceState.asStateFlow()

    private var isMonitoring = false

    data class PerformanceState(
        val memoryUsage: MemoryUsage = MemoryUsage(),
        val cacheHitRate: Float = 0.0f,
        val frameRate: Float = 60.0f,
        val memoryPressure: MemoryPressure = MemoryPressure.NORMAL,
        val settingsLoadTime: Long = 0L,
        val searchPerformance: SearchPerformance = SearchPerformance()
    )

    data class MemoryUsage(
        val usedMemory: Long = 0L,
        val totalMemory: Long = 0L,
        val availableMemory: Long = 0L,
        val maxMemory: Long = 0L
    )

    data class SearchPerformance(
        val averageSearchTime: Long = 0L,
        val cacheHitRate: Float = 0.0f,
        val indexSize: Long = 0L
    )

    enum class MemoryPressure {
        NORMAL, HIGH, CRITICAL
    }

    fun startMonitoring() {
        if (isMonitoring) return
        isMonitoring = true

        scope.launch {
            while (isActive) {
                updatePerformanceMetrics()
                delay(5000) // Update every 5 seconds
            }
        }
    }

    fun stopMonitoring() {
        isMonitoring = false
    }

    private suspend fun updatePerformanceMetrics() {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory

        val memoryUsage = MemoryUsage(
            usedMemory = usedMemory,
            totalMemory = totalMemory,
            availableMemory = freeMemory,
            maxMemory = maxMemory
        )

        val memoryPressure = when {
            usedMemory > maxMemory * 0.9 -> MemoryPressure.CRITICAL
            usedMemory > maxMemory * 0.7 -> MemoryPressure.HIGH
            else -> MemoryPressure.NORMAL
        }

        _performanceState.value = _performanceState.value.copy(
            memoryUsage = memoryUsage,
            memoryPressure = memoryPressure
        )
    }

    fun recordSettingsLoadTime(loadTime: Long) {
        _performanceState.value = _performanceState.value.copy(
            settingsLoadTime = loadTime
        )
    }

    fun recordSearchPerformance(searchTime: Long, cacheHit: Boolean) {
        val currentSearchPerf = _performanceState.value.searchPerformance

        // Simple running average calculation
        val newAverageTime = if (currentSearchPerf.averageSearchTime == 0L) {
            searchTime
        } else {
            (currentSearchPerf.averageSearchTime + searchTime) / 2
        }

        // Simple cache hit rate calculation (would be more sophisticated in production)
        val newCacheHitRate = if (cacheHit) {
            (currentSearchPerf.cacheHitRate + 1.0f) / 2.0f
        } else {
            currentSearchPerf.cacheHitRate / 2.0f
        }

        _performanceState.value = _performanceState.value.copy(
            searchPerformance = currentSearchPerf.copy(
                averageSearchTime = newAverageTime,
                cacheHitRate = newCacheHitRate.coerceIn(0.0f, 1.0f)
            )
        )
    }

    fun getMemoryInfo(): MemoryUsage {
        return _performanceState.value.memoryUsage
    }

    fun isMemoryPressureHigh(): Boolean {
        return _performanceState.value.memoryPressure != MemoryPressure.NORMAL
    }

    fun shouldReduceAnimations(): Boolean {
        return _performanceState.value.memoryPressure == MemoryPressure.CRITICAL
    }

    fun getRecommendations(): List<PerformanceRecommendation> {
        val recommendations = mutableListOf<PerformanceRecommendation>()
        val state = _performanceState.value

        when (state.memoryPressure) {
            MemoryPressure.HIGH -> {
                recommendations.add(
                    PerformanceRecommendation(
                        type = RecommendationType.MEMORY,
                        title = "High Memory Usage",
                        description = "Consider closing other apps to improve performance",
                        priority = Priority.MEDIUM
                    )
                )
            }
            MemoryPressure.CRITICAL -> {
                recommendations.add(
                    PerformanceRecommendation(
                        type = RecommendationType.MEMORY,
                        title = "Critical Memory Usage",
                        description = "Animations have been reduced to improve performance",
                        priority = Priority.HIGH
                    )
                )
            }
            MemoryPressure.NORMAL -> {
                // No memory recommendations
            }
        }

        if (state.searchPerformance.averageSearchTime > 1000) {
            recommendations.add(
                PerformanceRecommendation(
                    type = RecommendationType.SEARCH,
                    title = "Slow Search Performance",
                    description = "Search results may take longer than expected",
                    priority = Priority.LOW
                )
            )
        }

        return recommendations
    }

    data class PerformanceRecommendation(
        val type: RecommendationType,
        val title: String,
        val description: String,
        val priority: Priority
    )

    enum class RecommendationType {
        MEMORY, SEARCH, ANIMATION, GENERAL
    }

    enum class Priority {
        LOW, MEDIUM, HIGH
    }
}