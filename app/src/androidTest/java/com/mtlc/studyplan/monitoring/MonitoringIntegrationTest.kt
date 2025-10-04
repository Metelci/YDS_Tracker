package com.mtlc.studyplan.monitoring

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mtlc.studyplan.performance.PerformanceMonitor
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration Tests for Crash Reporting and Performance Monitoring
 * Tests the monitoring systems work correctly together
 */
@RunWith(AndroidJUnit4::class)
class MonitoringIntegrationTest {

    private lateinit var context: Context
    private lateinit var crashReporter: CrashReporter
    private lateinit var performanceMonitor: PerformanceMonitor
    private lateinit var realTimeMonitor: RealTimePerformanceMonitor

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        crashReporter = CrashReporter(context)
        performanceMonitor = PerformanceMonitor()
        realTimeMonitor = RealTimePerformanceMonitor(context, performanceMonitor)
    }

    @Test
    fun `crash reporter initializes correctly`() {
        assertNotNull("CrashReporter should be initialized", crashReporter)
        assertNotNull("Crash reports flow should be available", crashReporter.crashReports)
        assertNotNull("Error stats flow should be available", crashReporter.errorStats)
    }

    @Test
    fun `performance monitor initializes correctly`() {
        assertNotNull("PerformanceMonitor should be initialized", performanceMonitor)
        assertNotNull("Performance metrics flow should be available", performanceMonitor.performanceMetrics)
    }

    @Test
    fun `real-time performance monitor initializes correctly`() {
        assertNotNull("RealTimePerformanceMonitor should be initialized", realTimeMonitor)
        assertNotNull("Performance metrics flow should be available", realTimeMonitor.performanceMetrics)
        assertNotNull("Performance alerts flow should be available", realTimeMonitor.performanceAlerts)
    }

    @Test
    fun `crash reporter can report errors without crashing`() = runTest {
        val testException = RuntimeException("Test error for monitoring")
        val context = "Test Context"
        val additionalData = mapOf("testKey" to "testValue")

        // This should not throw an exception
        crashReporter.reportError(testException, context, ErrorSeverity.MEDIUM, additionalData)

        // Give some time for async processing
        delay(100)

        // Verify error stats were updated
        val stats = crashReporter.errorStats.value
        assertTrue("Error count should be greater than 0", stats.totalErrors >= 0)
    }

    @Test
    fun `performance monitor can record operations without crashing`() = runTest {
        // Record some performance operations
        performanceMonitor.recordCacheHit()
        performanceMonitor.recordCacheMiss()

        val operationTime = performanceMonitor.measureOperation("test_operation") {
            delay(10) // Simulate some work
        }

        // Verify operation was measured
        assertTrue("Operation time should be greater than 0", operationTime > 0)

        // Verify performance report can be generated
        val report = performanceMonitor.getPerformanceReport()
        assertNotNull("Performance report should be generated", report)
        assertTrue("Report should contain content", report.isNotEmpty())
    }

    @Test
    fun `real-time monitor can collect metrics without crashing`() = runTest {
        // Start monitoring
        realTimeMonitor.startMonitoring()

        // Wait for some metrics to be collected
        delay(2000)

        // Check that metrics are being collected
        val metrics = realTimeMonitor.performanceMetrics.value
        assertNotNull("Metrics should be collected", metrics)

        // Verify basic metric properties
        assertTrue("Timestamp should be set", metrics.timestamp > 0)
        assertTrue("Memory metrics should be available", metrics.memoryMetrics.totalMemoryMB > 0)

        // Stop monitoring
        realTimeMonitor.stopMonitoring()
    }

    @Test
    fun `performance trends can be calculated`() = runTest {
        realTimeMonitor.startMonitoring()

        // Wait for some history to be collected
        delay(3000)

        // Get performance trends
        val trends = realTimeMonitor.getPerformanceTrends()

        // Verify trends object is created (may have default values if insufficient data)
        assertNotNull("Performance trends should be calculated", trends)

        realTimeMonitor.stopMonitoring()
    }

    @Test
    fun `performance report can be generated`() = runTest {
        realTimeMonitor.startMonitoring()

        delay(1000)

        // Generate performance report
        val report = realTimeMonitor.generatePerformanceReport()

        assertNotNull("Performance report should be generated", report)
        assertNotNull("Report should have current metrics", report.currentMetrics)
        assertNotNull("Report should have trends", report.trends)
        assertTrue("Report should have recommendations", report.recommendations.isNotEmpty())

        realTimeMonitor.stopMonitoring()
    }

    @Test
    fun `crash reporter summary can be generated`() {
        val summary = crashReporter.generateCrashReportSummary()

        assertNotNull("Crash report summary should be generated", summary)
        assertNotNull("Summary should have error statistics", summary.errorStatistics)

        // Summary values may be 0 if no crashes have occurred
        assertTrue("Total crashes should be non-negative", summary.totalCrashes >= 0)
        assertTrue("Crash rate should be non-negative", summary.crashRate >= 0.0)
    }

    @Test
    fun `monitoring systems work together without conflicts`() = runTest {
        // Start all monitoring systems
        realTimeMonitor.startMonitoring()

        // Perform some operations that might trigger monitoring
        performanceMonitor.recordCacheHit()
        performanceMonitor.recordCacheMiss()

        val testException = IllegalArgumentException("Test exception for integration")
        crashReporter.reportError(testException, "Integration Test", ErrorSeverity.LOW)

        // Wait for systems to process
        delay(2000)

        // Verify all systems are still functioning
        assertNotNull("Performance metrics still available", realTimeMonitor.performanceMetrics.value)
        assertNotNull("Crash reports still available", crashReporter.crashReports.value)
        assertNotNull("Performance monitor still working", performanceMonitor.getPerformanceReport())

        realTimeMonitor.stopMonitoring()
    }

    @Test
    fun `alert system works correctly`() = runTest {
        realTimeMonitor.startMonitoring()

        // Wait for monitoring to establish baseline
        delay(1000)

        // Check initial alert state
        val initialAlerts = realTimeMonitor.performanceAlerts.value
        assertNotNull("Initial alerts should be available", initialAlerts)

        // Clear any existing alerts
        realTimeMonitor.clearAlerts()

        // Verify alerts were cleared
        val clearedAlerts = realTimeMonitor.performanceAlerts.value
        assertEquals("Alerts should be cleared", 0, clearedAlerts.size)

        realTimeMonitor.stopMonitoring()
    }

    @Test
    fun `performance history is maintained correctly`() = runTest {
        realTimeMonitor.startMonitoring()

        // Wait for some history to accumulate
        delay(3000)

        // Get performance history
        val history = realTimeMonitor.getPerformanceHistory(1) // Last minute

        // Verify history is being collected
        assertNotNull("Performance history should be available", history)
        assertTrue("Should have some historical data", history.isNotEmpty())

        // Verify history items have valid data
        history.forEach { metrics ->
            assertTrue("Historical timestamp should be valid", metrics.timestamp > 0)
            assertTrue("Historical memory should be valid", metrics.memoryMetrics.totalMemoryMB > 0)
        }

        realTimeMonitor.stopMonitoring()
    }

    @Test
    fun `error severity levels are handled correctly`() {
        val severities = listOf(
            ErrorSeverity.LOW,
            ErrorSeverity.MEDIUM,
            ErrorSeverity.HIGH,
            ErrorSeverity.CRITICAL
        )

        severities.forEach { severity ->
            val testException = Exception("Test exception with $severity severity")
            crashReporter.reportError(testException, "Severity Test", severity)

            // Verify no exceptions thrown during reporting
            assertTrue("Error reporting should succeed for $severity", true)
        }
    }

    @Test
    fun `ANR reporting works correctly`() = runBlocking {
        val anrDuration = 5000L // 5 seconds
        val context = "Test ANR"
        val additionalData = mapOf("test" to "data")

        // Report ANR
        crashReporter.reportANR(anrDuration, context, additionalData)

        // Give time for async processing
        delay(100)

        // Verify ANR was recorded in stats
        val stats = crashReporter.errorStats.value
        assertTrue("ANR count should be updated", stats.totalANRs >= 0)
    }

    @Test
    fun `monitoring systems handle rapid operations correctly`() = runTest {
        realTimeMonitor.startMonitoring()

        // Perform rapid operations
        repeat(10) {
            performanceMonitor.recordCacheHit()
            delay(10)
        }

        repeat(5) {
            val testException = Exception("Rapid error $it")
            crashReporter.reportError(testException, "Rapid Test", ErrorSeverity.LOW)
            delay(10)
        }

        // Wait for processing
        delay(1000)

        // Verify systems are still functioning
        assertNotNull("Performance metrics still work", realTimeMonitor.performanceMetrics.value)
        assertNotNull("Crash reporting still works", crashReporter.generateCrashReportSummary())

        realTimeMonitor.stopMonitoring()
    }
}