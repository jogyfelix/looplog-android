package com.jolabs.looplog.storage.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import java.time.DayOfWeek

/**
 * Tracks metadata for scheduled alarms to enable verification and recovery.
 *
 * Each row represents one alarm for a specific habit on a specific day of the week.
 * This enables the verification worker to:
 * - Detect missing alarms
 * - Verify alarms are scheduled correctly
 * - Recover from alarm cancellations by the system
 */
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
    indices = [
        Index(value = ["habitId"]),
        Index(value = ["nextFireTime"])
    ]
)
data class AlarmMetadataTable(
    /** The habit this alarm belongs to */
    val habitId: Long,

    /** Day of week this alarm is scheduled for */
    val dayOfWeek: DayOfWeek,

    /** Time of day in milliseconds (epoch millis representing just the time component) */
    val timeOfDay: Long,

    /** Timestamp when this alarm was last scheduled/updated */
    val lastScheduledTime: Long,

    /** Calculated next fire time (epoch millis) for this alarm */
    val nextFireTime: Long,

    /** Whether this alarm is currently active */
    val isActive: Boolean = true
)
