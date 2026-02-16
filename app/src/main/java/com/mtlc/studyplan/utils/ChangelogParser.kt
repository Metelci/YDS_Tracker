@file:Suppress("TooManyFunctions")
package com.mtlc.studyplan.utils

import android.util.Log
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

/**
 * Parses CHANGELOG.md and extracts version notes
 */
object ChangelogParser {

    private const val TAG = "ChangelogParser"

    /**
     * Data class for parsed release notes
     */
    data class ReleaseNotes(
        val version: String,
        val date: String,
        val content: AnnotatedString
    )

    /**
     * Extracts the latest version entry from changelog
     *
     * Expected format:
     * ## [2.9.77] - 2026-02-16
     *
     * ### Section Title
     * - **Feature**: Description
     *   - Sub-item
     *
     * @param changelogText Full changelog markdown text
     * @return ReleaseNotes for latest version, or null if parsing fails
     */
    fun extractLatestRelease(changelogText: String): ReleaseNotes? {
        return try {
            val lines = changelogText.lines()

            // Find first version header: ## [2.9.77] - 2026-02-16
            val versionHeaderRegex = """^## \[([\d.]+)\] - (.+)$""".toRegex()
            var startIndex = -1
            var version = ""
            var date = ""

            for ((index, line) in lines.withIndex()) {
                val match = versionHeaderRegex.find(line)
                if (match != null) {
                    startIndex = index
                    version = match.groupValues[1]
                    date = match.groupValues[2]
                    break
                }
            }

            if (startIndex == -1) {
                Log.w(TAG, "No version header found in changelog")
                return null
            }

            // Find end of this version block (next ## [ or end of file)
            var endIndex = lines.size
            for (i in (startIndex + 1) until lines.size) {
                if (lines[i].startsWith("## [")) {
                    endIndex = i
                    break
                }
            }

            // Extract content lines (skip version header)
            val contentLines = lines.subList(startIndex + 1, endIndex)

            // Parse markdown to AnnotatedString
            val annotatedContent = parseMarkdownToAnnotatedString(contentLines)

            ReleaseNotes(version, date, annotatedContent)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse changelog", e)
            null
        }
    }

    /**
     * Converts markdown lines to AnnotatedString with basic formatting
     *
     * Supports:
     * - **bold**
     * - `code`
     * - Bullet lists (-)
     * - Headers (###)
     */
    private fun parseMarkdownToAnnotatedString(lines: List<String>): AnnotatedString {
        return buildAnnotatedString {
            for (line in lines) {
                when {
                    // Skip empty lines
                    line.isBlank() -> {
                        append("\n")
                    }

                    // Section header: ### Title
                    line.startsWith("### ") -> {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(line.substring(4))
                        }
                        append("\n")
                    }

                    // Bullet item: - Text or  - Text (with leading spaces for sub-items)
                    line.trimStart().startsWith("- ") -> {
                        val indent = line.takeWhile { it == ' ' }.length
                        val bulletText = line.trimStart().substring(2) // Remove "- "

                        // Add indentation
                        append(" ".repeat(indent))
                        append("• ")

                        // Parse inline formatting in bullet text
                        parseInlineFormatting(bulletText)
                        append("\n")
                    }

                    // Regular text
                    else -> {
                        parseInlineFormatting(line)
                        append("\n")
                    }
                }
            }
        }
    }

    /**
     * Parses inline markdown formatting: **bold**, `code`
     */
    private fun AnnotatedString.Builder.parseInlineFormatting(text: String) {
        var remaining = text
        var currentIndex = 0

        // Pattern: **bold** or `code`
        val boldRegex = """\*\*(.+?)\*\*""".toRegex()
        val codeRegex = """`(.+?)`""".toRegex()

        while (currentIndex < remaining.length) {
            // Find next formatting marker
            val boldMatch = boldRegex.find(remaining, currentIndex)
            val codeMatch = codeRegex.find(remaining, currentIndex)

            // Determine which comes first
            val nextMatch = when {
                boldMatch != null && codeMatch != null -> {
                    if (boldMatch.range.first < codeMatch.range.first) boldMatch else codeMatch
                }
                boldMatch != null -> boldMatch
                codeMatch != null -> codeMatch
                else -> null
            }

            if (nextMatch == null) {
                // No more formatting, append rest
                append(remaining.substring(currentIndex))
                break
            }

            // Append text before match
            append(remaining.substring(currentIndex, nextMatch.range.first))

            // Append formatted text
            when {
                nextMatch.value.startsWith("**") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(nextMatch.groupValues[1])
                    }
                }
                nextMatch.value.startsWith("`") -> {
                    withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) {
                        append(nextMatch.groupValues[1])
                    }
                }
            }

            currentIndex = nextMatch.range.last + 1
        }
    }

    /**
     * Creates fallback release notes for when changelog can't be loaded
     * Uses hardcoded v2.9.75 content
     */
    fun getFallbackNotes(versionName: String, versionCode: Long): ReleaseNotes {
        val content = buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append("Privacy & Security Enhancements")
            }
            append("\n\n")
            append("• Crash Log Consent Dialog\n")
            append("  Users must explicitly confirm before sharing crash logs\n\n")
            append("• Automatic Data Sanitization\n")
            append("  - Internal paths anonymized\n")
            append("  - IP addresses redacted\n")
            append("  - Package names genericized\n")
            append("  - User IDs removed\n\n")
            append("• Security Audit Resolved\n")
            append("  Enhanced data protection and privacy controls\n")
            append("  Security score improved: 92 → 95")
        }

        return ReleaseNotes(
            version = versionName,
            date = "Unknown",
            content = content
        )
    }
}
