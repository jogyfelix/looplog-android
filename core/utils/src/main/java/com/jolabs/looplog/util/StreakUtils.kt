package com.jolabs.looplog.util

/**
 * Formats streak counts for UI display.
 * Caps values over 999 to "999+".
 */
fun formatStreak(streak: Int): String {
    return if (streak > 999) "999+" else streak.toString()
}

/**
 * Formats string representations of streak counts for UI display.
 * Caps numeric values over 999 to "999+".
 */
fun formatStreak(streak: String?): String {
    if (streak.isNullOrEmpty()) return "0"
    val count = streak.toIntOrNull()
    return count?.let { formatStreak(it) } ?: streak
}
