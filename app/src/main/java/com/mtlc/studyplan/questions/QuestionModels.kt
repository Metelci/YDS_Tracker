package com.mtlc.studyplan.questions

import com.mtlc.studyplan.data.TaskLog
import com.mtlc.studyplan.data.VocabularyItem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Core models for local question generation
 */

enum class SkillCategory {
    GRAMMAR, READING, LISTENING, VOCAB
}

@Serializable
data class QuestionTemplate(
    val id: String,
    val category: SkillCategory, // Grammar, Reading, Listening, Vocab
    val difficulty: Int, // 1-5 scale matching current week progression
    val pattern: String, // Template with slots: "The committee's decision was ____ after hours of debate."
    @SerialName("correctAnswerSlot") val correctAnswerSlot: Int,
    val distractorPatterns: List<String> = emptyList(),
    val grammarFocus: String? = null, // "conditionals", "tenses", "modals"
    val vocabularyFocus: List<String> = emptyList(), // Target words for this template
    val explanation: String = "",
    // IntRange isn't trivially serializable; use startWeek/endWeek for JSON and map at load time
    val startWeek: Int = 1,
    val endWeek: Int = 30
) {
    @kotlinx.serialization.Transient
    val weekAppropriate: IntRange = startWeek..endWeek
}

data class GeneratedQuestion(
    val id: String,
    val category: SkillCategory,
    val prompt: String,
    val options: List<String>,
    val correctIndex: Int,
    val difficulty: Int,
    val explanation: String,
    val grammarFocus: String? = null,
    val vocabularyFocus: List<String> = emptyList(),
    val sourceTemplateId: String? = null
)

/**
 * Performance metrics for adaptive refinement
 */
data class QuestionPerformance(
    val questionId: String,
    val templateId: String?,
    val timestamp: Long,
    val category: SkillCategory,
    val difficulty: Int,
    val wasCorrect: Boolean,
    val responseTimeMs: Long
)

data class TemplateStats(
    val templateId: String,
    val timesServed: Int = 0,
    val timesCorrect: Int = 0,
    val averageTimeMs: Long = 0L
) {
    val accuracy: Float = if (timesServed > 0) timesCorrect.toFloat() / timesServed else 0f
}

/**
 * Captures common mistake patterns for targeted distractor generation
 */
data class ErrorPattern(
    val category: SkillCategory,
    val grammarFocus: String? = null,
    val pattern: String, // e.g., "subject_verb_disagreement", "wrong_tense_past_instead_of_present_perfect"
    val frequency: Int = 0
)

/**
 * A simple interface abstraction to request data needed for generation
 */
interface QuestionDataProvider {
    suspend fun getTaskLogs(): List<TaskLog>
    suspend fun getVocabulary(): List<VocabularyItem>
}

