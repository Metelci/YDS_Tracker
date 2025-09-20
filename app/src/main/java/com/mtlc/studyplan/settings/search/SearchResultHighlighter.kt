package com.mtlc.studyplan.settings.search

import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import androidx.core.content.ContextCompat
import com.mtlc.studyplan.R
import com.mtlc.studyplan.settings.search.SettingsSearchEngine.SearchResult
import com.mtlc.studyplan.settings.search.SettingsSearchEngine.HighlightRange

/**
 * Advanced search result highlighting with multiple highlight styles
 */
class SearchResultHighlighter(private val context: android.content.Context) {

    private val highlightBackgroundColor = ContextCompat.getColor(context, R.color.search_highlight_background)
    private val highlightTextColor = ContextCompat.getColor(context, R.color.search_highlight_text)
    private val secondaryHighlightColor = ContextCompat.getColor(context, R.color.search_highlight_secondary)

    /**
     * Highlight search matches in text with multiple styles
     */
    fun highlightMatches(
        text: String,
        query: String,
        highlightType: HighlightType = HighlightType.PRIMARY
    ): SpannableString {
        val spannable = SpannableString(text)

        if (query.isEmpty()) return spannable

        val matches = findAllMatches(text, query)

        matches.forEach { range ->
            applyHighlight(spannable, range, highlightType)
        }

        return spannable
    }

    /**
     * Highlight specific ranges with custom styling
     */
    fun highlightRanges(
        text: String,
        ranges: List<IntRange>,
        highlightType: HighlightType = HighlightType.PRIMARY
    ): SpannableString {
        val spannable = SpannableString(text)

        ranges.forEach { range ->
            if (range.first < text.length && range.last < text.length) {
                applyHighlight(spannable, range, highlightType)
            }
        }

        return spannable
    }

    /**
     * Create highlighted search snippet with context
     */
    fun createSearchSnippet(
        fullText: String,
        query: String,
        maxLength: Int = 120,
        contextChars: Int = 20
    ): SpannableString {
        val matches = findAllMatches(fullText, query)

        if (matches.isEmpty()) {
            return SpannableString(truncateText(fullText, maxLength))
        }

        val bestMatch = matches.first()
        val snippetStart = (bestMatch.first - contextChars).coerceAtLeast(0)
        val snippetEnd = (bestMatch.last + 1 + contextChars).coerceAtMost(fullText.length)

        val snippet = fullText.substring(snippetStart, snippetEnd)
        val adjustedRanges = matches.mapNotNull { range ->
            val adjustedStart = range.first - snippetStart
            val adjustedEnd = range.last - snippetStart

            if (adjustedStart >= 0 && adjustedEnd < snippet.length) {
                IntRange(adjustedStart, adjustedEnd)
            } else null
        }

        val highlightedSnippet = highlightRanges(snippet, adjustedRanges)

        // Add ellipsis if text was truncated
        if (snippetStart > 0 || snippetEnd < fullText.length) {
            val prefix = if (snippetStart > 0) "…" else ""
            val suffix = if (snippetEnd < fullText.length) "…" else ""
            return SpannableString("$prefix$highlightedSnippet$suffix")
        }

        return highlightedSnippet
    }

    /**
     * Highlight fuzzy matches with different intensity
     */
    fun highlightFuzzyMatches(
        text: String,
        query: String,
        similarityThreshold: Double = 0.7
    ): SpannableString {
        val words = query.split("\\s+".toRegex())
        val spannable = SpannableString(text)

        words.forEach { word ->
            val fuzzyMatches = findFuzzyMatches(text, word, similarityThreshold)
            fuzzyMatches.forEach { range ->
                applyHighlight(spannable, range, HighlightType.FUZZY)
            }
        }

        return spannable
    }

    /**
     * Find all exact and partial matches
     */
    private fun findAllMatches(text: String, query: String): List<IntRange> {
        val matches = mutableListOf<IntRange>()
        val lowerText = text.lowercase()
        val lowerQuery = query.lowercase()

        // Find exact matches
        var startIndex = 0
        while (true) {
            val index = lowerText.indexOf(lowerQuery, startIndex)
            if (index == -1) break

            matches.add(IntRange(index, index + query.length - 1))
            startIndex = index + 1
        }

        // Find word boundary matches
        val queryWords = lowerQuery.split("\\s+".toRegex())
        queryWords.forEach { word ->
            if (word.length >= 2) {
                var wordStartIndex = 0
                while (true) {
                    val wordIndex = lowerText.indexOf(word, wordStartIndex)
                    if (wordIndex == -1) break

                    // Check if it's a word boundary match
                    val isWordStart = wordIndex == 0 || !lowerText[wordIndex - 1].isLetterOrDigit()
                    val isWordEnd = wordIndex + word.length == lowerText.length ||
                                  !lowerText[wordIndex + word.length].isLetterOrDigit()

                    if (isWordStart || isWordEnd) {
                        val range = IntRange(wordIndex, wordIndex + word.length - 1)
                        if (!matches.any { it.overlaps(range) }) {
                            matches.add(range)
                        }
                    }

                    wordStartIndex = wordIndex + 1
                }
            }
        }

        return matches.sortedBy { it.first }
    }

