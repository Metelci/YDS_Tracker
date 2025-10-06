package com.mtlc.studyplan.core

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Task
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import com.mtlc.studyplan.ui.theme.FeatureKey
import com.mtlc.studyplan.ui.theme.featurePastelContainer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
// removed luminance-based dark theme checks
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtlc.studyplan.data.CompleteMurphyBookData
import com.mtlc.studyplan.data.DayPlan
import com.mtlc.studyplan.data.MurphyBook
import com.mtlc.studyplan.data.MurphyUnit
import com.mtlc.studyplan.data.PlanDataSource
import com.mtlc.studyplan.data.PlanTaskLocalizer
import com.mtlc.studyplan.data.StudyProgressRepository
import com.mtlc.studyplan.data.Task
import com.mtlc.studyplan.data.TaskPriority
import com.mtlc.studyplan.data.TaskRepository
import com.mtlc.studyplan.R
import com.mtlc.studyplan.data.WeekPlan
import com.mtlc.studyplan.integration.AppIntegrationManager
import com.mtlc.studyplan.shared.SharedAppViewModel
import com.mtlc.studyplan.ui.responsive.responsiveHeights
import com.mtlc.studyplan.ui.responsive.touchTargetSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// Pagination support for large datasets
data class TaskListState(
    val currentPage: Int = 0,
    val pageSize: Int = 20,
    val isLoading: Boolean = false,
    val hasMorePages: Boolean = true,
    val totalCount: Int = 0
)

// Background data processing for heavy operations
class BackgroundTaskProcessor(
    private val taskRepository: TaskRepository
) {
    private val processingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun processLargeDatasetAsync(
        operation: suspend () -> Unit,
        onProgress: (String) -> Unit = {},
        onComplete: (Result<Unit>) -> Unit = {}
    ) {
        processingScope.launch {
            try {
                withContext(Dispatchers.Main) {
                    onProgress("Starting background processing...")
                }

                operation()

                withContext(Dispatchers.Main) {
                    onProgress("Processing completed")
                    onComplete(Result.success(Unit))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onProgress("Processing failed: ${e.message}")
                    onComplete(Result.failure(e))
                }
            }
        }
    }

    suspend fun cleanupOldData(olderThanDays: Int = 90): Int {
        return withContext(Dispatchers.IO) {
            // Simulate cleanup of old completed tasks
            val cutoffTime = System.currentTimeMillis() - (olderThanDays * 24 * 60 * 60 * 1000L)

            // In a real implementation, this would archive or delete old data
            // For now, just return a simulated count
            val oldTasksCount = taskRepository.getAllTasksSync()
                .filter { it.completedAt != null && it.completedAt!! < cutoffTime }
                .size

            // Simulate processing time for large datasets
            delay(minOf(oldTasksCount * 10L, 5000L))

            oldTasksCount
        }
    }

    suspend fun compressTaskHistory(): Int {
        return withContext(Dispatchers.IO) {
            // Simulate data compression for archived tasks
            val completedTasks = taskRepository.getAllTasksSync()
                .filter { it.isCompleted }

            // Simulate compression processing
            delay(minOf(completedTasks.size * 5L, 3000L))

            completedTasks.size
        }
    }

    fun shutdown() {
        processingScope.cancel()
    }

    // Integration with data compression
    suspend fun compressStudyHistory(
        studyHistoryCompressor: StudySessionCompressor,
        sessions: List<CompressedStudySession>,
        archiveKey: String
    ): StudySessionCompressor.CompressionResult = withContext(Dispatchers.IO) {
        val startDate = sessions.minOfOrNull { it.date } ?: 0L
        val endDate = sessions.maxOfOrNull { it.date } ?: 0L

        studyHistoryCompressor.compressAndStore(archiveKey, sessions, startDate, endDate)
    }
}

// Memory-efficient data structures for study history
data class CompressedStudySession(
    val sessionId: String,
    val date: Long, // Timestamp
    val duration: Int, // Minutes
    val tasksCompleted: Int,
    val pointsEarned: Int,
    val bookProgress: Map<String, Int>, // Book -> pages read
    val flags: Int = 0 // Bit flags for boolean properties
) {
    companion object {
        // Bit flag constants
        const val FLAG_EARLY_MORNING = 1
        const val FLAG_WEEKEND = 2
        const val FLAG_STUDY_STREAK = 4
        const val FLAG_HIGH_PRODUCTIVITY = 8
    }

    fun isEarlyMorning(): Boolean = (flags and FLAG_EARLY_MORNING) != 0
    fun isWeekend(): Boolean = (flags and FLAG_WEEKEND) != 0
    fun isStudyStreak(): Boolean = (flags and FLAG_STUDY_STREAK) != 0
    fun isHighProductivity(): Boolean = (flags and FLAG_HIGH_PRODUCTIVITY) != 0
}

class StudyHistoryCompressor {
    private val compressedSessions = mutableListOf<CompressedStudySession>()
    private val sessionCache = mutableMapOf<String, CompressedStudySession>()

    fun compressSession(
        sessionId: String,
        date: Long,
        duration: Int,
        tasksCompleted: Int,
        pointsEarned: Int,
        bookProgress: Map<String, Int>,
        isEarlyMorning: Boolean = false,
        isWeekend: Boolean = false,
        isStudyStreak: Boolean = false,
        isHighProductivity: Boolean = false
    ): CompressedStudySession {
        var flags = 0
        if (isEarlyMorning) flags = flags or CompressedStudySession.FLAG_EARLY_MORNING
        if (isWeekend) flags = flags or CompressedStudySession.FLAG_WEEKEND
        if (isStudyStreak) flags = flags or CompressedStudySession.FLAG_STUDY_STREAK
        if (isHighProductivity) flags = flags or CompressedStudySession.FLAG_HIGH_PRODUCTIVITY

        val compressed = CompressedStudySession(
            sessionId = sessionId,
            date = date,
            duration = duration,
            tasksCompleted = tasksCompleted,
            pointsEarned = pointsEarned,
            bookProgress = bookProgress,
            flags = flags
        )

        compressedSessions.add(compressed)
        sessionCache[sessionId] = compressed

        return compressed
    }

