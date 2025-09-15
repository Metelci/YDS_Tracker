package com.mtlc.studyplan.calendar

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.mtlc.studyplan.data.DayPlan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * ICS (iCalendar) file exporter following RFC-5545 specification
 * Provides fallback when calendar permissions are denied
 */
object IcsExporter {

    private const val ICS_VERSION = "2.0"
    private const val PRODID = "-//StudyPlan//Study Session Planner//EN"
    private const val ICS_DATE_FORMAT = "yyyyMMdd'T'HHmmss"
    private const val ICS_UTC_DATE_FORMAT = "yyyyMMdd'T'HHmmss'Z'"

    /**
     * Export day plans as ICS byte array
     */
    suspend fun export(dayPlans: List<DayPlan>): ByteArray = withContext(Dispatchers.Default) {
        val icsContent = buildIcsContent(dayPlans)
        icsContent.toByteArray(Charsets.UTF_8)
    }

    /**
     * Export specific study slots as ICS
     */
    suspend fun exportStudySlots(studySlots: List<StudyTimeSlot>): ByteArray = withContext(Dispatchers.Default) {
        val icsContent = buildIcsContentFromSlots(studySlots)
        icsContent.toByteArray(Charsets.UTF_8)
    }

    /**
     * Save ICS file and share via Intent
     */
    suspend fun shareIcs(
        context: Context,
        bytes: ByteArray,
        filename: String
    ) = withContext(Dispatchers.IO) {
        val file = saveIcsToFile(context, bytes, filename)
        shareIcsFile(context, file, filename)
    }

    /**
     * Build complete ICS content from day plans
     */
    private fun buildIcsContent(dayPlans: List<DayPlan>): String {
        val studySlots = convertDayPlansToStudySlots(dayPlans)
        return buildIcsContentFromSlots(studySlots)
    }

    /**
     * Build ICS content from study slots
     */
    private fun buildIcsContentFromSlots(studySlots: List<StudyTimeSlot>): String {
        return buildString {
            // Calendar header
            appendLine("BEGIN:VCALENDAR")
            appendLine("VERSION:$ICS_VERSION")
            appendLine("PRODID:$PRODID")
            appendLine("CALSCALE:GREGORIAN")
            appendLine("METHOD:PUBLISH")
            
            // Add timezone information for local time
            val timeZone = TimeZone.getDefault()
            val zoneId = ZoneId.systemDefault()
            appendLine("BEGIN:VTIMEZONE")
            appendLine("TZID:${zoneId.id}")
            appendLine("BEGIN:STANDARD")
            appendLine("DTSTART:19700101T000000")
            appendLine("TZOFFSETFROM:${formatTimeZoneOffset(timeZone.rawOffset)}")
            appendLine("TZOFFSETTO:${formatTimeZoneOffset(timeZone.rawOffset)}")
            appendLine("END:STANDARD")
            appendLine("END:VTIMEZONE")

            // Add events
            studySlots.forEach { slot ->
                append(buildVEvent(slot))
            }

            // Calendar footer
            appendLine("END:VCALENDAR")
        }
    }

    /**
     * Build individual VEVENT from study slot
     */
    private fun buildVEvent(slot: StudyTimeSlot): String {
        return buildString {
            val uid = "studyplan-${slot.studyEventId}@studyplan.app"
            val now = LocalDateTime.now()
            val zoneId = ZoneId.systemDefault()
            
            appendLine("BEGIN:VEVENT")
            appendLine("UID:$uid")
            
            // Date/time formatting
            val startDateTime = slot.startDateTime
            val endDateTime = slot.endDateTime
            val dtStamp = now.format(DateTimeFormatter.ofPattern(ICS_UTC_DATE_FORMAT))
            
            // Use local time with timezone
            val dtStart = startDateTime.format(DateTimeFormatter.ofPattern(ICS_DATE_FORMAT))
            val dtEnd = endDateTime.format(DateTimeFormatter.ofPattern(ICS_DATE_FORMAT))
            
            appendLine("DTSTART;TZID=${zoneId.id}:$dtStart")
            appendLine("DTEND;TZID=${zoneId.id}:$dtEnd")
            appendLine("DTSTAMP:$dtStamp")
            
            // Event details
            appendLine("SUMMARY:${escapeIcsText(slot.title)}")
            if (slot.description.isNotEmpty()) {
                appendLine("DESCRIPTION:${escapeIcsText(slot.description)}")
            }
            
            // Categories and classification
            appendLine("CATEGORIES:Study,Education")
            appendLine("CLASS:PUBLIC")
            appendLine("STATUS:CONFIRMED")
            appendLine("TRANSP:OPAQUE")
            
            // Priority (normal)
            appendLine("PRIORITY:5")
            
            // Alarm for reminder (15 minutes before)
            appendLine("BEGIN:VALARM")
            appendLine("TRIGGER:-PT15M")
            appendLine("DESCRIPTION:Study session reminder")
            appendLine("ACTION:DISPLAY")
            appendLine("END:VALARM")
            
            appendLine("END:VEVENT")
        }
    }

