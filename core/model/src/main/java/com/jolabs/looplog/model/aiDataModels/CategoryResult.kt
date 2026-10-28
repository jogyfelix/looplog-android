package com.jolabs.looplog.model.aiDataModels

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CategoryResult(
    @SerialName("selected_ids")  val selectedIds: List<String>,
    @SerialName("confidence")    val confidence: Float,
) {
    companion object {
        // Safe fallback — used when parsing fails or model misbehaves
        val UNKNOWN = CategoryResult(
            selectedIds = emptyList(),
            confidence = 0f,
        )
    }
}