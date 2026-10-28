package com.jolabs.looplog.analytics

import com.google.common.truth.Truth.assertThat
import com.jolabs.looplog.analytics.model.AnalyticsError
import com.jolabs.looplog.analytics.model.AnalyticsEvent
import com.jolabs.looplog.analytics.model.DegradedState
import com.jolabs.looplog.analytics.model.ErrorSource
import com.jolabs.looplog.analytics.model.Feature
import android.util.Log
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultAnalyticsTrackerTest {

    private val scope = TestScope(UnconfinedTestDispatcher())

    private data class TestEvent(
        override val name: String = "test_event",
        override val params: Map<String, Any> = mapOf("key" to "value")
    ) : AnalyticsEvent

    @Test
    fun `trackEvent fans out to every provider with the same event`() {
        val providerA = mockk<AnalyticsProvider>(relaxed = true)
        val providerB = mockk<AnalyticsProvider>(relaxed = true)
        val tracker = DefaultAnalyticsTracker(setOf(providerA, providerB), scope)
        val event = TestEvent()

        tracker.trackEvent(event)

        verify(exactly = 1) { providerA.trackEvent(event) }
        verify(exactly = 1) { providerB.trackEvent(event) }
    }

    @Test
    fun `trackError and trackDegradedState reach providers`() {
        val provider = mockk<AnalyticsProvider>(relaxed = true)
        val tracker = DefaultAnalyticsTracker(setOf(provider), scope)
        val error = AnalyticsError.Soft(source = ErrorSource("test_source"))
        val state = DegradedState(feature = Feature("test_feature"), reason = "permission revoked")

        tracker.trackError(error)
        tracker.trackDegradedState(state)

        verify(exactly = 1) { provider.trackError(error) }
        verify(exactly = 1) { provider.trackDegradedState(state) }
    }

    @Test
    fun `a throwing provider does not block other providers or the caller`() {
        mockkStatic(Log::class)
        every { Log.w(any(), any<String>(), any()) } returns 0
        try {
            val failing = mockk<AnalyticsProvider>(relaxed = true)
            every { failing.trackEvent(any()) } throws IllegalStateException("boom")
            val healthy = mockk<AnalyticsProvider>(relaxed = true)
            val tracker = DefaultAnalyticsTracker(setOf(failing, healthy), scope)
            val event = TestEvent()

            tracker.trackEvent(event)

            verify(exactly = 1) { healthy.trackEvent(event) }
        } finally {
            unmockkStatic(Log::class)
        }
    }

    @Test
    fun `empty provider set is a no-op`() {
        val tracker = DefaultAnalyticsTracker(emptySet(), scope)

        tracker.trackEvent(TestEvent())
        tracker.trackError(AnalyticsError.Soft(source = ErrorSource.Unknown))
        tracker.trackDegradedState(DegradedState(feature = Feature.Unknown, reason = "none"))
    }

    @Test
    fun `TestEvent exposes wire name and params`() {
        val event = TestEvent(name = "custom_event", params = mapOf("p1" to 123))

        assertThat(event.name).isEqualTo("custom_event")
        assertThat(event.params).containsExactly("p1", 123)
    }
}
