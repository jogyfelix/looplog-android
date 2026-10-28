package com.jolabs.looplog.data.repository

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.jolabs.looplog.analytics.AnalyticsTracker
import com.jolabs.looplog.analytics.model.AnalyticsError
import com.jolabs.looplog.analytics.model.ErrorSource
import com.jolabs.looplog.storage.datastore.AppReviewStore
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

private const val KEY_REVIEW_ENGAGEMENT_DAY_THRESHOLD = "review_engagement_day_threshold"
private const val DEFAULT_ENGAGEMENT_DAY_THRESHOLD = 7

class AppReviewRepositoryImpl @Inject constructor(
    private val appReviewStore: AppReviewStore,
    private val analyticsTracker: AnalyticsTracker
) : AppReviewRepository {

    // Shares the process-wide Remote Config singleton with UpdateRepositoryImpl. Deliberately
    // does NOT call setDefaultsAsync/setConfigSettingsAsync here: those replace the whole
    // defaults map on the shared instance and would clobber the update repo's config. Instead
    // the threshold falls back to DEFAULT_ENGAGEMENT_DAY_THRESHOLD in code when Remote Config
    // has no value (getLong returns 0 for an unset key).
    private val remoteConfig: FirebaseRemoteConfig by lazy { FirebaseRemoteConfig.getInstance() }

    override suspend fun registerOpenDay(todayEpochDay: Long): Int =
        appReviewStore.registerOpenDay(todayEpochDay)

    override suspend fun isPromptShown(): Boolean = appReviewStore.isPromptShown()

    override suspend fun setPromptShown() = appReviewStore.setPromptShown()

    override suspend fun getEngagementDayThreshold(): Int {
        return try {
            val config = remoteConfig
            // fetchAndActivate() is a Task, not a suspend fun; await it so the getLong below
            // reads the freshly activated value rather than racing the fetch.
            suspendCancellableCoroutine { continuation ->
                config.fetchAndActivate().addOnCompleteListener { continuation.resume(Unit) }
            }
            val value = config.getLong(KEY_REVIEW_ENGAGEMENT_DAY_THRESHOLD)
            if (value > 0) value.toInt() else DEFAULT_ENGAGEMENT_DAY_THRESHOLD
        } catch (e: Exception) {
            analyticsTracker.trackError(
                AnalyticsError.Soft(
                    throwable = e,
                    source = ErrorSource.RemoteConfig,
                    message = "Failed to fetch review engagement threshold"
                )
            )
            DEFAULT_ENGAGEMENT_DAY_THRESHOLD
        }
    }
}
