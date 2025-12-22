package com.mtlc.studyplan.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtlc.studyplan.monitoring.CrashReport
import com.mtlc.studyplan.monitoring.CrashReporter
import com.mtlc.studyplan.monitoring.ErrorStatistics
import com.mtlc.studyplan.monitoring.PerformanceAlert
import com.mtlc.studyplan.monitoring.RealTimeMetrics
import com.mtlc.studyplan.monitoring.RealTimePerformanceMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state for the MonitoringDashboard.
 *
 * Aggregates data from multiple monitoring services into a single state object.
 */
data class MonitoringUiState(
    val crashReports: List<CrashReport> = emptyList(),
    val errorStats: ErrorStatistics = ErrorStatistics(),
    val performanceMetrics: RealTimeMetrics = RealTimeMetrics(),
    val performanceAlerts: List<PerformanceAlert> = emptyList(),
    val isLoading: Boolean = true,
    val lastRefreshTime: Long = System.currentTimeMillis()
)

/**
 * ViewModel for the MonitoringDashboard.
 *
 * Mediates between monitoring services (CrashReporter, RealTimePerformanceMonitor)
 * and the UI. Aggregates multiple flows into a single UI state flow for
 * efficient composition.
 *
 * Note: This ViewModel can be used either:
 * 1. Via standard Android ViewModel injection (viewModel() or hiltViewModel())
 * 2. Via remember {} for simpler use cases (flows are still properly collected)
 */
class MonitoringViewModel(
    private val crashReporter: CrashReporter,
    private val realTimeMonitor: RealTimePerformanceMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow(MonitoringUiState())
    val uiState: StateFlow<MonitoringUiState> = _uiState.asStateFlow()

    init {
        collectCrashReports()
        collectErrorStats()
        collectPerformanceMetrics()
        collectPerformanceAlerts()
    }

    private fun collectCrashReports() {
        viewModelScope.launch {
            crashReporter.crashReports.collect { reports ->
                _uiState.update { it.copy(
                    crashReports = reports.take(10),
                    isLoading = false
                ) }
            }
        }
    }

    private fun collectErrorStats() {
        viewModelScope.launch {
            crashReporter.errorStats.collect { stats ->
                _uiState.update { it.copy(errorStats = stats) }
            }
        }
    }

    private fun collectPerformanceMetrics() {
        viewModelScope.launch {
            realTimeMonitor.performanceMetrics.collect { metrics ->
                _uiState.update { it.copy(performanceMetrics = metrics) }
            }
        }
    }

    private fun collectPerformanceAlerts() {
        viewModelScope.launch {
            realTimeMonitor.performanceAlerts.collect { alerts ->
                _uiState.update { it.copy(performanceAlerts = alerts.take(5)) }
            }
        }
    }

    /**
     * Clears all crash reports from storage.
     */
    fun clearCrashReports() {
        viewModelScope.launch {
            crashReporter.clearCrashReports()
        }
    }

    /**
     * Clears all performance alerts.
     */
    fun clearAlerts() {
        viewModelScope.launch {
            realTimeMonitor.clearAlerts()
        }
    }

    /**
     * Generates a performance report string.
     */
    fun generatePerformanceReport(): String {
        return realTimeMonitor.generatePerformanceReport().toString()
    }

    /**
     * Refreshes UI state timestamp to trigger recomposition.
     */
    fun refresh() {
        _uiState.update { it.copy(lastRefreshTime = System.currentTimeMillis()) }
    }
}
