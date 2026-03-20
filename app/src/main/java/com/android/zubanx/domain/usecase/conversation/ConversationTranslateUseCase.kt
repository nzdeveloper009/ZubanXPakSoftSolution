package com.android.zubanx.domain.usecase.conversation

import com.android.zubanx.core.network.NetworkResult
import com.android.zubanx.data.remote.api.TranslateApiService
import com.android.zubanx.data.remote.dto.TranslateResponseDto

class ConversationTranslateUseCase(
    private val apiService: TranslateApiService
) {
    suspend operator fun invoke(
        text: String,
        sourceLang: String,
        targetLang: String
    ): NetworkResult<TranslateResponseDto> {
        if (text.isBlank()) return NetworkResult.Error("Text must not be blank")
        return apiService.translate(text.trim(), sourceLang, targetLang)
    }
}
