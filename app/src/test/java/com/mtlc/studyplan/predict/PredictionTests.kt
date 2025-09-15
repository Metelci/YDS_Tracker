package com.mtlc.studyplan.predict

import com.mtlc.studyplan.reports.pdf.UserDailyLoad
import com.mtlc.studyplan.reports.pdf.Skill
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import java.time.LocalDate
import kotlin.math.*
import kotlin.random.Random

/**
 * Tests for performance prediction models
 */
class PredictionTests {

    private lateinit var syntheticSnapshots: List<WeeklySnapshot>
    private lateinit var linearTrendData: Pair<Array<DoubleArray>, DoubleArray>

    @Before
    fun setup() {
        // Create synthetic weekly snapshots with linear trend
        syntheticSnapshots = generateSyntheticSnapshots(12, hasLinearTrend = true)
        
        // Create simple linear trend data for basic testing
        linearTrendData = generateLinearTrendData(10)
    }

    /**
     * Test ridge regression on synthetic linear trend data
     */
    @Test
    fun testLinearRegressionOnSyntheticTrend() {
        val (X, y) = linearTrendData
        val predictor = LinearPredictor()
        
        // Fit model
        val fittedModel = predictor.fit(X, y, lambda = 0.01)
        
        // Test predictions
        val predictions = fittedModel.predict(X)
        
        // Calculate R²
        val yMean = y.average()
        val totalSumSquares = y.sumOf { (it - yMean).pow(2) }
        val residualSumSquares = y.zip(predictions).sumOf { (actual, pred) -> (actual - pred).pow(2) }
        val rSquared = 1.0 - (residualSumSquares / totalSumSquares)
        
        assertTrue("R² should be > 0.8 for linear trend", rSquared > 0.8)
        
        // Test prediction near ground truth
        val testX = doubleArrayOf(5.0) // Mid-range test point
        val expectedY = 2.0 * 5.0 + 1.0 // Based on linear trend y = 2x + 1
        val prediction = fittedModel.predict(testX)
        val error = abs(prediction - expectedY)
        
        assertTrue("Prediction should be close to ground truth (error: $error)", error < 1.0)
    }

    /**
     * Test prediction interval coverage
     */
    @Test
    fun testPredictionIntervalCoverage() {
        val trials = 100
        val coverageCount = (1..trials).count { trial ->
            // Generate fresh linear data with noise for each trial
            val (X, y) = generateLinearTrendDataWithNoise(20, noiseLevel = 0.5)
            
            val predictor = LinearPredictor()
            val fittedModel = predictor.fit(X, y, lambda = 0.01)
            
            // Test on out-of-sample point
            val testX = doubleArrayOf(10.0)
            val trueY = 2.0 * 10.0 + 1.0 // Ground truth
            val predictionResult = fittedModel.predictWithInterval(testX, 0.95)
            
            // Check if true value is within predicted interval
            trueY >= predictionResult.lower && trueY <= predictionResult.upper
        }
        
        val coverageRate = coverageCount.toDouble() / trials
        assertTrue(
            "Interval should cover true value ≥80% of trials, actual: ${(coverageRate * 100).toInt()}%",
            coverageRate >= 0.80
        )
    }

    /**
     * Test ScoreForecaster with synthetic weekly data
     */
    @Test
    fun testScoreForecasterWithSyntheticData() = runBlocking {
        val forecaster = ScoreForecaster()
        
        // Create shadow exam scores that align with synthetic trend
        val shadowScores = syntheticSnapshots.mapIndexed { index, snapshot ->
            ShadowExamScore(
                date = snapshot.weekEnd,
                score = 50.0 + index * 2.0, // Linear improvement
                examType = ExamType.YDS_PRACTICE
            )
        }.take(8) // Use only some scores to test mixed target creation
        
        // Train model
        val trainingResult = forecaster.trainFromHistory(syntheticSnapshots, shadowScores)
        
        assertTrue("Training should succeed with sufficient data", trainingResult is TrainingResult.Success)
        
        if (trainingResult is TrainingResult.Success) {
            assertTrue("Model should be reliable", trainingResult.diagnostics.isReliable)
            assertTrue("R² should be reasonable", trainingResult.diagnostics.rSquared > 0.3)
        }
        
        // Test prediction
        val currentSnapshot = syntheticSnapshots.last()
        val prediction = forecaster.forecastNextExam(currentSnapshot)
        
        assertNotNull("Should get prediction", prediction)
        prediction?.let {
            assertTrue("Prediction should be in valid range", it.scoreMean in 0.0..100.0)
            assertTrue("Should have non-zero interval width", it.upper > it.lower)
            assertTrue("Features should be used", it.featuresUsed.isNotEmpty())
        }
    }

