package com.jolabs.looplog.habit.alarmManager

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val WORK_NAME = "reschedule_alarms"

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [33])
class BootReceiverRobolectricTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val config = Configuration.Builder()
            .setExecutor(SynchronousExecutor())
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
    }

    private fun workInfosForRescheduleWork() =
        WorkManager.getInstance(context).getWorkInfosForUniqueWork(WORK_NAME).get()

    @Test
    fun `BOOT_COMPLETED enqueues alarm verification work`() {
        BootReceiver().onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))

        assertThat(workInfosForRescheduleWork()).isNotEmpty()
    }

    @Test
    fun `QUICKBOOT_POWERON enqueues alarm verification work`() {
        BootReceiver().onReceive(context, Intent("android.intent.action.QUICKBOOT_POWERON"))

        assertThat(workInfosForRescheduleWork()).isNotEmpty()
    }

    @Test
    fun `MY_PACKAGE_REPLACED enqueues alarm verification work`() {
        BootReceiver().onReceive(context, Intent(Intent.ACTION_MY_PACKAGE_REPLACED))

        assertThat(workInfosForRescheduleWork()).isNotEmpty()
    }

    @Test
    fun `TIMEZONE_CHANGED enqueues alarm verification work`() {
        BootReceiver().onReceive(context, Intent(Intent.ACTION_TIMEZONE_CHANGED))

        assertThat(workInfosForRescheduleWork()).isNotEmpty()
    }

    @Test
    fun `TIME_SET enqueues alarm verification work`() {
        BootReceiver().onReceive(context, Intent(Intent.ACTION_TIME_CHANGED))

        assertThat(workInfosForRescheduleWork()).isNotEmpty()
    }

    @Test
    fun `an unrelated action does not enqueue alarm verification work`() {
        BootReceiver().onReceive(context, Intent("com.example.SOME_OTHER_ACTION"))

        assertThat(workInfosForRescheduleWork()).isEmpty()
    }
}
