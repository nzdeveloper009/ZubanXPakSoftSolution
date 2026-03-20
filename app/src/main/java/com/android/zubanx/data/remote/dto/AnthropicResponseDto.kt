package com.android.zubanx.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AnthropicResponseDto(
    @SerialName("content") val content: List<AnthropicContentBlock>,
    @SerialName("stop_reason") val stopReason: String? = null,
    @SerialName("usage") val usage: AnthropicUsage? = null
)

@Serializable
data class AnthropicContentBlock(
    @SerialName("type") val type: String,
    @SerialName("text") val text: String
)

@Serializable
data class AnthropicUsage(
    @SerialName("input_tokens") val inputTokens: Int,
    @SerialName("output_tokens") val outputTokens: Int
)
