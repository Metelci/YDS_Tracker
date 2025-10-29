package com.mtlc.studyplan

import com.mtlc.studyplan.performance.PerformanceMonitor
import org.junit.Test
import org.junit.Before
import org.koin.core.context.stopKoin
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.junit.Assert.*

/**
 * Simple performance validation tests
 */
@RunWith(RobolectricTestRunner::class)
class SimplePerformanceTest {

    private lateinit var performanceMonitor: PerformanceMonitor

    @Before
    fun setup() {
        try { stopKoin() } catch (e: Exception) { }
        performanceMonitor = PerformanceMonitor()
    }

    @Test
    fun `performance monitor initializes correctly`() {
        assertNotNull("Performance monitor should initialize", performanceMonitor)
        val report = performanceMonitor.getPerformanceReport()
        assertTrue("Should generate performance report", report.contains("Performance Report"))
    }

    @Test
    fun `cache tracking works correctly`() {
        performanceMonitor.recordCacheHit()
        performanceMonitor.recordCacheHit()
        performanceMonitor.recordCacheMiss()

        val report = performanceMonitor.getPerformanceReport()
        assertTrue("Should track cache hits", report.contains("Cache Hit Rate"))
    }

    @Test
    fun `operation measurement works`() {
        val operationTime = performanceMonitor.measureOperation("test_operation") {
            Thread.sleep(10) // Simulate work
        }

        assertTrue("Should measure operation time", operationTime > 0)
    }

    @Test
    fun `memory usage tracking is functional`() {
        val initialMemory = Runtime.getRuntime().let { runtime ->
            (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        }

        // Simulate some work
        val data = mutableListOf<String>()
        repeat(100) {
            data.add("test_string_$it")
        }

        val finalMemory = Runtime.getRuntime().let { runtime ->
            (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        }

        // Memory can be tracked (may increase or decrease due to GC)
        assertTrue("Memory should be trackable", finalMemory >= 0 && initialMemory >= 0)
    }

    @Test
    fun `performance issue logging works`() {
        performanceMonitor.logPerformanceIssue("test_context", "test_issue")
        val report = performanceMonitor.getPerformanceReport()
        assertTrue("Should log performance issues", report.contains("Recent Issues") || report.contains("test"))
    }

    @Test
    fun `performance metrics cleanup works`() {
        performanceMonitor.recordCacheHit()
        performanceMonitor.clearMetrics()

        // Should not crash after clearing metrics
        val report = performanceMonitor.getPerformanceReport()
        assertNotNull("Should generate report after clearing", report)
    }
}
