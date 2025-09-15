package com.mtlc.studyplan.feature.welcome

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.mtlc.studyplan.data.AppPrefsRepository
import com.mtlc.studyplan.data.dataStore
import com.mtlc.studyplan.ui.theme.LocalSpacing

const val WELCOME_ROUTE = "welcome"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeRoute(onDone: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val repo = remember { AppPrefsRepository((context as android.content.Context).dataStore) }
    val seen by repo.hasSeenWelcome.collectAsState(initial = false)
    val scope = rememberCoroutineScope()

    LaunchedEffect(seen) {
        if (seen) onDone()
    }

    WelcomeScreen(onGetStarted = {
        scope.launch {
            repo.setSeenWelcome()
            onDone()
        }
    })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WelcomeScreen(onGetStarted: () -> Unit) {
    val s = LocalSpacing.current
    Scaffold(topBar = { TopAppBar(title = { Text("Welcome") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(s.lg),
            verticalArrangement = Arrangement.spacedBy(s.md)
        ) {
            Icon(
                imageVector = Icons.Outlined.School,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.height(48.dp)
            )
            Text("Road to YDS", style = MaterialTheme.typography.headlineSmall)
            Text(
                "A focused 30‑week program with daily sessions. Personalize your plan, practice efficiently, and see honest progress.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text("• Today: your daily sessions", style = MaterialTheme.typography.bodyMedium)
            Text("• Practice: sets and drills", style = MaterialTheme.typography.bodyMedium)
            Text("• Progress: stats and insights", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(s.md))
            Button(onClick = onGetStarted) { Text("Get started") }
        }
    }
}
