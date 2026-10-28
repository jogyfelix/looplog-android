package com.jolabs.looplog.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.jolabs.looplog.analytics.model.AnalyticsEvent
import com.jolabs.looplog.analytics.model.DegradedState
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Product-analytics backend. Events go to Google Analytics for aggregation
 * (funnels, paths, user explorer); errors stay a no-op because Sentry owns
 * error reporting via [SentryAnalyticsProvider].
 */
class FirebaseAnalyticsProvider @Inject constructor(
    @ApplicationContext context: Context,
) : AnalyticsProvider {

    private val firebaseAnalytics = FirebaseAnalytics.getInstance(context)

    override fun trackEvent(event: AnalyticsEvent) {
        firebaseAnalytics.logEvent(event.name, event.params.toBundle())
    }

    override fun trackDegradedState(state: DegradedState) {
        firebaseAnalytics.logEvent(
            "degraded_state",
            Bundle().apply {
                putString("feature", state.feature.key)
                putString("reason", state.reason.take(MAX_PARAM_VALUE_LENGTH))
            },
        )
    }

    private fun Map<String, Any>.toBundle() = Bundle().apply {
        forEach { (key, value) ->
            when (value) {
                is String -> putString(key, value.take(MAX_PARAM_VALUE_LENGTH))
                is Int -> putLong(key, value.toLong())
                is Long -> putLong(key, value)
                is Float -> putDouble(key, value.toDouble())
                is Double -> putDouble(key, value)
                else -> putString(key, value.toString().take(MAX_PARAM_VALUE_LENGTH))
            }
        }
    }

    private companion object {
        /** GA4 drops event params whose string values exceed 100 characters. */
        const val MAX_PARAM_VALUE_LENGTH = 100
    }
}