    /**
     * Convert day plans to study slots with reasonable defaults
     */
    private fun convertDayPlansToStudySlots(dayPlans: List<DayPlan>): List<StudyTimeSlot> {
        val slots = mutableListOf<StudyTimeSlot>()
        val today = LocalDate.now()
        
        dayPlans.forEach { dayPlan ->
            val planDate = parseDate(dayPlan.day) ?: return@forEach
            
            // Skip past dates
            if (planDate.isBefore(today)) return@forEach
            
            // Skip if no tasks
            if (dayPlan.tasks.isEmpty()) return@forEach
            
            val slot = createStudySlotFromDayPlan(dayPlan, planDate)
            slots.add(slot)
        }
        
        return slots
    }

    /**
     * Create study slot from day plan with default timing
     */
    private fun createStudySlotFromDayPlan(dayPlan: DayPlan, date: LocalDate): StudyTimeSlot {
        // Calculate duration based on number of tasks
        val estimatedDuration = minOf(dayPlan.tasks.size * 20, 120) // 20 min per task, max 2 hours
        
        // Default to 9 AM start time
        val defaultStartTime = LocalTime.of(9, 0)
        
        // Generate title from first task
        val firstTask = dayPlan.tasks.firstOrNull()
        val title = if (firstTask != null) {
            "Study: ${firstTask.desc.take(40)}"
        } else {
            "Study Session"
        }
        
        // Generate description
        val description = buildString {
            appendLine("Study Plan for ${date.format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"))}")
            appendLine()
            
            if (dayPlan.tasks.size == 1) {
                val task = dayPlan.tasks.first()
                appendLine("Task: ${task.desc}")
                task.details?.let { 
                    appendLine("Details: $it")
                }
            } else {
                appendLine("Tasks to complete:")
                dayPlan.tasks.forEach { task ->
                    appendLine("• ${task.desc}")
                }
            }
            
