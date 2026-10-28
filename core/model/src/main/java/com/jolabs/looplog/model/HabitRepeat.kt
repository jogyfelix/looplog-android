package com.jolabs.looplog.model

import java.time.DayOfWeek

data class HabitRepeat(
    val habitId: Long,
    val dayOfWeek: DayOfWeek,
    val timeOfDay : Long?
)
