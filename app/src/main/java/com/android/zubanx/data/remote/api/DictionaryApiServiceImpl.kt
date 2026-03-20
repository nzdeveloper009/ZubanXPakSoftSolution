package com.android.zubanx.data.remote.api

import com.android.zubanx.core.network.NetworkResult
import com.android.zubanx.core.network.safeApiCall
import com.android.zubanx.data.remote.dto.DictionaryResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class DictionaryApiServiceImpl(
    private val client: HttpClient
) : DictionaryApiService {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun lookup(
        word: String,
        language: String
    ): NetworkResult<DictionaryResponseDto> = safeApiCall {
        val raw = client.get("$BASE_URL/$language/${word.trim().lowercase()}").bodyAsText()
        parseFirstEntry(raw, json)
            ?: throw IllegalStateException("No dictionary entry found for '$word'")
    }

    companion object {
        const val BASE_URL = "https://api.dictionaryapi.dev/api/v2/entries"

        fun parseFirstEntry(raw: String, json: Json = Json { ignoreUnknownKeys = true }): DictionaryResponseDto? {
            // The API returns an error object (not an array) for unknown words/phrases
            if (!raw.trimStart().startsWith('[')) return null
            val list = json.decodeFromString(ListSerializer(DictionaryResponseDto.serializer()), raw)
            return list.firstOrNull()
        }
    }
}
