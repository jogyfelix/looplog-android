package com.jolabs.looplog.storage.util

import androidx.room.TypeConverter
import java.time.DayOfWeek

/**
 * Room type converters for custom data types.
 */
class Converters {

    @TypeConverter
    fun fromDayOfWeek(value: DayOfWeek?): String? {
        return value?.name
    }

    @TypeConverter
    fun toDayOfWeek(value: String?): DayOfWeek? {
        return value?.let { DayOfWeek.valueOf(it) }
    }
}
