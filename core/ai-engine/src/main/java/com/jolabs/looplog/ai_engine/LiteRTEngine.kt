package com.jolabs.looplog.ai_engine

import android.content.Context
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.android.play.core.aipacks.AiPackManagerFactory
import com.google.android.play.core.aipacks.AiPackState
import com.google.android.play.core.aipacks.AiPackStateUpdateListener
import com.google.android.play.core.aipacks.model.AiPackStatus
import com.jolabs.looplog.ai_engine.helpers.getTotalSystemRam
import com.jolabs.looplog.analytics.AnalyticsTracker
import com.jolabs.looplog.analytics.model.AnalyticsError
import com.jolabs.looplog.model.aiDataModels.AvailableModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

class LiteRTEngine(
    private val context: Context,
    private val analyticsTracker: AnalyticsTracker
) : AIEngine {

    private val _state = MutableStateFlow<EngineState>(EngineState.Uninitialized)
    override val state: StateFlow<EngineState> = _state.asStateFlow()

    private var lmEngine: Engine? = null
    private var activeConversation: Conversation? = null
    private var isGpuSupported = true
    private var usingGpuBackend = false
    private var modelPath: String? = null
    private var lastSystemInstruction: String? = null
    private var isDeterministic: Boolean = false
    @Volatile private var modelUseAllowed = true

    private val aiPackName = "model_pack"
    private val modelFileName = "on-demand/model.litertlm"
    private val aiPackManager = AiPackManagerFactory.getInstance(context)

    override suspend fun isAvailable(): Boolean = withContext(Dispatchers.Default) {
        val totalRamGb = getTotalSystemRam(context)
        totalRamGb >= 4.0
    }

    override suspend fun isModelDownloaded(): Boolean = withContext(Dispatchers.IO) {
        installedModelPath() != null
    }

    override suspend fun hasIncompatibleModelPack(): Boolean = withContext(Dispatchers.IO) {
        aiPackManager.getPackLocation(aiPackName) != null && installedModelPath() == null
    }

    override suspend fun removeDownloadedModel() = withContext(Dispatchers.IO) {
        modelUseAllowed = false
        release()
        try {
            aiPackManager.cancel(listOf(aiPackName))
            removeInstalledPack()
        } catch (error: Exception) {
            Log.e("LiteRTEngine", "Failed to clean up AI pack", error)
        }
        Unit
    }

    override suspend fun availableModel(): AvailableModel {
        return AvailableModel.LITE_RT
    }

    override suspend fun initialize() {
        analyticsTracker.trackEvent(AIAnalytics.InitializationStarted("lite_rt"))
        val currentState = _state.value
        if (currentState is EngineState.Ready ||
            currentState is EngineState.LoadingIntoMemory ||
            currentState is EngineState.DownloadingModel ||
            currentState is EngineState.WaitingForWifi
        ) {
            return
        }
        modelUseAllowed = true

        val installedModelPath = installedModelPath()
        if (installedModelPath == null) {
            downloadModel()
        } else {
            loadModel(installedModelPath)
        }
    }

    private fun installedModelPath(): String? {
        val assetsPath = aiPackManager.getPackLocation(aiPackName)?.assetsPath() ?: return null
        return File(assetsPath, modelFileName)
            .takeIf { it.isFile && it.length() > 0L }
            ?.absolutePath
    }

    override fun triggerWifiConfirmation(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        try {
            aiPackManager.showConfirmationDialog(launcher)
        } catch (e: Exception) {
            Log.e("LiteRTEngine", "Failed to show confirmation dialog", e)
        }
    }

    private suspend fun downloadModel() = withContext(Dispatchers.Default) {
        analyticsTracker.trackEvent(AIAnalytics.ModelDownloadStarted("lite_rt"))
        _state.value = EngineState.DownloadingModel(0)

        suspendCancellableCoroutine { continuation ->
            val listener = object : AiPackStateUpdateListener {
                override fun onStateUpdate(state: AiPackState) {
                    if (!modelUseAllowed) return
                    when (val status = state.status()) {
                        AiPackStatus.PENDING -> {
                            Log.d("LiteRTEngine", "AI Pack download pending")
                            _state.value = EngineState.DownloadingModel(0)
                        }
                        AiPackStatus.DOWNLOADING -> {
                            val progress = if (state.totalBytesToDownload() > 0) {
                                (100.0 * state.bytesDownloaded() / state.totalBytesToDownload()).toInt()
                            } else 0
                            _state.value = EngineState.DownloadingModel(progress)
                        }
                        AiPackStatus.TRANSFERRING -> {
                            Log.d("LiteRTEngine", "AI Pack transferring to storage")
                            _state.value = EngineState.DownloadingModel(100)
                        }
                        AiPackStatus.WAITING_FOR_WIFI -> {
                            Log.d("LiteRTEngine", "AI Pack waiting for Wi-Fi")
                            _state.value = EngineState.WaitingForWifi
                        }
                        AiPackStatus.COMPLETED -> {
                            Log.d("LiteRTEngine", "AI Pack download completed")
                            analyticsTracker.trackEvent(AIAnalytics.ModelDownloadSuccess("lite_rt"))
                            aiPackManager.unregisterListener(this)
                            if (continuation.isActive) continuation.resume(Unit)
                        }
                        AiPackStatus.FAILED, AiPackStatus.CANCELED -> {
                            Log.e("LiteRTEngine", "AI Pack download failed or canceled (status: $status)")
                            analyticsTracker.trackError(
                                AnalyticsError.Hard(
                                    throwable = Exception("Download failed with status $status"),
                                    source = AIAnalytics.Sources.LiteRT,
                                    message = "LiteRT model download failed: $status"
                                )
                            )
                            aiPackManager.unregisterListener(this)
                            _state.value = EngineState.Error("Download failed or canceled (status: $status)", true)
                            if (continuation.isActive) continuation.resume(Unit)
                        }
                        else -> {
                            Log.d("LiteRTEngine", "AI Pack status update: $status")
                        }
                    }
                }
            }
            continuation.invokeOnCancellation {
                aiPackManager.unregisterListener(listener)
            }
            aiPackManager.registerListener(listener)
            aiPackManager.fetch(listOf(aiPackName)).addOnFailureListener { e ->
                Log.e("LiteRTEngine", "Failed to start AI Pack fetch", e)
                analyticsTracker.trackError(
                    AnalyticsError.Hard(
                        throwable = e,
                        source = AIAnalytics.Sources.LiteRT,
                        message = "LiteRT model fetch trigger failed"
                    )
                )
                aiPackManager.unregisterListener(listener)
                if (continuation.isActive) {
                    _state.value = EngineState.Error("Fetch failed: ${e.message}", true)
                    continuation.resume(Unit)
                }
            }
        }

        val installedModelPath = installedModelPath()
        if (!modelUseAllowed) {
            removeInstalledPack()
        } else if (installedModelPath != null) {
            loadModel(installedModelPath)
        } else {
            val error = "Downloaded AI pack does not contain $modelFileName"
            Log.e("LiteRTEngine", error)
            _state.value = EngineState.Error(error, isRecoverable = true)
        }
    }

    private suspend fun removeInstalledPack() {
        if (aiPackManager.getPackLocation(aiPackName) == null) return
        suspendCancellableCoroutine { continuation ->
            aiPackManager.removePack(aiPackName)
                .addOnSuccessListener {
                    Log.d("LiteRTEngine", "AI pack removed")
                    if (continuation.isActive) continuation.resume(Unit)
                }
                .addOnFailureListener { error ->
                    Log.e("LiteRTEngine", "Failed to remove AI pack", error)
                    if (continuation.isActive) continuation.resume(Unit)
                }
        }
    }

    private suspend fun loadModel(fullModelPath: String) = withContext(Dispatchers.Default) {
        if (!modelUseAllowed) return@withContext
        if (lmEngine != null) {
            _state.value = EngineState.Ready
            return@withContext
        }
        val startTime = System.currentTimeMillis()
        _state.value = EngineState.LoadingIntoMemory

        try {
            val cacheDirPath = context.cacheDir.path
            val totalRamGb = getTotalSystemRam(context)
            modelPath = fullModelPath

            if (isGpuSupported && totalRamGb >= 6.0) {
                try {
                    val gpuEngine = createEngine(fullModelPath, Backend.GPU(), cacheDirPath)
                    try {
                        if (!modelUseAllowed) {
                            gpuEngine.close()
                            return@withContext
                        }
                        probeGpuInference(gpuEngine)
                        if (!modelUseAllowed) {
                            gpuEngine.close()
                            return@withContext
                        }
                        lmEngine = gpuEngine
                    } catch (error: Exception) {
                        gpuEngine.close()
                        throw error
                    }
                    usingGpuBackend = true
                    Log.d("LiteRTEngine", "GPU engine verified with inference probe")
                    _state.value = EngineState.Ready
                    analyticsTracker.trackEvent(
                        AIAnalytics.InitializationSuccess("lite_rt", System.currentTimeMillis() - startTime)
                    )
                    return@withContext
                } catch (error: CancellationException) {
                    throw error
                } catch (e: Exception) {
                    Log.e("LiteRTEngine", "GPU unavailable, falling back to CPU", e)
                    analyticsTracker.trackEvent(AIAnalytics.GpuFallback(e.message ?: "Probe failed"))
                    isGpuSupported = false
                    usingGpuBackend = false
                    releaseLmEngine()
                }
            }

            val cpuEngine = createEngine(fullModelPath, Backend.CPU(), cacheDirPath)
            if (!modelUseAllowed) {
                cpuEngine.close()
                return@withContext
            }
            lmEngine = cpuEngine
            usingGpuBackend = false
            Log.d("LiteRTEngine", "CPU engine initialized successfully")
            _state.value = EngineState.Ready
            analyticsTracker.trackEvent(
                AIAnalytics.InitializationSuccess("lite_rt", System.currentTimeMillis() - startTime)
            )
        } catch (error: CancellationException) {
            _state.value = EngineState.Uninitialized
            throw error
        } catch (e: Exception) {
            Log.e("LiteRTEngine", "Failed to initialize engine", e)
            _state.value = EngineState.Error(e.message ?: "Failed to initialize LiteRT", isRecoverable = true)
            analyticsTracker.trackError(
                AnalyticsError.Hard(
                    throwable = e,
                    source = AIAnalytics.Sources.LiteRT,
                    message = "LiteRT initialization failed"
                )
            )
        }
    }

    private fun createEngine(modelPath: String, backend: Backend, cacheDir: String): Engine {
        val config = EngineConfig(
            modelPath = modelPath,
            backend = backend,
            cacheDir = cacheDir,
        )
        return Engine(config).apply { initialize() }
    }

    private suspend fun probeGpuInference(engine: Engine) {
        val probeConfig = ConversationConfig(
            systemInstruction = Contents.of("Reply with OK"),
            samplerConfig = SamplerConfig(topK = 1, topP = 1.0, temperature = 0.0),
        )
        val probeConversation = engine.createConversation(probeConfig)
        try {
            collectFlow(probeConversation.sendMessageAsync("ping").map { it.contents.toString() })
        } finally {
            probeConversation.cancelProcess()
            probeConversation.close()
        }
    }

    private suspend fun fallbackToCpu() = withContext(Dispatchers.Default) {
        check(modelUseAllowed) { "AI engine is not active" }
        val path = modelPath ?: throw IllegalStateException("Model path not set")
        isGpuSupported = false
        usingGpuBackend = false
        releaseLmEngine()
        val cpuEngine = createEngine(path, Backend.CPU(), context.cacheDir.path)
        if (!modelUseAllowed) {
            cpuEngine.close()
            return@withContext
        }
        lmEngine = cpuEngine
        Log.d("LiteRTEngine", "CPU fallback engine initialized after GPU inference failure")
    }

    private fun releaseLmEngine() {
        closeActiveConversation()
        lmEngine?.close()
        lmEngine = null
    }

    private fun closeActiveConversation() {
        val conversation = activeConversation ?: return
        activeConversation = null
        runCatching { conversation.cancelProcess() }
        runCatching { conversation.close() }
            .onFailure { Log.w("LiteRTEngine", "Failed to close conversation", it) }
    }

    private suspend fun collectFlow(flow: Flow<String>) {
        flow.collect { /* drain stream to surface async GPU errors */ }
    }

    private fun isGpuFailure(error: Throwable): Boolean {
        val messages = mutableListOf<String>()
        var current: Throwable? = error
        while (current != null) {
            current.message?.let { messages.add(it.lowercase()) }
            current = current.cause
        }
        val combined = messages.joinToString(" ")
        return combined.contains("opencl") ||
            combined.contains("litertlmjniexception") ||
            error::class.simpleName?.contains("LiteRtLm", ignoreCase = true) == true
    }

    override suspend fun startConversation(systemInstruction: String, isDeterministic: Boolean) {
        check(modelUseAllowed) { "AI engine is not active" }
        lastSystemInstruction = systemInstruction
        this.isDeterministic = isDeterministic
        val samplerConfig = if (isDeterministic) {
            SamplerConfig(topK = 1, topP = 1.0, temperature = 0.0)
        } else {
            SamplerConfig(topK = 10, topP = 0.95, temperature = 0.8)
        }
        val config = ConversationConfig(
            systemInstruction = Contents.of(systemInstruction),
            samplerConfig = samplerConfig,
        )

        closeActiveConversation()
        activeConversation = lmEngine?.createConversation(config)
    }

    override suspend fun sendMessage(prompt: String): Flow<String> = flow {
        check(modelUseAllowed) { "AI engine is not active" }
        try {
            emitAll(sendMessageInternal(prompt))
        } catch (e: Exception) {
            if (usingGpuBackend && isGpuFailure(e)) {
                Log.w("LiteRTEngine", "GPU inference failed during sendMessage, retrying on CPU")
                fallbackToCpu()
                val systemInstruction = lastSystemInstruction
                    ?: throw IllegalStateException("Conversation not initialized. Call startConversation() first.")
                startConversation(systemInstruction, isDeterministic)
                emitAll(sendMessageInternal(prompt))
            } else {
                throw e
            }
        }
    }

    private suspend fun sendMessageInternal(prompt: String): Flow<String> {
        val conversation = activeConversation
            ?: throw IllegalStateException("Conversation not initialized. Call startConversation() first.")
        val startTime = System.currentTimeMillis()
        analyticsTracker.trackEvent(AIAnalytics.InferenceStarted("lite_rt"))
        return conversation.sendMessageAsync(prompt).map {
            analyticsTracker.trackEvent(
                AIAnalytics.InferenceSuccess(
                    engine = "lite_rt",
                    durationMs = System.currentTimeMillis() - startTime,
                    backend = if (usingGpuBackend) "gpu" else "cpu"
                )
            )
            it.contents.toString()
        }
    }

    override fun resetConversation() {
        closeActiveConversation()
    }

    override fun release() {
        modelUseAllowed = false
        _state.value = EngineState.Uninitialized
        releaseLmEngine()
        modelPath = null
        lastSystemInstruction = null
        usingGpuBackend = false
    }
}
