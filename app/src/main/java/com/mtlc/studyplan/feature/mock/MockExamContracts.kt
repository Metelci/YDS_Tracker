package com.mtlc.studyplan.feature.mock

import androidx.compose.runtime.Immutable

@Immutable
data class MockQuestion(
    val id: Int,
    val section: String, // e.g., Reading, Grammar, Vocabulary
    val text: String,
    val options: List<String>,
    val correctIndex: Int
)

@Immutable
data class MockExamState(
    val questions: List<MockQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val remainingSeconds: Int = 180 * 60,
    val selections: Map<Int, Int> = emptyMap(), // questionId -> chosenIndex
    val markedForReview: Set<Int> = emptySet(), // questionId
    val isSubmitting: Boolean = false
)

sealed interface MockIntent {
    data object Load : MockIntent
    data class Jump(val index: Int) : MockIntent
    data class Select(val questionId: Int, val optionIndex: Int) : MockIntent
    data class ToggleReview(val questionId: Int) : MockIntent
    data object Tick : MockIntent
    data object Submit : MockIntent
}

object MockSampleData {
    fun questions(): List<MockQuestion> = (1..80).map { i ->
        val section = when {
            i <= 40 -> "Reading"
            i <= 60 -> "Grammar"
            else -> "Vocabulary"
        }
        MockQuestion(
            id = i,
            section = section,
            text = "Q$i: Placeholder question text for $section.",
            options = listOf("Option A", "Option B", "Option C", "Option D"),
            correctIndex = (i % 4)
        )
    }
}

