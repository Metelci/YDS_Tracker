package com.mtlc.studyplan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class TaskPriority(val value: Int) {
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    CRITICAL(4);

    companion object {
        fun fromTaskDescription(description: String): TaskPriority {
            val desc = description.lowercase()
            return when {
                desc.contains("exam") || desc.contains("test") || desc.contains("assessment") -> CRITICAL
                desc.contains("practice") || desc.contains("exercise") || desc.contains("drill") -> HIGH
                desc.contains("reading") || desc.contains("vocabulary") || desc.contains("grammar") -> MEDIUM
                else -> LOW
            }
        }
    }
}

@Composable
fun PriorityIndicator(
    priority: TaskPriority,
    modifier: Modifier = Modifier,
    showLabel: Boolean = false
) {
    val color = when (priority) {
        TaskPriority.CRITICAL -> MaterialTheme.colorScheme.error
        TaskPriority.HIGH -> MaterialTheme.colorScheme.primary
        TaskPriority.MEDIUM -> MaterialTheme.colorScheme.tertiary
        TaskPriority.LOW -> MaterialTheme.colorScheme.outline
    }

    val label = when (priority) {
        TaskPriority.CRITICAL -> "Critical"
        TaskPriority.HIGH -> "High"
        TaskPriority.MEDIUM -> "Medium"
        TaskPriority.LOW -> "Low"
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )

        if (showLabel) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun TaskCategoryChip(
    category: String,
    modifier: Modifier = Modifier
) {
    val (color, backgroundColor) = when (category.lowercase()) {
        "exam", "test", "assessment" -> MaterialTheme.colorScheme.error to MaterialTheme.colorScheme.errorContainer
        "practice", "exercise", "drill" -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.primaryContainer
        "reading", "vocabulary", "grammar" -> MaterialTheme.colorScheme.tertiary to MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.onSurface to MaterialTheme.colorScheme.surfaceVariant
    }

    AssistChip(
        onClick = { /* No action */ },
        label = {
            Text(
                text = category,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontSize = 10.sp
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = backgroundColor,
            labelColor = color
        ),
        modifier = modifier
    )
}

@Composable
fun EstimatedTimeChip(
    minutes: Int,
    modifier: Modifier = Modifier
) {
    val color = when {
        minutes >= 120 -> MaterialTheme.colorScheme.error
        minutes >= 60 -> MaterialTheme.colorScheme.primary
        minutes >= 30 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.outline
    }

    AssistChip(
        onClick = { /* No action */ },
        label = {
            Text(
                text = "${minutes}min",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = color.copy(alpha = 0.1f),
            labelColor = color
        ),
        modifier = modifier
    )
}