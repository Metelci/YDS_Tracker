package com.mtlc.studyplan.data

data class PracticeCategoryStat(
    val category: String,
    val total: Int,
    val answered: Int,
    val correct: Int,
    val accuracy: Float
)

data class PracticeSessionSummary(
    val sessionId: String,
    val timestamp: Long,
    val minutes: Int,
    val total: Int,
    val answered: Int,
    val correct: Int,
    val accuracy: Float,
    val perCategory: List<PracticeCategoryStat>
)

