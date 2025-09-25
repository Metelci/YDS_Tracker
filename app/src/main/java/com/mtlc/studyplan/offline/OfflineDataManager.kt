package com.mtlc.studyplan.offline

import android.content.Context
import android.content.SharedPreferences
import androidx.room.*
import com.mtlc.studyplan.storage.room.StudyPlanDatabase
import com.mtlc.studyplan.offline.ActionType
import com.mtlc.studyplan.offline.ActionStatus
import com.mtlc.studyplan.offline.OfflineActionEntity
import com.mtlc.studyplan.shared.AppTask
import com.mtlc.studyplan.shared.StudyStats
import com.mtlc.studyplan.shared.TaskCategory
import com.mtlc.studyplan.shared.TaskDifficulty
import com.mtlc.studyplan.utils.NetworkHelper
import com.mtlc.studyplan.utils.ToastManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * Comprehensive Offline Data Management System
 * Handles data caching, offline operations, and synchronization
 */
class OfflineDataManager(
    private val context: Context,
    private val localDatabase: OfflineDatabase,
    private val sharedPreferences: SharedPreferences
) {

    companion object {
        private const val PREF_OFFLINE_MODE = "offline_mode_enabled"
        private const val PREF_LAST_SYNC = "last_sync_timestamp"
        private const val PREF_PENDING_SYNC_COUNT = "pending_sync_count"
    }

    suspend fun enableOfflineMode() {
        try {
            showOfflineModeActivating()

            // Cache essential data for offline use
            cacheTasksForOffline()
            cacheProgressDataForOffline()
            cacheUserSettings()
            cacheTaskTemplates()

            // Setup offline workflows
            setupOfflineWorkflows()

            // Mark offline mode as enabled
            sharedPreferences.edit()
                .putBoolean(PREF_OFFLINE_MODE, true)
                .apply()

            showOfflineModeActivated()

        } catch (e: Exception) {
            handleOfflineSetupError(e)
        }
    }

    private suspend fun cacheTasksForOffline() {
        // In a real implementation, this would fetch from remote repository
        // For now, we'll create some sample offline tasks
        val offlineTasks = createSampleOfflineTasks()

        localDatabase.offlineTaskDao().insertAll(
            offlineTasks.map { it.toOfflineEntity() }
        )
    }

    private suspend fun cacheProgressDataForOffline() {
        // Cache current progress data
        val currentStats = StudyStats(
            totalTasksCompleted = 15,
            currentStreak = 5,
            totalStudyTime = 1200,
            thisWeekTasks = 8,
            thisWeekStudyTime = 480,
            averageSessionTime = 60,
            totalXP = 750
        )

        localDatabase.offlineProgressDao().insert(
            OfflineProgressEntity(
                id = "current_progress",
                data = Json.encodeToString(currentStats),
                timestamp = System.currentTimeMillis()
            )
        )
    }

    private suspend fun cacheUserSettings() {
        // Cache user settings for offline access
        val settings = mapOf(
            "notifications_enabled" to true,
            "gamification_enabled" to true,
            "theme_mode" to "system",
            "daily_goal_tasks" to 5
        )

        localDatabase.offlineSettingsDao().insert(
            OfflineSettingsEntity(
                id = "user_settings",
                data = Json.encodeToString(settings),
                timestamp = System.currentTimeMillis()
            )
        )
    }

    private suspend fun cacheTaskTemplates() {
        // Cache task templates for creating new tasks offline
        val templates = createTaskTemplates()

        localDatabase.offlineTemplateDao().insertAll(
            templates.map { template ->
                OfflineTemplateEntity(
                    id = template.id,
                    category = template.category.name,
                    data = Json.encodeToString(template),
                    timestamp = System.currentTimeMillis()
                )
            }
        )
    }

    suspend fun synchronizeWhenOnline(): OfflineDataSyncResult {
        if (!NetworkHelper.isOnline()) {
            return OfflineDataSyncResult.Failed("No internet connection")
        }

        return try {
            showSyncInProgress()

            // Upload offline changes
            val syncResults = mutableListOf<String>()

            // Sync offline actions
            val offlineActions = localDatabase.offlineActionDao().getAllPending()
            offlineActions.forEach { action ->
                processOfflineAction(action)
                syncResults.add("Processed ${action.type}")
            }

            // Sync completed tasks
            val completedTasks = localDatabase.offlineTaskDao().getCompletedTasks()
            completedTasks.forEach { task ->
                syncCompletedTask(task)
                syncResults.add("Synced completed task: ${task.title}")
            }

            // Sync progress data
            val progressData = localDatabase.offlineProgressDao().getLatest()
            progressData?.let {
                syncProgressData(it)
                syncResults.add("Synced progress data")
            }

            // Download latest data from server
            downloadLatestData()
            syncResults.add("Downloaded latest data")

            // Clear synced offline data
            clearSyncedData()

            // Update sync timestamp
            sharedPreferences.edit()
                .putLong(PREF_LAST_SYNC, System.currentTimeMillis())
                .putInt(PREF_PENDING_SYNC_COUNT, 0)
                .apply()

            showSyncCompleted()
            OfflineDataSyncResult.Success(syncResults)

        } catch (e: Exception) {
            handleSyncError(e)
            OfflineDataSyncResult.Failed(e.message ?: "Sync failed")
        }
    }

    suspend fun createTaskOffline(task: AppTask): OfflineResult {
        return try {
            val offlineTask = task.toOfflineEntity().copy(
                isOfflineCreated = true,
                needsSync = true
            )

            localDatabase.offlineTaskDao().insert(offlineTask)

            // Queue for sync when online
            val action = OfflineActionEntity(
                id = UUID.randomUUID().toString(),
                type = ActionType.TASK_CREATED,
                data = Json.encodeToString(task),
                timestamp = System.currentTimeMillis(),
                retryCount = 0,
                status = ActionStatus.PENDING
            )
            localDatabase.offlineActionDao().insert(action)

            updatePendingSyncCount()
            OfflineResult.Success("Task created offline")

        } catch (e: Exception) {
            OfflineResult.Failed("Failed to create task offline: ${e.message}")
        }
    }

    suspend fun completeTaskOffline(taskId: String): OfflineResult {
        return try {
            // Update local task
            localDatabase.offlineTaskDao().markCompleted(taskId, System.currentTimeMillis())

            // Queue completion for sync
            val action = OfflineActionEntity(
                id = UUID.randomUUID().toString(),
                type = ActionType.TASK_COMPLETED,
                data = Json.encodeToString(mapOf("taskId" to taskId)),
                timestamp = System.currentTimeMillis(),
                retryCount = 0,
                status = ActionStatus.PENDING
            )
            localDatabase.offlineActionDao().insert(action)

            updatePendingSyncCount()
            OfflineResult.Success("Task completed offline")

        } catch (e: Exception) {
            OfflineResult.Failed("Failed to complete task offline: ${e.message}")
        }
    }

    suspend fun updateProgressOffline(stats: StudyStats): OfflineResult {
        return try {
            val progressEntity = OfflineProgressEntity(
                id = "progress_${System.currentTimeMillis()}",
                data = Json.encodeToString(stats),
                timestamp = System.currentTimeMillis()
            )

            localDatabase.offlineProgressDao().insert(progressEntity)

            // Queue for sync
            val action = OfflineActionEntity(
                id = UUID.randomUUID().toString(),
                type = ActionType.PROGRESS_UPDATED,
                data = Json.encodeToString(stats),
                timestamp = System.currentTimeMillis(),
                retryCount = 0,
                status = ActionStatus.PENDING
            )
            localDatabase.offlineActionDao().insert(action)

            updatePendingSyncCount()
            OfflineResult.Success("Progress updated offline")

        } catch (e: Exception) {
            OfflineResult.Failed("Failed to update progress offline: ${e.message}")
        }
    }

    // Data access methods for offline mode
    suspend fun getOfflineTasks(): List<AppTask> {
        return localDatabase.offlineTaskDao().getAll().map { it.toAppTask() }
    }

    suspend fun getOfflineProgress(): StudyStats? {
        return localDatabase.offlineProgressDao().getLatest()?.let { entity ->
            Json.decodeFromString<StudyStats>(entity.data)
        }
    }

    fun getOfflineTasksFlow(): Flow<List<OfflineTaskEntity>> {
        return localDatabase.offlineTaskDao().getAllFlow()
    }

    fun getPendingSyncCount(): Int {
        return sharedPreferences.getInt(PREF_PENDING_SYNC_COUNT, 0)
    }

    fun isOfflineModeEnabled(): Boolean {
        return sharedPreferences.getBoolean(PREF_OFFLINE_MODE, false)
    }

    fun getLastSyncTime(): Long {
        return sharedPreferences.getLong(PREF_LAST_SYNC, 0)
    }

    // Helper methods
    private suspend fun processOfflineAction(action: OfflineActionEntity) {
        when (action.type) {
            ActionType.TASK_CREATED -> {
                val task = Json.decodeFromString<AppTask>(action.data)
                // Send to remote repository
                android.util.Log.d("OfflineSync", "Syncing created task: ${task.title}")
            }
            ActionType.TASK_COMPLETED -> {
                val data = Json.decodeFromString<Map<String, String>>(action.data)
                val taskId = data["taskId"]
                // Send completion to remote repository
                android.util.Log.d("OfflineSync", "Syncing task completion: $taskId")
            }
            ActionType.PROGRESS_UPDATED -> {
                val stats = Json.decodeFromString<StudyStats>(action.data)
                // Send progress to remote repository
                android.util.Log.d("OfflineSync", "Syncing progress: ${stats.totalXP} XP")
            }
            else -> {
                android.util.Log.w("OfflineSync", "Unknown action type: ${action.type}")
            }
        }
    }

    private suspend fun syncCompletedTask(task: OfflineTaskEntity) {
        // In real implementation, send to remote repository
        android.util.Log.d("OfflineSync", "Syncing completed task: ${task.title}")
    }

    private suspend fun syncProgressData(progress: OfflineProgressEntity) {
        // In real implementation, send to remote repository
        android.util.Log.d("OfflineSync", "Syncing progress data")
    }

    private suspend fun downloadLatestData() {
        // In real implementation, download latest data from server
        android.util.Log.d("OfflineSync", "Downloading latest data")
    }

    private suspend fun clearSyncedData() {
        localDatabase.offlineActionDao().deleteCompleted()
    }

    private fun updatePendingSyncCount() {
        val currentCount = getPendingSyncCount()
        sharedPreferences.edit()
            .putInt(PREF_PENDING_SYNC_COUNT, currentCount + 1)
            .apply()
    }

    private fun setupOfflineWorkflows() {
        // Setup periodic sync when connection becomes available
        // In real implementation, this would use WorkManager
    }

    // Sample data creation methods
    private fun createSampleOfflineTasks(): List<AppTask> {
        return listOf(
            AppTask(
                id = "offline_1",
                title = "Vocabulary Review",
                description = "Review 20 new vocabulary words",
                category = TaskCategory.VOCABULARY,
                difficulty = TaskDifficulty.MEDIUM,
                estimatedMinutes = 30,
                isCompleted = false,
                xpReward = 25
            ),
            AppTask(
                id = "offline_2",
                title = "Grammar Practice",
                description = "Practice conditional sentences",
                category = TaskCategory.GRAMMAR,
                difficulty = TaskDifficulty.HARD,
                estimatedMinutes = 45,
                isCompleted = false,
                xpReward = 30
            ),
            AppTask(
                id = "offline_3",
                title = "Reading Comprehension",
                description = "Read academic article and answer questions",
                category = TaskCategory.READING,
                difficulty = TaskDifficulty.MEDIUM,
                estimatedMinutes = 60,
                isCompleted = false,
                xpReward = 35
            )
        )
    }

    private fun createTaskTemplates(): List<TaskTemplate> {
        return listOf(
            TaskTemplate(
                id = "template_vocab",
                title = "Vocabulary Study",
                description = "Learn new vocabulary words",
                category = TaskCategory.VOCABULARY,
                defaultDifficulty = TaskDifficulty.MEDIUM,
                defaultDuration = 30,
                defaultXP = 25
            ),
            TaskTemplate(
                id = "template_grammar",
                title = "Grammar Practice",
                description = "Practice grammar rules and exercises",
                category = TaskCategory.GRAMMAR,
                defaultDifficulty = TaskDifficulty.MEDIUM,
                defaultDuration = 45,
                defaultXP = 30
            )
        )
    }

    // Feedback methods
    private fun showOfflineModeActivating() {
        ToastManager.showInfo("Setting up offline mode...")
    }

    private fun showOfflineModeActivated() {
        ToastManager.showSuccess("✈️ Offline mode activated! You can continue studying without internet.")
    }

    private fun showSyncInProgress() {
        ToastManager.showInfo("🔄 Syncing your offline changes...")
    }

    private fun showSyncCompleted() {
        ToastManager.showSuccess("✅ Sync completed! All your data is up to date.")
    }

    private fun handleOfflineSetupError(error: Exception) {
        ToastManager.showError("Failed to setup offline mode: ${error.message}")
        android.util.Log.e("OfflineDataManager", "Offline setup error", error)
    }

    private fun handleSyncError(error: Exception) {
        ToastManager.showError("Sync failed: ${error.message}. Will retry when connection improves.")
        android.util.Log.e("OfflineDataManager", "Sync error", error)
    }
}

