package com.mtlc.studyplan.predict

import kotlin.math.*

/**
 * Pure Kotlin implementation of Ridge Regression using closed-form solution
 * Formula: β = (X^T X + λI)^(-1) X^T y
 */
class LinearPredictor {
    
    private var coefficients: DoubleArray? = null
    private var intercept: Double = 0.0
    private var residualVariance: Double = 0.0
    private var nFeatures: Int = 0
    private var trainingSize: Int = 0
    
    /**
     * Fit ridge regression model using closed-form solution
     */
    fun fit(X: Array<DoubleArray>, y: DoubleArray, lambda: Double = 0.01): LinearPredictor {
        require(X.isNotEmpty()) { "Feature matrix cannot be empty" }
        require(X.size == y.size) { "Feature matrix and target vector must have same number of samples" }
        require(lambda >= 0) { "Lambda must be non-negative" }
        
        val nSamples = X.size
        val nFeats = X[0].size
        this.nFeatures = nFeats
        this.trainingSize = nSamples
        
        try {
            // Add intercept column (bias term)
            val XWithIntercept = addInterceptColumn(X)
            
            // Calculate X^T X
            val XTranspose = transpose(XWithIntercept)
            val XTX = multiply(XTranspose, XWithIntercept)
            
            // Add regularization term λI
            val regularizedXTX = addRegularization(XTX, lambda)
            
            // Calculate X^T y
            val XTy = multiplyVectorByMatrix(XTranspose, y)
            
            // Solve (X^T X + λI)^(-1) X^T y
            val coeffs = solveLinearSystem(regularizedXTX, XTy)
            
            // Extract intercept and coefficients
            this.intercept = coeffs[0]
            this.coefficients = coeffs.sliceArray(1 until coeffs.size)
            
            // Calculate residual variance for prediction intervals
            this.residualVariance = calculateResidualVariance(XWithIntercept, y, coeffs)
            
        } catch (e: Exception) {
            throw IllegalStateException("Failed to fit model: ${e.message}", e)
        }
        
        return this
    }
    
    /**
     * Make prediction for a single sample
     */
    fun predict(x: DoubleArray): Double {
        val coeffs = coefficients ?: throw IllegalStateException("Model not fitted")
        require(x.size == nFeatures) { "Input features must match training feature count: ${x.size} vs $nFeatures" }
        
        return intercept + coeffs.zip(x).sumOf { (coeff, feature) -> coeff * feature }
    }
    
    /**
     * Make predictions for multiple samples
     */
    fun predict(X: Array<DoubleArray>): DoubleArray {
        return X.map { predict(it) }.toDoubleArray()
    }
    
    /**
     * Calculate prediction interval (confidence band)
     */
    fun predictWithInterval(x: DoubleArray, confidence: Double = 0.95): PredictionResult {
        val prediction = predict(x)
        val standardError = calculatePredictionStandardError(x)
        
        // Use t-distribution for small samples, normal for large samples
        val degreesOfFreedom = trainingSize - nFeatures - 1
        val tValue = if (degreesOfFreedom > 30) {
            // Use normal approximation for large samples
            when (confidence) {
                0.95 -> 1.96
                0.90 -> 1.645
                0.99 -> 2.576
                else -> 1.96
            }
        } else {
            // Use t-distribution for small samples (simplified approximation)
            when (confidence) {
                0.95 -> 2.0 + (30 - degreesOfFreedom) * 0.1 / 30 // Rough approximation
                0.90 -> 1.7 + (30 - degreesOfFreedom) * 0.08 / 30
                0.99 -> 2.6 + (30 - degreesOfFreedom) * 0.15 / 30
                else -> 2.0
            }
        }
        
        val margin = tValue * standardError
        
        return PredictionResult(
            mean = prediction,
            lower = prediction - margin,
            upper = prediction + margin,
            standardError = standardError,
            confidence = confidence
        )
    }
    
    /**
     * Get feature importance (absolute coefficient values)
     */
    fun getFeatureImportance(): DoubleArray? {
        return coefficients?.map { abs(it) }?.toDoubleArray()
    }
    
    // Private helper methods for matrix operations
    
    private fun addInterceptColumn(X: Array<DoubleArray>): Array<DoubleArray> {
        return X.map { row -> doubleArrayOf(1.0) + row }.toTypedArray()
    }
    
    private fun transpose(matrix: Array<DoubleArray>): Array<DoubleArray> {
        if (matrix.isEmpty()) return arrayOf()
        
        val rows = matrix.size
        val cols = matrix[0].size
        
        return Array(cols) { col ->
            DoubleArray(rows) { row ->
                matrix[row][col]
            }
        }
    }
    
