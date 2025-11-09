package com.mtlc.studyplan.analytics

import app.cash.turbine.test
import com.mtlc.studyplan.testutils.CoroutineTestRule
import com.mtlc.studyplan.data.StudyProgressRepository
import kotlinx.coroutines.flow.flowOf
import com.mtlc.studyplan.repository.TaskRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsViewModelTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Mock
    private lateinit var analyticsEngine: AnalyticsEngine

    @Mock
    private lateinit var taskRepository: TaskRepository

    @Mock
    private lateinit var studyProgressRepository: StudyProgressRepository
    private lateinit var viewModel: AnalyticsViewModel

    // Test data
    private val testAnalyticsData = AnalyticsData(
        averageSessionMinutes = 45,
        totalStudyMinutes = 7200
    )

    private val testWeeklyData = listOf(
        WeeklyAnalyticsData(
            weekNumber = 1,
            averageAccuracy = 0.85f,
            hoursStudied = 15.0f,
            tasksCompleted = 25,
            productivityScore = 0.82f
        ),
        WeeklyAnalyticsData(
            weekNumber = 2,
            averageAccuracy = 0.88f,
            hoursStudied = 18.0f,
            tasksCompleted = 30,
            productivityScore = 0.85f
        )
    )

    private val testPerformanceData = PerformanceData(
        averageAccuracy = 0.87f,
        averageSpeed = 1.2f,
        consistencyScore = 0.92f,
        weakAreas = emptyList(),
        totalMinutes = 120,
        taskCount = 50
    )

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)

        // Setup default mock behaviors for suspend functions
        runBlocking {
            whenever(analyticsEngine.generateAnalytics(any(), any(), anyOrNull())).thenReturn(testAnalyticsData)
            whenever(analyticsEngine.getWeeklyData(any(), any())).thenReturn(testWeeklyData)
            whenever(analyticsEngine.getPerformanceData(any())).thenReturn(testPerformanceData)
        }
        // Setup repository mocks
        whenever(taskRepository.completedTasks).thenReturn(flowOf(emptyList()))
        whenever(studyProgressRepository.currentWeek).thenReturn(flowOf(1))
    }

    // Initial State Tests
    @Test
    fun `init loads analytics with LAST_30_DAYS timeframe`() = runTest {
        viewModel = AnalyticsViewModel(analyticsEngine, taskRepository, studyProgressRepository)
        advanceUntilIdle()

        verify(analyticsEngine).generateAnalytics(30)
        verify(analyticsEngine).getWeeklyData(30)
        verify(analyticsEngine).getPerformanceData(30)
    }

    @Test
    fun `initial state has correct defaults`() = runTest {
        viewModel = AnalyticsViewModel(analyticsEngine, taskRepository, studyProgressRepository)
        advanceUntilIdle()

        assertEquals(AnalyticsTab.OVERVIEW, viewModel.selectedTab.value)
        assertFalse(viewModel.isLoading.value)
        assertNotNull(viewModel.analyticsData.value)
    }

    @Test
    fun `initial load sets all data correctly`() = runTest {
        viewModel = AnalyticsViewModel(analyticsEngine, taskRepository, studyProgressRepository)
        advanceUntilIdle()

        assertEquals(testAnalyticsData, viewModel.analyticsData.value)
        assertEquals(testWeeklyData, viewModel.weeklyData.value)
        assertEquals(testPerformanceData, viewModel.performanceData.value)
    }

    // Load Analytics Tests
    @Test
    fun `loadAnalytics sets loading state correctly`() = runTest {
        viewModel = AnalyticsViewModel(analyticsEngine, taskRepository, studyProgressRepository)
        advanceUntilIdle()

        // Initial state should be false after init completes
        assertFalse(viewModel.isLoading.value)

        viewModel.loadAnalytics(AnalyticsTimeframe.LAST_7_DAYS)
        advanceUntilIdle()

        // After loading completes, should be false again
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `loadAnalytics with LAST_7_DAYS calls engine with correct days`() = runTest {
        viewModel = AnalyticsViewModel(analyticsEngine, taskRepository, studyProgressRepository)
        advanceUntilIdle()

        clearInvocations(analyticsEngine)

        viewModel.loadAnalytics(AnalyticsTimeframe.LAST_7_DAYS)
        advanceUntilIdle()

        verify(analyticsEngine).generateAnalytics(7)
        verify(analyticsEngine).getWeeklyData(7)
        verify(analyticsEngine).getPerformanceData(7)
    }

    @Test
    fun `loadAnalytics with LAST_90_DAYS calls engine with correct days`() = runTest {
        viewModel = AnalyticsViewModel(analyticsEngine, taskRepository, studyProgressRepository)
        advanceUntilIdle()

        clearInvocations(analyticsEngine)

        viewModel.loadAnalytics(AnalyticsTimeframe.LAST_90_DAYS)
        advanceUntilIdle()

        verify(analyticsEngine).generateAnalytics(90)
        verify(analyticsEngine).getWeeklyData(90)
        verify(analyticsEngine).getPerformanceData(90)
    }

    @Test
    fun `loadAnalytics with ALL_TIME calls engine with correct days`() = runTest {
        viewModel = AnalyticsViewModel(analyticsEngine, taskRepository, studyProgressRepository)
        advanceUntilIdle()

        clearInvocations(analyticsEngine)

        viewModel.loadAnalytics(AnalyticsTimeframe.ALL_TIME)
        advanceUntilIdle()

        verify(analyticsEngine).generateAnalytics(Int.MAX_VALUE)
        verify(analyticsEngine).getWeeklyData(Int.MAX_VALUE)
        verify(analyticsEngine).getPerformanceData(Int.MAX_VALUE)
    }

    @Test
    fun `loadAnalytics updates all state flows`() = runTest {
        viewModel = AnalyticsViewModel(analyticsEngine, taskRepository, studyProgressRepository)
        advanceUntilIdle()

        val newAnalyticsData = AnalyticsData(averageSessionMinutes = 60, totalStudyMinutes = 9000)
        val newWeeklyData = listOf(
            WeeklyAnalyticsData(
                weekNumber = 3,
                averageAccuracy = 0.9f,
                hoursStudied = 20.0f,
                tasksCompleted = 35,
                productivityScore = 0.88f
            )
        )
        val newPerformanceData = PerformanceData(
            averageAccuracy = 0.95f,
            averageSpeed = 1.5f,
            consistencyScore = 0.98f,
            weakAreas = emptyList(),
            totalMinutes = 180,
            taskCount = 75
        )

        whenever(analyticsEngine.generateAnalytics(eq(7), any(), anyOrNull()))
            .thenReturn(newAnalyticsData)
        whenever(analyticsEngine.getWeeklyData(eq(7), any())).thenReturn(newWeeklyData)
        whenever(analyticsEngine.getPerformanceData(7)).thenReturn(newPerformanceData)

        viewModel.loadAnalytics(AnalyticsTimeframe.LAST_7_DAYS)
        advanceUntilIdle()

        assertEquals(newAnalyticsData, viewModel.analyticsData.value)
        assertEquals(newWeeklyData, viewModel.weeklyData.value)
        assertEquals(newPerformanceData, viewModel.performanceData.value)
    }

    @Test
    fun `loadAnalytics handles exceptions gracefully`() = runTest {
        viewModel = AnalyticsViewModel(analyticsEngine, taskRepository, studyProgressRepository)
        advanceUntilIdle()

        whenever(analyticsEngine.generateAnalytics(any(), any(), anyOrNull())).thenThrow(RuntimeException("Network error"))

        viewModel.loadAnalytics(AnalyticsTimeframe.LAST_7_DAYS)
        advanceUntilIdle()

        // Should complete without crashing and set loading to false
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `loadAnalytics sets loading to false even on exception`() = runTest {
        viewModel = AnalyticsViewModel(analyticsEngine, taskRepository, studyProgressRepository)
        advanceUntilIdle()

        whenever(analyticsEngine.generateAnalytics(any(), any(), anyOrNull())).thenThrow(RuntimeException("Error"))

        viewModel.loadAnalytics(AnalyticsTimeframe.LAST_7_DAYS)
        advanceUntilIdle()

        assertFalse(viewModel.isLoading.value)
    }

    // Select Tab Tests
    @Test
    fun `selectTab updates selected tab`() = runTest {
        viewModel = AnalyticsViewModel(analyticsEngine, taskRepository, studyProgressRepository)
        advanceUntilIdle()

        viewModel.selectTab(AnalyticsTab.PATTERNS)

        assertEquals(AnalyticsTab.PATTERNS, viewModel.selectedTab.value)
    }

    @Test
    fun `selectTab with PERFORMANCE tab works correctly`() = runTest {
        viewModel = AnalyticsViewModel(analyticsEngine, taskRepository, studyProgressRepository)
        advanceUntilIdle()

        viewModel.selectTab(AnalyticsTab.PERFORMANCE)

        assertEquals(AnalyticsTab.PERFORMANCE, viewModel.selectedTab.value)
    }

    @Test
    fun `selectTab emits new values to StateFlow`() = runTest {
        viewModel = AnalyticsViewModel(analyticsEngine, taskRepository, studyProgressRepository)
        advanceUntilIdle()

        viewModel.selectedTab.test {
            assertEquals(AnalyticsTab.OVERVIEW, awaitItem())

            viewModel.selectTab(AnalyticsTab.PATTERNS)
            assertEquals(AnalyticsTab.PATTERNS, awaitItem())

            viewModel.selectTab(AnalyticsTab.PERFORMANCE)
            assertEquals(AnalyticsTab.PERFORMANCE, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    // Refresh Analytics Tests
    @Test
    fun `refreshAnalytics with 0 weeks calls LAST_7_DAYS`() = runTest {
        whenever(analyticsEngine.getWeeklyData(any(), any())).thenReturn(emptyList())

        viewModel = AnalyticsViewModel(analyticsEngine, taskRepository, studyProgressRepository)
        advanceUntilIdle()

        clearInvocations(analyticsEngine)

        viewModel.refreshAnalytics()
        advanceUntilIdle()

        verify(analyticsEngine).generateAnalytics(7)
    }

    @Test
    fun `refreshAnalytics with 1 week calls LAST_7_DAYS`() = runTest {
        val oneWeekData = listOf(
            WeeklyAnalyticsData(
                weekNumber = 1,
                averageAccuracy = 0.85f,
                hoursStudied = 15.0f,
                tasksCompleted = 25,
                productivityScore = 0.82f
            )
        )
        whenever(analyticsEngine.getWeeklyData(any(), any())).thenReturn(oneWeekData)

        viewModel = AnalyticsViewModel(analyticsEngine, taskRepository, studyProgressRepository)
        advanceUntilIdle()

        clearInvocations(analyticsEngine)

        viewModel.refreshAnalytics()
        advanceUntilIdle()

        verify(analyticsEngine).generateAnalytics(7)
    }

    @Test
    fun `refreshAnalytics with 3 weeks calls LAST_30_DAYS`() = runTest {
        val threeWeeksData = (1..3).map {
            WeeklyAnalyticsData(
                weekNumber = it,
                averageAccuracy = 0.85f,
                hoursStudied = 15.0f,
                tasksCompleted = 25,
                productivityScore = 0.82f
            )
        }
        whenever(analyticsEngine.getWeeklyData(any(), any())).thenReturn(threeWeeksData)

        viewModel = AnalyticsViewModel(analyticsEngine, taskRepository, studyProgressRepository)
        advanceUntilIdle()

        clearInvocations(analyticsEngine)

        viewModel.refreshAnalytics()
        advanceUntilIdle()

        verify(analyticsEngine).generateAnalytics(30)
    }

    @Test
    fun `refreshAnalytics with 8 weeks calls LAST_90_DAYS`() = runTest {
        val eightWeeksData = (1..8).map {
            WeeklyAnalyticsData(
                weekNumber = it,
                averageAccuracy = 0.85f,
                hoursStudied = 15.0f,
                tasksCompleted = 25,
                productivityScore = 0.82f
            )
        }
        whenever(analyticsEngine.getWeeklyData(any(), any())).thenReturn(eightWeeksData)

        viewModel = AnalyticsViewModel(analyticsEngine, taskRepository, studyProgressRepository)
        advanceUntilIdle()

        clearInvocations(analyticsEngine)

        viewModel.refreshAnalytics()
        advanceUntilIdle()

        verify(analyticsEngine).generateAnalytics(90)
    }

    @Test
    fun `refreshAnalytics with 15 weeks calls ALL_TIME`() = runTest {
        val fifteenWeeksData = (1..15).map {
            WeeklyAnalyticsData(
                weekNumber = it,
                averageAccuracy = 0.85f,
                hoursStudied = 15.0f,
                tasksCompleted = 25,
                productivityScore = 0.82f
            )
        }
        whenever(analyticsEngine.getWeeklyData(any(), any())).thenReturn(fifteenWeeksData)

        viewModel = AnalyticsViewModel(analyticsEngine, taskRepository, studyProgressRepository)
        advanceUntilIdle()

        clearInvocations(analyticsEngine)

        viewModel.refreshAnalytics()
        advanceUntilIdle()

        verify(analyticsEngine).generateAnalytics(Int.MAX_VALUE)
    }

    // Integration Tests
    @Test
    fun `complete workflow - load, select tab, refresh`() = runTest {
        viewModel = AnalyticsViewModel(analyticsEngine, taskRepository, studyProgressRepository)
        advanceUntilIdle()

        // Initial load with LAST_30_DAYS
        assertEquals(AnalyticsTab.OVERVIEW, viewModel.selectedTab.value)
        assertEquals(testWeeklyData, viewModel.weeklyData.value)

        // Select different tab
        viewModel.selectTab(AnalyticsTab.PERFORMANCE)
        assertEquals(AnalyticsTab.PERFORMANCE, viewModel.selectedTab.value)

        // Load different timeframe
        clearInvocations(analyticsEngine)
        viewModel.loadAnalytics(AnalyticsTimeframe.LAST_7_DAYS)
        advanceUntilIdle()

        verify(analyticsEngine).generateAnalytics(7)

        // Refresh
        clearInvocations(analyticsEngine)
        viewModel.refreshAnalytics()
        advanceUntilIdle()

        // Should use LAST_30_DAYS based on 2 weeks of data
        verify(analyticsEngine).generateAnalytics(30)
    }
}
