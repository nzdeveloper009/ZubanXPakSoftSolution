package com.android.zubanx.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GeminiResponseDto(
    @SerialName("candidates") val candidates: List<GeminiCandidate>
)

@Serializable
data class GeminiCandidate(
    @SerialName("content") val content: GeminiContent,
    @SerialName("finishReason") val finishReason: String? = null
)
