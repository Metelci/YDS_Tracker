package com.mtlc.studyplan.predict

import kotlin.math.*

/**
 * Prediction result for exam score forecasting
 */
data class Pred(
    val scoreMean: Double,
    val lower: Double,
    val upper: Double,
    val featuresUsed: List<String>,
    val confidence: Double = 0.95,
    val isReliable: Boolean = true,
    val modelQuality: String = "Good"
)

/**
 * Main forecaster for exam score prediction
 */
class ScoreForecaster {
    
    companion object {
        private const val MIN_WEEKS_REQUIRED = 8
        private const val MAX_WEEKS_USED = 12
        private const val DEFAULT_LAMBDA = 0.01
        private const val CONFIDENCE_LEVEL = 0.95
    }
    
    private var trainedModel: EnhancedLinearPredictor? = null
    private var featureNormalization: FeatureNormalization? = null
    private var lastTrainingSize: Int = 0
    
    /**
     * Train model from historical weekly snapshots and optional shadow exam scores
     */
    fun trainFromHistory(
        snapshots: List<WeeklySnapshot>,
        shadowExamScores: List<ShadowExamScore>? = null
    ): TrainingResult {
        
        // Validate sufficient data
        if (snapshots.size < MIN_WEEKS_REQUIRED) {
            return TrainingResult.InsufficientData("Need at least $MIN_WEEKS_REQUIRED weeks of data, have ${snapshots.size}")
        }
        
        if (!DataValidator.hasSufficientData(snapshots, MIN_WEEKS_REQUIRED)) {
            return TrainingResult.InsufficientData("Data quality insufficient for prediction")
        }
        
        try {
            // Use last N weeks for training (most recent and relevant)
            val recentSnapshots = snapshots.takeLast(MAX_WEEKS_USED)
            
            // Extract features
            val features = FeatureExtractor.createFeatureMatrix(recentSnapshots)
            val (normalizedFeatures, normalization) = FeatureExtractor.normalizeFeatures(features)
            
            // Create target vector
            val targets = if (shadowExamScores != null && shadowExamScores.isNotEmpty()) {
                createTargetFromShadowScores(recentSnapshots, shadowExamScores)
            } else {
                createProxyTargetFromAccuracy(recentSnapshots)
            }
            
            // Validate targets
            if (targets.any { it.isNaN() || it < 0 || it > 100 }) {
                return TrainingResult.Error("Invalid target values detected")
            }
            
            // Train model
            val model = EnhancedLinearPredictor()
            model.fitWithDiagnostics(normalizedFeatures, targets, DEFAULT_LAMBDA)
            
            // Store trained components
            this.trainedModel = model
            this.featureNormalization = normalization
            this.lastTrainingSize = recentSnapshots.size
            
            val diagnostics = model.getDiagnostics()
            
            return if (diagnostics?.isReliable == true) {
                TrainingResult.Success(
                    model = model,
                    diagnostics = diagnostics,
                    featuresUsed = WeeklySnapshot.getFeatureNames(),
                    trainingWeeks = recentSnapshots.size,
                    usedShadowScores = shadowExamScores != null && shadowExamScores.isNotEmpty()
                )
            } else {
                TrainingResult.LowQuality(
                    diagnostics?.getQualityDescription() ?: "Poor model quality",
                    diagnostics
                )
            }
            
        } catch (e: Exception) {
            return TrainingResult.Error("Training failed: ${e.message}")
        }
    }
    
    /**
     * Forecast next exam score based on current trajectory
     */
    fun forecastNextExam(currentSnapshot: WeeklySnapshot): Pred? {
        val model = trainedModel ?: return null
        val normalization = featureNormalization ?: return null
        
        try {
            // Normalize current features
            val features = currentSnapshot.toFeatureVector()
            val normalizedFeatures = FeatureExtractor.applyNormalization(features, normalization)
            
            // Make prediction with interval
            val predictionResult = model.predictWithInterval(normalizedFeatures, CONFIDENCE_LEVEL)
            
            // Map to YDS scale if needed
            val ydsResult = mapToYdsScale(predictionResult)
            
            val diagnostics = model.getDiagnostics()
            
            return Pred(
                scoreMean = ydsResult.mean,
                lower = ydsResult.lower,
                upper = ydsResult.upper,
                featuresUsed = WeeklySnapshot.getFeatureNames(),
                confidence = CONFIDENCE_LEVEL,
                isReliable = diagnostics?.isReliable ?: false,
                modelQuality = diagnostics?.getQualityDescription() ?: "Unknown"
            )
            
        } catch (e: Exception) {
            return null
        }
    }
    
