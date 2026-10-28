package com.jolabs.looplog.storage.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import java.time.DayOfWeek

@Entity(
    primaryKeys = ["habitId", "dayOfWeek"],
    foreignKeys = [
        ForeignKey(
            entity = HabitTable::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["habitId"])]
)
data class RepeatTable(
    val habitId: Long,
    val dayOfWeek: DayOfWeek,
    val timeOfDay : Long?
)
