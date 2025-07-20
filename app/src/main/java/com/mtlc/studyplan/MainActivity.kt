package com.mtlc.studyplan

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
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
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.mtlc.studyplan.notification.NotificationHelper
import com.mtlc.studyplan.ui.theme.YDSYOKDILKotlinComposeGorevTakipUygulamasiTheme
import com.mtlc.studyplan.worker.ReminderWorker
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.TimeUnit
import androidx.core.net.toUri

// --- VERİ MODELLERİ VE KAYNAĞI ---
// YENİ: Task modeline 'details' alanı eklendi
data class Task(val id: String, val desc: String, val details: String? = null)
data class DayPlan(val day: String, val tasks: List<Task>)
data class WeekPlan(val week: Int, val title: String, val days: List<DayPlan>)
data class Achievement(val id: String, val title: String, val description: String, val condition: (Set<String>) -> Boolean)
data class ExamInfo(val name: String, val applicationStart: LocalDate, val applicationEnd: LocalDate, val examDate: LocalDate)

// YENİ: 8 AYLIK (32 HAFTALIK) KAPSAMLI PLAN VE DETAYLI GÖREV AÇIKLAMALARI
object PlanDataSource {
    private fun generateWeeks(startWeek: Int, month: Int, units: List<Int>): List<WeekPlan> {
        val weeks = mutableListOf<WeekPlan>()
        val unitChunks = units.chunked(4)
        unitChunks.forEachIndexed { index, unitChunk ->
            val weekNumber = startWeek + index
            weeks.add(
                WeekPlan(
                    week = weekNumber,
                    title = "$month. Ay, $weekNumber. Hafta",
                    days = listOf(
                        DayPlan(day = "Pazartesi", tasks = listOf(
                            Task(id = "w${weekNumber}_t1", desc = "Okuma Pratiği", details = "Öneri: Oxford Bookworms (Stage 1-2) veya Penguin Readers (Level 1-2) serisinden bir kitap okumaya devam et. Anlamadığın yerleri not al."),
                            Task(id = "w${weekNumber}_t2", desc = "Kelime Çalışması", details = "Öneri: Okuduğun bölümden en az 10 yeni kelime çıkar. Anlamları ve örnek cümleleriyle birlikte Quizlet veya Anki'ye ekle.")
                        )),
                        DayPlan(day = "Salı", tasks = listOf(
                            Task(id = "w${weekNumber}_t3", desc = "Dinleme Pratiği", details = "Öneri: BBC 6 Minute English, VOA Learning English gibi kaynaklardan seviyene uygun bir bölümü önce altyazısız, sonra altyazılı dinle."),
                            Task(id = "w${weekNumber}_t4", desc = "Kelime Çalışması", details = "Öneri: Dinlediğin bölümden en az 5 yeni kelime veya kalıp öğren. Kelime uygulamana eklemeyi unutma.")
                        )),
                        DayPlan(day = "Çarşamba", tasks = listOf(
                            Task(id = "w${weekNumber}_t5", desc = "Gramer: Unit ${unitChunk[0]} ve ${unitChunk[1]}", details = "Kaynak: Raymond Murphy - Essential Grammar in Use (Kırmızı Kitap). Konuları dikkatlice oku."),
                            Task(id = "w${weekNumber}_t6", desc = "Alıştırma", details = "Çalıştığın iki gramer ünitesinin kitaptaki alıştırmalarını tamamla. Yanlışlarını kontrol et.")
                        )),
                        DayPlan(day = "Perşembe", tasks = listOf(
                            Task(id = "w${weekNumber}_t7", desc = "Tekrar: Okuma & Dinleme", details = "Hafta başında okuduğun ve dinlediğin materyalleri tekrar gözden geçir. Kelimelerin ne kadar aklında kaldığını kontrol et."),
                            Task(id = "w${weekNumber}_t8", desc = "Tekrar: Kelime", details = "Bu hafta öğrendiğin tüm kelimeleri kelime uygulaman üzerinden tekrar et. Flashcard'larla kendini test et.")
                        )),
                        DayPlan(day = "Cuma", tasks = listOf(
                            Task(id = "w${weekNumber}_t9", desc = "Gramer: Unit ${unitChunk.getOrNull(2)} ve ${unitChunk.getOrNull(3)}", details = "Kaynak: Raymond Murphy - Essential Grammar in Use (Kırmızı Kitap). Konuları dikkatlice oku."),
                            Task(id = "w${weekNumber}_t10", desc = "Alıştırma ve Tekrar", details = "Çalıştığın iki gramer ünitesinin alıştırmalarını yap ve hafta boyunca işlenen tüm gramer konularını hızlıca gözden geçir.")
                        )))
                )
            )
        }
        return weeks
    }

