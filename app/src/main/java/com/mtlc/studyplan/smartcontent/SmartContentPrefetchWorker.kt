package com.mtlc.studyplan.smartcontent

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.mtlc.studyplan.analytics.AnalyticsEngine
import com.mtlc.studyplan.data.ProgressRepository
import com.mtlc.studyplan.data.dataStore
import com.mtlc.studyplan.questions.QuestionService
import com.mtlc.studyplan.questions.VocabularyManager
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Background prefetch to prepare content during charging, improving perceived performance.
 */
class SmartContentPrefetchWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return runCatching {
            val ctx = applicationContext
            val repo = ProgressRepository(ctx.dataStore)
            val generator = QuestionService.buildGenerator(ctx, repo, ctx.dataStore)
            val vocab = VocabularyManager(ctx, repo)
            val smart = SmartContentManager(
                context = ctx,
                questionGenerator = generator,
                vocabularyManager = vocab,
                progressRepository = repo,
                smartScheduler = com.mtlc.studyplan.ai.SmartScheduler()
            )

            // Warm caches: daily packs for typical time budgets and recommendations
            listOf(15, 25, 40).forEach { mins ->
                runCatching { smart.generateDailyContentPack(mins) }
            }
            runCatching { smart.getContentRecommendations() }
            Result.success()
        }.getOrElse { Result.retry() }
    }

    companion object {
        private const val UNIQUE_WORK = "smart_content_prefetch"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED) // offline-first
                .setRequiresCharging(true)
                .build()

            val request = PeriodicWorkRequestBuilder<SmartContentPrefetchWorker>(12, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }
}

