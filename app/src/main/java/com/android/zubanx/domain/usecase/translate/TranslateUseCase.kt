package com.android.zubanx.domain.usecase.translate

import com.android.zubanx.core.network.NetworkResult
import com.android.zubanx.data.remote.api.AiExpertService
import com.android.zubanx.data.remote.api.AiExpertServiceImpl
import com.android.zubanx.data.remote.api.TranslateApiService
import com.android.zubanx.data.remote.dto.TranslateResponseDto
import com.android.zubanx.domain.model.Translation
import com.android.zubanx.domain.repository.TranslationRepository

/**
 * Orchestrates text translation.
 *
 * - `expert == "DEFAULT"` → Google Translate scraping via [TranslateApiService]
 * - `expert == "GPT" | "GEMINI" | "CLAUDE"` → AI call via [AiExpertService]
 *
 * On success, automatically saves the result to [TranslationRepository].
 */
class TranslateUseCase(
    private val translateApiService: TranslateApiService,
    private val aiExpertService: AiExpertService,
    private val translationRepository: TranslationRepository
) {
    suspend operator fun invoke(
        text: String,
        sourceLang: String,
        targetLang: String,
        expert: String
    ): NetworkResult<TranslateResponseDto> {
        if (text.isBlank()) return NetworkResult.Error("Text must not be blank")

        val result = if (expert == "DEFAULT") {
            translateApiService.translate(text, sourceLang, targetLang)
        } else {
            val prompt = AiExpertServiceImpl.buildTranslationPrompt(text, sourceLang, targetLang)
            when (val aiResult = aiExpertService.ask(expert, prompt)) {
                is NetworkResult.Success -> NetworkResult.Success(
                    TranslateResponseDto(
                        translatedText = aiResult.data.content,
                        sourceLang = sourceLang,
                        targetLang = targetLang
                    )
                )
                is NetworkResult.Error -> aiResult
            }
        }

        if (result is NetworkResult.Success) {
            translationRepository.saveTranslation(
                Translation(
                    sourceText = text,
                    translatedText = result.data.translatedText,
                    sourceLang = sourceLang,
                    targetLang = targetLang,
                    expert = expert,
                    timestamp = System.currentTimeMillis()
                )
            )
        }

        return result
    }
}
