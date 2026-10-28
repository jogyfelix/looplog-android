package com.jolabs.looplog.analytics.model

/**
 * [message] (and any [throwable] message) is shipped to third-party backends, so it
 * must never contain user-entered content such as habit names — reference ids instead.
 */
sealed class AnalyticsError {
    abstract val source: ErrorSource
    abstract val throwable: Throwable?
    abstract val message: String?

    /** Unrecoverable failure — the feature broke for the user. */
    data class Hard(
        override val throwable: Throwable,
        override val source: ErrorSource,
        override val message: String? = null,
    ) : AnalyticsError()

    /** Recovered or fallback-handled failure — user impact was limited. */
    data class Soft(
        override val source: ErrorSource,
        override val throwable: Throwable? = null,
        override val message: String? = null,
    ) : AnalyticsError()
}
