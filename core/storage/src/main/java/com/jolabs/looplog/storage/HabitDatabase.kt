package com.jolabs.looplog.storage

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.jolabs.looplog.storage.dao.HabitDao
import com.jolabs.looplog.storage.entity.AlarmMetadataTable
import com.jolabs.looplog.storage.entity.HabitEntryTable
import com.jolabs.looplog.storage.entity.HabitTable
import com.jolabs.looplog.storage.entity.RepeatTable
import com.jolabs.looplog.storage.entity.StreakTable
import com.jolabs.looplog.storage.util.Converters


@Database(
    entities = [
        HabitTable::class,
        HabitEntryTable::class,
        RepeatTable::class,
        StreakTable::class,
        AlarmMetadataTable::class
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class HabitDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    companion object {
        const val DATABASE_NAME = "habit_db"
    }
}