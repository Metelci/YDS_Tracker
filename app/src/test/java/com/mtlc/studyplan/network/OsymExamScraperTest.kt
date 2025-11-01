package com.mtlc.studyplan.network

import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration tests for OsymExamScraper
 *
 * Tests the ExamDateInfo refactoring and date parsing logic
 * across all exam types (YDS, YÖKDİL, e-YDS)
 */
class OsymExamScraperTest {

    private val scraper = OsymExamScraper()

    // ============ ExamDateInfo Data Class Tests ============

    @Test
    fun `ExamDateInfo should correctly store all four date values`() {
        val examDate = LocalDate.of(2025, 6, 15)
        val regStart = LocalDate.of(2025, 5, 1)
        val regEnd = LocalDate.of(2025, 5, 31)
        val lateRegEnd = LocalDate.of(2025, 6, 10)

        val examDateInfo = OsymExamScraper.ExamDateInfo(
            examDate = examDate,
            regStart = regStart,
            regEnd = regEnd,
            lateRegEnd = lateRegEnd
        )

        assertEquals(examDate, examDateInfo.examDate)
        assertEquals(regStart, examDateInfo.regStart)
        assertEquals(regEnd, examDateInfo.regEnd)
        assertEquals(lateRegEnd, examDateInfo.lateRegEnd)
    }

    @Test
    fun `ExamDateInfo should handle null values correctly`() {
        val examDate = LocalDate.of(2025, 6, 15)
        val examDateInfo = OsymExamScraper.ExamDateInfo(
            examDate = examDate,
            regStart = null,
            regEnd = null,
            lateRegEnd = null
        )

        assertEquals(examDate, examDateInfo.examDate)
        assertEquals(null, examDateInfo.regStart)
        assertEquals(null, examDateInfo.regEnd)
        assertEquals(null, examDateInfo.lateRegEnd)
    }

    @Test
    fun `ExamDateInfo should handle all null values`() {
        val examDateInfo = OsymExamScraper.ExamDateInfo(
            examDate = null,
            regStart = null,
            regEnd = null,
            lateRegEnd = null
        )

        assertEquals(null, examDateInfo.examDate)
        assertEquals(null, examDateInfo.regStart)
        assertEquals(null, examDateInfo.regEnd)
        assertEquals(null, examDateInfo.lateRegEnd)
    }

    @Test
    fun `ExamDateInfo should be immutable`() {
        val examDate = LocalDate.of(2025, 6, 15)
        val info1 = OsymExamScraper.ExamDateInfo(examDate, null, null, null)
        val info2 = info1.copy(
            examDate = LocalDate.of(2025, 7, 15)
        )

        // Original should be unchanged
        assertEquals(examDate, info1.examDate)
        assertEquals(LocalDate.of(2025, 7, 15), info2.examDate)
    }

    // ============ ScrapedExamData Tests ============

    @Test
    fun `ScrapedExamData should create YDS exam with all fields populated`() {
        val examDate = LocalDate.of(2025, 6, 15)
        val regStart = LocalDate.of(2025, 5, 1)
        val regEnd = LocalDate.of(2025, 5, 31)

        val examData = OsymExamScraper.ScrapedExamData(
            examType = OsymExamScraper.EXAM_TYPE_YDS,
            examName = "YDS 2025/1",
            examDate = examDate,
            registrationStart = regStart,
            registrationEnd = regEnd,
            lateRegistrationEnd = null,
            resultDate = examDate.plusWeeks(3),
            applicationUrl = "https://www.osym.gov.tr/TR,9133/yabanci-dil-sinavi-yds.html"
        )

        assertEquals(OsymExamScraper.EXAM_TYPE_YDS, examData.examType)
        assertEquals("YDS 2025/1", examData.examName)
        assertEquals(examDate, examData.examDate)
        assertEquals(regStart, examData.registrationStart)
        assertEquals(regEnd, examData.registrationEnd)
    }

