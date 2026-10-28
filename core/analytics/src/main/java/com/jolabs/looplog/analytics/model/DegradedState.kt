package com.jolabs.looplog.analytics.model

/**
 * A feature is still working but in a reduced capacity, e.g. exact alarms falling back
 * to inexact because the permission was revoked.
 *
 * [reason] is shipped to third-party backends, so it must never contain user-entered
 * content such as habit names — reference ids instead.
 */
data class DegradedState(
    val feature: Feature,
    val reason: String,
    val throwable: Throwable? = null,
)
