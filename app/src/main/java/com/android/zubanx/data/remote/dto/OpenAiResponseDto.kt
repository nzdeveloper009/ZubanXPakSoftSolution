package com.android.zubanx.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenAiResponseDto(
    @SerialName("choices") val choices: List<OpenAiChoice>,
    @SerialName("usage") val usage: OpenAiUsage? = null
)

@Serializable
data class OpenAiChoice(
    @SerialName("message") val message: OpenAiMessage,
    @SerialName("finish_reason") val finishReason: String? = null
)

@Serializable
data class OpenAiUsage(
    @SerialName("prompt_tokens") val promptTokens: Int,
    @SerialName("completion_tokens") val completionTokens: Int,
    @SerialName("total_tokens") val totalTokens: Int
)
