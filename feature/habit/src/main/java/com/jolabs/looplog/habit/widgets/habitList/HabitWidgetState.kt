package com.jolabs.looplog.habit.widgets.habitList

import com.jolabs.looplog.model.HabitBasic
import com.jolabs.looplog.model.HabitStatus
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class HabitItemDto(
    val id: Long,
    val name: String,
    val status: String,
    val description: String,
    val longestStreak: String,
    val currentStreak: String,
)

fun List<HabitBasic>.toDto(): List<HabitItemDto> =
    map { HabitItemDto(it.id, it.name, it.habitState.name, description = it.description, longestStreak = it.longestStreak, currentStreak = it.currentStreak) }

fun List<HabitItemDto>.toDomain(): List<HabitBasic> =
    map { HabitBasic(id = it.id, name = it.name, habitState = HabitStatus.valueOf(it.status), description = it.description, longestStreak = it.longestStreak, currentStreak = it.currentStreak, habitTime = null) }

fun encodeHabits(list: List<HabitItemDto>): String = Json.encodeToString(list)

fun decodeHabits(json: String): List<HabitItemDto> = try {
    Json.decodeFromString(json)
} catch (_: Exception) {
    emptyList()
}


