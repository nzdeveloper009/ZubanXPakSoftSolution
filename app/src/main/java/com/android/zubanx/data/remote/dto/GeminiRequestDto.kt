package com.android.zubanx.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GeminiRequestDto(
    @SerialName("contents") val contents: List<GeminiContent>
)

@Serializable
data class GeminiContent(
    @SerialName("parts") val parts: List<GeminiPart>,
    @SerialName("role") val role: String = "user"
)

@Serializable
data class GeminiPart(
    @SerialName("text") val text: String
)
