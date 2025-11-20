package com.mtlc.studyplan.studyplan

// removed luminance-based dark theme checks
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtlc.studyplan.R
import com.mtlc.studyplan.data.PlanDurationSettings
import com.mtlc.studyplan.data.PlanOverridesStore
import com.mtlc.studyplan.data.PlanRepository
import com.mtlc.studyplan.data.PlanSettingsStore
import com.mtlc.studyplan.data.PlanTask
import com.mtlc.studyplan.data.StudyProgressRepository
import com.mtlc.studyplan.data.TaskStats
import com.mtlc.studyplan.data.UserProgress
import com.mtlc.studyplan.integration.AppIntegrationManager
import com.mtlc.studyplan.ui.theme.appBackgroundBrush
import com.mtlc.studyplan.utils.settingsDataStore
import org.koin.compose.koinInject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyPlanOverviewScreen(
    appIntegrationManager: AppIntegrationManager,
    studyProgressRepository: StudyProgressRepository,
    onNavigateBack: () -> Unit = {},
    initialTab: StudyPlanTab = StudyPlanTab.WEEKLY
) {
    var selectedTab by remember { mutableStateOf(initialTab) }
    var selectedDay by remember { mutableStateOf<DailyStudyInfo?>(null) }

    val context = LocalContext.current
    val appContext = context.applicationContext
    val settingsStore = remember { PlanSettingsStore(appContext.settingsDataStore) }
    remember { PlanOverridesStore(appContext.settingsDataStore) }
    val planRepository: PlanRepository = koinInject()
    val progressRepo = remember { com.mtlc.studyplan.repository.progressRepository }

    val plan by planRepository.planFlow.collectAsState(initial = emptyList())
    val settings by settingsStore.settingsFlow.collectAsState(initial = PlanDurationSettings())
    val userProgress by progressRepo.userProgressFlow.collectAsState(initial = UserProgress())
    val completedTaskIds = remember(userProgress) { userProgress.completedTasks }

    // Collect data from AppIntegrationManager
    val taskStats by appIntegrationManager.getTaskStatsFlow().collectAsState(
        initial = TaskStats(0, 0, 0, 0)
    )

    // Get current week from study progress repository
    val currentWeek by studyProgressRepository.currentWeek.collectAsState(initial = 1)

    val startDate = remember(settings.startEpochDay) { LocalDate.ofEpochDay(settings.startEpochDay) }
    val dateFormatter = remember(settings.dateFormatPattern) {
        val pattern = settings.dateFormatPattern?.takeIf { it.isNotBlank() }
        val locale = Locale.getDefault()
        pattern?.let { DateTimeFormatter.ofPattern(it, locale) }
            ?: DateTimeFormatter.ofPattern("MMM dd", locale)
    }

    val currentWeekPlan = remember(plan, currentWeek) {
        plan.firstOrNull { it.week == currentWeek } ?: plan.firstOrNull()
    }

    val daysBeforeCurrentWeek = remember(plan, currentWeekPlan) {
        if (currentWeekPlan == null) 0 else {
            val index = plan.indexOfFirst { it.week == currentWeekPlan.week }
            if (index <= 0) 0 else plan.take(index).sumOf { it.days.size }
        }
    }

    val studySchedule = remember(
        plan,
        currentWeekPlan,
        completedTaskIds,
        dateFormatter,
        startDate,
        daysBeforeCurrentWeek
    ) {
        if (currentWeekPlan == null) {
            StudyScheduleData(emptyList(), emptyList())
        } else {
            val dailySchedules = currentWeekPlan.days.mapIndexed { dayIndex, dayPlan ->
                val globalIndex = daysBeforeCurrentWeek + dayIndex
                val date = startDate.plusDays(globalIndex.toLong())
                val planTasks = dayPlan.tasks
                val estimatedMinutes = planTasks.sumOf { estimateTaskDurationMinutes(it) }
                val completedCount = planTasks.count { completedTaskIds.contains(it.id) }
                DailySchedule(
                    weekNumber = currentWeekPlan.week,
                    weekTitle = currentWeekPlan.title,
                    dayIndex = dayIndex,
                    dayName = normalizeDayKey(dayPlan.day),
                    date = dateFormatter.format(date),
                    tasks = planTasks.map { it.desc },
                    estimatedTime = formatEstimatedTime(estimatedMinutes),
                    completionPercentage = if (planTasks.isNotEmpty()) (completedCount * 100 / planTasks.size) else 0,
                    planTasks = planTasks,
                    dateValue = date,
                    isToday = date == LocalDate.now()
                )
            }

            StudyScheduleData(
                dailySchedules = dailySchedules,
                weeklyGoals = emptyList()
            )
        }
    }

    LaunchedEffect(studySchedule.dailySchedules) {
        val currentSelected = selectedDay
        if (currentSelected != null) {
            val match = studySchedule.dailySchedules.firstOrNull { schedule ->
                schedule.dayName == normalizeDayKey(currentSelected.dayName) &&
                    schedule.date == currentSelected.date
            }
            if (match != null) {
                selectedDay = createDailyStudyInfo(match, completedTaskIds)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appBackgroundBrush())
    ) {
        Scaffold(
            topBar = {
                // Settings-style topbar with pastel gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFFBE9E7), // Light pastel red/pink
                                    Color(0xFFE3F2FD)  // Light pastel blue
                                ),
                                start = Offset.Zero,
                                end = Offset.Infinite
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .border(2.dp, Color(0xFF0066FF), RoundedCornerShape(24.dp))
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onNavigateBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.navigate_up),
                                    tint = Color(0xFF424242)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.study_overview_title),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF424242)
                                )
                                Text(
                                    text = stringResource(R.string.study_overview_subtitle),
                                    fontSize = 14.sp,
                                    color = Color(0xFF616161)
                                )
                            }
                        }
                    }
                }
            }
        }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
            // Tab Selector
            StudyPlanTabRow(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Content based on selected tab
            when (selectedTab) {
                StudyPlanTab.WEEKLY -> WeeklyScheduleView(
                    studySchedule = studySchedule,
                    taskStats = taskStats,
                    completedTaskIds = completedTaskIds,
                    onDayClick = { dayInfo ->
                        selectedDay = dayInfo
                        selectedTab = StudyPlanTab.DAILY
                    },
                    modifier = Modifier.weight(1f)
                )
                StudyPlanTab.DAILY -> DailyScheduleView(
                    selectedDay = selectedDay,
                    onBackToWeekly = { selectedTab = StudyPlanTab.WEEKLY },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
    }
}

@Composable
private fun StudyPlanTabRow(
    selectedTab: StudyPlanTab,
    onTabSelected: (StudyPlanTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StudyPlanTab.entries.forEach { tab ->
            StudyPlanTabChip(
                tab = tab,
                isSelected = selectedTab == tab,
                onClick = { onTabSelected(tab) }
            )
        }
    }
}

@Composable
private fun StudyPlanTabChip(
    tab: StudyPlanTab,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(200),
        label = "tab_background"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label = "tab_content"
    )

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        color = backgroundColor,
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val tabLabel = when (tab) {
                StudyPlanTab.WEEKLY -> stringResource(R.string.tasks_tab_weekly)
                StudyPlanTab.DAILY -> stringResource(R.string.tasks_tab_daily)
            }
            Icon(
                imageVector = tab.icon,
                contentDescription = tabLabel,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = tabLabel,
                color = contentColor,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun WeeklyScheduleView(
    studySchedule: StudyScheduleData,
    taskStats: TaskStats,
    completedTaskIds: Set<String>,
    onDayClick: (DailyStudyInfo) -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Current Week Overview
        item {
            CurrentWeekCard(
                taskStats = taskStats
            )
        }

        // Daily Schedule Cards
        item {
            Text(
                text = stringResource(R.string.tasks_week_schedule_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        items(studySchedule.dailySchedules) { dailySchedule ->
            DailyScheduleCard(
                dailySchedule = dailySchedule,
                onClick = {
                    onDayClick(createDailyStudyInfo(dailySchedule, completedTaskIds))
                }
            )
        }

        // Study Goals
        if (studySchedule.weeklyGoals.isNotEmpty()) {
            item {
                StudyGoalsCard(studySchedule.weeklyGoals)
            }
        }
    }
}


@Composable
private fun CurrentWeekCard(
    taskStats: TaskStats
) {
    MaterialTheme.colorScheme.primaryContainer
    val primary = MaterialTheme.colorScheme.primary
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer

    val gradientBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFFD4D8F0),  // Soft indigo-blue
            Color(0xFFF5F2FF)   // Very light indigo white
        )
    )

    val progressPercentage = if (taskStats.totalTasks > 0) taskStats.getProgressPercentage() else 0
    val remainingTasks = maxOf(0, taskStats.totalTasks - taskStats.completedTasks)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradientBrush)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header with title and circular progress
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.week_progress),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = onPrimaryContainer
                        )
                        if (taskStats.totalTasks > 0) {
                            Text(
                                text = "${taskStats.completedTasks} / ${taskStats.totalTasks} tasks",
                                style = MaterialTheme.typography.bodyMedium,
                                color = onPrimaryContainer.copy(alpha = 0.8f),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    // Larger, more prominent circular progress
                    Box(
                        modifier = Modifier.size(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { progressPercentage / 100f },
                            modifier = Modifier.size(48.dp),
                            color = primary,
                            strokeWidth = 5.dp,
                            trackColor = primary.copy(alpha = 0.25f)
                        )
                    }
                }

                // Show detailed metrics when user has task data
                if (taskStats.totalTasks > 0) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Stats row with better visual separation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatCard(
                            label = stringResource(R.string.tasks_week_stat_completed),
                            value = "${taskStats.completedTasks}",
                            icon = Icons.Filled.CheckCircle,
                            color = primary
                        )

                        StatCard(
                            label = stringResource(R.string.tasks_week_stat_remaining),
                            value = "$remainingTasks",
                            icon = Icons.Filled.Schedule,
                            color = onPrimaryContainer.copy(alpha = 0.7f)
                        )

                        StatCard(
                            label = "Total",
                            value = "${taskStats.totalTasks}",
                            icon = Icons.AutoMirrored.Filled.Assignment,
                            color = onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ProgressMetric(
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun DailyScheduleCard(
    dailySchedule: DailySchedule,
    onClick: () -> Unit
) {
    val capsuleShape = RoundedCornerShape(32.dp)
    val highlightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    val neutralColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    val backgroundColor = if (dailySchedule.isToday) highlightColor else neutralColor
    val border = if (dailySchedule.isToday) {
        BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = capsuleShape,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        color = backgroundColor,
        border = border
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (dailySchedule.isToday) {
                    Surface(
                        color = Color.Transparent,
                        shape = RoundedCornerShape(50),
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                    ) {
                        Text(
                            text = stringResource(R.string.today_label),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = localizedDayLabel(dailySchedule.dayName),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (dailySchedule.tasks.isEmpty()) {
                            stringResource(R.string.study_ready_to_plan)
                        } else {
                            stringResource(
                                R.string.study_tasks_summary,
                                dailySchedule.tasks.size,
                                dailySchedule.estimatedTime
                            )
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (dailySchedule.completionPercentage > 0) {
                LinearProgressIndicator(
                    progress = { dailySchedule.completionPercentage / 100f },
                    modifier = Modifier
                        .width(72.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(50)),
                    color = if (dailySchedule.completionPercentage >= 100) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                )
            }
        }
    }
}

// Data classes and helper functions
enum class StudyPlanTab(val title: String, val icon: ImageVector) {
    WEEKLY("Weekly", Icons.Filled.CalendarToday),
    DAILY("Daily", Icons.Filled.DateRange)
}

data class StudyScheduleData(
    val dailySchedules: List<DailySchedule>,
    val weeklyGoals: List<StudyGoal>
)

data class DailySchedule(
    val weekNumber: Int,
    val weekTitle: String,
    val dayIndex: Int,
    val dayName: String,
    val date: String,
    val tasks: List<String>,
    val estimatedTime: String,
    val completionPercentage: Int,
    val planTasks: List<PlanTask>,
    val dateValue: LocalDate?,
    val isToday: Boolean = false
)

data class StudyGoal(
    val title: String,
    val description: String,
    val progress: Int,
    val isCompleted: Boolean
)

enum class Priority { HIGH, MEDIUM, LOW }

// Helper function to create DailyStudyInfo from DailySchedule
private fun createDailyStudyInfo(
    dailySchedule: DailySchedule,
    completedTaskIds: Set<String>
): DailyStudyInfo {
    val planTasks = dailySchedule.planTasks
    val totalMinutes = planTasks.sumOf { estimateTaskDurationMinutes(it) }

    val tasks = planTasks.map { task ->
        DailyTask(
            title = task.desc,
            description = task.details.orEmpty(),
            estimatedDuration = formatEstimatedTime(estimateTaskDurationMinutes(task)),
            priority = determineTaskPriority(task),
            isCompleted = completedTaskIds.contains(task.id)
        )
    }

    val materials = planTasks
        .flatMap { buildStudyMaterials(it) }
        .distinctBy { it.title to it.type }

    val units = extractStudyUnits(planTasks, completedTaskIds)

    return DailyStudyInfo(
        weekTitle = dailySchedule.weekTitle,
        dayName = dailySchedule.dayName,
        date = dailySchedule.date,
        book = studyBookForWeek(dailySchedule.weekNumber),
        units = units,
        tasks = tasks,
        materials = materials,
        estimatedTime = dailySchedule.estimatedTime.ifBlank { formatEstimatedTime(totalMinutes) },
        completionPercentage = dailySchedule.completionPercentage,
        notes = ""
    )
}

private fun normalizeDayKey(raw: String): String {
    val normalized = normalizeText(raw.trim())
    return when {
        normalized.startsWith("pazartesi") || normalized.startsWith("pzt") || normalized.startsWith("mon") -> "monday"
        normalized.startsWith("sali") || normalized.startsWith("sal") || normalized.startsWith("tue") -> "tuesday"
        normalized.startsWith("carsamba") || normalized.startsWith("car") || normalized.startsWith("wed") -> "wednesday"
        normalized.startsWith("persembe") || normalized.startsWith("per") || normalized.startsWith("thu") -> "thursday"
        normalized.startsWith("cuma") || normalized.startsWith("fri") -> "friday"
        normalized.startsWith("cumartesi") || normalized.startsWith("cmt") || normalized.startsWith("sat") -> "saturday"
        normalized.startsWith("pazar") || normalized.startsWith("sun") -> "sunday"
        else -> normalized.ifEmpty { raw.lowercase(Locale.getDefault()) }
    }
}

private fun normalizeText(value: String): String {
    if (value.isEmpty()) return value
    val lower = value.lowercase(Locale.getDefault())
    val builder = StringBuilder(lower.length)
    for (ch in lower) {
        builder.append(
            when (ch) {
                '\u00E7', '\u00C7' -> 'c'
                '\u011F', '\u011E' -> 'g'
                '\u015F', '\u015E' -> 's'
                '\u0131', '\u0130' -> 'i'
                '\u00F6', '\u00D6' -> 'o'
                '\u00FC', '\u00DC' -> 'u'
                else -> ch
            }
        )
    }
    return builder.toString()
}

private fun formatEstimatedTime(minutes: Int): String {
    if (minutes <= 0) return "-"
    val hours = minutes / 60
    val remainder = minutes % 60
    return when {
        hours > 0 && remainder > 0 -> "${hours}h ${remainder}m"
        hours > 0 -> "${hours}h"
        else -> "${remainder}m"
    }
}

private fun determineTaskPriority(task: PlanTask): Priority {
    val text = normalizeText(task.desc + " " + (task.details ?: ""))
    return when {
        listOf("deneme", "exam", "quiz", "test", "soru").any { text.contains(it) } -> Priority.HIGH
        listOf("okuma", "reading", "dinleme", "listening", "analiz", "analysis").any { text.contains(it) } -> Priority.MEDIUM
        else -> Priority.LOW
    }
}

private fun estimateTaskDurationMinutes(task: PlanTask): Int {
    val text = normalizeText(task.desc + " " + (task.details ?: ""))
    val explicit = Regex("(\\d{2,3})\\s*(-\\s*(\\d{2,3}))?\\s*(dk|dakika|minute|min)", RegexOption.IGNORE_CASE)
    explicit.find(text)?.let { match ->
        val start = match.groupValues[1].toIntOrNull()
        val end = match.groupValues.getOrNull(3)?.toIntOrNull()
        if (start != null) {
            return if (end != null) (start + end) / 2 else start
        }
    }
    return when {
        text.contains("tam deneme") || text.contains("full exam") -> 180
        text.contains("mini deneme") || text.contains("mini exam") -> 70
        text.contains("okuma") || text.contains("reading") -> 45
        text.contains("kelime") || text.contains("vocab") -> 30
        text.contains("analiz") || text.contains("analysis") -> 30
        text.contains("dinleme") || text.contains("listening") -> 30
        text.contains("gramer") || text.contains("grammar") -> 40
        text.contains("pratik") || text.contains("practice") || text.contains("drill") -> 25
        else -> 30
    }
}

private fun buildStudyMaterials(task: PlanTask): List<StudyMaterial> {
    val details = task.details.orEmpty()
    val normalized = normalizeText(task.desc + " " + details)
    val url = Regex("(https?://\\S+)").find(details)?.value ?: ""
    val type = when {
        listOf("video", "dizi", "watch", "youtube").any { normalized.contains(it) } -> MaterialType.VIDEO
        listOf("podcast", "dinle", "listening", "audio").any { normalized.contains(it) } -> MaterialType.AUDIO
        listOf("pdf", "kitap", "book", "dokuman", "document").any { normalized.contains(it) } -> MaterialType.PDF
        listOf("quiz", "deneme", "test", "soru").any { normalized.contains(it) } -> MaterialType.QUIZ
        listOf("alistirma", "pratik", "exercise", "practice", "drill").any { normalized.contains(it) } -> MaterialType.EXERCISE
        else -> MaterialType.READING
    }
    return listOf(
        StudyMaterial(
            title = task.desc,
            type = type,
            url = url,
            description = details
        )
    )
}

private fun extractStudyUnits(
    planTasks: List<PlanTask>,
    completedTaskIds: Set<String>
): List<StudyUnit> {
    val unitRegex = Regex("(?:unite|unit|units)[^0-9]*(\\d[\\d,\\s\\-]*)", RegexOption.IGNORE_CASE)
    val numberRegex = Regex("\\d+")
    val pagesRegex = Regex("(?:sayfa|page|pages)[:\\s]*(\\d+(?:-\\d+)?)", RegexOption.IGNORE_CASE)
    val exerciseRegex = Regex("(?:exercise|alistirma|practice)[:\\s]*(\\d+(?:[\\-,]\\s*\\d+)*)", RegexOption.IGNORE_CASE)

    val units = mutableListOf<StudyUnit>()
    planTasks.forEach { task ->
        val details = task.details ?: return@forEach
        val normalized = normalizeText(details)
        val unitMatch = unitRegex.find(normalized)
        val numbers = unitMatch
            ?.groupValues
            ?.getOrNull(1)
            ?.let { numberRegex.findAll(it).mapNotNull { match -> match.value.toIntOrNull() }.toList() }
            .orEmpty()

        if (numbers.isEmpty()) return@forEach

        val pages = pagesRegex.find(normalized)?.groupValues?.getOrNull(1) ?: "-"
        val exercises = exerciseRegex.find(normalized)
            ?.groupValues
            ?.getOrNull(1)
            ?.let { numberRegex.findAll(it).map { match -> match.value }.toList() }
            ?: emptyList()

        numbers.forEach { unitNumber ->
            units += StudyUnit(
                title = task.desc,
                unitNumber = unitNumber,
                pages = pages,
                exercises = exercises,
                isCompleted = completedTaskIds.contains(task.id)
            )
        }
    }

    return units.distinctBy { it.unitNumber to it.title }
}

private fun studyBookForWeek(weekNumber: Int): StudyBook = when (weekNumber) {
    in 1..8 -> StudyBook.RED_BOOK
    in 9..18 -> StudyBook.BLUE_BOOK
    in 19..26 -> StudyBook.GREEN_BOOK
    else -> StudyBook.RED_BOOK
}

@Composable
private fun localizedDayLabel(dayKey: String): String {
    return when (dayKey.lowercase(Locale.getDefault())) {
        "monday" -> stringResource(R.string.monday)
        "tuesday" -> stringResource(R.string.tuesday)
        "wednesday" -> stringResource(R.string.wednesday)
        "thursday" -> stringResource(R.string.thursday)
        "friday" -> stringResource(R.string.friday)
        "saturday" -> stringResource(R.string.saturday)
        "sunday" -> stringResource(R.string.sunday)
        else -> dayKey.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
    }
}


// Additional composables for other sections...

@Composable
private fun StudyGoalsCard(goals: List<StudyGoal>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            goals.forEach { goal ->
                GoalItem(goal = goal)
                if (goal != goals.last()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun GoalItem(goal: StudyGoal) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (goal.isCompleted) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
            contentDescription = stringResource(
                if (goal.isCompleted) R.string.cd_completed else R.string.cd_in_progress
            ),
            tint = if (goal.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = goal.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (goal.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = goal.description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// Daily Study Info data class
data class DailyStudyInfo(
    val weekTitle: String,
    val dayName: String,
    val date: String,
    val book: StudyBook,
    val units: List<StudyUnit>,
    val tasks: List<DailyTask>,
    val materials: List<StudyMaterial>,
    val estimatedTime: String,
    val completionPercentage: Int,
    val notes: String = ""
)

data class StudyBook(
    val name: String,
    val color: Color,
    val description: String
) {
    companion object {
        val RED_BOOK = StudyBook("Red Book - Essential Grammar in Use", Color(0xFFE53935), "Foundation Level Grammar")
        val BLUE_BOOK = StudyBook("Blue Book - English Grammar in Use", Color(0xFF1976D2), "Intermediate Level Grammar")
        val GREEN_BOOK = StudyBook("Green Book - Advanced Grammar in Use", Color(0xFF388E3C), "Advanced Level Grammar")
    }
}

data class StudyUnit(
    val title: String,
    val unitNumber: Int,
    val pages: String,
    val exercises: List<String>,
    val isCompleted: Boolean = false
)

data class DailyTask(
    val title: String,
    val description: String,
    val estimatedDuration: String,
    val priority: Priority,
    val isCompleted: Boolean = false
)

data class StudyMaterial(
    val title: String,
    val type: MaterialType,
    val url: String = "",
    val description: String = ""
)

enum class MaterialType {
    VIDEO, AUDIO, PDF, EXERCISE, QUIZ, READING
}

@Composable
private fun DailyScheduleView(
    selectedDay: DailyStudyInfo?,
    onBackToWeekly: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(selectedDay) {
        if (selectedDay != null) {
            isLoading = true
            // Simulate loading delay
            kotlinx.coroutines.delay(800)
            isLoading = false
        }
    }

    if (selectedDay == null) {
        // No day selected - show placeholder
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.CalendarToday,
                    contentDescription = stringResource(R.string.cd_calendar),
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.tasks_daily_placeholder_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.tasks_daily_placeholder_body),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
                Button(
                    onClick = onBackToWeekly,
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text(stringResource(R.string.tasks_daily_placeholder_button))
                }
            }
        }
        return
    }

    if (isLoading) {
        // Loading state
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator()
                Text(
                    text = stringResource(R.string.tasks_daily_loading_message, selectedDay.dayName),
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    // Daily content
    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Header with day info
        item {
            DailyStudyHeader(
                dayInfo = selectedDay,
                onBackToWeekly = onBackToWeekly
            )
        }

        // Study Book Section
        item {
            StudyBookCard(selectedDay.book, selectedDay.units)
        }

        // Daily Tasks Section
        item {
            DailyTasksSection(selectedDay.tasks)
        }

        // Study Materials Section
        item {
            StudyMaterialsSection(selectedDay.materials)
        }

        // Notes Section
        if (selectedDay.notes.isNotEmpty()) {
            item {
                NotesSection(selectedDay.notes)
            }
        }
    }
}

@Composable
private fun DailyStudyHeader(
    dayInfo: DailyStudyInfo,
    onBackToWeekly: () -> Unit
) {
    // Empty header - button removed per user request
    Spacer(modifier = Modifier.height(0.dp))
}

@Composable
private fun StudyBookCard(book: StudyBook, units: List<StudyUnit>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = book.color,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = book.name.first().toString(),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = book.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = book.description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (units.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.tasks_daily_units_title),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                units.forEach { unit ->
                    StudyUnitItem(unit)
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun StudyUnitItem(unit: StudyUnit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (unit.isCompleted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (unit.isCompleted) Icons.Filled.CheckCircle else Icons.Filled.Circle,
            contentDescription = stringResource(
                if (unit.isCompleted) R.string.cd_completed else R.string.cd_in_progress
            ),
            tint = if (unit.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.tasks_daily_unit_title, unit.unitNumber, unit.title),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.tasks_daily_unit_pages, unit.pages),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (unit.exercises.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.tasks_daily_unit_exercises, unit.exercises.joinToString(", ")),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DailyTasksSection(tasks: List<DailyTask>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.tasks_daily_tasks_title),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (tasks.isEmpty()) {
                Text(
                    text = stringResource(R.string.tasks_daily_no_tasks),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                tasks.forEach { task ->
                    DailyTaskItem(task)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun DailyTaskItem(task: DailyTask) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (task.isCompleted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = if (task.isCompleted) Icons.Filled.CheckCircle else Icons.Filled.Circle,
            contentDescription = stringResource(
                if (task.isCompleted) R.string.cd_completed else R.string.cd_in_progress
            ),
            tint = if (task.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = task.description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Duration: ${task.estimatedDuration}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Surface(
            color = when (task.priority) {
                Priority.HIGH -> MaterialTheme.colorScheme.error
                Priority.MEDIUM -> MaterialTheme.colorScheme.tertiary
                Priority.LOW -> MaterialTheme.colorScheme.primary
            },
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(
                text = task.priority.name,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun StudyMaterialsSection(materials: List<StudyMaterial>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.tasks_daily_materials_title),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (materials.isEmpty()) {
                Text(
                    text = stringResource(R.string.tasks_daily_no_materials),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                materials.forEach { material ->
                    StudyMaterialItem(material)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun StudyMaterialItem(material: StudyMaterial) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { /* Handle material click */ }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val cdLabel = when (material.type) {
            MaterialType.VIDEO -> stringResource(R.string.cd_play)
            MaterialType.AUDIO -> stringResource(R.string.cd_feature)
            MaterialType.PDF -> stringResource(R.string.cd_resource_link)
            MaterialType.EXERCISE -> stringResource(R.string.cd_goal)
            MaterialType.QUIZ -> stringResource(R.string.cd_goal)
            MaterialType.READING -> stringResource(R.string.cd_resource_link)
        }
        Icon(
            imageVector = when (material.type) {
                MaterialType.VIDEO -> Icons.Filled.PlayArrow
                MaterialType.AUDIO -> Icons.AutoMirrored.Filled.VolumeUp
                MaterialType.PDF -> Icons.Filled.PictureAsPdf
                MaterialType.EXERCISE -> Icons.AutoMirrored.Filled.Assignment
                MaterialType.QUIZ -> Icons.Filled.Quiz
                MaterialType.READING -> Icons.AutoMirrored.Filled.MenuBook
            },
            contentDescription = cdLabel,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = material.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (material.description.isNotEmpty()) {
                Text(
                    text = material.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text(
            text = material.type.name,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun NotesSection(notes: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.StickyNote2,
                    contentDescription = stringResource(R.string.cd_info),
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.tasks_daily_notes_title),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = notes,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 20.sp
            )
        }
    }
}









