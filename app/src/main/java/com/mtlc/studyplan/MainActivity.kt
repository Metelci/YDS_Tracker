package com.mtlc.studyplan

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.mtlc.studyplan.notification.NotificationHelper
import com.mtlc.studyplan.ui.theme.YDSYÖKDİLKotlinComposeGörevTakipUygulamasıTheme
import com.mtlc.studyplan.worker.ReminderWorker
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

// --- VERİ MODELLERİ VE KAYNAĞI ---
data class Task(val id: String, val desc: String)
data class DayPlan(val day: String, val tasks: List<Task>)
data class WeekPlan(val week: Int, val title: String, val days: List<DayPlan>)

object PlanDataSource {
    val planData = listOf(
        WeekPlan(
            week = 1, title = "1. Hafta: Temel Atma ve Hızlanma", days = listOf(
                DayPlan(day = "Pazartesi", tasks = listOf(Task(id = "w1_t1", desc = "Ders 1: Okuma: Hikaye kitabına başla"), Task(id = "w1_t2", desc = "Ders 2: Kelime: Okumadan yeni kelimeler"))),
                DayPlan(day = "Salı", tasks = listOf(Task(id = "w1_t3", desc = "Ders 1: Dinleme: BBC 6 Minute English (Altyazılı)"), Task(id = "w1_t4", desc = "Ders 2: Kelime: Dinlemeden yeni kelimeler"))),
                DayPlan(day = "Çarşamba", tasks = listOf(Task(id = "w1_t5", desc = "Ders 1: Okuma: Hikayede ilerle"), Task(id = "w1_t6", desc = "Ders 2: Gramer: Unit 1 (am/is/are)"))),
                DayPlan(day = "Perşembe", tasks = listOf(Task(id = "w1_t7", desc = "Ders 1: Dinleme: Dünkü bölümü tekrar dinle"), Task(id = "w1_t8", desc = "Ders 2: Gramer: Unit 2 (Sorular)"))),
                DayPlan(day = "Cuma", tasks = listOf(Task(id = "w1_t9", desc = "Ders 1: Gramer: Unit 3 (Present Continuous)"), Task(id = "w1_t10", desc = "Ders 2: Gramer: Unit 4 (Present Continuous Sorular)")))
            )
        ),
        WeekPlan(
            week = 2, title = "2. Hafta: Zamanları Karşılaştırma", days = listOf(
                DayPlan(day = "Pazartesi", tasks = listOf(Task(id = "w2_t1", desc = "Ders 1: Okuma: Hikayede ilerle"), Task(id = "w2_t2", desc = "Ders 2: Kelime: Yeni kelimeler ve tekrar"))),
                DayPlan(day = "Salı", tasks = listOf(Task(id = "w2_t3", desc = "Ders 1: Dinleme: Yeni bir BBC bölümü"), Task(id = "w2_t4", desc = "Ders 2: Kelime: Yeni kelimeler ve tekrar"))),
                DayPlan(day = "Çarşamba", tasks = listOf(Task(id = "w2_t5", desc = "Ders 1: Okuma: Hikayede ilerle"), Task(id = "w2_t6", desc = "Ders 2: Gramer: Unit 5 (Simple Present)"))),
                DayPlan(day = "Perşembe", tasks = listOf(Task(id = "w2_t7", desc = "Ders 1: Dinleme: Dünkü bölümü tekrar dinle"), Task(id = "w2_t8", desc = "Ders 2: Gramer: Unit 6 (Simple Present Negatif)"))),
                DayPlan(day = "Cuma", tasks = listOf(Task(id = "w2_t9", desc = "Ders 1: Gramer: Unit 7 (Simple Present Sorular)"), Task(id = "w2_t10", desc = "Ders 2: Gramer: Unit 8 (Present Simple vs Continuous)")))
            )
        ),
        WeekPlan(
            week = 3, title = "3. Hafta: Geçmişe Yolculuk", days = listOf(
                DayPlan(day = "Pazartesi", tasks = listOf(Task(id = "w3_t1", desc = "Ders 1: Okuma: Hikayeyi bitirmeye odaklan"), Task(id = "w3_t2", desc = "Ders 2: Kelime: Genel kelime tekrarı"))),
                DayPlan(day = "Salı", tasks = listOf(Task(id = "w3_t3", desc = "Ders 1: Dinleme: Yeni bir BBC bölümü"), Task(id = "w3_t4", desc = "Ders 2: Kelime: Genel kelime tekrarı"))),
                DayPlan(day = "Çarşamba", tasks = listOf(Task(id = "w3_t5", desc = "Ders 1: Okuma: Yeni hikaye kitabına başla"), Task(id = "w3_t6", desc = "Ders 2: Gramer: Unit 9 (was/were)"))),
                DayPlan(day = "Perşembe", tasks = listOf(Task(id = "w3_t7", desc = "Ders 1: Dinleme: Dünkü bölümü tekrar dinle"), Task(id = "w3_t8", desc = "Ders 2: Gramer: Unit 10 (Simple Past - Düzenli Fiiller)"))),
                DayPlan(day = "Cuma", tasks = listOf(Task(id = "w3_t9", desc = "Ders 1: Gramer: Unit 11 (Simple Past - Düzensiz Fiiller)"), Task(id = "w3_t10", desc = "Ders 2: Gramer: Unit 12 (Simple Past Negatif/Soru)")))
            )
        ),
        WeekPlan(
            week = 4, title = "4. Hafta: Son Dokunuşlar ve Tekrar", days = listOf(
                DayPlan(day = "Pazartesi", tasks = listOf(Task(id = "w4_t1", desc = "Ders 1: Okuma: Yeni kitapta ilerle"), Task(id = "w4_t2", desc = "Ders 2: Kelime: Genel kelime tekrarı"))),
                DayPlan(day = "Salı", tasks = listOf(Task(id = "w4_t3", desc = "Ders 1: Dinleme: Eski bir bölümü dinleyip anlama seviyeni ölç"), Task(id = "w4_t4", desc = "Ders 2: Kelime: Genel kelime tekrarı"))),
                DayPlan(day = "Çarşamba", tasks = listOf(Task(id = "w4_t5", desc = "Ders 1: Okuma: Yeni kitapta ilerle"), Task(id = "w4_t6", desc = "Ders 2: Gramer: Unit 25 (there is/are)"))),
                DayPlan(day = "Perşembe", tasks = listOf(Task(id = "w4_t7", desc = "Ders 1: Dinleme: Yeni bir BBC bölümü"), Task(id = "w4_t8", desc = "Ders 2: Gramer: Unit 26 (some/any)"))),
                DayPlan(day = "Cuma", tasks = listOf(Task(id = "w4_t9", desc = "Ders 1: Gramer: Zorlanılan bir konunun tekrarı"), Task(id = "w4_t10", desc = "Ders 2: Gramer: Başka bir zor konunun tekrarı")))
            )
        )
    )
}

