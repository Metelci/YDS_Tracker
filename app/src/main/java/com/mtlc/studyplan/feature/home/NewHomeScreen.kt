@file:OptIn(ExperimentalMaterial3Api::class)
package com.mtlc.studyplan.feature.home

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface

/**
 * Temporary placeholder while the new home experience is rebuilt.
 */
@Composable
fun NewHomeScreen(
    sharedViewModel: com.mtlc.studyplan.shared.SharedAppViewModel? = null,
    navigationManager: com.mtlc.studyplan.navigation.StudyPlanNavigationManager? = null,
    settingsManager: com.mtlc.studyplan.settings.manager.SettingsManager? = null,
    offlineManager: com.mtlc.studyplan.offline.OfflineManager? = null,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Text(text = "New home screen placeholder – implementation pending")
    }
}
