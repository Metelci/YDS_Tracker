package com.mtlc.studyplan

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.WorkspacePremium
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.mtlc.studyplan.notification.NotificationHelper
import com.mtlc.studyplan.ui.theme.StudyPlanTheme
import com.mtlc.studyplan.worker.ReminderWorker
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.concurrent.TimeUnit

// --- VERİ MODELLERİ VE KAYNAĞI ---
data class Task(val id: String, val desc: String, val details: String? = null)
data class DayPlan(val day: String, val tasks: List<Task>)
data class WeekPlan(val week: Int, val month: Int, val title: String, val days: List<DayPlan>)
data class Achievement(val id: String, val title: String, val description: String, val condition: (Set<String>) -> Boolean)
data class ExamInfo(val name: String, val applicationStart: LocalDate, val applicationEnd: LocalDate, val examDate: LocalDate)

data class PlanUiState(
    val isLoading: Boolean = true,
    val userProgress: UserProgress = UserProgress()
)

object PlanDataSource {
    private fun createPreparationWeek(week: Int, level: String, book: String, units: String, practice: String): WeekPlan {
        val month = ((week - 1) / 4) + 1
        val weekId = "w${week}"
        return WeekPlan(week, month, "$month. Ay, $week. Hafta: $level Seviyesi", listOf(
            DayPlan("Pazartesi", listOf(
                Task("$weekId-t1", "Gramer: Ünite $units ($book)", "Kaynak: $book. Konuları dikkatlice oku."),
                Task("$weekId-t2", "Kelime Çalışması", "Seviyene uygun kelime listesinden (örneğin A2/B1 Oxford 3000) 20 yeni kelime öğren. Quizlet veya Anki'ye ekle.")
            )),
            DayPlan("Salı", listOf(
                Task("$weekId-t3", "Okuma Pratiği", "Öneri: Oxford Bookworms (Stage 2-3) veya newsinlevels.com gibi kaynaklardan en az 2 metin oku."),
                Task("$weekId-t4", "Gramer Alıştırmaları", "Pazartesi çalıştığın konuların kitaptaki alıştırmalarını tamamla.")
            )),
            DayPlan("Çarşamba", listOf(
                Task("$weekId-t5", "Soru Tipi: $practice", "Kaynak: Çıkmış YDS/YÖKDİL soruları veya güvenilir yayınevlerinin soru bankaları. En az 20 soru çöz."),
                Task("$weekId-t6", "Kelime Tekrarı", "Bu hafta öğrendiğin 20 kelimeyi tekrar et.")
            )),
            DayPlan("Perşembe", listOf(
                Task("$weekId-t7", "Dinleme Pratiği", "Öneri: BBC Learning English - 6 Minute English veya TED-Ed videolarını İngilizce altyazı ile izle."),
                Task("$weekId-t8", "Kelime Çalışması", "20 yeni kelime daha öğren.")
            )),
            DayPlan("Cuma", listOf(
                Task("$weekId-t9", "Soru Tipi: $practice", "Aynı soru tipinden 20 soru daha çözerek pekiştir."),
                Task("$weekId-t10", "Kelime Tekrarı", "Dün öğrendiğin 20 kelimeyi tekrar et.")
            )),
            DayPlan("Cumartesi", listOf(
                Task("$weekId-t11", "Haftalık Mini Deneme", "40 soruluk bir mini deneme çöz (kelime, gramer, okuma ağırlıklı)."),
                Task("$weekId-t12", "Serbest Çalışma", "İlgini çeken bir konuda İngilizce bir blog oku veya YouTube videosu izle.")
            )),
            DayPlan("Pazar", listOf(
                Task("$weekId-t13", "Haftalık Analiz ve Tekrar", "Mini deneme ve hafta içi yanlışlarını analiz et."),
                Task("$weekId-t14", "Keyif İçin Dinleme/İzleme", "Haftanın ödülü olarak İngilizce bir dizi/film izle. Anlamaya değil, keyif almaya odaklan.")
            ))
        ))
    }

