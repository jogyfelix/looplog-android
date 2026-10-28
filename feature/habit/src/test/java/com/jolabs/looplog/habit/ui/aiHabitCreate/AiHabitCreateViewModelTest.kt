package com.jolabs.looplog.habit.ui.aiHabitCreate

import android.content.Context
import android.util.Log
import com.google.common.truth.Truth.assertThat
import com.jolabs.looplog.ai_engine.AIAnalytics
import com.jolabs.looplog.ai_engine.AIEngine
import com.jolabs.looplog.ai_engine.EngineState
import com.jolabs.looplog.analytics.AnalyticsTracker
import com.jolabs.looplog.data.repository.HabitRepository
import com.jolabs.looplog.data.repository.ai.AiFeatureAvailability
import com.jolabs.looplog.data.repository.ai.AiFeatureRepository
import com.jolabs.looplog.domain.ExtractHabitsUseCase
import com.jolabs.looplog.habit.alarmManager.HabitAlarmManager
import com.jolabs.looplog.habit.util.MainDispatcherExtension
import com.jolabs.looplog.habit.utils.WidgetUpdateUtil
import com.jolabs.looplog.model.CreateHabit
import com.jolabs.looplog.model.aiDataModels.HabitTemplate
import io.mockk.coVerifySequence
import io.mockk.mockkObject
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.DayOfWeek

