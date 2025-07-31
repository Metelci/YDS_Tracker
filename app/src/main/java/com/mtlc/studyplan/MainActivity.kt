package com.mtlc.studyplan

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.core.net.toUri
import com.mtlc.studyplan.data.*
import com.mtlc.studyplan.workers.scheduleDailyReminder
import com.mtlc.studyplan.ui.theme.StudyPlanTheme
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Calendar

//region ANA ACTIVITY VE YARDIMCI FONKSİYONLAR
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StudyPlanTheme {
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                    onResult = { isGranted ->
                        if (isGranted) {
                            scheduleDailyReminder(this)
                        }
                    }
                )
                LaunchedEffect(key1 = true) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        scheduleDailyReminder(this@MainActivity)
                    }
                }
                PlanScreen()
            }
        }
    }
}

//region EKRAN COMPOSABLE'LARI
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PlanScreen() {
    val context = LocalContext.current
    val repository = remember { ProgressRepository(context.dataStore) }
    val userProgress by repository.userProgressFlow.collectAsState(initial = null)
    val coroutineScope = rememberCoroutineScope()

    val allTasks = remember { PlanDataSource.planData.flatMap { it.days }.flatMap { it.tasks } }
    val progress = if (allTasks.isNotEmpty() && userProgress != null) userProgress!!.completedTasks.size.toFloat() / allTasks.size else 0f
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "Overall Progress Animation")
    val snackbarHostState = remember { SnackbarHostState() }
    var showAchievementsSheet by remember { mutableStateOf(false) }

    val currentProgress = userProgress ?: UserProgress()

    if (showAchievementsSheet) {
        AchievementsSheet(unlockedAchievementIds = currentProgress.unlockedAchievements, onDismiss = { showAchievementsSheet = false })
    }

    Scaffold(
        topBar = { MainHeader() },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (userProgress == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    stickyHeader {
                        Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                            GamificationHeader(
                                streakCount = currentProgress.streakCount,
                                achievementsCount = currentProgress.unlockedAchievements.size,
                                onAchievementsClick = { showAchievementsSheet = true }
                            )
                            ExamCountdownCard()
                            OverallProgressCard(progress = animatedProgress)
                        }
                    }
                    items(PlanDataSource.planData, key = { it.week }) { weekPlan ->
                        WeekCard(
                            weekPlan = weekPlan,
                            completedTasks = currentProgress.completedTasks,
                            onToggleTask = { taskId ->
                                coroutineScope.launch {
                                    val currentTasks = currentProgress.completedTasks.toMutableSet()
                                    if (currentTasks.contains(taskId)) {
                                        currentTasks.remove(taskId)
                                    } else {
                                        currentTasks.add(taskId)
                                    }

                                    val today = Calendar.getInstance()
                                    val lastCompletion = Calendar.getInstance().apply { timeInMillis = currentProgress.lastCompletionDate }
                                    var newStreak = currentProgress.streakCount
                                    if (currentProgress.lastCompletionDate > 0) {
                                        val isSameDay = today.get(Calendar.DAY_OF_YEAR) == lastCompletion.get(Calendar.DAY_OF_YEAR) && today.get(Calendar.YEAR) == lastCompletion.get(Calendar.YEAR)
                                        if (!isSameDay) {
                                            lastCompletion.add(Calendar.DAY_OF_YEAR, 1)
                                            val isConsecutiveDay = today.get(Calendar.DAY_OF_YEAR) == lastCompletion.get(Calendar.DAY_OF_YEAR) && today.get(Calendar.YEAR) == lastCompletion.get(Calendar.YEAR)
                                            newStreak = if (isConsecutiveDay) newStreak + 1 else 1
                                        }
                                    } else if (currentTasks.isNotEmpty()) {
                                        newStreak = 1
                                    }

                                    val updatedProgressForCheck = currentProgress.copy(completedTasks = currentTasks, streakCount = newStreak)
                                    val newUnlocked = AchievementDataSource.allAchievements.filter { achievement ->
                                        !currentProgress.unlockedAchievements.contains(achievement.id) && achievement.condition(updatedProgressForCheck)
                                    }
                                    newUnlocked.forEach { achievement ->
                                        launch {
                                            snackbarHostState.showSnackbar(
                                                message = "Yeni Başarım: ${achievement.title}",
                                                actionLabel = "OK",
                                                withDismissAction = true,
                                                duration = SnackbarDuration.Short
                                            )
                                        }
                                    }
                                    val allUnlockedIds = currentProgress.unlockedAchievements + newUnlocked.map { it.id }

                                    repository.saveProgress(
                                        currentProgress.copy(
                                            completedTasks = currentTasks,
                                            streakCount = newStreak,
                                            lastCompletionDate = if(currentTasks.size > currentProgress.completedTasks.size) today.timeInMillis else currentProgress.lastCompletionDate,
                                            unlockedAchievements = allUnlockedIds
                                        )
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class) // Bu satırı ekleyin
@Composable
fun MainHeader() {
    val context = LocalContext.current

    // Surface ve Row yerine TopAppBar kullanıyoruz
    TopAppBar(
        title = {
            Text(
                text = "Road to YDS",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        actions = {
            IconButton(onClick = {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = "mailto:".toUri()
                    putExtra(Intent.EXTRA_EMAIL, arrayOf("metelci@gmail.com"))
                    putExtra(Intent.EXTRA_SUBJECT, "Road to YDS Uygulaması Geri Bildirimi")
                }
                context.startActivity(Intent.createChooser(intent, "E-posta gönder..."))
            }) {
                Icon(imageVector = Icons.Default.Email, contentDescription = "E-posta ile İletişim")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface // Arka plan rengi
        )
    )
}

@Composable
fun ExamCountdownCard() {
    val nextExam = remember { ExamCalendarDataSource.getNextExam() }
    if (nextExam == null) return

    val today = LocalDate.now()
    val daysToApplicationEnd = ChronoUnit.DAYS.between(today, nextExam.applicationEnd)
    val daysToExam = ChronoUnit.DAYS.between(today, nextExam.examDate)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = "Yaklaşan Sınav: ${nextExam.name}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                CountdownItem(
                    icon = Icons.Default.EditCalendar,
                    label = "Başvuru İçin Son",
                    days = daysToApplicationEnd + 1,
                    isExpired = daysToApplicationEnd < 0,
                    color = MaterialTheme.colorScheme.secondary
                )
                CountdownItem(
                    icon = Icons.Default.EventAvailable,
                    label = "Sınava Kalan Süre",
                    days = daysToExam,
                    isExpired = daysToExam < 0,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            val context = LocalContext.current
            val osymLink = "https://ais.osym.gov.tr/"
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, osymLink.toUri())
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = daysToExam >= 0
            ) {
                Text("ÖSYM Sayfasına Git")
            }
        }
    }
}

@Composable
fun CountdownItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, days: Long, isExpired: Boolean, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = icon, contentDescription = label, tint = color, modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.height(4.dp))
        if (!isExpired) {
            Text("$days gün", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.bodySmall)
        } else {
            Text("Süre Doldu", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsSheet(unlockedAchievementIds: Set<String>, onDismiss: () -> Unit) {
    val allAchievements = remember { AchievementDataSource.allAchievements }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)) {
            Text(text = "Başarımlar", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp)) {
                items(allAchievements) { achievement ->
                    val isUnlocked = unlockedAchievementIds.contains(achievement.id)
                    AchievementItem(achievement = achievement, isUnlocked = isUnlocked)
                }
            }
        }
    }
}

