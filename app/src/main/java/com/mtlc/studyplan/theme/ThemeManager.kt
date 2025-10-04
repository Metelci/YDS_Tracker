package com.mtlc.studyplan.theme

import android.content.BroadcastReceiver
import android.content.Context.MODE_PRIVATE
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Build
import com.mtlc.studyplan.settings.data.ThemeMode
import com.mtlc.studyplan.settings.data.SettingsKeys
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
class ThemeManager(
    private val context: Context
) {

    private val prefs = context.getSharedPreferences("study_plan_settings", MODE_PRIVATE)

    private val _currentTheme = MutableStateFlow(ThemeMode.SYSTEM)
    val currentTheme: StateFlow<ThemeMode> = _currentTheme.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(isSystemInDarkTheme())
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private var systemThemeReceiver: BroadcastReceiver? = null
    private var prefsListener: android.content.SharedPreferences.OnSharedPreferenceChangeListener? = null

    init {
        // Load persisted theme mode if available
        val saved = prefs.getString(SettingsKeys.Appearance.THEME_MODE, "system")
        _currentTheme.value = when (saved) {
            "light" -> ThemeMode.LIGHT
            "dark" -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
        updateEffectiveTheme()
        observeSystemThemeChanges()
        observePreferenceChanges()
    }

    fun setTheme(themeMode: ThemeMode) {
        _currentTheme.value = themeMode
        // Persist selection for future launches
        val value = when (themeMode) {
            ThemeMode.LIGHT -> "light"
            ThemeMode.DARK -> "dark"
            ThemeMode.SYSTEM -> "system"
        }
        prefs.edit().putString(SettingsKeys.Appearance.THEME_MODE, value).apply()
        updateEffectiveTheme()
    }

    private fun updateEffectiveTheme() {
        val isDark = when (_currentTheme.value) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
        }
        _isDarkTheme.value = isDark
    }

    private fun isSystemInDarkTheme(): Boolean {
        val configuration = context.resources.configuration
        return (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }

    private fun observeSystemThemeChanges() {
        if (systemThemeReceiver != null) return

        systemThemeReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (_currentTheme.value == ThemeMode.SYSTEM) {
                    updateEffectiveTheme()
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_CONFIGURATION_CHANGED)
        }

        try {
            context.registerReceiver(systemThemeReceiver, filter)
        } catch (e: Exception) {
            // Handle registration failure gracefully
        }
    }

    private fun observePreferenceChanges() {
        if (prefsListener != null) return

        prefsListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == SettingsKeys.Appearance.THEME_MODE) {
                val saved = prefs.getString(SettingsKeys.Appearance.THEME_MODE, "system")
                _currentTheme.value = when (saved) {
                    "light" -> ThemeMode.LIGHT
                    "dark" -> ThemeMode.DARK
                    else -> ThemeMode.SYSTEM
                }
                updateEffectiveTheme()
            }
        }
        try {
            prefs.registerOnSharedPreferenceChangeListener(prefsListener)
        } catch (_: Exception) { }
    }

    fun cleanup() {
        systemThemeReceiver?.let { receiver ->
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {
                // Handle unregistration failure gracefully
            }
        }
        systemThemeReceiver = null

        prefsListener?.let { listener ->
            try { prefs.unregisterOnSharedPreferenceChangeListener(listener) } catch (_: Exception) { }
        }
        prefsListener = null
    }

    fun getEffectiveThemeMode(): ThemeMode {
        return when (_currentTheme.value) {
            ThemeMode.SYSTEM -> if (isSystemInDarkTheme()) ThemeMode.DARK else ThemeMode.LIGHT
            else -> _currentTheme.value
        }
    }
}
