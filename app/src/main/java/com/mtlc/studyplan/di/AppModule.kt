package com.mtlc.studyplan.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.qualifier.named
import org.koin.dsl.module
import javax.inject.Qualifier

val koinAppModule = module {
    single<CoroutineDispatcher>(named("IoDispatcher")) { Dispatchers.IO }
    single<CoroutineDispatcher>(named("MainDispatcher")) { Dispatchers.Main }
    single<CoroutineDispatcher>(named("DefaultDispatcher")) { Dispatchers.Default }
    single<CoroutineDispatcher>(named("UnconfinedDispatcher")) { Dispatchers.Unconfined }

    single<CoroutineScope>(named("ApplicationScope")) {
        CoroutineScope(SupervisorJob() + get<CoroutineDispatcher>(named("DefaultDispatcher")))
    }

    single<Gson> { GsonBuilder().setPrettyPrinting().create() }
}

// Qualifier annotations for different dispatchers
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class UnconfinedDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
