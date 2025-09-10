package com.mtlc.studyplan.feature.today

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import kotlinx.coroutines.launch

@Composable
fun TodayRoute(
    vm: TodayViewModel = viewModel(),
    onNavigateToPlan: () -> Unit = {},
    onNavigateToLesson: (String) -> Unit = {}
) {
    val state by vm.state.collectAsStateWithLifecycle()

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
        onViewPlan = onNavigateToPlan,
        onRefresh = { vm.dispatch(TodayIntent.Load) },
        onReschedule = { id, at -> vm.dispatch(TodayIntent.Reschedule(id, at)) }
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
    onRefresh: () -> Unit = {},
    onReschedule: (String, java.time.LocalDateTime) -> Unit = {_, _ -> },
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(false) }
    val swipeRefreshState = com.google.accompanist.swiperefresh.rememberSwipeRefreshState(isRefreshing = refreshing)

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
                    Text("Start next")
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
                    val total = state.sessions.size.coerceAtLeast(1)
                    val completed = state.sessions.count { it.isCompleted }
                    val adherence = (completed * 100 / total)
                    val timeLeft = state.sessions.filter { !it.isCompleted }.sumOf { it.estMinutes }

                    Column(Modifier.fillMaxSize()) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "Time left today: ${timeLeft} min",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            AssistChip(onClick = { /*no-op*/ }, label = { Text("Adherence ${adherence}%") })
                        }
                        com.google.accompanist.swiperefresh.SwipeRefresh(
                            state = swipeRefreshState,
                            onRefresh = {
                                refreshing = true
                                onRefresh()
                                scope.launch {
                                    kotlinx.coroutines.delay(400)
                                    refreshing = false
                                }
                            },
                        ) {
                            LazyColumn(
                                modifier = modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(state.sessions, key = { it.id }) { item ->
                                    SwipeableSession(
                                        session = item,
                                        onStart = onStart,
                                        onComplete = onComplete,
                                        onReschedule = { at -> onReschedule(item.id, at) }
                                    )
                                }
                            }
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
    onSkip: (String) -> Unit,
    onReschedule: (java.time.LocalDateTime) -> Unit = {}
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
            var menuExpanded by remember { mutableStateOf(false) }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { onStart(s.id) }) { Text("Start") }
                TextButton(onClick = { onSkip(s.id) }) { Text("Skip") }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "More")
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(text = { Text("Reschedule later today") }, onClick = {
                        menuExpanded = false
                        onReschedule(java.time.LocalDateTime.now().plusHours(2))
                    })
                    DropdownMenuItem(text = { Text("Tomorrow") }, onClick = {
                        menuExpanded = false
                        onReschedule(java.time.LocalDateTime.now().plusDays(1))
                    })
                }
            }
        }
    }
}

@OptIn(androidx.compose.material.ExperimentalMaterialApi::class)
@Composable
private fun SwipeableSession(
    session: SessionUi,
    onStart: (String) -> Unit,
    onComplete: (String) -> Unit,
    onReschedule: (java.time.LocalDateTime) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val notificationManager = remember { context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager }
    val powerManager = remember { context.getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager }
    val dismissState = androidx.compose.material.rememberDismissState(
        confirmStateChange = { value ->
            when (value) {
                androidx.compose.material.DismissValue.DismissedToStart -> { // swipe left to complete
                    if (!powerManager.isPowerSaveMode && notificationManager.currentInterruptionFilter == android.app.NotificationManager.INTERRUPTION_FILTER_ALL) {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    }
                    onComplete(session.id)
                    true
                }
                androidx.compose.material.DismissValue.DismissedToEnd -> { // swipe right to reschedule
                    onReschedule(java.time.LocalDateTime.now().plusDays(1))
                    true
                }
                else -> false
            }
        }
    )
    androidx.compose.material.SwipeToDismiss(
        state = dismissState,
        background = {},
        dismissContent = {
            SessionCard(
                s = session,
                onStart = onStart,
                onComplete = onComplete,
                onSkip = { /* handled */ },
                onReschedule = onReschedule
            )
        }
    )
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
