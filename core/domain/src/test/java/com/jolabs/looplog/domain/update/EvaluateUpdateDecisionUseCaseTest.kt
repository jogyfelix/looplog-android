package com.jolabs.looplog.domain.update

import com.google.common.truth.Truth.assertThat
import com.jolabs.looplog.analytics.AnalyticsTracker
import com.jolabs.looplog.data.repository.UpdateRepository
import com.jolabs.looplog.model.update.PlayUpdateSignals
import com.jolabs.looplog.model.update.UpdateConfig
import com.jolabs.looplog.model.update.UpdateDecision
import com.jolabs.looplog.model.update.UpdateForceMode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private const val DAY_MILLIS = 24 * 60 * 60 * 1000L
private val NOW = 1_000_000_000_000L

class EvaluateUpdateDecisionUseCaseTest {

    private val updateRepository: UpdateRepository = mockk(relaxUnitFun = true)
    private val analyticsTracker: AnalyticsTracker = mockk(relaxed = true)
    private lateinit var evaluateUpdateDecision: EvaluateUpdateDecisionUseCase

    private val defaultConfig = UpdateConfig.DEFAULT // priority>=4, staleness>=7, AUTO, grace=3

    @BeforeEach
    fun setUp() {
        evaluateUpdateDecision = EvaluateUpdateDecisionUseCase(updateRepository, analyticsTracker)
        coEvery { updateRepository.getUpdateConfig() } returns defaultConfig
    }

    @Test
    fun `no update available returns NoUpdate and clears grace state`() = runTest {
        val signals = PlayUpdateSignals(
            updateAvailable = false,
            priority = 0,
            stalenessDays = 0,
            isImmediateAllowed = true,
            isFlexibleAllowed = true
        )

        val decision = evaluateUpdateDecision(signals, NOW)

        assertThat(decision).isEqualTo(UpdateDecision.NoUpdate)
        coVerify(exactly = 1) { updateRepository.clearGraceStarted() }
    }

    @Test
    fun `below thresholds returns Flexible when flexible allowed`() = runTest {
        val signals = PlayUpdateSignals(
            updateAvailable = true,
            priority = 2,
            stalenessDays = 1,
            isImmediateAllowed = true,
            isFlexibleAllowed = true
        )

        val decision = evaluateUpdateDecision(signals, NOW)

        assertThat(decision).isEqualTo(UpdateDecision.Flexible)
    }

    @Test
    fun `high priority and online returns MandatoryNow and clears grace state`() = runTest {
        val signals = PlayUpdateSignals(
            updateAvailable = true,
            priority = 5,
            stalenessDays = 0,
            isImmediateAllowed = true,
            isFlexibleAllowed = true
        )
        every { updateRepository.isOnline() } returns true

        val decision = evaluateUpdateDecision(signals, NOW)

        assertThat(decision).isEqualTo(UpdateDecision.MandatoryNow)
        coVerify(exactly = 1) { updateRepository.clearGraceStarted() }
    }

    @Test
    fun `stale and offline first detection starts grace period`() = runTest {
        val signals = PlayUpdateSignals(
            updateAvailable = true,
            priority = 0,
            stalenessDays = 10,
            isImmediateAllowed = true,
            isFlexibleAllowed = true
        )
        every { updateRepository.isOnline() } returns false
        coEvery { updateRepository.getGraceStartedAt() } returns null

        val decision = evaluateUpdateDecision(signals, NOW)

        assertThat(decision).isEqualTo(UpdateDecision.MandatoryGrace(3))
        coVerify(exactly = 1) { updateRepository.setGraceStartedAt(NOW) }
    }

    @Test
    fun `mandatory offline within grace period returns remaining days`() = runTest {
        val signals = PlayUpdateSignals(
            updateAvailable = true,
            priority = 5,
            stalenessDays = 0,
            isImmediateAllowed = true,
            isFlexibleAllowed = true
        )
        every { updateRepository.isOnline() } returns false
        coEvery { updateRepository.getGraceStartedAt() } returns NOW - DAY_MILLIS // 1 day elapsed

        val decision = evaluateUpdateDecision(signals, NOW)

        assertThat(decision).isEqualTo(UpdateDecision.MandatoryGrace(2))
    }

    @Test
    fun `mandatory offline after grace period expired forces MandatoryNow`() = runTest {
        val signals = PlayUpdateSignals(
            updateAvailable = true,
            priority = 5,
            stalenessDays = 0,
            isImmediateAllowed = true,
            isFlexibleAllowed = true
        )
        every { updateRepository.isOnline() } returns false
        coEvery { updateRepository.getGraceStartedAt() } returns NOW - 3 * DAY_MILLIS // grace exhausted

        val decision = evaluateUpdateDecision(signals, NOW)

        assertThat(decision).isEqualTo(UpdateDecision.MandatoryNow)
    }

    @Test
    fun `force flexible mode never returns mandatory even above thresholds`() = runTest {
        val signals = PlayUpdateSignals(
            updateAvailable = true,
            priority = 5,
            stalenessDays = 10,
            isImmediateAllowed = true,
            isFlexibleAllowed = true
        )
        coEvery { updateRepository.getUpdateConfig() } returns defaultConfig.copy(
            forceMode = UpdateForceMode.FORCE_FLEXIBLE
        )

        val decision = evaluateUpdateDecision(signals, NOW)

        assertThat(decision).isEqualTo(UpdateDecision.Flexible)
    }

    @Test
    fun `force mandatory mode forces mandatory even below thresholds`() = runTest {
        val signals = PlayUpdateSignals(
            updateAvailable = true,
            priority = 0,
            stalenessDays = 0,
            isImmediateAllowed = true,
            isFlexibleAllowed = true
        )
        coEvery { updateRepository.getUpdateConfig() } returns defaultConfig.copy(
            forceMode = UpdateForceMode.FORCE_MANDATORY
        )
        every { updateRepository.isOnline() } returns true

        val decision = evaluateUpdateDecision(signals, NOW)

        assertThat(decision).isEqualTo(UpdateDecision.MandatoryNow)
    }

    @Test
    fun `wants mandatory but immediate not allowed falls back to flexible`() = runTest {
        val signals = PlayUpdateSignals(
            updateAvailable = true,
            priority = 5,
            stalenessDays = 0,
            isImmediateAllowed = false,
            isFlexibleAllowed = true
        )

        val decision = evaluateUpdateDecision(signals, NOW)

        assertThat(decision).isEqualTo(UpdateDecision.Flexible)
    }

    @Test
    fun `no update type allowed returns NoUpdate`() = runTest {
        val signals = PlayUpdateSignals(
            updateAvailable = true,
            priority = 5,
            stalenessDays = 0,
            isImmediateAllowed = false,
            isFlexibleAllowed = false
        )

        val decision = evaluateUpdateDecision(signals, NOW)

        assertThat(decision).isEqualTo(UpdateDecision.NoUpdate)
    }
}
