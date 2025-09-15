package com.mtlc.studyplan.calendar

import android.Manifest
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.mtlc.studyplan.data.DayPlan
import com.mtlc.studyplan.data.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.*

/**
 * Interface for calendar operations (for testing abstraction)
 */
interface CalendarProvider {
    fun hasCalendarPermissions(): Boolean
    fun getAvailableCalendars(): List<CalendarInfo>
    fun insertEvent(calendarId: Long, event: CalendarEventData): Long?
    fun updateEvent(eventId: Long, event: CalendarEventData): Boolean
    fun deleteEvent(eventId: Long): Boolean
    fun getEventById(eventId: Long): CalendarEventData?
}

/**
 * Calendar event data for operations
 */
data class CalendarEventData(
    val calendarId: Long,
    val title: String,
    val description: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val allDay: Boolean = false,
    val reminderMinutes: Int = 0,
    val timeZone: String = TimeZone.getDefault().id,
    val uid: String? = null // Our app identifier
)

/**
 * Real implementation of CalendarProvider using CalendarContract
 */
class CalendarContractProvider(private val context: Context) : CalendarProvider {
    
    companion object {
        private const val APP_UID_PREFIX = "studyplan_"
    }

    override fun hasCalendarPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED &&
               ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED
    }

    override fun getAvailableCalendars(): List<CalendarInfo> {
        if (!hasCalendarPermissions()) return emptyList()

        val calendars = mutableListOf<CalendarInfo>()
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
            CalendarContract.Calendars.CALENDAR_COLOR
        )

        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            "${CalendarContract.Calendars.VISIBLE} = 1 AND ${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ${CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR}",
            null,
            "${CalendarContract.Calendars.IS_PRIMARY} DESC, ${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} ASC"
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                calendars.add(
                    CalendarInfo(
                        id = cursor.getLong(0),
                        displayName = cursor.getString(1) ?: "",
                        accountName = cursor.getString(2) ?: "",
                        accountType = cursor.getString(3) ?: "",
                        isPrimary = cursor.getInt(4) == 1,
                        accessLevel = cursor.getInt(5),
                        color = cursor.getInt(6)
                    )
                )
            }
        }

        return calendars
    }

    override fun insertEvent(calendarId: Long, event: CalendarEventData): Long? {
        if (!hasCalendarPermissions()) return null

        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, event.title)
            put(CalendarContract.Events.DESCRIPTION, event.description)
            put(CalendarContract.Events.DTSTART, event.startTimeMillis)
            put(CalendarContract.Events.DTEND, event.endTimeMillis)
            put(CalendarContract.Events.EVENT_TIMEZONE, event.timeZone)
            put(CalendarContract.Events.ALL_DAY, if (event.allDay) 1 else 0)
            put(CalendarContract.Events.HAS_ALARM, if (event.reminderMinutes > 0) 1 else 0)
            event.uid?.let { put(CalendarContract.Events.UID_2445, it) }
        }

        return try {
            val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            val eventId = uri?.let { ContentUris.parseId(it) }
            
            // Add reminder if specified
            if (eventId != null && event.reminderMinutes > 0) {
                addEventReminder(eventId, event.reminderMinutes)
            }
            
            eventId
        } catch (e: Exception) {
            null
        }
    }

    override fun updateEvent(eventId: Long, event: CalendarEventData): Boolean {
        if (!hasCalendarPermissions()) return false

        val values = ContentValues().apply {
            put(CalendarContract.Events.TITLE, event.title)
            put(CalendarContract.Events.DESCRIPTION, event.description)
            put(CalendarContract.Events.DTSTART, event.startTimeMillis)
            put(CalendarContract.Events.DTEND, event.endTimeMillis)
            put(CalendarContract.Events.EVENT_TIMEZONE, event.timeZone)
            put(CalendarContract.Events.ALL_DAY, if (event.allDay) 1 else 0)
            put(CalendarContract.Events.HAS_ALARM, if (event.reminderMinutes > 0) 1 else 0)
        }

        return try {
            val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
            val rowsUpdated = context.contentResolver.update(uri, values, null, null)
            
            // Update reminder
            if (rowsUpdated > 0) {
                removeEventReminders(eventId)
                if (event.reminderMinutes > 0) {
                    addEventReminder(eventId, event.reminderMinutes)
                }
            }
            
            rowsUpdated > 0
        } catch (e: Exception) {
            false
        }
    }

    override fun deleteEvent(eventId: Long): Boolean {
        if (!hasCalendarPermissions()) return false

        // Safety check: only delete events with our UID
        val existingEvent = getEventById(eventId)
        if (existingEvent?.uid?.startsWith(APP_UID_PREFIX) != true) {
            return false // Don't delete external events
        }

        return try {
            val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
            val rowsDeleted = context.contentResolver.delete(uri, null, null)
            rowsDeleted > 0
        } catch (e: Exception) {
            false
        }
    }

    override fun getEventById(eventId: Long): CalendarEventData? {
        if (!hasCalendarPermissions()) return null

        val projection = arrayOf(
            CalendarContract.Events.CALENDAR_ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.ALL_DAY,
            CalendarContract.Events.EVENT_TIMEZONE,
            CalendarContract.Events.UID_2445
        )

        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        
        return context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                CalendarEventData(
                    calendarId = cursor.getLong(0),
                    title = cursor.getString(1) ?: "",
                    description = cursor.getString(2) ?: "",
                    startTimeMillis = cursor.getLong(3),
                    endTimeMillis = cursor.getLong(4),
                    allDay = cursor.getInt(5) == 1,
                    timeZone = cursor.getString(6) ?: TimeZone.getDefault().id,
                    uid = cursor.getString(7)
                )
            } else null
        }
    }

    private fun addEventReminder(eventId: Long, minutes: Int) {
        val values = ContentValues().apply {
            put(CalendarContract.Reminders.EVENT_ID, eventId)
            put(CalendarContract.Reminders.MINUTES, minutes)
            put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
        }
        
        try {
            context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, values)
        } catch (e: Exception) {
            // Ignore reminder insertion failures
        }
    }

    private fun removeEventReminders(eventId: Long) {
        try {
            context.contentResolver.delete(
                CalendarContract.Reminders.CONTENT_URI,
                "${CalendarContract.Reminders.EVENT_ID} = ?",
                arrayOf(eventId.toString())
            )
        } catch (e: Exception) {
            // Ignore reminder deletion failures
        }
    }
}

