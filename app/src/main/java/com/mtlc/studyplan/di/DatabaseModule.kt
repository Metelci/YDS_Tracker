package com.mtlc.studyplan.di

import android.content.Context
import androidx.room.Room
import com.mtlc.studyplan.database.StudyPlanDatabase
import com.mtlc.studyplan.database.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideStudyPlanDatabase(
        @ApplicationContext context: Context
    ): StudyPlanDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            StudyPlanDatabase::class.java,
            "study_plan_database"
        )
        .addMigrations(/* add migrations as needed */)
        .fallbackToDestructiveMigration() // Only for development
        .build()
    }

    @Provides
    fun provideTaskDao(database: StudyPlanDatabase): TaskDao {
        return database.taskDao()
    }

    @Provides
    fun provideProgressDao(database: StudyPlanDatabase): ProgressDao {
        return database.progressDao()
    }

    @Provides
    fun provideAchievementDao(database: StudyPlanDatabase): AchievementDao {
        return database.achievementDao()
    }

    @Provides
    fun provideStreakDao(database: StudyPlanDatabase): StreakDao {
        return database.streakDao()
    }

    @Provides
    fun provideUserSettingsDao(database: StudyPlanDatabase): UserSettingsDao {
        return database.settingsDao()
    }

    @Provides
    fun provideSocialDao(database: StudyPlanDatabase): SocialDao {
        return database.socialDao()
    }
}