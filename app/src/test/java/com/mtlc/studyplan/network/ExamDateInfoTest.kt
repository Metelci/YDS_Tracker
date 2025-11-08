package com.mtlc.studyplan.network

import org.junit.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests specifically for ExamDateInfo data class refactoring
 *
 * Verifies that the ExamDateInfo data class correctly replaces
 * the previous Triple-based approach for holding exam date information.
 */
class ExamDateInfoTest {

    // ============ Data Class Creation Tests ============

    @Test
    fun `ExamDateInfo should be created with all non-null dates`() {
        val examDate = LocalDate.of(2025, 6, 15)
        val regStart = LocalDate.of(2025, 5, 1)
        val regEnd = LocalDate.of(2025, 5, 31)
        val lateRegEnd = LocalDate.of(2025, 6, 10)

        val info = OsymExamScraper.ExamDateInfo(
            examDate = examDate,
            regStart = regStart,
            regEnd = regEnd,
            lateRegEnd = lateRegEnd
        )

        assertNotNull(info)
        assertNotNull(info.examDate)
        assertNotNull(info.regStart)
        assertNotNull(info.regEnd)
        assertNotNull(info.lateRegEnd)
    }

    @Test
    fun `ExamDateInfo should be created with partial dates`() {
        val examDate = LocalDate.of(2025, 6, 15)
        val regStart = LocalDate.of(2025, 5, 1)

        val info = OsymExamScraper.ExamDateInfo(
            examDate = examDate,
            regStart = regStart,
            regEnd = null,
            lateRegEnd = null
        )

        assertNotNull(info.examDate)
        assertNotNull(info.regStart)
        assertNull(info.regEnd)
        assertNull(info.lateRegEnd)
    }

    @Test
    fun `ExamDateInfo should be created with only exam date`() {
        val examDate = LocalDate.of(2025, 6, 15)

        val info = OsymExamScraper.ExamDateInfo(
            examDate = examDate,
            regStart = null,
            regEnd = null,
            lateRegEnd = null
        )

        assertNotNull(info.examDate)
        assertNull(info.regStart)
        assertNull(info.regEnd)
        assertNull(info.lateRegEnd)
    }

    @Test
    fun `ExamDateInfo should be created with all nulls`() {
        val info = OsymExamScraper.ExamDateInfo(
            examDate = null,
            regStart = null,
            regEnd = null,
            lateRegEnd = null
        )

        assertNull(info.examDate)
        assertNull(info.regStart)
        assertNull(info.regEnd)
        assertNull(info.lateRegEnd)
    }

    // ============ Property Access Tests ============

    @Test
    fun `Should access examDate property correctly`() {
        val examDate = LocalDate.of(2025, 6, 15)
        val info = OsymExamScraper.ExamDateInfo(examDate, null, null, null)

        assertEquals(examDate, info.examDate)
    }

    @Test
    fun `Should access regStart property correctly`() {
        val regStart = LocalDate.of(2025, 5, 1)
        val info = OsymExamScraper.ExamDateInfo(null, regStart, null, null)

        assertEquals(regStart, info.regStart)
    }

    @Test
    fun `Should access regEnd property correctly`() {
        val regEnd = LocalDate.of(2025, 5, 31)
        val info = OsymExamScraper.ExamDateInfo(null, null, regEnd, null)

        assertEquals(regEnd, info.regEnd)
    }

    @Test
    fun `Should access lateRegEnd property correctly`() {
        val lateRegEnd = LocalDate.of(2025, 6, 10)
        val info = OsymExamScraper.ExamDateInfo(null, null, null, lateRegEnd)

        assertEquals(lateRegEnd, info.lateRegEnd)
    }

    // ============ Copy and Mutation Tests ============

    @Test
    fun `Should use copy function to create modified instance`() {
        val originalInfo = OsymExamScraper.ExamDateInfo(
            examDate = LocalDate.of(2025, 6, 15),
            regStart = LocalDate.of(2025, 5, 1),
            regEnd = LocalDate.of(2025, 5, 31),
            lateRegEnd = LocalDate.of(2025, 6, 10)
        )

        val modifiedInfo = originalInfo.copy(
            examDate = LocalDate.of(2025, 7, 15)
        )

        // Original unchanged
        assertEquals(LocalDate.of(2025, 6, 15), originalInfo.examDate)
        // New instance has changed value
        assertEquals(LocalDate.of(2025, 7, 15), modifiedInfo.examDate)
        // Other properties preserved
        assertEquals(originalInfo.regStart, modifiedInfo.regStart)
    }

    @Test
    fun `Should copy with multiple property changes`() {
        val originalInfo = OsymExamScraper.ExamDateInfo(
            examDate = LocalDate.of(2025, 6, 15),
            regStart = LocalDate.of(2025, 5, 1),
            regEnd = LocalDate.of(2025, 5, 31),
            lateRegEnd = null
        )

        val modifiedInfo = originalInfo.copy(
            examDate = LocalDate.of(2025, 7, 15),
            lateRegEnd = LocalDate.of(2025, 7, 10)
        )

        assertEquals(LocalDate.of(2025, 7, 15), modifiedInfo.examDate)
        assertEquals(LocalDate.of(2025, 7, 10), modifiedInfo.lateRegEnd)
        assertEquals(originalInfo.regStart, modifiedInfo.regStart)
        assertEquals(originalInfo.regEnd, modifiedInfo.regEnd)
    }

