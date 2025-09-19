package com.mtlc.studyplan.core

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtlc.studyplan.data.Task
import com.mtlc.studyplan.data.TaskPriority
import com.mtlc.studyplan.data.TaskStats
import com.mtlc.studyplan.integration.AppIntegrationManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkingHomeScreen(
    appIntegrationManager: AppIntegrationManager,
    onNavigateToTasks: () -> Unit = {},
    onNavigateToProgress: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    // Collect data from AppIntegrationManager
    val allTasks by appIntegrationManager.getAllTasks().collectAsState(initial = emptyList())
    val studyStreak by appIntegrationManager.studyStreak.collectAsState()
    val achievements by appIntegrationManager.achievements.collectAsState()

    // Calculate stats
    var taskStats by remember { mutableStateOf(TaskStats()) }

    LaunchedEffect(allTasks) {
        taskStats = appIntegrationManager.getTaskStats()
    }

    val todayTasks = remember(allTasks) {
        allTasks.filter { task ->
            val today = System.currentTimeMillis()
            val oneDayMs = 24 * 60 * 60 * 1000
            task.dueDate?.let { dueDate ->
                kotlin.math.abs(dueDate - today) < oneDayMs
            } ?: false
        }.take(3) // Show only first 3 for home screen
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Column(
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
            ) {
                Text(
                    text = "Good morning! 👋",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Ready to tackle your studies?",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Progress Overview Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = "${taskStats.getProgressPercentage()}%",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Today's Progress",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.clickable { onNavigateToProgress() }
                        ) {
                            Text(
                                text = studyStreak.currentStreak.toString(),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Day Streak",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    LinearProgressIndicator(
                        progress = { taskStats.completionRate },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "${taskStats.completedTasks} of ${taskStats.totalTasks} tasks completed",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Stats Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Completed Tasks Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToTasks() },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = taskStats.completedTasks.toString(),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "Completed",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }

                // Pending Tasks Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToTasks() },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = taskStats.pendingTasks.toString(),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Pending",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        // Streak Card
        if (studyStreak.currentStreak > 0) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToProgress() },
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFF9800).copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color(0xFFFF9800).copy(alpha = 0.2f),
                            shape = CircleShape,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(
                                    text = "🔥",
                                    fontSize = 20.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${studyStreak.currentStreak} day streak",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (studyStreak.isActiveToday()) "Keep it going! 🔥" else "Don't break the streak!",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (studyStreak.currentStreak >= 7) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text(
                                    text = "2x",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Today's Tasks Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today's Tasks",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                TextButton(
                    onClick = onNavigateToTasks
                ) {
                    Text("View All")
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Today's Task Items
        if (todayTasks.isNotEmpty()) {
            items(todayTasks) { task ->
                TaskItemCard(
                    task = task,
                    onToggleComplete = {
                        coroutineScope.launch {
                            if (task.isCompleted) {
                                // Mark as incomplete
                                appIntegrationManager.updateTask(task.copy(isCompleted = false, completedAt = null))
                            } else {
                                // Complete the task
                                appIntegrationManager.completeTask(task.id)
                            }
                        }
                    },
                    onTaskClick = { onNavigateToTasks() }
                )
            }
        } else {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.TaskAlt,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No tasks for today",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Great job! You're all caught up.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        // Bottom spacing
        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun TaskItemCard(
    task: Task,
    onToggleComplete: () -> Unit,
    onTaskClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val completionScale by animateFloatAsState(
        targetValue = if (task.isCompleted) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "completion_animation"
    )

    val priorityColor = when (task.priority) {
        TaskPriority.HIGH -> Color(0xFFE53E3E)
        TaskPriority.MEDIUM -> Color(0xFFFF9800)
        TaskPriority.LOW -> Color(0xFF4CAF50)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onTaskClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            else
                MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (task.isCompleted) 1.dp else 4.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Completion checkbox
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggleComplete() },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.outline
                )
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Task content
            Column(modifier = Modifier.weight(1f)) {
                // Category and priority
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = priorityColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = task.category,
                            fontSize = 10.sp,
                            color = priorityColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = task.priority.displayName,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Task title
                Text(
                    text = task.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (task.isCompleted)
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else
                        MaterialTheme.colorScheme.onSurface
                )

                if (task.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = task.description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                if (task.estimatedTime != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "⏱️ ${task.estimatedTime} min",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Priority indicator
            if (!task.isCompleted) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            color = priorityColor,
                            shape = CircleShape
                        )
                )
            } else {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Completed",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}