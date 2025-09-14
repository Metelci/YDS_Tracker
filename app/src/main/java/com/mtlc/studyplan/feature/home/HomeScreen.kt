@file:OptIn(ExperimentalMaterial3Api::class)
package com.mtlc.studyplan.feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mtlc.studyplan.data.*
import com.mtlc.studyplan.data.Task as DataTask
import com.mtlc.studyplan.data.DayPlan as DataDayPlan
import com.mtlc.studyplan.data.WeekPlan as DataWeekPlan
import com.mtlc.studyplan.data.dataStore
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val settingsStore = remember { PlanSettingsStore(appContext.dataStore) }
    val overridesStore = remember { PlanOverridesStore(appContext.dataStore) }
    val planRepo = remember { PlanRepository(overridesStore, settingsStore) }
    val progressRepo = remember { ProgressRepository(appContext.dataStore) }

    val plan by planRepo.planFlow.collectAsState(initial = emptyList())
    val progress by progressRepo.userProgressFlow.collectAsState(initial = UserProgress())

    val today = remember { LocalDate.now() }
    val settings by settingsStore.settingsFlow.collectAsState(initial = PlanDurationSettings())

    val totalTasks = remember(plan) { plan.flatMap { it.days }.flatMap { it.tasks }.size }
    val completedInPlan = remember(plan, progress) {
        val ids = plan.flatMap { it.days }.flatMap { it.tasks }.map { it.id }.toSet()
        progress.completedTasks.count { it in ids }
    }
    val progressRatio = if (totalTasks > 0) completedInPlan.toFloat() / totalTasks else 0f

    val absIndex = remember(settings.startEpochDay, today) {
        val start = LocalDate.ofEpochDay(settings.startEpochDay)
        val diff = ChronoUnit.DAYS.between(start, today).toInt()
        diff.coerceAtLeast(0)
    }
    val todayTasks = remember(plan, absIndex) {
        var idx = absIndex
        for (week in plan) {
            if (idx < week.days.size) {
                return@remember week.week to week.days[idx].tasks
            } else idx -= week.days.size
        }
        null
    }

    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(text = "Home", fontWeight = FontWeight.Bold) })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Progress ring for today's plan
            item {
                val tasks = todayTasks?.second.orEmpty()
                val plannedMinutes = remember(tasks) {
                    tasks.sumOf { com.mtlc.studyplan.ui.components.estimateTaskMinutes(it.desc, it.details) }
                }
                val completedMinutes = remember(tasks, progress.completedTasks) {
                    tasks.filter { it.id in progress.completedTasks }
                        .sumOf { com.mtlc.studyplan.ui.components.estimateTaskMinutes(it.desc, it.details) }
                }
                val ratio = remember(plannedMinutes, completedMinutes) {
                    if (plannedMinutes > 0) (completedMinutes.toFloat() / plannedMinutes).coerceIn(0f, 1f) else 0f
                }
                Box(Modifier.fillMaxWidth()) {
                    com.mtlc.studyplan.ui.components.ProgressRing(
                        progress = ratio,
                        label = "Today ${(ratio * 100).toInt()}%",
                        onConfettiPlayed = { /* could show a toast/snackbar if desired */ },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
            item {
                // Exam countdown
                val nextExam = ExamCalendarDataSource.getNextExam()
                if (nextExam != null) {
                    val daysToExam = ChronoUnit.DAYS.between(today, nextExam.examDate)
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Exam: ${nextExam.name}", style = MaterialTheme.typography.titleSmall)
                            Text("Date: ${nextExam.examDate}", style = MaterialTheme.typography.bodySmall)
                            LinearProgressIndicator(progress = { ((-daysToExam).coerceAtMost(0) / -1f).coerceIn(0f,1f) }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                            Text("Days left: ${daysToExam}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            item {
                // Streak and progress
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Streak: ${progress.streakCount} days")
                        LinearProgressIndicator(progress = { progressRatio }, modifier = Modifier.fillMaxWidth())
                        Text("Overall: ${completedInPlan} / ${totalTasks}")
                    }
                }
            }
            item {
                Text("Today’s Tasks", style = MaterialTheme.typography.titleMedium)
            }
            if (todayTasks != null) {
                val (weekNum, tasks) = todayTasks
                items(tasks, key = { it.id }) { t ->
                    val isDone = progress.completedTasks.contains(t.id)
                    ListItem(
                        headlineContent = { Text(t.desc) },
                        supportingContent = { if (t.details != null) Text(t.details!!) },
                        leadingContent = {
                            Checkbox(checked = isDone, onCheckedChange = {
                                coroutineScope.launch {
                                    val cur = progress.completedTasks.toMutableSet()
                                    if (isDone) cur.remove(t.id) else cur.add(t.id)
                                    progressRepo.saveProgress(progress.copy(completedTasks = cur))
                                }
                            })
                        }
                    )
                    HorizontalDivider()
                }
            } else {
                item { Text("No tasks scheduled for today.") }
            }
        }
    }
}
