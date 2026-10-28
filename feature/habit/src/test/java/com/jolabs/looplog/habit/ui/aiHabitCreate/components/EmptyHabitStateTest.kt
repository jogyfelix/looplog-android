package com.jolabs.looplog.habit.ui.aiHabitCreate.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class EmptyHabitStateTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `hidden content removes the empty state text and icon`() {
        composeTestRule.setContent {
            MaterialTheme {
                EmptyHabitState(showContent = false)
            }
        }

        composeTestRule.onAllNodesWithText("What do you want to improve?").assertCountEquals(0)
        composeTestRule.onAllNodesWithText(
            "Start with anything you’d like to change, build, or do more consistently."
        ).assertCountEquals(0)
        composeTestRule.onAllNodesWithContentDescription("No habits found").assertCountEquals(0)
    }
}
