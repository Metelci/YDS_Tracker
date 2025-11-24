@file:Suppress("LongParameterList")
package com.mtlc.studyplan.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mtlc.studyplan.data.Task

enum class Skill { GRAMMAR, READING, LISTENING, VOCAB, OTHER }

fun detectSkill(desc: String, details: String? = null): Skill {
    val s = (desc + " " + (details ?: "")).lowercase()
    return when {
        listOf("grammar", "gramer").any { s.contains(it) } -> Skill.GRAMMAR
        listOf("reading", "okuma").any { s.contains(it) } -> Skill.READING
        listOf("listening", "dinleme").any { s.contains(it) } -> Skill.LISTENING
        listOf("vocab", "kelime", "vocabulary").any { s.contains(it) } -> Skill.VOCAB
        else -> Skill.OTHER
    }
}

@Composable
fun TaskCard(
    task: Task,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    showExpandedDetails: Boolean = false,
    onExpandToggle: (() -> Unit)? = null,
    enableSwipeToComplete: Boolean = false,
    currentStreak: Int = 0,
    showPointsInfo: Boolean = true
) {
    val skill = remember(task) { detectSkill(task.desc, task.details) }
    val minutes = remember(task) { 30 } // Simple placeholder

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(task.desc, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    task.details?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(onClick = { onExpandToggle?.invoke() }, label = { Text(skill.name.lowercase().replaceFirstChar { it.titlecase() }) })
                        AssistChip(onClick = {}, label = { Text("$minutes min") })
                    }
                }
                Checkbox(checked = checked, onCheckedChange = onCheckedChange)
            }

            if (showExpandedDetails) {
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Text("Task Details", style = MaterialTheme.typography.labelLarge)
                task.details?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            }
        }
    }
}


