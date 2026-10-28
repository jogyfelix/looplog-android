package com.jolabs.looplog.habit.ui.aiHabitCreate.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performTextReplacement
import com.google.common.truth.Truth.assertThat
import com.jolabs.looplog.habit.R
import com.jolabs.looplog.habit.ui.aiHabitCreate.AiHabitSuggestion
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AiHabitEditBottomSheetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `sheet starts from suggestion and saves the edited draft`() {
        var savedTitle: String? = null
        var savedDescription: String? = null

        composeTestRule.setContent {
            MaterialTheme {
                AiHabitEditBottomSheet(
                    suggestion = suggestion(),
                    habitAlarmManager = null,
                    snackbarHostState = SnackbarHostState(),
                    onDismiss = {},
                    onSave = { title, description, _ ->
                        savedTitle = title
                        savedDescription = description
                    },
                )
            }
        }
        composeTestRule.mainClock.advanceTimeBy(1_000)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Drink water").assertIsDisplayed()
        composeTestRule.onNodeWithText("Have a glass after waking").assertIsDisplayed()
        composeTestRule.onNodeWithText("Drink water").performTextReplacement("Morning water")
        composeTestRule.onNodeWithText("Have a glass after waking").performTextReplacement("One glass")
        composeTestRule.onNodeWithTag("aiHabitEditSave")
            .performScrollTo()
            .performSemanticsAction(SemanticsActions.OnClick)

        composeTestRule.runOnIdle {
            assertThat(savedTitle).isEqualTo("Morning water")
            assertThat(savedDescription).isEqualTo("One glass")
        }
    }

    @Test
    fun `blank title disables save`() {
        composeTestRule.setContent {
            MaterialTheme {
                AiHabitEditBottomSheet(
                    suggestion = suggestion(),
                    habitAlarmManager = null,
                    snackbarHostState = SnackbarHostState(),
                    onDismiss = {},
                    onSave = { _, _, _ -> },
                )
            }
        }
        composeTestRule.mainClock.advanceTimeBy(1_000)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("aiHabitEditSave").assertIsEnabled()
        composeTestRule.onNodeWithText("Drink water").performTextReplacement("   ")
        composeTestRule.onNodeWithTag("aiHabitEditSave").assertIsNotEnabled()
    }

    private fun suggestion() = AiHabitSuggestion(
        id = "one",
        title = "Drink water",
        description = "Have a glass after waking",
        iconRes = R.drawable.start,
    )
}
