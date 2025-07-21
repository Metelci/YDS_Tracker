package com.mtlc.studyplan

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.mtlc.studyplan.ui.theme.StudyPlanTheme
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.concurrent.TimeUnit

// --- VERİ MODELLERİ ---
data class Task(val id: String, val desc: String, val details: String? = null, val grammarTopic: String? = null, val questionType: String? = null)
data class DayPlan(val day: String, val tasks: List<Task>)
data class WeekPlan(val week: Int, val month: Int, val title: String, val days: List<DayPlan>)
data class Achievement(val id: String, val title: String, val description: String, val condition: (UserProgress) -> Boolean) // Burayı değiştirdik!
data class ExamInfo(val name: String, val applicationStart: LocalDate, val applicationEnd: LocalDate, val examDate: LocalDate)
data class UserProgress(
    val completedTasks: Set<String> = emptySet(),
    val streakCount: Int = 0,
    val lastCompletionDate: Long = 0L,
    val unlockedAchievements: Set<String> = emptySet(),
)

// --- VERİ KAYNAKLARI ---
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
            DayPlan("Pazartesi", listOf(Task("$weekId-exam-1", "Tam Deneme Sınavı #1", "Kaynak: Çıkmış YDS/YÖKDİL sınavı. 80 soruyu 180 dakika içinde çöz."), Task("$weekId-analysis-1", "Deneme Analizi", "Yanlışlarını, boşlarını ve doğru yapsan bile emin olmadıklarını analiz et. Bilmediğin kelimeleri listele."))),
            DayPlan("Salı", listOf(Task("$weekId-exam-2", "Tam Deneme Sınavı #2", "Kaynak: Çıkmış YDS/YÖKDİL sınavı. 80 soruyu 180 dakika içinde çöz."), Task("$weekId-analysis-2", "Deneme Analizi", "Yanlışlarını, boşlarını ve doğru yapsan bile emin olmadıklarını analiz et, kelimelerini çıkar."))),
            DayPlan("Çarşamba", listOf(Task("$weekId-exam-3", "Tam Deneme Sınavı #3", "Kaynak: Çıkmış YDS/YÖKDİL sınavı. 80 soruyu 180 dakika içinde çöz."), Task("$weekId-analysis-3", "Deneme Analizi", "Yanlışlarını, boşlarını ve doğru yapsan bile emin olmadıklarını analiz et, kelimelerini çıkar."))),
            DayPlan("Perşembe", listOf(Task("$weekId-exam-4", "Tam Deneme Sınavı #4", "Kaynak: Çıkmış YDS/YÖKDİL sınavı. 80 soruyu 180 dakika içinde çöz."), Task("$weekId-analysis-4", "Deneme Analizi", "Yanlışlarını, boşlarını ve doğru yapsan bile emin olmadıklarını analiz et, kelimelerini çıkar."))),
            DayPlan("Cuma", listOf(Task("$weekId-exam-5", "Tam Deneme Sınavı #5", "Kaynak: Çıkmış YDS/YÖKDİL sınavı. 80 soruyu 180 dakika içinde çöz."), Task("$weekId-analysis-5", "Deneme Analizi", "Yanlışlarını, boşlarını ve doğru yapsan bile emin olmadıklarını analiz et, kelimelerini çıkar."))),
            DayPlan("Cumartesi", listOf(Task("$weekId-exam-6", "Tam Deneme Sınavı #6", "Kaynak: Çıkmış YDS/YÖKDİL sınavı. 80 soruyu 180 dakika içinde çöz."), Task("$weekId-t12", "Haftalık Kelime Tekrarı", "Bu hafta denemelerde çıkan bilmediğin tüm kelimeleri tekrar et."))),
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

// MainActivity.kt (AchievementDataSource object'i içinde)

object AchievementDataSource {
    // Mevcut hazırlık aşaması görevlerini doğru şekilde hesaplayın.
    // İlk 26 hafta (6 ay) hazırlık, sonraki 4 hafta (1 ay) deneme kampı ise:
    private val prepPhaseTasks = PlanDataSource.planData.take(26).flatMap { it.days }.flatMap { it.tasks }.map { it.id }.toSet()
    private val examCampTasks = PlanDataSource.planData.drop(26).flatMap { it.days }.flatMap { it.tasks }.map { it.id }.toSet()

