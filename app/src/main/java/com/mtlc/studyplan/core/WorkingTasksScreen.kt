package com.mtlc.studyplan.core

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
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtlc.studyplan.data.*
import com.mtlc.studyplan.integration.AppIntegrationManager
import com.mtlc.studyplan.ui.theme.DesignTokens
import com.mtlc.studyplan.ui.responsive.responsiveHeights
import com.mtlc.studyplan.ui.responsive.touchTargetSize
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkingTasksScreen(
    appIntegrationManager: AppIntegrationManager,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val plan = remember { PlanDataSource.planData }

    var selectedTab by remember { mutableStateOf(2) } // 0: Daily, 1: Weekly, 2: Plan, 3: Custom
    var selectedDay by remember { mutableStateOf<DayPlan?>(null) }

    val thisWeek = plan.firstOrNull()
    val weeklyIds = remember(thisWeek) { thisWeek?.days?.flatMap { it.tasks }?.map { it.id }?.toSet() ?: emptySet() }
    val weeklyCompleted = 0
    val weeklyTotal = remember(weeklyIds) { weeklyIds.size.coerceAtLeast(1) }
    val weeklyProgressPct = remember(weeklyCompleted, weeklyTotal) { (weeklyCompleted.toFloat() / weeklyTotal) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DesignTokens.Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
        Spacer(Modifier.height(12.dp))
        HeaderWithButtons()
        Spacer(Modifier.height(8.dp))
        SegmentedControl(
            segments = listOf("Daily", "Weekly", "Plan", "Custom"),
            selectedIndex = selectedTab,
            onSelect = { selectedTab = it }
        )
        Spacer(Modifier.height(8.dp))
        when (selectedTab) {
            0 -> DailyTab(selectedDay, onBackToPlan = { selectedTab = 2 })
            2 -> PlanTab(
                thisWeek = thisWeek,
                weeklyProgressPct = weeklyProgressPct,
                onDayClick = { day ->
                    selectedDay = day
                    selectedTab = 0  // Switch to Daily tab
                }
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
        color = DesignTokens.SegmentedRail,
        border = BorderStroke(1.dp, DesignTokens.SegmentedBorder)
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
                color = DesignTokens.SegmentedPill,
                contentColor = DesignTokens.Foreground,
            ) {}

            // Hit targets and labels
            Row(
                modifier = Modifier.height(heights.button),
                verticalAlignment = Alignment.CenterVertically
            ) {
                segments.forEachIndexed { index, label ->
                    val selected = index == selectedIndex
                    val textColor by animateColorAsState(
                        targetValue = if (selected) DesignTokens.Foreground else DesignTokens.MutedForeground,
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
private fun HeaderWithButtons() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Tasks", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = DesignTokens.Foreground)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            XpButton(xp = 0)
        }
    }
}

@Composable
private fun XpButton(xp: Int) {
    Surface(color = DesignTokens.SecondaryContainer, contentColor = DesignTokens.SecondaryContainerForeground, shape = RoundedCornerShape(24.dp)) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Star, contentDescription = null, tint = DesignTokens.Success, modifier = Modifier.size(16.dp))
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
    onDayClick: (DayPlan) -> Unit = {}
) {
    val cardShape = RoundedCornerShape(16.dp)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        // This Week's Study Plan
        item {
            Card(shape = cardShape, colors = CardDefaults.cardColors(containerColor = DesignTokens.Surface)) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CalendarToday, contentDescription = null, tint = DesignTokens.Primary)
                        Spacer(Modifier.width(8.dp))
                        Text("This Week's Study Plan", fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("Reading Comprehension Focus", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Week Progress", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${(weeklyProgressPct * 100).toInt()}%", color = DesignTokens.Success, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { weeklyProgressPct.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = DesignTokens.Success,
                        trackColor = DesignTokens.PrimaryContainer
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
            Card(shape = cardShape, colors = CardDefaults.cardColors(containerColor = DesignTokens.SecondaryContainer.copy(alpha = 0.35f))) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Upcoming Days", fontWeight = FontWeight.SemiBold)
                        Surface(shape = RoundedCornerShape(16.dp), color = DesignTokens.SurfaceVariant) {
                            Text("Planned", modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp), fontSize = 11.sp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Thu: Grammar deep dive + Vocabulary expansion", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Fri: Mixed practice + Mock test preparation", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Weekend: Review week + Prep next week's plan", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Plan Management
        item {
            Card(shape = cardShape, colors = CardDefaults.cardColors(containerColor = DesignTokens.PrimaryContainer.copy(alpha = 0.35f))) {
                Column(Modifier.padding(16.dp)) {
                    Text("Plan Management", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        ManagementTile(title = "Modify\nThis Week", icon = Icons.Filled.EditCalendar, modifier = Modifier.weight(1f))
                        ManagementTile(title = "Generate\nNext Week", icon = Icons.Filled.Quiz, modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { }) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = DesignTokens.Primary)
                        Spacer(Modifier.width(6.dp))
                        Text("View Planning Analytics", color = DesignTokens.Primary)
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
        color = DesignTokens.Surface,
        modifier = modifier.clickable { }
    ) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = DesignTokens.Success, modifier = Modifier.size(24.dp))
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
            color = DesignTokens.SecondaryContainer.copy(alpha = 0.25f),
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
                    Surface(shape = RoundedCornerShape(16.dp), color = DesignTokens.SurfaceVariant) {
                        Text(if (isToday) "In Progress" else "Completed", modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp), fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.height(6.dp))
                day.tasks.forEach { t ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = DesignTokens.Success, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(t.desc, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.weight(1f))
                        Text("09:00-09:30", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

// Daily Tab Implementation
@Composable
private fun DailyTab(selectedDay: DayPlan?, onBackToPlan: () -> Unit) {
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
                    tint = DesignTokens.MutedForeground
                )
                Text(
                    text = "Select a Day from the Plan",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = DesignTokens.MutedForeground
                )
                Text(
                    text = "Click on any day in the Plan tab to view detailed study materials and tasks",
                    fontSize = 14.sp,
                    color = DesignTokens.MutedForeground,
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
                    color = DesignTokens.MutedForeground
                )
            }
        }
        return
    }

    // Create study info based on selected day
    val studyInfo = createDailyStudyInfo(selectedDay)

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
                        color = DesignTokens.MutedForeground
                    )
                    Text(
                        text = studyInfo.weekTitle,
                        fontSize = 14.sp,
                        color = DesignTokens.MutedForeground
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
                    color = DesignTokens.MutedForeground
                )

                Surface(
                    color = when {
                        studyInfo.completionPercentage >= 100 -> DesignTokens.Success
                        studyInfo.completionPercentage > 0 -> Color(0xFFFF9800)
                        else -> DesignTokens.MutedForeground
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
    Card(
        colors = CardDefaults.cardColors(containerColor = DesignTokens.Surface),
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
                        color = DesignTokens.MutedForeground
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
                    StudyUnitItem(unit)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun StudyUnitItem(unit: StudyUnit) {
    Surface(
        color = if (unit.isCompleted) DesignTokens.Success.copy(alpha = 0.1f) else DesignTokens.SurfaceVariant,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (unit.isCompleted) Icons.Filled.CheckCircle else Icons.Filled.Circle,
                contentDescription = null,
                tint = if (unit.isCompleted) DesignTokens.Success else DesignTokens.MutedForeground,
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
                    color = DesignTokens.MutedForeground
                )
                if (unit.exercises.isNotEmpty()) {
                    Text(
                        text = "Exercises: ${unit.exercises.joinToString(", ")}",
                        fontSize = 12.sp,
                        color = DesignTokens.MutedForeground
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyTasksSection(tasks: List<DailyTask>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DesignTokens.Surface),
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
                    color = DesignTokens.MutedForeground,
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
        color = if (task.isCompleted) DesignTokens.Success.copy(alpha = 0.1f) else DesignTokens.SurfaceVariant,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = if (task.isCompleted) Icons.Filled.CheckCircle else Icons.Filled.Circle,
                contentDescription = null,
                tint = if (task.isCompleted) DesignTokens.Success else DesignTokens.MutedForeground,
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
                    color = DesignTokens.MutedForeground
                )
                Text(
                    text = "Duration: ${task.estimatedDuration}",
                    fontSize = 12.sp,
                    color = DesignTokens.MutedForeground
                )
            }

            Surface(
                color = when (task.priority) {
                    Priority.HIGH -> Color(0xFFE53935)
                    Priority.MEDIUM -> Color(0xFFFF9800)
                    Priority.LOW -> DesignTokens.Success
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
        colors = CardDefaults.cardColors(containerColor = DesignTokens.Surface),
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
                    color = DesignTokens.MutedForeground,
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
        color = DesignTokens.SurfaceVariant,
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
                tint = DesignTokens.Primary,
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
                        color = DesignTokens.MutedForeground
                    )
                }
            }

            Text(
                text = material.type.name,
                fontSize = 10.sp,
                color = DesignTokens.Primary,
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
private fun createDailyStudyInfo(dayPlan: DayPlan): DailyStudyInfo {
    val dayIndex = when (dayPlan.day.lowercase()) {
        "monday" -> 0
        "tuesday" -> 1
        "wednesday" -> 2
        "thursday" -> 3
        "friday" -> 4
        "saturday" -> 5
        "sunday" -> 6
        else -> 0
    }

    // Assign books based on day of week
    val book = when (dayIndex % 3) {
        0 -> StudyBook.RED_BOOK
        1 -> StudyBook.BLUE_BOOK
        else -> StudyBook.GREEN_BOOK
    }

    // Create sample units for the day
    val units = listOf(
        StudyUnit(
            title = "Present Tense Structures",
            unitNumber = 10 + dayIndex,
            pages = "${25 + dayIndex * 3}-${28 + dayIndex * 3}",
            exercises = listOf("10.1", "10.2", "10.3"),
            isCompleted = dayIndex < 3
        ),
        StudyUnit(
            title = "Question Formation",
            unitNumber = 11 + dayIndex,
            pages = "${29 + dayIndex * 3}-${32 + dayIndex * 3}",
            exercises = listOf("11.1", "11.2"),
            isCompleted = dayIndex < 2
        )
    )

    // Create sample tasks based on the day's existing tasks
    val tasks = dayPlan.tasks.take(3).mapIndexed { index, task ->
        DailyTask(
            title = task.desc,
            description = "Complete the assigned ${task.desc.lowercase()} exercises",
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
        weekTitle = "Week ${(dayIndex / 7) + 1} - ${book.description}",
        dayName = dayPlan.day,
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
