package com.mtlc.studyplan.reports.pdf

import com.mtlc.studyplan.analytics.Recommendation
import java.time.LocalDate

/**
 * Models for PDF report generation
 */

/**
 * Request object for generating a PDF report
 */
data class ReportRequest(
    val studentName: String?,
    val dateRange: ClosedRange<LocalDate>,
    val dailyLoads: List<UserDailyLoad>,
    val events: List<StudyEvent>,
    val skillMinutes: Map<Skill, Int>,
    val recommendations: List<Recommendation>
)

/**
 * Result object containing generated PDF data
 */
data class ReportResult(
    val bytes: ByteArray,
    val filename: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ReportResult

        if (!bytes.contentEquals(other.bytes)) return false
        if (filename != other.filename) return false

        return true
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + filename.hashCode()
        return result
    }
}

/**
 * Daily study load metrics
 */
data class UserDailyLoad(
    val date: LocalDate,
    val totalMinutes: Int,
    val tasksCompleted: Int,
    val averageAccuracy: Float,
    val skillBreakdown: Map<Skill, Int> = emptyMap(),
    val streakDayNumber: Int = 0
)

/**
 * Individual study event/session
 */
data class StudyEvent(
    val id: String,
    val timestamp: Long,
    val skill: Skill,
    val durationMinutes: Int,
    val accuracy: Float,
    val difficulty: EventDifficulty,
    val taskCount: Int = 1,
    val pointsEarned: Int = 0
)

/**
 * Skill categories for study tracking
 */
enum class Skill(val displayName: String, val color: Long) {
    GRAMMAR("Grammar", 0xFF2196F3),
    READING("Reading", 0xFF4CAF50),
    LISTENING("Listening", 0xFFFF9800),
    VOCABULARY("Vocabulary", 0xFF9C27B0),
    SPEAKING("Speaking", 0xFFF44336),
    WRITING("Writing", 0xFF607D8B),
    PRACTICE_EXAM("Practice Exam", 0xFF795548),
    OTHER("Other", 0xFF9E9E9E);

    companion object {
        fun fromString(skillName: String): Skill {
            return when (skillName.lowercase()) {
                "grammar", "gramer" -> GRAMMAR
                "reading", "okuma" -> READING
                "listening", "dinleme" -> LISTENING
                "vocabulary", "vocab", "kelime" -> VOCABULARY
                "speaking", "konuşma" -> SPEAKING
                "writing", "yazma" -> WRITING
                "exam", "practice", "mock", "sınav" -> PRACTICE_EXAM
                else -> OTHER
            }
        }
    }
}

/**
 * Event difficulty levels
 */
enum class EventDifficulty(val displayName: String, val multiplier: Float) {
    BEGINNER("Beginner", 0.8f),
    INTERMEDIATE("Intermediate", 1.0f),
    ADVANCED("Advanced", 1.2f),
    EXPERT("Expert", 1.5f)
}

/**
 * Summary statistics for report generation
 */
data class ReportStats(
    val totalMinutes: Int,
    val totalTasks: Int,
    val streakLength: Int,
    val averageAccuracy: Float,
    val completionRate: Float,
    val mostStudiedSkill: Skill?,
    val leastStudiedSkill: Skill?,
    val bestPerformingSkill: Skill?,
    val weakestSkill: Skill?,
    val dailyAverage: Float,
    val weeklyTrend: Float
)

/**
 * Chart data for visual representations in PDF
 */
data class ChartData(
    val labels: List<String>,
    val values: List<Float>,
    val colors: List<Long> = emptyList(),
    val title: String = "",
    val yAxisLabel: String = "",
    val maxValue: Float = values.maxOrNull() ?: 1f
)

/**
 * Insight item for recommendations section
 */
data class InsightItem(
    val title: String,
    val description: String,
    val type: InsightType,
    val priority: Int = 0, // 0 = highest
    val value: String = "",
    val trend: String = ""
)

/**
 * Types of insights for categorization
 */
enum class InsightType(val displayName: String, val icon: String) {
    STRENGTH("Strength", "💪"),
    WEAKNESS("Area for Improvement", "🎯"),
    TREND("Trend Analysis", "📈"),
    RECOMMENDATION("Recommendation", "💡"),
    ACHIEVEMENT("Achievement", "🏆")
}

/**
 * PDF generation configuration
 */
data class PdfConfig(
    val pageWidth: Float = 595f, // A4 width in points
    val pageHeight: Float = 842f, // A4 height in points
    val margin: Float = 72f, // 1 inch margins
    val primaryColor: Int = 0xFF2196F3.toInt(),
    val secondaryColor: Int = 0xFF4CAF50.toInt(),
    val textColor: Int = 0xFF212121.toInt(),
    val lightGrayColor: Int = 0xFFF5F5F5.toInt(),
    val titleSize: Float = 24f,
    val headingSize: Float = 18f,
    val bodySize: Float = 12f,
    val captionSize: Float = 10f
)

/**
 * QR code data for app download link
 */
data class QrData(
    val content: String,
    val size: Float = 100f,
    val errorCorrectionLevel: String = "M"
) {
    companion object {
        fun createAppQr(): QrData {
            return QrData(
                content = "https://play.google.com/store/apps/details?id=com.mtlc.studyplan",
                size = 80f
            )
        }
    }
}