    val allAchievements = listOf(
        // Mevcut başarımlar - Düzeltildi
        Achievement("first_task", "İlk Adım", "İlk görevini tamamladın!") { userProgress -> userProgress.completedTasks.isNotEmpty() },
        Achievement("hundred_tasks", "Yola Çıktın", "100 görevi tamamladın!") { userProgress -> userProgress.completedTasks.size >= 100 },
        Achievement("prep_complete", "Hazırlık Dönemi Bitti!", "6 aylık hazırlık dönemini tamamladın. Şimdi sıra denemelerde!") { userProgress -> userProgress.completedTasks.containsAll(prepPhaseTasks) },
        Achievement("first_exam_week", "Sınav Kampı Başladı!", "Son ay deneme kampına başladın!") { userProgress ->
            // Sınav kampı görevlerinden herhangi birini tamamladığında
            userProgress.completedTasks.any { taskId -> examCampTasks.contains(taskId) }
        },
        Achievement("ten_exams", "10 Deneme Bitti!", "Toplam 10 tam deneme sınavı çözdün!") { userProgress ->
            userProgress.completedTasks.count { it.contains("-exam-") } >= 10
        },

        // --- YENİ BAŞARIMLAR BAŞLANGICI ---

        // Çalışma Serisi Başarımları (Bunlar zaten doğruydu)
        Achievement("3_day_streak", "İstikrar Başlangıcı", "3 günlük çalışma serisine ulaştın!") { userProgress ->
            userProgress.streakCount >= 3
        },
        Achievement("7_day_streak", "Bir Hafta Tamam!", "7 günlük çalışma serisine ulaştın!") { userProgress ->
            userProgress.streakCount >= 7
        },
        Achievement("30_day_streak", "Bir Ay İstikrar", "30 günlük çalışma serisine ulaştın!") { userProgress ->
            userProgress.streakCount >= 30
        },

        // Görev Sayısı Başarımları - Düzeltildi
        Achievement("fifty_tasks", "Yarı Yoldasın", "50 görevi tamamladın!") { userProgress -> userProgress.completedTasks.size >= 50 },
        Achievement("two_hundred_tasks", "Çalışkan Arı", "200 görevi tamamladın!") { userProgress -> userProgress.completedTasks.size >= 200 },
        Achievement("all_tasks_completed", "Şampiyon!", "Tüm görevleri tamamladın!") { userProgress ->
            val allTaskIds = PlanDataSource.planData.flatMap { it.days }.flatMap { it.tasks }.map { it.id }.toSet()
            userProgress.completedTasks.containsAll(allTaskIds) && userProgress.completedTasks.size == allTaskIds.size
        },

        // Gramer Konusu Başarımları (Örnek: Belirli bir konudaki tüm görevleri bitirme) - Düzeltildi
        Achievement("master_tenses", "Zamanların Efendisi", "Tüm 'Tenses' gramer görevlerini tamamladın!") { userProgress ->
            val tensesTasks = PlanDataSource.planData.flatMap { it.days }
                .flatMap { it.tasks }
                .filter { it.grammarTopic == "Tenses" }
                .map { it.id }
                .toSet()
            tensesTasks.isNotEmpty() && userProgress.completedTasks.containsAll(tensesTasks)
        },
        Achievement("master_modals", "Modalların Üstadı", "Tüm 'Modals' gramer görevlerini tamamladın!") { userProgress ->
            val modalsTasks = PlanDataSource.planData.flatMap { it.days }
                .flatMap { it.tasks }
                .filter { it.grammarTopic == "Modals" }
                .map { it.id }
                .toSet()
            modalsTasks.isNotEmpty() && userProgress.completedTasks.containsAll(modalsTasks)
        },
        // ... Diğer gramer konuları için de benzer başarımlar eklenebilir.

        // Soru Tipi Başarımları (Örnek: Belirli bir soru tipinde yeterli sayıda soru çözme) - Düzeltildi
        Achievement("sentence_completion_pro", "Cümle Tamamlama Prosu", "Tüm 'Cümle Tamamlama' soru tipi görevlerini tamamladın!") { userProgress ->
            val sentenceCompletionTasks = PlanDataSource.planData.flatMap { it.days }
                .flatMap { it.tasks }
                .filter { it.questionType == "Cümle Tamamlama" }
                .map { it.id }
                .toSet()
            sentenceCompletionTasks.isNotEmpty() && userProgress.completedTasks.containsAll(sentenceCompletionTasks)
        },
        Achievement("paragraph_completion_pro", "Paragraf Tamamlama Prosu", "Tüm 'Paragraf Tamamlama' soru tipi görevlerini tamamladın!") { userProgress ->
            val paragraphCompletionTasks = PlanDataSource.planData.flatMap { it.days }
                .flatMap { it.tasks }
                .filter { it.questionType == "Paragraf Tamamlama" }
                .map { it.id }
                .toSet()
            paragraphCompletionTasks.isNotEmpty() && userProgress.completedTasks.containsAll(paragraphCompletionTasks)
        },
        // ... Diğer soru tipleri için de benzer başarımlar eklenebilir.

        // Sınav Takvimi Başarımları - Düzeltildi
        Achievement("first_exam_registered", "İlk Başvurunu Yaptın!", "Yaklaşan bir sınava başvuru tarihleri içinde kayıt oldun!") { userProgress ->
            userProgress.completedTasks.any { it.contains("applied_for_exam_") } // Farazi bir görev ID'si
        },
        Achievement("exam_day_ready", "Sınav Günü Hazır!", "Sınav gününe çok az kaldı, başarılar!") { userProgress ->
            val nextExam = ExamCalendarDataSource.getNextExam()
            if (nextExam != null) {
                val today = LocalDate.now()
                val daysToExam = ChronoUnit.DAYS.between(today, nextExam.examDate)
                daysToExam <= 7 && daysToExam >= 0 // Sınava 7 gün veya daha az kaldıysa
            } else {
                false
            }
        },

        // --- YENİ BAŞARIMLAR SONU ---
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

// --- DATASTORE VE REPOSITORY ---
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "yds_progress")

class ProgressRepository(private val dataStore: DataStore<Preferences>) {
    private object Keys {
        val COMPLETED_TASKS = stringSetPreferencesKey("completed_tasks")
        val STREAK_COUNT = intPreferencesKey("streak_count")
        val LAST_COMPLETION_DATE = longPreferencesKey("last_completion_date")
        val UNLOCKED_ACHIEVEMENTS = stringSetPreferencesKey("unlocked_achievements")
    }

