package com.mtlc.studyplan.eventbus

import com.mtlc.studyplan.offline.SyncResult
import com.mtlc.studyplan.settings.data.UserSettings
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class AppEventBus {

    private val _events = MutableSharedFlow<AppEvent>()
    val events: SharedFlow<AppEvent> = _events.asSharedFlow()

    suspend fun emitEvent(event: AppEvent) {
        _events.emit(event)
    }

    fun observeEvents(): SharedFlow<AppEvent> = events
}

sealed class AppEvent {
    data class SettingsUpdated(val settings: UserSettings) : AppEvent()
    data class TaskCompleted(val taskId: String) : AppEvent()
    data class TaskCreated(val taskId: String? = null) : AppEvent()
    data class AchievementUnlocked(val achievement: Any) : AppEvent()
    data class StreakUpdated(val newStreak: Int) : AppEvent()
    data object ProgressUpdated : AppEvent()
    data object NetworkConnected : AppEvent()
    data object NetworkDisconnected : AppEvent()
    data class SyncCompleted(val result: SyncResult) : AppEvent()
}
