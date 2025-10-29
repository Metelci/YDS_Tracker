package com.mtlc.studyplan.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.mtlc.studyplan.data.Task
import com.mtlc.studyplan.data.TaskPriority
import com.mtlc.studyplan.data.TaskRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TodayViewModel(
    private val taskRepository: TaskRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

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
        viewModelScope.launch(ioDispatcher) {
            _state.update { it.copy(isLoading = true, snackbar = null) }
            val result = runCatching { taskRepository.getTodaysTasks().map { it.toSessionUi() } }
            _state.update { current ->
                result.fold(
                    onSuccess = { sessions ->
                        current.copy(isLoading = false, sessions = sessions, snackbar = null)
                    },
                    onFailure = {
                        current.copy(
                            isLoading = false,
                            snackbar = "Unable to load today's sessions"
                        )
                    }
                )
            }
        }
    }

    private fun start(id: String) {
        _state.update { it.copy(snackbar = "Started session $id") }
    }

    private fun complete(id: String) {
        viewModelScope.launch(ioDispatcher) {
            val completionSucceeded = runCatching {
                val task = taskRepository.getTaskById(id)
                    ?: return@runCatching false
                taskRepository.updateTask(
                    task.copy(
                        isCompleted = true,
                        completedAt = System.currentTimeMillis()
                    )
                )
                true
            }.getOrDefault(false)

            _state.update { current ->
                if (completionSucceeded) {
                    val updatedSessions = current.sessions.map { session ->
                        if (session.id == id) session.copy(isCompleted = true) else session
                    }
                    current.copy(
                        sessions = updatedSessions,
                        snackbar = "Completed session $id"
                    )
                } else {
                    current.copy(snackbar = "Unable to complete session $id")
                }
            }
        }
    }

    private fun skip(id: String) {
        _state.update { it.copy(snackbar = "Skipped session $id") }
    }

    fun consumeSnackbar() {
        _state.update { it.copy(snackbar = null) }
    }

    private fun reschedule(id: String, at: java.time.LocalDateTime) {
        _state.update {
            it.copy(
                snackbar = "Rescheduled $id to ${at.toLocalDate()} ${at.toLocalTime()}"
            )
        }
    }

    private fun Task.toSessionUi(): SessionUi = SessionUi(
        id = id,
        title = title,
        section = category,
        estMinutes = estimatedMinutes,
        difficulty = priority.toDifficulty(),
        isCompleted = isCompleted
    )

    private fun TaskPriority.toDifficulty(): Int = when (this) {
        TaskPriority.LOW -> 1
        TaskPriority.MEDIUM -> 2
        TaskPriority.HIGH -> 3
        TaskPriority.CRITICAL -> 4
    }

    companion object {
        fun factory(
            taskRepository: TaskRepository,
            dispatcher: CoroutineDispatcher = Dispatchers.IO
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                if (modelClass.isAssignableFrom(TodayViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return TodayViewModel(taskRepository, dispatcher) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }
}