    private fun createExamCampWeek(week: Int): WeekPlan {
        val month = ((week - 1) / 4) + 1
        val weekId = "w${week}"
        return WeekPlan(week, month, "$month. Ay, $week. Hafta: Sınav Kampı", listOf(
            DayPlan("Pazartesi", listOf(Task("$weekId-t1", "Tam Deneme Sınavı", "Kaynak: Çıkmış YDS/YÖKDİL sınavı. 80 soruyu 180 dakika içinde çöz."), Task("$weekId-t2", "Deneme Analizi", "Yanlışlarını, boşlarını ve doğru yapsan bile emin olmadıklarını analiz et. Bilmediğin kelimeleri listele."))),
            DayPlan("Salı", listOf(Task("$weekId-t3", "Tam Deneme Sınavı", "Kaynak: Çıkmış YDS/YÖKDİL sınavı. 80 soruyu 180 dakika içinde çöz."), Task("$weekId-t4", "Deneme Analizi", "Yanlışlarını, boşlarını ve doğru yapsan bile emin olmadıklarını analiz et, kelimelerini çıkar."))),
            DayPlan("Çarşamba", listOf(Task("$weekId-t5", "Tam Deneme Sınavı", "Kaynak: Çıkmış YDS/YÖKDİL sınavı. 80 soruyu 180 dakika içinde çöz."), Task("$weekId-t6", "Deneme Analizi", "Yanlışlarını, boşlarını ve doğru yapsan bile emin olmadıklarını analiz et, kelimelerini çıkar."))),
            DayPlan("Perşembe", listOf(Task("$weekId-t7", "Tam Deneme Sınavı", "Kaynak: Çıkmış YDS/YÖKDİL sınavı. 80 soruyu 180 dakika içinde çöz."), Task("$weekId-t8", "Deneme Analizi", "Yanlışlarını, boşlarını ve doğru yapsan bile emin olmadıklarını analiz et, kelimelerini çıkar."))),
            DayPlan("Cuma", listOf(Task("$weekId-t9", "Tam Deneme Sınavı", "Kaynak: Çıkmış YDS/YÖKDİL sınavı. 80 soruyu 180 dakika içinde çöz."), Task("$weekId-t10", "Deneme Analizi", "Yanlışlarını, boşlarını ve doğru yapsan bile emin olmadıklarını analiz et, kelimelerini çıkar."))),
            DayPlan("Cumartesi", listOf(Task("$weekId-t11", "Tam Deneme Sınavı", "Kaynak: Çıkmış YDS/YÖKDİL sınavı. 80 soruyu 180 dakika içinde çöz."), Task("$weekId-t12", "Haftalık Kelime Tekrarı", "Bu hafta denemelerde çıkan bilmediğin tüm kelimeleri tekrar et."))),
            DayPlan("Pazar", listOf(Task("$weekId-t13", "Genel Tekrar ve Dinlenme", "Haftanın denemelerindeki genel hatalarını gözden geçir."), Task("$weekId-t14", "Strateji ve Motivasyon", "Gelecek haftanın stratejisini belirle ve zihnini dinlendir.")))
        ))
    }

    val planData: List<WeekPlan> = mutableListOf<WeekPlan>().apply {
        addAll(List(10) { i -> createPreparationWeek(i + 1, "A2-B1 Temel", "Kırmızı Kitap", "${i * 5 + 1}-${i * 5 + 5}", "Cümle Tamamlama") })
        addAll(List(10) { i -> createPreparationWeek(i + 11, "B1-B2 Gelişim", "Mavi Kitap", "${i * 5 + 1}-${i * 5 + 5}", "Paragraf Tamamlama") })
        addAll(List(6) { i -> createPreparationWeek(i + 21, "B2-C1 İleri", "Yeşil Kitap", "${i * 6 + 1}-${i * 6 + 6}", "Anlamı Bozan Cümle") })
        addAll(List(4) { i -> createExamCampWeek(i + 27) })
    }
}

object AchievementDataSource {
    private val prepPhaseTasks = PlanDataSource.planData.take(26).flatMap { it.days }.flatMap { it.tasks }.map { it.id }.toSet()

