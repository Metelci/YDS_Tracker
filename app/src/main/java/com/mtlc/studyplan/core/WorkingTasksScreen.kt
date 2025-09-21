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
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

    val thisWeek = plan.firstOrNull()
    val weeklyIds = remember(thisWeek) { thisWeek?.days?.flatMap { it.tasks }?.map { it.id }?.toSet() ?: emptySet() }
    val weeklyCompleted = 0
    val weeklyTotal = remember(weeklyIds) { weeklyIds.size.coerceAtLeast(1) }
    val weeklyProgressPct = remember(weeklyCompleted, weeklyTotal) { (weeklyCompleted.toFloat() / weeklyTotal) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DesignTokens.Background)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        HeaderWithXp(xp = 0)
        Spacer(Modifier.height(8.dp))
        SegmentedControl(
            segments = listOf("Daily", "Weekly", "Plan", "Custom"),
            selectedIndex = selectedTab,
            onSelect = { selectedTab = it }
        )
        Spacer(Modifier.height(8.dp))
        when (selectedTab) {
            2 -> PlanTab(thisWeek, weeklyProgressPct)
            else -> PlaceholderTab()
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
                    .height(36.dp)
                    .clip(RoundedCornerShape(20.dp)),
                color = DesignTokens.SegmentedPill,
                contentColor = DesignTokens.Foreground,
            ) {}

            // Hit targets and labels
            Row(Modifier.height(36.dp)) {
                segments.forEachIndexed { index, label ->
                    val selected = index == selectedIndex
                    val textColor by animateColorAsState(
                        targetValue = if (selected) DesignTokens.Foreground else DesignTokens.MutedForeground,
                        label = "seg_text_$index"
                    )
                    Box(
                        modifier = Modifier
                            .width(segWidth)
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
private fun HeaderWithXp(xp: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Tasks", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = DesignTokens.Foreground)
        Surface(color = DesignTokens.SecondaryContainer, contentColor = DesignTokens.SecondaryContainerForeground, shape = RoundedCornerShape(24.dp)) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = DesignTokens.Success, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("${xp} XP", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
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
private fun PlanTab(thisWeek: WeekPlan?, weeklyProgressPct: Float) {
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
                    DayScheduleList(thisWeek)
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
private fun DayScheduleList(week: WeekPlan?) {
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
