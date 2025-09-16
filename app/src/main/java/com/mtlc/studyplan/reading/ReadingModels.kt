package com.mtlc.studyplan.reading

import com.mtlc.studyplan.questions.SkillCategory
import kotlinx.serialization.Serializable

/**
 * Reading Content Curation System Models
 * Phase 3: Personalized Reading Materials
 */

@Serializable
enum class ReadingLevel(val displayName: String, val numericLevel: Int) {
    A2("A2 - Elementary", 1),
    B1("B1 - Intermediate", 2),
    B1_PLUS("B1+ - Upper Intermediate", 3),
    B2("B2 - Upper Intermediate", 4),
    B2_PLUS("B2+ - Advanced", 5),
    C1("C1 - Proficient", 6);
    
    companion object {
        fun fromWeek(week: Int): ReadingLevel = when {
            week <= 5 -> A2
            week <= 10 -> B1
            week <= 15 -> B1_PLUS
            week <= 22 -> B2
            week <= 28 -> B2_PLUS
            else -> C1
        }
    }
}

@Serializable
data class ReadingContent(
    val id: String,
    val title: String,
    val content: String,
    val difficulty: ReadingLevel,
    val estimatedTime: Int, // minutes
    val topics: List<String>, // "technology", "health", "business", "culture"
    val vocabularyFocus: List<String>, // Key words featured
    val grammarPatterns: List<String>, // Grammar structures highlighted
    val wordCount: Int,
    val averageSentenceLength: Float,
    val complexityScore: Float, // Calculated complexity metric (0.0 - 1.0)
    val weekAppropriate: IntRange, // Suitable for which weeks
    val comprehensionQuestions: List<String>? = null, // Optional follow-up questions
    val culturalContext: String? = null, // Cultural appropriateness note
    val sourceType: ContentSourceType = ContentSourceType.CURATED
) {
    val readabilityLevel: Float = calculateReadabilityLevel()
    val isAdvanced: Boolean = difficulty >= ReadingLevel.B2
    
    private fun calculateReadabilityLevel(): Float {
        // Flesch Reading Ease approximation
        val avgWordsPerSentence = wordCount.toFloat() / content.count { it == '.' || it == '!' || it == '?' }
        val avgSyllablesPerWord = estimateAverageSyllables()
        
        return (206.835f - (1.015f * avgWordsPerSentence) - (84.6f * avgSyllablesPerWord))
            .coerceIn(0f, 100f) / 100f
    }
    
    private fun estimateAverageSyllables(): Float {
        // Simple syllable estimation based on vowel clusters
        val vowels = "aeiouAEIOU"
        val wordCount = content.split(Regex("\\s+")).size
        var syllableCount = 0
        
        content.split(Regex("\\s+")).forEach { word ->
            var syllables = 0
            var prevWasVowel = false
            
            word.forEach { char ->
                if (char in vowels) {
                    if (!prevWasVowel) syllables++
                    prevWasVowel = true
                } else {
                    prevWasVowel = false
                }
            }
            
            syllableCount += maxOf(1, syllables)
        }
        
        return if (wordCount > 0) syllableCount.toFloat() / wordCount else 1f
    }
}

@Serializable
enum class ContentSourceType(val displayName: String) {
    NEWS_ARTICLE("News Article"),
    ACADEMIC_TEXT("Academic Text"), 
    BUSINESS_COMMUNICATION("Business Communication"),
    CULTURAL_TEXT("Cultural Text"),
    EXAM_PASSAGE("Exam Practice Passage"),
    CURATED("Curated Content")
}

/**
 * Time slot preferences for reading recommendations
 */
@Serializable
data class TimeSlot(
    val startHour: Int, // 24-hour format
    val endHour: Int,
    val preferredContentTypes: List<ContentSourceType> = emptyList(),
    val optimalReadingLength: Int = 5 // minutes
)

/**
 * Reading performance tracking
 */
