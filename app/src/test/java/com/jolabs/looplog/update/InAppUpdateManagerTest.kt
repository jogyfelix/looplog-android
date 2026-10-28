package com.jolabs.looplog.update

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.requestAppUpdateInfo
import com.google.common.truth.Truth.assertThat
import com.jolabs.looplog.data.repository.UpdateRepository
import com.jolabs.looplog.domain.update.EvaluateUpdateDecisionUseCase
import com.jolabs.looplog.model.update.UpdateConfig
import com.jolabs.looplog.model.update.UpdateDecision
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private const val DAY_MILLIS = 24 * 60 * 60 * 1000L

class InAppUpdateManagerTest {

    private val packageManager: PackageManager = mockk()
    private val context: Context = mockk {
        every { packageManager } returns this@InAppUpdateManagerTest.packageManager
    }
    private val appUpdateManager: AppUpdateManager = mockk(relaxed = true)
    private val evaluateUpdateDecision: EvaluateUpdateDecisionUseCase = mockk()
    private val updateRepository: UpdateRepository = mockk(relaxUnitFun = true)
    private val listenerSlot = slot<InstallStateUpdatedListener>()

    // A real Dispatchers.Default-backed scope would make onDownloadFailed's background
    // coroutine non-deterministic to assert on; this lets tests advance virtual time instead.
    private val testDispatcher = StandardTestDispatcher()
    private val managerScope = TestScope(testDispatcher)

    private lateinit var manager: InAppUpdateManager

