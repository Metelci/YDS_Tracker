package com.mtlc.studyplan.testutil

import com.mtlc.studyplan.database.entities.TaskEntity
import com.mtlc.studyplan.shared.TaskCategory
import com.mtlc.studyplan.shared.TaskDifficulty
import com.mtlc.studyplan.shared.TaskPriority
import java.util.*

/**
 * Test Data Factory
 * Provides consistent, realistic test data for all test scenarios
 */
object TestDataFactory {

    private val random = Random(42) // Fixed seed for reproducible tests

    // Base timestamps for consistent test data
    private const val BASE_TIMESTAMP = 1700000000000L // 2023-11-14

    /**
     * Create a single test task with customizable parameters
     */
    fun createTestTask(
        id: String = "task_${UUID.randomUUID()}",
        title: String = "Test Task",
        description: String = "Test task description",
        category: TaskCategory = TaskCategory.GRAMMAR,
        priority: TaskPriority = TaskPriority.MEDIUM,
        difficulty: TaskDifficulty = TaskDifficulty.MEDIUM,
        estimatedMinutes: Int = 30,
        actualMinutes: Int = 0,
        isCompleted: Boolean = false,
        completedAt: Long? = null,
        createdAt: Long = BASE_TIMESTAMP + random.nextInt(86400000), // Random time within 24h
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
        lastModified: Long = createdAt
    ): TaskEntity {
        return TaskEntity(
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
    }

    /**
     * Create a task with dependencies
     */
    fun createTaskWithDependencies(
        id: String,
        title: String,
        dependencies: List<String> = emptyList(),
        category: TaskCategory = TaskCategory.GRAMMAR
    ): TaskEntity {
        val tags = if (dependencies.isNotEmpty()) {
            dependencies.map { "dependency:$it" }
        } else emptyList()

        return createTestTask(
            id = id,
            title = title,
            category = category,
            tags = tags
        )
    }

    /**
     * Create a completed task
     */
    fun createCompletedTask(
        id: String,
        title: String,
        completedAt: Long = BASE_TIMESTAMP + random.nextInt(86400000),
        actualMinutes: Int = 25 + random.nextInt(20) // 25-45 minutes
    ): TaskEntity {
        return createTestTask(
            id = id,
            title = title,
            isCompleted = true,
            completedAt = completedAt,
            actualMinutes = actualMinutes,
            lastModified = completedAt
        )
    }

    /**
     * Create a pending task
     */
    fun createPendingTask(
        id: String,
        title: String,
        dueDate: Long? = BASE_TIMESTAMP + 86400000L + random.nextInt(604800000) // Next 1-7 days
    ): TaskEntity {
        return createTestTask(
            id = id,
            title = title,
            isCompleted = false,
            dueDate = dueDate
        )
    }

    /**
     * Create a high-priority urgent task
     */
    fun createUrgentTask(
        id: String,
        title: String,
        dueDate: Long = BASE_TIMESTAMP + 3600000L + random.nextInt(21600000) // Next 1-6 hours
    ): TaskEntity {
        return createTestTask(
            id = id,
            title = title,
            priority = TaskPriority.HIGH,
            dueDate = dueDate,
            estimatedMinutes = 15 + random.nextInt(30) // 15-45 minutes
        )
    }

    /**
     * Create a complete study plan with realistic tasks
     */
    fun createCompleteStudyPlan(): List<TaskEntity> {
        val tasks = mutableListOf<TaskEntity>()
        var taskIndex = 1

        // Grammar section
        tasks.addAll(createCategoryTasks("grammar", TaskCategory.GRAMMAR, 8, taskIndex))
        taskIndex += 8

        // Vocabulary section
        tasks.addAll(createCategoryTasks("vocab", TaskCategory.VOCABULARY, 6, taskIndex))
        taskIndex += 6

        // Reading section
        tasks.addAll(createCategoryTasks("reading", TaskCategory.READING, 5, taskIndex))
        taskIndex += 5

        // Listening section
        tasks.addAll(createCategoryTasks("listening", TaskCategory.LISTENING, 4, taskIndex))
        taskIndex += 4

        // Other section
        tasks.addAll(createCategoryTasks("other", TaskCategory.OTHER, 3, taskIndex))

        return tasks
    }

    private fun createCategoryTasks(
        prefix: String,
        category: TaskCategory,
        count: Int,
        startIndex: Int
    ): List<TaskEntity> {
        return (0 until count).map { index ->
            val taskNumber = startIndex + index
            val isCompleted = random.nextFloat() < 0.3f // 30% completion rate
            val priority = when {
                taskNumber <= 5 -> TaskPriority.HIGH
                taskNumber <= 15 -> TaskPriority.MEDIUM
                else -> TaskPriority.LOW
            }

            createTestTask(
                id = "${prefix}_task_$taskNumber",
                title = "${category.name.lowercase().replaceFirstChar { it.uppercase() }} Task $taskNumber",
                category = category,
                priority = priority,
                estimatedMinutes = 20 + random.nextInt(40), // 20-60 minutes
                isCompleted = isCompleted,
                completedAt = if (isCompleted) BASE_TIMESTAMP + random.nextInt(86400000) else null,
                actualMinutes = if (isCompleted) 15 + random.nextInt(35) else 0,
                pointsValue = 5 + random.nextInt(15), // 5-20 points
                streakContribution = isCompleted
            )
        }
    }

    /**
     * Create tasks for a specific week
     */
    fun createWeeklyTasks(weekNumber: Int): List<TaskEntity> {
        val weekStart = BASE_TIMESTAMP + (weekNumber - 1) * 7 * 24 * 60 * 60 * 1000L
        val tasks = mutableListOf<TaskEntity>()

        // Daily tasks for the week
        for (day in 1..7) {
            val dayStart = weekStart + (day - 1) * 24 * 60 * 60 * 1000L
            val dailyTasks = 3 + random.nextInt(3) // 3-5 tasks per day

            for (taskNum in 1..dailyTasks) {
                val category = TaskCategory.entries[random.nextInt(TaskCategory.entries.size)]
                val isCompleted = random.nextFloat() < 0.4f // 40% completion rate

                tasks.add(createTestTask(
                    id = "week${weekNumber}_day${day}_task${taskNum}",
                    title = "Week $weekNumber - Day $day - ${category.name} Practice $taskNum",
                    category = category,
                    estimatedMinutes = 25 + random.nextInt(35),
                    isCompleted = isCompleted,
                    completedAt = if (isCompleted) dayStart + random.nextInt(24 * 60 * 60 * 1000) else null,
                    actualMinutes = if (isCompleted) 20 + random.nextInt(30) else 0,
                    createdAt = dayStart,
                    dueDate = dayStart + 24 * 60 * 60 * 1000L,
                    pointsValue = 8 + random.nextInt(12)
                ))
            }
        }

        return tasks
    }

    /**
     * Create a performance test dataset
     */
    fun createPerformanceTestData(size: Int): List<TaskEntity> {
        return (1..size).map { index ->
            createTestTask(
                id = "perf_task_$index",
                title = "Performance Test Task $index",
                category = TaskCategory.entries[index % TaskCategory.entries.size],
                priority = TaskPriority.entries[index % TaskPriority.entries.size],
                estimatedMinutes = 10 + random.nextInt(50),
                isCompleted = index % 3 == 0, // Every 3rd task is completed
                pointsValue = 5 + random.nextInt(20)
            )
        }
    }

    /**
     * Create tasks with specific search criteria for testing
     */
    fun createSearchableTasks(): List<TaskEntity> {
        return listOf(
            createTestTask("search_grammar_1", "Basic Grammar Rules", category = TaskCategory.GRAMMAR),
            createTestTask("search_vocab_1", "Advanced Vocabulary Building", category = TaskCategory.VOCABULARY),
            createTestTask("search_reading_1", "Reading Comprehension Practice", category = TaskCategory.READING),
            createTestTask("search_completed_1", "Completed Grammar Task", category = TaskCategory.GRAMMAR, isCompleted = true),
            createTestTask("search_pending_1", "Pending Vocabulary Task", category = TaskCategory.VOCABULARY, isCompleted = false),
            createTestTask("search_high_priority", "Urgent Grammar Review", category = TaskCategory.GRAMMAR, priority = TaskPriority.HIGH),
            createTestTask("search_low_priority", "Optional Reading Exercise", category = TaskCategory.READING, priority = TaskPriority.LOW)
        )
    }

    /**
     * Create tasks for concurrent access testing
     */
    fun createConcurrentTestTasks(threadCount: Int, tasksPerThread: Int): List<TaskEntity> {
        val tasks = mutableListOf<TaskEntity>()

        for (thread in 1..threadCount) {
            for (task in 1..tasksPerThread) {
                tasks.add(createTestTask(
                    id = "concurrent_t${thread}_task${task}",
                    title = "Concurrent Task T$thread-$task",
                    category = TaskCategory.entries[(thread + task) % TaskCategory.entries.size],
                    createdAt = BASE_TIMESTAMP + thread * 1000L + task * 100L
                ))
            }
        }

        return tasks
    }

    /**
     * Create a realistic user study session
     */
    fun createStudySession(): List<TaskEntity> {
        val sessionStart = System.currentTimeMillis()
        return listOf(
            createTestTask(
                id = "session_warmup",
                title = "Warm-up Vocabulary Review",
                category = TaskCategory.VOCABULARY,
                priority = TaskPriority.MEDIUM,
                estimatedMinutes = 10,
                createdAt = sessionStart
            ),
            createTestTask(
                id = "session_grammar",
                title = "Grammar Pattern Practice",
                category = TaskCategory.GRAMMAR,
                priority = TaskPriority.HIGH,
                estimatedMinutes = 25,
                createdAt = sessionStart + 10 * 60 * 1000L
            ),
            createTestTask(
                id = "session_reading",
                title = "Reading Comprehension",
                category = TaskCategory.READING,
                priority = TaskPriority.MEDIUM,
                estimatedMinutes = 20,
                createdAt = sessionStart + 35 * 60 * 1000L
            ),
            createTestTask(
                id = "session_listening",
                title = "Listening Practice",
                category = TaskCategory.LISTENING,
                priority = TaskPriority.MEDIUM,
                estimatedMinutes = 15,
                createdAt = sessionStart + 55 * 60 * 1000L
            )
        )
    }

    /**
     * Reset random seed for reproducible tests
     */
    fun resetRandomSeed(seed: Long = 42) {
        random.setSeed(seed)
    }

    /**
     * Get random category for variety in tests
     */
    fun randomCategory(): TaskCategory = TaskCategory.entries[random.nextInt(TaskCategory.entries.size)]

    /**
     * Get random priority for variety in tests
     */
    fun randomPriority(): TaskPriority = TaskPriority.entries[random.nextInt(TaskPriority.entries.size)]

    /**
     * Generate random realistic task title
     */
    fun randomTaskTitle(category: TaskCategory): String {
        val adjectives = listOf("Basic", "Advanced", "Intermediate", "Essential", "Practical", "Comprehensive")
        val nouns = when (category) {
            TaskCategory.GRAMMAR -> listOf("Rules", "Patterns", "Structures", "Tenses", "Sentences")
            TaskCategory.VOCABULARY -> listOf("Words", "Terms", "Phrases", "Expressions", "Vocabulary")
            TaskCategory.READING -> listOf("Comprehension", "Passages", "Texts", "Articles", "Stories")
            TaskCategory.LISTENING -> listOf("Audio", "Conversations", "Lectures", "Podcasts", "Dialogues")
            TaskCategory.OTHER -> listOf("Tasks", "Activities", "Exercises", "Practice", "Drills")
        }

        val adjective = adjectives[random.nextInt(adjectives.size)]
        val noun = nouns[random.nextInt(nouns.size)]

        return "$adjective $noun Practice"
    }
}