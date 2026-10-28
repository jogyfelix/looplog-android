package com.jolabs.looplog.habit.ui.habitStats

import Resource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jolabs.looplog.data.repository.HabitRepository
import com.jolabs.looplog.model.DailyCompletionState
import com.jolabs.looplog.model.DailyStat
import com.jolabs.looplog.model.HabitBasic
import com.jolabs.looplog.model.HabitStatus
import com.jolabs.looplog.ui.UIEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

/**
 * ViewModel for the Stats screen - shows all habits without date filtering
 */
@HiltViewModel
class HabitStatsViewModel @Inject constructor(
    private val habitRepository: HabitRepository
) : ViewModel() {

    private val _uiEvent = MutableSharedFlow<UIEvent>()
    val uiEvent: SharedFlow<UIEvent> = _uiEvent

    // Tracks whether the WeekAtAGlanceCard morph animation has already played this session.
    // Lives in the ViewModel so it survives forward navigation (Home → Stats) and only resets
    // when the process is killed, not on back-stack re-entry.
    private val _hasWeeklyStatsAnimationPlayed = MutableStateFlow(false)
    val hasWeeklyStatsAnimationPlayed: StateFlow<Boolean> = _hasWeeklyStatsAnimationPlayed.asStateFlow()

    fun markWeeklyStatsAnimationPlayed() {
        _hasWeeklyStatsAnimationPlayed.value = true
    }

    // Always fetch all habits for stats view
    val habitList: StateFlow<Resource<List<HabitBasic>>> = habitRepository.getAllHabits()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Resource.Loading()
        )

    // Reactive rolling 7-day stats for WeekAtAGlanceCard.
    // Combines one flow per day; updates automatically when any habit entry changes.
    val weeklyStats: StateFlow<List<DailyStat>> = run {
        val today = LocalDate.now()
        val dayFlows = (6 downTo 0).map { daysAgo ->
            val date = today.minusDays(daysAgo.toLong())
            habitRepository.getHabitByDate(date.dayOfWeek, date.toEpochDay())
                .map { resource ->
                    val habits = (resource as? Resource.Success)?.data ?: emptyList()
                    val state = when {
                        habits.isEmpty() -> DailyCompletionState.NONE
                        habits.all { it.habitState == HabitStatus.COMPLETED } -> DailyCompletionState.PERFECT
                        habits.any { it.habitState == HabitStatus.COMPLETED } -> DailyCompletionState.PARTIAL
                        else -> DailyCompletionState.NONE
                    }
                    DailyStat(date = date, state = state)
                }
        }
        combine(dayFlows) { it.toList() }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Reactive overall completion rate (0–100) across the last 7 days.
    // Derived directly from the repository; re-emits whenever any habit entry changes.
    val globalCompletionPercentage: StateFlow<Int> = habitRepository.getWeeklyCompletionRate()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    // Reactive count of days where every scheduled habit was completed in the last 7 days.
    val perfectDaysCount: StateFlow<Int> = habitRepository.getPerfectDaysCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    /**
     * Exposes the repository for use with HabitAlarmManager.
     */
    fun getRepository(): HabitRepository = habitRepository
}
