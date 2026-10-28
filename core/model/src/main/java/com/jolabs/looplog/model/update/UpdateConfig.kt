package com.jolabs.looplog.model.update

enum class UpdateForceMode {
    AUTO,
    FORCE_FLEXIBLE,
    FORCE_MANDATORY
}

data class UpdateConfig(
    val mandatoryPriorityThreshold: Int,
    val staleDaysThreshold: Int,
    val forceMode: UpdateForceMode,
    val gracePeriodDays: Int
) {
    companion object {
        val DEFAULT = UpdateConfig(
            mandatoryPriorityThreshold = 4,
            staleDaysThreshold = 7,
            forceMode = UpdateForceMode.AUTO,
            gracePeriodDays = 3
        )
    }
}
