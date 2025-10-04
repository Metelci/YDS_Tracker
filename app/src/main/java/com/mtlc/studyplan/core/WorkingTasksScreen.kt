package com.mtlc.studyplan.core

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Task
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import com.mtlc.studyplan.ui.theme.FeatureKey
import com.mtlc.studyplan.ui.theme.featurePastelContainer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtlc.studyplan.data.CompleteMurphyBookData
import com.mtlc.studyplan.data.DayPlan
import com.mtlc.studyplan.data.MurphyBook
import com.mtlc.studyplan.data.MurphyUnit
import com.mtlc.studyplan.data.PlanDataSource
import com.mtlc.studyplan.data.PlanTaskLocalizer
import com.mtlc.studyplan.data.Task
import com.mtlc.studyplan.data.TaskPriority
import com.mtlc.studyplan.data.TaskRepository
import com.mtlc.studyplan.data.WeekPlan
import com.mtlc.studyplan.integration.AppIntegrationManager
import com.mtlc.studyplan.shared.SharedAppViewModel
import com.mtlc.studyplan.ui.responsive.responsiveHeights
import com.mtlc.studyplan.ui.responsive.touchTargetSize
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkingTasksScreen(
    appIntegrationManager: AppIntegrationManager,
    studyProgressRepository: com.mtlc.studyplan.data.StudyProgressRepository,
    taskRepository: TaskRepository,
    sharedViewModel: SharedAppViewModel,
    onNavigateToStudyPlan: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Get current week from study progress repository
    val currentWeek by studyProgressRepository.currentWeek.collectAsState(initial = 1)

    val plan = remember { PlanDataSource.planData }

    var selectedTab by remember { mutableStateOf(2) } // 0: Daily, 1: Weekly, 2: Plan
    var selectedDay by remember { mutableStateOf<DayPlan?>(null) }

    // Get the week that corresponds to user's current progress
    val thisWeek = remember(currentWeek) {
        plan.getOrNull(currentWeek - 1) ?: plan.firstOrNull()
    }
    val weeklyIds = remember(thisWeek) { thisWeek?.days?.flatMap { it.tasks }?.map { it.id }?.toSet() ?: emptySet() }
    val weeklyCompleted = 0
    val weeklyTotal = remember(weeklyIds) { weeklyIds.size.coerceAtLeast(1) }
    val weeklyProgressPct = remember(weeklyCompleted, weeklyTotal) { (weeklyCompleted.toFloat() / weeklyTotal) }

    val isDarkTheme = isSystemInDarkTheme()
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = if (isDarkTheme) {
                    // Seamless anthracite to light grey gradient for dark theme
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF2C2C2C), // Deep anthracite (top)
                            Color(0xFF3A3A3A), // Medium anthracite
                            Color(0xFF4A4A4A)  // Light anthracite (bottom)
                        )
                    )
                } else {
                    // Keep original light theme gradient unchanged
                    Brush.verticalGradient(colors = listOf(Color(0xFFEFF6FF), Color(0xFFF7FBFF)))
                }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
        Spacer(Modifier.height(12.dp))
        TasksGradientTopBar(appIntegrationManager)
        Spacer(Modifier.height(8.dp))
        SegmentedControl(
            segments = listOf("Daily", "Weekly", "Plan"),
            selectedIndex = selectedTab,
            onSelect = { selectedTab = it }
        )
        Spacer(Modifier.height(8.dp))
        when (selectedTab) {
            0 -> DailyTab(
                selectedDay = selectedDay,
                currentWeek = currentWeek,
                taskRepository = taskRepository,
                sharedViewModel = sharedViewModel,
                onBackToPlan = { selectedTab = 2 }
            )
            1 -> WeeklyTab(
                thisWeek = thisWeek,
                currentWeek = currentWeek,
                studyProgressRepository = studyProgressRepository,
                onNavigateToPlan = { selectedTab = 2 },
                onNavigateToStudyPlan = onNavigateToStudyPlan
            )
            2 -> PlanTab(
                thisWeek = thisWeek,
                weeklyProgressPct = weeklyProgressPct,
                onDayClick = { day ->
                    selectedDay = day
                    selectedTab = 0  // Switch to Daily tab
                },
                onNavigateToStudyPlan = onNavigateToStudyPlan
            )
        }
        }
    }
}

