package com.jolabs.looplog.habit.ui.habitCreate

import com.google.common.truth.Truth.assertThat
import com.jolabs.looplog.analytics.AnalyticsTracker
import com.jolabs.looplog.habit.HabitAnalytics
import com.jolabs.looplog.data.repository.HabitRepository
import com.jolabs.looplog.domain.CreateHabitUseCase
import com.jolabs.looplog.habit.alarmManager.HabitAlarmManager
import com.jolabs.looplog.habit.util.MainDispatcherExtension
import com.jolabs.looplog.ui.UIEvent
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import io.mockk.every
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.DayOfWeek

@ExperimentalCoroutinesApi
@ExtendWith(MainDispatcherExtension::class)
class CreateHabitViewModelTest {

    private val createHabitUseCase: CreateHabitUseCase = mockk()
    private val habitRepository: HabitRepository = mockk()
    private val analyticsTracker: AnalyticsTracker = mockk(relaxed = true)
    private val habitAlarmManager: HabitAlarmManager = mockk(relaxed = true)
    private lateinit var viewModel: CreateHabitViewModel

    @BeforeEach
    fun setUp() {
        mockkStatic(Dispatchers::class)
        every { Dispatchers.IO } returns UnconfinedTestDispatcher()
        viewModel = CreateHabitViewModel(createHabitUseCase, habitRepository, analyticsTracker, habitAlarmManager)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(Dispatchers::class)
    }

    @Test
    fun `initial state should be empty`() {
        assertThat(viewModel.habitName.value).isEmpty()
        assertThat(viewModel.selectedDays.value).isEmpty()
        assertThat(viewModel.habitDescription.value).isEmpty()
        assertThat(viewModel.timeOfDay.value).isNull()
    }

    @Test
    fun `onHabitNameChange should update habitName state`() {
        val newName = "New Habit"
        viewModel.onHabitNameChange(newName)
        assertThat(viewModel.habitName.value).isEqualTo(newName)
    }

    @Test
    fun `onSelectedDayToggle should add day if not present`() {
        viewModel.onSelectedDayToggle(DayOfWeek.MONDAY)
        assertThat(viewModel.selectedDays.value).containsExactly(DayOfWeek.MONDAY)
    }

    @Test
    fun `onSelectedDayToggle should remove day if present`() {
        viewModel.onSelectedDayToggle(DayOfWeek.MONDAY)
        viewModel.onSelectedDayToggle(DayOfWeek.MONDAY)
        assertThat(viewModel.selectedDays.value).isEmpty()
    }

    @Test
    fun `toggleAllDays true should select all days`() {
        viewModel.toggleAllDays(true)
        assertThat(viewModel.selectedDays.value).hasSize(7)
        assertThat(viewModel.selectedDays.value).containsAtLeastElementsIn(DayOfWeek.entries)
    }

    @Test
    fun `createHabit with blank name should emit validation message`() = runTest {
        val events = mutableListOf<UIEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiEvent.collect { events.add(it) }
        }

        viewModel.onHabitNameChange("")
        viewModel.createHabit()

        assertThat(events).isNotEmpty()
        assertThat(events.first()).isInstanceOf(UIEvent.ShowMessage::class.java)
        job.cancel()
    }

    @Test
    fun `createHabit with no days selected should emit validation message`() = runTest {
        val events = mutableListOf<UIEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiEvent.collect { events.add(it) }
        }

        viewModel.onHabitNameChange("Exercise")
        viewModel.toggleAllDays(false)
        viewModel.createHabit()

        assertThat(events).isNotEmpty()
        assertThat(events.first()).isInstanceOf(UIEvent.ShowMessage::class.java)
        job.cancel()
    }

    @Test
    fun `createHabit with valid input should call useCase and navigate up`() = runTest {
        val events = mutableListOf<UIEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiEvent.collect { events.add(it) }
        }

        val habitName = "Exercise"
        val day = DayOfWeek.MONDAY
        
        viewModel.onHabitNameChange(habitName)
        viewModel.onSelectedDayToggle(day)
        
        coEvery { createHabitUseCase(any()) } returns 1L

        viewModel.createHabit()

        assertThat(events).contains(UIEvent.NavigateUp)
        verify(exactly = 1) {
            analyticsTracker.trackEvent(
                HabitAnalytics.HabitCreated(habitType = "custom", hasReminder = false, repeatDayCount = 1)
            )
        }
        job.cancel()
    }

    @Test
    fun `deleteHabit should call repository and emit navigate up`() = runTest {
        val events = mutableListOf<UIEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiEvent.collect { events.add(it) }
        }

        val habitId = 1L
        coEvery { habitRepository.getHabitById(habitId) } returns mockk(relaxed = true)
        coEvery { habitRepository.deleteHabit(habitId) } returns 1

        viewModel.getHabit(habitId)
        viewModel.deleteHabit()

        assertThat(events).contains(UIEvent.NavigateUp)
        job.cancel()
    }

    @Test
    fun `deleteHabit when fromDetail is true should emit navigate back twice`() = runTest {
        val events = mutableListOf<UIEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiEvent.collect { events.add(it) }
        }

        val habitId = 1L
        coEvery { habitRepository.getHabitById(habitId) } returns mockk(relaxed = true)
        coEvery { habitRepository.deleteHabit(habitId) } returns 1

        viewModel.setFromDetail(true)
        viewModel.getHabit(habitId)
        viewModel.deleteHabit()

        assertThat(events).contains(UIEvent.NavigateBackTwice)
        job.cancel()
    }

    @Test
    fun `createHabit with time should schedule alarm before navigation`() = runTest {
        val habitName = "Exercise"
        val time = 3600000L
        
        viewModel.onHabitNameChange(habitName)
        viewModel.onSelectedDayToggle(DayOfWeek.MONDAY)
        viewModel.onTimeOfDayChange(time)
        
        coEvery { createHabitUseCase(any()) } returns 1L

        viewModel.createHabit()

        verify { habitAlarmManager.cancelAllHabitAlarms(1L) }
        verify {
            habitAlarmManager.scheduleHabitAlarm(
                1L,
                habitName,
                time,
                listOf(DayOfWeek.MONDAY)
            )
        }
    }
}