@Composable
fun AchievementItem(achievement: Achievement, isUnlocked: Boolean) {
    val contentAlpha = if (isUnlocked) 1f else 0.5f
    val iconColor = if (isUnlocked) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    Row(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = Icons.Default.WorkspacePremium, contentDescription = "Başarım İkonu", tint = iconColor, modifier = Modifier.size(40.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = achievement.title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha))
            Text(text = achievement.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha))
        }
    }
}

@Composable
fun GamificationHeader(streakCount: Int, achievementsCount: Int, onAchievementsClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        InfoChip(icon = Icons.Default.LocalFireDepartment, label = "Çalışma Serisi", value = "$streakCount gün", iconColor = MaterialTheme.colorScheme.error)
        Box(modifier = Modifier.clickable { onAchievementsClick() }) {
            InfoChip(icon = Icons.Default.WorkspacePremium, label = "Başarımlar", value = "$achievementsCount", iconColor = MaterialTheme.colorScheme.tertiary)
        }
    }
}

@Composable
fun InfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, iconColor: Color) {
    Card(shape = MaterialTheme.shapes.large, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = label, tint = iconColor)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(text = label, style = MaterialTheme.typography.bodySmall)
                Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun OverallProgressCard(progress: Float) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Genel İlerleme", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(progress = { progress }, modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(CircleShape))
        }
    }
}

@Composable
fun WeekCard(weekPlan: WeekPlan, completedTasks: Set<String>, onToggleTask: (String) -> Unit) {
    var isExpanded by remember { mutableStateOf(weekPlan.week == 1) }
    val weekTasks = remember { weekPlan.days.flatMap { it.tasks } }
    val completedInWeek = weekTasks.count { completedTasks.contains(it.id) }
    val rotationAngle by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, label = "Icon Rotation")

    val monthColors = remember {
        listOf(
            Color(0xFF009688), Color(0xFF3F51B5), Color(0xFF9C27B0),
            Color(0xFFFFC107), Color(0xFF673AB7), Color(0xFFFF5722),
            Color(0xFF795548), Color(0xFFE91E63), Color(0xFF4CAF50), Color(0xFF2196F3)
        )
    }
    val titleColor = monthColors[(weekPlan.month - 1) % monthColors.size]

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { isExpanded = !isExpanded },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(weekPlan.title, style = MaterialTheme.typography.titleMedium, color = titleColor, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("$completedInWeek / ${weekTasks.size} görev tamamlandı", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = "Genişlet/Daralt", modifier = Modifier.rotate(rotationAngle))
            }
            AnimatedVisibility(visible = isExpanded) {
                Column {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    weekPlan.days.forEach { dayPlan -> DaySection(dayPlan = dayPlan, completedTasks = completedTasks, onToggleTask = onToggleTask) }
                }
            }
        }
    }
}

@Composable
fun DaySection(dayPlan: DayPlan, completedTasks: Set<String>, onToggleTask: (String) -> Unit) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(dayPlan.day, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 8.dp))
        dayPlan.tasks.forEach { task -> TaskItem(task = task, isCompleted = completedTasks.contains(task.id), onToggleTask = onToggleTask) }
    }
}

@Composable
fun TaskItem(task: Task, isCompleted: Boolean, onToggleTask: (String) -> Unit) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isExpanded) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else Color.Transparent)
            .clickable { isExpanded = !isExpanded }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isCompleted,
                onCheckedChange = { onToggleTask(task.id) }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = task.desc,
                style = MaterialTheme.typography.bodyMedium.copy(
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else null,
                    color = if (isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                )
            )
        }
        AnimatedVisibility(visible = isExpanded && task.details != null) {
            Text(
                text = task.details ?: "",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 56.dp, end = 16.dp, bottom = 12.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
//endregion