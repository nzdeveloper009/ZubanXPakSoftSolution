package com.android.zubanx.data.remote.api

import com.android.zubanx.core.network.NetworkResult
import com.android.zubanx.data.remote.dto.DictionaryResponseDto
import io.ktor.client.HttpClient

/**
 * Stub implementation of [DictionaryApiService].
 *
 * Returns [NetworkResult.Error] with a "not implemented" message until Plan 6
 * adds the actual Free Dictionary API call.
 */
class DictionaryApiServiceImpl(
    private val client: HttpClient
) : DictionaryApiService {

    override suspend fun lookup(
        word: String,
        language: String
    ): NetworkResult<DictionaryResponseDto> {
        return NetworkResult.Error("DictionaryApiService: not implemented yet — see Plan 6")
    }
}
