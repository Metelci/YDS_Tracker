package com.mtlc.studyplan.smartcontent

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mtlc.studyplan.data.ProgressRepository
import com.mtlc.studyplan.data.dataStore
import com.mtlc.studyplan.questions.QuestionService
import com.mtlc.studyplan.questions.VocabularyManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class SmartContentPerformanceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun generateDailyPack_isFastEnough() = runBlocking {
        val repo = ProgressRepository(context.dataStore)
        val generator = QuestionService.buildGenerator(context, repo, context.dataStore)
        val vocab = VocabularyManager(context, repo)
        val smart = SmartContentManager(context, generator, vocab, repo, com.mtlc.studyplan.ai.SmartScheduler())

        val start = System.currentTimeMillis()
        val pack = smart.generateDailyContentPack(25)
        val elapsed = System.currentTimeMillis() - start

        // Allow generous upper bound for CI/emulator environments
        assertTrue("Daily pack generation too slow: ${elapsed}ms", elapsed < 1500)
        assertTrue(pack.vocabulary.size <= 15)
    }

    @Test
    fun recommendationCache_returnsCachedQuickly() = runBlocking {
        val repo = ProgressRepository(context.dataStore)
        val generator = QuestionService.buildGenerator(context, repo, context.dataStore)
        val vocab = VocabularyManager(context, repo)
        val smart = SmartContentManager(context, generator, vocab, repo, com.mtlc.studyplan.ai.SmartScheduler())

        smart.getContentRecommendations() // warm cache
        val t0 = System.nanoTime()
        smart.getContentRecommendations()
        val t1 = System.nanoTime() - t0
        assertTrue("Cached recommendations too slow", t1 < 10_000_000) // <10ms
    }
}

