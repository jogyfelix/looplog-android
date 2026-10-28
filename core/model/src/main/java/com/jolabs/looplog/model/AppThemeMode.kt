package com.jolabs.looplog.model

/**
 * User-selected light/dark preference. Persisted by name, so entries may be reordered but
 * not renamed without a migration.
 */
enum class AppThemeMode {
    /** Follow the system light/dark setting. */
    SYSTEM,
    LIGHT,
    DARK
}