// --- VERİ KAYIT (DataStore) ---
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "yds_progress")

class ProgressRepository(private val dataStore: DataStore<Preferences>) {
    private val COMPLETED_TASKS_KEY = stringSetPreferencesKey("completed_tasks")

    val completedTasksFlow = dataStore.data.map { preferences ->
        preferences[COMPLETED_TASKS_KEY] ?: emptySet()
    }

    suspend fun updateCompletedTasks(completedIds: Set<String>) {
        dataStore.edit { preferences ->
            preferences[COMPLETED_TASKS_KEY] = completedIds
        }
    }
}

// --- VIEWMODEL ---
class PlanViewModel(private val repository: ProgressRepository) : ViewModel() {
    val completedTasks: StateFlow<Set<String>> = repository.completedTasksFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, emptySet())

    fun toggleTask(taskId: String) {
        viewModelScope.launch {
            val currentTasks = completedTasks.value.toMutableSet()
            if (currentTasks.contains(taskId)) {
                currentTasks.remove(taskId)
            } else {
                currentTasks.add(taskId)
            }
            repository.updateCompletedTasks(currentTasks)
        }
    }
}

// --- ANA ACTIVITY ---
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Uygulama açıldığında Bildirim Kanalını oluştur
        NotificationHelper.createNotificationChannel(this)

        setContent {
            YDSYÖKDİLKotlinComposeGörevTakipUygulamasıTheme {
                // Bildirim iznini istemek için bir launcher oluştur
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                    onResult = { isGranted ->
                        if (isGranted) {
                            // İzin verildiğinde görevi zamanla
                            scheduleDailyReminder(this)
                        }
                    }
                )

                // Uygulama ilk açıldığında izni iste
                LaunchedEffect(key1 = true) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        // Android 13 altı için izin gerekmez, direkt zamanla
                        scheduleDailyReminder(this@MainActivity)
                    }
                }

                val context = LocalContext.current
                val viewModel: PlanViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return PlanViewModel(ProgressRepository(context.dataStore)) as T
                        }
                    }
                )
                PlanScreen(viewModel)
            }
        }
    }
}

