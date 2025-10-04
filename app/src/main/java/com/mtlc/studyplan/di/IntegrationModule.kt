package com.mtlc.studyplan.di

import com.mtlc.studyplan.eventbus.AppEventBus
import com.mtlc.studyplan.eventbus.EventBus
import com.mtlc.studyplan.integration.EnhancedAppIntegrationManager
import com.mtlc.studyplan.notifications.NotificationManager
import com.mtlc.studyplan.notifications.PushNotificationManager
import com.mtlc.studyplan.notifications.PushNotificationConfig
import com.mtlc.studyplan.repository.*
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
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

    // Enhanced integration layer
    single {
        EnhancedAppIntegrationManager(
            taskRepository = get<TaskRepository>(),
            achievementRepository = get<AchievementRepository>(),
            streakRepository = get<StreakRepository>(),
            userSettingsRepository = get<UserSettingsRepository>(),
            socialRepository = get<SocialRepository>(),
            eventBus = get<EventBus>(),
            applicationScope = get(qualifier = named("ApplicationScope"))
        )
    }
}
