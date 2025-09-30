package com.mtlc.studyplan.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.mtlc.studyplan.database.entities.TaskEntity
import com.mtlc.studyplan.shared.TaskCategory
import com.mtlc.studyplan.shared.TaskPriority

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks WHERE isActive = 1 ORDER BY priority DESC, dueDate ASC, createdAt ASC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE isActive = 1 AND isCompleted = 0 ORDER BY priority DESC, dueDate ASC")
    fun getPendingTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isActive = 1 AND isCompleted = 1 ORDER BY completedAt DESC")
    fun getCompletedTasks(): Flow<List<TaskEntity>>

    @Query("""
        SELECT * FROM tasks
        WHERE isActive = 1
        AND (dueDate IS NULL OR (dueDate >= :startOfDay AND dueDate < :endOfDay))
        ORDER BY priority DESC, createdAt ASC
    """)
    fun getTodayTasks(startOfDay: Long, endOfDay: Long): Flow<List<TaskEntity>>

    @Query("""
        SELECT * FROM tasks
        WHERE isActive = 1
        AND dueDate IS NOT NULL
        AND DATE(dueDate/1000, 'unixepoch') = DATE('now', '+1 day')
        ORDER BY priority DESC, createdAt ASC
    """)
    fun getTomorrowTasks(): Flow<List<TaskEntity>>

    @Query("""
        SELECT * FROM tasks
        WHERE isActive = 1
        AND dueDate IS NOT NULL
        AND DATE(dueDate/1000, 'unixepoch') BETWEEN DATE('now') AND DATE('now', '+7 days')
        ORDER BY dueDate ASC, priority DESC
    """)
    fun getUpcomingTasks(): Flow<List<TaskEntity>>

    @Query("""
        SELECT * FROM tasks
        WHERE isActive = 1
        AND dueDate IS NOT NULL
        AND dueDate < :currentTime
        AND isCompleted = 0
        ORDER BY dueDate ASC
    """)
    fun getOverdueTasks(currentTime: Long = System.currentTimeMillis()): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE category = :category AND isActive = 1 ORDER BY priority DESC, dueDate ASC")
    fun getTasksByCategory(category: TaskCategory): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE priority = :priority AND isActive = 1 ORDER BY dueDate ASC, createdAt ASC")
    fun getTasksByPriority(priority: TaskPriority): Flow<List<TaskEntity>>

    @Query("""
        SELECT * FROM tasks
        WHERE (title LIKE '%' || :searchQuery || '%'
        OR description LIKE '%' || :searchQuery || '%'
        OR notes LIKE '%' || :searchQuery || '%')
        AND isActive = 1
        ORDER BY priority DESC, dueDate ASC
    """)
    fun searchTasks(searchQuery: String): Flow<List<TaskEntity>>

    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 1")
    suspend fun getCompletedTasksCount(): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE category = :category AND isCompleted = 1")
    suspend fun getCompletedTasksInCategory(category: TaskCategory): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 1 AND DATE(completedAt/1000, 'unixepoch') = DATE('now')")
    suspend fun getTodayCompletedCount(): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE isActive = 1 AND isCompleted = 0")
    suspend fun getPendingTasksCount(): Int

    @Query("""
        SELECT COUNT(*) FROM tasks
        WHERE isCompleted = 1
        AND DATE(completedAt/1000, 'unixepoch') = :date
    """)
    suspend fun getCompletedTasksForDate(date: String): Int

    @Query("""
        SELECT SUM(actualMinutes) FROM tasks
        WHERE isCompleted = 1
        AND DATE(completedAt/1000, 'unixepoch') = DATE('now')
    """)
    suspend fun getTodayStudyMinutes(): Int?

    @Query("""
        SELECT SUM(pointsValue) FROM tasks
        WHERE isCompleted = 1
        AND DATE(completedAt/1000, 'unixepoch') = DATE('now')
    """)
    suspend fun getTodayPointsEarned(): Int?

    @Query("""
        SELECT * FROM tasks
        WHERE streakContribution = 1
        AND isCompleted = 1
        AND DATE(completedAt/1000, 'unixepoch') = :date
    """)
    suspend fun getStreakTasksForDate(date: String): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE parentTaskId = :parentId AND isActive = 1 ORDER BY orderIndex ASC")
    fun getSubTasks(parentId: String): Flow<List<TaskEntity>>

    @Query("SELECT DISTINCT category FROM tasks WHERE isActive = 1")
    suspend fun getAllCategories(): List<TaskCategory>

    @Query("""
        SELECT * FROM tasks
        WHERE reminderTime IS NOT NULL
        AND reminderTime <= :currentTime
        AND isCompleted = 0
        AND isActive = 1
        ORDER BY reminderTime ASC
    """)
    suspend fun getTasksWithDueReminders(currentTime: Long = System.currentTimeMillis()): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Update
    suspend fun updateTasks(tasks: List<TaskEntity>)

    @Query("UPDATE tasks SET isCompleted = :isCompleted, completedAt = :completedAt, actualMinutes = :actualMinutes WHERE id = :taskId")
    suspend fun updateTaskCompletion(taskId: String, isCompleted: Boolean, completedAt: Long?, actualMinutes: Int)

    @Query("UPDATE tasks SET isActive = 0 WHERE id = :taskId")
    suspend fun deleteTask(taskId: String)

    @Query("UPDATE tasks SET isActive = 0 WHERE id IN (:taskIds)")
    suspend fun deleteTasks(taskIds: List<String>)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun permanentlyDeleteTask(taskId: String)

    @Delete
    suspend fun deleteTaskEntity(task: TaskEntity)

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()

    // Batch operations
    @Transaction
    suspend fun moveTaskToCategory(taskId: String, newCategory: TaskCategory) {
        val task = getTaskById(taskId)
        task?.let {
            updateTask(it.copy(category = newCategory, lastModified = System.currentTimeMillis()))
        }
    }

    @Transaction
    suspend fun reorderTasks(tasks: List<TaskEntity>) {
        tasks.forEachIndexed { index, task ->
            updateTask(task.copy(orderIndex = index, lastModified = System.currentTimeMillis()))
        }
    }

    // Batch operations for better performance
    @Transaction
    suspend fun updateTasksBatch(tasks: List<TaskEntity>) {
        tasks.forEach { task ->
            updateTask(task)
        }
    }

    @Query("UPDATE tasks SET priority = :priority, lastModified = :lastModified WHERE id = :taskId")
    suspend fun updateTaskPriority(taskId: String, priority: TaskPriority, lastModified: Long = System.currentTimeMillis())

    @Transaction
    suspend fun updateTaskPriorities(updates: Map<String, TaskPriority>) {
        val currentTime = System.currentTimeMillis()
        updates.forEach { (taskId, priority) ->
            updateTaskPriority(taskId, priority, currentTime)
        }
    }
}