package com.jolabs.looplog.habit.ui.habitCreate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jolabs.looplog.analytics.AnalyticsTracker
import com.jolabs.looplog.habit.HabitAnalytics
import com.jolabs.looplog.habit.alarmManager.HabitAlarmManager
import com.jolabs.looplog.data.repository.HabitRepository
import com.jolabs.looplog.domain.CreateHabitUseCase
import com.jolabs.looplog.habit.R
import com.jolabs.looplog.model.CreateHabit
import com.jolabs.looplog.ui.UIEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import javax.inject.Inject

@HiltViewModel
class CreateHabitViewModel @Inject constructor(
    private val createHabitUseCase: CreateHabitUseCase,
    private val habitRepository: HabitRepository,
    private val analyticsTracker: AnalyticsTracker,
    val habitAlarmManager: HabitAlarmManager
) : ViewModel() {
    private val _selectedDays = MutableStateFlow<List<DayOfWeek>>(emptyList())
    val selectedDays : StateFlow<List<DayOfWeek>> = _selectedDays

    private val _habitName = MutableStateFlow("")
    val habitName : StateFlow<String> = _habitName

    private val _habitId = MutableStateFlow(0L)
    private val _createdAt = MutableStateFlow(0L)

    private val _habitDescription = MutableStateFlow("")
    val habitDescription : StateFlow<String> = _habitDescription

    private val _timeOfDay : MutableStateFlow<Long?> = MutableStateFlow(null)
    val timeOfDay : StateFlow<Long?> = _timeOfDay

    private val _fromDetail = MutableStateFlow(false)

    private val _uiEvent = MutableSharedFlow<UIEvent>()
    val uiEvent : SharedFlow<UIEvent> = _uiEvent


    internal fun onSelectedDayToggle(dayOfWeek: DayOfWeek) {
        _selectedDays.update { current ->
            if(dayOfWeek in current) current - dayOfWeek else current + dayOfWeek
        }
    }

    val allDays = DayOfWeek.entries.toList()
    internal fun toggleAllDays(show: Boolean) {
        _selectedDays.update { current ->
            if (!show) emptyList() else allDays
        }
    }

    internal fun onHabitNameChange(name : String) {
        _habitName.value = name
    }

    internal fun onHabitDescriptionChange(description : String) {
        _habitDescription.value = description
    }

    internal fun onTimeOfDayChange(time : Long?) {
        _timeOfDay.value = time
    }

    internal fun setFromDetail(fromDetail: Boolean) {
        _fromDetail.value = fromDetail
    }

    private fun clearInMemory() {
        _selectedDays.value = emptyList()
        _habitName.value = ""
        _habitDescription.value = ""
        _timeOfDay.value = null
    }

    override fun onCleared() {
        super.onCleared()
        clearInMemory()
    }

    internal fun getHabit(id : Long) {
        viewModelScope.launch() {
            if(id != 0L)
             {
                _habitId.value = id
                withContext(Dispatchers.IO) {
                    val habit = habitRepository.getHabitById(id)
                    _selectedDays.value = habit?.daysOfWeek ?: emptyList()
                    _habitName.value = habit?.name ?: ""
                    _habitDescription.value = habit?.description ?: ""
                    _timeOfDay.value = habit?.timeOfDay
                    _createdAt.value = habit?.createdAt ?: 0L
                }
            }
        }
    }

    internal fun createHabit() {
        viewModelScope.launch {

            if(_habitName.value.isBlank()){
                _uiEvent.emit(UIEvent.ShowMessage(UiMessage.StringRes(R.string.enter_a_habit_name)))
                return@launch
            }

            if(_selectedDays.value.isEmpty()){
                _uiEvent.emit(UIEvent.ShowMessage(UiMessage.StringRes(R.string.select_at_least_one_day)))
                return@launch
            }

            val isNewHabit = _habitId.value == 0L
            withContext(Dispatchers.IO) {
                val updatedHabitId = createHabitUseCase(
                    habit = CreateHabit(
                        id = _habitId.value,
                        name = _habitName.value.trim(),
                        description = _habitDescription.value.trim(),
                        daysOfWeek = _selectedDays.value,
                        timeOfDay = _timeOfDay.value,
                        createdAt = if(_habitId.value == 0L) System.currentTimeMillis() else _createdAt.value,
                        updatedAt = System.currentTimeMillis(),
                    )
                )
               if(updatedHabitId != -1L) {
                   _habitId.value = updatedHabitId
                   habitAlarmManager.cancelAllHabitAlarms(updatedHabitId)
                   _timeOfDay.value?.let { timeOfDay ->
                       habitAlarmManager.scheduleHabitAlarm(
                           updatedHabitId,
                           _habitName.value,
                           timeOfDay,
                           _selectedDays.value
                       )
                   }
                   val habitType = if (_selectedDays.value.size == allDays.size) "daily" else "custom"
                   val hasReminder = _timeOfDay.value != null
                   val repeatDayCount = _selectedDays.value.size
                   analyticsTracker.trackEvent(
                       if (isNewHabit) {
                           HabitAnalytics.HabitCreated(habitType, hasReminder, repeatDayCount)
                       } else {
                           HabitAnalytics.HabitUpdated(habitType, hasReminder, repeatDayCount)
                       }
                   )
               }
            }
            _uiEvent.emit(UIEvent.ShowMessage(UiMessage.StringRes(R.string.habit_saved)))
            _uiEvent.emit(UIEvent.NavigateUp)
        }
    }

    internal fun deleteHabit() {
        viewModelScope.launch(Dispatchers.IO) {
            habitAlarmManager.cancelAllHabitAlarms(_habitId.value)
            val result = habitRepository.deleteHabit(_habitId.value)
            if (result == 1) {
                analyticsTracker.trackEvent(HabitAnalytics.HabitDeleted)
                _uiEvent.emit(UIEvent.ShowMessage(UiMessage.StringRes(R.string.habit_deleted)))
                // If came from detail screen (Stats → Detail → Edit), navigate back twice
                // to skip both edit and detail screens. Otherwise navigate back once.
                if (_fromDetail.value) {
                    _uiEvent.emit(UIEvent.NavigateBackTwice)
                } else {
                    _uiEvent.emit(UIEvent.NavigateUp)
                }
            }
        }
    }

    internal suspend fun getRepeatDays(): List<DayOfWeek> =
        habitRepository.getRepeatDaysFromHabit(_habitId.value).map { it.dayOfWeek }

    /**
     * Exposes the repository for use with HabitAlarmManager.
     * This allows the UI to create HabitAlarmManager with repository access
     * while keeping the repository instance managed by the ViewModel.
     */
    fun getRepository(): HabitRepository = habitRepository
}
