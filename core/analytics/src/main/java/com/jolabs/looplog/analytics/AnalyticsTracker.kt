package com.jolabs.looplog.analytics

import com.jolabs.looplog.analytics.model.AnalyticsError
import com.jolabs.looplog.analytics.model.AnalyticsEvent
import com.jolabs.looplog.analytics.model.DegradedState

/**
 * Entry point for analytics. Inject this into ViewModels, workers, receivers, etc.
 * Calls are fire-and-forget and safe to make from any thread.
 */
interface AnalyticsTracker {
    fun trackEvent(event: AnalyticsEvent)
    fun trackError(error: AnalyticsError)
    fun trackDegradedState(state: DegradedState)
}
