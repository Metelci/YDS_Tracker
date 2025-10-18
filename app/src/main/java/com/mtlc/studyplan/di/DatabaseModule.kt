package com.mtlc.studyplan.di

import androidx.room.Room
import com.mtlc.studyplan.database.StudyPlanDatabase
import com.mtlc.studyplan.database.dao.AchievementDao
import com.mtlc.studyplan.database.dao.ProgressDao
import com.mtlc.studyplan.database.dao.StreakDao
import com.mtlc.studyplan.database.dao.TaskDao
import com.mtlc.studyplan.database.dao.UserSettingsDao
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val koinDatabaseModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            StudyPlanDatabase::class.java,
            "study_plan_database"
        )
            .fallbackToDestructiveMigration(true)
            .build()
    }

    single<TaskDao> { get<StudyPlanDatabase>().taskDao() }
    single<ProgressDao> { get<StudyPlanDatabase>().progressDao() }
    single<AchievementDao> { get<StudyPlanDatabase>().achievementDao() }
    single<StreakDao> { get<StudyPlanDatabase>().streakDao() }
    single<UserSettingsDao> { get<StudyPlanDatabase>().settingsDao() }
}
