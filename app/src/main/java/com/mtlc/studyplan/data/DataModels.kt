//region VERİ MODELLERİ
package com.mtlc.studyplan.data

data class Task(val id: String, val desc: String, val details: String? = null)
data class DayPlan(val day: String, val tasks: List<Task>)
data class WeekPlan(val week: Int, val month: Int, val title: String, val days: List<DayPlan>)
data class Achievement(val id: String, val title: String, val description: String, val condition: (UserProgress) -> Boolean)
data class ExamInfo(val name: String, val applicationStart: java.time.LocalDate, val applicationEnd: java.time.LocalDate, val examDate: java.time.LocalDate)
data class UserProgress(
    val completedTasks: Set<String> = emptySet(),
    val streakCount: Int = 0,
    val lastCompletionDate: Long = 0L,
    val unlockedAchievements: Set<String> = emptySet(),
)

data class TaskLog(
    val taskId: String,
    val timestampMillis: Long,
    val minutesSpent: Int,
    val correct: Boolean,
    val category: String,
)

data class WeaknessSummary(
    val category: String,
    val total: Int,
    val incorrect: Int,
    val incorrectRate: Double,
)
//endregion
