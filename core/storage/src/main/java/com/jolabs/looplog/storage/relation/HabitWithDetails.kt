package com.jolabs.looplog.storage.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.jolabs.looplog.storage.entity.HabitEntryTable
import com.jolabs.looplog.storage.entity.HabitTable
import com.jolabs.looplog.storage.entity.RepeatTable
import com.jolabs.looplog.storage.entity.StreakTable

data class HabitWithDetails(
    @Embedded val habit: HabitTable,
    @Embedded val habitEntry: HabitEntryTable?,
    @Relation(
        parentColumn = "id",
        entityColumn = "habitId"
    )
    val habitRepeat: List<RepeatTable>,
    @Relation(
        parentColumn = "id",
        entityColumn = "habitId"
    )
    val habitStreak: StreakTable?
)
