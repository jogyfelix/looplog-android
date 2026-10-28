package com.jolabs.looplog.habit.ui.aiHabitCreate.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.hasImeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.input.ImeAction
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AiHabitInputTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `input accepts multiple lines`() {
        val query = mutableStateOf("")

        composeTestRule.setContent {
            MaterialTheme {
                AiHabitInput(
                    query = query.value,
                    onQueryChange = { query.value = it },
                    onGenerate = {},
                    onClear = {},
                )
            }
        }

        val input = composeTestRule.onNode(hasSetTextAction())
        val singleLineHeight = input.fetchSemanticsNode().boundsInRoot.height

        input
            .assert(hasImeAction(ImeAction.Default))
            .performTextInput("Build a morning\nroutine")

        composeTestRule.waitForIdle()

        assertThat(query.value).isEqualTo("Build a morning\nroutine")
        assertThat(input.fetchSemanticsNode().boundsInRoot.height)
            .isAtLeast(singleLineHeight * 1.5f)
    }

    @Test
    fun `empty input shows the goal placeholder`() {
        composeTestRule.setContent {
            MaterialTheme {
                AiHabitInput(
                    query = "",
                    onQueryChange = {},
                    onGenerate = {},
                    onClear = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Enter your goal").assertIsDisplayed()
    }

    @Test
    fun `repeated focus requests focus the input without replacing the goal`() {
        val goal = "Build a reading habit"
        val focusRequestKey = mutableIntStateOf(0)
        var isInputFocused = false
        lateinit var otherFocusRequester: FocusRequester

        composeTestRule.setContent {
            otherFocusRequester = remember { FocusRequester() }
            MaterialTheme {
                Column {
                    AiHabitInput(
                        query = goal,
                        onQueryChange = {},
                        onGenerate = {},
                        onClear = {},
                        canGenerate = true,
                        focusRequestKey = focusRequestKey.intValue,
                        onFocusChange = { isInputFocused = it },
                    )
                    BasicTextField(
                        value = "",
                        onValueChange = {},
                        modifier = Modifier.focusRequester(otherFocusRequester),
                    )
                }
            }
        }

        val input = composeTestRule.onNodeWithText(goal).assertIsDisplayed().assertIsNotFocused()

        composeTestRule.runOnIdle { focusRequestKey.intValue = 1 }
        input.assertIsFocused()
        assertTrue(isInputFocused)

        composeTestRule.runOnIdle { otherFocusRequester.requestFocus() }
        input.assertIsNotFocused()
        assertFalse(isInputFocused)

        composeTestRule.runOnIdle { focusRequestKey.intValue = 2 }
        input.assertIsFocused()
    }
}
