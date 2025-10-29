package com.mtlc.studyplan.testutil

import com.mtlc.studyplan.validation.LoadTestSuite
import org.junit.runner.Description
import org.junit.runner.Result
import org.junit.runner.notification.Failure
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Test Results Aggregator
 * Collects, analyzes, and reports test results for CI/CD integration
 */
object TestResultsAggregator {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val testResults = mutableListOf<TestResult>()
    private val startTime = System.currentTimeMillis()

    data class TestResult(
        val testName: String,
        val className: String,
        val category: TestCategory,
        val status: TestStatus,
        val duration: Long,
        val failure: Failure? = null,
        val performanceMetrics: Map<String, Any> = emptyMap()
    )

    data class TestSuiteReport(
        val totalTests: Int,
        val passedTests: Int,
        val failedTests: Int,
        val skippedTests: Int,
        val totalDuration: Long,
        val averageDuration: Long,
        val successRate: Double,
        val resultsByCategory: Map<TestCategory, CategoryResults>,
        val topFailures: List<TestFailure>,
        val performanceSummary: PerformanceSummary,
        val recommendations: List<String>,
        val generatedAt: Long = System.currentTimeMillis()
    )

    data class CategoryResults(
        val total: Int,
        val passed: Int,
        val failed: Int,
        val skipped: Int,
        val averageDuration: Long
    )

    data class TestFailure(
        val testName: String,
        val failure: Failure,
        val frequency: Int
    )

    data class PerformanceSummary(
        val loadTestResults: List<LoadTestSuite.LoadTestResult> = emptyList(),
        val averageResponseTime: Long = 0,
        val peakMemoryUsage: Long = 0,
        val throughput: Double = 0.0
    )

    enum class TestStatus {
        PASSED, FAILED, SKIPPED, ERROR
    }

    /**
     * Record a test result
     */
    fun recordTestResult(
        description: Description,
        status: TestStatus,
        duration: Long,
        failure: Failure? = null,
        performanceMetrics: Map<String, Any> = emptyMap()
    ) {
        val category = determineTestCategory(description)
        val result = TestResult(
            testName = description.methodName ?: "Unknown",
            className = description.className ?: "Unknown",
            category = category,
            status = status,
            duration = duration,
            failure = failure,
            performanceMetrics = performanceMetrics
        )

        synchronized(testResults) {
            testResults.add(result)
        }

        // Log immediately for CI visibility
        logTestResult(result)
    }

    /**
     * Record JUnit test result
     */
    fun recordJUnitResult(result: Result) {
        result.failures.forEach { failure ->
            recordTestResult(
                failure.description,
                TestStatus.FAILED,
                0L, // Duration not available from JUnit
                failure
            )
        }

        // Record successful tests if we have access to them
        // Note: JUnit Result doesn't provide individual test durations by default
    }

    /**
     * Generate comprehensive test report
     */
    fun generateComprehensiveReport(): TestSuiteReport {
        val endTime = System.currentTimeMillis()
        val totalDuration = endTime - startTime

        val resultsByCategory = testResults.groupBy { it.category }
            .mapValues { (_, results) ->
                CategoryResults(
                    total = results.size,
                    passed = results.count { it.status == TestStatus.PASSED },
                    failed = results.count { it.status == TestStatus.FAILED },
                    skipped = results.count { it.status == TestStatus.SKIPPED },
                    averageDuration = results.map { it.duration }.average().toLong()
                )
            }

        val totalTests = testResults.size
        val passedTests = testResults.count { it.status == TestStatus.PASSED }
        val failedTests = testResults.count { it.status == TestStatus.FAILED }
        val skippedTests = testResults.count { it.status == TestStatus.SKIPPED }
        val successRate = if (totalTests > 0) (passedTests.toDouble() / totalTests) * 100 else 0.0
        val averageDuration = testResults.map { it.duration }.average().toLong()

        val topFailures = testResults
            .filter { it.status == TestStatus.FAILED }
            .groupBy { it.testName }
            .map { (testName, failures) ->
                TestFailure(
                    testName = testName,
                    failure = failures.firstOrNull()?.failure ?: org.junit.runner.notification.Failure(
                        org.junit.runner.Description.createTestDescription("UnknownClass", testName),
                        Throwable("Unknown failure")
                    ),
                    frequency = failures.size
                )
            }
            .sortedByDescending { it.frequency }
            .take(10)

        val performanceSummary = PerformanceSummary(
            // Load test results would be collected separately
            averageResponseTime = testResults
                .mapNotNull { it.performanceMetrics["responseTime"] as? Long }
                .average().toLong(),
            peakMemoryUsage = testResults
                .mapNotNull { it.performanceMetrics["memoryUsage"] as? Long }
                .maxOrNull() ?: 0L,
            throughput = testResults
                .mapNotNull { it.performanceMetrics["throughput"] as? Double }
                .average()
        )

        val recommendations = generateRecommendations(
            successRate,
            failedTests,
            performanceSummary,
            resultsByCategory
        )

        return TestSuiteReport(
            totalTests = totalTests,
            passedTests = passedTests,
            failedTests = failedTests,
            skippedTests = skippedTests,
            totalDuration = totalDuration,
            averageDuration = averageDuration,
            successRate = successRate,
            resultsByCategory = resultsByCategory,
            topFailures = topFailures,
            performanceSummary = performanceSummary,
            recommendations = recommendations
        )
    }

