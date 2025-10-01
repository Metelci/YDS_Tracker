package com.mtlc.studyplan

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mtlc.studyplan.navigation.AppNavHost
import com.mtlc.studyplan.shared.SharedAppViewModel
import com.mtlc.studyplan.data.PlanDataSource
import com.mtlc.studyplan.ui.base.LocaleAwareActivity
import com.mtlc.studyplan.theme.StudyPlanTheme
import com.mtlc.studyplan.theme.ThemeManager
import org.koin.android.ext.android.inject

class MinimalMainActivity : LocaleAwareActivity() {

    private val themeManager: ThemeManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge display
        enableEdgeToEdge()

        // Initialize PlanDataSource before creating ViewModels
        PlanDataSource.initialize(this)

        setContent {
            // Observe theme changes
            val isDarkTheme by themeManager.isDarkTheme.collectAsState(initial = isSystemInDarkTheme())

            // Update status bar appearance based on theme
            LaunchedEffect(isDarkTheme) {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    // In light mode: dark icons on light background
                    // In dark mode: light icons on dark background
                    isAppearanceLightStatusBars = !isDarkTheme
                    isAppearanceLightNavigationBars = !isDarkTheme
                }

                // Set status bar color with slight transparency for better separation
                window.statusBarColor = if (isDarkTheme) {
                    Color(0xFF1C1B1F).copy(alpha = 0.95f).toArgb()
                } else {
                    Color(0xFFFFFBFE).copy(alpha = 0.95f).toArgb()
                }

                // Set navigation bar color
                window.navigationBarColor = if (isDarkTheme) {
                    Color(0xFF1C1B1F).copy(alpha = 0.95f).toArgb()
                } else {
                    Color(0xFFFFFBFE).copy(alpha = 0.95f).toArgb()
                }
            }

            StudyPlanTheme(themeManager = themeManager) {
                GradientBackground(isDarkTheme = isDarkTheme) {
                    val sharedViewModel: SharedAppViewModel = viewModel()
                    AppNavHost(
                        sharedViewModel = sharedViewModel,
                        themeManager = themeManager
                    )
                }
            }
        }
    }
}

@Composable
private fun GradientBackground(
    isDarkTheme: Boolean,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isDarkTheme) {
                        listOf(
                            Color(0xFF1C1B1F), // Dark grey at top
                            Color(0xFF1C1B1F)  // Dark grey at bottom
                        )
                    } else {
                        listOf(
                            Color(0xFFF5F5F5), // Light grey at top
                            Color(0xFFFFFFFF)  // White at bottom
                        )
                    },
                    startY = 0f,
                    endY = Float.POSITIVE_INFINITY
                )
            )
    ) {
        content()
    }
}