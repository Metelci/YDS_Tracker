package com.mtlc.studyplan.feature.practice

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import com.mtlc.studyplan.ui.theme.Elevations
import com.mtlc.studyplan.ui.components.TwoPaneScaffold

@Composable
fun PracticeScreen() {
    val s = LocalSpacing.current
    var selected by remember { mutableStateOf(0) }
    val items = remember { List(20) { "Practice Set #${it + 1}" } }
    TwoPaneScaffold(
        list = {
            Text("Practice", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(s.xs))
            LazyColumn(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(s.xs)
            ) {
                itemsIndexed(items) { idx, label ->
                    ElevatedCard(
                        onClick = { selected = idx },
                        modifier = Modifier.fillMaxWidth().focusable(),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = if (idx == selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        shape = MaterialTheme.shapes.large,
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = Elevations.level1)
                    ) {
                        Text(label, Modifier.padding(s.sm))
                    }
                }
            }
        },
        detail = {
            Text("Details", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(s.xs))
            Text("Selected: ${items.getOrNull(selected) ?: "None"}", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(s.lg))
            ElevatedButton(onClick = { /* start practice */ }) { Text("Start") }
        }
    )
}

@Preview(widthDp = 840, heightDp = 600, showBackground = true)
@Composable
private fun PracticeLargePreview() { PracticeScreen() }

@Preview(widthDp = 360, heightDp = 640, showBackground = true)
@Composable
private fun PracticeSmallPreview() { PracticeScreen() }
