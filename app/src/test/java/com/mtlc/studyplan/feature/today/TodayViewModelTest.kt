package com.mtlc.studyplan.feature.today

import com.mtlc.studyplan.data.Task
import com.mtlc.studyplan.data.TaskPriority
import com.mtlc.studyplan.data.TaskRepository
import com.mtlc.studyplan.testutils.CoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModelTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    private lateinit var fakeRepository: FakeTaskRepository
    private lateinit var viewModel: TodayViewModel

    @Before
    fun setup() {
        fakeRepository = FakeTaskRepository(
            mutableListOf(
                Task(
                    id = "s1",
                    title = "Reading Sprint",
                    category = "Reading",
                    estimatedMinutes = 25,
                    priority = TaskPriority.MEDIUM
                ),
                Task(
                    id = "s2",
                    title = "Grammar Pack",
                    category = "Grammar",
                    estimatedMinutes = 20,
                    priority = TaskPriority.LOW
                )
            )
        )
        viewModel = TodayViewModel(
            taskRepository = fakeRepository,
            ioDispatcher = coroutineTestRule.testDispatcher
        )
    }

    @Test
    fun `load populates sessions from repository`() = runTest {
        viewModel.dispatch(TodayIntent.Load)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals(2, state.sessions.size)
        assertEquals("Reading Sprint", state.sessions.first().title)
    }

    @Test
    fun `complete marks session as finished`() = runTest {
        viewModel.dispatch(TodayIntent.Load)
        advanceUntilIdle()

        viewModel.dispatch(TodayIntent.Complete("s1"))
        advanceUntilIdle()

        val state = viewModel.state.value
        val completed = state.sessions.firstOrNull { it.id == "s1" }
        assertTrue(completed?.isCompleted == true)
        assertEquals("Completed session s1", state.snackbar)
    }

    @Test
    fun `complete shows error when task missing`() = runTest {
        viewModel.dispatch(TodayIntent.Complete("missing"))
        advanceUntilIdle()

        assertEquals("Unable to complete session missing", viewModel.state.value.snackbar)
    }

    @Test
    fun `skip only updates snackbar`() = runTest {
        viewModel.dispatch(TodayIntent.Skip("s1"))

        val state = viewModel.state.value
        assertEquals("Skipped session s1", state.snackbar)
        assertTrue(state.sessions.isEmpty())
    }

    @Test
    fun `reschedule formats snackbar message`() = runTest {
        val at = java.time.LocalDateTime.of(2024, 12, 24, 14, 30)
        viewModel.dispatch(TodayIntent.Reschedule("s1", at))

        assertEquals("Rescheduled s1 to 2024-12-24 14:30", viewModel.state.value.snackbar)
    }

    @Test
    fun `consumeSnackbar clears message`() = runTest {
        viewModel.dispatch(TodayIntent.Skip("s1"))
        assertEquals("Skipped session s1", viewModel.state.value.snackbar)

        viewModel.consumeSnackbar()
        assertNull(viewModel.state.value.snackbar)
    }

    private class FakeTaskRepository(
        private val tasks: MutableList<Task>
    ) : TaskRepository {
        override fun getAllTasks(): Flow<List<Task>> = emptyFlow()
        override suspend fun getAllTasksSync(): List<Task> = tasks.toList()
        override suspend fun getTaskById(id: String): Task? = tasks.find { it.id == id }
        override suspend fun insertTask(task: Task): Task {
            tasks.add(task)
            return task
        }

        override suspend fun updateTask(task: Task): Task {
            val index = tasks.indexOfFirst { it.id == task.id }
            if (index >= 0) {
                tasks[index] = task
            }
            return task
        }

        override suspend fun deleteTask(id: String) {
            tasks.removeIf { it.id == id }
        }

        override suspend fun getTodaysTasks(): List<Task> = tasks.toList()
        override suspend fun getUpcomingTasks(): List<Task> = emptyList()
        override suspend fun getTasksByCategory(category: String): List<Task> = emptyList()
        override suspend fun getEarlyMorningCompletedTasks(): List<Task> = emptyList()
        override suspend fun getLateNightCompletedTasks(): List<Task> = emptyList()
        override suspend fun getWeekendCompletedTasks(): List<Task> = emptyList()
        override suspend fun getWeekdayCompletedTasks(): List<Task> = emptyList()
        override suspend fun getTasksByPriority(priority: TaskPriority): List<Task> = emptyList()
        override suspend fun getTotalPointsEarned(): Int = 0
        override suspend fun getMaxTasksCompletedInOneDay(): Int = 0
        override suspend fun getAllTasksPaginated(page: Int, pageSize: Int): TaskRepository.PaginatedTasks =
            throw UnsupportedOperationException()

        override suspend fun getCompletedTasksPaginated(page: Int, pageSize: Int): TaskRepository.PaginatedTasks =
            throw UnsupportedOperationException()

        override suspend fun getPendingTasksPaginated(page: Int, pageSize: Int): TaskRepository.PaginatedTasks =
            throw UnsupportedOperationException()
    }
}