/**
 * Main calendar synchronization service
 */
class CalendarSync(
    private val context: Context,
    private val settingsRepository: CalendarSettingsRepository,
    private val calendarProvider: CalendarProvider = CalendarContractProvider(context)
) {

    companion object {
        private const val APP_UID_PREFIX = "studyplan_"
        private const val WEEKS_AHEAD = 4
    }

    /**
     * Synchronize the next 4 weeks of study sessions
     */
    suspend fun upsertNext4Weeks(dayPlans: List<DayPlan>): CalendarSyncResult = withContext(Dispatchers.IO) {
        val prefs = settingsRepository.calendarPrefsFlow.first()
        
        if (!prefs.enabled) {
            return@withContext CalendarSyncResult.Success
        }
        
        if (!calendarProvider.hasCalendarPermissions()) {
            return@withContext CalendarSyncResult.NoPermission
        }

        val targetCalendarId = prefs.targetCalendarId
            ?: return@withContext CalendarSyncResult.NoCalendarSelected

        try {
            val studySlots = generateStudySlotsFromPlans(dayPlans, prefs)
            val syncResults = mutableListOf<Boolean>()
            
            for (slot in studySlots) {
                val result = upsertStudySlot(slot, targetCalendarId, prefs)
                syncResults.add(result)
            }
            
            val successCount = syncResults.count { it }
            val failedCount = syncResults.count { !it }
            
            // Update sync statistics
            settingsRepository.updateSyncStats(System.currentTimeMillis(), successCount)
            
            when {
                failedCount == 0 -> CalendarSyncResult.Success
                successCount > 0 -> CalendarSyncResult.PartialSuccess(successCount, failedCount)
                else -> CalendarSyncResult.Error("Failed to sync any events")
            }
            
        } catch (e: Exception) {
            CalendarSyncResult.Error("Sync failed: ${e.message}", e)
        }
    }

    /**
     * Get available calendars for user selection
     */
    suspend fun getAvailableCalendars(): List<CalendarInfo> = withContext(Dispatchers.IO) {
        calendarProvider.getAvailableCalendars()
    }

    /**
     * Check calendar permissions
     */
    fun hasPermissions(): Boolean = calendarProvider.hasCalendarPermissions()

    /**
     * Generate study time slots from day plans
     */
    private fun generateStudySlotsFromPlans(
        dayPlans: List<DayPlan>,
        prefs: CalendarPrefs
    ): List<StudyTimeSlot> {
        val slots = mutableListOf<StudyTimeSlot>()
        val today = LocalDate.now()
        val endDate = today.plusWeeks(WEEKS_AHEAD.toLong())

        // Filter plans to next 4 weeks
        val relevantPlans = dayPlans.filter { plan ->
            val planDate = parseDate(plan.day)
            planDate != null && !planDate.isBefore(today) && !planDate.isAfter(endDate)
        }

        for (plan in relevantPlans) {
            val planDate = parseDate(plan.day) ?: continue
            val slot = generateStudySlotForDay(plan, planDate, prefs)
            if (slot != null) {
                slots.add(slot)
            }
        }

        return slots
    }

    /**
     * Generate a study slot for a specific day
     */
    private fun generateStudySlotForDay(
        dayPlan: DayPlan,
        date: LocalDate,
        prefs: CalendarPrefs
    ): StudyTimeSlot? {
        if (dayPlan.tasks.isEmpty()) return null

        // Calculate total duration from tasks or use default
        val totalDuration = dayPlan.tasks.sumOf { estimateTaskDuration(it) }
            .takeIf { it > 0 } ?: prefs.defaultDurationMin

        // Generate title from first task or generic title
        val firstTask = dayPlan.tasks.firstOrNull()
        val title = if (firstTask != null) {
            CalendarSettingsUtils.generateTitle(prefs.titleTemplate, firstTask.desc)
        } else {
            CalendarSettingsUtils.generateTitle(prefs.titleTemplate, "Study Session")
        }

        // Calculate optimal time slot
        val optimalSlot = CalendarSettingsUtils.calculateOptimalTimeSlot(
            date = date,
            quietHours = prefs.quietHours,
            durationMinutes = totalDuration
        ) ?: return null

        // Generate description
        val description = buildString {
            appendLine("Study Plan - ${date}")
            if (dayPlan.tasks.size == 1) {
                appendLine("Task: ${firstTask?.desc}")
                firstTask?.details?.let { appendLine("Details: $it") }
            } else {
                appendLine("Tasks (${dayPlan.tasks.size}):")
                dayPlan.tasks.take(3).forEach { task ->
                    appendLine("• ${task.desc}")
                }
                if (dayPlan.tasks.size > 3) {
                    appendLine("• ... and ${dayPlan.tasks.size - 3} more")
                }
            }
            appendLine()
            appendLine("Generated by StudyPlan App")
        }

        return StudyTimeSlot(
            date = date,
            startTime = optimalSlot.startTime,
            durationMinutes = totalDuration,
            title = title,
            description = description,
            studyEventId = generateStudyEventId(date, dayPlan)
        )
    }

    /**
     * Upsert a single study slot
     */
    private suspend fun upsertStudySlot(
        slot: StudyTimeSlot,
        calendarId: Long,
        prefs: CalendarPrefs
    ): Boolean {
        val (startMillis, endMillis) = slot.toEpochMillis()
        
        val eventData = CalendarEventData(
            calendarId = calendarId,
            title = slot.title,
            description = slot.description,
            startTimeMillis = startMillis,
            endTimeMillis = endMillis,
            reminderMinutes = prefs.remindersMinBefore,
            uid = "$APP_UID_PREFIX${slot.studyEventId}"
        )

        // Check if event already exists
        val existingEventId = settingsRepository.getCalendarEventId(slot.studyEventId)
        
        return if (existingEventId != null) {
            // Update existing event
            val success = calendarProvider.updateEvent(existingEventId, eventData)
            if (!success) {
                // If update failed, try to create new event
                val newEventId = calendarProvider.insertEvent(calendarId, eventData)
                if (newEventId != null) {
                    settingsRepository.addEventMapping(slot.studyEventId, newEventId)
                    true
                } else false
            } else true
        } else {
            // Create new event
            val newEventId = calendarProvider.insertEvent(calendarId, eventData)
            if (newEventId != null) {
                settingsRepository.addEventMapping(slot.studyEventId, newEventId)
                true
            } else false
        }
    }

    /**
     * Estimate task duration in minutes
     */
    private fun estimateTaskDuration(task: Task): Int {
        // Simple heuristic based on task description keywords
        val description = task.desc.lowercase() + " " + (task.details?.lowercase() ?: "")
        
        return when {
            description.contains("exam") || description.contains("test") -> 90
            description.contains("reading") -> 60
            description.contains("listening") -> 45
            description.contains("practice") -> 30
            description.contains("vocabulary") || description.contains("vocab") -> 20
            description.contains("grammar") -> 40
            else -> 30
        }
    }

    /**
     * Parse date string from day plan
     */
    private fun parseDate(dayString: String): LocalDate? {
        return try {
            // Handle various date formats
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
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Generate unique study event ID
     */
    private fun generateStudyEventId(date: LocalDate, dayPlan: DayPlan): String {
        val taskHash = dayPlan.tasks.joinToString(",") { it.id }.hashCode()
        return "${date}_${taskHash}"
    }

    /**
     * Remove all synced events (called when disabling sync)
     */
    suspend fun removeAllSyncedEvents(): CalendarSyncResult = withContext(Dispatchers.IO) {
        if (!calendarProvider.hasCalendarPermissions()) {
            return@withContext CalendarSyncResult.NoPermission
        }

        try {
            val mappings = settingsRepository.eventMappingsFlow.first()
            var successCount = 0
            var failedCount = 0

            for (mapping in mappings) {
                if (calendarProvider.deleteEvent(mapping.calendarEventId)) {
                    settingsRepository.removeEventMapping(mapping.studyEventId)
                    successCount++
                } else {
                    failedCount++
                }
            }

            when {
                failedCount == 0 -> CalendarSyncResult.Success
                successCount > 0 -> CalendarSyncResult.PartialSuccess(successCount, failedCount)
                else -> CalendarSyncResult.Error("Failed to remove any events")
            }
        } catch (e: Exception) {
            CalendarSyncResult.Error("Failed to remove events: ${e.message}", e)
        }
    }
}