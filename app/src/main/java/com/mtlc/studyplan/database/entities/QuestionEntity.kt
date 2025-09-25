package com.mtlc.studyplan.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.mtlc.studyplan.database.Converters

@Entity(tableName = "questions")
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