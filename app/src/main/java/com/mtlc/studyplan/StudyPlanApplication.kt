package com.mtlc.studyplan

import android.app.Application
import android.content.ComponentCallbacks2
import androidx.work.Configuration
import androidx.work.WorkManager
import com.mtlc.studyplan.architecture.ArchitectureOptimizer
import com.mtlc.studyplan.data.YdsExamService
import com.mtlc.studyplan.repository.ExamRepository
import com.mtlc.studyplan.services.NotificationSchedulerService
import com.mtlc.studyplan.workers.ExamSyncWorker
import com.mtlc.studyplan.BuildConfig
import com.mtlc.studyplan.utils.ApplicationContextProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import com.mtlc.studyplan.di.koinAppModule
import com.mtlc.studyplan.di.koinDatabaseModule
import com.mtlc.studyplan.di.koinEventBusModule
import com.mtlc.studyplan.di.koinIntegrationModule
import com.mtlc.studyplan.di.koinRepositoryModule
import com.mtlc.studyplan.di.koinSettingsModule
import com.mtlc.studyplan.di.koinViewModelModule
import com.mtlc.studyplan.work.KoinWorkerFactory
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class StudyPlanApplication : Application(), Configuration.Provider, ComponentCallbacks2 {
    private val architectureOptimizer by lazy { ArchitectureOptimizer(this) }
    private val examRepository: ExamRepository by inject()
    private val notificationSchedulerService: NotificationSchedulerService by inject()
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        ApplicationContextProvider.init(this)

        // Initialize Koin with all modules to maintain compatibility
        // Check if Koin is already started (for tests)
        if (GlobalContext.getOrNull() == null) {
        startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.INFO else Level.NONE)
            androidContext(this@StudyPlanApplication)
            modules(
                koinAppModule,
                koinDatabaseModule,
                koinRepositoryModule,
                koinEventBusModule,
                koinSettingsModule,
                koinIntegrationModule,
                koinViewModelModule
            )
        }
        }

        try {
            WorkManager.initialize(this, workManagerConfiguration)
        } catch (e: Exception) {
            // WorkManager already initialized or test environment
        }

        // Initialize architecture optimizer
        architectureOptimizer.initialize()

        // Initialize ÖSYM Integration
        initializeOsymIntegration()

        // Ensure notification workers are scheduled
        notificationSchedulerService.initializeNotificationScheduling()
    }

    private fun initializeOsymIntegration() {
        try {
            // Initialize YdsExamService with ExamRepository
            YdsExamService.initialize(examRepository)

            // Schedule periodic exam sync worker (runs daily)
            ExamSyncWorker.schedule(this)

            // Perform immediate sync if database is empty or stale
            applicationScope.launch {
                try {
                    val examCount = examRepository.getExamCount()
                    if (examCount == 0) {
                        // No exams in database, sync immediately
                        examRepository.syncExamsFromOsym()
                    }
                } catch (e: Exception) {
                    // Ignore errors, static fallback will be used
                }
            }
        } catch (e: Exception) {
            // Log error but don't crash the app
            e.printStackTrace()
        }
    }

    override val workManagerConfiguration: Configuration
        get() {
            val koin = GlobalContext.get()
            return Configuration.Builder()
                .setWorkerFactory(KoinWorkerFactory(koin))
                .build()
        }
        
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        
        // Forward to architecture optimizer
        architectureOptimizer.onTrimMemory(level)
    }
    
    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onLowMemory() {
        super.onLowMemory()
        // System is running critically low on memory
        architectureOptimizer.onLowMemory()
    }
}
