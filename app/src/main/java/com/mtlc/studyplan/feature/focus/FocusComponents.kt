package com.mtlc.studyplan.feature.focus

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mtlc.studyplan.ui.theme.LocalSpacing
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusSettingsSheet(
    config: FocusSessionConfig,
    onConfigUpdate: (FocusSessionConfig) -> Unit,
    onDismiss: () -> Unit
) {
    val spacing = LocalSpacing.current
    var studyDuration by remember { mutableStateOf(config.studyDuration.toFloat()) }
    var shortBreak by remember { mutableStateOf(config.shortBreak.toFloat()) }
    var longBreak by remember { mutableStateOf(config.longBreak.toFloat()) }
    var sessionsUntilLongBreak by remember { mutableStateOf(config.sessionsUntilLongBreak.toFloat()) }
    var enableBreaks by remember { mutableStateOf(config.enableBreaks) }
    var ambientSounds by remember { mutableStateOf(config.ambientSounds) }
    var blockNotifications by remember { mutableStateOf(config.blockNotifications) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(spacing.md)
                .padding(bottom = spacing.xl)
        ) {
            Text(
                text = "Focus Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = spacing.md)
            )

            // Study Duration
            SettingSlider(
                title = "Study Duration",
                value = studyDuration,
                onValueChange = { studyDuration = it },
                valueRange = 5f..60f,
                steps = 10,
                valueFormatter = { "${it.toInt()} min" }
            )

            Spacer(modifier = Modifier.height(spacing.md))

            // Enable Breaks Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Enable Breaks",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Include break periods between study sessions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = enableBreaks,
                    onCheckedChange = { enableBreaks = it }
                )
            }

            if (enableBreaks) {
                Spacer(modifier = Modifier.height(spacing.md))

                // Short Break Duration
                SettingSlider(
                    title = "Short Break",
                    value = shortBreak,
                    onValueChange = { shortBreak = it },
                    valueRange = 2f..15f,
                    steps = 12,
                    valueFormatter = { "${it.toInt()} min" }
                )

                Spacer(modifier = Modifier.height(spacing.md))

                // Long Break Duration
                SettingSlider(
                    title = "Long Break",
                    value = longBreak,
                    onValueChange = { longBreak = it },
                    valueRange = 10f..30f,
                    steps = 19,
                    valueFormatter = { "${it.toInt()} min" }
                )

                Spacer(modifier = Modifier.height(spacing.md))

                // Sessions until long break
                SettingSlider(
                    title = "Sessions Until Long Break",
                    value = sessionsUntilLongBreak,
                    onValueChange = { sessionsUntilLongBreak = it },
                    valueRange = 2f..8f,
                    steps = 5,
                    valueFormatter = { "${it.toInt()} sessions" }
                )
            }

            Spacer(modifier = Modifier.height(spacing.md))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(spacing.md))

            // Ambient Sounds
            SettingSwitch(
                title = "Ambient Sounds",
                subtitle = "Play focus sounds during study sessions",
                checked = ambientSounds,
                onCheckedChange = { ambientSounds = it },
                icon = Icons.AutoMirrored.Filled.VolumeUp
            )

            Spacer(modifier = Modifier.height(spacing.md))

            // Block Notifications
            SettingSwitch(
                title = "Block Notifications",
                subtitle = "Minimize distractions during focus sessions",
                checked = blockNotifications,
                onCheckedChange = { blockNotifications = it },
                icon = Icons.Default.DoNotDisturb
            )

            Spacer(modifier = Modifier.height(spacing.xl))

            // Save button
            Button(
                onClick = {
                    val updatedConfig = config.copy(
                        studyDuration = studyDuration.toInt(),
                        shortBreak = shortBreak.toInt(),
                        longBreak = longBreak.toInt(),
                        sessionsUntilLongBreak = sessionsUntilLongBreak.toInt(),
                        enableBreaks = enableBreaks,
                        ambientSounds = ambientSounds,
                        blockNotifications = blockNotifications
                    )
                    onConfigUpdate(updatedConfig)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Settings")
            }
        }
    }
}

@Composable
private fun SettingSlider(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueFormatter: (Float) -> String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = valueFormatter(value),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps
        )
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusStatsSheet(
    state: FocusSessionState,
    onDismiss: () -> Unit
) {
    val spacing = LocalSpacing.current
    val totalTimeMinutes = state.totalTimeElapsed / 60
    val efficiency = if (state.completedSessions > 0) {
        (state.completedSessions * state.config.studyDuration).toFloat() / maxOf(1, totalTimeMinutes) * 100
    } else 0f

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md)
                .padding(bottom = spacing.xl)
        ) {
            Text(
                text = "Session Statistics",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = spacing.md)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.md)
            ) {
                StatCard(
                    title = "Sessions",
                    value = state.completedSessions.toString(),
                    subtitle = "completed",
                    icon = Icons.Default.CheckCircle,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = "Total Time",
                    value = "${totalTimeMinutes}m",
                    subtitle = "studied",
                    icon = Icons.Default.Schedule,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(spacing.md))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.md)
            ) {
                StatCard(
                    title = "Efficiency",
                    value = "${efficiency.toInt()}%",
                    subtitle = "focus ratio",
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = "Current",
                    value = when (state.currentPhase) {
                        SessionPhase.STUDY -> "Study"
                        SessionPhase.SHORT_BREAK -> "Break"
                        SessionPhase.LONG_BREAK -> "Long Break"
                        SessionPhase.COMPLETED -> "Done"
                    },
                    subtitle = "phase",
                    icon = Icons.Default.PlayCircle,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.weight(1f)
                )
            }

            if (state.startTime != null) {
                Spacer(modifier = Modifier.height(spacing.md))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(spacing.md))

                InfoRow(
                    label = "Session Started",
                    value = state.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))
                )

                state.endTime?.let { endTime ->
                    InfoRow(
                        label = "Session Ended",
                        value = endTime.format(DateTimeFormatter.ofPattern("HH:mm"))
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