    val userProgressFlow = dataStore.data.map { preferences ->
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

// --- BİLDİRİM VE ARKA PLAN İŞLERİ ---
object NotificationHelper {
    private const val CHANNEL_ID_REMINDER = "YDS_REMINDER_CHANNEL"
    private const val CHANNEL_NAME_REMINDER = "Günlük Hatırlatıcılar"
    private const val NOTIFICATION_ID_REMINDER = 1

    private const val CHANNEL_ID_APPLICATION = "YDS_APPLICATION_CHANNEL"
    private const val CHANNEL_NAME_APPLICATION = "Sınav Başvuru Tarihleri"

    fun createNotificationChannel(context: Context) {
        val reminderChannel = NotificationChannel(
            CHANNEL_ID_REMINDER,
            CHANNEL_NAME_REMINDER,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Günlük çalışma planı hatırlatıcıları."
        }

        val applicationChannel = NotificationChannel(
            CHANNEL_ID_APPLICATION,
            CHANNEL_NAME_APPLICATION,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Önemli sınav başvuru başlangıç ve bitiş tarihleri."
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(reminderChannel)
        notificationManager.createNotificationChannel(applicationChannel)
    }

    fun showStudyReminderNotification(context: Context) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID_REMINDER)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Çalışma Zamanı!")
            .setContentText("Bugünkü hedeflerini tamamlamayı unutma.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID_REMINDER, builder.build())
    }

    fun showApplicationReminderNotification(context: Context, title: String, message: String, notificationId: Int) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID_APPLICATION)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, builder.build())
    }
}

class ReminderWorker(
    context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val today = LocalDate.now()

        val examsWithEventsToday = ExamCalendarDataSource.upcomingExams.filter {
            it.applicationStart == today || it.applicationEnd == today
        }

        var wasSpecialNotificationSent = false
        if (examsWithEventsToday.isNotEmpty()) {
            examsWithEventsToday.forEach { exam ->
                val title = "Sınav Başvuru Hatırlatıcısı"
                val message = if (today == exam.applicationStart) {
                    "${exam.name} için başvurular bugün başladı!"
                } else {
                    "${exam.name} için bugün son başvuru günü!"
                }
                NotificationHelper.showApplicationReminderNotification(
                    applicationContext,
                    title,
                    message,
                    exam.name.hashCode()
                )
            }
            wasSpecialNotificationSent = true
        }

        if (!wasSpecialNotificationSent) {
            NotificationHelper.showStudyReminderNotification(applicationContext)
        }

        return Result.success()
    }
}

// --- ANA ACTIVITY VE EKRANLAR ---
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.createNotificationChannel(this)
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

