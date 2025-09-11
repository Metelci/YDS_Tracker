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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mtlc.studyplan.ui.theme.LocalSpacing
import com.mtlc.studyplan.ui.theme.Elevations
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.navigation.NavController
import com.mtlc.studyplan.ui.components.EmptyState
import com.mtlc.studyplan.ui.components.ErrorState
import androidx.compose.material.icons.outlined.EventNote
import kotlinx.coroutines.launch
import com.mtlc.studyplan.ui.a11y.largeTouchTarget
import com.mtlc.studyplan.ui.a11y.LocalReducedMotion

@Composable
fun TodayRoute(
    vm: TodayViewModel = viewModel(),
    onNavigateToPlan: () -> Unit = {},
    onNavigateToLesson: (String) -> Unit = {},
    onNavigateToMock: () -> Unit = {}
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) {
        vm.dispatch(TodayIntent.Load)
        com.mtlc.studyplan.metrics.Analytics.track(context, "today_open")
    }

    TodayScreen(
        state = state,
        onStart = {
            com.mtlc.studyplan.metrics.Analytics.track(context, "session_start", mapOf("id" to it))
            vm.dispatch(TodayIntent.StartSession(it))
            onNavigateToLesson(it)
        },
        onComplete = {
            com.mtlc.studyplan.metrics.Analytics.track(context, "session_complete", mapOf("id" to it))
            vm.dispatch(TodayIntent.Complete(it))
        },
        onSkip = {
            com.mtlc.studyplan.metrics.Analytics.track(context, "session_skip", mapOf("id" to it))
            vm.dispatch(TodayIntent.Skip(it))
        },
        onSnackbarShown = { vm.consumeSnackbar() },
        onViewPlan = onNavigateToPlan,
        onNavigateToMock = onNavigateToMock,
        onRefresh = { vm.dispatch(TodayIntent.Load) },
        onReschedule = { id, at -> vm.dispatch(TodayIntent.Reschedule(id, at)) }
    )
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.material.ExperimentalMaterialApi::class)
@Composable
fun TodayScreen(
    state: TodayUiState,
    modifier: Modifier = Modifier,
    onStart: (String) -> Unit,
    onComplete: (String) -> Unit,
    onSkip: (String) -> Unit,
    onSnackbarShown: () -> Unit,
    onViewPlan: () -> Unit = {},
    onNavigateToMock: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onReschedule: (String, java.time.LocalDateTime) -> Unit = {_, _ -> },
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(false) }
    val pullRefreshState = rememberPullRefreshState(refreshing = refreshing, onRefresh = {
        refreshing = true
        onRefresh()
        scope.launch {
            kotlinx.coroutines.delay(400)
            refreshing = false
        }
    })

    LaunchedEffect(state.snackbar) {
        state.snackbar?.let { msg ->
            scope.launch { snackbarHostState.showSnackbar(message = msg) }
            onSnackbarShown()
        }
    }

    val sTokens = LocalSpacing.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Today") },
                actions = {
                    TextButton(onClick = onViewPlan, modifier = Modifier.largeTouchTarget()) {
                        Text("View Full Plan")
                    }
                    TextButton(onClick = onNavigateToMock, modifier = Modifier.largeTouchTarget()) {
                        Text("Mock Exam")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (state.sessions.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { onStart(state.sessions.first().id) },
                    modifier = Modifier.semantics { contentDescription = "Start next session" }
                ) {
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
                state.snackbar?.let { it.contains("error", ignoreCase = true) } == true -> {
                    ErrorState(
                        modifier = Modifier.fillMaxSize(),
                        title = "Couldn't load today",
                        message = state.snackbar ?: "",
                        onRetry = onRefresh,
                        onDiagnostics = { }
                    )
                }
                state.sessions.isEmpty() -> {
                    EmptyState(
                        modifier = Modifier.fillMaxSize(),
                        icon = Icons.Outlined.EventNote,
                        title = "No sessions today",
                        message = "Create or customize your plan to start.",
                        action = {
                            ElevatedButton(onClick = onViewPlan) { Text("View Plan") }
                        }
                    )
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
                                .padding(horizontal = sTokens.md, vertical = sTokens.xs),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(sTokens.sm)
                        ) {
                            Text(
                                "Time left today: ${timeLeft} min",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            AssistChip(
                                onClick = { /*no-op*/ },
                                label = { Text("Adherence ${adherence}%") },
                                modifier = Modifier.largeTouchTarget()
                            )
                        }
                        Box(
                            Modifier
                                .fillMaxSize()
                                .padding(horizontal = sTokens.md)
                                .pullRefresh(pullRefreshState)
                        ) {
                            LazyColumn(
                                modifier = modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(sTokens.sm)
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
                            PullRefreshIndicator(
                                refreshing = refreshing,
                                state = pullRefreshState,
                                modifier = Modifier.align(Alignment.TopCenter)
                            )
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
    Card(shape = MaterialTheme.shapes.large, elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = Elevations.level1)) {
        Column(Modifier.fillMaxWidth().padding(LocalSpacing.current.md)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    s.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                AssistChip(onClick = { /* no-op */ }, label = { Text(s.section) })
            }
            Spacer(Modifier.height(LocalSpacing.current.xs))
            Text("~${s.estMinutes} min  •  difficulty ${s.difficulty}", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(LocalSpacing.current.sm))
            var menuExpanded by remember { mutableStateOf(false) }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { onStart(s.id) }) { Text("Start") }
                TextButton(onClick = { onSkip(s.id) }) { Text("Skip") }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { menuExpanded = true }, modifier = Modifier.largeTouchTarget()) {
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
    val reducedMotion = LocalReducedMotion.current
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
    if (reducedMotion) {
        SessionCard(
            s = session,
            onStart = onStart,
            onComplete = onComplete,
            onSkip = { /* handled */ },
            onReschedule = onReschedule
        )
    } else {
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