    fun getSession(sessionId: String): CompressedStudySession? {
        return sessionCache[sessionId]
    }

    fun getSessionsInDateRange(startDate: Long, endDate: Long): List<CompressedStudySession> {
        return compressedSessions.filter { it.date in startDate..endDate }
    }

    fun getTotalStats(): StudyStats {
        if (compressedSessions.isEmpty()) return StudyStats()

        val totalDuration = compressedSessions.sumOf { it.duration }
        val totalTasks = compressedSessions.sumOf { it.tasksCompleted }
        val totalPoints = compressedSessions.sumOf { it.pointsEarned }
        val earlyMorningSessions = compressedSessions.count { it.isEarlyMorning() }
        val weekendSessions = compressedSessions.count { it.isWeekend() }
        val streakSessions = compressedSessions.count { it.isStudyStreak() }

        return StudyStats(
            totalSessions = compressedSessions.size,
            totalDuration = totalDuration,
            totalTasksCompleted = totalTasks,
            totalPointsEarned = totalPoints,
            earlyMorningSessions = earlyMorningSessions,
            weekendSessions = weekendSessions,
            streakSessions = streakSessions,
            averageSessionDuration = if (compressedSessions.isNotEmpty()) totalDuration / compressedSessions.size else 0,
            averageTasksPerSession = if (compressedSessions.isNotEmpty()) totalTasks / compressedSessions.size else 0
        )
    }

    fun clearOldSessions(olderThanDays: Int) {
        val cutoffTime = System.currentTimeMillis() - (olderThanDays * 24 * 60 * 60 * 1000L)
        compressedSessions.removeAll { it.date < cutoffTime }
        sessionCache.clear()
        // Rebuild cache with remaining sessions
        compressedSessions.forEach { sessionCache[it.sessionId] = it }
    }

    fun getMemoryUsage(): Int {
        // Rough estimation of memory usage in bytes
        return compressedSessions.size * 100 // ~100 bytes per compressed session
    }
}

data class StudyStats(
    val totalSessions: Int = 0,
    val totalDuration: Int = 0, // minutes
    val totalTasksCompleted: Int = 0,
    val totalPointsEarned: Int = 0,
    val earlyMorningSessions: Int = 0,
    val weekendSessions: Int = 0,
    val streakSessions: Int = 0,
    val averageSessionDuration: Int = 0,
    val averageTasksPerSession: Int = 0
)

// Data compression for archived study sessions
class StudySessionCompressor {
    private val compressedData = mutableMapOf<String, ByteArray>()
    private val metadata = mutableMapOf<String, CompressionMetadata>()

    data class CompressionMetadata(
        val originalSize: Int,
        val compressedSize: Int,
        val compressionRatio: Float,
        val compressionDate: Long,
        val dataRange: LongRange
    )

    fun compressAndStore(
        key: String,
        sessions: List<CompressedStudySession>,
        startDate: Long,
        endDate: Long
    ): CompressionResult {
        val originalData = sessions.toJsonString()
        val compressedData = compressData(originalData)

        val metadata = CompressionMetadata(
            originalSize = originalData.length,
            compressedSize = compressedData.size,
            compressionRatio = compressedData.size.toFloat() / originalData.length,
            compressionDate = System.currentTimeMillis(),
            dataRange = startDate..endDate
        )

        this.compressedData[key] = compressedData
        this.metadata[key] = metadata

        return CompressionResult(
            key = key,
            originalSize = originalData.length,
            compressedSize = compressedData.size,
            compressionRatio = metadata.compressionRatio,
            success = true
        )
    }

    fun decompress(key: String): List<CompressedStudySession>? {
        val compressed = compressedData[key] ?: return null
        val decompressed = decompressData(compressed)
        return decompressed.fromJsonString()
    }

    fun getCompressionStats(): CompressionStats {
        val totalOriginal = metadata.values.sumOf { it.originalSize }
        val totalCompressed = metadata.values.sumOf { it.compressedSize }
        val averageRatio = if (metadata.isNotEmpty()) {
            metadata.values.map { it.compressionRatio }.average().toFloat()
        } else 0f

        return CompressionStats(
            totalArchives = metadata.size,
            totalOriginalSize = totalOriginal,
            totalCompressedSize = totalCompressed,
            averageCompressionRatio = averageRatio,
            spaceSaved = totalOriginal - totalCompressed
        )
    }

    fun cleanupOldArchives(olderThanDays: Int) {
        val cutoffTime = System.currentTimeMillis() - (olderThanDays * 24 * 60 * 60 * 1000L)
        val keysToRemove = metadata.filter { it.value.compressionDate < cutoffTime }.keys
        keysToRemove.forEach { key ->
            compressedData.remove(key)
            metadata.remove(key)
        }
    }

    // Simple compression simulation (in real implementation, use GZIP or similar)
    private fun compressData(data: String): ByteArray {
        // Simulate compression by removing whitespace and using shorter representations
        val compressed = data
            .replace("\\s+".toRegex(), " ")
            .replace("{\"", "{")
            .replace("\":", ":")
            .replace(",\"", ",")
        return compressed.toByteArray(Charsets.UTF_8)
    }

    private fun decompressData(data: ByteArray): String {
        return String(data, Charsets.UTF_8)
    }

    // JSON serialization helpers (simplified)
    private fun List<CompressedStudySession>.toJsonString(): String {
        return this.joinToString(",", "[", "]") { session ->
            "{\"id\":\"${session.sessionId}\",\"date\":${session.date},\"duration\":${session.duration},\"tasks\":${session.tasksCompleted},\"points\":${session.pointsEarned},\"flags\":${session.flags}}"
        }
    }

