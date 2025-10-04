package com.mtlc.studyplan.validation

import android.content.Context
import android.util.Log
import com.mtlc.studyplan.data.StudyProgressRepository
import com.mtlc.studyplan.data.Task
import com.mtlc.studyplan.data.TaskPriority
import com.mtlc.studyplan.data.TaskRepository
import com.mtlc.studyplan.performance.PerformanceMonitor
import com.mtlc.studyplan.settings.manager.SettingsManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Load testing suite for performance validation under high user load scenarios
 * Simulates multiple concurrent users performing various app operations
 */
@Singleton
class LoadTestSuite @Inject constructor(
    private val context: Context,
    private val taskRepository: TaskRepository,
    private val studyProgressRepository: StudyProgressRepository,
    private val settingsManager: SettingsManager,
    private val performanceMonitor: PerformanceMonitor,
    private val operationSelector: () -> Int = { (0..2).random() }
) {

    companion object {
        private const val TAG = "LoadTestSuite"
        private const val MAX_CONCURRENT_USERS = 50
        private const val TEST_DURATION_MS = 30000L // 30 seconds
        private const val PROGRESS_UPDATE_INTERVAL = 5000L // 5 seconds
    }

    /**
     * Data class to group user load simulation parameters
     */
    private data class UserLoadParams(
        val userId: Int,
        val config: LoadTestConfig,
        val operationCounter: AtomicInteger,
        val successCounter: AtomicInteger,
        val failureCounter: AtomicInteger,
        val responseTimes: MutableList<Long>
    )

    /**
     * Data class to group test execution parameters
     */
    private data class TestParameters(
        val semaphore: Semaphore,
        val operationCounter: AtomicInteger,
        val successCounter: AtomicInteger,
        val failureCounter: AtomicInteger,
        val responseTimes: MutableList<Long>,
        val peakMemoryUsage: AtomicLong
    )

    /**
     * Data class to group monitoring jobs
     */
    private data class MonitoringJobs(
        val progressJob: Job,
        val memoryJob: Job
    )

    private val testScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    data class LoadTestResult(
        val totalOperations: Int,
        val successfulOperations: Int,
        val failedOperations: Int,
        val averageResponseTime: Long,
        val peakMemoryUsage: Long,
        val errorRate: Double,
        val throughput: Double, // operations per second
        val testDuration: Long
    )

    data class LoadTestConfig(
        val concurrentUsers: Int = 10,
        val testDurationMs: Long = TEST_DURATION_MS,
        val operationsPerUser: Int = 20,
        val includeDatabaseOperations: Boolean = true,
        val includeSettingsOperations: Boolean = true,
        val includePerformanceMonitoring: Boolean = true
    )

    /**
     * Run comprehensive load test simulating multiple users
     */
    suspend fun runComprehensiveLoadTest(
        config: LoadTestConfig = LoadTestConfig()
    ): Result<LoadTestResult> = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Starting comprehensive load test with ${config.concurrentUsers} users")

            val startTime = System.currentTimeMillis()
            val testParameters = initializeTestParameters(config)
            val monitoringJobs = launchMonitoringJobs(startTime, testParameters)
            val userJobs = launchUserSimulations(config, testParameters)

            waitForTestCompletion(config, userJobs)
            cleanupMonitoringJobs(monitoringJobs)

            val result = calculateTestResults(testParameters, startTime)
            Log.i(TAG, "Load test completed: $result")
            Result.success(result)

        } catch (e: CancellationException) {
            Log.e(TAG, "Load test was cancelled", e)
            Result.failure(e)
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Invalid state during load test", e)
            Result.failure(e)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Invalid argument during load test", e)
            Result.failure(e)
        }
    }

    private fun initializeTestParameters(config: LoadTestConfig): TestParameters {
        return TestParameters(
            semaphore = Semaphore(config.concurrentUsers),
            operationCounter = AtomicInteger(0),
            successCounter = AtomicInteger(0),
            failureCounter = AtomicInteger(0),
            responseTimes = mutableListOf(),
            peakMemoryUsage = AtomicLong(0)
        )
    }

    private fun CoroutineScope.launchMonitoringJobs(
        startTime: Long,
        params: TestParameters
    ): MonitoringJobs {
        val progressJob = launch {
            while (isActive) {
                delay(PROGRESS_UPDATE_INTERVAL)
                val elapsed = System.currentTimeMillis() - startTime
                val completed = params.operationCounter.get()
                Log.i(TAG, "Load test progress: $completed operations in ${elapsed}ms")
            }
        }

        val memoryJob = launch {
            while (isActive) {
                delay(1000) // Check every second
                val currentMemory = Runtime.getRuntime().let { runtime ->
                    runtime.totalMemory() - runtime.freeMemory()
                }
                params.peakMemoryUsage.updateAndGet { maxOf(it, currentMemory) }
            }
        }

        return MonitoringJobs(progressJob, memoryJob)
    }

    private fun CoroutineScope.launchUserSimulations(
        config: LoadTestConfig,
        params: TestParameters
    ): List<Job> {
        return (1..config.concurrentUsers).map { userId ->
            launch {
                params.semaphore.withPermit {
                    val userParams = UserLoadParams(
                        userId = userId,
                        config = config,
                        operationCounter = params.operationCounter,
                        successCounter = params.successCounter,
                        failureCounter = params.failureCounter,
                        responseTimes = params.responseTimes
                    )
                    simulateUserLoad(userParams)
                }
            }
        }
    }

    private suspend fun waitForTestCompletion(config: LoadTestConfig, userJobs: List<Job>) {
        try {
            withTimeout(config.testDurationMs) {
                userJobs.forEach { it.join() }
            }
        } catch (e: TimeoutCancellationException) {
            // Timeout is expected in load testing - handle gracefully by cancelling remaining operations
            // This is intentional behavior for load testing scenarios, not an error to suppress
            Log.w(TAG, "Load test timed out after ${config.testDurationMs}ms, cancelling remaining operations")
            userJobs.forEach { it.cancel() }
            // Continue with result generation - timeout is a valid test outcome
        }
    }

    private fun cleanupMonitoringJobs(jobs: MonitoringJobs) {
        jobs.progressJob.cancel()
        jobs.memoryJob.cancel()
    }

    private fun calculateTestResults(params: TestParameters, startTime: Long): LoadTestResult {
        val endTime = System.currentTimeMillis()
        val testDuration = endTime - startTime

        return LoadTestResult(
            totalOperations = params.operationCounter.get(),
            successfulOperations = params.successCounter.get(),
            failedOperations = params.failureCounter.get(),
            averageResponseTime = if (params.responseTimes.isNotEmpty()) params.responseTimes.average().toLong() else 0L,
            peakMemoryUsage = params.peakMemoryUsage.get(),
            errorRate = if (params.operationCounter.get() > 0) params.failureCounter.get().toDouble() / params.operationCounter.get() else 0.0,
            throughput = params.operationCounter.get().toDouble() / (testDuration / 1000.0),
            testDuration = testDuration
        )
    }

    /**
     * Simulate a single user's load on the system
     */
    private suspend fun simulateUserLoad(params: UserLoadParams) {
        repeat(params.config.operationsPerUser) { operationIndex ->
            try {
                val operationStart = System.nanoTime()

                // Randomly select operation type
                when (operationSelector()) {
                    0 -> performDatabaseOperation(params.userId, operationIndex)
                    1 -> performSettingsOperation(params.userId, operationIndex)
                    2 -> performPerformanceOperation(params.userId, operationIndex)
                }

                val operationEnd = System.nanoTime()
                val responseTime = (operationEnd - operationStart) / 1_000_000 // Convert to milliseconds

                params.operationCounter.incrementAndGet()
                params.successCounter.incrementAndGet()
                synchronized(params.responseTimes) {
                    params.responseTimes.add(responseTime)
                }

                // Small delay between operations to simulate realistic user behavior
                delay((10..100).random().toLong())

            } catch (e: CancellationException) {
                Log.w(TAG, "User ${params.userId} operation $operationIndex was cancelled", e)
                params.operationCounter.incrementAndGet()
                params.failureCounter.incrementAndGet()
            } catch (e: IllegalStateException) {
                Log.w(TAG, "Invalid state for user ${params.userId} operation $operationIndex", e)
                params.operationCounter.incrementAndGet()
                params.failureCounter.incrementAndGet()
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Invalid argument for user ${params.userId} operation $operationIndex", e)
                params.operationCounter.incrementAndGet()
                params.failureCounter.incrementAndGet()
            } catch (e: SecurityException) {
                Log.w(TAG, "Security violation for user ${params.userId} operation $operationIndex", e)
                params.operationCounter.incrementAndGet()
                params.failureCounter.incrementAndGet()
            }
        }
    }

    /**
     * Simulate database operations (CRUD on tasks and progress)
     */
    private suspend fun performDatabaseOperation(userId: Int, operationIndex: Int) {
        when ((0..3).random()) {
            0 -> { // Create task
                val taskData = mapOf(
                    "title" to "Load Test Task $userId-$operationIndex",
                    "description" to "Generated during load testing",
                    "category" to "testing",
                    "priority" to "medium",
                    "estimatedMinutes" to 30
                )
                taskRepository.insertTask(Task(
                    id = "load_test_${userId}_${operationIndex}",
                    title = taskData["title"] as String,
                    description = taskData["description"] as? String ?: "",
                    category = taskData["category"] as String,
                    priority = TaskPriority.MEDIUM,
                    estimatedMinutes = taskData["estimatedMinutes"] as Int,
                    createdAt = System.currentTimeMillis()
                ))
            }
            1 -> { // Read tasks
                taskRepository.getAllTasks().first()
            }
            2 -> { // Update progress
                studyProgressRepository.setCurrentWeek((operationIndex % 30) + 1)
            }
            3 -> { // Read progress
                studyProgressRepository.currentWeek.first()
            }
        }
    }

    /**
     * Simulate settings operations
     */
    private suspend fun performSettingsOperation(userId: Int, operationIndex: Int) {
        when ((0..2).random()) {
            0 -> { // Read settings
                settingsManager.currentSettings.first()
            }
            1 -> { // Update notification settings
                settingsManager.updateSetting(
                    com.mtlc.studyplan.settings.data.SettingsKey.NOTIFICATIONS_ENABLED,
                    (operationIndex % 2 == 0)
                )
            }
            2 -> { // Update theme
                settingsManager.updateSetting(
                    com.mtlc.studyplan.settings.data.SettingsKey.THEME_MODE,
                    if (operationIndex % 2 == 0) com.mtlc.studyplan.settings.data.ThemeMode.LIGHT
                    else com.mtlc.studyplan.settings.data.ThemeMode.DARK
                )
            }
        }
    }

    /**
     * Simulate performance monitoring operations
     */
    private suspend fun performPerformanceOperation(userId: Int, operationIndex: Int) {
        when ((0..2).random()) {
            0 -> { // Record cache operations
                performanceMonitor.recordCacheHit()
            }
            1 -> { // Measure operation
                performanceMonitor.measureOperation("load_test_operation_$userId") {
                    // Simulate some work
                    Thread.sleep((1..10).random().toLong())
                }
            }
            2 -> { // Get performance report
                performanceMonitor.getPerformanceReport()
            }
        }
    }

    /**
     * Run stress test with maximum concurrent users
     */
    suspend fun runStressTest(): Result<LoadTestResult> {
        return runComprehensiveLoadTest(
            LoadTestConfig(
                concurrentUsers = MAX_CONCURRENT_USERS,
                testDurationMs = TEST_DURATION_MS,
                operationsPerUser = 50
            )
        )
    }

    /**
     * Run endurance test for longer duration with moderate load
     */
    suspend fun runEnduranceTest(): Result<LoadTestResult> {
        return runComprehensiveLoadTest(
            LoadTestConfig(
                concurrentUsers = 20,
                testDurationMs = 120000L, // 2 minutes
                operationsPerUser = 100
            )
        )
    }

    /**
     * Generate load test report
     */
    fun generateLoadTestReport(result: LoadTestResult): String {
        return buildString {
            appendLine("=== Load Test Report ===")
            appendLine("Total Operations: ${result.totalOperations}")
            appendLine("Successful Operations: ${result.successfulOperations}")
            appendLine("Failed Operations: ${result.failedOperations}")
            appendLine("Error Rate: ${String.format(java.util.Locale.US, "%.2f", result.errorRate * 100)}%")
            appendLine("Average Response Time: ${result.averageResponseTime}ms")
            appendLine("Peak Memory Usage: ${result.peakMemoryUsage / (1024 * 1024)}MB")
            appendLine("Throughput: ${String.format(java.util.Locale.US, "%.2f", result.throughput)} ops/sec")
            appendLine("Test Duration: ${result.testDuration}ms")
            appendLine("========================")

            // Performance assessment
            appendLine("\nPerformance Assessment:")
            when {
                result.errorRate > 0.1 -> appendLine("❌ HIGH ERROR RATE - Requires investigation")
                result.averageResponseTime > 1000 -> appendLine("⚠️ SLOW RESPONSE TIMES - Consider optimization")
                result.throughput < 10 -> appendLine("⚠️ LOW THROUGHPUT - May impact user experience")
                else -> appendLine("✅ PERFORMANCE WITHIN ACCEPTABLE LIMITS")
            }
        }
    }
}
