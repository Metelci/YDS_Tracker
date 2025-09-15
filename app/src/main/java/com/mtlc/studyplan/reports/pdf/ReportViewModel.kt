package com.mtlc.studyplan.reports.pdf

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtlc.studyplan.analytics.AnalyticsEngine
import com.mtlc.studyplan.data.ProgressRepository
import com.mtlc.studyplan.data.StreakManager
import com.mtlc.studyplan.data.TaskLog
import com.mtlc.studyplan.data.UserProgress
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * ViewModel for coordinating PDF report generation
 */
class ReportViewModel(
    private val progressRepository: ProgressRepository,
    private val streakManager: StreakManager,
    private val analyticsEngine: AnalyticsEngine = AnalyticsEngine()
) : ViewModel() {

    private val pdfGenerator = PdfReportGenerator()

    private val _reportState = MutableStateFlow<ReportState>(ReportState.Idle)
    val reportState: StateFlow<ReportState> = _reportState.asStateFlow()

    private val _generatedReport = MutableStateFlow<ReportResult?>(null)
    val generatedReport: StateFlow<ReportResult?> = _generatedReport.asStateFlow()

    /**
     * Build and generate a PDF report for the specified date range
     */
    fun buildReport(
        dateRange: ClosedRange<LocalDate>,
        studentName: String? = null,
        includeName: Boolean = false
    ) {
        viewModelScope.launch {
            try {
                _reportState.value = ReportState.Loading("Collecting study data...")

                // Gather data from repositories
                val userProgress = progressRepository.userProgressFlow.first()
                val taskLogs = progressRepository.taskLogsFlow.first()

                _reportState.value = ReportState.Loading("Analyzing performance...")

                // Filter task logs to date range
                val startEpoch = dateRange.start.atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000
                val endEpoch = dateRange.endInclusive.plusDays(1).atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000
                val filteredLogs = taskLogs.filter { 
                    it.timestampMillis >= startEpoch && it.timestampMillis < endEpoch 
                }

                // Build report request
                val reportRequest = buildReportRequest(
                    dateRange = dateRange,
                    studentName = if (includeName) studentName else null,
                    userProgress = userProgress,
                    taskLogs = filteredLogs
                )

                _reportState.value = ReportState.Loading("Generating PDF report...")

                // Generate PDF
                val reportResult = pdfGenerator.generate(reportRequest)

                _generatedReport.value = reportResult
                _reportState.value = ReportState.Success(reportResult)

            } catch (e: Exception) {
                _reportState.value = ReportState.Error("Failed to generate report: ${e.message}")
            }
        }
    }

    /**
     * Build a ReportRequest from the collected data
     */
    private suspend fun buildReportRequest(
        dateRange: ClosedRange<LocalDate>,
        studentName: String?,
        userProgress: UserProgress,
        taskLogs: List<TaskLog>
    ): ReportRequest {
        // Generate analytics for the date range
        val days = java.time.temporal.ChronoUnit.DAYS.between(dateRange.start, dateRange.endInclusive).toInt() + 1
        val analyticsData = analyticsEngine.generateAnalytics(days, taskLogs, userProgress)

        // Build daily loads
        val dailyLoads = buildDailyLoads(dateRange, taskLogs, userProgress)

        // Build study events
        val studyEvents = buildStudyEvents(taskLogs)

        // Build skill minutes map
        val skillMinutes = buildSkillMinutes(taskLogs)

        return ReportRequest(
            studentName = studentName,
            dateRange = dateRange,
            dailyLoads = dailyLoads,
            events = studyEvents,
            skillMinutes = skillMinutes,
            recommendations = analyticsData.recommendations
        )
    }

    /**
     * Build daily load data from task logs
     */
    private fun buildDailyLoads(
        dateRange: ClosedRange<LocalDate>,
        taskLogs: List<TaskLog>,
        userProgress: UserProgress
    ): List<UserDailyLoad> {
        val dailyData = mutableMapOf<LocalDate, MutableList<TaskLog>>()

        // Group task logs by date
        taskLogs.forEach { log ->
            val date = LocalDateTime.ofEpochSecond(log.timestampMillis / 1000, 0, ZoneOffset.UTC).toLocalDate()
            dailyData.getOrPut(date) { mutableListOf() }.add(log)
        }

        // Generate daily loads for each day in range
        return generateSequence(dateRange.start) { it.plusDays(1) }
            .takeWhile { it <= dateRange.endInclusive }
            .mapIndexed { index, date ->
                val dayLogs = dailyData[date] ?: emptyList()
                val totalMinutes = dayLogs.sumOf { it.minutesSpent }
                val tasksCompleted = dayLogs.count { it.correct }
                val averageAccuracy = if (dayLogs.isNotEmpty()) {
                    dayLogs.map { if (it.correct) 1f else 0f }.average().toFloat()
                } else 0f

                // Build skill breakdown for the day
                val skillBreakdown = dayLogs.groupBy { 
                    Skill.fromString(it.category) 
                }.mapValues { (_, logs) -> 
                    logs.sumOf { it.minutesSpent } 
                }

                UserDailyLoad(
                    date = date,
                    totalMinutes = totalMinutes,
                    tasksCompleted = tasksCompleted,
                    averageAccuracy = averageAccuracy,
                    skillBreakdown = skillBreakdown,
                    streakDayNumber = if (tasksCompleted > 0) calculateStreakDay(date, userProgress) else 0
                )
            }
            .toList()
    }

    /**
     * Build study events from task logs
     */
    private fun buildStudyEvents(taskLogs: List<TaskLog>): List<StudyEvent> {
        return taskLogs.map { log ->
            StudyEvent(
                id = "${log.taskId}_${log.timestampMillis}",
                timestamp = log.timestampMillis,
                skill = Skill.fromString(log.category),
                durationMinutes = log.minutesSpent,
                accuracy = if (log.correct) 1f else 0f,
                difficulty = calculateEventDifficulty(log),
                taskCount = 1,
                pointsEarned = log.pointsEarned
            )
        }
    }

    /**
     * Build skill minutes map
     */
    private fun buildSkillMinutes(taskLogs: List<TaskLog>): Map<Skill, Int> {
        return taskLogs.groupBy { Skill.fromString(it.category) }
            .mapValues { (_, logs) -> logs.sumOf { it.minutesSpent } }
    }

    /**
     * Calculate streak day number for a given date
     */
    private fun calculateStreakDay(date: LocalDate, userProgress: UserProgress): Int {
        // Simplified calculation - in real implementation, this would be more sophisticated
        val daysSinceStart = java.time.temporal.ChronoUnit.DAYS.between(
            LocalDate.ofEpochDay(userProgress.lastCompletionDate / (24 * 60 * 60 * 1000)),
            date
        ).toInt()
        return minOf(daysSinceStart + 1, userProgress.streakCount)
    }

    /**
     * Calculate event difficulty based on task log metrics
     */
    private fun calculateEventDifficulty(log: TaskLog): EventDifficulty {
        return when {
            log.minutesSpent > 60 -> EventDifficulty.EXPERT
            log.minutesSpent > 30 -> EventDifficulty.ADVANCED
            log.minutesSpent > 15 -> EventDifficulty.INTERMEDIATE
            else -> EventDifficulty.BEGINNER
        }
    }

    /**
     * Get report for a common 4-week period
     */
    fun buildFourWeekReport(studentName: String? = null, includeName: Boolean = false) {
        val endDate = LocalDate.now()
        val startDate = endDate.minusWeeks(4)
        buildReport(startDate..endDate, studentName, includeName)
    }

    /**
     * Get report for last week
     */
    fun buildWeeklyReport(studentName: String? = null, includeName: Boolean = false) {
        val endDate = LocalDate.now()
        val startDate = endDate.minusWeeks(1)
        buildReport(startDate..endDate, studentName, includeName)
    }

    /**
     * Get report for last month
     */
    fun buildMonthlyReport(studentName: String? = null, includeName: Boolean = false) {
        val endDate = LocalDate.now()
        val startDate = endDate.minusMonths(1)
        buildReport(startDate..endDate, studentName, includeName)
    }

    /**
     * Clear current report state
     */
    fun clearReport() {
        _reportState.value = ReportState.Idle
        _generatedReport.value = null
    }

    /**
     * Check if sufficient data exists for report generation
     */
    suspend fun hasDataForReport(dateRange: ClosedRange<LocalDate>): Boolean {
        return try {
            val taskLogs = progressRepository.taskLogsFlow.first()
            val startEpoch = dateRange.start.atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000
            val endEpoch = dateRange.endInclusive.plusDays(1).atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000
            
            val filteredLogs = taskLogs.filter { 
                it.timestampMillis >= startEpoch && it.timestampMillis < endEpoch 
            }
            
            filteredLogs.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get preview statistics for report
     */
    suspend fun getReportPreview(dateRange: ClosedRange<LocalDate>): ReportPreview? {
        return try {
            val taskLogs = progressRepository.taskLogsFlow.first()
            val userProgress = progressRepository.userProgressFlow.first()
            
            val startEpoch = dateRange.start.atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000
            val endEpoch = dateRange.endInclusive.plusDays(1).atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000
            
            val filteredLogs = taskLogs.filter { 
                it.timestampMillis >= startEpoch && it.timestampMillis < endEpoch 
            }
            
            if (filteredLogs.isEmpty()) return null
            
            ReportPreview(
                totalMinutes = filteredLogs.sumOf { it.minutesSpent },
                totalTasks = filteredLogs.count { it.correct },
                averageAccuracy = filteredLogs.map { if (it.correct) 1f else 0f }.average().toFloat(),
                studyDays = filteredLogs.map { log ->
                    LocalDateTime.ofEpochSecond(log.timestampMillis / 1000, 0, ZoneOffset.UTC).toLocalDate()
                }.distinct().size,
                currentStreak = userProgress.streakCount,
                topSkill = filteredLogs.groupBy { it.category }
                    .maxByOrNull { (_, logs) -> logs.sumOf { it.minutesSpent } }?.key
            )
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * States for report generation
 */
sealed class ReportState {
    object Idle : ReportState()
    data class Loading(val message: String) : ReportState()
    data class Success(val report: ReportResult) : ReportState()
    data class Error(val message: String) : ReportState()
}

/**
 * Preview data for report generation
 */
data class ReportPreview(
    val totalMinutes: Int,
    val totalTasks: Int,
    val averageAccuracy: Float,
    val studyDays: Int,
    val currentStreak: Int,
    val topSkill: String?
) {
    val totalHours: Float = totalMinutes / 60f
    val dailyAverage: Float = if (studyDays > 0) totalMinutes.toFloat() / studyDays else 0f
    
    fun getFormattedAccuracy(): String = "${(averageAccuracy * 100).toInt()}%"
    
    fun getFormattedTime(): String = when {
        totalHours >= 1f -> String.format("%.1f hours", totalHours)
        else -> "$totalMinutes minutes"
    }
    
    fun getFormattedDailyAverage(): String = when {
        dailyAverage >= 60f -> String.format("%.1f hrs/day", dailyAverage / 60f)
        else -> "${dailyAverage.toInt()} min/day"
    }
}

/**
 * Factory for creating ReportViewModel instances
 */
class ReportViewModelFactory(
    private val progressRepository: ProgressRepository,
    private val streakManager: StreakManager
) : androidx.lifecycle.ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReportViewModel::class.java)) {
            return ReportViewModel(progressRepository, streakManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}