    /**
     * Get model performance summary
     */
    fun getModelSummary(): ModelSummary? {
        val model = trainedModel ?: return null
        val diagnostics = model.getDiagnostics() ?: return null
        
        return ModelSummary(
            trainingWeeks = lastTrainingSize,
            rSquared = diagnostics.rSquared,
            meanAbsoluteError = diagnostics.meanAbsoluteError,
            isReliable = diagnostics.isReliable,
            qualityDescription = diagnostics.getQualityDescription(),
            featuresUsed = WeeklySnapshot.getFeatureNames().size
        )
    }
    
    /**
     * Check if model is ready for prediction
     */
    fun isModelReady(): Boolean {
        return trainedModel != null && featureNormalization != null
    }
    
    // Private helper methods
    
    private fun createTargetFromShadowScores(
        snapshots: List<WeeklySnapshot>,
        shadowScores: List<ShadowExamScore>
    ): DoubleArray {
        return snapshots.map { snapshot ->
            // Find closest shadow score within reasonable time window (±2 weeks)
            val closestScore = shadowScores
                .filter { score ->
                    abs(java.time.temporal.ChronoUnit.DAYS.between(snapshot.weekEnd, score.date)) <= 14
                }
                .minByOrNull { score ->
                    abs(java.time.temporal.ChronoUnit.DAYS.between(snapshot.weekEnd, score.date))
                }
            
            // Use shadow score if available, otherwise proxy
            closestScore?.let { 
                it.score * it.examType.scaleFactor 
            } ?: createProxyScore(snapshot)
        }.toDoubleArray()
    }
    
    private fun createProxyTargetFromAccuracy(snapshots: List<WeeklySnapshot>): DoubleArray {
        return snapshots.map { createProxyScore(it) }.toDoubleArray()
    }
    
    private fun createProxyScore(snapshot: WeeklySnapshot): Double {
        // Enhanced proxy scoring algorithm
        val volumeScore = calculateVolumeScore(snapshot)
        val consistencyScore = snapshot.completionRate
        val balanceScore = calculateSkillBalanceScore(snapshot)
        val streakBonus = calculateStreakBonus(snapshot.streak)
        val intensityScore = calculateIntensityScore(snapshot)
        
        // Weighted combination with realistic coefficients
        val rawScore = (volumeScore * 0.20 +
                       consistencyScore * 0.25 +
                       balanceScore * 0.20 +
                       intensityScore * 0.25 +
                       streakBonus * 0.10)
        
        // Map to YDS scale (typically 0-100, but realistic range is 15-85)
        val ydsScore = (rawScore * 70 + 15).coerceIn(15.0, 85.0)
        
        return ydsScore
    }
    
    private fun calculateVolumeScore(snapshot: WeeklySnapshot): Double {
        // Optimal weekly study time is around 300-420 minutes
        val optimalMinutes = 360.0
        val minuteScore = (snapshot.minutes.toDouble() / optimalMinutes).coerceAtMost(1.0)
        
        // Optimal task count is around 12-20 per week
        val optimalTasks = 15.0
        val taskScore = (snapshot.tasks.toDouble() / optimalTasks).coerceAtMost(1.0)
        
        // Combine with diminishing returns
        return sqrt(minuteScore * taskScore)
    }
    
    private fun calculateSkillBalanceScore(snapshot: WeeklySnapshot): Double {
        // Ideal distribution for YDS: Reading 35%, Grammar 30%, Listening 20%, Vocab 15%
        val idealReading = 0.35
        val idealGrammar = 0.30
        val idealListening = 0.20
        val idealVocab = 0.15
        
        val deviations = listOf(
            abs(snapshot.readingShare - idealReading),
            abs(snapshot.grammarShare - idealGrammar),
            abs(snapshot.listeningShare - idealListening),
            abs(snapshot.vocabShare - idealVocab)
        )
        
        val averageDeviation = deviations.average()
        return maxOf(0.0, 1.0 - averageDeviation * 2.0) // Penalize large deviations
    }
    
    private fun calculateStreakBonus(streak: Int): Double {
        // Streak bonus with diminishing returns
        return (streak.toDouble() / 30.0).coerceAtMost(1.0).pow(0.5)
    }
    
    private fun calculateIntensityScore(snapshot: WeeklySnapshot): Double {
        val dailyAverage = snapshot.minutes / 7.0
        return when {
            dailyAverage >= 60 -> 1.0 // Excellent
            dailyAverage >= 45 -> 0.8 // Good
            dailyAverage >= 30 -> 0.6 // Fair
            dailyAverage >= 15 -> 0.4 // Minimal
            else -> 0.2 // Very low
        }
    }
    
