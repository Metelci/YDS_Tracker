@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
package com.mtlc.studyplan.feature.mock

import android.widget.Toast
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mtlc.studyplan.ui.theme.LocalSpacing
import com.mtlc.studyplan.ui.theme.Elevations
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mtlc.studyplan.core.viewModelFactory

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MockExamRoute(
    onSubmit: (MockResult) -> Unit,
    vmFactory: (() -> MockExamViewModel)? = null
) {
    val context = LocalContext.current
    val repo = remember { MockExamRepository(context.mockExamDataStore) }
    val vm: MockExamViewModel = viewModel(factory = viewModelFactory { MockExamViewModel(repo) })

    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        vm.dispatch(MockIntent.Load)
        com.mtlc.studyplan.metrics.Analytics.track(context, "mock_start")
    }

    var showExitConfirm by remember { mutableStateOf(false) }

    if (showExitConfirm) {
        AlertDialog(
            onDismissRequest = { showExitConfirm = false },
            confirmButton = {
                TextButton(onClick = { showExitConfirm = false }) { Text("Stay") }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirm = false; Toast.makeText(context, "Exam paused", Toast.LENGTH_SHORT).show() }) { Text("Exit") }
            },
            title = { Text("Leave exam?") },
            text = { Text("Your timer continues. You can resume later.") }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val mm = state.remainingSeconds / 60
                    val ss = state.remainingSeconds % 60
                    Text("Time left: %02d:%02d".format(mm, ss))
                },
                actions = {
                    TextButton(onClick = { vm.dispatch(MockIntent.Submit) }) { Text("Submit") }
                }
            )
        }
    ) { padding ->
        Row(Modifier.fillMaxSize().padding(padding)) {
            // Section map rail
            NavigationRail { SectionRail(state, onJump = { vm.dispatch(MockIntent.Jump(it)) }) }

            HorizontalDivider(Modifier.fillMaxHeight().width(1.dp))

            // Pager
            val pager = rememberPagerState(initialPage = state.currentIndex, pageCount = { state.questions.size })
            LaunchedEffect(state.currentIndex) { pager.scrollToPage(state.currentIndex) }
            HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { page ->
                val q = state.questions.getOrNull(page)
                if (q != null) {
                    QuestionCard(
                        question = q,
                        selectedIndex = state.selections[q.id],
                        marked = q.id in state.markedForReview,
                        onSelect = { vm.dispatch(MockIntent.Select(q.id, it)) },
                        onToggleReview = { vm.dispatch(MockIntent.ToggleReview(q.id)) }
                    )
                }
            }
        }
    }

    if (state.isSubmitting) {
        val result = computeResult(state)
        com.mtlc.studyplan.metrics.Analytics.track(
            context,
            "mock_submit",
            mapOf(
                "correct" to result.correct.toString(),
                "total" to result.total.toString(),
                "avg_sec_per_q" to result.avgSecPerQ.toString()
            )
        )
        onSubmit(result)
    }
}

@Composable
private fun SectionRail(state: MockExamState, onJump: (Int) -> Unit) {
    val sections = state.questions.groupBy { it.section }
    val s = LocalSpacing.current
    Column(Modifier.width(92.dp).padding(s.xs), verticalArrangement = Arrangement.spacedBy(s.xs)) {
        sections.entries.forEach { (section, items) ->
            Text(section, style = MaterialTheme.typography.labelLarge)
            FlowRowMain(items, state, onJump)
            HorizontalDivider()
        }
    }
}

@Composable
private fun FlowRowMain(items: List<MockQuestion>, state: MockExamState, onJump: (Int) -> Unit) {
    val s = LocalSpacing.current
    FlowRow(horizontalArrangement = Arrangement.spacedBy(s.xs / 2), verticalArrangement = Arrangement.spacedBy(s.xs / 2)) {
        items.forEach { q ->
            val answered = state.selections.containsKey(q.id)
            val marked = q.id in state.markedForReview
            val cd = buildString {
                append("Question ${q.id}")
                if (answered) append(", answered") else append(", not answered")
                if (marked) append(", marked for review")
            }
            AssistChip(
                onClick = { onJump(q.id - 1) },
                label = { Text(q.id.toString()) },
                leadingIcon = if (marked) ({ Text("★") }) else null,
                modifier = Modifier.semantics { contentDescription = cd }
            )
        }
    }
}

@Composable
private fun QuestionCard(
    question: MockQuestion,
    selectedIndex: Int?,
    marked: Boolean,
    onSelect: (Int) -> Unit,
    onToggleReview: () -> Unit
) {
    val s = LocalSpacing.current
    Card(Modifier.fillMaxSize().padding(s.md), shape = MaterialTheme.shapes.large, elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = Elevations.level1)) {
        Column(Modifier.fillMaxSize().padding(s.md).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(s.sm)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Q${question.id} - ${question.section}", fontWeight = FontWeight.SemiBold)
                FilterChip(
                    selected = marked,
                    onClick = onToggleReview,
                    label = { Text(if (marked) "Marked" else "Mark") },
                    modifier = Modifier.semantics { contentDescription = if (marked) "Marked for review" else "Mark for review" }
                )
            }
            Text(question.text)
            question.options.forEachIndexed { idx, opt ->
                ElevatedCard(
                    onClick = { onSelect(idx) },
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Option ${'A' + idx}: $opt" }.focusable(),
                    enabled = true
                ) {
                    Row(Modifier.fillMaxWidth().padding(s.sm), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedIndex == idx, onClick = { onSelect(idx) })
                        Spacer(Modifier.width(s.xs))
                        Text(text = "${'A' + idx}. $opt")
                    }
                }
            }
        }
    }
}

@Serializable
data class MockResult(
    val correct: Int,
    val total: Int,
    val avgSecPerQ: Int,
    val perSection: Map<String, Pair<Int, Int>>, // section -> (correct, total)
    val wrongIds: List<Int> = emptyList()
)

private fun computeResult(state: MockExamState): MockResult {
    val total = state.questions.size
    val answeredCorrect = state.questions.count { q -> state.selections[q.id] == q.correctIndex }
    val wrongIds = state.questions.filter { q -> state.selections[q.id] != null && state.selections[q.id] != q.correctIndex }.map { it.id }
    val elapsed = 180 * 60 - state.remainingSeconds
    val avg = if (total > 0 && elapsed >= 0) (elapsed / total) else 0
    val per = state.questions.groupBy({ it.section }) { it }
        .mapValues { (_, qs) ->
            val correct = qs.count { q -> state.selections[q.id] == q.correctIndex }
            correct to qs.size
        }
    return MockResult(answeredCorrect, total, avg, per, wrongIds)
}
