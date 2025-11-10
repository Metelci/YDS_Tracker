package com.mtlc.studyplan.di

import com.mtlc.studyplan.analytics.AnalyticsViewModel
import com.mtlc.studyplan.feature.onboarding.OnboardingViewModel
import com.mtlc.studyplan.feature.today.TodayViewModel
import com.mtlc.studyplan.settings.integration.AppIntegrationManager
import com.mtlc.studyplan.shared.SharedAppViewModel
import com.mtlc.studyplan.repository.TaskRepository
import org.koin.android.ext.koin.androidApplication
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val koinViewModelModule = module {
    viewModel {
        SharedAppViewModel(
            androidApplication(),
            get(),
            get(),
            get<AppIntegrationManager>(),
            get<TaskRepository>()
        )
    }
    viewModel { AnalyticsViewModel(get(), get(), get(), get()) }
    viewModel { OnboardingViewModel(get(), get()) }
    viewModel { TodayViewModel(get(), get(named("IoDispatcher"))) }
}
