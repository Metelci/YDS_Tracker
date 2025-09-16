package com.mtlc.studyplan.smartcontent

import com.mtlc.studyplan.data.VocabularyItem
import com.mtlc.studyplan.questions.GeneratedQuestion
import com.mtlc.studyplan.questions.SkillCategory
import com.mtlc.studyplan.reading.ReadingContent
import com.mtlc.studyplan.reading.TimeSlot
import kotlinx.serialization.Serializable
import kotlinx.serialization.Contextual

/**
 * Unified Smart Content System Models
 * Phase 4: Comprehensive Integration
 */

enum class ContentType {
    VOCABULARY, QUESTIONS, READING, MIXED
}

enum class SessionType {
    WARMUP, FOCUSED_PRACTICE, COMPREHENSIVE_REVIEW, EXAM_PREPARATION, SKILL_BUILDING
}

enum class CurveType {
    GRADUAL, STEEP, PLATEAU, ACCELERATED
}

@Serializable
data class DailyContentPack(
    val id: String,
    val date: Long,
    val vocabulary: List<VocabularyItem>,
    val questions: List<@Contextual GeneratedQuestion>,
    val reading: @Contextual ReadingContent?,
    val recommendedSequence: List<ContentType>,
    val estimatedTotalTime: Int,
    val focusAreas: List<SkillCategory>,
    val difficultyLevel: Float,
    val confidenceScore: Float
)

@Serializable
data class StudySession(
    val id: String,
    val sessionType: SessionType,
    val warmupVocabulary: List<VocabularyItem>,
    val mainContent: @Contextual MainContent,
    val reinforcementQuestions: List<@Contextual GeneratedQuestion>,
    val cooldownVocabulary: List<VocabularyItem>,
    val sessionGoals: List<String>,
    val estimatedDuration: Int,
    val optimalStartTime: TimeSlot?,
    val difficultyCurve: CurveType
)

sealed class MainContent {
    data class ReadingSession(val content: ReadingContent) : MainContent()
    data class QuestionSet(val questions: List<GeneratedQuestion>) : MainContent()
    data class MixedContent(val reading: ReadingContent?, val questions: List<GeneratedQuestion>) : MainContent()
}

@Serializable
data class ContentRecommendation(
    val contentId: String,
    val contentType: ContentType,
    val title: String,
    val description: String,
    val estimatedTime: Int,
    val difficulty: Float,
    val relevanceScore: Float,
    val reason: String,
    val skillFocus: List<SkillCategory>
)

@Serializable
data class ContentPerformance(
    val contentId: String,
    val contentType: ContentType,
    val accuracy: Float,
    val timeSpent: Long,
    val completionRate: Float,
    val userEngagement: Float,
    val timestamp: Long,
    val difficulty: Float,
    val skillCategory: SkillCategory?
)

@Serializable
data class UserLearningProfile(
    val userId: String,
    val learningSpeed: Map<SkillCategory, Float>,
    val retentionRate: Map<ContentType, Float>,
    val preferredDifficultyCurve: CurveType,
    val optimalSessionLength: Int,
    val peakPerformanceTimes: List<TimeSlot>,
    val weaknessPatterns: List<WeaknessPattern>,
    val interestAreas: List<String>,
    val cognitiveCapacity: Float,
    val motivationLevel: Float,
    val lastUpdated: Long
)

@Serializable
data class WeaknessPattern(
    val skillCategory: SkillCategory,
    val patternType: PatternType,
    val severity: Float,
    val frequency: Int,
    val lastOccurrence: Long
)

enum class PatternType {
    CONSISTENT_LOW_PERFORMANCE,
    TIME_PRESSURE_WEAKNESS,
    SPECIFIC_TOPIC_STRUGGLE,
    FATIGUE_RELATED_DECLINE,
    CONCEPTUAL_MISUNDERSTANDING
}

@Serializable
data class LearningPrediction(
    val contentId: String,
    val predictedAccuracy: Float,
    val predictedTime: Long,
    val confidenceLevel: Float,
    val riskFactors: List<String>,
    val recommendedAdjustments: List<String>
)

@Serializable
data class OptimalContentMix(
    val vocabularyRatio: Float,
    val questionsRatio: Float,
    val readingRatio: Float,
    val totalTime: Int,
    val difficultyDistribution: Map<Float, Float>,
    val skillBalance: Map<SkillCategory, Float>
)

@Serializable
data class PlateauArea(
    val skillCategory: SkillCategory,
    val plateauDuration: Long,
    val currentLevel: Float,
    val breakthroughStrategies: List<String>,
    val recommendedActions: List<String>
)

@Serializable
data class PathAdjustment(
    val adjustmentType: AdjustmentType,
    val targetSkill: SkillCategory?,
    val description: String,
    val expectedImpact: Float,
    val implementationDifficulty: Float
)

enum class AdjustmentType {
    DIFFICULTY_INCREASE,
    DIFFICULTY_DECREASE,
    FOCUS_SHIFT,
    PACE_ADJUSTMENT,
    METHOD_CHANGE,
    BREAK_INCREASE
}

@Serializable
data class LearningGoal(
    val skillCategory: SkillCategory,
    val targetLevel: Float,
    val timeframe: Long,
    val priority: Int,
    val currentProgress: Float
)

@Serializable
data class ContentEffectivenessScore(
    val contentId: String,
    val overallScore: Float,
    val learningImpact: Float,
    val userEngagement: Float,
    val retentionRate: Float,
    val difficultyAppropriateness: Float,
    val lastEvaluated: Long,
    val evaluationCount: Int
)

@Serializable
data class SystemEvolutionMetrics(
    val algorithmVersion: String,
    val performanceImprovement: Float,
    val userSatisfactionTrend: Float,
    val contentQualityScore: Float,
    val personalizationAccuracy: Float,
    val lastUpdated: Long
)
