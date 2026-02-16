package com.mtlc.studyplan.ui.components

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mtlc.studyplan.utils.ChangelogLoader
import com.mtlc.studyplan.utils.ChangelogParser

/**
 * About/What's New dialog that dynamically loads content from CHANGELOG.md
 *
 * @param versionName Current app version (e.g., "2.9.77")
 * @param versionCode Current build number (e.g., 106)
 * @param onDismiss Callback when dialog is dismissed
 */
@Composable
fun AboutDialog(
    versionName: String,
    versionCode: Long,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // Load release notes on first composition
    val releaseNotes = remember(versionName) {
        val changelogText = ChangelogLoader.loadFromAssets(context)

        if (changelogText != null) {
            ChangelogParser.extractLatestRelease(changelogText)
                ?: ChangelogParser.getFallbackNotes(versionName, versionCode)
        } else {
            // Fallback to hardcoded notes if file missing
            Log.w("AboutDialog", "CHANGELOG.md not found in assets, using fallback")
            ChangelogParser.getFallbackNotes(versionName, versionCode)
        }
    }

    // Warn if version mismatch
    if (releaseNotes.version != versionName) {
        Log.w(
            "AboutDialog",
            "Version mismatch: BuildConfig=$versionName, Changelog=${releaseNotes.version}"
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("What's New") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Version header
                Text(
                    text = "Version $versionName (Build $versionCode)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Version mismatch warning (if applicable)
                if (releaseNotes.version != versionName) {
                    Text(
                        text = "Note: Showing v${releaseNotes.version} notes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                // Release notes content (parsed from CHANGELOG)
                Text(
                    text = releaseNotes.content,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
