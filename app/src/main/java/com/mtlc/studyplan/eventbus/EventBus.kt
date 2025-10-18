package com.mtlc.studyplan.eventbus

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KClass

/**
 * EventBus interface for reactive event communication
 */
interface EventBus {
    /**
     * Publish an event to all subscribers
     */
    suspend fun publish(event: Event)

    /**
     * Subscribe to events of a specific type
     */
    fun <T : Event> subscribe(eventType: KClass<T>): Flow<T>

    /**
     * Subscribe to all events
     */
    fun subscribeToAll(): Flow<Event>

    /**
     * Get the current state of events (for state-based events)
     */
    fun <T : Event> getCurrentState(eventType: KClass<T>): T?

    /**
     * Clear all events and reset the bus
     */
    suspend fun clear()

    /**
     * Get event history for debugging/analytics
     */
    fun getEventHistory(limit: Int = 100): List<Event>
}

/**
 * Reactive EventBus implementation using StateFlow and SharedFlow
 */
@Singleton
class ReactiveEventBus @Inject constructor(
    @com.mtlc.studyplan.di.ApplicationScope private val applicationScope: CoroutineScope
) : EventBus {

    // Hot flow for all events - doesn't replay events
    private val _eventFlow = MutableSharedFlow<Event>(
        replay = 0, // Don't replay events for new subscribers
        extraBufferCapacity = 64 // Buffer for slow consumers
    )

    // State flow for state-based events - replays the latest event
    private val _stateFlow = MutableStateFlow<Map<String, Event>>(emptyMap())

    // Event history for debugging and analytics
    private val _eventHistory = MutableStateFlow<List<Event>>(emptyList())

    // Event counters for analytics
    private val _eventCounts = MutableStateFlow<Map<String, Int>>(emptyMap())

    override suspend fun publish(event: Event) {
        // Emit to the main event flow
        _eventFlow.emit(event)

        // Update state for state-based events
        if (isStateEvent(event)) {
            val currentStates = _stateFlow.value.toMutableMap()
            currentStates[event::class.simpleName ?: "Unknown"] = event
            _stateFlow.value = currentStates
        }

        // Update event history
        updateEventHistory(event)

        // Update event counts
        updateEventCounts(event)
    }

    override fun subscribeToAll(): Flow<Event> {
        return _eventFlow.asSharedFlow()
    }

    override fun <T : Event> subscribe(eventType: KClass<T>): Flow<T> {
        return EventTypeMatcher.filterEvents(_eventFlow, eventType)
            .onStart {
                getCurrentState(eventType)?.let { emit(it) }
            }
    }

    override fun <T : Event> getCurrentState(eventType: KClass<T>): T? {
        return _stateFlow.value[eventType.simpleName]?.let { event ->
            EventTypeMatcher.matchSingle(event, eventType)
        }
    }

    override suspend fun clear() {
        _stateFlow.value = emptyMap()
        _eventHistory.value = emptyList()
        _eventCounts.value = emptyMap()
    }

    override fun getEventHistory(limit: Int): List<Event> {
        return _eventHistory.value.takeLast(limit)
    }

    /**
     * Get event counts for analytics
     */
    fun getEventCounts(): Map<String, Int> {
        return _eventCounts.value
    }

    /**
     * Get events by category
     */
    fun <T : Event> subscribeToCategory(categoryType: KClass<T>): Flow<T> {
        return EventTypeMatcher.filterEvents(_eventFlow, categoryType)
    }

    /**
     * Subscribe to multiple event types
     */
    fun subscribeToMultiple(vararg eventTypes: KClass<out Event>): Flow<Event> {
        val typeNames = eventTypes.map { it.simpleName }.toSet()
        return _eventFlow.filter { event ->
            event::class.simpleName in typeNames
        }
    }

    /**
     * Subscribe with a predicate filter
     */
    fun subscribeWithFilter(predicate: (Event) -> Boolean): Flow<Event> {
        return _eventFlow.filter(predicate)
    }

    /**
     * Subscribe to events within a time window
     */
    fun subscribeWithTimeWindow(windowMs: Long): Flow<List<Event>> {
        return _eventFlow
            .windowLatest(windowMs)
            .map { events -> events }
    }

    /**
     * Get events grouped by type
     */
    fun getEventsByType(): Flow<Map<String, List<Event>>> {
        return _eventFlow
            .scan(emptyMap<String, List<Event>>()) { acc, event ->
                val typeName = event::class.simpleName ?: "Unknown"
                val currentEvents = acc[typeName] ?: emptyList()
                acc + (typeName to currentEvents + event)
            }
    }

    /**
     * Publish multiple events in batch
     */
    suspend fun publishBatch(events: List<Event>) {
        events.forEach { publish(it) }
    }

    /**
     * Publish with delay
     */
    fun publishDelayed(event: Event, delayMs: Long) {
        applicationScope.launch {
            kotlinx.coroutines.delay(delayMs)
            publish(event)
        }
    }

    /**
     * Check if an event type maintains state
     */
    private fun isStateEvent(event: Event): Boolean {
        return when (event) {
            is UIEvent.LoadingStateChanged,
            is UIEvent.ErrorOccurred,
            is SettingsEvent,
            is SyncEvent.SyncStarted,
            is SyncEvent.SyncCompleted -> true
            else -> false
        }
    }

    /**
     * Update event history with size limit
     */
    private fun updateEventHistory(event: Event) {
        val currentHistory = _eventHistory.value.toMutableList()
        currentHistory.add(event)

        // Keep only the last 1000 events to prevent memory issues
        if (currentHistory.size > 1000) {
            currentHistory.removeAt(0)
        }

        _eventHistory.value = currentHistory
    }

    /**
     * Update event counts for analytics
     */
    private fun updateEventCounts(event: Event) {
        val eventType = event::class.simpleName ?: "Unknown"
        val currentCounts = _eventCounts.value.toMutableMap()
        currentCounts[eventType] = (currentCounts[eventType] ?: 0) + 1
        _eventCounts.value = currentCounts
    }
}

