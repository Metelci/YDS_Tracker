package com.mtlc.studyplan

import android.app.Application
import android.content.ComponentCallbacks2
import androidx.work.Configuration
import androidx.work.WorkManager
import com.mtlc.studyplan.architecture.ArchitectureOptimizer
import com.mtlc.studyplan.architecture.FeatureModuleManager
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
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Koin with all modules to maintain compatibility
        startKoin {
            androidLogger(Level.INFO)
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
        
        WorkManager.initialize(this, workManagerConfiguration)

        // Initialize architecture optimizer
        architectureOptimizer.initialize()
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
    
    override fun onLowMemory() {
        super.onLowMemory()
        // System is running critically low on memory
        architectureOptimizer.onLowMemory()
    }
}