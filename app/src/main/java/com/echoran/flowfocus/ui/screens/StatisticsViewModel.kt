package com.echoran.flowfocus.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echoran.flowfocus.data.model.FocusSessionEntity
import com.echoran.flowfocus.data.repository.FocusSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

enum class StatsTimeRange {
    DAY, WEEK, MONTH, YEAR
}

data class DashboardStats(
    val totalMinutes: Int = 0,
    val sessionCount: Int = 0,
    val avgMinutesPerDay: Int = 0,
    val categoryDistribution: Map<String, Int> = emptyMap(),
    val timeDistribution: Map<Int, Int> = emptyMap(), // Hour -> Minutes
    val recentSessions: List<FocusSessionEntity> = emptyList()
)

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val sessionRepository: FocusSessionRepository
) : ViewModel() {

    private val _selectedRange = MutableStateFlow(StatsTimeRange.DAY)
    val selectedRange: StateFlow<StatsTimeRange> = _selectedRange.asStateFlow()

    private val _dashboardStats = MutableStateFlow(DashboardStats())
    val dashboardStats: StateFlow<DashboardStats> = _dashboardStats.asStateFlow()

    init {
        viewModelScope.launch {
            _selectedRange.collect { range ->
                refreshStats(range)
            }
        }
    }

    fun setTimeRange(range: StatsTimeRange) {
        _selectedRange.value = range
    }

    private fun refreshStats(range: StatsTimeRange) {
        val now = Calendar.getInstance()
        val start = now.clone() as Calendar
        
        when (range) {
            StatsTimeRange.DAY -> {
                start.set(Calendar.HOUR_OF_DAY, 0)
                start.set(Calendar.MINUTE, 0)
                start.set(Calendar.SECOND, 0)
            }
            StatsTimeRange.WEEK -> {
                start.set(Calendar.DAY_OF_WEEK, start.firstDayOfWeek)
                start.set(Calendar.HOUR_OF_DAY, 0)
            }
            StatsTimeRange.MONTH -> {
                start.set(Calendar.DAY_OF_MONTH, 1)
                start.set(Calendar.HOUR_OF_DAY, 0)
            }
            StatsTimeRange.YEAR -> {
                start.set(Calendar.DAY_OF_YEAR, 1)
                start.set(Calendar.HOUR_OF_DAY, 0)
            }
        }

        viewModelScope.launch {
            // Use a very large end time to ensure new sessions (which have larger timestamps) 
            // trigger the Flow emission since Room's Flow emits when the table changes.
            sessionRepository.getSessionsInRange(start.timeInMillis, Long.MAX_VALUE)
                .collect { sessions ->
                    _dashboardStats.value = calculateDashboardStats(sessions)
                }
        }
    }

    private fun calculateDashboardStats(sessions: List<FocusSessionEntity>): DashboardStats {
        if (sessions.isEmpty()) return DashboardStats()

        val totalMinutes = sessions.sumOf { it.durationMinutes }
        val categoryMap = sessions.groupBy { it.category }
            .mapValues { it.value.sumOf { s -> s.durationMinutes } }

        val hourMap = mutableMapOf<Int, Int>()
        sessions.forEach { session ->
            val cal = Calendar.getInstance().apply { timeInMillis = session.startTime }
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            hourMap[hour] = (hourMap[hour] ?: 0) + session.durationMinutes
        }

        return DashboardStats(
            totalMinutes = totalMinutes,
            sessionCount = sessions.size,
            avgMinutesPerDay = totalMinutes / (if (sessions.isEmpty()) 1 else 7), // Simple placeholder for now
            categoryDistribution = categoryMap,
            timeDistribution = hourMap,
            recentSessions = sessions.take(20)
        )
    }
}
