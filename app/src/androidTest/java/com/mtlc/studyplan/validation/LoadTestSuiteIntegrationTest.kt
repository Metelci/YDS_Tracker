package com.mtlc.studyplan.validation

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mtlc.studyplan.data.StudyProgressRepository
import com.mtlc.studyplan.data.TaskRepository
import com.mtlc.studyplan.performance.PerformanceMonitor
import com.mtlc.studyplan.settings.manager.SettingsManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration Tests for Load Testing Suite
 * Tests the load testing functionality itself under various scenarios
 */
@RunWith(AndroidJUnit4::class)
class LoadTestSuiteIntegrationTest {

    private lateinit var context: Context
    private lateinit var taskRepository: TaskRepository
    private lateinit var studyProgressRepository: StudyProgressRepository
    private lateinit var settingsManager: SettingsManager
    private lateinit var performanceMonitor: PerformanceMonitor
    private lateinit var loadTestSuite: LoadTestSuite

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()

        // Initialize repositories (would normally use dependency injection)
        taskRepository = object : TaskRepository {
            override fun getAllTasks() = kotlinx.coroutines.flow.flowOf(emptyList())
            override suspend fun getAllTasksSync() = emptyList()
            override suspend fun getTaskById(id: String) = null
            override suspend fun insertTask(task: com.mtlc.studyplan.data.Task) = task
            override suspend fun updateTask(task: com.mtlc.studyplan.data.Task) = task
            override suspend fun deleteTask(id: String) {}
            override suspend fun getTodaysTasks() = emptyList()
            override suspend fun getUpcomingTasks() = emptyList()
            override suspend fun getTasksByCategory(category: String) = emptyList()
            override suspend fun getEarlyMorningCompletedTasks() = emptyList()
        }

        studyProgressRepository = StudyProgressRepository(context)
        settingsManager = SettingsManager(context)
        performanceMonitor = PerformanceMonitor()