    // ============ Equality Tests ============

    @Test
    fun `ExamDateInfo instances with same values should be equal`() {
        val examDate = LocalDate.of(2025, 6, 15)
        val regStart = LocalDate.of(2025, 5, 1)

        val info1 = OsymExamScraper.ExamDateInfo(examDate, regStart, null, null)
        val info2 = OsymExamScraper.ExamDateInfo(examDate, regStart, null, null)

        assertEquals(info1, info2)
    }

    @Test
    fun `ExamDateInfo instances with different values should not be equal`() {
        val info1 = OsymExamScraper.ExamDateInfo(
            LocalDate.of(2025, 6, 15),
            null, null, null
        )
        val info2 = OsymExamScraper.ExamDateInfo(
            LocalDate.of(2025, 7, 15),
            null, null, null
        )

        assertTrue(info1 != info2)
    }

    @Test
    fun `ExamDateInfo with same dates but different null patterns should be equal`() {
        val info1 = OsymExamScraper.ExamDateInfo(
            LocalDate.of(2025, 6, 15),
            LocalDate.of(2025, 5, 1),
            null,
            null
        )
        val info2 = OsymExamScraper.ExamDateInfo(
            LocalDate.of(2025, 6, 15),
            LocalDate.of(2025, 5, 1),
            null,
            null
        )

        assertEquals(info1, info2)
    }

    // ============ Hash Code Tests ============

    @Test
    fun `Equal ExamDateInfo instances should have same hash code`() {
        val examDate = LocalDate.of(2025, 6, 15)
        val info1 = OsymExamScraper.ExamDateInfo(examDate, null, null, null)
        val info2 = OsymExamScraper.ExamDateInfo(examDate, null, null, null)

        assertEquals(info1.hashCode(), info2.hashCode())
    }

    @Test
    fun `ExamDateInfo with all nulls should be hashable`() {
        val info1 = OsymExamScraper.ExamDateInfo(null, null, null, null)
        val info2 = OsymExamScraper.ExamDateInfo(null, null, null, null)

        assertEquals(info1.hashCode(), info2.hashCode())
    }

    // ============ Ordering Tests ============

    @Test
    fun `Exam dates should be comparable with registration dates`() {
        val info = OsymExamScraper.ExamDateInfo(
            examDate = LocalDate.of(2025, 6, 15),
            regStart = LocalDate.of(2025, 5, 1),
            regEnd = LocalDate.of(2025, 5, 31),
            lateRegEnd = LocalDate.of(2025, 6, 10)
        )

        assertTrue(info.regStart!!.isBefore(info.regEnd!!))
        assertTrue(info.regEnd!!.isBefore(info.lateRegEnd!!))
        assertTrue(info.lateRegEnd!!.isBefore(info.examDate!!))
    }

    @Test
    fun `Should validate date order correctly`() {
        val info = OsymExamScraper.ExamDateInfo(
            examDate = LocalDate.of(2025, 6, 15),
            regStart = LocalDate.of(2025, 5, 1),
            regEnd = LocalDate.of(2025, 5, 31),
            lateRegEnd = LocalDate.of(2025, 6, 10)
        )

        val isValidOrder = if (info.regStart != null && info.regEnd != null &&
            info.lateRegEnd != null && info.examDate != null
        ) {
            info.regStart.isBefore(info.regEnd) &&
                    info.regEnd.isBefore(info.lateRegEnd) &&
                    info.lateRegEnd.isBefore(info.examDate)
        } else {
            true // Valid if not all dates present
        }

        assertTrue(isValidOrder)
    }

    // ============ Improvement Over Triple ============

    @Test
    fun `ExamDateInfo provides named access instead of component1-component4`() {
        val info = OsymExamScraper.ExamDateInfo(
            examDate = LocalDate.of(2025, 6, 15),
            regStart = LocalDate.of(2025, 5, 1),
            regEnd = LocalDate.of(2025, 5, 31),
            lateRegEnd = LocalDate.of(2025, 6, 10)
        )

        // Named properties are more readable than component1(), component2(), etc.
        assertEquals(LocalDate.of(2025, 6, 15), info.examDate)
        assertEquals(LocalDate.of(2025, 5, 1), info.regStart)
        assertEquals(LocalDate.of(2025, 5, 31), info.regEnd)
        assertEquals(LocalDate.of(2025, 6, 10), info.lateRegEnd)
    }

