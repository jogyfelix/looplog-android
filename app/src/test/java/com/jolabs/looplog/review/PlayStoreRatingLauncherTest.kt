package com.jolabs.looplog.review

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkConstructor
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PlayStoreRatingLauncherTest {

    private val activity: Activity = mockk(relaxed = true)
    private val uri: Uri = mockk()
    private val launcher = PlayStoreRatingLauncher()

    @BeforeEach
    fun setUp() {
        mockkStatic(Uri::class)
        mockkConstructor(Intent::class)
        every { Uri.parse(any()) } returns uri
        every { anyConstructed<Intent>().setPackage(any()) } answers { self as Intent }
        every { activity.packageName } returns "com.jolabs.looplog"
    }

    @AfterEach
    fun tearDown() {
        unmockkConstructor(Intent::class)
        unmockkStatic(Uri::class)
    }

    @Test
    fun `open uses Play Store app when available`() {
        val result = launcher.open(activity)

        assertThat(result.isSuccess).isTrue()
        verify(exactly = 1) { activity.startActivity(any()) }
        verify(exactly = 1) { anyConstructed<Intent>().setPackage("com.android.vending") }
        verify(exactly = 1) { Uri.parse("market://details?id=com.jolabs.looplog") }
        verify(exactly = 0) {
            Uri.parse("https://play.google.com/store/apps/details?id=com.jolabs.looplog")
        }
    }

    @Test
    fun `open falls back to browser when Play Store app is unavailable`() {
        every { activity.startActivity(any()) } throws ActivityNotFoundException() andThen Unit

        val result = launcher.open(activity)

        assertThat(result.isSuccess).isTrue()
        verify(exactly = 2) { activity.startActivity(any()) }
        verify(exactly = 1) { Uri.parse("market://details?id=com.jolabs.looplog") }
        verify(exactly = 1) {
            Uri.parse("https://play.google.com/store/apps/details?id=com.jolabs.looplog")
        }
    }
}
