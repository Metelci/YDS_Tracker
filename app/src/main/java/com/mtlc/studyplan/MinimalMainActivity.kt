package com.mtlc.studyplan

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mtlc.studyplan.navigation.AppNavHost
import com.mtlc.studyplan.settings.data.NavigationSettings
import com.mtlc.studyplan.settings.data.SettingsPreferencesManager
import com.mtlc.studyplan.shared.SharedAppViewModel
import com.mtlc.studyplan.data.PlanDataSource
import com.mtlc.studyplan.ui.base.LocaleAwareActivity
import com.mtlc.studyplan.ui.theme.StudyPlanTheme

class MinimalMainActivity : LocaleAwareActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize PlanDataSource before creating ViewModels
        PlanDataSource.initialize(this)

        setContent {
            // Get dark mode setting from preferences
            val settingsManager = remember { SettingsPreferencesManager(this@MinimalMainActivity) }
            val navSettings by settingsManager.navigationSettings.collectAsState(initial = NavigationSettings())

            // Use dark theme based on user preference
            val darkTheme = navSettings.darkMode

            StudyPlanTheme(darkTheme = darkTheme) {
                val sharedViewModel: SharedAppViewModel = viewModel()
                AppNavHost(
                    sharedViewModel = sharedViewModel
                )
            }
        }
    }
}