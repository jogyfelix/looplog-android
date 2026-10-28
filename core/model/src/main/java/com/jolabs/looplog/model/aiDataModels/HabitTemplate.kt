package com.jolabs.looplog.model.aiDataModels

data class HabitTemplate(
    val id: String,
    val title: String,
    val description: String = "",
    val frequency: Frequency = Frequency.DAILY,
) {
    enum class Frequency { DAILY, WEEKLY }
}
