package com.mtlc.studyplan.feature.reader

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Simple in-memory user glossary repository.
 * - No demo/placeholder definitions.
 * - Users can add vocabulary; lookups return an entry if present,
 *   otherwise a neutral "not found" message.
 */
class UserGlossaryRepo : GlossaryRepo {
    private val mutex = Mutex()
    private val entries = mutableMapOf<String, String>()

    override suspend fun lookup(word: String): String {
        val key = word.trim().lowercase()
        return mutex.withLock { entries[key] } ?: "No definition found"
    }

    override suspend fun addToVocab(word: String) {
        val key = word.trim().lowercase()
        mutex.withLock {
            // Store the word with an empty definition for now
            entries.putIfAbsent(key, "")
        }
    }
}

