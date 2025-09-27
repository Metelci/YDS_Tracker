package com.mtlc.studyplan.theme

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Build
import com.mtlc.studyplan.settings.data.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
class ThemeManager(
    private val context: Context
) {

    private val _currentTheme = MutableStateFlow(ThemeMode.SYSTEM)
    val currentTheme: StateFlow<ThemeMode> = _currentTheme.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(isSystemInDarkTheme())
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private var systemThemeReceiver: BroadcastReceiver? = null

    init {
        observeSystemThemeChanges()
    }

    fun setTheme(themeMode: ThemeMode) {
        _currentTheme.value = themeMode
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

    fun cleanup() {
        systemThemeReceiver?.let { receiver ->
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {
                // Handle unregistration failure gracefully
            }
        }
        systemThemeReceiver = null
    }

    fun getEffectiveThemeMode(): ThemeMode {
        return when (_currentTheme.value) {
            ThemeMode.SYSTEM -> if (isSystemInDarkTheme()) ThemeMode.DARK else ThemeMode.LIGHT
            else -> _currentTheme.value
        }
    }
}