package com.mtlc.studyplan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mtlc.studyplan.R
import com.mtlc.studyplan.Task
import com.mtlc.studyplan.ui.theme.*
import com.mtlc.studyplan.ui.components.estimateTaskMinutes

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
fun skillColor(skill: Skill): Color = when (skill) {
    Skill.GRAMMAR -> SkillGrammar
    Skill.READING -> SkillReading
    Skill.LISTENING -> SkillListening
    Skill.VOCAB -> SkillVocab
    Skill.OTHER -> MaterialTheme.colorScheme.outline
}

@Composable
fun TaskCard(
    task: Task,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val skill = remember(task) { detectSkill(task.desc, task.details) }
    val minutes = remember(task) { estimateTaskMinutes(task.desc, task.details) }
    val cd = remember(task, skill) { "${skill.name.lowercase()} ${task.desc}" }

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = cd },
        elevation = CardDefaults.elevatedCardElevation(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) }
                .padding(0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left colored strip with per-skill tonal mapping for current theme
            run {
                val isDark = isSystemInDarkTheme()
                val (lightA, darkA) = when (skill) {
                    Skill.GRAMMAR -> 0.14f to 0.30f  // blue: moderate tone change
                    Skill.READING -> 0.10f to 0.25f  // green: slightly subtler
                    Skill.LISTENING -> 0.08f to 0.40f // orange: stronger lift in dark
                    Skill.VOCAB -> 0.12f to 0.33f    // purple: balanced
                    Skill.OTHER -> 0.08f to 0.20f
                }
                val stripColor = skillStripTone(skillColor(skill), isDark, lightA, darkA)
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .heightIn(min = 56.dp)
                        .background(stripColor)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f).padding(vertical = 12.dp)) {
                Text(text = task.desc, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                task.details?.let {
                    if (it.isNotBlank()) Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(text = androidx.compose.ui.res.stringResource(id = R.string.task_minutes, minutes), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
            }

            Checkbox(checked = checked, onCheckedChange = onCheckedChange, modifier = Modifier.padding(12.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TaskCardPreview() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(16.dp)) {
        TaskCard(Task("t1", "Grammar Drills", "Tenses and modals"), checked = false, onCheckedChange = {})
        TaskCard(Task("t2", "Reading Practice", "News articles and summary"), checked = true, onCheckedChange = {})
        TaskCard(Task("t3", "Listening Session", "Podcast episode"), checked = false, onCheckedChange = {})
        TaskCard(Task("t4", "Vocab Review", "50 new words"), checked = false, onCheckedChange = {})
    }
}
