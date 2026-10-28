package com.jolabs.looplog.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.jolabs.looplog.model.update.UpdateConfig
import com.jolabs.looplog.model.update.UpdateForceMode
import com.jolabs.looplog.storage.datastore.UpdateGraceStore
import com.jolabs.looplog.analytics.AnalyticsTracker
import com.jolabs.looplog.analytics.model.AnalyticsError
import com.jolabs.looplog.analytics.model.ErrorSource
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.getValue
import kotlinx.coroutines.suspendCancellableCoroutine

private const val KEY_MANDATORY_PRIORITY_THRESHOLD = "update_mandatory_priority_threshold"
private const val KEY_STALE_DAYS_THRESHOLD = "update_stale_days_threshold"
private const val KEY_GRACE_PERIOD_DAYS = "update_grace_period_days"
private const val KEY_FORCE_MODE = "update_force_mode"

// A force-update kill switch needs to propagate quickly; the SDK's own default (12h) is far
// too slow for that, so use a much shorter throttle window instead of leaving it unset.
private const val MIN_FETCH_INTERVAL_SECONDS = 3600L

class UpdateRepositoryImpl @Inject constructor(
    private val context: Context,
    private val updateGraceStore: UpdateGraceStore,
    private val analyticsTracker: AnalyticsTracker
) : UpdateRepository {

    private val remoteConfig: FirebaseRemoteConfig by lazy {
        FirebaseRemoteConfig.getInstance().apply {
            setDefaultsAsync(
                mapOf(
                    KEY_MANDATORY_PRIORITY_THRESHOLD to UpdateConfig.DEFAULT.mandatoryPriorityThreshold,
                    KEY_STALE_DAYS_THRESHOLD to UpdateConfig.DEFAULT.staleDaysThreshold,
                    KEY_GRACE_PERIOD_DAYS to UpdateConfig.DEFAULT.gracePeriodDays,
                    KEY_FORCE_MODE to UpdateConfig.DEFAULT.forceMode.name
                )
            )
            setConfigSettingsAsync(
                FirebaseRemoteConfigSettings.Builder()
                    .setMinimumFetchIntervalInSeconds(MIN_FETCH_INTERVAL_SECONDS)
                    .build()
            )
        }
    }

    override suspend fun getUpdateConfig(): UpdateConfig {
        return try {
            val config = remoteConfig
            // fetchAndActivate() is a Task, not a suspend fun - without awaiting it here, the
            // getString/getLong calls below race the fetch and can read stale/default values
            // even when a newer config is already sitting on the server.
            suspendCancellableCoroutine<Unit> { continuation ->
                config.fetchAndActivate().addOnCompleteListener { continuation.resume(Unit) }
            }
            // Remote Config is a free-text field with no enum validation in the console, so a
            // stray case mismatch (e.g. "force_mandatory") would otherwise silently fall back
            // to AUTO instead of the intended forced mode.
            val forceMode = runCatching {
                UpdateForceMode.valueOf(config.getString(KEY_FORCE_MODE).trim().uppercase())
            }.getOrDefault(UpdateConfig.DEFAULT.forceMode)

            UpdateConfig(
                mandatoryPriorityThreshold = config.getLong(KEY_MANDATORY_PRIORITY_THRESHOLD).toInt(),
                staleDaysThreshold = config.getLong(KEY_STALE_DAYS_THRESHOLD).toInt(),
                forceMode = forceMode,
                gracePeriodDays = config.getLong(KEY_GRACE_PERIOD_DAYS).toInt()
            )
        } catch (e: Exception) {
            analyticsTracker.trackError(
                AnalyticsError.Soft(
                    throwable = e,
                    source = ErrorSource.RemoteConfig,
                    message = "Failed to fetch remote config"
                )
            )
            UpdateConfig.DEFAULT
        }
    }

    override suspend fun getGraceStartedAt(): Long? = updateGraceStore.getGraceStartedAt()

    override suspend fun setGraceStartedAt(timestamp: Long) =
        updateGraceStore.setGraceStartedAt(timestamp)

    override suspend fun clearGraceStarted() = updateGraceStore.clearGraceStarted()

    override fun isOnline(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
