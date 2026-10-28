package com.jolabs.looplog.data.repository

import Resource
import com.jolabs.looplog.data.mapper.toCreateHabit
import com.jolabs.looplog.data.mapper.toDomain
import com.jolabs.looplog.data.mapper.toEntity
import com.jolabs.looplog.data.mapper.toHabitTableEntity
import com.jolabs.looplog.data.mapper.toRepeatModel
import com.jolabs.looplog.storage.dao.HabitDao
import com.jolabs.looplog.storage.entity.HabitEntryStatus
import com.jolabs.looplog.storage.entity.StreakTable
import com.jolabs.looplog.model.CreateHabit
import com.jolabs.looplog.model.HabitBasic
import com.jolabs.looplog.model.HabitEntryModel
import com.jolabs.looplog.model.HabitRepeat
import com.jolabs.looplog.util.DateUtils.toLocalDateFromEpochDays
import com.jolabs.looplog.util.DateUtils.toLocalDateFromEpochMillis
import com.jolabs.looplog.model.HabitStatus
import com.jolabs.looplog.storage.relation.HabitWithDetails
import com.jolabs.looplog.analytics.AnalyticsTracker
import com.jolabs.looplog.analytics.model.AnalyticsError
import com.jolabs.looplog.analytics.model.ErrorSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import kotlin.math.roundToInt