    private fun String.fromJsonString(): List<CompressedStudySession> {
        if (this.isEmpty() || this == "[]") return emptyList()
        val jsonArray = this.removeSurrounding("[", "]")
        if (jsonArray.isEmpty()) return emptyList()

        return jsonArray.split("},{").map { item ->
            val cleanItem = item.removeSurrounding("{", "}")
            val fields = cleanItem.split(",").associate {
                val (key, value) = it.split(":", limit = 2)
                key.removeSurrounding("\"") to value
            }

            CompressedStudySession(
                sessionId = fields["id"] ?: "",
                date = fields["date"]?.toLong() ?: 0L,
                duration = fields["duration"]?.toInt() ?: 0,
                tasksCompleted = fields["tasks"]?.toInt() ?: 0,
                pointsEarned = fields["points"]?.toInt() ?: 0,
                bookProgress = emptyMap(),
                flags = fields["flags"]?.toInt() ?: 0
            )
        }
    }

    data class CompressionResult(
        val key: String,
        val originalSize: Int,
        val compressedSize: Int,
        val compressionRatio: Float,
        val success: Boolean
    )

    data class CompressionStats(
        val totalArchives: Int,
        val totalOriginalSize: Int,
        val totalCompressedSize: Int,
        val averageCompressionRatio: Float,
        val spaceSaved: Int
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkingTasksScreen(
    appIntegrationManager: AppIntegrationManager,
    studyProgressRepository: StudyProgressRepository,
    taskRepository: TaskRepository,
    sharedViewModel: SharedAppViewModel,
    onNavigateToStudyPlan: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val screenState = rememberWorkingTasksScreenState(
        studyProgressRepository = studyProgressRepository,
        taskRepository = taskRepository,
        onNavigateToStudyPlan = onNavigateToStudyPlan
    )

    WorkingTasksScreenContent(
        screenState = screenState,
        appIntegrationManager = appIntegrationManager,
        studyProgressRepository = studyProgressRepository,
        taskRepository = taskRepository,
        sharedViewModel = sharedViewModel,
        modifier = modifier
    )
}

@Composable
private fun rememberWorkingTasksScreenState(
    studyProgressRepository: StudyProgressRepository,
    taskRepository: TaskRepository,
    onNavigateToStudyPlan: () -> Unit
): WorkingTasksScreenState {
    val currentWeek by studyProgressRepository.currentWeek.collectAsState(initial = 1)
    val plan = remember { PlanDataSource.planData }

    val selectedTabState = remember { mutableStateOf(2) } // 0: Daily, 1: Weekly, 2: Plan
    val selectedDayState = remember { mutableStateOf<DayPlan?>(null) }

    // Get the week that corresponds to user's current progress
    val thisWeek = remember(currentWeek) {
        plan.getOrNull(currentWeek - 1) ?: plan.firstOrNull()
    }
    val weeklyIds = remember(thisWeek) {
        thisWeek?.days?.flatMap { it.tasks }?.map { it.id }?.toSet() ?: emptySet()
    }
    // Count actual completed tasks instead of hardcoded 0
    val allTasks by taskRepository.getAllTasks().collectAsState(initial = emptyList())
    val weeklyCompleted = remember(allTasks, weeklyIds) {
        allTasks.count { task -> 
            task.isCompleted && weeklyIds.contains(task.id) 
        }
    }
    val weeklyTotal = remember(weeklyIds) { weeklyIds.size.coerceAtLeast(1) }
    val weeklyProgressPct = remember(weeklyCompleted, weeklyTotal) {
        (weeklyCompleted.toFloat() / weeklyTotal)
    }
    val tasksById = remember(allTasks) {
        allTasks.associateBy { it.id }
    }
    val dayStatuses = remember(thisWeek, tasksById) {
        thisWeek?.days?.map { day ->
            val total = day.tasks.size
            val completed = day.tasks.count { planTask ->
                tasksById[planTask.id]?.isCompleted == true
            }
            val state = when {
                total == 0 -> DayCompletionState.EMPTY
                completed == 0 -> DayCompletionState.NOT_STARTED
                completed < total -> DayCompletionState.IN_PROGRESS
                else -> DayCompletionState.COMPLETED
            }
            DayScheduleStatus(
                dayName = day.day,
                completedTasks = completed,
                totalTasks = total,
                state = state
            )
        } ?: emptyList()
    }

    return WorkingTasksScreenState(
        currentWeek = currentWeek,
        selectedTab = selectedTabState.value,
        selectedDay = selectedDayState.value,
        thisWeek = thisWeek,
        weeklyProgressPct = weeklyProgressPct,
        dayStatuses = dayStatuses,
        onTabSelected = { selectedTabState.value = it },
        onDaySelected = { day ->
            selectedDayState.value = day
            selectedTabState.value = 0  // Switch to Daily tab
        },
        onNavigateToStudyPlan = onNavigateToStudyPlan
    )
}

data class WorkingTasksScreenState(
    val currentWeek: Int,
    val selectedTab: Int,
    val selectedDay: DayPlan?,
    val thisWeek: WeekPlan?,
    val weeklyProgressPct: Float,
    val dayStatuses: List<DayScheduleStatus>,
    val onTabSelected: (Int) -> Unit,
    val onDaySelected: (DayPlan) -> Unit,
    val onNavigateToStudyPlan: () -> Unit
)

data class DayScheduleStatus(
    val dayName: String,
    val completedTasks: Int,
    val totalTasks: Int,
    val state: DayCompletionState
)

enum class DayCompletionState {
    EMPTY,
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED
}

@Composable
private fun WorkingTasksScreenContent(
    screenState: WorkingTasksScreenState,
    appIntegrationManager: AppIntegrationManager,
    studyProgressRepository: StudyProgressRepository,
    taskRepository: TaskRepository,
    sharedViewModel: SharedAppViewModel,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = false
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(createBackgroundBrush(isDarkTheme))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(12.dp))
            TasksGradientTopBar(appIntegrationManager)
            Spacer(Modifier.height(8.dp))
            SegmentedControl(
                segments = listOf(
                    stringResource(R.string.tasks_tab_daily),
                    stringResource(R.string.tasks_tab_weekly),
                    stringResource(R.string.tasks_tab_plan)
                ),
                selectedIndex = screenState.selectedTab,
                onSelect = screenState.onTabSelected
            )
            Spacer(Modifier.height(8.dp))

            when (screenState.selectedTab) {
                0 -> DailyTab(
                    selectedDay = screenState.selectedDay,
                    currentWeek = screenState.currentWeek,
                    taskRepository = taskRepository,
                    sharedViewModel = sharedViewModel,
                    onBackToPlan = { screenState.onTabSelected(2) }
                )
                1 -> WeeklyTab(
                    thisWeek = screenState.thisWeek,
                    currentWeek = screenState.currentWeek,
                    studyProgressRepository = studyProgressRepository,
                    onNavigateToPlan = { screenState.onTabSelected(2) },
                    onNavigateToStudyPlan = screenState.onNavigateToStudyPlan
                )
                2 -> PlanTab(
                    thisWeek = screenState.thisWeek,
                    weeklyProgressPct = screenState.weeklyProgressPct,
                    dayStatuses = screenState.dayStatuses,
                    onDayClick = screenState.onDaySelected,
                    onNavigateToStudyPlan = screenState.onNavigateToStudyPlan
                )
            }
        }
    }
}