    /**
     * Test feature extraction from daily loads
     */
    @Test
    fun testFeatureExtractionFromDailyLoads() {
        val dailyLoads = generateSyntheticDailyLoads(30) // 30 days ≈ 4 weeks
        val snapshots = FeatureExtractor.extractWeeklySnapshots(dailyLoads)
        
        assertTrue("Should extract weekly snapshots", snapshots.isNotEmpty())
        assertTrue("Should have reasonable number of weeks", snapshots.size in 3..5)
        
        // Validate snapshot content
        snapshots.forEach { snapshot ->
            assertTrue("Minutes should be non-negative", snapshot.minutes >= 0)
            assertTrue("Tasks should be non-negative", snapshot.tasks >= 0)
            assertTrue("Completion rate should be 0-1", snapshot.completionRate in 0.0..1.0)
            assertTrue("Skill shares should sum ≤ 1.1", 
                      snapshot.readingShare + snapshot.listeningShare + 
                      snapshot.vocabShare + snapshot.grammarShare <= 1.1)
        }
    }

    /**
     * Test insufficient data handling
     */
    @Test
    fun testInsufficientDataHandling() = runBlocking {
        val forecaster = ScoreForecaster()
        
        // Test with too few weeks
        val fewSnapshots = syntheticSnapshots.take(5)
        val result = forecaster.trainFromHistory(fewSnapshots)
        
        assertTrue("Should return insufficient data result", result is TrainingResult.InsufficientData)
        
        // Test empty data
        val emptyResult = forecaster.trainFromHistory(emptyList())
        assertTrue("Should handle empty data", emptyResult is TrainingResult.InsufficientData)
    }

    /**
     * Test matrix operations in LinearPredictor
     */
    @Test
    fun testMatrixOperations() {
        // Test 2x2 matrix determinant
        val matrix2x2 = arrayOf(
            doubleArrayOf(2.0, 3.0),
            doubleArrayOf(1.0, 4.0)
        )
        val det = MatrixUtils.determinant2x2(matrix2x2)
        assertEquals("Determinant should be 5.0", 5.0, det, 1e-10)
        
        // Test identity matrix
        val identity = MatrixUtils.identityMatrix(3)
        assertEquals("Identity matrix should be 3x3", 3, identity.size)
        assertTrue("Diagonal should be 1.0", identity[0][0] == 1.0)
        assertTrue("Off-diagonal should be 0.0", identity[0][1] == 0.0)
        
        // Test vector norm
        val vector = doubleArrayOf(3.0, 4.0)
        val norm = MatrixUtils.vectorNorm(vector)
        assertEquals("Norm should be 5.0", 5.0, norm, 1e-10)
    }

    /**
     * Test data validation
     */
    @Test
    fun testDataValidation() {
        val validSnapshot = WeeklySnapshot(
            weekEnd = LocalDate.now(),
            minutes = 300,
            tasks = 15,
            completionRate = 0.8,
            streak = 7,
            readingShare = 0.3,
            listeningShare = 0.2,
            vocabShare = 0.2,
            grammarShare = 0.3
        )
        
        val validation = DataValidator.validateSnapshot(validSnapshot)
        assertTrue("Valid snapshot should pass validation", validation.isValid)
        
        // Test invalid snapshot
        val invalidSnapshot = validSnapshot.copy(
            minutes = -10,
            completionRate = 1.5
        )
        
        val invalidValidation = DataValidator.validateSnapshot(invalidSnapshot)
        assertFalse("Invalid snapshot should fail validation", invalidValidation.isValid)
        assertTrue("Should have error messages", invalidValidation.errors.isNotEmpty())
    }

