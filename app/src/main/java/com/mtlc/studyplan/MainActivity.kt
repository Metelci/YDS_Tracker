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

//region VERİ MODELLERİ
data class Task(val id: String, val desc: String, val details: String? = null)
data class DayPlan(val day: String, val tasks: List<Task>)
data class WeekPlan(val week: Int, val month: Int, val title: String, val days: List<DayPlan>)
data class Achievement(val id: String, val title: String, val description: String, val condition: (UserProgress) -> Boolean)
data class ExamInfo(val name: String, val applicationStart: LocalDate, val applicationEnd: LocalDate, val examDate: LocalDate)
data class UserProgress(
    val completedTasks: Set<String> = emptySet(),
    val streakCount: Int = 0,
    val lastCompletionDate: Long = 0L,
    val unlockedAchievements: Set<String> = emptySet(),
)
//endregion

//region VERİ KAYNAKLARI
object PlanDataSource {

    // Kırmızı Kitap'ın 8 haftalık "Sağlam Temel" programı için özel fonksiyon
    private fun createRedBookFoundationWeek(
        week: Int,
        monUnits: String, tueUnits: String, thuUnits: String, friUnits: String
    ): WeekPlan {
        val month = ((week - 1) / 4) + 1
        val weekId = "w${week}"
        val book = "Kırmızı Kitap - Essential Grammar in Use"

        return WeekPlan(week, month, "$month. Ay, $week. Hafta: Sağlam Temel", listOf(
            DayPlan("Pazartesi", listOf(
                Task("$weekId-pzt1", "1. Ders: Gramer Konuları", "$book, Üniteler: $monUnits"),
                Task("$weekId-pzt2", "2. Ders: Hızlı Pratik", "Çalıştığın konuların kitaptaki alıştırmalarını çözerek pekiştir.")
            )),
            DayPlan("Salı", listOf(
                Task("$weekId-sal1", "1. Ders: Gramer Konuları", "$book, Üniteler: $tueUnits"),
                Task("$weekId-sal2", "2. Ders: Hızlı Pratik", "Çalıştığın konuların kitaptaki alıştırmalarını çözerek pekiştir.")
            )),
            DayPlan("Çarşamba", listOf(
                Task("$weekId-car1", "1. Ders: Okuma ve Kelime", "Newsinlevels.com (Level 1-2) sitesinden en az 3 haber oku ve 15 yeni kelime çıkar."),
                Task("$weekId-car2", "2. Ders: Dinleme ve Tekrar", "ESL/British Council podcastlerinden bir bölüm dinle ve öğrendiğin kelimeleri tekrar et.")
            )),
            DayPlan("Perşembe", listOf(
                Task("$weekId-per1", "1. Ders: Gramer Konuları", "$book, Üniteler: $thuUnits"),
                Task("$weekId-per2", "2. Ders: Hızlı Pratik", "Çalıştığın konuların kitaptaki alıştırmalarını çözerek pekiştir.")
            )),
            DayPlan("Cuma", listOf(
                Task("$weekId-cum1", "1. Ders: Gramer Konuları", "$book, Üniteler: $friUnits"),
                Task("$weekId-cum2", "2. Ders: Hızlı Pratik", "Çalıştığın konuların kitaptaki alıştırmalarını çözerek pekiştir.")
            )),
            DayPlan("Cumartesi", listOf(
                Task("$weekId-cmt1", "1. Ders: Haftalık Kelime Tekrarı", "Bu hafta öğrendiğin tüm yeni kelimeleri (yaklaşık 40-50 kelime) flashcard uygulamasıyla tekrar et."),
                Task("$weekId-cmt2", "2. Ders: Keyif için İngilizce (Dizi/Film)", "İngilizce altyazılı bir dizi bölümü veya film izle. Anlamaya değil, kulağını doldurmaya odaklan.")
            )),
            DayPlan("Pazar", listOf(
                Task("$weekId-paz1", "1. Ders: Haftalık Genel Tekrar", "Bu hafta işlenen tüm gramer konularını hızlıca gözden geçir. Anlamadığın yerleri not al."),
                Task("$weekId-paz2", "2. Ders: Serbest Okuma/Dinleme", "İlgini çeken bir konuda İngilizce bir YouTube kanalı izle veya blog oku.")
            ))
        ))
    }

