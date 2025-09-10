package com.mtlc.studyplan.feature.reader

import androidx.compose.runtime.Immutable

@Immutable
data class PassageUi(
    val id: String,
    val title: String,
    val body: String
)

@Immutable
data class ReaderPrefs(
    val fontScaleSp: Float = 18f, // roughly bodyLarge
    val lineHeightMult: Float = 1.4f,
    val theme: ReaderTheme = ReaderTheme.System,
)

enum class ReaderTheme { System, Light, Dark, Sepia }

interface GlossaryRepo {
    suspend fun lookup(word: String): String
    suspend fun addToVocab(word: String)
}

class FakeGlossaryRepo : GlossaryRepo {
    private val added = mutableSetOf<String>()
    override suspend fun lookup(word: String): String =
        "${word}: a placeholder definition for demo"

    override suspend fun addToVocab(word: String) {
        added += word.lowercase()
    }
}

