package com.mtlc.studyplan.di

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val koinSettingsModule = module {
    single { com.mtlc.studyplan.settings.data.SettingsRepository(androidContext()) }
    single { com.mtlc.studyplan.settings.manager.SettingsManager(get(), get()) }
}

