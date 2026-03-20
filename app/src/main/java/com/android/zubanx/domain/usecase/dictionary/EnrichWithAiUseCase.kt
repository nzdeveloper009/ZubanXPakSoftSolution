package com.android.zubanx.domain.usecase.dictionary

import com.android.zubanx.core.network.NetworkResult
import com.android.zubanx.data.remote.api.AiExpertService

class EnrichWithAiUseCase(private val aiExpertService: AiExpertService) {
    suspend operator fun invoke(word: String, language: String, expert: String): NetworkResult<String> {
        if (expert == "DEFAULT") return NetworkResult.Error("AI enrichment requires a selected expert (GPT/Gemini/Claude)")
        val prompt = buildPrompt(word, language)
        return when (val result = aiExpertService.ask(expert, prompt)) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.content)
            is NetworkResult.Error -> result
        }
    }

    private fun buildPrompt(word: String, language: String): String =
        "Explain the word \"$word\" in the $language language. Include: " +
        "1) Clear definition in simple terms, " +
        "2) 2-3 example sentences, " +
        "3) Common synonyms, " +
        "4) Usage tips or common mistakes. " +
        "Be concise and practical."
}
