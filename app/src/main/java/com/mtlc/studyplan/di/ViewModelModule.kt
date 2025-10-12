package com.mtlc.studyplan.di

import com.mtlc.studyplan.analytics.AnalyticsViewModel
import com.mtlc.studyplan.features.onboarding.OnboardingViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val koinViewModelModule = module {
    viewModel { AnalyticsViewModel(get()) }
    viewModel { OnboardingViewModel(get(), get()) }
}