    val planData = listOf(
        *generateWeeks(1, 1, (1..16).toList()).toTypedArray(),
        *generateWeeks(5, 2, (17..32).toList()).toTypedArray(),
        *generateWeeks(9, 3, (33..48).toList()).toTypedArray(),
        *generateWeeks(13, 4, (49..64).toList()).toTypedArray(),
        *generateWeeks(17, 5, (65..80).toList()).toTypedArray(),
        *generateWeeks(21, 6, (81..96).toList()).toTypedArray(),
        *generateWeeks(25, 7, (97..112).toList()).toTypedArray(),
        *generateWeeks(29, 8, (113..128).toList()).toTypedArray()
    )
}

object AchievementDataSource {
    private val allTasks = PlanDataSource.planData.flatMap { it.days }.flatMap { it.tasks }
    private val month1Tasks = PlanDataSource.planData.take(4).flatMap { it.days }.flatMap { it.tasks }.map { it.id }.toSet()

    val allAchievements = listOf(
        Achievement("first_task", "İlk Adım", "İlk görevini tamamladın!") { it.isNotEmpty() },
        Achievement("ten_tasks", "Isınma Turları", "10 görevi tamamladın!") { it.size >= 10 },
        Achievement("fifty_tasks", "Yarı Maraton", "50 görevi tamamladın!") { it.size >= 50 },
        Achievement("month1_complete", "İlk Ay Bitti!", "İlk ayın tüm görevlerini tamamladın!") { it.containsAll(month1Tasks) },
        Achievement("halfway_there", "Yolun Yarısı!", "Tüm planın yarısını tamamladın!") { it.size >= (allTasks.size / 2) }
    )
}

object ExamCalendarDataSource {
    val upcomingExams = listOf(
        ExamInfo(name = "YÖKDİL/1 (İlkbahar)", applicationStart = LocalDate.of(2026, 1, 28), applicationEnd = LocalDate.of(2026, 2, 5), examDate = LocalDate.of(2026, 3, 22)),
        ExamInfo(name = "YDS/1 (İlkbahar)", applicationStart = LocalDate.of(2026, 2, 18), applicationEnd = LocalDate.of(2026, 2, 26), examDate = LocalDate.of(2026, 4, 12)),
        ExamInfo(name = "YÖKDİL/2 (Sonbahar)", applicationStart = LocalDate.of(2026, 7, 15), applicationEnd = LocalDate.of(2026, 7, 23), examDate = LocalDate.of(2026, 8, 23)),
        ExamInfo(name = "YDS/2 (Sonbahar)", applicationStart = LocalDate.of(2026, 8, 26), applicationEnd = LocalDate.of(2026, 9, 3), examDate = LocalDate.of(2026, 10, 25))
    )

    fun getNextExam(): ExamInfo? {
        val today = LocalDate.now()
        return upcomingExams.filter { it.examDate.isAfter(today) }.minByOrNull { it.examDate }
    }
}


private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "yds_progress")

class ProgressRepository(private val dataStore: DataStore<Preferences>) {
    private object Keys {
        val COMPLETED_TASKS = stringSetPreferencesKey("completed_tasks")
        val STREAK_COUNT = intPreferencesKey("streak_count")
        val LAST_COMPLETION_DATE = longPreferencesKey("last_completion_date")
        val UNLOCKED_ACHIEVEMENTS = stringSetPreferencesKey("unlocked_achievements")
    }

    val progressFlow = dataStore.data.map { preferences ->
        UserProgress(
            completedTasks = preferences[Keys.COMPLETED_TASKS] ?: emptySet(),
            streakCount = preferences[Keys.STREAK_COUNT] ?: 0,
            lastCompletionDate = preferences[Keys.LAST_COMPLETION_DATE] ?: 0L,
            unlockedAchievements = preferences[Keys.UNLOCKED_ACHIEVEMENTS] ?: emptySet()
        )
    }

