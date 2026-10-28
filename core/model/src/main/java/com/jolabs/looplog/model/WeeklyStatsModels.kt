package com.jolabs.looplog.model

import java.time.LocalDate

enum class DailyCompletionState {
    NONE,       // 0% Completed
    PARTIAL,    // >0% and <100% Completed
    PERFECT     // 100% Completed
}

data class DailyStat(
    val date: LocalDate,
    val state: DailyCompletionState
)