@Composable
private fun createBackgroundBrush(isDarkTheme: Boolean): Brush {
    return if (isDarkTheme) {
        // Seamless anthracite to light grey gradient for dark theme
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF2C2C2C), // Deep anthracite (top)
                Color(0xFF3A3A3A), // Medium anthracite
                Color(0xFF4A4A4A)  // Light anthracite (bottom)
            )
        )
    } else {
        // Keep original light theme gradient unchanged
        Brush.verticalGradient(colors = listOf(Color(0xFFEFF6FF), Color(0xFFF7FBFF)))
    }
}

@Composable
private fun SegmentedControl(
    segments: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val heights = responsiveHeights()
    touchTargetSize()
    // Rail
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        // Sliding indicator layout
        BoxWithConstraints(Modifier.padding(4.dp)) {
            val segmentCount = segments.size.coerceAtLeast(1)
            val segWidth: Dp = maxWidth / segmentCount
            val targetOffset = segWidth * selectedIndex
            val animatedOffset by animateDpAsState(
                targetValue = targetOffset,
                animationSpec = androidx.compose.animation.core.tween(250, easing = FastOutSlowInEasing),
                label = "seg_offset"
            )

            // Indicator behind text
            Surface(
                modifier = Modifier
                    .offset(x = animatedOffset)
                    .width(segWidth)
                    .height(heights.button)
                    .clip(RoundedCornerShape(20.dp)),
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {}

            // Hit targets and labels
            Row(
                modifier = Modifier.height(heights.button),
                verticalAlignment = Alignment.CenterVertically
            ) {
                segments.forEachIndexed { index, label ->
                    val selected = index == selectedIndex
                    val textColor by animateColorAsState(
                        targetValue = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        label = "seg_text_$index"
                    )
                    Box(
                        modifier = Modifier
                            .width(segWidth)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(20.dp))
                            .selectable(selected = selected, onClick = { onSelect(index) }, role = Role.Tab),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = textColor,
                            fontSize = 14.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TasksGradientTopBar(
    appIntegrationManager: AppIntegrationManager? = null
) {
    // Calculate real XP from completed tasks
    val allTasks by (appIntegrationManager?.getAllTasks()?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) })
    val completedTasks = allTasks.filter { it.isCompleted }
    val totalXP = completedTasks.size * 10 // 10 XP per completed task
    val isFirstTimeUser = allTasks.isEmpty()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Task,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.topbar_tasks_title),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isFirstTimeUser) {
                                stringResource(R.string.tasks_header_first_time_message)
                            } else {
                                stringResource(R.string.tasks_header_returning_message)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // XP Badge with pastel gradient
                Box(
                    modifier = Modifier
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                )
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Bolt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.tasks_xp_value, if (isFirstTimeUser) 0 else totalXP),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklyTab(
    thisWeek: WeekPlan?,
    currentWeek: Int,
    studyProgressRepository: StudyProgressRepository,
    onNavigateToPlan: () -> Unit = {},
    onNavigateToStudyPlan: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val cardShape = RoundedCornerShape(16.dp)

    // Calculate weekly stats from real plan data
    val totalTasks = remember(thisWeek) {
        thisWeek?.days?.sumOf { it.tasks.size } ?: 0
    }

    // For initial use: Show 0 completed tasks (user starts fresh)
    // This will be updated when task completion tracking is integrated
    val completedTasks = 0

    val weekProgress = remember(completedTasks, totalTasks) {
        if (totalTasks > 0) completedTasks.toFloat() / totalTasks else 0f
    }

    val currentDayOfWeek = remember { LocalDate.now().dayOfWeek.value }
    val daysCompleted = remember(currentDayOfWeek) {
        currentDayOfWeek.coerceIn(1, 7) // Monday=1, Sunday=7
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        // Week Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = cardShape,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                stringResource(R.string.tasks_week_title, currentWeek, 30),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                stringResource(R.string.tasks_week_day_progress, daysCompleted, 7),
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(
                                    "${(weekProgress * 100).toInt()}%",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Progress bar
                    Column {
                        LinearProgressIndicator(
                            progress = { weekProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.tasks_week_completed_summary, completedTasks, totalTasks),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Week Navigation
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = cardShape
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            if (currentWeek > 1) {
                                scope.launch {
                                    studyProgressRepository.setManualWeekOverride(currentWeek - 1)
                                }
                            }
                        },
                        enabled = currentWeek > 1,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.tasks_week_nav_previous_cd),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.tasks_week_nav_previous))
                    }

                    Text(
                        stringResource(R.string.tasks_week_nav_label, currentWeek),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    OutlinedButton(
                        onClick = {
                            if (currentWeek < 30) {
                                scope.launch {
                                    studyProgressRepository.setManualWeekOverride(currentWeek + 1)
                                }
                            }
                        },
                        enabled = currentWeek < 30,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.tasks_week_nav_next))
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = stringResource(R.string.tasks_week_nav_next_cd),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Daily Summary
        item {
            Text(
                stringResource(R.string.tasks_week_schedule_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        // Days of the week
        thisWeek?.days?.forEachIndexed { index, day ->
            item {
                WeeklyDayCard(
                    day = day,
                    dayNumber = index + 1,
                    isToday = index + 1 == currentDayOfWeek,
                    onClick = onNavigateToPlan
                )
            }
        }

        // Quick Actions
        item {
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.tasks_week_quick_actions_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = cardShape,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToPlan() }
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Filled.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.tasks_week_quick_action_plan),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = cardShape,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToStudyPlan() }
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Filled.School,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.tasks_week_quick_action_study),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }

        // Week Stats Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = cardShape,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.tasks_week_stats_title),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        WeekStatItem(
                            icon = Icons.Filled.CheckCircle,
                            label = stringResource(R.string.tasks_week_stat_completed),
                            value = "$completedTasks",
                            color = MaterialTheme.colorScheme.primary
                        )
                        WeekStatItem(
                            icon = Icons.Filled.Circle,
                            label = stringResource(R.string.tasks_week_stat_remaining),
                            value = "${totalTasks - completedTasks}",
                            color = MaterialTheme.colorScheme.secondary
                        )
                        WeekStatItem(
                            icon = Icons.AutoMirrored.Filled.MenuBook,
                            label = stringResource(R.string.tasks_week_stat_total),
                            value = "$totalTasks",
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklyDayCard(
    day: DayPlan,
    dayNumber: Int,
    isToday: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = if (isToday) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (isToday) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                modifier = Modifier.size(48.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        dayNumber.toString(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isToday) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        day.day,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (isToday) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                stringResource(R.string.tasks_week_today_badge),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.tasks_week_task_count, day.tasks.size),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = stringResource(R.string.tasks_week_view_cd),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WeekStatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PlanTab(
    thisWeek: WeekPlan?,
    weeklyProgressPct: Float,
    dayStatuses: List<DayScheduleStatus>,
    onDayClick: (DayPlan) -> Unit = {},
    onNavigateToStudyPlan: () -> Unit = {}
) {
    val cardShape = RoundedCornerShape(16.dp)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        // Study Plan Overview Card
        item {
            Card(
                shape = cardShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToStudyPlan() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.EditCalendar,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.tasks_plan_view_full),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.tasks_plan_view_full_subtitle),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = stringResource(R.string.tasks_plan_nav_cd),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // This Week's Study Plan
        item {
            Card(shape = cardShape, colors = CardDefaults.cardColors(containerColor = featurePastelContainer(FeatureKey.TASKS, "tasks_surface_block"))) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.tasks_plan_card_title), fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(stringResource(R.string.tasks_plan_focus), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.tasks_plan_progress_label), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${(weeklyProgressPct * 100).toInt()}%", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { weeklyProgressPct.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.tasks_plan_daily_schedule), fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    DayScheduleList(thisWeek, dayStatuses, onDayClick)
                }
            }
        }

        // Upcoming Days
        item {
            Card(shape = cardShape, colors = CardDefaults.cardColors(containerColor = featurePastelContainer(FeatureKey.TASKS, "tasks_secondary_block"))) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.tasks_week_upcoming_title), fontWeight = FontWeight.SemiBold)
                        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                            Text(stringResource(R.string.tasks_week_upcoming_badge), modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp), fontSize = 11.sp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.tasks_week_upcoming_placeholder),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            }
        }

    }
}