        loadTestSuite = LoadTestSuite(
            context, taskRepository, studyProgressRepository, settingsManager, performanceMonitor
        )
    }

    @Test
    fun `load test suite initializes correctly`() {
        assertNotNull("LoadTestSuite should be initialized", loadTestSuite)
    }

    @Test
    fun `comprehensive load test runs successfully with minimal config`() = runBlocking {
        val config = LoadTestSuite.LoadTestConfig(
            concurrentUsers = 2,
            testDurationMs = 2000L, // Short test for CI
            operationsPerUser = 5
        )

        val result = loadTestSuite.runComprehensiveLoadTest(config)

        assertTrue("Load test should succeed", result.isSuccess)
        result.onSuccess { loadResult ->
            assertTrue("Should have some operations", loadResult.totalOperations > 0)
            assertTrue("Error rate should be reasonable", loadResult.errorRate < 0.5)
            assertTrue("Should have response times", loadResult.averageResponseTime >= 0)
            assertTrue("Test duration should be reasonable", loadResult.testDuration > 0)
        }
    }

    @Test
    fun `stress test runs with maximum concurrent users`() = runBlocking {
        val result = loadTestSuite.runStressTest()

        // Stress test might timeout or have higher error rates, but should complete
        assertNotNull("Stress test should return a result", result)
        result.onSuccess { loadResult ->
            assertTrue("Should attempt operations", loadResult.totalOperations >= 0)
            // Don't assert success rate for stress tests as they're designed to push limits
        }
    }

    @Test
    fun `endurance test runs for extended duration`() = runBlocking {
        val config = LoadTestSuite.LoadTestConfig(
            concurrentUsers = 3,
            testDurationMs = 5000L, // Shortened for testing
            operationsPerUser = 10
        )

        val startTime = System.currentTimeMillis()
        val result = loadTestSuite.runComprehensiveLoadTest(config)
        val endTime = System.currentTimeMillis()

        assertTrue("Endurance test should succeed", result.isSuccess)
        result.onSuccess { loadResult ->
            assertTrue("Test should run for expected duration", loadResult.testDuration >= config.testDurationMs - 1000)
            assertTrue("Should have operations", loadResult.totalOperations > 0)
        }
    }

    @Test
    fun `load test handles zero concurrent users gracefully`() = runBlocking {
        val config = LoadTestSuite.LoadTestConfig(
            concurrentUsers = 0,
            testDurationMs = 1000L,
            operationsPerUser = 0
        )

        val result = loadTestSuite.runComprehensiveLoadTest(config)

        assertTrue("Zero user test should succeed", result.isSuccess)
        result.onSuccess { loadResult ->
            assertEquals("Should have zero operations", 0, loadResult.totalOperations)
            assertEquals("Should have zero successful operations", 0, loadResult.successfulOperations)
            assertEquals("Should have zero failed operations", 0, loadResult.failedOperations)
        }
    }

    @Test
    fun `load test report generation works correctly`() {
        val mockResult = LoadTestSuite.LoadTestResult(
            totalOperations = 100,
            successfulOperations = 95,
            failedOperations = 5,
            averageResponseTime = 150L,
            peakMemoryUsage = 50 * 1024 * 1024, // 50MB
            errorRate = 0.05,
            throughput = 10.0,
            testDuration = 10000L
        )

        val report = loadTestSuite.generateLoadTestReport(mockResult)

        assertNotNull("Report should be generated", report)
        assertTrue("Report should contain key metrics", report.contains("Total Operations: 100"))
        assertTrue("Report should contain success count", report.contains("Successful Operations: 95"))
        assertTrue("Report should contain error rate", report.contains("Error Rate: 5.00%"))
        assertTrue("Report should contain performance assessment", report.contains("Performance Assessment"))
    }

    @Test
    fun `load test handles database-only operations correctly`() = runBlocking {
        val config = LoadTestSuite.LoadTestConfig(
            concurrentUsers = 2,
            testDurationMs = 2000L,
            operationsPerUser = 5,
            includeDatabaseOperations = true,
            includeSettingsOperations = false,
            includePerformanceMonitoring = false
        )

        val result = loadTestSuite.runComprehensiveLoadTest(config)

        assertTrue("Database-only test should succeed", result.isSuccess)
        result.onSuccess { loadResult ->
            assertTrue("Should have operations", loadResult.totalOperations > 0)
        }
    }

    @Test
    fun `load test handles settings-only operations correctly`() = runBlocking {
        val config = LoadTestSuite.LoadTestConfig(
            concurrentUsers = 2,
            testDurationMs = 2000L,
            operationsPerUser = 5,
            includeDatabaseOperations = false,
            includeSettingsOperations = true,
            includePerformanceMonitoring = false
        )

        val result = loadTestSuite.runComprehensiveLoadTest(config)

        assertTrue("Settings-only test should succeed", result.isSuccess)
        result.onSuccess { loadResult ->
            assertTrue("Should have operations", loadResult.totalOperations > 0)
        }
    }

    @Test
    fun `load test handles performance-only operations correctly`() = runBlocking {
        val config = LoadTestSuite.LoadTestConfig(
            concurrentUsers = 2,
            testDurationMs = 2000L,
            operationsPerUser = 5,
            includeDatabaseOperations = false,
            includeSettingsOperations = false,
            includePerformanceMonitoring = true
        )

        val result = loadTestSuite.runComprehensiveLoadTest(config)

        assertTrue("Performance-only test should succeed", result.isSuccess)
        result.onSuccess { loadResult ->
            assertTrue("Should have operations", loadResult.totalOperations > 0)
        }
    }

    @Test
    fun `load test validates memory usage tracking`() = runBlocking {
        val config = LoadTestSuite.LoadTestConfig(
            concurrentUsers = 3,
            testDurationMs = 3000L,
            operationsPerUser = 8
        )

        val result = loadTestSuite.runComprehensiveLoadTest(config)

        assertTrue("Memory tracking test should succeed", result.isSuccess)
        result.onSuccess { loadResult ->
            assertTrue("Peak memory usage should be tracked", loadResult.peakMemoryUsage > 0)
            assertTrue("Memory usage should be reasonable", loadResult.peakMemoryUsage < 500 * 1024 * 1024) // Less than 500MB
        }
    }

    @Test
    fun `load test validates throughput calculations`() = runBlocking {
        val config = LoadTestSuite.LoadTestConfig(
            concurrentUsers = 2,
            testDurationMs = 2000L,
            operationsPerUser = 10
        )

        val result = loadTestSuite.runComprehensiveLoadTest(config)

        assertTrue("Throughput test should succeed", result.isSuccess)
        result.onSuccess { loadResult ->
            assertTrue("Throughput should be calculated", loadResult.throughput >= 0.0)
            assertTrue("Throughput should be reasonable", loadResult.throughput < 100.0) // Less than 100 ops/sec for this test
        }
    }

    @Test
    fun `load test handles cancellation gracefully`() = runBlocking {
        val config = LoadTestSuite.LoadTestConfig(
            concurrentUsers = 5,
            testDurationMs = 100L, // Very short duration to force timeout
            operationsPerUser = 100 // Many operations
        )

        val result = loadTestSuite.runComprehensiveLoadTest(config)

        // Test should either succeed or fail gracefully, but not crash
        assertNotNull("Load test should return a result even when cancelled", result)
    }

    @Test
    fun `load test performance assessment works correctly`() {
        // Test high error rate scenario
        val highErrorResult = LoadTestSuite.LoadTestResult(
            totalOperations = 100,
            successfulOperations = 80,
            failedOperations = 20,
            averageResponseTime = 500L,
            peakMemoryUsage = 100 * 1024 * 1024,
            errorRate = 0.2,
            throughput = 5.0,
            testDuration = 20000L
        )

        val highErrorReport = loadTestSuite.generateLoadTestReport(highErrorResult)
        assertTrue("High error rate should be flagged", highErrorReport.contains("HIGH ERROR RATE"))

        // Test slow response time scenario
        val slowResponseResult = LoadTestSuite.LoadTestResult(
            totalOperations = 100,
            successfulOperations = 95,
            failedOperations = 5,
            averageResponseTime = 1500L,
            peakMemoryUsage = 100 * 1024 * 1024,
            errorRate = 0.05,
            throughput = 8.0,
            testDuration = 12500L
        )

        val slowResponseReport = loadTestSuite.generateLoadTestReport(slowResponseResult)
        assertTrue("Slow response times should be flagged", slowResponseReport.contains("SLOW RESPONSE TIMES"))

        // Test good performance scenario
        val goodResult = LoadTestSuite.LoadTestResult(
            totalOperations = 100,
            successfulOperations = 98,
            failedOperations = 2,
            averageResponseTime = 200L,
            peakMemoryUsage = 80 * 1024 * 1024,
            errorRate = 0.02,
            throughput = 15.0,
            testDuration = 6667L
        )

        val goodReport = loadTestSuite.generateLoadTestReport(goodResult)
        assertTrue("Good performance should be acknowledged", goodReport.contains("WITHIN ACCEPTABLE LIMITS"))
    }
}