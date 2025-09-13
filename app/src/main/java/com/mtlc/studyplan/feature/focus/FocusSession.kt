package com.mtlc.studyplan.feature.focus

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.LocalDateTime

data class FocusSessionConfig(
    val studyDuration: Int = 25, // minutes
    val shortBreak: Int = 5, // minutes
    val longBreak: Int = 15, // minutes
    val sessionsUntilLongBreak: Int = 4,
    val taskId: String? = null,
    val taskTitle: String = "Focus Session",
    val ambientSounds: Boolean = false,
    val blockNotifications: Boolean = true,
    val enableBreaks: Boolean = true
)

data class FocusSessionState(
    val config: FocusSessionConfig,
    val currentPhase: SessionPhase,
    val timeRemaining: Int, // seconds
    val totalTimeElapsed: Int, // seconds
    val currentSession: Int, // current pomodoro number
    val isRunning: Boolean,
    val isPaused: Boolean,
    val completedSessions: Int,
    val startTime: LocalDateTime? = null,
    val endTime: LocalDateTime? = null
) {
    val progressPercentage: Float
        get() = when (currentPhase) {
            SessionPhase.STUDY -> 1f - (timeRemaining.toFloat() / (config.studyDuration * 60))
            SessionPhase.SHORT_BREAK, SessionPhase.LONG_BREAK -> {
                val breakDuration = if (currentPhase == SessionPhase.SHORT_BREAK) config.shortBreak else config.longBreak
                1f - (timeRemaining.toFloat() / (breakDuration * 60))
            }
            SessionPhase.COMPLETED -> 1f
        }

    val totalSessionTime: Int
        get() = when (currentPhase) {
            SessionPhase.STUDY -> config.studyDuration * 60
            SessionPhase.SHORT_BREAK -> config.shortBreak * 60
            SessionPhase.LONG_BREAK -> config.longBreak * 60
            SessionPhase.COMPLETED -> 0
        }
}

enum class SessionPhase {
    STUDY,
    SHORT_BREAK,
    LONG_BREAK,
    COMPLETED
}

sealed class FocusEvent {
    object Start : FocusEvent()
    object Pause : FocusEvent()
    object Resume : FocusEvent()
    object Stop : FocusEvent()
    object SkipBreak : FocusEvent()
    object NextPhase : FocusEvent()
    data class UpdateConfig(val config: FocusSessionConfig) : FocusEvent()
}

class FocusSessionManager {
    private var timerJob: Job? = null
    private var notificationJob: Job? = null

    private val _state = MutableStateFlow(
        FocusSessionState(
            config = FocusSessionConfig(),
            currentPhase = SessionPhase.STUDY,
            timeRemaining = 25 * 60, // 25 minutes in seconds
            totalTimeElapsed = 0,
            currentSession = 1,
            isRunning = false,
            isPaused = false,
            completedSessions = 0
        )
    )

    val state: StateFlow<FocusSessionState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<FocusSessionEvent>()
    val events: SharedFlow<FocusSessionEvent> = _events.asSharedFlow()

    fun handleEvent(event: FocusEvent) {
        when (event) {
            FocusEvent.Start -> startSession()
            FocusEvent.Pause -> pauseSession()
            FocusEvent.Resume -> resumeSession()
            FocusEvent.Stop -> stopSession()
            FocusEvent.SkipBreak -> skipBreak()
            FocusEvent.NextPhase -> moveToNextPhase()
            is FocusEvent.UpdateConfig -> updateConfig(event.config)
        }
    }

    private fun startSession() {
        val currentState = _state.value
        if (!currentState.isRunning) {
            _state.value = currentState.copy(
                isRunning = true,
                isPaused = false,
                startTime = if (currentState.startTime == null) LocalDateTime.now() else currentState.startTime
            )
            startTimer()
            _events.tryEmit(FocusSessionEvent.SessionStarted)
        }
    }

    private fun pauseSession() {
        _state.value = _state.value.copy(isPaused = true)
        timerJob?.cancel()
        _events.tryEmit(FocusSessionEvent.SessionPaused)
    }

    private fun resumeSession() {
        _state.value = _state.value.copy(isPaused = false)
        startTimer()
        _events.tryEmit(FocusSessionEvent.SessionResumed)
    }

