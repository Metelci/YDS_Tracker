package com.mtlc.studyplan.shared

import com.mtlc.studyplan.data.ExamInfo
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class ExamNotificationLogicTest {

    private val examYds1 = ExamInfo(
        name = "YDS/1",
        applicationStart = LocalDate.of(2026, 2, 1),
        applicationEnd = LocalDate.of(2026, 2, 10),
        examDate = LocalDate.of(2026, 4, 1)
    )

    private val exams = listOf(examYds1)

    @Test
    fun `when today is before application start, returns null`() {
        val today = LocalDate.of(2026, 1, 31)
        val result = ExamNotificationLogic.calculateActiveExamNotification(today, exams) { false }
        assertNull(result)
    }

    @Test
    fun `when today is application start, returns INFO notification`() {
        val today = LocalDate.of(2026, 2, 1)
        val result = ExamNotificationLogic.calculateActiveExamNotification(today, exams) { false }
        
        assertNotNull(result)
        assertEquals(NotificationType.INFO, result?.type)
        assertEquals("Başvurular Başladı!", result?.title)
    }

    @Test
    fun `when today is in middle of application, returns INFO notification`() {
        val today = LocalDate.of(2026, 2, 5)
        val result = ExamNotificationLogic.calculateActiveExamNotification(today, exams) { false }
        
        assertNotNull(result)
        assertEquals(NotificationType.INFO, result?.type)
    }

    @Test
    fun `when today is 3 days before end, returns WARNING notification`() {
        val today = LocalDate.of(2026, 2, 7) // End is 10th. 10-3 = 7. So 7,8,9,10 are last 3 days range (actually last 4 days inclusive logic?)
        // The code says: if (today >= end.minusDays(3) && today <= end)
        // 10-3 = 7. So >= 7.
        
        val result = ExamNotificationLogic.calculateActiveExamNotification(today, exams) { false }
        
        assertNotNull(result)
        assertEquals(NotificationType.WARNING, result?.type)
        assertEquals("⚠️ Son 3 Gün!", result?.title)
    }

    @Test
    fun `when today is last day, returns WARNING notification`() {
        val today = LocalDate.of(2026, 2, 10)
        val result = ExamNotificationLogic.calculateActiveExamNotification(today, exams) { false }
        
        assertNotNull(result)
        assertEquals(NotificationType.WARNING, result?.type)
    }

    @Test
    fun `when today is after application end, returns null`() {
        val today = LocalDate.of(2026, 2, 11)
        val result = ExamNotificationLogic.calculateActiveExamNotification(today, exams) { false }
        assertNull(result)
    }

    @Test
    fun `when notification is dismissed, returns null`() {
        val today = LocalDate.of(2026, 2, 5) // Should be INFO
        val result = ExamNotificationLogic.calculateActiveExamNotification(today, exams) { key -> 
            true // All dismissed
        }
        assertNull(result)
    }
    
    @Test
    fun `when notification is active but specific key dismissed, returns null`() {
        val today = LocalDate.of(2026, 2, 5)
        // Key logic: "open_${exam.name.filter { it.isLetterOrDigit() }}_${exam.examDate.year}"
        // YDS/1 -> open_YDS1_2026
        
        val result = ExamNotificationLogic.calculateActiveExamNotification(today, exams) { key -> 
            key == "open_YDS1_2026"
        }
        assertNull(result)
    }
}
