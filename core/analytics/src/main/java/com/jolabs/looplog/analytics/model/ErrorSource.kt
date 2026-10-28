package com.jolabs.looplog.analytics.model

/**
 * Represents where an error originated.
 * Features should define their own instances as constants or objects.
 */
@JvmInline
value class ErrorSource(val key: String) {
    companion object {
        val Unknown = ErrorSource("unknown")
        val Database = ErrorSource("database")
        val UseCase = ErrorSource("use_case")
        val RemoteConfig = ErrorSource("remote_config")
    }
}
