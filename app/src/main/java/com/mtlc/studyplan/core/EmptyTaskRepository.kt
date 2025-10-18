package com.mtlc.studyplan.core

import com.mtlc.studyplan.data.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Lightweight in-memory fallback used when dependency injection cannot provide
 * an actual `TaskRepository`. Ensures UI previews and screens can render without crashes.
 */
object EmptyTaskRepository : TaskRepository {
    override fun getAllTasks(): Flow<List<com.mtlc.studyplan.data.Task>> = flowOf(emptyList())
    override suspend fun getAllTasksSync(): List<com.mtlc.studyplan.data.Task> = emptyList()
    override suspend fun getTaskById(id: String): com.mtlc.studyplan.data.Task? = null
    override suspend fun insertTask(task: com.mtlc.studyplan.data.Task): com.mtlc.studyplan.data.Task = task
    override suspend fun updateTask(task: com.mtlc.studyplan.data.Task): com.mtlc.studyplan.data.Task = task
    override suspend fun deleteTask(id: String) {
        // No-op for empty repository
    }
    override suspend fun getTodaysTasks(): List<com.mtlc.studyplan.data.Task> = emptyList()
    override suspend fun getUpcomingTasks(): List<com.mtlc.studyplan.data.Task> = emptyList()
    override suspend fun getTasksByCategory(category: String): List<com.mtlc.studyplan.data.Task> = emptyList()
    override suspend fun getEarlyMorningCompletedTasks(): List<com.mtlc.studyplan.data.Task> = emptyList()
    override suspend fun getLateNightCompletedTasks(): List<com.mtlc.studyplan.data.Task> = emptyList()
    override suspend fun getWeekendCompletedTasks(): List<com.mtlc.studyplan.data.Task> = emptyList()
    override suspend fun getWeekdayCompletedTasks(): List<com.mtlc.studyplan.data.Task> = emptyList()
    override suspend fun getTasksByPriority(priority: com.mtlc.studyplan.data.TaskPriority): List<com.mtlc.studyplan.data.Task> = emptyList()
    override suspend fun getTotalPointsEarned(): Int = 0
    override suspend fun getMaxTasksCompletedInOneDay(): Int = 0

    override suspend fun getAllTasksPaginated(page: Int, pageSize: Int): TaskRepository.PaginatedTasks =
        TaskRepository.PaginatedTasks(emptyList(), 0, 0, 0, hasNextPage = false, hasPreviousPage = false)

    override suspend fun getCompletedTasksPaginated(page: Int, pageSize: Int): TaskRepository.PaginatedTasks =
        TaskRepository.PaginatedTasks(emptyList(), 0, 0, 0, hasNextPage = false, hasPreviousPage = false)

    override suspend fun getPendingTasksPaginated(page: Int, pageSize: Int): TaskRepository.PaginatedTasks =
        TaskRepository.PaginatedTasks(emptyList(), 0, 0, 0, hasNextPage = false, hasPreviousPage = false)
}