@Composable
private fun SegmentedControl(
    segments: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val heights = responsiveHeights()
    touchTargetSize()
    // Rail
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        // Sliding indicator layout
        BoxWithConstraints(Modifier.padding(4.dp)) {
            val segmentCount = segments.size.coerceAtLeast(1)
            val segWidth: Dp = maxWidth / segmentCount
            val targetOffset = segWidth * selectedIndex
            val animatedOffset by animateDpAsState(
                targetValue = targetOffset,
                animationSpec = androidx.compose.animation.core.tween(250, easing = FastOutSlowInEasing),
                label = "seg_offset"
            )

            // Indicator behind text
            Surface(
                modifier = Modifier
                    .offset(x = animatedOffset)
                    .width(segWidth)
                    .height(heights.button)
                    .clip(RoundedCornerShape(20.dp)),
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {}

            // Hit targets and labels
            Row(
                modifier = Modifier.height(heights.button),
                verticalAlignment = Alignment.CenterVertically
            ) {
                segments.forEachIndexed { index, label ->
                    val selected = index == selectedIndex
                    val textColor by animateColorAsState(
                        targetValue = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        label = "seg_text_$index"
                    )
                    Box(
                        modifier = Modifier
                            .width(segWidth)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(20.dp))
                            .selectable(selected = selected, onClick = { onSelect(index) }, role = Role.Tab),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = textColor,
                            fontSize = 14.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TasksGradientTopBar(
    appIntegrationManager: AppIntegrationManager? = null
) {
    // Calculate real XP from completed tasks
    val allTasks by (appIntegrationManager?.getAllTasks()?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) })
    val completedTasks = allTasks.filter { it.isCompleted }
    val totalXP = completedTasks.size * 10 // 10 XP per completed task
    val isFirstTimeUser = allTasks.isEmpty()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Task,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "Daily Tasks",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isFirstTimeUser) "Create your first task to get started!" else "Complete tasks to build your streak",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // XP Badge with pastel gradient
                Box(
                    modifier = Modifier
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                )
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Bolt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            if (isFirstTimeUser) "0 XP" else "$totalXP XP",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklyTab(
    thisWeek: WeekPlan?,
    currentWeek: Int,
    studyProgressRepository: com.mtlc.studyplan.data.StudyProgressRepository,
    onNavigateToPlan: () -> Unit = {},
    onNavigateToStudyPlan: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val cardShape = RoundedCornerShape(16.dp)

    // Calculate weekly stats from real plan data
    val totalTasks = remember(thisWeek) {
        thisWeek?.days?.sumOf { it.tasks.size } ?: 0
    }

    // For initial use: Show 0 completed tasks (user starts fresh)
    // This will be updated when task completion tracking is integrated
    val completedTasks = 0

    val weekProgress = remember(completedTasks, totalTasks) {
        if (totalTasks > 0) completedTasks.toFloat() / totalTasks else 0f
    }

    val currentDayOfWeek = remember { LocalDate.now().dayOfWeek.value }
    val daysCompleted = remember(currentDayOfWeek) {
        currentDayOfWeek.coerceIn(1, 7) // Monday=1, Sunday=7
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        // Week Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = cardShape,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Week $currentWeek of 30",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Day $daysCompleted of 7",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(
                                    "${(weekProgress * 100).toInt()}%",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Progress bar
                    Column {
                        LinearProgressIndicator(
                            progress = { weekProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "$completedTasks of $totalTasks tasks completed",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Week Navigation
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = cardShape
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            if (currentWeek > 1) {
                                scope.launch {
                                    studyProgressRepository.setManualWeekOverride(currentWeek - 1)
                                }
                            }
                        },
                        enabled = currentWeek > 1,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous Week",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Previous")
                    }

                    Text(
                        "Week $currentWeek",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    OutlinedButton(
                        onClick = {
                            if (currentWeek < 30) {
                                scope.launch {
                                    studyProgressRepository.setManualWeekOverride(currentWeek + 1)
                                }
                            }
                        },
                        enabled = currentWeek < 30,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Next")
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next Week",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Daily Summary
        item {
            Text(
                "This Week's Schedule",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        // Days of the week
        thisWeek?.days?.forEachIndexed { index, day ->
            item {
                WeeklyDayCard(
                    day = day,
                    dayNumber = index + 1,
                    isToday = index + 1 == currentDayOfWeek,
                    onClick = onNavigateToPlan
                )
            }
        }

        // Quick Actions
        item {
            Spacer(Modifier.height(8.dp))
            Text(
                "Quick Actions",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = cardShape,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToPlan() }
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Filled.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "View Plan",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = cardShape,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToStudyPlan() }
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Filled.School,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Study Plan",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }

        // Week Stats Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = cardShape,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Week Statistics",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        WeekStatItem(
                            icon = Icons.Filled.CheckCircle,
                            label = "Completed",
                            value = "$completedTasks",
                            color = MaterialTheme.colorScheme.primary
                        )
                        WeekStatItem(
                            icon = Icons.Filled.Circle,
                            label = "Remaining",
                            value = "${totalTasks - completedTasks}",
                            color = MaterialTheme.colorScheme.secondary
                        )
                        WeekStatItem(
                            icon = Icons.AutoMirrored.Filled.MenuBook,
                            label = "Total",
                            value = "$totalTasks",
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklyDayCard(
    day: DayPlan,
    dayNumber: Int,
    isToday: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = if (isToday) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (isToday) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                modifier = Modifier.size(48.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        dayNumber.toString(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isToday) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        day.day,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (isToday) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                "TODAY",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "${day.tasks.size} tasks",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = "View",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WeekStatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PlanTab(
    thisWeek: WeekPlan?,
    weeklyProgressPct: Float,
    onDayClick: (DayPlan) -> Unit = {},
    onNavigateToStudyPlan: () -> Unit = {}
) {
    val cardShape = RoundedCornerShape(16.dp)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        // Study Plan Overview Card
        item {
            Card(
                shape = cardShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToStudyPlan() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.EditCalendar,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "View Full Study Plan",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Comprehensive YDS preparation schedule",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = "Navigate to full study plan",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // This Week's Study Plan
        item {
            Card(shape = cardShape, colors = CardDefaults.cardColors(containerColor = featurePastelContainer(FeatureKey.TASKS, "tasks_surface_block"))) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("This Week's Study Plan", fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("Reading Comprehension Focus", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Week Progress", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${(weeklyProgressPct * 100).toInt()}%", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { weeklyProgressPct.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Daily Schedule", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    DayScheduleList(thisWeek, onDayClick)
                }
            }
        }

        // Upcoming Days
        item {
            Card(shape = cardShape, colors = CardDefaults.cardColors(containerColor = featurePastelContainer(FeatureKey.TASKS, "tasks_secondary_block"))) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Upcoming Days", fontWeight = FontWeight.SemiBold)
                        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                            Text("Planned", modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp), fontSize = 11.sp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Thu: Grammar deep dive + Vocabulary expansion", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Fri: Mixed practice + Practice test preparation", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Weekend: Review week + Prep next week's plan", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

    }
}

@Composable
private fun DayScheduleList(week: WeekPlan?, onDayClick: (DayPlan) -> Unit = {}) {
    if (week == null) {
        Text("No plan available", color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val fmt = DateTimeFormatter.ofPattern("EEE, MMM d").withLocale(Locale.getDefault())
    val monday = LocalDate.now().with(java.time.DayOfWeek.MONDAY)
    week.days.forEachIndexed { idx, day ->
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            tonalElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .clickable { onDayClick(day) }
        ) {
            Column(Modifier.padding(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    val dateLabel = monday.plusDays(idx.toLong()).format(fmt)
                    val isToday = LocalDate.now() == monday.plusDays(idx.toLong())
                    Text(if (isToday) "${day.day}, $dateLabel (Today)" else "${day.day}, $dateLabel", fontWeight = FontWeight.SemiBold)
                    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                        Text(if (isToday) "In Progress" else "Completed", modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp), fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.height(6.dp))
                day.tasks.forEach { t ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = t.desc,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "09:00-09:30",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

// Daily Tab Implementation
@Composable
private fun DailyTab(
    selectedDay: DayPlan?,
    currentWeek: Int = 1,
    taskRepository: TaskRepository,
    sharedViewModel: SharedAppViewModel,
    onBackToPlan: () -> Unit
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(selectedDay) {
        if (selectedDay != null) {
            isLoading = true
            kotlinx.coroutines.delay(800) // Simulate loading
            isLoading = false
        }
    }

    if (selectedDay == null) {
        // No day selected - show placeholder
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Select a Day from the Plan",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Click on any day in the Plan tab to view detailed study materials and tasks",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
                OutlinedButton(
                    onClick = onBackToPlan,
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text("Go to Plan")
                }
            }
        }
        return
    }

    if (isLoading) {
        // Loading state
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator()
                Text(
                    text = "Loading ${selectedDay.day} Study Plan...",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    // Create study info based on selected day
    val studyInfo = createDailyStudyInfo(selectedDay, currentWeek, context)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        // Header
        item {
            DailyStudyHeader(studyInfo, onBackToPlan)
        }

        // Study Book
        item {
            StudyBookCard(
                book = studyInfo.book,
                units = studyInfo.units,
                taskRepository = taskRepository,
                sharedViewModel = sharedViewModel
            )
        }

        // Daily Tasks
        item {
            DailyTasksSection(studyInfo.tasks)
        }

        // Study Materials
        item {
            StudyMaterialsSection(studyInfo.materials)
        }

        // Notes
        if (studyInfo.notes.isNotEmpty()) {
            item {
                NotesSection(studyInfo.notes)
            }
        }
    }
}

// Helper data classes and functions
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
    val description: String,
    val murphyBook: MurphyBook? = null
) {
    companion object {
        val RED_BOOK = StudyBook("Red Book - Essential Grammar in Use", Color(0xFFE53935), "Foundation Level Grammar", CompleteMurphyBookData.RED_BOOK)
        val BLUE_BOOK = StudyBook("Blue Book - English Grammar in Use", Color(0xFF1976D2), "Intermediate Level Grammar", CompleteMurphyBookData.BLUE_BOOK)
        val GREEN_BOOK = StudyBook("Green Book - Advanced Grammar in Use", Color(0xFF388E3C), "Advanced Level Grammar", CompleteMurphyBookData.GREEN_BOOK)
    }
}

data class StudyUnit(
    val title: String,
    val unitNumber: Int,
    val pages: String,
    val exercises: List<String>,
    val isCompleted: Boolean = false,
    val estimatedMinutes: Int = 30,
    val vocabulary: List<String> = emptyList(),
    val grammarTopic: String = "",
    val murphyUnit: MurphyUnit? = null
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

enum class Priority { HIGH, MEDIUM, LOW }

@Composable
private fun DailyStudyHeader(studyInfo: DailyStudyInfo, onBackToPlan: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = studyInfo.book.color.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = studyInfo.dayName,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = studyInfo.book.color
                    )
                    Text(
                        text = studyInfo.date,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = studyInfo.weekTitle,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedButton(onClick = onBackToPlan) {
                    Text("← Plan")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Estimated Time: ${studyInfo.estimatedTime}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    color = when {
                        studyInfo.completionPercentage >= 100 -> MaterialTheme.colorScheme.primary
                        studyInfo.completionPercentage > 0 -> Color(0xFFFF9800)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "${studyInfo.completionPercentage}%",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun StudyBookCard(
    book: StudyBook,
    units: List<StudyUnit>,
    taskRepository: TaskRepository,
    sharedViewModel: SharedAppViewModel
) {
    var selectedUnit by remember { mutableStateOf<StudyUnit?>(null) }
    var isCreatingTask by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Track completed Murphy tasks
    val completedTasks by taskRepository.getAllTasks().collectAsState(initial = emptyList())
    val murphyTaskIds = completedTasks
        .filter { it.category == "Murphy Grammar" && it.isCompleted }
        .map { it.id }
        .toSet()

    Card(
        colors = CardDefaults.cardColors(containerColor = featurePastelContainer(FeatureKey.TASKS, "tasks_row_card_1")),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = book.color,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = book.name.first().toString(),
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = book.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = book.description,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (units.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Units for Today",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Create stable callback using remember
                val onUnitClick = remember<(StudyUnit) -> Unit> { { unit ->
                    selectedUnit = unit
                    val taskId = generateMurphyTaskId(book, unit)
                    val isCompleted = murphyTaskIds.contains(taskId)
                    if (!isCompleted && !isCreatingTask) {
                        isCreatingTask = true
                    }
                }}

                units.forEach { unit ->
                    val taskId = generateMurphyTaskId(book, unit)
                    val isCompleted = murphyTaskIds.contains(taskId)

                    StudyUnitItem(
                        unit = unit.copy(isCompleted = isCompleted),
                        onClick = { onUnitClick(unit) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

    // Handle task creation and completion
    LaunchedEffect(selectedUnit, isCreatingTask) {
        if (selectedUnit != null && isCreatingTask) {
            val unit = selectedUnit!!
            val taskId = generateMurphyTaskId(book, unit)

            // Check if task already exists
            val existingTask = taskRepository.getTaskById(taskId)

            if (existingTask == null) {
                // Create new Murphy task
                val murphyTask = createMurphyTask(book, unit)
                taskRepository.insertTask(murphyTask)

                // Complete the task immediately (Murphy units are practice-based)
                val completedTask = murphyTask.copy(
                    isCompleted = true,
                    completedAt = System.currentTimeMillis(),
                    actualMinutes = unit.estimatedMinutes
                )
                taskRepository.updateTask(completedTask)

                // Trigger gamification and progress tracking
                sharedViewModel.completeTask(taskId)

                // Show completion feedback
                Toast.makeText(
                    context,
                    "Completed: ${unit.title}",
                    Toast.LENGTH_SHORT
                ).show()
            }

            isCreatingTask = false
            selectedUnit = null
        }
    }
}

@Composable
private fun StudyUnitItem(unit: StudyUnit, onClick: () -> Unit = {}) {
    Surface(
        color = if (unit.isCompleted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (unit.isCompleted) Icons.Filled.CheckCircle else Icons.Filled.Circle,
                contentDescription = null,
                tint = if (unit.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Unit ${unit.unitNumber}: ${unit.title}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Pages ${unit.pages}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (unit.exercises.isNotEmpty()) {
                    Text(
                        text = "Exercises: ${unit.exercises.joinToString(", ")}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyTasksSection(tasks: List<DailyTask>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = featurePastelContainer(FeatureKey.TASKS, "tasks_row_card_2")),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Daily Tasks",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (tasks.isEmpty()) {
                Text(
                    text = "No specific tasks for today. Focus on your study units!",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                // Use forEach for now since we're not in a LazyColumn context
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
    Surface(
        color = if (task.isCompleted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = if (task.isCompleted) Icons.Filled.CheckCircle else Icons.Filled.Circle,
                contentDescription = null,
                tint = if (task.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
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
                    Priority.HIGH -> Color(0xFFE53935)
                    Priority.MEDIUM -> Color(0xFFFF9800)
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
}

@Composable
private fun StudyMaterialsSection(materials: List<StudyMaterial>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = featurePastelContainer(FeatureKey.TASKS, "tasks_row_card_3")),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Study Materials",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (materials.isEmpty()) {
                Text(
                    text = "No additional materials for today. Focus on your textbook units!",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
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
    val context = LocalContext.current
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.clickable {
            if (material.url.isNotEmpty()) {
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(material.url))
                context.startActivity(intent)
            }
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (material.type) {
                    MaterialType.VIDEO -> Icons.Filled.PlayArrow
                    MaterialType.AUDIO -> Icons.Filled.Star
                    MaterialType.PDF -> Icons.Filled.Star
                    MaterialType.EXERCISE -> Icons.Filled.Quiz
                    MaterialType.QUIZ -> Icons.Filled.Quiz
                    MaterialType.READING -> Icons.Filled.Star
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = material.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (material.description.isNotEmpty()) {
                    Text(
                        text = material.description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = "Open link",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun NotesSection(notes: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = featurePastelContainer(FeatureKey.TASKS, "tasks_row_card_4")),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.EditCalendar,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Study Notes",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = notes,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                lineHeight = 20.sp
            )
        }
    }
}

// Helper function to create DailyStudyInfo from DayPlan
private fun createDailyStudyInfo(dayPlan: DayPlan, currentWeek: Int = 1, context: Context): DailyStudyInfo {
    val dayIndex = PlanTaskLocalizer.dayIndex(dayPlan.day)

    val localizedDayName = PlanTaskLocalizer.localizeDayName(dayPlan.day, context)

    val localizedPlanTasks = dayPlan.tasks.map { PlanTaskLocalizer.localize(it, context) }



    // Assign books based on the 30-week curriculum progression
    val book = when (currentWeek) {
        in 1..8 -> StudyBook.RED_BOOK      // Weeks 1-8: Red Book Foundation
        in 9..18 -> StudyBook.BLUE_BOOK    // Weeks 9-18: Blue Book Intermediate
        in 19..26 -> StudyBook.GREEN_BOOK  // Weeks 19-26: Green Book Advanced
        else -> StudyBook.RED_BOOK         // Weeks 27-30: Exam Camp (mixed/review - default to Red for now)
    }

    // Create units using actual Murphy book data
    val units = when {
        // Red Book: Units 1-114 across 8 weeks
        book == StudyBook.RED_BOOK -> {
            val murphyUnits = CompleteMurphyBookData.getUnitsForWeekAndDay(currentWeek, dayIndex)
            if (murphyUnits.isNotEmpty()) {
                murphyUnits.map { unit ->
                    StudyUnit(
                        title = unit.title,
                        unitNumber = unit.unitNumber,
                        pages = unit.pages,
                        exercises = unit.exercises,
                        isCompleted = dayIndex < 3,
                        estimatedMinutes = parseEstimatedMinutes(unit.estimatedTime),
                        vocabulary = unit.keyPoints.take(5), // Use keyPoints as vocabulary
                        grammarTopic = unit.title, // Use title as grammar topic
                        murphyUnit = unit
                    )
                }
            } else {
                // Fallback to basic unit if Murphy unit not found
                val baseUnitNumber = (currentWeek - 1) * 14 + dayIndex * 2 + 1
                listOf(
                    StudyUnit(
                        title = when (baseUnitNumber % 10) {
                            0 -> "Present Simple and Continuous"
                            1 -> "Past Simple and Continuous"
                            2 -> "Present Perfect"
                            3 -> "Modal Verbs"
                            4 -> "Future Forms"
                            5 -> "Conditionals"
                            6 -> "Passive Voice"
                            7 -> "Reported Speech"
                            8 -> "Gerunds and Infinitives"
                            else -> "Questions and Negatives"
                        },
                        unitNumber = minOf(baseUnitNumber, 114),
                        pages = "${baseUnitNumber * 2}-${baseUnitNumber * 2 + 3}",
                        exercises = listOf("${baseUnitNumber}.1", "${baseUnitNumber}.2"),
                        isCompleted = dayIndex < 3,
                        estimatedMinutes = 30,
                        vocabulary = listOf("essential", "basic", "fundamental"),
                        grammarTopic = "Essential Grammar"
                    )
                )
            }
        }

        // Blue Book: Intermediate level topics
        book == StudyBook.BLUE_BOOK -> {
            val murphyUnits = CompleteMurphyBookData.getUnitsForWeekAndDay(currentWeek, dayIndex)
            if (murphyUnits.isNotEmpty()) {
                murphyUnits.map { unit ->
                    StudyUnit(
                        title = unit.title,
                        unitNumber = unit.unitNumber,
                        pages = unit.pages,
                        exercises = unit.exercises,
                        isCompleted = dayIndex < 3,
                        estimatedMinutes = parseEstimatedMinutes(unit.estimatedTime),
                        vocabulary = unit.keyPoints.take(5), // Use keyPoints as vocabulary
                        grammarTopic = unit.title, // Use title as grammar topic
                        murphyUnit = unit
                    )
                }
            } else {
                // Fallback for Blue Book
                val weekInBlueBook = currentWeek - 8
                listOf(
                    StudyUnit(
                        title = when (weekInBlueBook) {
                            1 -> "Tenses Review (All Tenses Comparison)"
                            2 -> "Future in Detail (Continuous/Perfect)"
                            3 -> "Modals 1 (Ability, Permission, Advice)"
                            4 -> "Modals 2 (Deduction, Obligation, Regret)"
                            5 -> "Conditionals & Wish (All Types)"
                            6 -> "Passive Voice (All Tenses) & 'have something done'"
                            7 -> "Reported Speech (Questions, Commands, Advanced)"
                            8 -> "Noun Clauses & Relative Clauses"
                            9 -> "Gerunds & Infinitives (Advanced patterns)"
                            10 -> "Conjunctions & Connectors"
                            else -> "Advanced Grammar Review"
                        },
                        unitNumber = weekInBlueBook * 10 + dayIndex,
                        pages = "${weekInBlueBook * 8}-${weekInBlueBook * 8 + 7}",
                        exercises = listOf("${weekInBlueBook}.1", "${weekInBlueBook}.2", "${weekInBlueBook}.3"),
                        isCompleted = dayIndex < 3
                    )
                )
            }
        }

        // Green Book: Advanced level topics
        book == StudyBook.GREEN_BOOK -> {
            val murphyUnits = CompleteMurphyBookData.getUnitsForWeekAndDay(currentWeek, dayIndex)
            if (murphyUnits.isNotEmpty()) {
                murphyUnits.map { unit ->
                    StudyUnit(
                        title = unit.title,
                        unitNumber = unit.unitNumber,
                        pages = unit.pages,
                        exercises = unit.exercises,
                        isCompleted = dayIndex < 2,
                        murphyUnit = unit
                    )
                }
            } else {
                // Fallback for Green Book
                val weekInGreenBook = currentWeek - 18
                listOf(
                    StudyUnit(
                        title = when (weekInGreenBook) {
                            1 -> "Advanced Tense Nuances & Narrative Tenses"
                            2 -> "Inversion & Emphasis (Not only, Hardly...)"
                            3 -> "Advanced Modals (Speculation, Hypothetical)"
                            4 -> "Participle Clauses (-ing and -ed clauses)"
                            5 -> "Advanced Connectors & Discourse Markers"
                            6 -> "Hypothetical Meaning & Subjunctives"
                            7 -> "Adjectives & Adverbs (Advanced Uses)"
                            else -> "Prepositions & Phrasal Verbs (Advanced)"
                        },
                        unitNumber = weekInGreenBook * 5 + dayIndex,
                        pages = "${weekInGreenBook * 6}-${weekInGreenBook * 6 + 5}",
                        exercises = listOf("${weekInGreenBook}.1", "${weekInGreenBook}.2"),
                        isCompleted = dayIndex < 2
                    )
                )
            }
        }

        else -> emptyList()
    }

    // Create sample tasks based on the day's existing tasks
    val tasks = localizedPlanTasks.take(3).mapIndexed { index, task ->
        val description = "Complete the assigned ${task.desc.lowercase(Locale.getDefault())} exercises"

        DailyTask(
            title = task.desc,
            description = description,
            estimatedDuration = "${20 + index * 10} minutes",
            priority = when (index) {
                0 -> Priority.HIGH
                1 -> Priority.MEDIUM
                else -> Priority.LOW
            },
            isCompleted = index == 0 && dayIndex < 3
        )
    }
    // Real study materials from trusted platforms
    val materials = listOf(
        StudyMaterial(
            title = "British Council LearnEnglish",
            type = MaterialType.VIDEO,
            url = "https://learnenglish.britishcouncil.org/",
            description = "Grammar lessons, vocabulary and pronunciation exercises"
        ),
        StudyMaterial(
            title = "Cambridge English Practice",
            type = MaterialType.EXERCISE,
            url = "https://www.cambridgeenglish.org/learning-english/",
            description = "Official Cambridge exam practice materials"
        ),
        StudyMaterial(
            title = "BBC Learning English",
            type = MaterialType.AUDIO,
            url = "https://www.bbc.co.uk/learningenglish/",
            description = "Daily English lessons with audio and video content"
        ),
        StudyMaterial(
            title = "Duolingo English Test Prep",
            type = MaterialType.QUIZ,
            url = "https://englishtest.duolingo.com/prepare",
            description = "Interactive practice tests and quizzes"
        ),
        StudyMaterial(
            title = "Oxford Online English",
            type = MaterialType.VIDEO,
            url = "https://www.youtube.com/@OxfordOnlineEnglish",
            description = "Free video lessons covering grammar, vocabulary, and more"
        )
    ).take(3 + dayIndex % 2)

    // Create notes based on the day
    val notes = when (dayIndex) {
        0 -> "Focus on understanding the fundamental concepts. Take your time with the exercises."
        1 -> "Pay special attention to irregular verbs. Practice pronunciation."
        2 -> "This unit builds on previous lessons. Review Unit 9 if needed."
        3 -> "Challenge day! Try to complete all exercises without looking at answers first."
        4 -> "Review week: Good time to consolidate what you've learned this week."
        5 -> "Weekend practice: Take a more relaxed approach and focus on areas you found difficult."
        6 -> "Preparation day: Review the upcoming week's material and plan your study schedule."
        else -> ""
    }

    return DailyStudyInfo(
        weekTitle = "Week $currentWeek - ${book.description}",
        dayName = localizedDayName,
        date = LocalDate.now().plusDays(dayIndex.toLong()).format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())),
        book = book,
        units = units,
        tasks = tasks,
        materials = materials,
        estimatedTime = "2-3 hours",
        completionPercentage = if (dayIndex < 3) 100 else if (dayIndex < 5) 50 else 0,
        notes = notes
    )
}

/**
 * Murphy Task Integration Helper Functions
 * These functions handle the conversion between Murphy units and PlanTasks
 */

/**
 * Generate a unique task ID for a Murphy unit
 */
private fun generateMurphyTaskId(book: StudyBook, unit: StudyUnit): String {
    return "murphy_${book.name.replace(" ", "_").lowercase()}_unit_${unit.unitNumber}"
}

/**
 * Create a Task from a Murphy unit
 */
private fun createMurphyTask(book: StudyBook, unit: StudyUnit): Task {
    return Task(
        id = generateMurphyTaskId(book, unit),
        title = "${book.name} - Unit ${unit.unitNumber}: ${unit.title}",
        description = buildMurphyTaskDescription(book, unit),
        category = "Murphy Grammar",
        priority = TaskPriority.MEDIUM,
        dueDate = System.currentTimeMillis() + (24 * 60 * 60 * 1000), // Due tomorrow
        estimatedMinutes = unit.estimatedMinutes,
        tags = listOf("murphy", "grammar", book.name.lowercase(), "unit${unit.unitNumber}"),
        pointsValue = calculateMurphyTaskPoints(unit)
    )
}

/**
 * Build detailed description for Murphy task
 */
private fun buildMurphyTaskDescription(book: StudyBook, unit: StudyUnit): String {
    val description = StringBuilder()
    description.append("📖 ${book.name}\n")
    description.append("📄 Pages: ${unit.pages}\n")
    description.append("⏱️ Estimated time: ${unit.estimatedMinutes} minutes\n\n")

    if (unit.exercises.isNotEmpty()) {
        description.append("📝 Exercises:\n")
        unit.exercises.forEach { exercise ->
            description.append("• $exercise\n")
        }
        description.append("\n")
    }

    if (unit.vocabulary.isNotEmpty()) {
        description.append("📚 Key Vocabulary:\n")
        unit.vocabulary.take(5).forEach { vocab ->
            description.append("• $vocab\n")
        }
        if (unit.vocabulary.size > 5) {
            description.append("• ... and ${unit.vocabulary.size - 5} more words\n")
        }
        description.append("\n")
    }

    description.append("🎯 Grammar Focus: ${unit.grammarTopic}\n")
    description.append("📝 Practice completing the exercises and reviewing key concepts.")

    return description.toString()
}

/**
 * Calculate points value for Murphy task based on difficulty and content
 */
private fun calculateMurphyTaskPoints(unit: StudyUnit): Int {
    var points = 15 // Base points for grammar tasks

    // Add points for exercises
    points += unit.exercises.size * 2

    // Add points for vocabulary
    points += (unit.vocabulary.size / 5) * 3

    // Bonus for longer estimated time
    if (unit.estimatedMinutes > 30) points += 5
    if (unit.estimatedMinutes > 45) points += 5

    return points.coerceAtMost(35) // Cap at 35 points
}

/**
 * Parse estimated time string to minutes
 */
private fun parseEstimatedMinutes(estimatedTime: String): Int {
    return when {
        estimatedTime.contains("min", ignoreCase = true) -> {
            estimatedTime.filter { it.isDigit() }.toIntOrNull() ?: 30
        }
        estimatedTime.contains("hour", ignoreCase = true) -> {
            val hours = estimatedTime.filter { it.isDigit() }.toIntOrNull() ?: 1
            hours * 60
        }
        else -> 30 // Default 30 minutes
    }
}



