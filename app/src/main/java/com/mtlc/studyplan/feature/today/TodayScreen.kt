package com.mtlc.studyplan.feature.today

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch

@Composable
fun TodayRoute(
    vm: TodayViewModel = viewModel(),
    onNavigateToPlan: () -> Unit = {},
    onNavigateToLesson: (String) -> Unit = {}
) {
    val state by vm.state.collectAsState() // avoids lifecycle-compose dep

    LaunchedEffect(Unit) { vm.dispatch(TodayIntent.Load) }

    TodayScreen(
        state = state,
        onStart = {
            vm.dispatch(TodayIntent.StartSession(it))
            onNavigateToLesson(it)
        },
        onComplete = { vm.dispatch(TodayIntent.Complete(it)) },
        onSkip = { vm.dispatch(TodayIntent.Skip(it)) },
        onSnackbarShown = { vm.consumeSnackbar() },
        onViewPlan = onNavigateToPlan
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    state: TodayUiState,
    onStart: (String) -> Unit,
    onComplete: (String) -> Unit,
    onSkip: (String) -> Unit,
    onSnackbarShown: () -> Unit,
    onViewPlan: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.snackbar) {
        state.snackbar?.let { msg ->
            scope.launch { snackbarHostState.showSnackbar(message = msg) }
            onSnackbarShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Today") },
                actions = {
                    TextButton(onClick = onViewPlan) {
                        Text("View Full Plan")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (state.sessions.isNotEmpty()) {
                FloatingActionButton(onClick = { onStart(state.sessions.first().id) }) {
                    Text("Start")
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.sessions.isEmpty() -> {
                    EmptyState(Modifier.fillMaxSize())
                }
                else -> {
                    LazyColumn(
                        modifier = modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.sessions, key = { it.id }) { item ->
                            SessionCard(item, onStart, onComplete, onSkip)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionCard(
    s: SessionUi,
    onStart: (String) -> Unit,
    onComplete: (String) -> Unit,
    onSkip: (String) -> Unit
) {
    Card {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    s.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                AssistChip(onClick = { /* no-op */ }, label = { Text(s.section) })
            }
            Spacer(Modifier.height(8.dp))
            Text("~${s.estMinutes} min  •  difficulty ${s.difficulty}", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { onStart(s.id) }) { Text("Start") }
                OutlinedButton(onClick = { onComplete(s.id) }, enabled = !s.isCompleted) {
                    Text(if (s.isCompleted) "Completed" else "Complete")
                }
                TextButton(onClick = { onSkip(s.id) }) { Text("Skip") }
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Text("No sessions today. Create a plan to get started.")
    }
}

@Preview(showBackground = true)
@Composable
private fun TodayScreenPreview() {
    val previewState = TodayUiState(isLoading = false, sessions = FakeTodayData.sessions)
    TodayScreen(
        state = previewState,
        onStart = {}, onComplete = {}, onSkip = {}, onSnackbarShown = {}
    )
}