    // Mavi ve Yeşil Kitaplar için kullanılan standart fonksiyon
    private fun createAdvancedPreparationWeek(
        week: Int,
        level: String,
        book: String,
        grammarTopics: String,
        nextGrammarTopic: String,
        readingFocus: String,
        listeningFocus: String,
        questionType: String
    ): WeekPlan {
        val month = ((week - 1) / 4) + 1
        val weekId = "w${week}"

        val shouldStartAdvancedPractice = week >= 13

        val miniExamDayPlan = DayPlan(
            day = "",
            tasks = listOf(
                Task("$weekId-mini_exam", "1. Ders: Mini Deneme Sınavı", "40-50 soruluk bir deneme çöz (Süre: 60-75 dk). Okuma, gramer ve kelime ağırlıklı olmasına dikkat et."),
                Task("$weekId-mini_analysis", "2. Ders: Deneme Analizi ve Kelime Çalışması", "Tüm yanlışlarını ve boşlarını detaylıca analiz et. Bilmediğin kelimeleri not al ve kelime setine ekle.")
            )
        )

        val regularDays = mutableListOf<DayPlan>()

        regularDays.add(DayPlan("Pazartesi", listOf(
            Task("$weekId-t1", "1. Ders: Gramer Konusu", "Kaynak: $book. Haftanın konusu olan '$grammarTopics' üzerine detaylıca çalış."),
            Task("$weekId-t2", "2. Ders: Okuma Pratiği ve Kelime", "Kaynak: $readingFocus. Okuma yaparken en az 10 yeni kelime belirle ve anlamlarıyla birlikte not al.")
        )))
        regularDays.add(DayPlan("Salı", listOf(
            Task("$weekId-t3", "1. Ders: Gramer Alıştırmaları", "$book kitabından dünkü konunun alıştırmalarını eksiksiz tamamla."),
            Task("$weekId-t4", "2. Ders: Dinleme Pratiği ve Tekrar", "Kaynak: $listeningFocus. Aktif dinleme yap ve dünkü kelimeleri tekrar et.")
        )))
        if (shouldStartAdvancedPractice) {
            regularDays.add(miniExamDayPlan.copy(day = "Çarşamba"))
        } else {
            regularDays.add(DayPlan("Çarşamba", listOf(
                Task("$weekId-t5", "1. Ders: Gramer Pekiştirme", "Öğrendiğin gramer yapılarını kullanarak çeviri veya cümle kurma alıştırmaları yap."),
                Task("$weekId-t6", "2. Ders: Serbest Okuma", "İlgini çeken bir konuda İngilizce blog/makale oku.")
            )))
        }
        regularDays.add(DayPlan("Perşembe", listOf(
            Task("$weekId-t7", "1. Ders: Gramer Konusu (Devam)", "Haftanın gramer konusunu pekiştir ve ek alıştırmalar çöz."),
            Task("$weekId-t8", "2. Ders: Zorlu Okuma ve Kelime", "Kaynak: $readingFocus (Zor seviye). Yeni 10 kelime daha öğren.")
        )))
        if (shouldStartAdvancedPractice) {
            regularDays.add(DayPlan("Cuma", listOf(
                Task("$weekId-t9", "1. Ders: Soru Tipi Pratiği", "$questionType soru tipinden en az 20 soru çöz."),
                Task("$weekId-t10", "2. Ders: Soru Analizi ve Tekrar", "Çözdüğün sorulardaki yanlışlarını analiz et ve haftalık kelimeleri tekrar et.")
            )))
        } else {
            regularDays.add(DayPlan("Cuma", listOf(
                Task("$weekId-t9", "1. Ders: Haftalık Gramer Tekrarı", "Bu hafta işlenen tüm gramer konularını ve kurallarını tekrar et."),
                Task("$weekId-t10", "2. Ders: Serbest Dinleme", "İlgini çeken bir konuda İngilizce podcast/video izle.")
            )))
        }
        regularDays.add(DayPlan("Cumartesi", listOf(
            Task("$weekId-t11", "1. Ders: Gelecek Haftaya Hazırlık", "Gelecek haftanın konusu olan '$nextGrammarTopic' konusuna kısaca göz atarak ön hazırlık yap."),
            Task("$weekId-t12", "2. Ders: Haftalık Kelime Tekrarı", "Bu hafta öğrendiğin tüm kelimeleri (yaklaşık 20-30 kelime) flashcard uygulamasıyla tekrar et.")
        )))
        if (shouldStartAdvancedPractice) {
            regularDays.add(miniExamDayPlan.copy(day = "Pazar"))
        } else {
            regularDays.add(DayPlan("Pazar", listOf(
                Task("$weekId-t13", "1. Ders: Haftalık Analiz ve Planlama", "Haftanın genel bir değerlendirmesini yap. Güçlü ve zayıf yönlerini belirle."),
                Task("$weekId-t14", "2. Ders: Keyif için İngilizce", "İngilizce bir film/dizi izle veya oyun oyna. Amaç sadece dilin keyfini çıkarmak.")
            )))
        }

        return WeekPlan(week, month, "$month. Ay, $week. Hafta: $level Seviyesi", regularDays)
    }

