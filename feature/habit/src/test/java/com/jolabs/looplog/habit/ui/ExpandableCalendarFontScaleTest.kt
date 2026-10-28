package com.jolabs.looplog.habit.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import com.jolabs.looplog.habit.ui.components.ExpandableCalendar
import com.jolabs.looplog.habit.ui.components.rememberExpandableCalendarState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * ExpandableCalendar does its own LocalDensity-based layout math (fixed-dp cell/row heights
 * combined with sp-scaling text), which is the highest-risk place in the app for large-font-scale
 * overflow/clipping bugs. This is a coarse regression guard: it doesn't verify pixel-perfect
 * layout, just that composition and layout complete without throwing at 2x font scale.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ExpandableCalendarFontScaleTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `collapsed calendar lays out without crashing at 2x font scale`() {
        composeTestRule.setContent {
            val scaledDensity = Density(
                density = LocalDensity.current.density,
                fontScale = 2f
            )
            CompositionLocalProvider(LocalDensity provides scaledDensity) {
                MaterialTheme {
                    ExpandableCalendar(
                        selectedDate = LocalDate.of(2026, 1, 15),
                        state = rememberExpandableCalendarState(initialExpanded = false)
                    )
                }
            }
        }

        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun `expanded calendar grid lays out without crashing at 2x font scale`() {
        composeTestRule.setContent {
            val scaledDensity = Density(
                density = LocalDensity.current.density,
                fontScale = 2f
            )
            CompositionLocalProvider(LocalDensity provides scaledDensity) {
                MaterialTheme {
                    ExpandableCalendar(
                        selectedDate = LocalDate.of(2026, 1, 15),
                        state = rememberExpandableCalendarState(initialExpanded = true)
                    )
                }
            }
        }

        composeTestRule.onRoot().assertExists()
    }
}
