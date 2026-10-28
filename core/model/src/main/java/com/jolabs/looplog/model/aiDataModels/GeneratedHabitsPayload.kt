package com.jolabs.looplog.model.aiDataModels

import kotlinx.serialization.Serializable

@Serializable
data class GeneratedHabit(
    val name: String,
    val description: String,
)

@Serializable
data class GeneratedHabitsPayload(
    val habits: List<GeneratedHabit>,
)
