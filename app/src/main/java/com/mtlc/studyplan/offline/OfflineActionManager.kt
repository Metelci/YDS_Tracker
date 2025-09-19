package com.mtlc.studyplan.offline

import android.content.Context
import androidx.room.*
import com.mtlc.studyplan.workflows.OfflineAction
import com.mtlc.studyplan.utils.NetworkHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * Comprehensive Offline Action Management System
 * Handles queuing and syncing of actions performed while offline
 */
object OfflineActionManager {

    private lateinit var database: OfflineActionDatabase
    private var isInitialized = false

    fun initialize(context: Context) {
        if (isInitialized) return

        database = Room.databaseBuilder(
            context.applicationContext,
            OfflineActionDatabase::class.java,
            "offline_actions_db"
        ).build()

        isInitialized = true

        // Start monitoring network state for auto-sync
        CoroutineScope(Dispatchers.IO).launch {
            NetworkHelper.networkState.collect { state ->
                if (state == com.mtlc.studyplan.utils.NetworkState.CONNECTED) {
                    syncPendingActions()
                }
            }
        }
    }

    suspend fun queueAction(context: Context, action: OfflineAction) {
        if (!isInitialized) initialize(context)

        val entity = when (action) {
            is OfflineAction.ShareAchievement -> {
                OfflineActionEntity(
                    id = UUID.randomUUID().toString(),
                    type = ActionType.SHARE_ACHIEVEMENT,
                    data = Json.encodeToString(action),
                    timestamp = action.timestamp,
                    retryCount = 0,
                    status = ActionStatus.PENDING
                )
            }
            is OfflineAction.JoinGroup -> {
                OfflineActionEntity(
                    id = UUID.randomUUID().toString(),
                    type = ActionType.JOIN_GROUP,
                    data = Json.encodeToString(action),
                    timestamp = action.timestamp,
                    retryCount = 0,
                    status = ActionStatus.PENDING
                )
            }
            is OfflineAction.AddFriend -> {
                OfflineActionEntity(
                    id = UUID.randomUUID().toString(),
                    type = ActionType.ADD_FRIEND,
                    data = Json.encodeToString(action),
                    timestamp = action.timestamp,
                    retryCount = 0,
                    status = ActionStatus.PENDING
                )
            }
        }

        database.offlineActionDao().insert(entity)
    }

    suspend fun getPendingActionsCount(): Int {
        return if (isInitialized) {
            database.offlineActionDao().getPendingCount()
        } else {
            0
        }
    }

