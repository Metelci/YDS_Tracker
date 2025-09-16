package com.mtlc.studyplan.feature.questions

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mtlc.studyplan.analytics.AnalyticsEngine
import com.mtlc.studyplan.data.ProgressRepository
import com.mtlc.studyplan.data.TaskLog
import com.mtlc.studyplan.data.PracticeSessionSummary
import com.mtlc.studyplan.data.PracticeCategoryStat
import com.mtlc.studyplan.storage.room.PracticeSessionStore
import com.mtlc.studyplan.data.dataStore
import com.mtlc.studyplan.questions.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first

data class QuestionUI(
    val data: GeneratedQuestion,
    val selectedIndex: Int? = null,
    val checked: Boolean = false,
    val wasCorrect: Boolean? = null,
    val startedAt: Long = System.currentTimeMillis()
)

class QuestionsViewModel(app: Application) : AndroidViewModel(app) {
    private val context: Context get() = getApplication()

    // Dependencies
    private val progressRepository = ProgressRepository(context.dataStore)
    private val generator by lazy { QuestionService.buildGenerator(context, progressRepository, context.dataStore) }
    private val performanceTracker by lazy { RoomQuestionPerformanceTracker(context) }
    private val analyticsEngine by lazy { AnalyticsEngine() }
    private val practiceStore by lazy { com.mtlc.studyplan.storage.room.PracticeSessionStore(context) }

    private val _questions = MutableStateFlow<List<QuestionUI>>(emptyList())
    val questions: StateFlow<List<QuestionUI>> = _questions

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _category = MutableStateFlow<SkillCategory?>(null) // null = personalized
    val category: StateFlow<SkillCategory?> = _category

    private val _sessionStart = MutableStateFlow(0L)
    private val _headerCategoryAccuracy = MutableStateFlow<Map<SkillCategory, Float>>(emptyMap())
    val headerCategoryAccuracy: StateFlow<Map<SkillCategory, Float>> = _headerCategoryAccuracy

    data class SessionSummary(val total: Int, val answered: Int, val correct: Int, val accuracy: Float, val minutes: Int)

    fun setCategory(cat: SkillCategory?) { _category.value = cat }

    fun generate(count: Int = 10, currentWeek: Int? = null) {
        _loading.value = true
        viewModelScope.launch {
            _sessionStart.value = System.currentTimeMillis()
            val list = withContext(Dispatchers.Default) {
                val cat = _category.value
                when (cat) {
                    null -> generator.generatePersonalizedQuestions(count, currentWeek)
                    else -> generator.generateForCategory(cat, count, currentWeek)
                }
            }
            _questions.value = list.map { QuestionUI(data = it) }
            _loading.value = false
            refreshHeaderAnalytics()
        }
    }

    fun select(questionId: String, index: Int) {
        _questions.value = _questions.value.map { q ->
            if (q.data.id == questionId) q.copy(selectedIndex = index) else q
        }
    }

    fun check(questionId: String) {
        val q = _questions.value.firstOrNull { it.data.id == questionId } ?: return
        val sel = q.selectedIndex ?: return
        val correct = sel == q.data.correctIndex
        val elapsed = System.currentTimeMillis() - q.startedAt

        viewModelScope.launch {
            performanceTracker.recordResult(
                QuestionPerformance(
                    questionId = q.data.id,
                    templateId = q.data.sourceTemplateId,
                    timestamp = System.currentTimeMillis(),
                    category = q.data.category,
                    difficulty = q.data.difficulty,
                    wasCorrect = correct,
                    responseTimeMs = elapsed
                )
            )
        }

        _questions.value = _questions.value.map { item ->
            if (item.data.id == questionId) item.copy(checked = true, wasCorrect = correct) else item
        }
    }

    private suspend fun refreshHeaderAnalytics() {
        val logs = progressRepository.taskLogsFlow.first()
        val data = analyticsEngine.generateAnalytics(days = 30, taskLogs = logs, userProgress = null)
        val map = data.studyPatterns.categoryPerformance
        val bySkill: MutableMap<SkillCategory, Float> = mutableMapOf()
        map.forEach { (k, v) ->
            when {
                k.contains("grammar", true) -> bySkill[SkillCategory.GRAMMAR] = v
                k.contains("reading", true) -> bySkill[SkillCategory.READING] = v
                k.contains("listen", true) -> bySkill[SkillCategory.LISTENING] = v
                k.contains("vocab", true) || k.contains("kelime", true) || k.contains("vocabulary", true) -> bySkill[SkillCategory.VOCAB] = v
            }
        }
        _headerCategoryAccuracy.value = bySkill
    }

    fun buildSummary(): SessionSummary {
        val qs = _questions.value
        val answered = qs.count { it.selectedIndex != null }
        val correct = qs.count { it.wasCorrect == true || (it.selectedIndex != null && it.selectedIndex == it.data.correctIndex && it.checked) }
        val total = qs.size
        val minutes = ((System.currentTimeMillis() - (_sessionStart.value.takeIf { it>0 } ?: System.currentTimeMillis())) / 60000.0).toInt().coerceAtLeast(1)
        val acc = if (total>0) correct.toFloat()/total.toFloat() else 0f
        return SessionSummary(total = total, answered = answered, correct = correct, accuracy = acc, minutes = minutes)
    }

    fun submitAll(logToProgress: Boolean) {
        val summary = buildSummary()
        viewModelScope.launch {
            // Persist detailed per-category session summary
            val perCat = buildPerCategoryStats()
            practiceStore.save(
                PracticeSessionSummary(
                    sessionId = "practice-${System.currentTimeMillis()}",
                    timestamp = System.currentTimeMillis(),
                    minutes = summary.minutes,
                    total = summary.total,
                    answered = summary.answered,
                    correct = summary.correct,
                    accuracy = summary.accuracy,
                    perCategory = perCat
                )
            )

            if (logToProgress) {
                progressRepository.addTaskLog(
                    TaskLog(
                        taskId = "questions-${System.currentTimeMillis()}",
                        timestampMillis = System.currentTimeMillis(),
                        minutesSpent = summary.minutes,
                        correct = summary.accuracy >= 0.7f,
                        category = "practice"
                    )
                )
            }
            refreshHeaderAnalytics()
        }
    }

    private fun buildPerCategoryStats(): List<PracticeCategoryStat> {
        val groups = _questions.value.groupBy { it.data.category }
        return groups.map { (cat, items) ->
            val total = items.size
            val answered = items.count { it.selectedIndex != null }
            val correct = items.count { it.wasCorrect == true || (it.selectedIndex != null && it.selectedIndex == it.data.correctIndex && it.checked) }
            val acc = if (total > 0) correct.toFloat()/total.toFloat() else 0f
            PracticeCategoryStat(
                category = cat.name.lowercase(),
                total = total,
                answered = answered,
                correct = correct,
                accuracy = acc
            )
        }
    }
}
