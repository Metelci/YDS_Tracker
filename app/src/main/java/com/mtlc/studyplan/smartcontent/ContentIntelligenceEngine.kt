package com.mtlc.studyplan.smartcontent

import com.mtlc.studyplan.data.ProgressRepository
import com.mtlc.studyplan.data.TaskLog
import com.mtlc.studyplan.data.UserProgress
import com.mtlc.studyplan.questions.SkillCategory
import com.mtlc.studyplan.reading.TimeSlot
import kotlinx.coroutines.flow.first
import kotlin.math.min
import kotlin.math.max
import kotlin.math.abs
import kotlin.math.pow

/**
 * Advanced Intelligence Layer for Predictive Content Optimization
 * Implements machine learning-style algorithms for personalized learning
 */
class ContentIntelligenceEngine(
    private val progressRepository: ProgressRepository
) {
    private val performanceHistory = mutableListOf<ContentPerformance>()
    private val contentEffectiveness = mutableMapOf<String, ContentEffectivenessScore>()

    /**
     * Predict learning outcomes for specific content
     */
    suspend fun predictLearningOutcomes(
        content: Any,
        userProfile: UserLearningProfile
    ): LearningPrediction {
        val contentId = extractContentId(content)
        val historicalPerformance = getHistoricalPerformance(contentId)
        val userFactors = analyzeUserFactors(userProfile)

        val predictedAccuracy = calculatePredictedAccuracy(historicalPerformance, userFactors)
        val predictedTime = calculatePredictedTime(content, userProfile)
        val confidenceLevel = calculateConfidenceLevel(historicalPerformance.size, userFactors.consistency)
        val riskFactors = identifyRiskFactors(content, userProfile, historicalPerformance)
        val recommendedAdjustments = generateAdjustments(content, predictedAccuracy, userProfile)

        return LearningPrediction(
            contentId = contentId,
            predictedAccuracy = predictedAccuracy,
            predictedTime = predictedTime,
            confidenceLevel = confidenceLevel,
            riskFactors = riskFactors,
            recommendedAdjustments = recommendedAdjustments
        )
    }

    /**
     * Optimize content mix for available time and learning goals
     */
    suspend fun optimizeContentMix(
        availableTime: Int,
        learningGoals: List<LearningGoal>
    ): OptimalContentMix {
        val userProfile = buildUserLearningProfile("current_user")

        // Calculate optimal ratios based on learning goals and user profile
        val vocabularyRatio = calculateVocabularyRatio(learningGoals, userProfile)
        val questionsRatio = calculateQuestionsRatio(learningGoals, userProfile)
        val readingRatio = calculateReadingRatio(learningGoals, userProfile)

        // Normalize ratios to ensure they sum to 1.0
        val totalRatio = vocabularyRatio + questionsRatio + readingRatio
        val normalizedVocabRatio = vocabularyRatio / totalRatio
        val normalizedQuestionsRatio = questionsRatio / totalRatio
        val normalizedReadingRatio = readingRatio / totalRatio

        // Calculate difficulty distribution
        val difficultyDistribution = calculateDifficultyDistribution(
            availableTime, learningGoals, userProfile
        )

        // Calculate skill balance
        val skillBalance = calculateSkillBalance(learningGoals, userProfile)

        return OptimalContentMix(
            vocabularyRatio = normalizedVocabRatio,
            questionsRatio = normalizedQuestionsRatio,
            readingRatio = normalizedReadingRatio,
            totalTime = availableTime,
            difficultyDistribution = difficultyDistribution,
            skillBalance = skillBalance
        )
    }

    /**
     * Identify learning plateaus that need intervention
     */
    suspend fun identifyLearningPlateaus(): List<PlateauArea> {
        val userProfile = buildUserLearningProfile("current_user")
        val taskLogs = progressRepository.taskLogsFlow.first()
        val plateaus = mutableListOf<PlateauArea>()

        // Analyze each skill category for plateau patterns
        SkillCategory.values().forEach { skill ->
            val skillLogs = taskLogs.filter { log ->
                log.category.contains(skill.name, ignoreCase = true)
            }.sortedBy { it.timestampMillis }

            if (skillLogs.size >= 10) { // Need sufficient data
                val plateau = detectPlateau(skill, skillLogs, userProfile)
                if (plateau != null) {
                    plateaus.add(plateau)
                }
            }
        }

        return plateaus
    }

    /**
     * Suggest learning path adjustments based on performance analysis
     */
    suspend fun suggestLearningPathAdjustments(): List<PathAdjustment> {
        val userProfile = buildUserLearningProfile("current_user")
        val plateaus = identifyLearningPlateaus()
        val adjustments = mutableListOf<PathAdjustment>()

        // Generate adjustments for plateaus
        plateaus.forEach { plateau ->
            adjustments.addAll(generatePlateauAdjustments(plateau, userProfile))
        }

        // Generate proactive adjustments based on learning patterns
        adjustments.addAll(generateProactiveAdjustments(userProfile))

        return adjustments.sortedByDescending { it.expectedImpact }
    }

    /**
     * Build comprehensive user learning profile
     */
    suspend fun buildUserLearningProfile(userId: String): UserLearningProfile {
        val userProgress = progressRepository.userProgressFlow.first()
        val taskLogs = progressRepository.taskLogsFlow.first()
        val vocabularyProgress = progressRepository.vocabularyProgressFlow.first()

        // Calculate learning speed for each skill category
        val learningSpeed = calculateLearningSpeed(taskLogs)

        // Calculate retention rates for different content types
        val retentionRate = calculateRetentionRates(taskLogs)

        // Determine preferred difficulty curve
        val preferredDifficultyCurve = determineDifficultyCurve(taskLogs)

        // Calculate optimal session length
        val optimalSessionLength = calculateOptimalSessionLength(taskLogs)

        // Identify peak performance times
        val peakPerformanceTimes = identifyPeakPerformanceTimes(taskLogs)

        // Analyze weakness patterns
        val weaknessPatterns = analyzeWeaknessPatterns(taskLogs)

        // Identify interest areas
        val interestAreas = identifyInterestAreas(taskLogs, vocabularyProgress)

        // Calculate cognitive capacity and motivation
        val cognitiveCapacity = calculateCognitiveCapacity(taskLogs)
        val motivationLevel = calculateMotivationLevel(userProgress, taskLogs)

        return UserLearningProfile(
            userId = userId,
            learningSpeed = learningSpeed,
            retentionRate = retentionRate,
            preferredDifficultyCurve = preferredDifficultyCurve,
            optimalSessionLength = optimalSessionLength,
            peakPerformanceTimes = peakPerformanceTimes,
            weaknessPatterns = weaknessPatterns,
            interestAreas = interestAreas,
            cognitiveCapacity = cognitiveCapacity,
            motivationLevel = motivationLevel,
            lastUpdated = System.currentTimeMillis()
        )
    }

    /**
     * Update performance data for continuous learning
     */
    fun updatePerformanceData(performance: ContentPerformance) {
        performanceHistory.add(performance)

        // Keep only recent performance data (last 1000 entries)
        if (performanceHistory.size > 1000) {
            performanceHistory.removeAt(0)
        }
    }

    /**
     * Update content effectiveness scores
     */
    fun updateContentEffectiveness(contentId: String, performance: ContentPerformance) {
        val existingScore = contentEffectiveness[contentId] ?: ContentEffectivenessScore(
            contentId = contentId,
            overallScore = 0f,
            learningImpact = 0f,
            userEngagement = 0f,
            retentionRate = 0f,
            difficultyAppropriateness = 0f,
            lastEvaluated = System.currentTimeMillis(),
            evaluationCount = 0
        )

        // Calculate new scores using weighted moving average
        val weight = 1f / (existingScore.evaluationCount + 1)
        val newOverallScore = existingScore.overallScore * (1 - weight) + performance.accuracy * weight
        val newLearningImpact = existingScore.learningImpact * (1 - weight) + performance.accuracy * performance.skillCategory?.let { 1f } ?: 0.8f * weight
        val newUserEngagement = existingScore.userEngagement * (1 - weight) + performance.userEngagement * weight
        val newRetentionRate = existingScore.retentionRate * (1 - weight) + calculateRetentionRate(performance) * weight
        val newDifficultyAppropriateness = existingScore.difficultyAppropriateness * (1 - weight) + calculateDifficultyAppropriateness(performance) * weight

        contentEffectiveness[contentId] = existingScore.copy(
            overallScore = newOverallScore,
            learningImpact = newLearningImpact,
            userEngagement = newUserEngagement,
            retentionRate = newRetentionRate,
            difficultyAppropriateness = newDifficultyAppropriateness,
            lastEvaluated = System.currentTimeMillis(),
            evaluationCount = existingScore.evaluationCount + 1
        )
    }

    /**
     * Adapt system parameters based on performance feedback
     */
    fun adaptSystemParameters(performance: ContentPerformance) {
        // This would implement online learning algorithms to adjust:
        // - Recommendation weights
        // - Difficulty calculation parameters
        // - Content sequencing algorithms
        // - User profile parameters

        // For now, this is a placeholder for future ML implementation
    }

    // Private helper methods

    private fun extractContentId(content: Any): String {
        return when (content) {
            is com.mtlc.studyplan.data.VocabularyItem -> content.word
            is com.mtlc.studyplan.questions.GeneratedQuestion -> content.id
            is com.mtlc.studyplan.reading.ReadingContent -> content.id
            else -> "unknown_${System.currentTimeMillis()}"
        }
    }

    private fun getHistoricalPerformance(contentId: String): List<ContentPerformance> {
        return performanceHistory.filter { it.contentId == contentId }
    }

    private data class UserFactors(
        val skillLevel: Float,
        val consistency: Float,
        val fatigueLevel: Float,
        val motivationLevel: Float
    )

    private suspend fun analyzeUserFactors(userProfile: UserLearningProfile): UserFactors {
        val recentLogs = progressRepository.taskLogsFlow.first()
            .filter { System.currentTimeMillis() - it.timestampMillis < 7 * 24 * 60 * 60 * 1000 }

        val skillLevel = userProfile.learningSpeed.values.average()
        val consistency = calculateConsistency(recentLogs)
        val fatigueLevel = calculateFatigueLevel(recentLogs)
        val motivationLevel = userProfile.motivationLevel

        return UserFactors(skillLevel, consistency, fatigueLevel, motivationLevel)
    }

    private fun calculatePredictedAccuracy(
        historicalPerformance: List<ContentPerformance>,
        userFactors: UserFactors
    ): Float {
        if (historicalPerformance.isEmpty()) {
            // Base prediction on user factors
            return userFactors.skillLevel * (1 - userFactors.fatigueLevel * 0.3f) * userFactors.motivationLevel
        }

        // Use historical performance with user factor adjustments
        val historicalAverage = historicalPerformance.map { it.accuracy }.average()
        val adjustmentFactor = (userFactors.skillLevel - 0.5f) * 0.2f +
                              (userFactors.consistency - 0.5f) * 0.1f -
                              userFactors.fatigueLevel * 0.15f +
                              (userFactors.motivationLevel - 0.5f) * 0.1f

        return (historicalAverage + adjustmentFactor).coerceIn(0.1f, 0.95f)
    }

    private fun calculatePredictedTime(content: Any, userProfile: UserLearningProfile): Long {
        val baseTime = when (content) {
            is com.mtlc.studyplan.data.VocabularyItem -> 120000L // 2 minutes
            is com.mtlc.studyplan.questions.GeneratedQuestion -> 90000L // 1.5 minutes
            is com.mtlc.studyplan.reading.ReadingContent -> content.estimatedTime * 60000L
            else -> 180000L // 3 minutes default
        }

        // Adjust based on user profile
        val speedFactor = userProfile.learningSpeed.values.average()
        val adjustment = (1 - speedFactor) * 0.3f // Faster learners get up to 30% time reduction

        return (baseTime * (1 - adjustment)).toLong()
    }

    private fun calculateConfidenceLevel(sampleSize: Int, consistency: Float): Float {
        // Confidence increases with sample size and consistency
        val sizeFactor = min(sampleSize / 10f, 1f)
        return (sizeFactor * 0.7f + consistency * 0.3f).coerceIn(0.1f, 0.95f)
    }

    private fun identifyRiskFactors(
        content: Any,
        userProfile: UserLearningProfile,
        historicalPerformance: List<ContentPerformance>
    ): List<String> {
        val risks = mutableListOf<String>()

        // Difficulty mismatch
        val contentDifficulty = extractDifficulty(content)
        val userSkillLevel = userProfile.learningSpeed.values.average() * 5
        if (abs(contentDifficulty - userSkillLevel) > 2) {
            risks.add("Difficulty level may be ${if (contentDifficulty > userSkillLevel) "too high" else "too low"}")
        }

        // Low historical performance
        if (historicalPerformance.isNotEmpty()) {
            val avgPerformance = historicalPerformance.map { it.accuracy }.average()
            if (avgPerformance < 0.6f) {
                risks.add("Historical performance suggests difficulty with similar content")
            }
        }

        // Fatigue risk
        if (userProfile.cognitiveCapacity < 0.7f) {
            risks.add("User may be experiencing cognitive fatigue")
        }

        // Time pressure
        val predictedTime = calculatePredictedTime(content, userProfile)
        if (predictedTime > userProfile.optimalSessionLength * 60000L) {
            risks.add("Content may exceed optimal session length")
        }

        return risks
    }

    private fun generateAdjustments(
        content: Any,
        predictedAccuracy: Float,
        userProfile: UserLearningProfile
    ): List<String> {
        val adjustments = mutableListOf<String>()

        if (predictedAccuracy < 0.6f) {
            adjustments.add("Consider reducing difficulty or breaking into smaller segments")
        }

        if (predictedAccuracy > 0.9f) {
            adjustments.add("Consider increasing difficulty for better challenge")
        }

        val contentTime = calculatePredictedTime(content, userProfile)
        if (contentTime > 20 * 60 * 1000) { // 20 minutes
            adjustments.add("Consider splitting into multiple sessions")
        }

        return adjustments
    }

    private fun calculateVocabularyRatio(learningGoals: List<LearningGoal>, userProfile: UserLearningProfile): Float {
        val vocabGoals = learningGoals.count { it.skillCategory == SkillCategory.VOCAB }
        val vocabWeakness = userProfile.weaknessPatterns.count { it.skillCategory == SkillCategory.VOCAB }
        return (vocabGoals + vocabWeakness).toFloat() / max(learningGoals.size, 1)
    }

    private fun calculateQuestionsRatio(learningGoals: List<LearningGoal>, userProfile: UserLearningProfile): Float {
        val questionGoals = learningGoals.count { it.skillCategory != SkillCategory.VOCAB }
        val questionWeakness = userProfile.weaknessPatterns.count { it.skillCategory != SkillCategory.VOCAB }
        return (questionGoals + questionWeakness).toFloat() / max(learningGoals.size, 1)
    }

    private fun calculateReadingRatio(learningGoals: List<LearningGoal>, userProfile: UserLearningProfile): Float {
        val readingGoals = learningGoals.count { it.skillCategory == SkillCategory.READING }
        val readingWeakness = userProfile.weaknessPatterns.count { it.skillCategory == SkillCategory.READING }
        return (readingGoals + readingWeakness).toFloat() / max(learningGoals.size, 1)
    }

    private fun calculateDifficultyDistribution(
        availableTime: Int,
        learningGoals: List<LearningGoal>,
        userProfile: UserLearningProfile
    ): Map<Float, Float> {
        // Create a balanced difficulty distribution
        val distribution = mutableMapOf<Float, Float>()
        val difficultyLevels = listOf(1f, 2f, 3f, 4f, 5f)

        difficultyLevels.forEach { difficulty ->
            val weight = when {
                difficulty <= 2 -> 0.2f // Easy content
                difficulty == 3 -> 0.4f // Medium content (most common)
                difficulty >= 4 -> 0.4f // Challenging content
                else -> 0.1f
            }
            distribution[difficulty] = weight
        }

        return distribution
    }

    private fun calculateSkillBalance(learningGoals: List<LearningGoal>, userProfile: UserLearningProfile): Map<SkillCategory, Float> {
        val balance = mutableMapOf<SkillCategory, Float>()

        SkillCategory.values().forEach { skill ->
            val goalWeight = learningGoals.count { it.skillCategory == skill }.toFloat()
            val weaknessWeight = userProfile.weaknessPatterns.count { it.skillCategory == skill }.toFloat()
            val totalWeight = goalWeight + weaknessWeight + 1f // Base weight of 1
            balance[skill] = totalWeight / (learningGoals.size + userProfile.weaknessPatterns.size + SkillCategory.values().size).toFloat()
        }

        return balance
    }

    private suspend fun detectPlateau(
        skill: SkillCategory,
        skillLogs: List<TaskLog>,
        userProfile: UserLearningProfile
    ): PlateauArea? {
        // Analyze performance trend over last 20 sessions
        val recentLogs = skillLogs.takeLast(20)
        if (recentLogs.size < 10) return null

        val recentAccuracy = recentLogs.takeLast(10).count { it.correct }.toFloat() / 10
        val earlierAccuracy = recentLogs.take(10).count { it.correct }.toFloat() / 10

        // Check for plateau (less than 5% improvement over 10 sessions)
        val improvement = recentAccuracy - earlierAccuracy
        if (improvement < 0.05f && recentAccuracy < 0.8f) {
            val plateauDuration = calculatePlateauDuration(skillLogs)

            return PlateauArea(
                skillCategory = skill,
                plateauDuration = plateauDuration,
                currentLevel = recentAccuracy,
                breakthroughStrategies = generateBreakthroughStrategies(skill),
                recommendedActions = generatePlateauActions(skill, userProfile)
            )
        }

        return null
    }

    private fun calculatePlateauDuration(skillLogs: List<TaskLog>): Long {
        // Calculate how long performance has been stagnant
        val recentLogs = skillLogs.takeLast(20)
        val currentAccuracy = recentLogs.takeLast(5).count { it.correct }.toFloat() / 5

        var duration = 0L
        for (i in recentLogs.size - 6 downTo 0) {
            val windowAccuracy = recentLogs.drop(i).take(5).count { it.correct }.toFloat() / 5
            if (abs(windowAccuracy - currentAccuracy) < 0.05f) {
                duration += 5 // 5 sessions
            } else {
                break
            }
        }

        return duration
    }

    private fun generateBreakthroughStrategies(skill: SkillCategory): List<String> {
        return when (skill) {
            SkillCategory.GRAMMAR -> listOf(
                "Focus on one grammar pattern at a time",
                "Use spaced repetition for grammar rules",
                "Practice with real-world examples"
            )
            SkillCategory.READING -> listOf(
                "Read shorter passages with full comprehension focus",
                "Practice prediction strategies",
                "Use vocabulary pre-teaching"
            )
            SkillCategory.LISTENING -> listOf(
                "Start with slower, clearer audio",
                "Use transcripts for support",
                "Practice with familiar topics first"
            )
            SkillCategory.VOCAB -> listOf(
                "Connect new words to known concepts",
                "Use memory techniques (mnemonics)",
                "Practice in context, not isolation"
            )
        }
    }

    private fun generatePlateauActions(skill: SkillCategory, userProfile: UserLearningProfile): List<String> {
        val actions = mutableListOf<String>()

        actions.add("Take a short break from ${skill.name} practice")
        actions.add("Review foundational concepts in ${skill.name}")
        actions.add("Try a different practice format for ${skill.name}")

        if (userProfile.optimalSessionLength > 20) {
            actions.add("Shorten ${skill.name} practice sessions")
        }

        return actions
    }

    private fun generatePlateauAdjustments(plateau: PlateauArea, userProfile: UserLearningProfile): List<PathAdjustment> {
        val adjustments = mutableListOf<PathAdjustment>()

        // Difficulty adjustment
        if (plateau.currentLevel < 0.6f) {
            adjustments.add(PathAdjustment(
                adjustmentType = AdjustmentType.DIFFICULTY_DECREASE,
                targetSkill = plateau.skillCategory,
                description = "Reduce difficulty in ${plateau.skillCategory.name} to build confidence",
                expectedImpact = 0.7f,
                implementationDifficulty = 0.3f
            ))
        }

        // Method change
        adjustments.add(PathAdjustment(
            adjustmentType = AdjustmentType.METHOD_CHANGE,
            targetSkill = plateau.skillCategory,
            description = "Try different practice methods for ${plateau.skillCategory.name}",
            expectedImpact = 0.6f,
            implementationDifficulty = 0.5f
        ))

        // Break increase
        if (plateau.plateauDuration > 10) {
            adjustments.add(PathAdjustment(
                adjustmentType = AdjustmentType.BREAK_INCREASE,
                targetSkill = plateau.skillCategory,
                description = "Increase breaks during ${plateau.skillCategory.name} practice",
                expectedImpact = 0.5f,
                implementationDifficulty = 0.2f
            ))
        }

        return adjustments
    }

    private fun generateProactiveAdjustments(userProfile: UserLearningProfile): List<PathAdjustment> {
        val adjustments = mutableListOf<PathAdjustment>()

        // Check for over-focus on one area
        val skillBalance = userProfile.learningSpeed
        val mostAdvanced = skillBalance.maxByOrNull { it.value }
        val leastAdvanced = skillBalance.minByOrNull { it.value }

        if (mostAdvanced != null && leastAdvanced != null) {
            val gap = mostAdvanced.value - leastAdvanced.value
            if (gap > 0.3f) {
                adjustments.add(PathAdjustment(
                    adjustmentType = AdjustmentType.FOCUS_SHIFT,
                    targetSkill = leastAdvanced.key,
                    description = "Increase focus on ${leastAdvanced.key.name} to balance skills",
                    expectedImpact = 0.8f,
                    implementationDifficulty = 0.4f
                ))
            }
        }

        // Check session length optimization
        if (userProfile.optimalSessionLength > 30) {
            adjustments.add(PathAdjustment(
                adjustmentType = AdjustmentType.PACE_ADJUSTMENT,
                description = "Consider shorter, more frequent sessions",
                expectedImpact = 0.6f,
                implementationDifficulty = 0.3f
            ))
        }

        return adjustments
    }

    private fun calculateLearningSpeed(taskLogs: List<TaskLog>): Map<SkillCategory, Float> {
        val speedMap = mutableMapOf<SkillCategory, Float>()

        SkillCategory.values().forEach { skill ->
            val skillLogs = taskLogs.filter { log ->
                log.category.contains(skill.name, ignoreCase = true)
            }

            if (skillLogs.isNotEmpty()) {
                // Calculate average accuracy as proxy for learning speed
                val accuracy = skillLogs.count { it.correct }.toFloat() / skillLogs.size
                speedMap[skill] = accuracy
            } else {
                speedMap[skill] = 0.5f // Default
            }
        }

        return speedMap
    }

    private fun calculateRetentionRates(taskLogs: List<TaskLog>): Map<ContentType, Float> {
        // Simplified retention calculation
        val retentionMap = mutableMapOf<ContentType, Float>()

        ContentType.values().forEach { contentType ->
            // This would require more sophisticated tracking
            // For now, use a default based on task performance
            val relevantLogs = taskLogs.filter { log ->
                when (contentType) {
                    ContentType.VOCABULARY -> log.category.contains("vocab", ignoreCase = true)
                    ContentType.QUESTIONS -> !log.category.contains("reading", ignoreCase = true) && !log.category.contains("vocab", ignoreCase = true)
                    ContentType.READING -> log.category.contains("reading", ignoreCase = true)
                    ContentType.MIXED -> true
                }
            }

            val retention = if (relevantLogs.size > 5) {
                // Calculate consistency as proxy for retention
                val accuracies = relevantLogs.map { if (it.correct) 1f else 0f }
                val mean = accuracies.average()
                val variance = accuracies.map { (it - mean).pow(2) }.average()
                1f - variance.toFloat() // Lower variance = better retention
            } else {
                0.7f // Default
            }

            retentionMap[contentType] = retention
        }

        return retentionMap
    }

    private fun determineDifficultyCurve(taskLogs: List<TaskLog>): CurveType {
        if (taskLogs.size < 10) return CurveType.GRADUAL

        // Analyze performance progression
        val sortedLogs = taskLogs.sortedBy { it.timestampMillis }
        val firstHalf = sortedLogs.take(sortedLogs.size / 2)
        val secondHalf = sortedLogs.drop(sortedLogs.size / 2)

        val firstHalfAccuracy = firstHalf.count { it.correct }.toFloat() / firstHalf.size
        val secondHalfAccuracy = secondHalf.count { it.correct }.toFloat() / secondHalf.size

        val improvement = secondHalfAccuracy - firstHalfAccuracy

        return when {
            improvement > 0.2f -> CurveType.ACCELERATED
            improvement > 0.1f -> CurveType.GRADUAL
            improvement > -0.1f -> CurveType.PLATEAU
            else -> CurveType.STEEP
        }
    }

    private fun calculateOptimalSessionLength(taskLogs: List<TaskLog>): Int {
        // Analyze session lengths and performance correlation
        val sessionLengths = taskLogs.map { it.minutesSpent }
        val averageLength = sessionLengths.average()

        // Adjust based on performance at different lengths
        val shortSessions = taskLogs.filter { it.minutesSpent <= 15 }
        val longSessions = taskLogs.filter { it.minutesSpent > 15 }

        val shortPerformance = if (shortSessions.isNotEmpty()) {
            shortSessions.count { it.correct }.toFloat() / shortSessions.size
        } else 0.5f

        val longPerformance = if (longSessions.isNotEmpty()) {
            longSessions.count { it.correct }.toFloat() / longSessions.size
        } else 0.5f

        // Prefer shorter sessions if they perform better
        return if (shortPerformance > longPerformance + 0.05f) {
            min(15, averageLength.toInt())
        } else {
            min(25, max(10, averageLength.toInt()))
        }
    }

    private fun identifyPeakPerformanceTimes(taskLogs: List<TaskLog>): List<TimeSlot> {
        // Group logs by hour and calculate performance
        val performanceByHour = taskLogs.groupBy { log ->
            val hour = (log.timestampMillis / (60 * 60 * 1000)) % 24
            hour.toInt()
        }.mapValues { (_, logs) ->
            logs.count { it.correct }.toFloat() / logs.size
        }

        // Find top 3 performing hours
        val topHours = performanceByHour.entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key }

        return topHours.map { hour ->
            TimeSlot(hour, (hour + 2) % 24, emptyList(), 15)
        }
    }

    private fun analyzeWeaknessPatterns(taskLogs: List<TaskLog>): List<WeaknessPattern> {
        val patterns = mutableListOf<WeaknessPattern>()

        SkillCategory.values().forEach { skill ->
            val skillLogs = taskLogs.filter { log ->
                log.category.contains(skill.name, ignoreCase = true)
            }.sortedByDescending { it.timestampMillis }

            if (skillLogs.size >= 5) {
                val recentAccuracy = skillLogs.take(10).count { it.correct }.toFloat() / min(10, skillLogs.size)
                val overallAccuracy = skillLogs.count { it.correct }.toFloat() / skillLogs.size

                if (recentAccuracy < 0.7f) {
                    val severity = 1f - recentAccuracy
                    val frequency = skillLogs.size

                    val patternType = when {
                        recentAccuracy < overallAccuracy - 0.1f -> PatternType.TIME_PRESSURE_WEAKNESS
                        severity > 0.4f -> PatternType.CONSISTENT_LOW_PERFORMANCE
                        else -> PatternType.SPECIFIC_TOPIC_STRUGGLE
                    }

                    patterns.add(WeaknessPattern(
                        skillCategory = skill,
                        patternType = patternType,
                        severity = severity,
                        frequency = frequency,
                        lastOccurrence = skillLogs.first().timestampMillis
                    ))
                }
            }
        }

        return patterns.sortedByDescending { it.severity }
    }

    private fun identifyInterestAreas(taskLogs: List<TaskLog>, vocabularyProgress: List<com.mtlc.studyplan.data.VocabularyProgress>): List<String> {
        // Analyze task categories and vocabulary topics for interest patterns
        val categoryFrequency = taskLogs.groupBy { it.category }
            .mapValues { it.value.size }

        // Extract interest areas from high-frequency categories
        return categoryFrequency.entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key.lowercase() }
    }

    private fun calculateCognitiveCapacity(taskLogs: List<TaskLog>): Float {
        // Analyze performance degradation over time in sessions
        val recentLogs = taskLogs.sortedByDescending { it.timestampMillis }.take(20)
        if (recentLogs.size < 10) return 0.8f

        val earlyPerformance = recentLogs.takeLast(10).count { it.correct }.toFloat() / 10
        val latePerformance = recentLogs.take(10).count { it.correct }.toFloat() / 10

        val degradation = earlyPerformance - latePerformance
        return (1f - degradation.coerceAtLeast(0f)).coerceIn(0.3f, 1f)
    }

    private fun calculateMotivationLevel(userProgress: UserProgress, taskLogs: List<TaskLog>): Float {
        // Calculate based on streak, recent activity, and consistency
        val streakFactor = min(userProgress.streakCount / 30f, 1f)
        val recentActivity = taskLogs.count { System.currentTimeMillis() - it.timestampMillis < 7 * 24 * 60 * 60 * 1000 }
        val activityFactor = min(recentActivity / 20f, 1f)

        return (streakFactor * 0.4f + activityFactor * 0.6f).coerceIn(0.2f, 1f)
    }

    private fun calculateConsistency(taskLogs: List<TaskLog>): Float {
        if (taskLogs.size < 5) return 0.5f

        val accuracies = taskLogs.map { if (it.correct) 1f else 0f }
        val mean = accuracies.average()
        val variance = accuracies.map { (it - mean).pow(2) }.average()

        // Lower variance = higher consistency
        return (1f - variance.toFloat()).coerceIn(0.1f, 0.9f)
    }

    private fun calculateFatigueLevel(taskLogs: List<TaskLog>): Float {
        // Analyze recent performance decline
        val recentLogs = taskLogs.sortedByDescending { it.timestampMillis }.take(10)
        if (recentLogs.size < 5) return 0.2f

        val recentAccuracy = recentLogs.take(5).count { it.correct }.toFloat() / 5
        val earlierAccuracy = recentLogs.takeLast(5).count { it.correct }.toFloat() / 5

        return (earlierAccuracy - recentAccuracy).coerceIn(0f, 0.8f)
    }

    private fun extractDifficulty(content: Any): Float {
        return when (content) {
            is com.mtlc.studyplan.data.VocabularyItem -> content.difficulty.toFloat()
            is com.mtlc.studyplan.questions.GeneratedQuestion -> content.difficulty.toFloat()
            is com.mtlc.studyplan.reading.ReadingContent -> content.difficulty.numericLevel.toFloat()
            else -> 3f
        }
    }

    private fun calculateRetentionRate(performance: ContentPerformance): Float {
        // Simplified retention calculation based on performance consistency
        return performance.accuracy * performance.completionRate
    }

    private fun calculateDifficultyAppropriateness(performance: ContentPerformance): Float {
        // Calculate how appropriate the difficulty was based on performance
        val optimalAccuracy = 0.7f // Target accuracy
        val accuracyDiff = abs(performance.accuracy - optimalAccuracy)
        return (1f - accuracyDiff).coerceIn(0f, 1f)
    }
}