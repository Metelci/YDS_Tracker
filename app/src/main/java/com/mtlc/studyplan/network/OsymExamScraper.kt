package com.mtlc.studyplan.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ÖSYM Exam Scraper - Fetches real exam data from official ÖSYM website
 *
 * This service scrapes exam information including:
 * - Exam dates for YDS, YÖKDİL, and other exams
 * - Registration periods
 * - Result announcement dates
 * - Application URLs
 */
@Singleton
class OsymExamScraper @Inject constructor() {

    companion object {
        private const val OSYM_CALENDAR_URL = "https://www.osym.gov.tr/TR,8832/sinav-takvimi.html"
        private const val OSYM_YDS_URL = "https://www.osym.gov.tr/TR,9133/yabanci-dil-sinavi-yds.html"
        private const val OSYM_YOKDIL_URL = "https://www.osym.gov.tr/TR,9134/yokdil.html"

        private const val CONNECTION_TIMEOUT = 15000 // 15 seconds
        private const val READ_TIMEOUT = 15000 // 15 seconds

        // Exam type identifiers
        const val EXAM_TYPE_YDS = "YDS"
        const val EXAM_TYPE_YOKDIL = "YÖKDİL"
        const val EXAM_TYPE_E_YDS = "e-YDS"
    }

    data class ScrapedExamData(
        val examType: String,
        val examName: String,
        val examDate: LocalDate,
        val registrationStart: LocalDate?,
        val registrationEnd: LocalDate?,
        val lateRegistrationEnd: LocalDate?,
        val resultDate: LocalDate?,
        val applicationUrl: String,
        val scrapedAt: Long = System.currentTimeMillis(),
    )

    /**
     * Fetch all exam data from ÖSYM website
     * Returns list of scraped exam sessions
     */
    suspend fun fetchAllExams(): Result<List<ScrapedExamData>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val allExams = mutableListOf<ScrapedExamData>()

            // Fetch YDS exams
            val ydsExams = fetchYdsExams().getOrNull()
            if (ydsExams != null) {
                allExams.addAll(ydsExams)
            }

            // Fetch YÖKDİL exams
            val yokdilExams = fetchYokdilExams().getOrNull()
            if (yokdilExams != null) {
                allExams.addAll(yokdilExams)
            }

            // Fetch e-YDS exams
            val eydsExams = fetchEydsExams().getOrNull()
            if (eydsExams != null) {
                allExams.addAll(eydsExams)
            }

