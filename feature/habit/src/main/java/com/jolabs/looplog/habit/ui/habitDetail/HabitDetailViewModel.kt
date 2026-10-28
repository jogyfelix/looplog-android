package com.jolabs.looplog.habit.ui.habitDetail

import Resource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jolabs.looplog.data.repository.HabitRepository
import com.jolabs.looplog.analytics.AnalyticsTracker
import com.jolabs.looplog.analytics.model.AnalyticsError
import com.jolabs.looplog.habit.HabitAnalytics
import com.jolabs.looplog.habit.R
import com.jolabs.looplog.model.HabitEntryModel
import com.jolabs.looplog.model.HabitStatus
import UiMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * ViewModel for the Habit Detail screen showing individual habit statistics
 */
@HiltViewModel
class HabitDetailViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
    private val analyticsTracker: AnalyticsTracker
) : ViewModel() {

    private val _uiState = MutableStateFlow<HabitDetailUiState>(HabitDetailUiState.Loading)
    val uiState: StateFlow<HabitDetailUiState> = _uiState.asStateFlow()

    fun loadHabitDetail(habitId: Long) {
        viewModelScope.launch {
            _uiState.value = HabitDetailUiState.Loading

            try {
                // Fetch habit details
                val habit = habitRepository.getHabitBasicById(habitId)
                if (habit == null) {
                    _uiState.value = HabitDetailUiState.Error(UiMessage.StringRes(R.string.habit_not_found))
                    return@launch
                }

                // Fetch habit entries for calculations
                val entries = habitRepository.getHabitEntries(habitId)
                val repeatDays = habitRepository.getRepeatDaysFromHabit(habitId)

                // Calculate stats
                val heatmapData = calculateHeatmapData(entries, repeatDays.map { it.dayOfWeek })
                val consistencyData = calculateConsistencyData(entries, repeatDays.map { it.dayOfWeek })

                _uiState.value = HabitDetailUiState.Success(
                    habitId = habitId,
                    habitName = habit.name,
                    habitDescription = habit.description,
                    currentStreak = habit.currentStreak.toIntOrNull() ?: 0,
                    bestStreak = habit.longestStreak.toIntOrNull() ?: 0,
                    repeatDays = repeatDays.map { it.dayOfWeek },
                    heatmapData = heatmapData,
                    consistencyData = consistencyData
                )
            } catch (e: CancellationException) {
                // Scope was cancelled (e.g. user left the screen) — not a failure.
                throw e
            } catch (e: Exception) {
                analyticsTracker.trackError(
                    AnalyticsError.Hard(
                        throwable = e,
                        source = HabitAnalytics.Sources.Detail,
                        message = "Failed to load habit detail for habitId=$habitId"
                    )
                )
                _uiState.value = HabitDetailUiState.Error(
                    e.localizedMessage?.let { UiMessage.Text(it) } ?: UiMessage.StringRes(R.string.unknown_error)
                )
            }
        }
    }

    /**
     * Calculate heatmap data for the last 12 weeks (84 days)
     * Returns a list of HeatmapDay objects representing each day
     */
    private fun calculateHeatmapData(
        entries: List<HabitEntryModel>,
        repeatDays: List<DayOfWeek>
    ): List<HeatmapDay> {
        val today = LocalDate.now()
        val entryMap = entries.associateBy { LocalDate.ofEpochDay(it.date) }

        // Last 12 weeks = 84 days
        return (83 downTo 0).map { daysAgo ->
            val date = today.minusDays(daysAgo.toLong())
            val entry = entryMap[date]
            val isScheduled = repeatDays.contains(date.dayOfWeek)

            val state = when {
                !isScheduled -> CompletionState.NOT_SCHEDULED
                entry == null -> CompletionState.NONE
                entry.isCompleted == HabitStatus.COMPLETED -> CompletionState.COMPLETED
                entry.isCompleted == HabitStatus.SKIPPED -> CompletionState.SKIPPED
                else -> CompletionState.NONE
            }

            HeatmapDay(
                date = date,
                state = state,
                isToday = date == today
            )
        }
    }

    /**
     * Calculate consistency data by day of week for the last 30 days
     * Returns a map of DayOfWeek to completion percentage (0-100)
     */
    private fun calculateConsistencyData(
        entries: List<HabitEntryModel>,
        repeatDays: List<DayOfWeek>
    ): List<DayOfWeekStat> {
        val today = LocalDate.now()
        val last30Days = (29 downTo 0).map { today.minusDays(it.toLong()) }

        // Group by day of week
        val dayOfWeekMap = mutableMapOf<DayOfWeek, MutableList<Boolean>>()

        last30Days.forEach { date ->
            val dayOfWeek = date.dayOfWeek
            if (repeatDays.contains(dayOfWeek)) {
                val entry = entries.find { LocalDate.ofEpochDay(it.date) == date }
                val completed = entry?.isCompleted == HabitStatus.COMPLETED

                dayOfWeekMap.getOrPut(dayOfWeek) { mutableListOf() }.add(completed)
            }
        }

        // Calculate percentage for each day of week
        return DayOfWeek.entries.map { dayOfWeek ->
            val occurrences = dayOfWeekMap[dayOfWeek] ?: emptyList()
            val completedCount = occurrences.count { it }
            val totalCount = occurrences.size
            val percentage = if (totalCount > 0) {
                (completedCount.toFloat() / totalCount * 100).toInt()
            } else {
                0
            }

            DayOfWeekStat(
                dayOfWeek = dayOfWeek,
                percentage = percentage,
                completedCount = completedCount,
                totalCount = totalCount
            )
        }
    }
}

/**
 * UI state for the Habit Detail screen
 */
sealed class HabitDetailUiState {
    data object Loading : HabitDetailUiState()
    data class Success(
        val habitId: Long,
        val habitName: String,
        val habitDescription: String = "",
        val currentStreak: Int,
        val bestStreak: Int,
        val repeatDays: List<DayOfWeek>,
        val heatmapData: List<HeatmapDay>,
        val consistencyData: List<DayOfWeekStat>
    ) : HabitDetailUiState()
    data class Error(val message: UiMessage) : HabitDetailUiState()
}

/**
 * Represents a single day in the heatmap
 */
data class HeatmapDay(
    val date: LocalDate,
    val state: CompletionState,
    val isToday: Boolean = false
)

/**
 * Completion state for a day
 */
enum class CompletionState {
    NOT_SCHEDULED,  // Day is not in the habit's repeat days
    NONE,           // Scheduled but not completed
    COMPLETED,      // Completed
    SKIPPED         // Explicitly skipped
}

/**
 * Statistics for a specific day of the week
 */
data class DayOfWeekStat(
    val dayOfWeek: DayOfWeek,
    val percentage: Int,        // 0-100
    val completedCount: Int,
    val totalCount: Int
)
