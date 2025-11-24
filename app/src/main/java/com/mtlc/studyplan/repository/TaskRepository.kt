@file:Suppress("TooManyFunctions")
package com.mtlc.studyplan.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import com.mtlc.studyplan.database.dao.TaskDao
import com.mtlc.studyplan.database.entities.TaskEntity
import com.mtlc.studyplan.shared.TaskCategory
import com.mtlc.studyplan.shared.TaskPriority
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
    private val taskDao: TaskDao
) {

    // Reactive data flows
    private val _refreshTrigger = MutableStateFlow(0L)
    val refreshTrigger: StateFlow<Long> = _refreshTrigger.asStateFlow()

    // Core task flows
    val allTasks: Flow<List<TaskEntity>> = taskDao.getAllTasks()
    val pendingTasks: Flow<List<TaskEntity>> = taskDao.getPendingTasks()
    val completedTasks: Flow<List<TaskEntity>> = taskDao.getCompletedTasks()
    // Updated to use optimized query with parameters
    val todayTasks: Flow<List<TaskEntity>> = run {
        val (startOfDay, endOfDay) = todayBounds()
        taskDao.getTodayTasks(startOfDay, endOfDay)
    }
    val tomorrowTasks: Flow<List<TaskEntity>> = run {
        val (startOfTomorrow, endOfTomorrow) = tomorrowBounds()
        taskDao.getTomorrowTasks(startOfTomorrow, endOfTomorrow)
    }
    val upcomingTasks: Flow<List<TaskEntity>> = run {
        val (start, end) = upcomingRangeBounds()
        taskDao.getUpcomingTasks(start, end)
    }
    val overdueTasks: Flow<List<TaskEntity>> = taskDao.getOverdueTasks()

    // Analytics flows
    val totalTaskCount: Flow<Int> = allTasks.map { it.size }
    val pendingTaskCount: Flow<Int> = pendingTasks.map { it.size }
    val completedTaskCount: Flow<Int> = completedTasks.map { it.size }
    val overdueTaskCount: Flow<Int> = overdueTasks.map { it.size }

    // Progress flows
    val todayProgress: Flow<TaskProgress> = combine(
        todayTasks,
        pendingTasks,
        completedTasks
    ) { today, pending, completed ->
        val todayCompleted = today.count { it.isCompleted }
        val todayTotal = today.size
        TaskProgress(
            completedToday = todayCompleted,
            totalToday = todayTotal,
            pendingTotal = pending.size,
            completedTotal = completed.size,
            completionRate = if (todayTotal > 0) (todayCompleted.toFloat() / todayTotal) * 100 else 0f
        )
    }

    // Category flows
    val tasksByCategory: Flow<Map<TaskCategory, List<TaskEntity>>> = allTasks.map { tasks ->
        tasks.groupBy { it.category }
    }

    val categoryProgress: Flow<Map<TaskCategory, CategoryProgress>> = tasksByCategory.map { categoryMap ->
        categoryMap.mapValues { (_, tasks) ->
            val completed = tasks.count { it.isCompleted }
            val total = tasks.size
            CategoryProgress(
                completed = completed,
                total = total,
                percentage = if (total > 0) (completed.toFloat() / total) * 100 else 0f
            )
        }
    }

    // Task operations
    suspend fun getTaskById(taskId: String): TaskEntity? = taskDao.getTaskById(taskId)

    suspend fun insertTask(task: TaskEntity) {
        taskDao.insertTask(task)
        triggerRefresh()
    }

    suspend fun insertTasks(tasks: List<TaskEntity>) {
        taskDao.insertTasks(tasks)
        triggerRefresh()
    }

    suspend fun updateTask(task: TaskEntity) {
        taskDao.updateTask(task)
        triggerRefresh()
    }

    suspend fun updateTasks(tasks: List<TaskEntity>) {
        taskDao.updateTasks(tasks)
        triggerRefresh()
    }

    suspend fun completeTask(taskId: String, actualMinutes: Int = 0) {
        val completedAt = System.currentTimeMillis()
        taskDao.updateTaskCompletion(taskId, true, completedAt, actualMinutes)
        triggerRefresh()
    }

    suspend fun uncompleteTask(taskId: String) {
        taskDao.updateTaskCompletion(taskId, false, null, 0)
        triggerRefresh()
    }

    suspend fun deleteTask(taskId: String) {
        taskDao.deleteTask(taskId)
        triggerRefresh()
    }

    suspend fun deleteTasks(taskIds: List<String>) {
        taskDao.deleteTasks(taskIds)
        triggerRefresh()
    }

    suspend fun permanentlyDeleteTask(taskId: String) {
        taskDao.permanentlyDeleteTask(taskId)
        triggerRefresh()
    }

    // Category and priority operations
    fun getTasksByCategory(category: TaskCategory): Flow<List<TaskEntity>> =
        taskDao.getTasksByCategory(category)

    fun getTasksByPriority(priority: TaskPriority): Flow<List<TaskEntity>> =
        taskDao.getTasksByPriority(priority)

    suspend fun moveTaskToCategory(taskId: String, newCategory: TaskCategory) {
        taskDao.moveTaskToCategory(taskId, newCategory)
        triggerRefresh()
    }

    suspend fun reorderTasks(tasks: List<TaskEntity>) {
        taskDao.reorderTasks(tasks)
        triggerRefresh()
    }

    // Search operations
    fun searchTasks(query: String): Flow<List<TaskEntity>> = taskDao.searchTasks(query)

    // Analytics operations
    suspend fun getCompletedTasksCount(): Int = taskDao.getCompletedTasksCount()
    suspend fun getCompletedTasksInCategory(category: TaskCategory): Int =
        taskDao.getCompletedTasksInCategory(category)
    suspend fun getTodayCompletedCount(): Int {
        val (start, end) = todayBounds()
        return taskDao.getTodayCompletedCount(start, end)
    }
    suspend fun getPendingTasksCount(): Int = taskDao.getPendingTasksCount()
    suspend fun getCompletedTasksForDate(date: String): Int {
        val (start, end) = dayBounds(LocalDate.parse(date))
        return taskDao.getCompletedTasksForDate(start, end)
    }
    suspend fun getTodayStudyMinutes(): Int {
        val (start, end) = todayBounds()
        return taskDao.getTodayStudyMinutes(start, end) ?: 0
    }
    suspend fun getTodayPointsEarned(): Int {
        val (start, end) = todayBounds()
        return taskDao.getTodayPointsEarned(start, end) ?: 0
    }

    // Streak operations
    suspend fun getStreakTasksForDate(date: String): List<TaskEntity> {
        val (start, end) = dayBounds(LocalDate.parse(date))
        return taskDao.getStreakTasksForDate(start, end)
    }

    // Sub-task operations
    fun getSubTasks(parentId: String): Flow<List<TaskEntity>> = taskDao.getSubTasks(parentId)

    // Reminder operations
    suspend fun getTasksWithDueReminders(): List<TaskEntity> =
        taskDao.getTasksWithDueReminders()

    // Utility operations
    suspend fun getAllCategories(): List<TaskCategory> = taskDao.getAllCategories()

    suspend fun deleteAllTasks() {
        taskDao.deleteAllTasks()
        triggerRefresh()
    }

    // Reactive operations
    private fun triggerRefresh() {
        _refreshTrigger.value = System.currentTimeMillis()
    }

    // Batch operations
    suspend fun completeMultipleTasks(taskIds: List<String>, actualMinutes: Int = 0) {
        val completedAt = System.currentTimeMillis()
        taskIds.forEach { taskId ->
            taskDao.updateTaskCompletion(taskId, true, completedAt, actualMinutes)
        }
        triggerRefresh()
    }

    suspend fun updateTaskPriorities(updates: Map<String, TaskPriority>) {
        updates.forEach { (taskId, priority) ->
            val task = getTaskById(taskId)
            task?.let {
                updateTask(it.copy(priority = priority, lastModified = System.currentTimeMillis()))
            }
        }
        triggerRefresh()
    }

    // Data models for reactive flows
    data class TaskProgress(
        val completedToday: Int,
        val totalToday: Int,
        val pendingTotal: Int,
        val completedTotal: Int,
        val completionRate: Float
    )

    data class CategoryProgress(
        val completed: Int,
        val total: Int,
        val percentage: Float
    )

    // Advanced filtering and sorting
    fun getFilteredTasks(
        category: TaskCategory? = null,
        priority: TaskPriority? = null,
        isCompleted: Boolean? = null,
        hasReminder: Boolean? = null,
        isOverdue: Boolean? = null
    ): Flow<List<TaskEntity>> = allTasks.map { tasks ->
        tasks.filter { task ->
            (category == null || task.category == category) &&
            (priority == null || task.priority == priority) &&
            (isCompleted == null || task.isCompleted == isCompleted) &&
            (hasReminder == null || (task.reminderTime != null) == hasReminder) &&
            (isOverdue == null || (task.dueDate?.let { it < System.currentTimeMillis() } ?: false) == isOverdue)
        }
    }

    fun getSortedTasks(
        sortBy: TaskSortBy = TaskSortBy.PRIORITY,
        ascending: Boolean = false
    ): Flow<List<TaskEntity>> = allTasks.map { tasks ->
        when (sortBy) {
            TaskSortBy.PRIORITY -> tasks.sortedWith(
                if (ascending) compareBy { it.priority.ordinal }
                else compareByDescending { it.priority.ordinal }
            )
            TaskSortBy.DUE_DATE -> tasks.sortedWith(
                if (ascending) compareBy { it.dueDate ?: Long.MAX_VALUE }
                else compareByDescending { it.dueDate ?: 0L }
            )
            TaskSortBy.CREATED_DATE -> tasks.sortedWith(
                if (ascending) compareBy { it.createdAt }
                else compareByDescending { it.createdAt }
            )
            TaskSortBy.TITLE -> tasks.sortedWith(
                if (ascending) compareBy { it.title }
                else compareByDescending { it.title }
            )
            TaskSortBy.CATEGORY -> tasks.sortedWith(
                if (ascending) compareBy { it.category.name }
                else compareByDescending { it.category.name }
            )
        }
    }

    enum class TaskSortBy {
        PRIORITY, DUE_DATE, CREATED_DATE, TITLE, CATEGORY
    }

    private fun upcomingRangeBounds(daysAhead: Long = 7): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val start = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = today.plusDays(daysAhead + 1).atStartOfDay(zone).toInstant().toEpochMilli()
        return start to end
    }

    private fun dayBounds(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Pair<Long, Long> {
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return start to end
    }
    // Simple LRU Cache for frequently accessed data
    private class LruCache<K, V>(private val maxSize: Int) {
        private val cache = LinkedHashMap<K, V>(16, 0.75f, true)

        fun get(key: K): V? = cache[key]

        fun put(key: K, value: V): V? {
            if (cache.size >= maxSize && !cache.containsKey(key)) {
                val firstKey = cache.keys.iterator().next()
                cache.remove(firstKey)
            }
            return cache.put(key, value)
        }

        fun remove(key: K): V? = cache.remove(key)
        fun clear() = cache.clear()
    }

    // Cache for task statistics and frequently accessed data
    private val taskStatsCache = LruCache<String, Any>(50)
    private var lastCacheUpdate = 0L
    private val cacheValidityMs = 60_000L // 1 minute

    private fun isCacheValid(): Boolean {
        return (System.currentTimeMillis() - lastCacheUpdate) < cacheValidityMs
    }

    private fun todayBounds(): Pair<Long, Long> = dayBounds(LocalDate.now(ZoneId.systemDefault()))

    private fun tomorrowBounds(): Pair<Long, Long> =
        dayBounds(LocalDate.now(ZoneId.systemDefault()).plusDays(1))

    suspend fun getCachedTaskStats(key: String, provider: suspend () -> Any): Any {
        if (!isCacheValid()) {
            taskStatsCache.clear()
            lastCacheUpdate = System.currentTimeMillis()
        }

        return taskStatsCache.get(key) ?: run {
            val result = provider()
            taskStatsCache.put(key, result)
            result
        }
    }

    // Optimized task statistics with caching
    suspend fun getCachedTodayCompletedCount(): Int {
        return getCachedTaskStats("today_completed") {
            val (start, end) = todayBounds()
            taskDao.getTodayCompletedCount(start, end)
        } as Int
    }

    suspend fun getCachedPendingTasksCount(): Int {
        return getCachedTaskStats("pending_count") {
            taskDao.getPendingTasksCount()
        } as Int
    }

    // Pagination support for large datasets
    data class PaginatedResult<T>(
        val items: List<T>,
        val totalCount: Int,
        val currentPage: Int,
        val totalPages: Int,
        val hasNextPage: Boolean,
        val hasPreviousPage: Boolean
    )

    suspend fun getAllTasksPaginated(page: Int = 0, pageSize: Int = 50): PaginatedResult<TaskEntity> {
        val offset = page * pageSize
        val items = taskDao.getAllTasksPaginated(pageSize, offset)
        val totalCount = taskDao.getTotalActiveTasksCount()
        val totalPages = (totalCount + pageSize - 1) / pageSize

        return PaginatedResult(
            items = items,
            totalCount = totalCount,
            currentPage = page,
            totalPages = totalPages,
            hasNextPage = page < totalPages - 1,
            hasPreviousPage = page > 0
        )
    }

    suspend fun getCompletedTasksPaginated(page: Int = 0, pageSize: Int = 50): PaginatedResult<TaskEntity> {
        val offset = page * pageSize
        val items = taskDao.getCompletedTasksPaginated(pageSize, offset)
        val totalCount = getCompletedTasksCount()
        val totalPages = (totalCount + pageSize - 1) / pageSize

        return PaginatedResult(
            items = items,
            totalCount = totalCount,
            currentPage = page,
            totalPages = totalPages,
            hasNextPage = page < totalPages - 1,
            hasPreviousPage = page > 0
        )
    }

    suspend fun getPendingTasksPaginated(page: Int = 0, pageSize: Int = 50): PaginatedResult<TaskEntity> {
        val offset = page * pageSize
        val items = taskDao.getPendingTasksPaginated(pageSize, offset)
        val totalCount = getPendingTasksCount()
        val totalPages = (totalCount + pageSize - 1) / pageSize

        return PaginatedResult(
            items = items,
            totalCount = totalCount,
            currentPage = page,
            totalPages = totalPages,
            hasNextPage = page < totalPages - 1,
            hasPreviousPage = page > 0
        )
    }

    // Lazy loading for large datasets
    fun getTasksLazy(pageSize: Int = 50): Flow<List<TaskEntity>> = flow {
        var page = 0
        var hasMore = true

        while (hasMore) {
            val result = getAllTasksPaginated(page, pageSize)
            if (result.items.isNotEmpty()) {
                emit(result.items)
                page++
                hasMore = result.hasNextPage
            } else {
                hasMore = false
            }
        }
    }
}



