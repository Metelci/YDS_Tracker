package com.mtlc.studyplan.di

import com.mtlc.studyplan.gamification.GamificationManager
import com.mtlc.studyplan.notifications.NotificationManager
import com.mtlc.studyplan.notifications.PushNotificationConfig
import com.mtlc.studyplan.notifications.PushNotificationManager
import com.mtlc.studyplan.utils.settingsDataStore
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val koinIntegrationModule = module {
    // Data layer bridge for task repository usage outside Room scopes
    single<com.mtlc.studyplan.data.TaskRepository> { com.mtlc.studyplan.data.TaskRepositoryImpl(get()) }
    single {
        com.mtlc.studyplan.integration.AppIntegrationManager(
            get<com.mtlc.studyplan.data.TaskRepository>(),
            get<com.mtlc.studyplan.settings.data.SettingsRepository>(),
            get<com.mtlc.studyplan.repository.UserSettingsRepository>()
        )
    }

    // Settings-driven integrations
    single { GamificationManager(androidContext().settingsDataStore) }
    single<com.mtlc.studyplan.settings.integration.AppIntegrationManager> {
        com.mtlc.studyplan.settings.integration.AppIntegrationManager(
            androidContext(),
            get(),
            get()
        )
    }

    // System notifications coordinator
    single { NotificationManager(androidContext(), get(), get(), get()) }

    // Push notification manager for FCM
    single {
        PushNotificationManager(
            PushNotificationConfig(
                context = androidContext(),
                settingsManager = get(),
                notificationManager = get(),
                fcmTokenManager = get(),
                pushMessageHandler = get(),
                pushAnalyticsManager = get(),
                batteryAwarePushManager = get()
            )
        )
    }
}