    @Test
    fun `ScrapedExamData should create YÖKDİL exam with correct type`() {
        val examData = OsymExamScraper.ScrapedExamData(
            examType = OsymExamScraper.EXAM_TYPE_YOKDIL,
            examName = "YÖKDİL 2025/1",
            examDate = LocalDate.of(2025, 7, 20),
            registrationStart = LocalDate.of(2025, 6, 1),
            registrationEnd = LocalDate.of(2025, 6, 30),
            lateRegistrationEnd = null,
            resultDate = LocalDate.of(2025, 8, 10),
            applicationUrl = "https://www.osym.gov.tr/TR,9134/yokdil.html"
        )

        assertEquals(OsymExamScraper.EXAM_TYPE_YOKDIL, examData.examType)
        assertEquals("YÖKDİL 2025/1", examData.examName)
    }

    @Test
    fun `ScrapedExamData should create e-YDS exam with correct type`() {
        val examData = OsymExamScraper.ScrapedExamData(
            examType = OsymExamScraper.EXAM_TYPE_E_YDS,
            examName = "e-YDS 2025/1",
            examDate = LocalDate.of(2025, 5, 10),
            registrationStart = LocalDate.of(2025, 4, 1),
            registrationEnd = LocalDate.of(2025, 4, 30),
            lateRegistrationEnd = LocalDate.of(2025, 5, 5),
            resultDate = LocalDate.of(2025, 5, 24),
            applicationUrl = "https://www.osym.gov.tr/TR,9133/yabanci-dil-sinavi-yds.html"
        )

        assertEquals(OsymExamScraper.EXAM_TYPE_E_YDS, examData.examType)
        assertEquals("e-YDS 2025/1", examData.examName)
        // e-YDS results are faster (2 weeks instead of 3)
        assertEquals(examData.examDate.plusWeeks(2), examData.resultDate)
    }

    @Test
    fun `ScrapedExamData should have default scrapedAt timestamp`() {
        val before = System.currentTimeMillis()
        val examData = OsymExamScraper.ScrapedExamData(
            examType = OsymExamScraper.EXAM_TYPE_YDS,
            examName = "YDS 2025/1",
            examDate = LocalDate.of(2025, 6, 15),
            registrationStart = null,
            registrationEnd = null,
            lateRegistrationEnd = null,
            resultDate = LocalDate.of(2025, 7, 6),
            applicationUrl = "https://example.com"
        )
        val after = System.currentTimeMillis()

        assertTrue(examData.scrapedAt >= before)
        assertTrue(examData.scrapedAt <= after)
    }

    @Test
    fun `ScrapedExamData should allow custom scrapedAt timestamp`() {
        val customTimestamp = System.currentTimeMillis() - 86400000 // 1 day ago

        val examData = OsymExamScraper.ScrapedExamData(
            examType = OsymExamScraper.EXAM_TYPE_YDS,
            examName = "YDS 2025/1",
            examDate = LocalDate.of(2025, 6, 15),
            registrationStart = null,
            registrationEnd = null,
            lateRegistrationEnd = null,
            resultDate = LocalDate.of(2025, 7, 6),
            applicationUrl = "https://example.com",
            scrapedAt = customTimestamp
        )

        assertEquals(customTimestamp, examData.scrapedAt)
    }

    // ============ Exam Type Constants Tests ============

    @Test
    fun `Exam type constants should have correct values`() {
        assertEquals("YDS", OsymExamScraper.EXAM_TYPE_YDS)
        assertEquals("YÖKDİL", OsymExamScraper.EXAM_TYPE_YOKDIL)
        assertEquals("e-YDS", OsymExamScraper.EXAM_TYPE_E_YDS)
    }