    @BeforeEach
    fun setUp() {
        // requestAppUpdateInfo() is a top-level ktx extension, not a member of AppUpdateManager,
        // so it has to be mocked statically rather than stubbed on the appUpdateManager mock.
        mockkStatic("com.google.android.play.core.ktx.AppUpdateManagerKtxKt")
        // android.util.Log is not available in local unit tests.
        mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any(), any<String>()) } returns 0
        every { android.util.Log.w(any(), any<String>()) } returns 0
        // ApplicationInfo can't be constructed in local unit tests (android.jar stubs throw),
        // so use a mockk instance and set the public field directly.
        val playStoreInfo = mockk<ApplicationInfo>().also { it.enabled = true }
        every { packageManager.getApplicationInfo("com.android.vending", 0) } returns playStoreInfo
        every { appUpdateManager.registerListener(capture(listenerSlot)) } just Runs
        manager = InAppUpdateManager(context, appUpdateManager, evaluateUpdateDecision, updateRepository, managerScope)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `evaluate skips Play Core entirely when the Play Store is not installed`() = runTest {
        // Play Core 2.1.0 crashes fatally on its own handler thread when it can't bind to
        // the Play Store service, so the only safe behavior is to never call it at all.
        every { packageManager.getApplicationInfo("com.android.vending", 0) } throws
            PackageManager.NameNotFoundException()

        val decision = manager.evaluate()

        assertThat(decision).isEqualTo(UpdateDecision.NoUpdate)
        coVerify(exactly = 0) { appUpdateManager.requestAppUpdateInfo() }
    }

    @Test
    fun `evaluate skips Play Core entirely when the Play Store is disabled`() = runTest {
        val disabledInfo = mockk<ApplicationInfo>().also { it.enabled = false }
        every { packageManager.getApplicationInfo("com.android.vending", 0) } returns disabledInfo

        val decision = manager.evaluate()

        assertThat(decision).isEqualTo(UpdateDecision.NoUpdate)
        coVerify(exactly = 0) { appUpdateManager.requestAppUpdateInfo() }
    }

    @Test
    fun `evaluate returns NoUpdate instead of crashing when Play Core fails to fetch update info`() = runTest {
        // Reproduces the "Failed to bind to the service" crash seen on debug/sideloaded
        // installs and emulators without Play Store.
        coEvery { appUpdateManager.requestAppUpdateInfo() } throws
            RuntimeException("Failed to bind to the service.")

        val decision = manager.evaluate()

        assertThat(decision).isEqualTo(UpdateDecision.NoUpdate)
        assertThat(manager.decision.value).isEqualTo(UpdateDecision.NoUpdate)
        coVerify(exactly = 0) { evaluateUpdateDecision(any(), any()) }
    }

    @Test
    fun `evaluate rethrows CancellationException instead of swallowing it`() = runTest {
        coEvery { appUpdateManager.requestAppUpdateInfo() } throws CancellationException("cancelled")

        var caught: CancellationException? = null
        try {
            manager.evaluate()
        } catch (e: CancellationException) {
            caught = e
        }

        assertThat(caught).isNotNull()
    }

    @Test
    fun `evaluate sets readyToInstall when installStatus is DOWNLOADED`() = runTest {
        val info: AppUpdateInfo = mockk(relaxed = true)
        every { info.installStatus() } returns InstallStatus.DOWNLOADED
        coEvery { appUpdateManager.requestAppUpdateInfo() } returns info
        coEvery { evaluateUpdateDecision(any(), any()) } returns UpdateDecision.Flexible

        manager.evaluate()

        assertThat(manager.readyToInstall.value).isTrue()
    }

    @Test
    fun `evaluate resumes an in-progress developer-triggered immediate update`() = runTest {
        val info: AppUpdateInfo = mockk(relaxed = true)
        every { info.updateAvailability() } returns UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
        coEvery { appUpdateManager.requestAppUpdateInfo() } returns info
        coEvery { evaluateUpdateDecision(any(), any()) } returns UpdateDecision.NoUpdate
        val launcher: ActivityResultLauncher<IntentSenderRequest> = mockk()

        manager.evaluate(resumeLauncher = launcher)

        verify(exactly = 1) { appUpdateManager.startUpdateFlowForResult(info, launcher, any()) }
    }

    @Test
    fun `evaluate does not resume the immediate flow when no resumeLauncher is given`() = runTest {
        val info: AppUpdateInfo = mockk(relaxed = true)
        every { info.updateAvailability() } returns UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
        coEvery { appUpdateManager.requestAppUpdateInfo() } returns info
        coEvery { evaluateUpdateDecision(any(), any()) } returns UpdateDecision.NoUpdate

        manager.evaluate()

        verify(exactly = 0) { appUpdateManager.startUpdateFlowForResult(any(), any<ActivityResultLauncher<IntentSenderRequest>>(), any()) }
    }

    @Test
    fun `completeFlexibleUpdate resets readyToInstall and completes the Play Core update`() {
        val state: InstallState = mockk()
        every { state.installStatus() } returns InstallStatus.DOWNLOADED
        listenerSlot.captured.onStateUpdate(state)
        assertThat(manager.readyToInstall.value).isTrue()

        manager.completeFlexibleUpdate()

        assertThat(manager.readyToInstall.value).isFalse()
        verify(exactly = 1) { appUpdateManager.completeUpdate() }
    }

    @Test
    fun `onUpdateFlowResult sets immediateFlowFailed only when decision is MandatoryNow`() = runTest {
        val info: AppUpdateInfo = mockk(relaxed = true)
        coEvery { appUpdateManager.requestAppUpdateInfo() } returns info
        coEvery { evaluateUpdateDecision(any(), any()) } returns UpdateDecision.MandatoryNow
        manager.evaluate()

        manager.onUpdateFlowResult(succeeded = false)
        assertThat(manager.immediateFlowFailed.value).isTrue()

        manager.onUpdateFlowResult(succeeded = true)
        assertThat(manager.immediateFlowFailed.value).isFalse()
    }

    @Test
    fun `onUpdateFlowResult is a no-op when decision is not MandatoryNow`() = runTest {
        val info: AppUpdateInfo = mockk(relaxed = true)
        coEvery { appUpdateManager.requestAppUpdateInfo() } returns info
        coEvery { evaluateUpdateDecision(any(), any()) } returns UpdateDecision.Flexible
        manager.evaluate()

        manager.onUpdateFlowResult(succeeded = false)

        assertThat(manager.immediateFlowFailed.value).isFalse()
    }

    @Test
    fun `download failure with no prior grace period starts the grace countdown`() {
        coEvery { updateRepository.getUpdateConfig() } returns UpdateConfig.DEFAULT
        coEvery { updateRepository.getGraceStartedAt() } returns null

        val state: InstallState = mockk()
        every { state.installStatus() } returns InstallStatus.FAILED
        listenerSlot.captured.onStateUpdate(state)
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(manager.decision.value).isEqualTo(UpdateDecision.MandatoryGrace(UpdateConfig.DEFAULT.gracePeriodDays))
        coVerify(exactly = 1) { updateRepository.setGraceStartedAt(any()) }
    }

    @Test
    fun `download failure after the grace period has expired forces MandatoryNow`() {
        coEvery { updateRepository.getUpdateConfig() } returns UpdateConfig.DEFAULT
        val graceExhaustedAt = System.currentTimeMillis() -
            (UpdateConfig.DEFAULT.gracePeriodDays + 1) * DAY_MILLIS
        coEvery { updateRepository.getGraceStartedAt() } returns graceExhaustedAt

        val state: InstallState = mockk()
        every { state.installStatus() } returns InstallStatus.FAILED
        listenerSlot.captured.onStateUpdate(state)
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(manager.decision.value).isEqualTo(UpdateDecision.MandatoryNow)
    }
}
