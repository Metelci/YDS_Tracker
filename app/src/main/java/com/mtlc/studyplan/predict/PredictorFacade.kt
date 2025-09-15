package com.mtlc.studyplan.predict

import com.mtlc.studyplan.reports.pdf.UserDailyLoad
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Facade for ML prediction system - main entry point for UI components
 */
class PredictorFacade {
    
    private val forecaster = ScoreForecaster()
    private var lastTrainingResult: TrainingResult? = null
    
    /**
     * Main method for dashboard to get exam score forecast
     */
    suspend fun forecastNextExam(
        dailyLoads: List<UserDailyLoad>,
        shadowExamScores: List<ShadowExamScore>? = null
    ): ForecastResult = withContext(Dispatchers.Default) {
        
        try {
            // Extract weekly snapshots from daily loads
            val snapshots = FeatureExtractor.extractWeeklySnapshots(dailyLoads)
            
            if (snapshots.isEmpty()) {
                return@withContext ForecastResult.NoData("No study data available")
            }
            
            // Check data sufficiency
            if (!DataValidator.hasSufficientData(snapshots)) {
                return@withContext ForecastResult.InsufficientData(
                    message = "Need at least 8 weeks of study data for predictions",
                    currentWeeks = snapshots.size,
                    requiredWeeks = 8
                )
            }
            
            // Train model
            val trainingResult = forecaster.trainFromHistory(snapshots, shadowExamScores)
            lastTrainingResult = trainingResult
            
            when (trainingResult) {
                is TrainingResult.Success -> {
                    // Get current snapshot for prediction
                    val currentSnapshot = snapshots.lastOrNull()
                        ?: return@withContext ForecastResult.Error("No current data for prediction")
                    
                    // Make prediction
                    val prediction = forecaster.forecastNextExam(currentSnapshot)
                        ?: return@withContext ForecastResult.Error("Prediction failed")
                    
                    // Generate insights
                    val insights = generatePredictionInsights(prediction, currentSnapshot, trainingResult)
                    
                    ForecastResult.Success(
                        prediction = prediction,
                        insights = insights,
                        modelSummary = forecaster.getModelSummary()
                    )
                }
                
                is TrainingResult.InsufficientData -> {
                    ForecastResult.InsufficientData(trainingResult.message, snapshots.size, 8)
                }
                
                is TrainingResult.LowQuality -> {
                    ForecastResult.LowQuality(
                        message = trainingResult.message,
                        prediction = tryBasicPrediction(snapshots.last())
                    )
                }
                
                is TrainingResult.Error -> {
                    ForecastResult.Error(trainingResult.message)
                }
            }
            
        } catch (e: Exception) {
            ForecastResult.Error("Forecast failed: ${e.message}")
        }
    }
    
    /**
     * Get quick prediction without full training (for real-time updates)
     */
    suspend fun getQuickForecast(currentSnapshot: WeeklySnapshot): Pred? {
        return if (forecaster.isModelReady()) {
            forecaster.forecastNextExam(currentSnapshot)
        } else {
            tryBasicPrediction(currentSnapshot)
        }
    }
    
    /**
     * Get prediction confidence and reliability info
     */
    fun getPredictionReliability(): PredictionReliability? {
        val trainingResult = lastTrainingResult as? TrainingResult.Success ?: return null
        val modelSummary = forecaster.getModelSummary() ?: return null
        
        return PredictionReliability(
            isReliable = modelSummary.isReliable,
            accuracy = modelSummary.rSquared,
            dataQuality = modelSummary.qualityDescription,
            trainingWeeks = modelSummary.trainingWeeks,
            recommendation = modelSummary.getRecommendation()
        )
    }
    
    // Private helper methods
    
    private fun generatePredictionInsights(
        prediction: Pred,
        currentSnapshot: WeeklySnapshot,
        trainingResult: TrainingResult.Success
    ): List<PredictionInsight> {
        val insights = mutableListOf<PredictionInsight>()
        
        // Score interpretation
        val scoreInsight = when {
            prediction.scoreMean >= 65 -> PredictionInsight(
                type = InsightType.POSITIVE,
                title = "Strong Exam Readiness",
                description = "Your current trajectory suggests excellent exam performance",
                value = "${prediction.scoreMean.toInt()} points"
            )
            prediction.scoreMean >= 50 -> PredictionInsight(
                type = InsightType.NEUTRAL,
                title = "Good Progress",
                description = "You're making steady progress toward exam readiness",
                value = "${prediction.scoreMean.toInt()} points"
            )
            else -> PredictionInsight(
                type = InsightType.WARNING,
                title = "Improvement Needed",
                description = "Consider intensifying study efforts for better exam performance",
                value = "${prediction.scoreMean.toInt()} points"
            )
        }
        insights.add(scoreInsight)
        
        // Confidence insight
        val confidenceInsight = if (prediction.isReliable) {
            PredictionInsight(
                type = InsightType.INFO,
                title = "Reliable Prediction",
                description = "Based on ${trainingResult.trainingWeeks} weeks of consistent data",
                value = "${(prediction.confidence * 100).toInt()}% confidence"
            )
        } else {
            PredictionInsight(
                type = InsightType.WARNING,
                title = "Prediction Uncertainty",
                description = "Continue studying to improve forecast accuracy",
                value = "Lower confidence"
            )
        }
        insights.add(confidenceInsight)
        
        // Key factors
        val keyFactors = ScorePredictionUtils.identifyKeyFactors(currentSnapshot)
        keyFactors.take(2).forEach { factor ->
            insights.add(PredictionInsight(
                type = InsightType.INFO,
                title = "Key Factor",
                description = factor,
                value = ""
            ))
        }
        
        return insights
    }
    
