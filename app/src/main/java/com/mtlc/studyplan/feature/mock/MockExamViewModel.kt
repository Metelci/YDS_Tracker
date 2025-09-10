package com.mtlc.studyplan.feature.mock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MockExamViewModel(private val repo: MockExamRepository) : ViewModel() {
    private val _state = MutableStateFlow(MockExamState())
    val state: StateFlow<MockExamState> = _state.asStateFlow()

    private var ticker: Job? = null

    fun dispatch(intent: MockIntent) {
        when (intent) {
            is MockIntent.Load -> load()
            is MockIntent.Jump -> jump(intent.index)
            is MockIntent.Select -> select(intent.questionId, intent.optionIndex)
            is MockIntent.ToggleReview -> toggleReview(intent.questionId)
            is MockIntent.Tick -> tick()
            is MockIntent.Submit -> submit()
        }
    }

    private fun load() {
        viewModelScope.launch {
            val snapshot = repo.snapshotFlow.first()
            val questions = MockSampleData.questions()
            _state.update {
                it.copy(
                    questions = questions,
                    currentIndex = snapshot.currentIndex.coerceIn(0, questions.lastIndex),
                    remainingSeconds = snapshot.remainingSeconds,
                    markedForReview = snapshot.marked
                )
            }
            startTicker()
        }
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (state.value.remainingSeconds > 0) {
                kotlinx.coroutines.delay(1000)
                tick()
            }
        }
    }

    private fun tick() {
        _state.update { s ->
            val next = (s.remainingSeconds - 1).coerceAtLeast(0)
            s.copy(remainingSeconds = next)
        }
        persistSnapshot()
    }

    private fun jump(index: Int) {
        _state.update { it.copy(currentIndex = index.coerceIn(0, it.questions.lastIndex)) }
        persistSnapshot()
    }

    private fun select(questionId: Int, optionIndex: Int) {
        _state.update { s -> s.copy(selections = s.selections + (questionId to optionIndex)) }
    }

    private fun toggleReview(questionId: Int) {
        _state.update { s ->
            val newSet = s.markedForReview.toMutableSet().also {
                if (!it.add(questionId)) it.remove(questionId)
            }
            s.copy(markedForReview = newSet)
        }
        persistSnapshot()
    }

    private fun submit() {
        _state.update { it.copy(isSubmitting = true) }
    }

    private fun persistSnapshot() {
        val s = state.value
        viewModelScope.launch {
            repo.save(MockExamRepository.Snapshot(s.remainingSeconds, s.currentIndex, s.markedForReview))
        }
    }
}

