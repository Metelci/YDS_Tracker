package com.mtlc.studyplan.questions

import android.content.Context
import com.mtlc.studyplan.analytics.AnalyticsEngine
import com.mtlc.studyplan.data.TaskLog
import com.mtlc.studyplan.data.VocabularyItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

class QuestionGenerator(
    private val context: Context,
    private val analyticsEngine: AnalyticsEngine,
    private val vocabularyManager: VocabularyManager,
    private val dataProvider: QuestionDataProvider,
    private val performanceTracker: RoomQuestionPerformanceTracker,
    private val difficultyManager: DifficultyManager = DifficultyManager(analyticsEngine),
    private val templates: List<QuestionTemplate> = QuestionTemplateLoader.load(context)
) {
    private val rng get() = Random(System.currentTimeMillis())

    suspend fun generatePersonalizedQuestions(count: Int, currentWeek: Int? = null): List<GeneratedQuestion> = withContext(Dispatchers.Default) {
        val logs = dataProvider.getTaskLogs()
        val vocabCache = vocabularyManager.getAll()
        val templateStats = performanceTracker.getTemplateStats()
        val weakCategories = getWeakCategories(logs)
        val weakTargetCount = (count * 0.6).toInt().coerceAtLeast(1)
        val generalTargetCount = (count - weakTargetCount).coerceAtLeast(0)

        val selected = mutableListOf<GeneratedQuestion>()

        // 60% weak areas, distributed across found weak categories
        if (weakCategories.isNotEmpty()) {
            val perCat = (weakTargetCount.toFloat() / weakCategories.size).coerceAtLeast(1f).toInt()
            weakCategories.forEach { cat ->
                selected += getWeakAreaQuestions(cat, perCat, currentWeek, vocabCache, templateStats)
            }
        }

        // If not enough, top-up with general review by progressive difficulty
        if (selected.size < count) {
            val recent = logs.takeLast(30)
            val mix = SkillCategory.values().toList().shuffled(rng)
            val need = count - selected.size
            val steps = difficultyManager.buildProgression(
                startDifficulty = 3,
                targetCount = need
            )
            val general = steps.mapIndexedNotNull { idx, diff ->
                val cat = mix[idx % mix.size]
                pickTemplateAndGenerate(cat, diff, currentWeek, vocabCache, templateStats)
            }
            selected += general
        }

        // De-duplicate by id and trim to requested count
        selected.distinctBy { it.id }.shuffled(rng).take(count)
    }

    suspend fun getWeakAreaQuestions(category: SkillCategory): List<GeneratedQuestion> = withContext(Dispatchers.Default) {
        val vocabCache = vocabularyManager.getAll()
        val templateStats = performanceTracker.getTemplateStats()
        getWeakAreaQuestions(category, count = 5, currentWeek = null, vocabCache = vocabCache, templateStats = templateStats)
    }

    private suspend fun getWeakAreaQuestions(category: SkillCategory, count: Int, currentWeek: Int?, vocabCache: List<VocabularyItem>, templateStats: Map<String, TemplateStats>): List<GeneratedQuestion> {
        val logs = dataProvider.getTaskLogs()
        val recent = logs.takeLast(30)
        val userLevel = difficultyManager.calculateOptimalDifficulty(category, recent)
        val diffs = difficultyManager.buildProgression(userLevel, count)
        return diffs.mapNotNull { d -> pickTemplateAndGenerate(category, d, currentWeek, vocabCache, templateStats) }
    }

    suspend fun createVocabularyQuestions(targetWords: List<VocabularyItem>): List<GeneratedQuestion> {
        return targetWords.mapNotNull { makeVocabQuestion(it) }
    }

    suspend fun generateForCategory(category: SkillCategory, count: Int, currentWeek: Int? = null): List<GeneratedQuestion> = withContext(Dispatchers.Default) {
        val logs = dataProvider.getTaskLogs()
        val recent = logs.takeLast(30)
        val userLevel = difficultyManager.calculateOptimalDifficulty(category, recent)
        val diffs = difficultyManager.buildProgression(userLevel, count)
        val vocabCache = vocabularyManager.getAll()
        val templateStats = performanceTracker.getTemplateStats()
        diffs.mapNotNull { d -> pickTemplateAndGenerate(category, d, currentWeek, vocabCache, templateStats) }
    }

    private fun pickTemplateAndGenerate(category: SkillCategory, difficulty: Int, currentWeek: Int?, vocabCache: List<VocabularyItem>, templateStats: Map<String, TemplateStats>): GeneratedQuestion? {
        val pool = templates.filter { t ->
            t.category == category && t.difficulty == difficulty && (currentWeek == null || currentWeek in t.weekAppropriate)
        }
        val template = if (pool.isNotEmpty()) {
            // Weight by template stats: prefer those around 40-80% accuracy to maximize learning value
            pool.maxByOrNull { t ->
                val s = templateStats[t.id]
                val acc = s?.accuracy ?: 0.6f
                // Score peaks near 0.6; deprioritize extremes
                1.0 - kotlin.math.abs(acc - 0.6)
            }
        } else {
            templates.filter { it.category == category }.minByOrNull { kotlin.math.abs(it.difficulty - difficulty) }
        }
        return template?.let { fillTemplate(it, vocabCache) }
    }

    private fun fillTemplate(t: QuestionTemplate, vocabCache: List<VocabularyItem>): GeneratedQuestion? {
        return when (t.category) {
            SkillCategory.VOCAB -> fillVocabTemplate(t, vocabCache)
            SkillCategory.GRAMMAR -> fillGrammarTemplate(t)
            SkillCategory.READING, SkillCategory.LISTENING -> fillComprehensionTemplate(t)
        }
    }

    private fun fillGrammarTemplate(t: QuestionTemplate): GeneratedQuestion? {
        // Grammar templates already include distractor patterns; randomize order but keep track of correct index
        val rawOptions = t.distractorPatterns
        if (rawOptions.isEmpty()) return null
        val shuffled = rawOptions.shuffled(rng)
        val correct = when (t.correctAnswerSlot) {
            in rawOptions.indices -> rawOptions[t.correctAnswerSlot]
            else -> rawOptions.first()
        }
        val correctIdx = shuffled.indexOf(correct).coerceAtLeast(0)
        return GeneratedQuestion(
            id = "${t.id}-${System.nanoTime()}",
            category = t.category,
            prompt = t.pattern,
            options = shuffled,
            correctIndex = correctIdx,
            difficulty = t.difficulty,
            explanation = t.explanation,
            grammarFocus = t.grammarFocus,
            vocabularyFocus = t.vocabularyFocus,
            sourceTemplateId = t.id
        )
    }

    private fun fillComprehensionTemplate(t: QuestionTemplate): GeneratedQuestion? {
        val rawOptions = t.distractorPatterns
        if (rawOptions.isEmpty()) return null
        val shuffled = rawOptions.shuffled(rng)
        val correct = when (t.correctAnswerSlot) {
            in rawOptions.indices -> rawOptions[t.correctAnswerSlot]
            else -> rawOptions.first()
        }
        val correctIdx = shuffled.indexOf(correct).coerceAtLeast(0)
        return GeneratedQuestion(
            id = "${t.id}-${System.nanoTime()}",
            category = t.category,
            prompt = t.pattern,
            options = shuffled,
            correctIndex = correctIdx,
            difficulty = t.difficulty,
            explanation = t.explanation,
            grammarFocus = t.grammarFocus,
            vocabularyFocus = t.vocabularyFocus,
            sourceTemplateId = t.id
        )
    }

    private fun fillVocabTemplate(t: QuestionTemplate, vocabCache: List<VocabularyItem>): GeneratedQuestion? {
        // If template has a vocabularyFocus word, keep as-is; else pick a mid-difficulty word
        val focusWord = t.vocabularyFocus.firstOrNull()
        val target: VocabularyItem? = when {
            focusWord != null -> vocabCache.firstOrNull { it.word.equals(focusWord, true) }
            else -> vocabCache.filter { it.difficulty in 2..4 }.randomOrNull()
        }
        val options = mutableListOf<String>()
        val correct = if (target != null) {
            // if pattern has blank for definition or usage, use the word as correct fill; otherwise use semantic choice
            target.word
        } else {
            // fallback to template-provided correct answer slot text
            t.distractorPatterns.getOrNull(t.correctAnswerSlot) ?: t.distractorPatterns.firstOrNull() ?: return null
        }
        options.add(correct)
        val distractors = if (target != null) suggestDistractorsFromCache(target, vocabCache, max = 3) else emptyList()
        options.addAll(distractors)
        if (options.size < 4) {
            // fill remaining from template patterns
            t.distractorPatterns.filter { it != correct }.take(4 - options.size).forEach { options.add(it) }
        }
        val shuffled = options.distinct().take(4).shuffled(rng)
        val correctIdx = shuffled.indexOf(correct).coerceAtLeast(0)
        val prompt = if (t.pattern.contains("____")) t.pattern else t.pattern.replace("__", "____")
        return GeneratedQuestion(
            id = "${t.id}-${System.nanoTime()}",
            category = t.category,
            prompt = prompt,
            options = shuffled,
            correctIndex = correctIdx,
            difficulty = t.difficulty,
            explanation = if (target != null && t.explanation.isBlank()) target.definition else t.explanation,
            grammarFocus = t.grammarFocus,
            vocabularyFocus = if (target != null) listOf(target.word) else t.vocabularyFocus,
            sourceTemplateId = t.id
        )
    }

    private fun suggestDistractorsFromCache(target: VocabularyItem, cache: List<VocabularyItem>, max: Int): List<String> {
        val sameDiff = cache.filter { it.difficulty == target.difficulty && !it.word.equals(target.word, true) }
        val relatedTokens = (target.relatedWords + target.contexts).flatMap { it.split(' ') }.map { it.lowercase() }.toSet()
        val related = cache.filter { p ->
            !p.word.equals(target.word, true) && (
                p.relatedWords.any { it.lowercase() in relatedTokens } ||
                relatedTokens.any { tok -> p.definition.lowercase().contains(tok) }
            )
        }
        return (related + sameDiff).distinctBy { it.word }.map { it.word }.take(max)
    }

    private suspend fun makeVocabQuestion(item: VocabularyItem): GeneratedQuestion? {
        val correct = item.definition
        val distractorWords = vocabularyManager.suggestDistractors(item, max = 6)
        val distractorDefs = vocabularyManager.findByWords(distractorWords).map { it.definition }
        val pool = (listOf(correct) + distractorDefs).distinct().take(4)
        if (pool.size < 2) return null
        val options = pool.shuffled(rng)
        val correctIdx = options.indexOf(correct).coerceAtLeast(0)
        val prompt = "The word '${item.word}' most nearly means _____."
        return GeneratedQuestion(
            id = "vocab-${item.word}-${System.nanoTime()}",
            category = SkillCategory.VOCAB,
            prompt = prompt,
            options = options,
            correctIndex = correctIdx,
            difficulty = item.difficulty.coerceIn(1, 5),
            explanation = item.definition,
            grammarFocus = null,
            vocabularyFocus = listOf(item.word),
            sourceTemplateId = null
        )
    }

    private fun getWeakCategories(logs: List<TaskLog>): List<SkillCategory> {
        if (logs.isEmpty()) return emptyList()
        val accByCat = logs.groupBy { it.category.lowercase() }
            .mapValues { (_, cls) -> cls.map { if (it.correct) 1f else 0f }.average().toFloat() }

        val mapping = mapOf(
            "grammar" to SkillCategory.GRAMMAR,
            "reading" to SkillCategory.READING,
            "listening" to SkillCategory.LISTENING,
            "vocab" to SkillCategory.VOCAB,
            "vocabulary" to SkillCategory.VOCAB,
            "kelime" to SkillCategory.VOCAB
        )

        return accByCat
            .filter { it.value < 0.7f }
            .keys
            .mapNotNull { k -> mapping.entries.firstOrNull { k.contains(it.key, true) }?.value }
            .distinct()
    }
}
