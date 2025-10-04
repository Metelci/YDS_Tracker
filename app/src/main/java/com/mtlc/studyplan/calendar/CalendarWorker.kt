package com.mtlc.studyplan.calendar

import android.content.Context
import android.os.Build
import android.os.PowerManager
import com.mtlc.studyplan.power.PowerStateChecker
import androidx.work.*
import com.mtlc.studyplan.data.PlanDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * WorkManager worker for daily calendar synchronization
 */
class CalendarWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORKER_NAME = "calendar_sync_worker"
        const val TAG_CALENDAR_SYNC = "calendar_sync"
        private const val RETRY_DELAY_MINUTES = 30L
        private const val MAX_RETRY_ATTEMPTS = 3
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Battery optimization: avoid heavy sync in power saver or doze
            if (PowerStateChecker.isPowerConstrained(applicationContext)) {
                return@withContext Result.success(
                    workDataOf("message" to "Skipped calendar sync due to power saver/doze")
                )
            }
            // Get dependencies
            val settingsRepository = CalendarSettingsRepository(calendarDataStore)
            val calendarSync = CalendarSync(applicationContext, settingsRepository)
            
            // Check if sync is enabled
            val prefs = settingsRepository.calendarPrefsFlow.first()
            if (!prefs.enabled) {
                return@withContext Result.success()
            }

            // Check permissions
            if (!calendarSync.hasPermissions()) {
                // Permissions revoked - disable sync to prevent repeated failures
                settingsRepository.setCalendarEnabled(false)
                return@withContext Result.failure(
                    workDataOf("error" to "Calendar permissions revoked")
                )
            }

            // Get study plan data from data source
            val weekPlans = PlanDataSource.planData
            val dayPlans = weekPlans.flatMap { it.days }
            
            if (dayPlans.size == 0) {
                return@withContext Result.success(
                    workDataOf("message" to "No study plans to sync")
                )
            }

            // Perform sync
            val syncResult = calendarSync.upsertNext4Weeks(dayPlans)
            
            when (syncResult) {
                is CalendarSyncResult.Success -> {
                    Result.success(
                        workDataOf(
                            "sync_time" to System.currentTimeMillis(),
                            "message" to "Calendar sync completed successfully"
                        )
                    )
                }
                is CalendarSyncResult.PartialSuccess -> {
                    Result.success(
                        workDataOf(
                            "sync_time" to System.currentTimeMillis(),
                            "synced_count" to syncResult.syncedCount,
                            "failed_count" to syncResult.failedCount,
                            "message" to "Calendar sync partially completed"
                        )
                    )
                }
                is CalendarSyncResult.NoPermission -> {
                    // Disable sync due to permission issues
                    settingsRepository.setCalendarEnabled(false)
                    Result.failure(
                        workDataOf("error" to "Calendar permissions not granted")
                    )
                }
                is CalendarSyncResult.NoCalendarSelected -> {
                    // Disable sync - user needs to select a calendar
                    settingsRepository.setCalendarEnabled(false)
                    Result.failure(
                        workDataOf("error" to "No calendar selected for sync")
                    )
                }
                is CalendarSyncResult.NoCalendarsAvailable -> {
                    Result.failure(
                        workDataOf("error" to "No calendars available on device")
                    )
                }
                is CalendarSyncResult.Error -> {
                    // Retry on transient errors
                    if (runAttemptCount < MAX_RETRY_ATTEMPTS) {
                        Result.retry()
                    } else {
                        Result.failure(
                            workDataOf("error" to syncResult.message)
                        )
                    }
                }
            }
        } catch (e: Exception) {
            // Log error and retry if attempts remaining
            if (runAttemptCount < MAX_RETRY_ATTEMPTS) {
                Result.retry()
            } else {
                Result.failure(
                    workDataOf("error" to "Calendar sync failed: ${e.message}")
                )
            }
        }
    }


    /**
     * Get calendar datastore
     */
    private val calendarDataStore by lazy {
        applicationContext.calendarDataStore
    }
}

/**
 * Manager for calendar worker operations
 */
object CalendarWorkerManager {

