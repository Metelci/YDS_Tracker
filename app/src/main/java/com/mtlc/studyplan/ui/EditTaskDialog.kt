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
import com.mtlc.studyplan.ui.theme.LocalSpacing
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement

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
            val s = LocalSpacing.current
            androidx.compose.foundation.layout.Column(
                modifier = androidx.compose.ui.Modifier.padding(s.md),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(s.sm)
            ) {
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
