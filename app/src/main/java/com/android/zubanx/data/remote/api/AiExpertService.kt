package com.android.zubanx.data.remote.api

import com.android.zubanx.core.network.NetworkResult
import com.android.zubanx.data.remote.dto.AiExpertResponseDto

/**
 * Unified AI expert service interface.
 *
 * [expert] must be one of: `"GPT"`, `"GEMINI"`, `"CLAUDE"`, `"DEFAULT"`.
 * The implementation delegates to the correct backend based on [expert].
 *
 * This service is used for:
 * - AI-powered translation (when the user selects a non-default expert)
 * - Dictionary detail enrichment (Plans 6)
 * - Idiom / phrase / story explanations (Plans 7–8)
 *
 * Full implementation is added in Plans 5–8 as each feature is built.
 */
interface AiExpertService {
    suspend fun ask(expert: String, prompt: String): NetworkResult<AiExpertResponseDto>
}
