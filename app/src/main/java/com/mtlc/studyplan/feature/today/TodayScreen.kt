package com.mtlc.studyplan.feature.today

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.filled.CenterFocusWeak
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mtlc.studyplan.data.PlanDurationSettings
import com.mtlc.studyplan.data.PlanSettingsStore
import com.mtlc.studyplan.ui.a11y.largeTouchTarget
import com.mtlc.studyplan.ui.components.EmptyState
import com.mtlc.studyplan.ui.components.StudyPlanTopBar
import com.mtlc.studyplan.ui.components.StudyPlanTopBarStyle
import com.mtlc.studyplan.ui.theme.*
import com.mtlc.studyplan.ui.theme.LocalSpacing
import com.mtlc.studyplan.utils.settingsDataStore
import java.time.DayOfWeek
import java.time.LocalDate

@Composable
fun TodayRoute(
    vm: TodayViewModel = viewModel(),
    onNavigateToFocus: (String) -> Unit = {}
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

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
        onStart = { id -> vm.dispatch(TodayIntent.StartSession(id)) },
        onNavigateToFocus = onNavigateToFocus
    )
}

@Composable
fun TodayScreen(
    state: TodayUiState,
    dailyBudgetMinutes: Int?,
    onStart: (String) -> Unit,
    onNavigateToFocus: (String) -> Unit = {}
) {
    val sTokens = LocalSpacing.current
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            StudyPlanTopBar(
                title = "Today",
                style = StudyPlanTopBarStyle.Default,
                navigationIcon = Icons.AutoMirrored.Outlined.EventNote
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { /* add */ }) {
                Icon(Icons.Default.MoreVert, contentDescription = "More")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.snackbar != null -> Text(state.snackbar)
            state.sessions.isEmpty() -> EmptyState()
            else -> {
                val timePlanned = state.sessions.sumOf { it.estMinutes }
                val budget = dailyBudgetMinutes
                val delta = budget?.let { budget - timePlanned }
                val adherence = budget?.let { if (it > 0) (timePlanned * 100 / it) else 0 } ?: 0

                Column(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = sTokens.md, vertical = sTokens.xs),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(sTokens.sm)
                    ) {
                        Text(
                            if (budget != null) {
                                val deltaText = if (delta!! >= 0) "+${delta} min" else "$delta min"
                                "Planned: $timePlanned min · Budget: $budget min · $deltaText"
                            } else {
                                "Planned today: $timePlanned min"
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                        AssistChip(
                            onClick = { },
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
                                onNavigateToFocus = onNavigateToFocus
                            )
                        }
                        item { Spacer(Modifier.height(80.dp)) }
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
    onNavigateToFocus: (String) -> Unit = {}
) {
    isSystemInDarkTheme()
    // Rotate through pastel palette for visual variety while keeping contrast
    // Use inferred feature from package for stability across moves
    val container = inferredFeaturePastelContainer("com.mtlc.studyplan.feature.today", s.id)

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = container),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevations.level1)
    ) {
        Column(Modifier.fillMaxWidth().padding(LocalSpacing.current.md)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    s.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                AssistChip(onClick = { }, label = { Text(s.section) })
            }
            Spacer(Modifier.height(LocalSpacing.current.xs))
            Text("~${'$'}{s.estMinutes} min  ·  difficulty ${'$'}{s.difficulty}", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(LocalSpacing.current.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { onStart(s.id) }) { Text("Start") }
                OutlinedButton(onClick = { onNavigateToFocus(s.id) }) {
                    Icon(imageVector = Icons.Default.CenterFocusWeak, contentDescription = "Focus Mode", modifier = Modifier.size(16.dp))
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
    onNavigateToFocus: (String) -> Unit = {}
) {
    SessionCard(
        s = session,
        onStart = onStart,
        onSkip = { },
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
        onStart = {},
        onNavigateToFocus = {}
    )
}