    fun getPendingActionsFlow(): Flow<List<OfflineActionEntity>> {
        return if (isInitialized) {
            database.offlineActionDao().getAllPendingFlow()
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }

    suspend fun syncPendingActions() {
        if (!isInitialized || !NetworkHelper.isOnline()) return

        val pendingActions = database.offlineActionDao().getAllPending()

        pendingActions.forEach { actionEntity ->
            try {
                when (actionEntity.type) {
                    ActionType.SHARE_ACHIEVEMENT -> {
                        processShareAchievement(actionEntity)
                    }
                    ActionType.JOIN_GROUP -> {
                        processJoinGroup(actionEntity)
                    }
                    ActionType.ADD_FRIEND -> {
                        processAddFriend(actionEntity)
                    }
                    ActionType.COMPLETE_TASK -> {
                        processCompleteTask(actionEntity)
                    }
                    ActionType.UPDATE_PROGRESS -> {
                        processUpdateProgress(actionEntity)
                    }
                }

                // Mark as completed
                database.offlineActionDao().updateStatus(actionEntity.id, ActionStatus.COMPLETED)

            } catch (e: Exception) {
                handleSyncError(actionEntity, e)
            }
        }
    }

    private suspend fun processShareAchievement(actionEntity: OfflineActionEntity) {
        val action = Json.decodeFromString<OfflineAction.ShareAchievement>(actionEntity.data)
        // In a real implementation, this would call the social repository
        android.util.Log.d("OfflineSync", "Syncing share achievement: ${action.achievementId}")

        // Simulate network call
        kotlinx.coroutines.delay(500)
    }

    private suspend fun processJoinGroup(actionEntity: OfflineActionEntity) {
        val action = Json.decodeFromString<OfflineAction.JoinGroup>(actionEntity.data)
        // In a real implementation, this would call the social repository
        android.util.Log.d("OfflineSync", "Syncing join group: ${action.groupId}")

        // Simulate network call
        kotlinx.coroutines.delay(300)
    }

    private suspend fun processAddFriend(actionEntity: OfflineActionEntity) {
        val action = Json.decodeFromString<OfflineAction.AddFriend>(actionEntity.data)
        // In a real implementation, this would call the social repository
        android.util.Log.d("OfflineSync", "Syncing add friend: ${action.friendId}")

        // Simulate network call
        kotlinx.coroutines.delay(300)
    }

    private suspend fun processCompleteTask(actionEntity: OfflineActionEntity) {
        // Process task completion
        android.util.Log.d("OfflineSync", "Syncing task completion")
        kotlinx.coroutines.delay(200)
    }

    private suspend fun processUpdateProgress(actionEntity: OfflineActionEntity) {
        // Process progress update
        android.util.Log.d("OfflineSync", "Syncing progress update")
        kotlinx.coroutines.delay(200)
    }

    private suspend fun handleSyncError(actionEntity: OfflineActionEntity, error: Exception) {
        val newRetryCount = actionEntity.retryCount + 1

        if (newRetryCount >= MAX_RETRY_COUNT) {
            // Mark as failed after max retries
            database.offlineActionDao().updateStatus(actionEntity.id, ActionStatus.FAILED)
            android.util.Log.e("OfflineSync", "Action failed after $MAX_RETRY_COUNT retries: ${error.message}")
        } else {
            // Increment retry count
            database.offlineActionDao().updateRetryCount(actionEntity.id, newRetryCount)
            android.util.Log.w("OfflineSync", "Action retry $newRetryCount/${MAX_RETRY_COUNT}: ${error.message}")
        }
    }

    suspend fun clearCompletedActions() {
        if (isInitialized) {
            database.offlineActionDao().deleteCompleted()
        }
    }

    suspend fun clearAllActions() {
        if (isInitialized) {
            database.offlineActionDao().deleteAll()
        }
    }

    private companion object {
        const val MAX_RETRY_COUNT = 3
    }
}

// Database entities and DAO
@Entity(tableName = "offline_actions")
data class OfflineActionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "type") val type: ActionType,
    @ColumnInfo(name = "data") val data: String,
    @ColumnInfo(name = "timestamp") val timestamp: Long,
    @ColumnInfo(name = "retry_count") val retryCount: Int,
    @ColumnInfo(name = "status") val status: ActionStatus
)

enum class ActionType {
    SHARE_ACHIEVEMENT,
    JOIN_GROUP,
    ADD_FRIEND,
    COMPLETE_TASK,
    UPDATE_PROGRESS
}

enum class ActionStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED
}

@Dao
interface OfflineActionDao {
    @Query("SELECT * FROM offline_actions WHERE status = 'PENDING' ORDER BY timestamp ASC")
    suspend fun getAllPending(): List<OfflineActionEntity>

    @Query("SELECT * FROM offline_actions WHERE status = 'PENDING' ORDER BY timestamp ASC")
    fun getAllPendingFlow(): Flow<List<OfflineActionEntity>>

    @Query("SELECT COUNT(*) FROM offline_actions WHERE status = 'PENDING'")
    suspend fun getPendingCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(action: OfflineActionEntity)

    @Query("UPDATE offline_actions SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: ActionStatus)

    @Query("UPDATE offline_actions SET retry_count = :retryCount WHERE id = :id")
    suspend fun updateRetryCount(id: String, retryCount: Int)

    @Query("DELETE FROM offline_actions WHERE status = 'COMPLETED'")
    suspend fun deleteCompleted()

    @Query("DELETE FROM offline_actions")
    suspend fun deleteAll()

    @Query("DELETE FROM offline_actions WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Database(
    entities = [OfflineActionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class OfflineActionDatabase : RoomDatabase() {
    abstract fun offlineActionDao(): OfflineActionDao
}