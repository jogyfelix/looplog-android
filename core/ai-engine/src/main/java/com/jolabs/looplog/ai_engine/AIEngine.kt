package com.jolabs.looplog.ai_engine

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.jolabs.looplog.model.aiDataModels.AvailableModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface AIEngine {
    val state: StateFlow<EngineState>

    // Checks if the model is supported on the device
    suspend fun isAvailable(): Boolean

    // Checks if the model is already downloaded
    suspend fun isModelDownloaded(): Boolean

    /** Whether Play installed the pack without the model asset targeted to this device. */
    suspend fun hasIncompatibleModelPack(): Boolean

    /** Removes app-managed model files. System-managed models are unaffected. */
    suspend fun removeDownloadedModel()

    suspend fun availableModel() : AvailableModel

    // Initialization (triggers downloads if necessary)
    suspend fun initialize()

    // Triggers a UI dialog to allow downloading over mobile data
    fun triggerWifiConfirmation(launcher: ActivityResultLauncher<IntentSenderRequest>)

    // Multi-turn conversation management
    suspend fun startConversation(systemInstruction: String, isDeterministic: Boolean = false)
    suspend fun sendMessage(prompt: String): Flow<String>
    fun resetConversation()

    fun release()
}