    private fun stopSession() {
        timerJob?.cancel()
        notificationJob?.cancel()
        _state.value = _state.value.copy(
            isRunning = false,
            isPaused = false,
            endTime = LocalDateTime.now()
        )
        _events.tryEmit(FocusSessionEvent.SessionStopped)
    }

    private fun skipBreak() {
        val currentState = _state.value
        if (currentState.currentPhase == SessionPhase.SHORT_BREAK || currentState.currentPhase == SessionPhase.LONG_BREAK) {
            moveToNextPhase()
        }
    }

    private fun moveToNextPhase() {
        val currentState = _state.value
        val config = currentState.config

        when (currentState.currentPhase) {
            SessionPhase.STUDY -> {
                val newCompletedSessions = currentState.completedSessions + 1
                val isLongBreakTime = newCompletedSessions % config.sessionsUntilLongBreak == 0

                if (config.enableBreaks) {
                    val nextPhase = if (isLongBreakTime) SessionPhase.LONG_BREAK else SessionPhase.SHORT_BREAK
                    val breakDuration = if (isLongBreakTime) config.longBreak else config.shortBreak

                    _state.value = currentState.copy(
                        currentPhase = nextPhase,
                        timeRemaining = breakDuration * 60,
                        completedSessions = newCompletedSessions,
                        isRunning = true,
                        isPaused = false
                    )

                    _events.tryEmit(
                        if (isLongBreakTime) FocusSessionEvent.LongBreakStarted
                        else FocusSessionEvent.ShortBreakStarted
                    )
                } else {
                    // Skip break and start next study session
                    _state.value = currentState.copy(
                        currentPhase = SessionPhase.STUDY,
                        timeRemaining = config.studyDuration * 60,
                        currentSession = currentState.currentSession + 1,
                        completedSessions = newCompletedSessions,
                        isRunning = true,
                        isPaused = false
                    )
                    _events.tryEmit(FocusSessionEvent.NextStudySessionStarted)
                }
            }

            SessionPhase.SHORT_BREAK, SessionPhase.LONG_BREAK -> {
                _state.value = currentState.copy(
                    currentPhase = SessionPhase.STUDY,
                    timeRemaining = config.studyDuration * 60,
                    currentSession = currentState.currentSession + 1,
                    isRunning = true,
                    isPaused = false
                )
                _events.tryEmit(FocusSessionEvent.StudySessionStarted)
            }

            SessionPhase.COMPLETED -> {
                // Already completed, no action needed
            }
        }

        if (_state.value.isRunning) {
            startTimer()
        }
    }

    private fun updateConfig(newConfig: FocusSessionConfig) {
        val currentState = _state.value
        _state.value = currentState.copy(
            config = newConfig,
            timeRemaining = when (currentState.currentPhase) {
                SessionPhase.STUDY -> newConfig.studyDuration * 60
                SessionPhase.SHORT_BREAK -> newConfig.shortBreak * 60
                SessionPhase.LONG_BREAK -> newConfig.longBreak * 60
                SessionPhase.COMPLETED -> 0
            }
        )
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = CoroutineScope(Dispatchers.Main).launch {
            while (_state.value.isRunning && !_state.value.isPaused && _state.value.timeRemaining > 0) {
                delay(1000)
                val currentState = _state.value
                val newTimeRemaining = currentState.timeRemaining - 1
                val newTotalTimeElapsed = currentState.totalTimeElapsed + 1

                _state.value = currentState.copy(
                    timeRemaining = newTimeRemaining,
                    totalTimeElapsed = newTotalTimeElapsed
                )

                if (newTimeRemaining <= 0) {
                    _events.tryEmit(FocusSessionEvent.PhaseCompleted)
                    moveToNextPhase()
                    break
                }
            }
        }
    }

    fun cleanup() {
        timerJob?.cancel()
        notificationJob?.cancel()
    }
}

sealed class FocusSessionEvent {
    object SessionStarted : FocusSessionEvent()
    object SessionPaused : FocusSessionEvent()
    object SessionResumed : FocusSessionEvent()
    object SessionStopped : FocusSessionEvent()
    object StudySessionStarted : FocusSessionEvent()
    object NextStudySessionStarted : FocusSessionEvent()
    object ShortBreakStarted : FocusSessionEvent()
    object LongBreakStarted : FocusSessionEvent()
    object PhaseCompleted : FocusSessionEvent()
}