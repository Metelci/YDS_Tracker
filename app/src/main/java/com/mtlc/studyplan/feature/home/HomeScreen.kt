@file:Suppress("LongMethod", "CyclomaticComplexMethod")
@file:OptIn(ExperimentalMaterial3Api::class)
package com.mtlc.studyplan.feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mtlc.studyplan.R
import com.mtlc.studyplan.data.*
import com.mtlc.studyplan.data.Task as DataTask
import com.mtlc.studyplan.data.DayPlan as DataDayPlan
import com.mtlc.studyplan.data.WeekPlan as DataWeekPlan
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.core.DataStore
import android.content.Context
import com.mtlc.studyplan.utils.settingsDataStore
import com.mtlc.studyplan.ui.components.StudyPlanTopBar
import com.mtlc.studyplan.ui.components.StudyPlanTopBarStyle
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.os.ConfigurationCompat
import com.mtlc.studyplan.data.ExamCountdownManager
import java.util.Locale
import org.koin.compose.koinInject
import android.widget.Toast


@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val settingsStore = remember { PlanSettingsStore(appContext.settingsDataStore) }
    val overridesStore = remember { PlanOverridesStore(appContext.settingsDataStore) }
    val planRepo: PlanRepository = koinInject()
    val progressRepo = remember { com.mtlc.studyplan.repository.progressRepository }
    val examCountdownManager: ExamCountdownManager = koinInject()
    val examData by examCountdownManager.examData.collectAsState()
    val telemetry by YdsExamService.getTelemetry().collectAsState()
    var refreshInFlight by remember { mutableStateOf(false) }

    LaunchedEffect(examCountdownManager) {
        examCountdownManager.refreshNow()
    }

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
    val isDarkTheme = false
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
                title = stringResource(R.string.home_title),
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
                val currentStreak = progress.streakCount
                val totalAchievements = progress.unlockedAchievements.size
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
                            text = stringResource(R.string.home_today_progress_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = pluralStringResource(R.plurals.tasks_completed, completedTasks, completedTasks, plannedTasks),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (studyMinutes > 0) {
                            Text(
                                text = stringResource(R.string.home_today_progress_minutes, studyMinutes),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        // Always show points (even if 0)
                        Text(
                            text = stringResource(R.string.home_today_progress_points, pointsEarned),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (pointsEarned > 0) FontWeight.SemiBold else FontWeight.Normal
                        )

                        // Always show streak (even if 0)
                        Text(
                            text = stringResource(R.string.home_today_progress_streak, currentStreak),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (currentStreak > 0) FontWeight.SemiBold else FontWeight.Normal
                        )

                        // Always show awards/achievements count (even if 0)
                        if (totalAchievements > 0) {
                            Text(
                                text = stringResource(R.string.home_today_progress_achievements, totalAchievements),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
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
                val nextExam = YdsExamService.getNextExam()
                val dataStale = YdsExamService.isDataStale()
                if (nextExam != null) {
                    val rawDaysToExam = ChronoUnit.DAYS.between(today, nextExam.examDate).toInt()
                    val registrationStatus = YdsExamService.getRegistrationStatus()

                    // Get current locale for date formatting
                    val configuration = LocalConfiguration.current
                    val locale = ConfigurationCompat.getLocales(configuration)[0] ?: Locale.getDefault()
                    val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)

                    // Format dates according to locale
                    val formattedExamDate = nextExam.examDate.format(dateFormatter)
                    val formattedRegStart = nextExam.registrationStart.format(dateFormatter)
                    val formattedRegEnd = nextExam.registrationEnd.format(dateFormatter)
                    val formattedLateRegEnd = nextExam.lateRegistrationEnd.format(dateFormatter)

                    // Get localized status message
                    val statusMessage = when {
                        rawDaysToExam == 0 -> stringResource(R.string.exam_status_exam_day)
                        rawDaysToExam < 0 -> stringResource(R.string.exam_status_completed)
                        rawDaysToExam <= 7 -> stringResource(R.string.exam_status_final_week)
                        rawDaysToExam <= 30 -> stringResource(R.string.exam_status_almost_there)
                        rawDaysToExam <= 90 -> when (registrationStatus) {
                            YdsExamService.RegistrationStatus.NOT_OPEN_YET -> stringResource(R.string.exam_status_registration_opens_soon)
                            YdsExamService.RegistrationStatus.OPEN -> stringResource(R.string.exam_status_registration_open)
                            YdsExamService.RegistrationStatus.LATE_REGISTRATION -> stringResource(R.string.exam_status_late_registration)
                            YdsExamService.RegistrationStatus.CLOSED -> stringResource(R.string.exam_status_registration_closed)
                            YdsExamService.RegistrationStatus.NO_UPCOMING_EXAM -> stringResource(R.string.exam_status_preparation_time)
                        }
                        else -> stringResource(R.string.exam_status_long_term_planning)
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = cardColors[1 % cardColors.size])
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                stringResource(R.string.home_exam_card_title, nextExam.name),
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                stringResource(
                                    R.string.home_exam_card_last_updated,
                                    telemetry.lastSource.name.lowercase(Locale.getDefault()),
                                    java.text.DateFormat.getDateTimeInstance().format(java.util.Date(telemetry.lastUpdatedAt))
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            if (dataStale || telemetry.fallbackUsed) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)
                                ) {
                                    Text(
                                        stringResource(R.string.home_exam_card_stale),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    TextButton(
                                        onClick = {
                                            if (!refreshInFlight) {
                                                coroutineScope.launch {
                                                    refreshInFlight = true
                                                    val refreshed = YdsExamService.refreshExams()
                                                    examCountdownManager.forceRefresh()
                                                    refreshInFlight = false
                                                    val msg = if (refreshed) {
                                                        R.string.home_exam_card_refresh_success
                                                    } else {
                                                        R.string.home_exam_card_refresh_failure
                                                    }
                                                    Toast
                                                        .makeText(context, msg, Toast.LENGTH_SHORT)
                                                        .show()
                                                }
                                            }
                                        },
                                        enabled = !refreshInFlight
                                    ) {
                                        Text(stringResource(R.string.home_exam_card_refresh))
                                    }
                                }
                            }
                            Text(
                                stringResource(R.string.home_exam_card_date, formattedExamDate),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                stringResource(R.string.home_exam_card_status, statusMessage),
                                style = MaterialTheme.typography.bodySmall
                            )
                            LinearProgressIndicator(
                                progress = {
                                    if (rawDaysToExam > 0) (100 - rawDaysToExam.coerceAtMost(100)) / 100f else 1f
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .padding(top = 8.dp),
                                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                            val safeDaysToExam = rawDaysToExam.coerceAtLeast(0)
                            Text(stringResource(R.string.home_exam_card_days_remaining, safeDaysToExam),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(top = 4.dp)
                            )

                            // Show registration info if relevant
                            when (registrationStatus) {
                                YdsExamService.RegistrationStatus.OPEN -> {
                                    Text(
                                        stringResource(
                                            R.string.home_exam_card_registration_period,
                                            formattedRegStart,
                                            formattedRegEnd
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                YdsExamService.RegistrationStatus.LATE_REGISTRATION -> {
                                    Text(
                                        stringResource(
                                            R.string.home_exam_card_late_registration,
                                            formattedLateRegEnd
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
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
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                stringResource(R.string.home_study_progress_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            LinearProgressIndicator(
                                progress = { progressRatio },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    stringResource(R.string.home_study_progress_overall, completedInPlan, totalTasks),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (progress.totalXp > 0) {
                                        Text(
                                            stringResource(R.string.home_study_progress_total_xp, progress.totalXp),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    // Only show streak if it's greater than 0
                                    if (progress.streakCount > 0) {
                                        Text(
                                            stringResource(R.string.home_study_progress_current_streak, progress.streakCount),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item {
                Text(stringResource(R.string.home_today_tasks_title), style = MaterialTheme.typography.titleMedium)
            }
            if (todayTasks != null) {
                val (weekNum, tasks) = todayTasks
                items(tasks, key = { it.id }) { t ->
                    val isDone = progress.completedTasks.contains(t.id)
                    ListItem(
                        headlineContent = { Text(t.desc) },
                        supportingContent = { t.details?.let { Text(it) } },
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
                item { Text(stringResource(R.string.home_today_tasks_empty)) }
            }
        }
    }
}
