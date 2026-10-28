package com.jolabs.looplog.ai_engine

import com.jolabs.looplog.analytics.model.AnalyticsEvent
import com.jolabs.looplog.analytics.model.ErrorSource
import com.jolabs.looplog.analytics.model.Feature

object AIAnalytics {
    val Feature = Feature("ai")

    object Sources {
        val Nano = ErrorSource("ai_nano")
        val LiteRT = ErrorSource("ai_litert")
        val Orchestrator = ErrorSource("ai_orchestrator")
    }

    data class InitializationStarted(val engine: String) : AnalyticsEvent {
        override val name = "ai_initialization_started"
        override val params = mapOf("engine" to engine)
    }

    data class InitializationSuccess(val engine: String, val durationMs: Long) : AnalyticsEvent {
        override val name = "ai_initialization_success"
        override val params = mapOf("engine" to engine, "duration_ms" to durationMs)
    }

    data class ModelDownloadStarted(val engine: String) : AnalyticsEvent {
        override val name = "ai_model_download_started"
        override val params = mapOf("engine" to engine)
    }

    data class ModelDownloadSuccess(val engine: String) : AnalyticsEvent {
        override val name = "ai_model_download_success"
        override val params = mapOf("engine" to engine)
    }

    data class InferenceStarted(val engine: String) : AnalyticsEvent {
        override val name = "ai_inference_started"
        override val params = mapOf("engine" to engine)
    }

    data class InferenceSuccess(
        val engine: String,
        val durationMs: Long,
        val backend: String? = null
    ) : AnalyticsEvent {
        override val name = "ai_inference_success"
        override val params = buildMap {
            put("engine", engine)
            put("duration_ms", durationMs)
            backend?.let { put("backend", it) }
        }
    }

    data class GpuFallback(val reason: String) : AnalyticsEvent {
        override val name = "ai_gpu_fallback"
        override val params = mapOf("reason" to reason)
    }

    data class GenerationSuccess(val count: Int) : AnalyticsEvent {
        override val name = "ai_generation_success"
        override val params = mapOf("count" to count)
    }

    data object GenerationFailure : AnalyticsEvent {
        override val name = "ai_generation_failure"
    }

    data class HabitsAdded(val count: Int) : AnalyticsEvent {
        override val name = "ai_habits_added"
        override val params = mapOf("count" to count)
    }
}
