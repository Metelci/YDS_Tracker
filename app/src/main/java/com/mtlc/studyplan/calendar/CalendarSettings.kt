package com.mtlc.studyplan.calendar

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Calendar synchronization preferences
 */
data class CalendarPrefs(
    val enabled: Boolean = false,
    val targetCalendarId: Long? = null, // user-selected calendar
    val titleTemplate: String = "Study: %s",
    val defaultDurationMin: Int = 45,
    val remindersMinBefore: Int = 10,
    val quietHours: IntRange? = null, // e.g., 22..7 (10 PM to 7 AM)
    val lastSyncTime: Long = 0L,
    val syncedEventCount: Int = 0,
    val googleSyncEnabled: Boolean = false,
    val outlookSyncEnabled: Boolean = false,
    val includePastEvents: Boolean = false,
    val includeReminders: Boolean = true,
    val includedExamCategories: Set<String> = emptySet(),
    val icsFeedToken: String? = null,
    val lastIcsGeneratedAt: Long = 0L
)

/**
 * Calendar event mapping for sync tracking
 */
data class CalendarEventMapping(
    val studyEventId: String,
    val calendarEventId: Long,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Repository for calendar settings and event mappings
 */
val Context.calendarDataStore: DataStore<Preferences> by preferencesDataStore(name = "calendar_settings")

class CalendarSettingsRepository(private val dataStore: DataStore<Preferences>) {

    private object PrefsKeys {
        val ENABLED = booleanPreferencesKey("calendar_enabled")
        val TARGET_CALENDAR_ID = longPreferencesKey("target_calendar_id")
        val TITLE_TEMPLATE = stringPreferencesKey("title_template")
        val DEFAULT_DURATION_MIN = intPreferencesKey("default_duration_min")
        val REMINDERS_MIN_BEFORE = intPreferencesKey("reminders_min_before")
        val QUIET_HOURS_START = intPreferencesKey("quiet_hours_start")
        val QUIET_HOURS_END = intPreferencesKey("quiet_hours_end")
        val LAST_SYNC_TIME = longPreferencesKey("last_sync_time")
        val SYNCED_EVENT_COUNT = intPreferencesKey("synced_event_count")
        val EVENT_MAPPINGS = stringSetPreferencesKey("event_mappings")
        val GOOGLE_ENABLED = booleanPreferencesKey("calendar_google_enabled")
        val OUTLOOK_ENABLED = booleanPreferencesKey("calendar_outlook_enabled")
        val INCLUDE_PAST = booleanPreferencesKey("calendar_include_past")
        val INCLUDE_REMINDERS = booleanPreferencesKey("calendar_include_reminders")
        val INCLUDED_EXAM_TYPES = stringSetPreferencesKey("calendar_included_exam_types")
        val ICS_TOKEN = stringPreferencesKey("calendar_ics_token")
        val ICS_UPDATED_AT = longPreferencesKey("calendar_ics_updated_at")
    }

    val calendarPrefsFlow: Flow<CalendarPrefs> = dataStore.data.map { preferences ->
        val quietStart = preferences[PrefsKeys.QUIET_HOURS_START]
        val quietEnd = preferences[PrefsKeys.QUIET_HOURS_END]
        val quietHours = if (quietStart != null && quietEnd != null) {
            quietStart..quietEnd
        } else null

        CalendarPrefs(
            enabled = preferences[PrefsKeys.ENABLED] ?: false,
            targetCalendarId = preferences[PrefsKeys.TARGET_CALENDAR_ID],
            titleTemplate = preferences[PrefsKeys.TITLE_TEMPLATE] ?: "Study: %s",
            defaultDurationMin = preferences[PrefsKeys.DEFAULT_DURATION_MIN] ?: 45,
            remindersMinBefore = preferences[PrefsKeys.REMINDERS_MIN_BEFORE] ?: 10,
            quietHours = quietHours,
            lastSyncTime = preferences[PrefsKeys.LAST_SYNC_TIME] ?: 0L,
            syncedEventCount = preferences[PrefsKeys.SYNCED_EVENT_COUNT] ?: 0,
            googleSyncEnabled = preferences[PrefsKeys.GOOGLE_ENABLED] ?: false,
            outlookSyncEnabled = preferences[PrefsKeys.OUTLOOK_ENABLED] ?: false,
            includePastEvents = preferences[PrefsKeys.INCLUDE_PAST] ?: false,
            includeReminders = preferences[PrefsKeys.INCLUDE_REMINDERS] ?: true,
            includedExamCategories = preferences[PrefsKeys.INCLUDED_EXAM_TYPES] ?: emptySet(),
            icsFeedToken = preferences[PrefsKeys.ICS_TOKEN],
            lastIcsGeneratedAt = preferences[PrefsKeys.ICS_UPDATED_AT] ?: 0L
        )
    }

    suspend fun updateCalendarPrefs(prefs: CalendarPrefs) {
        dataStore.edit { preferences ->
            preferences[PrefsKeys.ENABLED] = prefs.enabled
            prefs.targetCalendarId?.let { preferences[PrefsKeys.TARGET_CALENDAR_ID] = it }
            preferences[PrefsKeys.TITLE_TEMPLATE] = prefs.titleTemplate
            preferences[PrefsKeys.DEFAULT_DURATION_MIN] = prefs.defaultDurationMin
            preferences[PrefsKeys.REMINDERS_MIN_BEFORE] = prefs.remindersMinBefore
            prefs.quietHours?.let { range ->
                preferences[PrefsKeys.QUIET_HOURS_START] = range.first
                preferences[PrefsKeys.QUIET_HOURS_END] = range.last
            }
            preferences[PrefsKeys.LAST_SYNC_TIME] = prefs.lastSyncTime
            preferences[PrefsKeys.SYNCED_EVENT_COUNT] = prefs.syncedEventCount
            preferences[PrefsKeys.GOOGLE_ENABLED] = prefs.googleSyncEnabled
            preferences[PrefsKeys.INCLUDE_PAST] = prefs.includePastEvents
            preferences[PrefsKeys.INCLUDE_REMINDERS] = prefs.includeReminders
            preferences[PrefsKeys.INCLUDED_EXAM_TYPES] = prefs.includedExamCategories
            prefs.icsFeedToken?.let { token ->
                preferences[PrefsKeys.ICS_TOKEN] = token
            } ?: run {
                preferences.remove(PrefsKeys.ICS_TOKEN)
            }
            preferences[PrefsKeys.ICS_UPDATED_AT] = prefs.lastIcsGeneratedAt
        }
    }

    suspend fun setCalendarEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PrefsKeys.ENABLED] = enabled
        }
    }

    suspend fun setTargetCalendar(calendarId: Long) {
        dataStore.edit { preferences ->
            preferences[PrefsKeys.TARGET_CALENDAR_ID] = calendarId
        }
    }

    suspend fun updateSyncStats(syncTime: Long, eventCount: Int) {
        dataStore.edit { preferences ->
            preferences[PrefsKeys.LAST_SYNC_TIME] = syncTime
            preferences[PrefsKeys.SYNCED_EVENT_COUNT] = eventCount
        }
    }

    suspend fun setGoogleSyncEnabled(enabled: Boolean) {

    suspend fun setOutlookSyncEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PrefsKeys.OUTLOOK_ENABLED] = enabled
        }
    }

        dataStore.edit { preferences ->
            preferences[PrefsKeys.GOOGLE_ENABLED] = enabled
        }
    }

    suspend fun setIncludePastEvents(include: Boolean) {
        dataStore.edit { preferences ->
            preferences[PrefsKeys.INCLUDE_PAST] = include
        }
    }

    suspend fun setIncludeReminders(include: Boolean) {
        dataStore.edit { preferences ->
            preferences[PrefsKeys.INCLUDE_REMINDERS] = include
        }
    }

    suspend fun setIncludedExamCategories(categories: Set<String>) {
        dataStore.edit { preferences ->
            preferences[PrefsKeys.INCLUDED_EXAM_TYPES] = categories
        }
    }

    suspend fun updateIcsFeedMetadata(token: String, generatedAt: Long = System.currentTimeMillis()) {
        dataStore.edit { preferences ->
            preferences[PrefsKeys.ICS_TOKEN] = token
            preferences[PrefsKeys.ICS_UPDATED_AT] = generatedAt
        }
    }

    suspend fun clearIcsFeedMetadata() {
        dataStore.edit { preferences ->
            preferences.remove(PrefsKeys.ICS_TOKEN)
            preferences[PrefsKeys.ICS_UPDATED_AT] = 0L
        }
    }

    // Event mapping management
    val eventMappingsFlow: Flow<List<CalendarEventMapping>> = dataStore.data.map { preferences ->
        val mappingStrings = preferences[PrefsKeys.EVENT_MAPPINGS] ?: emptySet()
        mappingStrings.mapNotNull { decodeEventMapping(it) }
    }

    suspend fun addEventMapping(studyEventId: String, calendarEventId: Long) {
        dataStore.edit { preferences ->
            val currentMappings = preferences[PrefsKeys.EVENT_MAPPINGS] ?: emptySet()
            val newMapping = encodeEventMapping(CalendarEventMapping(studyEventId, calendarEventId))
            preferences[PrefsKeys.EVENT_MAPPINGS] = currentMappings + newMapping
        }
    }

    suspend fun removeEventMapping(studyEventId: String) {
        dataStore.edit { preferences ->
            val currentMappings = preferences[PrefsKeys.EVENT_MAPPINGS] ?: emptySet()
            val filteredMappings = currentMappings.filter { mapping ->
                val decoded = decodeEventMapping(mapping)
                decoded?.studyEventId != studyEventId
            }.toSet()
            preferences[PrefsKeys.EVENT_MAPPINGS] = filteredMappings
        }
    }

    suspend fun getCalendarEventId(studyEventId: String): Long? {
        val mappings = eventMappingsFlow.map { mappings ->
            mappings.find { it.studyEventId == studyEventId }?.calendarEventId
        }
        return try {
            mappings.first()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun clearEventMappings() {
        dataStore.edit { preferences ->
            preferences.remove(PrefsKeys.EVENT_MAPPINGS)
        }
    }

    private fun encodeEventMapping(mapping: CalendarEventMapping): String {
        return "${mapping.studyEventId}|${mapping.calendarEventId}|${mapping.createdAt}"
    }

    private fun decodeEventMapping(encoded: String): CalendarEventMapping? {
        return try {
            val parts = encoded.split("|")
            if (parts.size >= 3) {
                CalendarEventMapping(
                    studyEventId = parts[0],
                    calendarEventId = parts[1].toLong(),
                    createdAt = parts[2].toLong()
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Calendar information for display in settings
 */
data class CalendarInfo(
    val id: Long,
    val displayName: String,
    val accountName: String,
    val accountType: String,
    val isPrimary: Boolean = false,
    val accessLevel: Int = 0,
    val color: Int = 0
) {
    val isWritable: Boolean
        get() = accessLevel >= 500 // ACCESS_CONTRIBUTOR or higher

    val description: String
        get() = if (accountName.isNotEmpty()) "$displayName ($accountName)" else displayName
}

/**
 * Time slot for calendar events
 */
data class StudyTimeSlot(
    val date: java.time.LocalDate,
    val startTime: java.time.LocalTime,
    val durationMinutes: Int,
    val title: String,
    val description: String = "",
    val studyEventId: String = "${date}_${startTime}"
) {
    val endTime: java.time.LocalTime
        get() = startTime.plusMinutes(durationMinutes.toLong())

    val startDateTime: java.time.LocalDateTime
        get() = java.time.LocalDateTime.of(date, startTime)

    val endDateTime: java.time.LocalDateTime
        get() = java.time.LocalDateTime.of(date, endTime)

    fun toEpochMillis(): Pair<Long, Long> {
        val zoneId = java.time.ZoneId.systemDefault()
        val startMillis = startDateTime.atZone(zoneId).toInstant().toEpochMilli()
        val endMillis = endDateTime.atZone(zoneId).toInstant().toEpochMilli()
        return startMillis to endMillis
    }
}

/**
 * Calendar sync result
 */
sealed class CalendarSyncResult {
    object Success : CalendarSyncResult()
    data class PartialSuccess(val syncedCount: Int, val failedCount: Int) : CalendarSyncResult()
    data class Error(val message: String, val cause: Throwable? = null) : CalendarSyncResult()
    object NoPermission : CalendarSyncResult()
    object NoCalendarSelected : CalendarSyncResult()
    object NoCalendarsAvailable : CalendarSyncResult()
}

/**
 * Utilities for working with calendar settings
 */
object CalendarSettingsUtils {

    /**
     * Check if a time is within quiet hours
     */
    fun isWithinQuietHours(time: java.time.LocalTime, quietHours: IntRange?): Boolean {
        if (quietHours == null) return false
        
        val hour = time.hour
        return if (quietHours.first <= quietHours.last) {
            // Normal range (e.g., 22..23 for 10 PM to 11 PM)
            hour in quietHours
        } else {
            // Overnight range (e.g., 22..7 for 10 PM to 7 AM)
            hour >= quietHours.first || hour <= quietHours.last
        }
    }

    /**
     * Adjust time to avoid quiet hours
     */
    fun adjustForQuietHours(
        time: java.time.LocalTime, 
        quietHours: IntRange?
    ): java.time.LocalTime {
        if (quietHours == null || !isWithinQuietHours(time, quietHours)) {
            return time
        }

        // Move to the end of quiet hours
        val adjustedHour = if (quietHours.first <= quietHours.last) {
            // Normal range - move to hour after end
            (quietHours.last + 1).coerceAtMost(23)
        } else {
            // Overnight range - move to hour after end (next day)
            (quietHours.last + 1).coerceAtMost(23)
        }

        return java.time.LocalTime.of(adjustedHour, 0)
    }

    /**
     * Generate study event title from template
     */
    fun generateTitle(template: String, taskDescription: String): String {
        return if (template.contains("%s")) {
            template.format(taskDescription.take(50))
        } else {
            "$template $taskDescription".take(100)
        }
    }

    /**
     * Calculate optimal study slot from user availability
     */
    fun calculateOptimalTimeSlot(
        date: java.time.LocalDate,
        availabilityStart: java.time.LocalTime = java.time.LocalTime.of(9, 0),
        availabilityEnd: java.time.LocalTime = java.time.LocalTime.of(17, 0),
        quietHours: IntRange? = null,
        durationMinutes: Int = 45
    ): StudyTimeSlot? {
        var currentTime = availabilityStart

        while (currentTime.plusMinutes(durationMinutes.toLong()) <= availabilityEnd) {
            val adjustedTime = adjustForQuietHours(currentTime, quietHours)
            
            if (adjustedTime.plusMinutes(durationMinutes.toLong()) <= availabilityEnd) {
                return StudyTimeSlot(
                    date = date,
                    startTime = adjustedTime,
                    durationMinutes = durationMinutes,
                    title = "Study Session"
                )
            }
            
            currentTime = currentTime.plusMinutes(30) // Try 30-minute increments
        }

        return null // No suitable slot found
    }
}
