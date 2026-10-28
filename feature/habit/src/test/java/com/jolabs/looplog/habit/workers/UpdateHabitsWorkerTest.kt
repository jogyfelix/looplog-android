package com.jolabs.looplog.habit.workers

import android.content.Context
import android.util.Log
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.google.common.truth.Truth.assertThat
import com.jolabs.looplog.data.repository.HabitRepository
import com.jolabs.looplog.habit.utils.WidgetUpdateUtil
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UpdateHabitsWorkerTest {

    private val context: Context = mockk(relaxed = true)
    private val workerParams: WorkerParameters = mockk(relaxed = true)
    private val habitRepository: HabitRepository = mockk()

    @BeforeEach
    fun setup() {
        mockkObject(WidgetUpdateUtil)
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any<String>()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(WidgetUpdateUtil)
        unmockkStatic(Log::class)
    }

    @Test
    fun `test UpdateHabitsWorker success`() = runTest {
        // Arrange
        coEvery { WidgetUpdateUtil.refreshHabitWidgets(any(), any()) } returns Unit
        
        val worker = UpdateHabitsWorker(context, workerParams, habitRepository)

        // Act
        val result = worker.doWork()

        // Assert
        assertThat(result).isEqualTo(ListenableWorker.Result.success())
    }

    @Test
    fun `test UpdateHabitsWorker retries after refresh failure`() = runTest {
        coEvery { WidgetUpdateUtil.refreshHabitWidgets(any(), any()) } throws RuntimeException("temporary")

        val worker = UpdateHabitsWorker(context, workerParams, habitRepository)

        assertThat(worker.doWork()).isEqualTo(ListenableWorker.Result.retry())
    }
}
