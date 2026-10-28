package com.jolabs.looplog.update

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.requestAppUpdateInfo
import com.jolabs.looplog.data.repository.UpdateRepository
import com.jolabs.looplog.di.ApplicationScope
import com.jolabs.looplog.domain.update.EvaluateUpdateDecisionUseCase
import com.jolabs.looplog.model.update.PlayUpdateSignals
import com.jolabs.looplog.model.update.UpdateDecision
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val DAY_MILLIS = 24 * 60 * 60 * 1000L
private const val TAG = "InAppUpdateManager"

/**
 * App-scoped wrapper around Play Core's AppUpdateManager. Delegates the flexible-vs-mandatory
 * decision to [EvaluateUpdateDecisionUseCase]; this class only drives the Play Core SDK and
 * exposes the resulting state for the UI to render.
 */
@Singleton
class InAppUpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appUpdateManager: AppUpdateManager,
    private val evaluateUpdateDecision: EvaluateUpdateDecisionUseCase,
    private val updateRepository: UpdateRepository,
    @ApplicationScope private val scope: CoroutineScope
) {
    private var latestAppUpdateInfo: AppUpdateInfo? = null

    // True from a startUpdateFlowForResult launch until its activity result arrives. Guards
    // against stacking a second Play dialog: rotating while Play's update screen is on top
    // recreates that screen and transiently resumes this app's activity, whose onResume
    // re-enters evaluate() while the first dialog is still showing.
    private var updateFlowInFlight = false

    private val _decision = MutableStateFlow<UpdateDecision>(UpdateDecision.NoUpdate)
    val decision: StateFlow<UpdateDecision> = _decision.asStateFlow()

    private val _readyToInstall = MutableStateFlow(false)
    val readyToInstall: StateFlow<Boolean> = _readyToInstall.asStateFlow()

    private val _immediateFlowFailed = MutableStateFlow(false)
    val immediateFlowFailed: StateFlow<Boolean> = _immediateFlowFailed.asStateFlow()

    init {
        appUpdateManager.registerListener { state ->
            when (state.installStatus()) {
                InstallStatus.DOWNLOADED -> _readyToInstall.value = true
                // A failed download can happen even on a "validated" network (throttling,
                // captive portal after validation, Play CDN issues) - treat it the same as
                // offline so a mandatory update doesn't just silently keep failing.
                InstallStatus.FAILED -> onDownloadFailed()
                else -> Unit
            }
        }
    }

    /**
     * Fetches the latest update info from Play, evaluates flexible-vs-mandatory, and updates
     * [decision]/[readyToInstall]. If [resumeLauncher] is provided and Play reports a
     * developer-triggered immediate update already in progress, resumes that flow.
     */
    suspend fun evaluate(resumeLauncher: ActivityResultLauncher<IntentSenderRequest>? = null): UpdateDecision {
        // Play Core 2.1.0 has a known bug (issuetracker.google.com/179402833) where a failed
        // Play Store service binding throws fatally on the library's OWN handler thread - no
        // app-side try/catch can reach it. The only reliable defense is to never invoke Play
        // Core when the Play Store isn't installed and enabled on the device.
        if (!isPlayStoreUsable()) {
            Log.w(TAG, "Play Store missing or disabled - skipping in-app update check")
            _decision.value = UpdateDecision.NoUpdate
            return UpdateDecision.NoUpdate
        }

        // Play Core throws (rather than failing gracefully) when it can't bind to the Play
        // Store service - e.g. debug/sideloaded installs, emulators without Play Store, or
        // devices without Play Store at all. Treat that the same as "no update available"
        // instead of crashing the app on every launch.
        val info = try {
            appUpdateManager.requestAppUpdateInfo()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            _decision.value = UpdateDecision.NoUpdate
            return UpdateDecision.NoUpdate
        }
        latestAppUpdateInfo = info

        if (info.installStatus() == InstallStatus.DOWNLOADED) {
            _readyToInstall.value = true
        }

        if (info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
            // An accepted flow is already running. Only resume it as an IMMEDIATE screen when
            // no launch is awaiting its result (otherwise the dialog stacks a duplicate on
            // top, e.g. on the transient onResume during rotation) and no download path is
            // active - an accepted flexible download reports this same availability, but is
            // already handled by the install listener / readyToInstall prompt.
            val status = info.installStatus()
            val downloadPathActive = status == InstallStatus.PENDING ||
                status == InstallStatus.DOWNLOADING ||
                status == InstallStatus.DOWNLOADED ||
                status == InstallStatus.INSTALLING
            if (resumeLauncher != null && !updateFlowInFlight && !downloadPathActive) {
                updateFlowInFlight = appUpdateManager.startUpdateFlowForResult(
                    info,
                    resumeLauncher,
                    AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                )
            }
            // Keep the current decision: an in-progress flow reports updateAvailable=false,
            // which would wrongly reset MandatoryNow to NoUpdate mid-flow and stop
            // onUpdateFlowResult from raising the blocking screen if the user backs out.
            return _decision.value
        }

        val signals = PlayUpdateSignals(
            updateAvailable = info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE,
            priority = info.updatePriority(),
            stalenessDays = info.clientVersionStalenessDays() ?: -1,
            isImmediateAllowed = info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE),
            isFlexibleAllowed = info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
        )
        val result = evaluateUpdateDecision(signals)
        Log.d(TAG, "signals=$signals -> decision=$result")
        _decision.value = result
        return result
    }

    fun startImmediateUpdate(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        val info = latestAppUpdateInfo ?: return
        if (updateFlowInFlight) return
        val started = appUpdateManager.startUpdateFlowForResult(
            info,
            launcher,
            AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
        )
        updateFlowInFlight = started
        // Only clear the failure once a flow actually launched, so the blocking screen's
        // retry button stays available when the launch itself couldn't start.
        if (started) _immediateFlowFailed.value = false
    }

    fun startFlexibleUpdate(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        val info = latestAppUpdateInfo ?: return
        if (updateFlowInFlight) return
        updateFlowInFlight = appUpdateManager.startUpdateFlowForResult(
            info,
            launcher,
            AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
        )
    }

    fun completeFlexibleUpdate() {
        _readyToInstall.value = false
        appUpdateManager.completeUpdate()
    }

    /**
     * Called by the Activity's launcher callback whenever a started update flow returns.
     * Only matters for MandatoryNow: a non-OK result there means Play's own IMMEDIATE screen
     * was backed out of or failed to launch, so the UI needs our blocking fallback screen.
     */
    fun onUpdateFlowResult(succeeded: Boolean) {
        updateFlowInFlight = false
        if (_decision.value == UpdateDecision.MandatoryNow) {
            _immediateFlowFailed.value = !succeeded
        }
    }

    private fun isPlayStoreUsable(): Boolean = try {
        context.packageManager.getApplicationInfo("com.android.vending", 0).enabled
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    private fun onDownloadFailed() {
        scope.launch {
            val config = updateRepository.getUpdateConfig()
            val startedAt = updateRepository.getGraceStartedAt()
                ?: System.currentTimeMillis().also { updateRepository.setGraceStartedAt(it) }
            val daysElapsed = (System.currentTimeMillis() - startedAt) / DAY_MILLIS
            val daysRemaining = (config.gracePeriodDays - daysElapsed).coerceAtLeast(0)
            _decision.value = if (daysRemaining > 0) {
                UpdateDecision.MandatoryGrace(daysRemaining.toInt())
            } else {
                UpdateDecision.MandatoryNow
            }
        }
    }
}
