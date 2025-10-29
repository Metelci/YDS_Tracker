package com.mtlc.studyplan.memory

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import androidx.collection.LruCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import java.lang.ref.WeakReference

/**
 * Memory Manager - Optimizes memory usage and prevents leaks
 */
object MemoryManager {
    
    private const val TAG = "MemoryManager"
    
    // Memory pressure levels
    enum class MemoryPressure {
        NORMAL, MODERATE, LOW
    }
    
    // Memory cache with dynamic sizing
    private var memoryCache: LruCache<String, Any>? = null
    
    // Memory pressure monitoring
    private val _memoryPressure = MutableStateFlow(MemoryPressure.NORMAL)
    val memoryPressure: StateFlow<MemoryPressure> = _memoryPressure.asStateFlow()
    
    // Coroutine scope for memory operations
    private val memoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Memory monitoring job
    private var monitoringJob: Job? = null
    
    /**
     * Initialize memory manager with context
     */
    fun initialize(context: Context) {
        if (memoryCache == null) {
            val cacheSize = calculateOptimalCacheSize(context)
            memoryCache = LruCache<String, Any>(cacheSize)
        }
        
        startMemoryMonitoring(context)
    }
    
    /**
     * Calculate optimal cache size based on available memory
     */
    private fun calculateOptimalCacheSize(context: Context): Int {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryClass = activityManager.memoryClass
        val isLargeHeap = (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_LARGE_HEAP) != 0
        
        val availableMemory = if (isLargeHeap && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
            activityManager.largeMemoryClass
        } else {
            memoryClass
        }
        
        // Use 10-15% of available memory for cache
        val optimalCacheSize = (availableMemory * 1024 * 1024 * 0.15).toInt()
        
        // Return a reasonable size between 4MB and 32MB
        return optimalCacheSize.coerceIn(4 * 1024 * 1024, 32 * 1024 * 1024)
    }
    
    /**
     * Start memory monitoring
     */
    private fun startMemoryMonitoring(context: Context) {
        monitoringJob?.cancel()
        
        monitoringJob = memoryScope.launch {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            try {
                while (isActive) {
                    val memoryInfo = ActivityManager.MemoryInfo()
                    activityManager.getMemoryInfo(memoryInfo)

                    val pressure = when {
                        memoryInfo.lowMemory -> MemoryPressure.LOW
                        memoryInfo.availMem < (memoryInfo.totalMem * 0.15) -> MemoryPressure.MODERATE
                        else -> MemoryPressure.NORMAL
                    }

                    _memoryPressure.value = pressure

                    delay(30000)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error monitoring memory", e)
            }
        }
    }
    
    /**
     * Stop memory monitoring
     */
    fun stopMemoryMonitoring() {
        monitoringJob?.cancel()
        memoryScope.coroutineContext.cancelChildren()
        monitoringJob = null
    }
    
    /**
     * Get memory cache with proper sizing
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getMemoryCache(): LruCache<String, T> {
        val cache = memoryCache ?: throw IllegalStateException("MemoryManager not initialized")
        return cache as LruCache<String, T>
    }
    
    /**
     * Clear memory cache to free up memory
     */
    fun clearMemoryCache() {
        memoryCache?.evictAll()
    }
    
    /**
     * Suggest garbage collection when memory pressure is high
     */
    fun suggestGarbageCollection(pressure: MemoryPressure) {
        if (pressure == MemoryPressure.LOW) {
            System.gc()
        }
    }
    
    /**
     * Memory-aware operation execution
     */
    suspend fun <T> executeMemoryAwareOperation(
        operation: suspend () -> T
    ): Result<T> {
        return try {
            // Check current memory pressure
            when (_memoryPressure.value) {
                MemoryPressure.LOW -> {
                    // Under low memory, try to free some resources first
                    clearMemoryCache()
                    suggestGarbageCollection(MemoryPressure.LOW)
                    kotlinx.coroutines.delay(100) // Give GC time to run
                }
                MemoryPressure.MODERATE -> {
                    // Under moderate memory, be more conservative
                    clearMemoryCache()
                }
                MemoryPressure.NORMAL -> {
                    // Normal memory, proceed normally
                }
            }
            
            Result.success(operation())
        } catch (e: OutOfMemoryError) {
            // Handle OOM gracefully
            clearMemoryCache()
            suggestGarbageCollection(MemoryPressure.LOW)
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Memory leak prevention utilities
     */
    object LeakPrevention {
        
        /**
         * Create weak reference to prevent memory leaks
         */
        fun <T> createWeakReference(obj: T): WeakReference<T> {
            return WeakReference(obj)
        }
        
        /**
         * Clear collection to prevent memory leaks
         */
        fun <T> clearCollection(collection: MutableCollection<T>) {
            collection.clear()
        }
        
        /**
         * Clear map to prevent memory leaks
         */
        fun <K, V> clearMap(map: MutableMap<K, V>) {
            map.clear()
        }
        
        /**
         * Nullify reference to prevent memory leaks
         */
        fun <T> nullifyReference(ref: T?): T? {
            return null
        }
    }
}
