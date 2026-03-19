package com.android.zubanx.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TranslateResponseDto(
    @SerialName("translatedText") val translatedText: String,
    @SerialName("sourceLang") val sourceLang: String,
    @SerialName("targetLang") val targetLang: String,
    @SerialName("detectedSourceLang") val detectedSourceLang: String? = null
)
