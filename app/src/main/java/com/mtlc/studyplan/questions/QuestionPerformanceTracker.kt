package com.mtlc.studyplan.questions

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class QuestionPerformanceTracker(
    private val dataStore: DataStore<Preferences>
) {
    private val json = Json { ignoreUnknownKeys = true }

    private object Keys {
        val QUESTION_PERF = stringSetPreferencesKey("question_performance_records")
        val TEMPLATE_STATS = stringSetPreferencesKey("question_template_stats")
    }

    fun recordResult(result: QuestionPerformance) {
        runBlocking {
            dataStore.edit { prefs ->
                val perfRaw = prefs[Keys.QUESTION_PERF] ?: emptySet()
                val updated = (perfRaw + json.encodeToString(result)).takeLast(1000).toSet()
                prefs[Keys.QUESTION_PERF] = updated

                val statsRaw = prefs[Keys.TEMPLATE_STATS] ?: emptySet()
                val statsMap = statsRaw.mapNotNull { runCatching { json.decodeFromString<TemplateStats>(it) }.getOrNull() }
                    .associateBy { it.templateId }
                    .toMutableMap()

                val templateId = result.templateId
                if (templateId != null) {
                    val prev = statsMap[templateId]
                    val newTimesServed = (prev?.timesServed ?: 0) + 1
                    val newTimesCorrect = (prev?.timesCorrect ?: 0) + if (result.wasCorrect) 1 else 0
                    val prevAvg = prev?.averageTimeMs ?: 0L
                    val newAvg = if (prev == null || prev.timesServed == 0) result.responseTimeMs
                    else ((prevAvg * prev.timesServed) + result.responseTimeMs) / (prev.timesServed + 1)
                    statsMap[templateId] = TemplateStats(
                        templateId = templateId,
                        timesServed = newTimesServed,
                        timesCorrect = newTimesCorrect,
                        averageTimeMs = newAvg
                    )
                }

                prefs[Keys.TEMPLATE_STATS] = statsMap.values.map { json.encodeToString(it) }.toSet()
            }
        }
    }

    fun getTemplateStats(): Map<String, TemplateStats> = runBlocking {
        val raw = dataStore.data.first()[Keys.TEMPLATE_STATS] ?: emptySet()
        raw.mapNotNull { runCatching { json.decodeFromString<TemplateStats>(it) }.getOrNull() }
            .associateBy { it.templateId }
    }
}

