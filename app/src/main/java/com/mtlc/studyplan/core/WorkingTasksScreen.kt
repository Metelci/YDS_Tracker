package com.mtlc.studyplan.core

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Task
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtlc.studyplan.data.*
import com.mtlc.studyplan.R
import com.mtlc.studyplan.integration.AppIntegrationManager
import com.mtlc.studyplan.ui.responsive.responsiveHeights
import com.mtlc.studyplan.ui.responsive.touchTargetSize
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.mtlc.studyplan.data.CompleteMurphyBookData
import com.mtlc.studyplan.data.MurphyTaskInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkingTasksScreen(
    appIntegrationManager: AppIntegrationManager,
    studyProgressRepository: com.mtlc.studyplan.data.StudyProgressRepository,
    onNavigateBack: () -> Unit = {},
    onNavigateToStudyPlan: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Get current week from study progress repository
    val currentWeek by studyProgressRepository.currentWeek.collectAsState(initial = 1)

    val plan = remember { PlanDataSource.planData }

    var selectedTab by remember { mutableStateOf(2) } // 0: Daily, 1: Weekly, 2: Plan, 3: Custom
    var selectedDay by remember { mutableStateOf<DayPlan?>(null) }

    // Get the week that corresponds to user's current progress
    val thisWeek = remember(currentWeek) {
        plan.getOrNull(currentWeek - 1) ?: plan.firstOrNull()
    }
    val weeklyIds = remember(thisWeek) { thisWeek?.days?.flatMap { it.tasks }?.map { it.id }?.toSet() ?: emptySet() }
    val weeklyCompleted = 0
    val weeklyTotal = remember(weeklyIds) { weeklyIds.size.coerceAtLeast(1) }
    val weeklyProgressPct = remember(weeklyCompleted, weeklyTotal) { (weeklyCompleted.toFloat() / weeklyTotal) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
            segments = listOf("Daily", "Weekly", "Plan", "Custom"),
            selectedIndex = selectedTab,
            onSelect = { selectedTab = it }
        )
        Spacer(Modifier.height(8.dp))
        when (selectedTab) {
            0 -> DailyTab(selectedDay, currentWeek = currentWeek, onBackToPlan = { selectedTab = 2 })
            2 -> PlanTab(
                thisWeek = thisWeek,
                weeklyProgressPct = weeklyProgressPct,
                onDayClick = { day ->
                    selectedDay = day
                    selectedTab = 0  // Switch to Daily tab
                },
                onNavigateToStudyPlan = onNavigateToStudyPlan
            )
            else -> PlaceholderTab()
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
    val touchTarget = touchTargetSize()
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
private fun XpButton(xp: Int) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer, shape = RoundedCornerShape(24.dp)) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("${xp} XP", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun PlaceholderTab() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Coming soon", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            Card(shape = cardShape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
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
            Card(shape = cardShape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))) {
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

        // Plan Management
        item {
            Card(shape = cardShape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))) {
                Column(Modifier.padding(16.dp)) {
                    Text("Plan Management", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        ManagementTile(
                            title = "Modify\nThis Week",
                            icon = Icons.Filled.EditCalendar,
                            modifier = Modifier.weight(1f)
                        )
                        ManagementTile(
                            title = "Generate\nNext Week",
                            icon = Icons.Filled.Quiz,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { }) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(6.dp))
                        Text("View Planning Analytics", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun ManagementTile(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 0.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.clickable { }
    ) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(8.dp))
            Text(title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, lineHeight = 14.sp)
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

// Enhanced Task Detail Modal with Murphy Book Information
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskDetailModal(
    task: PlanTask?,
    onDismiss: () -> Unit
) {
    if (task == null) return

    val murphyTaskInfo = CompleteMurphyBookData.parseMurphyTask(task.details)
    val isGrammarTask = task.desc.contains("Gramer Konulari", true)
    val showMurphyDetails = isGrammarTask && murphyTaskInfo != null

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Task Header
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = task.desc,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (task.details != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = task.details,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Murphy Book Information (if this is a grammar task)
            if (showMurphyDetails) {
                // Book Header
                val info = murphyTaskInfo!!
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = info.book.color.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Surface(
                                    color = info.book.color,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = info.book.name.first().toString(),
                                            color = Color.White,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = info.book.name,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = info.book.color
                                    )
                                    Text(
                                        text = info.book.level,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Units Information
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Units ${info.unitRange}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = info.book.color
                                )

                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "${info.units.size} units",
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Display individual units
                            info.units.take(3).forEach { unit ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "Unit ${unit.unitNumber}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "Pages ${unit.pages}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = unit.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            if (info.units.size > 3) {
                                Text(
                                    text = "...and ${info.units.size - 3} more units",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                }

                // Study Instructions
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Study Instructions",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = info.book.studyMethodology,
                                style = MaterialTheme.typography.bodyMedium,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }

                // Sample Unit Details (show first unit)
                if (info.units.isNotEmpty()) {
                    val sampleUnit = info.units.first()

                    // Key Grammar Points
                    if (sampleUnit.keyPoints.isNotEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Star,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Key Points (Sample from Unit ${sampleUnit.unitNumber})",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    sampleUnit.keyPoints.forEach { point ->
                                        Row(
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Circle,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(8.dp).padding(top = 8.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = point,
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Example Sentences
                    if (sampleUnit.exampleSentences.isNotEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.FormatQuote,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Example Sentences (Unit ${sampleUnit.unitNumber})",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    sampleUnit.exampleSentences.forEach { example ->
                                        Surface(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = example,
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier.padding(12.dp),
                                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Non-grammar task - show basic task details
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Assignment,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Study Task",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Complete this study task according to your plan. Check off when finished.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// Daily Tab Implementation
@Composable
private fun DailyTab(selectedDay: DayPlan?, currentWeek: Int = 1, onBackToPlan: () -> Unit) {
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
            StudyBookCard(studyInfo.book, studyInfo.units)
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
private fun StudyBookCard(book: StudyBook, units: List<StudyUnit>) {
    var selectedUnit by remember { mutableStateOf<StudyUnit?>(null) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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

                units.forEach { unit ->
                    StudyUnitItem(
                        unit = unit,
                        onClick = { selectedUnit = unit }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        // TODO: Integrate with real Murphy task system
        // Task detail modal removed - will be integrated with real PlanTask system
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.clickable { /* Handle material click */ }
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
                    fontWeight = FontWeight.Medium
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
}

@Composable
private fun NotesSection(notes: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.EditCalendar,
                    contentDescription = null,
                    tint = Color(0xFFFF8F00),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Study Notes",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = notes,
                fontSize = 14.sp,
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
                        isCompleted = dayIndex < 3
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
    // Create sample materials
    val materials = listOf(
        StudyMaterial(
            title = "Grammar Video Lesson",
            type = MaterialType.VIDEO,
            description = "Interactive video explaining today's grammar concepts"
        ),
        StudyMaterial(
            title = "Practice Worksheet",
            type = MaterialType.PDF,
            description = "Additional practice exercises"
        ),
        StudyMaterial(
            title = "Interactive Quiz",
            type = MaterialType.QUIZ,
            description = "Test your understanding of today's lesson"
        )
    ).take(2 + dayIndex % 2)

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

