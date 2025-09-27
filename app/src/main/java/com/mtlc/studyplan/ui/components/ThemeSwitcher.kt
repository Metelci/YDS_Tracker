package com.mtlc.studyplan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.mtlc.studyplan.settings.data.ThemeMode

@Composable
fun ThemeSwitcher(
    currentTheme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val hapticFeedback = LocalHapticFeedback.current

    val (icon, contentDescription) = when (currentTheme) {
        ThemeMode.LIGHT -> Icons.Default.LightMode to "Switch to dark theme"
        ThemeMode.DARK -> Icons.Default.DarkMode to "Switch to system theme"
        ThemeMode.SYSTEM -> Icons.Default.Brightness6 to "Switch to light theme"
    }

    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                CircleShape
            )
            .clickable(
                role = Role.Button,
                onClickLabel = contentDescription
            ) {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                val nextTheme = when (currentTheme) {
                    ThemeMode.LIGHT -> ThemeMode.DARK
                    ThemeMode.DARK -> ThemeMode.SYSTEM
                    ThemeMode.SYSTEM -> ThemeMode.LIGHT
                }
                onThemeChange(nextTheme)
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}