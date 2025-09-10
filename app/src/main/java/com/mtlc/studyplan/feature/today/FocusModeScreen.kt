package com.mtlc.studyplan.feature.today

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusModeScreen(
    sessionId: String,
    estMinutes: Int = 25,
    onExit: () -> Unit
) {
    var secondsLeft by remember { mutableIntStateOf(estMinutes * 60) }
    var running by remember { mutableStateOf(true) }
    var showExitConfirm by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }

    LaunchedEffect(running, finished) {
        if (running && !finished) {
            while (secondsLeft > 0) {
                delay(1000)
                secondsLeft -= 1
            }
            if (secondsLeft <= 0) {
                finished = true
            }
        }
    }

    if (showExitConfirm) {
        ExitDialog(onDismiss = { showExitConfirm = false }, onConfirm = onExit)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Focus Mode") },
                navigationIcon = {
                    IconButton(onClick = { showExitConfirm = true }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text("Session $sessionId", style = MaterialTheme.typography.titleLarge)
            BigTimer(secondsLeft)

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                FilledTonalButton(onClick = { running = !running }) {
                    Text(if (running) "Pause" else "Resume")
                }
                Button(onClick = { finished = true }) { Text("Finish") }
            }

            AnimatedVisibility(visible = finished) {
                Confetti()
            }
        }
    }
}

@Composable
private fun BigTimer(secondsLeft: Int) {
    val minutes = secondsLeft / 60
    val seconds = secondsLeft % 60
    val text = String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)
    Text(text, fontSize = 64.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun Confetti() {
    val infinite = rememberInfiniteTransition(label = "confetti")
    val alpha by infinite.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "alpha"
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        repeat(3) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.alpha(alpha)
            )
        }
    }
}

@Composable
private fun ExitDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { Button(onClick = onConfirm) { Text("Exit") } },
        dismissButton = { Text("Cancel", modifier = Modifier.padding(12.dp)) },
        title = { Text("Exit focus mode?") },
        text = { Text("Your timer will stop. You can resume later.") }
    )
}
