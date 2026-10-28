package com.jolabs.looplog.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.jolabs.looplog.storage.HabitDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context : Context) : HabitDatabase {
        /**
         * Migration from version 2 to 3: Add AlarmMetadataTable
         *
         * This table tracks scheduled alarms for verification and recovery.
         */
        val migration2To3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create AlarmMetadataTable
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS AlarmMetadataTable (
                        habitId INTEGER NOT NULL,
                        dayOfWeek TEXT NOT NULL,
                        timeOfDay INTEGER NOT NULL,
                        lastScheduledTime INTEGER NOT NULL,
                        nextFireTime INTEGER NOT NULL,
                        isActive INTEGER NOT NULL DEFAULT 1,
                        PRIMARY KEY(habitId, dayOfWeek),
                        FOREIGN KEY(habitId) REFERENCES HabitTable(id) ON DELETE CASCADE
                    )
                """.trimIndent())

                // Create indices for better query performance
                db.execSQL("CREATE INDEX IF NOT EXISTS index_AlarmMetadataTable_habitId ON AlarmMetadataTable(habitId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_AlarmMetadataTable_nextFireTime ON AlarmMetadataTable(nextFireTime)")
            }
        }

        return Room.databaseBuilder(
            context,
            HabitDatabase::class.java,
            HabitDatabase.DATABASE_NAME
        )
            .addMigrations(migration2To3)
            .build()
    }

    @Provides
    @Singleton
    fun provideHabitDao(habitDatabase: HabitDatabase) = habitDatabase.habitDao()

}