package com.mtlc.studyplan.ui

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.mtlc.studyplan.settings.data.UserSettings
import com.mtlc.studyplan.settings.manager.SettingsManager
import com.mtlc.studyplan.utils.HapticFeedbackManager
import com.mtlc.studyplan.utils.HapticType

val LocalUserSettings = compositionLocalOf { UserSettings.default() }

@Composable
fun SettingsAwareContent(
    settingsManager: SettingsManager,
    content: @Composable (UserSettings) -> Unit
) {
    val currentSettings by settingsManager.currentSettings.collectAsState()

    CompositionLocalProvider(
        LocalUserSettings provides currentSettings
    ) {
        content(currentSettings)
    }
}

@Composable
fun shouldShowGamification(): Boolean {
    val settings = LocalUserSettings.current
    return settings.gamificationEnabled
}

@Composable
fun shouldShowStreaks(): Boolean {
    val settings = LocalUserSettings.current
    return settings.gamificationEnabled && settings.streakWarningsEnabled
}

@Composable
fun shouldEnableHapticFeedback(): Boolean {
    val settings = LocalUserSettings.current
    return settings.hapticFeedbackEnabled
}

@Composable
fun shouldShowSocialFeatures(): Boolean {
    val settings = LocalUserSettings.current
    return settings.socialSharingEnabled
}

@Composable
fun shouldUseSmartScheduling(): Boolean {
    val settings = LocalUserSettings.current
    return settings.smartSchedulingEnabled
}

@Composable
fun SettingsAwareButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    hapticType: HapticType = HapticType.LIGHT_CLICK,
    content: @Composable RowScope.() -> Unit
) {
    val settings = LocalUserSettings.current
    val context = LocalContext.current

    Button(
        onClick = {
            if (settings.hapticFeedbackEnabled) {
                HapticFeedbackManager.performHapticFeedback(context, hapticType)
            }
            onClick()
        },
        modifier = modifier,
        enabled = enabled,
        content = content
    )
}

@Composable
fun SettingsAwareFilledTonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    hapticType: HapticType = HapticType.LIGHT_CLICK,
    content: @Composable RowScope.() -> Unit
) {
    val settings = LocalUserSettings.current
    val context = LocalContext.current

    FilledTonalButton(
        onClick = {
            if (settings.hapticFeedbackEnabled) {
                HapticFeedbackManager.performHapticFeedback(context, hapticType)
            }
            onClick()
        },
        modifier = modifier,
        enabled = enabled,
        content = content
    )
}

@Composable
fun SettingsAwareOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    hapticType: HapticType = HapticType.LIGHT_CLICK,
    content: @Composable RowScope.() -> Unit
) {
    val settings = LocalUserSettings.current
    val context = LocalContext.current

    OutlinedButton(
        onClick = {
            if (settings.hapticFeedbackEnabled) {
                HapticFeedbackManager.performHapticFeedback(context, hapticType)
            }
            onClick()
        },
        modifier = modifier,
        enabled = enabled,
        content = content
    )
}

@Composable
fun SettingsAwareTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    hapticType: HapticType = HapticType.LIGHT_CLICK,
    content: @Composable RowScope.() -> Unit
) {
    val settings = LocalUserSettings.current
    val context = LocalContext.current

    TextButton(
        onClick = {
            if (settings.hapticFeedbackEnabled) {
                HapticFeedbackManager.performHapticFeedback(context, hapticType)
            }
            onClick()
        },
        modifier = modifier,
        enabled = enabled,
        content = content
    )
}

@Composable
fun SettingsAwareFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hapticType: HapticType = HapticType.MEDIUM_CLICK,
    content: @Composable () -> Unit
) {
    val settings = LocalUserSettings.current
    val context = LocalContext.current

    FloatingActionButton(
        onClick = {
            if (settings.hapticFeedbackEnabled) {
                HapticFeedbackManager.performHapticFeedback(context, hapticType)
            }
            onClick()
        },
        modifier = modifier,
        content = content
    )
}

@Composable
fun SettingsAwareCard(
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    hapticType: HapticType = HapticType.LIGHT_CLICK,
    content: @Composable ColumnScope.() -> Unit
) {
    val settings = LocalUserSettings.current
    val context = LocalContext.current

    if (onClick != null) {
        Card(
            onClick = {
                if (settings.hapticFeedbackEnabled) {
                    HapticFeedbackManager.performHapticFeedback(context, hapticType)
                }
                onClick()
            },
            modifier = modifier,
            enabled = enabled,
            content = content
        )
    } else {
        Card(
            modifier = modifier,
            content = content
        )
    }
}
