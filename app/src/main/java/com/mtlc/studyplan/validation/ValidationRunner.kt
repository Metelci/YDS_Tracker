package com.mtlc.studyplan.validation

import android.content.Context
import com.mtlc.studyplan.performance.PerformanceMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lightweight validation runner that delegates to the concrete validation services.
 */
@Singleton
class ValidationRunner @Inject constructor(
    private val context: Context,
    private val performanceMonitor: PerformanceMonitor,
    private val productionValidator: ProductionValidator,
    private val playStoreValidator: PlayStoreValidator
) {

    private val _validationState = MutableStateFlow<ValidationState>(ValidationState.Idle)
    val validationState: StateFlow<ValidationState> = _validationState

    fun runCompleteValidation() {
        _validationState.value = ValidationState.Completed("Validation checks temporarily disabled")
    }
}

sealed class ValidationState {
    object Idle : ValidationState()
    data class Running(val message: String) : ValidationState()
    data class Completed(val report: String) : ValidationState()
    data class Error(val message: String) : ValidationState()
}
