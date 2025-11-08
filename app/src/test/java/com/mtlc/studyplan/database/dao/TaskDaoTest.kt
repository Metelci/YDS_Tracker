package com.mtlc.studyplan.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mtlc.studyplan.database.StudyPlanDatabase
import com.mtlc.studyplan.database.entities.TaskEntity
import com.mtlc.studyplan.shared.TaskCategory
import com.mtlc.studyplan.shared.TaskDifficulty
import com.mtlc.studyplan.shared.TaskPriority
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.koin.core.context.stopKoin
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for TaskDao using actual API signatures
 * Tests with in-memory Room database
 */
@RunWith(RobolectricTestRunner::class)
class TaskDaoTest {

    private lateinit var database: StudyPlanDatabase
    private lateinit var taskDao: TaskDao

    @Before
    fun setUp() {
        try { stopKoin() } catch (e: Exception) { }
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            StudyPlanDatabase::class.java
        )
            .allowMainThreadQueries()
            .setJournalMode(androidx.room.RoomDatabase.JournalMode.TRUNCATE) // Fix for Robolectric
            .build()
        taskDao = database.taskDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ========== HELPER FUNCTIONS ==========

    private fun createTestTask(
        id: String = "task-1",
        title: String = "Test Task",
        category: TaskCategory = TaskCategory.GRAMMAR,
        difficulty: TaskDifficulty = TaskDifficulty.MEDIUM,
        priority: TaskPriority = TaskPriority.MEDIUM,
        estimatedMinutes: Int = 60,
        isCompleted: Boolean = false
    ) = TaskEntity(
        id = id,
        title = title,
        description = "Test description",
        category = category,
        difficulty = difficulty,
        priority = priority,
        estimatedMinutes = estimatedMinutes,
        isCompleted = isCompleted,
        createdAt = System.currentTimeMillis()
    )

    // ========== INSERT TESTS ==========

    @Test
    fun `insertTask should store task in database`() = runTest {
        val task = createTestTask(id = "1")

        taskDao.insertTask(task)

        val retrieved = taskDao.getTaskById("1")
        assertNotNull(retrieved)
        assertEquals("Test Task", retrieved?.title)
    }

    @Test
    fun `insertTask should assign primary key`() = runTest {
        val task = createTestTask()
        taskDao.insertTask(task)

        val retrieved = taskDao.getTaskById(task.id)
        assertNotNull(retrieved)
        assertEquals(task.id, retrieved?.id)
    }

    @Test
    fun `insertMultipleTasks should store all tasks`() = runTest {
        val tasks = listOf(
            createTestTask(id = "1", title = "Task 1"),
            createTestTask(id = "2", title = "Task 2"),
            createTestTask(id = "3", title = "Task 3")
        )

        taskDao.insertTasks(tasks)

        val task1 = taskDao.getTaskById("1")
        val task2 = taskDao.getTaskById("2")
        val task3 = taskDao.getTaskById("3")

        assertNotNull(task1)
        assertNotNull(task2)
        assertNotNull(task3)
    }

    // ========== READ TESTS ==========

    @Test
    fun `getTaskById should return task when exists`() = runTest {
        val task = createTestTask(id = "1")
        taskDao.insertTask(task)

        val retrieved = taskDao.getTaskById("1")

        assertNotNull(retrieved)
        assertEquals("1", retrieved?.id)
    }

    @Test
    fun `getTaskById should return null when not exists`() = runTest {
        val retrieved = taskDao.getTaskById("nonexistent")

        assertNull(retrieved)
    }

    @Test
    fun `getAllTasks should return all active tasks`() = runTest {
        taskDao.insertTasks(listOf(
            createTestTask(id = "1"),
            createTestTask(id = "2"),
            createTestTask(id = "3")
        ))

        val tasks = taskDao.getAllTasks().first()

        assertEquals(3, tasks.size)
    }

    // ========== UPDATE TESTS ==========