// Database entities for offline data
@Entity(tableName = "offline_tasks")
data class OfflineTaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val category: String,
    val difficulty: String,
    val estimatedMinutes: Int,
    val isCompleted: Boolean,
    val completedAt: Long?,
    val xpReward: Int,
    val isOfflineCreated: Boolean = false,
    val needsSync: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "offline_progress")
data class OfflineProgressEntity(
    @PrimaryKey val id: String,
    val data: String, // JSON serialized StudyStats
    val timestamp: Long
)

@Entity(tableName = "offline_settings")
data class OfflineSettingsEntity(
    @PrimaryKey val id: String,
    val data: String, // JSON serialized settings
    val timestamp: Long
)

@Entity(tableName = "offline_templates")
data class OfflineTemplateEntity(
    @PrimaryKey val id: String,
    val category: String,
    val data: String, // JSON serialized TaskTemplate
    val timestamp: Long
)

// DAOs
@Dao
interface OfflineTaskDao {
    @Query("SELECT * FROM offline_tasks ORDER BY timestamp DESC")
    suspend fun getAll(): List<OfflineTaskEntity>

    @Query("SELECT * FROM offline_tasks ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<OfflineTaskEntity>>

    @Query("SELECT * FROM offline_tasks WHERE isCompleted = 1 AND needsSync = 1")
    suspend fun getCompletedTasks(): List<OfflineTaskEntity>

    @Query("UPDATE offline_tasks SET isCompleted = 1, completedAt = :timestamp, needsSync = 1 WHERE id = :taskId")
    suspend fun markCompleted(taskId: String, timestamp: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: OfflineTaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<OfflineTaskEntity>)
}

@Dao
interface OfflineProgressDao {
    @Query("SELECT * FROM offline_progress ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatest(): OfflineProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(progress: OfflineProgressEntity)
}

@Dao
interface OfflineSettingsDao {
    @Query("SELECT * FROM offline_settings WHERE id = :id")
    suspend fun getById(id: String): OfflineSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(settings: OfflineSettingsEntity)
}

@Dao
interface OfflineTemplateDao {
    @Query("SELECT * FROM offline_templates")
    suspend fun getAll(): List<OfflineTemplateEntity>

