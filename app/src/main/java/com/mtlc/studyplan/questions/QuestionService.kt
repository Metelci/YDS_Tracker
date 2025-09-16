package com.mtlc.studyplan.questions

import android.content.Context
import com.mtlc.studyplan.analytics.AnalyticsEngine
import com.mtlc.studyplan.data.ProgressRepository
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.runBlocking

object QuestionService {
    fun buildGenerator(
        context: Context,
        progressRepository: ProgressRepository,
        dataStore: DataStore<Preferences>
    ): QuestionGenerator {
        val analytics = AnalyticsEngine()
        val vocab = VocabularyManager(context, progressRepository)
        val provider = DefaultQuestionDataProvider(progressRepository, vocab)
        val perf = RoomQuestionPerformanceTracker(context)
        return QuestionGenerator(
            context = context,
            analyticsEngine = analytics,
            vocabularyManager = vocab,
            dataProvider = provider,
            performanceTracker = perf
        )
    }

    fun exampleGenerate(context: Context, progressRepository: ProgressRepository, dataStore: DataStore<Preferences>): List<GeneratedQuestion> = runBlocking {
        val gen = buildGenerator(context, progressRepository, dataStore)
        gen.generatePersonalizedQuestions(count = 10, currentWeek = 1)
    }
}