    private fun multiply(A: Array<DoubleArray>, B: Array<DoubleArray>): Array<DoubleArray> {
        require(A.isNotEmpty() && B.isNotEmpty()) { "Matrices cannot be empty" }
        require(A[0].size == B.size) { "Matrix dimensions incompatible for multiplication" }
        
        val rowsA = A.size
        val colsA = A[0].size
        val colsB = B[0].size
        
        return Array(rowsA) { i ->
            DoubleArray(colsB) { j ->
                (0 until colsA).sumOf { k -> A[i][k] * B[k][j] }
            }
        }
    }
    
    private fun multiplyVectorByMatrix(matrix: Array<DoubleArray>, vector: DoubleArray): DoubleArray {
        require(matrix.isNotEmpty()) { "Matrix cannot be empty" }
        require(matrix[0].size == vector.size) { "Matrix columns must match vector size" }
        
        return DoubleArray(matrix.size) { i ->
            matrix[i].zip(vector).sumOf { (m, v) -> m * v }
        }
    }
    
    private fun addRegularization(matrix: Array<DoubleArray>, lambda: Double): Array<DoubleArray> {
        val n = matrix.size
        val regularized = Array(n) { i ->
            matrix[i].copyOf()
        }
        
        // Add λ to diagonal elements (skip first element which is intercept)
        for (i in 1 until n) {
            regularized[i][i] += lambda
        }
        
        return regularized
    }
    
    private fun solveLinearSystem(A: Array<DoubleArray>, b: DoubleArray): DoubleArray {
        val n = A.size
        require(A.all { it.size == n }) { "Matrix must be square" }
        require(b.size == n) { "Vector size must match matrix size" }
        
        // Create augmented matrix [A|b]
        val augmented = Array(n) { i ->
            A[i].copyOf() + doubleArrayOf(b[i])
        }
        
        // Gaussian elimination with partial pivoting
        for (i in 0 until n) {
            // Find pivot
            var maxRow = i
            for (k in i + 1 until n) {
                if (abs(augmented[k][i]) > abs(augmented[maxRow][i])) {
                    maxRow = k
                }
            }
            
            // Swap rows
            if (maxRow != i) {
                val temp = augmented[i]
                augmented[i] = augmented[maxRow]
                augmented[maxRow] = temp
            }
            
            // Check for singular matrix
            if (abs(augmented[i][i]) < 1e-10) {
                throw IllegalStateException("Matrix is singular or nearly singular")
            }
            
            // Forward elimination
            for (k in i + 1 until n) {
                val factor = augmented[k][i] / augmented[i][i]
                for (j in i until n + 1) {
                    augmented[k][j] -= factor * augmented[i][j]
                }
            }
        }
        
        // Back substitution
        val solution = DoubleArray(n)
        for (i in n - 1 downTo 0) {
            solution[i] = augmented[i][n]
            for (j in i + 1 until n) {
                solution[i] -= augmented[i][j] * solution[j]
            }
            solution[i] /= augmented[i][i]
        }
        
        return solution
    }
    
    private fun calculateResidualVariance(X: Array<DoubleArray>, y: DoubleArray, coeffs: DoubleArray): Double {
        val predictions = X.map { row ->
            row.zip(coeffs).sumOf { (feature, coeff) -> feature * coeff }
        }
        
        val residuals = y.zip(predictions).map { (actual, pred) -> actual - pred }
        val sumSquaredResiduals = residuals.sumOf { it * it }
        
        val degreesOfFreedom = maxOf(1, y.size - coeffs.size)
        return sumSquaredResiduals / degreesOfFreedom
    }
    
    private fun calculatePredictionStandardError(x: DoubleArray): Double {
        // Simplified standard error calculation
        // For exact calculation, we'd need (X^T X)^(-1) which is computationally expensive
        // Use approximation based on residual variance and feature magnitude
        val featureMagnitude = sqrt(x.sumOf { it * it })
        val baseError = sqrt(residualVariance)
        
        // Adjust error based on how far this point is from training data "center"
        val distanceFactor = 1.0 + featureMagnitude * 0.1
        
        return baseError * distanceFactor
    }
}

/**
 * Prediction result with confidence interval
 */
data class PredictionResult(
    val mean: Double,
    val lower: Double,
    val upper: Double,
    val standardError: Double,
    val confidence: Double
) {
    val intervalWidth: Double
        get() = upper - lower
    
    fun isReliable(): Boolean = standardError < mean * 0.2 // Error < 20% of prediction
    
    fun toYdsScale(): PredictionResult {
        return copy(
            mean = mean.coerceIn(0.0, 100.0),
            lower = lower.coerceIn(0.0, 100.0),
            upper = upper.coerceIn(0.0, 100.0)
        )
    }
}

/**
 * Matrix utilities for linear algebra operations
 */
object MatrixUtils {
    
