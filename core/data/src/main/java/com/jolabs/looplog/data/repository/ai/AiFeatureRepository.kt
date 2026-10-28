package com.jolabs.looplog.data.repository.ai

import com.jolabs.looplog.model.aiDataModels.AvailableModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

enum class AiFeatureAvailability {
    Checking,
    Available,
    Unavailable,
}

interface AiFeatureRepository {
    val availability: StateFlow<AiFeatureAvailability>

    /** Flow indicating whether the user acknowledged the current AI notice. */
    val isAiNoticeAcknowledged: Flow<Boolean>

    /** Records acknowledgment of the current AI notice. */
    suspend fun acknowledgeAiNotice()

    /**
     * Checks if any AI engine is supported on this device.
     */
    suspend fun refreshAvailability(): AiFeatureAvailability

    suspend fun isAiFeatureAvailable(): Boolean

    /**
     * Returns the model type that will be used.
     */
    suspend fun availableModel(): AvailableModel

    /**
     * Checks if the model file is already present on the device.
     */
    suspend fun isModelDownloaded(): Boolean

    /**
     * Triggers the model download/initialization.
     */
    suspend fun initializeModel()

    /**
     * Releases AI resources.
     */
    fun release()
}
