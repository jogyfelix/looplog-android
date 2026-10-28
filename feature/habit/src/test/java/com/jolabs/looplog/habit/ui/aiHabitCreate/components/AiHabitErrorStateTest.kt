package com.jolabs.looplog.habit.ui.aiHabitCreate.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AiHabitErrorStateTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `language error uses the shared error layout with custom copy`() {
        val title = "English Only for Now"
        val message = "AI habit generation currently supports English only."

        composeTestRule.setContent {
            MaterialTheme {
                AiHabitErrorState(
                    errorTitle = title,
                    errorMessage = message,
                    onRetryClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText(title).assertIsDisplayed()
        composeTestRule.onNodeWithText(message).assertIsDisplayed()
        composeTestRule.onNodeWithText("Try Again").assertIsDisplayed()
    }
}
