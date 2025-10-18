package com.mtlc.studyplan.di

import com.mtlc.studyplan.notifications.NotificationManager
import com.mtlc.studyplan.notifications.PushNotificationConfig
import com.mtlc.studyplan.notifications.PushNotificationManager
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val koinIntegrationModule = module {
    // Lightweight integration manager used by notifications
    // Provide data-layer TaskRepository implementation that bridges to DB repository
    single<com.mtlc.studyplan.data.TaskRepository> { com.mtlc.studyplan.data.TaskRepositoryImpl(get()) }
    single { com.mtlc.studyplan.integration.AppIntegrationManager(get()) }

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
