package com.mtlc.studyplan.database.entities

import com.mtlc.studyplan.shared.TaskCategory
import com.mtlc.studyplan.shared.TaskDifficulty
import com.mtlc.studyplan.shared.TaskPriority
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for TaskEntity - Room database entity for tasks
 * Focus: Data structure validation, immutability, and field defaults
 */
class TaskEntityTest {

    // ========== HELPER FUNCTIONS ==========

    private fun createTestTask(
        id: String = "test-task-1",
        title: String = "Test Task",
        description: String = "Test Description",
        category: TaskCategory = TaskCategory.GRAMMAR,
        priority: TaskPriority = TaskPriority.MEDIUM,
        difficulty: TaskDifficulty = TaskDifficulty.MEDIUM,
        estimatedMinutes: Int = 60,
        actualMinutes: Int = 0,
        isCompleted: Boolean = false,
        completedAt: Long? = null,
        createdAt: Long = System.currentTimeMillis(),
        dueDate: Long? = null,
        tags: List<String> = emptyList(),
        streakContribution: Boolean = true,
        pointsValue: Int = 10,
        isActive: Boolean = true,
        parentTaskId: String? = null,
        orderIndex: Int = 0,
        notes: String? = null,
        attachments: List<String> = emptyList(),
        reminderTime: Long? = null,
        isRecurring: Boolean = false,
        recurringPattern: String? = null,
        lastModified: Long = System.currentTimeMillis()
    ) = TaskEntity(
        id = id,
        title = title,
        description = description,
        category = category,
        priority = priority,
        difficulty = difficulty,
        estimatedMinutes = estimatedMinutes,
        actualMinutes = actualMinutes,
        isCompleted = isCompleted,
        completedAt = completedAt,
        createdAt = createdAt,
        dueDate = dueDate,
        tags = tags,
        streakContribution = streakContribution,
        pointsValue = pointsValue,
        isActive = isActive,
        parentTaskId = parentTaskId,
        orderIndex = orderIndex,
        notes = notes,
        attachments = attachments,
        reminderTime = reminderTime,
        isRecurring = isRecurring,
        recurringPattern = recurringPattern,
        lastModified = lastModified
    )

    // ========== CONSTRUCTOR AND DEFAULTS TESTS ==========

    @Test
    fun `TaskEntity creates with all required fields`() {
        // Act
        val task = createTestTask()

        // Assert
        assertNotNull(task)
        assertEquals("test-task-1", task.id)
        assertEquals("Test Task", task.title)
        assertEquals("Test Description", task.description)
        assertEquals(TaskCategory.GRAMMAR, task.category)
    }

    @Test
    fun `TaskEntity uses default values correctly`() {
        // Arrange & Act
        val task = TaskEntity(
            title = "Minimal Task",
            description = "Description",
            category = TaskCategory.VOCABULARY,
            priority = TaskPriority.HIGH,
            estimatedMinutes = 30
        )

        // Assert
        assertNotNull(task.id) // Auto-generated UUID
        assertEquals("Minimal Task", task.title)
        assertEquals(TaskDifficulty.MEDIUM, task.difficulty) // Default
        assertEquals(0, task.actualMinutes) // Default
        assertFalse(task.isCompleted) // Default
        assertNull(task.completedAt) // Default
        assertTrue(task.streakContribution) // Default
            assertEquals(10, task.pointsValue) // Default
        assertTrue(task.isActive) // Default
        assertNull(task.parentTaskId) // Default
        assertEquals(0, task.orderIndex) // Default
        assertNull(task.notes) // Default
        assertTrue(task.attachments.isEmpty()) // Default
        assertNull(task.reminderTime) // Default
        assertFalse(task.isRecurring) // Default
        assertNull(task.recurringPattern) // Default
    }

    // ========== IMMUTABILITY TESTS (data class copy) ==========

    @Test
    fun `TaskEntity copy creates new instance with same values`() {
        // Arrange
        val original = createTestTask(id = "task-1", title = "Original")

        // Act
        val copy = original.copy()

        // Assert
        assertEquals(original, copy)
        assertEquals(original.id, copy.id)
        assertEquals(original.title, copy.title)
        assertTrue(original === copy == false) // Different instances
    }

    @Test
    fun `TaskEntity copy allows field override`() {
        // Arrange
        val original = createTestTask(id = "task-1", title = "Original")

        // Act
        val modified = original.copy(title = "Modified")

        // Assert
        assertEquals("task-1", modified.id) // Unchanged
        assertEquals("Modified", modified.title) // Changed
        assertNotNull(original) // Original unchanged
        assertEquals("Original", original.title)
    }

