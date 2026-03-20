package com.android.zubanx.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AnthropicRequestDto(
    @SerialName("model") val model: String,
    @SerialName("max_tokens") val maxTokens: Int,
    @SerialName("messages") val messages: List<AnthropicMessage>
)

@Serializable
data class AnthropicMessage(
    @SerialName("role") val role: String,
    @SerialName("content") val content: String
)