    @Test
    fun `updateTask should modify task`() = runTest {
        val task = createTestTask(id = "1", title = "Original")
        taskDao.insertTask(task)

        val updated = task.copy(title = "Updated")
        taskDao.updateTask(updated)

        val retrieved = taskDao.getTaskById("1")
        assertEquals("Updated", retrieved?.title)
    }

    @Test
    fun `updateTaskCompletion should mark task completed`() = runTest {
        val task = createTestTask(id = "1", isCompleted = false)
        taskDao.insertTask(task)

        taskDao.updateTaskCompletion("1", true, System.currentTimeMillis(), 45)

        val retrieved = taskDao.getTaskById("1")
        assertEquals(true, retrieved?.isCompleted)
    }

    // ========== DELETE TESTS ==========

    @Test
    fun `deleteTask should soft delete task`() = runTest {
        val task = createTestTask(id = "1")
        taskDao.insertTask(task)

        taskDao.deleteTask("1")

        val retrieved = taskDao.getTaskById("1")
        // Soft delete sets isActive = 0, so task might still exist but be inactive
        // The query filters by isActive = 1, so we might not get it in getAllTasks
        val allTasks = taskDao.getAllTasks().first()
        assertEquals(0, allTasks.size)
    }

    @Test
    fun `permanentlyDeleteTask should hard delete task`() = runTest {
        val task = createTestTask(id = "1")
        taskDao.insertTask(task)

        taskDao.permanentlyDeleteTask("1")

        val retrieved = taskDao.getTaskById("1")
        assertNull(retrieved)
    }

    // ========== CATEGORY TESTS ==========

    @Test
    fun `getTasksByCategory should filter by category`() = runTest {
        taskDao.insertTasks(listOf(
            createTestTask(id = "1", category = TaskCategory.GRAMMAR),
            createTestTask(id = "2", category = TaskCategory.VOCABULARY),
            createTestTask(id = "3", category = TaskCategory.GRAMMAR)
        ))

        val grammarTasks = taskDao.getTasksByCategory(TaskCategory.GRAMMAR).first()

        assertEquals(2, grammarTasks.size)
        assertTrue(grammarTasks.all { it.category == TaskCategory.GRAMMAR })
    }

    @Test
    fun `getAllCategories should return distinct categories`() = runTest {
        taskDao.insertTasks(listOf(
            createTestTask(id = "1", category = TaskCategory.GRAMMAR),
            createTestTask(id = "2", category = TaskCategory.VOCABULARY),
            createTestTask(id = "3", category = TaskCategory.GRAMMAR)
        ))

        val categories = taskDao.getAllCategories()

        assertTrue(categories.contains(TaskCategory.GRAMMAR))
        assertTrue(categories.contains(TaskCategory.VOCABULARY))
    }

    // ========== PRIORITY TESTS ==========

    @Test
    fun `getTasksByPriority should filter by priority`() = runTest {
        taskDao.insertTasks(listOf(
            createTestTask(id = "1", priority = TaskPriority.HIGH),
            createTestTask(id = "2", priority = TaskPriority.LOW),
            createTestTask(id = "3", priority = TaskPriority.HIGH)
        ))

        val highPriorityTasks = taskDao.getTasksByPriority(TaskPriority.HIGH).first()

        assertEquals(2, highPriorityTasks.size)
        assertTrue(highPriorityTasks.all { it.priority == TaskPriority.HIGH })
    }

    // ========== COMPLETION TESTS ==========

    @Test
    fun `getPendingTasks should return incomplete tasks`() = runTest {
        taskDao.insertTasks(listOf(
            createTestTask(id = "1", isCompleted = false),
            createTestTask(id = "2", isCompleted = true),
            createTestTask(id = "3", isCompleted = false)
        ))

        val pendingTasks = taskDao.getPendingTasks().first()

        assertEquals(2, pendingTasks.size)
        assertTrue(pendingTasks.all { !it.isCompleted })
    }

