package com.jolabs.looplog.analytics

import android.util.Log
import com.jolabs.looplog.analytics.model.AnalyticsError
import com.jolabs.looplog.analytics.model.AnalyticsEvent
import com.jolabs.looplog.analytics.model.DegradedState
import javax.inject.Inject

/** Debug-only provider that mirrors everything to Logcat under the "Analytics" tag. */
class LogcatAnalyticsProvider @Inject constructor() : AnalyticsProvider {

    override fun trackEvent(event: AnalyticsEvent) {
        Log.d(TAG, "event=${event.name} params=${event.params}")
    }

    override fun trackError(error: AnalyticsError) {
        val kind = when (error) {
            is AnalyticsError.Hard -> "hard"
            is AnalyticsError.Soft -> "soft"
        }
        Log.w(TAG, "error=$kind source=${error.source} message=${error.message}", error.throwable)
    }

    override fun trackDegradedState(state: DegradedState) {
        Log.w(TAG, "degraded feature=${state.feature} reason=${state.reason}", state.throwable)
    }

    private companion object {
        const val TAG = "Analytics"
    }
}
