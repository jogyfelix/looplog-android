package com.jolabs.looplog.domain

import com.google.common.truth.Truth.assertThat
import com.jolabs.looplog.analytics.AnalyticsTracker
import com.jolabs.looplog.data.repository.HabitRepository
import com.jolabs.looplog.model.CreateHabit
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.DayOfWeek

class CreateHabitUseCaseTest {

    private val habitRepository: HabitRepository = mockk()
    private val analyticsTracker: AnalyticsTracker = mockk(relaxed = true)
    private lateinit var createHabitUseCase: CreateHabitUseCase

    @BeforeEach
    fun setUp() {
        createHabitUseCase = CreateHabitUseCase(habitRepository, analyticsTracker)
    }

    @Test
    fun `invoke should call habitRepository createHabit and return habitId`() = runTest {
        // Arrange
        val createHabit = CreateHabit(
            id = 0L,
            name = "Exercise",
            description = "Daily morning workout",
            daysOfWeek = listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            timeOfDay = 28800000L, // 8:00 AM
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
        )
        val expectedHabitId = 1L
        coEvery { habitRepository.createHabit(createHabit) } returns expectedHabitId

        // Act
        val actualHabitId = createHabitUseCase(createHabit)

        // Assert
        assertThat(actualHabitId).isEqualTo(expectedHabitId)
        coVerify(exactly = 1) { habitRepository.createHabit(createHabit) }
    }
}