    val allAchievements = listOf(
        Achievement("first_task", "İlk Adım", "İlk görevini tamamladın!") { it.isNotEmpty() },
        Achievement("hundred_tasks", "Yola Çıktın", "100 görevi tamamladın!") { it.size >= 100 },
        Achievement("prep_complete", "Hazırlık Dönemi Bitti!", "6 aylık hazırlık dönemini tamamladın. Şimdi sıra denemelerde!") { it.containsAll(prepPhaseTasks) },
        Achievement("first_exam_week", "Sınav Kampı Başladı!", "Son ay deneme kampına başladın!") { it.any { id -> id.startsWith("w27") } },
        Achievement("ten_exams", "10 Deneme Bitti!", "Toplam 10 tam deneme sınavı çözdün!") {
            val examTasks = it.filter { id -> id.contains("Deneme Sınavı") }.distinctBy { id -> id.split("#").getOrNull(1) }
            examTasks.size >= 10
        }
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
    private val _uiState = MutableStateFlow(PlanUiState())
    val uiState: StateFlow<PlanUiState> = _uiState.asStateFlow()

    private val _newlyUnlockedAchievement = MutableSharedFlow<Achievement>()
    val newlyUnlockedAchievement: SharedFlow<Achievement> = _newlyUnlockedAchievement.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.progressFlow.collect { progress ->
                _uiState.value = PlanUiState(isLoading = false, userProgress = progress)
            }
        }
    }

    fun toggleTask(taskId: String) {
        viewModelScope.launch {
            val currentProgress = _uiState.value.userProgress
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
            StudyPlanTheme {
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
    val uiState by viewModel.uiState.collectAsState()
    val userProgress = uiState.userProgress
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            MainHeader()

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    stickyHeader {
                        Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                            GamificationHeader(
                                streakCount = userProgress.streakCount,
                                achievementsCount = userProgress.unlockedAchievements.size,
                                completedTasksCount = userProgress.completedTasks.size,
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
}

// GÜNCELLENDİ: Sabit Başlık Composable'ı (Gradient Arka Plan)
@Composable
fun MainHeader() {
    val context = LocalContext.current
    val headerGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFFA8E6CF), Color(0xFFFF8A80)) // Pastel Yeşil -> Pastel Kırmızı
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerGradient)
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Road to YDS",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.Black.copy(alpha = 0.8f) // Gradient üzerinde okunabilirlik için koyu renk
            )
            IconButton(onClick = {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = "mailto:".toUri()
                    putExtra(Intent.EXTRA_EMAIL, arrayOf("metelci@gmail.com"))
                    putExtra(Intent.EXTRA_SUBJECT, "Road to YDS Uygulaması Geri Bildirimi")
                }
                context.startActivity(Intent.createChooser(intent, "E-posta gönder..."))
            }) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = "E-posta ile İletişim",
                    tint = Color.Black.copy(alpha = 0.7f) // Gradient üzerinde okunabilirlik için koyu renk
                )
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
fun GamificationHeader(
    streakCount: Int,
    achievementsCount: Int,
    completedTasksCount: Int,
    onAchievementsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        InfoChip(icon = Icons.Default.LocalFireDepartment, label = "Çalışma Serisi", value = "$streakCount gün", iconColor = MaterialTheme.colorScheme.error)
        Box(modifier = Modifier.clickable { onAchievementsClick() }) {
            InfoChip(icon = Icons.Default.WorkspacePremium, label = "Başarımlar", value = "$achievementsCount", iconColor = MaterialTheme.colorScheme.tertiary)
        }
        InfoChip(icon = Icons.Default.CheckCircle, label = "Tamamlanan", value = "$completedTasksCount ders", iconColor = MaterialTheme.colorScheme.primary)
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

    val monthColors = remember {
        listOf(
            Color(0xFF009688), // 1. Ay: Canlı Yeşil
            Color(0xFF3F51B5), // 2. Ay: Indigo
            Color(0xFF9C27B0), // 3. Ay: Mor
            Color(0xFFFFC107), // 4. Ay: Kehribar (Amber)
            Color(0xFF673AB7), // 5. Ay: Derin Mor
            Color(0xFFFF5722), // 6. Ay: Derin Turuncu
            Color(0xFF795548), // 7. Ay: Kahverengi
            Color(0xFFE91E63)  // 8. Ay: Canlı Pembe
        )
    }
    val titleColor = monthColors[(weekPlan.month - 1) % monthColors.size]

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).clickable { isExpanded = !isExpanded },
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