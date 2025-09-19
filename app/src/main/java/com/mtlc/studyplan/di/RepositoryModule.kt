package com.mtlc.studyplan.di

import com.mtlc.studyplan.database.dao.*
import com.mtlc.studyplan.repository.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideTaskRepository(
        taskDao: TaskDao
    ): TaskRepository {
        return TaskRepository(taskDao)
    }

    @Provides
    @Singleton
    fun provideProgressRepository(
        progressDao: ProgressDao
    ): ProgressRepository {
        return ProgressRepository(progressDao)
    }

    @Provides
    @Singleton
    fun provideAchievementRepository(
        achievementDao: AchievementDao
    ): AchievementRepository {
        return AchievementRepository(achievementDao)
    }

    @Provides
    @Singleton
    fun provideStreakRepository(
        streakDao: StreakDao
    ): StreakRepository {
        return StreakRepository(streakDao)
    }

    @Provides
    @Singleton
    fun provideUserSettingsRepository(
        userSettingsDao: UserSettingsDao
    ): UserSettingsRepository {
        return UserSettingsRepository(userSettingsDao)
    }

    @Provides
    @Singleton
    fun provideSocialRepository(
        socialDao: SocialDao
    ): SocialRepository {
        return SocialRepository(socialDao)
    }
}