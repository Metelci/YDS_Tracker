package com.mtlc.studyplan.predict

import com.mtlc.studyplan.reports.pdf.UserDailyLoad
import com.mtlc.studyplan.reports.pdf.Skill
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.WeekFields
import kotlin.math.*

/**
 * Weekly performance snapshot for ML feature extraction
 */
data class WeeklySnapshot(
    val weekEnd: LocalDate,
    val minutes: Int,
    val tasks: Int,
    val completionRate: Double,
    val streak: Int,
    val readingShare: Double,    // 0..1
    val listeningShare: Double,  // 0..1
    val vocabShare: Double,      // 0..1
    val grammarShare: Double     // 0..1
) {
    /**
     * Convert to feature vector for ML model
     */
    fun toFeatureVector(): DoubleArray {
        return doubleArrayOf(
            minutes.toDouble(),
            tasks.toDouble(),
            completionRate,
            streak.toDouble(),
            readingShare,
            listeningShare,
            vocabShare,
            grammarShare,
            calculateWeeklyIntensity(),
            calculateConsistencyScore(),
            calculateSkillBalance(),
            calculateVolumeScore()
        )
    }

    /**
     * Get feature names for interpretability
     */
    companion object {
        fun getFeatureNames(): List<String> {
            return listOf(
                "minutes",
                "tasks",
                "completion_rate",
                "streak",
                "reading_share",
                "listening_share",
                "vocab_share",
                "grammar_share",
                "weekly_intensity",
                "consistency_score",
                "skill_balance",
                "volume_score"
            )
        }
    }

    /**
     * Calculate weekly study intensity (minutes/day normalized)
     */
    private fun calculateWeeklyIntensity(): Double {
        val dailyAverage = minutes / 7.0
        // Normalize to 0-1 scale where 60 min/day = 1.0
        return (dailyAverage / 60.0).coerceAtMost(2.0)
    }

    /**
     * Calculate consistency score based on completion rate and streak
     */
    private fun calculateConsistencyScore(): Double {
        val streakFactor = (streak.toDouble() / 30.0).coerceAtMost(1.0) // Normalize to 30-day max
        return (completionRate * 0.7 + streakFactor * 0.3)
    }

    /**
     * Calculate skill balance (how evenly distributed study time is)
     */
    fun calculateSkillBalance(): Double {
        val shares = listOf(readingShare, listeningShare, vocabShare, grammarShare)
        val nonZeroShares = shares.filter { it > 0.0 }
        
        if (nonZeroShares.isEmpty()) return 0.0
        
        // Calculate entropy as measure of balance
        val entropy = nonZeroShares.sumOf { share ->
            if (share > 0) -share * ln(share) else 0.0
        }
        
        // Normalize by max possible entropy for 4 skills
        val maxEntropy = ln(4.0)
        return (entropy / maxEntropy).coerceIn(0.0, 1.0)
    }

    /**
     * Calculate volume score (total study effort)
     */
    fun calculateVolumeScore(): Double {
        // Combine minutes and tasks with diminishing returns
        val minuteScore = (minutes.toDouble() / 300.0).coerceAtMost(1.0) // 300 min/week = 1.0
        val taskScore = (tasks.toDouble() / 20.0).coerceAtMost(1.0) // 20 tasks/week = 1.0
        return sqrt(minuteScore * taskScore)
    }
}

/**
 * Shadow exam score data for training
 */
data class ShadowExamScore(
    val date: LocalDate,
    val score: Double, // 0-100 scale
    val examType: ExamType,
    val confidence: Double = 1.0 // How reliable this score is
)

/**
 * Types of practice exams
 */
enum class ExamType(val displayName: String, val scaleFactor: Double) {
    YDS_PRACTICE("YDS Practice", 1.0),
    YOKDIL_PRACTICE("YÖKDİL Practice", 1.0),
    MINI_EXAM("Mini Exam", 0.8), // Slightly easier
    TOEFL_PRACTICE("TOEFL Practice", 0.9),
    IELTS_PRACTICE("IELTS Practice", 0.9),
    CUSTOM_TEST("Custom Test", 0.7)
}

/**
 * Feature extraction utilities
 */
