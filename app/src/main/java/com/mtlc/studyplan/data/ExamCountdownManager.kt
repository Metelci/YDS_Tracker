package com.mtlc.studyplan.data

import android.content.Context
import androidx.work.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * Manager for exam countdown updates and background refresh
 * Ensures the countdown is updated daily and exam data is current
 */
class ExamCountdownManager(private val context: Context) {

    private val _examData = MutableStateFlow(createCurrentExamData())
    val examData: StateFlow<ExamTracker> = _examData.asStateFlow()

    init {
        // Schedule daily countdown updates
        scheduleDailyCountdownUpdate()

        // Initial update
        updateExamData()
    }

    /**
     * Create current exam data using YdsExamService
     */
    private fun createCurrentExamData(): ExamTracker {
        return ExamTracker(
            targetScore = 80,
            currentPreparationLevel = calculateCurrentPreparationLevel()
        )
    }

    /**
     * Calculate preparation level based on time remaining
     * More time = lower preparation level (realistic assumption)
     */
    private fun calculateCurrentPreparationLevel(): Float {
        val daysToExam = YdsExamService.getDaysToNextExam()
        return when {
            daysToExam <= 0 -> 1.0f // Exam day or passed
            daysToExam <= 30 -> 0.8f // Final month - high preparation
            daysToExam <= 90 -> 0.6f // 3 months - good preparation
            daysToExam <= 180 -> 0.4f // 6 months - moderate preparation
            else -> 0.1f // Long term - early preparation
        }
    }

    /**
     * Update exam data (call this when app becomes active)
     */
    fun updateExamData() {
        _examData.value = createCurrentExamData()
    }

    /**
     * Schedule daily background updates for countdown
     */
    private fun scheduleDailyCountdownUpdate() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(true)
            .build()

        val dailyWorkRequest = PeriodicWorkRequestBuilder<ExamCountdownWorker>(
            1, TimeUnit.DAYS
        )
            .setConstraints(constraints)
            .setInitialDelay(calculateInitialDelay(), TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "exam_countdown_update",
            ExistingPeriodicWorkPolicy.REPLACE,
            dailyWorkRequest
        )
    }

    /**
     * Calculate delay until next midnight (when day changes)
     */
    private fun calculateInitialDelay(): Long {
        val now = LocalDateTime.now()
        val nextMidnight = now.plusDays(1).withHour(0).withMinute(1).withSecond(0).withNano(0)
        return java.time.Duration.between(now, nextMidnight).toMillis()
    }

    /**
     * Force refresh exam data (useful for testing or manual refresh)
     */
    fun forceRefresh() {
        updateExamData()
    }

    companion object {
        @Volatile
        private var INSTANCE: ExamCountdownManager? = null

        fun getInstance(context: Context): ExamCountdownManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ExamCountdownManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

/**
 * Background worker to update exam countdown daily
 */
class ExamCountdownWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        return try {
            // Update exam data in the background
            val manager = ExamCountdownManager.getInstance(applicationContext)
            manager.updateExamData()

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}