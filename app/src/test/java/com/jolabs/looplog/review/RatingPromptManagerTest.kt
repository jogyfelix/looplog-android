package com.jolabs.looplog.review

import android.app.Activity
import com.google.common.truth.Truth.assertThat
import com.jolabs.looplog.analytics.AnalyticsTracker
import com.jolabs.looplog.analytics.model.AnalyticsError
import com.jolabs.looplog.analytics.model.ErrorSource
import com.jolabs.looplog.data.repository.AppReviewRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RatingPromptManagerTest {

    private val repository: AppReviewRepository = mockk(relaxUnitFun = true)
    private val analyticsTracker: AnalyticsTracker = mockk(relaxed = true)
    private val playStoreRatingLauncher: PlayStoreRatingLauncher = mockk()

    private val testDispatcher = StandardTestDispatcher()
    private val managerScope = TestScope(testDispatcher)

    private lateinit var manager: RatingPromptManager

    @BeforeEach
    fun setUp() {
        coEvery { repository.getEngagementDayThreshold() } returns 7
        manager = RatingPromptManager(
            repository,
            managerScope,
            analyticsTracker,
            playStoreRatingLauncher,
        )
    }

    @Test
    fun `onEnjoyingYes opens Play Store rating and tracks acceptance`() {
        val activity = mockk<Activity>()
        every { playStoreRatingLauncher.open(activity) } returns Result.success(Unit)

        manager.onEnjoyingYes(activity)

        verify(exactly = 1) { playStoreRatingLauncher.open(activity) }
        verify(exactly = 1) {
            analyticsTracker.trackEvent(ReviewAnalytics.ReviewPromptResult(accepted = true))
        }
    }

    @Test
    fun `onEnjoyingYes reports a soft error when no rating page can open`() {
        val activity = mockk<Activity>()
        val failure = IllegalStateException("No activity can handle rating URL")
        every { playStoreRatingLauncher.open(activity) } returns Result.failure(failure)

        manager.onEnjoyingYes(activity)

        verify(exactly = 1) {
            analyticsTracker.trackError(
                AnalyticsError.Soft(
                    source = ErrorSource("play_store_rating"),
                    throwable = failure,
                    message = "Unable to open Play Store rating page",
                )
            )
        }
    }

    @Test
    fun `onAppOpen marks eligible when threshold reached and prompt not yet shown`() = runTest {
        coEvery { repository.registerOpenDay(any()) } returns 7
        coEvery { repository.isPromptShown() } returns false

        manager.onAppOpen()
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(manager.isEligible.value).isTrue()
        // Eligibility must not burn the one-time flag; that happens only at display time.
        coVerify(exactly = 0) { repository.setPromptShown() }
    }

    @Test
    fun `onAppOpen does not mark eligible below the threshold`() = runTest {
        coEvery { repository.registerOpenDay(any()) } returns 6
        coEvery { repository.isPromptShown() } returns false

        manager.onAppOpen()
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(manager.isEligible.value).isFalse()
    }

    @Test
    fun `onAppOpen honors a Remote Config threshold below the default`() = runTest {
        coEvery { repository.getEngagementDayThreshold() } returns 3
        coEvery { repository.registerOpenDay(any()) } returns 3
        coEvery { repository.isPromptShown() } returns false

        manager.onAppOpen()
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(manager.isEligible.value).isTrue()
    }

    @Test
    fun `onAppOpen does not mark eligible when the prompt was already shown`() = runTest {
        coEvery { repository.registerOpenDay(any()) } returns 10
        coEvery { repository.isPromptShown() } returns true

        manager.onAppOpen()
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(manager.isEligible.value).isFalse()
    }

    @Test
    fun `markPromptShown persists the flag, clears eligibility and tracks the event`() = runTest {
        coEvery { repository.registerOpenDay(any()) } returns 7
        coEvery { repository.isPromptShown() } returns false
        manager.onAppOpen()
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(manager.isEligible.value).isTrue()

        manager.markPromptShown()
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(manager.isEligible.value).isFalse()
        coVerify(exactly = 1) { repository.setPromptShown() }
        verify(exactly = 1) { analyticsTracker.trackEvent(ReviewAnalytics.ReviewPromptShown) }
    }

    @Test
    fun `onEnjoyingNo opens UserJot feedback and tracks rejection`() {
        val activity = mockk<Activity>(relaxed = true)

        manager.onEnjoyingNo(activity)

        verify(exactly = 1) { activity.startActivity(any()) }
        verify(exactly = 1) {
            analyticsTracker.trackEvent(ReviewAnalytics.ReviewPromptResult(accepted = false))
        }
    }
}
