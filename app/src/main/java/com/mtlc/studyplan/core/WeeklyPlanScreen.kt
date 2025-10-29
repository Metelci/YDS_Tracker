package com.mtlc.studyplan.core

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtlc.studyplan.data.PlanOverridesStore
import com.mtlc.studyplan.data.PlanRepository
import com.mtlc.studyplan.data.PlanSettingsStore
import com.mtlc.studyplan.data.PlanTask
import com.mtlc.studyplan.data.PlanTaskLocalizer
import com.mtlc.studyplan.data.TaskRepository
import com.mtlc.studyplan.data.WeekPlan
import com.mtlc.studyplan.data.StudyProgressRepository
import com.mtlc.studyplan.shared.SharedAppViewModel
import com.mtlc.studyplan.ui.theme.FeatureKey
import com.mtlc.studyplan.ui.theme.inferredFeaturePastelContainer
import com.mtlc.studyplan.utils.settingsDataStore
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.Flow
import org.koin.androidx.compose.get

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyPlanScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToDaily: (Int, Int) -> Unit = { _, _ -> },
    studyProgressRepository: StudyProgressRepository? = null,
    taskRepository: TaskRepository? = null,
    sharedViewModel: SharedAppViewModel? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    val resolvedStudyProgressRepository = studyProgressRepository ?: get()

    val resolvedTaskRepository = taskRepository ?: get()

    val settingsStore = remember { PlanSettingsStore(context.settingsDataStore) }
    val overridesStore = remember { PlanOverridesStore(context.settingsDataStore) }
    val planRepository = remember(context) { PlanRepository(context, overridesStore, settingsStore) }
    val planFlow: Flow<List<WeekPlan>> = sharedViewModel?.planFlow ?: planRepository.planFlow

    val weeks by planFlow.collectAsState(initial = emptyList())
    val currentWeekNumber by resolvedStudyProgressRepository.currentWeek.collectAsState(initial = 1)
    val allTasks by resolvedTaskRepository.getAllTasks().collectAsState(initial = emptyList())
    val completedTaskIds = remember(allTasks) {
        allTasks.filter { it.isCompleted }.map { it.id }.toSet()
    }

    val weekSummaries = remember(weeks, completedTaskIds, context) {
        weeks.mapIndexed { index, week ->
            buildWeekSummary(week, index, completedTaskIds, context)
        }
    }

    val backgroundBrush = remember {
        Brush.verticalGradient(colors = listOf(Color(0xFFEFF6FF), Color(0xFFF7FBFF)))
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                WeeklyPlanGradientTopBar(
                    onNavigateBack = onNavigateBack,
                    currentWeek = currentWeekNumber,
                    totalWeeks = weeks.size.coerceAtLeast(1)
                )
            },
            contentWindowInsets = WindowInsets.navigationBars
        ) { padding ->
            val navigationBarPadding = with(density) {
                WindowInsets.navigationBars.getBottom(this).toDp()
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = navigationBarPadding + 32.dp
                ),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                if (weekSummaries.isEmpty()) {
                    item { EmptyPlanCard() }
                } else {
                    itemsIndexed(weekSummaries) { _, summary ->
                        WeekPlanCard(
                            summary = summary,
                            isCurrentWeek = summary.week.week == currentWeekNumber,
                            onDayClick = { dayIndex ->
                                onNavigateToDaily(summary.weekIndex, dayIndex)
                            }
                        )
                    }
                }
            }
        }
    }
}

private data class WeekSummary(
    val week: WeekPlan,
    val weekIndex: Int,
    val totalTasks: Int,
    val completedTasks: Int,
    val progress: Float,
    val focus: String?,
    val daySummaries: List<DaySummary>
)

private data class DaySummary(
    val index: Int,
    val name: String,
    val totalTasks: Int,
    val completedTasks: Int,
    val primaryTask: PlanTask?,
    val secondaryTask: PlanTask?,
    val tasks: List<PlanTask>
) {
    val progress: Float = if (totalTasks > 0) completedTasks.toFloat() / totalTasks else 0f
    val statusLabel: String = "$completedTasks/$totalTasks"
}

