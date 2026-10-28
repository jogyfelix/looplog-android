package com.jolabs.looplog.habit.ui.aiHabitCreate.components

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.jolabs.looplog.habit.R
import com.jolabs.looplog.habit.ui.aiHabitCreate.AiHabitSuggestion
import com.jolabs.looplog.util.DateUtils
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SuggestionCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `edit action invokes edit without toggling the card`() {
        var editClicks = 0
        var cardClicks = 0

        composeTestRule.setContent {
            MaterialTheme {
                SuggestionCard(
                    suggestion = AiHabitSuggestion(
                        id = "one",
                        title = "Drink water",
                        description = "Have a glass",
                        iconRes = R.drawable.start,
                    ),
                    onClick = { cardClicks++ },
                    onDayToggle = {},
                    onEdit = { editClicks++ },
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Edit habit")
            .performClick()

        assertThat(editClicks).isEqualTo(1)
        assertThat(cardClicks).isEqualTo(0)
    }

    @Test
    fun `timed suggestion shows reminder above days with edit on the days row`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val timeOfDay = Calendar.getInstance().apply {
            set(1970, Calendar.JANUARY, 1, 7, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val expectedTime = DateUtils.formatHabitTime(
            timeOfDay,
            DateFormat.is24HourFormat(context),
        )!!

        composeTestRule.setContent {
            MaterialTheme {
                SuggestionCard(
                    suggestion = AiHabitSuggestion(
                        id = "one",
                        title = "Drink water",
                        description = "Have a glass",
                        iconRes = R.drawable.start,
                        timeOfDay = timeOfDay,
                    ),
                    onClick = {},
                    onDayToggle = {},
                    onEdit = {},
                )
            }
        }

        val timeBounds = composeTestRule
            .onNodeWithText(expectedTime, useUnmergedTree = true)
            .fetchSemanticsNode()
            .boundsInRoot
        val dayBounds = composeTestRule.onNodeWithText("M").fetchSemanticsNode().boundsInRoot
        val editBounds = composeTestRule
            .onNodeWithContentDescription("Edit habit")
            .fetchSemanticsNode()
            .boundsInRoot

        assertThat(timeBounds.bottom).isAtMost(dayBounds.top)
        assertThat(editBounds.center.y).isWithin(1f).of(dayBounds.center.y)
    }
}