    private fun createExamCampWeek(week: Int): WeekPlan {
        val month = ((week - 1) / 4) + 1
        val weekId = "w${week}"
        return WeekPlan(week, month, "$month. Ay, $week. Hafta: Sınav Kampı", listOf(
            DayPlan("Pazartesi", listOf(Task("$weekId-exam-1", "Tam Deneme Sınavı", "Kaynak: Son yıllara ait çıkmış bir YDS/YÖKDİL sınavı. 80 soruyu tam 180 dakika içinde çöz."), Task("$weekId-analysis-1", "Deneme Analizi", "Sınav sonrası en az 1 saat ara ver. Ardından yanlışlarını, boşlarını ve doğru yapsan bile emin olmadıklarını detaylıca analiz et. Bilmediğin kelimeleri listele."))),
            DayPlan("Salı", listOf(Task("$weekId-exam-2", "Tam Deneme Sınavı", "Kaynak: Güvenilir bir yayınevinin deneme sınavı. 80 soruyu tam 180 dakika içinde çöz."), Task("$weekId-analysis-2", "Deneme Analizi", "Dünkü gibi detaylı analiz yap. Özellikle tekrar eden hata tiplerine odaklan."))),
            DayPlan("Çarşamba", listOf(Task("$weekId-exam-3", "Tam Deneme Sınavı", "Kaynak: Çıkmış bir sınav. 80 soruyu tam 180 dakika içinde çöz."), Task("$weekId-analysis-3", "Deneme Analizi", "Analizini yap ve bu denemede öğrendiğin yeni kelimeleri tekrar et."))),
            DayPlan("Perşembe", listOf(Task("$weekId-exam-4", "Tam Deneme Sınavı", "Kaynak: Çıkmış bir sınav. 80 soruyu tam 180 dakika içinde çöz."), Task("$weekId-analysis-4", "Deneme Analizi", "Analizini yap ve bu denemede öğrendiğin yeni kelimeleri tekrar et."))),
            DayPlan("Cuma", listOf(Task("$weekId-exam-5", "Tam Deneme Sınavı", "Kaynak: Çıkmış bir sınav. 80 soruyu tam 180 dakika içinde çöz."), Task("$weekId-analysis-5", "Deneme Analizi", "Analizini yap ve bu denemede öğrendiğin yeni kelimeleri tekrar et."))),
            DayPlan("Cumartesi", listOf(Task("$weekId-exam-6", "Tam Deneme Sınavı", "Kaynak: Çıkmış bir sınav. 80 soruyu tam 180 dakika içinde çöz."), Task("$weekId-t12", "Haftalık Kelime Tekrarı", "Bu hafta denemelerde çıkan bilmediğin tüm kelimeleri flashcard uygulaması üzerinden tekrar et."))),
            DayPlan("Pazar", listOf(Task("$weekId-t13", "Genel Tekrar ve Dinlenme", "Haftanın denemelerindeki genel hata tiplerini (örn: zaman yönetimi, belirli soru tipi) gözden geçir."), Task("$weekId-t14", "Strateji ve Motivasyon", "Gelecek haftanın stratejisini belirle ve zihnini dinlendir. Sınava az kaldı!")))
        ))
    }