object FeatureExtractor {

    /**
     * Extract weekly snapshots from daily load history
     */
    fun extractWeeklySnapshots(dailyLoads: List<UserDailyLoad>): List<WeeklySnapshot> {
        if (dailyLoads.isEmpty()) return emptyList()

        // Group by week
        val weeklyGroups = dailyLoads.groupBy { dailyLoad ->
            val weekFields = WeekFields.of(DayOfWeek.MONDAY, 1)
            val weekEnd = dailyLoad.date.with(weekFields.dayOfWeek(), 7) // End of week (Sunday)
            weekEnd
        }

        return weeklyGroups.map { (weekEnd, loadsInWeek) ->
            val totalMinutes = loadsInWeek.sumOf { it.totalMinutes }
            val totalTasks = loadsInWeek.sumOf { it.tasksCompleted }
            val completionRate = if (loadsInWeek.isNotEmpty()) {
                loadsInWeek.count { it.tasksCompleted > 0 }.toDouble() / loadsInWeek.size
            } else 0.0

            // Calculate skill shares
            val skillTotals = mutableMapOf<Skill, Int>()
            loadsInWeek.forEach { load ->
                load.skillBreakdown.forEach { (skill, minutes) ->
                    skillTotals[skill] = (skillTotals[skill] ?: 0) + minutes
                }
            }

            val totalSkillMinutes = skillTotals.values.sum().toDouble().coerceAtLeast(1.0)
            val readingShare = (skillTotals[Skill.READING] ?: 0) / totalSkillMinutes
            val listeningShare = (skillTotals[Skill.LISTENING] ?: 0) / totalSkillMinutes
            val vocabShare = (skillTotals[Skill.VOCABULARY] ?: 0) / totalSkillMinutes
            val grammarShare = (skillTotals[Skill.GRAMMAR] ?: 0) / totalSkillMinutes

            // Get max streak from the week
            val maxStreak = loadsInWeek.maxOfOrNull { it.streakDayNumber } ?: 0

            WeeklySnapshot(
                weekEnd = weekEnd,
                minutes = totalMinutes,
                tasks = totalTasks,
                completionRate = completionRate,
                streak = maxStreak,
                readingShare = readingShare,
                listeningShare = listeningShare,
                vocabShare = vocabShare,
                grammarShare = grammarShare
            )
        }.sortedBy { it.weekEnd }
    }

    /**
     * Create feature matrix from weekly snapshots
     */
    fun createFeatureMatrix(snapshots: List<WeeklySnapshot>): Array<DoubleArray> {
        return snapshots.map { it.toFeatureVector() }.toTypedArray()
    }

    /**
     * Create target vector from shadow exam scores or proxy
     */
    fun createTargetVector(
        snapshots: List<WeeklySnapshot>,
        shadowScores: List<ShadowExamScore>? = null
    ): DoubleArray {
        return if (shadowScores != null && shadowScores.isNotEmpty()) {
            // Use actual exam scores as targets
            createTargetFromExamScores(snapshots, shadowScores)
        } else {
            // Use proxy target from study performance
            createProxyTarget(snapshots)
        }
    }

    /**
     * Create target vector from actual exam scores
     */
    private fun createTargetFromExamScores(
        snapshots: List<WeeklySnapshot>,
        shadowScores: List<ShadowExamScore>
    ): DoubleArray {
        return snapshots.map { snapshot ->
            // Find closest exam score to week end
            val closestScore = shadowScores.minByOrNull { score ->
                abs(java.time.temporal.ChronoUnit.DAYS.between(snapshot.weekEnd, score.date))
            }
            
            closestScore?.score ?: createProxyScoreForSnapshot(snapshot)
        }.toDoubleArray()
    }

    /**
     * Create proxy target from study performance metrics
     */
    private fun createProxyTarget(snapshots: List<WeeklySnapshot>): DoubleArray {
        return snapshots.map { createProxyScoreForSnapshot(it) }.toDoubleArray()
    }

