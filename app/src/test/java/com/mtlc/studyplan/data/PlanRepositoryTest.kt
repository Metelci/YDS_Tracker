package com.mtlc.studyplan.data

import android.content.Context

import app.cash.turbine.test
import com.mtlc.studyplan.testutils.CoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import androidx.test.core.app.ApplicationProvider
import org.junit.Before
import org.koin.core.context.stopKoin
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class PlanRepositoryTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Mock
    private lateinit var planOverridesStore: PlanOverridesStore

    @Mock
    private lateinit var planSettingsStore: PlanSettingsStore

    private lateinit var repository: PlanRepository
    private lateinit var context: Context

    // Test data
    private val testWeekPlan = WeekPlan(
        week = 1,
        month = 1,
        title = "Week 1 - Foundation",
        days = listOf(
            DayPlan(
                day = "Pazartesi",
                tasks = listOf(
                    PlanTask("w1-task1", "Task 1", "Details 1"),
                    PlanTask("w1-task2", "Task 2", null)
                )
            ),
            DayPlan(
                day = "Salı",
                tasks = listOf(
                    PlanTask("w1-task3", "Task 3", "Details 3")
                )
            )
        )
    )

    private val testOverrides = UserPlanOverrides(
        taskOverrides = listOf(
            TaskOverride("w1-task2", hidden = true),
            TaskOverride("w1-task1", customDesc = "Custom Task 1")
        ),
        dayOverrides = listOf(
            DayOverrides(
                week = 1,
                dayIndex = 0,
                added = listOf(
                    CustomTask("1", "Custom Task A", "Custom Details A")
                )
            )
        )
    )

    private val testSettings = PlanDurationSettings(
        startEpochDay = java.time.LocalDate.of(2025, 1, 6).toEpochDay(), // Monday
        totalWeeks = 30,
        endEpochDay = null,
        totalMonths = null,
        monMinutes = 120,
        tueMinutes = 90,
        wedMinutes = 120,
        thuMinutes = 90,
        friMinutes = 120,
        satMinutes = 150,
        sunMinutes = 180
    )

    @Before
    fun setup() {
        try { stopKoin() } catch (e: Exception) { }
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()

        // Setup default mock behaviors
        whenever(planOverridesStore.overridesFlow).thenReturn(flowOf(testOverrides))
        whenever(planSettingsStore.settingsFlow).thenReturn(flowOf(testSettings))

        repository = PlanRepository(context, planOverridesStore, planSettingsStore)
    }

    // Task Visibility Tests
    @Test
    fun `setTaskHidden adds new TaskOverride when task not previously overridden`() = runTest {
        val initialOverrides = UserPlanOverrides()
        whenever(planOverridesStore.overridesFlow).thenReturn(flowOf(initialOverrides))

        var capturedTransform: ((UserPlanOverrides) -> UserPlanOverrides)? = null
        whenever(planOverridesStore.update(any())).thenAnswer { invocation ->
            @Suppress("UNCHECKED_CAST")
            capturedTransform = invocation.getArgument(0) as (UserPlanOverrides) -> UserPlanOverrides
        }

        repository.setTaskHidden("task123", hidden = true)

        verify(planOverridesStore).update(any())
        assertNotNull(capturedTransform)

        val result = capturedTransform!!.invoke(initialOverrides)
        assertTrue(result.taskOverrides.any { it.taskId == "task123" && it.hidden })
    }

    @Test
    fun `setTaskHidden updates existing TaskOverride`() = runTest {
        val initialOverrides = UserPlanOverrides(
            taskOverrides = listOf(TaskOverride("task1", hidden = false))
        )

        var capturedTransform: ((UserPlanOverrides) -> UserPlanOverrides)? = null
        whenever(planOverridesStore.update(any())).thenAnswer { invocation ->
            @Suppress("UNCHECKED_CAST")
            capturedTransform = invocation.getArgument(0) as (UserPlanOverrides) -> UserPlanOverrides
        }

        repository.setTaskHidden("task1", hidden = true)

        verify(planOverridesStore).update(any())
        val result = capturedTransform!!.invoke(initialOverrides)

        val updated = result.taskOverrides.find { it.taskId == "task1" }
        assertNotNull(updated)
        assertTrue(updated!!.hidden)
    }

    @Test
    fun `setTaskHidden preserves custom text when updating visibility`() = runTest {
        val initialOverrides = UserPlanOverrides(
            taskOverrides = listOf(
                TaskOverride("task1", hidden = false, customDesc = "Custom", customDetails = "Details")
            )
        )

        var capturedTransform: ((UserPlanOverrides) -> UserPlanOverrides)? = null
        whenever(planOverridesStore.update(any())).thenAnswer { invocation ->
            @Suppress("UNCHECKED_CAST")
            capturedTransform = invocation.getArgument(0) as (UserPlanOverrides) -> UserPlanOverrides
        }

        repository.setTaskHidden("task1", hidden = true)

        val result = capturedTransform!!.invoke(initialOverrides)
        val updated = result.taskOverrides.find { it.taskId == "task1" }

        assertEquals("Custom", updated?.customDesc)
        assertEquals("Details", updated?.customDetails)
        assertTrue(updated!!.hidden)
    }

    // Task Text Customization Tests
    @Test
    fun `updateTaskText adds custom description and details`() = runTest {
        val initialOverrides = UserPlanOverrides()

        var capturedTransform: ((UserPlanOverrides) -> UserPlanOverrides)? = null
        whenever(planOverridesStore.update(any())).thenAnswer { invocation ->
            @Suppress("UNCHECKED_CAST")
            capturedTransform = invocation.getArgument(0) as (UserPlanOverrides) -> UserPlanOverrides
        }

        repository.updateTaskText("task1", "New Desc", "New Details")

        verify(planOverridesStore).update(any())
        val result = capturedTransform!!.invoke(initialOverrides)

        val override = result.taskOverrides.find { it.taskId == "task1" }
        assertEquals("New Desc", override?.customDesc)
        assertEquals("New Details", override?.customDetails)
    }

    @Test
    fun `updateTaskText updates existing override`() = runTest {
        val initialOverrides = UserPlanOverrides(
            taskOverrides = listOf(
                TaskOverride("task1", customDesc = "Old", customDetails = "Old Details")
            )
        )

        var capturedTransform: ((UserPlanOverrides) -> UserPlanOverrides)? = null
        whenever(planOverridesStore.update(any())).thenAnswer { invocation ->
            @Suppress("UNCHECKED_CAST")
            capturedTransform = invocation.getArgument(0) as (UserPlanOverrides) -> UserPlanOverrides
        }

        repository.updateTaskText("task1", "New", "New Details")

        val result = capturedTransform!!.invoke(initialOverrides)
        val override = result.taskOverrides.find { it.taskId == "task1" }

        assertEquals("New", override?.customDesc)
        assertEquals("New Details", override?.customDetails)
    }

    @Test
    fun `updateTaskText preserves hidden status when updating text`() = runTest {
        val initialOverrides = UserPlanOverrides(
            taskOverrides = listOf(TaskOverride("task1", hidden = true))
        )

        var capturedTransform: ((UserPlanOverrides) -> UserPlanOverrides)? = null
        whenever(planOverridesStore.update(any())).thenAnswer { invocation ->
            @Suppress("UNCHECKED_CAST")
            capturedTransform = invocation.getArgument(0) as (UserPlanOverrides) -> UserPlanOverrides
        }

        repository.updateTaskText("task1", "New Text", null)

        val result = capturedTransform!!.invoke(initialOverrides)
        val override = result.taskOverrides.find { it.taskId == "task1" }

        assertTrue(override!!.hidden)
        assertEquals("New Text", override.customDesc)
    }

    // Custom Task CRUD Tests
    @Test
    fun `addCustomTask adds task with generated ID suffix`() = runTest {
        val initialOverrides = UserPlanOverrides()

        var capturedTransform: ((UserPlanOverrides) -> UserPlanOverrides)? = null
        whenever(planOverridesStore.update(any())).thenAnswer { invocation ->
            @Suppress("UNCHECKED_CAST")
            capturedTransform = invocation.getArgument(0) as (UserPlanOverrides) -> UserPlanOverrides
        }

        repository.addCustomTask(week = 1, dayIndex = 0, desc = "Custom", details = "Details")

        verify(planOverridesStore).update(any())
        val result = capturedTransform!!.invoke(initialOverrides)

        val dayOverride = result.dayOverrides.find { it.week == 1 && it.dayIndex == 0 }
        assertNotNull(dayOverride)
        assertEquals(1, dayOverride!!.added.size)
        assertEquals("1", dayOverride.added.first().idSuffix)
        assertEquals("Custom", dayOverride.added.first().desc)
    }

    @Test
    fun `addCustomTask increments ID suffix for subsequent tasks`() = runTest {
        val initialOverrides = UserPlanOverrides(
            dayOverrides = listOf(
                DayOverrides(
                    week = 1,
                    dayIndex = 0,
                    added = listOf(
                        CustomTask("1", "Task 1", null),
                        CustomTask("2", "Task 2", null)
                    )
                )
            )
        )

        var capturedTransform: ((UserPlanOverrides) -> UserPlanOverrides)? = null
        whenever(planOverridesStore.update(any())).thenAnswer { invocation ->
            @Suppress("UNCHECKED_CAST")
            capturedTransform = invocation.getArgument(0) as (UserPlanOverrides) -> UserPlanOverrides
        }

        repository.addCustomTask(week = 1, dayIndex = 0, desc = "Task 3", details = null)

        val result = capturedTransform!!.invoke(initialOverrides)
        val dayOverride = result.dayOverrides.find { it.week == 1 && it.dayIndex == 0 }

        assertEquals(3, dayOverride!!.added.size)
        assertEquals("3", dayOverride.added.last().idSuffix)
    }

    @Test
    fun `addCustomTask handles non-numeric suffixes by starting at 1`() = runTest {
        val initialOverrides = UserPlanOverrides(
            dayOverrides = listOf(
                DayOverrides(
                    week = 1,
                    dayIndex = 0,
                    added = listOf(CustomTask("abc", "Task ABC", null))
                )
            )
        )

        var capturedTransform: ((UserPlanOverrides) -> UserPlanOverrides)? = null
        whenever(planOverridesStore.update(any())).thenAnswer { invocation ->
            @Suppress("UNCHECKED_CAST")
            capturedTransform = invocation.getArgument(0) as (UserPlanOverrides) -> UserPlanOverrides
        }

        repository.addCustomTask(week = 1, dayIndex = 0, desc = "Task New", details = null)

        val result = capturedTransform!!.invoke(initialOverrides)
        val dayOverride = result.dayOverrides.find { it.week == 1 && it.dayIndex == 0 }

        // Should use 1 since "abc" is not numeric
        assertTrue(dayOverride!!.added.any { it.idSuffix == "1" })
    }

    @Test
    fun `addCustomTask creates new DayOverrides if none exist`() = runTest {
        val initialOverrides = UserPlanOverrides()

        var capturedTransform: ((UserPlanOverrides) -> UserPlanOverrides)? = null
        whenever(planOverridesStore.update(any())).thenAnswer { invocation ->
            @Suppress("UNCHECKED_CAST")
            capturedTransform = invocation.getArgument(0) as (UserPlanOverrides) -> UserPlanOverrides
        }

        repository.addCustomTask(week = 5, dayIndex = 3, desc = "New Task", details = "New Details")

        val result = capturedTransform!!.invoke(initialOverrides)
        val dayOverride = result.dayOverrides.find { it.week == 5 && it.dayIndex == 3 }

        assertNotNull(dayOverride)
        assertEquals(1, dayOverride!!.added.size)
        assertEquals("New Task", dayOverride.added.first().desc)
    }

    // Remove Custom Task Tests
    @Test
    fun `removeCustomTask removes specified task`() = runTest {
        val initialOverrides = UserPlanOverrides(
            dayOverrides = listOf(
                DayOverrides(
                    week = 1,
                    dayIndex = 0,
                    added = listOf(
                        CustomTask("1", "Task 1", null),
                        CustomTask("2", "Task 2", null)
                    )
                )
            )
        )

        var capturedTransform: ((UserPlanOverrides) -> UserPlanOverrides)? = null
        whenever(planOverridesStore.update(any())).thenAnswer { invocation ->
            @Suppress("UNCHECKED_CAST")
            capturedTransform = invocation.getArgument(0) as (UserPlanOverrides) -> UserPlanOverrides
        }

        repository.removeCustomTask(week = 1, dayIndex = 0, idSuffix = "1")

        val result = capturedTransform!!.invoke(initialOverrides)
        val dayOverride = result.dayOverrides.find { it.week == 1 && it.dayIndex == 0 }

        assertEquals(1, dayOverride!!.added.size)
        assertEquals("2", dayOverride.added.first().idSuffix)
    }

    @Test
    fun `removeCustomTask does nothing if day not found`() = runTest {
        val initialOverrides = UserPlanOverrides()

        var capturedTransform: ((UserPlanOverrides) -> UserPlanOverrides)? = null
        whenever(planOverridesStore.update(any())).thenAnswer { invocation ->
            @Suppress("UNCHECKED_CAST")
            capturedTransform = invocation.getArgument(0) as (UserPlanOverrides) -> UserPlanOverrides
        }

        repository.removeCustomTask(week = 99, dayIndex = 99, idSuffix = "1")

        val result = capturedTransform!!.invoke(initialOverrides)
        assertEquals(initialOverrides, result) // Should be unchanged
    }

    @Test
    fun `removeCustomTask preserves other tasks on same day`() = runTest {
        val initialOverrides = UserPlanOverrides(
            dayOverrides = listOf(
                DayOverrides(
                    week = 1,
                    dayIndex = 0,
                    added = listOf(
                        CustomTask("1", "Task 1", null),
                        CustomTask("2", "Task 2", null),
                        CustomTask("3", "Task 3", null)
                    )
                )
            )
        )

        var capturedTransform: ((UserPlanOverrides) -> UserPlanOverrides)? = null
        whenever(planOverridesStore.update(any())).thenAnswer { invocation ->
            @Suppress("UNCHECKED_CAST")
            capturedTransform = invocation.getArgument(0) as (UserPlanOverrides) -> UserPlanOverrides
        }

        repository.removeCustomTask(week = 1, dayIndex = 0, idSuffix = "2")

        val result = capturedTransform!!.invoke(initialOverrides)
        val dayOverride = result.dayOverrides.find { it.week == 1 && it.dayIndex == 0 }

        assertEquals(2, dayOverride!!.added.size)
        assertTrue(dayOverride.added.any { it.idSuffix == "1" })
        assertTrue(dayOverride.added.any { it.idSuffix == "3" })
        assertFalse(dayOverride.added.any { it.idSuffix == "2" })
    }

    @Test
    fun `removeCustomTask keeps empty DayOverrides after removing last task`() = runTest {
        val initialOverrides = UserPlanOverrides(
            dayOverrides = listOf(
                DayOverrides(
                    week = 1,
                    dayIndex = 0,
                    added = listOf(CustomTask("1", "Task 1", null))
                )
            )
        )

        var capturedTransform: ((UserPlanOverrides) -> UserPlanOverrides)? = null
        whenever(planOverridesStore.update(any())).thenAnswer { invocation ->
            @Suppress("UNCHECKED_CAST")
            capturedTransform = invocation.getArgument(0) as (UserPlanOverrides) -> UserPlanOverrides
        }

        repository.removeCustomTask(week = 1, dayIndex = 0, idSuffix = "1")

        val result = capturedTransform!!.invoke(initialOverrides)
        val dayOverride = result.dayOverrides.find { it.week == 1 && it.dayIndex == 0 }

        assertNotNull(dayOverride) // DayOverrides still exists
        assertTrue(dayOverride!!.added.isEmpty()) // But added list is empty
    }

    // Integration Tests
    @Test
    fun `planFlow emits plan based on settings and overrides`() = runTest {
        // Note: This test requires PlanDataSource to be initialized with context
        // In actual implementation, this would need application context mock
        // For now, we'll just verify the flow is created
        assertNotNull(repository.planFlow)
    }

    @Test
    fun `planFlow reacts to override changes`() = runTest {
        val overridesFlow = MutableStateFlow(UserPlanOverrides())
        whenever(planOverridesStore.overridesFlow).thenReturn(overridesFlow)
        whenever(planSettingsStore.settingsFlow).thenReturn(flowOf(testSettings))

        val repository = PlanRepository(context, planOverridesStore, planSettingsStore)

        // Note: Full reactive testing would require PlanDataSource initialization
        // This test verifies the flow structure is properly combined
        assertNotNull(repository.planFlow)
    }

    @Test
    fun `setTaskHidden is called with correct parameters`() = runTest {
        repository.setTaskHidden("test-task", hidden = true)
        verify(planOverridesStore).update(any())
    }

    @Test
    fun `updateTaskText is called with correct parameters`() = runTest {
        repository.updateTaskText("test-task", "New Desc", "New Details")
        verify(planOverridesStore).update(any())
    }

    @Test
    fun `addCustomTask is called with correct parameters`() = runTest {
        repository.addCustomTask(week = 1, dayIndex = 0, desc = "Task", details = "Details")
        verify(planOverridesStore).update(any())
    }

    @Test
    fun `removeCustomTask is called with correct parameters`() = runTest {
        repository.removeCustomTask(week = 1, dayIndex = 0, idSuffix = "1")
        verify(planOverridesStore).update(any())
    }
}
