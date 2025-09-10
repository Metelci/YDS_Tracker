package com.mtlc.studyplan.feature.today


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


class TodayViewModel : ViewModel() {
private val _state = MutableStateFlow(TodayUiState(isLoading = true))
val state: StateFlow<TodayUiState> = _state.asStateFlow()


    fun dispatch(intent: TodayIntent) {
        when (intent) {
            is TodayIntent.Load -> load()
            is TodayIntent.StartSession -> start(intent.id)
            is TodayIntent.Complete -> complete(intent.id)
            is TodayIntent.Skip -> skip(intent.id)
            is TodayIntent.Reschedule -> reschedule(intent.id, intent.at)
        }
    }


private fun load() {
viewModelScope.launch {
_state.update { it.copy(isLoading = false, sessions = FakeTodayData.sessions) }
}
}


private fun start(id: String) {
// For starter scaffold, just show a snackbar hint
_state.update { it.copy(snackbar = "Started session $id") }
}


private fun complete(id: String) {
_state.update { s ->
val updated = s.sessions.map { if (it.id == id) it.copy(isCompleted = true) else it }
s.copy(sessions = updated, snackbar = "Completed session $id")
}
}


    private fun skip(id: String) {
        _state.update { it.copy(snackbar = "Skipped session $id") }
    }


    fun consumeSnackbar() { _state.update { it.copy(snackbar = null) } }

    private fun reschedule(id: String, at: java.time.LocalDateTime) {
        _state.update { it.copy(snackbar = "Rescheduled $id to ${at.toLocalDate()} ${at.toLocalTime()}") }
    }
}
