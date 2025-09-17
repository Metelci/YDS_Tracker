@file:OptIn(ExperimentalMaterial3Api::class)
package com.mtlc.studyplan.ui

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import com.mtlc.studyplan.data.PlanSettingsStore
import com.mtlc.studyplan.data.PlanDurationSettings
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.edit
import com.mtlc.studyplan.data.dataStore

@Composable
fun QuickNoteRoute(onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    Scaffold(topBar = { TopAppBar(title = { Text("Quick Note") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Note / Flashcard") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onClose) { Text("Cancel") }
                Button(enabled = text.isNotBlank(), onClick = {
                    scope.launch {
                        val key = stringSetPreferencesKey("quick_notes")
                        context.dataStore.edit { prefs ->
                            val cur = prefs[key] ?: emptySet()
                            prefs[key] = (cur + text).toList().takeLast(500).toSet()
                        }
                        onClose()
                    }
                }) { Text("Save") }
            }
        }
    }
}

@Composable
fun SettingsScreen() {
    // This function is kept for backward compatibility
    // The actual settings navigation is now handled in AppNavHost.kt
    Text(
        text = "Settings screen moved to new architecture",
        modifier = Modifier.padding(16.dp)
    )
}
