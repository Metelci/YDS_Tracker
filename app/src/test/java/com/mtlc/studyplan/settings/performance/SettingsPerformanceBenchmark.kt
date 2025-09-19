package com.mtlc.studyplan.settings.performance

import android.content.Context
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mtlc.studyplan.settings.PerformanceTest
import com.mtlc.studyplan.settings.backup.SettingsBackupManager
import com.mtlc.studyplan.settings.repository.SettingsRepositoryImpl
import com.mtlc.studyplan.settings.search.SettingsSearchEngine
import com.mtlc.studyplan.settings.search.SearchResultHighlighter
import com.mtlc.studyplan.settings.security.SettingsEncryption
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.system.measureTimeMillis

@RunWith(AndroidJUnit4::class)
class SettingsPerformanceBenchmark : PerformanceTest {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    private lateinit var context: Context
    private lateinit var repository: SettingsRepositoryImpl
    private lateinit var searchEngine: SettingsSearchEngine
    private lateinit var backupManager: SettingsBackupManager
    private lateinit var encryption: SettingsEncryption
    private lateinit var highlighter: SearchResultHighlighter

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        repository = SettingsRepositoryImpl(context)
        highlighter = SearchResultHighlighter(context)
        searchEngine = SettingsSearchEngine(context, repository, highlighter)
        encryption = SettingsEncryption(context)
        backupManager = SettingsBackupManager(context, repository, encryption)
    }

    @Test
    fun benchmarkSettingWrite() {
        benchmarkRule.measureRepeated {
            runBlocking {
                repository.setSetting("benchmark_key_${System.nanoTime()}", "benchmark_value")
            }
        }
    }

    @Test
    fun benchmarkSettingRead() = runBlocking {
        // Setup: Create test settings
        repeat(1000) { i ->
            repository.setSetting("read_test_$i", "value_$i")
        }

        benchmarkRule.measureRepeated {
            runBlocking {
                repository.getSetting("read_test_500", "default")
            }
        }
    }

    @Test
    fun benchmarkSettingSearch() = runBlocking {
        // Setup: Create large dataset
        repeat(10000) { i ->
            repository.setSetting("search_test_$i", "searchable_value_$i")
        }

        benchmarkRule.measureRepeated {
            runBlocking {
                searchEngine.search("searchable")
            }
        }
    }

    @Test
    fun benchmarkSettingsBulkWrite() {
        val bulkSettings = (1..1000).associate { "bulk_key_$it" to "bulk_value_$it" }

        benchmarkRule.measureRepeated {
            runBlocking {
                repository.importSettings(bulkSettings)
            }
        }
    }

    @Test
    fun benchmarkSettingsBackupCreate() = runBlocking {
        // Setup: Create test data
        repeat(5000) { i ->
            repository.setSetting("backup_test_$i", "backup_value_$i")
        }

        benchmarkRule.measureRepeated {
            runBlocking {
                backupManager.exportSettings()
            }
        }
    }

    @Test
    fun benchmarkSettingsEncryption() {
        val testData = "This is test data for encryption benchmarking".repeat(100)

        benchmarkRule.measureRepeated {
            val encrypted = encryption.encryptData(testData)
            encryption.decryptData(encrypted)
        }
    }

    @Test
    fun benchmarkSearchHighlighting() {
        val testText = "This is a long text that contains multiple searchable terms and should be used for highlighting performance testing".repeat(10)
        val query = "searchable"

        benchmarkRule.measureRepeated {
            highlighter.highlightMatches(testText, query)
        }
    }

    @Test
    fun benchmarkConcurrentReads() = runBlocking {
        // Setup test data
        repeat(1000) { i ->
            repository.setSetting("concurrent_read_$i", "value_$i")
        }

        benchmarkRule.measureRepeated {
            runBlocking {
                val jobs = (1..100).map { i ->
                    kotlinx.coroutines.async {
                        repository.getSetting("concurrent_read_${i % 1000}", "default")
                    }
                }
                jobs.forEach { it.await() }
            }
        }
    }

    @Test
    fun benchmarkConcurrentWrites() {
        benchmarkRule.measureRepeated {
            runBlocking {
                val jobs = (1..100).map { i ->
                    kotlinx.coroutines.async {
                        repository.setSetting("concurrent_write_$i", "value_$i")
                    }
                }
                jobs.forEach { it.await() }
            }
        }
    }

    @Test
    fun benchmarkSearchWithLargeDataset() = runBlocking {
        // Create large dataset with realistic data
        val categories = listOf("notification", "privacy", "accessibility", "theme", "backup", "sync")
        val types = listOf("enabled", "disabled", "mode", "setting", "preference", "option")

        repeat(50000) { i ->
            val category = categories[i % categories.size]
            val type = types[i % types.size]
            repository.setSetting("${category}_${type}_$i", "value_$i")
        }

        benchmarkRule.measureRepeated {
            runBlocking {
                searchEngine.search("notification")
            }
        }
    }

    @Test
    fun measureMemoryUsageGrowth() = runBlocking {
        val runtime = Runtime.getRuntime()
        val initialMemory = runtime.totalMemory() - runtime.freeMemory()
        val measurements = mutableListOf<Long>()

        // Measure memory at different data sizes
        val dataSizes = listOf(100, 500, 1000, 5000, 10000)

        for (size in dataSizes) {
            // Clear previous data
            repository.clearAllSettings()
            System.gc()

            // Add settings
            repeat(size) { i ->
                repository.setSetting("memory_test_$i", "memory_value_$i".repeat(10))
            }

            // Measure memory
            val currentMemory = runtime.totalMemory() - runtime.freeMemory()
            val memoryIncrease = currentMemory - initialMemory
            measurements.add(memoryIncrease)

            println("Settings count: $size, Memory increase: ${memoryIncrease / 1024}KB")
        }

        // Verify linear growth (not exponential)
        for (i in 1 until measurements.size) {
            val growthRatio = measurements[i].toFloat() / measurements[i - 1]
            assert(growthRatio < 10) { "Memory growth too high: $growthRatio" }
        }
    }

    @Test
    fun measureSearchPerformanceByDataSize() = runBlocking {
        val dataSizes = listOf(100, 1000, 10000, 50000)
        val searchTimes = mutableListOf<Long>()

        for (size in dataSizes) {
            // Setup data
            repository.clearAllSettings()
            repeat(size) { i ->
                repository.setSetting("search_perf_test_$i", "searchable_content_$i")
            }

            // Measure search time
            val searchTime = measureTimeMillis {
                runBlocking {
                    searchEngine.search("searchable")
                }
            }

            searchTimes.add(searchTime)
            println("Data size: $size, Search time: ${searchTime}ms")

            // Assert reasonable performance (should not grow exponentially)
            assert(searchTime < 1000) { "Search time too slow for $size items: ${searchTime}ms" }
        }

        // Verify search time doesn't grow exponentially
        for (i in 1 until searchTimes.size) {
            val timeRatio = searchTimes[i].toFloat() / searchTimes[i - 1]
            val sizeRatio = dataSizes[i].toFloat() / dataSizes[i - 1]

            // Time should not grow faster than data size
            assert(timeRatio <= sizeRatio * 2) {
                "Search time growing too fast: time ratio $timeRatio vs size ratio $sizeRatio"
            }
        }
    }

    @Test
    fun measureBackupPerformanceByDataSize() = runBlocking {
        val dataSizes = listOf(100, 1000, 5000, 10000)
        val backupTimes = mutableListOf<Long>()

        for (size in dataSizes) {
            // Setup data
            repository.clearAllSettings()
            repeat(size) { i ->
                repository.setSetting("backup_perf_test_$i", "backup_content_$i".repeat(10))
            }

            // Measure backup time
            val backupTime = measureTimeMillis {
                runBlocking {
                    backupManager.exportSettings()
                }
            }

            backupTimes.add(backupTime)
            println("Data size: $size, Backup time: ${backupTime}ms")

            // Assert reasonable performance
            assert(backupTime < 10000) { "Backup time too slow for $size items: ${backupTime}ms" }
        }
    }

    @Test
    fun stressTestConcurrentOperations() = runBlocking {
        val operationCount = 1000
        val concurrencyLevel = 50

        val startTime = System.currentTimeMillis()

        // Run concurrent mixed operations
        val jobs = (1..operationCount).chunked(concurrencyLevel).map { chunk ->
            kotlinx.coroutines.async {
                chunk.map { i ->
                    when (i % 4) {
                        0 -> repository.setSetting("stress_key_$i", "stress_value_$i")
                        1 -> repository.getSetting("stress_key_${i - 1}", "default")
                        2 -> repository.hasSetting("stress_key_${i - 2}")
                        else -> repository.searchSettings("stress")
                    }
                }
            }
        }

        jobs.forEach { it.await() }

        val totalTime = System.currentTimeMillis() - startTime
        println("Completed $operationCount mixed operations in ${totalTime}ms")

        // Assert reasonable performance under stress
        assert(totalTime < 30000) { "Stress test took too long: ${totalTime}ms" }
    }

    @Test
    fun measureCacheEffectiveness() = runBlocking {
        // Setup test data
        repeat(1000) { i ->
            repository.setSetting("cache_test_$i", "cache_value_$i")
        }

        val key = "cache_test_500"

        // First read (likely cache miss)
        val firstReadTime = measureTimeMillis {
            runBlocking {
                repository.getSetting(key, "default")
            }
        }

        // Subsequent reads (should be cache hits)
        val subsequentReadTimes = mutableListOf<Long>()
        repeat(10) {
            val readTime = measureTimeMillis {
                runBlocking {
                    repository.getSetting(key, "default")
                }
            }
            subsequentReadTimes.add(readTime)
        }

        val averageSubsequentTime = subsequentReadTimes.average()

        println("First read: ${firstReadTime}ms, Average subsequent: ${averageSubsequentTime}ms")

        // Cache should provide significant performance improvement
        assert(averageSubsequentTime <= firstReadTime) {
            "Cache not providing performance benefit: first=$firstReadTime, avg=$averageSubsequentTime"
        }
    }
}