    /**
     * Test cross-validation
     */
    @Test
    fun testCrossValidation() {
        val (X, y) = generateLinearTrendData(20) // Larger dataset for CV
        
        val cvResult = CrossValidation.kFoldCV(X, y, k = 5, lambda = 0.01)
        
        assertTrue("CV mean score should be positive", cvResult.meanScore > 0.0)
        assertTrue("CV should be stable", cvResult.scores.isNotEmpty())
        assertEquals("Should have 5 fold scores", 5, cvResult.scores.size)
    }

    /**
     * Test YDS scale mapping
     */
    @Test
    fun testYdsScaleMapping() {
        val prediction = PredictionResult(
            mean = 150.0, // Out of range
            lower = -20.0, // Out of range
            upper = 80.0,
            standardError = 5.0,
            confidence = 0.95
        )
        
        val mapped = mapToYdsScale(prediction)
        
        assertTrue("Mean should be clamped to YDS range", mapped.mean in 0.0..100.0)
        assertTrue("Lower should be clamped to YDS range", mapped.lower in 0.0..100.0)
        assertTrue("Upper should be clamped to YDS range", mapped.upper in 0.0..100.0)
    }

    /**
     * Test PredictionAPI integration
     */
    @Test
    fun testPredictionAPI() = runBlocking {
        val dailyLoads = generateSyntheticDailyLoads(60) // 8+ weeks
        
        // Test availability check
        val isAvailable = PredictionAPI.arePredictionsAvailable(dailyLoads)
        assertTrue("Should be available with sufficient data", isAvailable)
        
        // Test weeks until prediction
        val weeksUntil = PredictionAPI.getWeeksUntilPredictions(dailyLoads)
        assertEquals("Should be ready now", 0, weeksUntil)
        
        // Test actual prediction
        val forecast = PredictionAPI.getExamForecast(dailyLoads)
        assertTrue("Should get successful forecast", forecast is ForecastResult.Success)
    }

    // Helper methods for generating synthetic data

    private fun generateSyntheticSnapshots(count: Int, hasLinearTrend: Boolean = false): List<WeeklySnapshot> {
        val random = Random(42) // Fixed seed for reproducible tests
        val snapshots = mutableListOf<WeeklySnapshot>()
        
        for (i in 0 until count) {
            val weekEnd = LocalDate.now().minusWeeks((count - i - 1).toLong())
            
            val baseMinutes = if (hasLinearTrend) {
                200 + i * 15 // Linear increase
            } else {
                250 + random.nextInt(-50, 51) // Random variation
            }
            
            val baseTasks = if (hasLinearTrend) {
                10 + i
            } else {
                12 + random.nextInt(-3, 4)
            }
            
            val snapshot = WeeklySnapshot(
                weekEnd = weekEnd,
                minutes = baseMinutes.coerceAtLeast(0),
                tasks = baseTasks.coerceAtLeast(0),
                completionRate = (0.7 + random.nextDouble() * 0.3).coerceIn(0.0, 1.0),
                streak = (i + 1).coerceAtMost(30),
                readingShare = 0.3 + random.nextDouble(-0.1, 0.1),
                listeningShare = 0.25 + random.nextDouble(-0.1, 0.1),
                vocabShare = 0.2 + random.nextDouble(-0.05, 0.05),
                grammarShare = 0.25 + random.nextDouble(-0.1, 0.1)
            )
            
            snapshots.add(snapshot)
        }
        
        return snapshots
    }

    private fun generateLinearTrendData(size: Int): Pair<Array<DoubleArray>, DoubleArray> {
        // Generate data following y = 2x + 1
        val X = Array(size) { i -> doubleArrayOf(i.toDouble()) }
        val y = DoubleArray(size) { i -> 2.0 * i + 1.0 }
        
        return X to y
    }

    private fun generateLinearTrendDataWithNoise(
        size: Int, 
        noiseLevel: Double = 0.1
    ): Pair<Array<DoubleArray>, DoubleArray> {
        val random = Random(123) // Fixed seed
        
        // Generate data following y = 2x + 1 + noise
        val X = Array(size) { i -> doubleArrayOf(i.toDouble()) }
        val y = DoubleArray(size) { i -> 
            2.0 * i + 1.0 + random.nextGaussian() * noiseLevel
        }
        
        return X to y
    }