    /**
     * Calculate matrix determinant (for 2x2 and 3x3 matrices)
     */
    fun determinant2x2(matrix: Array<DoubleArray>): Double {
        require(matrix.size == 2 && matrix.all { it.size == 2 }) { "Matrix must be 2x2" }
        return matrix[0][0] * matrix[1][1] - matrix[0][1] * matrix[1][0]
    }
    
    /**
     * Calculate condition number (measure of numerical stability)
     */
    fun estimateConditionNumber(matrix: Array<DoubleArray>): Double {
        if (matrix.isEmpty()) return Double.POSITIVE_INFINITY
        
        // Simplified condition number estimation using diagonal dominance
        var minDiagonal = Double.POSITIVE_INFINITY
        var maxOffDiagonal = 0.0
        
        for (i in matrix.indices) {
            minDiagonal = minOf(minDiagonal, abs(matrix[i][i]))
            for (j in matrix[i].indices) {
                if (i != j) {
                    maxOffDiagonal = maxOf(maxOffDiagonal, abs(matrix[i][j]))
                }
            }
        }
        
        return if (minDiagonal > 1e-10) maxOffDiagonal / minDiagonal else Double.POSITIVE_INFINITY
    }
    
    /**
     * Create identity matrix
     */
    fun identityMatrix(size: Int): Array<DoubleArray> {
        return Array(size) { i ->
            DoubleArray(size) { j ->
                if (i == j) 1.0 else 0.0
            }
        }
    }
    
    /**
     * Add two matrices
     */
    fun addMatrices(A: Array<DoubleArray>, B: Array<DoubleArray>): Array<DoubleArray> {
        require(A.size == B.size) { "Matrices must have same dimensions" }
        require(A.all { it.size == B[0].size }) { "Matrices must have same dimensions" }
        
        return Array(A.size) { i ->
            DoubleArray(A[i].size) { j ->
                A[i][j] + B[i][j]
            }
        }
    }
    
    /**
     * Scale matrix by scalar
     */
    fun scaleMatrix(matrix: Array<DoubleArray>, scalar: Double): Array<DoubleArray> {
        return matrix.map { row ->
            row.map { it * scalar }.toDoubleArray()
        }.toTypedArray()
    }
    
    /**
     * Check if matrix is well-conditioned for inversion
     */
    fun isWellConditioned(matrix: Array<DoubleArray>, threshold: Double = 1e6): Boolean {
        val conditionNumber = estimateConditionNumber(matrix)
        return conditionNumber < threshold
    }
    
    /**
     * Calculate vector norm
     */
    fun vectorNorm(vector: DoubleArray): Double {
        return sqrt(vector.sumOf { it * it })
    }
    
    /**
     * Calculate dot product
     */
    fun dotProduct(a: DoubleArray, b: DoubleArray): Double {
        require(a.size == b.size) { "Vectors must have same size" }
        return a.zip(b).sumOf { (x, y) -> x * y }
    }
}

/**
 * Model validation and diagnostics
 */
data class ModelDiagnostics(
    val rSquared: Double,
    val meanAbsoluteError: Double,
    val rootMeanSquaredError: Double,
    val isWellConditioned: Boolean,
    val effectiveDataPoints: Int,
    val regularizationStrength: Double
) {
    val isReliable: Boolean
        get() = rSquared > 0.3 && isWellConditioned && effectiveDataPoints >= 5
    
    fun getQualityDescription(): String = when {
        rSquared > 0.7 && isWellConditioned -> "Excellent"
        rSquared > 0.5 && isWellConditioned -> "Good"
        rSquared > 0.3 -> "Fair"
        else -> "Poor"
    }
}

/**
 * Enhanced LinearPredictor with diagnostics
 */
class EnhancedLinearPredictor {
    
    private val basePredictor = LinearPredictor()
    
    private var diagnostics: ModelDiagnostics? = null
    
    /**
     * Fit model with diagnostic calculation
     */
    fun fitWithDiagnostics(X: Array<DoubleArray>, y: DoubleArray, lambda: Double = 0.01): EnhancedLinearPredictor {
        // Fit the base model
        basePredictor.fit(X, y, lambda)
        
        // Calculate diagnostics
        val predictions = predict(X)
        val rSquared = calculateRSquared(y, predictions)
        val mae = calculateMAE(y, predictions)
        val rmse = calculateRMSE(y, predictions)
        
        // Check matrix conditioning
        val XWithIntercept = addInterceptColumn(X)
        val XTranspose = transpose(XWithIntercept)
        val XTX = multiply(XTranspose, XWithIntercept)
        val isWellConditioned = MatrixUtils.isWellConditioned(XTX)
        
        diagnostics = ModelDiagnostics(
            rSquared = rSquared,
            meanAbsoluteError = mae,
            rootMeanSquaredError = rmse,
            isWellConditioned = isWellConditioned,
            effectiveDataPoints = y.size,
            regularizationStrength = lambda
        )
        
        return this
    }
    
