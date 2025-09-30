package com.mtlc.studyplan.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.mtlc.studyplan.database.Converters

@Entity(
    tableName = "questions",
    indices = [
        Index(value = ["examType", "difficulty"], name = "idx_questions_exam_difficulty"),
        Index(value = ["questionType"], name = "idx_questions_type"),
        Index(value = ["difficulty"], name = "idx_questions_difficulty"),
        Index(value = ["isDownloaded"], name = "idx_questions_downloaded"),
        Index(value = ["examType", "questionType"], name = "idx_questions_exam_type")
    ]
)
@TypeConverters(Converters::class)
data class QuestionEntity(
    @PrimaryKey
    val id: String,
    val examType: ExamType,
    val questionType: QuestionType,
    val difficulty: Difficulty,
    val questionText: String,
    val options: List<String>,
    val correctAnswer: Int,
    val explanation: String,
    val tags: List<String>,
    val isDownloaded: Boolean = false,
    val lastAccessed: Long = System.currentTimeMillis()
)

enum class ExamType {
    YDS, YOKDIL, KPDS, UDS
}

enum class QuestionType {
    GRAMMAR, READING, VOCABULARY, LISTENING
}

enum class Difficulty {
    A1, A2, B1, B2, C1, C2
}