package com.mtlc.studyplan.testutil

import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import kotlin.system.measureTimeMillis

/**
 * Performance Test Rule
 * Measures test execution time and records results for CI/CD integration
 */
class PerformanceTestRule : TestRule {

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                val startTime = System.nanoTime()

                try {
                    base.evaluate()

                    val duration = (System.nanoTime() - startTime) / 1_000_000 // Convert to milliseconds

                    // Record successful test
                    TestResultsAggregator.recordTestResult(
                        description = description,
                        status = TestResultsAggregator.TestStatus.PASSED,
                        duration = duration,
                        performanceMetrics = mapOf(
                            "executionTime" to duration,
                            "testCategory" to determineTestCategory(description).name
                        )
                    )

                    // Log performance for slow tests
                    if (duration > TestConfigurationManager.testTimeout) {
                        println("⚠️ SLOW TEST: ${description.methodName} took ${duration}ms")
                    }

                } catch (throwable: Throwable) {
                    val duration = (System.nanoTime() - startTime) / 1_000_000

                    // Record failed test
                    TestResultsAggregator.recordTestResult(
                        description = description,
                        status = TestResultsAggregator.TestStatus.FAILED,
                        duration = duration,
                        failure = org.junit.runner.notification.Failure(description, throwable),
                        performanceMetrics = mapOf(
                            "executionTime" to duration,
                            "failureType" to throwable::class.java.simpleName
                        )
                    )

                    // Re-throw the exception
                    throw throwable
                }
            }
        }
    }

    private fun determineTestCategory(description: Description): TestCategory {
        val className = description.className
        val methodName = description.methodName

        return when {
            className.contains("Database") || className.contains("Repository") -> TestCategory.INTEGRATION
            className.contains("LoadTest") || className.contains("Performance") || methodName.contains("Performance") -> TestCategory.PERFORMANCE
            className.contains("UI") || className.contains("Compose") || methodName.contains("UI") -> TestCategory.UI
            else -> TestCategory.UNIT
        }
    }
}