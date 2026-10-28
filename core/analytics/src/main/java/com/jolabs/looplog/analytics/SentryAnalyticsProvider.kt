package com.jolabs.looplog.analytics

import com.jolabs.looplog.analytics.model.AnalyticsError
import com.jolabs.looplog.analytics.model.AnalyticsEvent
import com.jolabs.looplog.analytics.model.DegradedState
import io.sentry.Breadcrumb
import io.sentry.Sentry
import io.sentry.SentryEvent
import io.sentry.SentryLevel
import javax.inject.Inject

class SentryAnalyticsProvider @Inject constructor() : AnalyticsProvider {

    override fun trackEvent(event: AnalyticsEvent) {
        val breadcrumb = Breadcrumb().apply {
            category = "analytics"
            message = event.name
            event.params.forEach { (key, value) ->
                setData(key, value)
            }
        }
        Sentry.addBreadcrumb(breadcrumb)
    }

    override fun trackError(error: AnalyticsError) {
        val sentryEvent = SentryEvent(error.throwable).apply {
            level = when (error) {
                is AnalyticsError.Hard -> SentryLevel.ERROR
                is AnalyticsError.Soft -> SentryLevel.WARNING
            }
            message = io.sentry.protocol.Message().apply {
                message = error.message ?: "Analytics Error"
            }
            setTag("source", error.source.key)
        }
        Sentry.captureEvent(sentryEvent)
    }

    override fun trackDegradedState(state: DegradedState) {
        val sentryEvent = SentryEvent().apply {
            level = SentryLevel.WARNING
            message = io.sentry.protocol.Message().apply {
                message = "Degraded State: ${state.reason}"
            }
            setTag("feature", state.feature.key)
        }
        Sentry.captureEvent(sentryEvent)
    }
}
