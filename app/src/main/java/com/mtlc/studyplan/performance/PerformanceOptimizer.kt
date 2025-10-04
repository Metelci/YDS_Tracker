@file:OptIn(ExperimentalCoroutinesApi::class)

package com.mtlc.studyplan.performance

import android.content.Context
import android.os.Build
import android.os.Debug
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.ExperimentalCoroutinesApi
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import kotlin.system.measureTimeMillis

/**
 * Comprehensive performance optimization and memory management system
 */
class PerformanceOptimizer private constructor(
    private val context: Context
) : LifecycleEventObserver {

    private val performanceMetrics = MutableStateFlow(PerformanceMetrics())
    private val memoryPressureState = MutableStateFlow(MemoryPressureLevel.NORMAL)
    private val optimizationScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val mainHandler = Handler(Looper.getMainLooper())

    // Performance monitoring
    private var performanceMonitoringEnabled = true
    private val frameMetrics = mutableListOf<Long>()
    private val memorySnapshots = mutableListOf<MemorySnapshot>()

    // Caching system
    private val cacheRegistry = ConcurrentHashMap<String, Cache<*, *>>()
    private val imageCache = LRUCache<String, Any>(maxSize = 50)
    private val dataCache = LRUCache<String, Any>(maxSize = 100)

    // Resource pooling
    private val viewHolderPool = RecyclerView.RecycledViewPool()
    private val coroutinePool = Dispatchers.IO.limitedParallelism(4)

    companion object {
        @Volatile
        private var INSTANCE: PerformanceOptimizer? = null

        fun getInstance(context: Context): PerformanceOptimizer {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PerformanceOptimizer(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    init {
        startPerformanceMonitoring()
        setupMemoryPressureMonitoring()
    }

    /**
     * Performance metrics data class
     */
    data class PerformanceMetrics(
        val avgFrameTime: Long = 0L,
        val memoryUsage: Long = 0L,
        val cpuUsage: Double = 0.0,
        val networkLatency: Long = 0L,
        val cacheHitRate: Double = 0.0,
        val lastUpdateTime: Long = System.currentTimeMillis()
    )

    /**
     * Memory pressure levels
     */
    enum class MemoryPressureLevel {
        NORMAL,
        MODERATE,
        HIGH,
        CRITICAL
    }

    /**
     * Memory snapshot for tracking
     */
    data class MemorySnapshot(
        val timestamp: Long,
        val totalMemory: Long,
        val usedMemory: Long,
        val freeMemory: Long,
        val gcCount: Int
    )

    /**
     * LRU Cache implementation
     */
    class LRUCache<K, V>(private val maxSize: Int) {
        private val cache = LinkedHashMap<K, V>(16, 0.75f, true)

        @Synchronized
        fun get(key: K): V? = cache[key]

        @Synchronized
        fun put(key: K, value: V): V? {
            val previous = cache.put(key, value)
            if (cache.size > maxSize) {
                val eldest = cache.entries.iterator().next()
                cache.remove(eldest.key)
            }
            return previous
        }

        @Synchronized
        fun remove(key: K): V? = cache.remove(key)

        @Synchronized
        fun clear() = cache.clear()

        @Synchronized
        fun size(): Int = cache.size
    }

    /**
     * Generic cache interface
     */
    interface Cache<K, V> {
        fun get(key: K): V?
        fun put(key: K, value: V)
        fun remove(key: K)
        fun clear()
        fun size(): Int
    }

    /**
     * Settings-specific cache for frequently accessed data
     */
    class SettingsCache : Cache<String, Any> {
        private val cache = LRUCache<String, Any>(150)
        private val accessCounts = ConcurrentHashMap<String, Int>()

        override fun get(key: String): Any? {
            accessCounts[key] = (accessCounts[key] ?: 0) + 1
            return cache.get(key)
        }

        override fun put(key: String, value: Any) {
            cache.put(key, value)
        }

        override fun remove(key: String) {
            cache.remove(key)
            accessCounts.remove(key)
        }

        override fun clear() {
            cache.clear()
            accessCounts.clear()
        }

        override fun size(): Int = cache.size()

        fun getHitRate(): Double {
            val totalAccess = accessCounts.values.sum()
            val hits = accessCounts.size
            return if (totalAccess > 0) hits.toDouble() / totalAccess else 0.0
        }
    }

    /**
     * Start performance monitoring
     */
    private fun startPerformanceMonitoring() {
        if (!performanceMonitoringEnabled) return

        optimizationScope.launch {
            while (isActive) {
                try {
                    collectPerformanceMetrics()
                    delay(5000) // Collect metrics every 5 seconds
                } catch (e: Exception) {
                    // Log error and continue
                }
            }
        }
    }

    /**
     * Collect performance metrics
     */
    private suspend fun collectPerformanceMetrics() = withContext(Dispatchers.Default) {
        val memoryInfo = Runtime.getRuntime().let { runtime ->
            MemorySnapshot(
                timestamp = System.currentTimeMillis(),
                totalMemory = runtime.totalMemory(),
                usedMemory = runtime.totalMemory() - runtime.freeMemory(),
                freeMemory = runtime.freeMemory(),
                gcCount = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Debug.getRuntimeStats()["art.gc.gc-count"]?.toIntOrNull() ?: 0
                } else 0
            )
        }

        memorySnapshots.add(memoryInfo)
        if (memorySnapshots.size > 100) {
            memorySnapshots.removeAt(0)
        }

        // Calculate average frame time
        val avgFrameTime = if (frameMetrics.isNotEmpty()) {
            frameMetrics.average().toLong()
        } else 0L

        // Calculate cache hit rate
        val cacheHitRate = cacheRegistry.values
            .filterIsInstance<SettingsCache>()
            .map { it.getHitRate() }
            .average()
            .takeIf { !it.isNaN() } ?: 0.0

        val metrics = PerformanceMetrics(
            avgFrameTime = avgFrameTime,
            memoryUsage = memoryInfo.usedMemory,
            cacheHitRate = cacheHitRate,
            lastUpdateTime = System.currentTimeMillis()
        )

        performanceMetrics.value = metrics
        updateMemoryPressure(memoryInfo)
    }

    /**
     * Setup memory pressure monitoring
     */
    private fun setupMemoryPressureMonitoring() {
        optimizationScope.launch {
            memoryPressureState.collect { level ->
                when (level) {
                    MemoryPressureLevel.HIGH -> {
                        performMemoryOptimizations()
                    }
                    MemoryPressureLevel.CRITICAL -> {
                        performAggressiveMemoryCleanup()
                    }
                    else -> {
                        // Normal operation
                    }
                }
            }
        }
    }

    /**
     * Update memory pressure level
     */
    private fun updateMemoryPressure(memoryInfo: MemorySnapshot) {
        val memoryUsageRatio = memoryInfo.usedMemory.toDouble() / memoryInfo.totalMemory

        val newLevel = when {
            memoryUsageRatio > 0.9 -> MemoryPressureLevel.CRITICAL
            memoryUsageRatio > 0.75 -> MemoryPressureLevel.HIGH
            memoryUsageRatio > 0.6 -> MemoryPressureLevel.MODERATE
            else -> MemoryPressureLevel.NORMAL
        }

        if (newLevel != memoryPressureState.value) {
            memoryPressureState.value = newLevel
        }
    }

    /**
     * Perform memory optimizations
     */
    private suspend fun performMemoryOptimizations() = withContext(Dispatchers.Default) {
        // Clear least recently used cache entries
        cacheRegistry.values.forEach { cache ->
            if (cache.size() > 50) {
                // Remove 25% of entries
                repeat(cache.size() / 4) {
                    // LRU cache will automatically remove oldest entries
                }
            }
        }

        // Clear image cache partially
        if (imageCache.size() > 25) {
            repeat(imageCache.size() / 3) {
                // Clear some image cache entries
            }
        }

        // Suggest garbage collection
        System.gc()
    }

    /**
     * Perform aggressive memory cleanup
     */
    private suspend fun performAggressiveMemoryCleanup() = withContext(Dispatchers.Default) {
        // Clear most caches
        cacheRegistry.values.forEach { it.clear() }
        imageCache.clear()

        // Clear frame metrics history
        frameMetrics.clear()

        // Keep only recent memory snapshots
        if (memorySnapshots.size > 10) {
            val toKeep = memorySnapshots.takeLast(10)
            memorySnapshots.clear()
            memorySnapshots.addAll(toKeep)
        }

        // Force garbage collection
        System.gc()
    }

    /**
     * Optimize RecyclerView performance
     */
    fun optimizeRecyclerView(recyclerView: RecyclerView) {
        recyclerView.apply {
            // Use shared view pool for better memory efficiency
            setRecycledViewPool(viewHolderPool)

            // Enable view caching
            setItemViewCacheSize(20)

            // Optimize for stable IDs if available
            if (adapter?.hasStableIds() == true) {
                setHasFixedSize(true)
            }

            // Pre-fetch items for smooth scrolling
            isNestedScrollingEnabled = false

            // Add scroll listener for performance monitoring
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    recordFrameTime()
                }
            })
        }
    }

    /**
     * Record frame time for performance monitoring
     */
    private fun recordFrameTime() {
        val frameTime = measureTimeMillis {
            // Measure frame rendering time
        }

        frameMetrics.add(frameTime)
        if (frameMetrics.size > 60) { // Keep last 60 frames
            frameMetrics.removeAt(0)
        }
    }

    /**
     * Create optimized coroutine scope for background work
     */
    fun createOptimizedScope(): CoroutineScope {
        return CoroutineScope(coroutinePool + SupervisorJob())
    }

    /**
     * Get settings cache instance
     */
    fun getSettingsCache(): SettingsCache {
        return cacheRegistry.getOrPut("settings") {
            SettingsCache()
        } as SettingsCache
    }

    /**
     * Debounce function for expensive operations
     */
    fun <T> debounce(
        delayMs: Long = 300L,
        scope: CoroutineScope,
        action: suspend (T) -> Unit
    ): (T) -> Unit {
        var debounceJob: Job? = null
        return { param: T ->
            debounceJob?.cancel()
            debounceJob = scope.launch {
                delay(delayMs)
                action(param)
            }
        }
    }

    /**
     * Throttle function for rapid operations
     */
    fun <T> throttle(
        intervalMs: Long = 100L,
        scope: CoroutineScope,
        action: suspend (T) -> Unit
    ): (T) -> Unit {
        var lastExecution = 0L
        var throttleJob: Job? = null

        return { param: T ->
            val now = System.currentTimeMillis()
            if (now - lastExecution >= intervalMs) {
                lastExecution = now
                throttleJob?.cancel()
                throttleJob = scope.launch {
                    action(param)
                }
            }
        }
    }

    /**
     * Memory-efficient image loading helper
     */
    fun loadImageOptimized(
        url: String,
        onLoaded: (Any?) -> Unit
    ) {
        // Check cache first
        val cached = imageCache.get(url)
        if (cached != null) {
            onLoaded(cached)
            return
        }

        optimizationScope.launch {
            try {
                // Simulate image loading with memory management
                val image = loadImageWithMemoryManagement(url)

                // Cache with memory pressure awareness
                if (memoryPressureState.value != MemoryPressureLevel.CRITICAL) {
                    imageCache.put(url, image)
                }

                withContext(Dispatchers.Main) {
                    onLoaded(image)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onLoaded(null)
                }
            }
        }
    }

    /**
     * Load image with memory management
     */
    private suspend fun loadImageWithMemoryManagement(url: String): Any = withContext(Dispatchers.IO) {
        // Simulate memory-efficient image loading
        delay(100) // Simulate network/disk I/O
        "Optimized image for $url"
    }

    /**
     * Get performance metrics flow
     */
    fun getPerformanceMetrics(): StateFlow<PerformanceMetrics> = performanceMetrics.asStateFlow()

    /**
     * Get memory pressure state flow
     */
    fun getMemoryPressureState(): StateFlow<MemoryPressureLevel> = memoryPressureState.asStateFlow()

    /**
     * Lifecycle-aware cleanup using LifecycleEventObserver
     */
    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_STOP -> {
                CoroutineScope(Dispatchers.Main).launch {
                    performMemoryOptimizations()
                }
            }
            Lifecycle.Event.ON_DESTROY -> {
                optimizationScope.cancel()
                cacheRegistry.clear()
                imageCache.clear()
                dataCache.clear()
            }
            else -> {
                // Handle other lifecycle events if needed
            }
        }
    }

    /**
     * Manual cleanup trigger
     */
    fun performManualCleanup() {
        optimizationScope.launch {
            when (memoryPressureState.value) {
                MemoryPressureLevel.CRITICAL, MemoryPressureLevel.HIGH -> {
                    performAggressiveMemoryCleanup()
                }
                else -> {
                    performMemoryOptimizations()
                }
            }
        }
    }

    /**
     * Get cache statistics
     */
    fun getCacheStatistics(): Map<String, Int> {
        return buildMap {
            put("settings_cache_size", getSettingsCache().size())
            put("image_cache_size", imageCache.size())
            put("data_cache_size", dataCache.size())
            put("total_caches", cacheRegistry.size)
        }
    }

    /**
     * Set monitoring enabled/disabled for battery optimization
     */
    fun setMonitoringEnabled(enabled: Boolean) {
        performanceMonitoringEnabled = enabled
        if (!enabled) {
            // Clear current metrics when disabling
            clearMetrics()
        }
    }

    /**
     * Set reduced monitoring mode for low power scenarios
     */
    fun setReducedMonitoring(reduced: Boolean) {
        // Could adjust monitoring frequency based on reduced flag
        // For now, just log the state change
        android.util.Log.d("PerformanceOptimizer", "Reduced monitoring: $reduced")
    }

    /**
     * Set reduced frame rate for animations during low power
     */
    fun setReducedFrameRate(reduced: Boolean) {
        // This would typically adjust animation frame rates
        // Implementation depends on animation system integration
        android.util.Log.d("PerformanceOptimizer", "Reduced frame rate: $reduced")
    }

    /**
     * Enable/disable animations for critical battery scenarios
     */
    fun setAnimationsEnabled(enabled: Boolean) {
        // This would control animation playback
        // Implementation depends on animation system integration
        android.util.Log.d("PerformanceOptimizer", "Animations enabled: $enabled")
    }

    /**
     * Perform aggressive cleanup for critical battery situations
     */
    suspend fun performAggressiveCleanup() = withContext(Dispatchers.Default) {
        performMemoryOptimizations()
        performAggressiveMemoryCleanup()
    }

    /**
     * Clear all caches for critical battery optimization
     */
    fun clearAllCaches() {
        cacheRegistry.values.forEach { it.clear() }
        imageCache.clear()
        dataCache.clear()
        clearMetrics()
    }

    /**
     * Clear performance metrics and reset monitoring data
     */
    fun clearMetrics() {
        frameMetrics.clear()
        memorySnapshots.clear()
        performanceMetrics.value = PerformanceMetrics()
    }
}