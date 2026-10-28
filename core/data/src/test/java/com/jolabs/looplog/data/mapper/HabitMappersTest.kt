package com.jolabs.looplog.data.mapper

import com.google.common.truth.Truth.assertThat
import com.jolabs.looplog.storage.entity.HabitEntryStatus
import com.jolabs.looplog.storage.entity.HabitEntryTable
import com.jolabs.looplog.storage.entity.HabitTable
import com.jolabs.looplog.storage.entity.RepeatTable
import com.jolabs.looplog.storage.entity.StreakTable
import com.jolabs.looplog.storage.relation.HabitWithDetails
import com.jolabs.looplog.model.CreateHabit
import com.jolabs.looplog.model.HabitEntryModel
import com.jolabs.looplog.model.HabitStatus
import org.junit.jupiter.api.Test
import java.time.DayOfWeek

class HabitMappersTest {

    @Test
    fun `CreateHabit toHabitTableEntity should map correctly`() {
        val createHabit = CreateHabit(
            id = 1L,
            name = "Exercise",
            description = "Daily run",
            daysOfWeek = listOf(DayOfWeek.MONDAY),
            timeOfDay = 3600000L,
            createdAt = 1000L,
            updatedAt = 2000L
        )

        val entity = createHabit.toHabitTableEntity()

        assertThat(entity.id).isEqualTo(1L)
        assertThat(entity.name).isEqualTo("Exercise")
        assertThat(entity.description).isEqualTo("Daily run")
        assertThat(entity.createdAt).isEqualTo(1000L)
        assertThat(entity.updatedAt).isEqualTo(2000L)
    }

    @Test
    fun `HabitWithDetails toDomain should map correctly`() {
        val habitTable = HabitTable(1L, "Exercise", "Daily run", 1000L, 2000L)
        val repeatTable = listOf(RepeatTable(1L, DayOfWeek.MONDAY, 3600000L))
        val habitEntry = HabitEntryTable(1L, 12345L, HabitEntryStatus.COMPLETED, 1500L)
        val streakTable = StreakTable(1L, 5, 10)

        val habitWithDetails = HabitWithDetails(
            habit = habitTable,
            habitEntry = habitEntry,
            habitRepeat = repeatTable,
            habitStreak = streakTable
        )

        val domain = habitWithDetails.toDomain()

        assertThat(domain.id).isEqualTo(1L)
        assertThat(domain.name).isEqualTo("Exercise")
        assertThat(domain.description).isEqualTo("Daily run")
        assertThat(domain.habitTime).isEqualTo(3600000L)
        assertThat(domain.currentStreak).isEqualTo("5")
        assertThat(domain.longestStreak).isEqualTo("10")
        assertThat(domain.habitState).isEqualTo(HabitStatus.COMPLETED)
        assertThat(domain.repeatDays).containsExactly(DayOfWeek.MONDAY)
    }

    @Test
    fun `HabitWithDetails toCreateHabit should map correctly`() {
        val habitTable = HabitTable(1L, "Exercise", "Daily run", 1000L, 2000L)
        val repeatTable = listOf(RepeatTable(1L, DayOfWeek.MONDAY, 3600000L))
        val streakTable = StreakTable(1L, 5, 10)

        val habitWithDetails = HabitWithDetails(
            habit = habitTable,
            habitEntry = null,
            habitRepeat = repeatTable,
            habitStreak = streakTable
        )

        val createHabit = habitWithDetails.toCreateHabit()

        assertThat(createHabit.id).isEqualTo(1L)
        assertThat(createHabit.name).isEqualTo("Exercise")
        assertThat(createHabit.description).isEqualTo("Daily run")
        assertThat(createHabit.daysOfWeek).containsExactly(DayOfWeek.MONDAY)
        assertThat(createHabit.timeOfDay).isEqualTo(3600000L)
        assertThat(createHabit.createdAt).isEqualTo(1000L)
        assertThat(createHabit.updatedAt).isEqualTo(2000L)
    }

    @Test
    fun `HabitEntryStatus toHabitStatus should map correctly`() {
        assertThat(HabitEntryStatus.NONE.toHabitStatus()).isEqualTo(HabitStatus.NONE)
        assertThat(HabitEntryStatus.COMPLETED.toHabitStatus()).isEqualTo(HabitStatus.COMPLETED)
        assertThat(HabitEntryStatus.SKIPPED.toHabitStatus()).isEqualTo(HabitStatus.SKIPPED)
    }

    @Test
    fun `HabitStatus toEntity should map correctly`() {
        assertThat(HabitStatus.NONE.toEntity()).isEqualTo(HabitEntryStatus.NONE)
        assertThat(HabitStatus.COMPLETED.toEntity()).isEqualTo(HabitEntryStatus.COMPLETED)
        assertThat(HabitStatus.SKIPPED.toEntity()).isEqualTo(HabitEntryStatus.SKIPPED)
    }

    @Test
    fun `HabitEntryModel toEntity should map correctly`() {
        val model = HabitEntryModel(1L, 12345L, HabitStatus.COMPLETED)
        val entity = model.toEntity()

        assertThat(entity.habitId).isEqualTo(1L)
        assertThat(entity.date).isEqualTo(12345L)
        assertThat(entity.isCompleted).isEqualTo(HabitEntryStatus.COMPLETED)
    }

    @Test
    fun `RepeatTable toRepeatModel should map correctly`() {
        val entity = RepeatTable(1L, DayOfWeek.MONDAY, 3600000L)
        val model = entity.toRepeatModel()

        assertThat(model.habitId).isEqualTo(1L)
        assertThat(model.dayOfWeek).isEqualTo(DayOfWeek.MONDAY)
        assertThat(model.timeOfDay).isEqualTo(3600000L)
    }
}
