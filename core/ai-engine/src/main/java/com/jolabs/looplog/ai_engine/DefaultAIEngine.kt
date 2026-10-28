package com.jolabs.looplog.ai_engine

import android.content.Context
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.jolabs.looplog.analytics.AnalyticsTracker
import com.jolabs.looplog.analytics.model.AnalyticsError
import com.jolabs.looplog.model.aiDataModels.AvailableModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultAIEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val nanoEngine: NanoEngine,
    private val analyticsTracker: AnalyticsTracker
) : AIEngine {

    private val liteRTEngineDelegate = lazy { LiteRTEngine(context, analyticsTracker) }
    private val liteRTEngine by liteRTEngineDelegate
    private var activeEngine: AIEngine? = null

    private val _state = MutableStateFlow<EngineState>(EngineState.Uninitialized)
    override val state: StateFlow<EngineState> = _state.asStateFlow()

    override suspend fun isAvailable(): Boolean {
        return nanoEngine.isAvailable() || liteRTEngine.isAvailable()
    }

    override suspend fun isModelDownloaded(): Boolean {
        if (nanoEngine.isAvailable() && nanoEngine.isModelDownloaded()) {
            return true
        }
        return liteRTEngine.isModelDownloaded()
    }

    override suspend fun hasIncompatibleModelPack(): Boolean {
        return !nanoEngine.isAvailable() && liteRTEngine.hasIncompatibleModelPack()
    }

    override suspend fun removeDownloadedModel() {
        if (activeEngine == liteRTEngine) {
            activeEngine = null
        }
        liteRTEngine.removeDownloadedModel()
    }

    override suspend fun availableModel(): AvailableModel {
        return if(nanoEngine.isAvailable()) AvailableModel.GEMINI_NANO else AvailableModel.LITE_RT
    }

    override suspend fun initialize() {
        if (_state.value is EngineState.Ready || _state.value is EngineState.LoadingIntoMemory) {
            return
        }
        _state.value = EngineState.LoadingIntoMemory

        // Try Nano first
        if (nanoEngine.isAvailable()) {
            try {
                activeEngine = nanoEngine
                coroutineScope {
                    val job = launch {
                        nanoEngine.state.collect { state ->
                            _state.value = state
                        }
                    }
                    try {
                        nanoEngine.initialize()
                    } finally {
                        job.cancel()
                    }
                }
                // Ensure the final state is captured
                _state.value = nanoEngine.state.value

                if (_state.value is EngineState.Ready) {
                    Log.d("DefaultAIEngine", "Gemini Nano is ready.")
                    return
                }
            } catch (error: CancellationException) {
                throw error
            } catch (e: Exception) {
                Log.w("DefaultAIEngine", "Nano initialization failed, falling back to LiteRT", e)
                analyticsTracker.trackError(
                    AnalyticsError.Soft(
                        throwable = e,
                        source = AIAnalytics.Sources.Orchestrator,
                        message = "Nano failed, falling back to LiteRT"
                    )
                )
            }
        }

        if (!liteRTEngine.isAvailable()) {
            activeEngine = null
            _state.value = EngineState.Error("AI is not supported on this device", false)
            return
        }

        // Fallback to LiteRT
        try {
            Log.d("DefaultAIEngine", "Falling back to LiteRT Engine.")
            activeEngine = liteRTEngine
            coroutineScope {
                val job = launch {
                    liteRTEngine.state.collect { state ->
                        _state.value = state
                    }
                }
                try {
                    liteRTEngine.initialize()
                } finally {
                    job.cancel()
                }
            }
            // Ensure the final state is captured even if initialize() returned immediately
            _state.value = liteRTEngine.state.value
        } catch (error: CancellationException) {
            throw error
        } catch (e: Exception) {
            Log.e("DefaultAIEngine", "LiteRT initialization also failed", e)
            _state.value = EngineState.Error(e.message ?: "Failed to initialize any AI engine", true)
            analyticsTracker.trackError(
                AnalyticsError.Hard(
                    throwable = e,
                    source = AIAnalytics.Sources.Orchestrator,
                    message = "All AI engines failed"
                )
            )
        }
    }

    override fun triggerWifiConfirmation(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        activeEngine?.triggerWifiConfirmation(launcher)
    }

    override suspend fun startConversation(systemInstruction: String, isDeterministic: Boolean) {
        activeEngine?.startConversation(systemInstruction, isDeterministic)
            ?: throw IllegalStateException("No active engine. Call initialize() first.")
    }

    override suspend fun sendMessage(prompt: String): Flow<String> {
        return activeEngine?.sendMessage(prompt)
            ?: throw IllegalStateException("No active engine. Call initialize() first.")
    }

    override fun resetConversation() {
        activeEngine?.resetConversation()
    }

    override fun release() {
        _state.value = EngineState.Uninitialized
        nanoEngine.release()
        if (liteRTEngineDelegate.isInitialized()) {
            liteRTEngineDelegate.value.release()
        }
        activeEngine = null
    }
}
