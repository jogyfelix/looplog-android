package com.jolabs.looplog.data.repository.ai

import android.content.Context
import com.jolabs.looplog.ai_engine.AIEngine
import com.jolabs.looplog.ai_engine.EngineState
import com.jolabs.looplog.storage.datastore.AiPreferences
import com.jolabs.looplog.model.aiDataModels.AvailableModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiFeatureRepositoryImpl @Inject constructor(
    private val aiPreferences: AiPreferences,
    private val aiEngine: AIEngine,
    private val featureFlagProvider: AiFeatureFlagProvider,
    @ApplicationContext private val context: Context,
) : AiFeatureRepository {

    private val availabilityMutex = Mutex()
    private val _availability = MutableStateFlow(AiFeatureAvailability.Checking)
    override val availability: StateFlow<AiFeatureAvailability> = _availability.asStateFlow()

    override val isAiNoticeAcknowledged: Flow<Boolean> = aiPreferences.isAiNoticeAcknowledged

    override suspend fun acknowledgeAiNotice() {
        aiPreferences.acknowledgeAiNotice()
    }

    override suspend fun refreshAvailability(): AiFeatureAvailability = availabilityMutex.withLock {
        val remoteEnabled = featureFlagProvider.refreshAndIsEnabled()
        val hardwareSupported = remoteEnabled && aiEngine.isAvailable()
        val appVersionCode = context.packageManager
            .getPackageInfo(context.packageName, 0)
            .longVersionCode
        val usesLiteRt = hardwareSupported && aiEngine.availableModel() == AvailableModel.LITE_RT
        val previouslyIncompatible = usesLiteRt &&
            aiPreferences.isModelPackIncompatible(appVersionCode)
        val incompatiblePack = usesLiteRt && aiEngine.hasIncompatibleModelPack()

        if (incompatiblePack) {
            aiPreferences.markModelPackIncompatible(appVersionCode)
        }

        val result = if (hardwareSupported && !previouslyIncompatible && !incompatiblePack) {
            AiFeatureAvailability.Available
        } else {
            AiFeatureAvailability.Unavailable
        }
        _availability.value = result

        if (result == AiFeatureAvailability.Unavailable) {
            aiEngine.release()
            aiEngine.removeDownloadedModel()
        }
        result
    }

    override suspend fun isAiFeatureAvailable(): Boolean {
        val current = availability.value
        return if (current == AiFeatureAvailability.Checking) {
            refreshAvailability() == AiFeatureAvailability.Available
        } else {
            current == AiFeatureAvailability.Available
        }
    }

    override suspend fun availableModel(): AvailableModel {
        return aiEngine.availableModel()
    }

    override suspend fun isModelDownloaded(): Boolean {
        return aiEngine.isModelDownloaded()
    }

    override suspend fun initializeModel() {
        if (!isAiFeatureAvailable()) return
        aiEngine.initialize()
        if (aiEngine.state.value is EngineState.Error && aiEngine.hasIncompatibleModelPack()) {
            val appVersionCode = context.packageManager
                .getPackageInfo(context.packageName, 0)
                .longVersionCode
            aiPreferences.markModelPackIncompatible(appVersionCode)
            _availability.value = AiFeatureAvailability.Unavailable
            aiEngine.removeDownloadedModel()
        }
    }

    override fun release() {
        aiEngine.release()
    }
}
