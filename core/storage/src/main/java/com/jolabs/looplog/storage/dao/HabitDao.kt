package com.jolabs.looplog.storage.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.jolabs.looplog.storage.entity.AlarmMetadataTable
import com.jolabs.looplog.storage.entity.HabitEntryTable
import com.jolabs.looplog.storage.entity.HabitTable
import com.jolabs.looplog.storage.entity.RepeatTable
import com.jolabs.looplog.storage.entity.StreakTable
import com.jolabs.looplog.storage.relation.HabitWithDetails

import kotlinx.coroutines.flow.Flow
import java.time.DayOfWeek

@Dao
interface HabitDao {
    @Upsert
    suspend fun addHabit(habit: HabitTable): Long

    @Query("DELETE FROM HabitTable WHERE id = :habitId")
    suspend fun deleteHabit(habitId: Long): Int

    @Query("DELETE FROM HabitEntryTable WHERE habitId = :habitId AND date = :date")
    suspend fun deleteHabitEntry(habitId: Long, date: Long): Int

    @Upsert
    suspend fun upsertHabitEntry(habitEntry: HabitEntryTable): Long

    @Upsert
    suspend fun addHabitRepetition(habitRepeat: RepeatTable): Long

    @Upsert
    suspend fun upsertHabitStreak(habitStreak: StreakTable): Long

    @Query("DELETE FROM RepeatTable WHERE habitId = :habitId AND dayOfWeek NOT IN (:newDays)")
    suspend fun deleteRemovedDays(habitId: Long, newDays: List<DayOfWeek>)

    @Transaction
    suspend fun upsertHabitWithDetails(
        habitTable: HabitTable,
        daysOfWeek: List<DayOfWeek>,
        timeOfDay: Long?
    ) : Long {
        val habitId = addHabit(habitTable)
        if (habitTable.id == 0L) {
            upsertHabitStreak(
                StreakTable(
                    habitId = habitId,
                    currentStreak = 0,
                    longestStreak = 0,
                )
            )

            daysOfWeek.map {
                addHabitRepetition(
                    RepeatTable(
                        habitId = habitId,
                        dayOfWeek = it,
                        timeOfDay = timeOfDay
                    )
                )
            }
        } else {
            val existingDays = getHabitRepetitionById(habitTable.id)
                .map { it.dayOfWeek }

            if (existingDays != daysOfWeek) {
                deleteRemovedDays(habitTable.id, daysOfWeek)

                val daysToInsert = daysOfWeek.filter { it !in existingDays }
                daysToInsert.forEach { day ->
                    addHabitRepetition(
                        RepeatTable(
                            habitId = habitTable.id,
                            dayOfWeek = day,
                            timeOfDay = timeOfDay
                        )
                    )
                }
            }

               val daysToUpdate = daysOfWeek.filter { it in existingDays }
               daysToUpdate.forEach { day ->
                   updateHabitRepetitionTime(habitTable.id, day, timeOfDay)
               }

        }
        return habitId
    }

    @Query("UPDATE RepeatTable SET timeOfDay = :newTime WHERE habitId = :habitId AND dayOfWeek = :day")
    suspend fun updateHabitRepetitionTime(habitId: Long, day: DayOfWeek, newTime: Long?)


    @Transaction
    @Query("SELECT * FROM HabitTable WHERE id=:habitId")
    suspend fun getHabitById(habitId: Long): HabitWithDetails?

    @Query("SELECT * FROM HabitEntryTable WHERE habitId=:habitId")
    suspend fun getHabitEntryById(habitId: Long): List<HabitEntryTable>

    @Query("SELECT * FROM StreakTable WHERE habitId=:habitId")
    suspend fun getHabitStreakById(habitId: Long): StreakTable

    @Query("SELECT * FROM RepeatTable WHERE habitId=:habitId")
    suspend fun getHabitRepetitionById(habitId: Long): List<RepeatTable>

    @Query("SELECT * FROM HabitEntryTable WHERE habitId=:habitId ORDER BY date DESC LIMIT 1")
    suspend fun getLastHabitEntryById(habitId: Long): HabitEntryTable?

    @Transaction
    @Query("""
        SELECT h.* FROM HabitTable h
        LEFT JOIN (SELECT habitId, MIN(timeOfDay) as timeOfDay FROM RepeatTable GROUP BY habitId) r ON h.id = r.habitId
        ORDER BY CASE WHEN r.timeOfDay IS NULL THEN 1 ELSE 0 END, r.timeOfDay ASC
    """)
    fun getAllHabits(): Flow<List<HabitWithDetails>>


