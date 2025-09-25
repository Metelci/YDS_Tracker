package com.mtlc.studyplan.storage.room

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "practice_sessions",
    indices = [Index("timestamp")]
)
data class PracticeSessionEntity(
    @PrimaryKey val sessionId: String,
    val timestamp: Long,
    val minutes: Int,
    val total: Int,
    val answered: Int,
    val correct: Int,
    val accuracy: Float
)

@Entity(
    tableName = "practice_category_stats",
    indices = [Index("sessionId"), Index("category")]
)
data class PracticeCategoryStatEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val category: String,
    val total: Int,
    val answered: Int,
    val correct: Int,
    val accuracy: Float
)

@Entity(
    tableName = "question_performance",
    indices = [Index("timestamp"), Index("category"), Index("templateId")]
)
data class QuestionPerformanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val questionId: String,
    val templateId: String?,
    val timestamp: Long,
    val category: String,
    val difficulty: Int,
    val wasCorrect: Boolean,
    val responseTimeMs: Long
)

@Entity(
    tableName = "task_logs",
    indices = [Index("timestampMillis"), Index("category")]
)
data class TaskLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: String,
    val timestampMillis: Long,
    val minutesSpent: Int,
    val correct: Boolean,
    val category: String,
    val pointsEarned: Int
)

@Entity(tableName = "vocab_progress")
data class VocabProgressEntity(
    @PrimaryKey val wordId: String,
    val masteryLevel: Float,
    val lastReviewDate: Long,
    val nextReviewDate: Long,
    val reviewCount: Int,
    val successCount: Int,
    val errorCount: Int,
    val currentInterval: Int
)

@Entity(
    tableName = "vocab_sessions",
    indices = [Index("startTime")]
)
data class VocabSessionEntity(
    @PrimaryKey val sessionId: String,
    val vocabularyItemsCsv: String,
    val startTime: Long,
    val endTime: Long?,
    val correctAnswers: Int,
    val totalQuestions: Int,
    val sessionType: String
)

@Entity(
    tableName = "reading_sessions",
    indices = [Index("startTime"), Index("contentId")]
)
data class ReadingSessionEntity(
    @PrimaryKey val sessionId: String,
    val contentId: String,
    val startTime: Long,
    val endTime: Long? = null,
    val currentPosition: Int = 0,
    val pauseTimestampsCsv: String = "", // Comma-separated pause timestamps
    val comprehensionAnswersCsv: String = "", // JSON string of answers
    val vocabularyLookupsCsv: String = "", // Comma-separated words
    val notes: String? = null,
    val isCompleted: Boolean = false
)

@Entity(
    tableName = "reading_speed_data",
    indices = [Index("timestamp"), Index("contentId")]
)
data class ReadingSpeedDataEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contentId: String,
    val wordsPerMinute: Int,
    val comprehensionAccuracy: Float,
    val completionTime: Long,
    val pauseCount: Int = 0,
    val rereadSectionsCsv: String = "", // Comma-separated sections
    val timestamp: Long
)

@Entity(
    tableName = "reading_performance_metrics",
    indices = [Index("userId"), Index("lastUpdated")]
)
data class ReadingPerformanceMetricsEntity(
    @PrimaryKey val userId: String,
    val averageWPM: Int,
    val averageComprehension: Float,
    val preferredReadingLength: Int,
    val strongTopicsCsv: String = "", // Comma-separated topics
    val challengingTopicsCsv: String = "", // Comma-separated topics
    val optimalReadingTimesJson: String = "", // JSON string of TimeSlot objects
    val readingEndurance: Int,
    val lastUpdated: Long
)

@Entity(
    tableName = "content_effectiveness",
    indices = [Index("contentId"), Index("lastEvaluated")]
)
data class ContentEffectivenessEntity(
    @PrimaryKey val contentId: String,
    val engagementScore: Float,
    val learningOutcome: Float,
    val userRating: Int? = null,
    val completionRate: Float,
    val averageComprehensionScore: Float,
    val vocabularyRetentionRate: Float,
    val lastEvaluated: Long
)
