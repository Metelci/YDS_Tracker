@file:OptIn(ExperimentalMaterial3Api::class)
package com.mtlc.studyplan.feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mtlc.studyplan.data.*
import com.mtlc.studyplan.data.Task as DataTask
import com.mtlc.studyplan.data.DayPlan as DataDayPlan
import com.mtlc.studyplan.data.WeekPlan as DataWeekPlan
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.core.DataStore
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import com.mtlc.studyplan.utils.settingsDataStore
import com.mtlc.studyplan.ui.components.StudyPlanTopBar
import com.mtlc.studyplan.ui.components.StudyPlanTopBarStyle
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit


@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val settingsStore = remember { PlanSettingsStore(appContext.settingsDataStore) }
    val overridesStore = remember { PlanOverridesStore(appContext.settingsDataStore) }
    val planRepo = remember { PlanRepository(overridesStore, settingsStore) }
    val progressRepo = remember { com.mtlc.studyplan.repository.progressRepository }

    val plan by planRepo.planFlow.collectAsState(initial = emptyList())
    val progress by progressRepo.userProgressFlow.collectAsState(initial = UserProgress())
    val todayStats by progressRepo.todayStats.collectAsState(initial = com.mtlc.studyplan.repository.ProgressRepository.DailyStats())

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

    val colorScheme = MaterialTheme.colorScheme
    val isDarkTheme = colorScheme.surface.luminance() < 0.5f
    val pastelPalette = remember {
        listOf(
            Color(0xFFFFF4F8), // blush pink
            Color(0xFFE8F4FF), // powder blue
            Color(0xFFEFFBF2), // mint
            Color(0xFFFFF6E8), // soft peach
            Color(0xFFEDE8FF)  // lavender
        )
    }
    val cardColors = remember(colorScheme, isDarkTheme) {
        pastelPalette.map { pastel ->
            if (isDarkTheme) {
                lerp(pastel, colorScheme.surface, 0.55f)
            } else {
                lerp(pastel, Color.White, 0.15f)
            }
        }
    }

    Scaffold(
        topBar = {
            StudyPlanTopBar(
                title = "Home",
                showLanguageSwitcher = true,
                style = StudyPlanTopBarStyle.Home
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Progress ring for today's plan - using real data
            item {
                val tasks = todayTasks?.second.orEmpty()
                val plannedTasks = tasks.size
                val completedTasks = todayStats.tasksCompleted
                val studyMinutes = todayStats.studyMinutes
                val pointsEarned = todayStats.pointsEarned
                val currentStreak = todayStats.streak
                val ratio = if (plannedTasks > 0) (completedTasks.toFloat() / plannedTasks).coerceIn(0f, 1f) else 0f

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardColors[0 % cardColors.size])
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Today's Progress",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$completedTasks / $plannedTasks tasks completed",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (studyMinutes > 0) {
                            Text(
                                text = "$studyMinutes minutes studied",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (pointsEarned > 0) {
                            Text(
                                text = "$pointsEarned XP earned today",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (currentStreak > 0) {
                            Text(
                                text = "${currentStreak}-day streak",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        LinearProgressIndicator(
                            progress = { ratio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .padding(top = 8.dp),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    }
                }
            }
            item {
                // Real exam countdown using YdsExamService
                val nextExam = YdsExamService.getNextExam()
                if (nextExam != null) {
                    val daysToExam = ChronoUnit.DAYS.between(today, nextExam.examDate)
                    val registrationStatus = YdsExamService.getRegistrationStatus()
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = cardColors[1 % cardColors.size])
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Next Exam: ${nextExam.name}", style = MaterialTheme.typography.titleSmall)
                            Text("Date: ${nextExam.examDate}", style = MaterialTheme.typography.bodySmall)
                            Text("Status: ${YdsExamService.getStatusMessage()}", style = MaterialTheme.typography.bodySmall)
                            LinearProgressIndicator(
                                progress = { if (daysToExam > 0) (100 - daysToExam.coerceAtMost(100)) / 100f else 1f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .padding(top = 8.dp),
                                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                            Text("Days remaining: ${daysToExam}", style = MaterialTheme.typography.bodySmall)

                            // Show registration info if relevant
                            when (registrationStatus) {
                                YdsExamService.RegistrationStatus.OPEN -> {
                                    Text("Registration period: ${nextExam.registrationStart} - ${nextExam.registrationEnd}",
                                         style = MaterialTheme.typography.bodySmall,
                                         color = MaterialTheme.colorScheme.primary)
                                }
                                YdsExamService.RegistrationStatus.LATE_REGISTRATION -> {
                                    Text("Late registration until: ${nextExam.lateRegistrationEnd}",
                                         style = MaterialTheme.typography.bodySmall,
                                         color = MaterialTheme.colorScheme.error)
                                }
                                else -> {}
                            }
                        }
                    }
                }
            }
            item {
                // Study statistics - using real progress data
                if (totalTasks > 0 || progress.totalXp > 0) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = cardColors[2 % cardColors.size])
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Study Progress", style = MaterialTheme.typography.titleSmall)
                            LinearProgressIndicator(
                                progress = { progressRatio },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                            Text("Overall: ${completedInPlan} / ${totalTasks} tasks completed")
                            if (progress.totalXp > 0) {
                                Text("Total XP: ${progress.totalXp}")
                            }
                            // Only show streak if it's greater than 0
                            if (progress.streakCount > 0) {
                                Text("Current streak: ${progress.streakCount} days")
                            }
                        }
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
                            Checkbox(
                                checked = isDone,
                                onCheckedChange = null // Disabled until progress tracking is connected
                            )
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
