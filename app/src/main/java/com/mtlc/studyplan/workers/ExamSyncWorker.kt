package com.mtlc.studyplan.workers

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import com.mtlc.studyplan.repository.ExamRepository
import java.util.concurrent.TimeUnit

/**
 * Worker that periodically syncs exam data from ÖSYM website
 * Runs daily to check for new exam announcements or updates
 */
class ExamSyncWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val examRepository: ExamRepository,
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "exam_sync"
        private const val SYNC_INTERVAL_HOURS = 24L // Sync once per day

        /**
         * Schedule periodic exam data sync
         */
        fun schedule(context: Context) {
            val workManager = WorkManager.getInstance(context)

            val syncRequest = PeriodicWorkRequestBuilder<ExamSyncWorker>(
                repeatInterval = SYNC_INTERVAL_HOURS,
                repeatIntervalTimeUnit = TimeUnit.HOURS,
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED) // Require internet
                        .setRequiresBatteryNotLow(true) // Avoid sync when battery is low
                        .setRequiresStorageNotLow(true) // Require sufficient storage
                        .build(),
                )
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS,
                )
                .build()

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP, // Keep existing work if already scheduled
                syncRequest,
            )
        }

        /**
         * Cancel exam sync worker
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }

    override suspend fun doWork(): Result {
        return try {
            // Test network connectivity first
            val isConnected = examRepository.testConnection()
            if (!isConnected) {
                // Network not available, retry later
                return Result.retry()
            }

            // Sync exams from ÖSYM website
            val syncResult = examRepository.syncExamsFromOsym()

            if (syncResult.isSuccess) {
                val examCount = syncResult.getOrDefault(0)
                // Successfully synced exams
                // Note: New exam notifications will be handled by ExamNotificationService
                Result.success()
            } else {
                // Sync failed, retry with backoff
                Result.retry()
            }
        } catch (e: Exception) {
            // Unexpected error, retry
            Result.retry()
        }
    }
}
