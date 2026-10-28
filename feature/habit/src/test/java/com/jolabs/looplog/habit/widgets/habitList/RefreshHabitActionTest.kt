package com.jolabs.looplog.habit.widgets.habitList

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import com.jolabs.looplog.habit.utils.WidgetUpdateUtil
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RefreshHabitActionTest {

    private val context: Context = mockk(relaxed = true)
    private val glanceId: GlanceId = mockk()
    private val parameters: ActionParameters = mockk()

    @BeforeEach
    fun setUp() {
        mockkObject(WidgetUpdateUtil)
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(WidgetUpdateUtil)
    }

    @Test
    fun `onAction should call WidgetUpdateUtil refreshHabitWidgets`() = runTest {
        // Arrange
        coEvery { WidgetUpdateUtil.refreshHabitWidgets(any()) } returns Unit
        val action = RefreshHabitAction()

        // Act
        action.onAction(context, glanceId, parameters)

        // Assert
        coVerify { WidgetUpdateUtil.refreshHabitWidgets(context) }
    }
}
