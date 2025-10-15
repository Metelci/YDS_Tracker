package com.mtlc.studyplan.di

import com.mtlc.studyplan.analytics.AnalyticsViewModel
import com.mtlc.studyplan.feature.today.TodayViewModel
import com.mtlc.studyplan.features.onboarding.OnboardingViewModel
import com.mtlc.studyplan.settings.viewmodel.AccessibilityViewModel
import com.mtlc.studyplan.settings.viewmodel.AdvancedToggleViewModel
import com.mtlc.studyplan.settings.viewmodel.GamificationSettingsViewModel
import com.mtlc.studyplan.settings.viewmodel.MainSettingsViewModel
import com.mtlc.studyplan.settings.viewmodel.NotificationSettingsViewModel
import com.mtlc.studyplan.settings.viewmodel.PerformanceViewModel
import com.mtlc.studyplan.settings.viewmodel.PrivacySettingsViewModel
import com.mtlc.studyplan.settings.viewmodel.SettingsDetailViewModel
import com.mtlc.studyplan.settings.viewmodel.SettingsViewModel
import com.mtlc.studyplan.shared.SharedAppViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val koinViewModelModule = module {
    // Analytics
    viewModel { AnalyticsViewModel(get()) }

    // Onboarding
    viewModel { OnboardingViewModel(get(), get()) }
}