    val planData: List<WeekPlan> = mutableListOf<WeekPlan>().apply {

        // --- 1. FAZ: KIRMIZI KİTAP "SAĞLAM TEMEL" PROGRAMI --- (8 Hafta)
        add(createRedBookFoundationWeek(1, "1-4", "5-8", "9-12", "13-16"))
        add(createRedBookFoundationWeek(2, "17-20", "21-24", "25-28", "29-32"))
        add(createRedBookFoundationWeek(3, "33-36", "37-40", "41-44", "45-48"))
        add(createRedBookFoundationWeek(4, "49-52", "53-56", "57-60", "61-64"))
        add(createRedBookFoundationWeek(5, "65-68", "69-72", "73-76", "77-80"))
        add(createRedBookFoundationWeek(6, "81-84", "85-88", "89-92", "93-96"))
        add(createRedBookFoundationWeek(7, "97-100", "101-104", "105-108", "109-112"))
        add(createRedBookFoundationWeek(8, "113-115", "Genel Tekrar 1-50", "Genel Tekrar 51-115", "Zayıf Konu Analizi"))


        // --- 2. FAZ: MAVİ KİTAP (B1-B2 GELİŞİMİ) --- (10 Hafta)
        val blueBook = "Mavi Kitap - English Grammar in Use"
        val blueBookTopics = listOf(
            "Tenses Review (Tüm Zamanların Karşılaştırması)", "Future in Detail (Continuous/Perfect)",
            "Modals 1 (Ability, Permission, Advice)", "Modals 2 (Deduction, Obligation, Regret)",
            "Conditionals & Wish (Tüm Tipler & İleri Düzey)", "Passive Voice (Tüm Zamanlar) & 'have something done'",
            "Reported Speech (Sorular, Komutlar, İleri Düzey)", "Noun Clauses & Relative Clauses",
            "Gerunds & Infinitives (İleri kalıplar)", "Conjunctions & Connectors"
        )
        blueBookTopics.forEachIndexed { index, topic ->
            val weekNumber = index + 9 // 8 hafta bitti, 9. haftadan başlıyoruz
            val nextTopic = if (index + 1 < blueBookTopics.size) blueBookTopics[index + 1] else "Yeşil Kitap - Advanced Tenses"
            add(createAdvancedPreparationWeek(weekNumber, "B1-B2 Gelişimi", blueBook, topic, nextTopic, "The Guardian, BBC News", "TED-Ed Videoları", "Cümle Tamamlama"))
        }

        // --- 3. FAZ: YEŞİL KİTAP (C1 USTALIĞI) --- (8 Hafta)
        val greenBook = "Yeşil Kitap - Advanced Grammar in Use"
        val greenBookTopics = listOf(
            "Advanced Tense Nuances & Narrative Tenses", "Inversion & Emphasis (Not only, Hardly...)",
            "Advanced Modals (Speculation, Hypothetical)", "Participle Clauses (-ing ve -ed clauses)",
            "Advanced Connectors & Discourse Markers", "Hypothetical Meaning & Subjunctives",
            "Adjectives & Adverbs (İleri Kullanımlar)", "Prepositions & Phrasal Verbs (İleri Düzey)"
        )
        greenBookTopics.forEachIndexed { index, topic ->
            val weekNumber = index + 19 // 8+10=18 hafta bitti, 19. haftadan başlıyoruz
            val nextTopic = if (index + 1 < greenBookTopics.size) greenBookTopics[index + 1] else "Genel Tekrar ve Sınav Kampı"
            add(createAdvancedPreparationWeek(weekNumber, "C1 Ustalığı", greenBook, topic, nextTopic, "National Geographic, Scientific American", "NPR, BBC Radio 4 Podcast'leri", "Paragraf Tamamlama & Anlam Bütünlüğünü Bozan Cümle"))
        }

        // --- 4. FAZ: SINAV KAMPI --- (4 Hafta)
        addAll(List(4) { i -> createExamCampWeek(i + 27) })
    }
}

