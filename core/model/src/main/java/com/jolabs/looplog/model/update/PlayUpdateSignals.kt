package com.jolabs.looplog.model.update

data class PlayUpdateSignals(
    val updateAvailable: Boolean,
    val priority: Int,
    val stalenessDays: Int,
    val isImmediateAllowed: Boolean,
    val isFlexibleAllowed: Boolean
)
