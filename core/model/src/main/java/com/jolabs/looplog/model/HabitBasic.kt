package com.jolabs.looplog.model

import java.time.DayOfWeek

data class HabitBasic(
    val id: Long,
    val name: String,
    val description: String,
    val longestStreak:String,
    val currentStreak: String,
    val habitTime: Long?,
    val habitState: HabitStatus,
    val repeatDays: List<DayOfWeek> = emptyList()
)

enum class HabitStatus {
    NONE,COMPLETED,SKIPPED
}