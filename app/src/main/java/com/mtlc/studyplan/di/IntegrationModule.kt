package com.mtlc.studyplan.di

import android.content.Context
import com.mtlc.studyplan.integration.EnhancedAppIntegrationManager
import com.mtlc.studyplan.repository.*
import com.mtlc.studyplan.eventbus.EventBus
import kotlinx.coroutines.CoroutineScope
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object IntegrationModule {

    @Provides
    @Singleton
    fun provideEnhancedAppIntegrationManager(
        @ApplicationContext context: Context,
        taskRepository: TaskRepository,
        progressRepository: ProgressRepository,
        achievementRepository: AchievementRepository,
        streakRepository: StreakRepository,
        userSettingsRepository: UserSettingsRepository,
        socialRepository: SocialRepository,
        eventBus: EventBus,
        @ApplicationScope applicationScope: CoroutineScope
    ): EnhancedAppIntegrationManager {
        return EnhancedAppIntegrationManager(
            context = context,
            taskRepository = taskRepository,
            progressRepository = progressRepository,
            achievementRepository = achievementRepository,
            streakRepository = streakRepository,
            userSettingsRepository = userSettingsRepository,
            socialRepository = socialRepository,
            eventBus = eventBus,
            applicationScope = applicationScope
        )
    }
}