    /**
     * Generate proxy exam score from study performance
     */
    private fun createProxyScoreForSnapshot(snapshot: WeeklySnapshot): Double {
        // Heuristic scoring based on study patterns
        val volumeScore = (snapshot.minutes.toDouble() / 300.0).coerceAtMost(1.0) // 300 min/week baseline
        val taskScore = (snapshot.tasks.toDouble() / 15.0).coerceAtMost(1.0) // 15 tasks/week baseline
        val consistencyScore = snapshot.completionRate
        val streakBonus = (snapshot.streak.toDouble() / 14.0).coerceAtMost(0.2) // Max 20% bonus
        val balanceScore = snapshot.calculateSkillBalance()

        // Weighted combination
        val rawScore = (volumeScore * 0.25 + 
                       taskScore * 0.25 + 
                       consistencyScore * 0.30 + 
                       balanceScore * 0.20) + streakBonus

        // Convert to YDS-like scale (0-100)
        val ydsScore = (rawScore * 85 + 15).coerceIn(0.0, 100.0) // Map to 15-100 range

        return ydsScore
    }

    /**
     * Normalize features for better ML performance
     */
    fun normalizeFeatures(features: Array<DoubleArray>): Pair<Array<DoubleArray>, FeatureNormalization> {
        if (features.isEmpty()) return features to FeatureNormalization.empty()

        val featureCount = features.first().size
        val means = DoubleArray(featureCount)
        val stds = DoubleArray(featureCount)

        // Calculate means
        for (f in 0 until featureCount) {
            means[f] = features.map { it[f] }.average()
        }

        // Calculate standard deviations
        for (f in 0 until featureCount) {
            val variance = features.map { (it[f] - means[f]).pow(2) }.average()
            stds[f] = sqrt(variance).takeIf { it > 1e-8 } ?: 1.0 // Avoid division by zero
        }

        // Normalize features
        val normalizedFeatures = features.map { row ->
            row.mapIndexed { f, value ->
                (value - means[f]) / stds[f]
            }.toDoubleArray()
        }.toTypedArray()

        val normalization = FeatureNormalization(means, stds)
        return normalizedFeatures to normalization
    }

    /**
     * Apply feature normalization to new data
     */
    fun applyNormalization(features: DoubleArray, normalization: FeatureNormalization): DoubleArray {
        return features.mapIndexed { i, value ->
            (value - normalization.means[i]) / normalization.stds[i]
        }.toDoubleArray()
    }
}

/**
 * Feature normalization parameters
 */