class HabitRepositoryImpl @Inject constructor(
    private val habitDao: HabitDao,
    private val analyticsTracker: AnalyticsTracker
) : HabitRepository {

    /**
     * Sorts habits chronologically by their notification time.
     * Habits with no notification time set appear at the end.
     * Extracts LocalTime from the epoch millis to ignore the date component.
     */
    private val habitComparator = compareBy<HabitBasic> { it.habitTime == null }
        .thenBy { habit ->
            habit.habitTime?.let {
                Instant.ofEpochMilli(it)
                    .atZone(ZoneId.systemDefault())
                    .toLocalTime()
            } ?: LocalTime.MAX
        }
        .thenBy { it.name }

    // A habit's RepeatTable rows apply to every matching day of week regardless of when it was
    // created, so getHabitByDate would otherwise report a habit as "not completed" (dragging
    // down completion rate / perfect-day stats) on days before it existed. Excludes those.
    private fun List<HabitWithDetails>.excludeNotYetCreated(date: LocalDate): List<HabitWithDetails> =
        filter { it.habit.createdAt.toLocalDateFromEpochMillis() <= date }

    override suspend fun createHabit(habit: CreateHabit) : Long {
        return try {
            val habitId = habitDao.upsertHabitWithDetails(habit.toHabitTableEntity(), daysOfWeek = habit.daysOfWeek, timeOfDay = habit.timeOfDay)
            if(habit.id > 0L){
                val(currentStreak,longestStreak)=calculateStreak(habit.id)
                habitDao.upsertHabitStreak(StreakTable(habit.id,currentStreak,longestStreak))
            }
            habitId
        } catch (e: Exception) {
            analyticsTracker.trackError(
                AnalyticsError.Hard(
                    throwable = e,
                    source = ErrorSource.Database,
                    message = "Failed to create/update habit in DB: habitId=${habit.id}"
                )
            )
            throw e
        }
    }

    override suspend fun deleteHabit(habitId: Long) : Int {
        return habitDao.deleteHabit(habitId)
    }

    override suspend fun getRepeatDaysFromHabit(habitId: Long): List<HabitRepeat> {
        return habitDao.getHabitRepetitionById(habitId).map { it.toRepeatModel() }
    }


    override fun getAllHabits(): Flow<Resource<List<HabitBasic>>> = flow {
        emit(Resource.Loading())
        habitDao.getAllHabits().collect { habitsWithDetails ->
            val habits = habitsWithDetails.map { it.toDomain() }
                .sortedWith(habitComparator)
            emit(Resource.Success(habits))
        }
    }.catch { e ->
        emit(Resource.Error(e.localizedMessage ?: "An unexpected error occurred"))
    }

    override fun getAllHabitsDirect(): List<HabitBasic> {
       return habitDao.getAllHabitsDirect().map { it.toDomain() }
           .sortedWith(habitComparator)
    }


    override suspend fun getHabitById(id: Long): CreateHabit? {
        val habit = habitDao.getHabitById(id)
        return habit?.toCreateHabit()
    }

    override suspend fun getHabitBasicById(id: Long): HabitBasic? {
        val habit = habitDao.getHabitById(id)
        return habit?.toDomain()
    }

    override fun getHabitByDate(dayOfWeek: DayOfWeek, epochDate: Long): Flow<Resource<List<HabitBasic>>> = flow {
        emit(Resource.Loading())
        val date = epochDate.toLocalDateFromEpochDays()
         habitDao.getHabitByDate(dayOfWeek, epochDate).collect { habitsWithDetails ->
             val habits = habitsWithDetails.excludeNotYetCreated(date)
                 .map { it.toDomain() }
                 .sortedWith(habitComparator)
             emit(Resource.Success(habits))
         }
    }.catch { e ->
        emit(Resource.Error(e.localizedMessage ?: "An unexpected error occurred"))
    }

    override suspend fun getHabitByDateOnce(
        dayOfWeek: DayOfWeek,
        epochDate: Long
    ): List<HabitBasic> {
        val date = epochDate.toLocalDateFromEpochDays()
        return habitDao.getHabitByDateDirect(dayOfWeek, epochDate)
            .excludeNotYetCreated(date)
            .map { it.toDomain() }
            .sortedWith(habitComparator)
    }

    override fun getWeeklyCompletionRate(): Flow<Int> {
        val today = LocalDate.now()
        // Build one Flow per day for the last 7 days.
        // Each Flow emits a (completedCount, totalCount) pair whenever habit data changes.
        val dayFlows = (6 downTo 0).map { daysAgo ->
            val date = today.minusDays(daysAgo.toLong())
            habitDao.getHabitByDate(date.dayOfWeek, date.toEpochDay())
                .map { habitsWithDetails ->
                    val habits = habitsWithDetails.excludeNotYetCreated(date).map { it.toDomain() }
                    val completed = habits.count { it.habitState == HabitStatus.COMPLETED }
                    completed to habits.size
                }
        }
        // Combine all 7 flows: re-emits whenever any single day changes.
        return combine(dayFlows) { pairs ->
            val total = pairs.sumOf { it.second }
            val completed = pairs.sumOf { it.first }
            if (total == 0) 0 else ((completed.toFloat() / total) * 100).roundToInt()
        }
    }

    override fun getPerfectDaysCount(): Flow<Int> {
        val today = LocalDate.now()
        val dayFlows = (6 downTo 0).map { daysAgo ->
            val date = today.minusDays(daysAgo.toLong())
            habitDao.getHabitByDate(date.dayOfWeek, date.toEpochDay())
                .map { habitsWithDetails ->
                    val habits = habitsWithDetails.excludeNotYetCreated(date).map { it.toDomain() }
                    // A day is "perfect" only when there are scheduled habits AND all are completed
                    habits.isNotEmpty() && habits.all { it.habitState == HabitStatus.COMPLETED }
                }
        }
        return combine(dayFlows) { results -> results.count { it } }
    }

    override suspend fun upsertHabitEntry(habitEntry: HabitEntryModel) {
        try {
            val entity = habitEntry.toEntity()
            habitDao.upsertHabitEntry(entity)
            val(currentStreak,longestStreak) = calculateStreak(entity.habitId)
            habitDao.upsertHabitStreak(StreakTable(entity.habitId,currentStreak,longestStreak))
        } catch (e: Exception) {
            analyticsTracker.trackError(
                AnalyticsError.Soft(
                    throwable = e,
                    source = ErrorSource.Database,
                    message = "Failed to upsert habit entry for ID: ${habitEntry.habitId}"
                )
            )
            throw e
        }
    }


    private suspend fun calculateStreak(habitId: Long): Pair<Int, Int> {
        val allEntries = habitDao.getHabitEntryById(habitId)
        val completedEntries = allEntries.filter { it.isCompleted == HabitEntryStatus.COMPLETED }
        val statusMap = allEntries.associate { it.date.toLocalDateFromEpochDays() to it.isCompleted }

        val repeatDaysSet = habitDao.getHabitRepetitionById(habitId)
            .map { it.dayOfWeek }
            .toSet()

        if (completedEntries.isEmpty()) return 0 to 0

        val validDates = completedEntries
            .map { it.date.toLocalDateFromEpochDays() }
            .sorted()
            .filter { it.dayOfWeek in repeatDaysSet }

        if (validDates.isEmpty()) return 0 to 0

        // -------- Longest streak --------
        var longestStreak = 1
        var tempStreak = 1

        for (i in 1 until validDates.size) {
            if (areConsecutiveValidDays(validDates[i - 1], validDates[i], repeatDaysSet)) {
                tempStreak++
            } else {
                tempStreak = 1
            }
            longestStreak = maxOf(longestStreak, tempStreak)
        }

        // -------- Current streak --------
        var currentStreak = 1
        var lastDate = validDates.last()

        while (true) {
            val prevExpected = getPreviousValidDate(lastDate, repeatDaysSet)
            if (prevExpected != null && validDates.contains(prevExpected)) {
                currentStreak++
                lastDate = prevExpected
            } else {
                break
            }
        }

        val today = LocalDate.now()
        val lastScheduled = getPreviousValidDate(today.plusDays(1), repeatDaysSet)

        if (lastScheduled != null && lastScheduled > validDates.last()) {
            if (lastScheduled < today) {
                // The most recent scheduled day was in the past and wasn't completed
                currentStreak = 0
            } else if (lastScheduled == today) {
                val status = statusMap[today] ?: HabitEntryStatus.NONE
                if (status == HabitEntryStatus.SKIPPED) {
                    // Today is skipped
                    currentStreak = 0
                } else {
                    // Today is NONE, check if there's any missed scheduled day between validDates.last() and today
                    val prevScheduled = getPreviousValidDate(today, repeatDaysSet)
                    if (prevScheduled != null && prevScheduled > validDates.last()) {
                        currentStreak = 0
                    }
                }
            }
        }

        return currentStreak to longestStreak
    }


    private fun areConsecutiveValidDays(prev: LocalDate, next: LocalDate, repeatDays: Set<DayOfWeek>): Boolean {
        var expected = prev
        while (true) {
            expected = expected.plusDays(1)
            if (expected.dayOfWeek in repeatDays) break
        }
        return expected == next
    }

    private fun getPreviousValidDate(fromDate: LocalDate, repeatDays: Set<DayOfWeek>): LocalDate? {
        var prev = fromDate.minusDays(1)
        var counter = 0
        while (prev.dayOfWeek !in repeatDays) {
            prev = prev.minusDays(1)
            counter++
            if (counter > 7) return null // safety guard
        }
        return prev
    }

    // ===== Alarm Metadata Operations =====

    override suspend fun saveAlarmMetadata(
        habitId: Long,
        dayOfWeek: DayOfWeek,
        timeOfDay: Long,
        nextFireTime: Long
    ) {
        val metadata = com.jolabs.looplog.storage.entity.AlarmMetadataTable(
            habitId = habitId,
            dayOfWeek = dayOfWeek,
            timeOfDay = timeOfDay,
            lastScheduledTime = System.currentTimeMillis(),
            nextFireTime = nextFireTime,
            isActive = true
        )
        habitDao.upsertAlarmMetadata(metadata)
    }

    override suspend fun deleteAlarmMetadata(habitId: Long, dayOfWeek: DayOfWeek) {
        habitDao.deleteAlarmMetadata(habitId, dayOfWeek)
    }

    override suspend fun deleteAllAlarmMetadataForHabit(habitId: Long) {
        habitDao.deleteAlarmMetadataForHabit(habitId)
    }

    override suspend fun getHabitEntries(habitId: Long): List<HabitEntryModel> {
        return habitDao.getHabitEntryById(habitId).map {
            HabitEntryModel(
                habitId = it.habitId,
                date = it.date,
                isCompleted = when (it.isCompleted) {
                    HabitEntryStatus.COMPLETED -> HabitStatus.COMPLETED
                    HabitEntryStatus.SKIPPED -> HabitStatus.SKIPPED
                    else -> HabitStatus.NONE
                }
            )
        }
    }

}