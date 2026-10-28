package com.jolabs.looplog.habit.ui.aiHabitCreate.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AiHabitQuickGoalsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `clicking a quick goal selects it`() {
        var selectedGoal: String? = null

        composeTestRule.setContent {
            MaterialTheme {
                AiHabitQuickGoals(onGoalClick = { selectedGoal = it })
            }
        }

        val goal = "Build a healthier morning routine"
        composeTestRule.onNodeWithText(goal).assertIsDisplayed().performClick()

        assertThat(selectedGoal).isEqualTo(goal)
    }
}
