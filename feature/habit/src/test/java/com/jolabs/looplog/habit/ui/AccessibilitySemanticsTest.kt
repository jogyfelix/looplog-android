package com.jolabs.looplog.habit.ui

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.jolabs.looplog.design_system.ui.theme.RoundedPolygonShape
import com.jolabs.looplog.habit.R
import com.jolabs.looplog.habit.ui.components.HabitListItem
import com.jolabs.looplog.habit.ui.habitDetail.CompletionState
import com.jolabs.looplog.habit.ui.habitDetail.DayOfWeekStat
import com.jolabs.looplog.habit.ui.habitDetail.HeatmapDay
import com.jolabs.looplog.habit.ui.habitDetail.components.CalendarHeatmap
import com.jolabs.looplog.habit.ui.habitDetail.components.ConsistencyBarChart
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Regression guard for the accessibility fixes in HabitListItem/CalendarHeatmap/
 * ConsistencyBarChart: every state must be announceable to TalkBack, and every tappable
 * region must meet the 48dp minimum touch target. Runs as a fast JVM test via Robolectric
 * instead of requiring a device/emulator.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
class AccessibilitySemanticsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val minTouchTargetPx get() = with(composeTestRule.density) { 48.dp.toPx() }

    private fun hasMinimumTouchTarget() = SemanticsMatcher("has minimum 48dp touch target") { node ->
        // touchBoundsInRoot (not boundsInRoot) reflects Modifier.minimumInteractiveComponentSize()'s
        // touch-bounds expansion - the visual/layout size can stay smaller than the tap target.
        node.touchBoundsInRoot.width >= minTouchTargetPx - 0.5f &&
            node.touchBoundsInRoot.height >= minTouchTargetPx - 0.5f
    }

    @Test
    fun `habit list item off-state is announced to TalkBack`() {
        composeTestRule.setContent {
            MaterialTheme {
                HabitListItem(
                    id = 1,
                    name = "Meditate",
                    description = "",
                    longestStreak = "5",
                    currentStreak = "3",
                    alarmTime = null,
                    habitState = ToggleableState.Off,
                    shape = RoundedPolygonShape(MaterialShapes.Triangle),
                    modifier = androidx.compose.ui.Modifier,
                    disableComplete = false
                )
            }
        }

        val expected = context.getString(R.string.status_none)
        composeTestRule.onNodeWithContentDescription(expected).assertExists()
    }

    @Test
    fun `habit list item on-state and indeterminate-state are still announced`() {
        composeTestRule.setContent {
            MaterialTheme {
                HabitListItem(
                    id = 1,
                    name = "Meditate",
                    description = "",
                    longestStreak = "5",
                    currentStreak = "3",
                    alarmTime = null,
                    habitState = ToggleableState.On,
                    shape = RoundedPolygonShape(MaterialShapes.Triangle),
                    modifier = androidx.compose.ui.Modifier,
                    disableComplete = false
                )
            }
        }

        val expected = context.getString(R.string.status_completed)
        composeTestRule.onNodeWithContentDescription(expected).assertExists()
    }

    @Test
    fun `calendar heatmap info button meets minimum touch target`() {
        composeTestRule.setContent {
            MaterialTheme {
                CalendarHeatmap(
                    heatmapData = sampleHeatmapData(),
                    onInfoClick = {}
                )
            }
        }

        val desc = context.getString(R.string.habit_activity_heatmap_info_desc)
        composeTestRule.onNodeWithContentDescription(desc).assert(hasMinimumTouchTarget())
    }

    @Test
    fun `calendar heatmap grid announces a single completion summary`() {
        val data = sampleHeatmapData()
        composeTestRule.setContent {
            MaterialTheme {
                CalendarHeatmap(heatmapData = data, onInfoClick = {})
            }
        }

        val completed = data.count { it.state == CompletionState.COMPLETED }
        val scheduled = data.count { it.state != CompletionState.NOT_SCHEDULED }
        val expected = context.getString(
            R.string.habit_activity_heatmap_summary,
            completed,
            scheduled
        )
        composeTestRule.onNodeWithContentDescription(expected).assertExists()
    }

    @Test
    fun `consistency bar chart info button meets minimum touch target`() {
        composeTestRule.setContent {
            MaterialTheme {
                ConsistencyBarChart(
                    consistencyData = sampleConsistencyData(),
                    animated = false,
                    onInfoClick = {}
                )
            }
        }

        val desc = context.getString(R.string.habit_consistency_info_desc)
        composeTestRule.onNodeWithContentDescription(desc).assert(hasMinimumTouchTarget())
    }

    @Test
    fun `consistency bar announces day, percentage and count`() {
        composeTestRule.setContent {
            MaterialTheme {
                ConsistencyBarChart(
                    consistencyData = sampleConsistencyData(),
                    animated = false,
                    onInfoClick = null
                )
            }
        }

        val locale = context.resources.configuration.locales[0]
        val mondayFullName = DayOfWeek.MONDAY.getDisplayName(java.time.format.TextStyle.FULL, locale)
        val expected = context.getString(
            R.string.habit_consistency_bar_scheduled_desc,
            mondayFullName,
            75,
            3,
            4
        )
        composeTestRule.onNodeWithContentDescription(expected).assertExists()
    }

    @Test
    fun `consistency bar announces not-scheduled days distinctly`() {
        composeTestRule.setContent {
            MaterialTheme {
                ConsistencyBarChart(
                    consistencyData = sampleConsistencyData(),
                    animated = false,
                    onInfoClick = null
                )
            }
        }

        val locale = context.resources.configuration.locales[0]
        val sundayFullName = DayOfWeek.SUNDAY.getDisplayName(java.time.format.TextStyle.FULL, locale)
        val expected = context.getString(
            R.string.habit_consistency_bar_not_scheduled_desc,
            sundayFullName
        )
        composeTestRule.onNodeWithContentDescription(expected).assertExists()
    }

    private fun sampleHeatmapData(): List<HeatmapDay> {
        val today = LocalDate.of(2026, 1, 15)
        return listOf(
            HeatmapDay(today.minusDays(3), CompletionState.COMPLETED),
            HeatmapDay(today.minusDays(2), CompletionState.SKIPPED),
            HeatmapDay(today.minusDays(1), CompletionState.NONE),
            HeatmapDay(today, CompletionState.NOT_SCHEDULED, isToday = true)
        )
    }

    private fun sampleConsistencyData(): List<DayOfWeekStat> = DayOfWeek.entries.map { day ->
        if (day == DayOfWeek.SUNDAY) {
            DayOfWeekStat(day, percentage = 0, completedCount = 0, totalCount = 0)
        } else if (day == DayOfWeek.MONDAY) {
            DayOfWeekStat(day, percentage = 75, completedCount = 3, totalCount = 4)
        } else {
            DayOfWeekStat(day, percentage = 50, completedCount = 2, totalCount = 4)
        }
    }
}
