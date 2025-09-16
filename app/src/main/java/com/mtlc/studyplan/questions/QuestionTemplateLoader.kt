package com.mtlc.studyplan.questions

import android.content.Context
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

object QuestionTemplateLoader {
    private val json by lazy { Json { ignoreUnknownKeys = true } }

    fun load(context: Context): List<QuestionTemplate> {
        return runCatching {
            context.assets.open("question_templates.json").bufferedReader().use { reader ->
                val text = reader.readText()
                json.decodeFromString<List<QuestionTemplate>>(text)
            }
        }.getOrElse { emptyList() }
    }
}