    @Test
    fun `TaskEntity copy with completion update`() {
        // Arrange
        val original = createTestTask(isCompleted = false, completedAt = null)
        val completionTime = System.currentTimeMillis()

        // Act
        val completed = original.copy(
            isCompleted = true,
            completedAt = completionTime,
            actualMinutes = 45
        )

        // Assert
        assertFalse(original.isCompleted) // Original unchanged
        assertTrue(completed.isCompleted)
        assertNull(original.completedAt)
        assertEquals(completionTime, completed.completedAt)
        assertEquals(45, completed.actualMinutes)
    }

    // ========== FIELD VALIDATION TESTS ==========

    @Test
    fun `TaskEntity handles empty title`() {
        // Act
        val task = createTestTask(title = "")

        // Assert
        assertEquals("", task.title)
    }

    @Test
    fun `TaskEntity handles very long title`() {
        // Arrange
        val longTitle = "A".repeat(500)

        // Act
        val task = createTestTask(title = longTitle)

        // Assert
        assertEquals(longTitle, task.title)
        assertEquals(500, task.title.length)
    }

    @Test
    fun `TaskEntity handles unicode characters in title and description`() {
        // Arrange
        val unicodeTitle = "Turkish: Türkçe, Arabic: العربية, Chinese: 中文"
        val unicodeDesc = "Emoji: 🎓 📚 ✅"

        // Act
        val task = createTestTask(title = unicodeTitle, description = unicodeDesc)

        // Assert
        assertEquals(unicodeTitle, task.title)
        assertEquals(unicodeDesc, task.description)
    }

    @Test
    fun `TaskEntity handles special characters in title`() {
        // Arrange
        val specialTitle = "Test@#$%^&*()_+-=[]{}|;':\",./<>?"

        // Act
        val task = createTestTask(title = specialTitle)

        // Assert
        assertEquals(specialTitle, task.title)
    }

    @Test
    fun `TaskEntity handles newlines and tabs in description`() {
        // Arrange
        val multilineDesc = "Line 1\nLine 2\nLine 3\nWith\tTabs"

        // Act
        val task = createTestTask(description = multilineDesc)

        // Assert
        assertEquals(multilineDesc, task.description)
    }

    // ========== CATEGORY AND DIFFICULTY TESTS ==========

    @Test
    fun `TaskEntity supports all task categories`() {
        // Assert - verify all categories can be used
        TaskCategory.values().forEach { category ->
            val task = createTestTask(category = category)
            assertEquals(category, task.category)
        }
    }

    @Test
    fun `TaskEntity supports all task difficulties`() {
        // Assert - verify all difficulties can be used
        TaskDifficulty.values().forEach { difficulty ->
            val task = createTestTask(difficulty = difficulty)
            assertEquals(difficulty, task.difficulty)
        }
    }

    @Test
    fun `TaskEntity supports all task priorities`() {
        // Assert - verify all priorities can be used
        TaskPriority.values().forEach { priority ->
            val task = createTestTask(priority = priority)
            assertEquals(priority, task.priority)
        }
    }

    // ========== COMPLETION STATE TESTS ==========

    @Test
    fun `TaskEntity tracks completion state correctly`() {
        // Arrange
        val incompletedTask = createTestTask(isCompleted = false, completedAt = null)

        // Act
        val completedTask = incompletedTask.copy(
            isCompleted = true,
            completedAt = 1000L
        )

        // Assert
        assertFalse(incompletedTask.isCompleted)
        assertNull(incompletedTask.completedAt)
        assertTrue(completedTask.isCompleted)
        assertNotNull(completedTask.completedAt)
    }

    @Test
    fun `TaskEntity can store completion timestamp`() {
        // Arrange
        val completionTime = System.currentTimeMillis()

        // Act
        val task = createTestTask(
            isCompleted = true,
            completedAt = completionTime
        )

        // Assert
        assertEquals(completionTime, task.completedAt)
    }

    // ========== TIME TRACKING TESTS ==========

    @Test
    fun `TaskEntity tracks estimated and actual minutes`() {
        // Act
        val task = createTestTask(
            estimatedMinutes = 60,
            actualMinutes = 45
        )

        // Assert
        assertEquals(60, task.estimatedMinutes)
        assertEquals(45, task.actualMinutes)
    }

    @Test
    fun `TaskEntity handles zero minutes`() {
        // Act
        val task = createTestTask(estimatedMinutes = 0, actualMinutes = 0)

        // Assert
        assertEquals(0, task.estimatedMinutes)
        assertEquals(0, task.actualMinutes)
    }

