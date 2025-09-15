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
    val context = LocalContext.current
    val appContext = context.applicationContext
    val settingsStore = remember { PlanSettingsStore(appContext.dataStore) }
    val cfg by settingsStore.settingsFlow.collectAsState(initial = PlanDurationSettings())
    var showPlanSettings by remember { mutableStateOf(false) }
    var lastSaved by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Plan settings and preferences")
            Button(onClick = { showPlanSettings = true }) { Text(text = androidx.compose.ui.res.stringResource(id = com.mtlc.studyplan.R.string.open_plan_settings)) }
            lastSaved?.let { Text("Last note saved: $it") }
            Text("Notes are stored locally; more settings can be added here.")
        }
    }

    if (showPlanSettings) {
        com.mtlc.studyplan.ui.PlanSettingsDialog(
            startEpochDay = cfg.startEpochDay,
            totalWeeks = cfg.totalWeeks,
            endEpochDay = cfg.endEpochDay,
            totalMonths = cfg.totalMonths,
            monMinutes = cfg.monMinutes,
            tueMinutes = cfg.tueMinutes,
            wedMinutes = cfg.wedMinutes,
            thuMinutes = cfg.thuMinutes,
            friMinutes = cfg.friMinutes,
            satMinutes = cfg.satMinutes,
            sunMinutes = cfg.sunMinutes,
            dateFormatPattern = cfg.dateFormatPattern,
            onDismiss = { showPlanSettings = false },
            onSave = { startEpochDay, weeks, endEpochDay, totalMonths, mon, tue, wed, thu, fri, sat, sun, pattern ->
                scope.launch {
                    settingsStore.update {
                        it.copy(
                            startEpochDay = startEpochDay,
                            totalWeeks = weeks,
                            endEpochDay = endEpochDay,
                            totalMonths = totalMonths,
                            monMinutes = mon,
                            tueMinutes = tue,
                            wedMinutes = wed,
                            thuMinutes = thu,
                            friMinutes = fri,
                            satMinutes = sat,
                            sunMinutes = sun,
                            dateFormatPattern = pattern,
                        )
                    }
                }
                showPlanSettings = false
            }
        )
    }
}
