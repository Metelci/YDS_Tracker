package com.mtlc.studyplan.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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

    // Pagination support for large datasets
    data class PaginatedTasks(
        val tasks: List<Task>,
        val totalCount: Int,
        val currentPage: Int,
        val totalPages: Int,
        val hasNextPage: Boolean,
        val hasPreviousPage: Boolean
    )

    suspend fun getAllTasksPaginated(page: Int = 0, pageSize: Int = 50): PaginatedTasks
    suspend fun getCompletedTasksPaginated(page: Int = 0, pageSize: Int = 50): PaginatedTasks
    suspend fun getPendingTasksPaginated(page: Int = 0, pageSize: Int = 50): PaginatedTasks
}

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val databaseTaskRepository: com.mtlc.studyplan.repository.TaskRepository
) : TaskRepository {

    override fun getAllTasks(): Flow<List<Task>> {
        return databaseTaskRepository.allTasks.map { entities ->
            entities.map { entity -> entity.toTask() }
        }
    }

    override suspend fun getAllTasksSync(): List<Task> {
        return databaseTaskRepository.allTasks.first().map { it.toTask() }
    }

    override suspend fun getTaskById(id: String): Task? {
        return databaseTaskRepository.getTaskById(id)?.toTask()
    }

    override suspend fun insertTask(task: Task): Task {
        databaseTaskRepository.insertTask(task.toTaskEntity())
        return task
    }

    override suspend fun updateTask(task: Task): Task {
        databaseTaskRepository.updateTask(task.toTaskEntity())
        return task
    }

    override suspend fun deleteTask(id: String) {
        databaseTaskRepository.deleteTask(id)
    }

    override suspend fun getTodaysTasks(): List<Task> {
        return databaseTaskRepository.todayTasks.first().map { it.toTask() }
    }

    override suspend fun getUpcomingTasks(): List<Task> {
        return databaseTaskRepository.upcomingTasks.first().map { it.toTask() }
    }

    override suspend fun getTasksByCategory(category: String): List<Task> {
        val taskCategory = when (category) {
            "YDS Vocabulary" -> com.mtlc.studyplan.shared.TaskCategory.VOCABULARY
            "YDS Grammar" -> com.mtlc.studyplan.shared.TaskCategory.GRAMMAR
            "YDS Reading" -> com.mtlc.studyplan.shared.TaskCategory.READING
            "YDS Listening" -> com.mtlc.studyplan.shared.TaskCategory.LISTENING
            else -> com.mtlc.studyplan.shared.TaskCategory.OTHER
        }
        return databaseTaskRepository.getTasksByCategory(taskCategory).first().map { it.toTask() }
    }

    override suspend fun getEarlyMorningCompletedTasks(): List<Task> {
        return databaseTaskRepository.allTasks.first()
            .filter { entity ->
                entity.isCompleted && entity.completedAt != null && isEarlyMorning(entity.completedAt)
            }
            .map { it.toTask() }
    }

    override suspend fun getAllTasksPaginated(page: Int, pageSize: Int): TaskRepository.PaginatedTasks {
        val result = databaseTaskRepository.getAllTasksPaginated(page, pageSize)
        return TaskRepository.PaginatedTasks(
            tasks = result.items.map { it.toTask() },
            totalCount = result.totalCount,
            currentPage = result.currentPage,
            totalPages = result.totalPages,
            hasNextPage = result.hasNextPage,
            hasPreviousPage = result.hasPreviousPage
        )
    }

    override suspend fun getCompletedTasksPaginated(page: Int, pageSize: Int): TaskRepository.PaginatedTasks {
        val result = databaseTaskRepository.getCompletedTasksPaginated(page, pageSize)
        return TaskRepository.PaginatedTasks(
            tasks = result.items.map { it.toTask() },
            totalCount = result.totalCount,
            currentPage = result.currentPage,
            totalPages = result.totalPages,
            hasNextPage = result.hasNextPage,
            hasPreviousPage = result.hasPreviousPage
        )
    }

    override suspend fun getPendingTasksPaginated(page: Int, pageSize: Int): TaskRepository.PaginatedTasks {
        val result = databaseTaskRepository.getPendingTasksPaginated(page, pageSize)
        return TaskRepository.PaginatedTasks(
            tasks = result.items.map { it.toTask() },
            totalCount = result.totalCount,
            currentPage = result.currentPage,
            totalPages = result.totalPages,
            hasNextPage = result.hasNextPage,
            hasPreviousPage = result.hasPreviousPage
        )
    }

    private fun isEarlyMorning(timestamp: Long): Boolean {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        return hour < 9 // Before 9 AM
    }

    private fun com.mtlc.studyplan.database.entities.TaskEntity.toTask(): Task {
        return Task(
            id = this.id,
            title = this.title,
            description = this.description,
            category = this.category.name,
            priority = when (this.priority) {
                com.mtlc.studyplan.shared.TaskPriority.LOW -> TaskPriority.LOW
                com.mtlc.studyplan.shared.TaskPriority.MEDIUM -> TaskPriority.MEDIUM
                com.mtlc.studyplan.shared.TaskPriority.HIGH -> TaskPriority.HIGH
                com.mtlc.studyplan.shared.TaskPriority.CRITICAL -> TaskPriority.CRITICAL
            },
            dueDate = this.dueDate,
            createdAt = this.createdAt,
            completedAt = this.completedAt,
            isCompleted = this.isCompleted,
            estimatedMinutes = this.estimatedMinutes,
            actualMinutes = if (this.actualMinutes > 0) this.actualMinutes else null,
            tags = this.tags,
            streakContribution = this.streakContribution,
            pointsValue = this.pointsValue
        )
    }

    private fun Task.toTaskEntity(): com.mtlc.studyplan.database.entities.TaskEntity {
        return com.mtlc.studyplan.database.entities.TaskEntity(
            id = this.id,
            title = this.title,
            description = this.description,
            category = when (this.category) {
                "YDS Vocabulary" -> com.mtlc.studyplan.shared.TaskCategory.VOCABULARY
                "YDS Grammar" -> com.mtlc.studyplan.shared.TaskCategory.GRAMMAR
                "YDS Reading" -> com.mtlc.studyplan.shared.TaskCategory.READING
                "YDS Listening" -> com.mtlc.studyplan.shared.TaskCategory.LISTENING
                else -> com.mtlc.studyplan.shared.TaskCategory.OTHER
            },
            priority = when (this.priority) {
                TaskPriority.LOW -> com.mtlc.studyplan.shared.TaskPriority.LOW
                TaskPriority.MEDIUM -> com.mtlc.studyplan.shared.TaskPriority.MEDIUM
                TaskPriority.HIGH -> com.mtlc.studyplan.shared.TaskPriority.HIGH
                TaskPriority.CRITICAL -> com.mtlc.studyplan.shared.TaskPriority.CRITICAL
            },
            estimatedMinutes = this.estimatedMinutes,
            actualMinutes = this.actualMinutes ?: 0,
            isCompleted = this.isCompleted,
            completedAt = this.completedAt,
            createdAt = this.createdAt,
            dueDate = this.dueDate,
            tags = this.tags,
            streakContribution = this.streakContribution,
            pointsValue = this.pointsValue
        )
    }
}
