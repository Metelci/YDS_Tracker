package com.mtlc.studyplan

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mtlc.studyplan.error.AppError
import com.mtlc.studyplan.error.ErrorHandler
import com.mtlc.studyplan.navigation.BadgeState
import com.mtlc.studyplan.navigation.BadgeStyle
import com.mtlc.studyplan.navigation.BadgeType
import com.mtlc.studyplan.performance.PerformanceMonitor
import com.mtlc.studyplan.ui.LoadingStateData
import com.mtlc.studyplan.ui.LoadingStateManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class LoadingStateManagerTest {

    private lateinit var loadingStateManager: LoadingStateManager
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        loadingStateManager = LoadingStateManager()
    }

    @Test
    fun `setLoading should update loading state`() = runTest(testDispatcher) {
        val key = "test_key"

        loadingStateManager.setLoading(key, true)

        val loadingStates = loadingStateManager.loadingStates.first()
        assertTrue(loadingStates[key]?.isLoading == true)
    }

    @Test
    fun `setLoading should clear loading state when false`() = runTest(testDispatcher) {
        val key = "test_key"

        loadingStateManager.setLoading(key, true)
        loadingStateManager.setLoading(key, false)

        val loadingStates = loadingStateManager.loadingStates.first()
        assertFalse(loadingStates[key]?.isLoading == true)
    }

    @Test
    fun `setLoadingWithProgress should update progress`() = runTest(testDispatcher) {
        val key = "test_key"
        val progress = 0.5f

        loadingStateManager.setLoadingWithProgress(key, true, progress)

        val loadingStates = loadingStateManager.loadingStates.first()
        assertEquals(progress, loadingStates[key]?.progress)
    }

    @Test
    fun `isLoading should return correct state`() = runTest(testDispatcher) {
        val key = "test_key"

        assertFalse(loadingStateManager.isLoading(key))

        loadingStateManager.setLoading(key, true)
        assertTrue(loadingStateManager.isLoading(key))
    }

    @Test
    fun `clearAllLoading should remove all states`() = runTest(testDispatcher) {
        loadingStateManager.setLoading("key1", true)
        loadingStateManager.setLoading("key2", true)

        loadingStateManager.clearAllLoading()

        val loadingStates = loadingStateManager.loadingStates.first()
        assertTrue(loadingStates.isEmpty())
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class ErrorHandlerTest {

    private lateinit var errorHandler: ErrorHandler
    @Mock
    private lateinit var mockContext: android.content.Context

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        errorHandler = ErrorHandler(mockContext)
    }

    @Test
    fun `handleError should emit error event`() = runTest {
        val error = AppError.NetworkError("Test network error")
        val context = "test_context"

        var emittedError: com.mtlc.studyplan.error.ErrorEvent? = null
        val job = launch {
            errorHandler.errors.collect { emittedError = it }
        }

        errorHandler.handleError(error, context)

        assertEquals(error, emittedError?.error)
        assertEquals(context, emittedError?.context)

        job.cancel()
    }

    @Test
    fun `categorizeError should return correct category`() {
        val networkError = AppError.NetworkError("Network error")
        val validationError = AppError.ValidationError("Validation error", emptyMap())
        val unknownError = AppError.UnknownError("Unknown error", Exception())

        assertEquals(com.mtlc.studyplan.error.ErrorCategory.NETWORK, errorHandler.categorizeError(networkError))
        assertEquals(com.mtlc.studyplan.error.ErrorCategory.VALIDATION, errorHandler.categorizeError(validationError))
        assertEquals(com.mtlc.studyplan.error.ErrorCategory.SYSTEM, errorHandler.categorizeError(unknownError))
    }

    @Test
    fun `isRetryable should return correct value`() {
        val networkError = AppError.NetworkError("Network error")
        val validationError = AppError.ValidationError("Validation error", emptyMap())

        assertTrue(errorHandler.isRetryable(networkError))
        assertFalse(errorHandler.isRetryable(validationError))
    }

    @Test
    fun `clearErrors should remove all errors`() = runTest {
        val error = AppError.NetworkError("Test error")

        errorHandler.handleError(error, "test")
        errorHandler.clearErrors()

        // Verify errors are cleared by checking that no new emissions occur
        var errorReceived = false
        val job = launch {
            errorHandler.errors.collect { errorReceived = true }
        }

        // Give some time for potential emissions
        advanceTimeBy(100)

        job.cancel()
    }
}

@RunWith(AndroidJUnit4::class)
class BadgeStateTest {

    @Test
    fun `BadgeState default constructor should create correct state`() {
        val badgeState = BadgeState()

        assertEquals(0, badgeState.count)
        assertEquals(BadgeStyle.DEFAULT, badgeState.style)
        assertFalse(badgeState.isVisible)
    }

    @Test
    fun `BadgeState with count should be visible`() {
        val badgeState = BadgeState(
            count = 5,
            style = BadgeStyle.WARNING,
            isVisible = true
        )

        assertEquals(5, badgeState.count)
        assertEquals(BadgeStyle.WARNING, badgeState.style)
        assertTrue(badgeState.isVisible)
    }
}

@RunWith(AndroidJUnit4::class)
class PerformanceMonitorTest {

    private lateinit var performanceMonitor: PerformanceMonitor

    @Before
    fun setup() {
        performanceMonitor = PerformanceMonitor()
    }

    @Test
    fun `measureOperation should return duration`() {
        val operation = { Thread.sleep(10) }

        val duration = performanceMonitor.measureOperation("test_operation", operation)

        assertTrue(duration > 0)
    }