    @Transaction
    @Query("""
        SELECT h.* FROM HabitTable h
        LEFT JOIN (SELECT habitId, MIN(timeOfDay) as timeOfDay FROM RepeatTable GROUP BY habitId) r ON h.id = r.habitId
        ORDER BY CASE WHEN r.timeOfDay IS NULL THEN 1 ELSE 0 END, r.timeOfDay ASC
    """)
    fun getAllHabitsDirect(): List<HabitWithDetails>


    @Transaction
    @Query("SELECT * FROM HabitTable h INNER JOIN RepeatTable r ON h.id = r.habitId LEFT JOIN HabitEntryTable e ON h.id = e.habitId AND e.date = :selectedDate LEFT JOIN StreakTable s ON h.id = s.habitId WHERE r.dayOfWeek = :dayOfWeek ORDER BY  CASE WHEN r.timeOfDay IS NULL THEN 1 ELSE 0 END, r.timeOfDay ASC")
    fun getHabitByDate(
        dayOfWeek: DayOfWeek,
        selectedDate: Long
    ): Flow<List<HabitWithDetails>>

    @Transaction
    @Query("""
  SELECT * FROM HabitTable h
  INNER JOIN RepeatTable r ON h.id = r.habitId
  LEFT JOIN HabitEntryTable e ON h.id = e.habitId AND e.date = :selectedDate
  LEFT JOIN StreakTable s ON h.id = s.habitId
  WHERE r.dayOfWeek = :dayOfWeek
  ORDER BY  CASE WHEN r.timeOfDay IS NULL THEN 1 ELSE 0 END, r.timeOfDay ASC
""")
    suspend fun getHabitByDateDirect(
        dayOfWeek: DayOfWeek,
        selectedDate: Long
    ): List<HabitWithDetails>

    // ===== Alarm Metadata Methods =====

    @Upsert
    suspend fun upsertAlarmMetadata(alarmMetadata: AlarmMetadataTable)

    @Query("SELECT * FROM AlarmMetadataTable WHERE habitId = :habitId")
    suspend fun getAlarmMetadataForHabit(habitId: Long): List<AlarmMetadataTable>

    @Query("SELECT * FROM AlarmMetadataTable WHERE habitId = :habitId AND dayOfWeek = :dayOfWeek")
    suspend fun getAlarmMetadata(habitId: Long, dayOfWeek: DayOfWeek): AlarmMetadataTable?

    @Query("SELECT * FROM AlarmMetadataTable WHERE isActive = 1")
    suspend fun getAllActiveAlarmMetadata(): List<AlarmMetadataTable>

    @Query("DELETE FROM AlarmMetadataTable WHERE habitId = :habitId")
    suspend fun deleteAlarmMetadataForHabit(habitId: Long)

    @Query("DELETE FROM AlarmMetadataTable WHERE habitId = :habitId AND dayOfWeek = :dayOfWeek")
    suspend fun deleteAlarmMetadata(habitId: Long, dayOfWeek: DayOfWeek)

    @Query("UPDATE AlarmMetadataTable SET isActive = :isActive WHERE habitId = :habitId AND dayOfWeek = :dayOfWeek")
    suspend fun updateAlarmActiveStatus(habitId: Long, dayOfWeek: DayOfWeek, isActive: Boolean)

    // ===== Backup / restore (whole-table reads + atomic replace) =====

    @Query("SELECT * FROM HabitTable")
    suspend fun getAllHabitsList(): List<HabitTable>

    @Query("SELECT * FROM HabitEntryTable")
    suspend fun getAllEntries(): List<HabitEntryTable>

    @Query("SELECT * FROM RepeatTable")
    suspend fun getAllRepeats(): List<RepeatTable>

    @Query("SELECT * FROM StreakTable")
    suspend fun getAllStreaks(): List<StreakTable>

    @Insert
    suspend fun insertHabits(habits: List<HabitTable>)

    @Insert
    suspend fun insertEntries(entries: List<HabitEntryTable>)

    @Insert
    suspend fun insertRepeats(repeats: List<RepeatTable>)

    @Insert
    suspend fun insertStreaks(streaks: List<StreakTable>)

    @Query("DELETE FROM HabitTable")
    suspend fun deleteAllHabits()

    /**
     * Atomically replaces all habit data with the given rows. Deleting habits cascades to
     * entries/repeats/streaks/alarm metadata; the provided rows are then inserted with their
     * original ids preserved (the tables are empty, so there are no collisions). Runs in one
     * transaction, so any failure rolls the whole restore back.
     */
    @Transaction
    suspend fun replaceAllData(
        habits: List<HabitTable>,
        entries: List<HabitEntryTable>,
        repeats: List<RepeatTable>,
        streaks: List<StreakTable>
    ) {
        deleteAllHabits()
        insertHabits(habits)
        insertRepeats(repeats)
        insertStreaks(streaks)
        insertEntries(entries)
    }
}

