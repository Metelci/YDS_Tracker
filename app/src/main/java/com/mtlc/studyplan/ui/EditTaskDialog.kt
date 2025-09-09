package com.mtlc.studyplan.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@Composable
fun EditTaskDialog(
    initialDesc: String,
    initialDetails: String,
    onDismiss: () -> Unit,
    onSave: (newDesc: String, newDetails: String) -> Unit,
) {
    var desc by remember(initialDesc) { mutableStateOf(initialDesc) }
    var details by remember(initialDetails) { mutableStateOf(initialDetails) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Task") },
        text = {
            androidx.compose.foundation.layout.Column {
                TextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Title") }
                )
                TextField(
                    value = details,
                    onValueChange = { details = it },
                    label = { Text("Details") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(desc, details) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