    @Test
    fun `TaskEntity handles large minute values`() {
        // Act
        val task = createTestTask(
            estimatedMinutes = 10000,
            actualMinutes = 5000
        )

        // Assert
        assertEquals(10000, task.estimatedMinutes)
        assertEquals(5000, task.actualMinutes)
    }

    // ========== TAGS AND ATTACHMENTS TESTS ==========

    @Test
    fun `TaskEntity can store multiple tags`() {
        // Arrange
        val tags = listOf("grammar", "exercise", "important")

        // Act
        val task = createTestTask(tags = tags)

        // Assert
        assertEquals(tags, task.tags)
        assertEquals(3, task.tags.size)
    }

    @Test
    fun `TaskEntity handles empty tags list`() {
        // Act
        val task = createTestTask(tags = emptyList())

        // Assert
        assertTrue(task.tags.isEmpty())
    }

    @Test
    fun `TaskEntity can store multiple attachments`() {
        // Arrange
        val attachments = listOf("file1.pdf", "file2.jpg", "file3.doc")

        // Act
        val task = createTestTask(attachments = attachments)

        // Assert
        assertEquals(attachments, task.attachments)
        assertEquals(3, task.attachments.size)
    }

    @Test
    fun `TaskEntity handles empty attachments list`() {
        // Act
        val task = createTestTask(attachments = emptyList())

        // Assert
        assertTrue(task.attachments.isEmpty())
    }

    // ========== HIERARCHY TESTS ==========

    @Test
    fun `TaskEntity can have parent task ID for subtasks`() {
        // Arrange
        val parentId = "parent-task-123"

        // Act
        val subtask = createTestTask(parentTaskId = parentId)

        // Assert
        assertEquals(parentId, subtask.parentTaskId)
    }

    @Test
    fun `TaskEntity with no parent has null parentTaskId`() {
        // Act
        val task = createTestTask(parentTaskId = null)

        // Assert
        assertNull(task.parentTaskId)
    }

    // ========== RECURRENCE TESTS ==========

    @Test
    fun `TaskEntity tracks recurrence configuration`() {
        // Act
        val recurringTask = createTestTask(
            isRecurring = true,
            recurringPattern = "DAILY"
        )

        // Assert
        assertTrue(recurringTask.isRecurring)
        assertEquals("DAILY", recurringTask.recurringPattern)
    }

    @Test
    fun `TaskEntity non-recurring has null pattern`() {
        // Act
        val task = createTestTask(isRecurring = false)

        // Assert
        assertFalse(task.isRecurring)
        assertNull(task.recurringPattern)
    }

    @Test
    fun `TaskEntity supports various recurrence patterns`() {
        // Arrange
        val patterns = listOf("DAILY", "WEEKLY", "MONTHLY", "YEARLY")

        // Assert
        patterns.forEach { pattern ->
            val task = createTestTask(isRecurring = true, recurringPattern = pattern)
            assertEquals(pattern, task.recurringPattern)
        }
    }

    // ========== STREAK CONTRIBUTION TESTS ==========

    @Test
    fun `TaskEntity tracks streak contribution`() {
        // Act
        val streakTask = createTestTask(streakContribution = true)
        val nonStreakTask = createTestTask(streakContribution = false)

        // Assert
        assertTrue(streakTask.streakContribution)
        assertFalse(nonStreakTask.streakContribution)
    }

    // ========== POINTS VALUE TESTS ==========

    @Test
    fun `TaskEntity stores points value`() {
        // Act
        val task = createTestTask(pointsValue = 250)

        // Assert
        assertEquals(250, task.pointsValue)
    }

    @Test
    fun `TaskEntity handles zero points`() {
        // Act
        val task = createTestTask(pointsValue = 0)

        // Assert
        assertEquals(0, task.pointsValue)
    }

    @Test
    fun `TaskEntity handles large points values`() {
        // Act
        val task = createTestTask(pointsValue = 10000)

        // Assert
        assertEquals(10000, task.pointsValue)
    }

    // ========== ACTIVE STATE TESTS ==========

    @Test
    fun `TaskEntity tracks active state`() {
        // Act
        val activeTask = createTestTask(isActive = true)
        val inactiveTask = createTestTask(isActive = false)

        // Assert
        assertTrue(activeTask.isActive)
        assertFalse(inactiveTask.isActive)
    }

    // ========== DATE TESTS ==========

