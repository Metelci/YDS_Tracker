package com.mtlc.studyplan.settings.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mtlc.studyplan.settings.data.SettingsRepository

/**
 * Factory for creating SettingsDetailViewModel with dependencies
 */
class SettingsDetailViewModelFactory(
    private val repository: SettingsRepository,
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsDetailViewModel::class.java)) {
            return SettingsDetailViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}