    /**
     * Find fuzzy matches using similarity calculation
     */
    private fun findFuzzyMatches(
        text: String,
        word: String,
        threshold: Double
    ): List<IntRange> {
        val matches = mutableListOf<IntRange>()
        val words = text.split("\\s+".toRegex())
        var currentIndex = 0

        words.forEach { textWord ->
            val wordStart = text.indexOf(textWord, currentIndex)
            if (wordStart != -1) {
                val similarity = calculateSimilarity(textWord.lowercase(), word.lowercase())
                if (similarity >= threshold) {
                    matches.add(IntRange(wordStart, wordStart + textWord.length - 1))
                }
                currentIndex = wordStart + textWord.length
            }
        }

        return matches
    }

    /**
     * Calculate text similarity using Jaro-Winkler distance
     */
    private fun calculateSimilarity(s1: String, s2: String): Double {
        if (s1 == s2) return 1.0
        if (s1.isEmpty() || s2.isEmpty()) return 0.0

        val matchWindow = maxOf(s1.length, s2.length) / 2 - 1
        val s1Matches = BooleanArray(s1.length)
        val s2Matches = BooleanArray(s2.length)
        var matches = 0
        var transpositions = 0

        // Find matches
        for (i in s1.indices) {
            val start = maxOf(0, i - matchWindow)
            val end = minOf(i + matchWindow + 1, s2.length)

            for (j in start until end) {
                if (s2Matches[j] || s1[i] != s2[j]) continue
                s1Matches[i] = true
                s2Matches[j] = true
                matches++
                break
            }
        }

        if (matches == 0) return 0.0

        // Count transpositions
        var k = 0
        for (i in s1.indices) {
            if (!s1Matches[i]) continue
            while (!s2Matches[k]) k++
            if (s1[i] != s2[k]) transpositions++
            k++
        }

        val jaro = (matches.toDouble() / s1.length +
                   matches.toDouble() / s2.length +
                   (matches - transpositions / 2.0) / matches) / 3.0

        // Jaro-Winkler bonus for common prefix
        var prefix = 0
        for (i in 0 until minOf(s1.length, s2.length, 4)) {
            if (s1[i] == s2[i]) prefix++ else break
        }

        return jaro + 0.1 * prefix * (1 - jaro)
    }

    /**
     * Apply highlight styling to spannable text
     */
    private fun applyHighlight(
        spannable: SpannableString,
        range: IntRange,
        highlightType: HighlightType
    ) {
        val start = range.first.coerceAtLeast(0)
        val end = (range.last + 1).coerceAtMost(spannable.length)

        if (start >= end) return

        when (highlightType) {
            HighlightType.PRIMARY -> {
                spannable.setSpan(
                    BackgroundColorSpan(highlightBackgroundColor),
                    start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                spannable.setSpan(
                    ForegroundColorSpan(highlightTextColor),
                    start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                spannable.setSpan(
                    StyleSpan(Typeface.BOLD),
                    start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            HighlightType.SECONDARY -> {
                spannable.setSpan(
                    BackgroundColorSpan(secondaryHighlightColor),
                    start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                spannable.setSpan(
                    StyleSpan(Typeface.BOLD),
                    start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            HighlightType.FUZZY -> {
                spannable.setSpan(
                    StyleSpan(Typeface.ITALIC),
                    start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                spannable.setSpan(
                    ForegroundColorSpan(highlightTextColor),
                    start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
    }

    /**
     * Truncate text to specified length with proper word boundaries
     */
    private fun truncateText(text: String, maxLength: Int): String {
        if (text.length <= maxLength) return text

        val truncated = text.substring(0, maxLength)
        val lastSpace = truncated.lastIndexOf(' ')

        return if (lastSpace > maxLength * 0.8) {
            truncated.substring(0, lastSpace) + "…"
        } else {
            truncated + "…"
        }
    }

    /**
     * Check if two ranges overlap
     */
    private fun IntRange.overlaps(other: IntRange): Boolean {
        return this.first <= other.last && other.first <= this.last
    }

    /**
     * Highlight type enum for different styling
     */
    enum class HighlightType {
        PRIMARY,    // Main search matches
        SECONDARY,  // Secondary/related matches
        FUZZY       // Fuzzy/similarity matches
    }
}

/**
 * Extension functions for search result highlighting
 */
fun SearchResult.getHighlightedTitle(highlighter: SearchResultHighlighter): SpannableString {
    val intRanges = this.highlightRanges.map { it.start..it.end }
    return highlighter.highlightRanges(this.item.title, intRanges)
}

fun SearchResult.getHighlightedDescription(
    highlighter: SearchResultHighlighter,
    query: String
): SpannableString {
    return highlighter.createSearchSnippet(this.item.description, query)
}