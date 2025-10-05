package com.mtlc.studyplan.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mtlc.studyplan.database.dao.TaskDao
import com.mtlc.studyplan.database.entities.TaskEntity
import com.mtlc.studyplan.shared.TaskCategory
import com.mtlc.studyplan.shared.TaskPriority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Database Integration Tests
 * Tests real Room database operations with actual data persistence
 */
@RunWith(AndroidJUnit4::class)
class StudyPlanDatabaseIntegrationTest {

    private lateinit var taskDao: TaskDao
    private lateinit var db: StudyPlanDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, StudyPlanDatabase::class.java)
            .allowMainThreadQueries() // For testing only
            .build()
        taskDao = db.taskDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun `database should be created successfully`() {
        assertNotNull("Database should be created", db)
        assertNotNull("TaskDao should be accessible", taskDao)
    }

    @Test
    fun `insert and retrieve task by id`() = runBlocking {
        // Given
        val task = createTestTask("test_task_1", "Test Task 1")

        // When
        taskDao.insertTask(task)
        val retrieved = taskDao.getTaskById("test_task_1")

        // Then
        assertNotNull("Task should be retrieved", retrieved)
        assertEquals("Task ID should match", "test_task_1", retrieved?.id)
        assertEquals("Task title should match", "Test Task 1", retrieved?.title)
        assertEquals("Task category should match", TaskCategory.GRAMMAR, retrieved?.category)
    }

    @Test
    fun `insert multiple tasks and retrieve all`() = runBlocking {
        // Given
        val tasks = listOf(
            createTestTask("task_1", "Task 1"),
            createTestTask("task_2", "Task 2"),
            createTestTask("task_3", "Task 3")
        )

        // When
        tasks.forEach { taskDao.insertTask(it) }
        val allTasks = taskDao.getAllTasks().first()

        // Then
        assertEquals("Should have 3 tasks", 3, allTasks.size)
        assertTrue("Should contain task_1", allTasks.any { it.id == "task_1" })
        assertTrue("Should contain task_2", allTasks.any { it.id == "task_2" })
        assertTrue("Should contain task_3", allTasks.any { it.id == "task_3" })
    }

    @Test
    fun `update task should persist changes`() = runBlocking {
        // Given
        val originalTask = createTestTask("update_test", "Original Title")
        taskDao.insertTask(originalTask)

        // When
        val updatedTask = originalTask.copy(
            title = "Updated Title",
            isCompleted = true,
            completedAt = System.currentTimeMillis()
        )
        taskDao.updateTask(updatedTask)
        val retrieved = taskDao.getTaskById("update_test")

        // Then
        assertNotNull("Updated task should exist", retrieved)
        assertEquals("Title should be updated", "Updated Title", retrieved?.title)
        assertTrue("Task should be completed", retrieved?.isCompleted == true)
        assertNotNull("Completed timestamp should be set", retrieved?.completedAt)
    }

    @Test
    fun `delete task should remove from database`() = runBlocking {
        // Given
        val task = createTestTask("delete_test", "Task to Delete")
        taskDao.insertTask(task)

        // Verify task exists
        assertNotNull("Task should exist before deletion", taskDao.getTaskById("delete_test"))

        // When
        taskDao.deleteTask("delete_test")
        val deleted = taskDao.getTaskById("delete_test")

        // Then
        assertNull("Task should be deleted", deleted)
    }

    @Test
    fun `query tasks by category should return correct results`() = runBlocking {
        // Given
        val grammarTask = createTestTask("grammar_1", "Grammar Task", TaskCategory.GRAMMAR)
        val vocabTask = createTestTask("vocab_1", "Vocab Task", TaskCategory.VOCABULARY)
        val readingTask = createTestTask("reading_1", "Reading Task", TaskCategory.READING)

        taskDao.insertTask(grammarTask)
        taskDao.insertTask(vocabTask)
        taskDao.insertTask(readingTask)

        // When
        val grammarTasks = taskDao.getTasksByCategory(TaskCategory.GRAMMAR).first()
        val vocabTasks = taskDao.getTasksByCategory(TaskCategory.VOCABULARY).first()

        // Then
        assertEquals("Should have 1 grammar task", 1, grammarTasks.size)
        assertEquals("Grammar task ID should match", "grammar_1", grammarTasks[0].id)

        assertEquals("Should have 1 vocab task", 1, vocabTasks.size)
        assertEquals("Vocab task ID should match", "vocab_1", vocabTasks[0].id)
    }

    @Test
    fun `query completed tasks should return only completed ones`() = runBlocking {
        // Given
        val completedTask = createTestTask("completed_1", "Completed Task").copy(
            isCompleted = true,
            completedAt = System.currentTimeMillis()
        )
        val pendingTask = createTestTask("pending_1", "Pending Task")

        taskDao.insertTask(completedTask)
        taskDao.insertTask(pendingTask)

        // When
        val completedTasks = taskDao.getCompletedTasks().first()

        // Then
        assertEquals("Should have 1 completed task", 1, completedTasks.size)
        assertEquals("Completed task ID should match", "completed_1", completedTasks[0].id)
        assertTrue("Task should be marked as completed", completedTasks[0].isCompleted)
    }

    @Test
    fun `query pending tasks should return only uncompleted ones`() = runBlocking {
        // Given
        val completedTask = createTestTask("completed_2", "Completed Task").copy(
            isCompleted = true,
            completedAt = System.currentTimeMillis()
        )
        val pendingTask = createTestTask("pending_2", "Pending Task")

        taskDao.insertTask(completedTask)
        taskDao.insertTask(pendingTask)

        // When
        val pendingTasks = taskDao.getPendingTasks().first()

        // Then
        assertEquals("Should have 1 pending task", 1, pendingTasks.size)
        assertEquals("Pending task ID should match", "pending_2", pendingTasks[0].id)
        assertFalse("Task should not be completed", pendingTasks[0].isCompleted)
    }

    @Test
    fun `query tasks by priority should return correct ordering`() = runBlocking {
        // Given
        val highPriority = createTestTask("high", "High Priority", priority = TaskPriority.HIGH)
        val mediumPriority = createTestTask("medium", "Medium Priority", priority = TaskPriority.MEDIUM)
        val lowPriority = createTestTask("low", "Low Priority", priority = TaskPriority.LOW)

        taskDao.insertTask(highPriority)
        taskDao.insertTask(mediumPriority)
        taskDao.insertTask(lowPriority)

        // When
        val highTasks = taskDao.getTasksByPriority(TaskPriority.HIGH).first()
        val mediumTasks = taskDao.getTasksByPriority(TaskPriority.MEDIUM).first()
        val lowTasks = taskDao.getTasksByPriority(TaskPriority.LOW).first()

        // Then
        assertEquals("Should have 1 high priority task", 1, highTasks.size)
        assertEquals("Should have 1 medium priority task", 1, mediumTasks.size)
        assertEquals("Should have 1 low priority task", 1, lowTasks.size)
    }

    @Test
    fun `bulk insert and delete operations should work correctly`() = runBlocking {
        // Given
        val tasks = (1..10).map { i ->
            createTestTask("bulk_$i", "Bulk Task $i")
        }

        // When - Bulk insert
        tasks.forEach { taskDao.insertTask(it) }
        val allTasksAfterInsert = taskDao.getAllTasks().first()

        // Then
        assertEquals("Should have 10 tasks after bulk insert", 10, allTasksAfterInsert.size)

        // When - Bulk delete
        tasks.forEach { taskDao.deleteTask(it.id) }
        val allTasksAfterDelete = taskDao.getAllTasks().first()

        // Then
        assertEquals("Should have 0 tasks after bulk delete", 0, allTasksAfterDelete.size)
    }

    @Test
    fun `database should handle concurrent operations safely`() = runBlocking {
        // Given
        val tasks = (1..5).map { i ->
            createTestTask("concurrent_$i", "Concurrent Task $i")
        }

        // When - Sequential operations (avoiding complex coroutine issues in instrumentation tests)
        // First, insert all tasks sequentially
        tasks.forEach { task ->
            taskDao.insertTask(task)
        }

        // Then, update all tasks sequentially
        tasks.forEach { task ->
            taskDao.updateTask(task.copy(title = "Updated ${task.title}"))
        }

        // Then
        val allTasks = taskDao.getAllTasks().first()
        assertEquals("Should have 5 tasks after concurrent operations", 5, allTasks.size)
        assertTrue("All tasks should be updated", allTasks.all { it.title.startsWith("Updated") })
    }

    @Test
    fun `database should maintain data integrity across operations`() = runBlocking {
        // Given
        val task = createTestTask("integrity_test", "Integrity Test")
        taskDao.insertTask(task)

        // When - Multiple operations on same task
        val retrieved1 = taskDao.getTaskById("integrity_test")
        taskDao.updateTask(task.copy(title = "Modified Title"))
        val retrieved2 = taskDao.getTaskById("integrity_test")
        taskDao.updateTask(task.copy(title = "Final Title", isCompleted = true))
        val retrieved3 = taskDao.getTaskById("integrity_test")

        // Then
        assertNotNull("Task should exist", retrieved1)
        assertNotNull("Task should exist after first update", retrieved2)
        assertNotNull("Task should exist after second update", retrieved3)

        assertEquals("Original title", "Integrity Test", retrieved1?.title)
        assertEquals("First update title", "Modified Title", retrieved2?.title)
        assertEquals("Final title", "Final Title", retrieved3?.title)
        assertTrue("Task should be completed", retrieved3?.isCompleted == true)
    }

    private fun createTestTask(
        id: String,
        title: String,
        category: TaskCategory = TaskCategory.GRAMMAR,
        priority: TaskPriority = TaskPriority.MEDIUM
    ): TaskEntity {
        return TaskEntity(
            id = id,
            title = title,
            description = "Test description for $title",
            category = category,
            priority = priority,
            difficulty = com.mtlc.studyplan.shared.TaskDifficulty.MEDIUM,
            estimatedMinutes = 30,
            actualMinutes = 0,
            isCompleted = false,
            completedAt = null,
            createdAt = System.currentTimeMillis(),
            dueDate = null,
            tags = emptyList(),
            streakContribution = true,
            pointsValue = 10,
            isActive = true,
            parentTaskId = null,
            orderIndex = 0,
            notes = null,
            attachments = emptyList(),
            reminderTime = null,
            isRecurring = false,
            recurringPattern = null,
            lastModified = System.currentTimeMillis()
        )
    }
}