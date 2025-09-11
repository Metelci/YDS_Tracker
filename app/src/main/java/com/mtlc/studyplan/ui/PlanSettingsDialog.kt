package com.mtlc.studyplan.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.mtlc.studyplan.R
import java.time.LocalDate
import com.mtlc.studyplan.ui.theme.LocalSpacing
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanSettingsDialog(
    startEpochDay: Long,
    totalWeeks: Int,
    endEpochDay: Long?,
    totalMonths: Int?,
    onDismiss: () -> Unit,
    onSave: (newStartEpochDay: Long, newTotalWeeks: Int, newEndEpochDay: Long?, newTotalMonths: Int?) -> Unit,
) {
    val startDateInitial = remember(startEpochDay) { LocalDate.ofEpochDay(startEpochDay) }
    var startDateText by remember { mutableStateOf(startDateInitial.toString()) }
    var weeksText by remember { mutableStateOf(totalWeeks.toString()) }
    var endDateText by remember { mutableStateOf(endEpochDay?.let { LocalDate.ofEpochDay(it).toString() } ?: "") }
    var monthsText by remember { mutableStateOf(totalMonths?.toString() ?: "") }
    var errorText by remember { mutableStateOf<String?>(null) }

    val title = stringResource(id = R.string.plan_settings_title)
    val startLabel = stringResource(id = R.string.plan_settings_start_date)
    val weeksLabel = stringResource(id = R.string.plan_settings_total_weeks)
    val saveLabel = stringResource(id = R.string.save)
    val cancelLabel = stringResource(id = R.string.cancel)
    val invalidInputMsg = stringResource(id = R.string.plan_settings_invalid_input)
    val endLabel = stringResource(id = R.string.plan_settings_end_date_optional)
    val monthsLabel = stringResource(id = R.string.plan_settings_total_months_optional)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            val s = LocalSpacing.current
            androidx.compose.foundation.layout.Column(
                modifier = androidx.compose.ui.Modifier.padding(s.md),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(s.sm)
            ) {
                TextField(
                    value = startDateText,
                    onValueChange = { startDateText = it },
                    label = { Text(startLabel) },
                    placeholder = { Text("YYYY-MM-DD") }
                )
                TextField(
                    value = endDateText,
                    onValueChange = { endDateText = it },
                    label = { Text(endLabel) },
                    placeholder = { Text("YYYY-MM-DD") }
                )
                TextField(
                    value = weeksText,
                    onValueChange = { weeksText = it.filter { ch -> ch.isDigit() } },
                    label = { Text(weeksLabel) }
                )
                TextField(
                    value = monthsText,
                    onValueChange = { monthsText = it.filter { ch -> ch.isDigit() } },
                    label = { Text(monthsLabel) }
                )
                errorText?.let { Text(text = it, color = androidx.compose.ui.graphics.Color.Red) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                runCatching {
                    val parsedStart = LocalDate.parse(startDateText)
                    val parsedEnd = endDateText.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) }
                    val parsedMonths = monthsText.takeIf { it.isNotBlank() }?.toInt()?.coerceIn(1, 24)

                    val computedWeeks = when {
                        parsedEnd != null -> kotlin.math.max(1, java.time.temporal.ChronoUnit.WEEKS.between(parsedStart, parsedEnd).toInt() + 1)
                        parsedMonths != null -> (parsedMonths * 4).coerceIn(1, 104)
                        weeksText.isNotBlank() -> weeksText.toInt().coerceIn(1, 104)
                        else -> totalWeeks
                    }
                    onSave(parsedStart.toEpochDay(), computedWeeks, parsedEnd?.toEpochDay(), parsedMonths)
                }.onFailure {
                    errorText = invalidInputMsg
                }
            }) { Text(text = saveLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(text = cancelLabel) } }
    )
}
