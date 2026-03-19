package com.android.zubanx.data.remote.api

import com.android.zubanx.core.network.NetworkResult
import com.android.zubanx.data.remote.dto.AiExpertResponseDto
import io.ktor.client.HttpClient

/**
 * Stub implementation of [AiExpertService].
 *
 * Always returns [NetworkResult.Error] with a "not implemented" message.
 * Real delegation to GPT / Gemini / Claude / Default backends is added
 * incrementally in Plans 5–8 as each feature is built.
 */
class AiExpertServiceImpl(
    private val client: HttpClient
) : AiExpertService {

    override suspend fun ask(
        expert: String,
        prompt: String
    ): NetworkResult<AiExpertResponseDto> {
        return NetworkResult.Error(
            "AiExpertService[$expert]: not implemented yet — see Plans 5-8"
        )
    }
}
