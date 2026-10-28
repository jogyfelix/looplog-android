package com.jolabs.looplog.analytics.model

/**
 * Represents an app feature area.
 * Features should define their own instances as constants or objects.
 */
@JvmInline
value class Feature(val key: String) {
    companion object {
        val Unknown = Feature("unknown")
    }
}
