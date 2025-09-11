package com.mtlc.studyplan.feature.progress

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mtlc.studyplan.ui.theme.LocalSpacing
import com.mtlc.studyplan.ui.components.TwoPaneScaffold
import com.mtlc.studyplan.data.dataStore
import com.mtlc.studyplan.data.ProgressRepository
import com.mtlc.studyplan.data.UserProgress

@Composable
fun ProgressScreen() {
    val s = LocalSpacing.current
    var selected by remember { mutableStateOf(0) }
    val items = remember { listOf("Week 1", "Week 2", "Week 3", "Week 4", "Week 5", "Week 6", "Week 7") }
    val context = androidx.compose.ui.platform.LocalContext.current
    val ds = (context as android.content.Context).dataStore
    val repo = remember { ProgressRepository(ds) }
    val userProgress by repo.userProgressFlow.collectAsState(initial = UserProgress())
    TwoPaneScaffold(
        list = {
            Text("Progress", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(s.xs))
            LazyColumn(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(s.xs)
            ) {
                itemsIndexed(items) { idx, label ->
                    ListItem(
                        headlineContent = { Text(label) },
                        supportingContent = { Text("Summary metrics…") },
                        trailingContent = { if (idx == selected) AssistChip(onClick = {}, label = { Text("Selected") }) }
                    )
                    Divider()
                }
            }
        },
        detail = {
            Text("Details", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(s.xs))
            Text("Selected: ${items.getOrNull(selected) ?: "None"}", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(s.sm))
            Text("Completed tasks: ${userProgress.completedTasks.size}")
            Text("Streak: ${userProgress.streakCount}")
            LinearProgressIndicator(progress = { (selected + 1) / (items.size.toFloat()) })
        }
    )
}

@Preview(widthDp = 840, heightDp = 600, showBackground = true)
@Composable
private fun ProgressLargePreview() { ProgressScreen() }

@Preview(widthDp = 360, heightDp = 640, showBackground = true)
@Composable
private fun ProgressSmallPreview() { ProgressScreen() }
