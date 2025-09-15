package com.mtlc.studyplan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.ceil

@Composable
fun StudyHeatmap(
    entries: Map<LocalDate, Int>,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    cellSize: Dp = 12.dp,
    gap: Dp = 3.dp,
) {
    val today = LocalDate.now()
    val startOfThisWeek = today.minusDays(((today.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7).toLong())
    val start = startOfThisWeek.minusWeeks(11) // 12 weeks total: weeks 0..11

    // Prepare 12 columns × 7 rows (Mon..Sun)
    val weeks = remember(start) { (0 until 12).map { w -> start.plusWeeks(w.toLong()) } }
    val dateFmt = remember { DateTimeFormatter.ofPattern("EEE, MMM d") }
    val maxCount = remember(entries) { entries.values.maxOrNull() ?: 0 }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        weeks.forEach { weekStart ->
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                DayOfWeek.values().forEach { dow ->
                    // Compose DayOfWeek enum is Mon=1..Sun=7; match grid order Mon..Sun
                    val date = weekStart.plusDays((dow.value - DayOfWeek.MONDAY.value).toLong())
                    val count = entries[date] ?: 0
                    val color = cellColor(count, maxCount)
                    val cd = "${date.format(dateFmt)} — ${count} tasks"
                    Box(
                        modifier = Modifier
                            .padding(end = gap)
                            .size(cellSize)
                            .background(color = color, shape = MaterialTheme.shapes.extraSmall)
                            .semantics { contentDescription = cd }
                            .clickable { onDayClick(date) }
                    )
                }
            }
        }
    }
}

@Composable
private fun cellColor(count: Int, maxCount: Int) = when {
    count <= 0 -> MaterialTheme.colorScheme.surfaceVariant
    else -> {
        // 4 intensity steps for 1+
        val step = when {
            count >= ceil((maxCount * 0.75).coerceAtLeast(1.0)).toInt() -> 4
            count >= ceil((maxCount * 0.5).coerceAtLeast(1.0)).toInt() -> 3
            count >= ceil((maxCount * 0.25).coerceAtLeast(1.0)).toInt() -> 2
            else -> 1
        }
        when (step) {
            1 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
            2 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
            3 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
            else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
        }
    }
}

