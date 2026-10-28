package com.jolabs.looplog.data.repository.ai

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.jolabs.looplog.analytics.AnalyticsTracker
import com.jolabs.looplog.analytics.model.AnalyticsError
import com.jolabs.looplog.analytics.model.ErrorSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

private const val AI_FEATURE_ENABLED_KEY = "ai_feature_enabled"
private const val MIN_FETCH_INTERVAL_SECONDS = 3600L

interface AiFeatureFlagProvider {
    suspend fun refreshAndIsEnabled(): Boolean
}

@Singleton
class FirebaseAiFeatureFlagProvider @Inject constructor(
    private val analyticsTracker: AnalyticsTracker,
) : AiFeatureFlagProvider {

    private val remoteConfig: FirebaseRemoteConfig by lazy { FirebaseRemoteConfig.getInstance() }

    override suspend fun refreshAndIsEnabled(): Boolean {
        try {
            suspendCancellableCoroutine { continuation ->
                remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
                    if (!continuation.isActive) return@addOnCompleteListener
                    if (task.isSuccessful) {
                        continuation.resume(Unit)
                    } else {
                        continuation.resume(Unit)
                        task.exception?.let(::reportFetchFailure)
                    }
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            reportFetchFailure(error)
        }

        val value = remoteConfig.getValue(AI_FEATURE_ENABLED_KEY)
        return if (value.source == FirebaseRemoteConfig.VALUE_SOURCE_STATIC) {
            true
        } else {
            value.asBoolean()
        }
    }

    private fun reportFetchFailure(error: Exception) {
        analyticsTracker.trackError(
            AnalyticsError.Soft(
                throwable = error,
                source = ErrorSource.RemoteConfig,
                message = "Failed to fetch AI feature flag",
            )
        )
    }
}
