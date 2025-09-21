package com.mtlc.studyplan.settings.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import android.annotation.SuppressLint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToCategory: (String) -> Unit = {},
    onBack: () -> Unit = {}
) {
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) }
    ) { _ ->
        Text("Settings screen placeholder")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailScreen(
    title: String,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
) {
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    Scaffold(
        topBar = { TopAppBar(title = { Text(title) }) }
    ) { _ ->
        Text("Category details placeholder")
    }
}