    /**
     * Get model diagnostics
     */
    fun getDiagnostics(): ModelDiagnostics? = diagnostics
    
    /**
     * Delegate prediction methods to base predictor
     */
    fun predict(x: DoubleArray): Double = basePredictor.predict(x)
    fun predict(X: Array<DoubleArray>): DoubleArray = basePredictor.predict(X)
    fun predictWithInterval(x: DoubleArray, confidence: Double = 0.95): PredictionResult = basePredictor.predictWithInterval(x, confidence)
    
    // Private helper methods repeated here since Kotlin doesn't have inheritance of private methods
    private fun addInterceptColumn(X: Array<DoubleArray>): Array<DoubleArray> {
        return X.map { row -> doubleArrayOf(1.0) + row }.toTypedArray()
    }
    
    private fun transpose(matrix: Array<DoubleArray>): Array<DoubleArray> {
        if (matrix.isEmpty()) return arrayOf()
        val rows = matrix.size
        val cols = matrix[0].size
        return Array(cols) { col -> DoubleArray(rows) { row -> matrix[row][col] } }
    }
    
    private fun multiply(A: Array<DoubleArray>, B: Array<DoubleArray>): Array<DoubleArray> {
        require(A.isNotEmpty() && B.isNotEmpty()) { "Matrices cannot be empty" }
        require(A[0].size == B.size) { "Matrix dimensions incompatible" }
        
        val rowsA = A.size
        val colsA = A[0].size
        val colsB = B[0].size
        
        return Array(rowsA) { i ->
            DoubleArray(colsB) { j ->
                (0 until colsA).sumOf { k -> A[i][k] * B[k][j] }
            }
        }
    }
    
    private fun calculateRSquared(actual: DoubleArray, predicted: DoubleArray): Double {
        val actualMean = actual.average()
        val totalSumSquares = actual.sumOf { (it - actualMean).pow(2) }
        val residualSumSquares = actual.zip(predicted).sumOf { (a, p) -> (a - p).pow(2) }
        
        return if (totalSumSquares > 1e-10) {
            1.0 - (residualSumSquares / totalSumSquares)
        } else 0.0
    }
    
    private fun calculateMAE(actual: DoubleArray, predicted: DoubleArray): Double {
        return actual.zip(predicted).map { (a, p) -> abs(a - p) }.average()
    }
    
    private fun calculateRMSE(actual: DoubleArray, predicted: DoubleArray): Double {
        val mse = actual.zip(predicted).map { (a, p) -> (a - p).pow(2) }.average()
        return sqrt(mse)
    }
}

/**
 * Cross-validation utilities for model selection
 */
object CrossValidation {
    
    /**
     * Perform k-fold cross-validation
     */
    fun kFoldCV(X: Array<DoubleArray>, y: DoubleArray, k: Int = 5, lambda: Double = 0.01): CVResult {
        require(k > 1) { "K must be > 1" }
        require(X.size >= k) { "Need at least k samples for k-fold CV" }
        
        val n = X.size
        val foldSize = n / k
        val scores = mutableListOf<Double>()
        
        for (fold in 0 until k) {
            val testStart = fold * foldSize
            val testEnd = if (fold == k - 1) n else (fold + 1) * foldSize
            
            // Split data
            val trainX = X.filterIndexed { i, _ -> i < testStart || i >= testEnd }.toTypedArray()
            val trainY = y.filterIndexed { i, _ -> i < testStart || i >= testEnd }.toDoubleArray()
            val testX = X.sliceArray(testStart until testEnd)
            val testY = y.sliceArray(testStart until testEnd)
            
            // Train and evaluate
            try {
                val model = LinearPredictor().fit(trainX, trainY, lambda)
                val predictions = model.predict(testX)
                val score = calculateRSquared(testY, predictions)
                scores.add(score)
            } catch (e: Exception) {
                scores.add(0.0) // Failed fold
            }
        }
        
        return CVResult(
            meanScore = scores.average(),
            stdScore = sqrt(scores.map { (it - scores.average()).pow(2) }.average()),
            scores = scores
        )
    }
    
    private fun calculateRSquared(actual: DoubleArray, predicted: DoubleArray): Double {
        val actualMean = actual.average()
        val totalSumSquares = actual.sumOf { (it - actualMean).pow(2) }
        val residualSumSquares = actual.zip(predicted).sumOf { (a, p) -> (a - p).pow(2) }
        
        return if (totalSumSquares > 1e-10) {
            1.0 - (residualSumSquares / totalSumSquares)
        } else 0.0
    }
}

/**
 * Cross-validation result
 */
data class CVResult(
    val meanScore: Double,
    val stdScore: Double,
    val scores: List<Double>
) {
    val isStable: Boolean
        get() = stdScore < 0.2 // CV scores have low variance
}