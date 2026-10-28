package com.jolabs.looplog.review

import com.jolabs.looplog.analytics.model.AnalyticsEvent

/**
 * Analytics definitions for the rating nudge.
 */
object ReviewAnalytics {

    data object ReviewPromptShown : AnalyticsEvent {
        override val name = "review_prompt_shown"
    }

    data class ReviewPromptResult(val accepted: Boolean) : AnalyticsEvent {
        override val name = "review_prompt_result"
        override val params = mapOf("accepted" to accepted)
    }
}