    /**
     * Map prediction result to YDS scale
     */
    private fun mapToYdsScale(result: PredictionResult): PredictionResult {
        // Ensure scores are in realistic YDS range
        return result.copy(
            mean = result.mean.coerceIn(0.0, 100.0),
            lower = result.lower.coerceIn(0.0, 100.0),
            upper = result.upper.coerceIn(0.0, 100.0)
        )
    }
}

/**
 * Training result types
 */
sealed class TrainingResult {
    data class Success(
        val model: EnhancedLinearPredictor,
        val diagnostics: ModelDiagnostics,
        val featuresUsed: List<String>,
        val trainingWeeks: Int,
        val usedShadowScores: Boolean
    ) : TrainingResult()
    
    data class InsufficientData(val message: String) : TrainingResult()
    data class LowQuality(val message: String, val diagnostics: ModelDiagnostics?) : TrainingResult()
    data class Error(val message: String) : TrainingResult()
}

/**
 * Model performance summary
 */
data class ModelSummary(
    val trainingWeeks: Int,
    val rSquared: Double,
    val meanAbsoluteError: Double,
    val isReliable: Boolean,
    val qualityDescription: String,
    val featuresUsed: Int
) {
    fun getAccuracyDescription(): String = when {
        rSquared > 0.8 -> "Highly accurate predictions"
        rSquared > 0.6 -> "Good prediction accuracy"
        rSquared > 0.4 -> "Moderate prediction accuracy"
        else -> "Low prediction accuracy"
    }
    
    fun getRecommendation(): String = when {
        !isReliable -> "Continue studying to improve prediction accuracy"
        rSquared < 0.5 -> "Need more consistent study patterns for better predictions"
        trainingWeeks < 10 -> "Predictions will improve with more study history"
        else -> "Model is performing well"
    }
}

/**
 * Score prediction utilities
 */
object ScorePredictionUtils {
    