    /**
     * Schedule daily calendar sync
     */
    fun scheduleDailySync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            // Battery-aware: avoid running when battery is low; allow while not charging
            .setRequiresBatteryNotLow(true)
            .setRequiresCharging(false)
            .setRequiresDeviceIdle(false)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<CalendarWorker>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = TimeUnit.DAYS,
            flexTimeInterval = 2,
            flexTimeIntervalUnit = TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                Duration.ofMinutes(RETRY_DELAY_MINUTES)
            )
            .addTag(CalendarWorker.TAG_CALENDAR_SYNC)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                CalendarWorker.WORKER_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
    }

    /**
     * Cancel daily calendar sync
     */
    fun cancelDailySync(context: Context) {
        WorkManager.getInstance(context)
            .cancelUniqueWork(CalendarWorker.WORKER_NAME)
    }

    /**
     * Request immediate sync (one-time)
     */
    fun requestImmediateSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(true)
            .build()

        val immediateSyncRequest = OneTimeWorkRequestBuilder<CalendarWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                Duration.ofMinutes(RETRY_DELAY_MINUTES)
            )
            .addTag(CalendarWorker.TAG_CALENDAR_SYNC)
            .build()

        WorkManager.getInstance(context)
            .enqueue(immediateSyncRequest)
    }

    /**
     * Get sync work status
     */
    suspend fun getSyncStatus(context: Context): WorkInfo? {
        return try {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val workManager = WorkManager.getInstance(context)
                val workInfos = workManager.getWorkInfosForUniqueWork(CalendarWorker.WORKER_NAME)
                
                // Use blocking get() call since we're already in IO context
                val workInfoList = workInfos.get(5, TimeUnit.SECONDS)
                workInfoList.firstOrNull()
            }
        } catch (e: TimeoutException) {
            null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Check if sync is currently running
     */
    suspend fun isSyncRunning(context: Context): Boolean {
        val workInfo = getSyncStatus(context)
        return workInfo?.state == WorkInfo.State.RUNNING
    }

    /**
     * Get last sync result
     */
    suspend fun getLastSyncResult(context: Context): CalendarWorkerResult? {
        val workInfo = getSyncStatus(context) ?: return null
        
        return when (workInfo.state) {
            WorkInfo.State.SUCCEEDED -> {
                val outputData = workInfo.outputData
                CalendarWorkerResult.Success(
                    syncTime = outputData.getLong("sync_time", 0L),
                    message = outputData.getString("message") ?: "Sync completed",
                    syncedCount = outputData.getInt("synced_count", 0),
                    failedCount = outputData.getInt("failed_count", 0)
                )
            }
            WorkInfo.State.FAILED -> {
                val outputData = workInfo.outputData
                CalendarWorkerResult.Failed(
                    error = outputData.getString("error") ?: "Unknown error",
                    lastAttempt = System.currentTimeMillis()
                )
            }
            WorkInfo.State.RUNNING -> CalendarWorkerResult.Running
            WorkInfo.State.ENQUEUED -> CalendarWorkerResult.Pending
            WorkInfo.State.BLOCKED -> CalendarWorkerResult.Blocked
            WorkInfo.State.CANCELLED -> CalendarWorkerResult.Cancelled
        }
    }

    private const val RETRY_DELAY_MINUTES = 30L
}

/**
 * Result of calendar worker execution
 */
sealed class CalendarWorkerResult {
    object Running : CalendarWorkerResult()
    object Pending : CalendarWorkerResult()
    object Blocked : CalendarWorkerResult()
    object Cancelled : CalendarWorkerResult()
    
    data class Success(
        val syncTime: Long,
        val message: String,
        val syncedCount: Int = 0,
        val failedCount: Int = 0
    ) : CalendarWorkerResult()
    
    data class Failed(
        val error: String,
        val lastAttempt: Long
    ) : CalendarWorkerResult()
}

/**
 * Extension functions for calendar sync management
 */
suspend fun Context.enableCalendarSync() {
    val settingsRepository = CalendarSettingsRepository(calendarDataStore)
    settingsRepository.setCalendarEnabled(true)
    CalendarWorkerManager.scheduleDailySync(this)
}

suspend fun Context.disableCalendarSync() {
    val settingsRepository = CalendarSettingsRepository(calendarDataStore)
    settingsRepository.setCalendarEnabled(false)
    CalendarWorkerManager.cancelDailySync(this)
    
    // Optionally remove all synced events
    val calendarSync = CalendarSync(this, settingsRepository)
    if (calendarSync.hasPermissions()) {
        calendarSync.removeAllSyncedEvents()
    }
}

suspend fun Context.triggerCalendarSync() {
    CalendarWorkerManager.requestImmediateSync(this)
}

/**
 * Calendar sync observer for UI
 */
class CalendarSyncObserver(private val context: Context) {
    
    /**
     * Observe sync status changes
     */
    fun observeSyncStatus() = WorkManager.getInstance(context)
        .getWorkInfosForUniqueWorkLiveData(CalendarWorker.WORKER_NAME)

    /**
     * Get current sync status as flow
     */
    suspend fun getSyncStatusFlow() = kotlinx.coroutines.flow.flow {
        while (true) {
            val result = CalendarWorkerManager.getLastSyncResult(context)
            emit(result)
            kotlinx.coroutines.delay(5000) // Check every 5 seconds when observing
        }
    }
}

/**
 * Utilities for calendar worker
 */
object CalendarWorkerUtils {
    
    /**
     * Calculate next sync time based on current schedule
     */
    fun getNextSyncTime(): Long {
        val now = System.currentTimeMillis()
        val nextMidnight = java.time.LocalDateTime.now()
            .toLocalDate()
            .plusDays(1)
            .atTime(2, 0) // 2 AM next day
            .atZone(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        
        return nextMidnight
    }
    
    /**
     * Format sync result for display
     */
    fun formatSyncResult(result: CalendarWorkerResult): String {
        return when (result) {
            is CalendarWorkerResult.Success -> {
                val time = java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault())
                    .format(java.util.Date(result.syncTime))
                "Last sync: $time"
            }
            is CalendarWorkerResult.Failed -> "Sync failed: ${result.error}"
            is CalendarWorkerResult.Running -> "Syncing..."
            is CalendarWorkerResult.Pending -> "Sync scheduled"
            is CalendarWorkerResult.Blocked -> "Sync blocked"
            is CalendarWorkerResult.Cancelled -> "Sync cancelled"
        }
    }
    
    /**
     * Check if worker should run based on settings
     */
    suspend fun shouldRunSync(context: Context): Boolean {
        val settingsRepository = CalendarSettingsRepository(context.calendarDataStore)
        val prefs = settingsRepository.calendarPrefsFlow.first()
        val calendarSync = CalendarSync(context, settingsRepository)
        
        return prefs.enabled && 
               prefs.targetCalendarId != null && 
               calendarSync.hasPermissions()
    }
}