@Composable
private fun DayScheduleList(
    week: WeekPlan?,
    dayStatuses: List<DayScheduleStatus>,
    onDayClick: (DayPlan) -> Unit = {}
) {
    if (week == null) {
        Text(stringResource(R.string.tasks_plan_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val fmt = DateTimeFormatter.ofPattern("EEE, MMM d").withLocale(Locale.getDefault())
    val monday = LocalDate.now().with(java.time.DayOfWeek.MONDAY)
    week.days.forEachIndexed { idx, day ->
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            tonalElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .clickable { onDayClick(day) }
        ) {
            Column(Modifier.padding(12.dp)) {
                val dayDate = monday.plusDays(idx.toLong())
                val isToday = LocalDate.now() == dayDate
                val statusInfo = dayStatuses.getOrNull(idx)
                val statusState = statusInfo?.state ?: if (day.tasks.isEmpty()) {
                    DayCompletionState.EMPTY
                } else {
                    DayCompletionState.NOT_STARTED
                }
                val baseStatusLabel = when (statusState) {
                    DayCompletionState.COMPLETED -> stringResource(R.string.tasks_plan_status_completed)
                    DayCompletionState.IN_PROGRESS -> stringResource(R.string.tasks_plan_status_in_progress)
                    DayCompletionState.NOT_STARTED -> stringResource(R.string.tasks_plan_status_not_started)
                    DayCompletionState.EMPTY -> stringResource(R.string.tasks_plan_status_scheduled)
                }
                val statusLabel = if (isToday) {
                    stringResource(R.string.tasks_plan_status_today, baseStatusLabel)
                } else {
                    baseStatusLabel
                }
                val (statusContainerColor, statusContentColor) = when (statusState) {
                    DayCompletionState.COMPLETED -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
                    DayCompletionState.IN_PROGRESS -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
                    DayCompletionState.NOT_STARTED -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
                    DayCompletionState.EMPTY -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    val dateLabel = dayDate.format(fmt)
                    val dayLabel = if (isToday) {
                        stringResource(R.string.tasks_plan_day_label_today, day.day, dateLabel)
                    } else {
                        stringResource(R.string.tasks_plan_day_label, day.day, dateLabel)
                    }
                    Text(dayLabel, fontWeight = FontWeight.SemiBold)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = statusContainerColor
                    ) {
                        Text(
                            statusLabel,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                            fontSize = 11.sp,
                            color = statusContentColor
                        )
                    }
                }
                statusInfo?.takeIf { it.totalTasks > 0 }?.let { info ->
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.tasks_plan_task_summary, info.completedTasks, info.totalTasks),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.tasks_plan_day_placeholder),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }
    }
}

// Daily Tab Implementation
@Composable
private fun DailyTab(
    selectedDay: DayPlan?,
    currentWeek: Int = 1,
    taskRepository: TaskRepository,
    sharedViewModel: SharedAppViewModel,
    onBackToPlan: () -> Unit
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(selectedDay) {
        if (selectedDay != null) {
            isLoading = true
            kotlinx.coroutines.delay(800) // Simulate loading
            isLoading = false
        }
    }

    if (selectedDay == null) {
        // No day selected - show placeholder
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.tasks_daily_placeholder_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.tasks_daily_placeholder_body),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
                OutlinedButton(
                    onClick = onBackToPlan,
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text(stringResource(R.string.tasks_daily_placeholder_button))
                }
            }
        }
        return
    }

    if (isLoading) {
        // Loading state
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator()
                Text(
                    text = stringResource(R.string.tasks_daily_loading_message, selectedDay.day),
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    // Create study info based on selected day
    val studyInfo = createDailyStudyInfo(selectedDay, currentWeek, context)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        // Header
        item {
            DailyStudyHeader(studyInfo, onBackToPlan)
        }

        // Study Book
        item {
            StudyBookCard(
                book = studyInfo.book,
                units = studyInfo.units,
                taskRepository = taskRepository,
                sharedViewModel = sharedViewModel
            )
        }

        // Daily Tasks
        item {
            DailyTasksSection(studyInfo.tasks)
        }

        // Study Materials
        item {
            StudyMaterialsSection(studyInfo.materials)
        }

        // Notes
        if (studyInfo.notes.isNotEmpty()) {
            item {
                NotesSection(studyInfo.notes)
            }
        }
    }
}

