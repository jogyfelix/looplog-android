package com.jolabs.looplog.analytics

import android.util.Log
import com.jolabs.looplog.analytics.model.AnalyticsError
import com.jolabs.looplog.analytics.model.AnalyticsEvent
import com.jolabs.looplog.analytics.model.DegradedState
import com.jolabs.looplog.di.AnalyticsScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultAnalyticsTracker @Inject constructor(
    private val providers: Set<@JvmSuppressWildcards AnalyticsProvider>,
    @AnalyticsScope private val scope: CoroutineScope,
) : AnalyticsTracker {

    override fun trackEvent(event: AnalyticsEvent) = dispatch { it.trackEvent(event) }

    override fun trackError(error: AnalyticsError) = dispatch { it.trackError(error) }

    override fun trackDegradedState(state: DegradedState) = dispatch { it.trackDegradedState(state) }

    private fun dispatch(block: (AnalyticsProvider) -> Unit) {
        scope.launch {
            providers.forEach { provider ->
                try {
                    block(provider)
                } catch (e: Exception) {
                    Log.w(TAG, "Analytics provider ${provider.javaClass.simpleName} failed", e)
                }
            }
        }
    }

    private companion object {
        const val TAG = "AnalyticsTracker"
    }
}
