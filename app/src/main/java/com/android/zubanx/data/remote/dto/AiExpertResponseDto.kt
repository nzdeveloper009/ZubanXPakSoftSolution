package com.android.zubanx.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AiExpertResponseDto(
    @SerialName("expert") val expert: String,
    @SerialName("content") val content: String,
    @SerialName("tokensUsed") val tokensUsed: Int? = null,
    @SerialName("errorMessage") val errorMessage: String? = null
)