// Helper data classes and functions
data class DailyStudyInfo(
    val weekTitle: String,
    val dayName: String,
    val date: String,
    val book: StudyBook,
    val units: List<StudyUnit>,
    val tasks: List<DailyTask>,
    val materials: List<StudyMaterial>,
    val estimatedTime: String,
    val completionPercentage: Int,
    val notes: String = ""
)

data class StudyBook(
    val name: String,
    val color: Color,
    val description: String,
    val murphyBook: MurphyBook? = null
) {
    companion object {
        val RED_BOOK = StudyBook("Red Book - Essential Grammar in Use", Color(0xFFE53935), "Foundation Level Grammar", CompleteMurphyBookData.RED_BOOK)
        val BLUE_BOOK = StudyBook("Blue Book - English Grammar in Use", Color(0xFF1976D2), "Intermediate Level Grammar", CompleteMurphyBookData.BLUE_BOOK)
        val GREEN_BOOK = StudyBook("Green Book - Advanced Grammar in Use", Color(0xFF388E3C), "Advanced Level Grammar", CompleteMurphyBookData.GREEN_BOOK)
    }
}

data class StudyUnit(
    val title: String,
    val unitNumber: Int,
    val pages: String,
    val exercises: List<String>,
    val isCompleted: Boolean = false,
    val estimatedMinutes: Int = 30,
    val vocabulary: List<String> = emptyList(),
    val grammarTopic: String = "",
    val murphyUnit: MurphyUnit? = null
)

data class DailyTask(
    val title: String,
    val description: String,
    val estimatedDuration: String,
    val priority: Priority,
    val isCompleted: Boolean = false
)

data class StudyMaterial(
    val title: String,
    val type: MaterialType,
    val url: String = "",
    val description: String = ""
)

enum class MaterialType {
    VIDEO, AUDIO, PDF, EXERCISE, QUIZ, READING
}

enum class Priority { HIGH, MEDIUM, LOW }