            if (allExams.isEmpty()) {
                Result.failure(Exception("No exam data found"))
            } else {
                Result.success(allExams)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch YDS exam data
     */
    private suspend fun fetchYdsExams(): Result<List<ScrapedExamData>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val htmlContent = fetchUrl(OSYM_YDS_URL)
            val exams = parseYdsHtml(htmlContent)
            Result.success(exams)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch YÖKDİL exam data
     */
    private suspend fun fetchYokdilExams(): Result<List<ScrapedExamData>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val htmlContent = fetchUrl(OSYM_YOKDIL_URL)
            val exams = parseYokdilHtml(htmlContent)
            Result.success(exams)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch e-YDS exam data
     */
    private suspend fun fetchEydsExams(): Result<List<ScrapedExamData>> = withContext(Dispatchers.IO) {
        return@withContext try {
            // e-YDS info might be on the same page as YDS or separate
            val htmlContent = fetchUrl(OSYM_YDS_URL)
            val exams = parseEydsHtml(htmlContent)
            Result.success(exams)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch URL content with proper error handling
     */
    private fun fetchUrl(urlString: String): String {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection

        return try {
            connection.requestMethod = "GET"
            connection.connectTimeout = CONNECTION_TIMEOUT
            connection.readTimeout = READ_TIMEOUT
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android) StudyPlan App")
            connection.setRequestProperty("Accept", "text/html")
            connection.setRequestProperty("Accept-Language", "tr-TR,tr;q=0.9,en-US;q=0.8,en;q=0.7")

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream, "UTF-8"))
                val response = reader.readText()
                reader.close()
                response
            } else {
                throw Exception("HTTP Error: ${connection.responseCode}")
            }
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Parse YDS exam data from HTML
     * This is a simplified parser - you'll need to adapt based on actual HTML structure
     */
    private fun parseYdsHtml(html: String): List<ScrapedExamData> {
        val exams = mutableListOf<ScrapedExamData>()

        // Look for YDS-specific patterns and extract relevant text blocks
        val ydsPattern = Regex("""(YDS\s+\d{4}/\d+[^<>"']{0,300})""", RegexOption.IGNORE_CASE)
        val ydsMatches = ydsPattern.findAll(html)

        for (match in ydsMatches) {
            val textBlock = match.value
            val examName = extractExamName(textBlock) ?: continue

            // Extract dates from the context around this exam
            val datePattern = Regex("""\b(\d{2})\.(\d{2})\.(\d{4})\b""")
            val dateMatches = datePattern.findAll(textBlock + " " +
                getNextContext(html, match.range.first, 500)) // Look for dates in nearby context

            val dates = dateMatches.map { matchResult ->
                val (day, month, year) = matchResult.destructured
                try {
                    LocalDate.of(year.toInt(), month.toInt(), day.toInt())
                } catch (e: Exception) {
                    null
                }
            }.filterNotNull().sorted().take(10) // Take up to 10 closest dates and sort them

            // Try to identify the most likely dates for this exam based on context
            val (examDate, regStart, regEnd, lateRegEnd) = identifyExamDates(textBlock, dates.toList())

            if (examDate != null && examDate.isAfter(LocalDate.now())) {
                exams.add(
                    ScrapedExamData(
                        examType = EXAM_TYPE_YDS,
                        examName = examName,
                        examDate = examDate,
                        registrationStart = regStart,
                        registrationEnd = regEnd,
                        lateRegistrationEnd = lateRegEnd,
                        resultDate = examDate.plusWeeks(3), // Estimate: 3 weeks after exam
                        applicationUrl = OSYM_YDS_URL,
                    )
                )
            }
        }

        return exams
    }
    
    /**
     * Extract exam name from a text block
     */
    private fun extractExamName(textBlock: String): String? {
        val ydsPattern = Regex("""YDS\s+(\d{4}/\d+)""", RegexOption.IGNORE_CASE)
        val match = ydsPattern.find(textBlock) ?: return null
        return "YDS ${match.groupValues[1]}"
    }
    
    /**
     * Get context text after a specific position
     */
    private fun getNextContext(html: String, startPos: Int, length: Int): String {
        val endPos = minOf(startPos + length, html.length)
        return html.substring(startPos, endPos)
    }
    
    /**
     * Identify the most likely dates for an exam based on context
     */
    private fun identifyExamDates(context: String, dates: List<LocalDate>):
            Triple<LocalDate?, LocalDate?, LocalDate?, LocalDate?> {
        if (dates.isEmpty()) return Triple(null, null, null, null)
        
        // Look for registration and exam date patterns in the context
        val registrationKeywords = listOf("başvuru", "kayıt", "registration", "apply", "application")
        val examKeywords = listOf("sınav", "exam", "yazılı", "test")
        
        // If we have multiple dates, try to identify which is most likely the exam date
        // Usually the exam date is the furthest in the future among relevant dates
        val possibleExamDates = dates.filter { it.isAfter(LocalDate.now().minusDays(30)) }
        
        if (possibleExamDates.size >= 4) {
            // If we have 4 or more dates, assume [regStart, regEnd, lateRegEnd, examDate] pattern
            return Triple(
                possibleExamDates[0], // registration start
                possibleExamDates[1], // registration end
                possibleExamDates[2], // late registration end
                possibleExamDates[3]  // exam date
            )
        } else if (possibleExamDates.size == 3) {
            // If we have 3 dates, assume [regStart, regEnd, examDate] pattern
            return Triple(
                possibleExamDates[0], // registration start
                possibleExamDates[1], // registration end
                null,                 // no late registration
                possibleExamDates[2] // exam date
            )
        } else if (possibleExamDates.size == 2) {
            // If we have 2 dates, assume [regStart, examDate] or [regEnd, examDate]
            // For now, assume first is registration start, second is exam
            return Triple(
                possibleExamDates[0], // registration start
                null,                 // no registration end specified
                null,                 // no late registration
                possibleExamDates[1] // exam date
            )
        } else if (possibleExamDates.size == 1) {
            // If we have only 1 date, it's likely the exam date
            return Triple(
                null,                 // no registration start
                null,                 // no registration end
                null,                 // no late registration
                possibleExamDates[0] // exam date
            )
        }
        
        // If no dates are relevant, return all nulls
        return Triple(null, null, null, null)
    }

    /**
     * Parse YÖKDİL exam data from HTML
     */
    private fun parseYokdilHtml(html: String): List<ScrapedExamData> {
        val exams = mutableListOf<ScrapedExamData>()

        // Look for YÖKDİL-specific patterns and extract relevant text blocks
        val yokdilPattern = Regex("""(YÖKDİL\s+\d{4}/\d+[^<>"']{0,300})""", RegexOption.IGNORE_CASE)
        val yokdilMatches = yokdilPattern.findAll(html)

        for (match in yokdilMatches) {
            val textBlock = match.value
            val examName = extractYokdilExamName(textBlock) ?: continue

            // Extract dates from the context around this exam
            val datePattern = Regex("""\b(\d{2})\.(\d{2})\.(\d{4})\b""")
            val dateMatches = datePattern.findAll(textBlock + " " +
                getNextContext(html, match.range.first, 500)) // Look for dates in nearby context

            val dates = dateMatches.map { matchResult ->
                val (day, month, year) = matchResult.destructured
                try {
                    LocalDate.of(year.toInt(), month.toInt(), day.toInt())
                } catch (e: Exception) {
                    null
                }
            }.filterNotNull().sorted().take(10) // Take up to 10 closest dates and sort them

            // Try to identify the most likely dates for this exam based on context
            val (examDate, regStart, regEnd, lateRegEnd) = identifyExamDates(textBlock, dates.toList())

            if (examDate != null && examDate.isAfter(LocalDate.now())) {
                exams.add(
                    ScrapedExamData(
                        examType = EXAM_TYPE_YOKDIL,
                        examName = examName,
                        examDate = examDate,
                        registrationStart = regStart,
                        registrationEnd = regEnd,
                        lateRegistrationEnd = lateRegEnd,
                        resultDate = examDate.plusWeeks(3), // Estimate: 3 weeks after exam
                        applicationUrl = OSYM_YOKDIL_URL,
                    )
                )
            }
        }

        return exams
    }
    
    /**
     * Extract YÖKDİL exam name from a text block
     */
    private fun extractYokdilExamName(textBlock: String): String? {
        val yokdilPattern = Regex("""YÖKDİL\s+(\d{4}/\d+)""", RegexOption.IGNORE_CASE)
        val match = yokdilPattern.find(textBlock) ?: return null
        return "YÖKDİL ${match.groupValues[1]}"
    }

    /**
     * Parse e-YDS exam data from HTML
     */
    private fun parseEydsHtml(html: String): List<ScrapedExamData> {
        val exams = mutableListOf<ScrapedExamData>()

        // Look for e-YDS-specific patterns and extract relevant text blocks
        val eydsPattern = Regex("""(e-YDS\s+\d{4}/\d+[^<>"']{0,300})""", RegexOption.IGNORE_CASE)
        val eydsMatches = eydsPattern.findAll(html)

        for (match in eydsMatches) {
            val textBlock = match.value
            val examName = extractEydsExamName(textBlock) ?: continue

            // Extract dates from the context around this exam
            val datePattern = Regex("""\b(\d{2})\.(\d{2})\.(\d{4})\b""")
            val dateMatches = datePattern.findAll(textBlock + " " +
                getNextContext(html, match.range.first, 500)) // Look for dates in nearby context

            val dates = dateMatches.map { matchResult ->
                val (day, month, year) = matchResult.destructured
                try {
                    LocalDate.of(year.toInt(), month.toInt(), day.toInt())
                } catch (e: Exception) {
                    null
                }
            }.filterNotNull().sorted().take(10) // Take up to 10 closest dates and sort them

            // Try to identify the most likely dates for this exam based on context
            val (examDate, regStart, regEnd, lateRegEnd) = identifyExamDates(textBlock, dates.toList())

            if (examDate != null && examDate.isAfter(LocalDate.now())) {
                exams.add(
                    ScrapedExamData(
                        examType = EXAM_TYPE_E_YDS,
                        examName = examName,
                        examDate = examDate,
                        registrationStart = regStart,
                        registrationEnd = regEnd,
                        lateRegistrationEnd = lateRegEnd,
                        resultDate = examDate.plusWeeks(2), // e-YDS results are faster
                        applicationUrl = OSYM_YDS_URL,
                    )
                )
            }
        }

        return exams
    }
    
    /**
     * Extract e-YDS exam name from a text block
     */
    private fun extractEydsExamName(textBlock: String): String? {
        val eydsPattern = Regex("""e-YDS\s+(\d{4}/\d+)""", RegexOption.IGNORE_CASE)
        val match = eydsPattern.find(textBlock) ?: return null
        return "e-YDS ${match.groupValues[1]}"
    }

    /**
     * Check if scraper is accessible (network connectivity test)
     */
    suspend fun testConnection(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val connection = URL(OSYM_CALENDAR_URL).openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            connection.connectTimeout = 5000
            connection.responseCode == HttpURLConnection.HTTP_OK
        } catch (e: Exception) {
            false
        }
    }
}
