package com.mtlc.studyplan

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import com.mtlc.studyplan.ai.SmartScheduler
import com.mtlc.studyplan.ai.SmartSuggestion
import com.mtlc.studyplan.data.OnboardingRepository
import com.mtlc.studyplan.data.PlanOverridesStore
import com.mtlc.studyplan.data.PlanRepository
import com.mtlc.studyplan.data.ProgressRepository
import com.mtlc.studyplan.data.TaskLog
import com.mtlc.studyplan.data.UserPlanOverrides
import com.mtlc.studyplan.data.UserProgress
import com.mtlc.studyplan.data.dataStore
import com.mtlc.studyplan.navigation.AppNavHost
import com.mtlc.studyplan.ui.CustomizePlanScreen
import com.mtlc.studyplan.ui.EditTaskDialog
import com.mtlc.studyplan.ui.components.EstimatedTimeChip
import com.mtlc.studyplan.ui.components.PriorityIndicator
import com.mtlc.studyplan.ui.components.SmartSuggestionsCard
import com.mtlc.studyplan.ui.components.TaskCategoryChip
import com.mtlc.studyplan.ui.components.TaskPriority
import com.mtlc.studyplan.ui.components.TooltipManager
import com.mtlc.studyplan.ui.components.TooltipTrigger
import com.mtlc.studyplan.ui.components.UndoAction
import com.mtlc.studyplan.ui.components.UndoSnackbarEffect
import com.mtlc.studyplan.ui.components.rememberUndoManager
import com.mtlc.studyplan.utils.Constants
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Calendar

//region VERİ MODELLERİ
data class Task(val id: String, val desc: String, val details: String? = null)
data class DayPlan(val day: String, val tasks: List<Task>)
data class WeekPlan(val week: Int, val month: Int, val title: String, val days: List<DayPlan>)
data class Achievement(val id: String, val title: String, val description: String, val condition: (UserProgress) -> Boolean)
data class ExamInfo(val name: String, val applicationStart: LocalDate, val applicationEnd: LocalDate, val examDate: LocalDate)
// Use UserProgress from data module
//endregion

//region VERİ KAYNAKLARI
object PlanDataSource {
    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    internal fun getAppContext(): Context {
        return appContext ?: throw IllegalStateException("PlanDataSource must be initialized with context before use")
    }

