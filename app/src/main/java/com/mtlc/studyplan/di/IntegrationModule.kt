package com.mtlc.studyplan.di

import com.mtlc.studyplan.integration.EnhancedAppIntegrationManager
import com.mtlc.studyplan.repository.*
import com.mtlc.studyplan.eventbus.EventBus
import com.mtlc.studyplan.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object IntegrationModule {

    @Provides
    @Singleton
    fun provideEnhancedAppIntegrationManager(
        taskRepository: TaskRepository,
        achievementRepository: AchievementRepository,
        streakRepository: StreakRepository,
        userSettingsRepository: UserSettingsRepository,
        socialRepository: SocialRepository,
        eventBus: EventBus,
        @ApplicationScope applicationScope: CoroutineScope
    ): EnhancedAppIntegrationManager {
        return EnhancedAppIntegrationManager(
            taskRepository = taskRepository,
            achievementRepository = achievementRepository,
            streakRepository = streakRepository,
            userSettingsRepository = userSettingsRepository,
            socialRepository = socialRepository,
            eventBus = eventBus,
            applicationScope = applicationScope
        )
    }
}