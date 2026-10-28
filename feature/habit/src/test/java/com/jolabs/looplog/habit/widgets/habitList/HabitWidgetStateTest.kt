package com.jolabs.looplog.habit.widgets.habitList

import com.google.common.truth.Truth.assertThat
import com.jolabs.looplog.model.HabitBasic
import com.jolabs.looplog.model.HabitStatus
import org.junit.jupiter.api.Test
import java.time.DayOfWeek

class HabitWidgetStateTest {

    @Test
    fun `toDto should correctly map domain habits to DTOs`() {
        val habits = listOf(
            HabitBasic(
                id = 1L,
                name = "Water",
                description = "Drink water",
                longestStreak = "5",
                currentStreak = "3",
                habitTime = 12345L,
                habitState = HabitStatus.COMPLETED,
                repeatDays = listOf(DayOfWeek.MONDAY)
            )
        )

        val dtos = habits.toDto()

        assertThat(dtos).hasSize(1)
        val dto = dtos[0]
        assertThat(dto.id).isEqualTo(1L)
        assertThat(dto.name).isEqualTo("Water")
        assertThat(dto.status).isEqualTo("COMPLETED")
        assertThat(dto.description).isEqualTo("Drink water")
        assertThat(dto.longestStreak).isEqualTo("5")
        assertThat(dto.currentStreak).isEqualTo("3")
    }

    @Test
    fun `toDomain should correctly map DTOs back to domain habits`() {
        val dtos = listOf(
            HabitItemDto(
                id = 2L,
                name = "Exercise",
                status = "SKIPPED",
                description = "Morning run",
                longestStreak = "10",
                currentStreak = "0"
            )
        )

        val habits = dtos.toDomain()

        assertThat(habits).hasSize(1)
        val habit = habits[0]
        assertThat(habit.id).isEqualTo(2L)
        assertThat(habit.name).isEqualTo("Exercise")
        assertThat(habit.habitState).isEqualTo(HabitStatus.SKIPPED)
        assertThat(habit.description).isEqualTo("Morning run")
        assertThat(habit.longestStreak).isEqualTo("10")
        assertThat(habit.currentStreak).isEqualTo("0")
        // Note: DTO doesn't store habitTime or repeatDays as per current implementation
        assertThat(habit.habitTime).isNull()
    }

    @Test
    fun `encode and decode should maintain data integrity`() {
        val originalDtos = listOf(
            HabitItemDto(1L, "Water", "COMPLETED", "Desc", "5", "3"),
            HabitItemDto(2L, "Gym", "NONE", "Desc 2", "10", "1")
        )

        val json = encodeHabits(originalDtos)
        val decodedDtos = decodeHabits(json)

        assertThat(decodedDtos).isEqualTo(originalDtos)
    }

    @Test
    fun `decodeHabits with invalid json should return empty list`() {
        val invalidJson = "{ invalid: json }"
        val result = decodeHabits(invalidJson)
        assertThat(result).isEmpty()
    }
}
