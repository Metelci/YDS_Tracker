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
    monMinutes: Int,
    tueMinutes: Int,
    wedMinutes: Int,
    thuMinutes: Int,
    friMinutes: Int,
    satMinutes: Int,
    sunMinutes: Int,
    dateFormatPattern: String?,
    onDismiss: () -> Unit,
    onSave: (
        newStartEpochDay: Long,
        newTotalWeeks: Int,
        newEndEpochDay: Long?,
        newTotalMonths: Int?,
        newMonMinutes: Int,
        newTueMinutes: Int,
        newWedMinutes: Int,
        newThuMinutes: Int,
        newFriMinutes: Int,
        newSatMinutes: Int,
        newSunMinutes: Int,
        newDateFormatPattern: String?,
    ) -> Unit,
) {
    val startDateInitial = remember(startEpochDay) { LocalDate.ofEpochDay(startEpochDay) }
    var startDateText by remember { mutableStateOf(startDateInitial.toString()) }
    var weeksText by remember { mutableStateOf(totalWeeks.toString()) }
    var endDateText by remember { mutableStateOf(endEpochDay?.let { LocalDate.ofEpochDay(it).toString() } ?: "") }
    var monthsText by remember { mutableStateOf(totalMonths?.toString() ?: "") }
    var errorText by remember { mutableStateOf<String?>(null) }
    var monText by remember { mutableStateOf(monMinutes.toString()) }
    var tueText by remember { mutableStateOf(tueMinutes.toString()) }
    var wedText by remember { mutableStateOf(wedMinutes.toString()) }
    var thuText by remember { mutableStateOf(thuMinutes.toString()) }
    var friText by remember { mutableStateOf(friMinutes.toString()) }
    var satText by remember { mutableStateOf(satMinutes.toString()) }
    var sunText by remember { mutableStateOf(sunMinutes.toString()) }
    var datePatternText by remember { mutableStateOf(dateFormatPattern ?: "") }

    val title = stringResource(id = R.string.plan_settings_title)
    val startLabel = stringResource(id = R.string.plan_settings_start_date)
    val weeksLabel = stringResource(id = R.string.plan_settings_total_weeks)
    val saveLabel = stringResource(id = R.string.save)
    val availabilityTitle = stringResource(id = R.string.plan_settings_availability_title)
    val monLabel = stringResource(id = R.string.plan_settings_mon_minutes)
    val tueLabel = stringResource(id = R.string.plan_settings_tue_minutes)
    val wedLabel = stringResource(id = R.string.plan_settings_wed_minutes)
    val thuLabel = stringResource(id = R.string.plan_settings_thu_minutes)
    val friLabel = stringResource(id = R.string.plan_settings_fri_minutes)
    val satLabel = stringResource(id = R.string.plan_settings_sat_minutes)
    val sunLabel = stringResource(id = R.string.plan_settings_sun_minutes)
    val cancelLabel = stringResource(id = R.string.cancel)
    val invalidInputMsg = stringResource(id = R.string.plan_settings_invalid_input)
    val endLabel = stringResource(id = R.string.plan_settings_end_date_optional)
    val monthsLabel = stringResource(id = R.string.plan_settings_total_months_optional)
    val useNextExamLabel = stringResource(id = R.string.plan_settings_use_next_exam_date)
    val datePatternLabel = stringResource(id = R.string.plan_settings_date_pattern_optional)
    val datePatternHint = stringResource(id = R.string.plan_settings_date_pattern_hint)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            val s = LocalSpacing.current
            androidx.compose.foundation.layout.Column(
                modifier = androidx.compose.ui.Modifier.padding(s.md),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(s.sm)
            ) {
                Text(text = availabilityTitle)
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
                val nextExam = remember { com.mtlc.studyplan.data.ExamCalendarDataSource.getNextExam() }
                if (nextExam != null) {
                    TextButton(onClick = { endDateText = nextExam.examDate.toString() }) {
                        Text(useNextExamLabel)
                    }
                }
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
                // Availability fields
                TextField(
                    value = monText,
                    onValueChange = { monText = it.filter { ch -> ch.isDigit() } },
                    label = { Text(monLabel) }
                )
                TextField(
                    value = tueText,
                    onValueChange = { tueText = it.filter { ch -> ch.isDigit() } },
                    label = { Text(tueLabel) }
                )
                TextField(
                    value = wedText,
                    onValueChange = { wedText = it.filter { ch -> ch.isDigit() } },
                    label = { Text(wedLabel) }
                )
                TextField(
                    value = thuText,
                    onValueChange = { thuText = it.filter { ch -> ch.isDigit() } },
                    label = { Text(thuLabel) }
                )
                TextField(
                    value = friText,
                    onValueChange = { friText = it.filter { ch -> ch.isDigit() } },
                    label = { Text(friLabel) }
                )
                TextField(
                    value = satText,
                    onValueChange = { satText = it.filter { ch -> ch.isDigit() } },
                    label = { Text(satLabel) }
                )
                TextField(
                    value = sunText,
                    onValueChange = { sunText = it.filter { ch -> ch.isDigit() } },
                    label = { Text(sunLabel) }
                )
                // Date format quick selector
                Text(text = datePatternLabel)
                androidx.compose.foundation.layout.Row(
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(s.sm)
                ) {
                    val presets = listOf(
                        "yyyy-MM-dd",
                        "dd.MM.yyyy",
                        "MMM d, yyyy"
                    )
                    TextButton(onClick = { datePatternText = "" }) { Text(stringResource(id = R.string.plan_settings_date_use_locale)) }
                    presets.forEach { p -> TextButton(onClick = { datePatternText = p }) { Text(p) } }
                }

                // Date format
                TextField(
                    value = datePatternText,
                    onValueChange = { datePatternText = it },
                    label = { Text(datePatternLabel) },
                    placeholder = { Text(datePatternHint) }
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
                        parsedEnd != null -> {
                            val totalDays = java.time.temporal.ChronoUnit.DAYS.between(parsedStart, parsedEnd).toInt() + 1
                            val startOffset = parsedStart.dayOfWeek.value - 1 // Monday=0
                            val weeksNeeded = (startOffset + totalDays + 6) / 7 // ceil((startOffset+days)/7)
                            kotlin.math.max(1, weeksNeeded)
                        }
                        parsedMonths != null -> (parsedMonths * 4).coerceIn(1, 104)
                        weeksText.isNotBlank() -> weeksText.toInt().coerceIn(1, 104)
                        else -> totalWeeks
                    }
                    val minMon = monText.toIntOrNull()?.coerceIn(0, 600) ?: 0
                    val minTue = tueText.toIntOrNull()?.coerceIn(0, 600) ?: 0
                    val minWed = wedText.toIntOrNull()?.coerceIn(0, 600) ?: 0
                    val minThu = thuText.toIntOrNull()?.coerceIn(0, 600) ?: 0
                    val minFri = friText.toIntOrNull()?.coerceIn(0, 600) ?: 0
                    val minSat = satText.toIntOrNull()?.coerceIn(0, 600) ?: 0
                    val minSun = sunText.toIntOrNull()?.coerceIn(0, 600) ?: 0
                    onSave(
                        parsedStart.toEpochDay(),
                        computedWeeks,
                        parsedEnd?.toEpochDay(),
                        parsedMonths,
                        minMon, minTue, minWed, minThu, minFri, minSat, minSun,
                        datePatternText.trim().ifBlank { null }
                    )
                }.onFailure {
                    errorText = invalidInputMsg
                }
            }) { Text(text = saveLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(text = cancelLabel) } }
    )
}
