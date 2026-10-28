package com.jolabs.looplog.model

/**
 * User-selected accent color. Identity only — the actual color values live in the
 * design-system's accent presets. [SYSTEM] keeps the app's default behavior (wallpaper
 * dynamic color on Android 12+, static scheme otherwise). Persisted by name.
 */
enum class AppAccent {
    SYSTEM,
    BLUE,
    VIOLET,
    TEAL,
    AMBER,
    ROSE
}
