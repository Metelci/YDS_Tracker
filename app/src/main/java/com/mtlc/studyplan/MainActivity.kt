package com.mtlc.studyplan

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
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
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.mtlc.studyplan.ui.theme.StudyPlanTheme
import com.mtlc.studyplan.data.ProgressRepository
import com.mtlc.studyplan.data.dataStore
import com.mtlc.studyplan.ui.WelcomeScreen
import com.mtlc.studyplan.utils.Constants
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
        val book = PlanDataSource.getAppContext().getString(R.string.red_book)

        return WeekPlan(week, month, getAppContext().getString(R.string.month_week_format, month, week, PlanDataSource.getAppContext().getString(R.string.foundation_level)), listOf(
            DayPlan(PlanDataSource.getAppContext().getString(R.string.monday), listOf(
                Task("$weekId-pzt1", getAppContext().getString(R.string.grammar_topics_lesson), "$book, ${getAppContext().getString(R.string.units)}: $monUnits"),
                Task("$weekId-pzt2", PlanDataSource.getAppContext().getString(R.string.quick_practice_lesson), PlanDataSource.getAppContext().getString(R.string.practice_description_1))
            )),
            DayPlan(PlanDataSource.getAppContext().getString(R.string.tuesday), listOf(
                Task("$weekId-sal1", PlanDataSource.getAppContext().getString(R.string.grammar_topics_lesson), "$book, ${PlanDataSource.getAppContext().getString(R.string.units)}: $tueUnits"),
                Task("$weekId-sal2", PlanDataSource.getAppContext().getString(R.string.quick_practice_lesson), PlanDataSource.getAppContext().getString(R.string.practice_description_1))
            )),
            DayPlan(PlanDataSource.getAppContext().getString(R.string.wednesday), listOf(
                Task("$weekId-car1", PlanDataSource.getAppContext().getString(R.string.reading_vocabulary_lesson), PlanDataSource.getAppContext().getString(R.string.reading_description_1)),
                Task("$weekId-car2", PlanDataSource.getAppContext().getString(R.string.listening_repeat_lesson), PlanDataSource.getAppContext().getString(R.string.listening_description_1))
            )),
            DayPlan(PlanDataSource.getAppContext().getString(R.string.thursday), listOf(
                Task("$weekId-per1", PlanDataSource.getAppContext().getString(R.string.grammar_topics_lesson), "$book, ${PlanDataSource.getAppContext().getString(R.string.units)}: $thuUnits"),
                Task("$weekId-per2", PlanDataSource.getAppContext().getString(R.string.quick_practice_lesson), PlanDataSource.getAppContext().getString(R.string.practice_description_1))
            )),
            DayPlan(PlanDataSource.getAppContext().getString(R.string.friday), listOf(
                Task("$weekId-cum1", PlanDataSource.getAppContext().getString(R.string.grammar_topics_lesson), "$book, ${PlanDataSource.getAppContext().getString(R.string.units)}: $friUnits"),
                Task("$weekId-cum2", PlanDataSource.getAppContext().getString(R.string.quick_practice_lesson), PlanDataSource.getAppContext().getString(R.string.practice_description_1))
            )),
            DayPlan(PlanDataSource.getAppContext().getString(R.string.saturday), listOf(
                Task("$weekId-cmt1", PlanDataSource.getAppContext().getString(R.string.weekly_vocabulary_lesson), PlanDataSource.getAppContext().getString(R.string.vocabulary_description_1)),
                Task("$weekId-cmt2", PlanDataSource.getAppContext().getString(R.string.entertainment_english_lesson), PlanDataSource.getAppContext().getString(R.string.entertainment_description_1))
            )),
            DayPlan(PlanDataSource.getAppContext().getString(R.string.sunday), listOf(
                Task("$weekId-paz1", PlanDataSource.getAppContext().getString(R.string.weekly_general_repeat), PlanDataSource.getAppContext().getString(R.string.general_repeat_description_1)),
                Task("$weekId-paz2", PlanDataSource.getAppContext().getString(R.string.free_reading_listening), getAppContext().getString(R.string.free_reading_description_1))
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
Task("$weekId-mini_exam", PlanDataSource.getAppContext().getString(R.string.mini_exam_lesson), PlanDataSource.getAppContext().getString(R.string.mini_exam_description)),
Task("$weekId-mini_analysis", PlanDataSource.getAppContext().getString(R.string.mini_analysis_lesson), PlanDataSource.getAppContext().getString(R.string.mini_analysis_description)),
            )
        )

        val regularDays = mutableListOf<DayPlan>()

        regularDays.add(DayPlan(PlanDataSource.getAppContext().getString(R.string.monday), listOf(
            Task("$weekId-t1", PlanDataSource.getAppContext().getString(R.string.grammar_topic_lesson), PlanDataSource.getAppContext().getString(R.string.grammar_topic_description, book, grammarTopics)),
            Task("$weekId-t2", PlanDataSource.getAppContext().getString(R.string.quick_practice_lesson), PlanDataSource.getAppContext().getString(R.string.practice_description_1))
        )))
        regularDays.add(DayPlan(PlanDataSource.getAppContext().getString(R.string.tuesday), listOf(
            Task("$weekId-t3", PlanDataSource.getAppContext().getString(R.string.grammar_exercises_lesson), PlanDataSource.getAppContext().getString(R.string.grammar_exercises_description, book)),
            Task("$weekId-t4", PlanDataSource.getAppContext().getString(R.string.listening_repeat_lesson), PlanDataSource.getAppContext().getString(R.string.listening_description_1))
        )))
        if (shouldStartAdvancedPractice) {
            regularDays.add(miniExamDayPlan.copy(day = PlanDataSource.getAppContext().getString(R.string.wednesday)))
        } else {
            regularDays.add(DayPlan(PlanDataSource.getAppContext().getString(R.string.wednesday), listOf(
                Task("$weekId-t5", PlanDataSource.getAppContext().getString(R.string.grammar_reinforcement_lesson), PlanDataSource.getAppContext().getString(R.string.grammar_reinforcement_description)),
                Task("$weekId-t6", PlanDataSource.getAppContext().getString(R.string.free_reading_listening), PlanDataSource.getAppContext().getString(R.string.free_reading_description_1))
            )))
        }
        regularDays.add(DayPlan(PlanDataSource.getAppContext().getString(R.string.thursday), listOf(
            Task("$weekId-t7", PlanDataSource.getAppContext().getString(R.string.grammar_topic_continuation_lesson), PlanDataSource.getAppContext().getString(R.string.grammar_topic_continuation_description)),
            Task("$weekId-t8", PlanDataSource.getAppContext().getString(R.string.reading_vocabulary_lesson), PlanDataSource.getAppContext().getString(R.string.reading_description_1))
        )))
        if (shouldStartAdvancedPractice) {
            regularDays.add(DayPlan(PlanDataSource.getAppContext().getString(R.string.friday), listOf(
                Task("$weekId-t9", PlanDataSource.getAppContext().getString(R.string.question_type_practice_lesson), PlanDataSource.getAppContext().getString(R.string.question_type_practice_description, questionType)),
                Task("$weekId-t10", PlanDataSource.getAppContext().getString(R.string.exam_analysis), PlanDataSource.getAppContext().getString(R.string.exam_analysis_description)),
            )))
        } else {
            regularDays.add(DayPlan(PlanDataSource.getAppContext().getString(R.string.friday), listOf(
                Task("$weekId-t9", PlanDataSource.getAppContext().getString(R.string.weekly_grammar_review_lesson), PlanDataSource.getAppContext().getString(R.string.weekly_grammar_review_description)),
                Task("$weekId-t10", PlanDataSource.getAppContext().getString(R.string.free_reading_listening), PlanDataSource.getAppContext().getString(R.string.free_reading_description_1)),
            )))
        }
        regularDays.add(DayPlan(PlanDataSource.getAppContext().getString(R.string.saturday), listOf(
            Task("$weekId-t11", PlanDataSource.getAppContext().getString(R.string.next_week_preparation_lesson), PlanDataSource.getAppContext().getString(R.string.next_week_preparation_description, nextGrammarTopic)),
            Task("$weekId-t12", PlanDataSource.getAppContext().getString(R.string.weekly_vocabulary_lesson), PlanDataSource.getAppContext().getString(R.string.vocabulary_description_1))
        )))
        if (shouldStartAdvancedPractice) {
            regularDays.add(miniExamDayPlan.copy(day = PlanDataSource.getAppContext().getString(R.string.sunday)))
        } else {
            regularDays.add(DayPlan(PlanDataSource.getAppContext().getString(R.string.sunday), listOf(
                Task("$weekId-t13", PlanDataSource.getAppContext().getString(R.string.weekly_analysis_planning_lesson), PlanDataSource.getAppContext().getString(R.string.weekly_analysis_planning_description)),
                Task("$weekId-t14", PlanDataSource.getAppContext().getString(R.string.entertainment_english_lesson), PlanDataSource.getAppContext().getString(R.string.entertainment_description_1)),
            )))
        }

        return WeekPlan(week, month, PlanDataSource.getAppContext().getString(R.string.month_week_format, month, week, level), regularDays)
    }

    private fun createExamCampWeek(week: Int): WeekPlan {
        val month = ((week - 1) / 4) + 1
        val weekId = "w${week}"
        return WeekPlan(week, month, PlanDataSource.getAppContext().getString(R.string.exam_camp_week_title, month, week), listOf(
            DayPlan(PlanDataSource.getAppContext().getString(R.string.monday), listOf(Task("$weekId-exam-1", PlanDataSource.getAppContext().getString(R.string.full_exam), PlanDataSource.getAppContext().getString(R.string.full_exam_description)), Task("$weekId-analysis-1", PlanDataSource.getAppContext().getString(R.string.exam_analysis), PlanDataSource.getAppContext().getString(R.string.exam_analysis_description)))),
            DayPlan(PlanDataSource.getAppContext().getString(R.string.tuesday), listOf(Task("$weekId-exam-2", PlanDataSource.getAppContext().getString(R.string.full_exam), PlanDataSource.getAppContext().getString(R.string.full_exam_description_2)), Task("$weekId-analysis-2", PlanDataSource.getAppContext().getString(R.string.exam_analysis), PlanDataSource.getAppContext().getString(R.string.exam_analysis_description_2)))),
            DayPlan(PlanDataSource.getAppContext().getString(R.string.wednesday), listOf(Task("$weekId-exam-3", PlanDataSource.getAppContext().getString(R.string.full_exam), PlanDataSource.getAppContext().getString(R.string.full_exam_description_3)), Task("$weekId-analysis-3", PlanDataSource.getAppContext().getString(R.string.exam_analysis), PlanDataSource.getAppContext().getString(R.string.exam_analysis_description_3)))),
            DayPlan(PlanDataSource.getAppContext().getString(R.string.thursday), listOf(Task("$weekId-exam-4", PlanDataSource.getAppContext().getString(R.string.full_exam), PlanDataSource.getAppContext().getString(R.string.full_exam_description_3)), Task("$weekId-analysis-4", PlanDataSource.getAppContext().getString(R.string.exam_analysis), PlanDataSource.getAppContext().getString(R.string.exam_analysis_description_3)))),
            DayPlan(PlanDataSource.getAppContext().getString(R.string.friday), listOf(Task("$weekId-exam-5", PlanDataSource.getAppContext().getString(R.string.full_exam), PlanDataSource.getAppContext().getString(R.string.full_exam_description_3)), Task("$weekId-analysis-5", PlanDataSource.getAppContext().getString(R.string.exam_analysis), PlanDataSource.getAppContext().getString(R.string.exam_analysis_description_3)))),
            DayPlan(PlanDataSource.getAppContext().getString(R.string.saturday), listOf(Task("$weekId-exam-6", PlanDataSource.getAppContext().getString(R.string.full_exam), PlanDataSource.getAppContext().getString(R.string.full_exam_description_3)), Task("$weekId-t12", PlanDataSource.getAppContext().getString(R.string.weekly_vocabulary_lesson), PlanDataSource.getAppContext().getString(R.string.weekly_vocabulary_review_description)))),
            DayPlan(PlanDataSource.getAppContext().getString(R.string.sunday), listOf(Task("$weekId-t13", PlanDataSource.getAppContext().getString(R.string.general_repeat), PlanDataSource.getAppContext().getString(R.string.general_review_description)), Task("$weekId-t14", PlanDataSource.getAppContext().getString(R.string.strategy_motivation), PlanDataSource.getAppContext().getString(R.string.strategy_motivation_description))))
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
        add(createRedBookFoundationWeek(8, "113-115", PlanDataSource.getAppContext().getString(R.string.general_review_1_50), PlanDataSource.getAppContext().getString(R.string.general_review_51_115), PlanDataSource.getAppContext().getString(R.string.weak_topic_analysis)))


        // --- 2. FAZ: MAVİ KİTAP (B1-B2 GELİŞİMİ) --- (10 Hafta)
        val blueBook = PlanDataSource.getAppContext().getString(R.string.blue_book_full)
        val blueBookTopics = listOf(
            PlanDataSource.getAppContext().getString(R.string.tenses_review_topic), PlanDataSource.getAppContext().getString(R.string.future_in_detail_topic),
            PlanDataSource.getAppContext().getString(R.string.modals_1_topic), PlanDataSource.getAppContext().getString(R.string.modals_2_topic),
            PlanDataSource.getAppContext().getString(R.string.conditionals_wish_topic), PlanDataSource.getAppContext().getString(R.string.passive_voice_topic),
            PlanDataSource.getAppContext().getString(R.string.reported_speech_topic), PlanDataSource.getAppContext().getString(R.string.noun_clauses_topic),
            PlanDataSource.getAppContext().getString(R.string.gerunds_infinitives_topic), PlanDataSource.getAppContext().getString(R.string.conjunctions_connectors_topic)
        )
        blueBookTopics.forEachIndexed { index, topic ->
            val weekNumber = index + 9 // 8 hafta bitti, 9. haftadan başlıyoruz
            val nextTopic = if (index + 1 < blueBookTopics.size) blueBookTopics[index + 1] else PlanDataSource.getAppContext().getString(R.string.green_book_advanced_tenses)
            add(createAdvancedPreparationWeek(weekNumber, "B1-B2 Gelişimi", blueBook, topic, PlanDataSource.getAppContext().getString(R.string.advanced_tenses),
                PlanDataSource.getAppContext().getString(R.string.sentence_completion)))
        }

        // --- 3. FAZ: YEŞİL KİTAP (C1 USTALIĞI) --- (8 Hafta)
        val greenBook = PlanDataSource.getAppContext().getString(R.string.green_book)
        val greenBookTopics = listOf(
            PlanDataSource.getAppContext().getString(R.string.advanced_tense_nuances_topic), PlanDataSource.getAppContext().getString(R.string.inversion_emphasis_topic),
            PlanDataSource.getAppContext().getString(R.string.advanced_modals_topic), PlanDataSource.getAppContext().getString(R.string.participle_clauses_topic),
            PlanDataSource.getAppContext().getString(R.string.advanced_connectors_topic), PlanDataSource.getAppContext().getString(R.string.hypothetical_meaning_topic),
            PlanDataSource.getAppContext().getString(R.string.adjectives_adverbs_topic), PlanDataSource.getAppContext().getString(R.string.prepositions_phrasal_verbs_topic)
        )
        greenBookTopics.forEachIndexed { index, topic ->
            val weekNumber = index + 19 // 8+10=18 hafta bitti, 19. haftadan başlıyoruz
            val nextTopic = if (index + 1 < greenBookTopics.size) greenBookTopics[index + 1] else getAppContext().getString(R.string.final_review_exam_camp)
            add(createAdvancedPreparationWeek(weekNumber, "C1 Ustalığı", greenBook, topic, PlanDataSource.getAppContext().getString(R.string.final_review_exam_camp),
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
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = Constants.DATASTORE_NAME)

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
            .setContentTitle(PlanDataSource.getAppContext().getString(R.string.study_time_title))
            .setContentText(PlanDataSource.getAppContext().getString(R.string.study_time_message))
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
                val title = PlanDataSource.getAppContext().getString(R.string.exam_reminder_title)
                val message = if (today == exam.applicationStart) {
                    PlanDataSource.getAppContext().getString(R.string.exam_applications_started, exam.name)
                } else {
                    PlanDataSource.getAppContext().getString(R.string.exam_last_day_applications, exam.name)
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

    fun updateLocale(context: Context, language: String): Context {
        val locale = java.util.Locale(language)
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

        enableEdgeToEdge()
        setContent {
            StudyPlanTheme {
                val repository = remember { ProgressRepository(this@MainActivity.dataStore) }
                val hasSeenWelcome by repository.hasSeenWelcomeFlow.collectAsState(initial = false)
                val coroutineScope = rememberCoroutineScope()

                if (hasSeenWelcome) {
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
                } else {
                    WelcomeScreen(
                        onGetStarted = {
                            coroutineScope.launch {
                                repository.setHasSeenWelcome(true)
                            }
                        }
                    )
                }
            }
        }
    }
}

fun scheduleDailyReminder(context: Context) {
    val hour = Constants.NOTIFICATION_REMINDER_HOUR
    val minute = Constants.NOTIFICATION_REMINDER_MINUTE

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

    // Use OneTimeWorkRequest for initial delay, then PeriodicWorkRequest for daily repeats
    val initialWorkRequest = androidx.work.OneTimeWorkRequest.Builder(ReminderWorker::class.java)
        .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
        .addTag(Constants.DAILY_REMINDER_WORK + "_initial")
        .build()

    val periodicWorkRequest = androidx.work.PeriodicWorkRequest.Builder(
        ReminderWorker::class.java,
        Constants.REMINDER_INTERVAL_HOURS, // Removed .toLong()
        TimeUnit.HOURS
    )
        .addTag(Constants.DAILY_REMINDER_WORK)
        .build()

    val workManager = WorkManager.getInstance(context)
    workManager.enqueue(initialWorkRequest)
    workManager.enqueueUniquePeriodicWork(Constants.DAILY_REMINDER_WORK, ExistingPeriodicWorkPolicy.REPLACE, periodicWorkRequest)
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

    // Optimize: Pre-calculate all tasks
    val allTasks = remember { PlanDataSource.planData.flatMap { it.days }.flatMap { it.tasks } }
    val totalTasks = remember(allTasks) { allTasks.size }

    // Optimize: Use derivedStateOf for progress calculation
    val progress by remember {
        derivedStateOf { // Removed <Float>
            if (totalTasks > 0 && userProgress != null) {
                userProgress!!.completedTasks.size.toFloat() / totalTasks
            } else 0f
        }
    }
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
                    items(
                        items = PlanDataSource.planData,
                        key = { it.week }
                    ) { weekPlan ->
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
fun CountdownItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, days: Long, isExpired: Boolean, color: Color) {
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
fun GamificationHeader(streakCount: Int, achievementsCount: Int, onAchievementsClick: () -> Unit) {
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
        Box(modifier = Modifier.clickable { onAchievementsClick() }) {
            InfoChip(icon = Icons.Default.WorkspacePremium, label = achievementsLabel, value = "$achievementsCount", iconColor = MaterialTheme.colorScheme.tertiary)
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
fun WeekCard(weekPlan: WeekPlan, completedTasks: Set<String>, onToggleTask: (String) -> Unit) {
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
                    Text(tasksCompleted.format(completedInWeek, weekTasks.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = PlanDataSource.getAppContext().getString(R.string.expand_collapse), modifier = Modifier.rotate(rotationAngle))
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
    val context = LocalContext.current
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
            text = dayText,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
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