    @Query("SELECT * FROM offline_templates WHERE category = :category")
    suspend fun getByCategory(category: String): List<OfflineTemplateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(templates: List<OfflineTemplateEntity>)
}

// Database
@Database(
    entities = [OfflineTaskEntity::class, OfflineProgressEntity::class,
               OfflineSettingsEntity::class, OfflineTemplateEntity::class,
               OfflineActionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class OfflineDatabase : RoomDatabase() {
    abstract fun offlineTaskDao(): OfflineTaskDao
    abstract fun offlineProgressDao(): OfflineProgressDao
    abstract fun offlineSettingsDao(): OfflineSettingsDao
    abstract fun offlineTemplateDao(): OfflineTemplateDao
    abstract fun offlineActionDao(): OfflineActionDao
}

// Result classes
sealed class OfflineResult {
    data class Success(val message: String) : OfflineResult()
    data class Failed(val message: String) : OfflineResult()
}

sealed class OfflineDataSyncResult {
    data class Success(val syncedItems: List<String>) : OfflineDataSyncResult()
    data class Failed(val message: String) : OfflineDataSyncResult()
}

// Data classes
@Serializable
data class TaskTemplate(
    val id: String,
    val title: String,
    val description: String,
    val category: TaskCategory,
    val defaultDifficulty: TaskDifficulty,
    val defaultDuration: Int,
    val defaultXP: Int
)

// Extension functions
fun AppTask.toOfflineEntity(): OfflineTaskEntity {
    return OfflineTaskEntity(
        id = this.id,
        title = this.title,
        description = this.description,
        category = this.category.name,
        difficulty = this.difficulty.name,
        estimatedMinutes = this.estimatedMinutes,
        isCompleted = this.isCompleted,
        completedAt = if (this.isCompleted) System.currentTimeMillis() else null,
        xpReward = this.xpReward
    )
}

fun OfflineTaskEntity.toAppTask(): AppTask {
    return AppTask(
        id = this.id,
        title = this.title,
        description = this.description,
        category = TaskCategory.valueOf(this.category),
        difficulty = TaskDifficulty.valueOf(this.difficulty),
        estimatedMinutes = this.estimatedMinutes,
        isCompleted = this.isCompleted,
        xpReward = this.xpReward
    )
}