    private fun generateSyntheticDailyLoads(days: Int): List<UserDailyLoad> {
        val random = Random(789)
        val loads = mutableListOf<UserDailyLoad>()
        
        for (i in 0 until days) {
            val date = LocalDate.now().minusDays((days - i - 1).toLong())
            val hasActivity = random.nextFloat() > 0.2 // 80% chance of study activity
            
            if (hasActivity) {
                val skillBreakdown = mapOf(
                    Skill.GRAMMAR to random.nextInt(0, 50),
                    Skill.READING to random.nextInt(0, 60),
                    Skill.LISTENING to random.nextInt(0, 40),
                    Skill.VOCABULARY to random.nextInt(0, 30)
                )
                
                val totalMinutes = skillBreakdown.values.sum()
                val tasksCompleted = (totalMinutes / 25).coerceAtLeast(1) // Rough estimate
                
                loads.add(UserDailyLoad(
                    date = date,
                    totalMinutes = totalMinutes,
                    tasksCompleted = tasksCompleted,
                    averageAccuracy = 0.7f + random.nextFloat() * 0.25f,
                    skillBreakdown = skillBreakdown,
                    streakDayNumber = if (i > 0 && loads.last().totalMinutes > 0) {
                        loads.last().streakDayNumber + 1
                    } else 1
                ))
            } else {
                loads.add(UserDailyLoad(
                    date = date,
                    totalMinutes = 0,
                    tasksCompleted = 0,
                    averageAccuracy = 0f,
                    streakDayNumber = 0
                ))
            }
        }
        
        return loads
    }

    /**
     * Test feature extraction edge cases
     */
    @Test
    fun testFeatureExtractionEdgeCases() {
        // Test empty input
        val emptySnapshots = FeatureExtractor.extractWeeklySnapshots(emptyList())
        assertTrue("Should handle empty input", emptySnapshots.isEmpty())
        
        // Test single day
        val singleDay = listOf(UserDailyLoad(
            date = LocalDate.now(),
            totalMinutes = 60,
            tasksCompleted = 3,
            averageAccuracy = 0.8f,
            skillBreakdown = mapOf(Skill.GRAMMAR to 60)
        ))
        
        val singleSnapshot = FeatureExtractor.extractWeeklySnapshots(singleDay)
        assertEquals("Should create one snapshot", 1, singleSnapshot.size)
        
        val snapshot = singleSnapshot.first()
        assertEquals("Should have correct minutes", 60, snapshot.minutes)
        assertEquals("Should have correct tasks", 3, snapshot.tasks)
        assertEquals("Grammar should be 100% of time", 1.0, snapshot.grammarShare, 0.01)
    }

    /**
     * Test model diagnostics
     */
    @Test
    fun testModelDiagnostics() {
        val (X, y) = linearTrendData
        val predictor = EnhancedLinearPredictor()
        
        predictor.fitWithDiagnostics(X, y, lambda = 0.01)
        val diagnostics = predictor.getDiagnostics()
        
        assertNotNull("Should have diagnostics", diagnostics)
        diagnostics?.let {
            assertTrue("R² should be high for linear data", it.rSquared > 0.8)
            assertTrue("Should be well conditioned", it.isWellConditioned)
            assertTrue("Should be reliable", it.isReliable)
            assertEquals("Should use all data points", X.size, it.effectiveDataPoints)
        }
    }

    /**
     * Test forecaster with insufficient data
     */
    @Test
    fun testInsufficientDataHandling() = runBlocking {
        val forecaster = ScoreForecaster()
        
        // Test with too few snapshots
        val fewSnapshots = generateSyntheticSnapshots(5) // Less than minimum required
        val result = forecaster.trainFromHistory(fewSnapshots)
        
        assertTrue("Should return insufficient data", result is TrainingResult.InsufficientData)
        
        // Test prediction without training
        assertFalse("Model should not be ready", forecaster.isModelReady())
        
        val prediction = forecaster.forecastNextExam(fewSnapshots.last())
        assertNull("Should not predict without training", prediction)
    }