            appendLine()
            appendLine("Generated by StudyPlan app")
            appendLine("Duration: $estimatedDuration minutes")
        }
        
        return StudyTimeSlot(
            date = date,
            startTime = defaultStartTime,
            durationMinutes = estimatedDuration,
            title = title,
            description = description.trim(),
            studyEventId = generateEventId(date, dayPlan)
        )
    }

    /**
     * Parse date string from day plan
     */
    private fun parseDate(dayString: String): LocalDate? {
        return try {
            when {
                dayString.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) -> LocalDate.parse(dayString)
                dayString.matches(Regex("\\d{2}/\\d{2}/\\d{4}")) -> {
                    val parts = dayString.split("/")
                    LocalDate.of(parts[2].toInt(), parts[1].toInt(), parts[0].toInt())
                }
                dayString.matches(Regex("\\d{2}-\\d{2}-\\d{4}")) -> {
                    val parts = dayString.split("-")
                    LocalDate.of(parts[2].toInt(), parts[1].toInt(), parts[0].toInt())
                }
                // Try to parse day names relative to current week
                dayString.lowercase() in listOf("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday") -> {
                    val targetDayOfWeek = java.time.DayOfWeek.valueOf(dayString.uppercase())
                    val today = LocalDate.now()
                    val daysUntilTarget = targetDayOfWeek.value - today.dayOfWeek.value
                    val adjustedDays = if (daysUntilTarget < 0) daysUntilTarget + 7 else daysUntilTarget
                    today.plusDays(adjustedDays.toLong())
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Generate unique event ID
     */
    private fun generateEventId(date: LocalDate, dayPlan: DayPlan): String {
        val taskIds = dayPlan.tasks.joinToString(",") { it.id }
        val hash = taskIds.hashCode()
        return "${date}_${hash}"
    }

    /**
     * Save ICS bytes to temporary file
     */
    private fun saveIcsToFile(context: Context, bytes: ByteArray, filename: String): File {
        val cacheDir = File(context.cacheDir, "calendar_exports")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }

        val file = File(cacheDir, filename)
        FileOutputStream(file).use { outputStream ->
            outputStream.write(bytes)
            outputStream.flush()
        }

        return file
    }

    /**
     * Share ICS file using Android's share intent
     */
    private fun shareIcsFile(context: Context, file: File, filename: String) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/calendar"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Study Schedule")
                putExtra(Intent.EXTRA_TEXT, "Study schedule exported from StudyPlan app")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooserIntent = Intent.createChooser(shareIntent, "Share Study Schedule")
            if (chooserIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(chooserIntent)
            } else {
                throw IllegalStateException("No apps available to handle ICS files")
            }
        } catch (e: Exception) {
            throw IcsExportException("Failed to share ICS file: ${e.message}", e)
        }
    }

    /**
     * Escape text for ICS format (RFC-5545)
     */
    private fun escapeIcsText(text: String): String {
        return text
            .replace("\\", "\\\\")    // Escape backslashes first
            .replace(";", "\\;")       // Escape semicolons
            .replace(",", "\\,")       // Escape commas
            .replace("\n", "\\n")      // Escape newlines
            .replace("\r", "\\r")      // Escape carriage returns
            .take(200)                 // Limit length to avoid issues
    }

    /**
     * Format timezone offset for ICS
     */
    private fun formatTimeZoneOffset(offsetMillis: Int): String {
        val offsetMinutes = offsetMillis / (1000 * 60)
        val hours = offsetMinutes / 60
        val minutes = kotlin.math.abs(offsetMinutes % 60)
        
        val sign = if (offsetMillis >= 0) "+" else "-"
        return String.format("%s%02d%02d", sign, kotlin.math.abs(hours), minutes)
    }

    /**
     * Generate default filename for ICS export
     */
    fun generateIcsFilename(prefix: String = "study-schedule"): String {
        val date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        return "$prefix-$date.ics"
    }

    /**
     * Validate ICS content for basic correctness
     */
    fun validateIcsContent(content: String): IcsValidationResult {
        val errors = mutableListOf<String>()
        
        if (!content.contains("BEGIN:VCALENDAR")) {
            errors.add("Missing VCALENDAR begin")
        }
        
        if (!content.contains("END:VCALENDAR")) {
            errors.add("Missing VCALENDAR end")
        }
        
        if (!content.contains("VERSION:")) {
            errors.add("Missing VERSION property")
        }
        
        if (!content.contains("PRODID:")) {
            errors.add("Missing PRODID property")
        }
        
        val beginEvents = content.split("BEGIN:VEVENT").size - 1
        val endEvents = content.split("END:VEVENT").size - 1
        
        if (beginEvents != endEvents) {
            errors.add("Mismatched VEVENT begin/end count")
        }
        
        return IcsValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            eventCount = beginEvents
        )
    }
}

/**
 * Exception thrown during ICS export operations
 */
class IcsExportException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Result of ICS validation
 */
data class IcsValidationResult(
    val isValid: Boolean,
    val errors: List<String>,
    val eventCount: Int
) {
    val errorMessage: String
        get() = errors.joinToString("; ")
}

/**
 * Utilities for working with ICS files
 */
object IcsUtils {
    
    /**
     * Check if device can handle ICS files
     */
    fun canHandleIcsFiles(context: Context): Boolean {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            type = "text/calendar"
        }
        return intent.resolveActivity(context.packageManager) != null
    }
    
    /**
     * Get list of apps that can handle ICS files
     */
    fun getIcsHandlerApps(context: Context): List<String> {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            type = "text/calendar"
        }
        
        val packageManager = context.packageManager
        val resolveInfos = packageManager.queryIntentActivities(intent, 0)
        
        return resolveInfos.map { resolveInfo ->
            resolveInfo.loadLabel(packageManager).toString()
        }.distinct().sorted()
    }
    
    /**
     * Create Intent for viewing ICS file
     */
    fun createViewIntent(context: Context, file: File): Intent {
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)
        
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "text/calendar")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    
    /**
     * Estimate ICS file size
     */
    fun estimateIcsSize(eventCount: Int): Long {
        // Rough estimate: ~500 bytes per event + ~200 bytes overhead
        return (eventCount * 500L) + 200L
    }
}