package com.jolabs.looplog.util

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class StreakUtilsTest {

    @Test
    fun `formatStreak with Int returns original string up to 999`() {
        assertThat(formatStreak(0)).isEqualTo("0")
        assertThat(formatStreak(1)).isEqualTo("1")
        assertThat(formatStreak(99)).isEqualTo("99")
        assertThat(formatStreak(999)).isEqualTo("999")
    }

    @Test
    fun `formatStreak with Int returns 999+ when greater than 999`() {
        assertThat(formatStreak(1000)).isEqualTo("999+")
        assertThat(formatStreak(1001)).isEqualTo("999+")
        assertThat(formatStreak(12345)).isEqualTo("999+")
    }

    @Test
    fun `formatStreak with String returns original string up to 999`() {
        assertThat(formatStreak("0")).isEqualTo("0")
        assertThat(formatStreak("42")).isEqualTo("42")
        assertThat(formatStreak("999")).isEqualTo("999")
    }

    @Test
    fun `formatStreak with String returns 999+ when greater than 999`() {
        assertThat(formatStreak("1000")).isEqualTo("999+")
        assertThat(formatStreak("9999")).isEqualTo("999+")
    }

    @Test
    fun `formatStreak with null or empty String returns 0`() {
        assertThat(formatStreak(null as String?)).isEqualTo("0")
        assertThat(formatStreak("")).isEqualTo("0")
    }
}
