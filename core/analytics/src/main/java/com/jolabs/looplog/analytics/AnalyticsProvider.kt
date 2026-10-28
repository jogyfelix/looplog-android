package com.jolabs.looplog.analytics

import com.jolabs.looplog.analytics.model.AnalyticsError
import com.jolabs.looplog.analytics.model.AnalyticsEvent
import com.jolabs.looplog.analytics.model.DegradedState

/**
 * SPI for analytics backends. Register an implementation with Hilt via `@IntoSet` so
 * [AnalyticsTracker] fans out to it; callers never depend on this interface directly.
 *
 * All functions default to no-ops so a provider overrides only what it supports.
 * Implementations must not throw and must not block; heavy work belongs inside the SDK's
 * own queue. Suggested mapping for planned backends:
 * - Crashlytics: Hard → recordException; Soft → log + non-fatal; DegradedState → setCustomKey + log; events → breadcrumb log.
 * - Sentry:      Hard → captureException(ERROR); Soft → captureException/Message(WARNING); DegradedState → captureMessage(WARNING, feature tag); events → addBreadcrumb.
 * - Firebase:    events → logEvent(event.name, event.params); DegradedState → logEvent("degraded_state", ...); errors → no-op.
 * - Mixpanel:    events → track(event.name, event.params); DegradedState → track("degraded_state", ...); errors → no-op.
 */
interface AnalyticsProvider {
    fun trackEvent(event: AnalyticsEvent) {}
    fun trackError(error: AnalyticsError) {}
    fun trackDegradedState(state: DegradedState) {}
}
