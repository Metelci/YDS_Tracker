package com.mtlc.studyplan.data

import com.mtlc.studyplan.shared.AppTask
import org.mockito.Mockito.lenient
import com.mtlc.studyplan.shared.SharedAppViewModel
import com.mtlc.studyplan.shared.StudyStats
import com.mtlc.studyplan.shared.TaskCategory
import com.mtlc.studyplan.shared.TaskDifficulty
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Phase 4 Data Consistency Manager Tests
 * Tests for DataConsistencyManager - ensures data integrity across app state
 */
@RunWith(MockitoJUnitRunner.Silent::class)
class DataConsistencyManagerTest_Phase4 {

    @Mock
    private lateinit var sharedViewModel: SharedAppViewModel

    @Mock
    private lateinit var localRepository: LocalRepository

    private lateinit var consistencyManager: DataConsistencyManager

    @Before
    fun setUp() {
        consistencyManager = DataConsistencyManager(sharedViewModel, localRepository)
    }

    private fun completedTask(id: String = "1", xp: Int = 100) = AppTask(
        id = id,
        title = "Task $id",
        description = "Test",
        category = TaskCategory.GRAMMAR,
        difficulty = TaskDifficulty.MEDIUM,
        estimatedMinutes = 60,
        isCompleted = true,
        xpReward = xp
    )

    private fun stats(
        totalTasks: Int = 0,
        totalTime: Int = 0,
        totalXp: Int = 0,
        streak: Int = 0
    ) = StudyStats(
        totalTasksCompleted = totalTasks,
        totalStudyTime = totalTime,
        totalXP = totalXp,
        currentStreak = streak
    )

    @Test
    fun `returns Consistent when data matches`() = runTest {
        val tasks = listOf(completedTask())
        whenever(sharedViewModel.allTasks).thenReturn(MutableStateFlow(tasks))
        whenever(sharedViewModel.todayTasks).thenReturn(MutableStateFlow(emptyList()))
        whenever(sharedViewModel.currentStreak).thenReturn(MutableStateFlow(0))
        whenever(sharedViewModel.studyStats).thenReturn(MutableStateFlow(stats(totalTasks = 1, totalTime = 60, totalXp = 100)))

        val result = consistencyManager.ensureDataConsistency()

        assertTrue(result is ConsistencyResult.Consistent || result is ConsistencyResult.Fixed)
    }

    @Test
    fun `fixes simple mismatches`() = runTest {
        val tasks = listOf(completedTask("1"), completedTask("2"))
        whenever(sharedViewModel.allTasks).thenReturn(MutableStateFlow(tasks))
        whenever(sharedViewModel.todayTasks).thenReturn(MutableStateFlow(emptyList()))
        whenever(sharedViewModel.currentStreak).thenReturn(MutableStateFlow(0))
        whenever(sharedViewModel.studyStats).thenReturn(MutableStateFlow(stats(totalTasks = 0)))

        val result = consistencyManager.ensureDataConsistency()

        assertTrue(result is ConsistencyResult.Fixed)
        val fixed = result as ConsistencyResult.Fixed
        assertTrue(fixed.inconsistencies.isNotEmpty())
        assertTrue(fixed.fixResults.isNotEmpty())
    }

    @Test
    fun `handles validation exceptions gracefully`() = runTest {
        whenever(sharedViewModel.allTasks).thenReturn(MutableStateFlow(emptyList()))
        whenever(sharedViewModel.todayTasks).thenReturn(MutableStateFlow(emptyList()))
        whenever(sharedViewModel.currentStreak).thenReturn(MutableStateFlow(0))
        doThrow(IllegalStateException("Test exception")).whenever(sharedViewModel).studyStats

        val result = consistencyManager.ensureDataConsistency()

        assertTrue(result is ConsistencyResult.Error)
        val error = result as ConsistencyResult.Error
        assertTrue(error.message.isNotBlank())
    }

    @Test
    fun `report summarises current state`() = runTest {
        val tasks = listOf(completedTask("1"), completedTask("2"), completedTask("3"))
        whenever(sharedViewModel.allTasks).thenReturn(MutableStateFlow(tasks))
        whenever(sharedViewModel.todayTasks).thenReturn(MutableStateFlow(tasks.take(1)))
        whenever(sharedViewModel.currentStreak).thenReturn(MutableStateFlow(2))
        whenever(sharedViewModel.studyStats).thenReturn(MutableStateFlow(stats(totalTasks = 3, totalTime = 180, totalXp = 300, streak = 2)))

        val report = consistencyManager.getConsistencyReport()

        assertEquals(3, report.totalTasks)
        assertEquals(3, report.completedTasks)
        assertEquals(2, report.currentStats.currentStreak)
    }
}



