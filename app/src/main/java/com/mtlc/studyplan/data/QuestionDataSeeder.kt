package com.mtlc.studyplan.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mtlc.studyplan.database.entities.*
import com.mtlc.studyplan.repository.QuestionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuestionDataSeeder @Inject constructor(
    private val context: Context,
    private val questionRepository: QuestionRepository,
    private val gson: Gson = Gson()
) {

    suspend fun seedSampleQuestions() = withContext(Dispatchers.IO) {
        try {
            // Check if questions are already seeded
            val existingCount = questionRepository.getTotalQuestionCount()
            if (existingCount > 0) {
                return@withContext // Already seeded
            }

            // Load sample questions from assets
            val questionsJson = loadQuestionsFromAssets()
            if (questionsJson.isNotEmpty()) {
                val questions = parseQuestionsJson(questionsJson)
                questionRepository.insertQuestions(questions)
            }
        } catch (e: Exception) {
            // Log error but don't crash the app
            e.printStackTrace()
        }
    }

    private fun loadQuestionsFromAssets(): String {
        return try {
            context.assets.open("questions/sample_questions.json")
                .bufferedReader()
                .use { it.readText() }
        } catch (e: Exception) {
            ""
        }
    }

    private fun parseQuestionsJson(json: String): List<QuestionEntity> {
        return try {
            val questionListType = object : TypeToken<List<QuestionJson>>() {}.type
            val questionJsons: List<QuestionJson> = gson.fromJson(json, questionListType)

            questionJsons.map { jsonQuestion ->
                QuestionEntity(
                    id = jsonQuestion.id,
                    examType = ExamType.valueOf(jsonQuestion.examType),
                    questionType = QuestionType.valueOf(jsonQuestion.questionType),
                    difficulty = Difficulty.valueOf(jsonQuestion.difficulty),
                    questionText = jsonQuestion.questionText,
                    options = jsonQuestion.options,
                    correctAnswer = jsonQuestion.correctAnswer,
                    explanation = jsonQuestion.explanation,
                    tags = jsonQuestion.tags,
                    isDownloaded = true, // Sample questions are pre-downloaded
                    lastAccessed = System.currentTimeMillis()
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Data class for JSON parsing
    private data class QuestionJson(
        val id: String,
        val examType: String,
        val questionType: String,
        val difficulty: String,
        val questionText: String,
        val options: List<String>,
        val correctAnswer: Int,
        val explanation: String,
        val tags: List<String>
    )
}