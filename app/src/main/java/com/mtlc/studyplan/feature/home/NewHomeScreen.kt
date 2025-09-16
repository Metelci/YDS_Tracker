@file:OptIn(ExperimentalMaterial3Api::class)
package com.mtlc.studyplan.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtlc.studyplan.data.*
import com.mtlc.studyplan.data.Task as DataTask
import com.mtlc.studyplan.data.dataStore
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import com.mtlc.studyplan.ui.theme.DesignTokens
import java.util.*

@Composable
fun NewHomeScreen() {
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

    // Calculate today's progress
    val absIndex = remember(settings.startEpochDay, today) {
        val start = LocalDate.ofEpochDay(settings.startEpochDay)
        val diff = ChronoUnit.DAYS.between(start, today).toInt()
        diff.coerceAtLeast(0)
    }

    val todayTasks = remember(plan, absIndex) {
        var idx = absIndex
        for (week in plan) {
            if (idx < week.days.size) {
                return@remember week.days[idx].tasks
            } else idx -= week.days.size
        }
        emptyList<DataTask>()
    }

    val todayProgress = remember(todayTasks, progress.completedTasks) {
        if (todayTasks.isEmpty()) 0f
        else todayTasks.count { it.id in progress.completedTasks }.toFloat() / todayTasks.size
    }

    val todayPoints: Int = remember(todayTasks, progress.completedTasks) {
        todayTasks.filter { it.id in progress.completedTasks }.size * 10 // 10 points per task
    }

    val tasksCompleted: Int = remember(todayTasks, progress.completedTasks) {
        todayTasks.count { it.id in progress.completedTasks }
    }

    val coroutineScope = rememberCoroutineScope()

    // Calculate days to exam
    val nextExam = ExamCalendarDataSource.getNextExam()
    val daysToExam = nextExam?.let { ChronoUnit.DAYS.between(today, it.examDate) } ?: 274

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DesignTokens.Background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with greeting
        item {
            Column(
                modifier = Modifier.padding(top = 24.dp)
            ) {
                Text(
                    text = "Günaydın! 👋",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = DesignTokens.Foreground
                )
                Text(
                    text = "YDS sınavını kazanmaya hazır mısın?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DesignTokens.MutedForeground,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Main progress card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = DesignTokens.ExamBlue
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${(todayProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = DesignTokens.Foreground
                            )
                            Text(
                                text = "Bugün",
                                style = MaterialTheme.typography.bodyMedium,
                                color = DesignTokens.MutedForeground
                            )
                        }
                        Column(
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "-$daysToExam",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = DesignTokens.Foreground
                            )
                            Text(
                                text = "YDS'ye Gün",
                                style = MaterialTheme.typography.bodyMedium,
                                color = DesignTokens.MutedForeground
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LinearProgressIndicator(
                        progress = { todayProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = DesignTokens.Success,
                        trackColor = DesignTokens.Muted
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Sınav Hazırlığı",
                        style = MaterialTheme.typography.bodySmall,
                        color = DesignTokens.MutedForeground
                    )
                }
            }
        }

        // Stats row (Points and Tasks)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Points card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = DesignTokens.SuccessContainer
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$todayPoints",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = DesignTokens.Foreground
                        )
                        Text(
                            text = "Bugün Puan",
                            style = MaterialTheme.typography.bodySmall,
                            color = DesignTokens.MutedForeground
                        )
                    }
                }

                // Tasks done card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = DesignTokens.TertiaryContainer
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$tasksCompleted/${todayTasks.size}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = DesignTokens.Foreground
                        )
                        Text(
                            text = "Görev Tamamlandı",
                            style = MaterialTheme.typography.bodySmall,
                            color = DesignTokens.MutedForeground
                        )
                    }
                }
            }
        }

        // Streak card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = DesignTokens.TertiaryContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🔥",
                        fontSize = 24.sp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${progress.streakCount} gün seri",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = DesignTokens.Foreground
                        )
                        Text(
                            text = "Ateştesin! 🔥",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DesignTokens.MutedForeground
                        )
                    }
                    Surface(
                        color = DesignTokens.Tertiary,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Text(
                            text = "2x",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = DesignTokens.TertiaryContainerForeground
                        )
                    }
                }
            }
        }

        // YDS Exam 2024 card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = DesignTokens.PrimaryContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.EventNote,
                            contentDescription = null,
                            tint = DesignTokens.Primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "YDS Sınavı 2024",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = DesignTokens.Foreground
                        )
                    }

                    Text(
                        text = "Sınav günü!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DesignTokens.MutedForeground,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "İlerleme",
                            style = MaterialTheme.typography.bodySmall,
                            color = DesignTokens.MutedForeground
                        )
                        Text(
                            text = "100%",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = DesignTokens.Primary
                        )
                    }

                    LinearProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = DesignTokens.Primary,
                        trackColor = DesignTokens.Muted
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Schedule,
                            contentDescription = null,
                            tint = DesignTokens.MutedForeground,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "15 Aralık 2024",
                            style = MaterialTheme.typography.bodySmall,
                            color = DesignTokens.MutedForeground
                        )
                    }
                }
            }
        }

        // Smart Suggestion card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = DesignTokens.PrimaryContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = DesignTokens.Primary,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Psychology,
                                contentDescription = null,
                                tint = DesignTokens.PrimaryForeground,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Akıllı Öneri",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = DesignTokens.Foreground
                            )
                            Text(
                                text = "AI destekli öneriler",
                                style = MaterialTheme.typography.bodySmall,
                                color = DesignTokens.MutedForeground
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "İlerlemenize göre, bugün okuma anlama üzerine odaklanın. Dilbilgisi için %85 hazırsınız ancak okuma hızınızı %15 artırabilirsiniz.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DesignTokens.Foreground,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = { },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = DesignTokens.Primary
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = DesignTokens.Primary
                        )
                    ) {
                        Text(
                            text = "Önerilen Görevi Dene",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }

        // Today's Tasks section
        item {
            Text(
                text = "Bugünün Görevleri",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = DesignTokens.Foreground,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Task items
        items(todayTasks.take(3), key = { it.id }) { task ->
            TaskItemCard(
                task = task,
                isCompleted = progress.completedTasks.contains(task.id),
                onToggleComplete = { taskId ->
                    coroutineScope.launch {
                        val cur = progress.completedTasks.toMutableSet()
                        if (taskId in cur) cur.remove(taskId) else cur.add(taskId)
                        progressRepo.saveProgress(progress.copy(completedTasks = cur))
                    }
                }
            )
        }

        // Bottom spacing for navigation
        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun TaskItemCard(
    task: DataTask,
    isCompleted: Boolean,
    onToggleComplete: (String) -> Unit
) {
    val category = when {
        task.desc.contains("Grammar", ignoreCase = true) || task.desc.contains("Dilbilgisi", ignoreCase = true) -> "Dilbilgisi"
        task.desc.contains("Reading", ignoreCase = true) || task.desc.contains("Okuma", ignoreCase = true) -> "Okuma"
        task.desc.contains("Vocabulary", ignoreCase = true) || task.desc.contains("Kelime", ignoreCase = true) -> "Kelime"
        else -> "Genel"
    }

    val categoryColor = when (category) {
        "Dilbilgisi" -> DesignTokens.Primary
        "Okuma" -> DesignTokens.Secondary
        "Kelime" -> DesignTokens.PointsGreen
        else -> DesignTokens.MutedForeground
    }

    val backgroundColor = if (isCompleted) DesignTokens.SuccessContainer else DesignTokens.Card

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category color indicator
            Surface(
                color = categoryColor,
                shape = RoundedCornerShape(2.dp),
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
            ) {}

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = categoryColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = category,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = categoryColor,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        color = DesignTokens.MutedForeground.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "15dk",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = DesignTokens.MutedForeground
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = task.desc,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isCompleted) DesignTokens.MutedForeground else DesignTokens.Foreground
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "⭐ 50 XP",
                    style = MaterialTheme.typography.bodySmall,
                    color = DesignTokens.MutedForeground
                )
            }

            // Complete button or checkmark
            if (isCompleted) {
                Surface(
                    color = DesignTokens.Success,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Tamamlandı",
                        tint = DesignTokens.SuccessContainer,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            } else {
                OutlinedButton(
                    onClick = { onToggleComplete(task.id) },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = DesignTokens.Primary
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = DesignTokens.Primary
                    )
                ) {
                    Text(
                        text = "Başla",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}