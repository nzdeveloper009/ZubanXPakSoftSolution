package com.android.zubanx.data.remote.api

import com.android.zubanx.core.network.NetworkResult
import com.android.zubanx.data.remote.dto.TranslateResponseDto
import io.ktor.client.HttpClient

/**
 * Stub implementation of [TranslateApiService].
 *
 * Returns [NetworkResult.Error] with a "not implemented" message until Plan 5
 * adds the actual Google Translate scraping logic.
 */
class TranslateApiServiceImpl(
    private val client: HttpClient
) : TranslateApiService {

    override suspend fun translate(
        text: String,
        sourceLang: String,
        targetLang: String
    ): NetworkResult<TranslateResponseDto> {
        return NetworkResult.Error("TranslateApiService: not implemented yet — see Plan 5")
    }
}
