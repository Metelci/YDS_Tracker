package com.mtlc.studyplan.feature.today


import androidx.compose.runtime.Immutable


@Immutable
data class SessionUi(
val id: String,
val title: String,
val section: String, // Reading, Grammar, Vocab, Translation
val estMinutes: Int,
val difficulty: Int, // 1..5
val isCompleted: Boolean = false
)


@Immutable
data class TodayUiState(
val isLoading: Boolean = false,
val sessions: List<SessionUi> = emptyList(),
val snackbar: String? = null
)


sealed interface TodayIntent {
    data object Load : TodayIntent
    data class StartSession(val id: String) : TodayIntent
    data class Complete(val id: String) : TodayIntent
    data class Skip(val id: String) : TodayIntent
    data class Reschedule(val id: String, val at: java.time.LocalDateTime) : TodayIntent
}
