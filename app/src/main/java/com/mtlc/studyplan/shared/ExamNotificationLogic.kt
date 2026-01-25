package com.mtlc.studyplan.shared

import com.mtlc.studyplan.data.ExamInfo
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object ExamNotificationLogic {

    fun calculateActiveExamNotification(
        today: LocalDate,
        exams: List<ExamInfo>,
        isDismissed: (String) -> Boolean
    ): ExamNotification? {
        val notifications = exams.mapNotNull { exam ->
            val start = exam.applicationStart
            val end = exam.applicationEnd
            
            // Check for "Last 3 Days" first (High Priority)
            if (!today.isBefore(end.minusDays(3)) && !today.isAfter(end)) {
                val key = "last3_${exam.name.filter { it.isLetterOrDigit() }}_${exam.examDate.year}"
                if (!isDismissed(key)) {
                     ExamNotification(
                        key = key,
                        title = "⚠️ Son 3 Gün!",
                        message = "${exam.name} başvuruları ${end.format(DateTimeFormatter.ofPattern("dd MMMM"))} tarihinde bitiyor.",
                        type = NotificationType.WARNING,
                        priority = 2
                    )
                } else null
            } else if (!today.isBefore(start) && !today.isAfter(end)) {
                 // Check for "Application Started" (Normal Priority)
                val key = "open_${exam.name.filter { it.isLetterOrDigit() }}_${exam.examDate.year}"
                if (!isDismissed(key)) {
                    ExamNotification(
                        key = key,
                        title = "Başvurular Başladı!",
                        message = "${exam.name} için başvurularınızı yapabilirsiniz.",
                        type = NotificationType.INFO,
                        priority = 1
                    )
                } else null
            } else {
                null
            }
        }

        return notifications.maxByOrNull { it.priority }
    }
}
