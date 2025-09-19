package com.mtlc.studyplan.settings.integration

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtlc.studyplan.settings.data.SettingsKeys
import com.mtlc.studyplan.settings.data.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Integration layer between settings and app theming system
 */
class ThemeIntegration(
    private val context: Context,
    private val settingsRepository: SettingsRepository
) {

    data class ThemeState(
        val darkTheme: Boolean = false,
        val dynamicColor: Boolean = true,
        val accentColor: String = "blue",
        val fontSize: Float = 1.0f,
        val fontFamily: String = "default",
        val animationSpeed: Float = 1.0f,
        val reducedMotion: Boolean = false,
        val highContrast: Boolean = false
    )

    private val _themeState = MutableStateFlow(ThemeState())
    val themeState: StateFlow<ThemeState> = _themeState.asStateFlow()

    init {
        observeThemeSettings()
    }

    private fun observeThemeSettings() {
        settingsRepository.settingsState
            .map { settings ->
                ThemeState(
                    darkTheme = when (settings[SettingsKeys.Appearance.THEME_MODE] as? String) {
                        "dark" -> true
                        "light" -> false
                        else -> false // system default handled in compose
                    },
                    dynamicColor = settings[SettingsKeys.Appearance.ACCENT_COLOR] as? String != "custom",
                    accentColor = settings[SettingsKeys.Appearance.ACCENT_COLOR] as? String ?: "blue",
                    fontSize = when (settings[SettingsKeys.Appearance.FONT_SIZE] as? String) {
                        "small" -> 0.85f
                        "normal" -> 1.0f
                        "large" -> 1.15f
                        "xl" -> 1.3f
                        else -> 1.0f
                    },
                    fontFamily = settings[SettingsKeys.Appearance.FONT_FAMILY] as? String ?: "default",
                    animationSpeed = when (settings[SettingsKeys.Appearance.ANIMATION_SPEED] as? String) {
                        "disabled" -> 0f
                        "slow" -> 0.5f
                        "normal" -> 1.0f
                        "fast" -> 1.5f
                        else -> 1.0f
                    },
                    reducedMotion = settings[SettingsKeys.Accessibility.REDUCED_MOTION] as? Boolean ?: false,
                    highContrast = settings[SettingsKeys.Accessibility.HIGH_CONTRAST_MODE] as? Boolean ?: false
                )
            }
            .onEach { _themeState.value = it }
            .launchIn(kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main))
    }

    /**
     * Apply theme settings to the app
     */
    suspend fun updateThemeMode(mode: String) {
        settingsRepository.updateSetting(SettingsKeys.Appearance.THEME_MODE, mode)
    }

    suspend fun updateAccentColor(color: String) {
        settingsRepository.updateSetting(SettingsKeys.Appearance.ACCENT_COLOR, color)
    }

    suspend fun updateFontSize(size: String) {
        settingsRepository.updateSetting(SettingsKeys.Appearance.FONT_SIZE, size)
    }

    suspend fun updateAnimationSpeed(speed: String) {
        settingsRepository.updateSetting(SettingsKeys.Appearance.ANIMATION_SPEED, speed)
    }

    suspend fun toggleReducedMotion() {
        val current = _themeState.value.reducedMotion
        settingsRepository.updateSetting(SettingsKeys.Accessibility.REDUCED_MOTION, !current)
    }

    suspend fun toggleHighContrast() {
        val current = _themeState.value.highContrast
        settingsRepository.updateSetting(SettingsKeys.Accessibility.HIGH_CONTRAST_MODE, !current)
    }

    /**
     * Get theme mode for compose
     */
    fun getComposeThemeMode(): ThemeMode {
        return when (_themeState.value.darkTheme) {
            true -> ThemeMode.Dark
            false -> ThemeMode.Light
        }
    }

    /**
     * Check if we should use system theme
     */
    fun shouldUseSystemTheme(): Boolean {
        val themeMode = settingsRepository.settingsState.value[SettingsKeys.Appearance.THEME_MODE] as? String
        return themeMode == "system" || themeMode == null
    }
}

enum class ThemeMode {
    Light, Dark, System
}

/**
 * Composable that provides theme state
 */
@Composable
fun rememberThemeState(themeIntegration: ThemeIntegration): ThemeIntegration.ThemeState {
    return themeIntegration.themeState.collectAsState().value
}

/**
 * ViewModel for theme management in UI
 */
class ThemeViewModel(
    private val themeIntegration: ThemeIntegration
) : ViewModel() {

    val themeState = themeIntegration.themeState

    fun updateThemeMode(mode: String) {
        viewModelScope.launch {
            themeIntegration.updateThemeMode(mode)
        }
    }

    fun updateAccentColor(color: String) {
        viewModelScope.launch {
            themeIntegration.updateAccentColor(color)
        }
    }

    fun updateFontSize(size: String) {
        viewModelScope.launch {
            themeIntegration.updateFontSize(size)
        }
    }

    fun updateAnimationSpeed(speed: String) {
        viewModelScope.launch {
            themeIntegration.updateAnimationSpeed(speed)
        }
    }

    fun toggleReducedMotion() {
        viewModelScope.launch {
            themeIntegration.toggleReducedMotion()
        }
    }

    fun toggleHighContrast() {
        viewModelScope.launch {
            themeIntegration.toggleHighContrast()
        }
    }
}