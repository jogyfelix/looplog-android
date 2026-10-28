package com.jolabs.looplog.habit.ui.habitHome

import Resource
import android.content.Context
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jolabs.looplog.data.repository.HabitRepository
import com.jolabs.looplog.data.repository.SettingsRepository
import com.jolabs.looplog.analytics.AnalyticsTracker
import com.jolabs.looplog.analytics.model.AnalyticsError
import com.jolabs.looplog.habit.HabitAnalytics
import com.jolabs.looplog.habit.R
import com.jolabs.looplog.habit.utils.NotificationUtil
import com.jolabs.looplog.habit.utils.WidgetUpdateUtil
import com.jolabs.looplog.habit.utils.getHabitShape as getHelperHabitShape
import com.jolabs.looplog.habit.utils.getHabitShapeWrapper as getHelperHabitShapeWrapper
import com.jolabs.looplog.model.HabitBasic
import com.jolabs.looplog.model.HabitEntryModel
import com.jolabs.looplog.model.HabitStatus
import com.jolabs.looplog.ui.UIEvent
import com.jolabs.looplog.util.DateUtils.todayEpochDay
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate


enum class HabitFilterOption(val resId: Int) {
    TODAY(R.string.filter_today),
    YESTERDAY(R.string.filter_yesterday),
    ALL(R.string.filter_all)
}

data class HabitHomeUiState(
    val selectedDate: Long = todayEpochDay(),
    val selectedDay: DayOfWeek = LocalDate.now().dayOfWeek,
    val selectedOption: HabitFilterOption? = HabitFilterOption.TODAY,
    val options: List<HabitFilterOption> = HabitFilterOption.entries,
    val isCustomDateSelected: Boolean = false
)


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@HiltViewModel
class HabitHomeViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
    private val settingsRepository: SettingsRepository,
    private val analyticsTracker: AnalyticsTracker,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(HabitHomeUiState())
    val uiState: StateFlow<HabitHomeUiState> = _uiState

    val showHabitSections: StateFlow<Boolean> = settingsRepository.showHabitSections
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )


    private val _uiEvent = MutableSharedFlow<UIEvent>()
    val uiEvent : SharedFlow<UIEvent> = _uiEvent

    // Cache for formatted alarm times to prevent recomposition
    private val formattedTimeCache = mutableMapOf<Triple<Long, Long?, Boolean>, String?>()

    fun getHabitShape(habitId: Long): androidx.graphics.shapes.RoundedPolygon {
        return getHelperHabitShape(habitId)
    }

    fun getFormattedAlarmTime(habitId: Long, habitTime: Long?, is24Hour: Boolean): String? {
        return formattedTimeCache.getOrPut(Triple(habitId, habitTime, is24Hour)) {
            com.jolabs.looplog.util.DateUtils.formatHabitTime(habitTime, is24Hour)
        }
    }

    fun getHabitShapeWrapper(habitId: Long): androidx.compose.ui.graphics.Shape {
        return getHelperHabitShapeWrapper(habitId)
    }


    fun updateSelectedDate(date: Long) {
        _uiState.update { 
            it.copy(
                selectedDate = date,
                isCustomDateSelected = true,
                selectedOption = null // Clear the selected option when custom date is selected
            ) 
        }
    }

    fun updateSelectedDay(day: DayOfWeek) {
        _uiState.update { it.copy(selectedDay = day) }
    }

    fun updateSelectedOption(option: HabitFilterOption) {
        val today = LocalDate.now()
        val (newDate, newDay) = when (option) {
            HabitFilterOption.TODAY -> {
                Pair(today.toEpochDay(), today.dayOfWeek)
            }
            HabitFilterOption.YESTERDAY -> {
                val yesterday = today.minusDays(1)
                Pair(yesterday.toEpochDay(), yesterday.dayOfWeek)
            }
            HabitFilterOption.ALL -> {
                // Keep today's date for "All" but mark as not custom date
                Pair(today.toEpochDay(), today.dayOfWeek)
            }
        }

        _uiState.update {
            it.copy(
                selectedOption = option,
                selectedDate = newDate,
                selectedDay = newDay,
                isCustomDateSelected = false
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val habitList: StateFlow<Resource<List<HabitBasic>>> = _uiState
        .flatMapLatest { state ->
            if (state.selectedOption == HabitFilterOption.ALL) {
                habitRepository.getAllHabits()
            } else {
                habitRepository.getHabitByDate(state.selectedDay, state.selectedDate)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Resource.Loading()
        )

    internal fun updateHabitEntry(habitId : Long, date : Long, status : HabitStatus) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                habitRepository.upsertHabitEntry(
                    HabitEntryModel(
                        habitId = habitId,
                        date = date,
                        isCompleted = status
                    )
                )
                if (status == HabitStatus.COMPLETED || status == HabitStatus.SKIPPED) {
                    NotificationUtil.cancelHabitNotification(context, habitId)
                }
                analyticsTracker.trackEvent(
                    HabitAnalytics.HabitStatusChanged(status, HabitAnalytics.EntrySources.Home)
                )
                WidgetUpdateUtil.requestHabitWidgetsRefresh(context)
            } catch (e: CancellationException) {
                // Scope was cancelled (e.g. user left the screen) — not a failure.
                throw e
            } catch (e: Exception) {
                analyticsTracker.trackError(
                    AnalyticsError.Hard(
                        throwable = e,
                        source = HabitAnalytics.Sources.Home,
                        message = "Failed to update habit entry: habitId=$habitId, status=$status"
                    )
                )
            }
        }
    }

    /**
     * Exposes the repository for use with HabitAlarmManager.
     */
    fun getRepository(): HabitRepository = habitRepository

}