    private fun tryBasicPrediction(snapshot: WeeklySnapshot): Pred {
        // Simple heuristic-based prediction when ML model fails
        val volumeScore = (snapshot.minutes.toDouble() / 300.0).coerceAtMost(1.0)
        val consistencyScore = snapshot.completionRate
        val streakBonus = (snapshot.streak.toDouble() / 14.0).coerceAtMost(0.2)
        
        val basePrediction = (volumeScore * 0.4 + consistencyScore * 0.4 + streakBonus) * 70 + 15
        val uncertainty = 15.0 // Wide interval for heuristic prediction
        
        return Pred(
            scoreMean = basePrediction.coerceIn(15.0, 85.0),
            lower = (basePrediction - uncertainty).coerceIn(0.0, 100.0),
            upper = (basePrediction + uncertainty).coerceIn(0.0, 100.0),
            featuresUsed = listOf("basic_heuristics"),
            confidence = 0.5,
            isReliable = false,
            modelQuality = "Heuristic"
        )
    }
}

/**
 * Forecast result types
 */
sealed class ForecastResult {
    data class Success(
        val prediction: Pred,
        val insights: List<PredictionInsight>,
        val modelSummary: ModelSummary?
    ) : ForecastResult()
    
    data class InsufficientData(
        val message: String,
        val currentWeeks: Int,
        val requiredWeeks: Int
    ) : ForecastResult()
    
    data class LowQuality(
        val message: String,
        val prediction: Pred? = null
    ) : ForecastResult()
    
    data class NoData(val message: String) : ForecastResult()
    data class Error(val message: String) : ForecastResult()
}

/**
 * Prediction insight for UI display
 */
data class PredictionInsight(
    val type: InsightType,
    val title: String,
    val description: String,
    val value: String,
    val actionable: Boolean = false,
    val action: String? = null
)

/**
 * Types of insights
 */
enum class InsightType(val displayName: String, val icon: String) {
    POSITIVE("Positive", "✅"),
    WARNING("Warning", "⚠️"),
    NEUTRAL("Info", "ℹ️"),
    INFO("Info", "📊")
}

/**
 * Prediction reliability information
 */
data class PredictionReliability(
    val isReliable: Boolean,
    val accuracy: Double,
    val dataQuality: String,
    val trainingWeeks: Int,
    val recommendation: String
) {
    fun getReliabilityDescription(): String = when {
        isReliable && accuracy > 0.7 -> "High reliability"
        isReliable && accuracy > 0.5 -> "Good reliability"
        accuracy > 0.3 -> "Moderate reliability"
        else -> "Low reliability"
    }
}

/**
 * Simple prediction interface for dashboard integration
 */
object PredictionAPI {
    
    private val facade = PredictorFacade()
    
    /**
     * Get exam score forecast - main entry point for UI
     */
    suspend fun getExamForecast(dailyLoads: List<UserDailyLoad>): ForecastResult {
        return facade.forecastNextExam(dailyLoads)
    }
    
    /**
     * Get quick forecast for real-time updates
     */
    suspend fun getQuickForecast(
        dailyLoads: List<UserDailyLoad>
    ): Pred? {
        if (dailyLoads.isEmpty()) return null
        
        val snapshots = FeatureExtractor.extractWeeklySnapshots(dailyLoads)
        val currentSnapshot = snapshots.lastOrNull() ?: return null
        
        return facade.getQuickForecast(currentSnapshot)
    }
    
    /**
     * Check if predictions are available
     */
    suspend fun arePredictionsAvailable(dailyLoads: List<UserDailyLoad>): Boolean {
        val snapshots = FeatureExtractor.extractWeeklySnapshots(dailyLoads)
        return DataValidator.hasSufficientData(snapshots, 8)
    }
    
    /**
     * Get minimum weeks needed for prediction
     */
    fun getMinimumWeeksForPrediction(): Int = 8
    
    /**
     * Calculate weeks until predictions are available
     */
    suspend fun getWeeksUntilPredictions(dailyLoads: List<UserDailyLoad>): Int {
        val snapshots = FeatureExtractor.extractWeeklySnapshots(dailyLoads)
        val currentWeeks = snapshots.size
        return maxOf(0, 8 - currentWeeks)
    }
}

/**
 * Prediction status for UI state management
 */
sealed class PredictionStatus {
    object Loading : PredictionStatus()
    object Ready : PredictionStatus()
    data class InsufficientData(val weeksNeeded: Int) : PredictionStatus()
    data class Error(val message: String) : PredictionStatus()
}