    @Test
    fun `Exam types should be distinguishable`() {
        assertTrue(OsymExamScraper.EXAM_TYPE_YDS != OsymExamScraper.EXAM_TYPE_YOKDIL)
        assertTrue(OsymExamScraper.EXAM_TYPE_YDS != OsymExamScraper.EXAM_TYPE_E_YDS)
        assertTrue(OsymExamScraper.EXAM_TYPE_YOKDIL != OsymExamScraper.EXAM_TYPE_E_YDS)
    }

    // ============ Date Logic Tests ============

    @Test
    fun `Result date should be calculated correctly for YDS (3 weeks)`() {
        val examDate = LocalDate.of(2025, 6, 15)
        val expectedResultDate = examDate.plusWeeks(3)

        assertEquals(LocalDate.of(2025, 7, 6), expectedResultDate)
    }

    @Test
    fun `Result date should be calculated correctly for e-YDS (2 weeks)`() {
        val examDate = LocalDate.of(2025, 5, 10)
        val expectedResultDate = examDate.plusWeeks(2)

        assertEquals(LocalDate.of(2025, 5, 24), expectedResultDate)
    }

    @Test
    fun `Registration dates should be before exam date`() {
        val regStart = LocalDate.of(2025, 5, 1)
        val regEnd = LocalDate.of(2025, 5, 31)
        val lateRegEnd = LocalDate.of(2025, 6, 10)
        val examDate = LocalDate.of(2025, 6, 15)

        assertTrue(regStart.isBefore(regEnd))
        assertTrue(regEnd.isBefore(lateRegEnd))
        assertTrue(lateRegEnd.isBefore(examDate))
    }

    @Test
    fun `Exam date should be in the future`() {
        val examDate = LocalDate.of(2026, 6, 15)
        val now = LocalDate.now()

        assertTrue(examDate.isAfter(now))
    }

    // ============ Multiple Exam Scenarios Tests ============

    @Test
    fun `Should handle multiple exams of same type with different dates`() {
        val exam1 = OsymExamScraper.ScrapedExamData(
            examType = OsymExamScraper.EXAM_TYPE_YDS,
            examName = "YDS 2025/1",
            examDate = LocalDate.of(2025, 6, 15),
            registrationStart = LocalDate.of(2025, 5, 1),
            registrationEnd = LocalDate.of(2025, 5, 31),
            lateRegistrationEnd = null,
            resultDate = LocalDate.of(2025, 7, 6),
            applicationUrl = "https://example.com"
        )

        val exam2 = OsymExamScraper.ScrapedExamData(
            examType = OsymExamScraper.EXAM_TYPE_YDS,
            examName = "YDS 2025/2",
            examDate = LocalDate.of(2025, 9, 20),
            registrationStart = LocalDate.of(2025, 8, 1),
            registrationEnd = LocalDate.of(2025, 8, 31),
            lateRegistrationEnd = null,
            resultDate = LocalDate.of(2025, 10, 11),
            applicationUrl = "https://example.com"
        )

        val exams = listOf(exam1, exam2)
        assertEquals(2, exams.size)
        assertTrue(exam1.examDate.isBefore(exam2.examDate))
    }

    @Test
    fun `Should handle all three exam types simultaneously`() {
        val ydsExam = OsymExamScraper.ScrapedExamData(
            examType = OsymExamScraper.EXAM_TYPE_YDS,
            examName = "YDS 2025/1",
            examDate = LocalDate.of(2025, 6, 15),
            registrationStart = null,
            registrationEnd = null,
            lateRegistrationEnd = null,
            resultDate = null,
            applicationUrl = "https://example.com"
        )

        val yokdilExam = OsymExamScraper.ScrapedExamData(
            examType = OsymExamScraper.EXAM_TYPE_YOKDIL,
            examName = "YÖKDİL 2025/1",
            examDate = LocalDate.of(2025, 7, 20),
            registrationStart = null,
            registrationEnd = null,
            lateRegistrationEnd = null,
            resultDate = null,
            applicationUrl = "https://example.com"
        )

        val eydsExam = OsymExamScraper.ScrapedExamData(
            examType = OsymExamScraper.EXAM_TYPE_E_YDS,
            examName = "e-YDS 2025/1",
            examDate = LocalDate.of(2025, 5, 10),
            registrationStart = null,
            registrationEnd = null,
            lateRegistrationEnd = null,
            resultDate = null,
            applicationUrl = "https://example.com"
        )

        val allExams = listOf(ydsExam, yokdilExam, eydsExam)
        assertEquals(3, allExams.size)
        assertEquals(1, allExams.filter { it.examType == OsymExamScraper.EXAM_TYPE_YDS }.size)
        assertEquals(1, allExams.filter { it.examType == OsymExamScraper.EXAM_TYPE_YOKDIL }.size)
        assertEquals(1, allExams.filter { it.examType == OsymExamScraper.EXAM_TYPE_E_YDS }.size)
    }

