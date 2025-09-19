package com.mtlc.studyplan.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

interface TaskRepository {
    fun getAllTasks(): Flow<List<Task>>
    suspend fun getAllTasksSync(): List<Task>
    suspend fun getTaskById(id: String): Task?
    suspend fun insertTask(task: Task): Task
    suspend fun updateTask(task: Task): Task
    suspend fun deleteTask(id: String)
    suspend fun getTodaysTasks(): List<Task>
    suspend fun getUpcomingTasks(): List<Task>
    suspend fun getTasksByCategory(category: String): List<Task>
    suspend fun getEarlyMorningCompletedTasks(): List<Task>
}

@Singleton
class TaskRepositoryImpl @Inject constructor() : TaskRepository {

    private val _tasks = MutableStateFlow<List<Task>>(generateSampleTasks())
    private val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    override fun getAllTasks(): Flow<List<Task>> {
        return tasks
    }

    override suspend fun getAllTasksSync(): List<Task> {
        return _tasks.value
    }

    override suspend fun getTaskById(id: String): Task? {
        return _tasks.value.find { it.id == id }
    }

    override suspend fun insertTask(task: Task): Task {
        val currentTasks = _tasks.value.toMutableList()
        currentTasks.add(task)
        _tasks.value = currentTasks
        return task
    }

    override suspend fun updateTask(task: Task): Task {
        val currentTasks = _tasks.value.toMutableList()
        val index = currentTasks.indexOfFirst { it.id == task.id }
        if (index != -1) {
            currentTasks[index] = task
            _tasks.value = currentTasks
        }
        return task
    }

    override suspend fun deleteTask(id: String) {
        val currentTasks = _tasks.value.toMutableList()
        currentTasks.removeAll { it.id == id }
        _tasks.value = currentTasks
    }

    override suspend fun getTodaysTasks(): List<Task> {
        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)
        val todayStart = today.timeInMillis

        today.add(Calendar.DAY_OF_MONTH, 1)
        val tomorrowStart = today.timeInMillis

        return _tasks.value.filter { task ->
            task.dueDate?.let { dueDate ->
                dueDate >= todayStart && dueDate < tomorrowStart
            } ?: false
        }
    }

    override suspend fun getUpcomingTasks(): List<Task> {
        val now = System.currentTimeMillis()
        return _tasks.value.filter { task ->
            !task.isCompleted && task.dueDate != null && task.dueDate > now
        }.sortedBy { it.dueDate }
    }

    override suspend fun getTasksByCategory(category: String): List<Task> {
        return _tasks.value.filter { it.category == category }
    }

    override suspend fun getEarlyMorningCompletedTasks(): List<Task> {
        return _tasks.value.filter { task ->
            task.isCompleted && task.completedAt != null && isEarlyMorning(task.completedAt)
        }
    }

    private fun isEarlyMorning(timestamp: Long): Boolean {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        return hour < 9 // Before 9 AM
    }

    private fun generateSampleTasks(): List<Task> {
        val now = System.currentTimeMillis()
        val oneHour = 60 * 60 * 1000L
        val oneDay = 24 * oneHour

        return listOf(
            Task(
                id = "1",
                title = "Complete Algebra homework Ch. 5",
                description = "Solve problems 1-20 on quadratic equations",
                category = "Mathematics",
                priority = TaskPriority.HIGH,
                dueDate = now + oneDay,
                estimatedTime = 60,
                tags = listOf("homework", "algebra")
            ),
            Task(
                id = "2",
                title = "Read Pride and Prejudice Ch. 1-3",
                description = "Read and take notes for upcoming discussion",
                category = "English Literature",
                priority = TaskPriority.MEDIUM,
                dueDate = now + 2 * oneDay,
                estimatedTime = 90,
                tags = listOf("reading", "literature")
            ),
            Task(
                id = "3",
                title = "Lab report: Chemical reactions",
                description = "Write up findings from Wednesday's chemistry lab",
                category = "Science",
                priority = TaskPriority.HIGH,
                dueDate = now + 3 * oneDay,
                estimatedTime = 120,
                tags = listOf("lab", "chemistry")
            ),
            Task(
                id = "4",
                title = "Study for Biology test on cells",
                description = "Review chapters 4-6 and practice diagrams",
                category = "Science",
                priority = TaskPriority.MEDIUM,
                dueDate = now + 4 * oneDay,
                estimatedTime = 150,
                tags = listOf("study", "biology", "test")
            ),
            Task(
                id = "5",
                title = "Research project: World War II",
                description = "Gather sources and create outline",
                category = "History",
                priority = TaskPriority.LOW,
                dueDate = now + 7 * oneDay,
                estimatedTime = 180,
                tags = listOf("research", "history", "project")
            ),
            Task(
                id = "6",
                title = "Practice integration problems",
                description = "Complete practice set from textbook",
                category = "Mathematics",
                priority = TaskPriority.MEDIUM,
                isCompleted = true,
                completedAt = now - oneDay,
                estimatedTime = 45,
                tags = listOf("practice", "calculus")
            ),
            Task(
                id = "7",
                title = "Review vocabulary for quiz",
                description = "Spanish vocabulary list for Friday quiz",
                category = "Language",
                priority = TaskPriority.MEDIUM,
                dueDate = now + oneDay,
                estimatedTime = 30,
                tags = listOf("vocabulary", "spanish", "quiz")
            ),
            Task(
                id = "8",
                title = "Code practice: Python loops",
                description = "Complete HackerRank loop challenges",
                category = "Computer Science",
                priority = TaskPriority.LOW,
                dueDate = now + 5 * oneDay,
                estimatedTime = 75,
                tags = listOf("coding", "python", "practice")
            ),
            Task(
                id = "9",
                title = "Write essay draft: Shakespeare analysis",
                description = "First draft of Hamlet character analysis",
                category = "English Literature",
                priority = TaskPriority.HIGH,
                dueDate = now + 6 * oneDay,
                estimatedTime = 180,
                tags = listOf("essay", "shakespeare", "analysis")
            ),
            Task(
                id = "10",
                title = "Physics problem set #7",
                description = "Momentum and energy conservation problems",
                category = "Science",
                priority = TaskPriority.MEDIUM,
                isCompleted = true,
                completedAt = now - 2 * oneDay,
                estimatedTime = 90,
                tags = listOf("physics", "problems", "homework")
            )
        )
    }
}