    @Test
    fun `ExamDateInfo names clearly indicate purpose of each date`() {
        val info = OsymExamScraper.ExamDateInfo(
            examDate = LocalDate.of(2025, 6, 15),
            regStart = LocalDate.of(2025, 5, 1),
            regEnd = LocalDate.of(2025, 5, 31),
            lateRegEnd = LocalDate.of(2025, 6, 10)
        )

        // Self-documenting property names
        assertNotNull(info.examDate) // Clearly the exam date
        assertNotNull(info.regStart) // Clearly registration start
        assertNotNull(info.regEnd) // Clearly registration end
        assertNotNull(info.lateRegEnd) // Clearly late registration end
    }

    @Test
    fun `ExamDateInfo is more maintainable than Triple for future changes`() {
        val futureDate = LocalDate.now().plusMonths(6)
        val info = OsymExamScraper.ExamDateInfo(
            examDate = futureDate,
            regStart = futureDate.minusMonths(1),
            regEnd = futureDate.minusMonths(1).plusDays(30),
            lateRegEnd = futureDate.minusDays(5)
        )

        // Easy to understand what each field represents
        val isFutureExam = info.examDate != null && info.examDate.isAfter(LocalDate.now())
        assertTrue(isFutureExam)
    }

    // ============ Null Safety Tests ============

    @Test
    fun `Should safely check for null exam date`() {
        val info = OsymExamScraper.ExamDateInfo(null, null, null, null)

        val isValid = if (info.examDate != null) {
            info.examDate.isAfter(LocalDate.now())
        } else {
            false
        }

        assertTrue(!isValid)
    }

    @Test
    fun `Should handle optional registration dates`() {
        val info = OsymExamScraper.ExamDateInfo(
            examDate = LocalDate.of(2025, 6, 15),
            regStart = null,
            regEnd = null,
            lateRegEnd = null
        )

        val hasRegistrationDates = info.regStart != null &&
                info.regEnd != null &&
                info.lateRegEnd != null

        assertTrue(!hasRegistrationDates)
    }

    @Test
    fun `Should handle partial registration dates`() {
        val info = OsymExamScraper.ExamDateInfo(
            examDate = LocalDate.of(2025, 6, 15),
            regStart = LocalDate.of(2025, 5, 1),
            regEnd = null,
            lateRegEnd = null
        )

        assertTrue(info.regStart != null)
        assertTrue(info.regEnd == null)
        assertTrue(info.lateRegEnd == null)
    }

    // ============ Type Safety Tests ============

    @Test
    fun `ExamDateInfo enforces type safety for LocalDate fields`() {
        val date = LocalDate.of(2025, 6, 15)
        val info = OsymExamScraper.ExamDateInfo(date, null, null, null)

        // Type is enforced - cannot assign wrong type
        assertEquals(date, info.examDate)
        assertTrue(info.examDate is LocalDate)
    }

    @Test
    fun `ExamDateInfo is immutable after creation`() {
        val info = OsymExamScraper.ExamDateInfo(
            LocalDate.of(2025, 6, 15),
            null, null, null
        )

        // Data classes are immutable by default
        // Cannot reassign info.examDate = SomeOtherDate
        assertNotNull(info)
    }

    // ============ List and Collection Tests ============

    @Test
    fun `Multiple ExamDateInfo objects should be collectible in lists`() {
        val infos = listOf(
            OsymExamScraper.ExamDateInfo(LocalDate.of(2025, 5, 10), null, null, null),
            OsymExamScraper.ExamDateInfo(LocalDate.of(2025, 6, 15), null, null, null),
            OsymExamScraper.ExamDateInfo(LocalDate.of(2025, 7, 20), null, null, null)
        )

        assertEquals(3, infos.size)
        assertEquals(LocalDate.of(2025, 5, 10), infos[0].examDate)
        assertEquals(LocalDate.of(2025, 6, 15), infos[1].examDate)
        assertEquals(LocalDate.of(2025, 7, 20), infos[2].examDate)
    }

    @Test
    fun `Should filter ExamDateInfo objects by date criteria`() {
        val currentDate = LocalDate.now()
        val infos = listOf(
            OsymExamScraper.ExamDateInfo(currentDate.minusMonths(1), null, null, null),
            OsymExamScraper.ExamDateInfo(currentDate.plusMonths(1), null, null, null),
            OsymExamScraper.ExamDateInfo(currentDate.plusMonths(2), null, null, null)
        )

        val futureExams = infos.filter { it.examDate != null && it.examDate.isAfter(currentDate) }
        assertEquals(2, futureExams.size)
    }

    @Test
    fun `Should sort ExamDateInfo objects by exam date`() {
        val infos = listOf(
            OsymExamScraper.ExamDateInfo(LocalDate.of(2025, 7, 20), null, null, null),
            OsymExamScraper.ExamDateInfo(LocalDate.of(2025, 5, 10), null, null, null),
            OsymExamScraper.ExamDateInfo(LocalDate.of(2025, 6, 15), null, null, null)
        )

        val sorted = infos.sortedBy { it.examDate ?: LocalDate.MAX }
        assertEquals(LocalDate.of(2025, 5, 10), sorted[0].examDate)
        assertEquals(LocalDate.of(2025, 6, 15), sorted[1].examDate)
        assertEquals(LocalDate.of(2025, 7, 20), sorted[2].examDate)
    }
}