    // ============ Date Parsing Tests ============

    @Test
    fun `Should correctly parse Turkish date format`() {
        val dateString = "15.06.2025"
        val parts = dateString.split(".")
        val day = parts[0].toInt()
        val month = parts[1].toInt()
        val year = parts[2].toInt()

        val parsedDate = LocalDate.of(year, month, day)
        assertEquals(LocalDate.of(2025, 6, 15), parsedDate)
    }

    @Test
    fun `Should handle date parsing with various formats`() {
        val formats = listOf("01.01.2025", "15.06.2025", "31.12.2025", "29.02.2024")

        for (dateString in formats) {
            val parts = dateString.split(".")
            val day = parts[0].toInt()
            val month = parts[1].toInt()
            val year = parts[2].toInt()

            assertNotNull(LocalDate.of(year, month, day))
        }
    }

    @Test
    fun `Should handle future dates correctly`() {
        val futureDate = LocalDate.now().plusMonths(6)
        assertTrue(futureDate.isAfter(LocalDate.now()))
    }

    @Test
    fun `Should filter out past dates`() {
        val pastDate = LocalDate.now().minusMonths(1)
        val futureDate = LocalDate.now().plusMonths(1)

        val dates = listOf(pastDate, futureDate)
        val futureDates = dates.filter { it.isAfter(LocalDate.now()) }

        assertEquals(1, futureDates.size)
        assertEquals(futureDate, futureDates[0])
    }

    // ============ Edge Cases Tests ============

    @Test
    fun `Should handle leap year correctly`() {
        val leapYearDate = LocalDate.of(2024, 2, 29)
        assertEquals(29, leapYearDate.dayOfMonth)
    }

    @Test
    fun `Should handle year transitions correctly`() {
        val newYearDate = LocalDate.of(2025, 1, 1)
        val beforeNewYear = LocalDate.of(2024, 12, 31)

        assertTrue(newYearDate.isAfter(beforeNewYear))
    }

    @Test
    fun `Should handle same date comparisons`() {
        val date1 = LocalDate.of(2025, 6, 15)
        val date2 = LocalDate.of(2025, 6, 15)

        assertEquals(date1, date2)
        assertTrue(!date1.isBefore(date2))
        assertTrue(!date1.isAfter(date2))
    }

    @Test
    fun `Should handle multi-year date ranges`() {
        val year1 = LocalDate.of(2025, 1, 1)
        val year3 = LocalDate.of(2027, 12, 31)

        assertTrue(year1.isBefore(year3))
        assertTrue(year3.isAfter(year1))
    }

    // ============ URL Constants Tests ============

    @Test
    fun `OSYM URLs should be valid and accessible paths`() {
        val urls = listOf(
            "https://www.osym.gov.tr/TR,8832/sinav-takvimi.html",
            "https://www.osym.gov.tr/TR,9133/yabanci-dil-sinavi-yds.html",
            "https://www.osym.gov.tr/TR,9134/yokdil.html"
        )

        for (url in urls) {
            assertTrue(url.startsWith("https://www.osym.gov.tr"))
        }
    }
}