    /**
     * Calculate expected score improvement based on study plan changes
     */
    fun calculateScoreImprovement(
        currentPrediction: Pred,
        improvedSnapshot: WeeklySnapshot,
        forecaster: ScoreForecaster
    ): Double? {
        return try {
            val improvedPrediction = forecaster.forecastNextExam(improvedSnapshot)
            improvedPrediction?.let { 
                it.scoreMean - currentPrediction.scoreMean 
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Generate recommendation based on prediction
     */
    fun generateRecommendation(prediction: Pred): String {
        return when {
            prediction.scoreMean >= 65 -> "You're on track for a strong performance! Keep up the excellent work."
            prediction.scoreMean >= 50 -> "Good progress! Focus on weak areas to boost your score further."
            prediction.scoreMean >= 35 -> "Steady improvement needed. Consider increasing study intensity."
            else -> "Significant effort required. Review study strategy and increase practice time."
        }
    }
    
    /**
     * Calculate score percentile (approximate)
     */
    fun calculateScorePercentile(score: Double): Double {
        // Rough YDS percentile mapping based on historical data
        return when {
            score >= 80 -> 95.0
            score >= 70 -> 85.0
            score >= 60 -> 70.0
            score >= 50 -> 55.0
            score >= 40 -> 40.0
            score >= 30 -> 25.0
            else -> 10.0
        }
    }
    
    /**
     * Identify key factors affecting prediction
     */
    fun identifyKeyFactors(snapshot: WeeklySnapshot): List<String> {
        val factors = mutableListOf<String>()
        
        // Study volume factors
        val dailyMinutes = snapshot.minutes / 7.0
        when {
            dailyMinutes < 30 -> factors.add("Low study volume (${dailyMinutes.toInt()} min/day)")
            dailyMinutes > 90 -> factors.add("High study intensity (${dailyMinutes.toInt()} min/day)")
        }
        
        // Consistency factors
        if (snapshot.completionRate < 0.7) {
            factors.add("Inconsistent study habits (${(snapshot.completionRate * 100).toInt()}% completion)")
        }
        
        // Streak factors
        when {
            snapshot.streak >= 21 -> factors.add("Excellent streak momentum (${snapshot.streak} days)")
            snapshot.streak >= 7 -> factors.add("Good consistency (${snapshot.streak} day streak)")
            snapshot.streak < 3 -> factors.add("Need better consistency (${snapshot.streak} day streak)")
        }
        
        // Skill balance factors
        val balanceScore = snapshot.calculateSkillBalance()
        if (balanceScore < 0.6) {
            factors.add("Unbalanced skill focus - diversify study areas")
        }
        
        // Identify dominant and weak skills
        val skillShares = mapOf(
            "Reading" to snapshot.readingShare,
            "Grammar" to snapshot.grammarShare,
            "Listening" to snapshot.listeningShare,
            "Vocabulary" to snapshot.vocabShare
        )
        
        val dominantSkill = skillShares.maxByOrNull { it.value }
        val weakSkill = skillShares.filter { it.value > 0 }.minByOrNull { it.value }
        
        dominantSkill?.let { (skill, share) ->
            if (share > 0.5) factors.add("Heavy focus on $skill (${(share * 100).toInt()}%)")
        }
        
        weakSkill?.let { (skill, share) ->
            if (share < 0.15) factors.add("$skill needs more attention (${(share * 100).toInt()}%)")
        }
        
        return factors
    }
}

/**
 * Prediction confidence calculator
 */
object ConfidenceCalculator {
    
    /**
     * Calculate prediction confidence based on model quality and data recency
     */
    fun calculateConfidence(
        modelDiagnostics: ModelDiagnostics,
        dataRecency: Int, // Days since last data point
        predictionRange: Double // Width of prediction interval
    ): Double {
        // Base confidence from model quality
        val modelConfidence = when {
            modelDiagnostics.rSquared > 0.7 -> 0.9
            modelDiagnostics.rSquared > 0.5 -> 0.7
            modelDiagnostics.rSquared > 0.3 -> 0.5
            else -> 0.3
        }
        
        // Reduce confidence for stale data
        val recencyFactor = when {
            dataRecency <= 7 -> 1.0
            dataRecency <= 14 -> 0.9
            dataRecency <= 30 -> 0.7
            else -> 0.5
        }
        
        // Reduce confidence for wide prediction intervals
        val precisionFactor = when {
            predictionRange <= 10 -> 1.0
            predictionRange <= 20 -> 0.8
            predictionRange <= 30 -> 0.6
            else -> 0.4
        }
        
        return (modelConfidence * recencyFactor * precisionFactor).coerceIn(0.1, 1.0)
    }
}

/**
 * Future TFLite integration hook
 */
interface PredictionEngine {
    suspend fun predict(features: DoubleArray): PredictionResult
    fun isLoaded(): Boolean
}

/**
 * TFLite engine placeholder for future implementation
 */
class TFLiteEngine : PredictionEngine {
    override suspend fun predict(features: DoubleArray): PredictionResult {
        throw NotImplementedError("TFLite integration not yet implemented")
    }
    
    override fun isLoaded(): Boolean = false
}

/**
 * Fallback to linear predictor when TFLite not available
 */
class LinearPredictionEngine(private val predictor: LinearPredictor) : PredictionEngine {
    override suspend fun predict(features: DoubleArray): PredictionResult {
        return predictor.predictWithInterval(features)
    }
    
    override fun isLoaded(): Boolean = true
}

/**
 * Score forecasting facade with multiple engine support
 */
class ScoreForecasterV2(
    private val primaryEngine: PredictionEngine = TFLiteEngine(),
    private val fallbackEngine: PredictionEngine? = null
) {
    
    suspend fun forecast(snapshot: WeeklySnapshot): Pred? {
        val features = snapshot.toFeatureVector()
        
        return try {
            if (primaryEngine.isLoaded()) {
                val result = primaryEngine.predict(features)
                convertToPred(result, WeeklySnapshot.getFeatureNames())
            } else if (fallbackEngine?.isLoaded() == true) {
                val result = fallbackEngine.predict(features)
                convertToPred(result, WeeklySnapshot.getFeatureNames())
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun convertToPred(result: PredictionResult, features: List<String>): Pred {
        return Pred(
            scoreMean = result.mean,
            lower = result.lower,
            upper = result.upper,
            featuresUsed = features,
            confidence = result.confidence,
            isReliable = result.isReliable()
        )
    }
}

/**
 * Map prediction to YDS scale utility
 */
fun mapToYdsScale(result: PredictionResult): PredictionResult {
    // YDS scores typically range from 0-100, but realistic range is 15-85
    fun clampToYdsRange(score: Double): Double = score.coerceIn(0.0, 100.0)
    
    return result.copy(
        mean = clampToYdsRange(result.mean),
        lower = clampToYdsRange(result.lower),
        upper = clampToYdsRange(result.upper)
    )
}