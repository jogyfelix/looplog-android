package com.jolabs.looplog.model

/**
 * How the biometric / device-credential app lock behaves.
 *
 * Persisted by name (see the app-lock DataStore), so entries may be reordered freely but
 * must not be renamed without a migration.
 */
enum class AppLockMode {
    /** No lock. */
    DISABLED,

    /**
     * Authenticate on launch and again whenever the app returns to the foreground after
     * being backgrounded beyond a short grace period.
     */
    ON_APP_OPEN_AND_RESUME
}
