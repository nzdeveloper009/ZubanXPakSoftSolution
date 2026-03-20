package com.android.zubanx.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenAiRequestDto(
    @SerialName("model") val model: String,
    @SerialName("messages") val messages: List<OpenAiMessage>,
    @SerialName("max_tokens") val maxTokens: Int = 1024,
    @SerialName("temperature") val temperature: Double = 0.3
)

@Serializable
data class OpenAiMessage(
    @SerialName("role") val role: String,
    @SerialName("content") val content: String
)
