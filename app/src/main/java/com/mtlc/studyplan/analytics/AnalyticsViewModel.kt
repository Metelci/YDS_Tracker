package com.mtlc.studyplan.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AnalyticsViewModel(
    private val analyticsEngine: AnalyticsEngine
) : ViewModel() {

    private val _analyticsData = MutableStateFlow<AnalyticsData?>(null)
    val analyticsData: StateFlow<AnalyticsData?> = _analyticsData.asStateFlow()

    private val _weeklyData = MutableStateFlow<List<WeeklyAnalyticsData>>(emptyList())
    val weeklyData: StateFlow<List<WeeklyAnalyticsData>> = _weeklyData.asStateFlow()

    private val _performanceData = MutableStateFlow<PerformanceData?>(null)
    val performanceData: StateFlow<PerformanceData?> = _performanceData.asStateFlow()

    private val _selectedTab = MutableStateFlow(AnalyticsTab.OVERVIEW)
    val selectedTab: StateFlow<AnalyticsTab> = _selectedTab.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadAnalytics(AnalyticsTimeframe.LAST_30_DAYS)
    }

    fun loadAnalytics(timeframe: AnalyticsTimeframe) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val data = analyticsEngine.generateAnalytics(timeframe.days)
                _analyticsData.value = data

                val weekly = analyticsEngine.getWeeklyData(timeframe.days)
                _weeklyData.value = weekly

                val performance = analyticsEngine.getPerformanceData(timeframe.days)
                _performanceData.value = performance
            } catch (e: Exception) {
                // Handle error - could emit error state
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectTab(tab: AnalyticsTab) {
        _selectedTab.value = tab
    }

    fun refreshAnalytics() {
        val currentTimeframe = when (_weeklyData.value.size) {
            in 0..1 -> AnalyticsTimeframe.LAST_7_DAYS
            in 2..4 -> AnalyticsTimeframe.LAST_30_DAYS
            in 5..12 -> AnalyticsTimeframe.LAST_90_DAYS
            else -> AnalyticsTimeframe.ALL_TIME
        }
        loadAnalytics(currentTimeframe)
    }
}