package com.mtlc.studyplan.testutil

import android.os.Build

/**
 * Test Configuration Manager
 * Manages test execution parameters based on environment and CI/CD context
 */
object TestConfigurationManager {

    // Environment detection
    val isCI: Boolean = System.getenv("CI") == "true" ||
                       System.getenv("CONTINUOUS_INTEGRATION") == "true" ||
                       System.getenv("BUILD_NUMBER") != null

    val isEmulator: Boolean = Build.FINGERPRINT.contains("generic") ||
                             Build.FINGERPRINT.contains("emulator") ||
                             Build.MODEL.contains("Emulator")

    val isDebugBuild: Boolean = Build.TYPE == "debug"

    // Test timeouts (CI runs faster, local can be more lenient)
    val testTimeout: Long = when {
        isCI -> 30000L  // 30 seconds for CI
        isEmulator -> 45000L  // 45 seconds for emulator
        else -> 60000L  // 60 seconds for device
    }

    val loadTestTimeout: Long = when {
        isCI -> 120000L  // 2 minutes for CI
        isEmulator -> 180000L  // 3 minutes for emulator
        else -> 300000L  // 5 minutes for device
    }

    // Test execution flags
    val enablePerformanceTests: Boolean = !isCI || System.getenv("RUN_PERF_TESTS") == "true"
    val enableLoadTests: Boolean = !isCI || System.getenv("RUN_LOAD_TESTS") == "true"
    val enableUITests: Boolean = !isCI || System.getenv("RUN_UI_TESTS") == "true"
    val enableFlakyTests: Boolean = System.getenv("RUN_FLAKY_TESTS") == "true"

    // Test data configuration
    val testDataSize: Int = when {
        isCI -> 100  // Smaller dataset for CI
        else -> 1000  // Larger dataset for local testing
    }

    val concurrentUsersForLoadTest: Int = when {
        isCI -> 5
        isEmulator -> 10
        else -> 20
    }

    // Reporting configuration
    val enableDetailedReporting: Boolean = !isCI || System.getenv("DETAILED_REPORTS") == "true"
    val reportFormat: ReportFormat = when (System.getenv("REPORT_FORMAT")) {
        "XML" -> ReportFormat.XML
        "HTML" -> ReportFormat.HTML
        "JSON" -> ReportFormat.JSON
        else -> ReportFormat.JSON
    }

    // Device-specific configuration
    val deviceProfile: DeviceProfile = determineDeviceProfile()

    private fun determineDeviceProfile(): DeviceProfile {
        val screenWidth = android.content.res.Resources.getSystem().displayMetrics.widthPixels
        val screenHeight = android.content.res.Resources.getSystem().displayMetrics.heightPixels
        val density = android.content.res.Resources.getSystem().displayMetrics.density

        return when {
            screenWidth >= 1200 -> DeviceProfile.TABLET_LARGE
            screenWidth >= 800 -> DeviceProfile.TABLET
            screenHeight >= 1920 -> DeviceProfile.PHONE_LARGE
            else -> DeviceProfile.PHONE
        }
    }

    // Test categories to run
    val enabledTestCategories: Set<TestCategory> = determineEnabledCategories()

    private fun determineEnabledCategories(): Set<TestCategory> {
        val allCategories = TestCategory.entries.toSet()

        return when {
            isCI -> {
                // In CI, run unit and integration tests by default
                setOf(TestCategory.UNIT, TestCategory.INTEGRATION)
            }
            else -> {
                // Locally, run all tests
                allCategories
            }
        }.let { baseCategories ->
            // Add performance tests if enabled
            if (enablePerformanceTests) {
                baseCategories + TestCategory.PERFORMANCE
            } else {
                baseCategories
            }
        }.let { categories ->
            // Add load tests if enabled
            if (enableLoadTests) {
                categories + TestCategory.LOAD
            } else {
                categories
            }
        }.let { categories ->
            // Add UI tests if enabled
            if (enableUITests) {
                categories + TestCategory.UI
            } else {
                categories
            }
        }
    }

    // Test retry configuration
    val maxRetries: Int = when {
        isCI -> 2  // Retry flaky tests in CI
        else -> 0  // Don't retry locally
    }

    val retryOnlyFlakyTests: Boolean = isCI

    // Logging configuration
    val enableVerboseLogging: Boolean = !isCI || System.getenv("VERBOSE_LOGGING") == "true"
    val logTestResults: Boolean = true
    val logPerformanceMetrics: Boolean = enablePerformanceTests

    // Cleanup configuration
    val cleanupAfterTest: Boolean = true
    val cleanupTestData: Boolean = true
    val resetAppState: Boolean = !isCI  // Don't reset in CI to save time

    fun shouldRunTest(testCategory: TestCategory): Boolean {
        return testCategory in enabledTestCategories
    }

    fun getTestTimeout(testType: TestType): Long {
        return when (testType) {
            TestType.UNIT -> testTimeout / 2
            TestType.INTEGRATION -> testTimeout
            TestType.UI -> testTimeout * 2
            TestType.PERFORMANCE -> testTimeout * 3
            TestType.LOAD -> loadTestTimeout
        }
    }

    fun getTestDataSize(testType: TestType): Int {
        return when (testType) {
            TestType.LOAD -> testDataSize / 10  // Smaller for load tests
            TestType.PERFORMANCE -> testDataSize / 2  // Medium for performance tests
            else -> testDataSize
        }
    }

    fun printConfiguration() {
        println("=== Test Configuration ===")
        println("Environment: ${if (isCI) "CI" else "Local"}")
        println("Device: ${if (isEmulator) "Emulator" else "Physical Device"} (${deviceProfile})")
        println("Build Type: ${if (isDebugBuild) "Debug" else "Release"}")
        println("Test Timeout: ${testTimeout}ms")
        println("Load Test Timeout: ${loadTestTimeout}ms")
        println("Enabled Categories: ${enabledTestCategories.joinToString()}")
        println("Performance Tests: $enablePerformanceTests")
        println("Load Tests: $enableLoadTests")
        println("UI Tests: $enableUITests")
        println("Test Data Size: $testDataSize")
        println("Concurrent Users: $concurrentUsersForLoadTest")
        println("Report Format: $reportFormat")
        println("Max Retries: $maxRetries")
        println("==========================")
    }
}

enum class DeviceProfile {
    PHONE, PHONE_LARGE, TABLET, TABLET_LARGE
}

enum class TestCategory {
    UNIT, INTEGRATION, UI, PERFORMANCE, LOAD
}

enum class TestType {
    UNIT, INTEGRATION, UI, PERFORMANCE, LOAD
}

enum class ReportFormat {
    XML, HTML, JSON
}