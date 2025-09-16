package com.mtlc.studyplan.reading

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mtlc.studyplan.data.ProgressRepository
import com.mtlc.studyplan.data.dataStore
import com.mtlc.studyplan.questions.VocabularyManager
import com.mtlc.studyplan.questions.QuestionGenerator
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Main Reading Screen - Entry point for reading activities
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingScreen(
    onNavigateToSession: (String) -> Unit = {},
    onNavigateToProgress: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: ReadingScreenViewModel = viewModel {
        ReadingScreenViewModel(
            context = context,
            progressRepository = ProgressRepository(context.dataStore)
        )
    }
    
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadReadingData()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Reading Practice",
                        fontWeight = FontWeight.SemiBold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToProgress) {
                        Icon(Icons.Default.Analytics, contentDescription = "Progress")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ReadingOverviewCard(uiState.overview)
            }
            
            item {
                QuickActionButtons(
                    onStartQuickRead = { viewModel.startQuickReading() },
                    onStartVocabFocus = { viewModel.startVocabularyFocusedReading() },
                    onStartComprehension = { viewModel.startComprehensionReading() }
                )
            }
            
            item {
                Text(
                    "Recommended for You",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            items(uiState.recommendations) { recommendation ->
                ReadingRecommendationCard(
                    recommendation = recommendation,
                    onStartReading = { onNavigateToSession(recommendation.content.id) }
                )
            }
        }
    }
}

@Composable
fun ReadingOverviewCard(overview: ReadingOverview) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Reading Progress",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "Week ${overview.currentWeek} • Level ${overview.currentLevel.displayName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
                
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .background(
                            Brush.sweepGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${(overview.weeklyProgress * 100).toInt()}%",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "Articles Read",
                    value = overview.articlesReadThisWeek.toString(),
                    icon = Icons.AutoMirrored.Filled.Article
                )
                StatItem(
                    label = "Avg WPM",
                    value = overview.averageWPM.toString(),
                    icon = Icons.Default.Speed
                )
                StatItem(
                    label = "Comprehension",
                    value = "${(overview.averageComprehension * 100).toInt()}%",
                    icon = Icons.Default.Psychology
                )
            }
        }
    }
}

