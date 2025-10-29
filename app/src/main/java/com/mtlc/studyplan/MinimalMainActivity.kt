package com.mtlc.studyplan

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import com.mtlc.studyplan.navigation.AppNavHost
import com.mtlc.studyplan.shared.SharedAppViewModel
import com.mtlc.studyplan.ui.theme.StudyPlanTheme
import com.mtlc.studyplan.ui.base.LocaleAwareActivity
import org.koin.androidx.compose.get
import org.koin.androidx.compose.koinViewModel

/**
 * Minimal MainActivity as the app's entry point.
 * Uses AppNavHost for proper navigation with bottom bar.
 */

class MinimalMainActivity : LocaleAwareActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val sharedViewModel: SharedAppViewModel = koinViewModel()
            val taskRepository: com.mtlc.studyplan.data.TaskRepository = get()
            val studyProgressRepository: com.mtlc.studyplan.data.StudyProgressRepository = get()
            val mainAppIntegrationManager: com.mtlc.studyplan.integration.AppIntegrationManager = get()
            val settingsIntegrationManager: com.mtlc.studyplan.settings.integration.AppIntegrationManager = get()

            StudyPlanTheme {
                // Apply robust system bar styling on supported devices
                val window = (this@MinimalMainActivity).window
                // Gradient background is light (pastel blue/lavender), so use dark status bar icons
                val useDarkIcons = true
                SideEffect {
                    // Draw behind system bars and control icon appearance explicitly
                    WindowCompat.setDecorFitsSystemWindows(window, false)
                    @Suppress("DEPRECATION")
                    window.statusBarColor = Color.Transparent.toArgb()
                    val controller = WindowInsetsControllerCompat(window, window.decorView)
                    controller.isAppearanceLightStatusBars = useDarkIcons
                }
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent // Allow gradient background to show through
                ) {
                    AppNavHost(
                        sharedViewModel = sharedViewModel,
                        mainAppIntegrationManager = mainAppIntegrationManager,
                        studyProgressRepository = studyProgressRepository,
                        taskRepository = taskRepository,
                        appIntegrationManager = settingsIntegrationManager
                    )
                }
            }
        }
    }
}
