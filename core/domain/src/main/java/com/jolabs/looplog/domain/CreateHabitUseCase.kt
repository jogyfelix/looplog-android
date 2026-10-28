package com.jolabs.looplog.domain

import com.jolabs.looplog.data.repository.HabitRepository
import com.jolabs.looplog.model.CreateHabit
import com.jolabs.looplog.analytics.AnalyticsTracker
import com.jolabs.looplog.analytics.model.AnalyticsError
import com.jolabs.looplog.analytics.model.ErrorSource
import javax.inject.Inject

class CreateHabitUseCase @Inject constructor(
    private val habitRepository: HabitRepository,
    private val analyticsTracker: AnalyticsTracker
) {
    suspend operator fun invoke (habit: CreateHabit) : Long {
        return try {
            habitRepository.createHabit(habit)
        } catch (e: Exception) {
            analyticsTracker.trackError(
                AnalyticsError.Hard(
                    throwable = e,
                    source = ErrorSource.UseCase,
                    message = "Workflow failure: CreateHabitUseCase"
                )
            )
            throw e
        }
    }
}