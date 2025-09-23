package com.mtlc.studyplan.settings.viewmodel

import android.content.Context
import com.mtlc.studyplan.core.ViewModelFactory
import com.mtlc.studyplan.settings.data.SettingsRepository

/**
 * Factory for creating SettingsDetailViewModel with dependencies
 */
class SettingsDetailViewModelFactory(
    private val repository: SettingsRepository,
    private val context: Context
) : ViewModelFactory<SettingsDetailViewModel>({ SettingsDetailViewModel(repository, context) })