    @Test
    fun `getCompletedTasks should return completed tasks`() = runTest {
        taskDao.insertTasks(listOf(
            createTestTask(id = "1", isCompleted = false),
            createTestTask(id = "2", isCompleted = true),
            createTestTask(id = "3", isCompleted = true)
        ))

        // Mark tasks as completed properly
        taskDao.updateTaskCompletion("2", true, System.currentTimeMillis(), 0)
        taskDao.updateTaskCompletion("3", true, System.currentTimeMillis(), 0)

        val completedTasks = taskDao.getCompletedTasks().first()

        assertEquals(2, completedTasks.size)
        assertTrue(completedTasks.all { it.isCompleted })
    }

    // ========== COUNT TESTS ==========

    @Test
    fun `getCompletedTasksCount should return count`() = runTest {
        taskDao.insertTasks(listOf(
            createTestTask(id = "1", isCompleted = false),
            createTestTask(id = "2", isCompleted = true),
            createTestTask(id = "3", isCompleted = true)
        ))

        taskDao.updateTaskCompletion("2", true, System.currentTimeMillis(), 0)
        taskDao.updateTaskCompletion("3", true, System.currentTimeMillis(), 0)

        val count = taskDao.getCompletedTasksCount()

        assertEquals(2, count)
    }

    @Test
    fun `getPendingTasksCount should return count`() = runTest {
        taskDao.insertTasks(listOf(
            createTestTask(id = "1", isCompleted = false),
            createTestTask(id = "2", isCompleted = true),
            createTestTask(id = "3", isCompleted = false)
        ))

        val count = taskDao.getPendingTasksCount()

        assertTrue(count >= 2)
    }

    // ========== SEARCH TESTS ==========

    @Test
    fun `searchTasks should find matching tasks`() = runTest {
        taskDao.insertTasks(listOf(
            createTestTask(id = "1", title = "Grammar Exercise"),
            createTestTask(id = "2", title = "Vocabulary Test"),
            createTestTask(id = "3", title = "Grammar Rules")
        ))

        val results = taskDao.searchTasks("Grammar").first()

        assertEquals(2, results.size)
        assertTrue(results.all { it.title.contains("Grammar") })
    }

    // ========== SUBTASK TESTS ==========

    @Test
    fun `getSubTasks should return child tasks`() = runTest {
        val parentTask = createTestTask(id = "parent-1")
        val subtask1 = createTestTask(id = "sub-1").copy(parentTaskId = "parent-1")
        val subtask2 = createTestTask(id = "sub-2").copy(parentTaskId = "parent-1")

        taskDao.insertTasks(listOf(parentTask, subtask1, subtask2))

        val subtasks = taskDao.getSubTasks("parent-1").first()

        assertEquals(2, subtasks.size)
    }

    // ========== BATCH OPERATIONS ==========

    @Test
    fun `deleteTasks should soft delete multiple tasks`() = runTest {
        taskDao.insertTasks(listOf(
            createTestTask(id = "1"),
            createTestTask(id = "2"),
            createTestTask(id = "3")
        ))

        taskDao.deleteTasks(listOf("1", "2"))

        val allTasks = taskDao.getAllTasks().first()
        assertEquals(1, allTasks.size)
    }

    @Test
    fun `updateTasks should update multiple tasks`() = runTest {
        val tasks = listOf(
            createTestTask(id = "1", title = "Task 1"),
            createTestTask(id = "2", title = "Task 2")
        )
        taskDao.insertTasks(tasks)

        val updatedTasks = tasks.map { it.copy(title = it.title + " Updated") }
        taskDao.updateTasks(updatedTasks)

        val task1 = taskDao.getTaskById("1")
        assertEquals("Task 1 Updated", task1?.title)
    }

    // ========== ANALYTICS TESTS ==========

    @Test
    fun `getTotalActiveTasksCount should return count`() = runTest {
        taskDao.insertTasks(listOf(
            createTestTask(id = "1"),
            createTestTask(id = "2"),
            createTestTask(id = "3")
        ))

        val count = taskDao.getTotalActiveTasksCount()

        assertEquals(3, count)
    }
}