    suspend fun saveProgress(progress: UserProgress) {
        dataStore.edit { preferences ->
            preferences[Keys.COMPLETED_TASKS] = progress.completedTasks
            preferences[Keys.STREAK_COUNT] = progress.streakCount
            preferences[Keys.LAST_COMPLETION_DATE] = progress.lastCompletionDate
            preferences[Keys.UNLOCKED_ACHIEVEMENTS] = progress.unlockedAchievements
        }
    }
}

data class UserProgress(
    val completedTasks: Set<String> = emptySet(),
    val streakCount: Int = 0,
    val lastCompletionDate: Long = 0L,
    val unlockedAchievements: Set<String> = emptySet()
)

class PlanViewModel(private val repository: ProgressRepository) : ViewModel() {
    private val _userProgress = MutableStateFlow(UserProgress())
    val userProgress: StateFlow<UserProgress> = _userProgress.asStateFlow()

    private val _newlyUnlockedAchievement = MutableSharedFlow<Achievement>()
    val newlyUnlockedAchievement: SharedFlow<Achievement> = _newlyUnlockedAchievement.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.progressFlow.collect {
                _userProgress.value = it
            }
        }
    }

    fun toggleTask(taskId: String) {
        viewModelScope.launch {
            val currentProgress = _userProgress.value
            val currentTasks = currentProgress.completedTasks.toMutableSet()
            val wasCompleted = currentTasks.contains(taskId)

            if (wasCompleted) {
                currentTasks.remove(taskId)
                repository.saveProgress(currentProgress.copy(completedTasks = currentTasks))
            } else {
                currentTasks.add(taskId)
                updateStreakAndAchievements(currentProgress, currentTasks)
            }
        }
    }

    private suspend fun updateStreakAndAchievements(currentProgress: UserProgress, newCompletedTasks: Set<String>) {
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
        } else {
            newStreak = 1
        }
        val newUnlocked = AchievementDataSource.allAchievements.filter { achievement ->
            !currentProgress.unlockedAchievements.contains(achievement.id) && achievement.condition(newCompletedTasks)
        }
        newUnlocked.forEach { _newlyUnlockedAchievement.emit(it) }
        val allUnlockedIds = currentProgress.unlockedAchievements + newUnlocked.map { it.id }
        repository.saveProgress(
            UserProgress(
                completedTasks = newCompletedTasks,
                streakCount = newStreak,
                lastCompletionDate = today.timeInMillis,
                unlockedAchievements = allUnlockedIds
            )
        )
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.createNotificationChannel(this)
        setContent {
            YDSYOKDILKotlinComposeGorevTakipUygulamasiTheme {
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                    onResult = { isGranted -> if (isGranted) { scheduleDailyReminder(this) } }
                )
                LaunchedEffect(key1 = true) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        scheduleDailyReminder(this@MainActivity)
                    }
                }
                val context = LocalContext.current
                val viewModel: PlanViewModel = viewModel(factory = PlanViewModelFactory(ProgressRepository(context.dataStore)))
                PlanScreen(viewModel)
            }
        }
    }
}

fun scheduleDailyReminder(context: Context) {
    val reminderRequest = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS).build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork("YDS_DAILY_REMINDER", ExistingPeriodicWorkPolicy.KEEP, reminderRequest)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PlanScreen(viewModel: PlanViewModel) {
    val userProgress by viewModel.userProgress.collectAsState()
    val allTasks = remember { PlanDataSource.planData.flatMap { it.days }.flatMap { it.tasks } }
    val progress = if (allTasks.isNotEmpty()) userProgress.completedTasks.size.toFloat() / allTasks.size else 0f
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "Overall Progress Animation")
    val snackbarHostState = remember { SnackbarHostState() }
    var showAchievementsSheet by remember { mutableStateOf(false) }

    if (showAchievementsSheet) {
        AchievementsSheet(unlockedAchievementIds = userProgress.unlockedAchievements, onDismiss = { showAchievementsSheet = false })
    }

    LaunchedEffect(Unit) {
        viewModel.newlyUnlockedAchievement.collect { achievement ->
            snackbarHostState.showSnackbar(
                message = "Yeni Başarım: ${achievement.title}",
                actionLabel = "OK",
                withDismissAction = true,
                duration = SnackbarDuration.Short
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            MainHeader()
            LazyColumn(modifier = Modifier.weight(1f)) {
                stickyHeader {
                    Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                        GamificationHeader(
                            streakCount = userProgress.streakCount,
                            achievementsCount = userProgress.unlockedAchievements.size,
                            onAchievementsClick = { showAchievementsSheet = true }
                        )
                        ExamCountdownCard()
                        OverallProgressCard(progress = animatedProgress)
                    }
                }
                items(PlanDataSource.planData, key = { it.week }) { weekPlan ->
                    WeekCard(
                        weekPlan = weekPlan,
                        completedTasks = userProgress.completedTasks,
                        onToggleTask = viewModel::toggleTask
                    )
                }
            }
        }
    }
}