@Composable
fun StatItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun QuickActionButtons(
    onStartQuickRead: () -> Unit,
    onStartVocabFocus: () -> Unit,
    onStartComprehension: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ActionButton(
            text = "Quick Read",
            icon = Icons.Default.FastForward,
            onClick = onStartQuickRead,
            modifier = Modifier.weight(1f)
        )
        ActionButton(
            text = "Vocabulary",
            icon = Icons.AutoMirrored.Filled.MenuBook,
            onClick = onStartVocabFocus,
            modifier = Modifier.weight(1f)
        )
        ActionButton(
            text = "Comprehension",
            icon = Icons.Default.Quiz,
            onClick = onStartComprehension,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun ActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedButton(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = text,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingRecommendationCard(
    recommendation: ReadingRecommendation,
    onStartReading: () -> Unit
) {
    Card(
        onClick = onStartReading,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        recommendation.content.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        recommendation.reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                
                PriorityBadge(recommendation.priority)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                InfoChip(
                    label = recommendation.content.difficulty.displayName,
                    color = MaterialTheme.colorScheme.primary
                )
                InfoChip(
                    label = "${recommendation.content.estimatedTime}min",
                    color = MaterialTheme.colorScheme.secondary
                )
                InfoChip(
                    label = recommendation.content.topics.first(),
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
fun PriorityBadge(priority: RecommendationPriority) {
    val (color, text) = when (priority) {
        RecommendationPriority.HIGH -> MaterialTheme.colorScheme.error to "High Priority"
        RecommendationPriority.MEDIUM -> MaterialTheme.colorScheme.primary to "Recommended"
        RecommendationPriority.LOW -> MaterialTheme.colorScheme.outline to "Optional"
    }
    
    Surface(
        color = color.copy(alpha = 0.1f),
        contentColor = color,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun InfoChip(
    label: String,
    color: Color
) {
    Surface(
        color = color.copy(alpha = 0.1f),
        contentColor = color,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium
        )
    }
}

// ViewModel
class ReadingScreenViewModel(
    private val context: android.content.Context,
    private val progressRepository: ProgressRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ReadingScreenUiState())
    val uiState: StateFlow<ReadingScreenUiState> = _uiState.asStateFlow()
    
    private var readingIntegration: ReadingSystemIntegration? = null
    
    fun loadReadingData() {
        viewModelScope.launch {
            try {
                // Initialize integration
                val vocabularyManager = com.mtlc.studyplan.questions.VocabularyManager(context, progressRepository)
                val analyticsEngine = com.mtlc.studyplan.analytics.AnalyticsEngine()
                val dataProvider = com.mtlc.studyplan.questions.DefaultQuestionDataProvider(progressRepository, vocabularyManager)
                val performanceTracker = com.mtlc.studyplan.questions.RoomQuestionPerformanceTracker(context)
                val questionGenerator = QuestionGenerator(context, analyticsEngine, vocabularyManager, dataProvider, performanceTracker)
                readingIntegration = ReadingSystemIntegration(
                    context, progressRepository, vocabularyManager, questionGenerator
                )
                
                // Load overview data
                val userProgress = progressRepository.userProgressFlow.first()
                val taskLogs = progressRepository.taskLogsFlow.first()
                
                val currentWeek = calculateCurrentWeek(userProgress.completedTasks.size)
                val readingLogs = taskLogs.filter { it.category.contains("reading", ignoreCase = true) }
                
                val overview = ReadingOverview(
                    currentWeek = currentWeek,
                    currentLevel = ReadingLevel.fromWeek(currentWeek),
                    weeklyProgress = calculateWeeklyProgress(readingLogs),
                    articlesReadThisWeek = countArticlesThisWeek(readingLogs),
                    averageWPM = calculateAverageWPM(readingLogs),
                    averageComprehension = calculateAverageComprehension(readingLogs)
                )
                
                // Load recommendations
                val curator = ContentCuratorFactory.create(context, progressRepository, vocabularyManager)
                val recommendations = generateRecommendations(curator)
                
                _uiState.update {
                    it.copy(
                        overview = overview,
                        recommendations = recommendations,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = e.message ?: "Failed to load reading data",
                        isLoading = false
                    )
                }
            }
        }
    }
    
    fun startQuickReading() {
        // Implementation for quick reading session
    }
    
    fun startVocabularyFocusedReading() {
        // Implementation for vocabulary-focused reading
    }
    
    fun startComprehensionReading() {
        // Implementation for comprehension-focused reading
    }
    
    private fun calculateCurrentWeek(completedTasks: Int): Int {
        return kotlin.math.min(kotlin.math.max(completedTasks / 10, 1), 30)
    }
    
    private fun calculateWeeklyProgress(readingLogs: List<com.mtlc.studyplan.data.TaskLog>): Float {
        val thisWeekLogs = readingLogs.filter { log ->
            val daysSinceLog = (System.currentTimeMillis() - log.timestampMillis) / (24 * 60 * 60 * 1000)
            daysSinceLog <= 7
        }
        return kotlin.math.min(thisWeekLogs.size.toFloat() / 5f, 1f) // Target: 5 readings per week
    }
    
    private fun countArticlesThisWeek(readingLogs: List<com.mtlc.studyplan.data.TaskLog>): Int {
        return readingLogs.count { log ->
            val daysSinceLog = (System.currentTimeMillis() - log.timestampMillis) / (24 * 60 * 60 * 1000)
            daysSinceLog <= 7
        }
    }
    
    private fun calculateAverageWPM(readingLogs: List<com.mtlc.studyplan.data.TaskLog>): Int {
        if (readingLogs.isEmpty()) return 200
        
        // Simplified calculation based on time spent
        val averageTime = readingLogs.map { it.minutesSpent }.average()
        return when {
            averageTime < 5 -> 250
            averageTime > 15 -> 180
            else -> 220
        }
    }
    
    private fun calculateAverageComprehension(readingLogs: List<com.mtlc.studyplan.data.TaskLog>): Float {
        return if (readingLogs.isNotEmpty()) {
            readingLogs.count { it.correct }.toFloat() / readingLogs.size
        } else 0.75f
    }
    
    private suspend fun generateRecommendations(curator: ContentCurator): List<ReadingRecommendation> {
        val recommendations = mutableListOf<ReadingRecommendation>()
        
        // Get content for different time allocations
        listOf(5, 10, 15).forEach { timeSlot ->
            curator.recommendReading(timeSlot)?.let { content ->
                recommendations.add(
                    ReadingRecommendation(
                        content = content,
                        reason = "Perfect for a ${timeSlot}-minute session",
                        priority = when (timeSlot) {
                            10 -> RecommendationPriority.HIGH
                            15 -> RecommendationPriority.MEDIUM
                            else -> RecommendationPriority.LOW
                        }
                    )
                )
            }
        }
        
        return recommendations.take(6)
    }
}

// Data classes
data class ReadingScreenUiState(
    val overview: ReadingOverview = ReadingOverview(),
    val recommendations: List<ReadingRecommendation> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

data class ReadingOverview(
    val currentWeek: Int = 1,
    val currentLevel: ReadingLevel = ReadingLevel.A2,
    val weeklyProgress: Float = 0f,
    val articlesReadThisWeek: Int = 0,
    val averageWPM: Int = 200,
    val averageComprehension: Float = 0.75f
)