@Composable
private fun DailyStudyHeader(studyInfo: DailyStudyInfo, onBackToPlan: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = studyInfo.book.color.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = studyInfo.dayName,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = studyInfo.book.color
                    )
                    Text(
                        text = studyInfo.date,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = studyInfo.weekTitle,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedButton(onClick = onBackToPlan) {
                    Text(stringResource(R.string.tasks_daily_back_to_plan))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.tasks_daily_estimated_time, studyInfo.estimatedTime),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    color = when {
                        studyInfo.completionPercentage >= 100 -> MaterialTheme.colorScheme.primary
                        studyInfo.completionPercentage > 0 -> Color(0xFFFF9800)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "${studyInfo.completionPercentage}%",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun StudyBookCard(
    book: StudyBook,
    units: List<StudyUnit>,
    taskRepository: TaskRepository,
    sharedViewModel: SharedAppViewModel
) {
    var selectedUnit by remember { mutableStateOf<StudyUnit?>(null) }
    var isCreatingTask by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Track completed Murphy tasks
    val completedTasks by taskRepository.getAllTasks().collectAsState(initial = emptyList())
    val murphyTaskIds = completedTasks
        .filter { it.category == "Murphy Grammar" && it.isCompleted }
        .map { it.id }
        .toSet()

    Card(
        colors = CardDefaults.cardColors(containerColor = featurePastelContainer(FeatureKey.TASKS, "tasks_row_card_1")),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = book.color,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = book.name.first().toString(),
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = book.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = book.description,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (units.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.tasks_daily_units_title),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Create stable callback using remember
                val onUnitClick = remember<(StudyUnit) -> Unit> { { unit ->
                    selectedUnit = unit
                    val taskId = generateMurphyTaskId(book, unit)
                    val isCompleted = murphyTaskIds.contains(taskId)
                    if (!isCompleted && !isCreatingTask) {
                        isCreatingTask = true
                    }
                }}

                units.forEach { unit ->
                    val taskId = generateMurphyTaskId(book, unit)
                    val isCompleted = murphyTaskIds.contains(taskId)

                    StudyUnitItem(
                        unit = unit.copy(isCompleted = isCompleted),
                        onClick = { onUnitClick(unit) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

    // Handle task creation and completion
    LaunchedEffect(selectedUnit, isCreatingTask) {
        if (selectedUnit != null && isCreatingTask) {
            val unit = selectedUnit!!
            val taskId = generateMurphyTaskId(book, unit)

            // Check if task already exists
            val existingTask = taskRepository.getTaskById(taskId)

            if (existingTask == null) {
                // Create new Murphy task
                val murphyTask = createMurphyTask(book, unit)
                taskRepository.insertTask(murphyTask)

                // Complete the task immediately (Murphy units are practice-based)
                val completedTask = murphyTask.copy(
                    isCompleted = true,
                    completedAt = System.currentTimeMillis(),
                    actualMinutes = unit.estimatedMinutes
                )
                taskRepository.updateTask(completedTask)

                // Trigger gamification and progress tracking
                sharedViewModel.completeTask(taskId)

                // Show completion feedback
                Toast.makeText(
                    context,
                    "Completed: ${unit.title}",
                    Toast.LENGTH_SHORT
                ).show()
            }

            isCreatingTask = false
            selectedUnit = null
        }
    }
}

@Composable
private fun StudyUnitItem(unit: StudyUnit, onClick: () -> Unit = {}) {
    Surface(
        color = if (unit.isCompleted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (unit.isCompleted) Icons.Filled.CheckCircle else Icons.Filled.Circle,
                contentDescription = null,
                tint = if (unit.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.tasks_daily_unit_title, unit.unitNumber, unit.title),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = stringResource(R.string.tasks_daily_unit_pages, unit.pages),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (unit.exercises.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.tasks_daily_unit_exercises, unit.exercises.joinToString(", ")),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyTasksSection(tasks: List<DailyTask>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = featurePastelContainer(FeatureKey.TASKS, "tasks_row_card_2")),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.tasks_daily_tasks_title),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (tasks.isEmpty()) {
                Text(
                    text = stringResource(R.string.tasks_daily_no_tasks),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                // Use forEach for now since we're not in a LazyColumn context
                tasks.forEach { task ->
                    DailyTaskItem(task)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun DailyTaskItem(task: DailyTask) {
    Surface(
        color = if (task.isCompleted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = if (task.isCompleted) Icons.Filled.CheckCircle else Icons.Filled.Circle,
                contentDescription = null,
                tint = if (task.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = task.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.tasks_daily_duration, task.estimatedDuration),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                color = when (task.priority) {
                    Priority.HIGH -> Color(0xFFE53935)
                    Priority.MEDIUM -> Color(0xFFFF9800)
                    Priority.LOW -> MaterialTheme.colorScheme.primary
                },
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = task.priority.name,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun StudyMaterialsSection(materials: List<StudyMaterial>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = featurePastelContainer(FeatureKey.TASKS, "tasks_row_card_3")),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.tasks_daily_materials_title),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (materials.isEmpty()) {
                Text(
                    text = stringResource(R.string.tasks_daily_no_materials),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                materials.forEach { material ->
                    StudyMaterialItem(material)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun StudyMaterialItem(material: StudyMaterial) {
    val context = LocalContext.current
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.clickable {
            if (material.url.isNotEmpty()) {
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(material.url))
                context.startActivity(intent)
            }
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (material.type) {
                    MaterialType.VIDEO -> Icons.Filled.PlayArrow
                    MaterialType.AUDIO -> Icons.Filled.Star
                    MaterialType.PDF -> Icons.Filled.Star
                    MaterialType.EXERCISE -> Icons.Filled.Quiz
                    MaterialType.QUIZ -> Icons.Filled.Quiz
                    MaterialType.READING -> Icons.Filled.Star
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = material.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (material.description.isNotEmpty()) {
                    Text(
                        text = material.description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = stringResource(R.string.tasks_material_open_link_cd),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun NotesSection(notes: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = featurePastelContainer(FeatureKey.TASKS, "tasks_row_card_4")),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.EditCalendar,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.tasks_daily_notes_title),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = notes,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                lineHeight = 20.sp
            )
        }
    }
}

// Helper function to create DailyStudyInfo from DayPlan
private fun createDailyStudyInfo(dayPlan: DayPlan, currentWeek: Int = 1, context: Context): DailyStudyInfo {
    val dayIndex = PlanTaskLocalizer.dayIndex(dayPlan.day)

    val localizedDayName = PlanTaskLocalizer.localizeDayName(dayPlan.day, context)




    // Assign books based on the 30-week curriculum progression
    val book = when (currentWeek) {
        in 1..8 -> StudyBook.RED_BOOK      // Weeks 1-8: Red Book Foundation
        in 9..18 -> StudyBook.BLUE_BOOK    // Weeks 9-18: Blue Book Intermediate
        in 19..26 -> StudyBook.GREEN_BOOK  // Weeks 19-26: Green Book Advanced
        else -> StudyBook.RED_BOOK         // Weeks 27-30: Exam Camp (mixed/review - default to Red for now)
    }

        // Create neutral placeholders for initial plan generation
    val units = emptyList<StudyUnit>()
    val tasks = emptyList<DailyTask>()
    val materials = emptyList<StudyMaterial>()
    val estimatedTimeValue = context.getString(R.string.tasks_daily_estimated_time_default)
    val notes = ""

    return DailyStudyInfo(
        weekTitle = context.getString(R.string.tasks_week_nav_label, currentWeek),
        dayName = localizedDayName,
        date = LocalDate.now().plusDays(dayIndex.toLong()).format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())),
        book = book,
        units = units,
        tasks = tasks,
        materials = materials,
        estimatedTime = estimatedTimeValue,
        completionPercentage = 0,
        notes = notes
    )
}

// Paginated Task List Component for Large Datasets
@Composable
fun PaginatedTaskList(
    taskRepository: TaskRepository,
    modifier: Modifier = Modifier,
    pageSize: Int = 20,
    showCompletedTasks: Boolean = true
) {
    var taskListState by remember { mutableStateOf(TaskListState(pageSize = pageSize)) }
    var currentTasks by remember { mutableStateOf<List<Task>>(emptyList()) }
    val scope = rememberCoroutineScope()

    // Load initial page
    LaunchedEffect(taskListState.currentPage, showCompletedTasks) {
        taskListState = taskListState.copy(isLoading = true)
        try {
            val result = if (showCompletedTasks) {
                taskRepository.getCompletedTasksPaginated(taskListState.currentPage, pageSize)
            } else {
                taskRepository.getPendingTasksPaginated(taskListState.currentPage, pageSize)
            }

            currentTasks = result.tasks
            taskListState = taskListState.copy(
                isLoading = false,
                hasMorePages = result.hasNextPage,
                totalCount = result.totalCount
            )
        } catch (e: Exception) {
            taskListState = taskListState.copy(isLoading = false)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Header with pagination info
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${if (showCompletedTasks) "Completed" else "Pending"} Tasks (${taskListState.totalCount})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            if (taskListState.totalCount > pageSize) {
                Text(
                    text = "Page ${taskListState.currentPage + 1}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Task list
        if (taskListState.isLoading && currentTasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (currentTasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No ${if (showCompletedTasks) "completed" else "pending"} tasks found",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(currentTasks) { task ->
                    TaskListItem(task = task)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Loading indicator for next page
                if (taskListState.isLoading && currentTasks.isNotEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }

        // Pagination controls
        if (taskListState.totalCount > pageSize) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        if (taskListState.currentPage > 0) {
                            taskListState = taskListState.copy(currentPage = taskListState.currentPage - 1)
                        }
                    },
                    enabled = taskListState.currentPage > 0 && !taskListState.isLoading,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(stringResource(R.string.tasks_week_nav_previous))
                }

                Text(
                    text = "${taskListState.currentPage + 1} / ${maxOf(1, (taskListState.totalCount + pageSize - 1) / pageSize)}",
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedButton(
                    onClick = {
                        if (taskListState.hasMorePages) {
                            taskListState = taskListState.copy(currentPage = taskListState.currentPage + 1)
                        }
                    },
                    enabled = taskListState.hasMorePages && !taskListState.isLoading,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(stringResource(R.string.tasks_week_nav_next))
                }
            }
        }
    }
}

@Composable
private fun TaskListItem(task: Task) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Completion indicator
            Surface(
                shape = CircleShape,
                color = if (task.isCompleted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                modifier = Modifier.size(24.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (task.isCompleted) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Task content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (task.description.isNotEmpty()) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Task metadata
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = task.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (task.estimatedMinutes > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${task.estimatedMinutes}min",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (task.pointsValue > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${task.pointsValue}pts",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Murphy Task Integration Helper Functions
 * These functions handle the conversion between Murphy units and PlanTasks
 */

/**
 * Generate a unique task ID for a Murphy unit
 */
private fun generateMurphyTaskId(book: StudyBook, unit: StudyUnit): String {
    return "murphy_${book.name.replace(" ", "_").lowercase()}_unit_${unit.unitNumber}"
}

/**
 * Create a Task from a Murphy unit
 */
private fun createMurphyTask(book: StudyBook, unit: StudyUnit): Task {
    return Task(
        id = generateMurphyTaskId(book, unit),
        title = "${book.name} - Unit ${unit.unitNumber}: ${unit.title}",
        description = buildMurphyTaskDescription(book, unit),
        category = "Murphy Grammar",
        priority = TaskPriority.MEDIUM,
        dueDate = System.currentTimeMillis() + (24 * 60 * 60 * 1000), // Due tomorrow
        estimatedMinutes = unit.estimatedMinutes,
        tags = listOf("murphy", "grammar", book.name.lowercase(), "unit${unit.unitNumber}"),
        pointsValue = calculateMurphyTaskPoints(unit)
    )
}

/**
 * Build detailed description for Murphy task
 */
private fun buildMurphyTaskDescription(book: StudyBook, unit: StudyUnit): String {
    val description = StringBuilder()
    description.append("📖 ${book.name}\n")
    description.append("📄 Pages: ${unit.pages}\n")
    description.append("⏱️ Estimated time: ${unit.estimatedMinutes} minutes\n\n")

    if (unit.exercises.isNotEmpty()) {
        description.append("📝 Exercises:\n")
        unit.exercises.forEach { exercise ->
            description.append("• $exercise\n")
        }
        description.append("\n")
    }

    if (unit.vocabulary.isNotEmpty()) {
        description.append("📚 Key Vocabulary:\n")
        unit.vocabulary.take(5).forEach { vocab ->
            description.append("• $vocab\n")
        }
        if (unit.vocabulary.size > 5) {
            description.append("• ... and ${unit.vocabulary.size - 5} more words\n")
        }
        description.append("\n")
    }

    description.append("🎯 Grammar Focus: ${unit.grammarTopic}\n")
    description.append("📝 Practice completing the exercises and reviewing key concepts.")

    return description.toString()
}

/**
 * Calculate points value for Murphy task based on difficulty and content
 */
private fun calculateMurphyTaskPoints(unit: StudyUnit): Int {
    var points = 15 // Base points for grammar tasks

    // Add points for exercises
    points += unit.exercises.size * 2

    // Add points for vocabulary
    points += (unit.vocabulary.size / 5) * 3

    // Bonus for longer estimated time
    if (unit.estimatedMinutes > 30) points += 5
    if (unit.estimatedMinutes > 45) points += 5

    return points.coerceAtMost(35) // Cap at 35 points
}

/**
 * Parse estimated time string to minutes
 */
private fun parseEstimatedMinutes(estimatedTime: String): Int {
    return when {
        estimatedTime.contains("min", ignoreCase = true) -> {
            estimatedTime.filter { it.isDigit() }.toIntOrNull() ?: 30
        }
        estimatedTime.contains("hour", ignoreCase = true) -> {
            val hours = estimatedTime.filter { it.isDigit() }.toIntOrNull() ?: 1
            hours * 60
        }
        else -> 30 // Default 30 minutes
    }
}



