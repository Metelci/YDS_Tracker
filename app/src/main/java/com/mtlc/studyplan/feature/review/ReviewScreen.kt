package com.mtlc.studyplan.feature.review

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(result: MockResultUi, onRetrySet: (List<Int>) -> Unit, onBack: () -> Unit = {}) {
    Scaffold(topBar = { TopAppBar(title = { Text("Review & Insights") }) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Summary", style = MaterialTheme.typography.titleMedium)
            Text("Correct: ${result.correct}/${result.total}")
            Text("Avg sec/Q: ${result.avgSecPerQ}")

            Divider()
            Text("Per-section", style = MaterialTheme.typography.titleMedium)
            val maxPct = remember(result.perSection) { result.perSection.maxOfOrNull { if (it.total > 0) (it.correct * 100 / it.total) else 0 } ?: 100 }
            result.perSection.forEach { s ->
                val pct = if (s.total > 0) (s.correct * 100 / s.total) else 0
                AccuracyBar(section = s.section, percent = pct, avgSec = s.avgSecPerQ, max = maxPct)
            }

            Divider()
            Text("Wrong Questions", style = MaterialTheme.typography.titleMedium)
            if (result.wrongIds.isEmpty()) {
                Text("No wrong answers. Great job!")
            } else {
                result.wrongIds.chunked(5).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { id ->
                            Text("Q$id", modifier = Modifier.semantics { contentDescription = "Wrong question $id" })
                        }
                    }
                }
                Button(onClick = { onRetrySet(result.wrongIds.take(15)) }) { Text("Start Retry Set (${result.wrongIds.take(15).size})") }
            }
        }
    }
}

@Composable
private fun AccuracyBar(section: String, percent: Int, avgSec: Int, max: Int) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("$section: $percent% • ${avgSec}s/Q", fontWeight = FontWeight.SemiBold)
        val barMaxWidth = 1f
        val normalized = if (max > 0) percent.toFloat() / max.toFloat() else 0f
        val primary = Color(0xFF1E88E5) // blue
        val alt = Color(0xFFF57C00) // orange (color-blind safe pairing)
        Row(
            Modifier
                .fillMaxWidth()
                .height(12.dp)
                .background(alt.copy(alpha = 0.25f))
                .semantics { contentDescription = "$section accuracy $percent percent" },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .fillMaxWidth(normalized.coerceIn(0f, barMaxWidth))
                    .fillMaxHeight()
                    .background(primary)
            )
        }
    }
}
