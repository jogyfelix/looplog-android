package com.jolabs.looplog.ai_engine

import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.GenerateContentRequest
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.TextPart
import com.jolabs.looplog.analytics.AnalyticsTracker
import com.jolabs.looplog.analytics.model.AnalyticsError
import com.jolabs.looplog.model.aiDataModels.AvailableModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class NanoEngine @Inject constructor(
    private val analyticsTracker: AnalyticsTracker
) : AIEngine {

    private val _state = MutableStateFlow<EngineState>(EngineState.Uninitialized)
    override val state: StateFlow<EngineState> = _state.asStateFlow()

    private var generativeModel: GenerativeModel? = null

    private var systemInstruction: String? = null
    private var isDeterministic: Boolean = false
    private val conversationHistory = mutableListOf<String>()

    private fun getGenerativeModel(): GenerativeModel = synchronized(this) {
        generativeModel ?: Generation.getClient().also { generativeModel = it }
    }

    override suspend fun isAvailable(): Boolean {
        // Temporarily disabled until Gemini Nano can be tested on a supported device.
//        return try {
//            val status = generativeModel.checkStatus()
//            status != FeatureStatus.UNAVAILABLE
//        } catch (e: Exception) {
//            false
//        }
        return false
    }

    override suspend fun isModelDownloaded(): Boolean {
        return try {
            getGenerativeModel().checkStatus() == FeatureStatus.AVAILABLE
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun hasIncompatibleModelPack(): Boolean = false

    override suspend fun removeDownloadedModel() = Unit

    override suspend fun availableModel(): AvailableModel {
        return AvailableModel.GEMINI_NANO
    }

    override suspend fun initialize() {
        val startTime = System.currentTimeMillis()
        analyticsTracker.trackEvent(AIAnalytics.InitializationStarted("nano"))
        try {
            val model = getGenerativeModel()
            val status = model.checkStatus()
            when (status) {
                FeatureStatus.AVAILABLE -> {
                    _state.value = EngineState.Ready
                    analyticsTracker.trackEvent(
                        AIAnalytics.InitializationSuccess("nano", System.currentTimeMillis() - startTime)
                    )
                }
                FeatureStatus.DOWNLOADABLE -> {
                    analyticsTracker.trackEvent(AIAnalytics.ModelDownloadStarted("nano"))
                    _state.value = EngineState.DownloadingModel(0)
                    model.download().collect { downloadStatus ->
                        // Assuming downloadStatus has progress info if updated in future SDKs
                        // For now just keep it in downloading state
                        Log.d("NanoEngine", "Download status: $downloadStatus")
                    }
                    _state.value = EngineState.Ready
                    analyticsTracker.trackEvent(AIAnalytics.ModelDownloadSuccess("nano"))
                    analyticsTracker.trackEvent(
                        AIAnalytics.InitializationSuccess("nano", System.currentTimeMillis() - startTime)
                    )
                }
                FeatureStatus.DOWNLOADING -> {
                    _state.value = EngineState.DownloadingModel(0)
                }
                FeatureStatus.UNAVAILABLE -> {
                    _state.value = EngineState.Error("Gemini Nano is not supported on this device", false)
                    analyticsTracker.trackError(
                        AnalyticsError.Soft(
                            source = AIAnalytics.Sources.Nano,
                            message = "Gemini Nano unavailable"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("NanoEngine", "Initialization failed", e)
            _state.value = EngineState.Error(e.message ?: "Unknown error during initialization", true)
            analyticsTracker.trackError(
                AnalyticsError.Hard(
                    throwable = e,
                    source = AIAnalytics.Sources.Nano,
                    message = "Nano initialization failed"
                )
            )
        }
    }

    override fun triggerWifiConfirmation(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        // No-op for Nano as it handles its own internal download via Play Services
    }

    override suspend fun startConversation(systemInstruction: String, isDeterministic: Boolean) {
        this.systemInstruction = systemInstruction
        this.isDeterministic = isDeterministic
        conversationHistory.clear()
    }

    override suspend fun sendMessage(prompt: String): Flow<String> = flow {
        val startTime = System.currentTimeMillis()
        analyticsTracker.trackEvent(AIAnalytics.InferenceStarted("nano"))
        val fullPrompt = buildString {
            systemInstruction?.let {
                append("System: $it\n")
            }
            conversationHistory.forEach {
                append(it)
                append("\n")
            }
            append("User: $prompt\n")
            append("Assistant: ")
        }

        val request = GenerateContentRequest.Builder(TextPart(fullPrompt)).apply {
            temperature = if (isDeterministic) 0.0f else 0.7f
            maxOutputTokens = 512
        }.build()

        try {
            val response = getGenerativeModel().generateContent(request)
            val responseText = response.candidates.firstOrNull()?.text ?: ""

            // Update history
            conversationHistory.add("User: $prompt")
            conversationHistory.add("Assistant: $responseText")

            emit(responseText)
            analyticsTracker.trackEvent(
                AIAnalytics.InferenceSuccess("nano", System.currentTimeMillis() - startTime)
            )
        } catch (e: Exception) {
            Log.e("NanoEngine", "AI inference failed")
            analyticsTracker.trackError(
                AnalyticsError.Hard(
                    throwable = IllegalStateException("Nano inference failed"),
                    source = AIAnalytics.Sources.Nano,
                    message = "Nano inference failed"
                )
            )
            throw e
        }
    }

    override fun resetConversation() {
        systemInstruction = null
        conversationHistory.clear()
    }

    override fun release() {
        _state.value = EngineState.Uninitialized
        resetConversation()
        synchronized(this) {
            generativeModel?.close()
            generativeModel = null
        }
    }
}