object AchievementDataSource {
    // Hazırlık dönemi artık ilk 26 hafta
    private val prepPhaseTasks = PlanDataSource.planData.take(26).flatMap { it.days }.flatMap { it.tasks }.map { it.id }.toSet()

    val allAchievements = listOf(
        Achievement("first_task", "İlk Adım", "İlk görevini tamamladın!") { userProgress -> userProgress.completedTasks.isNotEmpty() },
        Achievement("hundred_tasks", "Yola Çıktın", "100 görevi tamamladın!") { userProgress -> userProgress.completedTasks.size >= 100 },
        Achievement("prep_complete", "Hazırlık Dönemi Bitti!", "6 aylık hazırlık dönemini tamamladın. Şimdi sıra denemelerde!") { userProgress ->
            userProgress.completedTasks.containsAll(prepPhaseTasks)
        },
        Achievement("first_exam_week", "Sınav Kampı Başladı!", "Son ay deneme kampına başladın!") { userProgress ->
            userProgress.completedTasks.any { taskId -> taskId.startsWith("w27") } // Kamp 27. haftada başlıyor
        },
        Achievement("ten_exams", "10 Deneme Bitti!", "Toplam 10 tam deneme sınavı çözdün!") { userProgress ->
            userProgress.completedTasks.count { it.contains("-exam-") } >= 10
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
//endregion

//region DATASTORE VE REPOSITORY
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
//endregion

//region BİLDİRİM VE ARKA PLAN İŞLERİ
object NotificationHelper {
    private const val CHANNEL_ID_REMINDER = "YDS_REMINDER_CHANNEL"
    private const val NOTIFICATION_ID_REMINDER = 1

    private const val CHANNEL_ID_APPLICATION = "YDS_APPLICATION_CHANNEL"

    // GÜNLÜK HATIRLATICI BİLDİRİMİ İÇİN DÜZELTME
    fun showStudyReminderNotification(context: Context) {
        // 1. Adım: Uygulamayı açacak olan Intent'i oluştur.
        // Bu, MainActivity'yi hedef alır.
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        // 2. Adım: Intent'i bir PendingIntent'e sar.
        // Bu, bildirimin başka bir uygulamadan (sistem arayüzü) sizin uygulamanızı güvenli bir şekilde başlatmasını sağlar.
        val pendingIntent: android.app.PendingIntent = android.app.PendingIntent.getActivity(
            context,
            0,
            intent,
            android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_REMINDER)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Çalışma Zamanı!")
            .setContentText("Bugünkü hedeflerini tamamlamayı unutma.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent) // <-- 3. Adım: PendingIntent'i bildirime ekle.

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID_REMINDER, builder.build())
    }

    // SINAV BAŞVURU BİLDİRİMİ İÇİN DE AYNI DÜZELTME
    fun showApplicationReminderNotification(context: Context, title: String, message: String, notificationId: Int) {
        // Aynı şekilde bu bildirim için de bir PendingIntent oluşturuyoruz.
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: android.app.PendingIntent = android.app.PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_APPLICATION)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent) // <-- Aynı şekilde buraya da ekliyoruz.

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
//endregion

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
//endregion

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