@Composable
fun MainHeader() {
    val context = LocalContext.current
    Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shadowElevation = 3.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(contentAlignment = Alignment.CenterStart) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.FactCheck,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp).offset(x = (-16).dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                )
                Text(text = "Road to YDS", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
            IconButton(onClick = {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    "mailto:".toUri().also { data = it }
                    putExtra(Intent.EXTRA_EMAIL, arrayOf("metelci@gmail.com"))
                    putExtra(Intent.EXTRA_SUBJECT, "Road to YDS Uygulaması Geri Bildirimi")
                }
                context.startActivity(Intent.createChooser(intent, "E-posta gönder..."))
            }) {
                Icon(imageVector = Icons.Default.Email, contentDescription = "E-posta ile İletişim")
            }
        }
    }
}

@Composable
fun ExamCountdownCard() {
    val nextExam = remember { ExamCalendarDataSource.getNextExam() }
    if (nextExam == null) return

    val today = LocalDate.now()
    val daysToApplicationEnd = ChronoUnit.DAYS.between(today, nextExam.applicationEnd)
    val daysToExam = ChronoUnit.DAYS.between(today, nextExam.examDate)

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Yaklaşan Sınav: ${nextExam.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.EditCalendar, contentDescription = "Başvuru Tarihi", tint = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Başvuru İçin Son", style = MaterialTheme.typography.bodySmall)
                    if (daysToApplicationEnd >= 0) {
                        Text("${daysToApplicationEnd + 1} gün", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    } else {
                        Text("Süre Doldu", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.EventAvailable, contentDescription = "Sınav Tarihi", tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Sınava Kalan Süre", style = MaterialTheme.typography.bodySmall)
                    if (daysToExam >= 0) {
                        Text("$daysToExam gün", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    } else {
                        Text("Sınav Geçti", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsSheet(unlockedAchievementIds: Set<String>, onDismiss: () -> Unit) {
    val allAchievements = remember { AchievementDataSource.allAchievements }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
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
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
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
    Row(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp), horizontalArrangement = Arrangement.SpaceAround) {
        InfoChip(icon = Icons.Default.LocalFireDepartment, label = "Çalışma Serisi", value = "$streakCount gün", iconColor = MaterialTheme.colorScheme.error)
        Box(modifier = Modifier.clickable { onAchievementsClick() }) {
            InfoChip(icon = Icons.Default.WorkspacePremium, label = "Başarımlar", value = "$achievementsCount", iconColor = MaterialTheme.colorScheme.tertiary)
        }
    }
}

@Composable
fun InfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, iconColor: Color) {
    Card(shape = MaterialTheme.shapes.large, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
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
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Genel İlerleme", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape))
        }
    }
}

@Composable
fun WeekCard(weekPlan: WeekPlan, completedTasks: Set<String>, onToggleTask: (String) -> Unit) {
    var isExpanded by remember { mutableStateOf(weekPlan.week == 1) }
    val weekTasks = remember { weekPlan.days.flatMap { it.tasks } }
    val completedInWeek = weekTasks.count { completedTasks.contains(it.id) }
    val rotationAngle by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, label = "Icon Rotation")

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).clickable { isExpanded = !isExpanded },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(weekPlan.title, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("$completedInWeek / ${weekTasks.size} görev tamamlandı", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = "Genişlet/Daralt", modifier = Modifier.rotate(rotationAngle))
            }
            AnimatedVisibility(visible = isExpanded) {
                Column {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        thickness = DividerDefaults.Thickness,
                        color = DividerDefaults.color
                    )
                    weekPlan.days.forEach { dayPlan ->
                        DaySection(
                            dayPlan = dayPlan,
                            completedTasks = completedTasks,
                            onToggleTask = onToggleTask
                        )
                    }
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

// YENİ: TaskItem artık kendi içinde genişleyebilir
@Composable
fun TaskItem(task: Task, isCompleted: Boolean, onToggleTask: (String) -> Unit) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if(isExpanded) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else Color.Transparent)
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
