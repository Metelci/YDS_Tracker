package com.mtlc.studyplan.questions

import android.content.Context
import com.mtlc.studyplan.data.VocabularyItem
import com.mtlc.studyplan.data.VocabularyProgress
import com.mtlc.studyplan.data.ProgressRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class VocabularyManager(
    private val context: Context,
    private val progressRepository: ProgressRepository
) {
    private val json by lazy { Json { ignoreUnknownKeys = true } }
    private val cacheFlow = MutableStateFlow<List<VocabularyItem>>(emptyList())
    @Volatile private var loaded = false

    suspend fun getAll(): List<VocabularyItem> {
        ensureLoaded()
        return applyProgress(cacheFlow.value)
    }

    fun flowAll(): Flow<List<VocabularyItem>> = cacheFlow

    suspend fun getByDifficulty(range: IntRange): List<VocabularyItem> = getAll().filter { it.difficulty in range }

    suspend fun getByWeeks(weeks: IntRange): List<VocabularyItem> = getAll().filter { it.weekIntroduced in weeks }

    suspend fun findByWords(words: Collection<String>): List<VocabularyItem> {
        val set = words.map { it.lowercase() }.toSet()
        return getAll().filter { it.word.lowercase() in set }
    }

    suspend fun suggestDistractors(target: VocabularyItem, max: Int): List<String> {
        val pool = getAll()
        val sameDiff = pool.filter { it.difficulty == target.difficulty && it.word != target.word }
        val related = (target.relatedWords + target.contexts.joinToString(" ") { it })
            .flatMap { token ->
                pool.filter { p -> p.word != target.word && (p.relatedWords.any { it.contains(token, true) } || p.definition.contains(token, true)) }
            }
        val combined = (related + sameDiff).distinctBy { it.word }.map { it.word }
        return combined.take(max)
    }

    private suspend fun ensureLoaded() {
        if (loaded && cacheFlow.value.isNotEmpty()) return
        val loadedItems = withContext(Dispatchers.IO) { loadFromAssets() }
        cacheFlow.update { loadedItems }
        loaded = true
    }

    private fun loadFromAssets(): List<VocabularyItem> {
        return runCatching {
            context.assets.open("vocabulary_database.json").bufferedReader().use { reader ->
                val text = reader.readText()
                json.decodeFromString<List<VocabularyItem>>(text)
            }
        }.getOrElse { emptyList() }
    }

    private suspend fun applyProgress(items: List<VocabularyItem>): List<VocabularyItem> {
        val progress = progressRepository.vocabularyProgressFlow.first().associateBy { it.wordId }
        return items.map { v ->
            val p: VocabularyProgress? = progress[v.word]
            if (p != null) v.copy(
                masteryLevel = p.masteryLevel,
                lastEncountered = p.lastReviewDate,
                errorCount = p.errorCount,
                successRate = p.successRate
            ) else v
        }
    }
}
