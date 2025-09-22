package com.mtlc.studyplan.feature.today

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.filled.CenterFocusWeak
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mtlc.studyplan.utils.settingsDataStore
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mtlc.studyplan.data.PlanDurationSettings
import com.mtlc.studyplan.data.PlanSettingsStore
import com.mtlc.studyplan.ui.a11y.largeTouchTarget
import com.mtlc.studyplan.ui.components.EmptyState
import com.mtlc.studyplan.ui.components.ErrorState
import com.mtlc.studyplan.ui.theme.Elevations
import com.mtlc.studyplan.ui.theme.LocalSpacing
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate


@Composable
fun TodayRoute(
    vm: TodayViewModel = viewModel(),
    onNavigateToPlan: () -> Unit = {},
    onNavigateToLesson: (String) -> Unit = {},
    onNavigateToMock: () -> Unit = {},
    onNavigateToFocus: (String) -> Unit = {}
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Read daily study budget from PlanSettings
    val appContext = LocalContext.current.applicationContext as Context
    val settingsStore = remember { PlanSettingsStore(appContext.settingsDataStore) }
    val settings by settingsStore.settingsFlow.collectAsState(initial = PlanDurationSettings())
    val today = remember { LocalDate.now().dayOfWeek }
    val dailyBudgetMinutes = remember(settings, today) {
        when (today) {
            DayOfWeek.MONDAY -> settings.monMinutes
            DayOfWeek.TUESDAY -> settings.tueMinutes
            DayOfWeek.WEDNESDAY -> settings.wedMinutes
            DayOfWeek.THURSDAY -> settings.thuMinutes
            DayOfWeek.FRIDAY -> settings.friMinutes
            DayOfWeek.SATURDAY -> settings.satMinutes
            DayOfWeek.SUNDAY -> settings.sunMinutes
        }
    }

    LaunchedEffect(Unit) {
        vm.dispatch(TodayIntent.Load)
        com.mtlc.studyplan.metrics.Analytics.track(context, "today_open")
    }

    TodayScreen(
        state = state,
        dailyBudgetMinutes = dailyBudgetMinutes,
        onStart = {
            com.mtlc.studyplan.metrics.Analytics.track(context, "session_start", mapOf("id" to it))
            vm.dispatch(TodayIntent.StartSession(it))
            onNavigateToLesson(it)
        },
        onSnackbarShown = { vm.consumeSnackbar() },
        onViewPlan = onNavigateToPlan,
        onNavigateToMock = onNavigateToMock,
        onRefresh = { vm.dispatch(TodayIntent.Load) },
        onReschedule = { id, at -> vm.dispatch(TodayIntent.Reschedule(id, at)) },
        onNavigateToFocus = onNavigateToFocus
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    state: TodayUiState,
    dailyBudgetMinutes: Int? = null,
    onStart: (String) -> Unit,
    onSnackbarShown: () -> Unit,
    onViewPlan: () -> Unit = {},
    onNavigateToMock: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onReschedule: (String, java.time.LocalDateTime) -> Unit = {_, _ -> },
    onNavigateToFocus: (String) -> Unit = {},
    showFloatingActionButton: Boolean = false,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    // Bind pull-to-refresh indicator to UI state
    val pullRefreshState = rememberPullToRefreshState()

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
            if (showFloatingActionButton && state.sessions.isNotEmpty()) {
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
                state.snackbar?.contains("error", ignoreCase = true) == true -> {
                    ErrorState(
                        modifier = Modifier.fillMaxSize(),
                        title = "Couldn't load today",
                        message = state.snackbar,
                        onRetry = onRefresh,
                        onDiagnostics = { }
                    )
                }
                state.sessions.isEmpty() -> {
                    EmptyState(
                        modifier = Modifier.fillMaxSize(),
                        icon = Icons.AutoMirrored.Outlined.EventNote,
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
                    val timePlanned = state.sessions.filter { !it.isCompleted }.sumOf { it.estMinutes }
                    val budget = dailyBudgetMinutes
                    val delta = budget?.let { it - timePlanned }

                    Column(Modifier.fillMaxSize()) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = sTokens.md, vertical = sTokens.xs),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(sTokens.sm)
                        ) {
                            Text(
                                if (budget != null) {
                                    // Planned vs. budget summary
                                    val deltaText = if (delta!! >= 0) "+${delta} min" else "$delta min"
                                    "Planned: $timePlanned min • Budget: $budget min • $deltaText"
                                } else {
                                    // Fallback
                                    "Planned today: $timePlanned min"
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )
                            AssistChip(
                                onClick = { /*no-op*/ },
                                label = { Text("Adherence ${adherence}%") },
                                modifier = Modifier.largeTouchTarget()
                            )
                        }
                        if (budget != null && budget > 0) {
                            val ratio = (timePlanned.toFloat() / budget).coerceIn(0f, 1f)
                            LinearProgressIndicator(
                                progress = { ratio },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = sTokens.md)
                            )
                        }
                        PullToRefreshBox(
                            state = pullRefreshState,
                            isRefreshing = false,
                            onRefresh = { onRefresh() }
                        ) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = sTokens.md),
                                verticalArrangement = Arrangement.spacedBy(sTokens.sm)
                            ) {
                                items(state.sessions, key = { it.id }) { item ->
                                    SwipeableSession(
                                        session = item,
                                        onStart = onStart,
                                        onReschedule = { at -> onReschedule(item.id, at) },
                                        onNavigateToFocus = onNavigateToFocus
                                    )
                                }
                                // Add a spacer at the end to account for the FAB
                                item {
                                    Spacer(modifier = Modifier.height(80.dp))
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
    onSkip: (String) -> Unit,
    onReschedule: (java.time.LocalDateTime) -> Unit = {},
    onNavigateToFocus: (String) -> Unit = {}
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
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { onStart(s.id) }) { Text("Start") }
                OutlinedButton(
                    onClick = {
                        // Navigate to Focus Mode for this task
                        onNavigateToFocus(s.id)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.CenterFocusWeak,
                        contentDescription = "Focus Mode",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Focus")
                }
                TextButton(onClick = { onSkip(s.id) }) { Text("Skip") }
            }
        }
    }
}

@Composable
private fun SwipeableSession(
    session: SessionUi,
    onStart: (String) -> Unit,
    onReschedule: (java.time.LocalDateTime) -> Unit,
    onNavigateToFocus: (String) -> Unit = {}
) {
    // TODO: Implement swipe-to-dismiss functionality for Material 3
    // For now, using basic card without swipe gestures
    SessionCard(
        s = session,
        onStart = onStart,
        onSkip = { /* swipe functionality removed */ },
        onReschedule = onReschedule,
        onNavigateToFocus = onNavigateToFocus
    )
}

@Preview(showBackground = true)
@Composable
private fun TodayScreenPreview() {
    val previewState = TodayUiState(isLoading = false, sessions = FakeTodayData.sessions)
    TodayScreen(
        state = previewState,
        dailyBudgetMinutes = 60,
        onStart = {}, onSnackbarShown = {}
    )
}