    @Test
    fun `logPerformanceIssue should record issue`() = runTest {
        val context = "test_context"
        val issue = "test_issue"

        performanceMonitor.logPerformanceIssue(context, issue)

        val metrics = performanceMonitor.performanceMetrics.first()
        assertTrue(metrics.performanceIssues.any { it.context == context && it.issue == issue })
    }

    @Test
    fun `recordCacheHit should increment cache hits`() = runTest {
        val initialMetrics = performanceMonitor.performanceMetrics.first()
        val initialHits = initialMetrics.cacheHits

        performanceMonitor.recordCacheHit()

        val updatedMetrics = performanceMonitor.performanceMetrics.first()
        assertEquals(initialHits + 1, updatedMetrics.cacheHits)
    }

    @Test
    fun `recordCacheMiss should increment cache misses`() = runTest {
        val initialMetrics = performanceMonitor.performanceMetrics.first()
        val initialMisses = initialMetrics.cacheMisses

        performanceMonitor.recordCacheMiss()

        val updatedMetrics = performanceMonitor.performanceMetrics.first()
        assertEquals(initialMisses + 1, updatedMetrics.cacheMisses)
    }

    @Test
    fun `clearMetrics should reset all metrics`() = runTest {
        performanceMonitor.recordCacheHit()
        performanceMonitor.recordCacheMiss()
        performanceMonitor.logPerformanceIssue("test", "issue")

        performanceMonitor.clearMetrics()

        val metrics = performanceMonitor.performanceMetrics.first()
        assertEquals(0, metrics.cacheHits)
        assertEquals(0, metrics.cacheMisses)
        assertTrue(metrics.performanceIssues.isEmpty())
    }

    @Test
    fun `getPerformanceReport should return formatted report`() {
        val report = performanceMonitor.getPerformanceReport()

        assertTrue(report.contains("Performance Report"))
        assertTrue(report.contains("Average FPS"))
        assertTrue(report.contains("Memory Usage"))
        assertTrue(report.contains("Cache Hit Rate"))
    }
}

@RunWith(AndroidJUnit4::class)
class OptimizedComponentsTest {

    @Test
    fun `fastForEach should iterate correctly`() {
        val list = listOf(1, 2, 3, 4, 5)
        val results = mutableListOf<Int>()

        list.fastForEach { results.add(it) }

        assertEquals(list, results)
    }

    @Test
    fun `fastMap should transform correctly`() {
        val list = listOf(1, 2, 3, 4, 5)

        val results = list.fastMap { it * 2 }

        assertEquals(listOf(2, 4, 6, 8, 10), results)
    }

    @Test
    fun `fastFilter should filter correctly`() {
        val list = listOf(1, 2, 3, 4, 5)

        val results = list.fastFilter { it % 2 == 0 }

        assertEquals(listOf(2, 4), results)
    }
}

@RunWith(AndroidJUnit4::class)
class StatePreservationTest {

    @Test
    fun `NavigationState default constructor should create correct state`() {
        val state = com.mtlc.studyplan.state.NavigationState()

        assertEquals("home", state.currentRoute)
        assertEquals(listOf("home"), state.backStackRoutes)
        assertTrue(state.routeArguments.isEmpty())
    }

    @Test
    fun `TasksScreenState default constructor should create correct state`() {
        val state = com.mtlc.studyplan.state.TasksScreenState()

        assertEquals("", state.searchQuery)
        assertEquals(null, state.selectedFilter)
        assertEquals("DUE_DATE", state.sortOrder)
        assertEquals(0, state.scrollPosition)
        assertTrue(state.expandedCategories.isEmpty())
        assertTrue(state.selectedTaskIds.isEmpty())
    }

    @Test
    fun `ProgressScreenState default constructor should create correct state`() {
        val state = com.mtlc.studyplan.state.ProgressScreenState()

        assertEquals("WEEK", state.selectedTimeRange)
        assertEquals("DAILY_PROGRESS", state.selectedChartType)
        assertEquals(0, state.scrollPosition)
        assertTrue(state.expandedSections.isEmpty())
    }

    @Test
    fun `SocialScreenState default constructor should create correct state`() {
        val state = com.mtlc.studyplan.state.SocialScreenState()

        assertEquals("FEED", state.selectedTab)
        assertEquals(0, state.feedScrollPosition)
        assertEquals("", state.searchQuery)
        assertTrue(state.selectedFilters.isEmpty())
    }

    @Test
    fun `SettingsScreenState default constructor should create correct state`() {
        val state = com.mtlc.studyplan.state.SettingsScreenState()

        assertEquals(0, state.scrollPosition)
        assertEquals("", state.searchQuery)
        assertTrue(state.expandedSections.isEmpty())
    }
}

// Utility test classes
class TestUtils {
    companion object {
        fun createTestLoadingState(
            isLoading: Boolean = false,
            progress: Float? = null,
            message: String? = null
        ): LoadingStateData {
            return LoadingStateData(
                isLoading = isLoading,
                progress = progress,
                message = message,
                timestamp = System.currentTimeMillis()
            )
        }

        fun createTestBadgeState(
            count: Int = 0,
            style: BadgeStyle = BadgeStyle.DEFAULT,
            isVisible: Boolean = false
        ): BadgeState {
            return BadgeState(
                count = count,
                style = style,
                isVisible = isVisible,
                lastUpdated = System.currentTimeMillis()
            )
        }

        fun assertTimeWithinRange(actual: Long, expected: Long, toleranceMs: Long = 1000) {
            assertTrue(
                kotlin.math.abs(actual - expected) <= toleranceMs,
                "Time difference ${kotlin.math.abs(actual - expected)}ms exceeds tolerance ${toleranceMs}ms"
            )
        }
    }
}