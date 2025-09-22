package com.mtlc.studyplan.network

import com.mtlc.studyplan.data.YdsExamService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Lightweight client that attempts to retrieve YDS exam dates from the official ÖSYM website.
 *
 * Notes:
 * - The ÖSYM site structure can change; parsing is best‑effort and resilient.
 * - If parsing fails, callers should fall back to built‑in schedules in YdsExamService.
 */
object OsymExamCalendarClient {

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .callTimeout(10, TimeUnit.SECONDS)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale("tr", "TR"))

    /**
     * Fetch upcoming YDS sessions with application and exam dates from ÖSYM.
     * Returns an empty list if fetching/parsing fails.
     */
    suspend fun fetchYdsExams(): List<YdsExamService.YdsExamSession> = withContext(Dispatchers.IO) {
        // Try a small set of likely sources. Site structure may change.
        val candidates = listOf(
            // Root (often links to the yearly exam calendar)
            "https://www.osym.gov.tr/",
            // Generic calendar pages for some years (kept as fallbacks)
            "https://www.osym.gov.tr/TR,21436/sinav-takvimi.html",
            // AIS (landing; sometimes contains calendar references)
            "https://ais.osym.gov.tr/",
        )

        val pages = candidates.mapNotNull { url ->
            runCatching { httpGet(url) }.getOrNull()
        }

        for (html in pages) {
            val parsed = parseYdsFromHtml(html)
            if (parsed.isNotEmpty()) return@withContext parsed
        }

        emptyList()
    }

    private fun httpGet(url: String): String {
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", "StudyPlan/1.0 (Android)")
            .get()
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IllegalStateException("HTTP ${'$'}{resp.code}")
            return resp.body?.string().orEmpty()
        }
    }

    // Best‑effort parser. Looks for YDS sections and extracts dd.MM.yyyy dates near them.
    private fun parseYdsFromHtml(html: String): List<YdsExamService.YdsExamSession> {
        val results = mutableListOf<YdsExamService.YdsExamSession>()

        // Normalize for matching while keeping original for slicing
        val normalized = html.replace("\u00A0", " ") // nbsp -> space
        val lower = normalized.lowercase(Locale("tr", "TR"))

        // Find likely YDS anchors
        val anchors = Regex("yds\\s*/?\\s*(20\\d{2})?", RegexOption.IGNORE_CASE)
            .findAll(lower)
            .map { it.range.first }
            .toList()

        if (anchors.isEmpty()) return emptyList()

        val dateRegex = Regex("""(\d{1,2}\.\d{1,2}\.\d{4})""")
        val rangeRegex = Regex("""(\d{1,2}\.\d{1,2}\.\d{4})\s*[-–]\s*(\d{1,2}\.\d{1,2}\.\d{4})""")

        for (idx in anchors) {
            val start = (idx - 800).coerceAtLeast(0)
            val end = (idx + 1600).coerceAtMost(normalized.length)
            val window = normalized.substring(start, end)
            val windowLower = window.lowercase(Locale("tr", "TR"))

            // Heuristic: must contain both application and exam keywords near dates
            val hasApp = windowLower.contains("başvuru") || windowLower.contains("basvuru")
            val hasExam = windowLower.contains("sınav") || windowLower.contains("sinav")
            if (!hasApp && !hasExam) continue

            val ranges = rangeRegex.findAll(window).map { it.groupValues.drop(1) }.toList()
            val singles = dateRegex.findAll(window).map { it.groupValues[1] }.toList()

            // Deduplicate singles that are part of ranges
            val singlesOnly = singles.filter { s -> ranges.none { it.contains(s) } }

            // Try to build a session: [applicationStart, applicationEnd], lateRegistration, examDate, resultDate
            val app = ranges.firstOrNull()?.let { it[0] to it[1] }
            val late = singlesOnly.getOrNull(0)
            val exam = singlesOnly.getOrNull(1)
            val result = singlesOnly.getOrNull(2)

            if (app != null && exam != null) {
                runCatching {
                    val session = YdsExamService.YdsExamSession(
                        name = deriveSessionName(windowLower),
                        examDate = LocalDate.parse(exam, dateFormatter),
                        registrationStart = LocalDate.parse(app.first, dateFormatter),
                        registrationEnd = LocalDate.parse(app.second, dateFormatter),
                        lateRegistrationEnd = late?.let { LocalDate.parse(it, dateFormatter) }
                            ?: LocalDate.parse(app.second, dateFormatter),
                        resultDate = result?.let { LocalDate.parse(it, dateFormatter) }
                            ?: LocalDate.parse(exam, dateFormatter).plusDays(20),
                        applicationUrl = "https://ais.osym.gov.tr/"
                    )
                    results.add(session)
                }
            }
        }

        // Sort and unique by exam date
        return results
            .distinctBy { it.examDate }
            .sortedBy { it.examDate }
    }

    private fun deriveSessionName(blockLower: String): String {
        // Attempt to infer /1 or /2 from surrounding text; otherwise generic label
        val currentYear = LocalDate.now().year
        return when {
            Regex("yds\\s*/?\\s*1").containsMatchIn(blockLower) -> "YDS $currentYear/1"
            Regex("yds\\s*/?\\s*2").containsMatchIn(blockLower) -> "YDS $currentYear/2"
            else -> "YDS"
        }
    }
}

