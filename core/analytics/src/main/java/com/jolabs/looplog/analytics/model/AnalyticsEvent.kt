package com.jolabs.looplog.analytics.model

/**
 * Interface for analytics events. Each event carries its wire [name] and exposes its
 * fields through [params] so providers can serialize any event generically.
 *
 * Implementations should be defined within the feature modules that use them.
 *
 * [params] must never contain user-entered content (habit names, descriptions, etc.) —
 * everything here is shipped to third-party backends. Identify entities by id and
 * describe them with enums/counts/booleans instead.
 */
interface AnalyticsEvent {
    val name: String
    val params: Map<String, Any> get() = emptyMap()
}
