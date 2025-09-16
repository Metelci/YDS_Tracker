@file:OptIn(ExperimentalMaterial3Api::class)
package com.mtlc.studyplan.feature.questions

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mtlc.studyplan.questions.SkillCategory

@Composable
fun QuestionsScreen(vm: QuestionsViewModel = viewModel()) {
    val questions by vm.questions.collectAsState()
    val loading by vm.loading.collectAsState()
    val category by vm.category.collectAsState()
    val headerAcc by vm.headerCategoryAccuracy.collectAsState()
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    var showSummary by remember { mutableStateOf(false) }
    var logSession by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Practice Questions") },
                navigationIcon = {
                    IconButton(onClick = { backDispatcher?.onBackPressed() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Controls
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    val label = when (category) {
                        null -> "Personalized"
                        SkillCategory.GRAMMAR -> "Grammar"
                        SkillCategory.READING -> "Reading"
                        SkillCategory.LISTENING -> "Listening"
                        SkillCategory.VOCAB -> "Vocabulary"
                    }
                    OutlinedTextField(
                        value = label,
                        onValueChange = {},
                        label = { Text("Mode") },
                        readOnly = true,
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).width(180.dp)
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(text = { Text("Personalized") }, onClick = { vm.setCategory(null); expanded = false })
                        DropdownMenuItem(text = { Text("Grammar") }, onClick = { vm.setCategory(SkillCategory.GRAMMAR); expanded = false })
                        DropdownMenuItem(text = { Text("Reading") }, onClick = { vm.setCategory(SkillCategory.READING); expanded = false })
                        DropdownMenuItem(text = { Text("Listening") }, onClick = { vm.setCategory(SkillCategory.LISTENING); expanded = false })
                        DropdownMenuItem(text = { Text("Vocabulary") }, onClick = { vm.setCategory(SkillCategory.VOCAB); expanded = false })
                    }
                }

                var count by remember { mutableStateOf(10) }
                OutlinedTextField(
                    value = count.toString(),
                    onValueChange = { v -> v.toIntOrNull()?.let { count = it.coerceIn(1, 50) } },
                    label = { Text("Count") },
                    singleLine = true,
                    modifier = Modifier.width(100.dp)
                )

                Button(onClick = { vm.generate(count) }, enabled = !loading) {
                    if (loading) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Generate")
                }
                Button(onClick = { showSummary = true }, enabled = questions.isNotEmpty()) { Text("Submit All") }
            }

            Spacer(Modifier.height(12.dp))

            // Header: recent accuracy by category (AnalyticsEngine)
            if (headerAcc.isNotEmpty()) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Accuracy by category (last 30d)", style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            headerAcc.forEach { (cat, value) ->
                                AssistChip(onClick = {}, label = { Text("${cat.name.lowercase().replaceFirstChar { it.titlecase() }} ${(value*100).toInt()}%") })
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            if (questions.isEmpty() && !loading) {
                Text("No questions yet. Tap Generate to start.")
            }

            LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(questions, key = { it.data.id }) { q ->
                    QuestionCard(
                        question = q,
                        onSelect = { idx -> vm.select(q.data.id, idx) },
                        onCheck = { vm.check(q.data.id) }
                    )
                }
            }

            // Summary dialog
            if (showSummary) {
                val s = vm.buildSummary()
                val perCat = remember(questions) {
                    questions.groupBy { it.data.category }.mapValues { (_, items) ->
                        val total = items.size
                        val correct = items.count { it.wasCorrect == true || (it.selectedIndex != null && it.selectedIndex == it.data.correctIndex && it.checked) }
                        val answered = items.count { it.selectedIndex != null }
                        Triple(total, answered, if (total > 0) (correct.toFloat() / total.toFloat()) else 0f)
                    }
                }
                AlertDialog(
                    onDismissRequest = { showSummary = false },
                    confirmButton = {
                        TextButton(onClick = {
                            vm.submitAll(logSession)
                            showSummary = false
                        }) { Text(if (logSession) "Submit & Log" else "Submit") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showSummary = false }) { Text("Cancel") }
                    },
                    title = { Text("Session Summary") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Answered: ${s.answered}/${s.total}")
                            Text("Correct: ${s.correct}")
                            Text("Accuracy: ${(s.accuracy * 100).toInt()}%")
                            Text("Minutes: ${s.minutes}")
                            if (perCat.isNotEmpty()) {
                                HorizontalDivider()
                                Text("By Category", style = MaterialTheme.typography.labelLarge)
                                perCat.forEach { (cat, triple) ->
                                    val (t, a, acc) = triple
                                    Text("${cat.name.lowercase().replaceFirstChar { it.titlecase() }}: $a/$t • ${(acc * 100).toInt()}%")
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Checkbox(checked = logSession, onCheckedChange = { logSession = it })
                                Text("Add to Progress as a session")
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun QuestionCard(
    question: QuestionUI,
    onSelect: (Int) -> Unit,
    onCheck: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(question.data.prompt, style = MaterialTheme.typography.titleMedium)

            question.data.options.forEachIndexed { idx, opt ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = question.selectedIndex == idx,
                        onClick = { onSelect(idx) }
                    )
                    Text(opt)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onCheck, enabled = question.selectedIndex != null && !question.checked) {
                    Text("Check")
                }
                if (question.checked) {
                    val text = if (question.wasCorrect == true) "Correct" else "Incorrect"
                    val color = if (question.wasCorrect == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    Text(text, color = color, fontWeight = FontWeight.SemiBold)
                }
            }

            if (question.checked && question.data.explanation.isNotBlank()) {
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Text("Explanation", style = MaterialTheme.typography.labelLarge)
                Text(question.data.explanation, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
