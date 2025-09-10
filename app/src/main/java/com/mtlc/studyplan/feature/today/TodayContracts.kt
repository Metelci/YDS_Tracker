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


object FakeTodayData {
val sessions = listOf(
SessionUi(
id = "s1",
title = "Reading Sprint (20Q)",
section = "Reading",
estMinutes = 25,
difficulty = 3
),
SessionUi(
id = "s2",
title = "Grammar Pack (Tenses)",
section = "Grammar",
estMinutes = 20,
difficulty = 2
),
SessionUi(
id = "s3",
title = "Vocabulary (10 words)",
section = "Vocab",
estMinutes = 15,
difficulty = 2
)
)
}