    /**
     * Export report to CI/CD system
     */
    fun exportToCI(report: TestSuiteReport) {
        when (TestConfigurationManager.reportFormat) {
            ReportFormat.JSON -> exportAsJson(report)
            ReportFormat.XML -> exportAsXml(report)
            ReportFormat.HTML -> exportAsHtml(report)
        }

        // Set CI environment variables
        setCIEnvironmentVariables(report)

        // Log summary for CI
        logCISummary(report)
    }

    /**
     * Export as JSON
     */
    private fun exportAsJson(report: TestSuiteReport) {
        val json = buildString {
            appendLine("{")
            appendLine("  \"summary\": {")
            appendLine("    \"totalTests\": ${report.totalTests},")
            appendLine("    \"passedTests\": ${report.passedTests},")
            appendLine("    \"failedTests\": ${report.failedTests},")
            appendLine("    \"skippedTests\": ${report.skippedTests},")
            appendLine("    \"successRate\": ${report.successRate},")
            appendLine("    \"totalDuration\": ${report.totalDuration}")
            appendLine("  },")
            appendLine("  \"categories\": {")
            report.resultsByCategory.forEach { (category, results) ->
                appendLine("    \"${category}\": {")
                appendLine("      \"total\": ${results.total},")
                appendLine("      \"passed\": ${results.passed},")
                appendLine("      \"failed\": ${results.failed}")
                appendLine("    },")
            }
            if (report.resultsByCategory.isNotEmpty()) {
                // Remove last comma
                deleteCharAt(length - 2)
            }
            appendLine("  },")
            appendLine("  \"recommendations\": ${report.recommendations.joinToString(", ", "[", "]") { "\"$it\"" }}")
            appendLine("}")
        }