data class FeatureNormalization(
    val means: DoubleArray,
    val stds: DoubleArray
) {
    companion object {
        fun empty() = FeatureNormalization(doubleArrayOf(), doubleArrayOf())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FeatureNormalization

        if (!means.contentEquals(other.means)) return false
        if (!stds.contentEquals(other.stds)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = means.contentHashCode()
        result = 31 * result + stds.contentHashCode()
        return result
    }
}

/**
 * Performance trend analysis
 */
object TrendAnalyzer {
    
    /**
     * Calculate linear trend in study metrics
     */
    fun calculateLinearTrend(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        
        val n = values.size
        val x = (0 until n).map { it.toDouble() }
        val y = values
        
        val xMean = x.average()
        val yMean = y.average()
        
        val numerator = x.zip(y).sumOf { (xi, yi) -> (xi - xMean) * (yi - yMean) }
        val denominator = x.sumOf { (it - xMean).pow(2) }
        
        return if (denominator > 1e-8) numerator / denominator else 0.0
    }
    
    /**
     * Detect study performance momentum
     */
    fun calculateMomentum(snapshots: List<WeeklySnapshot>): Double {
        if (snapshots.size < 3) return 0.0
        
        val recentWeeks = snapshots.takeLast(4)
        val minutesTrend = calculateLinearTrend(recentWeeks.map { it.minutes.toDouble() })
        val completionTrend = calculateLinearTrend(recentWeeks.map { it.completionRate })
        val streakTrend = calculateLinearTrend(recentWeeks.map { it.streak.toDouble() })
        
        // Weighted momentum score
        return (minutesTrend * 0.4 + completionTrend * 0.4 + streakTrend * 0.2)
            .coerceIn(-1.0, 1.0)
    }
    
    /**
     * Detect if student is improving or declining
     */
    fun getPerformanceDirection(snapshots: List<WeeklySnapshot>): PerformanceDirection {
        val momentum = calculateMomentum(snapshots)
        
        return when {
            momentum > 0.1 -> PerformanceDirection.IMPROVING
            momentum < -0.1 -> PerformanceDirection.DECLINING
            else -> PerformanceDirection.STABLE
        }
    }
}

/**
 * Performance direction enum
 */
enum class PerformanceDirection(val displayName: String, val icon: String) {
    IMPROVING("Improving", "📈"),
    DECLINING("Declining", "📉"),
    STABLE("Stable", "➡️")
}

/**
 * Extended feature engineering for advanced models
 */
object AdvancedFeatures {
    
    /**
     * Calculate exponentially weighted moving averages
     */
    fun calculateEWMA(values: List<Double>, alpha: Double = 0.3): List<Double> {
        if (values.isEmpty()) return emptyList()
        
        val ewma = mutableListOf<Double>()
        var currentEwma = values.first()
        ewma.add(currentEwma)
        
        for (i in 1 until values.size) {
            currentEwma = alpha * values[i] + (1 - alpha) * currentEwma
            ewma.add(currentEwma)
        }
        
        return ewma
    }
    
    /**
     * Calculate feature interactions
     */
    fun createInteractionFeatures(snapshot: WeeklySnapshot): DoubleArray {
        return doubleArrayOf(
            // Study intensity interactions
            snapshot.minutes * snapshot.completionRate,
            snapshot.tasks * snapshot.completionRate,
            
            // Skill focus interactions
            snapshot.readingShare * snapshot.grammarShare,
            snapshot.listeningShare * snapshot.vocabShare,
            
            // Consistency interactions
            snapshot.streak * snapshot.completionRate,
            
            // Balance vs. volume
            snapshot.calculateSkillBalance() * snapshot.calculateVolumeScore()
        )
    }
    
    /**
     * Calculate lag features (previous week impact)
     */
    fun createLagFeatures(snapshots: List<WeeklySnapshot>, lagWeeks: Int = 2): Array<DoubleArray> {
        val features = mutableListOf<DoubleArray>()
        
        for (i in lagWeeks until snapshots.size) {
            val current = snapshots[i].toFeatureVector()
            val lagged = mutableListOf<Double>()
            
            // Add current week features
            lagged.addAll(current.toList())
            
            // Add previous weeks features
            for (lag in 1..lagWeeks) {
                val previousSnapshot = snapshots[i - lag]
                lagged.addAll(previousSnapshot.toFeatureVector().toList())
            }
            
            features.add(lagged.toDoubleArray())
        }
        
        return features.toTypedArray()
    }
}

/**
 * Data validation utilities
 */
object DataValidator {
    
    /**
     * Validate weekly snapshot data quality
     */
    fun validateSnapshot(snapshot: WeeklySnapshot): ValidationResult {
        val errors = mutableListOf<String>()
        
        if (snapshot.minutes < 0) errors.add("Negative minutes")
        if (snapshot.tasks < 0) errors.add("Negative tasks")
        if (snapshot.completionRate < 0 || snapshot.completionRate > 1) errors.add("Invalid completion rate")
        if (snapshot.streak < 0) errors.add("Negative streak")
        
        val totalShare = snapshot.readingShare + snapshot.listeningShare + 
                        snapshot.vocabShare + snapshot.grammarShare
        if (totalShare > 1.1) errors.add("Skill shares sum > 1.0") // Allow small rounding errors
        
        return ValidationResult(errors.isEmpty(), errors)
    }
    
    /**
     * Check if sufficient data exists for prediction
     */
    fun hasSufficientData(snapshots: List<WeeklySnapshot>, minWeeks: Int = 8): Boolean {
        if (snapshots.size < minWeeks) return false
        
        // Check for meaningful data (not all zeros)
        val hasActivity = snapshots.any { it.minutes > 0 && it.tasks > 0 }
        if (!hasActivity) return false
        
        // Check for recent data (last 4 weeks should have some activity)
        val recentActivity = snapshots.takeLast(4).any { it.minutes > 0 }
        return recentActivity
    }
}

/**
 * Validation result
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String>
) {
    val errorMessage: String
        get() = errors.joinToString("; ")
}