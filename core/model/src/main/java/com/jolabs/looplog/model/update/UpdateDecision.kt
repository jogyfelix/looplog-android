package com.jolabs.looplog.model.update

sealed class UpdateDecision {
    data object NoUpdate : UpdateDecision()
    data object Flexible : UpdateDecision()
    data object MandatoryNow : UpdateDecision()
    data class MandatoryGrace(val daysRemaining: Int) : UpdateDecision()
}