    /**
     * Test proxy score generation
     */
    @Test
    fun testProxyScoreGeneration() {
        val highPerformanceSnapshot = WeeklySnapshot(
            weekEnd = LocalDate.now(),
            minutes = 400, // High volume
            tasks = 20,    // Good task completion
            completionRate = 0.9, // Excellent consistency
            streak = 14,   // Good streak
            readingShare = 0.35,
            listeningShare = 0.25,
            vocabShare = 0.15,
            grammarShare = 0.25
        )
        
        val lowPerformanceSnapshot = WeeklySnapshot(
            weekEnd = LocalDate.now(),
            minutes = 100, // Low volume
            tasks = 5,     // Few tasks
            completionRate = 0.4, // Poor consistency
            streak = 2,    // Short streak
            readingShare = 0.8, // Unbalanced
            listeningShare = 0.1,
            vocabShare = 0.05,
            grammarShare = 0.05
        )
        
        val forecaster = ScoreForecaster()
        
        // Train on mixed data to test proxy scoring
        val mixedSnapshots = listOf(lowPerformanceSnapshot) + generateSyntheticSnapshots(10) + listOf(highPerformanceSnapshot)
        val result = forecaster.trainFromHistory(mixedSnapshots)
        
        if (result is TrainingResult.Success) {
            val highPred = forecaster.forecastNextExam(highPerformanceSnapshot)
            val lowPred = forecaster.forecastNextExam(lowPerformanceSnapshot)
            
            assertNotNull("Should predict for high performance", highPred)
            assertNotNull("Should predict for low performance", lowPred)
            
            if (highPred != null && lowPred != null) {
                assertTrue("High performance should predict higher score", 
                          highPred.scoreMean > lowPred.scoreMean)
            }
        }
    }

    /**
     * Test prediction interval width validation
     */
    @Test
    fun testPredictionIntervalWidth() {
        val (X, y) = generateLinearTrendDataWithNoise(15, noiseLevel = 2.0) // High noise
        val predictor = LinearPredictor()
        
        val fittedModel = predictor.fit(X, y, lambda = 0.1) // Higher regularization
        val testX = doubleArrayOf(7.5)
        
        val result = fittedModel.predictWithInterval(testX, 0.95)
        
        assertTrue("Should have positive interval width", result.intervalWidth > 0)
        assertTrue("Interval should be reasonable size", result.intervalWidth < 50.0) // Not too wide
        assertTrue("Standard error should be positive", result.standardError > 0)
    }

    // Extension function for Random.nextDouble with range
    private fun Random.nextDouble(from: Double, to: Double): Double {
        return from + (to - from) * nextDouble()
    }

    // Extension function for Random.nextInt with range  
    private fun Random.nextInt(from: Int, to: Int): Int {
        return from + nextInt(to - from + 1)
    }
}

/**
 * Performance benchmark tests
 */
class PredictionPerformanceTests {
    
    @Test
    fun testPredictionPerformance() = runBlocking {
        val largeDataset = generateLargeSyntheticData(1000)
        val snapshots = FeatureExtractor.extractWeeklySnapshots(largeDataset)
        
        val startTime = System.currentTimeMillis()
        
        val forecaster = ScoreForecaster()
        val result = forecaster.trainFromHistory(snapshots.take(50)) // Reasonable size
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        assertTrue("Training should complete within 2 seconds", duration < 2000)
        assertTrue("Should handle large dataset", result is TrainingResult.Success)
    }
    
    private fun generateLargeSyntheticData(days: Int): List<UserDailyLoad> {
        val random = Random(456)
        return (0 until days).map { i ->
            val date = LocalDate.now().minusDays((days - i - 1).toLong())
            UserDailyLoad(
                date = date,
                totalMinutes = random.nextInt(20, 120),
                tasksCompleted = random.nextInt(1, 8),
                averageAccuracy = 0.6f + random.nextFloat() * 0.3f,
                skillBreakdown = mapOf(
                    Skill.GRAMMAR to random.nextInt(0, 40),
                    Skill.READING to random.nextInt(0, 50)
                ),
                streakDayNumber = (i % 20) + 1
            )
        }
    }
}