private fun buildWeekSummary(
    week: WeekPlan,
    weekIndex: Int,
    completedTaskIds: Set<String>,
    context: Context
): WeekSummary {
    val daySummaries = week.days.mapIndexed { dayIndex, day ->
        val localizedName = PlanTaskLocalizer.localizeDayName(day.day, context)
        val total = day.tasks.size
        val completed = day.tasks.count { completedTaskIds.contains(it.id) }
        val primaryTask = day.tasks.firstOrNull()
        val secondaryTask = day.tasks.drop(1).firstOrNull()
        DaySummary(
            index = dayIndex,
            name = localizedName,
            totalTasks = total,
            completedTasks = completed,
            primaryTask = primaryTask,
            secondaryTask = secondaryTask,
            tasks = day.tasks
        )
    }

    val totalTasks = daySummaries.sumOf { it.totalTasks }
    val completedTasks = daySummaries.sumOf { it.completedTasks }
    val progress = if (totalTasks > 0) completedTasks.toFloat() / totalTasks else 0f
    val focus = daySummaries
        .mapNotNull { summary ->
            summary.primaryTask?.desc ?: summary.secondaryTask?.desc
        }
        .firstOrNull()

    return WeekSummary(
        week = week,
        weekIndex = weekIndex,
        totalTasks = totalTasks,
        completedTasks = completedTasks,
        progress = progress,
        focus = focus,
        daySummaries = daySummaries
    )
}

@Composable
private fun WeekPlanCard(
    summary: WeekSummary,
    isCurrentWeek: Boolean,
    onDayClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = inferredFeaturePastelContainer(
                FeatureKey.TASKS.name,
                summary.week.title
            )
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Week ${summary.week.week}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = summary.week.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    summary.focus?.takeIf { it.isNotBlank() }?.let { focus ->
                        Text(
                            text = focus,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Surface(
                    color = when {
                        summary.progress >= 1f -> Color(0xFF4CAF50)
                        summary.progress > 0f -> Color(0xFFFF9800)
                        else -> Color(0xFF9E9E9E)
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "${(summary.progress * 100).roundToInt()}%",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            if (isCurrentWeek) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Current week in progress",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LinearProgressIndicator(
                progress = { summary.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = when {
                    summary.progress >= 1f -> Color(0xFF4CAF50)
                    summary.progress > 0f -> Color(0xFFFF9800)
                    else -> Color(0xFF9E9E9E)
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "${summary.completedTasks} of ${summary.totalTasks} tasks completed",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (summary.daySummaries.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    summary.daySummaries.take(3).forEach { day ->
                        DayPreviewRow(day)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(summary.daySummaries) { day ->
                        WeekDayCard(
                            day = day,
                            onClick = { onDayClick(day.index) }
                        )
                    }
                }
            } else {
                Text(
                    text = "No tasks scheduled for this week yet.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun DayPreviewRow(day: DaySummary) {
    Column {
        Text(
            text = day.name,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        val primary = day.primaryTask?.desc ?: "No tasks scheduled"
        Text(
            text = primary,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        day.primaryTask?.details?.takeIf { it.isNotBlank() }?.let { detail ->
            Text(
                text = detail,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun WeekDayCard(
    day: DaySummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = when {
        day.progress >= 1f -> Color(0xFF4CAF50)
        day.progress > 0f -> Color(0xFFFFF176)
        else -> MaterialTheme.colorScheme.surface
    }
    val contentColor = if (day.progress >= 1f) Color.White else MaterialTheme.colorScheme.onSurface

    Card(
        modifier = modifier
            .width(140.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = day.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = contentColor
            )
            Text(
                text = "${day.completedTasks}/${day.totalTasks} tasks",
                fontSize = 12.sp,
                color = contentColor.copy(alpha = 0.8f)
            )
            day.primaryTask?.let { task ->
                Text(
                    text = task.desc,
                    fontSize = 11.sp,
                    color = contentColor.copy(alpha = 0.9f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun EmptyPlanCard() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "No weekly plan available",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Complete the onboarding planner to generate your personalized Raymond Murphy study schedule.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WeeklyPlanGradientTopBar(
    onNavigateBack: () -> Unit,
    currentWeek: Int,
    totalWeeks: Int
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFBE9E7),
                            Color(0xFFE3F2FD)
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
                            contentDescription = "Back",
                            tint = Color(0xFF424242)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(com.mtlc.studyplan.R.string.study_overview_title),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF424242)
                        )
                        Text(
                            text = stringResource(com.mtlc.studyplan.R.string.study_overview_subtitle),
                            fontSize = 14.sp,
                            color = Color(0xFF616161)
                        )
                    }
                }
            }
        }
    }
}





