package com.jolabs.looplog.data.repository

import Resource
import com.jolabs.looplog.model.CreateHabit
import com.jolabs.looplog.model.HabitBasic
import com.jolabs.looplog.model.HabitEntryModel
import com.jolabs.looplog.model.HabitRepeat
import kotlinx.coroutines.flow.Flow
import java.time.DayOfWeek

interface HabitRepository {
    suspend fun createHabit(habit: CreateHabit) : Long

    suspend fun deleteHabit(habitId: Long) : Int

    suspend fun getRepeatDaysFromHabit(habitId : Long) : List<HabitRepeat>
    fun getAllHabits() : Flow<Resource<List<HabitBasic>>>
    fun getAllHabitsDirect() : List<HabitBasic>

    suspend fun getHabitById(id : Long) : CreateHabit?
    suspend fun getHabitBasicById(id: Long): HabitBasic?
    fun getHabitByDate(dayOfWeek: DayOfWeek,epochDate : Long) : Flow<Resource<List<HabitBasic>>>

    suspend fun getHabitByDateOnce(dayOfWeek: DayOfWeek, epochDate: Long): List<HabitBasic>

    /**
     * Returns a reactive stream of the overall completion rate (0–100) across the last 7 days.
     * Days with no scheduled habits contribute 0 to both numerator and denominator.
     */
    fun getWeeklyCompletionRate(): Flow<Int>

    /**
     * Returns a reactive count of "perfect days" (all scheduled habits completed) in the last 7 days.
     * Days with no scheduled habits are excluded from the count.
     */
    fun getPerfectDaysCount(): Flow<Int>

    suspend fun upsertHabitEntry(habitEntry: HabitEntryModel)

    // Alarm metadata operations
    suspend fun saveAlarmMetadata(
        habitId: Long,
        dayOfWeek: DayOfWeek,
        timeOfDay: Long,
        nextFireTime: Long
    )

    suspend fun deleteAlarmMetadata(habitId: Long, dayOfWeek: DayOfWeek)

    suspend fun deleteAllAlarmMetadataForHabit(habitId: Long)

    /**
     * Returns all habit entries for a specific habit.
     * Used for calculating habit statistics and heatmap data.
     */
    suspend fun getHabitEntries(habitId: Long): List<HabitEntryModel>
}