@Serializable
data class ReadingSpeedData(
    val contentId: String,
    val wordsPerMinute: Int,
    val comprehensionAccuracy: Float, // 0.0 - 1.0
    val completionTime: Long, // milliseconds
    val pauseCount: Int = 0,
    val rereadSections: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class ReadingPerformanceMetrics(
    val userId: String,
    val averageWPM: Int,
    val averageComprehension: Float,
    val preferredReadingLength: Int, // minutes
    val strongTopics: List<String>,
    val challengingTopics: List<String>,
    val optimalReadingTimes: List<TimeSlot>,
    val readingEndurance: Int, // maximum productive reading session length in minutes
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Content recommendation context
 */
data class ReadingRecommendationContext(
    val availableTime: Int, // minutes
    val currentWeek: Int,
    val weakAreas: List<SkillCategory>,
    val vocabularyMastery: Map<String, Float>,
    val recentTopics: List<String>,
    val preferredDifficulty: ReadingLevel?,
    val timeOfDay: Int, // hour of day
    val sessionType: ReadingSessionType = ReadingSessionType.GENERAL_PRACTICE
)

@Serializable
enum class ReadingSessionType(val displayName: String) {
    GENERAL_PRACTICE("General Practice"),
    VOCABULARY_FOCUS("Vocabulary Building"),
    SPEED_READING("Speed Reading"),
    COMPREHENSION_FOCUS("Comprehension Focus"),
    EXAM_PREPARATION("Exam Preparation"),
    TOPIC_EXPLORATION("Topic Exploration")
}

/**
 * Content effectiveness tracking
 */
@Serializable
data class ContentEffectiveness(
    val contentId: String,
    val engagementScore: Float, // based on completion rates and time spent
    val learningOutcome: Float, // based on subsequent performance
    val userRating: Int?, // 1-5 star rating
    val completionRate: Float, // percentage of users who complete it
    val averageComprehensionScore: Float,
    val vocabularyRetentionRate: Float, // how well vocabulary from this content is retained
    val lastEvaluated: Long = System.currentTimeMillis()
)

/**
 * Content sequencing and curriculum alignment
 */
@Serializable
data class ContentSequence(
    val sequenceId: String,
    val title: String,
    val description: String,
    val contentIds: List<String>,
    val targetWeeks: IntRange,
    val skillFocus: List<SkillCategory>,
    val difficultyProgression: List<ReadingLevel>,
    val estimatedCompletionTime: Int // total minutes for sequence
)

/**
 * Reading session state and progress
 */
@Serializable
data class ReadingSession(
    val sessionId: String,
    val contentId: String,
    val startTime: Long,
    val endTime: Long? = null,
    val currentPosition: Int = 0, // character position in content
    val pauseTimestamps: List<Long> = emptyList(),
    val comprehensionAnswers: Map<Int, String> = emptyMap(), // question index to answer
    val vocabularyLookups: List<String> = emptyList(), // words looked up during reading
    val notes: String? = null,
    val isCompleted: Boolean = false
) {
    val duration: Long = (endTime ?: System.currentTimeMillis()) - startTime
    val effectiveReadingTime: Long = calculateEffectiveReadingTime()
    
    private fun calculateEffectiveReadingTime(): Long {
        if (pauseTimestamps.size < 2) return duration
        
        var totalPauseTime = 0L
        for (i in 0 until pauseTimestamps.size step 2) {
            val pauseStart = pauseTimestamps.getOrNull(i) ?: continue
            val pauseEnd = pauseTimestamps.getOrNull(i + 1) ?: System.currentTimeMillis()
            totalPauseTime += pauseEnd - pauseStart
        }
        
        return duration - totalPauseTime
    }
}

/**
 * Topic rotation and variety management
 */
@Serializable
data class TopicRotationState(
    val recentTopics: Map<String, Long>, // topic to last encounter timestamp
    val topicFrequency: Map<String, Int>, // topic to frequency count
    val avoidanceList: List<String> = emptyList(), // topics to temporarily avoid
    val preferredTopics: List<String> = emptyList(), // user-preferred topics
    val lastRotationUpdate: Long = System.currentTimeMillis()
) {
    fun getTopicWeight(topic: String, currentTime: Long = System.currentTimeMillis()): Float {
        val lastEncounter = recentTopics[topic] ?: 0L
        val daysSinceLastEncounter = (currentTime - lastEncounter) / (24 * 60 * 60 * 1000)
        val frequency = topicFrequency[topic] ?: 0
        
        // Higher weight for topics not seen recently, lower weight for overused topics
        return when {
            topic in avoidanceList -> 0.1f
            topic in preferredTopics -> 1.5f
            daysSinceLastEncounter > 7 -> 1.2f
            daysSinceLastEncounter > 3 -> 1.0f
            frequency > 5 -> 0.6f
            else -> 0.8f
        }
    }
}