fun scheduleDailyReminder(context: Context) {
    val hour = 17 // Sabit saat 17:00
    val minute = 0

    val now = Calendar.getInstance()
    val nextNotificationTime = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        if (before(now)) {
            add(Calendar.DAY_OF_MONTH, 1)
        }
    }
    val initialDelay = nextNotificationTime.timeInMillis - now.timeInMillis

    val reminderRequest = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
        .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
        .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork("YDS_DAILY_REMINDER", ExistingPeriodicWorkPolicy.REPLACE, reminderRequest)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PlanScreen() {
    val context = LocalContext.current
    val repository = remember { ProgressRepository(context.dataStore) }
    val userProgress by repository.userProgressFlow.collectAsState(initial = UserProgress())
    val coroutineScope = rememberCoroutineScope()

    val allTasks = remember { PlanDataSource.planData.flatMap { it.days }.flatMap { it.tasks } }
    val progress = if (allTasks.isNotEmpty()) userProgress.completedTasks.size.toFloat() / allTasks.size else 0f
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "Overall Progress Animation")
    val snackbarHostState = remember { SnackbarHostState() }
    var showAchievementsSheet by remember { mutableStateOf(false) }

    if (showAchievementsSheet) {
        AchievementsSheet(unlockedAchievementIds = userProgress.unlockedAchievements, onDismiss = { showAchievementsSheet = false })
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

            if (userProgress.completedTasks.isEmpty() && userProgress.streakCount == 0) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Hoş Geldiniz!", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(bottom = 16.dp))
                        Text("YDS'ye hazırlanmaya başlamak için ilk görevinizi işaretleyin.",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(horizontal = 32.dp),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Icon(Icons.Default.ExpandMore, contentDescription = "Aşağı kaydır", modifier = Modifier.size(48.dp))
                    }
                }
            } else {
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
                            onToggleTask = { taskId ->
                                coroutineScope.launch {
                                    val currentTasks = userProgress.completedTasks.toMutableSet()
                                    if (currentTasks.contains(taskId)) {
                                        currentTasks.remove(taskId)
                                    } else {
                                        currentTasks.add(taskId)
                                    }

                                    // Streak & Achievement logic
                                    val today = Calendar.getInstance()
                                    val lastCompletion = Calendar.getInstance().apply { timeInMillis = userProgress.lastCompletionDate }
                                    var newStreak = userProgress.streakCount
                                    if (userProgress.lastCompletionDate > 0) {
                                        val isSameDay = today.get(Calendar.DAY_OF_YEAR) == lastCompletion.get(Calendar.DAY_OF_YEAR) && today.get(Calendar.YEAR) == lastCompletion.get(Calendar.YEAR)
                                        if (!isSameDay) {
                                            lastCompletion.add(Calendar.DAY_OF_YEAR, 1)
                                            val isConsecutiveDay = today.get(Calendar.DAY_OF_YEAR) == lastCompletion.get(Calendar.DAY_OF_YEAR) && today.get(Calendar.YEAR) == lastCompletion.get(Calendar.YEAR)
                                            newStreak = if (isConsecutiveDay) newStreak + 1 else 1
                                        }
                                    } else if (currentTasks.isNotEmpty()) {
                                        newStreak = 1
                                    }

                                    val newUnlocked = AchievementDataSource.allAchievements.filter { achievement ->
                                        !userProgress.unlockedAchievements.contains(achievement.id) && achievement.condition(userProgress.copy(
                                            completedTasks = currentTasks, // Güncel görev listesi
                                            streakCount = newStreak,       // Güncel streak
                                            lastCompletionDate = if(currentTasks.size > userProgress.completedTasks.size) today.timeInMillis else userProgress.lastCompletionDate // Güncel tarih
                                        )) // Başarım koşuluna güncel UserProgress'i gönderiyoruz
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
                                    val allUnlockedIds = userProgress.unlockedAchievements + newUnlocked.map { it.id }

                                    repository.saveProgress(
                                        userProgress.copy(
                                            completedTasks = currentTasks,
                                            streakCount = newStreak,
                                            lastCompletionDate = if(currentTasks.size > userProgress.completedTasks.size) today.timeInMillis else userProgress.lastCompletionDate,
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

@Composable
fun MainHeader() {
    val context = LocalContext.current
    Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shadowElevation = 3.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Road to YDS", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)

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
            // YENİ: Başvuru veya Bilgi Linki
            Spacer(modifier = Modifier.height(16.dp))
            val context = LocalContext.current
            val osymLink = "https://ais.osym.gov.tr/" // ÖSYM'nin ilgili sayfası
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, osymLink.toUri())
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = daysToExam >= 0 // Sınav geçmemişse aktif olsun
            ) {
                Text("ÖSYM Sayfasına Git")
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
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
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
            Color(0xFF009688), Color(0xFF3F51B5), Color(0xFF9C27B0),
            Color(0xFFFFC107), Color(0xFF673AB7), Color(0xFFFF5722),
            Color(0xFF795548), Color(0xFFE91E63)
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
