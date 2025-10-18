package com.mtlc.studyplan.di

import com.mtlc.studyplan.database.dao.AchievementDao
import com.mtlc.studyplan.database.dao.StreakDao
import com.mtlc.studyplan.database.dao.TaskDao
import com.mtlc.studyplan.database.dao.UserSettingsDao
import com.mtlc.studyplan.data.OnboardingRepository
import com.mtlc.studyplan.data.PlanSettingsStore
import com.mtlc.studyplan.data.StudyProgressRepository
import com.mtlc.studyplan.repository.AchievementRepository
import com.mtlc.studyplan.repository.StreakRepository
import com.mtlc.studyplan.repository.TaskRepository
import com.mtlc.studyplan.repository.UserSettingsRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val koinRepositoryModule = module {
    single { TaskRepository(get<TaskDao>()) }
    single { AchievementRepository(get<AchievementDao>()) }
    single { StreakRepository(get<StreakDao>()) }
    single { UserSettingsRepository(get<UserSettingsDao>()) }
    single { StudyProgressRepository(androidContext()) }
    single { OnboardingRepository(get()) }
    single { PlanSettingsStore(get()) }
}
