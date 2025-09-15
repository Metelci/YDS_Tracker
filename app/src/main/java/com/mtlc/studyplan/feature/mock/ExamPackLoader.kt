package com.mtlc.studyplan.feature.mock

import android.content.Context
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ExamPack(
    val title: String = "",
    @SerialName("duration_seconds") val durationSeconds: Int = 180 * 60,
    val questions: List<ExamPackQuestion> = emptyList()
)

@Serializable
data class ExamPackQuestion(
    val id: Int,
    val section: String,
    val text: String,
    val options: List<String>,
    @SerialName("correct_index") val correctIndex: Int
)

object ExamPackLoader {
    private val json = Json { ignoreUnknownKeys = true }

    fun listAssetPacks(context: Context): List<String> {
        return try {
            context.assets.list("exams")?.filter { it.endsWith(".json", true) }?.sorted().orEmpty()
        } catch (_: Throwable) { emptyList() }
    }

    fun loadFromAssets(context: Context, fileName: String): ExamPack? {
        return try {
            context.assets.open("exams/$fileName").use { stream ->
                val text = stream.bufferedReader().readText()
                json.decodeFromString(ExamPack.serializer(), text)
            }
        } catch (_: Throwable) { null }
    }
}

