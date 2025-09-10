package com.mtlc.studyplan.feature.reader

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    passage: PassageUi,
    onBack: () -> Unit = {},
    prefsRepo: ReaderPrefsRepository = ReaderPrefsRepository(LocalContext.current.readerPrefsDataStore),
    glossaryRepo: GlossaryRepo = FakeGlossaryRepo(),
) {
    val scope = rememberCoroutineScope()
    val prefs by prefsRepo.prefsFlow.collectAsState(initial = ReaderPrefs())
    var showControls by remember { mutableStateOf(false) }

    // Timer
    var seconds by remember { mutableIntStateOf(0) }
    LaunchedEffect(passage.id) {
        seconds = 0
        while (true) {
            delay(1000)
            seconds += 1
        }
    }

    val words = remember(passage.body) {
        passage.body.split(Regex("\\s+")).filter { it.isNotBlank() }
    }
    val wpm by remember(seconds, words.size) {
        derivedStateOf { if (seconds > 0) (words.size * 60 / seconds).coerceAtLeast(0) else 0 }
    }

    var definition by remember { mutableStateOf<String?>(null) }
    var selectedWord by remember { mutableStateOf<String?>(null) }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isDark = when (prefs.theme) {
        ReaderTheme.System -> when (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> true
            else -> false
        }
        ReaderTheme.Dark -> true
        ReaderTheme.Light, ReaderTheme.Sepia -> false
    }
    val sepiaBg = Color(0xFFF5E9D4)
    val bg = when (prefs.theme) {
        ReaderTheme.Sepia -> sepiaBg
        else -> if (isDark) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.background
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(passage.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showControls = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Reader settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .background(bg)
                .padding(padding)
        ) {
            // WPM bar
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("WPM: $wpm", style = MaterialTheme.typography.labelLarge)
                val mm = seconds / 60
                val ss = seconds % 60
                Text("Time: %02d:%02d".format(mm, ss), style = MaterialTheme.typography.labelLarge)
            }

            val scroll = rememberScrollState()
            var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

            Text(
                text = AnnotatedString(passage.body),
                fontSize = prefs.fontScaleSp.sp,
                lineHeight = (prefs.fontScaleSp * prefs.lineHeightMult).sp,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(scroll)
                    .pointerInput(passage.id, prefs) {
                        detectTapGestures(
                            onLongPress = { offset: Offset ->
                                layoutResult?.let { lr ->
                                    val position = lr.getOffsetForPosition(offset)
                                    val text = lr.layoutInput.text.text
                                    val word = extractWordAt(text, position)
                                    if (word.isNotBlank()) {
                                        selectedWord = word
                                        scope.launch {
                                            definition = glossaryRepo.lookup(word)
                                        }
                                    }
                                }
                            }
                        )
                    },
                onTextLayout = { layoutResult = it }
            )
        }
    }

    if (showControls) {
        ReaderControlsSheet(
            prefs = prefs,
            onDismiss = { showControls = false },
            onChange = { new -> scope.launch { prefsRepo.save(new) } }
        )
    }

    if (definition != null && selectedWord != null) {
        AlertDialog(
            onDismissRequest = { definition = null; selectedWord = null },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { glossaryRepo.addToVocab(selectedWord!!) }
                    definition = null; selectedWord = null
                }) { Text("Add to vocab") }
            },
            dismissButton = {
                TextButton(onClick = { definition = null; selectedWord = null }) { Text("Close") }
            },
            title = { Text(selectedWord!!) },
            text = { Text(definition!!) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderControlsSheet(
    prefs: ReaderPrefs,
    onDismiss: () -> Unit,
    onChange: (ReaderPrefs) -> Unit
) {
    var localPrefs by remember { mutableStateOf(prefs) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Reading Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Font size: ${"%.0f".format(localPrefs.fontScaleSp)}sp")
            Slider(
                value = localPrefs.fontScaleSp,
                onValueChange = { localPrefs = localPrefs.copy(fontScaleSp = it) },
                valueRange = 14f..28f
            )
            Text("Line height: ${"%.1f".format(localPrefs.lineHeightMult)}x")
            Slider(
                value = localPrefs.lineHeightMult,
                onValueChange = { localPrefs = localPrefs.copy(lineHeightMult = it) },
                valueRange = 1.2f..2.0f
            )
            Text("Theme")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(ReaderTheme.System, ReaderTheme.Light, ReaderTheme.Dark, ReaderTheme.Sepia).forEach { t ->
                    FilterChip(
                        selected = localPrefs.theme == t,
                        onClick = { localPrefs = localPrefs.copy(theme = t) },
                        label = { Text(t.name) }
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { onChange(localPrefs); onDismiss() }) { Text("Apply") }
            }
        }
    }
}

private fun extractWordAt(text: String, offset: Int): String {
    if (text.isEmpty()) return ""
    var start = offset.coerceIn(0, text.lastIndex)
    var end = start
    while (start > 0 && text[start - 1].isLetter()) start--
    while (end < text.length && text[end].isLetter()) end++
    return text.substring(start, end).trim().trim(',', '.', ';', ':', '!', '?', '"', '\'', ')', '(')
}

@Preview(showBackground = true)
@Composable
private fun ReaderPreview() {
    ReaderScreen(
        passage = PassageUi(
            id = "p1",
            title = "Sample Passage",
            body = List(20) { "This is a sample reading passage for preview mode." }.joinToString("\n\n")
        ),
        onBack = {}
    )
}
