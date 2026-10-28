package com.jolabs.looplog.model

import java.time.DayOfWeek

data class CreateHabit(
    val id: Long,
    val name: String,
    val description: String,
    val daysOfWeek: List<DayOfWeek>,
    val timeOfDay:Long?,
    val createdAt: Long,
    val updatedAt: Long
)
