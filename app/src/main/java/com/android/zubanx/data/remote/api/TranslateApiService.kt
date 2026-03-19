package com.android.zubanx.data.remote.api

import com.android.zubanx.core.network.NetworkResult
import com.android.zubanx.data.remote.dto.TranslateResponseDto

/**
 * Network service for text translation.
 *
 * The default backend is Google Translate scraping. When the user selects an AI expert
 * (GPT/Gemini/Claude), [AiExpertService] is used instead. This service handles only
 * the scraping path.
 *
 * Implementation is a stub — real scraping logic is added in Plan 5.
 */
interface TranslateApiService {
    suspend fun translate(
        text: String,
        sourceLang: String,
        targetLang: String
    ): NetworkResult<TranslateResponseDto>
}