fun scheduleDailyReminder(context: Context) {
    // Günde bir kez çalışacak şekilde periyodik bir istek oluşturuyoruz.
    val reminderRequest = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
        .build()

    // Aynı işin tekrar tekrar kurulmasını önlemek için UNIQUE bir isimle zamanlıyoruz.
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "YDS_DAILY_REMINDER",
        ExistingPeriodicWorkPolicy.KEEP, // Eğer zaten kuruluysa, eskisini koru
        reminderRequest
    )
}

// --- COMPOSE EKRANLARI VE BİLEŞENLERİ ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanScreen(viewModel: PlanViewModel) {
    val completedTasks by viewModel.completedTasks.collectAsState()
    val allTasks = remember { PlanDataSource.planData.flatMap { it.days }.flatMap { it.tasks } }
    val progress = if (allTasks.isNotEmpty()) completedTasks.size.toFloat() / allTasks.size else 0f
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "Overall Progress Animation")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("YDS/YÖKDİL Planı", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            contentPadding = paddingValues,
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                OverallProgressCard(progress = animatedProgress)
            }
            items(PlanDataSource.planData, key = { it.week }) { weekPlan ->
                WeekCard(
                    weekPlan = weekPlan,
                    completedTasks = completedTasks,
                    onToggleTask = viewModel::toggleTask
                )
            }
        }
    }
}

@Composable
fun OverallProgressCard(progress: Float) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Genel İlerleme",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape),
            )
        }
    }
}

@Composable
fun WeekCard(
    weekPlan: WeekPlan,
    completedTasks: Set<String>,
    onToggleTask: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(weekPlan.week == 1) }
    val weekTasks = remember { weekPlan.days.flatMap { it.tasks } }
    val completedInWeek = weekTasks.count { completedTasks.contains(it.id) }
    val rotationAngle by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, label = "Icon Rotation")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { isExpanded = !isExpanded },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(weekPlan.title, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "$completedInWeek / ${weekTasks.size} görev tamamlandı",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Genişlet/Daralt",
                    modifier = Modifier.rotate(rotationAngle)
                )
            }
            AnimatedVisibility(visible = isExpanded) {
                Column {
                    Divider(modifier = Modifier.padding(vertical = 12.dp))
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
fun DaySection(
    dayPlan: DayPlan,
    completedTasks: Set<String>,
    onToggleTask: (String) -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(
            dayPlan.day,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        dayPlan.tasks.forEach { task ->
            TaskItem(
                task = task,
                isCompleted = completedTasks.contains(task.id),
                onToggle = onToggleTask
            )
        }
    }
}

@Composable
fun TaskItem(task: Task, isCompleted: Boolean, onToggle: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onToggle(task.id) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isCompleted,
            onCheckedChange = { onToggle(task.id) }
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
}
