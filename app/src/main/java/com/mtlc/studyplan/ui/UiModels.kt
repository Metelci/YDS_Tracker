package com.mtlc.studyplan.ui

import java.time.LocalDateTime

// UI models for different features
data class PassageUi(
    val id: String,
    val title: String,
    val content: String,
    val difficulty: Int = 1,
    val readingTimeMinutes: Int = 5,
    val source: String = "Practice"
)

data class MockResultUi(
    val examId: String,
    val totalQuestions: Int,
    val correctAnswers: Int,
    val totalTimeMinutes: Int,
    val categoryResults: Map<String, CategoryResult>,
    val completedAt: LocalDateTime
) {
    val accuracy: Float get() = correctAnswers.toFloat() / totalQuestions
}

data class CategoryResult(
    val category: String,
    val correct: Int,
    val total: Int,
    val averageTimePerQuestion: Float
) {
    val accuracy: Float get() = correct.toFloat() / total
}

// Placeholder screens for navigation
data class ScreenConfig(
    val title: String,
    val showBackButton: Boolean = true,
    val showActions: Boolean = false
)