        val reportFile = File("test-results.json")
        reportFile.writeText(json)
        println("Test report exported to: ${reportFile.absolutePath}")
    }

    /**
     * Export as XML (JUnit format)
     */
    private fun exportAsXml(report: TestSuiteReport) {
        val xml = buildString {
            appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
            appendLine("<testsuites>")
            appendLine("  <testsuite name=\"StudyPlanTestSuite\" tests=\"${report.totalTests}\" failures=\"${report.failedTests}\" skipped=\"${report.skippedTests}\" time=\"${report.totalDuration / 1000.0}\">")

            testResults.forEach { result ->
                val status = when (result.status) {
                    TestStatus.PASSED -> "passed"
                    TestStatus.FAILED -> "failed"
                    TestStatus.SKIPPED -> "skipped"
                    TestStatus.ERROR -> "error"
                }

                appendLine("    <testcase name=\"${result.testName}\" classname=\"${result.className}\" time=\"${result.duration / 1000.0}\">")

                if (result.failure != null) {
                    appendLine("      <failure message=\"${result.failure.message}\" type=\"${result.failure.exception::class.java.name}\">")
                    appendLine("        ${result.failure.trace}")
                    appendLine("      </failure>")
                }

                appendLine("    </testcase>")
            }

            appendLine("  </testsuite>")
            appendLine("</testsuites>")
        }

        val reportFile = File("test-results.xml")
        reportFile.writeText(xml)
        println("JUnit XML report exported to: ${reportFile.absolutePath}")
    }

    /**
     * Export as HTML
     */
    private fun exportAsHtml(report: TestSuiteReport) {
        val html = buildString {
            appendLine("<!DOCTYPE html>")
            appendLine("<html>")
            appendLine("<head>")
            appendLine("  <title>Test Results Report</title>")
            appendLine("  <style>")
            appendLine("    body { font-family: Arial, sans-serif; margin: 20px; }")
            appendLine("    .summary { background: #f0f0f0; padding: 15px; border-radius: 5px; margin-bottom: 20px; }")
            appendLine("    .passed { color: green; }")
            appendLine("    .failed { color: red; }")
            appendLine("    .skipped { color: orange; }")
            appendLine("    table { border-collapse: collapse; width: 100%; }")
            appendLine("    th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }")
            appendLine("    th { background-color: #f2f2f2; }")
            appendLine("  </style>")
            appendLine("</head>")
            appendLine("<body>")
            appendLine("  <h1>Test Results Report</h1>")
            appendLine("  <div class=\"summary\">")
            appendLine("    <h2>Summary</h2>")
            appendLine("    <p>Total Tests: ${report.totalTests}</p>")
            appendLine("    <p class=\"passed\">Passed: ${report.passedTests}</p>")
            appendLine("    <p class=\"failed\">Failed: ${report.failedTests}</p>")
            appendLine("    <p class=\"skipped\">Skipped: ${report.skippedTests}</p>")
            appendLine("    <p>Success Rate: ${String.format("%.1f", report.successRate)}%</p>")
            appendLine("    <p>Total Duration: ${report.totalDuration}ms</p>")
            appendLine("  </div>")

            if (report.topFailures.isNotEmpty()) {
                appendLine("  <h2>Top Failures</h2>")
                appendLine("  <table>")
                appendLine("    <tr><th>Test Name</th><th>Frequency</th><th>Error</th></tr>")
                report.topFailures.forEach { failure ->
                    appendLine("    <tr>")
                    appendLine("      <td>${failure.testName}</td>")
                    appendLine("      <td>${failure.frequency}</td>")
                    appendLine("      <td>${failure.failure.message}</td>")
                    appendLine("    </tr>")
                }
                appendLine("  </table>")
            }

            appendLine("  <h2>Recommendations</h2>")
            appendLine("  <ul>")
            report.recommendations.forEach { recommendation ->
                appendLine("    <li>$recommendation</li>")
            }
            appendLine("  </ul>")

            appendLine("</body>")
            appendLine("</html>")
        }

        val reportFile = File("test-results.html")
        reportFile.writeText(html)
        println("HTML report exported to: ${reportFile.absolutePath}")
    }

    /**
     * Set CI environment variables for downstream jobs
     */
    private fun setCIEnvironmentVariables(report: TestSuiteReport) {
        // These would be set as environment variables in CI
        println("##teamcity[setParameter name='test.successRate' value='${report.successRate}']")
        println("##teamcity[setParameter name='test.failedCount' value='${report.failedTests}']")
        println("##teamcity[setParameter name='test.totalCount' value='${report.totalTests}']")
        println("##teamcity[setParameter name='test.duration' value='${report.totalDuration}']")

        // GitHub Actions
        println("::set-output name=test-success-rate::${report.successRate}")
        println("::set-output name=test-failed-count::${report.failedTests}")
        println("::set-output name=test-total-count::${report.totalTests}")
    }

    /**
     * Log summary for CI systems
     */
    private fun logCISummary(report: TestSuiteReport) {
        println("=== Test Execution Summary ===")
        println("Total Tests: ${report.totalTests}")
        println("Passed: ${report.passedTests}")
        println("Failed: ${report.failedTests}")
        println("Skipped: ${report.skippedTests}")
        println("Success Rate: ${String.format("%.1f", report.successRate)}%")
        println("Total Duration: ${report.totalDuration}ms")
        println("Average Test Duration: ${report.averageDuration}ms")
        println("==============================")

        if (report.failedTests > 0) {
            println("❌ Test suite has failures")
            report.topFailures.take(5).forEach { failure ->
                println("  - ${failure.testName}: ${failure.failure.message}")
            }
        } else {
            println("✅ All tests passed")
        }
    }

    private fun determineTestCategory(description: Description): TestCategory {
        val className = description.className

        return when {
            className.contains("Database") || className.contains("Repository") -> TestCategory.INTEGRATION
            className.contains("LoadTest") || className.contains("Performance") -> TestCategory.PERFORMANCE
            className.contains("UI") || className.contains("Compose") -> TestCategory.UI
            else -> TestCategory.UNIT
        }
    }

    private fun logTestResult(result: TestResult) {
        val status = when (result.status) {
            TestStatus.PASSED -> "✅ PASS"
            TestStatus.FAILED -> "❌ FAIL"
            TestStatus.SKIPPED -> "⏭️ SKIP"
            TestStatus.ERROR -> "💥 ERROR"
        }

        println("$status ${result.className}.${result.testName} (${result.duration}ms)")

        if (result.failure != null) {
            println("   Error: ${result.failure.message}")
        }
    }

    private fun generateRecommendations(
        successRate: Double,
        failedTests: Int,
        performance: PerformanceSummary,
        categories: Map<TestCategory, CategoryResults>
    ): List<String> {
        val recommendations = mutableListOf<String>()

        if (successRate < 80.0) {
            recommendations.add("Overall test success rate is low (${String.format("%.1f", successRate)}%). Focus on fixing failing tests.")
        }

        if (failedTests > 10) {
            recommendations.add("High number of test failures ($failedTests). Consider investigating test stability and environment issues.")
        }

        categories.forEach { (category, results) ->
            val categorySuccessRate = if (results.total > 0) (results.passed.toDouble() / results.total) * 100 else 0.0
            if (categorySuccessRate < 70.0) {
                recommendations.add("$category tests have low success rate (${String.format("%.1f", categorySuccessRate)}%). Review $category test implementation.")
            }
        }

        if (performance.averageResponseTime > 1000) {
            recommendations.add("Average response time is high (${performance.averageResponseTime}ms). Consider performance optimizations.")
        }

        if (performance.throughput < 10.0 && performance.throughput > 0) {
            recommendations.add("Test throughput is low (${String.format("%.1f", performance.throughput)} ops/sec). Consider parallel test execution.")
        }

        if (recommendations.isEmpty()) {
            recommendations.add("Test suite is performing well. Continue monitoring and consider adding more comprehensive tests.")
        }

        return recommendations
    }

    /**
     * Clear all recorded results
     */
    fun clearResults() {
        testResults.clear()
    }

    /**
     * Get current test results
     */
    fun getTestResults(): List<TestResult> = testResults.toList()
}
