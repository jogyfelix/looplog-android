package com.jolabs.looplog.habit.ui.habitHome

import Resource
import com.google.common.truth.Truth.assertThat
import com.jolabs.looplog.analytics.AnalyticsTracker
import com.jolabs.looplog.data.repository.HabitRepository
import com.jolabs.looplog.habit.util.MainDispatcherExtension
import com.jolabs.looplog.model.HabitBasic
import com.jolabs.looplog.model.HabitStatus
import com.jolabs.looplog.ui.UIEvent
import com.jolabs.looplog.util.DateUtils
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

@ExperimentalCoroutinesApi
@ExtendWith(MainDispatcherExtension::class)
class HabitHomeViewModelTest {

    private val habitRepository: HabitRepository = mockk()
    private val settingsRepository: com.jolabs.looplog.data.repository.SettingsRepository = mockk {
        every { showHabitSections } returns flowOf(true)
    }
    private val analyticsTracker: AnalyticsTracker = mockk(relaxed = true)
    private lateinit var viewModel: HabitHomeViewModel
    
    // Fixed "today" for testing: 2024-01-01 (Monday)
    private val fixedToday = LocalDate.of(2024, 1, 1)

    @BeforeEach
    fun setUp() {
        mockkStatic(LocalDate::class)
        mockkObject(DateUtils)
        
        every { LocalDate.now() } returns fixedToday
        every { LocalDate.now(any<ZoneId>()) } returns fixedToday
        every { DateUtils.todayEpochDay() } returns fixedToday.toEpochDay()
        
        // Mock default repository returns to prevent crashes during init
        every { habitRepository.getAllHabits() } returns flowOf(Resource.Success(emptyList()))
        every { habitRepository.getHabitByDate(any(), any()) } returns flowOf(Resource.Success(emptyList()))

        viewModel = HabitHomeViewModel(habitRepository, settingsRepository, analyticsTracker, mockk(relaxed = true))
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(LocalDate::class)
        unmockkObject(DateUtils)
    }

    @Test
    fun `initial state should represent today`() {
        val state = viewModel.uiState.value
        assertThat(state.selectedDate).isEqualTo(fixedToday.toEpochDay())
        assertThat(state.selectedDay).isEqualTo(DayOfWeek.MONDAY)
        assertThat(state.selectedOption).isEqualTo(HabitFilterOption.TODAY)
        assertThat(state.isCustomDateSelected).isFalse()
    }

    @Test
    fun `updateSelectedOption Yesterday should update state to previous day`() {
        viewModel.updateSelectedOption(HabitFilterOption.YESTERDAY)
        
        val state = viewModel.uiState.value
        assertThat(state.selectedDate).isEqualTo(fixedToday.minusDays(1).toEpochDay())
        assertThat(state.selectedOption).isEqualTo(HabitFilterOption.YESTERDAY)
        assertThat(state.isCustomDateSelected).isFalse()
    }

    @Test
    fun `updateSelectedDate should mark custom date as selected`() {
        val customDate = fixedToday.plusDays(5).toEpochDay()
        viewModel.updateSelectedDate(customDate)
        
        val state = viewModel.uiState.value
        assertThat(state.selectedDate).isEqualTo(customDate)
        assertThat(state.isCustomDateSelected).isTrue()
        assertThat(state.selectedOption).isNull()
    }

    @Test
    fun `updateHabitEntry should call repository upsert`() = runTest {
        val habitId = 1L
        val date = fixedToday.toEpochDay()
        val status = HabitStatus.COMPLETED

        coEvery { habitRepository.upsertHabitEntry(any()) } returns Unit

        viewModel.updateHabitEntry(habitId, date, status)

        coVerify { 
            habitRepository.upsertHabitEntry(withArg { 
                assertThat(it.habitId).isEqualTo(habitId)
                assertThat(it.date).isEqualTo(date)
                assertThat(it.isCompleted).isEqualTo(status)
            }) 
        }
    }

    @Test
    fun `habitList should react to selected date change`() = runTest {
        val habits = listOf(mockk<HabitBasic>())
        val customDate = fixedToday.plusDays(2)
        
        every { habitRepository.getHabitByDate(customDate.dayOfWeek, customDate.toEpochDay()) } returns flowOf(Resource.Success(habits))

        // Collect habitList to keep it active (WhileSubscribed)
        val collectedResults = mutableListOf<Resource<List<HabitBasic>>>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.habitList.collect { collectedResults.add(it) }
        }

        viewModel.updateSelectedDate(customDate.toEpochDay())
        viewModel.updateSelectedDay(customDate.dayOfWeek)

        assertThat(collectedResults.last()).isInstanceOf(Resource.Success::class.java)
        assertThat((collectedResults.last() as Resource.Success).data).isEqualTo(habits)
        
        job.cancel()
    }
}
