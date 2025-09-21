package com.mtlc.studyplan.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

        // YDS-focused English tasks only
        return listOf(
            Task(
                id = "yds-1",
                title = "YDS Vocabulary: Academic Words Set 1",
                description = "Study 30 high-frequency YDS academic words",
                category = "YDS Vocabulary",
                priority = TaskPriority.HIGH,
                dueDate = now + oneDay,
                estimatedTime = 40,
                tags = listOf("yds", "english", "vocabulary")
            ),
            Task(
                id = "yds-2",
                title = "YDS Grammar: Tenses & Modals",
                description = "Targeted practice on mixed tenses and modal verbs",
                category = "YDS Grammar",
                priority = TaskPriority.MEDIUM,
                dueDate = now + 2 * oneDay,
                estimatedTime = 60,
                tags = listOf("yds", "english", "grammar")
            ),
            Task(
                id = "yds-3",
                title = "YDS Reading: Inference Questions",
                description = "Solve 10 inference questions from past papers",
                category = "YDS Reading",
                priority = TaskPriority.MEDIUM,
                dueDate = now + 3 * oneDay,
                estimatedTime = 45,
                tags = listOf("yds", "english", "reading")
            ),
            Task(
                id = "yds-4",
                title = "YDS Cloze Test Practice",
                description = "Complete 2 cloze tests focusing on connectors",
                category = "YDS Grammar",
                priority = TaskPriority.HIGH,
                dueDate = now + 4 * oneDay,
                estimatedTime = 50,
                tags = listOf("yds", "english", "grammar", "cloze")
            ),
            Task(
                id = "yds-5",
                title = "YDS Listening: Short Talks",
                description = "Practice 3 short talks and answer 15 questions",
                category = "YDS Listening",
                priority = TaskPriority.LOW,
                dueDate = now + 5 * oneDay,
                estimatedTime = 35,
                tags = listOf("yds", "english", "listening")
            ),
            Task(
                id = "yds-6",
                title = "YDS Sentence Completion",
                description = "Solve 15 sentence completion items (B1-B2)",
                category = "YDS Reading",
                priority = TaskPriority.MEDIUM,
                isCompleted = true,
                completedAt = now - oneDay,
                estimatedTime = 40,
                tags = listOf("yds", "english", "reading", "completion")
            ),
            Task(
                id = "yds-7",
                title = "YDS Paraphrasing Practice",
                description = "Paraphrase 20 sentences using target structures",
                category = "YDS Grammar",
                priority = TaskPriority.MEDIUM,
                dueDate = now + oneDay,
                estimatedTime = 30,
                tags = listOf("yds", "english", "grammar", "paraphrase")
            ),
            Task(
                id = "yds-8",
                title = "YDS Vocabulary: Collocations Set 2",
                description = "Memorize and test 25 key collocations",
                category = "YDS Vocabulary",
                priority = TaskPriority.LOW,
                dueDate = now + 2 * oneDay,
                estimatedTime = 30,
                tags = listOf("yds", "english", "vocabulary", "collocation")
            ),
            Task(
                id = "yds-9",
                title = "YDS Reading: Paragraph Completion",
                description = "Solve 8 paragraph completion questions",
                category = "YDS Reading",
                priority = TaskPriority.HIGH,
                dueDate = now + 3 * oneDay,
                estimatedTime = 50,
                tags = listOf("yds", "english", "reading", "paragraph")
            ),
            Task(
                id = "yds-10",
                title = "YDS Full Practice Exam (1)",
                description = "Complete a full mock exam and review mistakes",
                category = "YDS Practice Exam",
                priority = TaskPriority.MEDIUM,
                isCompleted = true,
                completedAt = now - 2 * oneDay,
                estimatedTime = 120,
                tags = listOf("yds", "english", "exam")
            )
        )
    }
}