    @Test
    fun `TaskEntity stores creation and modification timestamps`() {
        // Arrange
        val createdAt = System.currentTimeMillis()
        val lastModified = System.currentTimeMillis() + 1000

        // Act
        val task = createTestTask(createdAt = createdAt, lastModified = lastModified)

        // Assert
        assertEquals(createdAt, task.createdAt)
        assertEquals(lastModified, task.lastModified)
    }

    @Test
    fun `TaskEntity can have due date`() {
        // Arrange
        val dueDate = System.currentTimeMillis() + 86400000L // Tomorrow

        // Act
        val task = createTestTask(dueDate = dueDate)

        // Assert
        assertNotNull(task.dueDate)
        assertEquals(dueDate, task.dueDate)
    }

    @Test
    fun `TaskEntity can have reminder time`() {
        // Arrange
        val reminderTime = System.currentTimeMillis() + 3600000L // 1 hour later

        // Act
        val task = createTestTask(reminderTime = reminderTime)

        // Assert
        assertNotNull(task.reminderTime)
        assertEquals(reminderTime, task.reminderTime)
    }

    @Test
    fun `TaskEntity without dates has null values`() {
        // Act
        val task = createTestTask(dueDate = null, reminderTime = null)

        // Assert
        assertNull(task.dueDate)
        assertNull(task.reminderTime)
    }

    // ========== ORDERING TESTS ==========

    @Test
    fun `TaskEntity stores order index`() {
        // Act
        val task1 = createTestTask(orderIndex = 0)
        val task2 = createTestTask(orderIndex = 5)
        val task3 = createTestTask(orderIndex = 10)

        // Assert
        assertEquals(0, task1.orderIndex)
        assertEquals(5, task2.orderIndex)
        assertEquals(10, task3.orderIndex)
    }

    // ========== NOTES TESTS ==========

    @Test
    fun `TaskEntity can store notes`() {
        // Arrange
        val notes = "Important: Remember to review conjugations"

        // Act
        val task = createTestTask(notes = notes)

        // Assert
        assertEquals(notes, task.notes)
    }

    @Test
    fun `TaskEntity with no notes has null value`() {
        // Act
        val task = createTestTask(notes = null)

        // Assert
        assertNull(task.notes)
    }

    @Test
    fun `TaskEntity handles long notes`() {
        // Arrange
        val longNotes = "Note ".repeat(100) // 500 characters

        // Act
        val task = createTestTask(notes = longNotes)

        // Assert
        assertEquals(longNotes, task.notes)
    }

    // ========== EQUALITY TESTS ==========

    @Test
    fun `TaskEntity equality based on all fields`() {
        // Arrange
        val task1 = createTestTask(
            id = "task-1",
            title = "Test Task",
            isCompleted = false
        )
        val task2 = createTestTask(
            id = "task-1",
            title = "Test Task",
            isCompleted = false
        )
        val task3 = createTestTask(
            id = "task-1",
            title = "Different Title",
            isCompleted = false
        )

        // Assert
        assertEquals(task1, task2)
        assertTrue(task1 == task2)
        assertTrue(task1 != task3)
    }

    @Test
    fun `TaskEntity hash code consistent`() {
        // Arrange
        val task1 = createTestTask(id = "task-1")
        val task2 = createTestTask(id = "task-1")

        // Assert
        assertEquals(task1.hashCode(), task2.hashCode())
    }

    // ========== EDGE CASE TESTS ==========

    @Test
    fun `TaskEntity with all minimum values`() {
        // Act
        val task = TaskEntity(
            title = "",
            description = "",
            category = TaskCategory.VOCABULARY,
            priority = TaskPriority.LOW,
            estimatedMinutes = 0,
            pointsValue = 0
        )

        // Assert
        assertEquals("", task.title)
        assertEquals(0, task.estimatedMinutes)
        assertEquals(0, task.pointsValue)
    }

    @Test
    fun `TaskEntity with maximum reasonable values`() {
        // Act
        val task = createTestTask(
            title = "T".repeat(200),
            estimatedMinutes = 999,
            pointsValue = 9999,
            orderIndex = 999
        )

        // Assert
        assertEquals(200, task.title.length)
        assertEquals(999, task.estimatedMinutes)
        assertEquals(9999, task.pointsValue)
        assertEquals(999, task.orderIndex)
    }

    @Test
    fun `TaskEntity toString representation`() {
        // Arrange
        val task = createTestTask(id = "task-1", title = "Test")

        // Act
        val stringRepresentation = task.toString()

        // Assert
        assertNotNull(stringRepresentation)
        assertTrue(stringRepresentation.contains("TaskEntity"))
        assertTrue(stringRepresentation.contains("task-1") || stringRepresentation.contains("Test"))
    }
}
