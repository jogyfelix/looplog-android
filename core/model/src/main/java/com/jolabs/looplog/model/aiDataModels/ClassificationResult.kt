package com.jolabs.looplog.model.aiDataModels

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ClassificationResult(
    @SerialName("is_safe") val isSafe: Boolean,
    @SerialName("confidence") val confidence: Float,
    @SerialName("detected_risk") val detectedRisk: String?,
    @SerialName("user_goal") val userGoal: String,
) {
    companion object {
        val UNKNOWN = ClassificationResult(
            isSafe = false,
            confidence = 0f,
            detectedRisk = "parse_failure",
            userGoal = "unsafe",
        )
    }
}
