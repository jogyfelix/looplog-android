package com.jolabs.looplog.domain.update

import com.jolabs.looplog.data.repository.UpdateRepository
import com.jolabs.looplog.model.update.PlayUpdateSignals
import com.jolabs.looplog.model.update.UpdateDecision
import com.jolabs.looplog.model.update.UpdateForceMode
import com.jolabs.looplog.analytics.AnalyticsTracker
import com.jolabs.looplog.analytics.model.AnalyticsError
import com.jolabs.looplog.analytics.model.ErrorSource
import javax.inject.Inject

private const val DAY_MILLIS = 24 * 60 * 60 * 1000L

class EvaluateUpdateDecisionUseCase @Inject constructor(
    private val updateRepository: UpdateRepository,
    private val analyticsTracker: AnalyticsTracker
) {
    suspend operator fun invoke(
        signals: PlayUpdateSignals,
        now: Long = System.currentTimeMillis()
    ): UpdateDecision {
        return try {
            if (!signals.updateAvailable) {
                updateRepository.clearGraceStarted()
                return UpdateDecision.NoUpdate
            }

            val config = updateRepository.getUpdateConfig()
            val wantsMandatory = when (config.forceMode) {
                UpdateForceMode.FORCE_MANDATORY -> true
                UpdateForceMode.FORCE_FLEXIBLE -> false
                UpdateForceMode.AUTO ->
                    signals.priority >= config.mandatoryPriorityThreshold ||
                        signals.stalenessDays >= config.staleDaysThreshold
            }

            if (wantsMandatory && signals.isImmediateAllowed) {
                if (updateRepository.isOnline()) {
                    updateRepository.clearGraceStarted()
                    return UpdateDecision.MandatoryNow
                }

                val graceStartedAt = updateRepository.getGraceStartedAt() ?: now.also {
                    updateRepository.setGraceStartedAt(it)
                }
                val daysElapsed = (now - graceStartedAt) / DAY_MILLIS
                val daysRemaining = config.gracePeriodDays - daysElapsed
                return if (daysRemaining > 0) {
                    UpdateDecision.MandatoryGrace(daysRemaining.toInt())
                } else {
                    UpdateDecision.MandatoryNow
                }
            }

            updateRepository.clearGraceStarted()
            if (signals.isFlexibleAllowed) UpdateDecision.Flexible else UpdateDecision.NoUpdate
        } catch (e: Exception) {
            analyticsTracker.trackError(
                AnalyticsError.Soft(
                    throwable = e,
                    source = ErrorSource.UseCase,
                    message = "Error evaluating update decision"
                )
            )
            UpdateDecision.NoUpdate
        }
    }
}
