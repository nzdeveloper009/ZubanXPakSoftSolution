package com.android.zubanx.data.remote.api

import com.android.zubanx.core.network.NetworkResult
import com.android.zubanx.data.remote.dto.DictionaryResponseDto

/**
 * Network service for the Free Dictionary API.
 *
 * Endpoint: `GET https://api.dictionaryapi.dev/api/v2/entries/{language}/{word}`
 *
 * Returns the first entry from the API's list response. Full implementation
 * is added in Plan 6 (Dictionary feature).
 */
interface DictionaryApiService {
    suspend fun lookup(word: String, language: String): NetworkResult<DictionaryResponseDto>
}
