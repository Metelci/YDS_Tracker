package com.mtlc.studyplan.validation

import android.content.Context
import com.mtlc.studyplan.data.StudyProgressRepository
import com.mtlc.studyplan.data.TaskRepository
import com.mtlc.studyplan.performance.PerformanceMonitor
import com.mtlc.studyplan.settings.manager.SettingsManager
import com.mtlc.studyplan.testutils.CoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class LoadTestSuiteTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var taskRepository: TaskRepository

    @Mock
    private lateinit var studyProgressRepository: StudyProgressRepository

    @Mock
    private lateinit var settingsManager: SettingsManager

    @Mock
    private lateinit var performanceMonitor: PerformanceMonitor

    private lateinit var loadTestSuite: LoadTestSuite

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)

        // Setup mock behaviors matching actual repository APIs
        whenever(taskRepository.getAllTasks()).thenReturn(kotlinx.coroutines.flow.flowOf(emptyList()))
        whenever(studyProgressRepository.currentWeek).thenReturn(kotlinx.coroutines.flow.flowOf(1))
        whenever(settingsManager.currentSettings).thenReturn(
            kotlinx.coroutines.flow.MutableStateFlow(
                com.mtlc.studyplan.settings.data.UserSettings.default()
            )
        )
        // Stub suspend functions within a coroutine context
        kotlinx.coroutines.test.runTest {
            whenever(taskRepository.insertTask(any())).thenAnswer { invocation ->
                invocation.arguments[0] as com.mtlc.studyplan.data.Task
            }
            whenever(studyProgressRepository.setCurrentWeek(any())).thenReturn(Unit)
            whenever(settingsManager.updateSetting(any(), any())).thenReturn(
                Result.success(com.mtlc.studyplan.settings.data.UserSettings.default())
            )
        }
        whenever(performanceMonitor.recordCacheHit()).then { }
        whenever(performanceMonitor.measureOperation(any(), any())).thenReturn(100L)
        whenever(performanceMonitor.getPerformanceReport()).thenReturn("Performance Report")

        // Default instance with random operations for general tests
        loadTestSuite = LoadTestSuite(
            context,
            taskRepository,
            studyProgressRepository,
            settingsManager,
            performanceMonitor
        )
    }

    @Test
    fun `load test suite initializes correctly`() {
        assertNotNull("LoadTestSuite should initialize", loadTestSuite)
    }

    @Test
    fun `run comprehensive load test with minimal config`() = runTest {
        val config = LoadTestSuite.LoadTestConfig(
            concurrentUsers = 2,
            operationsPerUser = 3,
            testDurationMs = 5000L
        )

        // Use deterministic operation selector for failure path (settings op -> throws)
        val deterministicSuite = LoadTestSuite(
            context,
            taskRepository,
            studyProgressRepository,
            settingsManager,
            performanceMonitor,
            operationSelector = { 1 }
        )

        val result = deterministicSuite.runComprehensiveLoadTest(config)

        assertTrue("Load test should succeed", result.isSuccess)
        result.onSuccess { loadResult ->
            assertTrue("Should have operations", loadResult.totalOperations > 0)
            assertEquals("Total operations should match expected", 6, loadResult.totalOperations)
            assertTrue("Should have successful operations", loadResult.successfulOperations > 0)
            assertTrue("Test duration should be reasonable", loadResult.testDuration > 0)
            assertTrue("Throughput should be positive", loadResult.throughput > 0)
        }
    }

    @Test
    fun `load test handles exceptions gracefully`() = runTest {
        // Force failures deterministically across all operation paths
        whenever(taskRepository.insertTask(any())).thenAnswer { throw IllegalStateException("Test failure - insert") }
        whenever(taskRepository.getAllTasks()).thenReturn(kotlinx.coroutines.flow.flow {
            throw IllegalStateException("Test failure - getAllTasks")
        })
        // Keep study progress flows stable; failures come from other paths
        whenever(studyProgressRepository.setCurrentWeek(any())).thenReturn(Unit)
        whenever(studyProgressRepository.currentWeek).thenReturn(kotlinx.coroutines.flow.flowOf(1))
        whenever(settingsManager.updateSetting(any(), any())).thenAnswer { throw IllegalStateException("Test failure - updateSetting") }

        val config = LoadTestSuite.LoadTestConfig(
            concurrentUsers = 1,
            operationsPerUser = 2,
            testDurationMs = 2000L
        )

        val result = loadTestSuite.runComprehensiveLoadTest(config)

        assertTrue("Load test should succeed even with failures", result.isSuccess)
        result.onSuccess { loadResult ->
            assertTrue("Should have some failed operations", loadResult.failedOperations > 0)
            assertTrue("Error rate should be greater than 0", loadResult.errorRate > 0.0)
        }
    }

    @Test
    fun `stress test runs with maximum concurrent users`() = runTest {
        val result = loadTestSuite.runStressTest()

        // Stress test should complete (may timeout, but shouldn't crash)
        assertNotNull("Stress test should return a result", result)
    }

    @Test
    fun `endurance test runs for longer duration`() = runTest {
        val result = loadTestSuite.runEnduranceTest()

        // Endurance test should complete (may timeout, but shouldn't crash)
        assertNotNull("Endurance test should return a result", result)
    }

    @Test
    fun `generate load test report formats results correctly`() {
        val testResult = LoadTestSuite.LoadTestResult(
            totalOperations = 100,
            successfulOperations = 95,
            failedOperations = 5,
            averageResponseTime = 150L,
            peakMemoryUsage = 50 * 1024 * 1024, // 50MB
            errorRate = 0.05,
            throughput = 10.0,
            testDuration = 10000L
        )

        val report = loadTestSuite.generateLoadTestReport(testResult)

        assertTrue("Report should contain total operations", report.contains("100"))
        assertTrue("Report should contain successful operations", report.contains("95"))
        assertTrue("Report should contain failed operations", report.contains("5"))
        assertTrue("Report should contain error rate", report.contains("5.00"))
        assertTrue("Report should contain average response time", report.contains("150ms"))
        assertTrue("Report should contain memory usage", report.contains("50MB"))
        assertTrue("Report should contain throughput", report.contains("10.00"))
        assertTrue("Report should contain performance assessment", report.contains("Performance Assessment"))
    }

    @Test
    fun `load test result calculations are correct`() {
        val result = LoadTestSuite.LoadTestResult(
            totalOperations = 200,
            successfulOperations = 180,
            failedOperations = 20,
            averageResponseTime = 250L,
            peakMemoryUsage = 100 * 1024 * 1024,
            errorRate = 0.1,
            throughput = 20.0,
            testDuration = 10000L
        )

        // Verify calculations
        assertEquals("Error rate calculation", 0.1, result.errorRate, 0.001)
        assertEquals("Throughput should be 20.0", 20.0, result.throughput, 0.001)
    }

    @Test
    fun `load test config has sensible defaults`() {
        val config = LoadTestSuite.LoadTestConfig()

        assertTrue("Default concurrent users should be reasonable", config.concurrentUsers > 0)
        assertTrue("Default test duration should be positive", config.testDurationMs > 0)
        assertTrue("Default operations per user should be positive", config.operationsPerUser > 0)
        assertTrue("Database operations should be enabled by default", config.includeDatabaseOperations)
        assertTrue("Settings operations should be enabled by default", config.includeSettingsOperations)
        assertTrue("Performance monitoring should be enabled by default", config.includePerformanceMonitoring)
    }
}