@OptIn(ExperimentalCoroutinesApi::class)
class AiHabitCreateViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcherExtension = MainDispatcherExtension()

    private val extractHabitsUseCase: ExtractHabitsUseCase = mockk()
    private val habitRepository: HabitRepository = mockk(relaxed = true)
    private val aiFeatureRepository: AiFeatureRepository = mockk(relaxed = true)
    private val aiEngine: AIEngine = mockk(relaxed = true)
    private val analyticsTracker: AnalyticsTracker = mockk(relaxed = true)
    private val context: Context = mockk(relaxed = true)
    private val habitAlarmManager: HabitAlarmManager = mockk(relaxed = true)
    private lateinit var viewModel: AiHabitCreateViewModel

    @BeforeEach
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        mockkObject(WidgetUpdateUtil)
        every { WidgetUpdateUtil.requestHabitWidgetsRefresh(any()) } returns Unit
        everyAiFeatureIsReady()
        viewModel = AiHabitCreateViewModel(
            extractHabitsUseCase,
            habitRepository,
            aiFeatureRepository,
            aiEngine,
            analyticsTracker,
            context,
            habitAlarmManager,
        )
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(Log::class)
        unmockkObject(WidgetUpdateUtil)
    }

    @Test
    fun `non-English goal remains editable and shows language error state`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            val goal = "Hindi goal"
            coEvery { extractHabitsUseCase(goal) } returns Result.failure(Exception("english_only"))

            viewModel.onQueryChange(goal)
            viewModel.generateSuggestions()
            testScheduler.advanceUntilIdle()

            assertThat(viewModel.uiState.value.query).isEqualTo(goal)
            assertThat(viewModel.uiState.value.isEnglishOnlyError).isTrue()
            assertThat(viewModel.uiState.value.isError).isFalse()
            verify(exactly = 0) { analyticsTracker.trackEvent(AIAnalytics.GenerationFailure) }
    }

    @Test
    fun `retry after language error clears query and error state`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            coEvery { extractHabitsUseCase(any()) } returns Result.failure(Exception("english_only"))

            viewModel.onQueryChange("Hindi goal")
            viewModel.generateSuggestions()
            testScheduler.advanceUntilIdle()
            viewModel.resetError()

            assertThat(viewModel.uiState.value.query).isEmpty()
            assertThat(viewModel.uiState.value.isEnglishOnlyError).isFalse()
        }

    @Test
    fun `retry after general error clears query and error state`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            coEvery { extractHabitsUseCase(any()) } returns Result.failure(Exception("generation_failed"))

            viewModel.onQueryChange("Build a reading habit")
            viewModel.generateSuggestions()
            testScheduler.advanceUntilIdle()
            viewModel.resetError()

            assertThat(viewModel.uiState.value.query).isEmpty()
            assertThat(viewModel.uiState.value.isError).isFalse()
        }

    @Test
    fun `editing clears English-only error`() = runTest(mainDispatcherExtension.testDispatcher) {
        coEvery { extractHabitsUseCase(any()) } returns Result.failure(Exception("english_only"))

        viewModel.onQueryChange("Hindi goal")
        viewModel.generateSuggestions()
        testScheduler.advanceUntilIdle()
        viewModel.onQueryChange("Build a reading habit")

        assertThat(viewModel.uiState.value.isEnglishOnlyError).isFalse()
    }

    @Test
    fun `generation is single flight`() = runTest(mainDispatcherExtension.testDispatcher) {
        val goal = "Build a reading habit"
        coEvery { extractHabitsUseCase(goal) } returns Result.success(emptyList())

        viewModel.onQueryChange(goal)
        viewModel.generateSuggestions()
        viewModel.generateSuggestions()
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 1) { extractHabitsUseCase(goal) }
    }

    @Test
    fun `editing a suggestion trims fields and selects only that suggestion`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            generateSuggestions(
                HabitTemplate("one", "Drink water", "Have a glass"),
                HabitTemplate("two", "Read", "Read ten pages"),
            )

            viewModel.updateSuggestion(
                suggestionId = "two",
                title = "  Read a book  ",
                description = "  Read before bed  ",
                timeOfDay = 3_600_000L,
            )

            val suggestions = viewModel.uiState.value.suggestions
            assertThat(suggestions.first()).isEqualTo(
                AiHabitSuggestion("one", "Drink water", "Have a glass", com.jolabs.looplog.habit.R.drawable.start)
            )
            assertThat(suggestions.last()).isEqualTo(
                AiHabitSuggestion(
                    id = "two",
                    title = "Read a book",
                    description = "Read before bed",
                    iconRes = com.jolabs.looplog.habit.R.drawable.start,
                    selected = true,
                    timeOfDay = 3_600_000L,
                )
            )
        }

    @Test
    fun `adding an edited timed suggestion persists it before scheduling its alarm`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            generateSuggestions(HabitTemplate("one", "Drink water", "Have a glass"))
            viewModel.updateSuggestion(
                suggestionId = "one",
                title = "Morning water",
                description = "One glass after waking",
                timeOfDay = 3_600_000L,
            )
            coEvery { habitRepository.createHabit(any()) } returns 42L

            viewModel.addSelectedHabits {}
            testScheduler.advanceUntilIdle()

            coVerifySequence {
                habitRepository.createHabit(
                    match<CreateHabit> {
                        it.name == "Morning water" &&
                            it.description == "One glass after waking" &&
                            it.timeOfDay == 3_600_000L &&
                            it.daysOfWeek == DayOfWeek.entries
                    }
                )
                habitAlarmManager.scheduleHabitAlarm(
                    42L,
                    "Morning water",
                    3_600_000L,
                    DayOfWeek.entries,
                )
            }
        }

    @Test
    fun `adding an untimed suggestion does not schedule an alarm`() =
        runTest(mainDispatcherExtension.testDispatcher) {
            generateSuggestions(HabitTemplate("one", "Drink water", "Have a glass"))
            viewModel.toggleSuggestion("one")
            coEvery { habitRepository.createHabit(any()) } returns 42L

            viewModel.addSelectedHabits {}
            testScheduler.advanceUntilIdle()

            coVerify(exactly = 1) { habitRepository.createHabit(match { it.timeOfDay == null }) }
            verify(exactly = 0) {
                habitAlarmManager.scheduleHabitAlarm(any(), any(), any(), any())
            }
        }

    private suspend fun kotlinx.coroutines.test.TestScope.generateSuggestions(
        vararg templates: HabitTemplate,
    ) {
        val goal = "Build a better routine"
        coEvery { extractHabitsUseCase(goal) } returns Result.success(templates.toList())
        viewModel.onQueryChange(goal)
        viewModel.generateSuggestions()
        testScheduler.advanceUntilIdle()
    }

    private fun everyAiFeatureIsReady() {
        every { aiFeatureRepository.availability } returns MutableStateFlow(AiFeatureAvailability.Available)
        every { aiFeatureRepository.isAiNoticeAcknowledged } returns flowOf(true)
        every { aiEngine.state } returns MutableStateFlow(EngineState.Ready)
        coEvery { aiFeatureRepository.isAiFeatureAvailable() } returns true
        coEvery { aiFeatureRepository.isModelDownloaded() } returns true
    }
}
