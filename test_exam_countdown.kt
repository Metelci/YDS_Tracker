import java.time.LocalDate
import java.time.temporal.ChronoUnit

// Quick test for YDS exam countdown calculation
fun main() {
    val examDate = LocalDate.of(2025, 7, 5)  // YDS 2025/1
    val today = LocalDate.now()
    val daysToExam = ChronoUnit.DAYS.between(today, examDate).toInt()

    println("Today: $today")
    println("YDS Exam Date: $examDate")
    println("Days to YDS Exam: $daysToExam")

    val statusMessage = when {
        daysToExam == 0 -> "Exam day!"
        daysToExam < 0 -> "Exam completed"
        daysToExam <= 7 -> "Final week!"
        daysToExam <= 30 -> "Almost there!"
        daysToExam <= 90 -> "Preparation phase"
        else -> "Long-term planning"
    }

    println("Status: $statusMessage")
}