    // Kırmızı Kitap'ın 8 haftalık "Sağlam Temel" programı için özel fonksiyon
    private fun createRedBookFoundationWeek(
        week: Int,
        monUnits: String, tueUnits: String, thuUnits: String, friUnits: String
    ): WeekPlan {
        val month = ((week - 1) / 4) + 1
        val weekId = "w${week}"
        val book = getAppContext().getString(R.string.red_book)

        return WeekPlan(week, month, getAppContext().getString(R.string.month_week_format, month, week, getAppContext().getString(R.string.foundation_level)), listOf(
            DayPlan(
                getAppContext().getString(R.string.monday), listOf(
                Task("$weekId-pzt1", getAppContext().getString(R.string.grammar_topics_lesson), "$book, ${getAppContext().getString(R.string.units)}: $monUnits"),
                Task("$weekId-pzt2", getAppContext().getString(R.string.quick_practice_lesson), getAppContext().getString(R.string.practice_description_1))
            )),
            DayPlan(
                getAppContext().getString(R.string.tuesday), listOf(
                Task("$weekId-sal1", getAppContext().getString(R.string.grammar_topics_lesson), "$book, ${getAppContext().getString(R.string.units)}: $tueUnits"),
                Task("$weekId-sal2", getAppContext().getString(R.string.quick_practice_lesson), getAppContext().getString(R.string.practice_description_1))
            )),
            DayPlan(
                getAppContext().getString(R.string.wednesday), listOf(
                Task("$weekId-car1", getAppContext().getString(R.string.reading_vocabulary_lesson), getAppContext().getString(R.string.reading_description_1)),
                Task("$weekId-car2", getAppContext().getString(R.string.listening_repeat_lesson), getAppContext().getString(R.string.listening_description_1))
            )),
            DayPlan(
                getAppContext().getString(R.string.thursday), listOf(
                Task("$weekId-per1", getAppContext().getString(R.string.grammar_topics_lesson), "$book, ${getAppContext().getString(R.string.units)}: $thuUnits"),
                Task("$weekId-per2", getAppContext().getString(R.string.quick_practice_lesson), getAppContext().getString(R.string.practice_description_1))
            )),
            DayPlan(
                getAppContext().getString(R.string.friday), listOf(
                Task("$weekId-cum1", getAppContext().getString(R.string.grammar_topics_lesson), "$book, ${getAppContext().getString(R.string.units)}: $friUnits"),
                Task("$weekId-cum2", getAppContext().getString(R.string.quick_practice_lesson), getAppContext().getString(R.string.practice_description_1))
            )),
            DayPlan(
                getAppContext().getString(R.string.saturday), listOf(
                Task("$weekId-cmt1", getAppContext().getString(R.string.weekly_vocabulary_lesson), getAppContext().getString(R.string.vocabulary_description_1)),
                Task("$weekId-cmt2", getAppContext().getString(R.string.entertainment_english_lesson), getAppContext().getString(R.string.entertainment_description_1))
            )),
            DayPlan(
                getAppContext().getString(R.string.sunday), listOf(
                Task("$weekId-paz1", getAppContext().getString(R.string.weekly_general_repeat), getAppContext().getString(R.string.general_repeat_description_1)),
                Task("$weekId-paz2", getAppContext().getString(R.string.free_reading_listening), getAppContext().getString(R.string.free_reading_description_1))
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
        questionType: String
    ): WeekPlan {
        val month = ((week - 1) / 4) + 1
        val weekId = "w${week}"

        val shouldStartAdvancedPractice = week >= 13

        val miniExamDayPlan = DayPlan(
            day = "",
            tasks = listOf(
Task("$weekId-mini_exam", getAppContext().getString(R.string.mini_exam_lesson), getAppContext().getString(R.string.mini_exam_description)),
Task("$weekId-mini_analysis", getAppContext().getString(R.string.mini_analysis_lesson), getAppContext().getString(R.string.mini_analysis_description)),
            )
        )

        val regularDays = mutableListOf<DayPlan>()

        regularDays.add(DayPlan(
            getAppContext().getString(R.string.monday), listOf(
            Task("$weekId-t1", getAppContext().getString(R.string.grammar_topic_lesson), getAppContext().getString(R.string.grammar_topic_description, book, grammarTopics)),
            Task("$weekId-t2", getAppContext().getString(R.string.quick_practice_lesson), getAppContext().getString(R.string.practice_description_1))
        )))
        regularDays.add(DayPlan(
            getAppContext().getString(R.string.tuesday), listOf(
            Task("$weekId-t3", getAppContext().getString(R.string.grammar_exercises_lesson), getAppContext().getString(R.string.grammar_exercises_description, book)),
            Task("$weekId-t4", getAppContext().getString(R.string.listening_repeat_lesson), getAppContext().getString(R.string.listening_description_1))
        )))
        if (shouldStartAdvancedPractice) {
            regularDays.add(miniExamDayPlan.copy(day = getAppContext().getString(R.string.wednesday)))
        } else {
            regularDays.add(DayPlan(
                getAppContext().getString(R.string.wednesday), listOf(
                Task("$weekId-t5", getAppContext().getString(R.string.grammar_reinforcement_lesson), getAppContext().getString(R.string.grammar_reinforcement_description)),
                Task("$weekId-t6", getAppContext().getString(R.string.free_reading_listening), getAppContext().getString(R.string.free_reading_description_1))
            )))
        }
        regularDays.add(DayPlan(
            getAppContext().getString(R.string.thursday), listOf(
            Task("$weekId-t7", getAppContext().getString(R.string.grammar_topic_continuation_lesson), getAppContext().getString(R.string.grammar_topic_continuation_description)),
            Task("$weekId-t8", getAppContext().getString(R.string.reading_vocabulary_lesson), getAppContext().getString(R.string.reading_description_1))
        )))
        if (shouldStartAdvancedPractice) {
            regularDays.add(DayPlan(
                getAppContext().getString(R.string.friday), listOf(
                Task("$weekId-t9", getAppContext().getString(R.string.question_type_practice_lesson), getAppContext().getString(R.string.question_type_practice_description, questionType)),
                Task("$weekId-t10", getAppContext().getString(R.string.exam_analysis), getAppContext().getString(R.string.exam_analysis_description)),
            )))
        } else {
            regularDays.add(DayPlan(
                getAppContext().getString(R.string.friday), listOf(
                Task("$weekId-t9", getAppContext().getString(R.string.weekly_grammar_review_lesson), getAppContext().getString(R.string.weekly_grammar_review_description)),
                Task("$weekId-t10", getAppContext().getString(R.string.free_reading_listening), getAppContext().getString(R.string.free_reading_description_1)),
            )))
        }
        regularDays.add(DayPlan(
            getAppContext().getString(R.string.saturday), listOf(
            Task("$weekId-t11", getAppContext().getString(R.string.next_week_preparation_lesson), getAppContext().getString(R.string.next_week_preparation_description, nextGrammarTopic)),
            Task("$weekId-t12", getAppContext().getString(R.string.weekly_vocabulary_lesson), getAppContext().getString(R.string.vocabulary_description_1))
        )))
        if (shouldStartAdvancedPractice) {
            regularDays.add(miniExamDayPlan.copy(day = getAppContext().getString(R.string.sunday)))
        } else {
            regularDays.add(DayPlan(
                getAppContext().getString(R.string.sunday), listOf(
                Task("$weekId-t13", getAppContext().getString(R.string.weekly_analysis_planning_lesson), getAppContext().getString(R.string.weekly_analysis_planning_description)),
                Task("$weekId-t14", getAppContext().getString(R.string.entertainment_english_lesson), getAppContext().getString(R.string.entertainment_description_1)),
            )))
        }

        return WeekPlan(week, month, getAppContext().getString(R.string.month_week_format, month, week, level), regularDays)
    }

    private fun createExamCampWeek(week: Int): WeekPlan {
        val month = ((week - 1) / 4) + 1
        val weekId = "w${week}"
        return WeekPlan(week, month, getAppContext().getString(R.string.exam_camp_week_title, month, week), listOf(
            DayPlan(getAppContext().getString(R.string.monday), listOf(Task("$weekId-exam-1", getAppContext().getString(R.string.full_exam), getAppContext().getString(R.string.full_exam_description)), Task("$weekId-analysis-1", getAppContext().getString(R.string.exam_analysis), getAppContext().getString(R.string.exam_analysis_description)))),
            DayPlan(getAppContext().getString(R.string.tuesday), listOf(Task("$weekId-exam-2", getAppContext().getString(R.string.full_exam), getAppContext().getString(R.string.full_exam_description_2)), Task("$weekId-analysis-2", getAppContext().getString(R.string.exam_analysis), getAppContext().getString(R.string.exam_analysis_description_2)))),
            DayPlan(getAppContext().getString(R.string.wednesday), listOf(Task("$weekId-exam-3", getAppContext().getString(R.string.full_exam), getAppContext().getString(R.string.full_exam_description_3)), Task("$weekId-analysis-3", getAppContext().getString(R.string.exam_analysis), getAppContext().getString(R.string.exam_analysis_description_3)))),
            DayPlan(getAppContext().getString(R.string.thursday), listOf(Task("$weekId-exam-4", getAppContext().getString(R.string.full_exam), getAppContext().getString(R.string.full_exam_description_3)), Task("$weekId-analysis-4", getAppContext().getString(R.string.exam_analysis), getAppContext().getString(R.string.exam_analysis_description_3)))),
            DayPlan(getAppContext().getString(R.string.friday), listOf(Task("$weekId-exam-5", getAppContext().getString(R.string.full_exam), getAppContext().getString(R.string.full_exam_description_3)), Task("$weekId-analysis-5", getAppContext().getString(R.string.exam_analysis), getAppContext().getString(R.string.exam_analysis_description_3)))),
            DayPlan(getAppContext().getString(R.string.saturday), listOf(Task("$weekId-exam-6", getAppContext().getString(R.string.full_exam), getAppContext().getString(R.string.full_exam_description_3)), Task("$weekId-t12", getAppContext().getString(R.string.weekly_vocabulary_lesson), getAppContext().getString(R.string.weekly_vocabulary_review_description)))),
            DayPlan(getAppContext().getString(R.string.sunday), listOf(Task("$weekId-t13", getAppContext().getString(R.string.general_repeat), getAppContext().getString(R.string.general_review_description)), Task("$weekId-t14", getAppContext().getString(R.string.strategy_motivation), getAppContext().getString(R.string.strategy_motivation_description))))
        ))
    }

    val planData: List<WeekPlan> by lazy {
        mutableListOf<WeekPlan>().apply {

        // --- 1. FAZ: KIRMIZI KİTAP "SAĞLAM TEMEL" PROGRAMI --- (8 Hafta)
        add(createRedBookFoundationWeek(1, "1-4", "5-8", "9-12", "13-16"))
        add(createRedBookFoundationWeek(2, "17-20", "21-24", "25-28", "29-32"))
        add(createRedBookFoundationWeek(3, "33-36", "37-40", "41-44", "45-48"))
        add(createRedBookFoundationWeek(4, "49-52", "53-56", "57-60", "61-64"))
        add(createRedBookFoundationWeek(5, "65-68", "69-72", "73-76", "77-80"))
        add(createRedBookFoundationWeek(6, "81-84", "85-88", "89-92", "93-96"))
        add(createRedBookFoundationWeek(7, "97-100", "101-104", "105-108", "109-112"))
        add(createRedBookFoundationWeek(8, "113-115", getAppContext().getString(R.string.general_review_1_50), getAppContext().getString(R.string.general_review_51_115), getAppContext().getString(R.string.weak_topic_analysis)))


        // --- 2. FAZ: MAVİ KİTAP (B1-B2 GELİŞİMİ) --- (10 Hafta)
        val blueBook = getAppContext().getString(R.string.blue_book_full)
        val blueBookTopics = listOf(
            getAppContext().getString(R.string.tenses_review_topic), getAppContext().getString(R.string.future_in_detail_topic),
            getAppContext().getString(R.string.modals_1_topic), getAppContext().getString(R.string.modals_2_topic),
            getAppContext().getString(R.string.conditionals_wish_topic), getAppContext().getString(R.string.passive_voice_topic),
            getAppContext().getString(R.string.reported_speech_topic), getAppContext().getString(R.string.noun_clauses_topic),
            getAppContext().getString(R.string.gerunds_infinitives_topic), getAppContext().getString(R.string.conjunctions_connectors_topic)
        )
        blueBookTopics.forEachIndexed { index, topic ->
            val weekNumber = index + 9 // 8 hafta bitti, 9. haftadan başlıyoruz
            add(createAdvancedPreparationWeek(weekNumber, "B1-B2 Gelişimi", blueBook, topic, getAppContext().getString(R.string.advanced_tenses),
                getAppContext().getString(R.string.sentence_completion)))
        }

        // --- 3. FAZ: YEŞİL KİTAP (C1 USTALIĞI) --- (8 Hafta)
        val greenBook = getAppContext().getString(R.string.green_book)
        val greenBookTopics = listOf(
            getAppContext().getString(R.string.advanced_tense_nuances_topic), getAppContext().getString(R.string.inversion_emphasis_topic),
            getAppContext().getString(R.string.advanced_modals_topic), getAppContext().getString(R.string.participle_clauses_topic),
            getAppContext().getString(R.string.advanced_connectors_topic), getAppContext().getString(R.string.hypothetical_meaning_topic),
            getAppContext().getString(R.string.adjectives_adverbs_topic), getAppContext().getString(R.string.prepositions_phrasal_verbs_topic)
        )
        greenBookTopics.forEachIndexed { index, topic ->
            val weekNumber = index + 19 // 8+10=18 hafta bitti, 19. haftadan başlıyoruz
            add(createAdvancedPreparationWeek(weekNumber, "C1 Ustalığı", greenBook, topic, getAppContext().getString(R.string.final_review_exam_camp),
                getAppContext().getString(R.string.paragraph_completion)))
        }

        // --- 4. FAZ: SINAV KAMPI --- (4 Hafta)
        addAll(List(4) { i -> createExamCampWeek(i + 27) })
        }.toList()
    }
}

object AchievementDataSource {
    // Hazırlık dönemi artık ilk 26 hafta
    private val prepPhaseTasks = PlanDataSource.planData.take(Constants.PREP_PHASE_END_WEEK).flatMap { it.days }.flatMap { it.tasks }.map { it.id }.toSet()

    val allAchievements = listOf(
        Achievement("first_task", PlanDataSource.getAppContext().getString(R.string.first_step_achievement_title), PlanDataSource.getAppContext().getString(R.string.first_step_achievement_desc)) { userProgress -> userProgress.completedTasks.isNotEmpty() },
        Achievement("hundred_tasks", PlanDataSource.getAppContext().getString(R.string.hundred_tasks_achievement_title), PlanDataSource.getAppContext().getString(R.string.hundred_tasks_achievement_desc)) { userProgress -> userProgress.completedTasks.size >= Constants.HUNDRED_TASKS_THRESHOLD },
        Achievement("prep_complete", PlanDataSource.getAppContext().getString(R.string.prep_complete_achievement_title), PlanDataSource.getAppContext().getString(R.string.prep_complete_achievement_desc)) { userProgress ->
            userProgress.completedTasks.containsAll(prepPhaseTasks)
        },
        Achievement("first_exam_week", PlanDataSource.getAppContext().getString(R.string.first_exam_week_achievement_title), PlanDataSource.getAppContext().getString(R.string.first_exam_week_achievement_desc)) { userProgress ->
            userProgress.completedTasks.any { taskId -> taskId.startsWith("w${Constants.EXAM_WEEK_START}") }
        },
        Achievement("ten_exams", PlanDataSource.getAppContext().getString(R.string.ten_exams_achievement_title), PlanDataSource.getAppContext().getString(R.string.ten_exams_achievement_desc)) { userProgress ->
            userProgress.completedTasks.count { it.contains("-exam-") } >= Constants.TEN_EXAMS_THRESHOLD
        }
    )
}

object ExamCalendarDataSource {
    val upcomingExams = listOf(
        ExamInfo(name = PlanDataSource.getAppContext().getString(R.string.yokdil_spring), applicationStart = LocalDate.of(2026, 1, 28), applicationEnd = LocalDate.of(2026, 2, 5), examDate = LocalDate.of(2026, 3, 22)),
        ExamInfo(name = PlanDataSource.getAppContext().getString(R.string.yds_spring), applicationStart = LocalDate.of(2026, 2, 18), applicationEnd = LocalDate.of(2026, 2, 26), examDate = LocalDate.of(2026, 4, 12)),
        ExamInfo(name = PlanDataSource.getAppContext().getString(R.string.yokdil_fall), applicationStart = LocalDate.of(2026, 7, 15), applicationEnd = LocalDate.of(2026, 7, 23), examDate = LocalDate.of(2026, 8, 23)),
        ExamInfo(name = PlanDataSource.getAppContext().getString(R.string.yds_fall), applicationStart = LocalDate.of(2026, 8, 26), applicationEnd = LocalDate.of(2026, 9, 3), examDate = LocalDate.of(2026, 10, 25))
    )

    fun getNextExam(): ExamInfo? {
        val today = LocalDate.now()
        return upcomingExams.filter { it.examDate.isAfter(today) }.minByOrNull { it.examDate }
    }
}
//endregion

//region DATASTORE VE REPOSITORY
// Centralized in data module: use applicationContext.dataStore via com.mtlc.studyplan.data.dataStore
//endregion

//region BİLDİRİM VE ARKA PLAN İŞLERİ
object NotificationHelper {
    private const val CHANNEL_ID_REMINDER = "YDS_REMINDER_CHANNEL"
    private const val NOTIFICATION_ID_REMINDER = 1

    private const val CHANNEL_ID_APPLICATION = "YDS_APPLICATION_CHANNEL"

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
//endregion

//region DİL YÖNETİMİ
object LanguageManager {
    fun getCurrentLanguage(context: Context): String {
        val sharedPrefs = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        return sharedPrefs.getString(Constants.PREF_SELECTED_LANGUAGE, Constants.LANGUAGE_TURKISH)
            ?: Constants.LANGUAGE_TURKISH
    }

    fun setLanguage(context: Context, language: String) {
        val sharedPrefs = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        sharedPrefs.edit().putString(Constants.PREF_SELECTED_LANGUAGE, language).apply()
    }

    @SuppressLint("AppBundleLocaleChanges")
    fun updateLocale(context: Context, language: String): Context {
        val locale = java.util.Locale.forLanguageTag(language)
        java.util.Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        return context.createConfigurationContext(config)
    }
}
//endregion

//region ANA ACTIVITY VE YARDIMCI FONKSİYONLAR
class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context?) {
        val currentLanguage = LanguageManager.getCurrentLanguage(newBase ?: super.getBaseContext())
        val updatedContext = LanguageManager.updateLocale(newBase ?: super.getBaseContext(), currentLanguage)
        super.attachBaseContext(updatedContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // PlanDataSource'u initialize et
        PlanDataSource.initialize(this)

        // Get real certificate pins for network security configuration
        // UNCOMMENT THIS LINE IN DEBUG MODE TO GET ACTUAL CERTIFICATE HASHES
        // CertificatePinRetriever.getCertificatePins()

        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                AppNavHost()
            }
        }
    }
}

//endregion

//region EKRAN COMPOSABLE'LARI
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PlanScreen() {
    val context = LocalContext.current
    val appContext = context.applicationContext as Context
    val repository = remember { ProgressRepository(appContext.dataStore) }
    val userProgress by repository.userProgressFlow.collectAsState(initial = null)
    val taskLogs by repository.taskLogsFlow.collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()

    // Tooltip/Onboarding state
    val onboardingRepo = remember { OnboardingRepository(appContext.dataStore) }
    val shownTooltips by onboardingRepo.shownTooltips.collectAsState(initial = emptySet())
    val isOnboardingComplete by onboardingRepo.isOnboardingCompleted.collectAsState(initial = false)
    var currentTooltipId by remember { mutableStateOf<String?>(null) }

    // Undo system
    val undoManager = rememberUndoManager()
    var recentUndoAction by remember { mutableStateOf<UndoAction?>(null) }

    // Current week view state
    var showAllWeeks by remember { mutableStateOf(false) }


    // Smart scheduling
    val smartScheduler = remember { SmartScheduler() }
    var smartSuggestions by remember { mutableStateOf<List<SmartSuggestion>>(emptyList()) }
    var dismissedSuggestions by remember { mutableStateOf<Set<String>>(emptySet()) }

    // Generate smart suggestions
    LaunchedEffect(taskLogs, userProgress) {
        if (taskLogs.isNotEmpty()) {
            userProgress?.let { progress ->
                val pattern = smartScheduler.analyzeUserPatterns(taskLogs, progress)
                val suggestions = smartScheduler.generateSuggestions(pattern, taskLogs, progress)
                smartSuggestions = suggestions.filterNot { it.id in dismissedSuggestions }
            }
        }
    }

    // Show next tooltip in sequence if onboarding not complete, but only on first visit
    var hasTriggeredInitialTooltip by remember { mutableStateOf(false) }
    LaunchedEffect(shownTooltips, isOnboardingComplete) {
        if (!isOnboardingComplete && !hasTriggeredInitialTooltip && currentTooltipId == null) {
            val nextTooltip = OnboardingRepository.onboardingFlow.firstOrNull { it !in shownTooltips }
            if (nextTooltip != null) {
                currentTooltipId = nextTooltip
                hasTriggeredInitialTooltip = true
            }
        }
    }

    // Overrides + merged plan
    val overridesStore = remember { PlanOverridesStore(appContext.dataStore) }
    val settingsStore = remember { com.mtlc.studyplan.data.PlanSettingsStore(appContext.dataStore) }
    val planRepo = remember { PlanRepository(overridesStore, settingsStore) }
    val mergedPlanData by planRepo.planFlow.collectAsState(initial = emptyList())
    val mergedPlanUi = remember(mergedPlanData) {
        mergedPlanData.map { it.toUiWeekPlan() }
    }
    val overrides by overridesStore.overridesFlow.collectAsState(initial = UserPlanOverrides())
    val planSettings by settingsStore.settingsFlow.collectAsState(initial = com.mtlc.studyplan.data.PlanDurationSettings())

    var showCustomize by remember { mutableStateOf(false) }
    var showPlanSettings by remember { mutableStateOf(false) }
    var editState by remember { mutableStateOf<Triple<String, String?, String?>?>(null) }
    var showAbout by remember { mutableStateOf(false) }

    // Use the effective (post-merge & post-compression) plan for progress
    val effectiveTasks = remember(mergedPlanData) { mergedPlanData.flatMap { it.days }.flatMap { it.tasks } }
    val effectiveTaskIds = remember(effectiveTasks) { effectiveTasks.map { it.id }.toSet() }
    val totalTasks = remember(effectiveTasks) { effectiveTasks.size }

    // Optimize: Use derivedStateOf for progress calculation
    val progress by remember( userProgress, effectiveTaskIds, totalTasks ) {
        derivedStateOf {
            if (totalTasks > 0 && userProgress != null) {
                val completedInPlan = userProgress!!.completedTasks.count { it in effectiveTaskIds }
                completedInPlan.toFloat() / totalTasks
            } else 0f
        }
    }
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "Overall Progress Animation")
    val snackbarHostState = remember { SnackbarHostState() }
    var showAchievementsSheet by remember { mutableStateOf(false) }
    var pendingLogTask by remember { mutableStateOf<Pair<String, String>?>(null) } // taskId to composed text
    var pendingLogInitialMinutes by remember { mutableStateOf(0) }

    val currentProgress = userProgress ?: UserProgress()

    // Handle undo snackbar
    UndoSnackbarEffect(
        undoManager = undoManager,
        snackbarHostState = snackbarHostState,
        recentAction = recentUndoAction,
        onActionConsumed = { recentUndoAction = null }
    )

    if (showAchievementsSheet) {
        AchievementsSheet(unlockedAchievementIds = currentProgress.unlockedAchievements, onDismiss = { showAchievementsSheet = false })
    }

    if (showAbout) {
        AboutSheet(onDismiss = { showAbout = false })
    }

    pendingLogTask?.let { (taskId, text) ->
        TaskLogDialog(
            taskText = text,
            initialMinutes = pendingLogInitialMinutes,
            onDismiss = { pendingLogTask = null },
            onSave = { minutes, correct, category ->
                coroutineScope.launch {
                    repository.addTaskLog(TaskLog(taskId, System.currentTimeMillis(), minutes, correct, category))
                }
                pendingLogTask = null
            }
        )
    }

    TooltipManager(
        tooltips = OnboardingRepository.availableTooltips,
        currentTooltipId = currentTooltipId,
        onDismiss = { tooltipId ->
            coroutineScope.launch {
                onboardingRepo.markTooltipShown(tooltipId)
                // Mark onboarding complete if all main tooltips shown
                if (OnboardingRepository.onboardingFlow.all { it in (shownTooltips + tooltipId) }) {
                    onboardingRepo.markOnboardingCompleted()
                    hasTriggeredInitialTooltip = false // Reset for next session
                }
            }
            currentTooltipId = null
        }
    ) {
        Scaffold(
            topBar = {
                if (!showCustomize)
                    MainHeader(
                        onOpenCustomize = { showCustomize = true },
                        onOpenAbout = { showAbout = true },
                        onOpenPlanSettings = { showPlanSettings = true },
                        showTooltips = !isOnboardingComplete,
                        shownTooltips = shownTooltips,
                        onShowTooltip = { tooltipId -> currentTooltipId = tooltipId },
                        isOnboardingComplete = isOnboardingComplete
                    )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (showCustomize) {
                // Customize screen with edit dialog
                CustomizePlanScreen(
                    plan = mergedPlanData,
                    overrides = overrides,
                    startEpochDay = planSettings.startEpochDay,
                    onBack = { showCustomize = false },
                    onToggleHidden = { id, hidden -> coroutineScope.launch { planRepo.setTaskHidden(id, hidden) } },
                    onRequestEdit = { id, currentDesc, currentDetails -> editState = Triple(id, currentDesc, currentDetails) },
                    onAddTask = { week, dayIndex -> coroutineScope.launch { planRepo.addCustomTask(week, dayIndex, "New task", null) } },
                )
                editState?.let { (taskId, curDesc, curDetails) ->
                    EditTaskDialog(
                        initialDesc = curDesc ?: "",
                        initialDetails = curDetails ?: "",
                        onDismiss = { editState = null },
                        onSave = { newDesc, newDetails ->
                            coroutineScope.launch { planRepo.updateTaskText(taskId, newDesc, newDetails) }
                            editState = null
                        }
                    )
                }
            } else if (userProgress == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val weekStartOffsets = remember(mergedPlanUi) {
                    var acc = 0
                    mergedPlanUi.map { week ->
                        val offset = acc
                        acc += week.days.size
                        offset
                    }
                }

                val absoluteStartDate = remember(planSettings.startEpochDay) { LocalDate.ofEpochDay(planSettings.startEpochDay) }
                val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM dd") }

                // Calculate current week based on start date
                val currentWeek = remember(absoluteStartDate) {
                    val today = LocalDate.now()
                    val daysSinceStart = ChronoUnit.DAYS.between(absoluteStartDate, today)
                    val weekNumber = (daysSinceStart / 7).toInt() + 1
                    weekNumber.coerceAtLeast(1).coerceAtMost(mergedPlanUi.size)
                }

                // Filter weeks to show
                val weeksToShow = remember(mergedPlanUi, showAllWeeks, currentWeek) {
                    if (showAllWeeks) {
                        mergedPlanUi
                    } else {
                        mergedPlanUi.filter { week ->
                            // Show current week and 1 week before/after
                            week.week in (currentWeek - 1)..(currentWeek + 1)
                        }.ifEmpty { mergedPlanUi.take(3) } // Fallback to first 3 weeks
                    }
                }

                LazyColumn(modifier = Modifier.weight(1f)) {
                    stickyHeader {
                        Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                            GamificationHeader(
                                streakCount = currentProgress.streakCount,
                                achievementsCount = currentProgress.unlockedAchievements.size,
                                onAchievementsClick = { showAchievementsSheet = true },
                                showTooltips = !isOnboardingComplete,
                                shownTooltips = shownTooltips,
                                onShowTooltip = { tooltipId -> currentTooltipId = tooltipId }
                            )
                            ExamCountdownCard()
                            OverallProgressCard(progress = animatedProgress)

                            // View toggle button
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                OutlinedButton(
                                    onClick = { showAllWeeks = !showAllWeeks },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = if (showAllWeeks)
                                            context.getString(R.string.show_current_period)
                                        else
                                            context.getString(R.string.show_all_weeks),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                    // Smart suggestions below header so it scrolls
                    if (smartSuggestions.isNotEmpty()) {
                        item {
                            SmartSuggestionsCard(
                                suggestions = smartSuggestions,
                                onSuggestionClick = { suggestion ->
                                    when (suggestion.type) {
                                        com.mtlc.studyplan.ai.SuggestionType.WEAK_AREA_FOCUS -> {
                                            suggestion.category?.let { category ->
                                                coroutineScope.launch {
                                                    planRepo.addCustomTask(
                                                        currentWeek,
                                                        0,
                                                        "Focus: $category",
                                                        "Targeted practice based on AI analysis of your weak areas"
                                                    )
                                                }
                                            }
                                        }
                                        else -> {
                                            dismissedSuggestions = dismissedSuggestions + suggestion.id
                                        }
                                    }
                                },
                                onDismissSuggestion = { suggestion ->
                                    dismissedSuggestions = dismissedSuggestions + suggestion.id
                                    smartSuggestions = smartSuggestions.filterNot { it.id == suggestion.id }
                                }
                            )
                        }
                    }
                    itemsIndexed(
                        items = weeksToShow,
                        key = { _, it -> it.week }
                    ) { displayIndex, weekPlan ->
                        val originalIndex = mergedPlanUi.indexOfFirst { it.week == weekPlan.week }
                        val weekStartDate = remember(originalIndex, weekStartOffsets, absoluteStartDate) {
                            absoluteStartDate.plusDays(weekStartOffsets.getOrElse(originalIndex) { originalIndex * 7 }.toLong())
                        }
                        WeekCard(
                            weekPlan = weekPlan,
                            completedTasks = currentProgress.completedTasks,
                            weekStartDate = weekStartDate,
                            dateFormatter = dateFormatter,
                            onToggleTask = { taskId, taskText ->
                                coroutineScope.launch {
                                    val wasCompleted = currentProgress.completedTasks.contains(taskId)
                                    val previousProgress = currentProgress.copy()
                                    val currentTasks = currentProgress.completedTasks.toMutableSet()

                                    if (wasCompleted) {
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

                                    // Optimize: Cache achievement calculations
                                    val updatedProgressForCheck = currentProgress.copy(completedTasks = currentTasks, streakCount = newStreak)
                                    val newUnlocked = AchievementDataSource.allAchievements.filter { achievement ->
                                        !currentProgress.unlockedAchievements.contains(achievement.id) && achievement.condition(updatedProgressForCheck)
                                    }
                                    newUnlocked.forEach { achievement ->
                                        launch {
                                            snackbarHostState.showSnackbar(
                                                message = PlanDataSource.getAppContext().getString(R.string.new_achievement_unlocked, achievement.title),
                                                actionLabel = "OK",
                                                withDismissAction = true,
                                                duration = SnackbarDuration.Short
                                            )
                                        }
                                    }
                                    val allUnlockedIds = currentProgress.unlockedAchievements + newUnlocked.map { it.id }

                                    val newProgress = currentProgress.copy(
                                        completedTasks = currentTasks,
                                        streakCount = newStreak,
                                        lastCompletionDate = if(currentTasks.size > currentProgress.completedTasks.size) today.timeInMillis else currentProgress.lastCompletionDate,
                                        unlockedAchievements = allUnlockedIds
                                    )

                                    repository.saveProgress(newProgress)

                                    // Create undo action
                                    if (!wasCompleted) {
                                        // Task was just completed - create undo action
                                        val undoAction = UndoAction(
                                            id = "task_$taskId",
                                            actionDescription = "Task completed",
                                            undoAction = {
                                                repository.saveProgress(previousProgress)
                                            }
                                        )
                                        undoManager.addUndoAction(undoAction)
                                        recentUndoAction = undoAction

                                        // Prompt for log
                                        pendingLogInitialMinutes = estimateDurationMinutes(taskText)
                                        pendingLogTask = taskId to taskText
                                    }
                                }
                            },
                            onAddBooster = { week, dayIndex, desc, details ->
                                coroutineScope.launch { planRepo.addCustomTask(week, dayIndex, desc, details) }
                            },
                            weaknessSummaries = computeWeakness(taskLogs)
                        )
                    }
                }
            }
        }
    }

    if (showPlanSettings) {
        val cfg = planSettings
        com.mtlc.studyplan.ui.PlanSettingsDialog(
            startEpochDay = cfg.startEpochDay,
            totalWeeks = cfg.totalWeeks,
            endEpochDay = cfg.endEpochDay,
            totalMonths = cfg.totalMonths,
            monMinutes = cfg.monMinutes,
            tueMinutes = cfg.tueMinutes,
            wedMinutes = cfg.wedMinutes,
            thuMinutes = cfg.thuMinutes,
            friMinutes = cfg.friMinutes,
            satMinutes = cfg.satMinutes,
            sunMinutes = cfg.sunMinutes,
            dateFormatPattern = cfg.dateFormatPattern,
            onDismiss = { showPlanSettings = false },
            onSave = { startEpochDay, weeks, endEpochDay, totalMonths, mon, tue, wed, thu, fri, sat, sun, pattern ->
                coroutineScope.launch {
                    settingsStore.update {
                        it.copy(
                            startEpochDay = startEpochDay,
                            totalWeeks = weeks,
                            endEpochDay = endEpochDay,
                            totalMonths = totalMonths,
                            monMinutes = mon,
                            tueMinutes = tue,
                            wedMinutes = wed,
                            thuMinutes = thu,
                            friMinutes = fri,
                            satMinutes = sat,
                            sunMinutes = sun,
                            dateFormatPattern = pattern,
                        )
                    }
                }
                showPlanSettings = false
            }
        )
    }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainHeader(
    onOpenCustomize: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenPlanSettings: () -> Unit,
    showTooltips: Boolean = false,
    shownTooltips: Set<String> = emptySet(),
    onShowTooltip: (String) -> Unit = {},
    isOnboardingComplete: Boolean = false
) {
    val context = LocalContext.current
    val currentLanguage = remember { LanguageManager.getCurrentLanguage(context) }

    // Cache frequently used strings
    val appTitle = remember { context.getString(R.string.app_title) }

// Move string caching to individual composables where needed

    // Surface ve Row yerine TopAppBar kullanıyoruz
    TopAppBar(
        title = {
            Text(
                text = appTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        actions = {
            // Dil değiştirme butonları
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TooltipTrigger(
                    tooltipId = "customize_plan",
                    isVisible = showTooltips && !isOnboardingComplete && "customize_plan" !in shownTooltips,
                    onShowTooltip = onShowTooltip
                ) {
                    IconButton(onClick = onOpenCustomize) {
                        Icon(imageVector = Icons.Default.EditCalendar, contentDescription = PlanDataSource.getAppContext().getString(R.string.customize_cd))
                    }
                }
                TooltipTrigger(
                    tooltipId = "plan_settings",
                    isVisible = showTooltips && !isOnboardingComplete && "plan_settings" !in shownTooltips,
                    onShowTooltip = onShowTooltip
                ) {
                    IconButton(onClick = onOpenPlanSettings) {
                        Icon(imageVector = Icons.Default.EventAvailable, contentDescription = PlanDataSource.getAppContext().getString(R.string.plan_settings_title))
                    }
                }
                IconButton(onClick = onOpenAbout) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = PlanDataSource.getAppContext().getString(R.string.about_title))
                }
                TooltipTrigger(
                    tooltipId = "language_switch",
                    isVisible = showTooltips && !isOnboardingComplete && "language_switch" !in shownTooltips,
                    onShowTooltip = onShowTooltip
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "TR",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (currentLanguage == "tr") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .clickable {
                                    LanguageManager.setLanguage(context, "tr")
                                    (context as? MainActivity)?.recreate()
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                        Text(
                            text = "EN",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (currentLanguage == "en") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .clickable {
                                    LanguageManager.setLanguage(context, "en")
                                    (context as? MainActivity)?.recreate()
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                val emailSubject = remember { context.getString(R.string.email_subject) }
                val emailChooserTitle = remember { context.getString(R.string.email_chooser_title) }
                IconButton(onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = "mailto:".toUri()
                        putExtra(Intent.EXTRA_EMAIL, arrayOf(Constants.EMAIL_ADDRESS))
                        putExtra(Intent.EXTRA_SUBJECT, emailSubject)
                    }
                    context.startActivity(Intent.createChooser(intent, emailChooserTitle))
                }) {
                    Icon(imageVector = Icons.Default.Email, contentDescription = emailChooserTitle)
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface // Arka plan rengi
        )
    )
}

@Composable
fun ExamCountdownCard() {
    val context = LocalContext.current
    val nextExam = remember { ExamCalendarDataSource.getNextExam() }
    if (nextExam == null) return

    val today = LocalDate.now()
    val daysToApplicationEnd = ChronoUnit.DAYS.between(today, nextExam.applicationEnd)
    val daysToExam = ChronoUnit.DAYS.between(today, nextExam.examDate)

    // Cache frequently used strings
    val upcomingExam = remember { context.getString(R.string.upcoming_exam) }
    val applicationDeadline = remember { context.getString(R.string.application_deadline) }
    val timeUntilExam = remember { context.getString(R.string.time_until_exam) }
    // val daysUnit = remember { context.getString(R.string.days_unit) } // Removed
    // val timeExpired = remember { context.getString(R.string.time_expired) } // Removed
    val osymButton = remember { context.getString(R.string.osym_button) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(text = "$upcomingExam ${nextExam.name}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                CountdownItem(
                    icon = Icons.Default.EditCalendar,
                    label = applicationDeadline,
                    days = daysToApplicationEnd + 1,
                    isExpired = daysToApplicationEnd < 0,
                    color = MaterialTheme.colorScheme.secondary
                )
                CountdownItem(
                    icon = Icons.Default.EventAvailable,
                    label = timeUntilExam,
                    days = daysToExam,
                    isExpired = daysToExam < 0,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Constants.OSYM_URL.toUri())
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth().height(36.dp),
                contentPadding = PaddingValues(vertical = 6.dp),
                enabled = daysToExam >= 0
            ) {
                Text(osymButton, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun CountdownItem(icon: ImageVector, label: String, days: Long, isExpired: Boolean, color: Color) {
    val context = LocalContext.current

    // Cache frequently used strings
    val daysUnit = remember { context.getString(R.string.days_unit) }
    val timeExpired = remember { context.getString(R.string.time_expired) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = icon, contentDescription = label, tint = color, modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.height(4.dp))
        if (!isExpired) {
            Text(daysUnit.format(days), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.bodySmall)
        } else {
            Text(timeExpired, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsSheet(unlockedAchievementIds: Set<String>, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val allAchievements = remember { AchievementDataSource.allAchievements }

    // Cache frequently used strings
    val achievementsTitle = remember { context.getString(R.string.achievements_title) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)) {
            Text(text = achievementsTitle, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
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
        Icon(imageVector = Icons.Default.WorkspacePremium, contentDescription = PlanDataSource.getAppContext().getString(R.string.achievement_icon_description), tint = iconColor, modifier = Modifier.size(40.dp))
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
    onAchievementsClick: () -> Unit,
    showTooltips: Boolean = false,
    shownTooltips: Set<String> = emptySet(),
    onShowTooltip: (String) -> Unit = {}
) {
    val context = LocalContext.current

    // Cache frequently used strings
    val streakLabel = remember { context.getString(R.string.streak_label) }
    val achievementsLabel = remember { context.getString(R.string.achievements_label) }
    val daysUnit = remember { context.getString(R.string.days_unit) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        InfoChip(icon = Icons.Default.LocalFireDepartment, label = streakLabel, value = daysUnit.format(streakCount), iconColor = MaterialTheme.colorScheme.error)
        TooltipTrigger(
            tooltipId = "achievements",
            isVisible = showTooltips && "achievements" !in shownTooltips,
            onShowTooltip = onShowTooltip
        ) {
            Box(modifier = Modifier.clickable { onAchievementsClick() }) {
                InfoChip(icon = Icons.Default.WorkspacePremium, label = achievementsLabel, value = "$achievementsCount", iconColor = MaterialTheme.colorScheme.tertiary)
            }
        }
    }
}

@Composable
fun InfoChip(icon: ImageVector, label: String, value: String, iconColor: Color) {
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
    val context = LocalContext.current

    // Cache frequently used strings
    val overallProgress = remember { context.getString(R.string.overall_progress) }
    val percentFormat = remember { context.getString(R.string.percent_format) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(overallProgress, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(percentFormat.format((progress * 100).toInt()), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
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
fun WeekCard(
    weekPlan: WeekPlan,
    completedTasks: Set<String>,
    weekStartDate: LocalDate,
    dateFormatter: java.time.format.DateTimeFormatter,
    onToggleTask: (String, String) -> Unit,
    onAddBooster: (week: Int, dayIndex: Int, desc: String, details: String?) -> Unit,
    weaknessSummaries: Map<String, com.mtlc.studyplan.data.WeaknessSummary>,
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(weekPlan.week == 1) }

    // Cache frequently used strings
    val tasksCompleted = remember { context.getString(R.string.tasks_completed) }

    // Optimize: Pre-calculate week tasks and completed count
    val weekTasks = remember(weekPlan) { weekPlan.days.flatMap { it.tasks } }
    val completedInWeek by remember(weekPlan, completedTasks) {
        derivedStateOf { weekTasks.count { completedTasks.contains(it.id) } } // Removed <Int>
    }

    val rotationAngle by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, label = "Icon Rotation")

    val monthColors = remember {
        listOf(
            Color(0xFF009688), Color(0xFF3F51B5), Color(0xFF9C27B0),
            Color(0xFFFFC107), Color(0xFF673AB7), Color(0xFFFF5722),
            Color(0xFF795548), Color(0xFFE91E63), Color(0xFF4CAF50), Color(0xFF2196F3)
        )
    }
    val titleColor = monthColors[(weekPlan.month - 1) % monthColors.size]

    val weekEndDate = remember(weekStartDate, weekPlan) { weekStartDate.plusDays((weekPlan.days.size - 1).coerceAtLeast(0).toLong()) }
    val df = remember(dateFormatter) { dateFormatter }

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
                    val weekRange = "${weekStartDate.format(df)} - ${weekEndDate.format(df)}"
                    Text(weekRange, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(tasksCompleted.format(completedInWeek, weekTasks.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = PlanDataSource.getAppContext().getString(R.string.expand_collapse), modifier = Modifier.rotate(rotationAngle))
            }
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + fadeIn(
                    animationSpec = tween(
                        durationMillis = 300,
                        easing = FastOutSlowInEasing
                    )
                ),
                exit = shrinkVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeOut(
                    animationSpec = tween(
                        durationMillis = 200,
                        easing = FastOutLinearInEasing
                    )
                )
            ) {
                Column(
                    modifier = Modifier.animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                ) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    weekPlan.days.forEachIndexed { idx, dayPlan ->
                        DaySection(
                            weekNumber = weekPlan.week,
                            dayPlan = dayPlan,
                            dayIndex = idx,
                            weekStartDate = weekStartDate,
                            completedTasks = completedTasks,
                            dateFormatter = dateFormatter,
                            weaknessSummaries = weaknessSummaries,
                            onToggleTask = onToggleTask,
                            onAddBooster = onAddBooster,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DaySection(
    weekNumber: Int,
    dayPlan: DayPlan,
    dayIndex: Int,
    weekStartDate: LocalDate,
    completedTasks: Set<String>,
    dateFormatter: java.time.format.DateTimeFormatter,
    weaknessSummaries: Map<String, com.mtlc.studyplan.data.WeaknessSummary>,
    onToggleTask: (String, String) -> Unit,
    onAddBooster: (week: Int, dayIndex: Int, desc: String, details: String?) -> Unit,
) {
    val context = LocalContext.current
    val dayDate = remember(weekStartDate, dayIndex) { weekStartDate.plusDays(dayIndex.toLong()) }
    val df = remember(dateFormatter) { dateFormatter }
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        val dayResource = when (dayPlan.day) {
            PlanDataSource.getAppContext().getString(R.string.monday) -> R.string.monday
            PlanDataSource.getAppContext().getString(R.string.tuesday) -> R.string.tuesday
            PlanDataSource.getAppContext().getString(R.string.wednesday) -> R.string.wednesday
            PlanDataSource.getAppContext().getString(R.string.thursday) -> R.string.thursday
            PlanDataSource.getAppContext().getString(R.string.friday) -> R.string.friday
            PlanDataSource.getAppContext().getString(R.string.saturday) -> R.string.saturday
            PlanDataSource.getAppContext().getString(R.string.sunday) -> R.string.sunday
            else -> null
        }
        val dayText = remember(dayResource, dayPlan.day) {
            dayResource?.let { context.getString(it) } ?: dayPlan.day
        }
        Text(
            text = "$dayText – ${dayDate.format(df)}",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        dayPlan.tasks.forEach { task ->
            val isDone = completedTasks.contains(task.id)
            com.mtlc.studyplan.ui.components.TaskCard(
                task = task,
                checked = isDone,
                onCheckedChange = { new -> if (new != isDone) onToggleTask(task.id, task.desc + " " + (task.details ?: "")) }
            )
            Spacer(Modifier.height(6.dp))
        }

        // Booster suggestion based on weaknesses
        val topWeak = remember(weaknessSummaries) {
            weaknessSummaries.values.sortedByDescending { it.incorrectRate }.take(1).firstOrNull { it.total >= 3 && it.incorrectRate >= 0.4 }
        }
        if (topWeak != null) {
            Spacer(Modifier.height(6.dp))
            val boosterTitle = PlanDataSource.getAppContext().getString(R.string.booster_suggestion)
            val desc = remember(topWeak) { "Booster: ${topWeak.category.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }} – 15 dk" }
            val details = PlanDataSource.getAppContext().getString(R.string.booster_details, topWeak.category)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "$boosterTitle: ${topWeak.category} (${(topWeak.incorrectRate * 100).toInt()}%)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = { onAddBooster(weekNumber, dayIndex, desc, details) }) {
                    Text(PlanDataSource.getAppContext().getString(R.string.add_booster))
                }
            }
        }
    }
}

@Composable
fun TaskItem(task: Task, isCompleted: Boolean, onToggleTask: () -> Unit) {
    var isExpanded by remember { mutableStateOf(false) }

    val priority = remember(task.desc) { TaskPriority.fromTaskDescription(task.desc) }
    val estimatedMinutes = remember(task.desc, task.details) {
        estimateDurationMinutes("${task.desc} ${task.details ?: ""}")
    }

    val category = remember(task.desc) {
        when {
            task.desc.contains("exam", ignoreCase = true) -> "Exam"
            task.desc.contains("practice", ignoreCase = true) -> "Practice"
            task.desc.contains("reading", ignoreCase = true) -> "Reading"
            task.desc.contains("vocabulary", ignoreCase = true) -> "Vocabulary"
            task.desc.contains("grammar", ignoreCase = true) -> "Grammar"
            task.desc.contains("listening", ignoreCase = true) -> "Listening"
            else -> "Study"
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (priority == TaskPriority.CRITICAL) 4.dp else 1.dp
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Checkbox(
                    checked = isCompleted,
                    onCheckedChange = { onToggleTask() },
                    modifier = Modifier.padding(end = 8.dp)
                )

                Column(modifier = Modifier.weight(1f)) {
                    // Priority and category indicators
                    Row(
                        modifier = Modifier.padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PriorityIndicator(priority = priority)
                        TaskCategoryChip(category = category)
                        Spacer(modifier = Modifier.weight(1f))
                        EstimatedTimeChip(minutes = estimatedMinutes)
                    }

                    // Task description
                    Text(
                        text = task.desc,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = when (priority) {
                                TaskPriority.CRITICAL -> FontWeight.Bold
                                TaskPriority.HIGH -> FontWeight.SemiBold
                                else -> FontWeight.Normal
                            },
                            textDecoration = if (isCompleted) TextDecoration.LineThrough else null
                        ),
                        color = if (isCompleted)
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Expandable details section
            AnimatedVisibility(
                visible = isExpanded && task.details != null,
                enter = expandVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeIn(
                    animationSpec = tween(
                        durationMillis = 250,
                        easing = FastOutSlowInEasing
                    )
                ),
                exit = shrinkVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessHigh
                    )
                ) + fadeOut(
                    animationSpec = tween(
                        durationMillis = 150,
                        easing = FastOutLinearInEasing
                    )
                )
            ) {
                Column(
                    modifier = Modifier.animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                ) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                    Text(
                        text = task.details ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val versionName = remember {
        try {
            val pm = context.packageManager
            val pkg = context.packageName
            @Suppress("DEPRECATION")
            val info = pm.getPackageInfo(pkg, 0)
            info.versionName ?: ""
        } catch (_: Exception) { "" }
    }
    val aboutTitle = remember { context.getString(R.string.about_title) }
    val aboutVersion = remember(versionName) { context.getString(R.string.about_version, versionName) }
    val featureTitle = remember { context.getString(R.string.about_feature_customize_title) }
    val featureDesc = remember { context.getString(R.string.about_feature_customize_desc) }
    val persistence = remember { context.getString(R.string.about_data_persistence) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(text = aboutTitle, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(text = aboutVersion, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            Text(text = featureTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(text = featureDesc, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Text(text = persistence, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismiss) { Text("OK") }
            }
        }
    }
}

// Map data-layer plan models to UI models defined in this file
private fun com.mtlc.studyplan.data.WeekPlan.toUiWeekPlan(): WeekPlan =
    WeekPlan(
        week = this.week,
        month = this.month,
        title = this.title,
        days = this.days.map { d -> DayPlan(d.day, d.tasks.map { t -> Task(t.id, t.desc, t.details) }) }
    )

private fun estimateDurationMinutes(text: String): Int {
    val s = text.lowercase()
    Regex("(\\d{2,3})\\s*(-\\s*(\\d{2,3}))?\\s*(dk|dakika|minute|min)").find(s)?.let { m ->
        val a = m.groupValues[1].toIntOrNull() ?: return@let null
        val b = m.groupValues.getOrNull(3)?.toIntOrNull()
        return b?.let { (a + it) / 2 } ?: a
    }
    return when {
        s.contains("tam deneme") || s.contains("full exam") -> 180
        s.contains("mini deneme") || s.contains("mini exam") -> 70
        s.contains("okuma") || s.contains("reading") -> 45
        s.contains("kelime") || s.contains("vocab") -> 30
        s.contains("analiz") || s.contains("analysis") -> 30
        s.contains("dinleme") || s.contains("listening") -> 30
        s.contains("hizli pratik") || s.contains("pratik") || s.contains("drill") -> 25
        s.contains("gramer") || s.contains("grammar") -> 40
        else -> 30
    }
}

private fun categorizeTask(text: String): String {
    val s = text.lowercase()
    return when {
        s.contains("kelime") || s.contains("vocab") -> "vocabulary"
        s.contains("okuma") || s.contains("reading") -> "reading"
        s.contains("dinleme") || s.contains("listening") -> "listening"
        s.contains("deneme") || s.contains("exam") -> "exam"
        s.contains("analiz") || s.contains("analysis") -> "analysis"
        s.contains("gramer") || s.contains("grammar") -> "grammar"
        else -> "general"
    }
}

@Composable
private fun computeWeakness(logs: List<TaskLog>): Map<String, com.mtlc.studyplan.data.WeaknessSummary> {
    val grouped = logs.groupBy { it.category.ifBlank { "general" } }
    return grouped.mapValues { (_, list) ->
        val total = list.size
        val incorrect = list.count { !it.correct }
        val rate = if (total > 0) incorrect.toDouble() / total else 0.0
        com.mtlc.studyplan.data.WeaknessSummary(category = list.first().category, total = total, incorrect = incorrect, incorrectRate = rate)
    }
}

@Composable
fun TaskLogDialog(taskText: String, initialMinutes: Int, onDismiss: () -> Unit, onSave: (minutes: Int, correct: Boolean, category: String) -> Unit) {
    var minutesText by remember { mutableStateOf(initialMinutes.toString()) }
    var correct by remember { mutableStateOf(true) }
    val defaultCategory = remember(taskText) { categorizeTask(taskText) }
    var categoryText by remember { mutableStateOf(defaultCategory) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = PlanDataSource.getAppContext().getString(R.string.log_task_result)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = taskText, style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(value = minutesText, onValueChange = { minutesText = it.filter { ch -> ch.isDigit() } }, label = { Text(PlanDataSource.getAppContext().getString(R.string.minutes_spent)) })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = correct, onCheckedChange = { correct = it })
                    Spacer(Modifier.width(8.dp))
                    Text(text = PlanDataSource.getAppContext().getString(R.string.mark_correct))
                }
                OutlinedTextField(value = categoryText, onValueChange = { categoryText = it }, label = { Text(PlanDataSource.getAppContext().getString(R.string.category_label)) })
            }
        },
        confirmButton = { TextButton(onClick = { onSave(minutesText.toIntOrNull() ?: 0, correct, categoryText.ifBlank { defaultCategory }) }) { Text(text = PlanDataSource.getAppContext().getString(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(text = PlanDataSource.getAppContext().getString(R.string.cancel)) } }
    )
}
//endregion
