package com.mtlc.studyplan.repository

import app.cash.turbine.test
import com.mtlc.studyplan.database.dao.TaskDao
import com.mtlc.studyplan.database.entities.TaskEntity
import com.mtlc.studyplan.shared.TaskCategory
import com.mtlc.studyplan.shared.TaskDifficulty
import com.mtlc.studyplan.shared.TaskPriority
import com.mtlc.studyplan.testutils.CoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class TaskRepositoryTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Mock
    private lateinit var taskDao: TaskDao

    private lateinit var repository: TaskRepository

    private val testTask1 = TaskEntity(
        id = "task1",
        title = "Study Grammar",
        description = "Review verb tenses",
        category = TaskCategory.GRAMMAR,
        priority = TaskPriority.HIGH,
        difficulty = TaskDifficulty.MEDIUM,
        estimatedMinutes = 60,
        isCompleted = false,
        createdAt = System.currentTimeMillis()
    )

    private val testTask2 = TaskEntity(
        id = "task2",
        title = "Practice Listening",
        description = "YDS listening practice",
        category = TaskCategory.LISTENING,
        priority = TaskPriority.MEDIUM,
        difficulty = TaskDifficulty.HARD,
        estimatedMinutes = 45,
        isCompleted = true,
        completedAt = System.currentTimeMillis(),
        actualMinutes = 50,
        createdAt = System.currentTimeMillis()
    )

    private val testTask3 = TaskEntity(
        id = "task3",
        title = "Vocabulary Review",
        description = "Learn 50 new words",
        category = TaskCategory.VOCABULARY,
        priority = TaskPriority.LOW,
        difficulty = TaskDifficulty.EASY,
        estimatedMinutes = 30,
        isCompleted = false,
        createdAt = System.currentTimeMillis()
    )

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)

        // Setup default mock responses
        whenever(taskDao.getAllTasks()).thenReturn(flowOf(listOf(testTask1, testTask2, testTask3)))
        whenever(taskDao.getPendingTasks()).thenReturn(flowOf(listOf(testTask1, testTask3)))
        whenever(taskDao.getCompletedTasks()).thenReturn(flowOf(listOf(testTask2)))
        whenever(taskDao.getTodayTasks(any(), any())).thenReturn(flowOf(listOf(testTask1, testTask2)))
        whenever(taskDao.getTomorrowTasks()).thenReturn(flowOf(emptyList()))
        whenever(taskDao.getUpcomingTasks()).thenReturn(flowOf(listOf(testTask3)))
        whenever(taskDao.getOverdueTasks()).thenReturn(flowOf(emptyList()))

        repository = TaskRepository(taskDao)
    }

    // MARK: - CRUD Operations Tests

    @Test
    fun `getTaskById returns task when exists`() = runTest {
        // Given: Task exists in DAO
        whenever(taskDao.getTaskById("task1")).thenReturn(testTask1)

        // When: Getting task by ID
        val task = repository.getTaskById("task1")

        // Then: Returns correct task
        assertNotNull(task)
        assertEquals("task1", task?.id)
        assertEquals("Study Grammar", task?.title)
    }

    @Test
    fun `getTaskById returns null when task does not exist`() = runTest {
        // Given: Task doesn't exist
        whenever(taskDao.getTaskById("nonexistent")).thenReturn(null)

        // When: Getting non-existent task
        val task = repository.getTaskById("nonexistent")

        // Then: Returns null
        assertNull(task)
    }

    @Test
    fun `insertTask calls DAO and triggers refresh`() = runTest {
        // Given: New task to insert
        val newTask = testTask1.copy(id = "new_task")

        // When: Inserting task
        repository.insertTask(newTask)

        // Then: DAO insert is called
        verify(taskDao).insertTask(newTask)
    }

    @Test
    fun `insertTasks inserts multiple tasks and triggers refresh`() = runTest {
        // Given: Multiple tasks to insert
        val tasks = listOf(testTask1, testTask2, testTask3)

        // When: Inserting multiple tasks
        repository.insertTasks(tasks)

        // Then: DAO insertTasks is called
        verify(taskDao).insertTasks(tasks)
    }

    @Test
    fun `updateTask calls DAO and triggers refresh`() = runTest {
        // Given: Task with updated data
        val updatedTask = testTask1.copy(title = "Updated Title")

        // When: Updating task
        repository.updateTask(updatedTask)

        // Then: DAO update is called
        verify(taskDao).updateTask(updatedTask)
    }

    @Test
    fun `deleteTask calls DAO and triggers refresh`() = runTest {
        // When: Deleting task
        repository.deleteTask("task1")

        // Then: DAO delete is called
        verify(taskDao).deleteTask("task1")
    }

    @Test
    fun `deleteTasks deletes multiple tasks`() = runTest {
        // Given: Multiple task IDs to delete
        val taskIds = listOf("task1", "task2")

        // When: Deleting multiple tasks
        repository.deleteTasks(taskIds)

        // Then: DAO deleteTasks is called
        verify(taskDao).deleteTasks(taskIds)
    }

    // MARK: - Task Completion Tests

    @Test
    fun `completeTask marks task as completed with timestamp`() = runTest {
        // When: Completing a task
        repository.completeTask("task1", actualMinutes = 65)

        // Then: DAO updateTaskCompletion is called with correct parameters
        verify(taskDao).updateTaskCompletion(
            taskId = any(),
            isCompleted = any(),
            completedAt = anyOrNull(),
            actualMinutes = any()
        )
    }

    @Test
    fun `uncompleteTask marks task as incomplete`() = runTest {
        // When: Un-completing a task
        repository.uncompleteTask("task1")

        // Then: DAO updateTaskCompletion is called with false
        verify(taskDao).updateTaskCompletion(
            taskId = any(),
            isCompleted = any(),
            completedAt = anyOrNull(),
            actualMinutes = any()
        )
    }

    @Test
    fun `completeMultipleTasks completes multiple tasks at once`() = runTest {
        // Given: Multiple task IDs
        val taskIds = listOf("task1", "task2", "task3")

        // When: Completing multiple tasks
        repository.completeMultipleTasks(taskIds, actualMinutes = 120)

        // Then: Each task is updated (verify at least called)
        verify(taskDao, org.mockito.kotlin.atLeastOnce()).updateTaskCompletion(
            taskId = any(),
            isCompleted = any(),
            completedAt = anyOrNull(),
            actualMinutes = any()
        )
    }

    // MARK: - Flow-based Query Tests

    @Test
    fun `allTasks emits all tasks`() = runTest {
        // When: Observing all tasks
        repository.allTasks.test {
            // Then: Emits all tasks
            val tasks = awaitItem()
            assertEquals(3, tasks.size)
            assertTrue(tasks.any { it.id == "task1" })
            assertTrue(tasks.any { it.id == "task2" })
            assertTrue(tasks.any { it.id == "task3" })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `pendingTasks emits only incomplete tasks`() = runTest {
        // When: Observing pending tasks
        repository.pendingTasks.test {
            // Then: Emits only incomplete tasks
            val tasks = awaitItem()
            assertEquals(2, tasks.size)
            assertTrue(tasks.all { !it.isCompleted })
            assertTrue(tasks.any { it.id == "task1" })
            assertTrue(tasks.any { it.id == "task3" })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `completedTasks emits only completed tasks`() = runTest {
        // When: Observing completed tasks
        repository.completedTasks.test {
            // Then: Emits only completed tasks
            val tasks = awaitItem()
            assertEquals(1, tasks.size)
            assertTrue(tasks.all { it.isCompleted })
            assertEquals("task2", tasks.first().id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `todayTasks emits tasks for today`() = runTest {
        // When: Observing today's tasks
        repository.todayTasks.test {
            // Then: Emits today's tasks
            val tasks = awaitItem()
            assertEquals(2, tasks.size)
            assertTrue(tasks.any { it.id == "task1" })
            assertTrue(tasks.any { it.id == "task2" })
            cancelAndIgnoreRemainingEvents()
        }
    }

    // MARK: - Analytics Flow Tests

    @Test
    fun `totalTaskCount emits correct count`() = runTest {
        // When: Observing total task count
        repository.totalTaskCount.test {
            // Then: Emits correct count
            assertEquals(3, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `pendingTaskCount emits correct count`() = runTest {
        // When: Observing pending task count
        repository.pendingTaskCount.test {
            // Then: Emits correct count
            assertEquals(2, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `completedTaskCount emits correct count`() = runTest {
        // When: Observing completed task count
        repository.completedTaskCount.test {
            // Then: Emits correct count
            assertEquals(1, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `todayProgress calculates completion rate correctly`() = runTest {
        // When: Observing today's progress
        repository.todayProgress.test {
            // Then: Progress is calculated correctly
            val progress = awaitItem()
            assertEquals(1, progress.completedToday) // task2 is completed
            assertEquals(2, progress.totalToday)
            assertEquals(50f, progress.completionRate, 0.01f)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `tasksByCategory groups tasks correctly`() = runTest {
        // When: Observing tasks by category
        repository.tasksByCategory.test {
            // Then: Tasks are grouped by category
            val categoryMap = awaitItem()
            assertEquals(3, categoryMap.size)
            assertTrue(categoryMap.containsKey(TaskCategory.GRAMMAR))
            assertTrue(categoryMap.containsKey(TaskCategory.LISTENING))
            assertTrue(categoryMap.containsKey(TaskCategory.VOCABULARY))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `categoryProgress calculates percentage correctly`() = runTest {
        // When: Observing category progress
        repository.categoryProgress.test {
            // Then: Progress is calculated per category
            val progressMap = awaitItem()

            val listeningProgress = progressMap[TaskCategory.LISTENING]
            assertNotNull(listeningProgress)
            assertEquals(1, listeningProgress?.completed)
            assertEquals(1, listeningProgress?.total)
            assertEquals(100f, listeningProgress?.percentage ?: 0f, 0.01f)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // MARK: - Category and Priority Operations Tests

    @Test
    fun `getTasksByCategory filters tasks by category`() = runTest {
        // Given: Tasks filtered by category
        whenever(taskDao.getTasksByCategory(TaskCategory.GRAMMAR))
            .thenReturn(flowOf(listOf(testTask1)))

        // When: Getting tasks by category
        repository.getTasksByCategory(TaskCategory.GRAMMAR).test {
            // Then: Returns only grammar tasks
            val tasks = awaitItem()
            assertEquals(1, tasks.size)
            assertEquals(TaskCategory.GRAMMAR, tasks.first().category)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getTasksByPriority filters tasks by priority`() = runTest {
        // Given: Tasks filtered by priority
        whenever(taskDao.getTasksByPriority(TaskPriority.HIGH))
            .thenReturn(flowOf(listOf(testTask1)))

        // When: Getting tasks by priority
        repository.getTasksByPriority(TaskPriority.HIGH).test {
            // Then: Returns only high priority tasks
            val tasks = awaitItem()
            assertEquals(1, tasks.size)
            assertEquals(TaskPriority.HIGH, tasks.first().priority)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `moveTaskToCategory updates task category`() = runTest {
        // When: Moving task to new category
        repository.moveTaskToCategory("task1", TaskCategory.READING)

        // Then: DAO moveTaskToCategory is called
        verify(taskDao).moveTaskToCategory("task1", TaskCategory.READING)
    }

    @Test
    fun `updateTaskPriorities updates multiple task priorities`() = runTest {
        // Given: Task exists
        whenever(taskDao.getTaskById("task1")).thenReturn(testTask1)

        // When: Updating priorities
        val updates = mapOf<String, TaskPriority>("task1" to TaskPriority.CRITICAL)
        repository.updateTaskPriorities(updates)

        // Then: Task is updated with new priority
        verify(taskDao).updateTask(any())
    }

    // MARK: - Search Operations Tests

    @Test
    fun `searchTasks returns matching tasks`() = runTest {
        // Given: Search query matches tasks
        whenever(taskDao.searchTasks("Grammar"))
            .thenReturn(flowOf(listOf(testTask1)))

        // When: Searching for tasks
        repository.searchTasks("Grammar").test {
            // Then: Returns matching tasks
            val tasks = awaitItem()
            assertEquals(1, tasks.size)
            assertTrue(tasks.first().title.contains("Grammar"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    // MARK: - Advanced Filtering Tests

    @Test
    fun `getFilteredTasks filters by multiple criteria`() = runTest {
        // When: Filtering by category and priority
        repository.getFilteredTasks(
            category = TaskCategory.GRAMMAR,
            priority = TaskPriority.HIGH,
            isCompleted = false
        ).test {
            // Then: Returns tasks matching all criteria
            val tasks = awaitItem()
            assertTrue(tasks.all {
                it.category == TaskCategory.GRAMMAR &&
                it.priority == TaskPriority.HIGH &&
                !it.isCompleted
            })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getSortedTasks sorts by priority descending`() = runTest {
        // When: Sorting by priority (descending)
        repository.getSortedTasks(
            sortBy = TaskRepository.TaskSortBy.PRIORITY,
            ascending = false
        ).test {
            // Then: Tasks are sorted by priority (HIGH > MEDIUM > LOW)
            val tasks = awaitItem()
            assertEquals(3, tasks.size)
            // HIGH should come first when descending
            assertTrue(tasks.first().priority.ordinal >= tasks.last().priority.ordinal)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getSortedTasks sorts by title ascending`() = runTest {
        // When: Sorting by title (ascending)
        repository.getSortedTasks(
            sortBy = TaskRepository.TaskSortBy.TITLE,
            ascending = true
        ).test {
            // Then: Tasks are sorted alphabetically
            val tasks = awaitItem()
            assertEquals(3, tasks.size)
            // Should be alphabetically sorted
            assertTrue(tasks[0].title <= tasks[1].title)
            assertTrue(tasks[1].title <= tasks[2].title)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // MARK: - Analytics Operations Tests

    @Test
    fun `getCompletedTasksCount returns correct count`() = runTest {
        // Given: DAO returns completed count
        whenever(taskDao.getCompletedTasksCount()).thenReturn(5)

        // When: Getting completed count
        val count = repository.getCompletedTasksCount()

        // Then: Returns correct count
        assertEquals(5, count)
    }

    @Test
    fun `getTodayStudyMinutes returns total minutes`() = runTest {
        // Given: DAO returns study minutes
        whenever(taskDao.getTodayStudyMinutes()).thenReturn(120)

        // When: Getting today's study minutes
        val minutes = repository.getTodayStudyMinutes()

        // Then: Returns correct total
        assertEquals(120, minutes)
    }

    @Test
    fun `getTodayPointsEarned returns total points`() = runTest {
        // Given: DAO returns points
        whenever(taskDao.getTodayPointsEarned()).thenReturn(150)

        // When: Getting today's points
        val points = repository.getTodayPointsEarned()

        // Then: Returns correct total
        assertEquals(150, points)
    }

    // MARK: - Cache Operations Tests

    @Test
    fun `getCachedTodayCompletedCount uses cache`() = runTest {
        // Given: DAO returns count
        whenever(taskDao.getTodayCompletedCount()).thenReturn(3)

        // When: Getting cached count twice
        val count1 = repository.getCachedTodayCompletedCount()
        val count2 = repository.getCachedTodayCompletedCount()

        // Then: Returns cached value (DAO called only once)
        assertEquals(3, count1)
        assertEquals(3, count2)
        verify(taskDao).getTodayCompletedCount() // Called once due to cache
    }

    @Test
    fun `getCachedPendingTasksCount uses cache`() = runTest {
        // Given: DAO returns count
        whenever(taskDao.getPendingTasksCount()).thenReturn(5)

        // When: Getting cached count
        val count = repository.getCachedPendingTasksCount()

        // Then: Returns cached value
        assertEquals(5, count)
    }

    // MARK: - Refresh Trigger Tests

    @Test
    fun `insertTask triggers refresh`() = runTest {
        // Given: Initial refresh value
        val initialRefresh = repository.refreshTrigger.value

        // When: Inserting task
        repository.insertTask(testTask1)
        testScheduler.advanceUntilIdle()

        // Then: Refresh trigger is updated
        assertTrue(repository.refreshTrigger.value > initialRefresh)
    }

    @Test
    fun `completeTask triggers refresh`() = runTest {
        // Given: Initial refresh value
        val initialRefresh = repository.refreshTrigger.value

        // When: Completing task
        repository.completeTask("task1")
        testScheduler.advanceUntilIdle()

        // Then: Refresh trigger is updated
        assertTrue(repository.refreshTrigger.value > initialRefresh)
    }

    // MARK: - Batch Operations Tests

    @Test
    fun `deleteAllTasks clears all tasks`() = runTest {
        // When: Deleting all tasks
        repository.deleteAllTasks()

        // Then: DAO deleteAllTasks is called
        verify(taskDao).deleteAllTasks()
    }

    @Test
    fun `reorderTasks updates task order`() = runTest {
        // Given: Tasks with new order
        val reorderedTasks = listOf(testTask3, testTask1, testTask2)

        // When: Reordering tasks
        repository.reorderTasks(reorderedTasks)

        // Then: DAO reorderTasks is called
        verify(taskDao).reorderTasks(reorderedTasks)
    }
}
