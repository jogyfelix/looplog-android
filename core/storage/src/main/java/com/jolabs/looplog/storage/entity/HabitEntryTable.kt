package com.jolabs.looplog.storage.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(primaryKeys = ["habitId", "date"],
    foreignKeys = [
        ForeignKey(
            entity = HabitTable::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["habitId"])])
data class HabitEntryTable(
    val habitId: Long,
    val date: Long,
    val isCompleted: HabitEntryStatus?,
    val updatedAtMillis: Long
)

enum class HabitEntryStatus {
    NONE,COMPLETED,SKIPPED
}
