package com.mtlc.studyplan.feature.focus

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mtlc.studyplan.ui.a11y.LocalReducedMotion
import com.mtlc.studyplan.ui.theme.LocalSpacing
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusModeScreen(
    taskId: String? = null,
    taskTitle: String = "Focus Session",
    onExit: () -> Unit,
    focusManager: FocusSessionManager = remember { FocusSessionManager() }
) {
    val state by focusManager.state.collectAsStateWithLifecycle()
    val spacing = LocalSpacing.current
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val reducedMotion = LocalReducedMotion.current

    var showSettings by remember { mutableStateOf(false) }
    var showStats by remember { mutableStateOf(false) }

    // Handle session events
    LaunchedEffect(focusManager) {
        focusManager.events.collect { event ->
            when (event) {
                FocusSessionEvent.SessionStarted -> { /* Handle start */ }
                FocusSessionEvent.PhaseCompleted -> { /* Handle phase completion */ }
                else -> { /* Handle other events */ }
            }
        }
    }

    // Initialize with task info if provided
    LaunchedEffect(taskId, taskTitle) {
        if (taskId != null) {
            val updatedConfig = state.config.copy(
                taskId = taskId,
                taskTitle = taskTitle
            )
            focusManager.handleEvent(FocusEvent.UpdateConfig(updatedConfig))
        }
    }

    DisposableEffect(focusManager) {
        onDispose {
            focusManager.cleanup()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = when (state.currentPhase) {
                        SessionPhase.STUDY -> listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                        SessionPhase.SHORT_BREAK -> listOf(
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                        )
                        SessionPhase.LONG_BREAK -> listOf(
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                        )
                        SessionPhase.COMPLETED -> listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar
            FocusTopBar(
                taskTitle = state.config.taskTitle,
                currentSession = state.currentSession,
                completedSessions = state.completedSessions,
                onSettingsClick = { showSettings = true },
                onStatsClick = { showStats = true },
                onExitClick = onExit,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(spacing.xl))

            // Phase indicator
            PhaseIndicator(
                phase = state.currentPhase,
                sessionNumber = state.currentSession,
                modifier = Modifier.padding(bottom = spacing.lg)
            )

            // Timer display
            TimerDisplay(
                timeRemaining = state.timeRemaining,
                progress = state.progressPercentage,
                phase = state.currentPhase,
                isRunning = state.isRunning,
                isPaused = state.isPaused,
                reducedMotion = reducedMotion,
                modifier = Modifier
                    .size(280.dp)
                    .weight(1f)
            )

            Spacer(modifier = Modifier.height(spacing.lg))

            // Control buttons
            FocusControls(
                isRunning = state.isRunning,
                isPaused = state.isPaused,
                phase = state.currentPhase,
                onStartClick = { focusManager.handleEvent(FocusEvent.Start) },
                onPauseClick = { focusManager.handleEvent(FocusEvent.Pause) },
                onResumeClick = { focusManager.handleEvent(FocusEvent.Resume) },
                onStopClick = { focusManager.handleEvent(FocusEvent.Stop) },
                onSkipClick = { focusManager.handleEvent(FocusEvent.SkipBreak) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Settings sheet
        if (showSettings) {
            FocusSettingsSheet(
                config = state.config,
                onConfigUpdate = { newConfig ->
                    focusManager.handleEvent(FocusEvent.UpdateConfig(newConfig))
                },
                onDismiss = { showSettings = false }
            )
        }

        // Stats sheet
        if (showStats) {
            FocusStatsSheet(
                state = state,
                onDismiss = { showStats = false }
            )
        }
    }
}

@Composable
private fun FocusTopBar(
    taskTitle: String,
    currentSession: Int,
    completedSessions: Int,
    onSettingsClick: () -> Unit,
    onStatsClick: () -> Unit,
    onExitClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onExitClick) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Exit Focus Mode",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = taskTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Session $currentSession • $completedSessions completed",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row {
            IconButton(onClick = onStatsClick) {
                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = "Statistics",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun PhaseIndicator(
    phase: SessionPhase,
    sessionNumber: Int,
    modifier: Modifier = Modifier
) {
    val (text, color) = when (phase) {
        SessionPhase.STUDY -> "Focus Time" to MaterialTheme.colorScheme.primary
        SessionPhase.SHORT_BREAK -> "Short Break" to MaterialTheme.colorScheme.tertiary
        SessionPhase.LONG_BREAK -> "Long Break" to MaterialTheme.colorScheme.secondary
        SessionPhase.COMPLETED -> "Completed" to MaterialTheme.colorScheme.outline
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = color,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun TimerDisplay(
    timeRemaining: Int,
    progress: Float,
    phase: SessionPhase,
    isRunning: Boolean,
    isPaused: Boolean,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier
) {
    val minutes = timeRemaining / 60
    val seconds = timeRemaining % 60

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = if (reducedMotion) snap() else tween(1000),
        label = "Timer progress"
    )

    val pulseAnimation = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseAnimation.animateFloat(
        initialValue = 1f,
        targetValue = if (isRunning && !reducedMotion) 1.02f else 1f,
        animationSpec = infiniteRepeating(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse scale"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Progress ring
        CircularProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxSize()
                .scale(if (isRunning && !reducedMotion) pulseScale else 1f),
            strokeWidth = 8.dp,
            color = when (phase) {
                SessionPhase.STUDY -> MaterialTheme.colorScheme.primary
                SessionPhase.SHORT_BREAK -> MaterialTheme.colorScheme.tertiary
                SessionPhase.LONG_BREAK -> MaterialTheme.colorScheme.secondary
                SessionPhase.COMPLETED -> MaterialTheme.colorScheme.outline
            },
            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
        )

        // Timer text
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = String.format("%02d:%02d", minutes, seconds),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 48.sp
            )

            Text(
                text = when {
                    isPaused -> "Paused"
                    isRunning -> when (phase) {
                        SessionPhase.STUDY -> "Focus"
                        SessionPhase.SHORT_BREAK -> "Rest"
                        SessionPhase.LONG_BREAK -> "Break"
                        SessionPhase.COMPLETED -> "Done"
                    }
                    else -> "Ready"
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FocusControls(
    isRunning: Boolean,
    isPaused: Boolean,
    phase: SessionPhase,
    onStartClick: () -> Unit,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onStopClick: () -> Unit,
    onSkipClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing.md, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Skip button (only during breaks)
        if ((phase == SessionPhase.SHORT_BREAK || phase == SessionPhase.LONG_BREAK) && isRunning) {
            OutlinedButton(
                onClick = onSkipClick,
                modifier = Modifier.size(56.dp),
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Skip break"
                )
            }
        }

        // Main control button
        FloatingActionButton(
            onClick = when {
                !isRunning -> onStartClick
                isPaused -> onResumeClick
                else -> onPauseClick
            },
            modifier = Modifier.size(80.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(
                imageVector = when {
                    !isRunning -> Icons.Default.PlayArrow
                    isPaused -> Icons.Default.PlayArrow
                    else -> Icons.Default.Pause
                },
                contentDescription = when {
                    !isRunning -> "Start"
                    isPaused -> "Resume"
                    else -> "Pause"
                },
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }

        // Stop button
        if (isRunning || isPaused) {
            OutlinedButton(
                onClick = onStopClick,
                modifier = Modifier.size(56.dp),
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Stop"
                )
            }
        }
    }
}