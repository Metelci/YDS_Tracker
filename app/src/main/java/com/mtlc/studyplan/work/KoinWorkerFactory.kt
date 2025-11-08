package com.mtlc.studyplan.work

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.mtlc.studyplan.integration.AppIntegrationManager
import com.mtlc.studyplan.notifications.NotificationManager
import com.mtlc.studyplan.repository.ExamRepository
import com.mtlc.studyplan.workers.DailyStudyReminderWorker
import com.mtlc.studyplan.workers.ExamSyncWorker
import com.mtlc.studyplan.workers.ExamNotificationWorker
import com.mtlc.studyplan.workers.ReminderWorker
import org.koin.core.Koin

class KoinWorkerFactory(
    private val koin: Koin
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            DailyStudyReminderWorker::class.qualifiedName -> {
                val notificationManager: NotificationManager = koin.get()
                val appIntegrationManager: AppIntegrationManager = koin.get()
                DailyStudyReminderWorker(
                    appContext,
                    workerParameters,
                    notificationManager,
                    appIntegrationManager
                )
            }

            ReminderWorker::class.qualifiedName -> {
                val notificationManager: NotificationManager = koin.get()
                ReminderWorker(
                    appContext,
                    workerParameters,
                    notificationManager
                )
            }

            ExamSyncWorker::class.qualifiedName -> {
                val examRepository: ExamRepository = koin.get()
                ExamSyncWorker(
                    appContext,
                    workerParameters,
                    examRepository
                )
            }

            ExamNotificationWorker::class.qualifiedName -> {
                val examRepository: ExamRepository = koin.get()
                val notificationManager: NotificationManager = koin.get()
                ExamNotificationWorker(
                    appContext,
                    workerParameters,
                    examRepository,
                    notificationManager
                )
            }

            else -> null
        }
    }
}