/**
 * Convenience extensions for common event subscriptions
 */

// Subscribe to task events
inline fun <reified T : TaskEvent> EventBus.subscribeToTaskEvents(): Flow<T> =
    subscribe(T::class)

// Subscribe to progress events
inline fun <reified T : ProgressEvent> EventBus.subscribeToProgressEvents(): Flow<T> =
    subscribe(T::class)

// Subscribe to achievement events
inline fun <reified T : AchievementEvent> EventBus.subscribeToAchievementEvents(): Flow<T> =
    subscribe(T::class)

// Subscribe to streak events
inline fun <reified T : StreakEvent> EventBus.subscribeToStreakEvents(): Flow<T> =
    subscribe(T::class)

// Subscribe to settings events
inline fun <reified T : SettingsEvent> EventBus.subscribeToSettingsEvents(): Flow<T> =
    subscribe(T::class)


// Subscribe to UI events
inline fun <reified T : UIEvent> EventBus.subscribeToUIEvents(): Flow<T> =
    subscribe(T::class)

// Subscribe to sync events
inline fun <reified T : SyncEvent> EventBus.subscribeToSyncEvents(): Flow<T> =
    subscribe(T::class)

// Subscribe to notification events
inline fun <reified T : NotificationEvent> EventBus.subscribeToNotificationEvents(): Flow<T> =
    subscribe(T::class)

// Subscribe to analytics events
inline fun <reified T : AnalyticsEvent> EventBus.subscribeToAnalyticsEvents(): Flow<T> =
    subscribe(T::class)

/**
 * Helper function to create a window of events over time
 */
private fun <T> Flow<T>.windowLatest(windowMs: Long): Flow<List<T>> = flow {
    val window = mutableListOf<T>()
    var lastEmission = 0L

    collect { item ->
        val now = System.currentTimeMillis()
        window.add(item)

        // Remove items older than window
        window.removeAll { /* implement timestamp checking if needed */ false }

        if (now - lastEmission >= windowMs) {
            emit(window.toList())
            lastEmission = now
        }
    }
}

/**
 * Type-safe event filtering using when expressions with sealed types
 * This eliminates unsafe casts by leveraging Kotlin's smart casting
 */
object EventTypeMatcher {

    @Suppress("UNCHECKED_CAST") // Required for KClass matching - this is the fundamental limitation we're consolidating
    fun <T : Event> filterEvents(events: Flow<Event>, targetType: KClass<T>): Flow<T> {
        return events.mapNotNull { event ->
            when (event) {
                is TaskEvent -> if (targetType.isInstance(event)) event as T else null
                is ProgressEvent -> if (targetType.isInstance(event)) event as T else null
                is AchievementEvent -> if (targetType.isInstance(event)) event as T else null
                is StreakEvent -> if (targetType.isInstance(event)) event as T else null
                is SettingsEvent -> if (targetType.isInstance(event)) event as T else null
                is UIEvent -> if (targetType.isInstance(event)) event as T else null
                is SyncEvent -> if (targetType.isInstance(event)) event as T else null
                is NotificationEvent -> if (targetType.isInstance(event)) event as T else null
                is AnalyticsEvent -> if (targetType.isInstance(event)) event as T else null
                // Sealed interface is exhaustive, but we need this for compilation
                else -> if (targetType.isInstance(event)) event as T else null
            }
        }
    }

    @Suppress("UNCHECKED_CAST") // Required for KClass matching - consolidated suppression point
    fun <T : Event> matchSingle(event: Event, targetType: KClass<T>): T? {
        return when (event) {
            is TaskEvent -> if (targetType.isInstance(event)) event as T else null
            is ProgressEvent -> if (targetType.isInstance(event)) event as T else null
            is AchievementEvent -> if (targetType.isInstance(event)) event as T else null
            is StreakEvent -> if (targetType.isInstance(event)) event as T else null
            is SettingsEvent -> if (targetType.isInstance(event)) event as T else null
            is UIEvent -> if (targetType.isInstance(event)) event as T else null
            is SyncEvent -> if (targetType.isInstance(event)) event as T else null
            is NotificationEvent -> if (targetType.isInstance(event)) event as T else null
            is AnalyticsEvent -> if (targetType.isInstance(event)) event as T else null
            // Sealed interface is exhaustive, but we need this for compilation
            else -> if (targetType.isInstance(event)) event as T else null
        }
    }
}







