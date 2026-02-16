package com.mtlc.studyplan.utils

import android.content.Context
import android.util.Log

/**
 * Loads CHANGELOG.md content from app assets
 * Follows pattern from ExamPackLoader.kt
 */
object ChangelogLoader {

    private const val TAG = "ChangelogLoader"

    /**
     * Loads changelog from assets/changelog.md
     * @param context Android context for accessing assets
     * @return Full changelog text or null if file not found
     */
    fun loadFromAssets(context: Context): String? {
        return try {
            context.assets.open("changelog.md").use { stream ->
                stream.bufferedReader().readText()
            }
        } catch (e: Throwable) {
            // Log error but don't crash - caller will handle fallback
            Log.e(TAG, "Failed to load changelog from assets", e)
            null
        }
    }
}
